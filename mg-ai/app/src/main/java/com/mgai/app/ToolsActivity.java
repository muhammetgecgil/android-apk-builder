package com.mgai.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import org.json.JSONObject;

public class ToolsActivity extends Activity {
    private static final String PREFS="mg_ai_v02";
    private EditText endpoint, input;
    private TextView output;

    @Override protected void onCreate(Bundle b){super.onCreate(b); build();}

    private void build(){
        SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(18),dp(18),dp(18)); root.setBackgroundColor(Color.rgb(244,246,248));
        TextView title=new TextView(this); title.setText("Tools + Agents"); title.setTextSize(26); title.setTypeface(null,android.graphics.Typeface.BOLD); root.addView(title);
        TextView note=new TextView(this); note.setText("v0.7 • izin kontrollü araçlar + agent planlama"); note.setTextColor(Color.DKGRAY); root.addView(note);
        endpoint=new EditText(this); endpoint.setHint("Tools server base URL"); endpoint.setText(p.getString("tools_endpoint","")); root.addView(endpoint);
        Button save=new Button(this); save.setText("Endpoint kaydet"); save.setAllCaps(false); save.setOnClickListener(v->p.edit().putString("tools_endpoint",endpoint.getText().toString().trim()).apply()); root.addView(save);
        input=new EditText(this); input.setHint("Hesap ifadesi veya agent hedefi"); input.setMinLines(2); root.addView(input);
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        Button calc=new Button(this); calc.setText("Hesapla"); calc.setAllCaps(false); calc.setOnClickListener(v->runCalc()); row.addView(calc,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
        Button plan=new Button(this); plan.setText("Agent Planı"); plan.setAllCaps(false); plan.setOnClickListener(v->runPlan()); row.addView(plan,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1)); root.addView(row);
        ScrollView sv=new ScrollView(this); output=new TextView(this); output.setText("Araç sonucu burada görünecek. Robot görev izni bu ekrandan verilmez."); output.setPadding(0,dp(14),0,0); sv.addView(output); root.addView(sv,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1));
        setContentView(root);
    }
    private String base(){return endpoint.getText().toString().trim();}
    private void runCalc(){String x=input.getText().toString().trim(); if(TextUtils.isEmpty(base())||TextUtils.isEmpty(x))return; output.setText("Çalışıyor…"); ToolsClient.calculator(base(),x,cb());}
    private void runPlan(){String x=input.getText().toString().trim(); if(TextUtils.isEmpty(base())||TextUtils.isEmpty(x))return; output.setText("Planlanıyor…"); ToolsClient.plan(base(),x,cb());}
    private ToolsClient.Callback cb(){return new ToolsClient.Callback(){public void onSuccess(JSONObject v){runOnUiThread(()->output.setText(v.toString()));} public void onError(String m){runOnUiThread(()->output.setText("Hata: "+m));}};}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
