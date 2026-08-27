package com.mg.machineelementspro;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class MachineElementStudioActivity extends Activity {
    private static final int[] MODULES={1,2,0,3,5,4,17,15};
    private static final String[] NAMES={"Mil (Shaft)","Rulman","Civata","Dişli Çark","Kama","Yay","Kaplin","Kayış-Kasnak"};
    private final EditText[] inputs=new EditText[6];
    private TextView title,status,result;
    private ElementView visual;
    private Button product;
    private int selected=0;
    private double[] lastValues;

    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(build());selectElement(0);}

    private View build(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(Color.rgb(244,247,250));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(16),dp(14),dp(28));sc.addView(root);
        root.addView(txt("MAKİNE ELEMANI SEÇ – GÖRSEL HESAP",23,true,Color.rgb(15,23,42)));
        TextView sub=txt("Boyut, kuvvet, tork ve devir girdileri seçilen elemanın teknik görseli üzerinde canlı gösterilir.",14,false,Color.rgb(71,85,105));sub.setPadding(0,dp(4),0,dp(12));root.addView(sub);

        GridLayout grid=new GridLayout(this);grid.setColumnCount(2);root.addView(grid,new LinearLayout.LayoutParams(-1,-2));
        for(int i=0;i<NAMES.length;i++){final int ix=i;Button b=new Button(this);b.setText(NAMES[i]);b.setAllCaps(false);b.setOnClickListener(v->selectElement(ix));GridLayout.LayoutParams gp=new GridLayout.LayoutParams();gp.width=0;gp.height=dp(54);gp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);gp.setMargins(dp(3),dp(3),dp(3),dp(3));grid.addView(b,gp);}

        title=txt("",20,true,Color.rgb(15,23,42));title.setPadding(0,dp(14),0,dp(6));root.addView(title);
        TextView hint=txt("GİRDİLER",12,true,Color.rgb(37,99,235));root.addView(hint);
        for(int i=0;i<6;i++){inputs[i]=new EditText(this);inputs[i].setSingleLine(true);inputs[i].setTextSize(15);inputs[i].setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);inputs[i].addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int b,int c){updateVisual();} public void afterTextChanged(Editable e){}});root.addView(inputs[i],lp(-1,dp(54),dp(5)));}

        TextView vh=txt("GÖRSEL / SERBEST CİSİM ŞEMASI",12,true,Color.rgb(37,99,235));vh.setPadding(0,dp(14),0,dp(4));root.addView(vh);
        visual=new ElementView();root.addView(visual,lp(-1,dp(300),0));

        Button calc=new Button(this);calc.setText("HESAPLA");calc.setTextColor(Color.WHITE);calc.setTextSize(16);calc.setAllCaps(false);calc.setBackgroundColor(Color.rgb(30,64,175));calc.setOnClickListener(v->calculate());root.addView(calc,lp(-1,dp(56),dp(12)));
        status=txt("",16,true,Color.rgb(15,118,110));status.setPadding(dp(10),dp(10),dp(10),0);root.addView(status);
        result=txt("Değerleri girin; eleman görseli girdilerle birlikte güncellenir.",15,false,Color.rgb(30,41,59));result.setLineSpacing(0,1.2f);result.setPadding(dp(10),dp(8),dp(10),dp(8));root.addView(result);
        product=new Button(this);product.setText("BU HESABA GÖRE GERÇEK ÜRÜN BUL");product.setAllCaps(false);product.setTextColor(Color.WHITE);product.setBackgroundColor(Color.rgb(5,150,105));product.setVisibility(View.GONE);product.setOnClickListener(v->openProduct());root.addView(product,lp(-1,dp(54),dp(8)));
        return sc;
    }

    private void selectElement(int ix){selected=ix;int m=MODULES[ix];title.setText(NAMES[ix]+" • "+CalculationEngine.MODULES[m]);String[] labels=CalculationEngine.LABELS[m];for(int i=0;i<6;i++){inputs[i].setHint(labels[i]);inputs[i].setText("");inputs[i].setVisibility(labels[i].isEmpty()?View.GONE:View.VISIBLE);}lastValues=null;status.setText("");result.setText("Değerleri girin; eleman görseli girdilerle birlikte güncellenir.");product.setVisibility(View.GONE);updateVisual();}

    private void calculate(){try{int m=MODULES[selected];double[] v=values(true);CalculationEngine.Result r=CalculationEngine.calculate(m,v);lastValues=v;status.setText(r.status);status.setTextColor(r.status.contains("UYGUN DEĞİL")||r.status.contains("KISA")?Color.rgb(185,28,28):r.status.contains("SINIRDA")||r.status.contains("ORTA")?Color.rgb(180,83,9):Color.rgb(15,118,110));result.setText(r.title+"\n\n"+r.body+"\n"+r.note);product.setVisibility(productType()>=0?View.VISIBLE:View.GONE);visual.invalidate();}catch(Exception e){Toast.makeText(this,e.getMessage()==null?"Girdileri kontrol edin":e.getMessage(),Toast.LENGTH_LONG).show();}}

    private double[] values(boolean strict){double[] v=new double[6];for(int i=0;i<6;i++){if(inputs[i].getVisibility()==View.GONE)continue;String s=inputs[i].getText().toString().trim().replace(',','.');if(s.isEmpty()){if(strict)throw new IllegalArgumentException("Eksik alan: "+inputs[i].getHint());v[i]=0;}else try{v[i]=Double.parseDouble(s);}catch(Exception e){if(strict)throw new IllegalArgumentException("Geçersiz sayı: "+inputs[i].getHint());}}return v;}
    private void updateVisual(){if(visual!=null){visual.data=values(false);visual.invalidate();}}

    private int productType(){int m=MODULES[selected];if(m==2)return 0;if(m==0)return 1;if(m==17)return 2;if(m==15)return 4;return -1;}
    private void openProduct(){if(lastValues==null)return;int type=productType();if(type<0)return;Intent i=new Intent(this,ProductCatalogActivity.class);i.putExtra(ProductCatalogActivity.EXTRA_TYPE,type);if(type==0){i.putExtra(ProductCatalogActivity.EXTRA_V0,String.valueOf(guessBearingBore(lastValues[0])));i.putExtra(ProductCatalogActivity.EXTRA_V1,String.valueOf(lastValues[0]/1000.0));}else if(type==1){i.putExtra(ProductCatalogActivity.EXTRA_V0,String.valueOf(lastValues[2]));}else if(type==2){i.putExtra(ProductCatalogActivity.EXTRA_V0,String.valueOf(lastValues[0]));i.putExtra(ProductCatalogActivity.EXTRA_V1,"25");i.putExtra(ProductCatalogActivity.EXTRA_V2,"1500");}else if(type==4){i.putExtra(ProductCatalogActivity.EXTRA_V0,String.valueOf(lastValues[0]));i.putExtra(ProductCatalogActivity.EXTRA_V1,String.valueOf(lastValues[1]));i.putExtra(ProductCatalogActivity.EXTRA_V2,String.valueOf(lastValues[2]));}startActivity(i);}
    private double guessBearingBore(double cN){return cN>80000?50:cN>50000?40:cN>30000?35:25;}

    private TextView txt(String s,int sp,boolean bold,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);return t;}
    private LinearLayout.LayoutParams lp(int w,int h,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.topMargin=top;return p;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}

    private final class ElementView extends View {
        double[] data=new double[6];Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);Paint text=new Paint(Paint.ANTI_ALIAS_FLAG);
        ElementView(){super(MachineElementStudioActivity.this);setBackgroundColor(Color.rgb(20,27,35));text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();drawGrid(c,w,h);int m=MODULES[selected];if(m==1)shaft(c,w,h);else if(m==2)bearing(c,w,h);else if(m==0)bolt(c,w,h);else if(m==3)gear(c,w,h);else if(m==5)key(c,w,h);else if(m==4)spring(c,w,h);else if(m==17)coupling(c,w,h);else belt(c,w,h);}
        void drawGrid(Canvas c,float w,float h){p.setStrokeWidth(1);p.setColor(Color.rgb(35,46,58));for(int x=0;x<w;x+=dp(24))c.drawLine(x,0,x,h,p);for(int y=0;y<h;y+=dp(24))c.drawLine(0,y,w,y,p);}
        void line(Canvas c,int color,float sw,float x1,float y1,float x2,float y2){p.setColor(color);p.setStrokeWidth(sw);p.setStyle(Paint.Style.STROKE);c.drawLine(x1,y1,x2,y2,p);p.setStyle(Paint.Style.FILL);}
        void label(Canvas c,String s,float x,float y,int color){text.setTextSize(dp(12));text.setColor(color);c.drawText(s,x,y,text);}
        void arrow(Canvas c,float x1,float y1,float x2,float y2,int color,String lab){line(c,color,dp(3),x1,y1,x2,y2);double a=Math.atan2(y2-y1,x2-x1);float L=dp(11);Path q=new Path();q.moveTo(x2,y2);q.lineTo((float)(x2-L*Math.cos(a-.55)),(float)(y2-L*Math.sin(a-.55)));q.lineTo((float)(x2-L*Math.cos(a+.55)),(float)(y2-L*Math.sin(a+.55)));q.close();p.setColor(color);c.drawPath(q,p);label(c,lab,x2+dp(5),y2,color);}
        void shaft(Canvas c,float w,float h){float y=h*.5f,x1=w*.14f,x2=w*.86f;line(c,Color.LTGRAY,dp(18),x1,y,x2,y);p.setColor(Color.rgb(100,116,139));c.drawRect(x1-dp(12),y-dp(35),x1+dp(12),y+dp(35),p);c.drawRect(x2-dp(12),y-dp(35),x2+dp(12),y+dp(35),p);arrow(c,w*.52f,y-dp(75),w*.52f,y-dp(20),Color.rgb(239,68,68),"M="+fmt(data[0])+" N·m");arrow(c,w*.35f,y+dp(70),w*.35f,y+dp(22),Color.rgb(34,197,94),"T="+fmt(data[1])+" N·m");label(c,"d = "+fmt(data[2])+" mm",w*.43f,y+dp(60),Color.WHITE);label(c,"Sy = "+fmt(data[3])+" MPa",dp(12),dp(22),Color.rgb(147,197,253));}
        void bearing(Canvas c,float w,float h){float cx=w*.5f,cy=h*.5f,r=Math.min(w,h)*.25f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(16));p.setColor(Color.LTGRAY);c.drawCircle(cx,cy,r,p);p.setStrokeWidth(dp(5));p.setColor(Color.rgb(100,116,139));c.drawCircle(cx,cy,r*.52f,p);p.setStyle(Paint.Style.FILL);for(int k=0;k<10;k++){double a=k*Math.PI*2/10;p.setColor(Color.rgb(203,213,225));c.drawCircle((float)(cx+Math.cos(a)*r*.75),(float)(cy+Math.sin(a)*r*.75),dp(8),p);}arrow(c,cx,dp(18),cx,cy-r-dp(5),Color.rgb(239,68,68),"P="+fmt(data[1])+" N");label(c,"C="+fmt(data[0])+" N",dp(12),dp(22),Color.rgb(147,197,253));label(c,"n="+fmt(data[2])+" rpm",dp(12),h-dp(12),Color.WHITE);}
        void bolt(Canvas c,float w,float h){float y=h*.5f,x1=w*.2f,x2=w*.8f;line(c,Color.LTGRAY,dp(20),x1,y,x2,y);p.setColor(Color.rgb(148,163,184));c.drawRect(x1-dp(28),y-dp(32),x1,y+dp(32),p);for(int k=0;k<6;k++)line(c,Color.rgb(100,116,139),dp(2),x2-dp(45)+k*dp(9),y-dp(12),x2-dp(36)+k*dp(9),y+dp(12));arrow(c,x2+dp(20),y,x2+dp(80),y,Color.rgb(239,68,68),"F="+fmt(data[0])+" N");arrow(c,w*.5f,y+dp(80),w*.5f,y+dp(20),Color.rgb(59,130,246),"V="+fmt(data[1])+" N");label(c,"d="+fmt(data[2])+" mm",dp(12),dp(22),Color.WHITE);}
        void gear(Canvas c,float w,float h){float cx=w*.48f,cy=h*.52f,r=Math.min(w,h)*.26f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(8));p.setColor(Color.LTGRAY);c.drawCircle(cx,cy,r,p);for(int k=0;k<18;k++){double a=k*Math.PI*2/18;float a1x=(float)(cx+Math.cos(a)*r),a1y=(float)(cy+Math.sin(a)*r),a2x=(float)(cx+Math.cos(a)*(r+dp(14))),a2y=(float)(cy+Math.sin(a)*(r+dp(14)));line(c,Color.LTGRAY,dp(7),a1x,a1y,a2x,a2y);}p.setStyle(Paint.Style.FILL);arrow(c,cx+r+dp(45),cy,cx+r+dp(5),cy,Color.rgb(239,68,68),"Ft="+fmt(data[0])+" N");label(c,"b="+fmt(data[1])+" mm • m="+fmt(data[2])+" mm",dp(12),dp(22),Color.WHITE);}
        void key(Canvas c,float w,float h){float y=h*.55f;line(c,Color.LTGRAY,dp(30),w*.15f,y,w*.85f,y);p.setColor(Color.rgb(234,179,8));c.drawRect(w*.42f,y-dp(27),w*.62f,y-dp(12),p);arrow(c,w*.52f,y+dp(80),w*.52f,y+dp(25),Color.rgb(34,197,94),"T="+fmt(data[0])+" N·m");label(c,"Mil d="+fmt(data[1])+" • kama "+fmt(data[2])+"x"+fmt(data[3])+"x"+fmt(data[4])+" mm",dp(12),dp(22),Color.WHITE);}
        void spring(Canvas c,float w,float h){float x=w*.5f,y0=dp(45),y1=h-dp(45);Path q=new Path();q.moveTo(x,y0);int turns=8;for(int i=1;i<=turns*16;i++){float yy=y0+(y1-y0)*i/(turns*16f);float xx=(float)(x+Math.sin(i*Math.PI/8)*w*.18f);q.lineTo(xx,yy);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(7));p.setColor(Color.LTGRAY);c.drawPath(q,p);p.setStyle(Paint.Style.FILL);arrow(c,x,y0-dp(8),x,y0+dp(35),Color.rgb(239,68,68),"F="+fmt(data[0])+" N");label(c,"D="+fmt(data[1])+" mm • tel d="+fmt(data[2])+" mm",dp(12),dp(22),Color.WHITE);}
        void coupling(Canvas c,float w,float h){float y=h*.52f;line(c,Color.LTGRAY,dp(18),w*.12f,y,w*.88f,y);p.setColor(Color.rgb(100,116,139));c.drawCircle(w*.43f,y,dp(48),p);c.drawCircle(w*.57f,y,dp(48),p);arrow(c,w*.26f,y+dp(75),w*.26f,y+dp(22),Color.rgb(34,197,94),"T="+fmt(data[0])+" N·m");label(c,"n bolt="+fmt(data[1])+" • PCD="+fmt(data[2])+" mm • d="+fmt(data[3])+" mm",dp(12),dp(22),Color.WHITE);}
        void belt(Canvas c,float w,float h){float cy=h*.55f,r1=dp(54),r2=dp(32),x1=w*.3f,x2=w*.72f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(8));p.setColor(Color.LTGRAY);c.drawCircle(x1,cy,r1,p);c.drawCircle(x2,cy,r2,p);line(c,Color.rgb(148,163,184),dp(8),x1,cy-r1,x2,cy-r2);line(c,Color.rgb(148,163,184),dp(8),x1,cy+r1,x2,cy+r2);p.setStyle(Paint.Style.FILL);arrow(c,x1,cy-r1-dp(65),x1,cy-r1-dp(8),Color.rgb(34,197,94),"P="+fmt(data[0])+" kW");label(c,"d="+fmt(data[1])+" mm • n="+fmt(data[2])+" rpm",dp(12),dp(22),Color.WHITE);}
        String fmt(double x){return Math.abs(x)>=1000?String.format(Locale.US,"%.0f",x):String.format(Locale.US,"%.2f",x);}
    }
}
