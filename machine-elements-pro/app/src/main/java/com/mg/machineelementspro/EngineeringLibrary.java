package com.mg.machineelementspro;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EngineeringLibrary {
    public static final class Material {
        public final String name; public final double E,G,Sy,Sut,density;
        Material(String name,double E,double G,double Sy,double Sut,double density){this.name=name;this.E=E;this.G=G;this.Sy=Sy;this.Sut=Sut;this.density=density;}
    }

    public static final Material[] MATERIALS={
            new Material("AISI 1020 normalized",205000,80000,350,420,7.87),
            new Material("AISI 1045 normalized",205000,80000,530,625,7.85),
            new Material("AISI 4140 Q&T (generic)",205000,80000,655,850,7.85),
            new Material("AISI 4340 Q&T (generic)",205000,80000,745,930,7.85),
            new Material("Al 6061-T6",68900,26000,276,310,2.70),
            new Material("Al 7075-T6",71700,26900,503,572,2.81),
            new Material("Ti-6Al-4V Grade 5",114000,44000,880,950,4.43),
            new Material("SS 304 annealed",193000,77000,215,505,8.00),
            new Material("SS 316 annealed",193000,77000,205,515,8.00)
    };

    public static final double[] PREFERRED_SHAFT_MM={6,8,10,12,15,16,18,20,22,25,28,30,32,35,38,40,42,45,48,50,55,60,65,70,75,80,85,90,95,100,110,120,130,140,150};
    public static final int[] METRIC_BOLT_M={3,4,5,6,8,10,12,14,16,18,20,22,24,27,30,33,36,39,42,45,48,52,56,60,64};

    public static double selectPreferredShaft(double required){
        for(double d:PREFERRED_SHAFT_MM) if(d>=required) return d;
        throw new IllegalArgumentException("Tercihli mil çapı kütüphane aralığını aşıyor.");
    }

    public static int selectMetricBolt(double requiredNominal){
        for(int d:METRIC_BOLT_M) if(d>=requiredNominal) return d;
        throw new IllegalArgumentException("Metrik civata kütüphane aralığını aşıyor.");
    }

    public static double requiredSolidShaftDiameter(double M_Nm,double T_Nm,double Sy,double targetFos){
        if(Sy<=0||targetFos<=0) throw new IllegalArgumentException("Sy ve hedef FoS pozitif olmalı.");
        double M=Math.abs(M_Nm)*1000.0,T=Math.abs(T_Nm)*1000.0;
        double coeff=Math.sqrt(Math.pow(32.0*M/Math.PI,2)+3.0*Math.pow(16.0*T/Math.PI,2));
        if(coeff==0) return 0;
        return Math.cbrt(coeff*targetFos/Sy);
    }

    public static String materialTable(){
        StringBuilder b=new StringBuilder();
        for(Material m:MATERIALS){
            b.append(m.name).append("\n  E=").append(fmt(m.E)).append(" MPa, G=").append(fmt(m.G)).append(" MPa, Sy=").append(fmt(m.Sy)).append(" MPa, Sut=").append(fmt(m.Sut)).append(" MPa, ρ=").append(fmt(m.density)).append(" g/cm³\n\n");
        }
        return b.toString();
    }

    private static String fmt(double x){return String.format(Locale.US,"%.5g",x);}
    private EngineeringLibrary(){}
}
