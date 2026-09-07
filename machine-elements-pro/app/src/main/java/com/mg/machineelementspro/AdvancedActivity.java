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

public class AdvancedActivity extends Activity {
    private final EditText[] inputs = new EditText[6];
    private Spinner moduleSpinner;
    private TextView resultTitle, resultBody, resultStatus, resultNote;
    private int selectedModule;

    @Override protected void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);setContentView(buildUi());updateFields(0);}

    private View buildUi(){
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(Color.rgb(241,245,249));
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(20),dp(18),dp(32)); scroll.addView(root);
        TextView title=text("ADVANCED ENGINEERING",24,true,Color.rgb(15,23,42)); root.addView(title);
        TextView sub=text("Sistem seviyesi hesaplar • bağlantılı yük aktarımı",14,false,Color.rgb(71,85,105)); sub.setPadding(0,dp(4),0,dp(10)); root.addView(sub);
        Button back=new Button(this); back.setText("← Temel hesaplara dön"); back.setAllCaps(false); back.setOnClickListener(v->finish()); root.addView(back,lp(-1,dp(48),0));
        Button drivetrain=new Button(this);drivetrain.setText("DRIVETRAIN SYSTEM →");drivetrain.setAllCaps(false);drivetrain.setTypeface(Typeface.DEFAULT,Typeface.BOLD);drivetrain.setTextColor(Color.WHITE);drivetrain.setBackgroundColor(Color.rgb(109,40,217));drivetrain.setOnClickListener(v->startActivity(new Intent(this,DrivetrainActivity.class)));root.addView(drivetrain,lp(-1,dp(54),dp(10)));
        moduleSpinner=new Spinner(this); moduleSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,AdvancedCalculationEngine.MODULES)); root.addView(moduleSpinner,lp(-1,dp(56),dp(10)));
        for(int i=0;i<inputs.length;i++){inputs[i]=new EditText(this);inputs[i].setTextSize(16);inputs[i].setSingleLine(true);inputs[i].setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);inputs[i].setPadding(dp(12),dp(8),dp(12),dp(8));root.addView(inputs[i],lp(-1,dp(58),dp(6)));}
        Button calc=new Button(this);calc.setText("İLERİ HESAPLA");calc.setAllCaps(false);calc.setTextColor(Color.WHITE);calc.setTypeface(Typeface.DEFAULT,Typeface.BOLD);calc.setBackgroundColor(Color.rgb(30,64,175));root.addView(calc,lp(-1,dp(56),dp(14)));
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(16),dp(16),dp(16));card.setBackgroundColor(Color.WHITE);root.addView(card,lp(-1,-2,dp(14)));
        resultTitle=text("Sonuç",18,true,Color.rgb(15,23,42)); resultStatus=text("",15,true,Color.rgb(30,64,175)); resultStatus.setPadding(0,dp(8),0,dp(8)); resultBody=text("Değerleri girin.",16,false,Color.rgb(30,41,59)); resultBody.setLineSpacing(0,1.22f); resultNote=text("",12,false,Color.rgb(100,116,139)); resultNote.setPadding(0,dp(12),0,0); card.addView(resultTitle);card.addView(resultStatus);card.addView(resultBody);card.addView(resultNote);
        TextView foot=text("Drivetrain System dişli kuvvetlerinden başlayıp mil ve rulmanlara kadar aynı yük zincirini çözer. Buradaki 8 modül ise bağlantı, pres geçme, yorulma ve alt-sistem kontrolleridir.",11,false,Color.rgb(100,116,139));foot.setPadding(0,dp(16),0,0);root.addView(foot);
        moduleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){@Override public void onItemSelected(AdapterView<?> p,View v,int pos,long id){selectedModule=pos;updateFields(pos);}@Override public void onNothingSelected(AdapterView<?> p){}});
        calc.setOnClickListener(v->calculate()); return scroll;
    }

    private void updateFields(int m){String[] labels=AdvancedCalculationEngine.LABELS[m];for(int i=0;i<6;i++){inputs[i].setHint(labels[i]);inputs[i].setVisibility(labels[i].isEmpty()?View.GONE:View.VISIBLE);inputs[i].setText("");}resultTitle.setText("Sonuç");resultStatus.setText("");resultBody.setText("Değerleri girin.");resultNote.setText("");}
    private void calculate(){try{double[] v=new double[6];for(int i=0;i<6;i++){if(inputs[i].getVisibility()==View.GONE){v[i]=0;continue;}String s=inputs[i].getText().toString().trim().replace(',','.');if(s.isEmpty())throw new IllegalArgumentException("Eksik alan: "+AdvancedCalculationEngine.LABELS[selectedModule][i]);v[i]=Double.parseDouble(s);}AdvancedCalculationEngine.Result r=AdvancedCalculationEngine.calculate(selectedModule,v);resultTitle.setText(r.title);resultStatus.setText(r.status);resultStatus.setTextColor(r.status.contains("UYGUN DEĞİL")?Color.rgb(185,28,28):r.status.contains("SINIRDA")?Color.rgb(180,83,9):Color.rgb(30,64,175));resultBody.setText(r.body);resultNote.setText(r.note);}catch(Exception e){Toast.makeText(this,e.getMessage()==null?"Girişleri kontrol edin.":e.getMessage(),Toast.LENGTH_LONG).show();}}
    private TextView text(String s,int sp,boolean bold,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(color);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private LinearLayout.LayoutParams lp(int w,int h,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.topMargin=top;return p;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
