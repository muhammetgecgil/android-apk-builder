package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V64Activity extends V63Activity {
  EditText[] zy=new EditText[8], zz=new EditText[8], sgx=new EditText[8], sgy=new EditText[8], sgz=new EditText[8];
  TextView mappingSummary,sgInspector; Mapping3DView mapping3D;

  static class SG {String id;double x,y,z,strain,disp,fx,fy,fz,r;int zone;}
  ArrayList<SG> gauges=new ArrayList<>();

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(8),dp(8),dp(8),dp(8));p.setBackground(bg(Color.rgb(2,15,26),14));
    p.addView(tx("REAL LOAD-ZONE / PAD / STRAIN-GAUGE MAPPING",19,true,Color.WHITE));
    p.addView(tx("Independent Y/Z pad coordinates + SG XYZ locations; HESAPLA VE GÖSTER updates signed loads and sensor response",9,false,Color.rgb(180,210,230)));
    mappingSummary=card("Set zone Y/Z and strain-gauge coordinates, then press HESAPLA VE GÖSTER.",Color.rgb(15,49,69));p.addView(mappingSummary,lp());
    p.addView(tx("LOAD-ZONE PAD COORDINATES [mm]",13,true,Color.rgb(247,207,77)),lp());
    for(int i=0;i<8;i++){
      LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.addView(tx("Z"+(i+1),8,true,Color.WHITE),new LinearLayout.LayoutParams(0,dp(42),.7f));
      zy[i]=compact("0");zz[i]=compact("0");r.addView(zy[i],new LinearLayout.LayoutParams(0,dp(42),1));r.addView(zz[i],new LinearLayout.LayoutParams(0,dp(42),1));p.addView(r,lp());
    }
    p.addView(tx("STRAIN-GAUGE XYZ COORDINATES [mm]",13,true,Color.rgb(247,207,77)),lp());
    for(int i=0;i<8;i++){
      LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.addView(tx("SG"+(i+1),8,true,Color.WHITE),new LinearLayout.LayoutParams(0,dp(42),.7f));
      sgx[i]=compact(String.format(Locale.US,"%.1f",-3500+i*1000.0));sgy[i]=compact("0");sgz[i]=compact("0");r.addView(sgx[i],new LinearLayout.LayoutParams(0,dp(42),1));r.addView(sgy[i],new LinearLayout.LayoutParams(0,dp(42),1));r.addView(sgz[i],new LinearLayout.LayoutParams(0,dp(42),1));p.addView(r,lp());
    }
    sgInspector=card("Tap a strain gauge or mapped pad in 3D.",Color.rgb(18,48,66));p.addView(sgInspector,lp());
    mapping3D=new Mapping3DView();p.addView(mapping3D,new LinearLayout.LayoutParams(-1,dp(980)));
    root.addView(p,Math.min(2,root.getChildCount()),lp());
  }

  @Override void calculateAndShow(){
    super.calculateAndShow();
    if(!solvedValid)return;
    int n=Math.min(solved.size(),8);double k=Math.max(1,qd(qStiffness)),g=Math.max(1,qd(qGaugeLength));
    for(int i=0;i<n;i++){SNode s=solved.get(i);s.y=qd(zy[i]);s.z=qd(zz[i]);s.mx=s.y*s.fz-s.z*s.fy;s.my=s.z*s.fx-s.x*s.fz;s.mz=s.x*s.fy-s.y*s.fx;}
    buildConnections();connection2D.invalidate();connection3D.invalidate();structural3D.nodes=new ArrayList<>(solved);structural3D.invalidate();
    gauges.clear();int ng=Math.min(8,Math.max(1,qi(qStrainCount,1,8)));double peak=0;
    for(int i=0;i<ng;i++){
      SG q=new SG();q.id="SG"+(i+1);q.x=qd(sgx[i]);q.y=qd(sgy[i]);q.z=qd(sgz[i]);double best=Double.MAX_VALUE;SNode near=null;
      for(SNode s:solved){double dx=q.x-s.x,dy=q.y-s.y,dz=q.z-s.z,dd=dx*dx+dy*dy+dz*dz;if(dd<best){best=dd;near=s;}}
      if(near!=null){q.zone=near.section;q.fx=near.fx;q.fy=near.fy;q.fz=near.fz;q.r=near.r;q.disp=q.r/k;q.strain=q.disp/g*1e6;peak=Math.max(peak,Math.abs(q.strain));}
      gauges.add(q);
    }
    mappingSummary.setText(String.format(Locale.US,"MAPPING CALCULATED\n%d real pad coordinates • %d strain gauges\nSigned force transfer follows each mapped zone XYZ. Peak predicted gauge strain %.1f µε\nTap 3D markers to inspect zone/SG load, displacement and strain.",n,gauges.size(),peak));
    mapping3D.invalidate();
  }

  class Mapping3DView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);ArrayList<Object> hit=new ArrayList<>();ArrayList<float[]> hp=new ArrayList<>();
    Mapping3DView(){super(V64Activity.this);setBackgroundColor(Color.rgb(1,9,17));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    float[] pr(double x,double y,double z,float cx,float cy,double sc){return new float[]{(float)(cx+(x-y)*.68*sc),(float)(cy+(x+y)*.18*sc-z*.55*sc)};}
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_DOWN)return true;float best=1e9f;int bi=-1;for(int i=0;i<hp.size();i++){float dx=e.getX()-hp.get(i)[0],dy=e.getY()-hp.get(i)[1],dd=dx*dx+dy*dy;if(dd<best){best=dd;bi=i;}}if(bi>=0&&best<dp(45)*dp(45)){Object o=hit.get(bi);if(o instanceof SNode){SNode s=(SNode)o;sgInspector.setText(String.format(Locale.US,"PAD Z%d • XYZ %.1f / %.1f / %.1f mm\nFx %+.1f N | Fy %+.1f N | Fz %+.1f N | R %.1f N\nMx %+.1f | My %+.1f | Mz %+.1f Nmm\nDisp %.5f mm • actuator A%d",s.section+1,s.x,s.y,s.z,s.fx,s.fy,s.fz,s.r,s.mx,s.my,s.mz,s.disp,s.act+1));}else{SG q=(SG)o;sgInspector.setText(String.format(Locale.US,"%s • XYZ %.1f / %.1f / %.1f mm • nearest Z%d\nMapped Fx %+.1f N | Fy %+.1f N | Fz %+.1f N | R %.1f N\nDisplacement %.5f mm • predicted strain %.1f µε",q.id,q.x,q.y,q.z,q.zone+1,q.fx,q.fy,q.fz,q.r,q.disp,q.strain));}}return true;}
    @Override protected void onDraw(Canvas c){super.onDraw(c);hit.clear();hp.clear();t.setColor(Color.WHITE);t.setTextSize(dp(14));c.drawText("3D REAL PAD + STRAIN-GAUGE MAP",dp(14),dp(30),t);if(!solvedValid){t.setTextSize(dp(9));c.drawText("Press HESAPLA VE GÖSTER.",dp(14),dp(65),t);return;}int W=getWidth();float cx=W*.5f,cy=dp(300);double len=Math.max(1,qd(qLength)),sc=(W-dp(70))/len;p.setColor(Color.rgb(65,76,88));c.drawRoundRect(new RectF(dp(30),cy-dp(36),W-dp(30),cy+dp(36)),dp(36),dp(36),p);
      for(SNode s:solved){float[] q=pr(s.x,s.y,s.z,cx,cy,sc);p.setColor(Color.WHITE);c.drawCircle(q[0],q[1],dp(7),p);hit.add(s);hp.add(q);t.setTextSize(dp(6));t.setColor(Color.WHITE);c.drawText("Z"+(s.section+1),q[0]+dp(7),q[1],t);}
      for(SG g:gauges){float[] q=pr(g.x,g.y,g.z,cx,cy,sc);p.setColor(Color.rgb(247,207,77));c.drawRect(q[0]-dp(5),q[1]-dp(5),q[0]+dp(5),q[1]+dp(5),p);hit.add(g);hp.add(q);t.setTextSize(dp(6));t.setColor(Color.rgb(247,207,77));c.drawText(g.id,q[0]+dp(7),q[1],t);}
      t.setColor(Color.rgb(190,210,225));t.setTextSize(dp(7));c.drawText("White circles = real pad points • yellow squares = strain gauges",dp(14),dp(920),t);
    }
  }
}
