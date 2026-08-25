package com.mg.machineelementspro;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class DesignOptimizerActivity extends Activity {
    private Spinner mode;
    private final EditText[] f=new EditText[5];
    private TextView out;
    private final String[][] labels={
            {"Radial load Fr [N]","Axial load Fa [N]","Speed [rpm]","Target life [h]","Required static FoS"},
            {"Service tension [N]","Target preload [N]","Target proof FoS","",""},
            {"Nominal diameter [mm]","","","",""},
            {"Bending moment [N·m]","Torque [N·m]","Target FoS","",""}
    };

    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(build());}

    private ScrollView build(){
        ScrollView s=new ScrollView(this);s.setBackgroundColor(Color.rgb(248,250,252));
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(18),dp(20),dp(18),dp(30));s.addView(r);
        TextView t=txt("DESIGN OPTIMIZER",24,true);r.addView(t);
        TextView sub=txt("Rulman • civata preload/property class • H7/g6 tolerans • hafif mil optimizasyonu",13,false);sub.setPadding(0,dp(4),0,dp(12));r.addView(sub);
        mode=new Spinner(this);mode.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Bearing life + static selection","Bolt preload + property class","H7/g6 numerical fit","Lightweight shaft material"}));r.addView(mode,new LinearLayout.LayoutParams(-1,dp(54)));
        for(int i=0;i<f.length;i++){f[i]=new EditText(this);f[i].setSingleLine(true);f[i].setTextSize(16);f[i].setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(56));p.topMargin=dp(8);r.addView(f[i],p);}
        mode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int pos,long id){fields(pos);}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        Button run=new Button(this);run.setText("OPTİMİZE ET");run.setAllCaps(false);run.setTypeface(Typeface.DEFAULT,Typeface.BOLD);run.setTextColor(Color.WHITE);run.setBackgroundColor(Color.rgb(126,34,206));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(56));bp.topMargin=dp(14);r.addView(run,bp);
        out=txt("Bir optimizasyon modu seçip değerleri girin.",15,false);out.setPadding(dp(14),dp(14),dp(14),dp(14));out.setBackgroundColor(Color.WHITE);LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(-1,-2);op.topMargin=dp(14);r.addView(out,op);
        run.setOnClickListener(v->calc());fields(0);return s;
    }

    private void fields(int m){for(int i=0;i<5;i++){f[i].setHint(labels[m][i]);f[i].setVisibility(labels[m][i].isEmpty()?android.view.View.GONE:android.view.View.VISIBLE);f[i].setText("");}}
    private double val(int i){String s=f[i].getText().toString().trim().replace(',','.');if(s.isEmpty())throw new IllegalArgumentException("Eksik alan: "+f[i].getHint());return Double.parseDouble(s);}
    private void calc(){try{int m=mode.getSelectedItemPosition();String x;
        if(m==0){DesignOptimizationEngine.BearingPick p=DesignOptimizationEngine.selectBearing(val(0),val(1),val(2),val(3),val(4));x="Seçim: "+p.designation+"\nL10h ≈ "+fmt(p.lifeHours)+" h\nStatik FoS ≈ "+fmt(p.staticFoS);}
        else if(m==1){DesignOptimizationEngine.BoltPick p=DesignOptimizationEngine.selectBolt(val(0),val(1),val(2));x="Seçim: "+p.size+" / "+p.propertyClass+"\nUygulanan preload ≈ "+fmt(p.preloadN)+" N\nProof FoS ≈ "+fmt(p.proofFoS);}
        else if(m==2){double[] q=DesignOptimizationEngine.basicHoleH7ShaftG6(val(0));x="Hole: "+fmt(q[0])+" … "+fmt(q[1])+" mm\nShaft: "+fmt(q[2])+" … "+fmt(q[3])+" mm\nMin clearance ≈ "+fmt(q[4])+" mm\nMax clearance ≈ "+fmt(q[5])+" mm";}
        else {DesignOptimizationEngine.MaterialOption p=DesignOptimizationEngine.optimizeShaft(val(0),val(1),val(2));x="Öneri: "+p.material+"\nGerekli çap ≈ "+fmt(p.diameterMm)+" mm\nKütle ≈ "+fmt(p.kgPerM)+" kg/m\nScore ≈ "+fmt(p.score);}
        out.setText(x+"\n\nNot: Katalog ve tolerans sonuçları bu sürümde mühendislik ön seçimidir; üretim serbest bırakma öncesi güncel standart ve üretici verisiyle doğrulanmalıdır.");
    }catch(Exception e){Toast.makeText(this,e.getMessage()==null?"Girişleri kontrol edin.":e.getMessage(),Toast.LENGTH_LONG).show();}}
    private TextView txt(String s,int z,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(Color.rgb(30,41,59));if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private String fmt(double x){return String.format(java.util.Locale.US,"%.5g",x);}private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
