package com.mg.structuralai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** One-button autonomous flow with persistent CAD session recovery and safe ZIP batch analysis. */
public final class AutoMainActivity extends Activity {
    private static final int PICK_MODEL=2101;
    private static final String PREFS="structural_ai_session";
    private static final String KEY_URI="last_model_uri";
    private static final String KEY_ZIP_ENTRY="last_zip_entry";
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private TextView status,report,badge;
    private InteractiveModelView viewport;
    private MeshModel model;
    private String modelName="model";
    private AssemblyBodyDecomposer.Result assembly;
    private ContactCandidateEngine.Result contacts;
    private CadImportGateway.ImportedModel importedModel;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(6,56,6,8);root.setBackgroundColor(Color.rgb(8,18,31));
        TextView title=new TextView(this);title.setText("STRUCTURAL AI • AUTONOMOUS");title.setTextColor(Color.WHITE);title.setTextSize(22);title.setGravity(Gravity.CENTER);root.addView(title);
        TextView sub=new TextView(this);sub.setText("v"+BuildConfig.VERSION_NAME+" • PRODUCTION MESH + ZIP BATCH ANALYSIS • OCCT ARM64");sub.setTextColor(Color.LTGRAY);sub.setGravity(Gravity.CENTER);root.addView(sub);
        badge=new TextView(this);badge.setText("MODE: WAITING FOR MODEL");badge.setTextColor(Color.WHITE);badge.setGravity(Gravity.CENTER);badge.setPadding(10,8,10,8);badge.setBackgroundColor(Color.rgb(70,70,78));root.addView(badge);
        viewport=new InteractiveModelView(this);root.addView(viewport,new LinearLayout.LayoutParams(-1,520));
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
        Button choose=new Button(this);choose.setText("MODEL / ZIP");choose.setOnClickListener(v->pickModel());actions.addView(choose,new LinearLayout.LayoutParams(0,-2,1));
        Button auto=new Button(this);auto.setText("AUTO ANALYZE");auto.setOnClickListener(v->runAuto());actions.addView(auto,new LinearLayout.LayoutParams(0,-2,1));
        Button manual=new Button(this);manual.setText("MANUEL");manual.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));actions.addView(manual,new LinearLayout.LayoutParams(0,-2,1));root.addView(actions);
        status=new TextView(this);status.setText("ZIP: tek model seç veya TÜMÜNÜ sırayla analiz et • STEP/STP/IGES/IGS/BREP/STL/OBJ");status.setTextColor(Color.rgb(120,220,190));status.setPadding(4,8,4,5);root.addView(status);
        Button fullReport=new Button(this);fullReport.setText("RAPORU TAM EKRAN OKU / KAYDIR");fullReport.setOnClickListener(v->showFullReport());root.addView(fullReport);
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setVerticalScrollBarEnabled(true);sc.setScrollbarFadingEnabled(false);report=new TextView(this);report.setTextColor(Color.WHITE);report.setTextSize(12);report.setTextIsSelectable(true);report.setPadding(6,6,6,18);
        report.setText("Production Mesh / Archive Gate:\n1) Direct CAD/mesh veya ZIP arşivi\n2) ZIP içindeki desteklenen modeller listelenir\n3) Çoklu ZIP tek dokunuşla batch analiz edilebilir\n4) Zip-slip / archive-size safety gate\n5) Her model bağımsız import/topology/contact/FEM gate'inden geçer\n6) Bir modelin hatası diğer modele sahte PASS vermez\n\nGerçek servis yükü veya malzeme uydurulmaz.");
        sc.addView(report,new ScrollView.LayoutParams(-1,-2));root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        restoreLastModel();
    }

    private void showFullReport(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setVerticalScrollBarEnabled(true);sc.setScrollbarFadingEnabled(false);
        TextView tv=new TextView(this);tv.setText(report==null?"":report.getText());tv.setTextColor(Color.WHITE);tv.setTextSize(13);tv.setTextIsSelectable(true);tv.setPadding(24,18,24,40);tv.setBackgroundColor(Color.rgb(8,18,31));sc.addView(tv,new ScrollView.LayoutParams(-1,-2));
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Analiz / Engineering Raporu").setView(sc).setPositiveButton("KAPAT",null).create();
        d.setOnShowListener(x->{if(d.getWindow()!=null)d.getWindow().setLayout(-1,-1);});d.show();if(d.getWindow()!=null)d.getWindow().setLayout(-1,-1);
    }

    private void pickModel(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,PICK_MODEL);}
    @Override protected void onActivityResult(int rq,int rc,Intent data){super.onActivityResult(rq,rc,data);if(rq!=PICK_MODEL||rc!=RESULT_OK||data==null)return;Uri uri=data.getData();if(uri==null)return;try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString(KEY_URI,uri.toString()).remove(KEY_ZIP_ENTRY).apply();loadModel(uri,false);}
    private void restoreLastModel(){String s=getSharedPreferences(PREFS,MODE_PRIVATE).getString(KEY_URI,null);if(s==null||s.isEmpty())return;try{loadModel(Uri.parse(s),true);}catch(Exception e){getSharedPreferences(PREFS,MODE_PRIVATE).edit().remove(KEY_URI).remove(KEY_ZIP_ENTRY).apply();}}
    private void loadModel(Uri uri,boolean restoring){String outerName=getName(uri);if(ZipModelArchive.isZipName(outerName)){loadZip(uri,outerName,restoring);return;}modelName=outerName;status.setText(restoring?"Oturum geri yükleniyor: "+modelName:"Model okunuyor: "+modelName);badge.setText("MODE: IMPORT + GEOMETRY QA");executor.submit(()->{try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IllegalStateException("Model stream açılamadı");importModelStream(modelName,in,restoring,null);}catch(Exception e){showImportBlocked(e);}});}
    private void loadZip(Uri uri,String zipName,boolean restoring){badge.setText("MODE: ZIP ARCHIVE QA");status.setText(restoring?"ZIP oturumu geri yükleniyor: "+zipName:"ZIP içeriği taranıyor: "+zipName);String saved=restoring?getSharedPreferences(PREFS,MODE_PRIVATE).getString(KEY_ZIP_ENTRY,null):null;executor.submit(()->{try{if(saved!=null&&!saved.isEmpty()){extractAndImportZipEntry(uri,zipName,saved,true);return;}List<String> entries;try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IllegalStateException("ZIP stream açılamadı");entries=ZipModelArchive.listSupported(in);}if(entries.isEmpty())throw new IllegalStateException("ZIP içinde desteklenen STEP/STP/IGES/IGS/BREP/STL/OBJ modeli yok");if(entries.size()==1){extractAndImportZipEntry(uri,zipName,entries.get(0),restoring);return;}runOnUiThread(()->showZipEntryChooser(uri,zipName,entries));}catch(Exception e){showImportBlocked(e);}});}

    private void showZipEntryChooser(Uri uri,String zipName,List<String> entries){
        String[] items=new String[entries.size()+1];items[0]="▶ TÜM MODELLERİ SIRAYLA ANALİZ ET ("+entries.size()+")";for(int i=0;i<entries.size();i++)items[i+1]=entries.get(i);
        new AlertDialog.Builder(this).setTitle("ZIP analiz modu").setMessage(zipName+" • "+entries.size()+" desteklenen model bulundu").setItems(items,(d,which)->{
            if(which==0){runZipBatch(uri,zipName);return;}
            String entry=entries.get(which-1);status.setText("ZIP modeli açılıyor: "+entry);badge.setText("MODE: ZIP EXTRACT + CAD IMPORT");executor.submit(()->{try{extractAndImportZipEntry(uri,zipName,entry,false);}catch(Exception e){showImportBlocked(e);}});
        }).setNegativeButton("İptal",null).show();
    }

    private void runZipBatch(Uri uri,String zipName){
        getSharedPreferences(PREFS,MODE_PRIVATE).edit().remove(KEY_ZIP_ENTRY).apply();badge.setText("MODE: ZIP BATCH ANALYSIS");badge.setBackgroundColor(Color.rgb(70,85,145));status.setText("ZIP modelleri sırayla analiz ediliyor…");report.setText("ZIP BATCH ANALYSIS\n"+zipName+"\n\nBaşlatılıyor…");
        executor.submit(()->{try{
            ZipBatchAnalysisEngine.Result br=ZipBatchAnalysisEngine.analyze(()->getContentResolver().openInputStream(uri),getCacheDir(),new ZipBatchAnalysisEngine.Listener(){
                @Override public void onEntryStarted(int index,int total,String entry){runOnUiThread(()->{badge.setText("MODE: ZIP BATCH "+index+" / "+total);status.setText("["+index+"/"+total+"] "+entry+" • import/QA");});}
                @Override public void onGeometryReady(int index,int total,String entry,MeshModel m,AssemblyBodyDecomposer.Result b,ContactCandidateEngine.Result c){runOnUiThread(()->{viewport.setModel(m);status.setText("["+index+"/"+total+"] "+entry+" • bodies="+b.bodies.size()+" • contacts="+c.activeCandidates());});}
                @Override public void onSolverMesh(int index,int total,String entry,String scenario,int cells,TetMeshData mesh,MeshQualityReport quality){runOnUiThread(()->{viewport.setMeshPreview(mesh);badge.setText("ZIP "+index+"/"+total+" • LIVE MESH "+cells);status.setText(entry+" • "+scenario+" • "+cells+" cells • TET4="+mesh.tets.size());});}
                @Override public void onEntryFinished(int index,int total,ZipBatchAnalysisEngine.EntryResult r){runOnUiThread(()->{if(r.displayMesh!=null&&r.displayFem!=null)viewport.setResult(r.displayMesh,r.displayFem);status.setText("["+index+"/"+total+"] "+r.summary());});}
            });
            runOnUiThread(()->{report.setText(br.summary+"\n\nHer ZIP entry bağımsız safety/engineering gate ile değerlendirildi. QA_ONLY, BLOCKED veya ERROR bir numerical PASS değildir.");boolean clean=br.blocked==0&&br.error==0;badge.setText(clean?"MODE: ZIP BATCH COMPLETE":"MODE: ZIP BATCH COMPLETE • REVIEW");badge.setBackgroundColor(clean?Color.rgb(20,125,70):Color.rgb(145,90,35));status.setText("ZIP tamamlandı • PASS="+br.numericalPass+" • QA_ONLY="+br.qaOnly+" • BLOCKED="+br.blocked+" • ERROR="+br.error);});
        }catch(Exception e){runOnUiThread(()->{badge.setText("MODE: ZIP BATCH ERROR");badge.setBackgroundColor(Color.rgb(145,45,45));status.setText("ZIP batch durdu");report.setText("ZIP BATCH ERROR\n"+e.getMessage());});}});
    }

    private void extractAndImportZipEntry(Uri uri,String zipName,String entry,boolean restoring) throws Exception {File extracted;try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IllegalStateException("ZIP stream açılamadı");extracted=ZipModelArchive.extract(in,entry,getCacheDir());}getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString(KEY_ZIP_ENTRY,entry).apply();modelName=new File(entry).getName();try(InputStream modelIn=new FileInputStream(extracted)){importModelStream(modelName,modelIn,restoring,"ZIP: "+zipName+" → "+entry);}finally{if(!extracted.delete())extracted.deleteOnExit();}}
    private void importModelStream(String name,InputStream in,boolean restoring,String archiveSource) throws Exception {CadImportGateway.ImportedModel im=CadImportGateway.read(name,in,getCacheDir());MeshModel m=im.mesh;SurfaceTopologyReport t=SurfaceTopologyReport.evaluate(m);if(!t.closedManifold)throw new IllegalStateException("Kapalı/manifold değil: "+t.summary());AssemblyBodyDecomposer.Result ad=AssemblyBodyDecomposer.decompose(m);ContactCandidateEngine.Result cr=ContactCandidateEngine.analyze(m,ad);model=m;assembly=ad;contacts=cr;importedModel=im;String ar=assemblyReport(ad,cr),provenance=provenanceReport(im,m);runOnUiThread(()->{viewport.setModel(m);status.setText(restoring?"Oturum geri yüklendi — AUTO ANALYZE hazır":(ad.isAssembly()?"Assembly bulundu — contact gate hazır":"Tek gövde hazır — AUTO ANALYZE'a bas"));badge.setText(ad.isAssembly()?"MODE: ASSEMBLY / CONTACT QA":"MODE: MODEL READY");badge.setBackgroundColor(ad.isAssembly()?Color.rgb(135,90,25):Color.rgb(20,105,105));String archive=archiveSource==null?"":"\nARCHIVE SOURCE\n"+archiveSource+"\n";report.setText("MODEL READY\n\n"+name+"\n"+t.summary()+archive+"\n\n"+provenance+"\n\nNATIVE CAD METADATA\n"+im.metadataSummary+"\n"+im.assemblySummary+"\n\nACTUAL MODEL BODY / CONTACT QA\n"+ar+(restoring?"\n\nSESSION RESTORE: PASS":""));});}
    private void showImportBlocked(Exception e){runOnUiThread(()->{badge.setText("MODE: IMPORT / GEOMETRY BLOCKED");badge.setBackgroundColor(Color.rgb(145,45,45));status.setText("Model güvenli şekilde bloke edildi");report.setText("IMPORT / GEOMETRY GATE\n"+e.getMessage()+"\n\nZIP güvenliği, exact CAD transferi veya geometri QA başarısızsa analiz başlatılmaz.");});}

    private void runAuto(){
        if(model==null){status.setText("Önce model seç");return;}if(assembly!=null&&assembly.isAssembly()){runAssemblyAuto();return;}
        badge.setText("MODE: MULTI-BENCHMARK SELF TEST");badge.setBackgroundColor(Color.rgb(80,70,135));status.setText("Solver + contact + production mesh regression çalışıyor…");final String actualQa=assemblyReport(assembly,contacts),provenance=provenanceReport(importedModel,model);
        executor.submit(()->{try{AutonomousRegressionGate.Result rg=AutonomousRegressionGate.run();ProfileRegressionGate.Result pg=ProfileRegressionGate.run();if(!rg.pass||!pg.pass){runOnUiThread(()->{badge.setText("MODE: ENGINEERING GATE BLOCKED");badge.setBackgroundColor(Color.rgb(145,45,45));status.setText("Dahili engineering verification başarısız");report.setText("ENGINEERING TRUST STATE: NUMERICAL_BLOCKED — MULTI-GATE FAILED\n\nIN-APP FEM / THEORY / CONTACT / MESH REGRESSION\n"+rg.summary+"\n\nPROFILE REGRESSION\n"+pg.summary);});return;}
            runOnUiThread(()->{badge.setText("MODE: LIVE SOLVER MESH");badge.setBackgroundColor(Color.rgb(25,80,130));status.setText("Engineering Gate PASS → gerçek convergence mesh zinciri başlıyor…");});final StringBuilder liveTrace=new StringBuilder();AutonomousAnalysisRunner.Result r=AutonomousAnalysisRunner.run(model,(scenario,cells,mesh,quality)->{synchronized(liveTrace){liveTrace.append(scenario).append(" | level=").append(cells).append(" | nodes=").append(mesh.nodes.size()).append(" | TET4=").append(mesh.tets.size()).append(" | ").append(quality.pass?"QA PASS":"QA FAIL").append('\n');}runOnUiThread(()->{viewport.setMeshPreview(mesh);badge.setText("MODE: LIVE SOLVER MESH • "+cells);badge.setBackgroundColor(Color.rgb(25,80,130));status.setText(scenario+" • "+cells+" cells • nodes="+mesh.nodes.size()+" • TET4="+mesh.tets.size());report.setText("ENGINEERING TRUST STATE: ANALYSIS_IN_PROGRESS\n\nLIVE SOLVER CONVERGENCE MESH\nScenario: "+scenario+"\nLevel: "+cells+" cells/longest-axis\nNodes: "+mesh.nodes.size()+"\nTET4: "+mesh.tets.size()+"\nMesh QA: "+quality.summary()+"\n\nBu görüntü ayrı preview mesh değildir; solver'ın gerçek convergence mesh seviyesidir.\n\n"+provenance+"\n\nACTUAL MODEL BODY / CONTACT QA\n"+actualQa);});});String trace; synchronized(liveTrace){trace=liveTrace.toString();}final String finalTrace=trace;runOnUiThread(()->{viewport.setResult(r.displayMesh,r.displayFem);String trust=r.numericallyReady?"NUMERICAL_PASS / DESIGN_EVIDENCE_REQUIRED":"NUMERICAL_BLOCKED";report.setText("ENGINEERING TRUST STATE: "+trust+"\nNumerical convergence is authoritative here; material, service-load and allowable release remain separate evidence gates.\n\nLIVE SOLVER MESH TRACE\n"+finalTrace+"\nThe live meshes above were actual solver convergence meshes; final viewport now shows the selected solved mesh/result.\n\n"+provenance+"\n\nACTUAL MODEL BODY / CONTACT QA\n"+actualQa+"\n\nIN-APP FEM / THEORY / CONTACT / MESH REGRESSION\n"+rg.summary+"\n\nPROFILE REGRESSION\n"+pg.summary+"\n\n"+r.report);status.setText(r.numericallyReady?"Otonom numerik analiz tamamlandı — engineering gate PASS":"Otonom analiz QA tarafından bloke edildi");badge.setText(r.numericallyReady?"MODE: ENGINEERING TRUST NUMERICAL PASS":"MODE: ENGINEERING TRUST BLOCKED");badge.setBackgroundColor(r.numericallyReady?Color.rgb(20,125,70):Color.rgb(145,45,45));});}catch(Exception e){runOnUiThread(()->{badge.setText("MODE: AUTO ERROR");badge.setBackgroundColor(Color.rgb(145,45,45));status.setText("Otonom analiz durdu");report.setText("ENGINEERING TRUST STATE: NUMERICAL_BLOCKED\n\nHata: "+e.getMessage());});}});
    }

    private void runAssemblyAuto(){badge.setText("MODE: CONTACT REGRESSION + ASSEMBLY FEM");badge.setBackgroundColor(Color.rgb(80,70,135));status.setText("Contact kanıtı + yük aktarımı kontrol ediliyor…");final String ar=assemblyReport(assembly,contacts),provenance=provenanceReport(importedModel,model);executor.submit(()->{try{AssemblyAutonomousRunner.Result r=AssemblyAutonomousRunner.run(model);runOnUiThread(()->{viewport.setResult(r.mesh,r.fem);String trust=r.ready?"NUMERICAL_PASS / DESIGN_EVIDENCE_REQUIRED":"NUMERICAL_BLOCKED";report.setText("ENGINEERING TRUST STATE: "+trust+"\n\n"+provenance+"\n\nACTUAL MODEL BODY / CONTACT QA\n"+ar+"\n\n"+r.report);status.setText(r.ready?"Assembly numerik analizi tamamlandı":"Assembly numerik gate bloke edildi");badge.setText(r.ready?"MODE: ENGINEERING TRUST NUMERICAL PASS":"MODE: CONTACT QA BLOCKED");badge.setBackgroundColor(r.ready?Color.rgb(20,125,70):Color.rgb(145,45,45));});}catch(Exception e){runOnUiThread(()->{badge.setText("MODE: CONTACT QA BLOCKED");badge.setBackgroundColor(Color.rgb(145,45,45));status.setText("Assembly contact çözümü güvenli gate'i geçmedi");report.setText("ENGINEERING TRUST STATE: NUMERICAL_BLOCKED\n\n"+provenance+"\n\nACTUAL MODEL BODY / CONTACT QA\n"+ar+"\n\nCONTACT / ASSEMBLY SAFETY GATE\n"+e.getMessage()+"\n\nFrictionless, frictional, no-separation veya belirsiz temaslar çözüme zorlanmaz.");});}});}
    @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);if(viewport!=null)viewport.post(viewport::invalidate);}
    private String provenanceReport(CadImportGateway.ImportedModel im,MeshModel m){if(m==null)return "IMPORT PROVENANCE\nUnavailable";String fmt=im==null?m.sourceFormat:im.format.toString();String exact=im!=null&&im.exactCadSource?"YES":"NO";String unit=m.authoritativeUnit?("AUTHORITATIVE scale="+m.sourceUnitScaleM+" m/transferred-unit"):"NOT AUTHORITATIVE — parametric magnitude inference only";return "IMPORT PROVENANCE\nFormat: "+fmt+" | exact CAD source: "+exact+"\nUnit evidence: "+unit+"\n"+m.sourceUnitReason;}
    private String assemblyReport(AssemblyBodyDecomposer.Result a,ContactCandidateEngine.Result c){StringBuilder s=new StringBuilder();s.append("Actual imported-model bodies: ").append(a==null?0:a.bodies.size()).append("\n");if(a!=null)for(AssemblyBodyDecomposer.Body b:a.bodies)s.append(b.summary()).append("\n");if(c!=null){s.append("Actual model contact candidates: ").append(c.activeCandidates()).append("\n");for(ContactCandidateEngine.Pair p:c.pairs)if(p.state!=ContactCandidateEngine.State.FAR)s.append(p.summary()).append("\n");}s.append("Synthetic regression body counts, if shown later, are self-test fixtures and are NOT the imported-model body count.");return s.toString();}
    private String getName(Uri uri){String n="model";Cursor c=getContentResolver().query(uri,null,null,null,null);if(c!=null){try{int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(c.moveToFirst()&&i>=0)n=c.getString(i);}finally{c.close();}}return n;}
    @Override protected void onDestroy(){super.onDestroy();executor.shutdownNow();}
}
