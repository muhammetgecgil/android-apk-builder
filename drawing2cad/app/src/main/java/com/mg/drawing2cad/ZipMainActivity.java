package com.mg.drawing2cad;

import android.content.*;
import android.net.Uri;
import android.app.AlertDialog;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** v2.2 ZIP-aware launcher: adds compressed-package support to both directions. */
public class ZipMainActivity extends MainActivity {

  @Override void pick(){
    Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
    i.addCategory(Intent.CATEGORY_OPENABLE);
    i.setType("*/*");
    if(mode==M23){
      i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{
        "image/*","application/pdf","application/zip","application/x-zip-compressed","application/octet-stream"
      });
    }else{
      i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{
        "model/stl","model/obj","application/zip","application/x-zip-compressed","application/octet-stream","text/plain"
      });
    }
    startActivityForResult(i,PICK);
  }

  @Override void load3d(Uri u)throws Exception{
    String n=fileName(u), l=n.toLowerCase(Locale.ROOT);
    if(l.endsWith(".zip")||l.endsWith(".cbz")||l.endsWith(".jar")){
      archiveUri=u;
      list3dArchive(u);
      return;
    }
    super.load3d(u);
  }

  void list3dArchive(Uri u)throws Exception{
    archiveEntries.clear();
    try(InputStream in=getContentResolver().openInputStream(u);
        ZipInputStream z=new ZipInputStream(new BufferedInputStream(in))){
      ZipEntry e;
      while((e=z.getNextEntry())!=null){
        if(e.isDirectory()) continue;
        String l=e.getName().toLowerCase(Locale.ROOT);
        if(l.endsWith(".stl")||l.endsWith(".obj")) archiveEntries.add(e.getName());
      }
    }
    if(archiveEntries.isEmpty()) throw new IOException("ZIP içinde STL/OBJ 3D model yok");
    String[] a=archiveEntries.toArray(new String[0]);
    new AlertDialog.Builder(this)
      .setTitle("ZIP içinden 3D model seç")
      .setItems(a,(q,w)->{
        try{ load3dArchiveEntry(a[w]); }
        catch(Exception ex){ status.setText("ZIP 3D hatası: "+ex.getMessage()); }
      })
      .setNegativeButton("İptal",null)
      .show();
  }

  void load3dArchiveEntry(String wanted)throws Exception{
    String low=wanted.toLowerCase(Locale.ROOT);
    File f=new File(getCacheDir(),"mg22_"+Math.abs(wanted.hashCode())+(low.endsWith(".obj")?".obj":".stl"));
    boolean ok=false;
    try(InputStream in=getContentResolver().openInputStream(archiveUri);
        ZipInputStream z=new ZipInputStream(new BufferedInputStream(in))){
      ZipEntry e;
      while((e=z.getNextEntry())!=null){
        if(!e.getName().equals(wanted)) continue;
        try(FileOutputStream o=new FileOutputStream(f)){
          byte[] b=new byte[32768]; int n; long total=0;
          while((n=z.read(b))>0){
            total+=n;
            if(total>160L*1024*1024) throw new IOException("ZIP içindeki model 160 MB sınırını aşıyor");
            o.write(b,0,n);
          }
        }
        ok=true; break;
      }
    }
    if(!ok) throw new IOException("ZIP girdisi bulunamadı");

    byte[] data;
    try(InputStream in=new FileInputStream(f); ByteArrayOutputStream o=new ByteArrayOutputStream()){
      byte[] b=new byte[32768]; int n;
      while((n=in.read(b))>0) o.write(b,0,n);
      data=o.toByteArray();
    }
    mesh = low.endsWith(".obj")
      ? parseObj(new String(data, StandardCharsets.UTF_8))
      : parseStl(data);
    if(mesh.isEmpty()) throw new IOException("ZIP içindeki 3D geometri okunamadı");
    source=null; voxels=null; cad.setMesh(mesh);
    fileInfo.setText("ZIP › "+wanted+" • "+mesh.size()+" üçgen");
    float[] b=bounds(mesh);
    status.setText(String.format(Locale.US,
      "✓ ZIP modeli açıldı • %.2f × %.2f × %.2f • teknik resim üretmeye hazır",
      b[3]-b[0],b[4]-b[1],b[5]-b[2]));
  }
}
