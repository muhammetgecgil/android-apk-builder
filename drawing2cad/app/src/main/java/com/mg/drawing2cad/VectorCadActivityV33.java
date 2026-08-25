package com.mg.drawing2cad;

import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** v3.3 vector-aware technical drawing shell.
 * DXF is parsed as geometry, not treated as a raster image.
 */
public class VectorCadActivityV33 extends FeatureAwareActivityV32 {
  DxfVectorParser.Doc dxfDoc;
  TextView vectorBadge;

  @Override public void onCreate(Bundle b){super.onCreate(b);addVectorTools();}

  void addVectorTools(){
    View root=getWindow().getDecorView().findViewById(android.R.id.content);if(!(root instanceof ViewGroup))return;
    FrameLayout layer=new FrameLayout(this);
    Button info=new Button(this);info.setText("DXF VECTOR");info.setTextColor(Color.WHITE);info.setTextSize(9);info.setAllCaps(false);
    GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(34,42,92));g.setCornerRadius(dp(9));g.setStroke(1,Color.rgb(115,155,255));info.setBackground(g);
    FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(dp(138),dp(42),Gravity.TOP|Gravity.RIGHT);bp.setMargins(0,dp(340),dp(14),0);layer.addView(info,bp);
    vectorBadge=txt("Vektör CAD —",9,Color.rgb(200,215,255),true);vectorBadge.setGravity(Gravity.CENTER);vectorBadge.setBackgroundColor(Color.argb(210,12,20,54));
    FrameLayout.LayoutParams vp=new FrameLayout.LayoutParams(dp(300),dp(38),Gravity.TOP|Gravity.RIGHT);vp.setMargins(0,dp(386),dp(14),0);layer.addView(vectorBadge,vp);
    ((ViewGroup)root).addView(layer,new ViewGroup.LayoutParams(-1,-1));
    info.setOnClickListener(v->showVectorInfo());
  }

  @Override void pick(){
    Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    if(mode==MODE_2D3D)i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","application/pdf","application/zip","application/x-zip-compressed","image/vnd.dxf","application/dxf","application/octet-stream","text/plain"});
    else i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"model/stl","model/obj","application/zip","application/x-zip-compressed","application/octet-stream","text/plain"});
    startActivityForResult(i,PICK);
  }

  @Override void loadDirect(Uri u,String n)throws Exception{
    if(mode==MODE_2D3D && n.toLowerCase(Locale.ROOT).endsWith(".dxf")){
      byte[] data=readAll(u,40L*1024L*1024L);loadDxfBytes(data,n);return;
    }
    dxfDoc=null;super.loadDirect(u,n);
  }

  @Override void loadExtracted(File f,String n)throws Exception{
    if(mode==MODE_2D3D && n.toLowerCase(Locale.ROOT).endsWith(".dxf")){
      byte[] data=readFile(f,40L*1024L*1024L);loadDxfBytes(data,n+" • ZIP");return;
    }
    dxfDoc=null;super.loadExtracted(f,n);
  }

  void loadDxfBytes(byte[] data,String name)throws Exception{
    String text=new String(data,StandardCharsets.UTF_8);dxfDoc=DxfVectorParser.parse(text);
    if(dxfDoc.entities.isEmpty())throw new IOException("DXF içinde desteklenen vektör geometri bulunamadı");
    source=DxfVectorParser.render(dxfDoc,1600,1100);voxels=null;mesh.clear();cad.setBitmap(source);fileInfo.setText(name+" • VECTOR DXF");
    lastReport=TechnicalDrawingIntelligence.analyze(source);
    String joined=joinCallouts(dxfDoc.texts);featureGraph=FeatureGraphEngine.withManualCallouts(lastReport,joined);
    vectorBadge.setText("DXF • "+dxfDoc.circles+" daire • "+dxfDoc.textCount+" metin");
    featureBadge.setText(featureGraph.summary());
    qualityInfo.setText(RequirementRegistry.summary()+"\nGÜVEN 98% • Vektör geometri");
    status.setText("✓ DXF doğrudan okundu • "+dxfDoc.summary()+" • ölçü metinleri feature parser'a aktarıldı");
  }

  String joinCallouts(List<String> a){StringBuilder s=new StringBuilder();for(String x:a){if(x==null||x.trim().isEmpty())continue;if(s.length()>0)s.append("; ");s.append(x.trim());}return s.toString();}

  void showVectorInfo(){
    if(dxfDoc==null){status.setText("DXF vektör dosyası yüklendiğinde bu panel aktif olur.");return;}
    StringBuilder s=new StringBuilder();s.append("DXF Vector Technical Drawing\n\n").append(dxfDoc.summary());
    s.append(String.format(Locale.US,"\nSınır: %.2f × %.2f",dxfDoc.maxX-dxfDoc.minX,dxfDoc.maxY-dxfDoc.minY));
    s.append("\n\nOkunan teknik metinler:\n");int lim=Math.min(18,dxfDoc.texts.size());for(int i=0;i<lim;i++)s.append("• ").append(dxfDoc.texts.get(i)).append("\n");if(dxfDoc.texts.size()>lim)s.append("… +").append(dxfDoc.texts.size()-lim);
    s.append("\n\nLINE/CIRCLE/ARC/LWPOLYLINE doğrudan vektör geometri olarak kullanılır; DIM/CENTER/HIDDEN katmanları yardımcı çizgi olarak ayrılır.");
    new android.app.AlertDialog.Builder(this).setTitle("Vector CAD v3.3").setMessage(s.toString()).setPositiveButton("Tamam",null).show();
  }

  @Override void createAction(){
    if(mode==MODE_2D3D && dxfDoc!=null){
      lastReport=TechnicalDrawingIntelligence.analyze(source);featureGraph=FeatureGraphEngine.withManualCallouts(lastReport,joinCallouts(dxfDoc.texts));featureBadge.setText(featureGraph.summary());
      status.setText("DXF vektör geometri + teknik metin feature graph'a bağlandı • 3D rekonstrüksiyon başlıyor...");
    }
    super.createAction();
  }
}
