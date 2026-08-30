package com.mg.structuralai;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Safe multi-model ZIP analysis engine.
 *
 * Re-opens the archive for every extraction so ZipModelArchive remains the single
 * authority for zip-slip, entry-count and expanded-size limits. Each model is
 * independently imported and gated; one bad entry never converts another entry
 * into a false PASS.
 */
public final class ZipBatchAnalysisEngine {
    public interface ZipStreamProvider { InputStream open() throws Exception; }
    public interface Listener {
        void onEntryStarted(int index,int total,String entry);
        void onGeometryReady(int index,int total,String entry,MeshModel model,AssemblyBodyDecomposer.Result bodies,ContactCandidateEngine.Result contacts);
        void onSolverMesh(int index,int total,String entry,String scenario,int cells,TetMeshData mesh,MeshQualityReport quality);
        void onEntryFinished(int index,int total,EntryResult result);
    }

    public enum State { NUMERICAL_PASS, QA_ONLY, BLOCKED, ERROR }

    public static final class EntryResult {
        public final String entry;
        public final State state;
        public final boolean exactCad;
        public final boolean authoritativeUnit;
        public final int bodies;
        public final int activeContacts;
        public final TetMeshData displayMesh;
        public final StaticFemSolver.Result displayFem;
        public final String report;
        EntryResult(String entry,State state,boolean exactCad,boolean authoritativeUnit,int bodies,int activeContacts,TetMeshData displayMesh,StaticFemSolver.Result displayFem,String report){
            this.entry=entry;this.state=state;this.exactCad=exactCad;this.authoritativeUnit=authoritativeUnit;this.bodies=bodies;this.activeContacts=activeContacts;this.displayMesh=displayMesh;this.displayFem=displayFem;this.report=report;
        }
        public boolean numericalPass(){return state==State.NUMERICAL_PASS;}
        public String summary(){return entry+" | "+state+" | bodies="+bodies+" | contacts="+activeContacts+" | exactCAD="+exactCad+" | authoritativeUnit="+authoritativeUnit;}
    }

    public static final class Result {
        public final List<EntryResult> entries;
        public final int numericalPass,qaOnly,blocked,error;
        public final String summary;
        Result(List<EntryResult> entries){
            this.entries=Collections.unmodifiableList(new ArrayList<>(entries));
            int p=0,q=0,b=0,e=0;StringBuilder s=new StringBuilder("ZIP BATCH ANALYSIS\n");
            for(EntryResult r:entries){if(r.state==State.NUMERICAL_PASS)p++;else if(r.state==State.QA_ONLY)q++;else if(r.state==State.BLOCKED)b++;else e++;s.append(r.summary()).append('\n');}
            numericalPass=p;qaOnly=q;blocked=b;error=e;
            s.append(String.format(Locale.US,"TOTAL=%d | NUMERICAL_PASS=%d | QA_ONLY=%d | BLOCKED=%d | ERROR=%d",entries.size(),p,q,b,e));
            summary=s.toString();
        }
    }

    private ZipBatchAnalysisEngine(){}

    public static Result analyze(ZipStreamProvider provider,File cacheDir,Listener listener) throws Exception {
        if(provider==null)throw new IllegalArgumentException("ZIP stream provider null");
        if(cacheDir==null)throw new IllegalArgumentException("cacheDir null");
        final List<String> names;
        try(InputStream in=provider.open()){if(in==null)throw new IllegalStateException("ZIP stream açılamadı");names=ZipModelArchive.listSupported(in);}
        if(names.isEmpty())throw new IllegalStateException("ZIP içinde desteklenen analiz modeli yok");
        ArrayList<EntryResult> out=new ArrayList<>();
        for(int i=0;i<names.size();i++){
            String entry=names.get(i);int index=i+1;if(listener!=null)listener.onEntryStarted(index,names.size(),entry);
            EntryResult r=analyzeOne(provider,cacheDir,entry,index,names.size(),listener);
            out.add(r);if(listener!=null)listener.onEntryFinished(index,names.size(),r);
        }
        return new Result(out);
    }

