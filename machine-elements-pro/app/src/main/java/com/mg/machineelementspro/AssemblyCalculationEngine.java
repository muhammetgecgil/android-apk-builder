package com.mg.machineelementspro;

import java.util.Locale;

public final class AssemblyCalculationEngine {
    public static final String[] MODULES = {
            "Stepped shaft – 3 istasyonlu",
            "Bolt group – 4 koordinatlı",
            "Bearing arrangement – locating/floating",
            "Preferred shaft diameter selector",
            "Critical mode ranking"
    };

    public static final String[][] LABELS = {
            {"L (mm)", "x1 (mm)", "F1 (N)", "x2 (mm)", "F2 (N)", "d (mm)"},
            {"Fx (N)", "Fy (N)", "Mz (N·m)", "r (mm)", "Civata d (mm)", "Sy (MPa)"},
            {"Fa (N)", "FrA (N)", "FrB (N)", "C_A (N)", "C_B (N)", "rpm"},
            {"M (N·m)", "T (N·m)", "Sy (MPa)", "Hedef FoS", "d min (mm)", "d max (mm)"},
            {"FoS civata", "FoS mil", "FoS rulman", "FoS dişli", "FoS kaynak", "FoS pim"}
    };

    public static Result calculate(int module, double[] v){
        switch(module){
            case 0:return steppedShaft(v);
            case 1:return boltGroup(v);
            case 2:return bearingArrangement(v);
            case 3:return preferredDiameter(v);
            case 4:return ranking(v);
            default:throw new IllegalArgumentException("Geçersiz assembly modülü");
        }
    }

    private static double pos(double x,String n){if(!(x>0))throw new IllegalArgumentException(n+" > 0 olmalı");return x;}

    private static Result steppedShaft(double[] v){
        double L=pos(v[0],"L"),x1=v[1],F1=v[2],x2=v[3],F2=v[4],d=pos(v[5],"d");
        if(x1<0||x1>L||x2<0||x2>L)throw new IllegalArgumentException("Yük istasyonları açıklık içinde olmalı");
        double RB=(F1*x1+F2*x2)/L; double RA=F1+F2-RB;
        double[] xs={0,x1,x2,L};
        double mMax=0; double xMax=0;
        for(double x:xs){double m=RA*x-(x>=x1?F1*(x-x1):0)-(x>=x2?F2*(x-x2):0);if(Math.abs(m)>Math.abs(mMax)){mMax=m;xMax=x;}}
        double sigma=Math.abs(32*mMax/(Math.PI*Math.pow(d,3)));
        return new Result("Stepped shaft – çoklu istasyon ön çözümü",
                fmt("RA",RA,"N")+"\n"+fmt("RB",RB,"N")+"\n"+fmt("Kritik moment",mMax/1000.0,"N·m")+"\n"+fmt("Kritik x",xMax,"mm")+"\n"+fmt("Nominal eğilme",sigma,"MPa"),
                "BİLGİSEL – reaksiyon ve kritik kesit bulundu",
                "Bu sürüm iki yük istasyonu ve tek çap için statik denge/gerilme ön çözümüdür. Sonraki sürümde her segment için ayrı çap, omuz Kt ve yorulma eklenecek.");
    }

    private static Result boltGroup(double[] v){
        double Fx=v[0],Fy=v[1],Mz=v[2]*1000.0,r=pos(v[3],"r"),d=pos(v[4],"d"),sy=pos(v[5],"Sy");
        int n=4; double directX=Fx/n,directY=Fy/n; double torsion=Mz/(n*r);
        double max=0; int idx=0;
        double[][] pts={{r,0},{0,r},{-r,0},{0,-r}};
        for(int i=0;i<n;i++){
            double x=pts[i][0],y=pts[i][1];
            double tx=-torsion*y/r,ty=torsion*x/r;
            double vx=directX+tx,vy=directY+ty; double mag=Math.hypot(vx,vy);
            if(mag>max){max=mag;idx=i+1;}
        }
        double area=Math.PI*d*d/4.0; double tau=max/area; double vm=Math.sqrt(3)*tau; double fos=sy/vm;
        return new Result("4-civatalı koordinatlı grup",
                fmt("Kritik civata yükü",max,"N")+"\n"+"Kritik civata: #"+idx+"\n"+fmt("Kesme gerilmesi",tau,"MPa")+"\n"+fmt("von Mises eşdeğer",vm,"MPa")+"\n"+fmt("FoS",fos,""),
                verdict(fos),
                "Dört civata + eksenlerden geçen simetrik koordinat modeli. Sonraki sürümde kullanıcı koordinat listesi, eksantrik çekme ve temas sürtünmesi eklenecek.");
    }

