package com.mg.structuralai;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Routes mesh formats to Java parser and exact CAD formats to the native OCCT kernel. */
public final class CadImportGateway {
    public enum Format { STEP, IGES, BREP, STL, OBJ, GLTF, GLB, PLY, VRML, UNKNOWN }
    public static final class ImportedModel {
        public final MeshModel mesh; public final Format format; public final boolean exactCadSource; public final String assemblySummary,metadataSummary;
        ImportedModel(MeshModel m,Format f,boolean exact,String assembly,String metadata){mesh=m;format=f;exactCadSource=exact;assemblySummary=assembly;metadataSummary=metadata;}
    }
    private CadImportGateway(){}
    public static Format detect(String name){String n=name==null?"":name.toLowerCase(Locale.US);if(n.endsWith(".step")||n.endsWith(".stp"))return Format.STEP;if(n.endsWith(".iges")||n.endsWith(".igs"))return Format.IGES;if(n.endsWith(".brep"))return Format.BREP;if(n.endsWith(".stl"))return Format.STL;if(n.endsWith(".obj"))return Format.OBJ;if(n.endsWith(".gltf"))return Format.GLTF;if(n.endsWith(".glb"))return Format.GLB;if(n.endsWith(".ply"))return Format.PLY;if(n.endsWith(".wrl")||n.endsWith(".vrml"))return Format.VRML;return Format.UNKNOWN;}

    /** Backward-compatible surface import. Exact CAD needs a writable cache directory. */
    public static ImportedModel read(String name,InputStream in) throws IOException {return read(name,in,null);}

    public static ImportedModel read(String name,InputStream in,File cacheDir) throws IOException {
        Format f=detect(name);
        if(f==Format.STL||f==Format.OBJ){
            MeshModel m=MeshParser.parse(name,in);
            m.setImportMetadata(f.toString(),false,Double.NaN,"STL/OBJ carries no authoritative unit metadata; magnitude inference is parametric only");
            return new ImportedModel(m,f,false,"single tessellated model","mesh format; no authoritative CAD assembly/material metadata");
        }
        if(f==Format.STEP||f==Format.IGES||f==Format.BREP){
            if(!NativeOcctBridge.isAvailable())throw new IllegalStateException("Exact "+f+" import requires the OCCT native CAD kernel; this APK build does not contain it.");
            if(cacheDir==null)throw new IllegalStateException("Exact CAD import requires application cache storage for the native reader.");
            if(!cacheDir.exists()&&!cacheDir.mkdirs())throw new IOException("CAD cache directory could not be created");
            File tmp=File.createTempFile("structural_ai_","."+f.toString().toLowerCase(Locale.US),cacheDir);
            long handle=0;
            try{
                copyBounded(in,tmp,150*1024*1024);
                handle=NativeOcctBridge.importCadFile(tmp.getAbsolutePath(),f.toString());
                if(handle==0)throw new IOException("OCCT could not translate the "+f+" model");
                String metadata=NativeOcctBridge.metadataJson(handle);
                String assembly=NativeOcctBridge.assemblyTreeJson(handle);
                String validity=NativeOcctBridge.validityReportJson(handle);
                String obj=NativeOcctBridge.tessellateToObj(handle,0.15,0.35);
                if(obj==null||obj.length()<32)throw new IOException("OCCT imported the CAD file but produced no usable tessellation");
                MeshModel m=MeshParser.parse("occt-transfer.obj",new ByteArrayInputStream(obj.getBytes(StandardCharsets.UTF_8)));
                // OCCT transfer is explicitly normalized to millimetres in the native bridge.
                m.setImportMetadata(f.toString(),true,0.001,"Authoritative CAD transfer: OCCT honored source units and normalized transferred geometry to millimetres");
                return new ImportedModel(m,f,true,assembly,metadata+" | "+validity);
            }finally{
                if(handle!=0)NativeOcctBridge.release(handle);
                if(!tmp.delete())tmp.deleteOnExit();
            }
        }
        throw new IllegalArgumentException("Unsupported CAD format: "+f+". P0: STEP, IGES, BREP, STL, OBJ.");
    }

    private static void copyBounded(InputStream in,File out,int maxBytes) throws IOException{
        byte[] b=new byte[65536];int total=0,r;
        try(FileOutputStream f=new FileOutputStream(out)){
            while((r=in.read(b))!=-1){total+=r;if(total>maxBytes)throw new IOException("CAD file exceeds 150 MB mobile import limit");f.write(b,0,r);}
        }
    }
}
