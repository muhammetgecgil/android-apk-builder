package com.mg.drawing2cad;

import android.graphics.*;
import java.util.*;

/** Minimal offline ASCII DXF parser for technical drawings.
 * Supports LINE, CIRCLE, ARC, LWPOLYLINE, TEXT/MTEXT and preserves layer names.
 */
public final class DxfVectorParser {
  public static abstract class Entity { public String layer="0"; }
  public static final class Ln extends Entity { public float x1,y1,x2,y2; }
  public static final class Circ extends Entity { public float x,y,r; }
  public static final class ArcE extends Entity { public float x,y,r,a1,a2; }
  public static final class Poly extends Entity { public final ArrayList<PointF> pts=new ArrayList<>(); public boolean closed; }
  public static final class Txt extends Entity { public float x,y,h=2.5f; public String text=""; }
  public static final class Doc {
    public final ArrayList<Entity> entities=new ArrayList<>();
    public final ArrayList<String> texts=new ArrayList<>();
    public float minX=Float.POSITIVE_INFINITY,minY=Float.POSITIVE_INFINITY,maxX=Float.NEGATIVE_INFINITY,maxY=Float.NEGATIVE_INFINITY;
    public int lines,circles,arcs,polylines,textCount;
    public String summary(){return lines+" çizgi • "+circles+" daire • "+arcs+" yay • "+polylines+" polyline • "+textCount+" metin";}
  }

  public static Doc parse(String s){
    Doc d=new Doc(); if(s==null)return d;
    String[] a=s.replace("\r","").split("\n");
    int i=0; while(i+1<a.length){
      String code=a[i].trim(), val=a[i+1].trim(); i+=2;
      if(!"0".equals(code))continue;
      String type=val.toUpperCase(Locale.ROOT);
      int start=i; while(i+1<a.length && !"0".equals(a[i].trim()))i+=2;
      int end=i;
      try{
        if("LINE".equals(type)){Ln e=new Ln(); e.layer=str(a,start,end,8,"0"); e.x1=num(a,start,end,10,0);e.y1=num(a,start,end,20,0);e.x2=num(a,start,end,11,0);e.y2=num(a,start,end,21,0);d.entities.add(e);d.lines++;bounds(d,e.x1,e.y1);bounds(d,e.x2,e.y2);}
        else if("CIRCLE".equals(type)){Circ e=new Circ();e.layer=str(a,start,end,8,"0");e.x=num(a,start,end,10,0);e.y=num(a,start,end,20,0);e.r=Math.abs(num(a,start,end,40,0));d.entities.add(e);d.circles++;bounds(d,e.x-e.r,e.y-e.r);bounds(d,e.x+e.r,e.y+e.r);}
        else if("ARC".equals(type)){ArcE e=new ArcE();e.layer=str(a,start,end,8,"0");e.x=num(a,start,end,10,0);e.y=num(a,start,end,20,0);e.r=Math.abs(num(a,start,end,40,0));e.a1=num(a,start,end,50,0);e.a2=num(a,start,end,51,360);d.entities.add(e);d.arcs++;bounds(d,e.x-e.r,e.y-e.r);bounds(d,e.x+e.r,e.y+e.r);}
        else if("LWPOLYLINE".equals(type)){Poly e=new Poly();e.layer=str(a,start,end,8,"0");e.closed=((int)num(a,start,end,70,0)&1)!=0;Float x=null;for(int j=start;j+1<end;j+=2){String c=a[j].trim(),v=a[j+1].trim();if("10".equals(c)){x=f(v);}else if("20".equals(c)&&x!=null){float y=f(v);e.pts.add(new PointF(x,y));bounds(d,x,y);x=null;}}if(e.pts.size()>1){d.entities.add(e);d.polylines++;}}
        else if("TEXT".equals(type)||"MTEXT".equals(type)){Txt e=new Txt();e.layer=str(a,start,end,8,"0");e.x=num(a,start,end,10,0);e.y=num(a,start,end,20,0);e.h=Math.max(.5f,num(a,start,end,40,2.5f));String t=str(a,start,end,"MTEXT".equals(type)?1:1,"");e.text=t.replace("\\P"," ").trim();d.entities.add(e);d.textCount++;if(!e.text.isEmpty())d.texts.add(e.text);bounds(d,e.x,e.y);}
      }catch(Exception ignored){}
    }
    if(!Float.isFinite(d.minX)){d.minX=d.minY=0;d.maxX=d.maxY=100;}
    return d;
  }

  static float num(String[]a,int s,int e,int code,float def){String x=str(a,s,e,code,null);return x==null?def:f(x);} static float f(String s){try{return Float.parseFloat(s.replace(',','.'));}catch(Exception e){return 0;}}
  static String str(String[]a,int s,int e,int code,String def){String c=String.valueOf(code);for(int i=s;i+1<e;i+=2)if(c.equals(a[i].trim()))return a[i+1].trim();return def;}
  static void bounds(Doc d,float x,float y){d.minX=Math.min(d.minX,x);d.maxX=Math.max(d.maxX,x);d.minY=Math.min(d.minY,y);d.maxY=Math.max(d.maxY,y);}

  public static Bitmap render(Doc d,int W,int H){
    W=Math.max(640,W);H=Math.max(480,H);Bitmap b=Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.WHITE);
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.2f);p.setColor(Color.BLACK);Paint tp=new Paint(Paint.ANTI_ALIAS_FLAG);tp.setColor(Color.rgb(110,110,110));tp.setTextSize(16);
    float dx=Math.max(1e-3f,d.maxX-d.minX),dy=Math.max(1e-3f,d.maxY-d.minY),pad=50;float sc=Math.min((W-2*pad)/dx,(H-2*pad)/dy);
    for(Entity q:d.entities){boolean helper=q.layer!=null&&(q.layer.toUpperCase(Locale.ROOT).contains("DIM")||q.layer.toUpperCase(Locale.ROOT).contains("CENTER")||q.layer.toUpperCase(Locale.ROOT).contains("TEXT")||q.layer.toUpperCase(Locale.ROOT).contains("HIDDEN"));p.setColor(helper?Color.rgb(175,175,175):Color.BLACK);p.setStrokeWidth(helper?1.2f:2.6f);
      if(q instanceof Ln){Ln e=(Ln)q;c.drawLine(X(e.x1),Y(e.y1),X(e.x2),Y(e.y2),p);}else if(q instanceof Circ){Circ e=(Circ)q;c.drawCircle(X(e.x),Y(e.y),e.r*sc,p);}else if(q instanceof ArcE){ArcE e=(ArcE)q;RectF r=new RectF(X(e.x-e.r),Y(e.y+e.r),X(e.x+e.r),Y(e.y-e.r));float sweep=e.a2-e.a1;if(sweep<=0)sweep+=360;c.drawArc(r,-e.a1,-sweep,false,p);}else if(q instanceof Poly){Poly e=(Poly)q;Path path=new Path();for(int k=0;k<e.pts.size();k++){PointF pt=e.pts.get(k);if(k==0)path.moveTo(X(pt.x),Y(pt.y));else path.lineTo(X(pt.x),Y(pt.y));}if(e.closed)path.close();c.drawPath(path,p);}else if(q instanceof Txt){Txt e=(Txt)q;tp.setTextSize(Math.max(12,e.h*sc*.6f));c.drawText(e.text,X(e.x),Y(e.y),tp);}
    }
    return b;
    float X(float x){return pad+(x-d.minX)*sc;} float Y(float y){return H-pad-(y-d.minY)*sc;}
  }

  private DxfVectorParser(){}
}
