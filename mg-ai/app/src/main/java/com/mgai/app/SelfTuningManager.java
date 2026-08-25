package com.mgai.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class SelfTuningManager {
    public static final class LearnedProfile {
        public final int contextSize;
        public final int threads;
        public final int maxTokens;
        public final double score;
        public final int samples;
        LearnedProfile(int contextSize,int threads,int maxTokens,double score,int samples){
            this.contextSize=contextSize;this.threads=threads;this.maxTokens=maxTokens;this.score=score;this.samples=samples;
        }
        public String summary(){return String.format(Locale.US,"Öğrenilmiş • %d thread • ctx %d • max %d • skor %.2f • %d örnek",threads,contextSize,maxTokens,score,samples);}
    }

    private static final String PREF="mg_ai_self_tuning";
    private static final int MIN_SAMPLES=6;
    private static final int MAX_SAMPLES=40;
    private static final int MAX_TRENDS=20;
    private SelfTuningManager(){}

    private static SharedPreferences p(Context c){return c.getApplicationContext().getSharedPreferences(PREF,Context.MODE_PRIVATE);}

    public static synchronized void observe(Context c, AdaptivePerformanceManager.Profile profile, LocalInferenceBridge.Metrics m){
        if(c==null||profile==null||m==null||m.totalMs<=0||m.generatedTokens<4)return;
        float temp=AdaptivePerformanceManager.batteryTemperatureC(c);
        if(temp>=43f)return;
        double heatPenalty=temp>38f?(temp-38f)*0.9:0.0;
        double ttftPenalty=Math.max(0,m.ttftMs-1200)/1000.0;
        double score=m.tokensPerSecond-(heatPenalty+ttftPenalty);
        String key=key(profile.contextSize,profile.threads);
        SharedPreferences sp=p(c);
        int n=Math.min(MAX_SAMPLES,sp.getInt(key+"_n",0)+1);
        double old=Double.longBitsToDouble(sp.getLong(key+"_avg",Double.doubleToRawLongBits(score)));
        if(n==1)old=score;
        double avg=old+(score-old)/n;
        sp.edit().putInt(key+"_n",n).putLong(key+"_avg",Double.doubleToRawLongBits(avg)).putInt(key+"_max",profile.maxTokens).apply();
        recomputeBest(c);
    }

    private static String key(int ctx,int threads){return "c"+ctx+"_t"+threads;}

    private static void recomputeBest(Context c){
        SharedPreferences sp=p(c);
        int cores=Math.max(4,Runtime.getRuntime().availableProcessors());
        int[] contexts={2048,3072,4096};
        int[] threads={Math.max(2,cores/3),Math.max(2,cores/2),Math.max(3,cores-2)};
        double best=-1e9;int bestCtx=0,bestThreads=0,bestMax=384,bestN=0;
        for(int ctx:contexts){for(int th:threads){
            String k=key(ctx,th);int n=sp.getInt(k+"_n",0);if(n<MIN_SAMPLES)continue;
            double avg=Double.longBitsToDouble(sp.getLong(k+"_avg",Double.doubleToRawLongBits(-1e9)));
            if(avg>best){best=avg;bestCtx=ctx;bestThreads=th;bestMax=sp.getInt(k+"_max",384);bestN=n;}
        }}
        if(bestCtx>0){sp.edit().putInt("best_ctx",bestCtx).putInt("best_threads",bestThreads).putInt("best_max",bestMax).putLong("best_score",Double.doubleToRawLongBits(best)).putInt("best_samples",bestN).putString("best_source","pasif öğrenme").apply();}
    }

    public static synchronized void saveBenchmarkWinner(Context c,int contextSize,int threads,int maxTokens,double score,String report){
        if(c==null)return;
        long now=System.currentTimeMillis();
        SharedPreferences sp=p(c);
        sp.edit().putInt("best_ctx",contextSize).putInt("best_threads",threads).putInt("best_max",maxTokens).putLong("best_score",Double.doubleToRawLongBits(score)).putInt("best_samples",1).putString("best_source","aktif benchmark").putString("benchmark_report",report==null?"":report).putLong("benchmark_at",now).apply();
        appendTrend(c,now,contextSize,threads,score,report);
    }

    private static void appendTrend(Context c,long at,int ctx,int threads,double score,String report){
        SharedPreferences sp=p(c);
        int count=Math.min(MAX_TRENDS,sp.getInt("trend_count",0));
        for(int i=count;i>=1;i--){
            String prev=sp.getString("trend_"+(i-1),"");
            if(!prev.isEmpty() && i<MAX_TRENDS)sp.edit().putString("trend_"+i,prev).apply();
        }
        String model="model";
        try{File f=LocalModelManager.activeModel(c);if(f!=null)model=f.getName();}catch(Throwable ignored){}
        double tps=parseWinnerMetric(report,"tok/sn med/p95 ",'/');
        double ttft=parseWinnerMetric(report,"TTFT med/p95 ",'/');
        double total=parseWinnerMetric(report,"toplam med/p95 ",'/');
        double temp=parseWinnerMetric(report,"ΔT med ",'°');
        String entry=at+"\t"+clean(model)+"\t"+ctx+"\t"+threads+"\t"+String.format(Locale.US,"%.3f",score)+"\t"+String.format(Locale.US,"%.3f",tps)+"\t"+String.format(Locale.US,"%.0f",ttft)+"\t"+String.format(Locale.US,"%.0f",total)+"\t"+String.format(Locale.US,"%.2f",temp);
        sp.edit().putString("trend_0",entry).putInt("trend_count",Math.min(MAX_TRENDS,count+1)).apply();
    }

    private static String clean(String s){return s==null?"":s.replace('\t',' ').replace('\n',' ');}

    private static double parseWinnerMetric(String report,String key,char endChar){
        if(report==null)return 0;
        int k=report.lastIndexOf("KAZANAN:");
        String src=k>=0?report.substring(k):report;
        int i=src.indexOf(key);if(i<0)return 0;i+=key.length();
        int e=src.indexOf(endChar,i);if(e<0)e=src.indexOf(' ',i);if(e<0)e=src.length();
        try{return Double.parseDouble(src.substring(i,e).trim());}catch(Exception ignored){return 0;}
    }

    public static LearnedProfile learned(Context c){
        if(c==null)return null;SharedPreferences sp=p(c);int ctx=sp.getInt("best_ctx",0);if(ctx<=0)return null;
        return new LearnedProfile(ctx,sp.getInt("best_threads",4),sp.getInt("best_max",384),Double.longBitsToDouble(sp.getLong("best_score",Double.doubleToRawLongBits(0))),sp.getInt("best_samples",0));
    }

    public static String summary(Context c){
        LearnedProfile l=learned(c);if(l==null)return "Self-tuning: öğreniyor (en az 6 örnek/profil veya aktif benchmark)";
        return "Self-tuning: "+l.summary()+" • kaynak: "+p(c).getString("best_source","bilinmiyor");
    }

    public static String benchmarkReport(Context c){return c==null?"":p(c).getString("benchmark_report","");}

    public static String trendSummary(Context c){
        if(c==null)return "Benchmark geçmişi yok.";
        SharedPreferences sp=p(c);int n=sp.getInt("trend_count",0);if(n<=0)return "Benchmark geçmişi yok.";
        StringBuilder sb=new StringBuilder("TARİH | MODEL | PROFİL | SKOR | TOK/S | TTFT | TOPLAM | ΔT");
        SimpleDateFormat df=new SimpleDateFormat("dd.MM HH:mm",Locale.getDefault());
        for(int i=0;i<n;i++){
            String raw=sp.getString("trend_"+i,"");String[] a=raw.split("\\t");if(a.length<9)continue;
            try{sb.append("\n").append(df.format(new Date(Long.parseLong(a[0])))).append(" | ").append(a[1]).append(" | ").append(a[2]).append("/").append(a[3]).append(" | ").append(a[4]).append(" | ").append(a[5]).append(" | ").append(a[6]).append(" ms | ").append(a[7]).append(" ms | ").append(a[8]).append("°C");}catch(Exception ignored){}
        }
        return sb.toString();
    }

    public static String trendInsight(Context c){
        if(c==null)return "Trend verisi yok.";
        SharedPreferences sp=p(c);int n=sp.getInt("trend_count",0);if(n<2)return "Trend yorumu için en az 2 benchmark gerekir.";
        String[] newest=sp.getString("trend_0","").split("\\t");
        String[] oldest=sp.getString("trend_"+(n-1),"").split("\\t");
        if(newest.length<9||oldest.length<9)return "Trend verisi okunamadı.";
        try{
            double tpsN=Double.parseDouble(newest[5]),tpsO=Double.parseDouble(oldest[5]);
            double ttftN=Double.parseDouble(newest[6]),ttftO=Double.parseDouble(oldest[6]);
            double tpsPct=tpsO==0?0:(tpsN-tpsO)*100.0/tpsO;
            double ttftPct=ttftO==0?0:(ttftN-ttftO)*100.0/ttftO;
            String speed=tpsPct>5?"hızlandı":tpsPct<-5?"yavaşladı":"benzer hızda";
            String latency=ttftPct>8?"ilk token gecikmesi arttı":ttftPct<-8?"ilk token gecikmesi azaldı":"TTFT benzer";
            return String.format(Locale.US,"İlk → son benchmark: token hızı %+.1f%% (%s), TTFT %+.1f%% (%s).",tpsPct,speed,ttftPct,latency);
        }catch(Exception e){return "Trend yorumu üretilemedi.";}
    }

    public static String profileTable(Context c){
        if(c==null)return "Veri yok";
        SharedPreferences sp=p(c);int cores=Math.max(4,Runtime.getRuntime().availableProcessors());
        int[] contexts={2048,3072,4096};int[] threads={Math.max(2,cores/3),Math.max(2,cores/2),Math.max(3,cores-2)};
        StringBuilder sb=new StringBuilder("ctx | thread | örnek | ort. skor | max token");
        for(int ctx:contexts){for(int th:threads){String k=key(ctx,th);int n=sp.getInt(k+"_n",0);double avg=n>0?Double.longBitsToDouble(sp.getLong(k+"_avg",Double.doubleToRawLongBits(0))):0;sb.append(String.format(Locale.US,"\n%d | %d | %d | %.2f | %d",ctx,th,n,avg,sp.getInt(k+"_max",384)));}}
        return sb.toString();
    }

    public static String selectionReason(Context c){
        LearnedProfile l=learned(c);
        if(l==null)return "Henüz yeterli öğrenme verisi yok. Normal kullanım örnekleri birikiyor veya aktif benchmark başlatılabilir.";
        return "Seçilen profil en yüksek hız/ısı düzeltilmiş skora sahip. Kaynak: "+p(c).getString("best_source","bilinmiyor")+". Termal güvenlik 43°C ve üzerinde bu seçimi geçici olarak geçersiz kılabilir.";
    }

    public static void reset(Context c){if(c!=null)p(c).edit().clear().apply();}
}
