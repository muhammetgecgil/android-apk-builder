package com.mgai.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class BenchmarkTrendStore {
    public static final class Entry {
        public final long at;
        public final String model;
        public final int ctx;
        public final int threads;
        public final double score;
        public final long ttftP95;
        public final double tpsMedian;
        public final long totalP95;
        public final double tempRise;
        Entry(long at,String model,int ctx,int threads,double score,long ttftP95,double tpsMedian,long totalP95,double tempRise){
            this.at=at;this.model=model;this.ctx=ctx;this.threads=threads;this.score=score;this.ttftP95=ttftP95;this.tpsMedian=tpsMedian;this.totalP95=totalP95;this.tempRise=tempRise;
        }
        public String date(){return new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date(at));}
        public String summary(){return String.format(Locale.US,"%s • %s • ctx %d • %d th • skor %.2f • %.1f tok/sn • p95 TTFT %d ms • p95 toplam %d ms • ΔT %.1f°C",date(),model,ctx,threads,score,tpsMedian,ttftP95,totalP95,tempRise);}
    }

    private static final String PREF="mg_ai_benchmark_trends";
    private static final int MAX=20;
    private BenchmarkTrendStore(){}
    private static SharedPreferences p(Context c){return c.getApplicationContext().getSharedPreferences(PREF,Context.MODE_PRIVATE);}

    public static synchronized void add(Context c,String model,int ctx,int threads,double score,long ttftP95,double tpsMedian,long totalP95,double tempRise){
        SharedPreferences sp=p(c);int n=Math.min(MAX,sp.getInt("count",0));
        for(int i=Math.min(MAX-1,n);i>=1;i--){copy(sp,i-1,i);}
        put(sp,0,new Entry(System.currentTimeMillis(),model==null?"model":model,ctx,threads,score,ttftP95,tpsMedian,totalP95,tempRise));
        sp.edit().putInt("count",Math.min(MAX,n+1)).apply();
    }
    private static void copy(SharedPreferences sp,int from,int to){
        Entry e=read(sp,from);if(e!=null)put(sp,to,e);
    }
    private static void put(SharedPreferences sp,int i,Entry e){
        String k="e"+i+"_";
        sp.edit().putLong(k+"at",e.at).putString(k+"model",e.model).putInt(k+"ctx",e.ctx).putInt(k+"threads",e.threads).putLong(k+"score",Double.doubleToRawLongBits(e.score)).putLong(k+"ttft",e.ttftP95).putLong(k+"tps",Double.doubleToRawLongBits(e.tpsMedian)).putLong(k+"total",e.totalP95).putLong(k+"temp",Double.doubleToRawLongBits(e.tempRise)).apply();
    }
    private static Entry read(SharedPreferences sp,int i){
        String k="e"+i+"_";long at=sp.getLong(k+"at",0);if(at<=0)return null;
        return new Entry(at,sp.getString(k+"model","model"),sp.getInt(k+"ctx",0),sp.getInt(k+"threads",0),Double.longBitsToDouble(sp.getLong(k+"score",Double.doubleToRawLongBits(0))),sp.getLong(k+"ttft",0),Double.longBitsToDouble(sp.getLong(k+"tps",Double.doubleToRawLongBits(0))),sp.getLong(k+"total",0),Double.longBitsToDouble(sp.getLong(k+"temp",Double.doubleToRawLongBits(0))));
    }
    public static List<Entry> entries(Context c){SharedPreferences sp=p(c);int n=Math.min(MAX,sp.getInt("count",0));List<Entry> out=new ArrayList<>();for(int i=0;i<n;i++){Entry e=read(sp,i);if(e!=null)out.add(e);}return out;}
    public static String trendSummary(Context c){List<Entry> es=entries(c);if(es.isEmpty())return "Trend: henüz benchmark geçmişi yok.";if(es.size()==1)return "Trend: ilk benchmark kaydı oluşturuldu.";Entry a=es.get(0),b=es.get(1);double dtps=a.tpsMedian-b.tpsMedian;long dttft=a.ttftP95-b.ttftP95;double dtemp=a.tempRise-b.tempRise;return String.format(Locale.US,"Son koşu önceki koşuya göre: token/s %+.1f • p95 TTFT %+d ms • ΔT %+.1f°C",dtps,dttft,dtemp);}
    public static void clear(Context c){p(c).edit().clear().apply();}
}
