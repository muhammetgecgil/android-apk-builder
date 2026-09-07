package com.mg.quakewatch;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Stage-1 research layer: change-point, sequence classification, segment status,
 * prediction passport, reason-for-change and data-health reporting.
 * It is an experimental operational forecast aid, not deterministic prediction.
 */
public final class Stage1ResearchEngine {
    private static final String PREF="quake_stage1";
    private static final String MODEL="QIE-STAGE1-2.1.0";

    public static final class Result {
        public final String summary, changePoint, sequence, segments, passport, whyChanged, health;
        public final double healthScore;
        Result(String s,String c,String q,String g,String p,String w,String h,double hs){summary=s;changePoint=c;sequence=q;segments=g;passport=p;whyChanged=w;health=h;healthScore=hs;}
    }

    private static final class E { double lat,lon,mag,depth; long time; String fault,place; }
    private static final class Seg { String name; int n24,n7; double maxMag=-9,sumDepth,lastLat,lastLon; long newest; }

    public static Result analyze(Context ctx, TurkeyAnalyzer.Report r) throws Exception {
        JSONArray evj=new JSONArray(r.eventsJson); List<E> ev=new ArrayList<>(); long now=System.currentTimeMillis(), newest=0;
        for(int i=0;i<evj.length();i++){JSONObject o=evj.getJSONObject(i);E e=new E();e.lat=o.optDouble("lat");e.lon=o.optDouble("lon");e.mag=o.optDouble("mag");e.depth=o.optDouble("depth");e.time=o.optLong("time");e.fault=o.optString("fault","Bilinmeyen fay");e.place=o.optString("place","");ev.add(e);newest=Math.max(newest,e.time);} 
        Collections.sort(ev, Comparator.comparingLong((E x)->x.time));

        String cp=changePoint(ev,now); String seq=sequence(ev,now); String seg=segmentReport(ev,now);
        JSONArray hot=new JSONArray(r.hotspotsJson); JSONObject top=hot.length()>0?hot.getJSONObject(0):new JSONObject();
        SharedPreferences p=ctx.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        String why=whyChanged(p,top,r.maxScore); String pass=recordPassport(p,top,r.maxScore,now);
        double freshnessHours=newest==0?999:(now-newest)/3600000.0; double healthScore=100;
        if(ev.size()<20)healthScore-=25;if(freshnessHours>6)healthScore-=30;else if(freshnessHours>2)healthScore-=15;
        String health=String.format(Locale.US,
                "VERİ SAĞLIĞI • %.0f/100\nUSGS katalog: BAĞLI • %d olay\nEn yeni olay yaşı: %.1f saat\nFay geometrisi: yerleşik/sadeleştirilmiş\nAFAD canlı füzyon: henüz bağlı değil\nKandilli canlı füzyon: henüz bağlı değil\nInSAR/GNSS: gerçek akış bağlı değilse skora girmez\nKural: eksik kaynaklar güveni artırmaz.",
                Math.max(0,healthScore),ev.size(),freshnessHours);
        String summary=String.format(Locale.US,"AŞAMA 1 • %s\nTürkiye birleşik tepe skor: %.1f/100\nKatalog: %d olay\nModel artık davranış değişimini, dizi tipini, segment durumunu ve tahmin pasaportunu ayrı kaydediyor.",MODEL,r.maxScore,ev.size());
        return new Result(summary,cp,seq,seg,pass,why,health,Math.max(0,healthScore));
    }

