package com.mg.quakewatch;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class FaultModel {
    public static final class Segment {
        public final String name, system, type;
        public final double[][] pts;
        Segment(String n,String s,String t,double[][] p){name=n;system=s;type=t;pts=p;}
    }

    // Schematic major active-fault traces for analysis/navigation. They are intentionally
    // simplified and must not be used as an official engineering fault map.
    private static final Segment[] S = new Segment[]{
        new Segment("KAF • Marmara Batı","Kuzey Anadolu Fayı","sağ yanal doğrultu atımlı",new double[][]{{40.79,27.45},{40.75,28.05},{40.78,28.62},{40.84,29.15}}),
        new Segment("KAF • İzmit","Kuzey Anadolu Fayı","sağ yanal doğrultu atımlı",new double[][]{{40.72,29.20},{40.72,29.75},{40.74,30.20},{40.76,30.55}}),
        new Segment("KAF • Düzce-Bolu","Kuzey Anadolu Fayı","sağ yanal doğrultu atımlı",new double[][]{{40.78,30.55},{40.84,31.15},{40.78,31.75},{40.69,32.30}}),
        new Segment("KAF • Orta Anadolu","Kuzey Anadolu Fayı","sağ yanal doğrultu atımlı",new double[][]{{40.70,32.30},{40.85,33.20},{40.98,34.25},{40.97,35.45},{40.75,36.40}}),
        new Segment("KAF • Tokat-Erzincan","Kuzey Anadolu Fayı","sağ yanal doğrultu atımlı",new double[][]{{40.73,36.40},{40.55,37.30},{40.35,38.25},{39.95,39.50}}),
        new Segment("KAF • Erzincan-Karlıova","Kuzey Anadolu Fayı","sağ yanal doğrultu atımlı",new double[][]{{39.95,39.50},{39.75,40.45},{39.55,41.20}}),
        new Segment("DAF • Karlıova-Palu","Doğu Anadolu Fayı","sol yanal doğrultu atımlı",new double[][]{{39.55,41.20},{39.15,40.75},{38.75,40.20},{38.45,39.70}}),
        new Segment("DAF • Palu-Pütürge","Doğu Anadolu Fayı","sol yanal doğrultu atımlı",new double[][]{{38.45,39.70},{38.25,39.05},{38.05,38.35},{37.85,37.65}}),
        new Segment("DAF • Pazarcık-Amanos","Doğu Anadolu Fayı","sol yanal doğrultu atımlı",new double[][]{{37.85,37.65},{37.55,37.15},{37.20,36.70},{36.75,36.30},{36.25,36.05}}),
        new Segment("Malatya-Ovacık","Malatya-Ovacık Fay Zonu","doğrultu atımlı",new double[][]{{39.30,39.30},{38.95,38.55},{38.55,37.80},{38.10,37.25}}),
        new Segment("Gediz Grabeni","Batı Anadolu","normal fay",new double[][]{{38.70,27.25},{38.65,28.00},{38.55,28.75},{38.45,29.40}}),
        new Segment("Büyük Menderes Grabeni","Batı Anadolu","normal fay",new double[][]{{37.88,27.20},{37.88,28.00},{37.75,28.80},{37.65,29.35}}),
        new Segment("Küçük Menderes Grabeni","Batı Anadolu","normal fay",new double[][]{{38.15,27.15},{38.15,27.85},{38.05,28.45}}),
        new Segment("Fethiye-Burdur Fay Zonu","GB Anadolu","normal/oblik fay",new double[][]{{36.65,29.10},{37.10,29.55},{37.55,30.00},{38.00,30.35}}),
        new Segment("Ecemiş Fayı","Orta Anadolu","doğrultu atımlı",new double[][]{{36.80,34.60},{37.55,34.75},{38.20,34.95},{38.85,35.10}}),
        new Segment("Tuz Gölü Fay Zonu","Orta Anadolu","normal/oblik fay",new double[][]{{38.05,33.30},{38.55,33.55},{39.10,33.80},{39.65,34.10}})
    };

    public static Segment[] segments(){return S;}

    public static String json() throws Exception{
        JSONArray a=new JSONArray();
        for(Segment s:S){
            JSONObject o=new JSONObject();o.put("name",s.name);o.put("system",s.system);o.put("type",s.type);
            JSONArray p=new JSONArray();for(double[] q:s.pts){JSONArray x=new JSONArray();x.put(q[0]);x.put(q[1]);p.put(x);}o.put("pts",p);a.put(o);
        }
        return a.toString();
    }

    public static final class Nearest { public final String name,system,type; public final double km; Nearest(Segment s,double d){name=s.name;system=s.system;type=s.type;km=d;} }

    public static Nearest nearest(double lat,double lon){
        Segment best=S[0];double bd=Double.MAX_VALUE;
        for(Segment s:S){for(int i=0;i<s.pts.length-1;i++){double d=pointSegmentKm(lat,lon,s.pts[i][0],s.pts[i][1],s.pts[i+1][0],s.pts[i+1][1]);if(d<bd){bd=d;best=s;}}}
        return new Nearest(best,bd);
    }

    private static double pointSegmentKm(double lat,double lon,double aLat,double aLon,double bLat,double bLon){
        double cl=Math.cos(Math.toRadians(lat));
        double x=lon*111.32*cl,y=lat*110.57, ax=aLon*111.32*cl,ay=aLat*110.57,bx=bLon*111.32*cl,by=bLat*110.57;
        double dx=bx-ax,dy=by-ay,den=dx*dx+dy*dy;double t=den==0?0:((x-ax)*dx+(y-ay)*dy)/den;t=Math.max(0,Math.min(1,t));double px=ax+t*dx,py=ay+t*dy;double ex=x-px,ey=y-py;return Math.sqrt(ex*ex+ey*ey);
    }
    private FaultModel(){}
}