    private static EntryResult analyzeOne(ZipStreamProvider provider,File cacheDir,String entry,int index,int total,Listener listener){
        File extracted=null;
        try{
            try(InputStream zip=provider.open()){if(zip==null)throw new IllegalStateException("ZIP stream açılamadı");extracted=ZipModelArchive.extract(zip,entry,cacheDir);}
            CadImportGateway.ImportedModel im;
            try(InputStream modelIn=new FileInputStream(extracted)){im=CadImportGateway.read(new File(entry).getName(),modelIn,cacheDir);}
            MeshModel m=im.mesh;
            SurfaceTopologyReport topo=SurfaceTopologyReport.evaluate(m);
            if(!topo.closedManifold)return result(entry,State.BLOCKED,im,m,null,null,null,null,"GEOMETRY BLOCKED\n"+topo.summary());
            AssemblyBodyDecomposer.Result bodies=AssemblyBodyDecomposer.decompose(m);
            ContactCandidateEngine.Result contacts=ContactCandidateEngine.analyze(m,bodies);
            if(listener!=null)listener.onGeometryReady(index,total,entry,m,bodies,contacts);

            if(bodies!=null&&bodies.isAssembly()){
                try{
                    AssemblyAutonomousRunner.Result a=AssemblyAutonomousRunner.run(m);
                    State state=a.ready?State.NUMERICAL_PASS:State.BLOCKED;
                    return result(entry,state,im,m,bodies,contacts,a.mesh,a.fem,"ASSEMBLY\n"+a.report);
                }catch(Exception ex){
                    return result(entry,State.BLOCKED,im,m,bodies,contacts,null,null,"ASSEMBLY SAFETY GATE BLOCKED\n"+safeMessage(ex));
                }
            }

            try{
                AutonomousAnalysisRunner.Result a=AutonomousAnalysisRunner.run(m,(scenario,cells,mesh,quality)->{
                    if(listener!=null)listener.onSolverMesh(index,total,entry,scenario,cells,mesh,quality);
                });
                State state=a.numericallyReady?State.NUMERICAL_PASS:State.BLOCKED;
                return result(entry,state,im,m,bodies,contacts,a.displayMesh,a.displayFem,a.report);
            }catch(Exception ex){
                // Geometry/contact QA remains useful evidence even when a safe autonomous
                // load case cannot be established. Do not call this a numerical PASS.
                return result(entry,State.QA_ONLY,im,m,bodies,contacts,null,null,"GEOMETRY/CONTACT QA PASS — AUTONOMOUS FEM NOT RELEASED\n"+safeMessage(ex));
            }
        }catch(Exception ex){
            return new EntryResult(entry,State.ERROR,false,false,0,0,null,null,"IMPORT ERROR\n"+safeMessage(ex));
        }finally{if(extracted!=null&&extracted.exists()&&!extracted.delete())extracted.deleteOnExit();}
    }

    private static EntryResult result(String entry,State state,CadImportGateway.ImportedModel im,MeshModel m,AssemblyBodyDecomposer.Result bodies,ContactCandidateEngine.Result contacts,TetMeshData mesh,StaticFemSolver.Result fem,String detail){
        int bc=bodies==null?0:bodies.bodies.size();int cc=contacts==null?0:contacts.activeCandidates();
        StringBuilder r=new StringBuilder();
        r.append(entry).append('\n').append("STATE: ").append(state).append('\n');
        r.append("Format: ").append(im==null?"unknown":im.format).append(" | exact CAD=").append(im!=null&&im.exactCadSource).append('\n');
        r.append("Unit evidence: ").append(m!=null&&m.authoritativeUnit?"AUTHORITATIVE":"NOT AUTHORITATIVE").append('\n');
        r.append("Bodies: ").append(bc).append(" | active contacts: ").append(cc).append('\n');
        if(detail!=null)r.append('\n').append(detail);
        return new EntryResult(entry,state,im!=null&&im.exactCadSource,m!=null&&m.authoritativeUnit,bc,cc,mesh,fem,r.toString());
    }

    private static String safeMessage(Throwable t){String s=t==null?null:t.getMessage();return s==null||s.trim().isEmpty()?(t==null?"unknown":t.getClass().getSimpleName()):s;}
}
