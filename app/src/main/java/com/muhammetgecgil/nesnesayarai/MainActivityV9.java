package com.muhammetgecgil.nesnesayarai;

import android.graphics.*;
import android.view.*;
import android.widget.*;
import androidx.camera.view.PreviewView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.*;

public class MainActivityV9 extends MainActivityV8 {

 @Override void ui(){
  FrameLayout root=new FrameLayout(this);
  preview=new PreviewView(this); preview.setScaleType(PreviewView.ScaleType.FILL_CENTER); root.addView(preview,new FrameLayout.LayoutParams(-1,-1));
  overlay=new CenterOverlay(); root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));

  LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(14),0,dp(14),0); top.setBackgroundColor(Color.argb(210,10,18,22));
  TextView title=new TextView(this); title.setText("Nesne Sayar AI"); title.setTextColor(Color.WHITE); title.setTextSize(22); top.addView(title,new LinearLayout.LayoutParams(0,dp(58),1));
  status=new TextView(this); status.setText("V111 ROI Kesin Tarama"); status.setTextColor(Color.LTGRAY); status.setTextSize(12); status.setGravity(Gravity.CENTER); top.addView(status,new LinearLayout.LayoutParams(dp(190),dp(58)));
  result=new TextView(this); result.setText("0"); result.setTextColor(Color.rgb(45,245,130)); result.setTextSize(34); result.setGravity(Gravity.END|Gravity.CENTER_VERTICAL); top.addView(result,new LinearLayout.LayoutParams(dp(58),dp(58)));
  FrameLayout.LayoutParams tlp=new FrameLayout.LayoutParams(-1,dp(58),Gravity.TOP); root.addView(top,tlp);

  pen=btn("✎"); pen.setTextSize(24); pen.setBackgroundColor(Color.rgb(0,145,78));
  FrameLayout.LayoutParams plp=new FrameLayout.LayoutParams(dp(58),dp(58),Gravity.TOP|Gravity.START); plp.leftMargin=dp(8); plp.topMargin=dp(68); root.addView(pen,plp);

  LinearLayout bot=new LinearLayout(this); Button src=btn("CANLI"); mode=btn("FARKLI"); Button count=btn("SAY"),clear=btn("SİL"); count.setBackgroundColor(Color.rgb(0,145,78));
  for(Button x:new Button[]{src,mode,count,clear}) bot.addView(x,new LinearLayout.LayoutParams(0,dp(60),1));
  FrameLayout.LayoutParams blp=new FrameLayout.LayoutParams(-1,dp(60),Gravity.BOTTOM); root.addView(bot,blp);

  ViewCompat.setOnApplyWindowInsetsListener(root,(v,i)->{Insets x=i.getInsets(WindowInsetsCompat.Type.systemBars()); tlp.topMargin=x.top; plp.topMargin=x.top+dp(66); blp.bottomMargin=x.bottom+dp(10); top.setLayoutParams(tlp); pen.setLayoutParams(plp); bot.setLayoutParams(blp); return i;});
  setContentView(root);

  overlay.drawMode=true;
  status.setText("Bölgeyi çiz");

  pen.setOnClickListener(v->{overlay.drawMode=!overlay.drawMode; pen.setBackgroundColor(overlay.drawMode?Color.rgb(0,145,78):Color.rgb(19,58,68)); status.setText(overlay.drawMode?"Bölgeyi çiz":"ROI kilitli");});
  mode.setOnClickListener(v->{same=!same; mode.setText(same?"AYNI":"FARKLI"); sample=null; overlay.sample=null; overlay.invalidate(); refresh();});
  count.setOnClickListener(v->{if(overlay.copyRoi().size()<3){status.setText("Önce alanı çiz"); overlay.drawMode=true; pen.setBackgroundColor(Color.rgb(0,145,78)); return;} Bitmap f=latest; if(f!=null) analyze(f.copy(Bitmap.Config.ARGB_8888,false));});
  clear.setOnClickListener(v->{boxes=new ArrayList<>(); sample=null; overlay.boxes=Collections.emptyList(); overlay.sample=null; overlay.clearRoi(); overlay.drawMode=true; pen.setBackgroundColor(Color.rgb(0,145,78)); overlay.invalidate(); result.setText("0"); status.setText("Bölgeyi çiz");});
 }

 @Override List<RectF> cleanRaw(List<RectF> raw,int W,int H,List<PointF> rv,int vw,int vh){
  List<RectF> a=new ArrayList<>();
  for(RectF r:raw){
   float af=area(r)/(W*(float)H); if(af<.00012f||af>.55f) continue;
   if(!rv.isEmpty() && roiCoverage(r,W,H,rv,vw,vh)<.82f) continue;
   a.add(new RectF(r));
  }
  List<RectF> d=dedup(a), out=new ArrayList<>();
  for(int i=0;i<d.size();i++){
   RectF A=d.get(i); int children=0;
   for(int j=0;j<d.size();j++) if(i!=j){RectF B=d.get(j); if(area(B)<area(A)*.42f && A.contains(B.centerX(),B.centerY())) children++;}
   if(children<2) out.add(A);
  }
  return out;
 }

 @Override List<RectF> hybrid(Bitmap src,List<RectF> raw,List<PointF> rv,int vw,int vh){
  List<RectF> base=super.hybrid(src,raw,rv,vw,vh); int W=src.getWidth(),H=src.getHeight();
  List<RectF> strict=cleanRaw(raw,W,H,rv,vw,vh); List<RectF> kept=new ArrayList<>();
  for(RectF b:base){
   if(!rv.isEmpty() && roiCoverage(b,W,H,rv,vw,vh)<.68f) continue;
   int kids=0; for(RectF s:strict) if(area(s)<area(b)*.42f && b.contains(s.centerX(),s.centerY())) kids++;
   if(kids>=2) continue;
   kept.add(b);
  }
  if(kept.size()<=1 && strict.size()>=2) kept=new ArrayList<>(strict);
  kept=dedup(kept);
  kept.sort((a,b)->{int q=Float.compare(a.centerY(),b.centerY()); return q!=0?q:Float.compare(a.centerX(),b.centerX());});
  return kept;
 }

 float roiCoverage(RectF r,int W,int H,List<PointF> poly,int vw,int vh){
  if(poly.isEmpty()) return 1f; int in=0,total=0;
  for(int gy=0;gy<7;gy++) for(int gx=0;gx<7;gx++){
   float x=r.left+(gx+.5f)*r.width()/7f, y=r.top+(gy+.5f)*r.height()/7f;
   PointF p=mapToView(x,y,W,H,vw,vh); total++; if(point(p.x,p.y,poly)) in++;
  }
  return in/(float)total;
 }

 class CenterOverlay extends Overlay {
  @Override protected void onDraw(Canvas c){
   if(!roi.isEmpty()) c.drawPath(path,rp);
   for(int i=0;i<boxes.size();i++){
    RectF r=map(boxes.get(i)); c.drawRoundRect(r,dp(7),dp(7),box);
    float cx=r.centerX(), cy=r.centerY();
    cx=Math.max(dp(18),Math.min(getWidth()-dp(18),cx)); cy=Math.max(dp(82),Math.min(getHeight()-dp(82),cy));
    c.drawCircle(cx,cy,dp(16),num); c.drawText(""+(i+1),cx,cy+dp(6),txt);
   }
   if(sample!=null) c.drawRoundRect(map(sample),dp(9),dp(9),sp);
  }
 }
}
