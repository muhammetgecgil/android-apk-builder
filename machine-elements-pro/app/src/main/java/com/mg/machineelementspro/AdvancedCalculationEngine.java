package com.mg.machineelementspro;

import java.util.Locale;

public final class AdvancedCalculationEngine {
    public static final String[] MODULES = {
            "Civata Pro – ön yük + ayrılma + proof",
            "Civata grubu – eksantrik kesme + moment",
            "Mil sistemi – iki düzlem reaksiyon + gerilme",
            "Helisel dişli – kuvvet bileşenleri",
            "Rulman çifti – yöneten L10h ömrü",
            "Pres geçme – tork + göbek çevresel gerilme",
            "İçi boş mil – eğilme + burulma",
            "Yorulma Pro – Marin + Goodman"
    };

    public static final String[][] LABELS = {
            {"Ön yük Fp (N)", "Harici eksenel yük Fa (N)", "Civata rijitliği kb (N/mm)", "Parça rijitliği km (N/mm)", "Proof Sp (MPa)", "Gerilme alanı As (mm²)"},
            {"Doğrudan kesme V (N)", "Moment M (N·m)", "Civata sayısı n", "Civata dairesi yarıçapı r (mm)", "Civata kesme alanı A (mm²)", "İzin verilen kayma (MPa)"},
            {"Mesnet açıklığı L (mm)", "Yük konumu a (mm)", "Teğetsel Ft (N)", "Radyal Fr (N)", "Mil çapı d (mm)", "Akma Sy (MPa)"},
            {"Tork T (N·m)", "Hatve çapı d (mm)", "Normal basınç açısı αn (deg)", "Helis açısı β (deg)", "", ""},
            {"C1 (N)", "P1 (N)", "C2 (N)", "P2 (N)", "Devir n (rpm)", "Üs p"},
            {"Temas basıncı p (MPa)", "Mil çapı d (mm)", "Geçme boyu L (mm)", "Sürtünme μ", "Göbek dış çapı D (mm)", "Göbek akma Sy (MPa)"},
            {"Eğilme momenti M (N·m)", "Tork T (N·m)", "Dış çap do (mm)", "İç çap di (mm)", "Akma Sy (MPa)", ""},
            {"Nominal Se' (MPa)", "Yüzey ka", "Boyut kb", "Yük kc", "Alternatif σa (MPa)", "Ortalama σm / Sut oranı"}
    };

    public static Result calculate(int module, double[] v) {
        switch (module) {
            case 0: return boltPro(v);
            case 1: return boltGroup(v);
            case 2: return shaftSystem(v);
            case 3: return helicalGear(v);
            case 4: return bearingPair(v);
            case 5: return pressFit(v);
            case 6: return hollowShaft(v);
            case 7: return fatiguePro(v);
            default: throw new IllegalArgumentException("Geçersiz ileri modül");
        }
    }

    private static Result boltPro(double[] v) {
        double fp=pos(v[0],"Fp"), fa=nonNeg(v[1],"Fa"), kb=pos(v[2],"kb"), km=pos(v[3],"km"), sp=pos(v[4],"Sp"), as=pos(v[5],"As");
        double phi=kb/(kb+km);
        double boltLoad=fp+phi*fa;
        double clamp=fp-(1.0-phi)*fa;
        double sep=fp/(1.0-phi);
        double sigma=boltLoad/as;
        double fos=sp/sigma;
        String status=clamp>0 && fos>=1.2 ? "UYGUN – bağlantı kapalı" : clamp<=0 ? "UYGUN DEĞİL – bağlantı ayrılıyor" : "SINIRDA – proof marjı düşük";
        return new Result("Civata Pro bağlantı rijitliği", lines(
                f("Yük paylaşım katsayısı Φ",phi,""), f("Civata toplam yükü",boltLoad,"N"), f("Kalan sıkma kuvveti",clamp,"N"), f("Ayrılma başlangıç yükü",sep,"N"), f("Civata gerilmesi",sigma,"MPa"), f("Proof FoS",fos,"")), status,
                "Rijitlik-temelli elastik bağlantı modeli. Detaylı VDI-2230 yönelimli yol; preload scatter, settlement, sıcaklık, diş/baş sürtünmesi ve yorulmayı ayrıca ele alacaktır.");
    }

