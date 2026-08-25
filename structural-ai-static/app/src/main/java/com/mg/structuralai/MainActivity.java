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

/** v0.8: interactive model picking + real static FEM + contour viewport. */
public class MainActivity extends Activity {
    private static final int PICK_MODEL=1001;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private TextView status,report,modeBadge;
    private Spinner unitSpinner,axisSpinner;
    private EditText loadInput,eInput,nuInput,yieldInput;
    private InteractiveModelView viewport;
    private MeshModel currentModel;
    private VoxelTetMesher.Result currentMesh;
    private MeshModel.V3 supportPoint,loadPoint;
    private String currentName="model";

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(22,22,22,22); root.setBackgroundColor(Color.rgb(10,22,38));
        TextView title=new TextView(this); title.setText("STRUCTURAL AI STATIC"); title.setTextColor(Color.WHITE); title.setTextSize(24); title.setGravity(Gravity.CENTER_HORIZONTAL); root.addView(title);
        TextView sub=new TextView(this); sub.setText("v0.8 • 3D PICK → BC/LOAD → REAL FEM → CONTOUR"); sub.setTextColor(Color.LTGRAY); sub.setGravity(Gravity.CENTER_HORIZONTAL); root.addView(sub);
        modeBadge=new TextView(this); modeBadge.setText("MOD: HAZIR"); modeBadge.setTextColor(Color.WHITE); modeBadge.setGravity(Gravity.CENTER); modeBadge.setBackgroundColor(Color.rgb(70,70,78)); modeBadge.setPadding(12,8,12,8); root.addView(modeBadge,new LinearLayout.LayoutParams(-1,-2));

        viewport=new InteractiveModelView(this); viewport.setPickListener((p,idx)->{
            if(viewport.getPickMode()==InteractiveModelView.PickMode.SUPPORT){supportPoint=p;viewport.setSupportPoint(p);status.setText("Mesnet bölgesi seçildi • vertex "+idx);}
            else if(viewport.getPickMode()==InteractiveModelView.PickMode.LOAD){loadPoint=p;viewport.setLoadPoint(p);status.setText("Yük bölgesi seçildi • vertex "+idx);}
            viewport.setPickMode(InteractiveModelView.PickMode.NONE);
        });
        root.addView(viewport,new LinearLayout.LayoutParams(-1,420));

        LinearLayout actions=new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        Button pickSupport=new Button(this); pickSupport.setText("MESNET SEÇ"); pickSupport.setOnClickListener(v->{viewport.setPickMode(InteractiveModelView.PickMode.SUPPORT);status.setText("Model üzerinde mesnet noktasına dokun");});
        Button pickLoad=new Button(this); pickLoad.setText("YÜK YÜZEYİ SEÇ"); pickLoad.setOnClickListener(v->{viewport.setPickMode(InteractiveModelView.PickMode.LOAD);status.setText("Model üzerinde yük noktasına dokun");});
        actions.addView(pickSupport,new LinearLayout.LayoutParams(0,-2,1)); actions.addView(pickLoad,new LinearLayout.LayoutParams(0,-2,1)); root.addView(actions);

