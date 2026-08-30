package com.mg.structuralai;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Locks ZIP model discovery/extraction and zip-slip fail-closed behavior. */
public final class ZipArchiveRegressionGate {
    public static final class Result { public final boolean pass; public final String summary; Result(boolean p,String s){pass=p;summary=s;} }
    private ZipArchiveRegressionGate(){}

    public static Result run(){
        File dir=null;
        try{
            byte[] good=archive(false);
            List<String> names=ZipModelArchive.listSupported(new ByteArrayInputStream(good));
            boolean filtered=names.size()==2&&names.contains("models/a.stl")&&names.contains("models/b.obj");
            dir=new File(System.getProperty("java.io.tmpdir"),"structural_zip_reg_"+System.nanoTime());
            if(!dir.mkdirs())throw new IllegalStateException("temp dir create failed");
            File extracted=ZipModelArchive.extract(new ByteArrayInputStream(good),"models/a.stl",dir);
            boolean extractedOk=extracted.isFile()&&extracted.length()>0&&read(extracted).contains("solid regression");
            if(!extracted.delete())extracted.deleteOnExit();

            boolean slipBlocked=false;
            try{ZipModelArchive.listSupported(new ByteArrayInputStream(archive(true)));}catch(Exception expected){slipBlocked=true;}
            boolean missingBlocked=false;
            try{ZipModelArchive.extract(new ByteArrayInputStream(good),"models/missing.stl",dir);}catch(Exception expected){missingBlocked=true;}

            boolean pass=filtered&&extractedOk&&slipBlocked&&missingBlocked;
            return new Result(pass,"ZIP ARCHIVE REGRESSION "+(pass?"PASS":"FAIL")+" | supportedFiltering="+filtered+" | extraction="+extractedOk+" | zipSlipBlocked="+slipBlocked+" | missingEntryBlocked="+missingBlocked);
        }catch(Throwable t){return new Result(false,"ZIP ARCHIVE REGRESSION ERROR: "+safe(t));}
        finally{if(dir!=null)deleteTree(dir);}
    }

    private static byte[] archive(boolean malicious) throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(ZipOutputStream z=new ZipOutputStream(out)){
            put(z,"README.txt","ignore me");
            put(z,"models/a.stl","solid regression\nendsolid regression\n");
            put(z,"models/b.obj","o regression\nv 0 0 0\n");
            if(malicious)put(z,"../escape.stl","solid bad\nendsolid bad\n");
        }
        return out.toByteArray();
    }
    private static void put(ZipOutputStream z,String name,String body) throws Exception {z.putNextEntry(new ZipEntry(name));z.write(body.getBytes(StandardCharsets.UTF_8));z.closeEntry();}
    private static String read(File f) throws Exception {StringBuilder s=new StringBuilder();try(FileInputStream in=new FileInputStream(f)){byte[] b=new byte[1024];int n;while((n=in.read(b))>0)s.append(new String(b,0,n,StandardCharsets.UTF_8));}return s.toString();}
    private static void deleteTree(File f){if(f==null||!f.exists())return;if(f.isDirectory()){File[] a=f.listFiles();if(a!=null)for(File x:a)deleteTree(x);}if(!f.delete())f.deleteOnExit();}
    private static String safe(Throwable t){String s=t==null?null:t.getMessage();return s==null||s.isEmpty()?(t==null?"unknown":t.getClass().getSimpleName()):s;}
}
