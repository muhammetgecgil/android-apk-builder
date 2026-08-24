package com.mg.quakewatch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class QuakeMonitorService extends Service {
    private static final String CH_MON="quake_watch_monitor";
    private static final String CH_ALERT="quake_watch_alerts";
    private static final int FG=1001;
    private final ScheduledExecutorService exec=Executors.newSingleThreadScheduledExecutor();
    private double lastAlertScore=0, lastInfra=0;

    @Override public void onCreate(){
        super.onCreate(); createChannels();
        startForeground(FG, buildNotification("Dünya + Türkiye tahmin denetimi izleniyor"));
        exec.scheduleWithFixedDelay(this::check,0,15,TimeUnit.MINUTES);
    }

    private void check(){
        try{
            QuakeAnalyzer.Report r=QuakeAnalyzer.fetchAndAnalyze();
            FusionEngine.Context fx=null;
            try{fx=FusionEngine.fetch(r.maxScore,0);}catch(Exception ignored){}
            NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            String line="Sismik "+Math.round(r.maxScore)+"/100"+(fx==null?"":" • çevre/altyapı "+Math.round(fx.infrastructureIndex)+"/100");
            nm.notify(FG,buildNotification(line));

            if(r.maxScore>=80 && (lastAlertScore<80 || r.maxScore-lastAlertScore>=6)){
                nm.notify(2001,buildAlert("Yüksek sismik anomali","Dünya kataloğunda yüksek göreli sismik aktivite: "+Math.round(r.maxScore)+"/100. Haritayı açıp bölgeyi kontrol edin. Bu kesin deprem tahmini değildir."));
            } else if(r.maxScore>=65 && lastAlertScore<65){
                nm.notify(2002,buildAlert("Sismik aktivite artışı","Bazı bölgelerde kısa dönem sismik aktivite belirgin arttı: "+Math.round(r.maxScore)+"/100."));
            }

            if(fx!=null && fx.infrastructureIndex>=65 && (lastInfra<65 || fx.infrastructureIndex-lastInfra>=10)){
                nm.notify(2101,buildAlert("Çevresel / sanayi etki uyarısı","NOAA Kp ve çevresel bağlamdan altyapı etki endeksi "+Math.round(fx.infrastructureIndex)+"/100. GNSS, HF, uydu, güç şebekesi ve uzun iletken hatlar için uzay hava durumunu kontrol edin."));
            }

            // Turkey forecast ledger: save what the model said, then score it later against observed events.
            try{
                TurkeyAnalyzer.Report tr=TurkeyAnalyzer.fetchAndAnalyze();
                PredictionAudit.verifyAgainstCatalog(this,tr.eventsJson);
                PredictionAudit.recordTurkeyForecast(this,tr);
                if(tr.maxScore>=65){
                    nm.notify(2201,buildAlert("Türkiye risk bölgesi","Türkiye QIE modeli bir veya daha fazla bölgede yüksek göreli risk/anomali gösteriyor: "+Math.round(tr.maxScore)+"/100. Gerekçeler ve tahmin günlüğü raporda saklanıyor."));
                }
                if(PredictionAudit.hasNewHit(this)){
                    nm.notify(2202,buildAlert("Tahmin denetimi: isabet kaydı","Daha önce risk olarak kaydedilen bir bölgede tanımlı doğrulama kriterini karşılayan deprem gözlendi. Ayrıntılı raporda hangi değerlerle hesaplandığı görülebilir."));
                }
            }catch(Exception ignored){}

            lastAlertScore=r.maxScore; if(fx!=null)lastInfra=fx.infrastructureIndex;
        }catch(Exception ignored){}
    }

    private void createChannels(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel mon=new NotificationChannel(CH_MON,"Quake Watch izleme",NotificationManager.IMPORTANCE_LOW);
            mon.setDescription("Arka planda dünya ve Türkiye tahmin denetimi");
            NotificationChannel alert=new NotificationChannel(CH_ALERT,"Quake Watch kritik uyarılar",NotificationManager.IMPORTANCE_HIGH);
            alert.setDescription("Sismik anomali, risk bölgesi ve tahmin doğrulama uyarıları"); alert.enableVibration(true);
            NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE); nm.createNotificationChannel(mon); nm.createNotificationChannel(alert);
        }
    }

    private PendingIntent pi(){Intent i=new Intent(this,MainActivity.class);return PendingIntent.getActivity(this,0,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);}
    private Notification buildNotification(String text){Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CH_MON):new Notification.Builder(this);return b.setContentTitle("Quake Watch Fusion").setContentText(text).setSmallIcon(android.R.drawable.ic_dialog_info).setContentIntent(pi()).setOngoing(true).build();}
    private Notification buildAlert(String title,String text){Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CH_ALERT):new Notification.Builder(this);return b.setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setSmallIcon(android.R.drawable.ic_dialog_alert).setContentIntent(pi()).setAutoCancel(true).setPriority(Notification.PRIORITY_HIGH).build();}

    @Override public void onDestroy(){exec.shutdownNow();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
