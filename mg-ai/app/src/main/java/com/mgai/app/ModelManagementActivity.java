package com.mgai.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ModelManagementActivity extends Activity {
    private TextView output;
    private EditText endpoint;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20),dp(24),dp(20),dp(24)); root.setBackgroundColor(Color.rgb(244,246,248));
        TextView title=new TextView(this); title.setText("Model Yönetimi"); title.setTextSize(28); title.setTextColor(Color.rgb(20,24,32)); root.addView(title);
        TextView info=new TextView(this); info.setText("Active / staged / previous model durumunu görüntüler. Aktivasyon ve rollback yalnız açık onay ile çalışır."); info.setTextSize(14); info.setTextColor(Color.DKGRAY); info.setPadding(0,dp(8),0,dp(12)); root.addView(info);
        endpoint=new EditText(this); endpoint.setHint("Registry endpoint, örn. http://10.0.2.2:8090"); endpoint.setText(getPreferences(MODE_PRIVATE).getString("registry_endpoint","")); root.addView(endpoint);
        Button save=new Button(this); save.setText("Endpoint Kaydet"); save.setAllCaps(false); save.setOnClickListener(v->{getPreferences(MODE_PRIVATE).edit().putString("registry_endpoint",endpoint.getText().toString().trim()).apply(); output.setText("Endpoint kaydedildi.");}); root.addView(save);
        Button refresh=new Button(this); refresh.setText("Durumu Yenile"); refresh.setAllCaps(false); refresh.setOnClickListener(v->loadState()); root.addView(refresh);
        Button activate=new Button(this); activate.setText("Staged Modeli Aktive Et"); activate.setAllCaps(false); activate.setOnClickListener(v->postApproval("activate")); root.addView(activate);
        Button rollback=new Button(this); rollback.setText("Önceki Modele Rollback"); rollback.setAllCaps(false); rollback.setOnClickListener(v->postApproval("rollback")); root.addView(rollback);
        output=new TextView(this); output.setText("Henüz durum yüklenmedi."); output.setTextSize(14); output.setTextColor(Color.rgb(30,35,45)); output.setPadding(0,dp(18),0,0); root.addView(output);
        scroll.addView(root); setContentView(scroll);
    }

    private void loadState(){
        String base=endpoint.getText().toString().trim();
        output.setText("Yükleniyor...");
        new Thread(()->{
            try { String s=ModelRegistryClient.get(base+"/v1/model-registry/state"); runOnUiThread(()->output.setText(s)); }
            catch(Exception e){ runOnUiThread(()->output.setText("Hata: "+e.getMessage())); }
        }).start();
    }

    private void postApproval(String action){
        String base=endpoint.getText().toString().trim();
        output.setText("İşleniyor...");
        new Thread(()->{
            try { String s=ModelRegistryClient.post(base+"/v1/model-registry/"+action,"{\"explicit_approval\":true}"); runOnUiThread(()->output.setText(s)); }
            catch(Exception e){ runOnUiThread(()->output.setText("Hata: "+e.getMessage())); }
        }).start();
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