    private static Result bearingArrangement(double[] v){
        double Fa=Math.abs(v[0]),FrA=pos(Math.abs(v[1]),"FrA"),FrB=pos(Math.abs(v[2]),"FrB"),CA=pos(v[3],"C_A"),CB=pos(v[4],"C_B"),rpm=pos(v[5],"rpm");
        double PA=Math.hypot(FrA,Fa); double PB=FrB;
        double LA=Math.pow(CA/PA,3)*1e6/(60*rpm); double LB=Math.pow(CB/PB,3)*1e6/(60*rpm);
        String gov=LA<=LB?"LOCATING (A)":"FLOATING (B)";
        return new Result("Locating/Floating rulman düzeni",
                fmt("P_A",PA,"N")+"\n"+fmt("P_B",PB,"N")+"\n"+fmt("L10h A",LA,"h")+"\n"+fmt("L10h B",LB,"h")+"\n"+"Yöneten rulman: "+gov,
                Math.min(LA,LB)>=10000?"UYGUN – ömür yüksek":"KONTROL – ömür hedefi projeye göre değerlendirilmeli",
                "Basitleştirilmiş modelde eksenel yük yalnız locating rulmana atanır. X/Y faktörleri ve gerçek temas açısı ileride veri tabanlı uygulanacak.");
    }

    private static Result preferredDiameter(double[] v){
        double M=Math.abs(v[0])*1000,T=Math.abs(v[1])*1000,sy=pos(v[2],"Sy"),target=pos(v[3],"Hedef FoS"),dMin=pos(v[4],"d min"),dMax=pos(v[5],"d max");
        if(dMax<dMin)throw new IllegalArgumentException("d max >= d min olmalı");
        double[] pref={6,8,10,12,14,16,18,20,22,25,28,30,32,35,38,40,45,50,55,60,65,70,75,80,85,90,95,100,110,120,130,140,150};
        double chosen=Double.NaN, fos=0, vm=0;
        for(double d:pref){if(d<dMin||d>dMax)continue;double s=32*M/(Math.PI*Math.pow(d,3));double tau=16*T/(Math.PI*Math.pow(d,3));double eq=Math.sqrt(s*s+3*tau*tau);double f=sy/eq;if(f>=target){chosen=d;fos=f;vm=eq;break;}}
        if(Double.isNaN(chosen))return new Result("Tercihli çap seçimi","Aralıkta hedefi sağlayan tercihli çap bulunamadı.","UYGUN DEĞİL","Aralığı büyütün veya malzeme/yükleri gözden geçirin.");
        return new Result("Tercihli mil çapı seçimi",fmt("Seçilen çap",chosen,"mm")+"\n"+fmt("von Mises",vm,"MPa")+"\n"+fmt("FoS",fos,""),"UYGUN – en küçük geçen tercihli çap","Liste ön tercih çaplarıdır; gerçek standart/tedarik serisi daha sonra proje kütüphanesinden seçilecektir.");
    }

    private static Result ranking(double[] v){
        String[] names={"Civata","Mil","Rulman","Dişli","Kaynak","Pim"};
        double min=Double.POSITIVE_INFINITY;int idx=-1;
        StringBuilder b=new StringBuilder();
        for(int i=0;i<6;i++){double f=pos(v[i],names[i]+" FoS");b.append(names[i]).append(": ").append(String.format(Locale.US,"%.3f",f)).append("\n");if(f<min){min=f;idx=i;}}
        b.append("KRİTİK: ").append(names[idx]);
        return new Result("Assembly kritik mod sıralaması",b.toString(),verdict(min),"En düşük emniyet katsayısı assembly seviyesinde yöneten mod olarak işaretlenir.");
    }

    private static String verdict(double f){if(f>=2)return "UYGUN – yüksek marj";if(f>=1.5)return "UYGUN";if(f>=1)return "SINIRDA";return "UYGUN DEĞİL";}
    private static String fmt(String n,double v,String u){return String.format(Locale.US,"%s: %.5g%s",n,v,u.isEmpty()?"":" "+u);}

    public static final class Result{public final String title,body,status,note;Result(String t,String b,String s,String n){title=t;body=b;status=s;note=n;}}
}
