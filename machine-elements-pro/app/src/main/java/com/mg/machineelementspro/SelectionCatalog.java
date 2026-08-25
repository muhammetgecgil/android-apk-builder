package com.mg.machineelementspro;

import java.util.ArrayList;
import java.util.List;

public final class SelectionCatalog {
    private SelectionCatalog() {}

    public static final BoltClass[] BOLT_CLASSES = {
            new BoltClass("8.8", 640, 800, 580),
            new BoltClass("10.9", 900, 1000, 830),
            new BoltClass("12.9", 1080, 1200, 970)
    };

    public static final Bearing[] BEARINGS = {
            new Bearing("6200",10,30,9,5070,2360),
            new Bearing("6201",12,32,10,6890,3100),
            new Bearing("6202",15,35,11,8060,3750),
            new Bearing("6203",17,40,12,9560,4750),
            new Bearing("6204",20,47,14,13500,6550),
            new Bearing("6205",25,52,15,14800,7800),
            new Bearing("6206",30,62,16,19500,11200),
            new Bearing("6207",35,72,17,25500,15300),
            new Bearing("6208",40,80,18,29100,17800),
            new Bearing("6209",45,85,19,32500,20400),
            new Bearing("6210",50,90,20,35100,23200)
    };

    public static BoltClass findBoltClass(String name){
        for(BoltClass b:BOLT_CLASSES) if(b.name.equals(name)) return b;
        throw new IllegalArgumentException("Bilinmeyen civata sınıfı: "+name);
    }

    public static Bearing selectBearing(double boreMin, double requiredC){
        for(Bearing b:BEARINGS) if(b.bore>=boreMin && b.C>=requiredC) return b;
        throw new IllegalArgumentException("Katalog aralığında uygun rulman bulunamadı.");
    }

    public static String fitGuidance(String fit){
        if(fit==null) return "Geçme seçin.";
        switch(fit){
            case "H7/g6": return "Tipik hassas boşluklu geçme; serbest dönme/kayma gereken genel uygulamalar için ön seçim.";
            case "H7/h6": return "Yakın boşluklu/konumlama geçmesi; kolay montaj ve iyi merkezleme için ön seçim.";
            case "H7/k6": return "Geçiş geçmesi; hafif sıkılık ve iyi konumlama için ön seçim.";
            case "H7/m6": return "Hafif pres geçme; sökülebilir ancak sıkı merkezleme gereken uygulamalar için ön seçim.";
            case "H7/p6": return "Belirgin pres geçme; tork aktarımı/kalıcı montaj için ön seçim. Gerçek tolerans limitleri çap kademesine göre hesaplanmalıdır.";
            default: return "Bu sürümde bilgi notu yok.";
        }
    }

    public static List<String> compareMaterialsForShaft(double M_Nm,double T_Nm,double targetFos){
        List<String> out=new ArrayList<>();
        for(MaterialLibrary.Material m:MaterialLibrary.MATERIALS){
            double d=requiredSolidShaftDiameter(M_Nm,T_Nm,m.sy,targetFos);
            double pref=MaterialLibrary.nextPreferredShaftDiameter(d);
            double area=Math.PI*pref*pref/4.0;
            double massPerM=area*1e-6*m.density;
            out.add(m.name+" | d_req="+round(d)+" mm | d_pref="+round(pref)+" mm | kütle≈"+round(massPerM)+" kg/m");
        }
        return out;
    }

    public static double requiredSolidShaftDiameter(double M_Nm,double T_Nm,double sy,double fos){
        if(sy<=0||fos<=0) throw new IllegalArgumentException("Sy ve FoS pozitif olmalı.");
        double M=Math.abs(M_Nm)*1000.0,T=Math.abs(T_Nm)*1000.0;
        double coeff=Math.sqrt(Math.pow(32.0*M/Math.PI,2)+3.0*Math.pow(16.0*T/Math.PI,2));
        double allow=sy/fos;
        return Math.cbrt(coeff/allow);
    }

    private static String round(double x){return String.format(java.util.Locale.US,"%.3f",x);}

    public static final class BoltClass{
        public final String name; public final double sy,sut,proof;
        BoltClass(String n,double sy,double sut,double proof){this.name=n;this.sy=sy;this.sut=sut;this.proof=proof;}
    }
    public static final class Bearing{
        public final String code; public final double bore,od,width,C,C0;
        Bearing(String c,double b,double od,double w,double C,double C0){this.code=c;this.bore=b;this.od=od;this.width=w;this.C=C;this.C0=C0;}
    }
}
