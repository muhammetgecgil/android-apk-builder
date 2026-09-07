package com.muhammetgecgil.wifiradar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @Override protected void onCreate(Bundle state){super.onCreate(state);try{build();}catch(Throwable t){TextView e=Ui.text(this,"Güvenli başlangıç modu\n\n"+t.getClass().getSimpleName()+": "+String.valueOf(t.getMessage()),16,Ui.WHITE,false);e.setPadding(Ui.dp(this,20),Ui.dp(this,50),Ui.dp(this,20),Ui.dp(this,20));e.setBackgroundColor(Ui.BG);setContentView(e);}}
    private void build(){
        LinearLayout root=Ui.shell(this,"WiFi Bilimsel Radar");ScrollView sv=Ui.scroll(this);LinearLayout col=Ui.column(this);sv.addView(col);
        TextView hero=Ui.text(this,"RF ölçümünü iddiadan ayıran mühendislik laboratuvarı.",23,Ui.WHITE,true);hero.setPadding(0,Ui.dp(this,14),0,Ui.dp(this,4));col.addView(hero);
        col.addView(Ui.text(this,"ESP32 CSI ile çoklu düğüm füzyonu veya telefondaki standart Wi‑Fi ölçümleriyle sensörsüz RF laboratuvarı. Sonuçlar güven değeriyle gösterilir.",14,Ui.MUTED,false));
        col.addView(Ui.card(this,"ESP32 CSI Füzyon","1–4 düğüm • kalite/tazelik ağırlığı • HMM aktivite durumu • RF kanıt haritası",Ui.GREEN,v->startActivity(new Intent(this,CsiActivity.class))));
        col.addView(Ui.card(this,"Sensörsüz Wi‑Fi Laboratuvarı","BSSID/RSSI parmak izi • robust RF değişim skoru • telefon hareketi reddi",Ui.CYAN,v->startActivity(new Intent(this,AmbientActivity.class))));
        col.addView(Ui.card(this,"Bilimsel kapsam ve gizlilik","Ne ölçülür, ne ölçülmez, izinler ve yerel veri işleme modeli",Ui.YELLOW,v->startActivity(new Intent(this,AboutActivity.class))));
        TextView badge=Ui.text(this,"PLAY v5 • target API 36 • bulut/hesap/reklam yok",12,Ui.GREEN,true);badge.setPadding(Ui.dp(this,12),Ui.dp(this,10),Ui.dp(this,12),Ui.dp(this,10));badge.setBackground(Ui.round(this,Ui.PANEL,Ui.GREEN,14));col.addView(badge);
        root.addView(sv,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));root.addView(Ui.text(this,"MG RF LAB 5.0 • Android 16 / API 36",11,Ui.MUTED,false),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,28)));setContentView(root);
    }
}
