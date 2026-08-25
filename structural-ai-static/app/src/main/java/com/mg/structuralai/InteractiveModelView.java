package com.mg.structuralai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lightweight offline engineering viewport: projection, multi-picking, FEM contours and deformed overlay. */
public final class InteractiveModelView extends View {
    public interface PickListener { void onPick(MeshModel.V3 point, int vertexIndex); }
    public enum PickMode { NONE, SUPPORT, LOAD }
    private MeshModel model; private StaticFemSolver.Result result; private TetMeshData tetMesh;
    private PickListener listener; private PickMode mode=PickMode.NONE;
    private final List<MeshModel.V3> supportPoints=new ArrayList<>(), loadPoints=new ArrayList<>();
    private double yaw=0.65,pitch=-0.45; private float lastX,lastY; private boolean dragging=false;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);

    public InteractiveModelView(Context c){super(c);setBackgroundColor(Color.rgb(7,15,28));}
    public void setPickListener(PickListener l){listener=l;}
    public void setModel(MeshModel m){model=m;result=null;tetMesh=null;supportPoints.clear();loadPoints.clear();invalidate();}
    public void setResult(TetMeshData tm,StaticFemSolver.Result r){tetMesh=tm;result=r;invalidate();}
    public void setPickMode(PickMode m){mode=m;invalidate();} public PickMode getPickMode(){return mode;}
    public void addSupportPoint(MeshModel.V3 v){if(v!=null)supportPoints.add(v);invalidate();}
    public void addLoadPoint(MeshModel.V3 v){if(v!=null)loadPoints.add(v);invalidate();}
    public void clearSupportPoints(){supportPoints.clear();invalidate();} public void clearLoadPoints(){loadPoints.clear();invalidate();}
    public void setSupportPoint(MeshModel.V3 v){supportPoints.clear();if(v!=null)supportPoints.add(v);invalidate();}
    public void setLoadPoint(MeshModel.V3 v){loadPoints.clear();if(v!=null)loadPoints.add(v);invalidate();}

    @Override protected void onDraw(Canvas c){super.onDraw(c);if(model==null||model.vertices.isEmpty()){drawText(c,"Model yüklenmedi",24,38,Color.LTGRAY);return;}
        double[] box=projectBounds();double sx=(getWidth()-32)/Math.max(box[2]-box[0],1e-9),sy=(getHeight()-48)/Math.max(box[3]-box[1],1e-9),s=Math.min(sx,sy);double ox=getWidth()/2.0-s*(box[0]+box[2])/2.0,oy=getHeight()/2.0-s*(box[1]+box[3])/2.0;
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.1f);p.setColor(Color.rgb(78,120,150));int maxTri=Math.min(model.triangles.size(),5000);
        for(int ti=0;ti<maxTri;ti++){int[] t=model.triangles.get(ti);if(t.length<3)continue;double[] a=pr(model.vertices.get(t[0])),b=pr(model.vertices.get(t[1])),d=pr(model.vertices.get(t[2]));c.drawLine((float)(ox+s*a[0]),(float)(oy-s*a[1]),(float)(ox+s*b[0]),(float)(oy-s*b[1]),p);c.drawLine((float)(ox+s*b[0]),(float)(oy-s*b[1]),(float)(ox+s*d[0]),(float)(oy-s*d[1]),p);c.drawLine((float)(ox+s*d[0]),(float)(oy-s*d[1]),(float)(ox+s*a[0]),(float)(oy-s*a[1]),p);}
        if(result!=null&&tetMesh!=null)drawResults(c,ox,oy,s);
        for(int i=0;i<supportPoints.size();i++)drawMarker(c,supportPoints.get(i),ox,oy,s,Color.CYAN,"S"+(i+1));
        for(int i=0;i<loadPoints.size();i++)drawMarker(c,loadPoints.get(i),ox,oy,s,Color.YELLOW,"L"+(i+1));
        drawText(c,"Mod: "+mode+" • sürükle: döndür • dokun: patch ekle",12,getHeight()-14,Color.LTGRAY);
    }

    private void drawResults(Canvas c,double ox,double oy,double s){double max=Math.max(result.maxVonMisesPa,1e-12);double modelDiag=Math.max(model.diagonal(),1e-12);double uMax=Math.max(result.maxDisplacementM,1e-20);double amp=Math.min(250.0,0.12*modelDiag/uMax);p.setStyle(Paint.Style.FILL);int n=Math.min(tetMesh.tets.size(),3500);
        for(int e=0;e<n;e++){int[] t=tetMesh.tets.get(e);MeshModel.V3 m=deformedCentroid(t,amp);double[] z=pr(m);double r=Math.max(0,Math.min(1,result.elementVonMisesPa[e]/max));int col=Color.rgb((int)(255*r),(int)(220*(1-Math.abs(r-.5)*2)),(int)(255*(1-r)));p.setColor(col);c.drawCircle((float)(ox+s*z[0]),(float)(oy-s*z[1]),3.4f,p);}
        drawText(c,String.format(Locale.US,"VM %.3f MPa • U %.4f mm • deform x%.1f",result.maxVonMisesPa/1e6,result.maxDisplacementM*1000,amp),12,22,Color.WHITE);
    }
    private MeshModel.V3 deformedCentroid(int[] t,double amp){double x=0,y=0,z=0;for(int n:t){MeshModel.V3 a=tetMesh.nodes.get(n);x+=a.x+amp*result.displacement[3*n];y+=a.y+amp*result.displacement[3*n+1];z+=a.z+amp*result.displacement[3*n+2];}return new MeshModel.V3(x/4,y/4,z/4);}
    private void drawMarker(Canvas c,MeshModel.V3 v,double ox,double oy,double s,int color,String label){if(v==null)return;double[] a=pr(v);float x=(float)(ox+s*a[0]),y=(float)(oy-s*a[1]);p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawCircle(x,y,8,p);drawText(c,label,x+9,y-7,color);}
    private void drawText(Canvas c,String text,float x,float y,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextSize(20);c.drawText(text,x,y,p);}
    @Override public boolean onTouchEvent(MotionEvent e){if(model==null)return true;if(e.getAction()==MotionEvent.ACTION_DOWN){lastX=e.getX();lastY=e.getY();dragging=false;return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){float dx=e.getX()-lastX,dy=e.getY()-lastY;if(Math.abs(dx)+Math.abs(dy)>5)dragging=true;yaw+=dx*.008;pitch+=dy*.008;pitch=Math.max(-1.4,Math.min(1.4,pitch));lastX=e.getX();lastY=e.getY();invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_UP&&!dragging&&mode!=PickMode.NONE){int idx=nearestVertex(e.getX(),e.getY());if(idx>=0&&listener!=null)listener.onPick(model.vertices.get(idx),idx);return true;}return true;}
    private int nearestVertex(float tx,float ty){double[] box=projectBounds();double sx=(getWidth()-32)/Math.max(box[2]-box[0],1e-9),sy=(getHeight()-48)/Math.max(box[3]-box[1],1e-9),s=Math.min(sx,sy);double ox=getWidth()/2.0-s*(box[0]+box[2])/2.0,oy=getHeight()/2.0-s*(box[1]+box[3])/2.0;int best=-1;double bd=Double.POSITIVE_INFINITY;for(int i=0;i<model.vertices.size();i++){double[] a=pr(model.vertices.get(i));double x=ox+s*a[0],y=oy-s*a[1],d=(x-tx)*(x-tx)+(y-ty)*(y-ty);if(d<bd){bd=d;best=i;}}return bd<2500?best:-1;}
    private double[] projectBounds(){double minx=Double.POSITIVE_INFINITY,miny=minx,maxx=-minx,maxy=-minx;for(MeshModel.V3 v:model.vertices){double[] a=pr(v);minx=Math.min(minx,a[0]);maxx=Math.max(maxx,a[0]);miny=Math.min(miny,a[1]);maxy=Math.max(maxy,a[1]);}return new double[]{minx,miny,maxx,maxy};}
    private double[] pr(MeshModel.V3 v){double cy=Math.cos(yaw),sy=Math.sin(yaw),cp=Math.cos(pitch),sp=Math.sin(pitch);double x=cy*v.x-sy*v.z,z=sy*v.x+cy*v.z,y=cp*v.y-sp*z;return new double[]{x,y};}
}
