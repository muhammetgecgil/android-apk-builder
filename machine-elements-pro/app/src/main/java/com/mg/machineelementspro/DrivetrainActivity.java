package com.mg.machineelementspro;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class DrivetrainActivity extends Activity {
    private final EditText[] e=new EditText[12];
    private TextView result,status,note;
    private Button bearingProducts,couplingProducts;
    private DrivetrainEngine.Input lastInput;
    private DrivetrainEngine.Result lastResult;
    private static final String[] L={"Tork (N·m)","Devir (rpm)","Dişli hatve çapı (mm)","Basınç açısı (deg)","Helis açısı (deg)","Rulman açıklığı (mm)","Dişli konumu A'dan (mm)","Mil çapı (mm)","Mil akma Sy (MPa)","Rulman C1 (N)","Rulman C2 (N)","Rulman üs p (3 / 3.333)"};
    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(ui());}
    private LinearLayout.LayoutParams lp(int w,int h,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.topMargin=top;return p;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView t(String s,int sp,boolean bold,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private ScrollView ui(){
        ScrollView s=new ScrollView(this);s.setBackgroundColor(Color.rgb(248,250,252));
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(18),dp(20),dp(18),dp(32));s.addView(r);
        r.addView(t("DRIVETRAIN SYSTEM",24,true,Color.rgb(15,23,42)));
        TextView sub=t("Dişli → mil → iki rulman bağlı çözüm → gerçek ürün adayı",14,false,Color.rgb(71,85,105));sub.setPadding(0,dp(4),0,dp(10));r.addView(sub);
        Button back=new Button(this);back.setText("← Advanced Engineering");back.setAllCaps(false);back.setOnClickListener(v->finish());r.addView(back,lp(-1,dp(48),0));
        for(int i=0;i<e.length;i++){e[i]=new EditText(this);e[i].setHint(L[i]);e[i].setTextSize(15);e[i].setSingleLine(true);e[i].setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);r.addView(e[i],lp(-1,dp(54),dp(5)));}
        Button calc=new Button(this);calc.setText("SİSTEMİ ÇÖZ");calc.setAllCaps(false);calc.setTypeface(Typeface.DEFAULT,Typeface.BOLD);calc.setTextColor(Color.WHITE);calc.setBackgroundColor(Color.rgb(109,40,217));r.addView(calc,lp(-1,dp(56),dp(14)));
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(16),dp(16),dp(16));card.setBackgroundColor(Color.WHITE);r.addView(card,lp(-1,-2,dp(14)));
        status=t("",15,true,Color.rgb(109,40,217));result=t("Girdileri tamamlayın.",15,false,Color.rgb(30,41,59));result.setLineSpacing(0,1.2f);note=t("",12,false,Color.rgb(100,116,139));note.setPadding(0,dp(10),0,0);card.addView(status);card.addView(result);card.addView(note);
        bearingProducts=new Button(this);bearingProducts.setText("BU HESABA GÖRE RULMAN BUL");bearingProducts.setAllCaps(false);bearingProducts.setVisibility(View.GONE);r.addView(bearingProducts,lp(-1,dp(54),dp(10)));
        couplingProducts=new Button(this);couplingProducts.setText("BU HESABA GÖRE KAPLİN BUL");couplingProducts.setAllCaps(false);couplingProducts.setVisibility(View.GONE);r.addView(couplingProducts,lp(-1,dp(54),dp(8)));
        calc.setOnClickListener(v->solve());
        bearingProducts.setOnClickListener(v->openBearingProducts());
        couplingProducts.setOnClickListener(v->openCouplingProducts());
        return s;
    }
    private double v(int i){String s=e[i].getText().toString().trim().replace(',','.');if(s.isEmpty())throw new IllegalArgumentException("Eksik alan: "+L[i]);return Double.parseDouble(s);}
    private void solve(){try{
        DrivetrainEngine.Input x=new DrivetrainEngine.Input();x.torqueNm=v(0);x.rpm=v(1);x.pitchDiameterMm=v(2);x.pressureAngleDeg=v(3);x.helixAngleDeg=v(4);x.spanMm=v(5);x.gearPositionMm=v(6);x.shaftDiameterMm=v(7);x.shaftYieldMpa=v(8);x.bearingC1N=v(9);x.bearingC2N=v(10);x.bearingExponent=v(11);
        DrivetrainEngine.Result q=DrivetrainEngine.calculate(x);lastInput=x;lastResult=q;
        status.setText(q.status);status.setTextColor(q.status.contains("UYGUN DEĞİL")?Color.rgb(185,28,28):q.status.contains("SINIRDA")?Color.rgb(180,83,9):Color.rgb(109,40,217));result.setText(q.body);note.setText(q.note);
        bearingProducts.setVisibility(View.VISIBLE);couplingProducts.setVisibility(View.VISIBLE);
    }catch(Exception ex){bearingProducts.setVisibility(View.GONE);couplingProducts.setVisibility(View.GONE);Toast.makeText(this,ex.getMessage()==null?"Girdileri kontrol edin.":ex.getMessage(),Toast.LENGTH_LONG).show();}}
    private void openBearingProducts(){if(lastInput==null||lastResult==null)return;Intent i=new Intent(this,ProductCatalogActivity.class);i.putExtra("type",0);i.putExtra("v0",lastInput.shaftDiameterMm);i.putExtra("v1",Math.max(lastInput.bearingC1N,lastInput.bearingC2N));startActivity(i);}
    private void openCouplingProducts(){if(lastInput==null)return;Intent i=new Intent(this,ProductCatalogActivity.class);i.putExtra("type",2);i.putExtra("v0",lastInput.torqueNm);i.putExtra("v1",lastInput.shaftDiameterMm);startActivity(i);}
}
