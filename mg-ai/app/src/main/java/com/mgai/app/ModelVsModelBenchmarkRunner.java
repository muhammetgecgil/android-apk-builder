package com.mgai.app;

import android.content.Context;
import java.io.File;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ModelVsModelBenchmarkRunner {
    public interface Listener {void onProgress(String text);void onComplete(String report);void onError(String message);}
    private static final int PERF_ROUNDS=2;
    private static final String PREFS="mg_model_compare", KEY_REPORT="last_report";
    private ModelVsModelBenchmarkRunner(){}
    private static final class Result {final File model;final long loadMed,ttftMed,totalMed;final double tpsMed,tempRiseMed,performance,quality,overall;final String qualityDetail;Result(File m,long l,long t,long total,double tps,double rise,double perf,double q,double o,String d){model=m;loadMed=l;ttftMed=t;totalMed=total;tpsMed=tps;tempRiseMed=rise;performance=perf;quality=q;overall=o;qualityDetail=d;}}
    private static final class QualityResult {final double score;final String detail;QualityResult(double s,String d){score=s;detail=d;}}
    public static String lastReport(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY_REPORT,"");}

    public static void run(Context c,Listener listener){
        Context app=c.getApplicationContext();
        new Thread(()->{
            File original=LocalModelManager.activeModel(app);boolean winnerActivated=false;
            try{
                List<File> models=LocalModelManager.installedModels(app);if(models.size()<2)throw new IllegalStateException("Model karşılaştırması için en az 2 kurulu GGUF gerekir.");if(!LocalInferenceBridge.nativeAvailable())throw new IllegalStateException("llama.cpp native runtime hazır değil.");
                AiModeManager.Mode mode=AiModeManager.get(app);AdaptivePerformanceManager.Profile p=AdaptivePerformanceManager.choose(app);
                int ctx=Math.min(p.contextSize,AiModeManager.contextCap(mode));int threads=Math.min(p.threads,AiModeManager.threadCap(app,mode));
                double qw=AiModeManager.qualityWeight(mode),pw=AiModeManager.performanceWeight(mode);
                String perfPrompt="Kısa benchmark testi. Türkçe olarak üç maddede mobil yapay zekada hız, doğruluk ve enerji verimliliği dengesini açıkla.";List<Result> results=new ArrayList<>();
                for(int i=0;i<models.size();i++){
                    File m=models.get(i);long[] loads=new long[PERF_ROUNDS],ttfts=new long[PERF_ROUNDS],totals=new long[PERF_ROUNDS];double[] tps=new double[PERF_ROUNDS],rises=new double[PERF_ROUNDS];
                    for(int r=0;r<PERF_ROUNDS;r++){
                        thermalGuard(app);float before=AdaptivePerformanceManager.batteryTemperatureC(app);if(listener!=null)listener.onProgress("Performans • "+AiModeManager.title(mode)+" • model "+(i+1)+"/"+models.size()+" • tur "+(r+1)+"/"+PERF_ROUNDS+": "+m.getName());long engine=0;
                        try{long s=System.nanoTime();engine=LocalInferenceBridge.createEngine(m.getAbsolutePath(),ctx,threads);loads[r]=(System.nanoTime()-s)/1_000_000L;if(engine==0)throw new IllegalStateException("Engine açılamadı: "+m.getName());LocalInferenceBridge.generate(engine,perfPrompt,Math.min(128,AiModeManager.tokenBudget(mode)),0.2f);LocalInferenceBridge.Metrics lm=LocalInferenceBridge.lastMetrics();ttfts[r]=lm.ttftMs;totals[r]=lm.totalMs;tps[r]=lm.tokensPerSecond;float after=AdaptivePerformanceManager.batteryTemperatureC(app);rises[r]=(before>0&&after>0)?Math.max(0,after-before):0;}finally{if(engine!=0)try{LocalInferenceBridge.destroyEngine(engine);}catch(Throwable ignored){}}
                    }
                    long loadMed=median(loads),ttftMed=median(ttfts),totalMed=median(totals);double tpsMed=median(tps),riseMed=median(rises);double performance=performanceScore(tpsMed,ttftMed,loadMed,riseMed,mode);QualityResult qr=qualityScore(app,m,ctx,threads,listener,i+1,models.size());double overall=qr.score*qw+performance*pw;results.add(new Result(m,loadMed,ttftMed,totalMed,tpsMed,riseMed,performance,qr.score,overall,qr.detail));
                }
                results.sort(Comparator.comparingDouble((Result r)->r.overall).reversed());StringBuilder report=new StringBuilder("AKILLI MODEL SEÇİCİ • v0.58\nMod: ").append(AiModeManager.title(mode)).append(String.format(Locale.US," • kalite %d%% + performans %d%%\n",Math.round(qw*100),Math.round(pw*100))).append("Profil: ctx ").append(ctx).append(" • ").append(threads).append(" thread • max ").append(AiModeManager.tokenBudget(mode)).append(" token");
                for(int i=0;i<results.size();i++){Result r=results.get(i);report.append(String.format(Locale.US,"\n\n%d. %s\nGenel %.1f/100 • kalite %.1f/100 • performans %.1f/100\n%.1f tok/sn • TTFT %d ms • toplam %d ms • load %d ms • ΔT %.1f°C\nKalite testleri: %s",i+1,r.model.getName(),r.overall,r.quality,r.performance,r.tpsMed,r.ttftMed,r.totalMed,r.loadMed,r.tempRiseMed,r.qualityDetail));}
                Result best=results.get(0);LocalModelManager.activateExisting(app,best.model);winnerActivated=true;report.append("\n\n★ ").append(AiModeManager.title(mode).toUpperCase(Locale.US)).append(" MODUNDA EN İYİ MODEL AKTİF: ").append(best.model.getName()).append(String.format(Locale.US," • %.1f/100",best.overall));app.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY_REPORT,report.toString()).apply();if(listener!=null)listener.onComplete(report.toString());
            }catch(Throwable t){if(!winnerActivated&&original!=null)try{LocalModelManager.activateExisting(app,original);}catch(Throwable ignored){}if(listener!=null)listener.onError(t.getMessage()==null?t.toString():t.getMessage());}
        },"mg-ai-mode-aware-selector").start();
    }

    private static QualityResult qualityScore(Context app,File model,int ctx,int threads,Listener listener,int modelIndex,int modelCount)throws Exception{
        String[] prompts={"Yalnızca sonucu yaz. 17 ile 23 sayılarının çarpımı kaçtır?","Talimat testi: Sadece MAVİ kelimesini yaz; başka hiçbir şey yazma.","Ali, Ayşe'den uzundur. Ayşe, Mehmet'ten uzundur. En kısa kişi kimdir? Sadece adı yaz.","Havuç, elma ve muz seçeneklerinden hangisi sebzedir? Sadece tek kelimeyle cevap ver.","XZ-991 adlı tamamen uydurma bir gezegenin başkenti nedir? Doğrulanmış bilgin yoksa bunu açıkça söyle; bilgi uydurma."};double points=0;StringBuilder detail=new StringBuilder();long engine=0;
        try{thermalGuard(app);engine=LocalInferenceBridge.createEngine(model.getAbsolutePath(),Math.max(2048,Math.min(ctx,3072)),threads);if(engine==0)throw new IllegalStateException("Kalite testi engine açılamadı: "+model.getName());for(int i=0;i<prompts.length;i++){thermalGuard(app);if(listener!=null)listener.onProgress("Kalite • model "+modelIndex+"/"+modelCount+" • test "+(i+1)+"/"+prompts.length+": "+model.getName());String answer=LocalInferenceBridge.generate(engine,prompts[i],64,0.0f);boolean pass=qualityPass(i,answer);if(pass)points+=20;if(detail.length()>0)detail.append(" • ");detail.append(i+1).append(pass?"✓":"✗");}}finally{if(engine!=0)try{LocalInferenceBridge.destroyEngine(engine);}catch(Throwable ignored){}}return new QualityResult(points,detail.toString());
    }
    private static boolean qualityPass(int test,String answer){String n=norm(answer);if(test==0)return n.contains("391");if(test==1)return compact(n).equals("mavi");if(test==2)return n.contains("mehmet")&&wordCount(n)<=8;if(test==3)return n.contains("havuc")&&wordCount(n)<=8;if(test==4)return n.contains("bilmiyorum")||n.contains("bilinmiyor")||n.contains("bilgi yok")||n.contains("dogrulanmis bilgi")||n.contains("uydurma")||n.contains("belirlenemez");return false;}
    private static String norm(String s){if(s==null)return "";String x=Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.US);return x.replaceAll("[^a-z0-9çğıöşü\\s]"," ").replaceAll("\\s+"," ").trim();}
    private static String compact(String s){return s.replaceAll("[^a-z0-9çğıöşü]","");}private static int wordCount(String s){return s==null||s.trim().isEmpty()?0:s.trim().split("\\s+").length;}
    private static void thermalGuard(Context c){float t=AdaptivePerformanceManager.batteryTemperatureC(c);if(t>=43f)throw new IllegalStateException("Termal güvenlik: test durduruldu ("+String.format(Locale.US,"%.1f°C",t)+").");}
    private static double performanceScore(double tps,long ttft,long load,double rise,AiModeManager.Mode mode){double speed=clamp(tps/20.0*100.0,0,100),latency=clamp(100.0-(Math.max(0,ttft-400)/20.0),0,100),loading=clamp(100.0-(Math.max(0,load-1000)/50.0),0,100),thermal=clamp(100.0-rise*30.0,0,100);if(mode==AiModeManager.Mode.SPEED)return speed*0.65+latency*0.25+thermal*0.05+loading*0.05;if(mode==AiModeManager.Mode.COOL)return thermal*0.50+speed*0.25+latency*0.15+loading*0.10;return speed*0.50+latency*0.25+thermal*0.15+loading*0.10;}
    private static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}private static long median(long[] a){long[] x=a.clone();Arrays.sort(x);return x[x.length/2];}private static double median(double[] a){double[] x=a.clone();Arrays.sort(x);return x[x.length/2];}
}
