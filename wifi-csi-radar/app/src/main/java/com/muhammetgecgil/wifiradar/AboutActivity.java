package com.muhammetgecgil.wifiradar;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public final class AboutActivity extends Activity {
    @Override protected void onCreate(Bundle s){super.onCreate(s);LinearLayout root=Ui.shell(this,"Bilimsel kapsam ve gizlilik");ScrollView sv=Ui.scroll(this);LinearLayout col=Ui.column(this);sv.addView(col);col.addView(Ui.button(this,"‹ Ana ekran",Ui.PANEL2,v->finish()),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,44)));col.addView(section("ESP32 CSI modu","Uyumlu ESP32 düğümlerinden presence_evidence, intensity, quality, stability ve baseline_drift ölçümleri okunur. Düğümler kalite ve tazelik ağırlıklı log-odds füzyonuyla birleştirilir. Aktivite etiketi zamansal HMM ile yumuşatılır."));col.addView(section("Sensörsüz mod","Android'in erişime açtığı BSSID/RSSI tarama sonuçları kullanılır. Ham CSI alınmaz. Robust RSSI değişimi, EWMA/CUSUM ve telefonun sensör hareketi birlikte değerlendirilir."));col.addView(section("Ne iddia etmiyoruz?","Tek telefonla duvar arkasından kesin insan koordinatı, iskelet pozu veya kimlik tespiti yapılmaz. CSI ısı haritası fiziksel kişi koordinatı değil, sensörlerin RF kanıt dağılımıdır."));col.addView(section("Gizlilik","Hesap, reklam SDK'sı, analitik SDK'sı veya bulut telemetrisi yoktur. Wi‑Fi ölçümleri cihazda işlenir. ESP32 bağlantısı yalnız kullanıcı tarafından girilen özel/yerel IP adreslerine yapılır."));col.addView(section("İzinler","Wi‑Fi tarama sonuçları için Android'in NEARBY_WIFI_DEVICES ve ACCESS_FINE_LOCATION izinleri gerekir. Uygulama GPS konum geçmişi tutmaz ve arka plan konum izni istemez."));root.addView(sv,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));setContentView(root);}
    private View section(String h,String b){LinearLayout box=Ui.column(this);box.setPadding(Ui.dp(this,14),Ui.dp(this,12),Ui.dp(this,14),Ui.dp(this,12));box.setBackground(Ui.round(this,Ui.PANEL,0,14));box.addView(Ui.text(this,h,16,Ui.GREEN,true));box.addView(Ui.text(this,b,13,Ui.WHITE,false));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.setMargins(0,Ui.dp(this,6),0,Ui.dp(this,6));box.setLayoutParams(p);return box;}
}
