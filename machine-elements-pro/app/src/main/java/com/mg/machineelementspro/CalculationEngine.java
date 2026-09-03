package com.mg.machineelementspro;

import java.util.Locale;

public final class CalculationEngine {
    public static final String[] MODULES = {
            "Civata – birleşik çekme/kesme",
            "Mil – eğilme + burulma",
            "Rulman – L10 ömür",
            "Düz dişli – Lewis eğilme",
            "Basma yayı – gerilme + sehim",
            "Kama – kesme + ezilme",
            "Kaynak – köşe kaynağı",
            "Yorulma – Goodman",
            "Pim – kesme + yatak basıncı",
            "Kolon – Euler burkulma",
            "Kiriş – basit mesnet orta yük",
            "Mil – burulma açısı",
            "Güç vidası – kare diş",
            "Diş sıyırma – ön kontrol",
            "Civata – ön yük + ayrılma",
            "Kayış – güç ve gerilmeler",
            "Zincir – çekme kuvveti",
            "Kaplin civataları – tork aktarımı",
            "Rulman – eşdeğer dinamik yük",
            "Disk fren – tork kapasitesi"
    };

    public static final String[][] LABELS = {
            {"Eksenel yük F (N)", "Kesme yükü V (N)", "Civata çapı d (mm)", "Akma dayanımı Sy (MPa)", "", ""},
            {"Eğilme momenti M (N·m)", "Tork T (N·m)", "Mil çapı d (mm)", "Akma dayanımı Sy (MPa)", "", ""},
            {"Dinamik kapasite C (N)", "Eşdeğer yük P (N)", "Devir n (rpm)", "Üs p (3 / 3.333)", "", ""},
            {"Teğetsel kuvvet Ft (N)", "Yüz genişliği b (mm)", "Modül m (mm)", "Lewis Y", "İzin verilen gerilme (MPa)", ""},
            {"Kuvvet F (N)", "Ortalama çap D (mm)", "Tel çapı d (mm)", "Aktif sarım Na", "G (MPa)", "Ssy (MPa)"},
            {"Tork T (N·m)", "Mil çapı d (mm)", "Kama genişliği b (mm)", "Kama yüksekliği h (mm)", "Kama boyu L (mm)", "İzin verilen (MPa)"},
            {"Kuvvet F (N)", "Kaynak ayağı a (mm)", "Etkin uzunluk L (mm)", "İzin verilen kayma (MPa)", "", ""},
            {"Alternatif gerilme σa (MPa)", "Ortalama gerilme σm (MPa)", "Düzeltilmiş Se (MPa)", "Çekme dayanımı Sut (MPa)", "", ""},
            {"Kuvvet F (N)", "Pim çapı d (mm)", "Kulak kalınlığı t (mm)", "Kesme düzlemi (1/2)", "İzin verilen kayma (MPa)", "İzin verilen yatak (MPa)"},
            {"Eksenel basma P (N)", "E (MPa)", "Atalet I (mm⁴)", "Boy L (mm)", "Etkin boy katsayısı K", "Hedef FoS"},
            {"Orta nokta yükü F (N)", "Açıklık L (mm)", "E (MPa)", "Atalet I (mm⁴)", "Kesit modülü Z (mm³)", "Akma Sy (MPa)"},
            {"Tork T (N·m)", "Boy L (mm)", "Çap d (mm)", "G (MPa)", "İzin verilen açı (deg)", ""},
            {"Yük W (N)", "Ortalama çap dm (mm)", "Lead l (mm)", "Sürtünme μ", "Yaka ort. çap dc (mm)", "Yaka sürtünme μc"},
            {"Eksenel yük F (N)", "Ortalama diş çapı d2 (mm)", "Kavrama boyu Le (mm)", "Etkin diş payı q (0-1)", "İzin verilen kayma (MPa)", "Hedef FoS"},
            {"Ön yük Fi (N)", "Dış eksenel yük Fe (N)", "Civata rijitliği kb (N/mm)", "Bağlantı rijitliği kj (N/mm)", "K faktörü", "Nominal çap d (mm)"},
            {"Güç P (kW)", "Küçük kasnak çapı d (mm)", "Devir n (rpm)", "Sürtünme μ", "Sarım açısı (deg)", "İzin verilen T1 (N)"},
            {"Güç P (kW)", "Devir n (rpm)", "Adım dairesi yarıçapı r (mm)", "Servis katsayısı Ks", "İzin verilen çekme (N)", ""},
            {"Tork T (N·m)", "Civata sayısı n", "Hatve dairesi çapı D (mm)", "Civata çapı d (mm)", "İzin verilen kayma (MPa)", ""},
            {"Radyal yük Fr (N)", "Eksenel yük Fa (N)", "X faktörü", "Y faktörü", "Dinamik kapasite C (N)", "Üs p"},
            {"Sürtünme μ", "Sıkma kuvveti F (N)", "Ortalama yarıçap rm (mm)", "Sürtünme yüzeyi sayısı z", "İstenen tork (N·m)", ""}
    };

