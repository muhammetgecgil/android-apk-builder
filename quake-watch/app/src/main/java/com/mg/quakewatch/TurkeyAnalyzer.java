package com.mg.quakewatch;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TurkeyAnalyzer {
    // No minimum-magnitude filter: request every event the source actually catalogs in the Turkey box.
    private static final String API="https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=now-7days&minlatitude=34&maxlatitude=43&minlongitude=25&maxlongitude=46&orderby=time&limit=20000";
    private static final DecimalFormat DF=new DecimalFormat("0.00");
    static final class E{double lat,lon,mag,dep;long t;String p;E(double a,double o,double m,double d,long tt,String pp){lat=a;lon=o;mag=m;dep=d;t=tt;p=pp;}}
    static final class C{int a,b;List<E> es=new ArrayList<>();double score,bv,ratio,etas,migration;C(int x,int y){a=x;b=y;}double lat(){return 34.0+a*0.25+0.125;}double lon(){return 25.0+b*0.25+0.125;}}
    public static final class Report{
        public final int eventCount; public final double maxScore; public final String text; public final String hotspotsJson; public final String eventsJson; public final String faultsJson;
        Report(int n,double s,String t,String j,String e,String f){eventCount=n;maxScore=s;text=t;hotspotsJson=j;eventsJson=e;faultsJson=f;}
    }

    public static Report fetchAndAnalyze() throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(API).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestProperty("User-Agent","QuakeWatch-Turkey/1.5");
        BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder sb=new StringBuilder();String l;while((l=br.readLine())!=null)sb.append(l);br.close();
        JSONArray f=new JSONObject(sb.toString()).getJSONArray("features");long now=System.currentTimeMillis();Map<String,C> cells=new HashMap<>();List<E> all=new ArrayList<>();
        for(int i=0;i<f.length();i++){
            JSONObject x=f.getJSONObject(i),p=x.getJSONObject("properties"); JSONArray co=x.getJSONObject("geometry").getJSONArray("coordinates");
            double m=p.isNull("mag")?0:p.getDouble("mag"); E e=new E(co.getDouble(1),co.getDouble(0),m,co.optDouble(2,0),p.getLong("time"),p.optString("place","Bilinmeyen")); all.add(e);
            int a=(int)Math.floor((e.lat-34.0)/0.25),b=(int)Math.floor((e.lon-25.0)/0.25);String k=a+":"+b;C z=cells.get(k);if(z==null){z=new C(a,b);cells.put(k,z);}z.es.add(e);
        }
        List<C> ranked=new ArrayList<>();
        for(C z:cells.values()){
            int n6=0,n24=0,n7=z.es.size(),ncmp=0;double sum=0,maxM=-9;double recentLat=0,recentLon=0,oldLat=0,oldLon=0;int nr=0,no=0;
            for(E e:z.es){long age=now-e.t;if(age<=6*3600000L)n6++;if(age<=24*3600000L)n24++;if(e.mag>=0.5){sum+=e.mag;ncmp++;}maxM=Math.max(maxM,e.mag);if(age<=24*3600000L){recentLat+=e.lat;recentLon+=e.lon;nr++;}else if(age<=72*3600000L){oldLat+=e.lat;oldLon+=e.lon;no++;}}
            double expected24=Math.max(0.25,n7/7.0);z.ratio=(n24+0.25)/(expected24+0.25);z.bv=ncmp>=6?0.4342944819/Math.max(0.05,sum/ncmp-0.45):1.0;
            double et=0;for(E e:z.es){double h=Math.max(.05,(now-e.t)/3600000.0);et+=Math.exp(Math.min(7,1.10*Math.max(0,e.mag-1.8)))/Math.pow(h+.2,1.08);}z.etas=Math.log1p(et);
            if(nr>0&&no>0){double dlat=recentLat/nr-oldLat/no,dlon=recentLon/nr-oldLon/no;z.migration=Math.min(1.0,Math.sqrt(dlat*dlat+dlon*dlon)/0.20);}else z.migration=0;
            double raw=1.9*Math.log1p(z.ratio)+1.0*z.etas+0.8*Math.log1p(n6*2+n24)+0.8*Math.max(0,1.05-z.bv)+0.55*Math.max(0,maxM-2.5)+0.85*z.migration;
            z.score=100*(1-Math.exp(-raw/7.0));if(n7>=1)ranked.add(z);
        }
        Collections.sort(ranked,Comparator.comparingDouble((C q)->q.score).reversed());
        JSONArray hot=new JSONArray(); JSONArray ev=new JSONArray(); StringBuilder out=new StringBuilder("TÜRKİYE JEOLOJİ PROFESÖR MODU • 7 GÜN\n");
        out.append("Katalog olayı: ").append(all.size()).append("\nModel: 0.25° mikro-hücre + 6s/24s hızlanma + ETAS + b-değeri + aktivite göçü + en yakın ana fay bağlamı.\n\n");
        int ml=Math.min(160,ranked.size());for(int i=0;i<ml;i++){C z=ranked.get(i);JSONObject o=new JSONObject();o.put("lat",z.lat());o.put("lon",z.lon());o.put("score",z.score);o.put("count",z.es.size());o.put("rate",z.ratio);o.put("b",z.bv);o.put("etas",z.etas);o.put("migration",z.migration);hot.put(o);}
        all.sort((x,y)->Long.compare(y.t,x.t));
        for(E e:all){
            FaultModel.Nearest nf=FaultModel.nearest(e.lat,e.lon);
            JSONObject o=new JSONObject();o.put("lat",e.lat);o.put("lon",e.lon);o.put("mag",e.mag);o.put("depth",e.dep);o.put("time",e.t);o.put("place",e.p);
            o.put("fault",nf.name);o.put("faultSystem",nf.system);o.put("faultType",nf.type);o.put("faultKm",nf.km);ev.put(o);
        }
        int lim=Math.min(12,ranked.size());for(int i=0;i<lim;i++){C z=ranked.get(i);out.append(i+1).append(") ").append(String.format(Locale.US,"%.3f, %.3f",z.lat(),z.lon())).append(" • anomali ").append(DF.format(z.score)).append("/100\n   7g=").append(z.es.size()).append("  oran=").append(DF.format(z.ratio)).append("x  b≈").append(DF.format(z.bv)).append("  ETAS=").append(DF.format(z.etas)).append("  göç=").append(DF.format(z.migration*100)).append("%\n\n");}
        out.append("PROFESÖR YORUMU\n");
        if(lim>0){C z=ranked.get(0);if(z.score>=75)out.append("En aktif mikro-bölge güçlü kısa dönem kümelenme gösteriyor. Bu çoğu zaman artçı dizi veya yerel swarm olabilir; tek başına büyük deprem habercisi sayılmaz.\n");else if(z.score>=50)out.append("Bazı mikro-bölgelerde olağanın üzerinde aktivite var. Zaman içindeki göç yönü ve b-değeri birlikte izlenmeli.\n");else out.append("Model belirgin eşik üstü kısa dönem kümelenme saptamıyor.\n");}
        out.append("\nHaritadaki fay çizgileri sadeleştirilmiş eğitim/analiz geometrisidir; resmi mühendislik fay haritası yerine kullanılmamalıdır. Katalog sınırı: kaynak ağın raporladığı en küçük olaylar gösterilir. Kesin deprem yeri/zamanı/büyüklüğü tahmini değildir.");
        return new Report(all.size(),lim==0?0:ranked.get(0).score,out.toString(),hot.toString(),ev.toString(),FaultModel.json());
    }
    private TurkeyAnalyzer(){}
}
