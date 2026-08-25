package com.mg.structuralai;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_MODEL=1001;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private TextView status, report;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32,32,32,32);
        root.setBackgroundColor(Color.rgb(18,18,20));

        TextView title=new TextView(this);
        title.setText("STRUCTURAL AI STATIC");
        title.setTextColor(Color.WHITE); title.setTextSize(26); title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title,new LinearLayout.LayoutParams(-1,-2));

        TextView sub=new TextView(this);
        sub.setText("Modeli yükle → AI analiz senaryosunu kursun → otomatik statik tarama");
        sub.setTextColor(Color.LTGRAY); sub.setTextSize(14); sub.setPadding(0,12,0,24);
        root.addView(sub);

        Button pick=new Button(this);
        pick.setText("MODEL YÜKLE VE OTOMATİK ANALİZ ET");
        pick.setOnClickListener(v->pickModel());
        root.addView(pick,new LinearLayout.LayoutParams(-1,-2));

        status=new TextView(this);
        status.setText("Hazır — OBJ veya ASCII STL seçin");
        status.setTextColor(Color.rgb(120,220,160)); status.setPadding(0,18,0,12);
        root.addView(status);

        ScrollView scroll=new ScrollView(this);
        report=new TextView(this); report.setTextColor(Color.WHITE); report.setTextSize(15); report.setTextIsSelectable(true);
        report.setPadding(0,8,0,64);
        report.setText("Amaç: analistin rutin pre-processing kararlarını otomatikleştirmek.\n\nV0.1: geometri okuma + otonom varsayım motoru + hızlı fizik taraması.\nV0.2+: tetra mesh, sparse FEM, Von Mises, deplasman, reaksiyon, temas, mesh yakınsama ve otomatik rapor.");
        scroll.addView(report);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }

    private void pickModel(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"model/obj","application/sla","application/octet-stream","text/plain"});
        startActivityForResult(i,PICK_MODEL);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=PICK_MODEL || resultCode!=RESULT_OK || data==null) return;
        Uri uri=data.getData(); if(uri==null) return;
        final String name=getName(uri);
        status.setText("AI modeli inceliyor: "+name);
        report.setText("Geometri okunuyor, parça sınıflandırılıyor, mesnet/yük/malzeme adayları oluşturuluyor…");
        executor.submit(()->{
            try(InputStream in=getContentResolver().openInputStream(uri)){
                if(in==null) throw new Exception("Dosya açılamadı");
                MeshModel model=MeshParser.parse(name,in);
                AutoStaticEngine.Result r=new AutoStaticEngine().analyze(model);
                runOnUiThread(()->{ status.setText("Otomatik ön analiz tamamlandı"); report.setText(r.report); });
            }catch(Exception e){
                runOnUiThread(()->{ status.setText("Analiz durdu"); report.setText("Hata: "+e.getMessage()+"\n\nBinary STL ve STEP/IGES desteği sonraki parser/mesher katmanında eklenecek."); });
            }
        });
    }

    private String getName(Uri uri){
        String name="model";
        Cursor c=getContentResolver().query(uri,null,null,null,null);
        if(c!=null){ try{ int idx=c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if(c.moveToFirst()&&idx>=0) name=c.getString(idx); } finally{c.close();} }
        return name;
    }

    @Override protected void onDestroy(){ super.onDestroy(); executor.shutdownNow(); }
}
