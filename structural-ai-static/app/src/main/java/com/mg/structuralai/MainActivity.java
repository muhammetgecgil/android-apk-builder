package com.mg.structuralai;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.widget.*;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * APK entry point.
 * Locked requirements trace: SR-UX, SR-QA, SR-VV and solver/provenance separation.
 * Screening results and solver-grade FEM results are intentionally displayed as different modes.
 */
public class MainActivity extends Activity {
    private static final int PICK_MODEL=1001;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private TextView status, report, modeBadge;

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
        sub.setText("v0.4 • Otonom yapısal analiz çekirdeği");
        sub.setTextColor(Color.LTGRAY); sub.setTextSize(14); sub.setGravity(Gravity.CENTER_HORIZONTAL);
        sub.setPadding(0,10,0,16);
        root.addView(sub);

        modeBadge=new TextView(this);
        modeBadge.setText("MOD: HAZIR");
        modeBadge.setTextColor(Color.WHITE);
        modeBadge.setBackgroundColor(Color.rgb(70,70,78));
        modeBadge.setPadding(18,10,18,10);
        modeBadge.setGravity(Gravity.CENTER);
        root.addView(modeBadge,new LinearLayout.LayoutParams(-1,-2));

        Button pick=new Button(this);
        pick.setText("MODEL YÜKLE • AI ÖN TARAMA");
        pick.setOnClickListener(v->pickModel());
        root.addView(pick,new LinearLayout.LayoutParams(-1,-2));

        Button verify=new Button(this);
        verify.setText("GERÇEK FEM SOLVER DOĞRULAMA");
        verify.setOnClickListener(v->runFemVerification());
        root.addView(verify,new LinearLayout.LayoutParams(-1,-2));

        status=new TextView(this);
        status.setText("Hazır — OBJ / ASCII STL ön tarama veya FEM doğrulama seçin");
        status.setTextColor(Color.rgb(120,220,160)); status.setPadding(0,18,0,12);
        root.addView(status);

        ScrollView scroll=new ScrollView(this);
        report=new TextView(this);
        report.setTextColor(Color.WHITE); report.setTextSize(15); report.setTextIsSelectable(true);
        report.setPadding(0,8,0,64);
        report.setText(
            "İKİ AYRI SONUÇ SINIFI\n\n"+
            "1) AI ÖN TARAMA\nGeometri okunur ve yaklaşık/heuristic mühendislik taraması yapılır. Bu sonuç solver-grade FEA değildir.\n\n"+
            "2) GERÇEK FEM\nTET4 eleman rijitliği + global assembly + sparse PCG + sınır şartları + reaksiyon + Von Mises + denge kontrolü çalışır.\n\n"+
            "Sonraki adım: yüzey modelinden otomatik hacimsel tetra mesh üretip gerçek FEM hattına doğrudan bağlamak."
        );
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

    private void runFemVerification(){
        modeBadge.setText("MOD: GERÇEK FEM / VERIFICATION");
        modeBadge.setBackgroundColor(Color.rgb(25,100,65));
        status.setText("TET4 FEM benchmark çözülüyor…");
        report.setText("Global rijitlik matrisi kuruluyor → Dirichlet BC → nodal load → sparse PCG → stress recovery → equilibrium…");
        executor.submit(()->{
            try{
                FemBenchmarks.BenchmarkResult b=FemBenchmarks.unitTetSanity();
                runOnUiThread(()->{
                    if(b.pass){
                        status.setText("FEM solver doğrulaması GEÇTİ");
                        modeBadge.setText("MOD: GERÇEK FEM • VERIFIED");
                        modeBadge.setBackgroundColor(Color.rgb(20,125,70));
                        report.setText("SOLVER-GRADE FEM DOĞRULAMA\n\n"+b.message+
                            "\n\nKalite kapıları:\n• Sonlu sonuç: PASS\n• Pozitif deplasman/gerilme: PASS\n• Linear residual ≤ 1e-8: PASS\n• Global kuvvet dengesi: PASS\n\nBu test AI tahmini değildir; gerçek FEM çözüm hattını çalıştırır.");
                    } else {
                        status.setText("FEM solver doğrulaması BAŞARISIZ");
                        modeBadge.setText("MOD: FEM • QA BLOCKED");
                        modeBadge.setBackgroundColor(Color.rgb(150,45,45));
                        report.setText("SOLVER QA GATE BLOCKED\n\n"+b.message+"\n\nBu durumda uygulama mühendislik PASS sonucu üretemez.");
                    }
                });
            } catch(Exception e){
                runOnUiThread(()->{
                    status.setText("FEM solver doğrulaması durdu");
                    modeBadge.setText("MOD: FEM • ERROR");
                    modeBadge.setBackgroundColor(Color.rgb(150,45,45));
                    report.setText("Solver hatası: "+e.getMessage()+"\n\nQA kuralı: doğrulanamayan çözüm güvenli/PASS olarak gösterilemez.");
                });
            }
        });
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=PICK_MODEL || resultCode!=RESULT_OK || data==null) return;
        Uri uri=data.getData(); if(uri==null) return;
        final String name=getName(uri);
        modeBadge.setText("MOD: AI ÖN TARAMA • SOLVER-GRADE DEĞİL");
        modeBadge.setBackgroundColor(Color.rgb(130,90,20));
        status.setText("AI modeli inceliyor: "+name);
        report.setText("Geometri okunuyor, parça sınıflandırılıyor, mesnet/yük/malzeme adayları oluşturuluyor…");
        executor.submit(()->{
            try(InputStream in=getContentResolver().openInputStream(uri)){
                if(in==null) throw new Exception("Dosya açılamadı");
                MeshModel model=MeshParser.parse(name,in);
                AutoStaticEngine.Result r=new AutoStaticEngine().analyze(model);
                runOnUiThread(()->{
                    status.setText("AI ön tarama tamamlandı");
                    report.setText("UYARI: AŞAĞIDAKİ SONUÇ ÖN TARAMADIR, GERÇEK VOLUMETRİK FEM DEĞİLDİR.\n\n"+r.report);
                });
            }catch(Exception e){
                runOnUiThread(()->{
                    status.setText("Analiz durdu");
                    modeBadge.setText("MOD: INPUT ERROR");
                    modeBadge.setBackgroundColor(Color.rgb(150,45,45));
                    report.setText("Hata: "+e.getMessage()+"\n\nBinary STL ve STEP/IGES desteği CAD/mesher katmanında eklenecek.");
                });
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
