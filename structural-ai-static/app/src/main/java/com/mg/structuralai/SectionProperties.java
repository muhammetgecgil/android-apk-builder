package com.mg.structuralai;

import java.util.*;
import java.util.Locale;

/** Composite 2D section properties from extracted closed loops. First loop is material outer boundary; subsequent loops are voids. */
public final class SectionProperties {
    public static final class Result {
        public final boolean valid; public final double area,cx,cy,Ixx,Iyy,Ixy,I1,I2,thetaRad;
        Result(boolean v,double a,double x,double y,double ix,double iy,double ixy,double p1,double p2,double th){valid=v;area=a;cx=x;cy=y;Ixx=ix;Iyy=iy;Ixy=ixy;I1=p1;I2=p2;thetaRad=th;}
        public String summary(double scaleToM){
            if(!valid)return "section properties unavailable";
            double aMm2=area*scaleToM*scaleToM*1e6, f=Math.pow(scaleToM,4)*1e12;
            return String.format(Locale.US,"A=%.6g mm^2 | centroid=(%.6g, %.6g) mm | I1=%.6g mm^4 | I2=%.6g mm^4 | theta=%.3f deg",aMm2,cx*scaleToM*1000,cy*scaleToM*1000,I1*f,I2*f,Math.toDegrees(thetaRad));
        }
    }
    private static final class Raw {double A,CxN,CyN,Ix,Iy,Ixy;}
    private SectionProperties(){}

    public static Result compute(CrossSectionAnalyzer.Result s){
        if(s==null||!s.closed||s.loops.isEmpty())return invalid();
        double A=0,cxn=0,cyn=0,ix0=0,iy0=0,ixy0=0;
        for(int k=0;k<s.loops.size();k++){
            Raw r=raw(s.loops.get(k).p); double sign=k==0?1.0:-1.0; double aa=Math.abs(r.A);
            if(aa<=1e-20)continue;
            double cx=r.CxN/(6.0*r.A),cy=r.CyN/(6.0*r.A);
            double ix=Math.abs(r.Ix),iy=Math.abs(r.Iy),ixy=Math.copySign(Math.abs(r.Ixy),r.Ixy*r.A);
            A+=sign*aa;cxn+=sign*aa*cx;cyn+=sign*aa*cy;ix0+=sign*ix;iy0+=sign*iy;ixy0+=sign*ixy;
        }
        if(!(A>1e-20))return invalid();
        double cx=cxn/A,cy=cyn/A;
        double Ixx=ix0-A*cy*cy,Iyy=iy0-A*cx*cx,Ixy=ixy0-A*cx*cy;
        if(Ixx<0&&Math.abs(Ixx)<1e-10*Math.max(ix0,1))Ixx=0;if(Iyy<0&&Math.abs(Iyy)<1e-10*Math.max(iy0,1))Iyy=0;
        double avg=0.5*(Ixx+Iyy),rad=Math.sqrt(0.25*(Ixx-Iyy)*(Ixx-Iyy)+Ixy*Ixy);
        double i1=avg+rad,i2=avg-rad,theta=0.5*Math.atan2(-2.0*Ixy,Iyy-Ixx);
        return new Result(Double.isFinite(i1)&&i2>=-1e-12,A,cx,cy,Ixx,Iyy,Ixy,i1,Math.max(0,i2),theta);
    }
    private static Raw raw(List<double[]> p){Raw r=new Raw();for(int i=0;i<p.size()-1;i++){double x0=p.get(i)[0],y0=p.get(i)[1],x1=p.get(i+1)[0],y1=p.get(i+1)[1],c=x0*y1-x1*y0;r.A+=0.5*c;r.CxN+=(x0+x1)*c;r.CyN+=(y0+y1)*c;r.Ix+=(y0*y0+y0*y1+y1*y1)*c/12.0;r.Iy+=(x0*x0+x0*x1+x1*x1)*c/12.0;r.Ixy+=(2*x0*y0+x0*y1+x1*y0+2*x1*y1)*c/24.0;}return r;}
    private static Result invalid(){return new Result(false,0,0,0,0,0,0,0,0,0);}
}
