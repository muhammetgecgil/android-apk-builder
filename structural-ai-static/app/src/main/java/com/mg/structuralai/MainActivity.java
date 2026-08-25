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
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Structural AI Static mobile entry point. Screening, mesh QA and solver verification remain provenance-separated. */
public class MainActivity extends Activity {
    private static final int PICK_MODEL=1001;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private TextView status,report,modeBadge;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32,32,32,32); root.setBackgroundColor(Color.rgb(10,22,38));

        TextView title=new TextView(this);
        title.setText("STRUCTURAL AI STATIC"); title.setTextColor(Color.WHITE); title.setTextSize(26); title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title,new LinearLayout.LayoutParams(-1,-2));

        TextView sub=new TextView(this);
        sub.setText("v0.6 • AUTO MODEL → VOLUME MESH → FEM QA");
        sub.setTextColor(Color.LTGRAY); sub.setTextSize(14); sub.setGravity(Gravity.CENTER_HORIZONTAL); sub.setPadding(0,10,0,16);
        root.addView(sub);

        modeBadge=new TextView(this); modeBadge.setText("MOD: HAZIR"); modeBadge.setTextColor(Color.WHITE);
        modeBadge.setBackgroundColor(Color.rgb(70,70,78)); modeBadge.setPadding(18,10,18,10); modeBadge.setGravity(Gravity.CENTER);
        root.addView(modeBadge,new LinearLayout.LayoutParams(-1,-2));

        Button pick=new Button(this); pick.setText("MODEL YÜKLE • AUTO MODEL + TETRA MESH"); pick.setOnClickListener(v->pickModel());
        root.addView(pick,new LinearLayout.LayoutParams(-1,-2));

        Button verify=new Button(this); verify.setText("GERÇEK FEM SOLVER DOĞRULAMA"); verify.setOnClickListener(v->runFemVerification());
        root.addView(verify,new LinearLayout.LayoutParams(-1,-2));

        status=new TextView(this); status.setText("Hazır — OBJ / ASCII STL / Binary STL"); status.setTextColor(Color.rgb(120,220,190)); status.setPadding(0,18,0,12);
        root.addView(status);

        ScrollView scroll=new ScrollView(this); report=new TextView(this); report.setTextColor(Color.WHITE); report.setTextSize(15); report.setTextIsSelectable(true); report.setPadding(0,8,0,64);
        report.setText("v0.6 ANALİZ ZİNCİRİ\n\n1) Yüzey geometri import\n2) Closed/manifold topology gate\n3) Otomatik hacimsel TET4 mesh\n4) Ters/degenerate/shape-quality gate\n5) FEM solver benchmark\n\nKural: model birimi, fiziksel yük ve mesnet çözülmeden yüklenen parçaya mühendislik PASS verilmez.");
        scroll.addView(report); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
    }

    private void pickModel(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"model/obj","application/sla","application/octet-stream","text/plain"}); startActivityForResult(i,PICK_MODEL);
    }

    private void runFemVerification(){
        modeBadge.setText("MOD: GERÇEK FEM / VERIFICATION"); modeBadge.setBackgroundColor(Color.rgb(25,100,65));
        status.setText("TET4 FEM benchmark çözülüyor…"); report.setText("Global K → Dirichlet BC → nodal load → sparse PCG → stress recovery → equilibrium…");
        executor.submit(()->{
            try{
                FemBenchmarks.BenchmarkResult b=FemBenchmarks.unitTetSanity();
                runOnUiThread(()->{
                    if(b.pass){
                        status.setText("FEM solver doğrulaması GEÇTİ"); modeBadge.setText("MOD: GERÇEK FEM • VERIFIED"); modeBadge.setBackgroundColor(Color.rgb(20,125,70));
                        report.setText("SOLVER-GRADE FEM DOĞRULAMA\n\n"+b.message+"\n\n• TET4 stiffness: PASS\n• Global assembly: PASS\n• PCG residual: PASS\n• Reaction/load equilibrium: PASS\n\nBu test AI tahmini değildir; deterministik FEM çekirdeğini çalıştırır.");
                    }else{
                        status.setText("FEM solver doğrulaması BAŞARISIZ"); modeBadge.setText("MOD: FEM • QA BLOCKED"); modeBadge.setBackgroundColor(Color.rgb(150,45,45));
                        report.setText("SOLVER QA GATE BLOCKED\n\n"+b.message);
                    }
                });
            }catch(Exception e){ runOnUiThread(()->{ status.setText("FEM solver doğrulaması durdu"); modeBadge.setText("MOD: FEM • ERROR"); modeBadge.setBackgroundColor(Color.rgb(150,45,45)); report.setText("Solver hatası: "+e.getMessage()); }); }
        });
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=PICK_MODEL||resultCode!=RESULT_OK||data==null) return;
        Uri uri=data.getData(); if(uri==null) return; final String name=getName(uri);
        modeBadge.setText("MOD: AUTO MODEL + MESH QA"); modeBadge.setBackgroundColor(Color.rgb(15,95,115));
        status.setText("Model okunuyor ve hacim kontrol ediliyor: "+name); report.setText("Import → topology → volumetric tetra mesh → quality gate…");
        executor.submit(()->{
            try(InputStream in=getContentResolver().openInputStream(uri)){
                if(in==null) throw new Exception("Dosya açılamadı");
                MeshModel model=MeshParser.parse(name,in);
                AutoStaticEngine.Result screen=new AutoStaticEngine().analyze(model);
                SurfaceTopologyReport topo=SurfaceTopologyReport.evaluate(model);
                StringBuilder out=new StringBuilder();
                out.append("AUTO MODEL IMPORT\n\nDosya: ").append(name).append("\nVertex: ").append(model.vertices.size()).append("\nTriangles: ").append(model.triangles.size())
                    .append(String.format(Locale.US,"\nBounds: %.5g × %.5g × %.5g model-unit\n\n",model.dx(),model.dy(),model.dz()));
                out.append("SURFACE QA\n").append(topo.summary()).append("\n\n");
                if(topo.closedManifold){
                    // Scale=1 is intentionally topology-only here. Uniform scaling does not change shape quality.
                    VoxelTetMesher.Result mesh=VoxelTetMesher.generate(model,12,1.0);
                    out.append("AUTO VOLUMETRIC MESH\nGrid: ").append(mesh.nx).append("×").append(mesh.ny).append("×").append(mesh.nz)
                        .append("\nInside cells: ").append(mesh.insideCells).append("\nNodes: ").append(mesh.mesh.nodes.size()).append("\nTET4: ").append(mesh.mesh.tets.size())
                        .append("\n").append(mesh.quality.summary()).append("\n\n");
                    out.append(mesh.quality.pass?"MESH QA: PASS\n":"MESH QA: BLOCKED — FEM'e geçilemez\n");
                    out.append("\nÖNEMLİ: mesh geometrik/topolojik olarak üretildi; model birimi henüz fiziksel metre ölçeğine doğrulanmadı. Bu nedenle bu yüklenen model için stress/FOS PASS üretilmiyor.\n\n");
                }else{
                    out.append("VOLUME MESH: BLOCKED\nKapalı-manifold hacim doğrulanamadı. AI geometry-healing katmanı açığı kapatmadan solver'a geçmeyecek.\n\n");
                }
                out.append("AI ÖN TARAMA (solver sonucu değildir)\n\n").append(screen.report);
                final String text=out.toString();
                runOnUiThread(()->{ status.setText(topo.closedManifold?"Auto model + tetra mesh tamamlandı":"Yüzey QA modeli solver'dan bloke etti"); modeBadge.setText(topo.closedManifold?"MOD: MESH READY • PHYSICS PENDING":"MOD: GEOMETRY QA BLOCKED"); modeBadge.setBackgroundColor(topo.closedManifold?Color.rgb(20,115,105):Color.rgb(150,45,45)); report.setText(text); });
            }catch(Exception e){ runOnUiThread(()->{ status.setText("Analiz durdu"); modeBadge.setText("MOD: INPUT / MESH ERROR"); modeBadge.setBackgroundColor(Color.rgb(150,45,45)); report.setText("Hata: "+e.getMessage()+"\n\nQA kuralı: geçersiz geometri veya mesh solver'a aktarılmaz."); }); }
        });
    }

    private String getName(Uri uri){
        String name="model"; Cursor c=getContentResolver().query(uri,null,null,null,null);
        if(c!=null){ try{ int idx=c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if(c.moveToFirst()&&idx>=0) name=c.getString(idx); } finally{c.close();} } return name;
    }
    @Override protected void onDestroy(){ super.onDestroy(); executor.shutdownNow(); }
}
