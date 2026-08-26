package com.mg.stonefluorescence;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ModePanel extends LinearLayout {
    private final TextView title;
    private final TextView metrics;
    private final TextView instruction;

    public ModePanel(Context c) {
        super(c);
        setOrientation(VERTICAL);
        setPadding(dp(12), dp(10), dp(12), dp(10));
        setBackgroundColor(0xB0000000);
        title = tv("MODE", 18, true, Color.WHITE);
        metrics = tv("—", 14, true, Color.rgb(180,230,255));
        instruction = tv("—", 12, false, Color.rgb(220,225,232));
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        addView(title);
        addView(metrics);
        addView(instruction);
    }

    public void render(String mode, double b, double r, double g, double bl, double sat, double scatter, double variation) {
        String m = mode == null ? "NORMAL" : mode;
        title.setText(m + " • CANLI ÖLÇÜM");
        if (m.equals("UV365") || m.equals("UV395")) {
            metrics.setText(String.format(java.util.Locale.US,
                    "Floresans %.0f%%   Afterglow %.0f%%\nR/G %.2f   G/B %.2f   Doygunluk %.0f%%",
                    clamp((sat*0.65 + variation*0.35)*100), clamp(variation*120), ratio(r,g), ratio(g,bl), clamp(sat*100)));
            instruction.setText("365/395 nm harici UV kaynak kullan. Kamera görünür emisyonu ölçer; UV dalga boyunu doğrudan ölçmez.");
        } else if (m.equals("TRANSMISYON")) {
            metrics.setText(String.format(java.util.Locale.US,
                    "Geçirgenlik %.0f%%   İç yapı %.0f%%\nRGB: %.2f / %.2f / %.2f",
                    clamp(b*100), clamp(variation*130), r,g,bl));
            instruction.setText("Taşın arkasından homojen beyaz ışık ver. Koyu damar/inklüzyon bölgeleri düşük geçirgenlik olarak görünür.");
        } else if (m.equals("POLARİZE")) {
            metrics.setText(String.format(java.util.Locale.US,
                    "Yönsel değişim %.0f%%   Renk farkı %.0f%%\nR/G %.2f   G/B %.2f",
                    clamp(variation*150), clamp(sat*100), ratio(r,g), ratio(g,bl)));
            instruction.setText("Işıkta ve kamera önünde çapraz polarizer kullan; taşı döndür. Değişim yükseldikçe anizotropi/gerilim işareti güçlenir.");
        } else if (m.equals("KARANLIK ALAN")) {
            metrics.setText(String.format(java.util.Locale.US,
                    "Saçılma %.0f%%   Çatlak/kenar göstergesi %.0f%%\nParlaklık %.0f%%",
                    clamp(scatter*100), clamp(variation*145), clamp(b*100)));
            instruction.setText("Işığı yandan ver, doğrudan yansımayı kameraya sokma. İnklüzyon ve çatlaklar saçılma artışı olarak belirginleşir.");
        } else if (m.equals("PLEOKROİZM")) {
            metrics.setText(String.format(java.util.Locale.US,
                    "Açıya bağlı renk değişimi %.0f%%\nR %.2f   G %.2f   B %.2f   Sat %.0f%%",
                    clamp(variation*150), r,g,bl,clamp(sat*100)));
            instruction.setText("Taşı yavaşça döndür. Sabit beyaz ışık ve kilitli beyaz dengesi ile RGB değişimini izle.");
        } else if (m.equals("ZONLAMA")) {
            metrics.setText(String.format(java.util.Locale.US,
                    "Bölgesel renk farkı %.0f%%   Homojenlik %.0f%%\nR/G %.2f   G/B %.2f",
                    clamp(variation*160), clamp((1.0-variation)*100), ratio(r,g), ratio(g,bl)));
            instruction.setText("Taşı sabit tut. Bölgesel RGB değişimleri renk zonlanması ve bileşim farklılığı için optik ipucu verir.");
        } else if (m.equals("MAKRO")) {
            metrics.setText(String.format(java.util.Locale.US,
                    "Mikroyapı kontrastı %.0f%%   Kenar/çatlak %.0f%%\nDoygunluk %.0f%%",
                    clamp(variation*170), clamp(scatter*120), clamp(sat*100)));
            instruction.setText("Yakın çekim yap ve ekrana dokunarak netleştir. Tane sınırı, damar, yüzey çukuru ve kapanımları incele.");
        } else if (m.equals("PARLAKLIK")) {
            metrics.setText(String.format(java.util.Locale.US,
                    "Speküler yansıma %.0f%%   Saçılma %.0f%%\nOrtalama parlaklık %.0f%%",
                    clamp((b+sat*0.25)*100), clamp(scatter*100), clamp(b*100)));
            instruction.setText("Sabit açılı noktasal beyaz ışık kullan. Sonuç camsı/metalik/adamantin benzeri parlaklık sınıflandırmasına yardımcı olur.");
        } else if (m.equals("FINGERPRINT")) {
            metrics.setText(String.format(java.util.Locale.US,
                    "Optik imza: B%.0f S%.0f V%.0f\nR/G %.2f   G/B %.2f   Saçılma %.0f%%",
                    clamp(b*100), clamp(sat*100), clamp(variation*100), ratio(r,g), ratio(g,bl), clamp(scatter*100)));
            instruction.setText("Aynı taş için UV365, UV395, transmisyon, polarize ve beyaz ışık ölçümlerini karşılaştırarak aday mineral profilini güçlendir.");
        } else {
            metrics.setText(String.format(java.util.Locale.US,
                    "Parlaklık %.0f%%   Doygunluk %.0f%%\nR %.2f   G %.2f   B %.2f",
                    clamp(b*100), clamp(sat*100), r,g,bl));
            instruction.setText("Gerçek kamera görüntüsü. Bilimsel mod seçildiğinde bu panel o metoda özel canlı ölçümlere dönüşür.");
        }
    }

    private double ratio(double a,double b){ return a / Math.max(0.01,b); }
    private double clamp(double v){ return Math.max(0,Math.min(100,v)); }
    private TextView tv(String s,int sp,boolean bold,int color){ TextView t=new TextView(getContext()); t.setText(s); t.setTextSize(sp); t.setTextColor(color); if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD); return t; }
    private int dp(int x){ return (int)(x*getResources().getDisplayMetrics().density+0.5f); }
}
