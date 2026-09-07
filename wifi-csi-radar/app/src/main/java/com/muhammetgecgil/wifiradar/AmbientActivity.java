package com.muhammetgecgil.wifiradar;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AmbientActivity extends Activity implements SensorEventListener {
    private static final int REQ=5101;
    private final Handler main=new Handler(Looper.getMainLooper());
    private WifiManager wifi; private SensorManager sm; private Sensor accel,gyro;
    private BroadcastReceiver receiver; private boolean receiverRegistered,running;
    private final Map<String,Integer> current=new LinkedHashMap<>(),baseline=new LinkedHashMap<>();
    private List<ScanResult> latest=new ArrayList<>(); private long lastScan; private double motion,accelLp=9.81,gyroLp,rfEwma,cusum; private boolean baselineValid;
    private TextView headline,stats,quality,region,phone,list; private Button start; private RadarView radar;

    private final Runnable live=new Runnable(){@Override public void run(){if(!running)return;updateUi();main.postDelayed(this,1000);}};
    private final Runnable scans=new Runnable(){@Override public void run(){if(!running)return;scan();main.postDelayed(this,30000);}};

    @Override protected void onCreate(Bundle s){super.onCreate(s);wifi=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);sm=(SensorManager)getSystemService(Context.SENSOR_SERVICE);if(sm!=null){accel=sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);gyro=sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);}restoreBaseline();receiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){if(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(i.getAction()))consume();}};build();}

    private void build(){LinearLayout root=Ui.shell(this,"Sensörsüz Wi‑Fi Laboratuvarı");ScrollView sv=Ui.scroll(this);LinearLayout col=Ui.column(this);sv.addView(col);col.addView(Ui.button(this,"‹ Ana ekran",Ui.PANEL2,v->finish()),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,44)));headline=Ui.text(this,"Ölçüm bekleniyor",21,Ui.WHITE,true);headline.setGravity(android.view.Gravity.CENTER);headline.setPadding(Ui.dp(this,10),Ui.dp(this,13),Ui.dp(this,10),Ui.dp(this,13));headline.setBackground(Ui.round(this,Ui.PANEL,Ui.CYAN,16));col.addView(headline);radar=new RadarView(this);radar.setBackground(Ui.round(this,Ui.PANEL,0,16));col.addView(radar,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,300)));
        LinearLayout r=Ui.row(this);start=Ui.button(this,"Başlat",Ui.GREEN,v->toggle());r.addView(start,Ui.weight(this));r.addView(Ui.button(this,"Tara",Ui.CYAN,v->scan()),Ui.weight(this));r.addView(Ui.button(this,"Boş Oda",Ui.YELLOW,v->captureBaseline()),Ui.weight(this));col.addView(r);
        LinearLayout r2=Ui.row(this);r2.addView(Ui.button(this,"Bölge Kaydet",Ui.CYAN,v->promptZone()),Ui.weight(this));r2.addView(Ui.button(this,"İzinler",Ui.PANEL2,v->requestPerms()),Ui.weight(this));col.addView(r2);
        stats=Ui.metric(this);quality=Ui.metric(this);region=Ui.metric(this);phone=Ui.metric(this);col.addView(stats);col.addView(quality);col.addView(region);col.addView(phone);col.addView(Ui.note(this,"Bilimsel sınır: Android standart Wi‑Fi taraması ham CSI sağlamaz. Bu mod insanın kesin X‑Y konumunu değil, RF parmak izi değişimini ve telefonun öğrenilmiş bölgesini gösterir."));list=Ui.text(this,"Henüz tarama yok",12,Ui.MUTED,false);list.setPadding(Ui.dp(this,12),Ui.dp(this,12),Ui.dp(this,12),Ui.dp(this,18));col.addView(list);root.addView(sv,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));setContentView(root);updateUi();}

    private void toggle(){if(running){stop();return;}if(!hasPerms()){requestPerms();return;}if(!locationEnabled()){promptLocation();return;}running=true;start.setText("Durdur");registerReceiverSafe();registerSensors();scan();main.post(live);main.postDelayed(scans,30000);}
    private void stop(){running=false;main.removeCallbacks(live);main.removeCallbacks(scans);unregisterReceiverSafe();if(sm!=null)sm.unregisterListener(this);if(start!=null)start.setText("Başlat");}
    private void registerSensors(){if(sm==null)return;if(accel!=null)sm.registerListener(this,accel,SensorManager.SENSOR_DELAY_UI);if(gyro!=null)sm.registerListener(this,gyro,SensorManager.SENSOR_DELAY_UI);}
    private void registerReceiverSafe(){if(receiverRegistered)return;IntentFilter f=new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(receiver,f);receiverRegistered=true;}
    private void unregisterReceiverSafe(){if(!receiverRegistered)return;try{unregisterReceiver(receiver);}catch(Exception ignored){}receiverRegistered=false;}
    private boolean hasPerms(){return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED&&(Build.VERSION.SDK_INT<33||checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES)==PackageManager.PERMISSION_GRANTED);}
    private void requestPerms(){ArrayList<String> p=new ArrayList<>();if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)p.add(Manifest.permission.ACCESS_FINE_LOCATION);if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES)!=PackageManager.PERMISSION_GRANTED)p.add(Manifest.permission.NEARBY_WIFI_DEVICES);if(p.isEmpty()){if(!locationEnabled())promptLocation();else Toast.makeText(this,"Wi‑Fi ölçüm izinleri hazır.",Toast.LENGTH_SHORT).show();}else requestPermissions(p.toArray(new String[0]),REQ);}
    @Override public void onRequestPermissionsResult(int r,String[]p,int[]g){super.onRequestPermissionsResult(r,p,g);if(r==REQ&&hasPerms()){if(!locationEnabled())promptLocation();else toggle();}}
    private boolean locationEnabled(){try{android.location.LocationManager lm=(android.location.LocationManager)getSystemService(Context.LOCATION_SERVICE);return lm==null||lm.isLocationEnabled();}catch(Exception e){return true;}}
    private void promptLocation(){new AlertDialog.Builder(this).setTitle("Konum hizmeti gerekli").setMessage("Android, Wi‑Fi tarama sonuçlarını vermek için sistem Konum hizmetinin açık olmasını isteyebilir. Uygulama GPS geçmişi tutmaz.").setNegativeButton("İptal",null).setPositiveButton("Ayarlar",(d,w)->startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))).show();}

    @SuppressWarnings("deprecation") private void scan(){if(wifi==null){Toast.makeText(this,"Wi‑Fi servisi bulunamadı.",Toast.LENGTH_SHORT).show();return;}if(!hasPerms()){requestPerms();return;}registerReceiverSafe();try{boolean ok=wifi.startScan();if(!ok)consume();}catch(SecurityException e){requestPerms();}catch(Exception e){Toast.makeText(this,"Tarama başlatılamadı.",Toast.LENGTH_SHORT).show();}}
    @SuppressWarnings("deprecation") private void consume(){if(wifi==null||!hasPerms())return;try{List<ScanResult> x=wifi.getScanResults();latest=new ArrayList<>(x==null?Collections.emptyList():x);latest.sort((a,b)->Integer.compare(b.level,a.level));current.clear();for(ScanResult s:latest)if(s.BSSID!=null)current.put(s.BSSID,s.level);lastScan=System.currentTimeMillis();updateStats();updateUi();}catch(Exception ignored){}}
    private void updateStats(){if(current.isEmpty())return;Map<String,Integer> ref=baselineValid?baseline:lastFp();if(ref.isEmpty()){saveLast(current);return;}ArrayList<Double>d=new ArrayList<>();for(Map.Entry<String,Integer>e:current.entrySet()){Integer old=ref.get(e.getKey());if(old!=null)d.add(Math.abs(e.getValue()-old)/18.0);}double med=median(d);rfEwma=.72*rfEwma+.28*clamp(med);double exp=baselineValid?.12:.18;cusum=Math.max(0,Math.min(4,cusum+rfEwma-exp-.03));if(!baselineValid)saveLast(current);}
    private void captureBaseline(){if(current.size()<3){Toast.makeText(this,"Önce en az 3 ağ içeren tarama yap.",Toast.LENGTH_LONG).show();return;}if(motion>.35){Toast.makeText(this,"Telefon hareket ediyor; sabitle ve tekrar dene.",Toast.LENGTH_LONG).show();return;}baseline.clear();baseline.putAll(current);baselineValid=true;rfEwma=0;cusum=0;persistBaseline();Toast.makeText(this,"Boş oda RF referansı kaydedildi.",Toast.LENGTH_SHORT).show();updateUi();}
    private void updateUi(){if(headline==null)return;int count=current.size(),common=0;if(baselineValid)for(String b:current.keySet())if(baseline.containsKey(b))common++;double age=lastScan==0?999:(System.currentTimeMillis()-lastScan)/1000.0;double q=clamp((baselineValid?common:count)/8.0)*clamp(1-age/65.0)*(.35+.65*clamp(1-motion/.75));double e=clamp(.58*rfEwma+.42*clamp(cusum/1.8));if(!baselineValid)e*=.5;String h;int c;if(count==0){h="Wi‑Fi verisi yok";c=Ui.RED;}else if(motion>.5){h="TELEFON HAREKETLİ • RF KARARI BASKILANDI";c=Ui.YELLOW;}else if(!baselineValid){h="RF REFERANSI GEREKLİ";c=Ui.CYAN;}else if(q<.22){h="DÜŞÜK ÖLÇÜM KALİTESİ";c=Ui.YELLOW;}else if(e>.62){h="RF ORTAM DEĞİŞİMİ YÜKSEK";c=Ui.RED;}else if(e>.34){h="RF ORTAM DEĞİŞİMİ ORTA";c=Ui.YELLOW;}else{h="RF ORTAMI REFERANSA YAKIN";c=Ui.GREEN;}headline.setText(h);headline.setTextColor(c);stats.setText(String.format(Locale.US,"Ağ sayısı %d • RF değişim %d/100 • CUSUM %.2f",count,(int)(100*e),cusum));quality.setText(String.format(Locale.US,"Ölçüm kalitesi %d/100 • ortak BSSID %d • tarama yaşı %.0f sn",(int)(100*q),common,age));region.setText("Wi‑Fi parmak izi bölgesi: "+inferZone());phone.setText(String.format(Locale.US,"Telefon hareket indeksi %d/100 • %s",(int)(100*clamp(motion)),motion<.25?"sabit":"hareket etkisi var"));radar.setData(latest,e,q);radar.invalidate();list.setText(format(latest));}

    private void promptZone(){if(current.size()<3){Toast.makeText(this,"Önce tarama yap.",Toast.LENGTH_SHORT).show();return;}final android.widget.EditText name=new android.widget.EditText(this);name.setHint("Örn: Salon Kapısı");new AlertDialog.Builder(this).setTitle("Bölge parmak izi").setMessage("Bu kayıt telefonun bulunduğu bölgeyi öğrenmek içindir; insan konumu değildir.").setView(name).setNegativeButton("İptal",null).setPositiveButton("Kaydet",(d,w)->saveZone(name.getText().toString().trim())).show();}
    private void saveZone(String n){if(n.isEmpty())n="Bölge";try{JSONArray a=new JSONArray(getPreferences(MODE_PRIVATE).getString("zones","[]"));JSONObject z=new JSONObject(),fp=new JSONObject();z.put("name",n);for(Map.Entry<String,Integer>e:current.entrySet())fp.put(e.getKey(),e.getValue());z.put("fp",fp);a.put(z);getPreferences(MODE_PRIVATE).edit().putString("zones",a.toString()).apply();Toast.makeText(this,"Bölge kaydedildi: "+n,Toast.LENGTH_SHORT).show();updateUi();}catch(Exception ignored){}}
    private String inferZone(){if(current.isEmpty())return"veri yok";try{JSONArray a=new JSONArray(getPreferences(MODE_PRIVATE).getString("zones","[]"));if(a.length()==0)return"öğrenilmiş bölge yok";String bestName="belirsiz";double best=1e9;for(int i=0;i<a.length();i++){JSONObject z=a.getJSONObject(i),fp=z.getJSONObject("fp");double sum=0;int common=0;for(String b:current.keySet())if(fp.has(b)){sum+=Math.abs(current.get(b)-fp.getInt(b));common++;}if(common<3)continue;double d=sum/common+Math.max(0,6-common)*2;if(d<best){best=d;bestName=z.optString("name","Bölge");}}return best>18?"belirsiz":bestName+String.format(Locale.US," • benzerlik %d",(int)(100*clamp(1-best/24.0)));}catch(Exception e){return"model okunamadı";}}
    private String format(List<ScanResult>x){if(x==null||x.isEmpty())return"Henüz tarama sonucu yok.";StringBuilder b=new StringBuilder("GÜÇLÜ ERİŞİM NOKTALARI\n\n");for(int i=0;i<Math.min(18,x.size());i++){ScanResult r=x.get(i);String ssid;if(Build.VERSION.SDK_INT>=33)ssid=r.getWifiSsid()==null?"(gizli)":r.getWifiSsid().toString().replace("\"","");else ssid=r.SSID==null||r.SSID.isEmpty()?"(gizli)":r.SSID;b.append(String.format(Locale.US,"%2d. %-20s %4d dBm  %s\n",i+1,trim(ssid,20),r.level,band(r.frequency)));}return b.toString();}
    private String band(int f){return f>=5925?"6 GHz":f>=4900?"5 GHz":"2.4 GHz";}

    private void persistBaseline(){JSONObject j=new JSONObject();try{for(Map.Entry<String,Integer>e:baseline.entrySet())j.put(e.getKey(),e.getValue());}catch(Exception ignored){}getPreferences(MODE_PRIVATE).edit().putString("baseline",j.toString()).apply();}
    private void restoreBaseline(){try{JSONObject j=new JSONObject(getPreferences(MODE_PRIVATE).getString("baseline","{}"));JSONArray n=j.names();if(n!=null)for(int i=0;i<n.length();i++){String k=n.getString(i);baseline.put(k,j.getInt(k));}baselineValid=baseline.size()>=3;}catch(Exception ignored){}}
    private void saveLast(Map<String,Integer>m){JSONObject j=new JSONObject();try{for(Map.Entry<String,Integer>e:m.entrySet())j.put(e.getKey(),e.getValue());}catch(Exception ignored){}getPreferences(MODE_PRIVATE).edit().putString("last",j.toString()).apply();}
    private Map<String,Integer> lastFp(){Map<String,Integer>m=new HashMap<>();try{JSONObject j=new JSONObject(getPreferences(MODE_PRIVATE).getString("last","{}"));JSONArray n=j.names();if(n!=null)for(int i=0;i<n.length();i++){String k=n.getString(i);m.put(k,j.getInt(k));}}catch(Exception ignored){}return m;}

    @Override public void onSensorChanged(SensorEvent e){if(!running)return;if(e.sensor.getType()==Sensor.TYPE_ACCELEROMETER){double m=Math.sqrt(e.values[0]*e.values[0]+e.values[1]*e.values[1]+e.values[2]*e.values[2]);accelLp=.92*accelLp+.08*m;double dyn=Math.abs(m-accelLp);motion=.82*motion+.18*clamp(dyn/2.5+gyroLp/2);}else if(e.sensor.getType()==Sensor.TYPE_GYROSCOPE){double g=Math.sqrt(e.values[0]*e.values[0]+e.values[1]*e.values[1]+e.values[2]*e.values[2]);gyroLp=.82*gyroLp+.18*Math.min(2,g);}}
    @Override public void onAccuracyChanged(Sensor s,int a){}
    @Override protected void onDestroy(){stop();super.onDestroy();}

    private static double clamp(double x){return Math.max(0,Math.min(1,x));}private static double median(List<Double>x){if(x.isEmpty())return 0;ArrayList<Double>a=new ArrayList<>(x);Collections.sort(a);int n=a.size();return n%2==1?a.get(n/2):.5*(a.get(n/2-1)+a.get(n/2));}private static String trim(String s,int n){return s.length()<=n?s:s.substring(0,n-1)+"…";}

    private static final class RadarView extends View {private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private List<ScanResult>d=new ArrayList<>();private double e,q;RadarView(Context c){super(c);}void setData(List<ScanResult>x,double a,double b){d=x==null?new ArrayList<>():new ArrayList<>(x);e=a;q=b;}@Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),cx=w/2,cy=h/2,rad=Math.min(w,h)*.42f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.rgb(30,76,86));for(int k=1;k<=4;k++)c.drawCircle(cx,cy,rad*k/4,p);c.drawLine(cx-rad,cy,cx+rad,cy,p);c.drawLine(cx,cy-rad,cx,cy+rad,p);p.setStyle(Paint.Style.FILL);for(int i=0;i<Math.min(28,d.size());i++){ScanResult r=d.get(i);double n=clamp((r.level+100)/60.0),rr=rad*(1-.82*n),a=2*Math.PI*((r.BSSID==null?i:r.BSSID.hashCode())&0xffff)/65535.0;float x=(float)(cx+rr*Math.cos(a)),y=(float)(cy+rr*Math.sin(a));p.setColor(r.frequency>=5925?Ui.YELLOW:r.frequency>=4900?Ui.CYAN:Ui.GREEN);c.drawCircle(x,y,4+(float)(6*n),p);}p.setColor(Ui.WHITE);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(24);c.drawText("ORTAM Wi‑Fi RF",16,30,p);p.setTypeface(Typeface.DEFAULT);p.setTextSize(18);p.setColor(Ui.MUTED);c.drawText(String.format(Locale.US,"ΔRF %d  Q %d",(int)(100*e),(int)(100*q)),16,h-16,p);}}
}
