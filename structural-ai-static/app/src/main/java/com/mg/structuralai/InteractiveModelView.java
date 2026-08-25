package com.mg.structuralai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import java.util.Locale;

/** Lightweight offline engineering viewport: isometric mesh projection, touch picking, BC/load markers and result contours. */
public final class InteractiveModelView extends View {
    public interface PickListener { void onPick(MeshModel.V3 point, int vertexIndex); }
    public enum PickMode { NONE, SUPPORT, LOAD }
    private MeshModel model;
    private StaticFemSolver.Result result;
    private TetMeshData tetMesh;
    private PickListener listener;
    private PickMode mode=PickMode.NONE;
    private MeshModel.V3 supportPoint, loadPoint;
    private double yaw=0.65, pitch=-0.45;
    private float lastX,lastY;
    private boolean dragging=false;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);

    public InteractiveModelView(Context c){ super(c); setBackgroundColor(Color.rgb(7,15,28)); }
    public void setPickListener(PickListener l){listener=l;}
    public void setModel(MeshModel m){model=m; result=null; tetMesh=null; invalidate();}
    public void setResult(TetMeshData tm, StaticFemSolver.Result r){tetMesh=tm; result=r; invalidate();}
    public void setPickMode(PickMode m){mode=m; invalidate();}
    public PickMode getPickMode(){return mode;}
    public void setSupportPoint(MeshModel.V3 v){supportPoint=v; invalidate();}
    public void setLoadPoint(MeshModel.V3 v){loadPoint=v; invalidate();}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c); if(model==null||model.vertices.isEmpty()) {drawText(c,"Model yüklenmedi",24,38,Color.LTGRAY); return;}
        double[] box=projectBounds(); double sx=(getWidth()-32)/Math.max(box[2]-box[0],1e-9), sy=(getHeight()-48)/Math.max(box[3]-box[1],1e-9); double s=Math.min(sx,sy);
        double ox=getWidth()/2.0-s*(box[0]+box[2])/2.0, oy=getHeight()/2.0-s*(box[1]+box[3])/2.0;
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.2f); p.setColor(Color.rgb(95,145,175));
        int maxTri=Math.min(model.triangles.size(),5000);
        for(int ti=0;ti<maxTri;ti++){
            int[] t=model.triangles.get(ti); if(t.length<3) continue;
            double[] a=pr(model.vertices.get(t[0])), b=pr(model.vertices.get(t[1])), d=pr(model.vertices.get(t[2]));
            c.drawLine((float)(ox+s*a[0]),(float)(oy-s*a[1]),(float)(ox+s*b[0]),(float)(oy-s*b[1]),p);
            c.drawLine((float)(ox+s*b[0]),(float)(oy-s*b[1]),(float)(ox+s*d[0]),(float)(oy-s*d[1]),p);
            c.drawLine((float)(ox+s*d[0]),(float)(oy-s*d[1]),(float)(ox+s*a[0]),(float)(oy-s*a[1]),p);
        }
        if(result!=null && tetMesh!=null) drawResults(c,ox,oy,s);
        drawMarker(c,supportPoint,ox,oy,s,Color.CYAN,"MESNET");
        drawMarker(c,loadPoint,ox,oy,s,Color.YELLOW,"YÜK");
        drawText(c,"Mod: "+mode+"  • sürükle: döndür • dokun: seç",12,getHeight()-14,Color.LTGRAY);
    }

    private void drawResults(Canvas c,double ox,double oy,double s){
        double max=Math.max(result.maxVonMisesPa,1e-12); p.setStyle(Paint.Style.FILL);
        int n=Math.min(tetMesh.tets.size(),3500);
        for(int e=0;e<n;e++){
            int[] t=tetMesh.tets.get(e); MeshModel.V3 a=tetMesh.nodes.get(t[0]),b=tetMesh.nodes.get(t[1]),d=tetMesh.nodes.get(t[2]),q=tetMesh.nodes.get(t[3]);
            MeshModel.V3 m=new MeshModel.V3((a.x+b.x+d.x+q.x)/4,(a.y+b.y+d.y+q.y)/4,(a.z+b.z+d.z+q.z)/4);
            double[] z=pr(m); double r=Math.max(0,Math.min(1,result.elementVonMisesPa[e]/max));
            int col=Color.rgb((int)(255*r),(int)(220*(1-Math.abs(r-.5)*2)),(int)(255*(1-r)));
            p.setColor(col); c.drawCircle((float)(ox+s*z[0]),(float)(oy-s*z[1]),3.3f,p);
        }
        drawText(c,String.format(Locale.US,"Von Mises max %.3f MPa • Umax %.4f mm",result.maxVonMisesPa/1e6,result.maxDisplacementM*1000),12,22,Color.WHITE);
    }

    private void drawMarker(Canvas c,MeshModel.V3 v,double ox,double oy,double s,int color,String label){ if(v==null)return; double[] a=pr(v); float x=(float)(ox+s*a[0]),y=(float)(oy-s*a[1]); p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawCircle(x,y,9,p);drawText(c,label,x+12,y-8,color); }
    private void drawText(Canvas c,String text,float x,float y,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextSize(22);c.drawText(text,x,y,p);}

    @Override public boolean onTouchEvent(MotionEvent e){
        if(model==null)return true;
        if(e.getAction()==MotionEvent.ACTION_DOWN){lastX=e.getX();lastY=e.getY();dragging=false;return true;}
        if(e.getAction()==MotionEvent.ACTION_MOVE){float dx=e.getX()-lastX,dy=e.getY()-lastY;if(Math.abs(dx)+Math.abs(dy)>5)dragging=true;yaw+=dx*.008;pitch+=dy*.008;pitch=Math.max(-1.4,Math.min(1.4,pitch));lastX=e.getX();lastY=e.getY();invalidate();return true;}
        if(e.getAction()==MotionEvent.ACTION_UP && !dragging && mode!=PickMode.NONE){int idx=nearestVertex(e.getX(),e.getY());if(idx>=0&&listener!=null)listener.onPick(model.vertices.get(idx),idx);return true;}
        return true;
    }

    private int nearestVertex(float tx,float ty){double[] box=projectBounds();double sx=(getWidth()-32)/Math.max(box[2]-box[0],1e-9),sy=(getHeight()-48)/Math.max(box[3]-box[1],1e-9),s=Math.min(sx,sy);double ox=getWidth()/2.0-s*(box[0]+box[2])/2.0,oy=getHeight()/2.0-s*(box[1]+box[3])/2.0;int best=-1;double bd=Double.POSITIVE_INFINITY;for(int i=0;i<model.vertices.size();i++){double[] a=pr(model.vertices.get(i));double x=ox+s*a[0],y=oy-s*a[1],d=(x-tx)*(x-tx)+(y-ty)*(y-ty);if(d<bd){bd=d;best=i;}}return bd<2500?best:-1;}
    private double[] projectBounds(){double minx=Double.POSITIVE_INFINITY,miny=minx,maxx=-minx,maxy=-minx;for(MeshModel.V3 v:model.vertices){double[] a=pr(v);minx=Math.min(minx,a[0]);maxx=Math.max(maxx,a[0]);miny=Math.min(miny,a[1]);maxy=Math.max(maxy,a[1]);}return new double[]{minx,miny,maxx,maxy};}
    private double[] pr(MeshModel.V3 v){double cy=Math.cos(yaw),sy=Math.sin(yaw),cp=Math.cos(pitch),sp=Math.sin(pitch);double x=cy*v.x-sy*v.z,z=sy*v.x+cy*v.z,y=cp*v.y-sp*z;return new double[]{x,y};}
}
