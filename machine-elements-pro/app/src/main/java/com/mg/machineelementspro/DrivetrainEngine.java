package com.mg.machineelementspro;

import java.util.Locale;

public final class DrivetrainEngine {
    public static final class Input {
        public double torqueNm, rpm, pitchDiameterMm, pressureAngleDeg, helixAngleDeg;
        public double spanMm, gearPositionMm, shaftDiameterMm, shaftYieldMpa;
        public double bearingC1N, bearingC2N, bearingExponent;
    }

    public static final class Result {
        public final double ft,fr,fa,ra,rb,momentNm,shaftStressMpa,shaftFos,bearing1Load,bearing2Load,l10h1,l10h2;
        public final String status,body,note;
        Result(double ft,double fr,double fa,double ra,double rb,double momentNm,double shaftStressMpa,double shaftFos,double bearing1Load,double bearing2Load,double l10h1,double l10h2,String status,String body,String note){
            this.ft=ft;this.fr=fr;this.fa=fa;this.ra=ra;this.rb=rb;this.momentNm=momentNm;this.shaftStressMpa=shaftStressMpa;this.shaftFos=shaftFos;this.bearing1Load=bearing1Load;this.bearing2Load=bearing2Load;this.l10h1=l10h1;this.l10h2=l10h2;this.status=status;this.body=body;this.note=note;
        }
    }

    public static Result calculate(Input x){
        pos(x.torqueNm,"Tork");pos(x.rpm,"Devir");pos(x.pitchDiameterMm,"Hatve çapı");pos(x.spanMm,"Mesnet açıklığı");pos(x.gearPositionMm,"Dişli konumu");pos(x.shaftDiameterMm,"Mil çapı");pos(x.shaftYieldMpa,"Sy");pos(x.bearingC1N,"C1");pos(x.bearingC2N,"C2");pos(x.bearingExponent,"Üs");
        if(x.gearPositionMm>=x.spanMm)throw new IllegalArgumentException("Dişli konumu mesnet açıklığından küçük olmalı.");
        if(x.pressureAngleDeg<=0||x.pressureAngleDeg>=45)throw new IllegalArgumentException("Basınç açısı 0-45° aralığında olmalı.");
        if(x.helixAngleDeg<0||x.helixAngleDeg>=60)throw new IllegalArgumentException("Helis açısı 0-60° aralığında olmalı.");
        double T=x.torqueNm*1000.0;
        double an=Math.toRadians(x.pressureAngleDeg), beta=Math.toRadians(x.helixAngleDeg);
        double ft=2*T/x.pitchDiameterMm;
        double fr=ft*Math.tan(an)/Math.cos(beta);
        double fa=ft*Math.tan(beta);
        double a=x.gearPositionMm,b=x.spanMm-a;
        double ray=ft*b/x.spanMm,rby=ft*a/x.spanMm;
        double raz=fr*b/x.spanMm,rbz=fr*a/x.spanMm;
        double ra=Math.hypot(ray,raz),rb=Math.hypot(rby,rbz);
        double my=ft*a*b/x.spanMm,mz=fr*a*b/x.spanMm;
        double m=Math.hypot(my,mz);
        double sigma=32*m/(Math.PI*Math.pow(x.shaftDiameterMm,3));
        double fos=x.shaftYieldMpa/sigma;
        double p1=Math.hypot(ra,fa*0.5),p2=Math.hypot(rb,fa*0.5);
        double l1=Math.pow(x.bearingC1N/p1,x.bearingExponent)*1_000_000.0/(60*x.rpm);
        double l2=Math.pow(x.bearingC2N/p2,x.bearingExponent)*1_000_000.0/(60*x.rpm);
        double life=Math.min(l1,l2);
        String status=(fos>=1.5&&life>=3000)?"UYGUN – sistem ön kontrolü":(fos>=1.0&&life>=1000)?"SINIRDA – sistem optimizasyonu gerekli":"UYGUN DEĞİL – kritik bileşen var";
        String critical=fos<1.0?"Mil statik dayanımı":(l1<l2?"Rulman 1 ömrü":"Rulman 2 ömrü");
        String body=lines(f("Dişli Ft",ft,"N"),f("Dişli Fr",fr,"N"),f("Dişli Fa",fa,"N"),f("Rulman A radyal reaksiyon",ra,"N"),f("Rulman B radyal reaksiyon",rb,"N"),f("Maks. bileşik moment",m/1000.0,"N·m"),f("Mil eğilme gerilmesi",sigma,"MPa"),f("Mil FoS",fos,""),f("Rulman-1 eşdeğer ön yük",p1,"N"),f("Rulman-2 eşdeğer ön yük",p2,"N"),f("Rulman-1 L10h",l1,"h"),f("Rulman-2 L10h",l2,"h"),"Kritik ön kontrol: "+critical);
        String note="Dişli → mil → mesnet reaksiyonu → rulman ömrü zinciri tek çözümde bağlanmıştır. Eksenel yükün rulmanlara %50/%50 dağıtımı bu ilk sistem sürümünde açık varsayımdır; gerçek locating/floating bearing düzeni sonraki sürümde seçilebilir olacaktır.";
        return new Result(ft,fr,fa,ra,rb,m/1000.0,sigma,fos,p1,p2,l1,l2,status,body,note);
    }

    private static double pos(double v,String n){if(!(v>0)||Double.isNaN(v)||Double.isInfinite(v))throw new IllegalArgumentException(n+" sıfırdan büyük olmalı.");return v;}
    private static String f(String n,double v,String u){return String.format(Locale.US,"%s: %.5g%s",n,v,u.isEmpty()?"":" "+u);}
    private static String lines(String...s){return String.join("\n",s);}
}