        LinearLayout cfg=new LinearLayout(this); cfg.setOrientation(LinearLayout.HORIZONTAL);
        unitSpinner=new Spinner(this); unitSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"mm","m","cm","in"}));
        axisSpinner=new Spinner(this); axisSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"-Z","+Z","-Y","+Y","-X","+X"}));
        cfg.addView(box("Birim",unitSpinner),new LinearLayout.LayoutParams(0,-2,1)); cfg.addView(box("Yük yönü",axisSpinner),new LinearLayout.LayoutParams(0,-2,1)); root.addView(cfg);

        LinearLayout r1=new LinearLayout(this); r1.setOrientation(LinearLayout.HORIZONTAL); loadInput=field("1000");eInput=field("210");r1.addView(box("Yük [N]",loadInput),new LinearLayout.LayoutParams(0,-2,1));r1.addView(box("E [GPa]",eInput),new LinearLayout.LayoutParams(0,-2,1));root.addView(r1);
        LinearLayout r2=new LinearLayout(this); r2.setOrientation(LinearLayout.HORIZONTAL);nuInput=field("0.30");yieldInput=field("355");r2.addView(box("Poisson",nuInput),new LinearLayout.LayoutParams(0,-2,1));r2.addView(box("Akma [MPa]",yieldInput),new LinearLayout.LayoutParams(0,-2,1));root.addView(r2);

        Button loadModel=new Button(this);loadModel.setText("MODEL YÜKLE + MESH HAZIRLA");loadModel.setOnClickListener(v->pickModel());root.addView(loadModel,new LinearLayout.LayoutParams(-1,-2));
        Button solve=new Button(this);solve.setText("SEÇİMLERLE GERÇEK FEM ÇÖZ");solve.setOnClickListener(v->solvePicked());root.addView(solve,new LinearLayout.LayoutParams(-1,-2));
        Button verify=new Button(this);verify.setText("FEM ÇEKİRDEĞİ DOĞRULA");verify.setOnClickListener(v->runFemVerification());root.addView(verify,new LinearLayout.LayoutParams(-1,-2));

        status=new TextView(this);status.setText("Hazır — OBJ / ASCII STL / Binary STL");status.setTextColor(Color.rgb(120,220,190));status.setPadding(0,8,0,6);root.addView(status);
        ScrollView scroll=new ScrollView(this);report=new TextView(this);report.setTextColor(Color.WHITE);report.setTextSize(13);report.setTextIsSelectable(true);report.setText("v0.8 AKIŞ\n\n1) Model yükle + topology/mesh QA\n2) 3B görünüşü parmakla döndür\n3) MESNET SEÇ → modele dokun\n4) YÜK YÜZEYİ SEÇ → modele dokun\n5) Yük yönü/büyüklüğü seç\n6) Gerçek TET4 + sparse PCG FEM\n7) Von Mises renk görünümü + deplasman + FoS + denge\n\nSeçilen noktalar çevresindeki lokal volumetrik düğüm yamalarına uygulanır; varsayılan min-X/max-X otomasyonu kaldırılmıştır.");scroll.addView(report);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }

    private TextView label(String s){TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.LTGRAY);v.setTextSize(11);return v;}
    private EditText field(String s){EditText e=new EditText(this);e.setText(s);e.setTextColor(Color.WHITE);e.setSingleLine(true);e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);return e;}
    private LinearLayout box(String title,android.view.View v){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(4,0,4,0);b.addView(label(title));b.addView(v);return b;}
    private void pickModel(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"model/obj","application/sla","application/octet-stream","text/plain"});startActivityForResult(i,PICK_MODEL);}
    private double parse(EditText e,String name){try{return Double.parseDouble(e.getText().toString().trim());}catch(Exception ex){throw new IllegalArgumentException(name+" sayısal olmalı");}}

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode!=PICK_MODEL||resultCode!=RESULT_OK||data==null)return;Uri uri=data.getData();if(uri==null)return;currentName=getName(uri);status.setText("Model okunuyor ve mesh hazırlanıyor: "+currentName);modeBadge.setText("MOD: GEOMETRY + MESH QA");modeBadge.setBackgroundColor(Color.rgb(15,95,115));supportPoint=null;loadPoint=null;
        final double scale=AutoFemSetup.unitScaleToMetres((String)unitSpinner.getSelectedItem());
        executor.submit(()->{try(InputStream in=getContentResolver().openInputStream(uri)){MeshModel m=MeshParser.parse(currentName,in);SurfaceTopologyReport topo=SurfaceTopologyReport.evaluate(m);if(!topo.closedManifold)throw new IllegalStateException("Kapalı/manifold değil: "+topo.summary());VoxelTetMesher.Result mr=VoxelTetMesher.generate(m,12,scale);if(!mr.quality.pass)throw new IllegalStateException("Mesh QA başarısız: "+mr.quality.summary());currentModel=m;currentMesh=mr;runOnUiThread(()->{viewport.setModel(m);viewport.setSupportPoint(null);viewport.setLoadPoint(null);status.setText("Model hazır — şimdi mesnet ve yük bölgesini seç");modeBadge.setText("MOD: MODEL READY • PICK BC/LOAD");modeBadge.setBackgroundColor(Color.rgb(20,115,105));report.setText("MODEL READY\n\n"+currentName+"\nSurface: "+topo.summary()+"\nNodes: "+mr.mesh.nodes.size()+"\nTET4: "+mr.mesh.tets.size()+"\n"+mr.quality.summary()+"\n\nSıradaki: MESNET SEÇ ve YÜK YÜZEYİ SEÇ.");});}catch(Exception e){runOnUiThread(()->{status.setText("Model/mesh durdu");modeBadge.setText("MOD: QA BLOCKED");modeBadge.setBackgroundColor(Color.rgb(150,45,45));report.setText("Hata: "+e.getMessage());});}});
    }

    private void solvePicked(){if(currentModel==null||currentMesh==null){status.setText("Önce model yükle");return;}if(supportPoint==null||loadPoint==null){status.setText("Mesnet ve yük noktalarının ikisini de seç");return;}final double load,eGPa,nu,yieldMPa;try{load=parse(loadInput,"Yük");eGPa=parse(eInput,"E");nu=parse(nuInput,"Poisson");yieldMPa=parse(yieldInput,"Akma");}catch(Exception e){status.setText(e.getMessage());return;}final String axis=(String)axisSpinner.getSelectedItem();final double scale=AutoFemSetup.unitScaleToMetres((String)unitSpinner.getSelectedItem());modeBadge.setText("MOD: REAL FEM SOLVING");status.setText("Seçilen BC/yük ile çözülüyor…");
        executor.submit(()->{try{LinearElasticMaterial mat=new LinearElasticMaterial("User isotropic",eGPa*1e9,nu,7850,yieldMPa*1e6);StaticFemSolver solver=new StaticFemSolver(currentMesh.mesh,mat);double fx=0,fy=0,fz=0;if(axis.equals("-Z"))fz=-load;else if(axis.equals("+Z"))fz=load;else if(axis.equals("-Y"))fy=-load;else if(axis.equals("+Y"))fy=load;else if(axis.equals("-X"))fx=-load;else fx=load;UserFemSetup.SetupResult setup=UserFemSetup.apply(solver,currentMesh.mesh,supportPoint,loadPoint,scale,fx,fy,fz);StaticFemSolver.Result fem=solver.solve();double fos=fem.maxVonMisesPa>0?mat.yieldPa/fem.maxVonMisesPa:Double.POSITIVE_INFINITY;boolean pass=fem.linearSolve.converged&&fem.forceEquilibriumRelativeError<1e-5&&Double.isFinite(fem.maxVonMisesPa);String txt=String.format(Locale.US,"REAL FEM • USER-PICKED BC/LOAD\n\nModel: %s\nFixed patch nodes: %d\nLoaded patch nodes: %d\nPatch radius: %.5g m\nLoad: %.5g N %s\n\nPCG converged: %s\nIterations: %d\nResidual: %.3e\nForce equilibrium error: %.3e\n\nMax displacement: %.6g mm\nMax Von Mises: %.6g MPa\nYield FoS: %.4f\n\nNUMERICAL QA: %s\n\nRenkli noktalar volumetrik eleman Von Mises seviyesini mavi→kırmızı ölçeğinde gösterir. Mesnet/yük seçimi lokal düğüm yamalarına eşlenmiştir.",currentName,setup.fixedNodes,setup.loadedNodes,setup.radiusM,load,axis,fem.linearSolve.converged,fem.linearSolve.iterations,fem.linearSolve.relativeResidual,fem.forceEquilibriumRelativeError,fem.maxDisplacementM*1000,fem.maxVonMisesPa/1e6,fos,pass?"PASS":"BLOCKED");runOnUiThread(()->{viewport.setResult(currentMesh.mesh,fem);status.setText(pass?"Gerçek FEM tamamlandı • kontur hazır":"FEM QA bloke edildi");modeBadge.setText(pass?"MOD: REAL FEM • CONTOUR":"MOD: FEM • QA BLOCKED");modeBadge.setBackgroundColor(pass?Color.rgb(20,125,70):Color.rgb(150,45,45));report.setText(txt);});}catch(Exception e){runOnUiThread(()->{status.setText("FEM çözülemedi");modeBadge.setText("MOD: FEM ERROR / BC REVIEW");modeBadge.setBackgroundColor(Color.rgb(150,45,45));report.setText("Hata: "+e.getMessage()+"\n\nMesnet yaması rijit-cisim modlarını yeterince kısıtlamıyor veya model numerik olarak kötü koşullu olabilir.");});}});
    }

    private void runFemVerification(){status.setText("Solver benchmark…");executor.submit(()->{try{FemBenchmarks.BenchmarkResult b=FemBenchmarks.unitTetSanity();runOnUiThread(()->{status.setText(b.pass?"FEM çekirdeği VERIFIED":"FEM çekirdeği FAILED");report.setText(b.message);});}catch(Exception e){runOnUiThread(()->status.setText("Benchmark hata: "+e.getMessage()));}});}
    private String getName(Uri uri){String name="model";Cursor c=getContentResolver().query(uri,null,null,null,null);if(c!=null){try{int idx=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(c.moveToFirst()&&idx>=0)name=c.getString(idx);}finally{c.close();}}return name;}
    @Override protected void onDestroy(){super.onDestroy();executor.shutdownNow();}
}
