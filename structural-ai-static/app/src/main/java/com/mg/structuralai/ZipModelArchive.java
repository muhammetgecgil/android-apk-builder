package com.mg.structuralai;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Safe CAD/mesh extraction from ZIP archives. Supports only analysis model formats and blocks zip-slip/bombs. */
public final class ZipModelArchive {
    public static final long MAX_ENTRY_BYTES=128L*1024L*1024L;
    public static final long MAX_TOTAL_BYTES=256L*1024L*1024L;
    public static final int MAX_ENTRIES=256;
    private static final Set<String> EXT=new HashSet<>(Arrays.asList("step","stp","iges","igs","brep","stl","obj"));
    private static final OutputStream DISCARD=new OutputStream(){@Override public void write(int b){} @Override public void write(byte[] b,int off,int len){}};
    private ZipModelArchive(){}

    public static List<String> listSupported(InputStream raw) throws IOException {
        ArrayList<String> out=new ArrayList<>();
        int count=0;
        try(ZipInputStream z=new ZipInputStream(new BufferedInputStream(raw))){
            ZipEntry e;
            while((e=z.getNextEntry())!=null){
                if(++count>MAX_ENTRIES)throw new IOException("ZIP çok fazla entry içeriyor (limit "+MAX_ENTRIES+")");
                if(e.isDirectory())continue;
                String safe=safeName(e.getName());
                if(isSupported(safe))out.add(safe);
            }
        }
        Collections.sort(out,String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    public static File extract(InputStream raw,String wanted,File cacheDir) throws IOException {
        String safeWanted=safeName(wanted);
        File dir=new File(cacheDir,"zip_models");
        if(!dir.exists()&&!dir.mkdirs())throw new IOException("ZIP cache klasörü oluşturulamadı");
        String base=new File(safeWanted).getName();
        if(!isSupported(base))throw new IOException("Desteklenmeyen ZIP modeli: "+base);
        File out=new File(dir,System.nanoTime()+"_"+base);
        long total=0; int count=0;
        try(ZipInputStream z=new ZipInputStream(new BufferedInputStream(raw))){
            ZipEntry e;
            while((e=z.getNextEntry())!=null){
                if(++count>MAX_ENTRIES)throw new IOException("ZIP entry limiti aşıldı");
                if(e.isDirectory())continue;
                String safe=safeName(e.getName());
                long entryBytes=0;
                OutputStream target=safe.equals(safeWanted)?new BufferedOutputStream(new FileOutputStream(out)):DISCARD;
                boolean closeTarget=target!=DISCARD;
                try{
                    byte[] buf=new byte[64*1024]; int n;
                    while((n=z.read(buf))>0){
                        entryBytes+=n; total+=n;
                        if(entryBytes>MAX_ENTRY_BYTES)throw new IOException("ZIP entry çok büyük (>128 MB)");
                        if(total>MAX_TOTAL_BYTES)throw new IOException("ZIP açılmış veri limiti aşıldı (>256 MB)");
                        target.write(buf,0,n);
                    }
                    target.flush();
                }finally{if(closeTarget)target.close();}
                if(safe.equals(safeWanted)){
                    if(out.length()==0){out.delete();throw new IOException("ZIP içindeki model boş");}
                    return out;
                }
            }
        }catch(IOException ex){out.delete();throw ex;}
        out.delete();throw new FileNotFoundException("ZIP entry bulunamadı: "+safeWanted);
    }

    public static boolean isZipName(String name){return name!=null&&name.toLowerCase(Locale.ROOT).endsWith(".zip");}
    public static boolean isSupported(String name){int p=name==null?-1:name.lastIndexOf('.');return p>=0&&EXT.contains(name.substring(p+1).toLowerCase(Locale.ROOT));}
    private static String safeName(String name) throws IOException {
        if(name==null||name.isEmpty())throw new IOException("Geçersiz ZIP entry");
        String n=name.replace('\\','/');
        if(n.startsWith("/")||n.contains("../")||n.equals("..")||n.contains(":"))throw new IOException("Güvensiz ZIP yolu engellendi: "+name);
        return n;
    }
}
