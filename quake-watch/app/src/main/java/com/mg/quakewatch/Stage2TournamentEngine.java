package com.mg.quakewatch;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Stage 2: blind rolling forecast tournament.
 * Each test day is forecast using only data strictly before that day.
 * Scores are research metrics, not deterministic earthquake prediction.
 */
public final class Stage2TournamentEngine {
    private static final String API="https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=now-45days&minlatitude=34&maxlatitude=43&minlongitude=25&maxlongitude=46&orderby=time&limit=20000";
    private static final long DAY=86400000L;
    private static final double CELL=0.5;
    private static final String[] MODELS={"ETAS","HAWKES","QIE","POISSON","FAULT-GRAPH"};

    static final class E { double lat,lon,mag,dep; long t; E(double a,double o,double m,double d,long tt){lat=a;lon=o;mag=m;dep=d;t=tt;} }
    static final class Cell {
        String key; double lat,lon; List<E> es=new ArrayList<>();
        Cell(String k,double a,double o){key=k;lat=a;lon=o;}
    }
    static final class DayForecast { String model,cell; double p,score; boolean hit; DayForecast(String m,String c,double pp,double s,boolean h){model=m;cell=c;p=pp;score=s;hit=h;} }
    static final class Metrics {
        String name; int n,hits,falseAlarms; double brier,logloss; List<DayForecast> rows=new ArrayList<>();
        double hitRate(){return n==0?0:100.0*hits/n;}
        double falseAlarmRate(){return n==0?0:100.0*falseAlarms/n;}
    }
    public static final class Result {
        public final String summary,tournament,blind,calibration,falseAlarms,winner,method;
        public final int eventCount,testDays; public final String winnerName; public final double winnerBrier;
        Result(String s,String t,String b,String c,String f,String w,String m,int e,int d,String wn,double wb){summary=s;tournament=t;blind=b;calibration=c;falseAlarms=f;winner=w;method=m;eventCount=e;testDays=d;winnerName=wn;winnerBrier=wb;}
    }

    public static Result run() throws Exception {
        List<E> all=fetch();
        if(all.size()<30) return new Result("Yeterli katalog verisi yok.","-","-","-","-","-","-",all.size(),0,"-",1);
        Collections.sort(all,Comparator.comparingLong(x->x.t));
        long now=System.currentTimeMillis();
        int testDays=21;
        Map<String,Metrics> mm=new LinkedHashMap<>(); for(String m:MODELS){Metrics q=new Metrics();q.name=m;mm.put(m,q);}

        for(int d=testDays;d>=1;d--){
            long testStart=now-d*DAY, testEnd=testStart+DAY, trainStart=testStart-7*DAY;
            Map<String,Cell> cells=buildCells(all,trainStart,testStart);
            if(cells.isEmpty()) continue;
            Map<String,Boolean> observed=observedCells(all,testStart,testEnd,3.0);
            for(String model:MODELS){
                String best=null; double bestScore=-1;
                for(Cell c:cells.values()){
                    double s=score(model,c,testStart,cells);
                    if(s>bestScore){bestScore=s;best=c.key;}
                }
                boolean hit=best!=null&&Boolean.TRUE.equals(observed.get(best));
                double p=probability(model,bestScore);
                Metrics z=mm.get(model);z.n++;if(hit)z.hits++;if(p>=0.50&&!hit)z.falseAlarms++;
                z.brier+=(p-(hit?1:0))*(p-(hit?1:0));
                double cp=Math.max(0.01,Math.min(0.99,p));z.logloss+=-(hit?Math.log(cp):Math.log(1-cp));
                z.rows.add(new DayForecast(model,best,p,bestScore,hit));
            }
        }
        for(Metrics z:mm.values()){if(z.n>0){z.brier/=z.n;z.logloss/=z.n;}}
        List<Metrics> rank=new ArrayList<>(mm.values());rank.sort(Comparator.comparingDouble(x->x.brier));
        Metrics win=rank.get(0);
        return new Result(summary(all,rank),tournament(rank),blind(rank),calibration(rank),falseAlarms(rank),winner(rank),method(),all.size(),win.n,win.name,win.brier);
    }

