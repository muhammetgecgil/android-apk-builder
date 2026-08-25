package com.mgai.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BenchmarkComparisonActivity extends Activity {
    private static final Pattern LINE=Pattern.compile("ctx (\\d+) • (\\d+) thread • load med/p95 (\\d+)/(\\d+) ms • TTFT med/p95 (\\d+)/(\\d+) ms • tok/sn med/p95 ([0-9.]+)/([0-9.]+) • toplam med/p95 (\\d+)/(\\d+) ms • ΔT med ([0-9.]+)°C • skor ([0-9.-]+)");
    private static final Pattern WIN=Pattern.compile("KAZANAN: ctx (\\d+) • (\\d+) thread • skor ([0-9.-]+)");

    static final class P {
        int ctx,threads; long loadMed,loadP95,ttftMed,ttftP95,totalMed,totalP95; double tpsMed,tpsP95,temp,score;
    }

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        ScrollView sv=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(22),dp(18),dp(24));root.setBackgroundColor(Color.rgb(244,246,248));sv.addView(root);
        TextView title=new TextView(this);title.setText("Benchmark Profil Karşılaştırması");title.setTextSize(27);title.setTextColor(Color.rgb(20,24,32));root.addView(title);
        TextView sub=new TextView(this);sub.setText("Medyan ve p95 sonuçlarıyla hız / gecikme / ısı karşılaştırması");sub.setTextSize(14);sub.setPadding(0,dp(4),0,dp(12));root.addView(sub);

        String report=SelfTuningManager.benchmarkReport(this);
        if(report==null||report.trim().isEmpty()){
            TextView empty=card(root);empty.setText("Henüz aktif benchmark sonucu yok. Self-Tuning ekranından Aktif Benchmark Başlat seçeneğini çalıştır.");setContentView(sv);return;
        }
        List<P> ps=parse(report);
        int winCtx=-1,winThreads=-1;double winScore=0;
        Matcher wm=WIN.matcher(report);if(wm.find()){winCtx=Integer.parseInt(wm.group(1));winThreads=Integer.parseInt(wm.group(2));winScore=Double.parseDouble(wm.group(3));}
        if(ps.isEmpty()){TextView raw=card(root);raw.setText(report);setContentView(sv);return;}

        double maxTps=maxTps(ps),maxTtft=maxTtft(ps),maxTotal=maxTotal(ps),maxTemp=maxTemp(ps);
        for(P p:ps){
            boolean winner=p.ctx==winCtx&&p.threads==winThreads;
            TextView h=card(root);h.setText((winner?"★ KAZANAN • ":"")+"ctx "+p.ctx+" • "+p.threads+" thread • skor "+String.format(Locale.US,"%.2f",p.score));h.setTextSize(17);
            addMetric(root,"Hız",p.tpsMed,maxTps,true,String.format(Locale.US,"%.1f tok/sn",p.tpsMed));
            addMetric(root,"TTFT p95",p.ttftP95,maxTtft,false,p.ttftP95+" ms");
            addMetric(root,"Toplam p95",p.totalP95,maxTotal,false,p.totalP95+" ms");
            addMetric(root,"Isı artışı",p.temp,maxTemp,false,String.format(Locale.US,"%.1f°C",p.temp));
            TextView detail=small(root);detail.setText("Engine yükleme med/p95: "+p.loadMed+"/"+p.loadP95+" ms • TTFT med/p95: "+p.ttftMed+"/"+p.ttftP95+" ms • toplam med/p95: "+p.totalMed+"/"+p.totalP95+" ms");
        }
        TextView why=card(root);
        why.setText("NEDEN BU PROFİL KAZANDI?\nKazanan skor, medyan token/s hızını ödüllendiriyor; yüksek p95 TTFT, sıcaklık artışı ve p95 engine yükleme süresini cezalandırıyor. Böylece yalnız en hızlı değil, daha kararlı ve termal olarak daha dengeli profil seçiliyor.\n\nKazanan: ctx "+winCtx+" • "+winThreads+" thread • skor "+String.format(Locale.US,"%.2f",winScore));
        setContentView(sv);
    }

    private List<P> parse(String r){List<P> out=new ArrayList<>();Matcher m=LINE.matcher(r);while(m.find()){P p=new P();p.ctx=Integer.parseInt(m.group(1));p.threads=Integer.parseInt(m.group(2));p.loadMed=Long.parseLong(m.group(3));p.loadP95=Long.parseLong(m.group(4));p.ttftMed=Long.parseLong(m.group(5));p.ttftP95=Long.parseLong(m.group(6));p.tpsMed=Double.parseDouble(m.group(7));p.tpsP95=Double.parseDouble(m.group(8));p.totalMed=Long.parseLong(m.group(9));p.totalP95=Long.parseLong(m.group(10));p.temp=Double.parseDouble(m.group(11));p.score=Double.parseDouble(m.group(12));out.add(p);}return out;}
    private void addMetric(LinearLayout root,String name,double value,double max,boolean higherBetter,String text){TextView t=small(root);double ratio=max<=0?0:value/max;if(!higherBetter)ratio=1.0-ratio;int n=Math.max(1,Math.min(20,(int)Math.round(ratio*20)));StringBuilder bar=new StringBuilder();for(int i=0;i<n;i++)bar.append('█');for(int i=n;i<20;i++)bar.append('░');t.setText(name+"  "+bar+"  "+text);}
    private TextView card(LinearLayout root){TextView t=new TextView(this);t.setTextSize(15);t.setTextColor(Color.rgb(30,35,45));t.setPadding(dp(12),dp(12),dp(12),dp(12));t.setBackgroundColor(Color.WHITE);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);lp.setMargins(0,dp(10),0,0);root.addView(t,lp);return t;}
    private TextView small(LinearLayout root){TextView t=new TextView(this);t.setTextSize(13);t.setTextColor(Color.rgb(65,72,84));t.setPadding(dp(8),dp(4),dp(8),dp(4));root.addView(t);return t;}
    private double maxTps(List<P> x){double m=0;for(P p:x)m=Math.max(m,p.tpsMed);return m;}
    private double maxTtft(List<P> x){double m=0;for(P p:x)m=Math.max(m,p.ttftP95);return m;}
    private double maxTotal(List<P> x){double m=0;for(P p:x)m=Math.max(m,p.totalP95);return m;}
    private double maxTemp(List<P> x){double m=0;for(P p:x)m=Math.max(m,p.temp);return m;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
