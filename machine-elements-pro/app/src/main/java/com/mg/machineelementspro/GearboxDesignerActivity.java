package com.mg.machineelementspro;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class GearboxDesignerActivity extends Activity {
    private final EditText[] in=new EditText[12];
    private TextView out;
    private Button productButton;
    private GearboxDesignEngine.GearboxResult lastResult;
    private double lastInputRpm,lastEfficiency;
    private final String[] hints={"Giriş torku (Nm)","Giriş devri (rpm)","Pinyon diş sayısı","Dişli diş sayısı","Modül (mm)","Yüz genişliği (mm)","Basınç açısı (deg)","Verim (0..1)","Mil Sy (MPa)","Mil hedef FoS","Dişli izinli eğilme (MPa)","Hedef ömür (h)"};

    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(build());}
    private ScrollView build(){
        ScrollView s=new ScrollView(this); LinearLayout r=new LinearLayout(this); r.setOrientation(LinearLayout.VERTICAL); r.setPadding(dp(18),dp(20),dp(18),dp(30)); s.addView(r);
        TextView t=new TextView(this);t.setText("GEARBOX DESIGNER");t.setTextSize(24);t.setTextColor(Color.rgb(15,23,42));r.addView(t);
        TextView sub=new TextView(this);sub.setText("Tek kademeli dişli kutusu • dişli kuvvetleri + mil + rulman + güvenlik");sub.setTextSize(14);sub.setTextColor(Color.rgb(71,85,105));r.addView(sub);
        for(int i=0;i<in.length;i++){in[i]=new EditText(this);in[i].setHint(hints[i]);in[i].setSingleLine(true);in[i].setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);r.addView(in[i],new LinearLayout.LayoutParams(-1,dp(56)));}
        String[] defaults={"100","1500","20","60","3","30","20","0.97","530","2","250","10000"};for(int i=0;i<in.length;i++)in[i].setText(defaults[i]);
        Button calc=new Button(this);calc.setText("DİŞLİ KUTUSUNU BOYUTLANDIR");r.addView(calc,new LinearLayout.LayoutParams(-1,dp(58)));
        productButton=new Button(this);productButton.setText("BU HESABA GÖRE REDÜKTÖR BUL →");productButton.setVisibility(android.view.View.GONE);r.addView(productButton,new LinearLayout.LayoutParams(-1,dp(56)));
        out=new TextView(this);out.setTextSize(16);out.setTextColor(Color.rgb(30,41,59));out.setPadding(0,dp(16),0,0);r.addView(out);
        calc.setOnClickListener(v->runCalc());productButton.setOnClickListener(v->openCatalog());return s;
    }
    private void runCalc(){try{
        double T=d(0),rpm=d(1),module=d(4),face=d(5),pa=d(6),eff=d(7),sy=d(8),fos=d(9),allow=d(10),life=d(11);lastInputRpm=rpm;lastEfficiency=eff;
        int z1=(int)Math.round(d(2)),z2=(int)Math.round(d(3));
        GearboxDesignEngine.GearboxResult x=GearboxDesignEngine.sizeSingleStage(T,rpm,z1,z2,module,face,pa,eff,sy,fos,allow,life);lastResult=x;
        out.setText(String.format(java.util.Locale.US,
            "Oran: %.3f\nÇıkış torku: %.2f Nm\nFt: %.1f N\nFr: %.1f N\nPinyon hatve çapı: %.2f mm\nDişli hatve çapı: %.2f mm\n\nMil-1 gerekli / tercihli: %.2f / %.1f mm\nMil-2 gerekli / tercihli: %.2f / %.1f mm\n\nRulman-1: %s • L10h %.0f h • s0 %.2f\nRulman-2: %s • L10h %.0f h • s0 %.2f\n\nDişli eğilme gerilmesi: %.2f MPa\nDişli FoS: %.2f",
            x.ratio,x.outputTorqueNm,x.tangentialForceN,x.radialForceN,x.pinionPitchDiameterMm,x.gearPitchDiameterMm,x.shaft1RequiredMm,x.shaft1PreferredMm,x.shaft2RequiredMm,x.shaft2PreferredMm,x.bearing1.designation,x.bearing1.lifeHours,x.bearing1.staticFoS,x.bearing2.designation,x.bearing2.lifeHours,x.bearing2.staticFoS,x.gearBendingStressMpa,x.gearSafetyFactor));
        productButton.setVisibility(android.view.View.VISIBLE);
    }catch(Exception e){Toast.makeText(this,e.getMessage()==null?"Girişleri kontrol edin":e.getMessage(),Toast.LENGTH_LONG).show();}}
    private void openCatalog(){if(lastResult==null)return;double powerKw=d(0)*lastInputRpm*2.0*Math.PI/60.0/1000.0;double outRpm=lastInputRpm/lastResult.ratio;Intent i=new Intent(this,ProductCatalogActivity.class);i.putExtra(ProductCatalogActivity.EXTRA_TYPE,3);i.putExtra(ProductCatalogActivity.EXTRA_V0,String.format(java.util.Locale.US,"%.4g",powerKw));i.putExtra(ProductCatalogActivity.EXTRA_V1,String.format(java.util.Locale.US,"%.4g",lastInputRpm));i.putExtra(ProductCatalogActivity.EXTRA_V2,String.format(java.util.Locale.US,"%.4g",outRpm));i.putExtra(ProductCatalogActivity.EXTRA_V3,String.format(java.util.Locale.US,"%.4g",lastResult.outputTorqueNm));startActivity(i);}
    private double d(int i){String s=in[i].getText().toString().trim().replace(',','.');if(s.isEmpty())throw new IllegalArgumentException(hints[i]+" eksik");return Double.parseDouble(s);}private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
