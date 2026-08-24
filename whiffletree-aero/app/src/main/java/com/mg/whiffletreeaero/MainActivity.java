package com.mg.whiffletreeaero;

import android.app.*;
import android.os.Bundle;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import java.util.*;

public class MainActivity extends Activity {
    private final int NAVY=Color.rgb(10,25,42), BLUE=Color.rgb(30,105,210), ORANGE=Color.rgb(235,155,30), GREEN=Color.rgb(48,170,85), RED=Color.rgb(220,70,70), PANEL=Color.rgb(20,39,58);
    private LinearLayout root, wizardBox, presetBox;
    private TankRigView rig;
    private TextView stepTitle, stepHelp, result, status;
    private Button prevBtn,nextBtn,calcBtn;
    private EditText tankLength, tankDiameter, totalLoad, targetMoment, pressureBar, safety, stationCount, layerCount, actuatorCount, padWidth, padLength, strokeMargin;
    private Spinner loadCase;
    private int step=0;
    private String activePreset="+Z Bending";

    private static final String[] CASES={"+Z Bending","-Z Bending","+Y Bending","-Y Bending","Pitch +My","Pitch -My","Roll +Mx","Roll -Mx","Yaw +Mz","Yaw -Mz","Combined Z + Pitch"};
    private static final String[] STEP_T={
        "1/7  Problemi seç","2/7  Tank geometrisini tanımla","3/7  Kesit (station) sayısını seç","4/7  Whiffletree katmanını seç","5/7  Aktüatör sayısını seç","6/7  Parça varsayımlarını kontrol et","7/7  Sonuçları doğrula"
    };
    private static final String[] STEP_H={
        "Önce hangi yük durumunu laboratuvarda üretmek istediğini seç. Bilmiyorsan +Z Bending varsayılanını kullan.",
        "Tank boyu ve çapı load station geometrisini ve moment kollarını etkiler. Varsayılanlar eğitim amaçlıdır.",
        "Daha fazla station, dağıtılmış yükü daha iyi temsil eder; ancak rod, pad ve beam sayısını artırır.",
        "1 katman basit, 2-3 katman daha az aktüatörle daha çok yük noktasını sürer. 3 katman EFT için iyi bir eğitim varsayılanıdır.",
        "Aktüatör sayısı toplam yük / kapasite hesabından daha fazlasıdır; moment kontrol bölgeleri, stroke ve rig geometrisi de önemlidir.",
        "Pad ölçüsü, hidrolik basınç, emniyet katsayısı ve stroke marjı ön tasarımın parça seçim girdileridir.",
        "ΣF ve ΣM hedeflerini, actuator yüklerini, piston çapını ve station kuvvetlerini kontrol et. Uygun değilse önceki adıma dön."
    };

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        ScrollView sc=new ScrollView(this);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(10),dp(10),dp(10),dp(30)); root.setBackgroundColor(NAVY); sc.addView(root);
        TextView title=tx("EFT WHIFFLETREE TASARIM & HESAP",25,true,Color.WHITE); root.addView(title);
        TextView sub=tx("Harici Yakıt Tankı • Statik Yük Testi • Görsel Sihirbaz",13,false,Color.rgb(180,200,220)); sub.setPadding(0,0,0,dp(10)); root.addView(sub);

        presetBox=new LinearLayout(this); presetBox.setOrientation(LinearLayout.HORIZONTAL);
        HorizontalScrollView hs=new HorizontalScrollView(this); hs.addView(presetBox); root.addView(hs,lp());
        for(String c:CASES){ Button btt=smallButton(c); btt.setOnClickListener(v->{ activePreset=((Button)v).getText().toString(); applyPreset(activePreset); }); presetBox.addView(btt,new LinearLayout.LayoutParams(dp(120),dp(44))); }

        status=card("Hazır • Varsayılan: +Z Bending",Color.rgb(14,45,70)); root.addView(status,lp());
        rig=new TankRigView(); root.addView(rig,new LinearLayout.LayoutParams(-1,dp(420)));

        wizardBox=new LinearLayout(this); wizardBox.setOrientation(LinearLayout.VERTICAL); wizardBox.setPadding(dp(10),dp(10),dp(10),dp(10)); wizardBox.setBackground(bg(Color.rgb(14,31,48),dp(10)));
        root.addView(wizardBox,lp());
        stepTitle=tx("",18,true,Color.WHITE); wizardBox.addView(stepTitle);
        stepHelp=tx("",13,false,Color.rgb(190,210,228)); stepHelp.setPadding(0,dp(4),0,dp(8)); wizardBox.addView(stepHelp);

        loadCase=new Spinner(this); loadCase.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,CASES)); wizardBox.addView(loadCase,lp());
        loadCase.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){ public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){activePreset=CASES[pos]; rig.invalidate();} public void onNothingSelected(android.widget.AdapterView<?> p){} });

        tankLength=field("Tank uzunluğu L [m]","7.6"); tankDiameter=field("Tank çapı D [m]","0.64");
        stationCount=field("Station sayısı","12"); layerCount=field("Whiffletree katman sayısı","3"); actuatorCount=field("Aktüatör sayısı","4");
        totalLoad=field("Hedef toplam yük |F| [kN]","305.7"); targetMoment=field("Hedef moment [kN·m]","1280");
        pressureBar=field("Hidrolik basınç [bar]","210"); safety=field("Boyutlandırma katsayısı","1.50");
        padWidth=field("Load pad genişliği [mm]","150"); padLength=field("Load pad uzunluğu [mm]","250"); strokeMargin=field("Stroke marjı [%]","25");

        LinearLayout nav=new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL);
        prevBtn=new Button(this); prevBtn.setText("← Geri"); nextBtn=new Button(this); nextBtn.setText("Devam →"); calcBtn=new Button(this); calcBtn.setText("HESAPLA");
        nav.addView(prevBtn,new LinearLayout.LayoutParams(0,dp(48),1)); nav.addView(nextBtn,new LinearLayout.LayoutParams(0,dp(48),1)); wizardBox.addView(nav,lp()); wizardBox.addView(calcBtn,lp());
        prevBtn.setOnClickListener(v->{if(step>0){step--;renderStep();}}); nextBtn.setOnClickListener(v->{if(step<6){step++;renderStep();} else calculate();}); calcBtn.setOnClickListener(v->calculate());

        result=card("Sorular ilerledikçe tank ve whiffletree görseli otomatik güncellenir.",Color.rgb(12,35,54)); result.setTextSize(14); root.addView(result,lp());
        root.addView(card("Not: Bu uygulama eğitim ve ön tasarım aracıdır. Nihai donanım seçimi için onaylı load report, FEM, üretici katalog limitleri ve ilgili test/airworthiness gereksinimleri kullanılmalıdır.",Color.rgb(65,45,12)),lp());

        applyPreset("+Z Bending"); renderStep(); setContentView(sc);
    }

    private EditText field(String label,String def){
        TextView l=tx(label,12,true,Color.rgb(180,205,225)); l.setPadding(0,dp(6),0,dp(2)); wizardBox.addView(l);
        EditText e=new EditText(this); e.setText(def); e.setTextColor(Color.WHITE); e.setHintTextColor(Color.GRAY); e.setTextSize(16); e.setSingleLine(true); e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED); e.setBackground(bg(Color.rgb(25,50,72),dp(8))); e.setPadding(dp(10),0,dp(10),0); wizardBox.addView(e,new LinearLayout.LayoutParams(-1,dp(44)));
        e.setOnFocusChangeListener((v,has)->{if(!has){safeCalcPreview();}}); return e;
    }

    private void renderStep(){
        stepTitle.setText(STEP_T[step]); stepHelp.setText(STEP_H[step]);
        loadCase.setVisibility(step==0?View.VISIBLE:View.GONE);
        setVis(tankLength,step==1); setVis(tankDiameter,step==1); setLabelVis(tankLength,step==1); setLabelVis(tankDiameter,step==1);
        setVis(stationCount,step==2); setLabelVis(stationCount,step==2);
        setVis(layerCount,step==3); setLabelVis(layerCount,step==3);
        setVis(actuatorCount,step==4); setLabelVis(actuatorCount,step==4);
        boolean hardware=step==5;
        for(EditText e:new EditText[]{totalLoad,targetMoment,pressureBar,safety,padWidth,padLength,strokeMargin}){setVis(e,hardware);setLabelVis(e,hardware);} 
        calcBtn.setVisibility(step==6?View.VISIBLE:View.GONE); prevBtn.setEnabled(step>0); nextBtn.setText(step==6?"Hesapla":"Devam →");
        safeCalcPreview();
    }
    private void setVis(View v,boolean yes){v.setVisibility(yes?View.VISIBLE:View.GONE);}    
    private void setLabelVis(EditText e,boolean yes){ int idx=wizardBox.indexOfChild(e); if(idx>0) wizardBox.getChildAt(idx-1).setVisibility(yes?View.VISIBLE:View.GONE); }

    private void applyPreset(String p){
        int pos=Arrays.asList(CASES).indexOf(p); if(pos>=0) loadCase.setSelection(pos);
        if(p.contains("+Z")){totalLoad.setText("305.7");targetMoment.setText("1280");}
        else if(p.contains("-Z")){totalLoad.setText("260");targetMoment.setText("-1080");}
        else if(p.contains("+Y")){totalLoad.setText("90");targetMoment.setText("360");}
        else if(p.contains("-Y")){totalLoad.setText("90");targetMoment.setText("-360");}
        else if(p.contains("Pitch")){totalLoad.setText("120");targetMoment.setText(p.contains("-")?"-150":"150");}
        else if(p.contains("Roll")){totalLoad.setText("40");targetMoment.setText(p.contains("-")?"-18":"18");}
        else if(p.contains("Yaw")){totalLoad.setText("50");targetMoment.setText(p.contains("-")?"-90":"90");}
        else {totalLoad.setText("220");targetMoment.setText("820");}
        status.setText("Preset yüklendi: "+p+" • Tüm alanlar düzenlenebilir."); safeCalcPreview();
    }

    private void safeCalcPreview(){ try{ Calculation c=compute(); rig.set(c); status.setText(String.format(Locale.US,"%s • ΣF %.1f kN • Hesap moment %.1f kN·m • %d station • %d actuator",activePreset,c.sumF,c.calcMoment,c.nStation,c.nAct)); }catch(Exception ignored){} }

    private void calculate(){
        try{
            Calculation c=compute(); rig.set(c);
            StringBuilder sb=new StringBuilder();
            sb.append("HESAP ÖZETİ\n");
            sb.append(String.format(Locale.US,"Yük durumu: %s\nStation: %d   Katman: %d   Aktüatör: %d\n\n",activePreset,c.nStation,c.layers,c.nAct));
            sb.append(String.format(Locale.US,"ΣF hedef / hesap: %.1f / %.1f kN\n",c.targetF,c.sumF));
            sb.append(String.format(Locale.US,"M hedef / hesap: %.1f / %.1f kN·m\nMoment hatası: %.2f %%\n\n",c.targetM,c.calcMoment,c.mErr));
            sb.append(String.format(Locale.US,"Boyutlandırma yükü: %.1f kN\nAktüatör başına tasarım yükü: %.1f kN\nTeorik piston çapı @ %.0f bar: %.1f mm\n",c.designF,c.actLoad,c.pbar,c.bore));
            sb.append(String.format(Locale.US,"Önerilen load-cell sınıfı (ön seçim): ≥ %.0f kN\nPad alanı: %.0f × %.0f mm\nOrtalama nominal pad basıncı: %.2f MPa\n\n",c.lcClass,c.padW,c.padL,c.padPressure));
            sb.append("STATION KUVVETLERİ\n"); for(int i=0;i<c.nStation;i++) sb.append(String.format(Locale.US,"S%d  x=%.2f m   F=%.1f kN\n",i+1,c.x[i],c.f[i]));
            sb.append("\nWHIFFLETREE\n"); sb.append(c.treeSummary);
            sb.append("\n\nKONTROL\n"); sb.append(Math.abs(c.mErr)<=1.0?"✓ Moment eşleşmesi eğitim toleransı içinde.\n":"! Moment eşleşmesi %1 dışında; station dağılımını optimize et.\n");
            sb.append(c.padPressure<8?"✓ Pad ortalama basıncı eğitim varsayımında düşük/orta.\n":"! Pad ortalama basıncı yüksek; gerçek skin/bulkhead FEM kontrolü gerekir.\n");
            result.setText(sb.toString()); step=6; renderStep();
        }catch(Exception e){result.setText("Girişleri kontrol et. Sayısal alanlar boş veya geçersiz olamaz.");}
    }

    private Calculation compute(){
        Calculation c=new Calculation();
        c.L=d(tankLength); c.D=d(tankDiameter); c.nStation=clampi((int)Math.round(d(stationCount)),4,16); c.layers=clampi((int)Math.round(d(layerCount)),1,3); c.nAct=clampi((int)Math.round(d(actuatorCount)),1,8);
        c.targetF=Math.abs(d(totalLoad)); c.targetM=d(targetMoment); c.pbar=Math.max(1,d(pressureBar)); c.sf=Math.max(1,d(safety)); c.padW=Math.max(20,d(padWidth)); c.padL=Math.max(20,d(padLength)); c.strokeMargin=Math.max(0,d(strokeMargin));
        c.x=new double[c.nStation]; c.f=new double[c.nStation]; double sumW=0;
        for(int i=0;i<c.nStation;i++){ double u=(i+.5)/c.nStation; c.x[i]=u*c.L; double w=Math.pow(Math.sin(Math.PI*u),0.72)+0.22; c.f[i]=w; sumW+=w; }
        for(int i=0;i<c.nStation;i++) c.f[i]=c.targetF*c.f[i]/sumW;
        double baseM=0; for(int i=0;i<c.nStation;i++) baseM+=c.f[i]*c.x[i];
        double desired=Math.abs(c.targetM); if(desired>1e-6){ double delta=desired-baseM; double xa=c.x[0], xb=c.x[c.nStation-1]; double move=delta/(xb-xa); c.f[c.nStation-1]+=move; c.f[0]-=move; if(c.f[0]<0||c.f[c.nStation-1]<0){ // fallback scale about center
                for(int i=0;i<c.nStation;i++) c.f[i]=c.targetF/c.nStation;
            }
        }
        c.sumF=0;c.calcMoment=0; for(int i=0;i<c.nStation;i++){ c.f[i]=Math.max(0,c.f[i]); c.sumF+=c.f[i]; c.calcMoment+=c.f[i]*c.x[i]; }
        if(c.sumF>0){ double s=c.targetF/c.sumF; c.sumF=0;c.calcMoment=0; for(int i=0;i<c.nStation;i++){c.f[i]*=s;c.sumF+=c.f[i];c.calcMoment+=c.f[i]*c.x[i];}}
        if(c.targetM<0)c.calcMoment*=-1;
        c.mErr=Math.abs(c.targetM)>1e-6?100*(c.calcMoment-c.targetM)/Math.abs(c.targetM):0;
        c.designF=c.targetF*c.sf; c.actLoad=c.designF/c.nAct; double area=c.actLoad*1000/(c.pbar*100000); c.bore=Math.sqrt(4*area/Math.PI)*1000;
        double[] std={10,20,25,50,75,100,150,200,250,300,500,750,1000}; c.lcClass=std[std.length-1]; for(double v:std){if(v>=c.actLoad*1.15){c.lcClass=v;break;}}
        c.padPressure=(c.targetF/c.nStation*1000)/((c.padW/1000)*(c.padL/1000))/1e6;
        StringBuilder ts=new StringBuilder(); int groups=(int)Math.ceil(c.nStation/2.0); ts.append("1. kademe: ").append(groups).append(" beam grubu. "); if(c.layers>=2)ts.append("2. kademe: yaklaşık ").append((int)Math.ceil(groups/2.0)).append(" üst grup. "); if(c.layers>=3)ts.append("3. kademe: actuator dağıtım seviyesi. "); ts.append("Her ikili beam için pivot oranı Fsol·Lsol = Fsağ·Lsağ ile hesaplanır."); c.treeSummary=ts.toString(); return c;
    }

    private class Calculation{ double L,D,targetF,targetM,pbar,sf,padW,padL,strokeMargin,sumF,calcMoment,mErr,designF,actLoad,bore,lcClass,padPressure; int nStation,layers,nAct; double[] x,f; String treeSummary; }

    private class TankRigView extends View{
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG); Calculation c;
        TankRigView(){super(MainActivity.this);setBackground(bg(Color.rgb(7,22,36),dp(10)));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
        void set(Calculation cc){c=cc;invalidate();}
        @Override protected void onDraw(Canvas cv){super.onDraw(cv); int w=getWidth(),h=getHeight(); if(w<=0||h<=0)return;
            p.setStrokeWidth(dp(2)); p.setStyle(Paint.Style.FILL); t.setTextSize(dp(11)); t.setColor(Color.WHITE);
            float left=w*.08f,right=w*.92f, cy=h*.27f, th=h*.16f;
            // tank shadow and body
            p.setColor(Color.rgb(30,45,58)); cv.drawOval(left+dp(5),cy-th/2+dp(7),right+dp(5),cy+th/2+dp(7),p);
            LinearGradient g=new LinearGradient(left,cy-th/2,right,cy+th/2,new int[]{Color.rgb(95,105,112),Color.rgb(210,215,217),Color.rgb(90,100,108)},null,Shader.TileMode.CLAMP); p.setShader(g); cv.drawOval(left,cy-th/2,right,cy+th/2,p); p.setShader(null); p.setStyle(Paint.Style.STROKE); p.setColor(Color.rgb(120,150,160)); cv.drawOval(left,cy-th/2,right,cy+th/2,p);
            Calculation x=c; if(x==null)return; int n=x.nStation;
            float beamY=h*.50f, beam2=h*.63f, actY=h*.82f;
            // stations and loads
            for(int i=0;i<n;i++){ float sx=left+(right-left)*(float)(x.x[i]/x.L); p.setStrokeWidth(dp(1));p.setColor(Color.rgb(55,170,120)); cv.drawLine(sx,cy-th*.55f,sx,cy+th*.55f,p); t.setTextSize(dp(9));cv.drawText("S"+(i+1),sx-dp(7),cy-th*.78f,t); String fv=String.format(Locale.US,"%.1f",x.f[i]);cv.drawText(fv,sx-dp(10),cy-th*.98f,t); p.setColor(BLUE);p.setStrokeWidth(dp(3));cv.drawLine(sx,cy-th*.92f,sx,cy-th*.58f,p); cv.drawLine(sx,cy-th*.58f,sx-dp(4),cy-th*.66f,p);cv.drawLine(sx,cy-th*.58f,sx+dp(4),cy-th*.66f,p);
                // link to first beam
                p.setColor(Color.LTGRAY);p.setStrokeWidth(dp(2));cv.drawLine(sx,cy+th*.55f,sx,beamY,p); p.setStyle(Paint.Style.FILL);p.setColor(GREEN);cv.drawRect(sx-dp(5),cy+th*.48f,sx+dp(5),cy+th*.60f,p);
            }
            // stage 1 beams pairwise
            p.setStrokeWidth(dp(5));p.setColor(ORANGE); for(int i=0;i<n;i+=2){float a=left+(right-left)*(float)(x.x[i]/x.L), b=(i+1<n)?left+(right-left)*(float)(x.x[i+1]/x.L):a; if(a==b){a-=dp(10);b+=dp(10);} cv.drawLine(a,beamY,b,beamY,p);}
            t.setTextSize(dp(10));t.setColor(Color.rgb(250,190,70));cv.drawText("1. Kademe",dp(10),beamY,t);
            if(x.layers>=2){p.setColor(Color.rgb(220,125,30)); for(int i=0;i<n;i+=4){float a=left+(right-left)*(float)(x.x[i]/x.L), b=left+(right-left)*(float)(x.x[Math.min(n-1,i+3)]/x.L); cv.drawLine(a,beam2,b,beam2,p); cv.drawLine((a+b)/2,beamY,(a+b)/2,beam2,p);} cv.drawText("2. Kademe",dp(10),beam2,t);} 
            float baseY=h*.93f; p.setColor(Color.rgb(70,85,95));p.setStyle(Paint.Style.FILL);cv.drawRect(w*.18f,baseY,w*.82f,baseY+dp(18),p);
            for(int a=0;a<x.nAct;a++){float ax=w*.25f+(x.nAct==1?0:a*(w*.5f/(x.nAct-1)));float top=x.layers>=2?beam2:beamY;p.setColor(Color.LTGRAY);p.setStrokeWidth(dp(2));cv.drawLine(ax,top,ax,actY,p);p.setColor(GREEN);cv.drawRect(ax-dp(9),actY-dp(18),ax+dp(9),actY+dp(6),p);p.setColor(Color.GRAY);cv.drawRect(ax-dp(7),actY+dp(6),ax+dp(7),baseY,p);t.setColor(Color.WHITE);t.setTextSize(dp(9));cv.drawText("ACT-"+(a+1),ax-dp(17),baseY-dp(4),t);} 
            t.setTextSize(dp(11));t.setColor(Color.WHITE);cv.drawText(String.format(Locale.US,"ΣF %.1f kN   M %.1f kN·m",x.sumF,x.calcMoment),dp(12),h-dp(8),t);
        }
    }

    private double d(EditText e){return Double.parseDouble(e.getText().toString().trim().replace(',','.'));}
    private int clampi(int v,int a,int b){return Math.max(a,Math.min(b,v));}
    private TextView tx(String s,int sz,boolean bold,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(sz);v.setTextColor(color);if(bold)v.setTypeface(Typeface.DEFAULT_BOLD);return v;}
    private TextView card(String s,int color){TextView v=tx(s,13,false,Color.WHITE);v.setPadding(dp(12),dp(10),dp(12),dp(10));v.setBackground(bg(color,dp(8)));return v;}
    private Button smallButton(String s){Button b=new Button(this);b.setText(s);b.setTextSize(11);b.setAllCaps(false);return b;}
    private GradientDrawable bg(int color,int r){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(r);g.setStroke(dp(1),Color.rgb(45,75,98));return g;}
    private LinearLayout.LayoutParams lp(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(5),0,dp(5));return p;}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
}
