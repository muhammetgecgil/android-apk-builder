package com.mgai.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ModelVsModelBenchmarkRunner {
    public interface Listener {void onProgress(String text);void onComplete(String report);void onError(String message);}
    private static final int ROUNDS=2;
    private static final String PREFS="mg_model_compare";
    private static final String KEY_REPORT="last_report";
    private ModelVsModelBenchmarkRunner(){}

    private static final class Result {
        final File model;final long loadMed,ttftMed,totalMed;final double tpsMed,tempRiseMed,score;
        Result(File model,long loadMed,long ttftMed,long totalMed,double tpsMed,double tempRiseMed,double score){this.model=model;this.loadMed=loadMed;this.ttftMed=ttftMed;this.totalMed=totalMed;this.tpsMed=tpsMed;this.tempRiseMed=tempRiseMed;this.score=score;}
    }

    public static String lastReport(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY_REPORT,"");}

    public static void run(Context c,Listener listener){
        Context app=c.getApplicationContext();
        new Thread(()->{
            File original=LocalModelManager.activeModel(app);
            boolean winnerActivated=false;
            try{
                List<File> models=LocalModelManager.installedModels(app);
                if(models.size()<2)throw new IllegalStateException("Model karşılaştırması için en az 2 kurulu GGUF gerekir.");
                if(!LocalInferenceBridge.nativeAvailable())throw new IllegalStateException("llama.cpp native runtime hazır değil.");
                AdaptivePerformanceManager.Profile p=AdaptivePerformanceManager.choose(app);
                int ctx=p.contextSize,threads=p.threads;
                String prompt="Kısa model karşılaştırma testi. Türkçe olarak üç maddede mobil yapay zekada hız, doğruluk ve enerji verimliliği dengesini açıkla.";
                List<Result> results=new ArrayList<>();
                for(int i=0;i<models.size();i++){
                    File m=models.get(i);
                    long[] loads=new long[ROUNDS],ttfts=new long[ROUNDS],totals=new long[ROUNDS];
                    double[] tps=new double[ROUNDS],rises=new double[ROUNDS];
                    for(int r=0;r<ROUNDS;r++){
                        float before=AdaptivePerformanceManager.batteryTemperatureC(app);
                        if(before>=43f)throw new IllegalStateException("Termal güvenlik: benchmark durduruldu ("+String.format(Locale.US,"%.1f°C",before)+").");
                        if(listener!=null)listener.onProgress("Model "+(i+1)+"/"+models.size()+" • tur "+(r+1)+"/"+ROUNDS+": "+m.getName());
                        long engine=0;
                        try{
                            long loadStart=System.nanoTime();
                            engine=LocalInferenceBridge.createEngine(m.getAbsolutePath(),ctx,threads);
                            loads[r]=(System.nanoTime()-loadStart)/1_000_000L;
                            if(engine==0)throw new IllegalStateException("Engine açılamadı: "+m.getName());
                            LocalInferenceBridge.generate(engine,prompt,128,0.2f);
                            LocalInferenceBridge.Metrics lm=LocalInferenceBridge.lastMetrics();
                            ttfts[r]=lm.ttftMs;totals[r]=lm.totalMs;tps[r]=lm.tokensPerSecond;
                            float after=AdaptivePerformanceManager.batteryTemperatureC(app);
                            rises[r]=(before>0&&after>0)?Math.max(0,after-before):0;
                        } finally {if(engine!=0)try{LocalInferenceBridge.destroyEngine(engine);}catch(Throwable ignored){}}
                    }
                    long loadMed=median(loads),ttftMed=median(ttfts),totalMed=median(totals);
                    double tpsMed=median(tps),riseMed=median(rises);
                    double sizePenalty=Math.max(0,m.length()/1073741824.0-1.0)*0.20;
                    double score=tpsMed-(Math.max(0,ttftMed-1200)/900.0)-(riseMed*2.2)-(loadMed/6000.0)-sizePenalty;
                    results.add(new Result(m,loadMed,ttftMed,totalMed,tpsMed,riseMed,score));
                }
                results.sort(Comparator.comparingDouble((Result r)->r.score).reversed());
                StringBuilder report=new StringBuilder("MODEL-VS-MODEL BENCHMARK • ").append(ROUNDS).append(" tur/model\nProfil: ctx ").append(ctx).append(" • ").append(threads).append(" thread");
                for(int i=0;i<results.size();i++){Result r=results.get(i);report.append(String.format(Locale.US,"\n%d. %s • %.2f GB • %.1f tok/sn • TTFT %d ms • toplam %d ms • load %d ms • ΔT %.1f°C • skor %.2f",i+1,r.model.getName(),r.model.length()/1073741824.0,r.tpsMed,r.ttftMed,r.totalMed,r.loadMed,r.tempRiseMed,r.score));}
                Result best=results.get(0);
                LocalModelManager.activateExisting(app,best.model);winnerActivated=true;
                report.append("\n\n★ OTOMATİK AKTİF MODEL: ").append(best.model.getName()).append(String.format(Locale.US," • skor %.2f",best.score));
                report.append("\nNot: Bu sıralama cihaz performansı/ısı dengesi içindir; model yanıt kalitesini tek başına ölçmez.");
                app.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY_REPORT,report.toString()).apply();
                if(listener!=null)listener.onComplete(report.toString());
            }catch(Throwable t){
                if(!winnerActivated&&original!=null)try{LocalModelManager.activateExisting(app,original);}catch(Throwable ignored){}
                if(listener!=null)listener.onError(t.getMessage()==null?t.toString():t.getMessage());
            }
        },"mg-ai-model-vs-model").start();
    }

    private static long median(long[] a){long[] x=a.clone();Arrays.sort(x);return x[x.length/2];}
    private static double median(double[] a){double[] x=a.clone();Arrays.sort(x);return x[x.length/2];}
}
