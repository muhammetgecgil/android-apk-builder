package com.mg.structuralai;

import java.util.*;

/** Mid-span section extraction from tessellated geometry. Intersects surface triangles with a plane normal to the dominant axis. */
public final class CrossSectionAnalyzer {
    public static final class Loop { public final List<double[]> p; public final double area,perimeter,circularity; Loop(List<double[]> q,double a,double per){p=q;area=Math.abs(a);perimeter=per;circularity=per>0?4*Math.PI*Math.abs(a)/(per*per):0;} }
    public static final class Result { public final int majorAxis; public final List<Loop> loops; public final boolean closed; public final String summary; Result(int a,List<Loop> l,boolean c,String s){majorAxis=a;loops=l;closed=c;summary=s;} }
    private static final class Seg { double[] a,b; Seg(double[] x,double[] y){a=x;b=y;} }
    private CrossSectionAnalyzer(){}

    public static Result analyze(MeshModel m){
        double[] d={m.dx(),m.dy(),m.dz()}; int ax=0;if(d[1]>d[ax])ax=1;if(d[2]>d[ax])ax=2;
        double cut=(min(m,ax)+max(m,ax))*0.5, tol=Math.max(m.diagonal()*1e-7,1e-9);
        List<Seg> segs=new ArrayList<>();
        for(int[] t:m.triangles){ if(t.length<3)continue; MeshModel.V3 A=m.vertices.get(t[0]),B=m.vertices.get(t[1]),C=m.vertices.get(t[2]);
            List<double[]> q=new ArrayList<>(); edge(A,B,ax,cut,tol,q);edge(B,C,ax,cut,tol,q);edge(C,A,ax,cut,tol,q);dedupe(q,tol);
            if(q.size()==2)segs.add(new Seg(q.get(0),q.get(1)));
        }
        List<Loop> loops=stitch(segs,tol*10); boolean closed=!loops.isEmpty();
        return new Result(ax,loops,closed,"sectionLoops="+loops.size()+" | closed="+closed);
    }
    private static void edge(MeshModel.V3 a,MeshModel.V3 b,int ax,double c,double tol,List<double[]> out){double da=coord(a,ax)-c,db=coord(b,ax)-c;if(Math.abs(da)<tol&&Math.abs(db)<tol)return;if(da*db>0)return;double den=da-db;if(Math.abs(den)<1e-30)return;double t=da/den;if(t<-1e-9||t>1+1e-9)return;double x=a.x+t*(b.x-a.x),y=a.y+t*(b.y-a.y),z=a.z+t*(b.z-a.z);out.add(project(x,y,z,ax));}
    private static List<Loop> stitch(List<Seg> s,double tol){List<Loop> out=new ArrayList<>();boolean[] used=new boolean[s.size()];for(int i=0;i<s.size();i++)if(!used[i]){List<double[]> p=new ArrayList<>();used[i]=true;p.add(s.get(i).a);p.add(s.get(i).b);double[] cur=s.get(i).b;boolean progress=true;while(progress&&!near(cur,p.get(0),tol)){progress=false;for(int j=0;j<s.size();j++)if(!used[j]){if(near(cur,s.get(j).a,tol)){used[j]=true;cur=s.get(j).b;p.add(cur);progress=true;break;}if(near(cur,s.get(j).b,tol)){used[j]=true;cur=s.get(j).a;p.add(cur);progress=true;break;}}}if(p.size()>=4&&near(p.get(p.size()-1),p.get(0),tol)){double a=shoelace(p),per=perimeter(p);if(Math.abs(a)>tol*tol)out.add(new Loop(p,a,per));}}out.sort((a,b)->Double.compare(b.area,a.area));return out;}
    private static double shoelace(List<double[]> p){double s=0;for(int i=0;i<p.size()-1;i++)s+=p.get(i)[0]*p.get(i+1)[1]-p.get(i+1)[0]*p.get(i)[1];return 0.5*s;}
    private static double perimeter(List<double[]> p){double s=0;for(int i=0;i<p.size()-1;i++){double dx=p.get(i+1)[0]-p.get(i)[0],dy=p.get(i+1)[1]-p.get(i)[1];s+=Math.hypot(dx,dy);}return s;}
    private static void dedupe(List<double[]> q,double t){for(int i=q.size()-1;i>=0;i--)for(int j=0;j<i;j++)if(near(q.get(i),q.get(j),t)){q.remove(i);break;}}
    private static boolean near(double[] a,double[] b,double t){return Math.hypot(a[0]-b[0],a[1]-b[1])<=t;}
    private static double[] project(double x,double y,double z,int a){return a==0?new double[]{y,z}:a==1?new double[]{x,z}:new double[]{x,y};}
    private static double coord(MeshModel.V3 p,int a){return a==0?p.x:a==1?p.y:p.z;}
    private static double min(MeshModel m,int a){return a==0?m.minX:a==1?m.minY:m.minZ;} private static double max(MeshModel m,int a){return a==0?m.maxX:a==1?m.maxY:m.maxZ;}
}
