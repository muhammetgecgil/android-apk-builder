package com.mg.structuralai;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Structural AI Static mobile entry point. Geometry, mesh and physics gates remain provenance-separated. */
public class MainActivity extends Activity {
    private static final int PICK_MODEL=1001;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private TextView status,report,modeBadge;
    private Spinner unitSpinner;
    private EditText loadInput,eInput,nuInput,yieldInput;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(28,28,28,28); root.setBackgroundColor(Color.rgb(10,22,38));

        TextView title=new TextView(this);
        title.setText("STRUCTURAL AI STATIC"); title.setTextColor(Color.WHITE); title.setTextSize(25); title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title,new LinearLayout.LayoutParams(-1,-2));

        TextView sub=new TextView(this);
        sub.setText("v0.7 • MODEL → TET4 → REAL STATIC FEM");
        sub.setTextColor(Color.LTGRAY); sub.setTextSize(14); sub.setGravity(Gravity.CENTER_HORIZONTAL); sub.setPadding(0,8,0,14);
        root.addView(sub);

        modeBadge=new TextView(this); modeBadge.setText("MOD: HAZIR"); modeBadge.setTextColor(Color.WHITE);
        modeBadge.setBackgroundColor(Color.rgb(70,70,78)); modeBadge.setPadding(16,8,16,8); modeBadge.setGravity(Gravity.CENTER);
        root.addView(modeBadge,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout settings=new LinearLayout(this); settings.setOrientation(LinearLayout.VERTICAL); settings.setPadding(0,10,0,6);
        unitSpinner=new Spinner(this);
        unitSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"mm","m","cm","in"}));
        settings.addView(label("MODEL BİRİMİ")); settings.addView(unitSpinner);

        LinearLayout row1=new LinearLayout(this); row1.setOrientation(LinearLayout.HORIZONTAL);
        loadInput=field("-1000"); eInput=field("210");
        row1.addView(box("Toplam Fz [N]",loadInput),new LinearLayout.LayoutParams(0,-2,1));
        row1.addView(box("E [GPa]",eInput),new LinearLayout.LayoutParams(0,-2,1)); settings.addView(row1);
        LinearLayout row2=new LinearLayout(this); row2.setOrientation(LinearLayout.HORIZONTAL);
        nuInput=field("0.30"); yieldInput=field("355");
        row2.addView(box("Poisson ν",nuInput),new LinearLayout.LayoutParams(0,-2,1));
        row2.addView(box("Akma [MPa]",yieldInput),new LinearLayout.LayoutParams(0,-2,1)); settings.addView(row2);
        root.addView(settings);

        Button pick=new Button(this); pick.setText("MODEL YÜKLE + GERÇEK STATİK ANALİZ"); pick.setOnClickListener(v->pickModel());
        root.addView(pick,new LinearLayout.LayoutParams(-1,-2));
        Button verify=new Button(this); verify.setText("FEM ÇEKİRDEĞİ DOĞRULA"); verify.setOnClickListener(v->runFemVerification());
        root.addView(verify,new LinearLayout.LayoutParams(-1,-2));

        status=new TextView(this); status.setText("Hazır — OBJ / ASCII STL / Binary STL"); status.setTextColor(Color.rgb(120,220,190)); status.setPadding(0,12,0,8);
        root.addView(status);

        ScrollView scroll=new ScrollView(this); report=new TextView(this); report.setTextColor(Color.WHITE); report.setTextSize(14); report.setTextIsSelectable(true); report.setPadding(0,8,0,64);
        report.setText("v0.7 ANALİZ ZİNCİRİ\n\n1) Yüzey import + topology gate\n2) SI birim dönüşümü\n3) Hacimsel TET4 mesh + kalite gate\n4) Açık varsayılan BC: min-X tam ankastre\n5) max-X düzlemine dağıtılmış global Fz\n6) Sparse PCG lineer statik FEM\n7) Von Mises, deplasman, denge ve FoS\n\nUYARI: otomatik mesnet/yük fiziksel fikstürü tahmin etmez; açık bir başlangıç senaryosudur. Nihai mühendislik kararı için gerçek sınır şartları kullanıcı tarafından doğrulanmalıdır.");
        scroll.addView(report); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
    }

    private TextView label(String s){ TextView v=new TextView(this); v.setText(s); v.setTextColor(Color.LTGRAY); v.setTextSize(12); return v; }
    private EditText field(String s){ EditText e=new EditText(this); e.setText(s); e.setTextColor(Color.WHITE); e.setHintTextColor(Color.GRAY); e.setSingleLine(true); e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED); return e; }
    private LinearLayout box(String title,EditText e){ LinearLayout b=new LinearLayout(this); b.setOrientation(LinearLayout.VERTICAL); b.setPadding(4,0,4,0); b.addView(label(title)); b.addView(e); return b; }

    private void pickModel(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"model/obj","application/sla","application/octet-stream","text/plain"}); startActivityForResult(i,PICK_MODEL);
    }

    private double parse(EditText e,String name){ try{return Double.parseDouble(e.getText().toString().trim());}catch(Exception ex){throw new IllegalArgumentException(name+" sayısal olmalı");} }

    private void runFemVerification(){
        modeBadge.setText("MOD: GERÇEK FEM / VERIFICATION"); modeBadge.setBackgroundColor(Color.rgb(25,100,65));
        status.setText("TET4 FEM benchmark çözülüyor…"); report.setText("Global K → Dirichlet BC → nodal load → sparse PCG → stress recovery → equilibrium…");
        executor.submit(()->{
            try{
                FemBenchmarks.BenchmarkResult b=FemBenchmarks.unitTetSanity();
                runOnUiThread(()->{
                    if(b.pass){ status.setText("FEM solver doğrulaması GEÇTİ"); modeBadge.setText("MOD: GERÇEK FEM • VERIFIED"); modeBadge.setBackgroundColor(Color.rgb(20,125,70));
                        report.setText("SOLVER-GRADE FEM DOĞRULAMA\n\n"+b.message+"\n\n• TET4 stiffness: PASS\n• Global assembly: PASS\n• PCG residual: PASS\n• Reaction/load equilibrium: PASS\n\nBu test AI tahmini değildir; deterministik FEM çekirdeğini çalıştırır.");
                    }else{ status.setText("FEM solver doğrulaması BAŞARISIZ"); modeBadge.setText("MOD: FEM • QA BLOCKED"); modeBadge.setBackgroundColor(Color.rgb(150,45,45)); report.setText("SOLVER QA GATE BLOCKED\n\n"+b.message); }
                });
            }catch(Exception e){ runOnUiThread(()->{ status.setText("FEM solver doğrulaması durdu"); modeBadge.setText("MOD: FEM • ERROR"); modeBadge.setBackgroundColor(Color.rgb(150,45,45)); report.setText("Solver hatası: "+e.getMessage()); }); }
        });
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=PICK_MODEL||resultCode!=RESULT_OK||data==null) return;
        Uri uri=data.getData(); if(uri==null) return; final String name=getName(uri);
        final String unit=(String)unitSpinner.getSelectedItem();
        final double totalLoad,eGPa,nu,yieldMPa;
        try{ totalLoad=parse(loadInput,"Yük"); eGPa=parse(eInput,"E"); nu=parse(nuInput,"Poisson"); yieldMPa=parse(yieldInput,"Akma"); }
        catch(Exception ex){ report.setText("Girdi hatası: "+ex.getMessage()); return; }

        modeBadge.setText("MOD: AUTO MODEL + REAL FEM"); modeBadge.setBackgroundColor(Color.rgb(15,95,115));
        status.setText("Model → mesh → gerçek statik çözüm: "+name); report.setText("Import → topology → SI scale → tetra mesh → BC/load → PCG → stress…");
        executor.submit(()->{
            try(InputStream in=getContentResolver().openInputStream(uri)){
                if(in==null) throw new Exception("Dosya açılamadı");
                MeshModel model=MeshParser.parse(name,in);
                SurfaceTopologyReport topo=SurfaceTopologyReport.evaluate(model);
                if(!topo.closedManifold) throw new IllegalStateException("Kapalı/manifold yüzey değil; hacim mesh ve FEM bloke edildi. "+topo.summary());
                double scale=AutoFemSetup.unitScaleToMetres(unit);
                VoxelTetMesher.Result mr=VoxelTetMesher.generate(model,12,scale);
                if(!mr.quality.pass) throw new IllegalStateException("Mesh kalite kapısı geçilemedi: "+mr.quality.summary());

                LinearElasticMaterial mat=new LinearElasticMaterial("User isotropic",eGPa*1e9,nu,7850.0,yieldMPa*1e6);
                StaticFemSolver solver=new StaticFemSolver(mr.mesh,mat);
                AutoFemSetup.SetupResult setup=AutoFemSetup.applyCantileverZ(solver,mr.mesh,totalLoad);
                StaticFemSolver.Result fem=solver.solve();
                double fos=fem.maxVonMisesPa>0?mat.yieldPa/fem.maxVonMisesPa:Double.POSITIVE_INFINITY;
                boolean eqPass=fem.forceEquilibriumRelativeError<1e-5;
                boolean solverPass=fem.linearSolve.converged && eqPass && Double.isFinite(fem.maxVonMisesPa) && Double.isFinite(fem.maxDisplacementM);

                StringBuilder out=new StringBuilder();
                out.append("REAL STATIC FEM RESULT\n\nDosya: ").append(name)
                   .append("\nModel unit: ").append(unit).append(" → SI metre")
                   .append(String.format(Locale.US,"\nBounds: %.5g × %.5g × %.5g %s",model.dx(),model.dy(),model.dz(),unit))
                   .append("\nSurface: ").append(topo.summary())
                   .append("\n\nVOLUME MESH\nGrid: ").append(mr.nx).append("×").append(mr.ny).append("×").append(mr.nz)
                   .append("\nNodes: ").append(mr.mesh.nodes.size()).append("\nTET4: ").append(mr.mesh.tets.size()).append("\n").append(mr.quality.summary())
                   .append("\n\nMATERIAL\nE: ").append(String.format(Locale.US,"%.3f GPa",eGPa)).append("\nν: ").append(String.format(Locale.US,"%.4f",nu)).append("\nYield: ").append(String.format(Locale.US,"%.3f MPa",yieldMPa))
                   .append("\n\nBOUNDARY / LOAD (EXPLICIT AUTO DEFAULT)\nSupport: ").append(setup.supportDescription).append("\nFixed nodes: ").append(setup.fixedNodes)
                   .append("\nLoad: ").append(setup.loadDescription).append("\nLoaded nodes: ").append(setup.loadedNodes).append(String.format(Locale.US,"\nTotal Fz: %.6g N",setup.totalLoadN))
                   .append("\n\nSOLVER\nPCG converged: ").append(fem.linearSolve.converged).append("\nIterations: ").append(fem.linearSolve.iterations)
                   .append(String.format(Locale.US,"\nRelative residual: %.3e\nForce equilibrium error: %.3e",fem.linearSolve.relativeResidual,fem.forceEquilibriumRelativeError))
                   .append(String.format(Locale.US,"\n\nRESULTS\nMax displacement: %.6g mm\nMax Von Mises: %.6g MPa\nYield FoS: %.4f",fem.maxDisplacementM*1000.0,fem.maxVonMisesPa/1e6,fos))
                   .append("\n\nQA: ").append(solverPass?"NUMERICAL PASS":"BLOCKED")
                   .append("\n\nMühendislik notu: Bu sonuç gerçek FEM çekirdeğinden gelir; ancak min-X ankastre / max-X Fz otomatik başlangıç sınır şartıdır. Gerçek test fikstürü ve yük uygulama yüzeyi doğrulanmadan tasarım onayı olarak kullanılmamalıdır.");
                final String text=out.toString();
                runOnUiThread(()->{ status.setText(solverPass?"Gerçek statik FEM tamamlandı":"FEM QA bloke edildi"); modeBadge.setText(solverPass?"MOD: REAL FEM • NUMERICAL PASS":"MOD: FEM • QA BLOCKED"); modeBadge.setBackgroundColor(solverPass?Color.rgb(20,125,70):Color.rgb(150,45,45)); report.setText(text); });
            }catch(Exception e){ runOnUiThread(()->{ status.setText("Analiz durdu"); modeBadge.setText("MOD: INPUT / MESH / FEM ERROR"); modeBadge.setBackgroundColor(Color.rgb(150,45,45)); report.setText("Hata: "+e.getMessage()+"\n\nQA kuralı: geçersiz geometri, mesh, fizik girdisi veya yakınsamayan çözüm sonuç üretemez."); }); }
        });
    }

    private String getName(Uri uri){
        String name="model"; Cursor c=getContentResolver().query(uri,null,null,null,null);
        if(c!=null){ try{ int idx=c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if(c.moveToFirst()&&idx>=0) name=c.getString(idx); } finally{c.close();} } return name;
    }
    @Override protected void onDestroy(){ super.onDestroy(); executor.shutdownNow(); }
}