    private static String changePoint(List<E> es,long now){
        long h=3600000L;int[] bins=new int[48];double[] dep=new double[48],mag=new double[48];int[] n=new int[48];
        for(E e:es){long age=now-e.time;if(age<0||age>=48*h)continue;int b=47-(int)(age/h);bins[b]++;dep[b]+=e.depth;mag[b]+=e.mag;n[b]++;}
        int best=-1;double bestZ=0;for(int cut=8;cut<40;cut++){double a=0,b=0;int na=0,nb=0;for(int i=0;i<cut;i++){a+=bins[i];na++;}for(int i=cut;i<48;i++){b+=bins[i];nb++;}double ma=a/na,mb=b/nb,z=Math.abs(mb-ma)/Math.sqrt(Math.max(.25,(ma+mb)/2.0));if(z>bestZ){bestZ=z;best=cut;}}
        int last6=0,prev6=0;for(int i=42;i<48;i++)last6+=bins[i];for(int i=36;i<42;i++)prev6+=bins[i];double rate=(last6+.5)/(prev6+.5);
        String level=bestZ>=2.5?"BELİRGİN":bestZ>=1.5?"ORTA":"ZAYIF";
        int hoursAgo=best<0?-1:48-best;
        return String.format(Locale.US,"CHANGE-POINT DETECTOR\nDeğişim gücü: %s • z≈%.2f\nTahmini rejim değişimi: yaklaşık %d saat önce\nSon 6s / önceki 6s aktivite oranı: %.2fx\nYorum: bu istatistiksel davranış değişimidir; tek başına büyük deprem öncüsü değildir.",level,bestZ,hoursAgo,rate);
    }

    private static String sequence(List<E> es,long now){
        long d7=7L*86400000L;List<E> w=new ArrayList<>();for(E e:es)if(now-e.time<=d7)w.add(e);if(w.isEmpty())return "SEQUENCE INTELLIGENCE\nYeterli olay yok.";
        E max=w.get(0);for(E e:w)if(e.mag>max.mag)max=e;int after=0,before=0,near=0;double sx=0,sy=0;for(E e:w){if(e.time>max.time)after++;else if(e.time<max.time)before++;double km=hav(max.lat,max.lon,e.lat,e.lon);if(km<80)near++;sx+=e.lat;sy+=e.lon;}
        double dominance=(double)after/Math.max(1,w.size()-1),compact=(double)near/w.size();String type;
        if(max.mag>=4.0&&dominance>0.65&&compact>0.45)type="ARTÇI-DİZİ BENZERİ";
        else if(max.mag<4.5&&compact>0.55&&w.size()>=12)type="SWARM / KÜME BENZERİ";
        else if(w.size()<6)type="ARKA PLAN / SEYREK"; else type="KARIŞIK DİZİ";
        return String.format(Locale.US,"SEQUENCE INTELLIGENCE\nSınıf: %s\n7 günlük olay: %d • baskın M%.2f\nAna olay sonrası oran: %.0f%% • 80 km kompaktlık: %.0f%%\nBu sınıflandırma katalog davranışıdır; 'öncü deprem' etiketi vermez.",type,w.size(),max.mag,100*dominance,100*compact);
    }

    private static String segmentReport(List<E> es,long now){
        Map<String,Seg> m=new HashMap<>();for(E e:es){Seg s=m.get(e.fault);if(s==null){s=new Seg();s.name=e.fault;m.put(e.fault,s);}long age=now-e.time;if(age<=7L*86400000L)s.n7++;if(age<=86400000L)s.n24++;s.maxMag=Math.max(s.maxMag,e.mag);s.sumDepth+=e.depth;s.newest=Math.max(s.newest,e.time);s.lastLat=e.lat;s.lastLon=e.lon;}
        List<Seg> a=new ArrayList<>(m.values());Collections.sort(a,(x,y)->Double.compare(segmentScore(y),segmentScore(x)));StringBuilder out=new StringBuilder("FAY SEGMENT DURUM KARTLARI\n");int lim=Math.min(8,a.size());for(int i=0;i<lim;i++){Seg s=a.get(i);double sc=segmentScore(s);out.append(String.format(Locale.US,"%d) %s • durum %.0f/100\n   24s=%d • 7g=%d • max M%.1f\n",i+1,s.name,sc,s.n24,s.n7,s.maxMag));}out.append("Segment skoru kısa dönem aktivite yoğunluğu ve maksimum büyüklük bağlamıdır; mühendislik tehlike hesabı değildir.");return out.toString();
    }
    private static double segmentScore(Seg s){double x=1.8*Math.log1p(s.n24*2.0)+1.1*Math.log1p(s.n7)+.8*Math.max(0,s.maxMag-2);return 100*(1-Math.exp(-x/8.0));}