    public static Result calculate(int m, double[] v) {
        switch (m) {
            case 0: return bolt(v); case 1: return shaft(v); case 2: return bearing(v); case 3: return gear(v);
            case 4: return spring(v); case 5: return key(v); case 6: return weld(v); case 7: return goodman(v);
            case 8: return pin(v); case 9: return euler(v); case 10: return beam(v); case 11: return torsion(v);
            case 12: return powerScrew(v); case 13: return threadStrip(v); case 14: return boltPreload(v); case 15: return belt(v);
            case 16: return chain(v); case 17: return couplingBolts(v); case 18: return bearingEquivalent(v); case 19: return brake(v);
            default: throw new IllegalArgumentException("Geçersiz modül");
        }
    }

    private static Result bolt(double[] v){ double F=nn(v[0]),V=nn(v[1]),d=p(v[2]),Sy=p(v[3]); double A=Math.PI*d*d/4,s=F/A,t=V/A,vm=Math.sqrt(s*s+3*t*t),n=fs(Sy,vm); return r("Birleşik civata kontrolü", f("Alan",A,"mm²")+f("Çekme",s,"MPa")+f("Kesme",t,"MPa")+f("von Mises",vm,"MPa")+f("FoS",n,""), verdict(n), "Nominal gövde alanı ile ön kontroldür; diş çekme alanı ve VDI 2230 ayrıntıları gelişmiş modülde."); }
    private static Result shaft(double[] v){ double M=nn(v[0])*1000,T=nn(v[1])*1000,d=p(v[2]),Sy=p(v[3]); double s=32*M/(Math.PI*Math.pow(d,3)),t=16*T/(Math.PI*Math.pow(d,3)),vm=Math.sqrt(s*s+3*t*t),n=fs(Sy,vm); return r("Mil statik dayanım",f("Eğilme",s,"MPa")+f("Burulma",t,"MPa")+f("von Mises",vm,"MPa")+f("FoS",n,""),verdict(n),"Dolu dairesel mil; çentik, sehim, kritik hız ve yorulma ayrıca değerlendirilir."); }
    private static Result bearing(double[] v){ double C=p(v[0]),P=p(v[1]),rpm=p(v[2]),exp=p(v[3]); double L=Math.pow(C/P,exp),h=L*1e6/(60*rpm); return r("Rulman temel L10",f("L10",L,"milyon dev")+f("L10h",h,"saat"),h>=10000?"UZUN ÖMÜR":h>=3000?"ORTA ÖMÜR":"KISA ÖMÜR","ISO 281 temel ömür yaklaşımı; değiştirilmiş ömür ve çevresel faktörler ayrı katmandır."); }
    private static Result gear(double[] v){ double Ft=p(Math.abs(v[0])),b=p(v[1]),m=p(v[2]),Y=p(v[3]),allow=p(v[4]); double s=Ft/(b*m*Y),n=fs(allow,s); return r("Dişli Lewis ön kontrolü",f("Diş dibi gerilmesi",s,"MPa")+f("FoS",n,""),verdict(n),"Ön boyutlandırmadır; ISO 6336 temas ve kök eğilme kontrolleri ayrı gelişmiş modüllere taşınacaktır."); }
    private static Result spring(double[] v){ double F=p(Math.abs(v[0])),D=p(v[1]),d=p(v[2]),Na=p(v[3]),G=p(v[4]),Ssy=p(v[5]); double C=D/d;if(C<=1)throw new IllegalArgumentException("Yay indeksi >1 olmalı"); double Kw=(4*C-1)/(4*C-4)+0.615/C,t=Kw*8*F*D/(Math.PI*Math.pow(d,3)),k=G*Math.pow(d,4)/(8*Math.pow(D,3)*Na),def=F/k,n=fs(Ssy,t); return r("Helisel basma yayı",f("Wahl K",Kw,"")+f("Kayma",t,"MPa")+f("k",k,"N/mm")+f("Sehim",def,"mm")+f("FoS",n,""),verdict(n),"Burkulma, solid boy ve yorulma kontrolleri ayrıca gereklidir."); }
    private static Result key(double[] v){ double T=p(Math.abs(v[0]))*1000,d=p(v[1]),b=p(v[2]),h=p(v[3]),L=p(v[4]),allow=p(v[5]); double ts=2*T/(d*b*L),cr=4*T/(d*h*L),g=Math.max(ts,cr),n=fs(allow,g); return r("Paralel kama",f("Kesme",ts,"MPa")+f("Ezilme",cr,"MPa")+f("FoS",n,""),verdict(n),"Kama/göbek malzemeleri ayrı izin verilen değerlerle değerlendirilmelidir."); }
    private static Result weld(double[] v){ double F=nn(v[0]),a=p(v[1]),L=p(v[2]),allow=p(v[3]); double throat=0.70710678*a,t=F/(throat*L),n=fs(allow,t); return r("Köşe kaynağı",f("Boğaz",throat,"mm")+f("Kayma",t,"MPa")+f("FoS",n,""),verdict(n),"Tek doğrusal kaynak ön kontrolüdür; kaynak grubu momenti ayrıca çözülmelidir."); }
    private static Result goodman(double[] v){ double sa=nn(v[0]),sm=v[1],Se=p(v[2]),Sut=p(v[3]); double q=sa/Se+sm/Sut;if(q<=0)throw new IllegalArgumentException("Goodman toplamı pozitif olmalı"); double n=1/q; return r("Goodman yorulma",f("σa/Se",sa/Se,"")+f("σm/Sut",sm/Sut,"")+f("FoS",n,""),verdict(n),"Se yüzey, boyut, yük, sıcaklık ve güvenilirlik etkileriyle düzeltilmiş olmalıdır."); }
    private static Result pin(double[] v){ double F=p(Math.abs(v[0])),d=p(v[1]),t=p(v[2]),planes=p(v[3]),as=p(v[4]),ab=p(v[5]);if(planes!=1&&planes!=2)throw new IllegalArgumentException("Kesme düzlemi 1 veya 2 olmalı"); double tau=F/(planes*Math.PI*d*d/4),br=F/(d*t),n=Math.min(fs(as,tau),fs(ab,br)); return r("Pim ve yatak",f("Pim kesme",tau,"MPa")+f("Yatak basıncı",br,"MPa")+f("FoS",n,""),verdict(n),"Lug net-section, shear-out ve pim eğilmesi ayrıca kontrol edilmelidir."); }
    private static Result euler(double[] v){ double P=p(Math.abs(v[0])),E=p(v[1]),I=p(v[2]),L=p(v[3]),K=p(v[4]),target=p(v[5]); double Pcr=Math.PI*Math.PI*E*I/Math.pow(K*L,2),n=Pcr/P; return r("Euler burkulma",f("Pcr",Pcr,"N")+f("FoS",n,"")+f("Hedef",target,""),n>=target?"UYGUN – hedef sağlandı":"UYGUN DEĞİL – hedef sağlanmadı","Yalnız elastik uzun kolon bölgesinde geçerlidir."); }
    private static Result beam(double[] v){ double F=p(Math.abs(v[0])),L=p(v[1]),E=p(v[2]),I=p(v[3]),Z=p(v[4]),Sy=p(v[5]); double M=F*L/4,s=M/Z,def=F*Math.pow(L,3)/(48*E*I),n=fs(Sy,s); return r("Basit mesnetli kiriş",f("Mmax",M/1000,"N·m")+f("Gerilme",s,"MPa")+f("Sehim",def,"mm")+f("FoS",n,""),verdict(n),"Yalnız orta noktadaki tekil yük içindir."); }
    private static Result torsion(double[] v){ double T=p(Math.abs(v[0]))*1000,L=p(v[1]),d=p(v[2]),G=p(v[3]),allow=p(v[4]); double J=Math.PI*Math.pow(d,4)/32,deg=Math.toDegrees(T*L/(J*G)),tau=16*T/(Math.PI*Math.pow(d,3)); return r("Mil burulma rijitliği",f("J",J,"mm⁴")+f("Kayma",tau,"MPa")+f("Açı",deg,"deg"),deg<=allow?"UYGUN – açı limiti":"UYGUN DEĞİL – açı limiti aşıldı","Dayanım kontrolü ayrıca gereklidir."); }
    private static Result powerScrew(double[] v){ double W=p(Math.abs(v[0])),dm=p(v[1]),lead=p(v[2]),mu=nn0(v[3]),dc=nn0(v[4]),muc=nn0(v[5]); double lam=Math.atan(lead/(Math.PI*dm)),phi=Math.atan(mu),Tt=W*dm/2*Math.tan(phi+lam),Tc=W*muc*dc/2,T=Tt+Tc,eta=(W*lead/(2*Math.PI))/T; return r("Kare diş güç vidası",f("Toplam tork",T/1000,"N·m")+f("Verim",eta*100,"%")+"Kendinden kilitleme: "+(phi>lam?"EVET":"HAYIR")+"\n","BİLGİSEL – dayanım ayrıca kontrol edilmeli","Çekirdek gerilmesi, diş basıncı ve burkulma ayrıca kontrol edilmelidir."); }
    private static Result threadStrip(double[] v){ double F=p(Math.abs(v[0])),d2=p(v[1]),Le=p(v[2]),q=p(v[3]),allow=p(v[4]),target=p(v[5]);if(q>1)throw new IllegalArgumentException("q <= 1 olmalı"); double A=Math.PI*d2*Le*q,t=F/A,n=fs(allow,t); return r("Diş sıyırma ön kontrol",f("Kesme alanı",A,"mm²")+f("Ortalama kayma",t,"MPa")+f("FoS",n,""),n>=target?"UYGUN – hedef sağlandı":"UYGUN DEĞİL – hedef sağlanmadı","Gerçek iç/dış diş geometrisi gelişmiş modülde uygulanacaktır."); }
    private static Result boltPreload(double[] v){ double Fi=p(v[0]),Fe=nn(v[1]),kb=p(v[2]),kj=p(v[3]),K=p(v[4]),d=p(v[5]); double C=kb/(kb+kj),Fb=Fi+C*Fe,Fcl=Fi-(1-C)*Fe,T=K*(d/1000)*Fi; String st=Fcl>0?"UYGUN – bağlantı kapalı":"UYGUN DEĞİL – bağlantı ayrılıyor"; return r("Civata ön yük ve ayrılma",f("Yük paylaşım C",C,"")+f("Maks civata yükü",Fb,"N")+f("Kalan sıkma",Fcl,"N")+f("Tahmini sıkma torku",T,"N·m"),st,"K-tork yöntemi yaklaşık olup sürtünme saçılımına duyarlıdır; VDI 2230 ayrıntılı yol ayrıca uygulanacaktır."); }
    private static Result belt(double[] v){ double P=p(v[0])*1000,d=p(v[1])/1000,n=p(v[2]),mu=p(v[3]),theta=Math.toRadians(p(v[4])),allow=p(v[5]); double speed=Math.PI*d*n/60,dT=P/speed,ratio=Math.exp(mu*theta),T2=dT/(ratio-1),T1=ratio*T2,fs=allow/T1; return r("Kayış güç aktarımı",f("Kayış hızı",speed,"m/s")+f("T1",T1,"N")+f("T2",T2,"N")+f("FoS",fs,""),verdict(fs),"Merkezkaç gerilmesi, kayış tipi ve üretici kapasite katsayıları ayrıca uygulanmalıdır."); }
    private static Result chain(double[] v){ double P=p(v[0])*1000,rpm=p(v[1]),r=p(v[2])/1000,Ks=p(v[3]),allow=p(v[4]); double omega=2*Math.PI*rpm/60,T=P/omega,F=Ks*T/r,n=allow/F; return r("Zincir çekme ön kontrol",f("Tork",T,"N·m")+f("Tasarım çekme",F,"N")+f("FoS",n,""),verdict(n),"Zincir adımı, diş sayısı, hız, yağlama ve katalog servis faktörleri ayrıca seçilmelidir."); }
    private static Result couplingBolts(double[] v){ double T=p(v[0])*1000,nBolt=p(v[1]),D=p(v[2]),d=p(v[3]),allow=p(v[4]); double F=2*T/(nBolt*D),tau=F/(Math.PI*d*d/4),n=allow/tau; return r("Kaplin civataları",f("Civata başına teğetsel yük",F,"N")+f("Kesme",tau,"MPa")+f("FoS",n,""),verdict(n),"Sürtünmeli ön yüklü kaplinlerde yük yolu farklıdır; bu mod doğrudan kesme aktarımı içindir."); }
    private static Result bearingEquivalent(double[] v){ double Fr=nn(v[0]),Fa=nn(v[1]),X=p(v[2]),Y=p(v[3]),C=p(v[4]),exp=p(v[5]); double P=X*Fr+Y*Fa,L=Math.pow(C/P,exp); return r("Rulman eşdeğer dinamik yük",f("P",P,"N")+f("L10",L,"milyon dev"),"BİLGİSEL – uygun X/Y seçimi kritik","X/Y değerleri rulman tipine ve Fa/Fr oranına göre katalog/standarttan seçilmelidir."); }
    private static Result brake(double[] v){ double mu=p(v[0]),F=p(v[1]),rm=p(v[2])/1000,z=p(v[3]),target=p(v[4]); double T=mu*F*rm*z,n=T/target; return r("Disk fren tork kapasitesi",f("Kapasite",T,"N·m")+f("Talep/Kapasite FoS",n,""),verdict(n),"Isıl kapasite, balata basıncı, fade ve dinamik fren çevrimi ayrıca kontrol edilmelidir."); }