    private static Map<String,Cell> buildCells(List<E> all,long start,long end){
        Map<String,Cell> out=new HashMap<>();
        for(E e:all){if(e.t<start||e.t>=end)continue;String k=key(e.lat,e.lon);Cell c=out.get(k);if(c==null){double[] cc=center(k);c=new Cell(k,cc[0],cc[1]);out.put(k,c);}c.es.add(e);}return out;
    }
    private static Map<String,Boolean> observedCells(List<E> all,long start,long end,double minMag){Map<String,Boolean> o=new HashMap<>();for(E e:all)if(e.t>=start&&e.t<end&&e.mag>=minMag)o.put(key(e.lat,e.lon),true);return o;}

    private static double score(String model,Cell c,long testStart,Map<String,Cell> cells){
        int n7=c.es.size(),n24=0,n6=0;double etas=0,magSum=0,maxM=-9;int bN=0;long latest=0;
        for(E e:c.es){double ageH=Math.max(.05,(testStart-e.t)/3600000.0);if(ageH<=24)n24++;if(ageH<=6)n6++;etas+=Math.exp(Math.min(6,0.95*Math.max(0,e.mag-1.5)))/Math.pow(ageH+.3,1.05);if(e.mag>=0.5){magSum+=e.mag;bN++;}maxM=Math.max(maxM,e.mag);latest=Math.max(latest,e.t);}
        double rate=(n24+.4)/(Math.max(.4,n7/7.0)+.4);
        double b=bN>=6?0.4342944819/Math.max(.08,magSum/bN-0.45):1.0;
        FaultModel.Nearest nf=FaultModel.nearest(c.lat,c.lon);double faultProx=Math.exp(-nf.km/35.0);
        if("POISSON".equals(model)) return Math.log1p(n7);
        if("ETAS".equals(model)) return Math.log1p(etas)+0.15*Math.max(0,maxM-2);
        if("HAWKES".equals(model)) return 1.4*Math.log1p(rate)+0.45*Math.log1p(n6*2+n24)+0.25*Math.log1p(etas);
        if("QIE".equals(model)) return 1.15*Math.log1p(rate)+0.75*Math.log1p(etas)+0.65*Math.max(0,1.05-b)+0.42*Math.max(0,maxM-2.5)+0.55*faultProx;
        double neigh=0;int cnt=0;int[] ij=parse(c.key);for(int da=-1;da<=1;da++)for(int db=-1;db<=1;db++){if(da==0&&db==0)continue;Cell q=cells.get((ij[0]+da)+":"+(ij[1]+db));if(q!=null){neigh+=q.es.size();cnt++;}}
        return 0.9*Math.log1p(n7)+0.8*faultProx+0.45*Math.log1p(neigh/(double)Math.max(1,cnt))+0.35*Math.log1p(rate);
    }

    private static double probability(String model,double s){
        double a,b;if("POISSON".equals(model)){a=0.85;b=2.0;}else if("ETAS".equals(model)){a=0.85;b=2.2;}else if("HAWKES".equals(model)){a=.75;b=2.3;}else if("QIE".equals(model)){a=.72;b=2.5;}else{a=.78;b=2.25;}
        double p=1.0/(1.0+Math.exp(-a*(s-b)));return Math.max(.03,Math.min(.92,p));
    }

