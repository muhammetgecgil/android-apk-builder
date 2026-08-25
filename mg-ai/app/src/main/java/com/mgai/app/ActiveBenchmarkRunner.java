package com.mgai.app;

import android.content.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ActiveBenchmarkRunner {
    public interface Listener {
        void onProgress(String text);
        void onComplete(String report);
        void onError(String message);
    }

    private ActiveBenchmarkRunner(){}

    private static final class Candidate {
        final int ctx,threads,max;
        Candidate(int ctx,int threads,int max){this.ctx=ctx;this.threads=threads;this.max=max;}
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
                double bestScore=-1e9;Candidate best=null;
                StringBuilder report=new StringBuilder("AKTİF BENCHMARK SONUÇLARI");

                for(int i=0;i<cs.size();i++){
                    Candidate x=cs.get(i);
                    float before=AdaptivePerformanceManager.batteryTemperatureC(app);
                    if(before>=43f)throw new IllegalStateException("Benchmark termal güvenlik nedeniyle durduruldu: "+String.format(Locale.US,"%.1f°C",before));
                    if(listener!=null)listener.onProgress("Profil "+(i+1)+"/"+cs.size()+": ctx "+x.ctx+" • "+x.threads+" thread");
                    long engine=0;
                    try{
                        engine=LocalInferenceBridge.createEngine(model.getAbsolutePath(),x.ctx,x.threads);
                        if(engine==0)throw new IllegalStateException("Engine oluşturulamadı: ctx="+x.ctx+" threads="+x.threads);
                        LocalInferenceBridge.generate(engine,prompt,x.max,0.2f);
                        LocalInferenceBridge.Metrics m=LocalInferenceBridge.lastMetrics();
                        float after=AdaptivePerformanceManager.batteryTemperatureC(app);
                        double tempRise=(before>0&&after>0)?Math.max(0,after-before):0;
                        double score=m.tokensPerSecond-(Math.max(0,m.ttftMs-1000)/900.0)-(tempRise*2.2);
                        report.append(String.format(Locale.US,"\nctx %d • %d thread • TTFT %d ms • %.1f tok/sn • toplam %d ms • ΔT %.1f°C • skor %.2f",x.ctx,x.threads,m.ttftMs,m.tokensPerSecond,m.totalMs,tempRise,score));
                        if(score>bestScore){bestScore=score;best=x;}
                    } finally {
                        if(engine!=0)try{LocalInferenceBridge.destroyEngine(engine);}catch(Throwable ignored){}
                    }
                }
                if(best==null)throw new IllegalStateException("Geçerli benchmark sonucu üretilemedi.");
                report.append(String.format(Locale.US,"\n\nKAZANAN: ctx %d • %d thread • skor %.2f",best.ctx,best.threads,bestScore));
                SelfTuningManager.saveBenchmarkWinner(app,best.ctx,best.threads,384,bestScore,report.toString());
                if(listener!=null)listener.onComplete(report.toString());
            }catch(Throwable t){if(listener!=null)listener.onError(t.getMessage()==null?t.toString():t.getMessage());}
        },"mg-ai-active-benchmark").start();
    }
}
