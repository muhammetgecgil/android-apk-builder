package com.mgai.app;

import android.content.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ModelVsModelBenchmarkRunner {
    public interface Listener {void onProgress(String text);void onComplete(String report);void onError(String message);}
    private ModelVsModelBenchmarkRunner(){}

    private static final class Result {
        final File model;final long loadMs,ttftMs,totalMs;final double tps,tempRise,score;
        Result(File model,long loadMs,long ttftMs,long totalMs,double tps,double tempRise,double score){this.model=model;this.loadMs=loadMs;this.ttftMs=ttftMs;this.totalMs=totalMs;this.tps=tps;this.tempRise=tempRise;this.score=score;}
    }

    public static void run(Context c,Listener listener){
        Context app=c.getApplicationContext();
        new Thread(()->{
            File original=null;
            try{
                List<File> models=LocalModelManager.installedModels(app);
                if(models.size()<2)throw new IllegalStateException("Model karşılaştırması için en az 2 kurulu GGUF gerekir.");
                if(!LocalInferenceBridge.nativeAvailable())throw new IllegalStateException("llama.cpp native runtime hazır değil.");
                original=LocalModelManager.activeModel(app);
                AdaptivePerformanceManager.Profile p=AdaptivePerformanceManager.choose(app);
                int ctx=p.contextSize,threads=p.threads;
                String prompt="Kısa model karşılaştırma testi. Türkçe olarak üç maddede mobil yapay zekada hız, doğruluk ve enerji verimliliği dengesini açıkla.";
                List<Result> results=new ArrayList<>();
                for(int i=0;i<models.size();i++){
                    File m=models.get(i);float before=AdaptivePerformanceManager.batteryTemperatureC(app);
                    if(before>=43f)throw new IllegalStateException("Termal güvenlik: benchmark durduruldu ("+String.format(Locale.US,"%.1f°C",before)+").");
                    if(listener!=null)listener.onProgress("Model "+(i+1)+"/"+models.size()+": "+m.getName());
                    long engine=0;long loadStart=System.nanoTime();
                    try{
                        engine=LocalInferenceBridge.createEngine(m.getAbsolutePath(),ctx,threads);
                        long loadMs=(System.nanoTime()-loadStart)/1_000_000L;
                        if(engine==0)throw new IllegalStateException("Engine açılamadı: "+m.getName());
                        LocalInferenceBridge.generate(engine,prompt,128,0.2f);
                        LocalInferenceBridge.Metrics lm=LocalInferenceBridge.lastMetrics();
                        float after=AdaptivePerformanceManager.batteryTemperatureC(app);double rise=(before>0&&after>0)?Math.max(0,after-before):0;
                        double sizePenalty=Math.max(0,m.length()/1073741824.0-1.0)*0.20;
                        double score=lm.tokensPerSecond-(Math.max(0,lm.ttftMs-1200)/900.0)-(rise*2.2)-(loadMs/6000.0)-sizePenalty;
                        results.add(new Result(m,loadMs,lm.ttftMs,lm.totalMs,lm.tokensPerSecond,rise,score));
                    } finally {if(engine!=0)try{LocalInferenceBridge.destroyEngine(engine);}catch(Throwable ignored){}}
                }
                results.sort(Comparator.comparingDouble((Result r)->r.score).reversed());
                StringBuilder report=new StringBuilder("MODEL-VS-MODEL BENCHMARK\nProfil: ctx ").append(ctx).append(" • ").append(threads).append(" thread");
                for(int i=0;i<results.size();i++){Result r=results.get(i);report.append(String.format(Locale.US,"\n%d. %s • %.2f GB • %.1f tok/sn • TTFT %d ms • toplam %d ms • load %d ms • ΔT %.1f°C • skor %.2f",i+1,r.model.getName(),r.model.length()/1073741824.0,r.tps,r.ttftMs,r.totalMs,r.loadMs,r.tempRise,r.score));}
                Result best=results.get(0);report.append("\n\nKAZANAN MODEL: ").append(best.model.getName()).append(String.format(Locale.US," • skor %.2f",best.score));
                if(listener!=null)listener.onComplete(report.toString());
            }catch(Throwable t){if(listener!=null)listener.onError(t.getMessage()==null?t.toString():t.getMessage());}
            finally{if(original!=null)try{LocalModelManager.activateExisting(app,original);}catch(Throwable ignored){}}
        },"mg-ai-model-vs-model").start();
    }
}
