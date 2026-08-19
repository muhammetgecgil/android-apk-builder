package com.mg.trainingassistant;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String PREFS = "training_assistant";
    private SharedPreferences prefs;
    private TextView status;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(44,60,44,54);
        root.setBackgroundColor(Color.rgb(16,20,24));
        scroll.addView(root);

        TextView title=text("Eğitim Asistanı 1.2",28,Color.WHITE); title.setPadding(0,0,0,10); root.addView(title);
        TextView sub=text("Hands-free eğitim izleme • OCR • Speech-to-Text • otomatik özet",16,Color.rgb(160,205,255)); sub.setPadding(0,0,0,24); root.addView(sub);
        TextView info=text("Eğitimi gerçekten oynatır ve içerik ekranda/seste ilerlerken not toplar. Play/Devam kontrollerini bulur; doğal bölüm sonundan sonra Next'e geçer. Quiz, sınav ve kişisel onay ekranlarında durur.",17,Color.rgb(210,216,222)); info.setPadding(0,0,0,26); root.addView(info);
        status=text("",18,Color.WHITE); status.setPadding(0,0,0,24); root.addView(status);

        Button access=button("1. Erişilebilirlik iznini aç"); access.setOnClickListener(v->showDisclosureThenOpenSettings()); root.addView(access);
        Button mic=button("2. Mikrofon / konuşma yazıya çevirme izni"); mic.setOnClickListener(v->requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},42)); root.addView(mic);
        Button start=button("3. HANDS-FREE EĞİTİMİ BAŞLAT"); start.setOnClickListener(v->{
            if(!prefs.getBoolean("consent",false)){showDisclosureThenOpenSettings();return;}
            prefs.edit().putBoolean("running",true).putBoolean("study_mode",true).apply(); refresh();
        }); root.addView(start);
        Button stop=button("Eğitimi / toplamayı durdur"); stop.setOnClickListener(v->{prefs.edit().putBoolean("running",false).apply();refresh();}); root.addView(stop);

        Button one=button("1 sayfalık özet oluştur"); one.setOnClickListener(v->showSummary(1)); root.addView(one);
        Button five=button("5 sayfalık özet oluştur"); five.setOnClickListener(v->showSummary(5)); root.addView(five);
        Button ten=button("10 sayfalık özet oluştur"); ten.setOnClickListener(v->showSummary(10)); root.addView(ten);
        Button clear=button("Toplanan eğitim notlarını temizle"); clear.setOnClickListener(v->{CaptureStore.clear(this); new AlertDialog.Builder(this).setMessage("Notlar temizlendi.").setPositiveButton("Tamam",null).show();}); root.addView(clear);

        TextView note=text("Not: Uygulama eğitim süresini sahte tamamlamaz ve quiz cevaplamaz. Eğitim gerçekten oynarken erişilebilirlik metni + OCR + STT ile içerik toplar; eğitim platformu oynatma tamamlanmadan Next'i açmıyorsa bekler.",14,Color.rgb(165,174,183)); note.setPadding(0,24,0,0); root.addView(note);
        setContentView(scroll); refresh();
    }

    private void showSummary(int pages){
        String s=CaptureStore.summarize(this,pages);
        new AlertDialog.Builder(this).setTitle(pages+" sayfalık eğitim özeti").setMessage(s)
                .setNegativeButton("Kapat",null)
                .setPositiveButton("Paylaş",(d,w)->{ Intent i=new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_TEXT,s); startActivity(Intent.createChooser(i,"Özeti paylaş")); }).show();
    }

    private void showDisclosureThenOpenSettings(){
        new AlertDialog.Builder(this).setTitle("Erişilebilirlik + eğitim içeriği açıklaması")
                .setMessage("Hands-free modda uygulama, eğitim ekrandayken görünür metin ve kontrolleri okuyabilir, ekran görüntüsünden OCR çıkarabilir ve sen izin verirsen mikrofondan duyulan eğitimi Speech-to-Text ile yazıya çevirebilir. Toplanan metin cihazda özet üretmek için saklanır; sunucuya gönderilmez. Play/Devam/Next yalnızca eğitim gezinmesi için kullanılır; quiz/sınav/kişisel onaylar otomatik yapılmaz.")
                .setNegativeButton("İptal",null).setPositiveButton("Kabul ediyorum",(d,w)->{prefs.edit().putBoolean("consent",true).apply();startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}).show();
    }

    @Override protected void onResume(){super.onResume();if(prefs!=null)refresh();}
    private void refresh(){boolean r=prefs.getBoolean("running",false);status.setText(r?"Durum: HANDS-FREE AKTİF • eğitim içeriği toplanıyor":"Durum: DURDURULDU");status.setTextColor(r?Color.rgb(94,234,138):Color.rgb(255,184,108));}
    private TextView text(String v,int s,int c){TextView t=new TextView(this);t.setText(v);t.setTextSize(s);t.setTextColor(c);t.setGravity(Gravity.START);return t;}
    private Button button(String v){Button b=new Button(this);b.setText(v);b.setTextSize(16);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,16);b.setLayoutParams(lp);return b;}
}