    private static Result boltGroup(double[] v) {
        double V=nonNeg(v[0],"V"), M=nonNeg(v[1],"M")*1000.0, n=pos(v[2],"n"), r=pos(v[3],"r"), A=pos(v[4],"A"), allow=pos(v[5],"İzin verilen");
        if (Math.rint(n)!=n || n<2) throw new IllegalArgumentException("Civata sayısı en az 2 tam sayı olmalı.");
        double direct=V/n;
        double torsional=M/(n*r);
        double worst=direct+torsional;
        double tau=worst/A;
        double fos=allow/tau;
        return new Result("Dairesel civata grubu – kritik civata", lines(f("Doğrudan yük/civata",direct,"N"),f("Moment yükü/civata",torsional,"N"),f("Kritik resultant (üst sınır)",worst,"N"),f("Kritik kayma",tau,"MPa"),f("Emniyet katsayısı",fos,"")), verdict(fos),
                "Eşit yarıçaplı simetrik civata grubu için konservatif en kötü aynı yön varsayımıdır. Serbest koordinatlı civata grubu çözücüsü bir sonraki seviyedir.");
    }

    private static Result shaftSystem(double[] v) {
        double L=pos(v[0],"L"), a=pos(v[1],"a"), Ft=nonNeg(v[2],"Ft"), Fr=nonNeg(v[3],"Fr"), d=pos(v[4],"d"), sy=pos(v[5],"Sy");
        if (a>=L) throw new IllegalArgumentException("Yük konumu açıklıktan küçük olmalı.");
        double b=L-a;
        double ray=Ft*b/L, rby=Ft*a/L, raz=Fr*b/L, rbz=Fr*a/L;
        double My=Ft*a*b/L, Mz=Fr*a*b/L;
        double M=Math.hypot(My,Mz);
        double sigma=32.0*M/(Math.PI*Math.pow(d,3));
        double fos=sy/sigma;
        return new Result("Mil–rulman reaksiyon sistemi", lines(f("A teğetsel reaksiyon",ray,"N"),f("B teğetsel reaksiyon",rby,"N"),f("A radyal reaksiyon",raz,"N"),f("B radyal reaksiyon",rbz,"N"),f("Bileşik maksimum moment",M/1000.0,"N·m"),f("Mil eğilme gerilmesi",sigma,"MPa"),f("FoS",fos,"")), verdict(fos),
                "Tek yük istasyonu ve iki basit mesnet için iki düzlem statik çözüm. Bu reaksiyonlar rulman ömür modülüne doğrudan aktarılacak sistem mimarisinin ilk adımıdır.");
    }

    private static Result helicalGear(double[] v) {
        double T=pos(v[0],"T")*1000.0, d=pos(v[1],"d"), an=Math.toRadians(pos(v[2],"αn")), beta=Math.toRadians(nonNeg(v[3],"β"));
        if (beta>=Math.toRadians(60)) throw new IllegalArgumentException("Helis açısı bu ön model için 60° altında olmalı.");
        double Ft=2*T/d;
        double Fr=Ft*Math.tan(an)/Math.cos(beta);
        double Fa=Ft*Math.tan(beta);
        double resultant=Math.sqrt(Ft*Ft+Fr*Fr+Fa*Fa);
        return new Result("Helisel dişli kuvvetleri", lines(f("Teğetsel Ft",Ft,"N"),f("Radyal Fr",Fr,"N"),f("Eksenel Fa",Fa,"N"),f("Toplam kuvvet",resultant,"N")), "BİLGİSEL – yük aktarımı hazır",
                "Normal basınç açısı ve helis açısıyla temel kuvvet ayrıştırmasıdır. Kuvvetler mil ve rulman sistemine aktarılabilir.");
    }

    private static Result bearingPair(double[] v) {
        double C1=pos(v[0],"C1"),P1=pos(v[1],"P1"),C2=pos(v[2],"C2"),P2=pos(v[3],"P2"),rpm=pos(v[4],"rpm"),p=pos(v[5],"p");
        double h1=Math.pow(C1/P1,p)*1_000_000.0/(60*rpm);
        double h2=Math.pow(C2/P2,p)*1_000_000.0/(60*rpm);
        double gov=Math.min(h1,h2);
        return new Result("Rulman çifti temel ömür", lines(f("Rulman-1 L10h",h1,"h"),f("Rulman-2 L10h",h2,"h"),f("Yöneten ömür",gov,"h"),"Yöneten rulman: "+(h1<=h2?"1":"2")), gov>=10000?"UYGUN – yüksek temel ömür":gov>=3000?"SINIRDA – görev ömrünü kontrol et":"UYGUN DEĞİL – kısa temel ömür",
                "ISO 281 temel ömür denklemi tabanlı iki-rulman karşılaştırmasıdır. Güvenilirlik ve aISO değiştirilmiş ömür yolu ayrıca eklenecektir.");
    }