    private static String summary(List<E> all,List<Metrics> r){Metrics w=r.get(0);return String.format(Locale.US,"Katalog: %d olay\nBlind test günü: %d\nEn düşük Brier: %s = %.3f\nHit-rate: %.1f%%\nFalse-alarm: %.1f%%\n\nBu sonuç yalnız son 21 günlük kayan test penceresinin sağlık kontrolüdür; uzun dönem üstünlük iddiası değildir.",all.size(),w.n,w.name,w.brier,w.hitRate(),w.falseAlarmRate());}
    private static String tournament(List<Metrics> r){StringBuilder s=new StringBuilder("SIRALAMA • düşük Brier daha iyi\n\n");int i=1;for(Metrics m:r)s.append(String.format(Locale.US,"%d) %-11s  Brier %.3f  LogLoss %.3f  Hit %.1f%%\n",i++,m.name,m.brier,m.logloss,m.hitRate()));return s.toString();}
    private static String blind(List<Metrics> r){return "BLIND BACKTEST PROTOKOLÜ\nHer test gününde model yalnız o günden önceki 7 günlük kataloğu gördü. Sonraki 24 saat tamamen saklı tutuldu. Hedef, modelin seçtiği 0.5° hücrede M≥3 olay olup olmamasıydı. Gelecek verisi feature veya model seçimine verilmedi.";}
    private static String calibration(List<Metrics> r){StringBuilder s=new StringBuilder("KALİBRASYON • tahmin pencereleri\n");for(Metrics m:r){int[] n=new int[4],h=new int[4];for(DayForecast x:m.rows){int k=Math.min(3,(int)(x.p*4));n[k]++;if(x.hit)h[k]++;}s.append("\n").append(m.name).append("\n");for(int k=0;k<4;k++){double lo=25*k,hi=25*(k+1);double obs=n[k]==0?0:100.0*h[k]/n[k];s.append(String.format(Locale.US,"  %.0f–%.0f%%: n=%d • gerçekleşen %.1f%%\n",lo,hi,n[k],obs));}}return s.toString();}
    private static String falseAlarms(List<Metrics> r){StringBuilder s=new StringBuilder("YANLIŞ ALARM KARŞILAŞTIRMASI\nP≥0.50 olup hedef hücrede M≥3 oluşmayan testler:\n\n");for(Metrics m:r)s.append(String.format(Locale.US,"%-11s %d / %d  (%.1f%%)\n",m.name,m.falseAlarms,m.n,m.falseAlarmRate()));return s.toString();}
    private static String winner(List<Metrics> r){Metrics w=r.get(0),second=r.size()>1?r.get(1):w;double delta=second.brier-w.brier;String verdict=delta>0.03?"Bu kısa testte anlamlı sayılabilecek bir fark oluştu; yine de bağımsız uzun dönem doğrulama gerekir.":"İlk iki model birbirine yakın; kazanan ilanı istatistiksel olarak zayıf.";return String.format(Locale.US,"GEÇİCİ LİDER: %s\nBrier %.3f • LogLoss %.3f • Hit %.1f%%\nİkinci: %s • Brier %.3f\nFark: %.3f\n\n%s",w.name,w.brier,w.logloss,w.hitRate(),second.name,second.brier,delta,verdict);}
    private static String method(){return "MODEL SETİ\nETAS: zaman-mekân tetiklenme yoğunluğu\nHAWKES: kısa dönem self-excitation / hızlanma\nQIE: hızlanma + ETAS + b-sinyali + büyüklük + fay yakınlığı\nPOISSON: basit arka-plan aktivitesi baseline\nFAULT-GRAPH: fay yakınlığı + komşu hücre aktivitesi\n\nAmaç modelin kendisini baseline'a karşı kanıtlamasıdır. Skorlar deprem olacak yüzdesi değildir.";}

    private static String key(double lat,double lon){int a=(int)Math.floor((lat-34)/CELL),b=(int)Math.floor((lon-25)/CELL);return a+":"+b;}
    private static int[] parse(String k){String[] x=k.split(":");return new int[]{Integer.parseInt(x[0]),Integer.parseInt(x[1])};}
    private static double[] center(String k){int[] x=parse(k);return new double[]{34+(x[0]+.5)*CELL,25+(x[1]+.5)*CELL};}
    private static List<E> fetch() throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(API).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestProperty("User-Agent","QuakeWatch-Stage2/2.3");
        BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder sb=new StringBuilder();String l;while((l=br.readLine())!=null)sb.append(l);br.close();
        JSONArray f=new JSONObject(sb.toString()).getJSONArray("features");List<E> out=new ArrayList<>();
        for(int i=0;i<f.length();i++){JSONObject x=f.getJSONObject(i),p=x.getJSONObject("properties");JSONArray co=x.getJSONObject("geometry").getJSONArray("coordinates");double m=p.isNull("mag")?0:p.getDouble("mag");out.add(new E(co.getDouble(1),co.getDouble(0),m,co.optDouble(2,0),p.getLong("time")));}return out;
    }
    private Stage2TournamentEngine(){}
}
