package com.mg.machineelementspro;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class CustomGeometryActivity extends Activity {
    private Spinner mode;
    private EditText projectName,geometry,p1,p2,p3,p4;
    private TextView result;
    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(build());loadProject();}

    private View build(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(Color.rgb(245,247,250));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(32));sc.addView(root);
        root.addView(txt("CUSTOM GEOMETRY LAB",24,true,Color.rgb(17,24,39)));
        TextView sub=txt("Serbest istasyon/koordinat girişi • proje kaydet/geri yükle",14,false,Color.rgb(75,85,99));sub.setPadding(0,dp(4),0,dp(10));root.addView(sub);
        Button back=new Button(this);back.setText("← Geri");back.setAllCaps(false);back.setOnClickListener(v->finish());root.addView(back,lp(-1,dp(48),0));
        projectName=field("Proje adı",false);root.addView(projectName,lp(-1,dp(56),dp(8)));
        mode=new Spinner(this);mode.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"User-defined shaft","Coordinate bolt group","Preferred shaft selector"}));root.addView(mode,lp(-1,dp(56),dp(8)));
        geometry=field("Geometri satırları",true);geometry.setMinLines(5);root.addView(geometry,lp(-1,dp(150),dp(8)));
        p1=field("P1",false);p2=field("P2",false);p3=field("P3",false);p4=field("P4",false);root.addView(p1,lp(-1,dp(56),dp(6)));root.addView(p2,lp(-1,dp(56),dp(6)));root.addView(p3,lp(-1,dp(56),dp(6)));root.addView(p4,lp(-1,dp(56),dp(6)));
        Button calc=new Button(this);calc.setText("ÇÖZ");calc.setAllCaps(false);calc.setTextColor(Color.WHITE);calc.setTypeface(Typeface.DEFAULT,Typeface.BOLD);calc.setBackgroundColor(Color.rgb(3,105,161));calc.setOnClickListener(v->solve());root.addView(calc,lp(-1,dp(56),dp(12)));
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);Button save=new Button(this);save.setText("Kaydet");save.setAllCaps(false);save.setOnClickListener(v->saveProject());Button load=new Button(this);load.setText("Geri yükle");load.setAllCaps(false);load.setOnClickListener(v->loadProject());row.addView(save,new LinearLayout.LayoutParams(0,dp(50),1));row.addView(load,new LinearLayout.LayoutParams(0,dp(50),1));root.addView(row,lp(-1,dp(50),dp(8)));
        result=txt("Mod seçildiğinde alanlar güncellenir.",15,false,Color.rgb(31,41,55));result.setPadding(dp(12),dp(14),dp(12),dp(14));result.setBackgroundColor(Color.WHITE);root.addView(result,lp(-1,-2,dp(12)));
        mode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){configure(pos);}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        return sc;
    }

    private void configure(int m){
        if(m==0){geometry.setVisibility(View.VISIBLE);geometry.setHint("Her satır: x,Fy,Fz\nÖrnek:\n200,1000,0\n500,-300,600");p1.setHint("Span L (mm)");p2.setHint("Mil çapı d (mm)");p3.setHint("Sy (MPa)");p4.setHint("Tork (N·m)");}
        else if(m==1){geometry.setVisibility(View.VISIBLE);geometry.setHint("Her satır: x,y\nÖrnek:\n-50,-40\n50,-40\n50,40\n-50,40");p1.setHint("Fx (N)");p2.setHint("Fy (N)");p3.setHint("Mz (N·m)");p4.setHint("Civata d / izin kayma: d,allow");}
        else {geometry.setVisibility(View.GONE);p1.setHint("Moment M (N·m)");p2.setHint("Tork T (N·m)");p3.setHint("Sy (MPa)");p4.setHint("Hedef FoS");}
    }

    private void solve(){try{int m=mode.getSelectedItemPosition();CustomGeometryEngine.Result r;if(m==0)r=CustomGeometryEngine.shaft(geometry.getText().toString(),num(p1),num(p2),num(p3),num(p4));else if(m==1){String[] q=p4.getText().toString().trim().replace(';',',').split(",");if(q.length!=2)throw new IllegalArgumentException("P4 format: boltDiameter,allowShear");r=CustomGeometryEngine.boltGroup(geometry.getText().toString(),num(p1),num(p2),num(p3),Double.parseDouble(q[0].trim()),Double.parseDouble(q[1].trim()));}else r=CustomGeometryEngine.selectPreferredShaft(num(p1),num(p2),num(p3),num(p4));result.setText(r.title+"\n\n"+r.status+"\n\n"+r.body+"\n\n"+r.note);}catch(Exception e){Toast.makeText(this,e.getMessage()==null?"Girişleri kontrol edin":e.getMessage(),Toast.LENGTH_LONG).show();}}
    private void saveProject(){String params=p1.getText()+"|"+p2.getText()+"|"+p3.getText()+"|"+p4.getText();ProjectStore.save(this,projectName.getText().toString(),String.valueOf(mode.getSelectedItemPosition()),geometry.getText().toString(),params);Toast.makeText(this,"Proje kaydedildi",Toast.LENGTH_SHORT).show();}
    private void loadProject(){ProjectStore.Snapshot s=ProjectStore.load(this);projectName.setText(s.name);int m=0;try{m=Integer.parseInt(s.mode);}catch(Exception ignored){}mode.setSelection(Math.max(0,Math.min(2,m)));geometry.setText(s.text);String[] p=s.params.split("\\|",-1);if(p.length>0)p1.setText(p[0]);if(p.length>1)p2.setText(p[1]);if(p.length>2)p3.setText(p[2]);if(p.length>3)p4.setText(p[3]);}
    private double num(EditText e){String s=e.getText().toString().trim().replace(',','.');if(s.isEmpty())throw new IllegalArgumentException("Eksik alan: "+e.getHint());return Double.parseDouble(s);}
    private EditText field(String hint,boolean multi){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(16);e.setSingleLine(!multi);e.setInputType(multi?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE:InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);return e;}
    private TextView txt(String s,int sp,boolean bold,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private LinearLayout.LayoutParams lp(int w,int h,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.topMargin=top;return p;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
