package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V62Activity extends V61Activity {
  LinearLayout zonePanel; EditText[] zx=new EditText[8], zfx=new EditText[8], zfy=new EditText[8], zfz=new EditText[8];
  Button calcShow; TextView calcSummary; Signed2DView signed2D; ArrayList<SNode> solved=new ArrayList<>(); boolean solvedValid=false;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(8),dp(8),dp(8),dp(8));p.setBackground(bg(Color.rgb(2,17,28),14));
    p.addView(tx("SIGNED LOAD-ZONE CALCULATOR — PRIMARY WORKFLOW",20,true,Color.WHITE));
    p.addView(tx("Enter +/− Fx, Fy, Fz for each EFT zone → HESAPLA VE GÖSTER → inspect every load path node in 2D/3D",9,false,Color.rgb(180,210,230)));
    calcSummary=card("Enter signed forces, then press HESAPLA VE GÖSTER.",Color.rgb(15,49,69));p.addView(calcSummary,lp());
    zonePanel=new LinearLayout(this);zonePanel.setOrientation(LinearLayout.VERTICAL);zonePanel.setPadding(dp(6),dp(6),dp(6),dp(6));zonePanel.setBackground(bg(Color.rgb(12,34,50),10));
    zonePanel.addView(tx("LOAD ZONES — signed N values",14,true,Color.WHITE));
    zonePanel.addView(tx("X% = location along tank. Negative force reverses vector direction.",8,false,Color.rgb(175,205,225)));
    for(int i=0;i<8;i++)addZoneRow(i);
    p.addView(zonePanel,lp());
    calcShow=new Button(this);calcShow.setText("HESAPLA VE GÖSTER");calcShow.setTextSize(18);calcShow.setOnClickListener(v->calculateAndShow());p.addView(calcShow,new LinearLayout.LayoutParams(-1,dp(62)));
    signed2D=new Signed2DView();p.addView(signed2D,new LinearLayout.LayoutParams(-1,dp(820)));
    root.addView(p,Math.min(1,root.getChildCount()),lp());
    seedZones();
  }

  EditText compact(String def){EditText e=new EditText(this);e.setText(def);e.setTextColor(Color.WHITE);e.setTextSize(13);e.setSingleLine(true);e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);e.setBackground(bg(Color.rgb(24,52,73),7));e.setPadding(dp(6),0,dp(6),0);return e;}
  TextView h(String s){return tx(s,8,true,Color.rgb(185,210,230));}
  void addZoneRow(int i){
    LinearLayout hdr=new LinearLayout(this);hdr.setOrientation(LinearLayout.HORIZONTAL);hdr.addView(h("Z"+(i+1)),new LinearLayout.LayoutParams(0,dp(26),.7f));hdr.addView(h("X%"),new LinearLayout.LayoutParams(0,dp(26),1));hdr.addView(h("Fx [N]"),new LinearLayout.LayoutParams(0,dp(26),1.4f));hdr.addView(h("Fy [N]"),new LinearLayout.LayoutParams(0,dp(26),1.4f));hdr.addView(h("Fz [N]"),new LinearLayout.LayoutParams(0,dp(26),1.4f));zonePanel.addView(hdr);
    LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);TextView id=tx("Z"+(i+1),9,true,Color.WHITE);row.addView(id,new LinearLayout.LayoutParams(0,dp(42),.7f));zx[i]=compact(String.format(Locale.US,"%.1f",(i+.5)*100/8.0));zfx[i]=compact("0");zfy[i]=compact("0");zfz[i]=compact("0");row.addView(zx[i],new LinearLayout.LayoutParams(0,dp(42),1));row.addView(zfx[i],new LinearLayout.LayoutParams(0,dp(42),1.4f));row.addView(zfy[i],new LinearLayout.LayoutParams(0,dp(42),1.4f));row.addView(zfz[i],new LinearLayout.LayoutParams(0,dp(42),1.4f));zonePanel.addView(row);
  }

  void seedZones(){
    int n=Math.min(8,Math.max(1,qi(qSections,1,8)));double fx=qd(qFx)/n,fy=qd(qFy)/n,fz=qd(qFz)/n;
    for(int i=0;i<8;i++){zx[i].setText(String.format(Locale.US,"%.1f",(i+.5)*100.0/n));if(i<n){zfx[i].setText(String.format(Locale.US,"%.0f",fx));zfy[i].setText(String.format(Locale.US,"%.0f",fy));zfz[i].setText(String.format(Locale.US,"%.0f",fz));}else{zfx[i].setText("0");zfy[i].setText("0");zfz[i].setText("0");}}
  }

  void calculateAndShow(){
    try{
      int n=Math.min(8,Math.max(1,qi(qSections,1,8))),na=Math.max(1,qi(qActs,1,12));double len=Math.max(1,qd(qLength)),yo=qd(qYoff),zo=qd(qZoff),fac=phaseFactor(),k=Math.max(1,qd(qStiffness)),g=Math.max(1,qd(qGaugeLength));
      solved.clear();double sx=0,sy=0,sz=0,smx=0,smy=0,smz=0,maxR=0,maxD=0,maxE=0;
      for(int i=0;i<n;i++){
        SNode s=new SNode();s.section=i;s.act=Math.min(na-1,(int)Math.floor((double)i*na/n));double pct=Math.max(0,Math.min(100,qd(zx[i])));s.x=-len/2+pct/100.0*len;s.y=yo;s.z=zo;s.fx=qd(zfx[i])*fac;s.fy=qd(zfy[i])*fac;s.fz=qd(zfz[i])*fac;s.r=Math.sqrt(s.fx*s.fx+s.fy*s.fy+s.fz*s.fz);s.mx=s.y*s.fz-s.z*s.fy;s.my=s.z*s.fx-s.x*s.fz;s.mz=s.x*s.fy-s.y*s.fx;s.disp=s.r/k;s.strain=s.disp/g*1e6;s.lc=s.r;solved.add(s);sx+=s.fx;sy+=s.fy;sz+=s.fz;smx+=s.mx;smy+=s.my;smz+=s.mz;maxR=Math.max(maxR,s.r);maxD=Math.max(maxD,s.disp);maxE=Math.max(maxE,s.strain);
      }
      solvedValid=true;signed2D.invalidate();structural3D.nodes=new ArrayList<>(solved);structural3D.invalidate();
      calcSummary.setText(String.format(Locale.US,"CALCULATED — %s / %s / %.0f%% command\nΣFx %.1f N | ΣFy %.1f N | ΣFz %.1f N | resultant %.1f N\nΣMx %.1f Nmm | ΣMy %.1f Nmm | ΣMz %.1f Nmm\nPeak zone resultant %.1f N | peak disp %.5f mm | peak strain %.1f µε\nNegative force signs are preserved. 2D/3D arrows reverse direction with sign.",qPhase.getSelectedItem().toString(),qActType.getSelectedItemPosition()==0?"HYDRAULIC":"ELECTRIC",(double)phaseLevel.getProgress(),sx,sy,sz,Math.sqrt(sx*sx+sy*sy+sz*sz),smx,smy,smz,maxR,maxD,maxE));
    }catch(Exception e){solvedValid=false;calcSummary.setText("CALCULATION BLOCKED — check load-zone numeric inputs.");}
  }

  class Signed2DView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);Signed2DView(){super(V62Activity.this);setBackgroundColor(Color.rgb(2,12,20));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    void arrow(Canvas c,float x1,float y1,float x2,float y2,int col){p.setColor(col);p.setStrokeWidth(dp(3));c.drawLine(x1,y1,x2,y2,p);double a=Math.atan2(y2-y1,x2-x1),l=dp(9);c.drawLine(x2,y2,(float)(x2-l*Math.cos(a-.55)),(float)(y2-l*Math.sin(a-.55)),p);c.drawLine(x2,y2,(float)(x2-l*Math.cos(a+.55)),(float)(y2-l*Math.sin(a+.55)),p);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);int W=getWidth();t.setColor(Color.WHITE);t.setTextSize(dp(14));c.drawText("2D SIGNED FORCE MAP — EFT / WHIFFLETREE",dp(14),dp(30),t);if(!solvedValid){t.setTextSize(dp(9));t.setColor(Color.rgb(180,200,215));c.drawText("Press HESAPLA VE GÖSTER to calculate the model.",dp(14),dp(65),t);return;}float left=dp(35),right=W-dp(35),cy=dp(210);p.setColor(Color.rgb(68,80,92));c.drawRoundRect(new RectF(left,cy-dp(42),right,cy+dp(42)),dp(42),dp(42),p);double len=Math.max(1,qd(qLength)),max=1;for(SNode s:solved)max=Math.max(max,s.r);float treeY=dp(430),actY=dp(650);for(SNode s:solved){float x=(float)(left+(s.x+len/2)/len*(right-left));p.setColor(Color.WHITE);c.drawCircle(x,cy,dp(5),p);float sc=(float)(dp(70)/max);arrow(c,x,cy,x+(float)(s.fx*sc),cy,Color.rgb(230,80,80));arrow(c,x,cy,x,cy-(float)(s.fz*sc),Color.rgb(80,150,240));arrow(c,x,cy,x+(float)(s.fy*sc*.55),cy+(float)(s.fy*sc*.35),Color.rgb(80,210,120));t.setTextSize(dp(6));t.setColor(Color.WHITE);c.drawText(String.format(Locale.US,"Z%d",s.section+1),x-dp(7),cy-dp(52),t);c.drawText(String.format(Locale.US,"Fx %.0f",s.fx),x-dp(18),cy-dp(70),t);c.drawText(String.format(Locale.US,"Fy %.0f",s.fy),x-dp(18),cy-dp(56),t);c.drawText(String.format(Locale.US,"Fz %.0f",s.fz),x-dp(18),cy-dp(42),t);float tx=left+(right-left)*(s.act+.5f)/Math.max(1,qi(qActs,1,12));p.setColor(Color.rgb(51,205,220));p.setStrokeWidth(dp(2));c.drawLine(x,cy+dp(48),tx,treeY,p);p.setColor(Color.rgb(247,207,77));c.drawCircle(tx,treeY,dp(6),p);c.drawLine(tx,treeY,tx,actY-dp(55),p);}int na=Math.max(1,qi(qActs,1,12));for(int a=0;a<na;a++){float ax=left+(right-left)*(a+.5f)/na;p.setColor(qActType.getSelectedItemPosition()==0?Color.rgb(67,190,113):Color.rgb(160,110,230));c.drawRoundRect(new RectF(ax-dp(16),actY-dp(55),ax+dp(16),actY+dp(25)),dp(6),dp(6),p);t.setColor(Color.WHITE);t.setTextSize(dp(7));c.drawText("A"+(a+1),ax-dp(7),actY+dp(45),t);}t.setTextSize(dp(7));t.setColor(Color.rgb(230,80,80));c.drawText("X / Fx",dp(18),dp(760),t);t.setColor(Color.rgb(80,210,120));c.drawText("Y / Fy",dp(90),dp(760),t);t.setColor(Color.rgb(80,150,240));c.drawText("Z / Fz",dp(162),dp(760),t);t.setColor(Color.rgb(190,210,225));c.drawText("Arrow direction follows sign. Labels retain signed N values.",dp(18),dp(790),t);}
  }
}
