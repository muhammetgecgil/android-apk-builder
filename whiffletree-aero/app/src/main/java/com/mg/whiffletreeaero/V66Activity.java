package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V66Activity extends V65Activity {
  TextView result2DSummary;
  Result2DView result2D;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(8),dp(8),dp(8),dp(8));p.setBackground(bg(Color.rgb(2,16,27),14));
    p.addView(tx("2D HESAP SONUÇLARI — WHIFFLETREE",19,true,Color.WHITE));
    p.addView(tx("HESAPLA sonrası kuvvet, moment ve deplasman değerleri doğrudan şema üzerinde",9,false,Color.rgb(180,210,230)));
    result2DSummary=card("Fx/Fy/Fz, actuator ve layer değerlerini girip HESAPLA VE 2D/3D GÖSTER'e bas.",Color.rgb(15,49,69));p.addView(result2DSummary,lp());
    result2D=new Result2DView();p.addView(result2D,new LinearLayout.LayoutParams(-1,dp(1180)));
    root.addView(p,1,lp());
  }

  @Override void runPrimary(){
    super.runPrimary();
    update2DResult();
  }

  @Override void calculateAndShow(){
    super.calculateAndShow();
    update2DResult();
  }

  void update2DResult(){
    if(result2D==null)return;
    if(!solvedValid||cnodes==null||cnodes.isEmpty()){
      result2DSummary.setText("2D RESULT NOT READY — press HESAPLA VE 2D / 3D GÖSTER.");
      result2D.invalidate();return;
    }
    double sf=0,maxR=0,maxM=0,maxD=0;String crit="-";
    for(CNode n:cnodes){sf+=n.type.contains("ACTUATOR")?n.r:0;double mm=Math.sqrt(n.mx*n.mx+n.my*n.my+n.mz*n.mz);if(n.r>maxR){maxR=n.r;crit=n.id;}maxM=Math.max(maxM,mm);maxD=Math.max(maxD,n.disp);}
    result2DSummary.setText(String.format(Locale.US,"2D RESULT READY\n%d node • %d actuator • %d layer\nPeak force %.1f N • peak moment %.1f Nmm • peak displacement %.5f mm\nCritical: %s\nAll signed Fx/Fy/Fz values are drawn on the 2D model.",cnodes.size(),Math.max(1,qi(qActs,1,12)),Math.max(1,qi(qLayers,1,4)),maxR,maxM,maxD,crit));
    result2D.invalidate();
  }

  class Result2DView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    Result2DView(){super(V66Activity.this);setBackgroundColor(Color.rgb(1,10,18));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    void line(Canvas c,float x1,float y1,float x2,float y2,int col,float w){p.setColor(col);p.setStrokeWidth(dp(w));c.drawLine(x1,y1,x2,y2,p);}
    void label(Canvas c,String s,float x,float y,int col,float size){t.setColor(col);t.setTextSize(dp(size));c.drawText(s,x,y,t);}
    @Override protected void onDraw(Canvas c){
      super.onDraw(c);int W=getWidth();
      label(c,"CALCULATED 2D LOAD PATH",dp(14),dp(28),Color.WHITE,14);
      if(!solvedValid||cnodes==null||cnodes.isEmpty()){label(c,"Press HESAPLA VE 2D / 3D GÖSTER",dp(14),dp(65),Color.rgb(185,205,220),9);return;}
      float left=dp(24),right=W-dp(24),tankY=dp(135);double len=Math.max(1,qd(qLength));int lays=Math.max(1,qi(qLayers,1,4));int acts=Math.max(1,qi(qActs,1,12));
      p.setColor(Color.rgb(65,77,89));c.drawRoundRect(new RectF(left,tankY-dp(34),right,tankY+dp(34)),dp(32),dp(32),p);
      label(c,"EFT",left+dp(8),tankY+dp(5),Color.WHITE,8);

      HashMap<String,float[]> pos=new HashMap<>();
      for(CNode n:cnodes){
        float x=(float)(left+(n.x+len/2.0)/len*(right-left));
        float y;
        if(n.type.equals("PAD"))y=tankY;
        else if(n.type.equals("LOAD CELL"))y=dp(820);
        else if(n.type.contains("ACTUATOR"))y=dp(970);
        else y=dp(285)+(Math.max(1,n.layer)-1)*dp(150);
        pos.put(n.id,new float[]{x,y});
      }

      // draw load-path lines by actuator path and layer order
      for(int a=0;a<acts;a++){
        ArrayList<CNode> path=new ArrayList<>();for(CNode n:cnodes)if(n.act==a)path.add(n);
        Collections.sort(path,(u,v)->Integer.compare(u.layer,v.layer));
        CNode prev=null;for(CNode n:path){if(prev!=null){float[] q1=pos.get(prev.id),q2=pos.get(n.id);if(q1!=null&&q2!=null)line(c,q1[0],q1[1],q2[0],q2[1],Color.rgb(115,140,160),1.5f);}prev=n;}
      }

      // nodes + engineering values
      for(CNode n:cnodes){
        float[] q=pos.get(n.id);if(q==null)continue;
        int col=n.type.equals("PAD")?Color.WHITE:n.type.equals("LOAD CELL")?Color.rgb(247,207,77):n.type.contains("ACTUATOR")?(pActType.getSelectedItemPosition()==0?Color.rgb(67,190,113):Color.rgb(160,110,230)):Color.rgb(51,205,220);
        p.setColor(col);c.drawCircle(q[0],q[1],dp(n.type.contains("ACTUATOR")?8:6),p);
        label(c,n.id,q[0]+dp(7),q[1]-dp(24),col,5.8f);
        label(c,String.format(Locale.US,"Fx %+.0f",n.fx),q[0]+dp(7),q[1]-dp(11),Color.rgb(230,100,100),5.2f);
        label(c,String.format(Locale.US,"Fy %+.0f",n.fy),q[0]+dp(7),q[1]+dp(2),Color.rgb(100,220,135),5.2f);
        label(c,String.format(Locale.US,"Fz %+.0f",n.fz),q[0]+dp(7),q[1]+dp(15),Color.rgb(100,165,245),5.2f);
        label(c,String.format(Locale.US,"R %.0f N",n.r),q[0]+dp(7),q[1]+dp(28),Color.WHITE,5.2f);
        if(!n.type.equals("PAD")){
          double mm=Math.sqrt(n.mx*n.mx+n.my*n.my+n.mz*n.mz);
          label(c,String.format(Locale.US,"M %.0f Nmm",mm),q[0]+dp(7),q[1]+dp(41),Color.rgb(247,207,77),5.0f);
          label(c,String.format(Locale.US,"d %.4f mm",n.disp),q[0]+dp(7),q[1]+dp(54),Color.rgb(190,210,225),5.0f);
        }
      }

      // global result strip
      double sx=0,sy=0,sz=0,smx=0,smy=0,smz=0;for(SNode s:solved){sx+=s.fx;sy+=s.fy;sz+=s.fz;smx+=s.mx;smy+=s.my;smz+=s.mz;}
      float by=dp(1090);p.setColor(Color.rgb(15,38,55));c.drawRoundRect(new RectF(left,by-dp(55),right,by+dp(46)),dp(8),dp(8),p);
      label(c,String.format(Locale.US,"ΣFx %+.0f N   ΣFy %+.0f N   ΣFz %+.0f N",sx,sy,sz),left+dp(10),by-dp(25),Color.WHITE,6.3f);
      label(c,String.format(Locale.US,"ΣMx %+.0f   ΣMy %+.0f   ΣMz %+.0f Nmm",smx,smy,smz),left+dp(10),by,Color.rgb(247,207,77),6.0f);
      label(c,"Red=Fx  Green=Fy  Blue=Fz  Yellow=Moment",left+dp(10),by+dp(28),Color.rgb(190,210,225),5.8f);
    }
  }
}
