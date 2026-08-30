package com.mg.structuralai;

import java.util.Locale;

/** Explicit physical load definition. Zero/default values are never promoted to a real load implicitly. */
public final class PhysicalLoadDefinition {
    public final double fx,fy,fz;
    public final double pressurePa;
    public final double mx,my,mz;
    public final double ax,ay,az;

    public PhysicalLoadDefinition(double fx,double fy,double fz,double pressurePa,
                                  double mx,double my,double mz,
                                  double ax,double ay,double az){
        this.fx=fx;this.fy=fy;this.fz=fz;this.pressurePa=pressurePa;
        this.mx=mx;this.my=my;this.mz=mz;this.ax=ax;this.ay=ay;this.az=az;
    }

    public static PhysicalLoadDefinition legacy(double fx,double fy,double fz,double pressurePa,boolean gravity){
        return new PhysicalLoadDefinition(fx,fy,fz,pressurePa,0,0,0,0,0,gravity?-9.80665:0);
    }

    public boolean hasForce(){return mag1(fx,fy,fz)>0;}
    public boolean hasPressure(){return Math.abs(pressurePa)>0;}
    public boolean hasMoment(){return mag1(mx,my,mz)>0;}
    public boolean hasAcceleration(){return mag1(ax,ay,az)>0;}
    public boolean hasAnyPhysicalLoad(){return hasForce()||hasPressure()||hasMoment()||hasAcceleration();}
    public boolean finite(){return finite(fx,fy,fz,pressurePa,mx,my,mz,ax,ay,az);}

    public String summary(){return String.format(Locale.US,
            "F=[%.6g, %.6g, %.6g] N | p=%.6g Pa | M=[%.6g, %.6g, %.6g] N·m | a=[%.6g, %.6g, %.6g] m/s²",
            fx,fy,fz,pressurePa,mx,my,mz,ax,ay,az);}

    private static double mag1(double x,double y,double z){return Math.abs(x)+Math.abs(y)+Math.abs(z);}
    private static boolean finite(double...a){for(double x:a)if(!Double.isFinite(x))return false;return true;}
}