    private static Result pressFit(double[] v) {
        double p=pos(v[0],"p"), d=pos(v[1],"d"), L=pos(v[2],"L"), mu=nonNeg(v[3],"μ"), D=pos(v[4],"D"), sy=pos(v[5],"Sy");
        if (D<=d) throw new IllegalArgumentException("Göbek dış çapı mil çapından büyük olmalı.");
        double area=Math.PI*d*L;
        double friction=mu*p*area;
        double torque=friction*d/2.0/1000.0;
        double hoop=p*(D*D+d*d)/(D*D-d*d);
        double fos=sy/hoop;
        return new Result("Pres geçme kapasitesi", lines(f("Temas alanı",area,"mm²"),f("Sürtünme kuvveti",friction,"N"),f("Aktarılabilir tork",torque,"N·m"),f("İç yüzey çevresel gerilme",hoop,"MPa"),f("Göbek FoS",fos,"")), verdict(fos),
                "Kalın cidarlı göbek için iç yüzey çevresel gerilme ve uniform temas basıncı varsayımı. Gerçek interferans-tolerans ve elastik uyumluluk çözümü sonraki katmandır.");
    }

    private static Result hollowShaft(double[] v) {
        double M=nonNeg(v[0],"M")*1000.0,T=nonNeg(v[1],"T")*1000.0,Do=pos(v[2],"do"),Di=nonNeg(v[3],"di"),sy=pos(v[4],"Sy");
        if (Di>=Do) throw new IllegalArgumentException("İç çap dış çaptan küçük olmalı.");
        double I=Math.PI*(Math.pow(Do,4)-Math.pow(Di,4))/64.0;
        double J=2*I;
        double sigma=M*(Do/2)/I;
        double tau=T*(Do/2)/J;
        double vm=Math.sqrt(sigma*sigma+3*tau*tau);
        double fos=sy/vm;
        return new Result("İçi boş mil statik dayanım", lines(f("I",I,"mm⁴"),f("J",J,"mm⁴"),f("Eğilme gerilmesi",sigma,"MPa"),f("Burulma kayması",tau,"MPa"),f("von Mises",vm,"MPa"),f("FoS",fos,"")), verdict(fos),
                "Dairesel içi boş mil için elastik nominal gerilmeler. Çentik, yorulma ve lokal geometri etkileri ayrıca uygulanmalıdır.");
    }

    private static Result fatiguePro(double[] v) {
        double se0=pos(v[0],"Se'"),ka=pos(v[1],"ka"),kb=pos(v[2],"kb"),kc=pos(v[3],"kc"),sa=nonNeg(v[4],"σa"),ratio=v[5];
        if (ka>1.5||kb>1.5||kc>1.5) throw new IllegalArgumentException("Marin katsayılarını kontrol edin.");
        double se=se0*ka*kb*kc;
        double invN=sa/se+ratio;
        if (invN<=0) throw new IllegalArgumentException("Goodman toplamı pozitif olmalı.");
        double n=1.0/invN;
        return new Result("Marin + Goodman yorulma", lines(f("Düzeltilmiş Se",se,"MPa"),f("Alternatif katkı σa/Se",sa/se,""),f("Ortalama katkı σm/Sut",ratio,""),f("Yorulma FoS",n,"")), verdict(n),
                "Bu adım yüzey, boyut ve yük Marin katsayılarını uygular. Sıcaklık, güvenilirlik, çentik duyarlılığı ve Kf/Kfs sonraki sürümde ayrı girdiler olacaktır.");
    }

    private static double pos(double x,String n){if(!(x>0)||Double.isNaN(x)||Double.isInfinite(x))throw new IllegalArgumentException(n+" sıfırdan büyük olmalı.");return x;}
    private static double nonNeg(double x,String n){if(x<0||Double.isNaN(x)||Double.isInfinite(x))throw new IllegalArgumentException(n+" negatif olamaz.");return x;}
    private static String verdict(double n){if(n>=2)return "UYGUN – yüksek marj";if(n>=1.5)return "UYGUN – normal marj";if(n>=1)return "SINIRDA – yeniden değerlendir";return "UYGUN DEĞİL";}
    private static String f(String n,double x,String u){return String.format(Locale.US,"%s: %.5g%s",n,x,u.isEmpty()?"":" "+u);}
    private static String lines(String... s){return String.join("\n",s);}

    public static final class Result { public final String title,body,status,note; Result(String t,String b,String s,String n){title=t;body=b;status=s;note=n;} }
}
