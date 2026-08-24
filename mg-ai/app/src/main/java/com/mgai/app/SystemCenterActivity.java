package com.mgai.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Iterator;

public class SystemCenterActivity extends Activity {
    private EditText endpoint; private TextView output;
    @Override protected void onCreate(Bundle b){super.onCreate(b);
        ScrollView scroll=new ScrollView(this); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20),dp(24),dp(20),dp(24)); root.setBackgroundColor(Color.rgb(244,246,248)); scroll.addView(root);
        TextView title=new TextView(this); title.setText("Sistem Merkezi"); title.setTextSize(28); title.setTextColor(Color.rgb(20,24,32)); root.addView(title);
        TextView sub=new TextView(this); sub.setText("Unified Orchestrator • servis sağlığı • model • training • robot safety"); sub.setTextSize(14); sub.setPadding(0,dp(4),0,dp(16)); root.addView(sub);
        endpoint=new EditText(this); endpoint.setHint("Orchestrator endpoint (örn. http://192.168.1.20:8090)"); endpoint.setText(getPreferences(MODE_PRIVATE).getString("endpoint","")); root.addView(endpoint);
        Button refresh=new Button(this); refresh.setText("Sistemi Tara"); refresh.setAllCaps(false); refresh.setOnClickListener(v->load()); root.addView(refresh);
        output=new TextView(this); output.setText("Henüz taranmadı."); output.setTextSize(14); output.setTextColor(Color.rgb(30,35,45)); output.setPadding(dp(12),dp(16),dp(12),dp(12)); root.addView(output);
        setContentView(scroll);
    }
    private void load(){String ep=endpoint.getText().toString().trim(); getPreferences(MODE_PRIVATE).edit().putString("endpoint",ep).apply(); output.setText("Kontrol ediliyor..."); new Thread(()->{try{JSONObject j=SystemCenterClient.status(ep); String text=pretty(j); runOnUiThread(()->output.setText(text));}catch(Exception e){runOnUiThread(()->output.setText("Bağlantı hatası: "+e.getMessage()));}}).start();}
    private String pretty(JSONObject j){StringBuilder s=new StringBuilder();
        s.append("Sistem: ").append(j.optString("status",j.optBoolean("ok",false)?"ok":"unknown")).append("\n");
        s.append("Automatic model activation: ").append(j.optBoolean("automatic_model_activation",false)).append("\n");
        s.append("Robot safety bypass: ").append(j.optBoolean("robot_safety_bypass",false)).append("\n\n");
        JSONObject services=j.optJSONObject("services"); if(services!=null){s.append("SERVİSLER\n"); Iterator<String> it=services.keys(); while(it.hasNext()){String k=it.next(); JSONObject v=services.optJSONObject(k); s.append("• ").append(k).append(": "); if(v!=null){s.append(v.optString("status",v.optBoolean("ok",false)?"ok":"unknown")); if(v.has("latency_ms")) s.append(" • ").append(v.optLong("latency_ms")).append(" ms");} else s.append(String.valueOf(services.opt(k))); s.append("\n");}}
        JSONObject model=j.optJSONObject("model_registry"); if(model!=null){s.append("\nMODEL\nActive: ").append(model.opt("active_model")).append("\nStaged: ").append(model.opt("staged_model")).append("\nPrevious: ").append(model.opt("previous_model")).append("\n");}
        JSONObject training=j.optJSONObject("training"); if(training!=null) s.append("\nTRAINING\n").append(training.toString()).append("\n");
        JSONObject safety=j.optJSONObject("robot_safety"); if(safety!=null) s.append("\nROBOT SAFETY\n").append(safety.toString()).append("\n");
        return s.toString();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
