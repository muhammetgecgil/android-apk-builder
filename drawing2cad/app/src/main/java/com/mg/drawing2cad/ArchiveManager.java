package com.mg.drawing2cad;

import android.content.*;
import android.net.Uri;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public final class ArchiveManager {
  public static final long MAX_ENTRY=120L*1024L*1024L;
  public static final int MAX_ENTRIES=5000;

  public static ArrayList<String> list(Context c, Uri uri, boolean mode2d)throws IOException{
    ArrayList<String> out=new ArrayList<>();int count=0;
    try(InputStream in=c.getContentResolver().openInputStream(uri);ZipInputStream z=new ZipInputStream(new BufferedInputStream(in))){
      ZipEntry e;while((e=z.getNextEntry())!=null){if(++count>MAX_ENTRIES)throw new IOException("ZIP çok fazla girdi içeriyor");if(e.isDirectory())continue;String n=e.getName();if(!safeName(n))continue;String l=n.toLowerCase(Locale.ROOT);if(mode2d?is2d(l):is3d(l))out.add(n);}
    }
    Collections.sort(out,String.CASE_INSENSITIVE_ORDER);return out;
  }

  public static File extract(Context c, Uri uri, String wanted)throws IOException{
    if(!safeName(wanted))throw new IOException("Güvensiz ZIP yolu");File root=new File(c.getCacheDir(),"mgcad_zip");if(!root.exists()&&!root.mkdirs())throw new IOException("Cache klasörü oluşturulamadı");String ext=".bin";int p=wanted.lastIndexOf('.');if(p>=0&&p>wanted.lastIndexOf('/'))ext=wanted.substring(p).replaceAll("[^A-Za-z0-9.]","_");File out=new File(root,"e_"+Math.abs(wanted.hashCode())+ext);
    boolean found=false;try(InputStream in=c.getContentResolver().openInputStream(uri);ZipInputStream z=new ZipInputStream(new BufferedInputStream(in))){ZipEntry e;while((e=z.getNextEntry())!=null){if(!e.isDirectory()&&wanted.equals(e.getName())){long total=0;try(OutputStream o=new BufferedOutputStream(new FileOutputStream(out))){byte[] b=new byte[32768];int n;while((n=z.read(b))>0){total+=n;if(total>MAX_ENTRY)throw new IOException("ZIP girdisi boyut sınırını aştı");o.write(b,0,n);}}found=true;break;}}}
    if(!found)throw new IOException("ZIP girdisi bulunamadı");return out;
  }

  public static boolean safeName(String n){if(n==null||n.length()==0||n.startsWith("/")||n.startsWith("\\"))return false;String x=n.replace('\\','/');for(String p:x.split("/"))if("..".equals(p))return false;return true;}
  public static boolean is2d(String l){return l.endsWith(".pdf")||l.endsWith(".png")||l.endsWith(".jpg")||l.endsWith(".jpeg")||l.endsWith(".webp")||l.endsWith(".dxf");}
  public static boolean is3d(String l){return l.endsWith(".stl")||l.endsWith(".obj");}
  private ArchiveManager(){}
}
