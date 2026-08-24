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
    private static final String CH="quake_watch_monitor";
    private static final int FG=1001;
    private final ScheduledExecutorService exec=Executors.newSingleThreadScheduledExecutor();
    private double lastAlertScore=0;

    @Override public void onCreate(){
        super.onCreate();
        createChannel();
        startForeground(FG, buildNotification("Dünya sismik aktivitesi izleniyor"));
        exec.scheduleWithFixedDelay(this::check, 0, 15, TimeUnit.MINUTES);
    }

    private void check(){
        try{
            QuakeAnalyzer.Report r=QuakeAnalyzer.fetchAndAnalyze();
            NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            nm.notify(FG, buildNotification("Son analiz: "+r.eventCount+" olay • en yüksek aktivite puanı "+Math.round(r.maxScore)+"/100"));
            if(r.maxScore>=75 && (lastAlertScore<75 || r.maxScore-lastAlertScore>=8)){
                nm.notify(2001, buildAlert("Sismik aktivite anomalisi", "Küresel katalogda yüksek istatistiksel aktivite puanı: "+Math.round(r.maxScore)+"/100. Bu bir deprem tahmini değildir."));
            }
            lastAlertScore=r.maxScore;
        }catch(Exception ignored){}
    }

    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel c=new NotificationChannel(CH,"Quake Watch izleme",NotificationManager.IMPORTANCE_DEFAULT);
            c.setDescription("Dünya deprem kataloğu ve istatistiksel anomali uyarıları");
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
    }

    private PendingIntent pi(){
        Intent i=new Intent(this,MainActivity.class);
        return PendingIntent.getActivity(this,0,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Notification buildNotification(String text){
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CH):new Notification.Builder(this);
        return b.setContentTitle("Quake Watch").setContentText(text).setSmallIcon(android.R.drawable.ic_dialog_info).setContentIntent(pi()).setOngoing(true).build();
    }

    private Notification buildAlert(String title,String text){
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CH):new Notification.Builder(this);
        return b.setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setSmallIcon(android.R.drawable.ic_dialog_alert).setContentIntent(pi()).setAutoCancel(true).build();
    }

    @Override public void onDestroy(){exec.shutdownNow();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
