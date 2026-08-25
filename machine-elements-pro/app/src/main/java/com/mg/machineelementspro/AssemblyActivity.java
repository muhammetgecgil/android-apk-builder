package com.mg.machineelementspro;

import android.app.Activity;
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

public class AssemblyActivity extends Activity {
    private final EditText[] inputs=new EditText[6];
    private Spinner spinner;
    private TextView title,status,body,note;
    private int selected;

    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(build());update(0);}

    private View build(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.rgb(238,242,255));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(32));scroll.addView(root);
        root.addView(text("ASSEMBLY DESIGNER",24,true,Color.rgb(30,41,59)));
        TextView sub=text("Bağlantılı eleman çözümü • otomatik seçim • kritik mod",14,false,Color.rgb(71,85,105));sub.setPadding(0,dp(4),0,dp(12));root.addView(sub);
        Button back=new Button(this);back.setText("← Geri");back.setAllCaps(false);back.setOnClickListener(v->finish());root.addView(back,new LinearLayout.LayoutParams(-1,dp(48)));
        spinner=new Spinner(this);spinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,AssemblyCalculationEngine.MODULES));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(56));sp.topMargin=dp(10);root.addView(spinner,sp);
        for(int i=0;i<6;i++){EditText e=new EditText(this);inputs[i]=e;e.setTextSize(16);e.setSingleLine(true);e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);e.setPadding(dp(12),dp(8),dp(12),dp(8));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(58));p.topMargin=dp(6);root.addView(e,p);}
        Button calc=new Button(this);calc.setText("ASSEMBLY HESAPLA");calc.setAllCaps(false);calc.setTextColor(Color.WHITE);calc.setTypeface(Typeface.DEFAULT,Typeface.BOLD);calc.setBackgroundColor(Color.rgb(67,56,202));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(56));cp.topMargin=dp(14);root.addView(calc,cp);
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(16),dp(16),dp(16));card.setBackgroundColor(Color.WHITE);LinearLayout.LayoutParams cardp=new LinearLayout.LayoutParams(-1,-2);cardp.topMargin=dp(14);root.addView(card,cardp);
        title=text("Sonuç",18,true,Color.rgb(15,23,42));status=text("",15,true,Color.rgb(67,56,202));status.setPadding(0,dp(8),0,dp(8));body=text("Değerleri girin.",16,false,Color.rgb(30,41,59));body.setLineSpacing(0,1.22f);note=text("",12,false,Color.rgb(100,116,139));note.setPadding(0,dp(12),0,0);card.addView(title);card.addView(status);card.addView(body);card.addView(note);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){@Override public void onItemSelected(AdapterView<?> p,View v,int pos,long id){selected=pos;update(pos);}@Override public void onNothingSelected(AdapterView<?> p){}});
        calc.setOnClickListener(v->calculate());return scroll;
    }

    private void update(int m){String[] l=AssemblyCalculationEngine.LABELS[m];for(int i=0;i<6;i++){inputs[i].setHint(l[i]);inputs[i].setText("");inputs[i].setVisibility(l[i].isEmpty()?View.GONE:View.VISIBLE);}title.setText("Sonuç");status.setText("");body.setText("Değerleri girin.");note.setText("");}
    private void calculate(){try{double[] v=new double[6];for(int i=0;i<6;i++){if(inputs[i].getVisibility()==View.GONE){v[i]=0;continue;}String s=inputs[i].getText().toString().trim().replace(',','.');if(s.isEmpty())throw new IllegalArgumentException("Eksik alan: "+AssemblyCalculationEngine.LABELS[selected][i]);v[i]=Double.parseDouble(s);}AssemblyCalculationEngine.Result r=AssemblyCalculationEngine.calculate(selected,v);title.setText(r.title);status.setText(r.status);status.setTextColor(r.status.contains("UYGUN DEĞİL")?Color.rgb(185,28,28):r.status.contains("SINIRDA")||r.status.contains("KONTROL")?Color.rgb(180,83,9):Color.rgb(67,56,202));body.setText(r.body);note.setText(r.note);}catch(Exception e){Toast.makeText(this,e.getMessage()==null?"Girişleri kontrol edin.":e.getMessage(),Toast.LENGTH_LONG).show();}}
    private TextView text(String s,int sp,boolean bold,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
