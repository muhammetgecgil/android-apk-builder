package com.mg.hafizadostum.v4;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class PrivacyActivity extends Activity {
    private static final int NAVY = Color.rgb(13,53,86);
    private static final int TEAL = Color.rgb(23,184,151);
    private static final int BG = Color.rgb(245,249,251);

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        UiUtil.prepareWindow(this);
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        sv.addView(root);
        setContentView(sv);
        UiUtil.applyInsets(root, 18, 18, 18, 28);

        root.addView(text("Gizlilik ve sağlık bilgisi", 28, NAVY, true));
        root.addView(text("Hafıza Dostum 5.0", 15, TEAL, true));
        root.addView(section("Veriler nerede tutulur?"));
        root.addView(body("Profil seçimin, rutinlerin, 'yaptım' kayıtların, eşya hafızan, güvenilen kişi bilgisi ve son 6 aylık arşivin uygulamanın cihaz içi özel alanında saklanır. Uygulamanın INTERNET izni yoktur ve kendi başına bu verileri bir sunucuya göndermez."));

        root.addView(section("Yedekleme"));
        root.addView(body("Yedek yalnızca Ayarlar > Yedeği dışa aktar seçeneğini kullandığında, senin seçtiğin konuma JSON dosyası olarak oluşturulur. Geri yükleme de yalnızca senin seçtiğin dosyadan yapılır. Yedek dosyasını kiminle paylaştığın kullanıcı kontrolündedir."));

        root.addView(section("Sesle soru"));
        root.addView(body("Sesli soru özelliği Android'in cihazda seçili konuşma tanıma hizmetini açar. Konuşmanın cihazda mı yoksa konuşma tanıma sağlayıcısının sunucularında mı işlendiği o hizmetin ayarlarına ve gizlilik politikasına bağlıdır. Hafıza Dostum kendi sunucusuna ses kaydı yüklemez."));

        root.addView(section("Sağlık ve ilaç sınırı"));
        root.addView(body("Hafıza Dostum bir tıbbi cihaz, teşhis aracı veya tedavi sistemi değildir. Demans/Alzheimer gibi ifadeler yalnızca daha sade arayüz ve daha yoğun hatırlatma desteği seçeneğini tarif eder. Uygulama ilaç dozu belirlemez, ilacı tekrar almanı önermez ve sağlık profesyonelinin talimatının yerini tutmaz. İlaçla ilgili kararlar reçete/etiket ve sağlık profesyoneli talimatına göre verilmelidir."));

        root.addView(section("Veri silme"));
        root.addView(body("Ayarlar > Tüm yerel verilerimi sil seçeneğiyle uygulamanın tuttuğu profil, rutin, eşya ve arşiv bilgilerini cihazdan silebilirsin. Uygulamayı kaldırmak da Android tarafından uygulamanın yerel verilerini kaldırır."));

        root.addView(section("İzinler"));
        root.addView(body("Uygulama yalnızca hatırlatmalar için bildirim, telefon yeniden başlatıldığında hatırlatmaları kurmak için açılış bildirimi ve titreşim izinlerini kullanır. Konum, rehber, kamera, mikrofon veya internet izni istemez. Sesle soru Android konuşma tanıma arayüzü üzerinden kullanıcı tarafından başlatılır."));

        Button close = new Button(this);
        close.setText("Geri dön"); close.setAllCaps(false); close.setTextSize(16); close.setOnClickListener(v -> finish());
        root.addView(close);
    }

    private TextView section(String s) { TextView t = text(s, 18, NAVY, true); t.setPadding(0, dp(18), 0, dp(5)); return t; }
    private TextView body(String s) { TextView t = text(s, 15, Color.DKGRAY, false); t.setLineSpacing(0,1.18f); return t; }
    private TextView text(String s, int sp, int color, boolean bold) { TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t; }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
}
