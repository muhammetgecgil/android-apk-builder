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
    private double lastAlertScore=0, lastInfra=0, lastTr=0;

    @Override public void onCreate(){super.onCreate();createChannels();startForeground(FG,buildNotification("Dünya + Türkiye tahmin denetimi izleniyor"));exec.scheduleWithFixedDelay(this::check,0,15,TimeUnit.MINUTES);}

    private void check(){
        try{
            int threshold=getSharedPreferences("research_console_settings",MODE_PRIVATE).getInt("threshold",65);
            QuakeAnalyzer.Report r=QuakeAnalyzer.fetchAndAnalyze();FusionEngine.Context fx=null;try{fx=FusionEngine.fetch(r.maxScore,0);}catch(Exception ignored){}
            NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);String line="Dünya "+Math.round(r.maxScore)+"/100"+(fx==null?"":" • çevre "+Math.round(fx.infrastructureIndex)+"/100");nm.notify(FG,buildNotification(line));
            if(r.maxScore>=threshold && (lastAlertScore<threshold || r.maxScore-lastAlertScore>=6))nm.notify(2001,buildAlert("Dünya sismik risk/anomali uyarısı","Göreli aktivite "+Math.round(r.maxScore)+"/100 • kullanıcı eşiği "+threshold+". Kesin deprem tahmini değildir."));
            if(fx!=null&&fx.infrastructureIndex>=65&&(lastInfra<65||fx.infrastructureIndex-lastInfra>=10))nm.notify(2101,buildAlert("Çevresel / sanayi etki uyarısı","Uzay-hava ve çevresel bağlam endeksi "+Math.round(fx.infrastructureIndex)+"/100. GNSS/HF/şebeke etkileri için Space Watch panelini kontrol edin."));
            try{
                TurkeyAnalyzer.Report tr=TurkeyAnalyzer.fetchAndAnalyze();PredictionAudit.verifyAgainstCatalog(this,tr.eventsJson);PredictionAudit.recordTurkeyForecast(this,tr);
                if(tr.maxScore>=threshold&&(lastTr<threshold||tr.maxScore-lastTr>=5))nm.notify(2201,buildAlert("Türkiye risk bölgesi","Türkiye QIE tepe skoru "+Math.round(tr.maxScore)+"/100 • eşik "+threshold+". Tahmin günlüğüne gerekçeleriyle kaydedildi."));
                if(PredictionAudit.hasNewHit(this))nm.notify(2202,buildAlert("Tahmin denetimi: isabet kaydı","Önceden kaydedilmiş bir risk bölgesinde tanımlı doğrulama kriterini karşılayan olay gözlendi. Denetim ekranında gerekçe ve hata mesafesi var."));
                lastTr=tr.maxScore;
            }catch(Exception ignored){}
            lastAlertScore=r.maxScore;if(fx!=null)lastInfra=fx.infrastructureIndex;
        }catch(Exception ignored){}
    }

    private void createChannels(){if(Build.VERSION.SDK_INT>=26){NotificationChannel mon=new NotificationChannel(CH_MON,"Quake Watch izleme",NotificationManager.IMPORTANCE_LOW);NotificationChannel alert=new NotificationChannel(CH_ALERT,"Quake Watch kritik uyarılar",NotificationManager.IMPORTANCE_HIGH);alert.enableVibration(true);NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);nm.createNotificationChannel(mon);nm.createNotificationChannel(alert);}}
    private PendingIntent pi(){Intent i=new Intent(this,ResearchConsoleActivity.class);return PendingIntent.getActivity(this,0,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);}
    private Notification buildNotification(String text){Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CH_MON):new Notification.Builder(this);return b.setContentTitle("Quake Watch Research Lab").setContentText(text).setSmallIcon(android.R.drawable.ic_dialog_info).setContentIntent(pi()).setOngoing(true).build();}
    private Notification buildAlert(String title,String text){Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CH_ALERT):new Notification.Builder(this);return b.setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setSmallIcon(android.R.drawable.ic_dialog_alert).setContentIntent(pi()).setAutoCancel(true).setPriority(Notification.PRIORITY_HIGH).build();}
    @Override public void onDestroy(){exec.shutdownNow();super.onDestroy();}@Override public IBinder onBind(Intent intent){return null;}
}
