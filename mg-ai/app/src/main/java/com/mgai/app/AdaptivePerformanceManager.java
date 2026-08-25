package com.mgai.app;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import java.util.ArrayDeque;
import java.util.Deque;

public final class AdaptivePerformanceManager {
    public static final class Profile {
        public final String name;
        public final int contextSize;
        public final int threads;
        public final int maxTokens;
        public final float temperatureC;
        public final String reason;
        Profile(String name,int contextSize,int threads,int maxTokens,float temperatureC,String reason){
            this.name=name;this.contextSize=contextSize;this.threads=threads;this.maxTokens=maxTokens;this.temperatureC=temperatureC;this.reason=reason;
        }
        public String summary(){
            String temp=temperatureC>0?String.format(java.util.Locale.US,"%.1f°C",temperatureC):"ısı n/a";
            return name+" • "+threads+" thread • ctx "+contextSize+" • max "+maxTokens+" • "+temp+" • "+reason;
        }
    }

    private static final long MIN_PROFILE_HOLD_MS=90_000L;
    private static final float TEMP_HYSTERESIS_C=1.5f;
    private static final Deque<String> history=new ArrayDeque<>();
    private static volatile Profile activeProfile;
    private static volatile long activeSince=0L;
    private AdaptivePerformanceManager(){}

    public static float batteryTemperatureC(Context c){
        if(c==null)return -1f;
        try{
            Intent i=c.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if(i==null)return -1f;
            int t=i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,-1);
            return t>0?t/10f:-1f;
        }catch(Throwable ignored){return -1f;}
    }

    private static Profile rawChoose(Context c){
        int cores=Math.max(4,Runtime.getRuntime().availableProcessors());
        float temp=batteryTemperatureC(c);
        LocalInferenceBridge.Metrics m=LocalInferenceBridge.lastMetrics();
        double tps=m==null?0.0:m.tokensPerSecond;
        long ttft=m==null?0:m.ttftMs;

        if(temp>=43f)return new Profile("SERİN",2048,Math.max(2,cores/3),256,temp,"yüksek sıcaklık");
        if(temp>=39f)return new Profile("DENGELİ",3072,Math.max(2,cores/2),384,temp,"ısı kontrolü");
        if(ttft>2500 || (tps>0 && tps<6.0))return new Profile("HIZLI",3072,Math.max(3,cores-2),384,temp,"yüksek TTFT / düşük token hızı");
        if(tps>=12.0 && (temp<0 || temp<37f))return new Profile("KALİTE",4096,Math.max(3,cores-2),512,temp,"performans payı mevcut");
        return new Profile("DENGELİ",4096,Math.max(2,cores-2),384,temp,"varsayılan adaptif profil");
    }

    public static synchronized Profile choose(Context c){
        Profile candidate=rawChoose(c);
        long now=System.currentTimeMillis();
        if(activeProfile==null){activate(candidate,now,"ilk profil");return activeProfile;}
        if(candidate.name.equals(activeProfile.name))return activeProfile;

        boolean emergencyHeat=candidate.temperatureC>=43f;
        boolean holdExpired=(now-activeSince)>=MIN_PROFILE_HOLD_MS;
        float currentTemp=activeProfile.temperatureC;
        float newTemp=candidate.temperatureC;
        boolean meaningfulTempDelta=currentTemp<=0 || newTemp<=0 || Math.abs(newTemp-currentTemp)>=TEMP_HYSTERESIS_C;

        if(emergencyHeat || (holdExpired && meaningfulTempDelta)){
            activate(candidate,now,candidate.reason);
        }
        return activeProfile;
    }

    private static void activate(Profile p,long now,String why){
        activeProfile=new Profile(p.name,p.contextSize,p.threads,p.maxTokens,p.temperatureC,why);
        activeSince=now;
        history.addFirst(now+" • "+activeProfile.summary());
        while(history.size()>8)history.removeLast();
    }

    public static synchronized String historySummary(){
        if(history.isEmpty())return "Profil geçmişi: boş";
        StringBuilder sb=new StringBuilder("Profil geçmişi:");
        for(String h:history)sb.append("\n").append(h);
        return sb.toString();
    }

    public static long activeAgeMs(){return activeProfile==null?0:Math.max(0,System.currentTimeMillis()-activeSince);}
    public static Profile active(){return activeProfile;}
}
