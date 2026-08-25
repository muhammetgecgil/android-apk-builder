package com.mgai.app;

import android.content.Context;
import android.content.SharedPreferences;

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

    private static int[] contexts(){return new int[]{2048,3072,4096};}
    private static int[] threadCandidates(){int cores=Math.max(4,Runtime.getRuntime().availableProcessors());return new int[]{Math.max(2,cores/3),Math.max(2,cores/2),Math.max(3,cores-2)};}

    private static void recomputeBest(Context c){
        SharedPreferences sp=p(c);
        double best=-1e9;int bestCtx=0,bestThreads=0,bestMax=384,bestN=0;
        for(int ctx:contexts()){for(int th:threadCandidates()){
            String k=key(ctx,th);int n=sp.getInt(k+"_n",0);if(n<MIN_SAMPLES)continue;
            double avg=Double.longBitsToDouble(sp.getLong(k+"_avg",Double.doubleToRawLongBits(-1e9)));
            if(avg>best){best=avg;bestCtx=ctx;bestThreads=th;bestMax=sp.getInt(k+"_max",384);bestN=n;}
        }}
        if(bestCtx>0){sp.edit().putInt("best_ctx",bestCtx).putInt("best_threads",bestThreads).putInt("best_max",bestMax).putLong("best_score",Double.doubleToRawLongBits(best)).putInt("best_samples",bestN).apply();}
    }

    public static LearnedProfile learned(Context c){
        if(c==null)return null;SharedPreferences sp=p(c);int ctx=sp.getInt("best_ctx",0);if(ctx<=0)return null;
        return new LearnedProfile(ctx,sp.getInt("best_threads",4),sp.getInt("best_max",384),Double.longBitsToDouble(sp.getLong("best_score",Double.doubleToRawLongBits(0))),sp.getInt("best_samples",0));
    }

    public static String summary(Context c){LearnedProfile l=learned(c);return l==null?"Self-tuning: öğreniyor (en az 6 örnek/profil)":"Self-tuning: "+l.summary();}

    public static String profileTable(Context c){
        if(c==null)return "Self-tuning verisi yok.";
        SharedPreferences sp=p(c);StringBuilder sb=new StringBuilder("CTX   THREAD   ÖRNEK   SKOR   MAX\n");boolean any=false;
        for(int ctx:contexts()){for(int th:threadCandidates()){
            String k=key(ctx,th);int n=sp.getInt(k+"_n",0);if(n<=0)continue;any=true;
            double avg=Double.longBitsToDouble(sp.getLong(k+"_avg",Double.doubleToRawLongBits(0)));
            int max=sp.getInt(k+"_max",384);
            sb.append(String.format(Locale.US,"%-5d %-8d %-7d %-6.2f %d\n",ctx,th,n,avg,max));
        }}
        if(!any)sb.append("Henüz ölçüm yok. Normal kullanımla otomatik örnek birikecek.\n");
        return sb.toString().trim();
    }

    public static String selectionReason(Context c){
        LearnedProfile l=learned(c);
        if(l==null)return "Henüz kalıcı seçim yapılmadı. Her aday profil için en az "+MIN_SAMPLES+" geçerli cevap örneği gerektiğinde sistem öğrenmeye devam eder. Skor; token/s hızından yüksek sıcaklık ve yüksek TTFT cezaları düşülerek hesaplanır.";
        return String.format(Locale.US,"Seçilen profil ctx %d / %d thread. Ortalama skor %.2f ile yeterli örneğe sahip adaylar arasında en yüksek değeri verdi. Termal güvenlik 43°C ve üzerinde bu öğrenilmiş profilin önüne geçer.",l.contextSize,l.threads,l.score);
    }

    public static int minSamples(){return MIN_SAMPLES;}
    public static void reset(Context c){if(c!=null)p(c).edit().clear().apply();}
}
