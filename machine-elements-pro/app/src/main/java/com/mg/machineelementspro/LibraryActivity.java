package com.mg.machineelementspro;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class LibraryActivity extends Activity {
    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(build());}
    private View build(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.rgb(248,250,252));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(30));scroll.addView(root);
        TextView t=new TextView(this);t.setText("ENGINEERING LIBRARY");t.setTextSize(24);t.setTextColor(Color.rgb(15,23,42));root.addView(t);
        TextView s=new TextView(this);s.setText("Malzemeler • tercihli mil çapları • metrik civata serisi");s.setTextSize(14);s.setTextColor(Color.rgb(71,85,105));s.setPadding(0,dp(4),0,dp(12));root.addView(s);
        Button back=new Button(this);back.setText("← Geri");back.setAllCaps(false);back.setOnClickListener(v->finish());root.addView(back);
        TextView m=new TextView(this);m.setText(EngineeringLibrary.materialTable());m.setTextSize(14);m.setTextColor(Color.rgb(30,41,59));m.setPadding(0,dp(14),0,dp(8));root.addView(m);
        TextView note=new TextView(this);note.setText("Not: Malzeme değerleri genel mühendislik ön tasarım verileridir; nihai tasarımda sertifika/standart/ısıl işlem ve sıcaklık koşulları doğrulanmalıdır. Tercihli ölçü seçimi, minimum hesap çapını bir üst kütüphane ölçüsüne yuvarlar.");note.setTextSize(12);note.setTextColor(Color.rgb(100,116,139));root.addView(note);
        return scroll;
    }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
