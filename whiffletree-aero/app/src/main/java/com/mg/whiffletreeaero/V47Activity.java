package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V47Activity extends V46Activity {
  final String[] wizardSteps={"1. TANK GEOMETRY","2. LOAD CASE","3. STATIONS","4. ACTUATOR LIMIT","5. LAYER LIMIT","6. PAD / CONTACT","7. SENSORS / DAQ","8. SAFETY LIMITS","9. GENERATED RIG"};
  int wiz=0; SeekBar wizSeek; TextView wizTitle,wizInfo; WizardView wizView;
  EditText wizLength,wizDiameter,wizForce,wizMoment,wizStations,wizMaxAct,wizMaxLayers,wizPadW,wizPadL,wizSample,wizWarn,wizAbort;
  Button useDefaults,generateRig;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    wizLength=field("Wizard tank length [m]","5.5");
    wizDiameter=field("Wizard tank diameter [m]","0.64");
    wizForce=field("Wizard target Fz [kN]","220");
    wizMoment=field("Wizard target My [kN·m]","50");
    wizStations=field("Wizard station count","8");
    wizMaxAct=field("Wizard max actuator count","6");
    wizMaxLayers=field("Wizard max layer count","4");
    wizPadW=field("Wizard pad width [mm]","180");
    wizPadL=field("Wizard pad length [mm]","220");
    wizSample=field("Wizard DAQ sample rate [Hz]","1000");
    wizWarn=field("Wizard warning tracking error [%]","2");
    wizAbort=field("Wizard abort tracking error [%]","10");

    LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(8),dp(8),dp(8),dp(8));panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("BANA RIG TASARLA / GUIDED DESIGN WIZARD",16,true,Color.WHITE));
    wizTitle=card("STEP 1/9 • TANK GEOMETRY",Color.rgb(20,48,68));panel.addView(wizTitle,lp());
    wizSeek=new SeekBar(this);wizSeek.setMax(8);panel.addView(wizSeek,new LinearLayout.LayoutParams(-1,dp(50)));
    LinearLayout buttons=new LinearLayout(this);useDefaults=new Button(this);generateRig=new Button(this);useDefaults.setText("ÖNERİLEN DEĞERLERİ KULLAN");generateRig.setText("RIG OLUŞTUR");buttons.addView(useDefaults,new LinearLayout.LayoutParams(0,dp(48),1));buttons.addView(generateRig,new LinearLayout.LayoutParams(0,dp(48),1));panel.addView(buttons);
    wizInfo=card("Wizard hazır.",Color.rgb(20,48,68));panel.addView(wizInfo,lp());
    wizView=new WizardView();panel.addView(wizView,new LinearLayout.LayoutParams(-1,dp(900)));root.addView(panel,27,lp());

    wizSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean u){wiz=p;refreshWizard();}});
    useDefaults.setOnClickListener(v->{applyWizardDefaults();refreshWizard();});
    generateRig.setOnClickListener(v->{applyWizardToModel();wiz=8;wizSeek.setProgress(8);refreshWizard();});
    refreshWizard();
  }

  void applyWizardDefaults(){
    switch(wiz){
      case 0:wizLength.setText("5.5");wizDiameter.setText("0.64");break;
      case 1:wizForce.setText("220");wizMoment.setText("50");break;
      case 2:wizStations.setText("8");break;
      case 3:wizMaxAct.setText("6");break;
      case 4:wizMaxLayers.setText("4");break;
      case 5:wizPadW.setText("180");wizPadL.setText("220");break;
      case 6:wizSample.setText("1000");break;
      case 7:wizWarn.setText("2");wizAbort.setText("10");break;
    }
  }

  void applyWizardToModel(){
    try{
      L.setText(wizLength.getText().toString());D.setText(wizDiameter.getText().toString());F.setText(wizForce.getText().toString());M.setText(wizMoment.getText().toString());stations.setText(wizStations.getText().toString());
      maxDesignerAct.setText(wizMaxAct.getText().toString());maxDesignerLayers.setText(wizMaxLayers.getText().toString());
      Calc c=compute(false);DesignChoice q=choose(c);if(q!=null){actCount.setText(String.valueOf(q.actuators));layers.setText(String.valueOf(q.layers));}
      warnTrack.setText(wizWarn.getText().toString());abortTrack.setText(wizAbort.getText().toString());
    }catch(Exception e){}
  }

  String stepText(){
    switch(wiz){
      case 0:return "Tankın fiziksel boyutlarını gir. Bilinmiyorsa 5.5 m uzunluk ve 0.64 m çap default alınabilir. Bu değerler station konumları, pad eğriliği ve moment kollarını etkiler.";
      case 1:return "Testte uygulanacak ana düşey kuvvet Fz ve pitch moment My değerlerini gir. Tasarım motoru station yüklerini bu hedefleri yaklaşık sağlayacak şekilde dağıtır.";
      case 2:return "Yükün yüzeye kaç noktadan aktarılacağını seç. Daha fazla station lokal basıncı azaltabilir fakat beam, joint ve sensör sayısını artırır.";
      case 3:return "Mekanik/hidrolik altyapının izin verdiği maksimum actuator sayısını belirt. Otomatik designer 1..N actuator alternatiflerini tarar.";
      case 4:return "İzin verilen maksimum whiffletree layer sayısını belirt. Gerekenden az layer station gruplarını taşıyamaz, fazla layer ise karmaşıklığı artırır.";
      case 5:return "Pad ölçülerini seç. Pad alanı ve tank eğriliği lokal temas basıncını belirler; final tasarım için gerçek skin/stringer allowables gerekir.";
      case 6:return "DAQ örnekleme hızını seç. Load cell, LVDT, pressure, strain ve displacement kanalları eşzamanlı izlenmelidir.";
      case 7:return "Safety/interlock eşiklarını tanımla. Warning/Hold/Abort seviyeleri gerçek rig hazard analysis ile doğrulanmalıdır.";
      default:return "Wizard girdileri ana modele aktarıldı. Aşağıdaki özet otomatik actuator/layer önerisini, 2D rig topolojisini, sensör yaklaşımını ve ilk BOM kapsamını gösterir.";
    }
  }

  void refreshWizard(){
    try{
      wizTitle.setText(String.format(Locale.US,"STEP %d/9 • %s",wiz+1,wizardSteps[wiz]));
      if(wiz<8){wizInfo.setText(stepText()+"\n\nDeğeri bilmiyorsan ÖNERİLEN DEĞERLERİ KULLAN butonuna basabilirsin.");}
      else{
        Calc c=compute(false);DesignChoice q=choose(c);double sum=0,peak=0;for(double v:c.fi){sum+=v;peak=Math.max(peak,v);}String rec=q==null?"-":q.actuators+" actuator / "+q.layers+" layer";
        wizInfo.setText(String.format(Locale.US,"GENERATED RIG CONCEPT\nTank %.2f m × %.2f m | Fz %.1f kN | My %.1f kN·m\nStations %d | recommended topology %s\nStation load sum %.1f kN | station peak %.1f kN\nPad %.0f × %.0f mm | DAQ %.0f Hz | warn %.1f%% | abort %.1f%%\nNext checks: beam/pivot, actuator/load cell, joint/pad, hydraulic, DAQ, collision and interlock review.",d(L),d(D),d(F),d(M),c.n,rec,sum,peak,d(wizPadW),d(wizPadL),d(wizSample),d(wizWarn),d(wizAbort)));
      }
      if(wizView!=null)wizView.invalidate();
    }catch(Exception e){}
  }

  class WizardView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);WizardView(){super(V47Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    @Override protected void onDraw(Canvas c){super.onDraw(c);int W=getWidth();t.setColor(Color.WHITE);t.setTextSize(dp(13));c.drawText("GUIDED EFT RIG DESIGN",dp(14),dp(28),t);float left=dp(25),right=W-dp(25),y=dp(85);for(int i=0;i<9;i++){float x=left+(right-left)*i/8f;p.setColor(i<wiz?Color.rgb(67,190,113):(i==wiz?Color.rgb(247,207,77):Color.rgb(80,95,110)));c.drawCircle(x,y,dp(7),p);if(i<8){p.setStrokeWidth(dp(3));p.setColor(i<wiz?Color.rgb(67,190,113):Color.rgb(80,95,110));c.drawLine(x+dp(7),y,left+(right-left)*(i+1)/8f-dp(7),y,p);}}
      float tankY=dp(250);p.setColor(Color.rgb(70,82,95));c.drawRoundRect(new RectF(left,tankY-dp(30),right,tankY+dp(30)),dp(30),dp(30),p);int ns=Math.max(2,(int)Math.round(d(wizStations)));int na=Math.max(1,Math.min(ns,(int)Math.round(d(wizMaxAct))));for(int i=0;i<ns;i++){float x=left+(right-left)*(i+.5f)/ns;p.setColor(Color.rgb(247,207,77));c.drawCircle(x,tankY,dp(5),p);p.setStrokeWidth(dp(2));p.setColor(Color.rgb(205,215,225));c.drawLine(x,tankY+dp(30),x,tankY+dp(90),p);}
      float ay=dp(430);for(int a=0;a<na;a++){float x=left+(right-left)*(a+.5f)/na;p.setColor(Color.rgb(51,205,220));c.drawCircle(x,ay,dp(8),p);p.setColor(Color.rgb(55,132,238));c.drawRoundRect(new RectF(x-dp(16),ay+dp(40),x+dp(16),ay+dp(105)),dp(7),dp(7),p);t.setTextSize(dp(6));t.setColor(Color.WHITE);c.drawText("A"+(a+1),x-dp(7),ay+dp(76),t);}
      float by=dp(585);p.setColor(Color.rgb(16,43,62));c.drawRoundRect(new RectF(dp(16),by,W-dp(16),by+dp(240)),dp(10),dp(10),p);t.setTextSize(dp(8));t.setColor(Color.rgb(247,207,77));c.drawText(wizardSteps[wiz],dp(28),by+dp(28),t);t.setColor(Color.WHITE);t.setTextSize(dp(7));String s=stepText();int max=78,line=0;for(int i=0;i<s.length();i+=max){c.drawText(s.substring(i,Math.min(s.length(),i+max)),dp(28),by+dp(58)+line*dp(22),t);line++;if(line>5)break;}t.setColor(Color.rgb(185,210,230));c.drawText("Wizard → auto topology → detailed engineering screens → BOM/report",dp(28),by+dp(215),t);}
  }
}
