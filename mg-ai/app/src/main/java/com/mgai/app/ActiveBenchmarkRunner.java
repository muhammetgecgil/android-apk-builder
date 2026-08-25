package com.mgai.app;

import android.content.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ActiveBenchmarkRunner {
    public interface Listener {
        void onProgress(String text);
        void onComplete(String report);
        void onError(String message);
    }

    private ActiveBenchmarkRunner(){}
    private static final int ROUNDS=3;

    private static final class Candidate {
        final int ctx,threads,max;
        Candidate(int ctx,int threads,int max){this.ctx=ctx;this.threads=threads;this.max=max;}
    }

    private static final class Stats {
        final long loadMedian,loadP95,ttftMedian,ttftP95,totalMedian,totalP95;
        final double tpsMedian,tpsP95,tempRiseMedian,score;
        Stats(long loadMedian,long loadP95,long ttftMedian,long ttftP95,long totalMedian,long totalP95,double tpsMedian,double tpsP95,double tempRiseMedian,double score){
            this.loadMedian=loadMedian;this.loadP95=loadP95;this.ttftMedian=ttftMedian;this.ttftP95=ttftP95;this.totalMedian=totalMedian;this.totalP95=totalP95;this.tpsMedian=tpsMedian;this.tpsP95=tpsP95;this.tempRiseMedian=tempRiseMedian;this.score=score;
        }
    }

    public static void run(Context c,Listener listener){
        final Context app=c.getApplicationContext();
        new Thread(()->{
            try{
                File model=LocalModelManager.activeModel(app);
                if(model==null||!model.isFile())throw new IllegalStateException("Aktif model bulunamadı.");
                if(!LocalInferenceBridge.nativeAvailable())throw new IllegalStateException("llama.cpp native runtime hazır değil.");
                int cores=Math.max(4,Runtime.getRuntime().availableProcessors());
                List<Candidate> cs=new ArrayList<>();
                cs.add(new Candidate(2048,Math.max(2,cores/3),128));
                cs.add(new Candidate(3072,Math.max(2,cores/2),128));
                cs.add(new Candidate(3072,Math.max(3,cores-2),128));
                cs.add(new Candidate(4096,Math.max(3,cores-2),128));

                String prompt="Kısa benchmark testi. Türkçe olarak üç maddede: hız, doğruluk ve enerji verimliliği arasındaki dengeyi açıkla.";
                double bestScore=-1e9;Candidate best=null;Stats bestStats=null;
                StringBuilder report=new StringBuilder("AKTİF BENCHMARK SONUÇLARI • ").append(ROUNDS).append(" tur/profil");

                for(int i=0;i<cs.size();i++){
                    Candidate x=cs.get(i);
                    long[] loads=new long[ROUNDS],ttfts=new long[ROUNDS],totals=new long[ROUNDS];
                    double[] tps=new double[ROUNDS],rises=new double[ROUNDS];
                    for(int r=0;r<ROUNDS;r++){
                        float before=AdaptivePerformanceManager.batteryTemperatureC(app);
                        if(before>=43f)throw new IllegalStateException("Benchmark termal güvenlik nedeniyle durduruldu: "+String.format(Locale.US,"%.1f°C",before));
                        if(listener!=null)listener.onProgress("Profil "+(i+1)+"/"+cs.size()+" • tur "+(r+1)+"/"+ROUNDS+": ctx "+x.ctx+" • "+x.threads+" thread");
                        long engine=0;
                        try{
                            long loadStart=System.nanoTime();
                            engine=LocalInferenceBridge.createEngine(model.getAbsolutePath(),x.ctx,x.threads);
                            loads[r]=(System.nanoTime()-loadStart)/1_000_000L;
                            if(engine==0)throw new IllegalStateException("Engine oluşturulamadı: ctx="+x.ctx+" threads="+x.threads);
                            LocalInferenceBridge.generate(engine,prompt,x.max,0.2f);
                            LocalInferenceBridge.Metrics m=LocalInferenceBridge.lastMetrics();
                            ttfts[r]=m.ttftMs;totals[r]=m.totalMs;tps[r]=m.tokensPerSecond;
                            float after=AdaptivePerformanceManager.batteryTemperatureC(app);
                            rises[r]=(before>0&&after>0)?Math.max(0,after-before):0;
                        } finally {
                            if(engine!=0)try{LocalInferenceBridge.destroyEngine(engine);}catch(Throwable ignored){}
                        }
                    }
                    Stats s=stats(loads,ttfts,totals,tps,rises);
                    report.append(String.format(Locale.US,"\nctx %d • %d thread • load med/p95 %d/%d ms • TTFT med/p95 %d/%d ms • tok/sn med/p95 %.1f/%.1f • toplam med/p95 %d/%d ms • ΔT med %.1f°C • skor %.2f",x.ctx,x.threads,s.loadMedian,s.loadP95,s.ttftMedian,s.ttftP95,s.tpsMedian,s.tpsP95,s.totalMedian,s.totalP95,s.tempRiseMedian,s.score));
                    if(s.score>bestScore){bestScore=s.score;best=x;bestStats=s;}
                }
                if(best==null||bestStats==null)throw new IllegalStateException("Geçerli benchmark sonucu üretilemedi.");
                report.append(String.format(Locale.US,"\n\nKAZANAN: ctx %d • %d thread • skor %.2f",best.ctx,best.threads,bestScore));
                SelfTuningManager.saveBenchmarkWinner(app,best.ctx,best.threads,384,bestScore,report.toString());
                BenchmarkTrendStore.add(app,model.getName(),best.ctx,best.threads,bestScore,bestStats.ttftP95,bestStats.tpsMedian,bestStats.totalP95,bestStats.tempRiseMedian);
                if(listener!=null)listener.onComplete(report.toString()+"\n"+BenchmarkTrendStore.trendSummary(app));
            }catch(Throwable t){if(listener!=null)listener.onError(t.getMessage()==null?t.toString():t.getMessage());}
        },"mg-ai-active-benchmark").start();
    }

    private static Stats stats(long[] loads,long[] ttfts,long[] totals,double[] tps,double[] rises){
        long loadMed=median(loads),load95=p95(loads),ttftMed=median(ttfts),ttft95=p95(ttfts),totalMed=median(totals),total95=p95(totals);
        double tpsMed=median(tps),tps95=p95(tps),riseMed=median(rises);
        double score=tpsMed-(Math.max(0,ttft95-1200)/900.0)-(riseMed*2.2)-(load95/6000.0);
        return new Stats(loadMed,load95,ttftMed,ttft95,totalMed,total95,tpsMed,tps95,riseMed,score);
    }
    private static long median(long[] a){long[] x=a.clone();Arrays.sort(x);return x[x.length/2];}
    private static long p95(long[] a){long[] x=a.clone();Arrays.sort(x);return x[Math.min(x.length-1,(int)Math.ceil(x.length*0.95)-1)];}
    private static double median(double[] a){double[] x=a.clone();Arrays.sort(x);return x[x.length/2];}
    private static double p95(double[] a){double[] x=a.clone();Arrays.sort(x);return x[Math.min(x.length-1,(int)Math.ceil(x.length*0.95)-1)];}
}
