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

public class MainActivity extends Activity {
    private final EditText[] inputs=new EditText[6];
    private Spinner moduleSpinner; private TextView resultTitle,resultBody,resultStatus,resultNote; private Button resultProductButton; private int selectedModule=0; private double[] lastValues;
    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(buildUi());updateFields(0);}

    private View buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.rgb(248,250,252));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(32));scroll.addView(root,new ScrollView.LayoutParams(-1,-2));
        root.addView(text("MACHINE ELEMENTS PRO",24,true,Color.rgb(15,23,42)));
        TextView sub=text("Makine elemanını seç • kuvvet ve boyutları görsel üzerinde gör • hesapla • gerçek ürüne git",14,false,Color.rgb(71,85,105));sub.setPadding(0,dp(4),0,dp(12));root.addView(sub);

        Button studio=navButton("MAKİNE ELEMANI SEÇ – GÖRSEL HESAP →",Color.rgb(30,64,175));studio.setTextSize(17);studio.setOnClickListener(v->startActivity(new Intent(this,MachineElementStudioActivity.class)));root.addView(studio,lp(-1,dp(66),0));
        Button product=navButton("TÜRKİYE + AVRUPA ÜRÜN SEÇİMİ →",Color.rgb(5,150,105));product.setOnClickListener(v->startActivity(new Intent(this,ProductCatalogActivity.class)));root.addView(product,lp(-1,dp(56),dp(8)));
        Button gearbox=navButton("GEARBOX DESIGNER →",Color.rgb(14,116,144));gearbox.setOnClickListener(v->startActivity(new Intent(this,GearboxDesignerActivity.class)));root.addView(gearbox,lp(-1,dp(54),dp(8)));
        Button drive=navButton("DRIVETRAIN SYSTEM →",Color.rgb(2,132,199));drive.setOnClickListener(v->startActivity(new Intent(this,DrivetrainActivity.class)));root.addView(drive,lp(-1,dp(54),dp(8)));
        Button assembly=navButton("ASSEMBLY DESIGNER →",Color.rgb(67,56,202));assembly.setOnClickListener(v->startActivity(new Intent(this,AssemblyActivity.class)));root.addView(assembly,lp(-1,dp(54),dp(8)));
        Button system=navButton("SYSTEM DESIGNER →",Color.rgb(8,145,178));system.setOnClickListener(v->startActivity(new Intent(this,SystemDesignerActivity.class)));root.addView(system,lp(-1,dp(54),dp(8)));
        Button advanced=navButton("ADVANCED ENGINEERING →",Color.rgb(51,65,85));advanced.setOnClickListener(v->startActivity(new Intent(this,AdvancedActivity.class)));root.addView(advanced,lp(-1,dp(54),dp(8)));

        TextView tools=text("Yardımcı araçlar",13,true,Color.rgb(100,116,139));tools.setPadding(0,dp(18),0,dp(4));root.addView(tools);
        Button review=navButton("DESIGN REVIEW →",Color.rgb(185,28,28));review.setOnClickListener(v->startActivity(new Intent(this,DesignReviewActivity.class)));root.addView(review,lp(-1,dp(50),0));
        Button report=navButton("ENGINEERING REPORT / PDF →",Color.rgb(75,85,99));report.setOnClickListener(v->startActivity(new Intent(this,EngineeringReportActivity.class)));root.addView(report,lp(-1,dp(50),dp(6)));
        Button projects=navButton("PROJECT MANAGER →",Color.rgb(71,85,105));projects.setOnClickListener(v->startActivity(new Intent(this,ProjectManagerActivity.class)));root.addView(projects,lp(-1,dp(50),dp(6)));
        Button optimizer=navButton("DESIGN OPTIMIZER →",Color.rgb(126,34,206));optimizer.setOnClickListener(v->startActivity(new Intent(this,DesignOptimizerActivity.class)));root.addView(optimizer,lp(-1,dp(50),dp(6)));
        Button catalog=navButton("SELECTION CATALOG →",Color.rgb(88,28,135));catalog.setOnClickListener(v->startActivity(new Intent(this,SelectionCatalogActivity.class)));root.addView(catalog,lp(-1,dp(50),dp(6)));
        Button custom=navButton("CUSTOM GEOMETRY LAB →",Color.rgb(3,105,161));custom.setOnClickListener(v->startActivity(new Intent(this,CustomGeometryActivity.class)));root.addView(custom,lp(-1,dp(50),dp(6)));
        Button library=navButton("ENGINEERING LIBRARY →",Color.rgb(55,65,81));library.setOnClickListener(v->startActivity(new Intent(this,LibraryActivity.class)));root.addView(library,lp(-1,dp(50),dp(6)));

        TextView classic=text("Klasik hızlı hesap",13,true,Color.rgb(15,118,110));classic.setPadding(0,dp(20),0,dp(4));root.addView(classic);
        moduleSpinner=new Spinner(this);moduleSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,CalculationEngine.MODULES));root.addView(moduleSpinner,lp(-1,dp(54),0));
        for(int i=0;i<inputs.length;i++){inputs[i]=new EditText(this);inputs[i].setTextSize(16);inputs[i].setSingleLine(true);inputs[i].setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);inputs[i].setPadding(dp(12),dp(8),dp(12),dp(8));root.addView(inputs[i],lp(-1,dp(58),dp(8)));}
        Button calc=new Button(this);calc.setText("HESAPLA");calc.setTextSize(16);calc.setTypeface(Typeface.DEFAULT,Typeface.BOLD);calc.setAllCaps(false);calc.setTextColor(Color.WHITE);calc.setBackgroundColor(Color.rgb(15,118,110));root.addView(calc,lp(-1,dp(56),dp(14)));
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(16),dp(16),dp(16));card.setBackgroundColor(Color.WHITE);root.addView(card,lp(-1,-2,dp(16)));
        resultTitle=text("Sonuç",18,true,Color.rgb(15,23,42));resultStatus=text("",15,true,Color.rgb(15,118,110));resultStatus.setPadding(0,dp(8),0,dp(8));resultBody=text("Değerleri girip hesaplayın.",16,false,Color.rgb(30,41,59));resultBody.setLineSpacing(0,1.25f);resultNote=text("",12,false,Color.rgb(100,116,139));resultNote.setPadding(0,dp(12),0,0);card.addView(resultTitle);card.addView(resultStatus);card.addView(resultBody);card.addView(resultNote);
        resultProductButton=navButton("BU HESABA GÖRE ÜRÜN BUL",Color.rgb(5,150,105));resultProductButton.setVisibility(View.GONE);resultProductButton.setOnClickListener(v->openProductFromResult());card.addView(resultProductButton,lp(-1,dp(54),dp(12)));
        moduleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){selectedModule=pos;updateFields(pos);}public void onNothingSelected(AdapterView<?> p){}});calc.setOnClickListener(v->calculate());return scroll;
    }
    private Button navButton(String label,int color){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(Color.WHITE);b.setBackgroundColor(color);return b;}
    private void updateFields(int module){String[] labels=CalculationEngine.LABELS[module];for(int i=0;i<inputs.length;i++){String label=labels[i];inputs[i].setHint(label);inputs[i].setVisibility(label.isEmpty()?View.GONE:View.VISIBLE);inputs[i].setText("");}resultTitle.setText("Sonuç");resultStatus.setText("");resultBody.setText("Değerleri girip hesaplayın.");resultNote.setText("");lastValues=null;if(resultProductButton!=null)resultProductButton.setVisibility(View.GONE);}
    private void calculate(){try{double[] v=new double[6];for(int i=0;i<inputs.length;i++){if(inputs[i].getVisibility()==View.GONE)continue;String s=inputs[i].getText().toString().trim().replace(',','.');if(s.isEmpty())throw new IllegalArgumentException("Eksik alan: "+CalculationEngine.LABELS[selectedModule][i]);v[i]=Double.parseDouble(s);}CalculationEngine.Result r=CalculationEngine.calculate(selectedModule,v);lastValues=v;resultTitle.setText(r.title);resultStatus.setText(r.status);resultStatus.setTextColor(r.status.contains("UYGUN DEĞİL")?Color.rgb(185,28,28):r.status.contains("SINIRDA")?Color.rgb(180,83,9):Color.rgb(15,118,110));resultBody.setText(r.body);resultNote.setText(r.note);if(selectedModule==15){resultProductButton.setText("BU HESABA GÖRE KAYIŞ BUL");resultProductButton.setVisibility(View.VISIBLE);}else if(selectedModule==16){resultProductButton.setText("BU HESABA GÖRE ZİNCİR BUL");resultProductButton.setVisibility(View.VISIBLE);}else resultProductButton.setVisibility(View.GONE);}catch(Exception e){Toast.makeText(this,e.getMessage()==null?"Girişleri kontrol edin.":e.getMessage(),Toast.LENGTH_LONG).show();}}
    private void openProductFromResult(){if(lastValues==null)return;Intent i=new Intent(this,ProductCatalogActivity.class);if(selectedModule==15){double P=lastValues[0],d=lastValues[1],rpm=lastValues[2],mu=lastValues[3],theta=Math.toRadians(lastValues[4]);double speed=Math.PI*(d/1000.0)*rpm/60.0;double delta=P*1000.0/speed;double ratio=Math.exp(mu*theta);double t2=delta/Math.max(1e-9,ratio-1.0);double t1=ratio*t2;i.putExtra(ProductCatalogActivity.EXTRA_TYPE,4);i.putExtra(ProductCatalogActivity.EXTRA_V0,String.valueOf(P));i.putExtra(ProductCatalogActivity.EXTRA_V1,String.valueOf(d));i.putExtra(ProductCatalogActivity.EXTRA_V2,String.valueOf(rpm));i.putExtra(ProductCatalogActivity.EXTRA_V3,String.valueOf(t1));}else if(selectedModule==16){double P=lastValues[0],rpm=lastValues[1],rmm=lastValues[2],ks=lastValues[3];double speed=2.0*Math.PI*(rmm/1000.0)*rpm/60.0;double pull=P*1000.0/speed;i.putExtra(ProductCatalogActivity.EXTRA_TYPE,5);i.putExtra(ProductCatalogActivity.EXTRA_V0,String.valueOf(P));i.putExtra(ProductCatalogActivity.EXTRA_V1,String.valueOf(rpm));i.putExtra(ProductCatalogActivity.EXTRA_V2,String.valueOf(pull));i.putExtra(ProductCatalogActivity.EXTRA_V3,String.valueOf(ks));}else return;startActivity(i);}
    private TextView text(String s,int sp,boolean bold,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(color);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private LinearLayout.LayoutParams lp(int w,int h,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.topMargin=top;return p;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
