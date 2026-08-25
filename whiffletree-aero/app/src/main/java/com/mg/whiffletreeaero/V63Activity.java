package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V63Activity extends V62Activity {
  TextView connectionSummary, selectedConnection;
  Connection2DView connection2D;
  Connection3DView connection3D;
  ArrayList<CNode> cnodes=new ArrayList<>();

  static class CNode {
    String id,type; int act,layer; double x,y,z,fx,fy,fz,r,mx,my,mz,disp,lc;
  }

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(8),dp(8),dp(8),dp(8));p.setBackground(bg(Color.rgb(2,16,27),14));
    p.addView(tx("LIVE NODE / CONNECTION FORCE INSPECTOR",20,true,Color.WHITE));
    p.addView(tx("HESAPLA VE GÖSTER sonrası her bağlantıdaki signed kuvvet, moment ve deplasmanı 2D/3D izle",9,false,Color.rgb(180,210,230)));
    connectionSummary=card("Press HESAPLA VE GÖSTER to build connection loads.",Color.rgb(15,49,69));p.addView(connectionSummary,lp());
    selectedConnection=card("Tap a node, beam/pivot, load-cell or actuator in either view.",Color.rgb(18,48,66));p.addView(selectedConnection,lp());
    connection2D=new Connection2DView();p.addView(connection2D,new LinearLayout.LayoutParams(-1,dp(900)));
    connection3D=new Connection3DView();p.addView(connection3D,new LinearLayout.LayoutParams(-1,dp(980)));
    root.addView(p,Math.min(2,root.getChildCount()),lp());
  }

  @Override void calculateAndShow(){
    super.calculateAndShow();
    if(solvedValid){buildConnections();connection2D.invalidate();connection3D.invalidate();}
  }

  void buildConnections(){
    cnodes.clear();
    int layersN=Math.max(1,qi(qLayers,1,4)),acts=Math.max(1,qi(qActs,1,12));
    double k=Math.max(1,qd(qStiffness));
    for(SNode s:solved){
      CNode pad=make("Z"+(s.section+1),"PAD",s.act,0,s.x,s.y,s.z,s.fx,s.fy,s.fz,k);cnodes.add(pad);
      double fx=s.fx,fy=s.fy,fz=s.fz;
      double px=s.x,py=s.y,pz=s.z;
      for(int l=1;l<=layersN;l++){
        double blend=Math.pow(0.58,l);
        double nx=s.x*blend, ny=s.y*blend, nz=s.z-80*l;
        CNode n=make("L"+l+"-S"+(s.section+1),l==layersN?"PIVOT":"BEAM NODE",s.act,l,nx,ny,nz,fx,fy,fz,k);cnodes.add(n);
        px=nx;py=ny;pz=nz;
      }
    }
    for(int a=0;a<acts;a++){
      double fx=0,fy=0,fz=0,x=0,y=0,z=-140;int count=0;
      for(SNode s:solved)if(s.act==a){fx+=s.fx;fy+=s.fy;fz+=s.fz;x+=s.x;y+=s.y;count++;}
      if(count>0){x/=count;y/=count;}
      CNode lc=make("LC"+(a+1),"LOAD CELL",a,layersN,x*.25,y*.25,z,fx,fy,fz,k);cnodes.add(lc);
      CNode act=make("ACT"+(a+1),qActType.getSelectedItemPosition()==0?"HYDRAULIC ACTUATOR":"ELECTRIC ACTUATOR",a,layersN+1,x*.18,y*.18,z-140,fx,fy,fz,k);cnodes.add(act);
    }
    double maxR=0,maxM=0,maxD=0;CNode crit=null;
    for(CNode n:cnodes){double mm=Math.sqrt(n.mx*n.mx+n.my*n.my+n.mz*n.mz);if(n.r>maxR){maxR=n.r;crit=n;}maxM=Math.max(maxM,mm);maxD=Math.max(maxD,n.disp);}
    connectionSummary.setText(String.format(Locale.US,"CONNECTION MODEL CALCULATED\n%d connection nodes • %d load zones • %d layers • %d actuators\nPeak connection force %.1f N • peak moment %.1f Nmm • peak displacement %.5f mm\nCritical node: %s\nTap any object in 2D or 3D to inspect signed Fx/Fy/Fz and load path.",cnodes.size(),solved.size(),layersN,acts,maxR,maxM,maxD,crit==null?"-":crit.id));
  }

  CNode make(String id,String type,int act,int layer,double x,double y,double z,double fx,double fy,double fz,double k){
    CNode n=new CNode();n.id=id;n.type=type;n.act=act;n.layer=layer;n.x=x;n.y=y;n.z=z;n.fx=fx;n.fy=fy;n.fz=fz;n.r=Math.sqrt(fx*fx+fy*fy+fz*fz);n.mx=y*fz-z*fy;n.my=z*fx-x*fz;n.mz=x*fy-y*fx;n.disp=n.r/k;n.lc=n.r;return n;
  }

  void inspect(CNode n){if(n==null)return;selectedConnection.setText(String.format(Locale.US,
    "%s — %s\nActuator path A%d • layer %d • XYZ %.1f / %.1f / %.1f mm\nFx %+,.1f N | Fy %+,.1f N | Fz %+,.1f N | R %.1f N\nMx %+,.1f | My %+,.1f | Mz %+,.1f Nmm\nDisplacement %.5f mm | load-cell basis %.1f N\nLoad path: EFT → pad → beam/pivot layers → load cell → actuator → strongback",
    n.id,n.type,n.act+1,n.layer,n.x,n.y,n.z,n.fx,n.fy,n.fz,n.r,n.mx,n.my,n.mz,n.disp,n.lc));}

  class Connection2DView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);ArrayList<CNode> hit=new ArrayList<>();ArrayList<float[]> hp=new ArrayList<>();
    Connection2DView(){super(V63Activity.this);setBackgroundColor(Color.rgb(2,12,20));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_DOWN)return true;float best=1e9f;int bi=-1;for(int i=0;i<hp.size();i++){float dx=e.getX()-hp.get(i)[0],dy=e.getY()-hp.get(i)[1],d=dx*dx+dy*dy;if(d<best){best=d;bi=i;}}if(bi>=0&&best<dp(40)*dp(40))inspect(hit.get(bi));return true;}
    @Override protected void onDraw(Canvas c){super.onDraw(c);hit.clear();hp.clear();t.setColor(Color.WHITE);t.setTextSize(dp(14));c.drawText("2D CONNECTION FORCE MAP",dp(14),dp(30),t);if(cnodes.isEmpty()){t.setTextSize(dp(9));c.drawText("Press HESAPLA VE GÖSTER.",dp(14),dp(65),t);return;}int W=getWidth();float left=dp(30),right=W-dp(30),tankY=dp(150);p.setColor(Color.rgb(68,80,92));c.drawRoundRect(new RectF(left,tankY-dp(34),right,tankY+dp(34)),dp(34),dp(34),p);double len=Math.max(1,qd(qLength));int layersN=Math.max(1,qi(qLayers,1,4));for(CNode n:cnodes){float x=(float)(left+(n.x+len/2)/len*(right-left));float y;if(n.type.equals("PAD"))y=tankY;else if(n.type.equals("LOAD CELL"))y=dp(620);else if(n.type.contains("ACTUATOR"))y=dp(740);else y=dp(260)+Math.max(0,n.layer-1)*dp(90);int col=n.type.equals("PAD")?Color.WHITE:n.type.equals("LOAD CELL")?Color.rgb(247,207,77):n.type.contains("ACTUATOR")?(qActType.getSelectedItemPosition()==0?Color.rgb(67,190,113):Color.rgb(160,110,230)):Color.rgb(51,205,220);p.setColor(col);c.drawCircle(x,y,dp(6),p);hit.add(n);hp.add(new float[]{x,y});t.setTextSize(dp(5.5f));t.setColor(col);c.drawText(n.id,x-dp(10),y-dp(10),t);t.setTextSize(dp(5));c.drawText(String.format(Locale.US,"%+.0f/%+.0f/%+.0f N",n.fx,n.fy,n.fz),x-dp(24),y+dp(18),t);}t.setColor(Color.rgb(190,210,225));t.setTextSize(dp(7));c.drawText("Tap any node. Signed values are Fx/Fy/Fz [N].",dp(14),dp(850),t);}
  }

  class Connection3DView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);ArrayList<CNode> hit=new ArrayList<>();ArrayList<float[]> hp=new ArrayList<>();
    Connection3DView(){super(V63Activity.this);setBackgroundColor(Color.rgb(1,10,18));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    float[] pr(CNode n,float cx,float cy,double sc){return new float[]{(float)(cx+(n.x-n.y)*.68*sc),(float)(cy+(n.x+n.y)*.18*sc-n.z*.55*sc)};}
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_DOWN)return true;float best=1e9f;int bi=-1;for(int i=0;i<hp.size();i++){float dx=e.getX()-hp.get(i)[0],dy=e.getY()-hp.get(i)[1],d=dx*dx+dy*dy;if(d<best){best=d;bi=i;}}if(bi>=0&&best<dp(45)*dp(45))inspect(hit.get(bi));return true;}
    @Override protected void onDraw(Canvas c){super.onDraw(c);hit.clear();hp.clear();t.setColor(Color.WHITE);t.setTextSize(dp(14));c.drawText("3D XYZ CONNECTION / LOAD-PATH VIEW",dp(14),dp(30),t);if(cnodes.isEmpty()){t.setTextSize(dp(9));c.drawText("Press HESAPLA VE GÖSTER.",dp(14),dp(65),t);return;}int W=getWidth();float cx=W*.5f,cy=dp(250);double len=Math.max(1,qd(qLength)),sc=(W-dp(70))/len;CNode prev=null;for(CNode n:cnodes){float[] q=pr(n,cx,cy,sc);int col=n.type.equals("PAD")?Color.WHITE:n.type.equals("LOAD CELL")?Color.rgb(247,207,77):n.type.contains("ACTUATOR")?(qActType.getSelectedItemPosition()==0?Color.rgb(67,190,113):Color.rgb(160,110,230)):Color.rgb(51,205,220);p.setColor(col);c.drawCircle(q[0],q[1],dp(6),p);hit.add(n);hp.add(q);t.setColor(col);t.setTextSize(dp(5.5f));c.drawText(n.id,q[0]+dp(6),q[1]-dp(5),t);if(prev!=null&&prev.act==n.act&&n.layer>=prev.layer){float[] a=pr(prev,cx,cy,sc);p.setStrokeWidth(dp(2));p.setColor(Color.rgb(130,150,165));c.drawLine(a[0],a[1],q[0],q[1],p);}prev=n;}t.setColor(Color.rgb(190,210,225));t.setTextSize(dp(7));c.drawText("White pad • cyan beam/pivot • yellow load-cell • green/purple actuator",dp(14),dp(920),t);}
  }
}
