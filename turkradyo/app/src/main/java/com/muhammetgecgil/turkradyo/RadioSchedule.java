package com.muhammetgecgil.turkradyo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.Locale;

/** Persistent, single wake alarm and independent monotonic sleep timer. */
final class RadioSchedule {
    static final String WAKE="com.muhammetgecgil.turkradyo.WAKE", SLEEP="com.muhammetgecgil.turkradyo.SLEEP";
    private static final int WAKE_ID=8201, SLEEP_ID=8299;
    private RadioSchedule() {}
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("radio",Context.MODE_PRIVATE);}
    private static AlarmManager manager(Context c){return (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);}
    static boolean exactAllowed(Context c){return Build.VERSION.SDK_INT<31||manager(c).canScheduleExactAlarms();}
    static long nextTime(int hour,int minute,long now){
        Calendar date=Calendar.getInstance();date.setTimeInMillis(now);
        date.set(Calendar.HOUR_OF_DAY,hour);date.set(Calendar.MINUTE,minute);date.set(Calendar.SECOND,0);date.set(Calendar.MILLISECOND,0);
        if(date.getTimeInMillis()<=now)date.add(Calendar.DAY_OF_YEAR,1);
        return date.getTimeInMillis();
    }
    private static PendingIntent pending(Context c,String action,int id,long when){
        return PendingIntent.getBroadcast(c,id,new Intent(c,AlarmReceiver.class).setAction(action).putExtra("when",when),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private static void cancelPending(Context c,String action,int id){
        PendingIntent p=PendingIntent.getBroadcast(c,id,new Intent(c,AlarmReceiver.class).setAction(action),PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);
        if(p!=null){manager(c).cancel(p);p.cancel();}
    }
    static synchronized String setWake(Context c,int hour,int minute,String url,String name,boolean daily){
        if(hour<0||hour>23||minute<0||minute>59||url==null||!url.matches("https?://.+"))return "{\"error\":\"Saat ve yayın seçimini kontrol et\"}";
        cancelPending(c,null,WAKE_ID);
        prefs(c).edit().putInt("wakeHour",hour).putInt("wakeMinute",minute).putString("wakeUrl",url).putString("wakeName",name==null?"Radyo":name)
                .putBoolean("wakeDaily",daily).putBoolean("wakeEnabled",true).putLong("wakeSavedAt",System.currentTimeMillis()).apply();
        scheduleWake(c,nextTime(hour,minute,System.currentTimeMillis()));return wakeJson(c);
    }
    private static void scheduleWake(Context c,long when){
        SharedPreferences p=prefs(c);p.edit().putLong("wakeWhen",when).putString("wakeStatus","permission").remove("wakeError").apply();
        cancelPending(c,WAKE,WAKE_ID);
        if(!exactAllowed(c))return; // Never claim an inexact wake-up is armed.
        try{
            PendingIntent show=PendingIntent.getActivity(c,8202,new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            manager(c).setAlarmClock(new AlarmManager.AlarmClockInfo(when,show),pending(c,WAKE,WAKE_ID,when));
            p.edit().putString("wakeStatus","scheduled").apply();
        }catch(SecurityException e){p.edit().putString("wakeStatus","permission").apply();}
        catch(RuntimeException e){p.edit().putString("wakeStatus","error").putString("wakeError","Alarm planlanamadı. Yeniden kurmayı dene.").apply();}
    }
    static synchronized String cancelWake(Context c){
        cancelPending(c,WAKE,WAKE_ID);cancelPending(c,null,WAKE_ID);
        prefs(c).edit().putBoolean("wakeEnabled",false).putString("wakeStatus","cancelled").remove("wakeError").apply();return wakeJson(c);
    }
    static synchronized String wakeJson(Context c){
        SharedPreferences p=prefs(c);
        try{
            boolean enabled=p.getBoolean("wakeEnabled",false),exact=exactAllowed(c);
            String status=enabled&&!exact?"permission":p.getString("wakeStatus","off");JSONObject o=new JSONObject();
            o.put("time",String.format(Locale.ROOT,"%02d:%02d",p.getInt("wakeHour",7),p.getInt("wakeMinute",0)));
            o.put("when",p.getLong("wakeWhen",0));o.put("savedAt",p.getLong("wakeSavedAt",0));o.put("name",p.getString("wakeName",""));o.put("url",p.getString("wakeUrl",""));
            o.put("daily",p.getBoolean("wakeDaily",false));o.put("enabled",enabled);o.put("scheduled",enabled&&"scheduled".equals(status));o.put("exactAllowed",exact);
            o.put("status",status);o.put("error",p.getString("wakeError",""));return o.toString();
        }catch(Exception e){return "{}";}
    }
    /** Only the current, due alarm may start playback. */
    static synchronized Intent consumeWake(Context c,Intent in){
        SharedPreferences p=prefs(c);long when=p.getLong("wakeWhen",0),now=System.currentTimeMillis();
        if(!p.getBoolean("wakeEnabled",false)||!"scheduled".equals(p.getString("wakeStatus",""))||when<=0||when>now||in.getLongExtra("when",0)!=when)return null;
        Intent play=new Intent(c,RadioService.class).setAction(RadioService.ACTION_PLAY).putExtra("url",p.getString("wakeUrl","")).putExtra("name",p.getString("wakeName","Radyo")).putExtra("alarmMode",true);
        cancelSleep(c,false); // An evening timer must not silence the wake-up.
        p.edit().putLong("wakeLastTriggeredAt",now).putBoolean("wakeEnabled",false).putString("wakeStatus","fired").apply();
        if(p.getBoolean("wakeDaily",false)){
            p.edit().putBoolean("wakeEnabled",true).apply();scheduleWake(c,nextTime(p.getInt("wakeHour",7),p.getInt("wakeMinute",0),now));
        }else cancelPending(c,WAKE,WAKE_ID);
        return play;
    }
    static synchronized void restore(Context c,String reason){
        SharedPreferences p=prefs(c);long now=System.currentTimeMillis();boolean boot=Intent.ACTION_BOOT_COMPLETED.equals(reason);
        if(p.getBoolean("wakeEnabled",false)){
            long when=p.getLong("wakeWhen",0);boolean permission="permission".equals(p.getString("wakeStatus",""));
            boolean clock=Intent.ACTION_TIME_CHANGED.equals(reason)||Intent.ACTION_TIMEZONE_CHANGED.equals(reason);
            if(permission||clock||p.getBoolean("wakeDaily",false)&&when<now-(boot?0:60_000))when=nextTime(p.getInt("wakeHour",7),p.getInt("wakeMinute",0),now);
            if(when>now)scheduleWake(c,when);
            else if(boot||when<now-60_000){p.edit().putBoolean("wakeEnabled",false).putString("wakeStatus","missed").apply();cancelPending(c,WAKE,WAKE_ID);}
        }
        if(boot){long left=p.getLong("sleepDeadline",0)-now;if(left>0)p.edit().putLong("sleepElapsedDeadline",SystemClock.elapsedRealtime()+left).apply();else cancelSleep(c,false);}
        if(SleepTimer.remaining(p)>0)scheduleSleep(c);
    }
    static synchronized String setSleep(Context c,long when,boolean fade){
        long delay=when-System.currentTimeMillis();if(delay<=0||delay>13*60*60_000L)return "{\"error\":\"1 dakika ile 12 saat 59 dakika arasında süre seç\"}";
        prefs(c).edit().putLong("sleepDeadline",when).putLong("sleepElapsedDeadline",SystemClock.elapsedRealtime()+delay).putBoolean("sleepFade",fade).apply();
        try{scheduleSleep(c);notifySleep(c);return sleepJson(c);}
        catch(RuntimeException e){cancelSleep(c,true);return "{\"error\":\"Zamanlayıcı başlatılamadı\"}";}
    }
    private static void scheduleSleep(Context c){
        SharedPreferences p=prefs(c);long when=p.getLong("sleepDeadline",0),at=SystemClock.elapsedRealtime()+Math.max(0,SleepTimer.remaining(p));
        PendingIntent delivery=pending(c,SLEEP,SLEEP_ID,when);cancelPending(c,null,SLEEP_ID);
        if(exactAllowed(c)){try{manager(c).setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,at,delivery);return;}catch(SecurityException ignored){}}
        manager(c).setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,at,delivery);
    }
    static synchronized String cancelSleep(Context c,boolean notify){
        cancelPending(c,SLEEP,SLEEP_ID);cancelPending(c,null,SLEEP_ID);
        prefs(c).edit().remove("sleepDeadline").remove("sleepElapsedDeadline").remove("sleepFade").apply();if(notify)notifySleep(c);return sleepJson(c);
    }
    private static void notifySleep(Context c){if(RadioService.isRunning)c.startService(new Intent(c,RadioService.class).setAction(RadioService.ACTION_SLEEP_TIMER));}
    static synchronized boolean finishSleepIfDue(Context c){
        SharedPreferences p=prefs(c);if(p.getLong("sleepDeadline",0)<=0||SleepTimer.remaining(p)>0)return false;cancelSleep(c,false);return true;
    }
    static synchronized void deliverSleep(Context c,Intent in){
        SharedPreferences p=prefs(c);long deadline=p.getLong("sleepDeadline",0);
        if(deadline<=0||in.getLongExtra("when",0)!=deadline||SleepTimer.remaining(p)>0)return;
        if(RadioService.isRunning)notifySleep(c);else finishSleepIfDue(c);
    }
    static synchronized String sleepJson(Context c){
        try{SharedPreferences p=prefs(c);long left=SleepTimer.remaining(p);return new JSONObject().put("when",left>0?System.currentTimeMillis()+left:0).put("fade",left>0&&p.getBoolean("sleepFade",false)).put("exactAllowed",exactAllowed(c)).toString();}catch(Exception e){return "{}";}
    }
}
