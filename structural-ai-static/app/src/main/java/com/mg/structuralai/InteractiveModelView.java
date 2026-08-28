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

/** Offline engineering viewport with real TET4 mesh, selectable FEM fields, extrema markers, probe and deformed overlay. */
public final class InteractiveModelView extends View {
    public interface PickListener { void onPick(MeshModel.V3 point, int vertexIndex); }
    public enum PickMode { NONE, SUPPORT, LOAD }
    public enum ResultField { MESH_QUALITY, U_TOTAL, U_X, U_Y, U_Z, VON_MISES, PRINCIPAL_E1, PRINCIPAL_E2, PRINCIPAL_E3, REACTION_MAG }
    private MeshModel model; private StaticFemSolver.Result result; private TetMeshData tetMesh;
    private PickListener listener; private PickMode mode=PickMode.NONE; private ResultField resultField=ResultField.VON_MISES;
    private final List<MeshModel.V3> supportPoints=new ArrayList<>(), loadPoints=new ArrayList<>();
    private double yaw=0.65,pitch=-0.45; private float lastX,lastY; private boolean dragging=false; private long downMs=0;
    private int probeElement=-1,probeNode=-1;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);

    public InteractiveModelView(Context c){super(c);setBackgroundColor(Color.rgb(7,15,28));}
    public void setPickListener(PickListener l){listener=l;}
    public void setModel(MeshModel m){model=m;result=null;tetMesh=null;supportPoints.clear();loadPoints.clear();resultField=ResultField.VON_MISES;probeElement=probeNode=-1;invalidate();}
    public void setResult(TetMeshData tm,StaticFemSolver.Result r){tetMesh=tm;result=r;resultField=ResultField.MESH_QUALITY;probeElement=probeNode=-1;invalidate();}
    public void setResultField(ResultField f){if(f!=null){resultField=f;probeElement=probeNode=-1;invalidate();}}
    public ResultField getResultField(){return resultField;}
    public void cycleResultField(){ResultField[] a=ResultField.values();resultField=a[(resultField.ordinal()+1)%a.length];probeElement=probeNode=-1;invalidate();}
    public void setPickMode(PickMode m){mode=m;invalidate();} public PickMode getPickMode(){return mode;}
    public void addSupportPoint(MeshModel.V3 v){if(v!=null)supportPoints.add(v);invalidate();}
    public void addLoadPoint(MeshModel.V3 v){if(v!=null)loadPoints.add(v);invalidate();}
    public void clearSupportPoints(){supportPoints.clear();invalidate();} public void clearLoadPoints(){loadPoints.clear();invalidate();}
    public void setSupportPoint(MeshModel.V3 v){supportPoints.clear();if(v!=null)supportPoints.add(v);invalidate();}
    public void setLoadPoint(MeshModel.V3 v){loadPoints.clear();if(v!=null)loadPoints.add(v);invalidate();}

    @Override protected void onDraw(Canvas c){super.onDraw(c);if(model==null||model.vertices.isEmpty()){drawText(c,"Model yüklenmedi",24,38,Color.LTGRAY);return;}
        double[] box=projectBounds();double sx=(getWidth()-32)/Math.max(box[2]-box[0],1e-9),sy=(getHeight()-48)/Math.max(box[3]-box[1],1e-9),s=Math.min(sx,sy);double ox=getWidth()/2.0-s*(box[0]+box[2])/2.0,oy=getHeight()/2.0-s*(box[1]+box[3])/2.0;
        if(resultField!=ResultField.MESH_QUALITY||tetMesh==null){
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.1f);p.setColor(Color.rgb(78,120,150));int maxTri=Math.min(model.triangles.size(),5000);
            for(int ti=0;ti<maxTri;ti++){int[] t=model.triangles.get(ti);if(t.length<3)continue;double[] a=pr(model.vertices.get(t[0])),b=pr(model.vertices.get(t[1])),d=pr(model.vertices.get(t[2]));c.drawLine((float)(ox+s*a[0]),(float)(oy-s*a[1]),(float)(ox+s*b[0]),(float)(oy-s*b[1]),p);c.drawLine((float)(ox+s*b[0]),(float)(oy-s*b[1]),(float)(ox+s*d[0]),(float)(oy-s*d[1]),p);c.drawLine((float)(ox+s*d[0]),(float)(oy-s*d[1]),(float)(ox+s*a[0]),(float)(oy-s*a[1]),p);}
        }
        if(result!=null&&tetMesh!=null)drawResults(c,ox,oy,s);
        for(int i=0;i<supportPoints.size();i++)drawMarker(c,supportPoints.get(i),ox,oy,s,Color.CYAN,"S"+(i+1));
        for(int i=0;i<loadPoints.size();i++)drawMarker(c,loadPoints.get(i),ox,oy,s,Color.YELLOW,"L"+(i+1));
        String hint=result!=null&&mode==PickMode.NONE?"dokun: MESH/sonuç alanı • uzun bas: probe • sürükle: döndür":"Mod: "+mode+" • sürükle: döndür • dokun: patch ekle";
        drawSmallText(c,hint,12,getHeight()-12,Color.LTGRAY);
    }

    private void drawResults(Canvas c,double ox,double oy,double s){
        boolean meshView=resultField==ResultField.MESH_QUALITY;
        double modelDiag=Math.max(model.diagonal(),1e-12),uMax=Math.max(result.maxDisplacementM,1e-20);double amp=meshView?0.0:Math.min(250.0,0.12*modelDiag/uMax);int n=Math.min(tetMesh.tets.size(),2500);
        boolean elem=isElementField();double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;int iMin=-1,iMax=-1;
        int count=elem?n:tetMesh.nodes.size();for(int i=0;i<count;i++){double v=elem?elementField(i):nodeField(i);if(v<min){min=v;iMin=i;}if(v>max){max=v;iMax=i;}}
        if(!Double.isFinite(min)||!Double.isFinite(max)){min=0;max=1;}double range=Math.max(max-min,1e-30);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(meshView?1.15f:0.85f);
        for(int e=0;e<n;e++){int[] t=tetMesh.tets.get(e);double v=elem?elementField(e):tetAverage(t);double q=meshView?Math.max(0,Math.min(1,v)):Math.max(0,Math.min(1,(v-min)/range));p.setColor(fieldColor(q));drawTetEdges(c,t,amp,ox,oy,s);}
        if(!meshView){p.setStyle(Paint.Style.FILL);for(int e=0;e<n;e++){int[] t=tetMesh.tets.get(e);MeshModel.V3 m=deformedCentroid(t,amp);double[] z=pr(m);double v=elem?elementField(e):tetAverage(t);double q=Math.max(0,Math.min(1,(v-min)/range));p.setColor(fieldColor(q));c.drawCircle((float)(ox+s*z[0]),(float)(oy-s*z[1]),2.0f,p);}}
        drawText(c,fieldLabel()+"  MAX "+fmtField(max)+"  MIN "+fmtField(min),12,22,Color.WHITE);
        drawSmallText(c,meshView?String.format(Locale.US,"TET4=%d • nodes=%d • undeformed engineering mesh",tetMesh.tets.size(),tetMesh.nodes.size()):String.format(Locale.US,"TET=%d • Umax %.4f mm • deform x%.1f",tetMesh.tets.size(),result.maxDisplacementM*1000,amp),12,43,Color.LTGRAY);
        drawLegend(c,min,max);
        if(iMax>=0)drawResultMarker(c,resultPoint(iMax,elem,amp),ox,oy,s,Color.WHITE,"MAX");
        if(iMin>=0)drawResultMarker(c,resultPoint(iMin,elem,amp),ox,oy,s,Color.LTGRAY,"MIN");
        if((elem&&probeElement>=0&&probeElement<n)||(!elem&&probeNode>=0&&probeNode<tetMesh.nodes.size())){int idx=elem?probeElement:probeNode;double val=elem?elementField(idx):nodeField(idx);MeshModel.V3 pt=resultPoint(idx,elem,amp);drawResultMarker(c,pt,ox,oy,s,Color.YELLOW,"P");drawSmallText(c,"PROBE "+fmtField(val),12,62,Color.YELLOW);}
    }

    private boolean isElementField(){return resultField==ResultField.MESH_QUALITY||resultField==ResultField.VON_MISES||resultField==ResultField.PRINCIPAL_E1||resultField==ResultField.PRINCIPAL_E2||resultField==ResultField.PRINCIPAL_E3;}
    private double nodeField(int i){double ux=result.displacement[3*i],uy=result.displacement[3*i+1],uz=result.displacement[3*i+2];switch(resultField){case U_X:return ux;case U_Y:return uy;case U_Z:return uz;case REACTION_MAG:{double rx=result.reactions[3*i],ry=result.reactions[3*i+1],rz=result.reactions[3*i+2];return Math.sqrt(rx*rx+ry*ry+rz*rz);}case U_TOTAL:return Math.sqrt(ux*ux+uy*uy+uz*uz);default:return 0;}}
    private double elementField(int e){if(resultField==ResultField.MESH_QUALITY)return tetQuality(tetMesh.tets.get(e));if(resultField==ResultField.VON_MISES)return result.elementVonMisesPa[e];int[] t=tetMesh.tets.get(e);double[] ue=new double[12];for(int a=0;a<4;a++)for(int k=0;k<3;k++)ue[3*a+k]=result.displacement[3*t[a]+k];double[] eps=Tet4Element.strain(tetMesh.nodes.get(t[0]),tetMesh.nodes.get(t[1]),tetMesh.nodes.get(t[2]),tetMesh.nodes.get(t[3]),ue);double[] pe=Tet4Element.principalValues(eps,true);if(resultField==ResultField.PRINCIPAL_E1)return pe[0];if(resultField==ResultField.PRINCIPAL_E2)return pe[1];return pe[2];}
    private double tetQuality(int[] t){MeshModel.V3 a=tetMesh.nodes.get(t[0]),b=tetMesh.nodes.get(t[1]),c=tetMesh.nodes.get(t[2]),d=tetMesh.nodes.get(t[3]);double v=Math.abs(dot(sub(b,a),cross(sub(c,a),sub(d,a))))/6.0;double sum=edge2(a,b)+edge2(a,c)+edge2(a,d)+edge2(b,c)+edge2(b,d)+edge2(c,d);if(v<=1e-30||sum<=1e-30)return 0;double q=12.0*Math.pow(3.0*v,2.0/3.0)/sum;return Math.max(0,Math.min(1,q));}
    private static MeshModel.V3 sub(MeshModel.V3 a,MeshModel.V3 b){return new MeshModel.V3(a.x-b.x,a.y-b.y,a.z-b.z);}private static MeshModel.V3 cross(MeshModel.V3 a,MeshModel.V3 b){return new MeshModel.V3(a.y*b.z-a.z*b.y,a.z*b.x-a.x*b.z,a.x*b.y-a.y*b.x);}private static double dot(MeshModel.V3 a,MeshModel.V3 b){return a.x*b.x+a.y*b.y+a.z*b.z;}private static double edge2(MeshModel.V3 a,MeshModel.V3 b){double x=a.x-b.x,y=a.y-b.y,z=a.z-b.z;return x*x+y*y+z*z;}
    private double tetAverage(int[] t){double q=0;for(int i:t)q+=nodeField(i);return q/4.0;}
    private String fieldLabel(){switch(resultField){case MESH_QUALITY:return "TET4 MESH QUALITY";case U_TOTAL:return "U TOTAL";case U_X:return "UX";case U_Y:return "UY";case U_Z:return "UZ";case REACTION_MAG:return "REACTION |R|";case PRINCIPAL_E1:return "PRINCIPAL STRAIN E1";case PRINCIPAL_E2:return "PRINCIPAL STRAIN E2";case PRINCIPAL_E3:return "PRINCIPAL STRAIN E3";default:return "VON MISES";}}
    private String fmtField(double v){if(resultField==ResultField.MESH_QUALITY)return String.format(Locale.US,"%.1f%%",v*100);if(resultField==ResultField.VON_MISES)return String.format(Locale.US,"%.3f MPa",v/1e6);if(resultField==ResultField.REACTION_MAG)return String.format(Locale.US,"%.3f N",v);if(resultField==ResultField.PRINCIPAL_E1||resultField==ResultField.PRINCIPAL_E2||resultField==ResultField.PRINCIPAL_E3)return String.format(Locale.US,"%.3f µε",v*1e6);return String.format(Locale.US,"%.5f mm",v*1000);}
    private int fieldColor(double q){q=Math.max(0,Math.min(1,q));return Color.rgb((int)(255*q),(int)(220*(1-Math.abs(q-.5)*2)),(int)(255*(1-q)));}
    private void drawLegend(Canvas c,double min,double max){float x=getWidth()-34,y0=58,h=Math.min(160,getHeight()/3f);for(int i=0;i<40;i++){double q=i/39.0;p.setColor(fieldColor(1-q));p.setStyle(Paint.Style.FILL);c.drawRect(x,y0+i*h/40f,x+16,y0+(i+1)*h/40f,p);}drawSmallText(c,"MAX",x-4,y0-6,Color.WHITE);drawSmallText(c,"MIN",x-4,y0+h+14,Color.WHITE);}
    private void drawTetEdges(Canvas c,int[] t,double amp,double ox,double oy,double s){int[][] edge={{0,1},{0,2},{0,3},{1,2},{1,3},{2,3}};for(int[] e:edge){MeshModel.V3 a=deformedNode(t[e[0]],amp),b=deformedNode(t[e[1]],amp);double[] pa=pr(a),pb=pr(b);c.drawLine((float)(ox+s*pa[0]),(float)(oy-s*pa[1]),(float)(ox+s*pb[0]),(float)(oy-s*pb[1]),p);}}
    private MeshModel.V3 deformedNode(int n,double amp){MeshModel.V3 a=tetMesh.nodes.get(n);return new MeshModel.V3(a.x+amp*result.displacement[3*n],a.y+amp*result.displacement[3*n+1],a.z+amp*result.displacement[3*n+2]);}
    private MeshModel.V3 deformedCentroid(int[] t,double amp){double x=0,y=0,z=0;for(int n:t){MeshModel.V3 a=deformedNode(n,amp);x+=a.x;y+=a.y;z+=a.z;}return new MeshModel.V3(x/4,y/4,z/4);}
    private MeshModel.V3 resultPoint(int idx,boolean elem,double amp){return elem?deformedCentroid(tetMesh.tets.get(idx),amp):deformedNode(idx,amp);}
    private void drawResultMarker(Canvas c,MeshModel.V3 v,double ox,double oy,double s,int color,String label){if(v==null)return;double[] a=pr(v);float x=(float)(ox+s*a[0]),y=(float)(oy-s*a[1]);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.5f);p.setColor(color);c.drawCircle(x,y,7,p);drawSmallText(c,label,x+8,y-7,color);}
    private void drawMarker(Canvas c,MeshModel.V3 v,double ox,double oy,double s,int color,String label){if(v==null)return;double[] a=pr(v);float x=(float)(ox+s*a[0]),y=(float)(oy-s*a[1]);p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawCircle(x,y,8,p);drawText(c,label,x+9,y-7,color);}
    private void drawText(Canvas c,String text,float x,float y,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextSize(20);c.drawText(text,x,y,p);}
    private void drawSmallText(Canvas c,String text,float x,float y,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextSize(12);c.drawText(text,x,y,p);}
    @Override public boolean onTouchEvent(MotionEvent e){if(model==null)return true;if(e.getAction()==MotionEvent.ACTION_DOWN){lastX=e.getX();lastY=e.getY();dragging=false;downMs=System.currentTimeMillis();return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){float dx=e.getX()-lastX,dy=e.getY()-lastY;if(Math.abs(dx)+Math.abs(dy)>5)dragging=true;yaw+=dx*.008;pitch+=dy*.008;pitch=Math.max(-1.4,Math.min(1.4,pitch));lastX=e.getX();lastY=e.getY();invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_UP&&!dragging){if(mode!=PickMode.NONE){int idx=nearestVertex(e.getX(),e.getY());if(idx>=0&&listener!=null)listener.onPick(model.vertices.get(idx),idx);}else if(result!=null){if(System.currentTimeMillis()-downMs>=450)probeAt(e.getX(),e.getY());else cycleResultField();}return true;}return true;}
    private void probeAt(float tx,float ty){if(isElementField()){probeElement=nearestElement(tx,ty);probeNode=-1;}else{probeNode=nearestTetNode(tx,ty);probeElement=-1;}invalidate();}
    private int nearestElement(float tx,float ty){double[] tr=screenTransform();double s=tr[0],ox=tr[1],oy=tr[2];int n=Math.min(tetMesh.tets.size(),2500),best=-1;double bd=Double.POSITIVE_INFINITY;for(int i=0;i<n;i++){MeshModel.V3 m=deformedCentroid(tetMesh.tets.get(i),0);double[] a=pr(m);double x=ox+s*a[0],y=oy-s*a[1],d=(x-tx)*(x-tx)+(y-ty)*(y-ty);if(d<bd){bd=d;best=i;}}return bd<3600?best:-1;}
    private int nearestTetNode(float tx,float ty){double[] tr=screenTransform();double s=tr[0],ox=tr[1],oy=tr[2];int best=-1;double bd=Double.POSITIVE_INFINITY;for(int i=0;i<tetMesh.nodes.size();i++){double[] a=pr(tetMesh.nodes.get(i));double x=ox+s*a[0],y=oy-s*a[1],d=(x-tx)*(x-tx)+(y-ty)*(y-ty);if(d<bd){bd=d;best=i;}}return bd<3600?best:-1;}
    private int nearestVertex(float tx,float ty){double[] tr=screenTransform();double s=tr[0],ox=tr[1],oy=tr[2];int best=-1;double bd=Double.POSITIVE_INFINITY;for(int i=0;i<model.vertices.size();i++){double[] a=pr(model.vertices.get(i));double x=ox+s*a[0],y=oy-s*a[1],d=(x-tx)*(x-tx)+(y-ty)*(y-ty);if(d<bd){bd=d;best=i;}}return bd<2500?best:-1;}
    private double[] screenTransform(){double[] box=projectBounds();double sx=(getWidth()-32)/Math.max(box[2]-box[0],1e-9),sy=(getHeight()-48)/Math.max(box[3]-box[1],1e-9),s=Math.min(sx,sy);double ox=getWidth()/2.0-s*(box[0]+box[2])/2.0,oy=getHeight()/2.0-s*(box[1]+box[3])/2.0;return new double[]{s,ox,oy};}
    private double[] projectBounds(){double minx=Double.POSITIVE_INFINITY,miny=minx,maxx=-minx,maxy=-minx;for(MeshModel.V3 v:model.vertices){double[] a=pr(v);minx=Math.min(minx,a[0]);maxx=Math.max(maxx,a[0]);miny=Math.min(miny,a[1]);maxy=Math.max(maxy,a[1]);}return new double[]{minx,miny,maxx,maxy};}
    private double[] pr(MeshModel.V3 v){double cy=Math.cos(yaw),sy=Math.sin(yaw),cp=Math.cos(pitch),sp=Math.sin(pitch);double x=cy*v.x-sy*v.z,z=sy*v.x+cy*v.z,y=cp*v.y-sp*z;return new double[]{x,y};}
}
