package com.mg.machineelementspro;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class SystemDesignerActivity extends Activity {
    private final EditText[] fields=new EditText[9];
    private TextView result;
    private Button productButton;
    private int mode=0;
    private SystemSelectionEngine.ShaftBearingFitResult lastBearingResult;
    private double lastRadialN,lastAxialN,lastRpm,lastLifeH;
    private double lastBoltDiameter;
    private final String[][] labels={
            {"Bending moment M [Nm]","Torque T [Nm]","Shaft Sy [MPa]","Target shaft FoS","Radial load Fr [N]","Axial load Fa [N]","Speed [rpm]","Target life [h]","Required static FoS"},
            {"Nominal bolt diameter [mm]","Target preload [N]","Nominal nut factor K","Preload scatter [%]","Nut-factor scatter [%]","","","",""}
    };
    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(build());update(0);}
    private View build(){
        ScrollView s=new ScrollView(this); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(18),dp(18),dp(28)); s.addView(root);
        root.addView(txt("SYSTEM DESIGNER",24,true,Color.rgb(15,23,42)));
        root.addView(txt("Shaft–bearing–fit zinciri ve civata sıkma saçılımı",14,false,Color.rgb(71,85,105)));
        Spinner sp=new Spinner(this); sp.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Shaft–Bearing–Fit Designer","Bolt Tightening Designer"})); root.addView(sp,new LinearLayout.LayoutParams(-1,dp(56)));
        for(int i=0;i<fields.length;i++){ fields[i]=new EditText(this); fields[i].setSingleLine(true); fields[i].setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED); root.addView(fields[i],lp(dp(56),8)); }
        Button calc=new Button(this); calc.setText("SİSTEMİ ÇÖZ"); calc.setAllCaps(false); calc.setTypeface(Typeface.DEFAULT,Typeface.BOLD); calc.setTextColor(Color.WHITE); calc.setBackgroundColor(Color.rgb(3,105,161)); root.addView(calc,lp(dp(56),12));
        productButton=new Button(this);productButton.setText("HESAPTAN ÜRÜN BUL →");productButton.setAllCaps(false);productButton.setVisibility(View.GONE);root.addView(productButton,lp(dp(54),8));
        result=txt("Değerleri girin.",16,false,Color.rgb(30,41,59)); result.setPadding(dp(14),dp(14),dp(14),dp(14)); result.setBackgroundColor(Color.WHITE); root.addView(result,lp(-2,14));
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){mode=pos;update(pos);}public void onNothingSelected(AdapterView<?> p){}});
        calc.setOnClickListener(v->solve()); productButton.setOnClickListener(v->openCatalog()); return s;
    }
    private void update(int m){for(int i=0;i<fields.length;i++){String x=labels[m][i]; fields[i].setHint(x); fields[i].setText(""); fields[i].setVisibility(x.isEmpty()?View.GONE:View.VISIBLE);} result.setText("Değerleri girin.");productButton.setVisibility(View.GONE);lastBearingResult=null;}
    private double val(int i){String x=fields[i].getText().toString().trim().replace(',','.'); if(x.isEmpty())throw new IllegalArgumentException("Eksik alan: "+labels[mode][i]); return Double.parseDouble(x);}
    private void solve(){try{
        if(mode==0){
            lastRadialN=val(4);lastAxialN=val(5);lastRpm=val(6);lastLifeH=val(7);
            SystemSelectionEngine.ShaftBearingFitResult r=SystemSelectionEngine.solveShaftBearingFit(val(0),val(1),val(2),val(3),lastRadialN,lastAxialN,lastRpm,lastLifeH,val(8));lastBearingResult=r;
            StringBuilder b=new StringBuilder(); b.append("Gerekli mil çapı: ").append(f(r.shaftRequiredMm)).append(" mm\nTercihli mil çapı: ").append(f(r.shaftPreferredMm)).append(" mm\nRulman: ").append(r.bearing).append("\nL10h: ").append(f(r.bearingLifeHours)).append(" h\nStatik FoS: ").append(f(r.bearingStaticFoS)).append("\nMil geçmesi: ").append(r.shaftFit).append("\nGövde geçmesi: ").append(r.housingFit);
            result.setText(b.toString()); productButton.setText("BU HESABA GÖRE RULMAN BUL →");productButton.setVisibility(View.VISIBLE);
        }else{
            lastBoltDiameter=val(0);SystemSelectionEngine.TighteningResult r=SystemSelectionEngine.tighteningScatter(lastBoltDiameter,val(1),val(2),val(3),val(4));
            result.setText("Preload min/nom/max: "+f(r.preloadMinN)+" / "+f(r.nominalPreloadN)+" / "+f(r.preloadMaxN)+" N\nTork min/nom/max: "+f(r.torqueMinNm)+" / "+f(r.torqueNominalNm)+" / "+f(r.torqueMaxNm)+" Nm");productButton.setText("BU HESABA GÖRE CIVATA BUL →");productButton.setVisibility(View.VISIBLE);
        }
    }catch(Exception e){Toast.makeText(this,e.getMessage()==null?"Girişleri kontrol edin":e.getMessage(),Toast.LENGTH_LONG).show();}}
    private void openCatalog(){
        Intent i=new Intent(this,ProductCatalogActivity.class);
        if(mode==0&&lastBearingResult!=null){
            double p=Math.hypot(lastRadialN,lastAxialN);double lifeRev=Math.max(1,lastLifeH*60.0*lastRpm);double cReq=p*Math.cbrt(lifeRev/1_000_000.0);
            i.putExtra(ProductCatalogActivity.EXTRA_TYPE,0);i.putExtra(ProductCatalogActivity.EXTRA_V0,f(lastBearingResult.shaftPreferredMm));i.putExtra(ProductCatalogActivity.EXTRA_V1,f(cReq));
        }else if(mode==1){i.putExtra(ProductCatalogActivity.EXTRA_TYPE,1);i.putExtra(ProductCatalogActivity.EXTRA_V0,f(lastBoltDiameter));i.putExtra(ProductCatalogActivity.EXTRA_V1,"10.9");}
        startActivity(i);
    }
    private String f(double x){return String.format(Locale.US,"%.4g",x);} private TextView txt(String s,int z,boolean b,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);if(b)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;} private LinearLayout.LayoutParams lp(int h,int t){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,h);p.topMargin=dp(t);return p;} private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
}