    private static double p(double x){ if(!(x>0)||Double.isNaN(x)||Double.isInfinite(x)) throw new IllegalArgumentException("Pozitif değer gerekli"); return x; }
    private static double nn(double x){ x=Math.abs(x); if(Double.isNaN(x)||Double.isInfinite(x)) throw new IllegalArgumentException("Geçersiz değer"); return x; }
    private static double nn0(double x){ if(x<0||Double.isNaN(x)||Double.isInfinite(x)) throw new IllegalArgumentException("Negatif değer olamaz"); return x; }
    private static double fs(double cap,double dem){ return dem==0?Double.POSITIVE_INFINITY:cap/dem; }
    private static String verdict(double n){ if(Double.isInfinite(n))return "YÜK YOK"; if(n>=2)return "UYGUN – yüksek marj"; if(n>=1.5)return "UYGUN – normal marj"; if(n>=1)return "SINIRDA – yeniden değerlendir"; return "UYGUN DEĞİL"; }
    private static String f(String name,double val,String unit){ String u=unit.isEmpty()?"":" "+unit; return String.format(Locale.US,"%s: %.5g%s\n",name,val,u); }
    private static Result r(String t,String b,String s,String n){ return new Result(t,b,s,n); }

    public static final class Result { public final String title,body,status,note; Result(String t,String b,String s,String n){title=t;body=b;status=s;note=n;} }
}
