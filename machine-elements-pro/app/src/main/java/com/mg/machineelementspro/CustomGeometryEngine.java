package com.mg.machineelementspro;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CustomGeometryEngine {
    private CustomGeometryEngine() {}

    public static final class Result {
        public final String title, body, status, note;
        Result(String title,String body,String status,String note){this.title=title;this.body=body;this.status=status;this.note=note;}
    }

    public static Result shaft(String stationText,double span,double diameter,double sy,double torqueNm){
        if(span<=0||diameter<=0||sy<=0) throw new IllegalArgumentException("Span, diameter and Sy must be > 0");
        List<double[]> loads=parseTriples(stationText);
        if(loads.isEmpty()) throw new IllegalArgumentException("At least one station is required: x,Fy,Fz");
        double sumFy=0,sumFz=0,momFy=0,momFz=0;
        for(double[] a:loads){double x=a[0];if(x<0||x>span)throw new IllegalArgumentException("Station x must be inside span");sumFy+=a[1];sumFz+=a[2];momFy+=a[1]*x;momFz+=a[2]*x;}
        double By=momFy/span, Ay=sumFy-By, Bz=momFz/span, Az=sumFz-Bz;
        double maxM=0,critX=0;
        List<Double> xs=new ArrayList<>();xs.add(0.0);xs.add(span);for(double[] a:loads)xs.add(a[0]);
        for(double x:xs){double My=Ay*x, Mz=Az*x;for(double[] a:loads){if(a[0]<=x){My-=a[1]*(x-a[0]);Mz-=a[2]*(x-a[0]);}}double m=Math.hypot(My,Mz);if(m>maxM){maxM=m;critX=x;}}
        double d3=Math.pow(diameter,3);double sigma=32.0*maxM/(Math.PI*d3);double tau=16.0*Math.abs(torqueNm*1000.0)/(Math.PI*d3);double vm=Math.sqrt(sigma*sigma+3*tau*tau);double fos=vm==0?Double.POSITIVE_INFINITY:sy/vm;
        String body=f("Ay",Ay,"N")+"\n"+f("By",By,"N")+"\n"+f("Az",Az,"N")+"\n"+f("Bz",Bz,"N")+"\n"+f("Critical x",critX,"mm")+"\n"+f("Critical M",maxM/1000.0,"N·m")+"\n"+f("von Mises",vm,"MPa")+"\n"+f("FoS",fos,"");
        return new Result("User-defined shaft load stations",body,verdict(fos),"Each line: x,Fy,Fz. Loads are solved in two bending planes with simple supports at x=0 and x=L; torque is added to von Mises stress.");
    }

    public static Result boltGroup(String coordText,double fx,double fy,double mzNm,double boltDiameter,double allowShear){
        if(boltDiameter<=0||allowShear<=0)throw new IllegalArgumentException("Bolt diameter and allowable shear must be > 0");
        List<double[]> pts=parsePairs(coordText);if(pts.size()<2)throw new IllegalArgumentException("At least two bolt coordinates are required: x,y");
        double cx=0,cy=0;for(double[] p:pts){cx+=p[0];cy+=p[1];}cx/=pts.size();cy/=pts.size();
        double sumR2=0;for(double[] p:pts){double dx=p[0]-cx,dy=p[1]-cy;sumR2+=dx*dx+dy*dy;}if(sumR2<=0)throw new IllegalArgumentException("Bolt coordinates cannot all coincide");
        double directX=fx/pts.size(),directY=fy/pts.size(),M=mzNm*1000.0,maxF=0;int critical=-1;
        for(int i=0;i<pts.size();i++){double dx=pts.get(i)[0]-cx,dy=pts.get(i)[1]-cy;double k=M/sumR2;double tx=-k*dy,ty=k*dx;double res=Math.hypot(directX+tx,directY+ty);if(res>maxF){maxF=res;critical=i;}}
        double area=Math.PI*boltDiameter*boltDiameter/4.0;double tau=maxF/area;double fos=allowShear/tau;
        String body=f("Centroid x",cx,"mm")+"\n"+f("Centroid y",cy,"mm")+"\n"+"Bolt count: "+pts.size()+"\nCritical bolt: #"+(critical+1)+"\n"+f("Critical resultant",maxF,"N")+"\n"+f("Bolt shear",tau,"MPa")+"\n"+f("FoS",fos,"");
        return new Result("Coordinate bolt-group solver",body,verdict(fos),"Each line: x,y. Direct in-plane force and torsional moment about the bolt-group centroid are superposed elastically.");
    }

    public static Result selectPreferredShaft(double momentNm,double torqueNm,double sy,double targetFos){
        if(sy<=0||targetFos<=0)throw new IllegalArgumentException("Sy and target FoS must be > 0");
        double[] pref={8,10,12,15,16,18,20,22,25,28,30,32,35,38,40,42,45,48,50,55,60,65,70,75,80,85,90,95,100,110,120,130,140,150};
        double M=Math.abs(momentNm)*1000,T=Math.abs(torqueNm)*1000;double chosen=Double.NaN,chosenFos=0;
        for(double d:pref){double s=32*M/(Math.PI*Math.pow(d,3)),t=16*T/(Math.PI*Math.pow(d,3)),vm=Math.sqrt(s*s+3*t*t),fos=vm==0?Double.POSITIVE_INFINITY:sy/vm;if(fos>=targetFos){chosen=d;chosenFos=fos;break;}}
        if(Double.isNaN(chosen))return new Result("Preferred shaft selector","Required size exceeds internal preferred-size table.","NO SIZE FOUND","Expand the preferred-size library or reduce demand.");
        return new Result("Preferred shaft selector",f("Selected diameter",chosen,"mm")+"\n"+f("Achieved FoS",chosenFos,"")+"\n"+f("Target FoS",targetFos,""),"SELECTED","The first passing diameter in the preferred metric size table is selected. Advanced versions will connect this to material and stock databases.");
    }

    private static List<double[]> parseTriples(String text){List<double[]> out=new ArrayList<>();for(String raw:text.split("\\r?\\n")){String s=raw.trim();if(s.isEmpty())continue;String[] p=s.replace(';',',').split(",");if(p.length!=3)throw new IllegalArgumentException("Station format must be x,Fy,Fz");out.add(new double[]{num(p[0]),num(p[1]),num(p[2])});}return out;}
    private static List<double[]> parsePairs(String text){List<double[]> out=new ArrayList<>();for(String raw:text.split("\\r?\\n")){String s=raw.trim();if(s.isEmpty())continue;String[] p=s.replace(';',',').split(",");if(p.length!=2)throw new IllegalArgumentException("Bolt format must be x,y");out.add(new double[]{num(p[0]),num(p[1])});}return out;}
    private static double num(String s){try{return Double.parseDouble(s.trim().replace(',','.'));}catch(Exception e){throw new IllegalArgumentException("Invalid number: "+s);}}
    private static String verdict(double fos){if(Double.isInfinite(fos))return "NO LOAD";if(fos>=2)return "PASS – high margin";if(fos>=1.5)return "PASS";if(fos>=1)return "MARGINAL";return "FAIL";}
    private static String f(String n,double v,String u){return String.format(Locale.US,"%s: %.5g%s",n,v,u.isEmpty()?"":" "+u);}
}
