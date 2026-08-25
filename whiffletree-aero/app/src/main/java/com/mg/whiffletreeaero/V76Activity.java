package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V76Activity extends V75Activity {
  LinearLayout simPanel;
  TextView simSummary, componentSummary, cadSummary;
  SeekBar loadCommand;
  Spinner simPhase;
  RigAssemblyView rigAssembly;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    simPanel=new LinearLayout(this);simPanel.setOrientation(LinearLayout.VERTICAL);simPanel.setPadding(dp(10),dp(10),dp(10),dp(10));simPanel.setBackground(bg(Color.rgb(2,15,25),16));
    simPanel.addView(tx("TEST ANALYSIS + SIMULATION + CAD ASSEMBLY",20,true,Color.WHITE));
    simPanel.addView(tx("Hesap → bileşen seçimi → load sequence simülasyonu → assembly hazırlığı. Tüm ekranlar aynı aktif solved load setini kullanır.",9,false,Color.rgb(180,210,230)));

    simPanel.addView(tx("TEST FAZI",11,true,Color.rgb(247,207,77)));
    simPhase=new Spinner(this);simPhase.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"PRELOAD","LIMIT LOAD","ULTIMATE LOAD","UNLOAD"}));simPanel.addView(simPhase,new LinearLayout.LayoutParams(-1,dp(50)));
    simPanel.addView(tx("LOAD COMMAND [%]",11,true,Color.rgb(247,207,77)));
    loadCommand=new SeekBar(this);loadCommand.setMax(150);loadCommand.setProgress(100);simPanel.addView(loadCommand,new LinearLayout.LayoutParams(-1,dp(54)));
    simSummary=card("Önce HESAPLA VE GÖSTER. Sonra load command ile test fazını simüle et.",Color.rgb(14,45,64));simPanel.addView(simSummary,lp());
    componentSummary=card("Bileşen seçim özeti hesap sonrası görünür.",Color.rgb(13,39,54));simPanel.addView(componentSummary,lp());
    rigAssembly=new RigAssemblyView();simPanel.addView(rigAssembly,new LinearLayout.LayoutParams(-1,dp(1150)));
    cadSummary=card("CAD assembly hazırlık özeti hesap sonrası görünür.",Color.rgb(13,39,54));simPanel.addView(cadSummary,lp());
    root.addView(simPanel,6,lp());

    loadCommand.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){refreshSimulation();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
    simPhase.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> a,View v,int p,long id){refreshSimulation();}public void onNothingSelected(android.widget.AdapterView<?> a){}});
  }

  @Override void runGuided(){super.runGuided();refreshSimulation();}
  @Override void runPrimary(){super.runPrimary();refreshSimulation();}
  @Override void calculateAndShow(){super.calculateAndShow();refreshSimulation();}

  double phaseFactor(){int p=simPhase==null?1:simPhase.getSelectedItemPosition();double cmd=(loadCommand==null?100:loadCommand.getProgress())/100.0;if(p==0)return Math.min(.25,cmd);if(p==1)return Math.min(1.0,cmd);if(p==2)return Math.min(1.5,cmd);return Math.max(0,1.0-cmd);}

  void refreshSimulation(){
    if(simSummary==null)return;
    if(!solvedValid||solved==null||solved.isEmpty()){
      simSummary.setText("SIMULATION WAITING — önce HESAPLA VE GÖSTER.");componentSummary.setText("Henüz bileşen hesabı yok.");cadSummary.setText("Henüz assembly datası yok.");if(rigAssembly!=null)rigAssembly.invalidate();return;
    }
    double k=phaseFactor();int na=Math.max(1,qi(qActs,1,12));double sx=0,sy=0,sz=0,peak=0,maxM=0,maxDisp=0;
    double[] ar=new double[na];
    for(SNode s:solved){sx+=s.fx*k;sy+=s.fy*k;sz+=s.fz*k;maxM=Math.max(maxM,Math.sqrt(s.mx*s.mx+s.my*s.my+s.mz*s.mz)*k);maxDisp=Math.max(maxDisp,Math.abs(s.disp)*k);ar[Math.min(na-1,s.act)]+=s.r*k;}
    for(double v:ar)peak=Math.max(peak,v);
    simSummary.setText(String.format(Locale.US,"LIVE TEST SIMULATION\n%s • Command %d%% • effective factor %.3f\nΣFx %+.0f N | ΣFy %+.0f N | ΣFz %+.0f N\nPeak actuator %.0f N | Max moment %.0f Nmm | Max displacement %.3f mm\nLoad-cell / actuator / layer values in the assembly view scale live with the command.",simPhase.getSelectedItem().toString(),loadCommand.getProgress(),k,sx,sy,sz,peak,maxM,maxDisp));

    boolean hyd=qActType.getSelectedItemPosition()==0;String actFamily=hyd?"MTS Series 244 hydraulic family":"Moog electric linear servoactuator family";String lcFamily="HBK U10M force transducer family";
    double lcDesign=peak*1.25;
    componentSummary.setText(String.format(Locale.US,"TEST COMPONENT SELECTION — ACTIVE DEMAND\nActuator: %s\nRequired peak force ≥ %.0f N\nLoad cell: %s\nRecommended rated capacity ≥ %.0f N\nBeam: %s | Pin/Clevis: %s\nSelection status: PRELIMINARY CANDIDATE. Current manufacturer datasheet, stroke, fatigue, side-load, attachment and environment must be verified before release.",actFamily,peak,lcFamily,lcDesign,beamMaterial.getSelectedItem().toString(),pinMaterial.getSelectedItem().toString()));

    cadSummary.setText(String.format(Locale.US,"CAD / ASSEMBLY BUILD MANIFEST\nEFT body envelope: L %.3f m × D %.3f m\n%d load stations / pads\n%d whiffletree layer(s)\n%d load-cell + actuator line(s)\nBeam material: %s\nPin/Clevis material: %s\n\nCAD STATUS\n• Manufacturer CAD available/verified → use original vendor geometry\n• Vendor CAD not verified → show GENERIC ENVELOPE only\n• Custom whiffletree beams, pads, pins and strongback → generated from calculated geometry\n• Final assembly structure: EFT → pad → rod/clevis → beam/pivot → load cell → actuator → strongback\n\nThis screen prepares assembly geometry and BOM; it does not claim unverified vendor models are original CAD.",qd(qLength),qd(qDiameter),solved.size(),Math.max(1,qi(qLayers,1,4)),na,beamMaterial.getSelectedItem().toString(),pinMaterial.getSelectedItem().toString()));
    rigAssembly.invalidate();
  }

  class RigAssemblyView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    RigAssemblyView(){super(V76Activity.this);setBackgroundColor(Color.rgb(1,9,16));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    float px(float v){return v*getResources().getDisplayMetrics().density;}
    void text(Canvas c,String s,float x,float y,int col,float size){t.setColor(col);t.setTextSize(px(size));c.drawText(s,x,y,t);}void line(Canvas c,float x1,float y1,float x2,float y2,int col,float w){p.setColor(col);p.setStrokeWidth(px(w));c.drawLine(x1,y1,x2,y2,p);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float L=px(20),R=getWidth()-px(20);text(c,"ASSEMBLY / TEST-RIG VISUAL — LIVE LOAD",L,px(28),Color.WHITE,12);if(!solvedValid||solved==null||solved.isEmpty()){text(c,"Önce HESAPLA VE GÖSTER",L,px(65),Color.LTGRAY,8);return;}double k=phaseFactor();double len=Math.max(1,qd(qLength));int na=Math.max(1,qi(qActs,1,12)),nl=Math.max(1,qi(qLayers,1,4));float tankY=px(145);p.setColor(Color.rgb(88,98,108));c.drawRoundRect(new RectF(L,tankY-px(40),R,tankY+px(40)),px(35),px(35),p);text(c,"EFT",L+px(8),tankY+px(4),Color.WHITE,7);
      ArrayList<float[]> cur=new ArrayList<>();for(SNode s:solved){float x=(float)(L+(s.x+len/2.0)/len*(R-L));double rr=s.r*k;p.setColor(Color.rgb(90,205,110));c.drawRect(x-px(6),tankY+px(42),x+px(6),tankY+px(54),p);text(c,"S"+(s.section+1),x-px(6),tankY-px(54),Color.WHITE,4.8f);text(c,String.format(Locale.US,"%.0fN",rr),x-px(12),tankY-px(39),Color.rgb(180,215,250),4.4f);cur.add(new float[]{x,tankY+px(55),(float)(s.fx*k),(float)(s.fy*k),(float)(s.fz*k)});}float y=px(285);
      for(int l=1;l<=nl;l++){int target=(l==nl)?na:Math.max(na,(int)Math.ceil(cur.size()/2.0));ArrayList<float[]> next=new ArrayList<>();text(c,"LAYER "+l,L,y-px(28),Color.rgb(247,190,70),6);for(int g=0;g<target;g++){int i0=(int)Math.floor((double)g*cur.size()/target),i1=Math.max(i0,Math.min(cur.size()-1,(int)Math.floor((double)(g+1)*cur.size()/target)-1));float x=0,fx=0,fy=0,fz=0;int n=0;for(int i=i0;i<=i1;i++){float[] q=cur.get(i);x+=q[0];fx+=q[2];fy+=q[3];fz+=q[4];n++;}x/=Math.max(1,n);for(int i=i0;i<=i1;i++){float[] q=cur.get(i);line(c,q[0],q[1],x,y-px(10),Color.rgb(145,160,172),1.5f);}p.setColor(Color.rgb(220,145,32));c.drawRoundRect(new RectF(x-px(34),y-px(8),x+px(34),y+px(8)),px(3),px(3),p);p.setColor(Color.LTGRAY);c.drawCircle(x,y,px(4),p);double rr=Math.sqrt(fx*fx+fy*fy+fz*fz);text(c,String.format(Locale.US,"%.0fN",rr),x-px(14),y+px(25),Color.WHITE,4.8f);next.add(new float[]{x,y+px(11),fx,fy,fz});}cur=next;y+=px(115);}float lcY=y+px(15),actY=lcY+px(125);text(c,"LOAD CELL",L,lcY-px(28),Color.rgb(105,230,125),6);text(c,"ACTUATOR",L,actY-px(45),Color.rgb(220,225,230),6);
      for(int a=0;a<na;a++){float ax=L+(R-L)*(a+.5f)/na;double rr=0;for(SNode s:solved)if(s.act==a)rr+=s.r*k;float[] src=cur.get(Math.min(cur.size()-1,a));line(c,src[0],src[1],ax,lcY-px(12),Color.rgb(165,175,185),1.4f);p.setColor(Color.rgb(73,181,95));c.drawRect(ax-px(9),lcY-px(12),ax+px(9),lcY+px(12),p);text(c,"LC"+(a+1),ax-px(10),lcY+px(28),Color.WHITE,4.8f);line(c,ax,lcY+px(13),ax,actY-px(34),Color.rgb(170,180,188),1.4f);p.setColor(qActType.getSelectedItemPosition()==0?Color.rgb(64,72,79):Color.rgb(116,78,160));c.drawRoundRect(new RectF(ax-px(15),actY-px(34),ax+px(15),actY+px(34)),px(5),px(5),p);text(c,"A"+(a+1),ax-px(6),actY+px(51),Color.WHITE,4.8f);text(c,String.format(Locale.US,"%.0fN",rr),ax-px(12),actY+px(68),Color.rgb(247,207,77),4.5f);}p.setColor(Color.rgb(45,52,59));c.drawRect(L,actY+px(95),R,actY+px(118),p);text(c,"STRONGBACK / GROUND REACTION",L+px(8),actY+px(143),Color.rgb(205,215,225),5.7f);text(c,"CAD visual uses verified vendor geometry when available; otherwise generic envelopes.",L,actY+px(190),Color.rgb(180,205,220),5.2f);
    }
  }
}
