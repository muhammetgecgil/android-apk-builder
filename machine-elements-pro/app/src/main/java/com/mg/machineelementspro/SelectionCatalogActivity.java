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

public class SelectionCatalogActivity extends Activity {
    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(build());}
    private android.view.View build(){
        ScrollView s=new ScrollView(this); LinearLayout r=new LinearLayout(this); r.setOrientation(LinearLayout.VERTICAL); r.setPadding(dp(18),dp(20),dp(18),dp(30)); s.addView(r);
        r.addView(text("SELECTION CATALOG",24,true));
        r.addView(text("Civata sınıfı • rulman • geçme • malzeme optimizasyonu",14,false));
        Spinner bolt=new Spinner(this); String[] bc={"8.8","10.9","12.9"}; bolt.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,bc)); r.addView(bolt);
        TextView boltOut=text("",15,false); r.addView(boltOut); bolt.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int pos,long id){SelectionCatalog.BoltClass c=SelectionCatalog.findBoltClass(bc[pos]);boltOut.setText("Sy="+c.sy+" MPa  Sut="+c.sut+" MPa  Proof≈"+c.proof+" MPa");}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        EditText bore=num("Min. rulman deliği (mm)"); EditText reqC=num("Gerekli C (N)"); r.addView(bore);r.addView(reqC); Button sel=new Button(this); sel.setText("RULMAN SEÇ"); r.addView(sel); TextView bearingOut=text("",15,false);r.addView(bearingOut);sel.setOnClickListener(v->{try{SelectionCatalog.Bearing x=SelectionCatalog.selectBearing(val(bore),val(reqC));bearingOut.setText(x.code+"  "+x.bore+"×"+x.od+"×"+x.width+" mm  C="+x.C+" N  C0="+x.C0+" N");}catch(Exception e){bearingOut.setText(e.getMessage());}});
        Spinner fit=new Spinner(this); String[] fs={"H7/g6","H7/h6","H7/k6","H7/m6","H7/p6"}; fit.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,fs));r.addView(fit);TextView fitOut=text("",14,false);r.addView(fitOut);fit.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int pos,long id){fitOut.setText(SelectionCatalog.fitGuidance(fs[pos]));}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        EditText M=num("Eğilme momenti M (N·m)"),T=num("Tork T (N·m)"),fos=num("Hedef FoS");r.addView(M);r.addView(T);r.addView(fos);Button compare=new Button(this);compare.setText("MALZEMELERİ KARŞILAŞTIR");r.addView(compare);TextView matOut=text("",14,false);matOut.setLineSpacing(0,1.2f);r.addView(matOut);compare.setOnClickListener(v->{try{StringBuilder z=new StringBuilder();for(String q:SelectionCatalog.compareMaterialsForShaft(val(M),val(T),val(fos)))z.append(q).append("\n");matOut.setText(z.toString());}catch(Exception e){matOut.setText(e.getMessage());}});
        Button back=new Button(this);back.setText("← Geri");back.setOnClickListener(v->finish());r.addView(back);return s;
    }
    private EditText num(String h){EditText e=new EditText(this);e.setHint(h);e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);return e;}
    private double val(EditText e){return Double.parseDouble(e.getText().toString().trim().replace(',','.'));}
    private TextView text(String x,int sp,boolean bold){TextView t=new TextView(this);t.setText(x);t.setTextSize(sp);t.setTextColor(Color.rgb(30,41,59));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
