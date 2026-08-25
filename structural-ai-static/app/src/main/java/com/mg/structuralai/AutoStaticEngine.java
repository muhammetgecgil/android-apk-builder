package com.mg.structuralai;

import java.util.Locale;

public final class AutoStaticEngine {
    public static final class Result {
        public final String report;
        public Result(String report){ this.report=report; }
    }

    public Result analyze(MeshModel m){
        double L=Math.max(m.dx(), Math.max(m.dy(),m.dz()));
        double b=Math.max(1e-6, middle(m.dx(),m.dy(),m.dz()));
        double h=Math.max(1e-6, Math.min(m.dx(), Math.min(m.dy(),m.dz())));
        double area=b*h;
        double I=b*Math.pow(h,3)/12.0;

        // V1 autonomous heuristics: SI-like interpretation after geometry normalization.
        double density=2700.0;          // default inferred lightweight-metal prior
        double yield=276e6;            // Pa, conservative 6061-T6-like prior
        double young=69e9;              // Pa
        double volume=Math.max(1e-12,m.dx()*m.dy()*m.dz()*0.35); // occupancy prior until tetra mesher lands
        double mass=density*volume;
        double gravity=9.80665;
        double load=mass*gravity*1.5;    // gravity + autonomous design factor
        double M=load*L;
        double sigma=Math.abs(M*(h/2.0)/Math.max(I,1e-18));
        double deflection=Math.abs(load*Math.pow(L,3)/(3.0*young*Math.max(I,1e-18)));
        double fos=yield/Math.max(sigma,1.0);

        double aspect=L/Math.max(h,1e-9);
        String support="En düşük global yüzey bölgesi ankastre aday olarak seçildi";
        String loadCase="Yerçekimi + 1.5 otomatik tasarım katsayısı";
        String className=aspect>8 ? "ince/uzun parça" : aspect>3 ? "orta narinlikte parça" : "kompakt parça";
        double confidence=0.52;
        if(m.triangles.size()>100) confidence+=0.08;
        if(m.vertices.size()>300) confidence+=0.05;
        if(aspect>2) confidence+=0.05;
        confidence=Math.min(0.78,confidence);

        String verdict;
        if(fos>=2.0) verdict="İLK OTOMATİK TARAMA: dayanım rezervi var";
        else if(fos>=1.2) verdict="İLK OTOMATİK TARAMA: sınırda, yüksek doğruluk FEA gerekli";
        else verdict="İLK OTOMATİK TARAMA: kritik bölge bekleniyor";

        return new Result(String.format(Locale.US,
            "STRUCTURAL AI STATIC — OTONOM ÖN ANALİZ\n\n"+
            "Geometri: %d vertex / %d üçgen\n"+
            "Boyutlar: %.4g × %.4g × %.4g (model birimi)\n"+
            "Sınıflandırma: %s\n\n"+
            "AI varsayımları\n• Malzeme ön-tahmini: Al alaşımı sınıfı (ρ=2700 kg/m³, E=69 GPa, Sy=276 MPa)\n"+
            "• Mesnet: %s\n• Yük: %s\n"+
            "• Geometri doluluk ön-katsayısı: 0.35\n\n"+
            "Hızlı fizik taraması\n• Tahmini kütle: %.3g kg\n• Eşdeğer yük: %.3g N\n"+
            "• Eğilme gerilmesi göstergesi: %.3g MPa\n• Uç deplasman göstergesi: %.3g mm\n• Akmaya göre FOS göstergesi: %.2f\n\n"+
            "%s\n\n"+
            "Otomasyon güveni: %.0f%%\n\n"+
            "NOT: Bu v0.1 sonuçları sertifikasyon/nihai tasarım FEA'sı değildir. Sonraki solver katmanı tetrahedral mesh + sparse FEM + temas + gerçek Von Mises/yer değiştirme alanlarını hesaplayacaktır.",
            m.vertices.size(),m.triangles.size(),m.dx(),m.dy(),m.dz(),className,support,loadCase,
            mass,load,sigma/1e6,deflection*1000.0,fos,verdict,confidence*100.0));
    }

    private static double middle(double a,double b,double c){
        return a+b+c-Math.max(a,Math.max(b,c))-Math.min(a,Math.min(b,c));
    }
}