    private static String whyChanged(SharedPreferences p,JSONObject top,double score){
        double old=p.getFloat("lastScore",-1), oldRate=p.getFloat("lastRate",0), oldB=p.getFloat("lastB",1), oldEt=p.getFloat("lastEtas",0), oldMig=p.getFloat("lastMig",0);
        double rate=top.optDouble("rate",0),bv=top.optDouble("b",1),et=top.optDouble("etas",0),mig=top.optDouble("migration",0);
        String txt;if(old<0)txt="NEDEN FİKRİM DEĞİŞTİ?\nİlk referans kayıt oluşturuldu; sonraki çalıştırmada bileşen farkları açıklanacak.";else txt=String.format(Locale.US,"NEDEN FİKRİM DEĞİŞTİ?\nToplam skor değişimi: %+.1f puan\nAktivite oranı: %+.2fx\nb-değeri: %+.2f\nETAS: %+.2f\nGöç: %+.0f puan\nPozitif/negatif değişimler model görüşünün neden değiştiğini açıklar.",score-old,rate-oldRate,bv-oldB,et-oldEt,100*(mig-oldMig));
        p.edit().putFloat("lastScore",(float)score).putFloat("lastRate",(float)rate).putFloat("lastB",(float)bv).putFloat("lastEtas",(float)et).putFloat("lastMig",(float)mig).apply();return txt;
    }

    private static String recordPassport(SharedPreferences p,JSONObject top,double score,long now)throws Exception{
        String core=MODEL+"|"+now+"|"+top.optDouble("lat")+"|"+top.optDouble("lon")+"|"+score+"|"+top.optDouble("q24")+"|"+top.optDouble("q7")+"|"+top.optDouble("confidence");
        String hash=sha256(core).substring(0,12).toUpperCase(Locale.US);String id="TR-"+new SimpleDateFormat("yyyyMMdd-HHmm",Locale.US).format(new Date(now))+"-"+hash.substring(0,4);
        JSONArray a=new JSONArray(p.getString("passports","[]"));JSONObject o=new JSONObject();o.put("id",id);o.put("time",now);o.put("model",MODEL);o.put("lat",top.optDouble("lat"));o.put("lon",top.optDouble("lon"));o.put("score",score);o.put("q24",top.optDouble("q24"));o.put("q7",top.optDouble("q7"));o.put("confidence",top.optDouble("confidence"));o.put("digest",hash);a.put(o);JSONArray b=new JSONArray();for(int i=Math.max(0,a.length()-100);i<a.length();i++)b.put(a.get(i));p.edit().putString("passports",b.toString()).apply();
        return String.format(Locale.US,"PREDICTION PASSPORT\nKimlik: %s\nModel: %s\nKonum: %.3f, %.3f\nBirleşik skor: %.1f/100 • QIE24 %.1f • QIE7g %.1f\nGüven: %.0f%%\nDigest: %s\nBu kayıt analiz anında sabitlenir ve denetim geçmişi için saklanır.",id,MODEL,top.optDouble("lat"),top.optDouble("lon"),score,top.optDouble("q24"),top.optDouble("q7"),top.optDouble("confidence"),hash);
    }

    private static String sha256(String s)throws Exception{MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] d=md.digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte x:d)b.append(String.format(Locale.US,"%02x",x));return b.toString();}
    private static double hav(double a,double o,double b,double p){double R=6371,dlat=Math.toRadians(b-a),dlon=Math.toRadians(p-o);double q=Math.sin(dlat/2)*Math.sin(dlat/2)+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(b))*Math.sin(dlon/2)*Math.sin(dlon/2);return 2*R*Math.asin(Math.sqrt(q));}
    private Stage1ResearchEngine(){}
}
