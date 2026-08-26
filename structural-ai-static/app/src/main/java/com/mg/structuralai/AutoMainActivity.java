package com.mg.structuralai;

import android.app.Activity;
import android.content.Intent;
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
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** One-button autonomous flow: choose model, then the app plans and runs the analysis itself. */
public final class AutoMainActivity extends Activity {
    private static final int PICK_MODEL=2101;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private TextView status,report,badge;
    private InteractiveModelView viewport;
    private MeshModel model;
    private String modelName="model";

    @Override public void onCreate(Bundle b){super.onCreate(b);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(18,72,18,18);root.setBackgroundColor(Color.rgb(8,18,31));
        TextView title=new TextView(this);title.setText("STRUCTURAL AI • AUTONOMOUS");title.setTextColor(Color.WHITE);title.setTextSize(22);title.setGravity(Gravity.CENTER);root.addView(title);
        TextView sub=new TextView(this);sub.setText("v1.4.0 AUTO ALPHA • MODEL → AUTO ANALYZE");sub.setTextColor(Color.LTGRAY);sub.setGravity(Gravity.CENTER);root.addView(sub);
        badge=new TextView(this);badge.setText("MODE: WAITING FOR MODEL");badge.setTextColor(Color.WHITE);badge.setGravity(Gravity.CENTER);badge.setPadding(10,8,10,8);badge.setBackgroundColor(Color.rgb(70,70,78));root.addView(badge);
        viewport=new InteractiveModelView(this);root.addView(viewport,new LinearLayout.LayoutParams(-1,430));
        Button choose=new Button(this);choose.setText("MODEL SEÇ");choose.setOnClickListener(v->pickModel());root.addView(choose);
        Button auto=new Button(this);auto.setText("AUTO ANALYZE");auto.setOnClickListener(v->runAuto());root.addView(auto);
        Button manual=new Button(this);manual.setText("GELİŞMİŞ / MANUEL MOD");manual.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));root.addView(manual);
        status=new TextView(this);status.setText("OBJ / ASCII STL / Binary STL");status.setTextColor(Color.rgb(120,220,190));status.setPadding(0,8,0,5);root.addView(status);
        ScrollView sc=new ScrollView(this);report=new TextView(this);report.setTextColor(Color.WHITE);report.setTextSize(12);report.setText("Otonom akış:\n1) Yalnız modeli seç\n2) AUTO ANALYZE\n3) Dahili cantilever regression\n4) Geometri QA\n5) Birim/malzeme belirsizliği yönetimi\n6) Mesnet/yük adayları\n7) Load-to-volume mapping QA\n8) 8/12/16 FEM convergence\n9) Gerekirse 20/24/28/32 adaptive escalation\n10) Sıfır-cevap watchdog\n11) Hotspot konum stabilitesi\n12) En güvenilir senaryo + confidence raporu\n\nRegression testi başarısız olursa kullanıcı modeli çözülmez. Adaptive katman mevcut conforming voxel-TET4 ağı güvenli şekilde inceltir; gerçek lokal octree refinement henüz ayrı geliştirme katmanıdır.");sc.addView(report);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }

    private void pickModel(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,PICK_MODEL);}
    @Override protected void onActivityResult(int rq,int rc,Intent data){super.onActivityResult(rq,rc,data);if(rq!=PICK_MODEL||rc!=RESULT_OK||data==null)return;Uri uri=data.getData();if(uri==null)return;modelName=getName(uri);status.setText("Model okunuyor: "+modelName);badge.setText("MODE: GEOMETRY QA");executor.submit(()->{try(InputStream in=getContentResolver().openInputStream(uri)){MeshModel m=MeshParser.parse(modelName,in);SurfaceTopologyReport t=SurfaceTopologyReport.evaluate(m);if(!t.closedManifold)throw new IllegalStateException("Kapalı/manifold değil: "+t.summary());model=m;runOnUiThread(()->{viewport.setModel(m);status.setText("Model hazır — AUTO ANALYZE'a bas");badge.setText("MODE: MODEL READY");badge.setBackgroundColor(Color.rgb(20,105,105));report.setText("MODEL READY\n\n"+modelName+"\n"+t.summary()+"\n\nBu noktadan sonra mesnet, yük, yön veya malzeme seçmen gerekmez.");});}catch(Exception e){runOnUiThread(()->{badge.setText("MODE: GEOMETRY BLOCKED");badge.setBackgroundColor(Color.rgb(145,45,45));status.setText("Model bloke edildi");report.setText("Hata: "+e.getMessage());});}});}

    private void runAuto(){if(model==null){status.setText("Önce model seç");return;}badge.setText("MODE: SELF TEST");badge.setBackgroundColor(Color.rgb(80,70,135));status.setText("Önce dahili FEM/load-mapping regression testi çalışıyor…");executor.submit(()->{try{
        AutonomousRegressionGate.Result rg=AutonomousRegressionGate.run();
        if(!rg.pass){runOnUiThread(()->{badge.setText("MODE: SELF TEST BLOCKED");badge.setBackgroundColor(Color.rgb(145,45,45));status.setText("Dahili regression başarısız — kullanıcı analizi başlatılmadı");report.setText(rg.summary);});return;}
        runOnUiThread(()->{badge.setText("MODE: AUTONOMOUS ANALYSIS");badge.setBackgroundColor(Color.rgb(25,80,130));status.setText("Regression PASS → AI planlıyor → convergence/adaptive çözüm çalışıyor…");});
        AutonomousAnalysisRunner.Result r=AutonomousAnalysisRunner.run(model);runOnUiThread(()->{viewport.setResult(r.displayMesh,r.displayFem);report.setText("IN-APP REGRESSION\n"+rg.summary+"\n\n"+r.report);status.setText(r.numericallyReady?"Otonom numerik analiz tamamlandı":"Otonom analiz QA tarafından bloke edildi");badge.setText(r.numericallyReady?"MODE: AUTO ANALYSIS COMPLETE":"MODE: AUTO QA BLOCKED");badge.setBackgroundColor(r.numericallyReady?Color.rgb(20,125,70):Color.rgb(145,45,45));});
    }catch(Exception e){runOnUiThread(()->{badge.setText("MODE: AUTO ERROR");badge.setBackgroundColor(Color.rgb(145,45,45));status.setText("Otonom analiz durdu");report.setText("Hata: "+e.getMessage());});}});}
    private String getName(Uri uri){String n="model";Cursor c=getContentResolver().query(uri,null,null,null,null);if(c!=null){try{int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(c.moveToFirst()&&i>=0)n=c.getString(i);}finally{c.close();}}return n;}
    @Override protected void onDestroy(){super.onDestroy();executor.shutdownNow();}
}
