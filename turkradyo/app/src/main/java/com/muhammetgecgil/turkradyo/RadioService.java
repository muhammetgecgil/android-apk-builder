package com.muhammetgecgil.turkradyo;

import android.app.*;
import android.content.*;
import android.media.*;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.media.MediaMetadata;
import android.net.*;
import android.os.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RadioService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener, MediaPlayer.OnTimedMetaDataAvailableListener {
    public static final String ACTION_PLAY="com.muhammetgecgil.turkradyo.PLAY", ACTION_PAUSE="com.muhammetgecgil.turkradyo.PAUSE", ACTION_RESUME="com.muhammetgecgil.turkradyo.RESUME", ACTION_STOP="com.muhammetgecgil.turkradyo.STOP", ACTION_PREV="com.muhammetgecgil.turkradyo.PREV", ACTION_NEXT="com.muhammetgecgil.turkradyo.NEXT", ACTION_VOLUME="com.muhammetgecgil.turkradyo.VOLUME", ACTION_GAIN="com.muhammetgecgil.turkradyo.GAIN", ACTION_EQ="com.muhammetgecgil.turkradyo.EQ", ACTION_NORMALIZE="com.muhammetgecgil.turkradyo.NORMALIZE", ACTION_SMOOTH="com.muhammetgecgil.turkradyo.SMOOTH";
    private static final int NOTIF_ID=1201;
    private static final String CHANNEL="radio_playback", PREF="radio", HISTORY="stationHistory";
    private static final Pattern STREAM_TITLE=Pattern.compile("StreamTitle\\s*=\\s*['\\\"]([^'\\\"]+)",Pattern.CASE_INSENSITIVE|Pattern.DOTALL);

    private MediaPlayer player;
    private LoudnessEnhancer enhancer;
    private Equalizer equalizer;
    private MediaSession mediaSession;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Runnable reconnectTask, watchdogTask, fadeTask, livenessTask, networkRecoveryTask;

    private String stationName="Türk Radyo", primaryUrl="", streamUrl="", networkType="unknown", lastTrackTitle="";
    private boolean userPaused=false, buffering=false, smooth=true, normalize=false, repairBusy=false, resumeAfterFocus=false;
    private float volume=1f;
    private int gainMb=0, reconnectAttempts=0, bufferCount=0, lastError=0, livenessMisses=0, livenessRecoveries=0, networkTransitions=0, repairFailures=0;
    private long playStartMs=0, startupMs=0, preparedAtMs=0, serviceStartMs=0, lastNetworkChangeMs=0, lastTrackMs=0;
    private final short[] eqLevels=new short[]{0,0,0,0,0};

    @Override public void onCreate(){
        super.onCreate();
        serviceStartMs=System.currentTimeMillis();
        createChannel();
        audioManager=(AudioManager)getSystemService(AUDIO_SERVICE);
        SharedPreferences p=getSharedPreferences(PREF,MODE_PRIVATE);
        smooth=p.getBoolean("smooth",true); normalize=p.getBoolean("normalize",false); volume=p.getFloat("volume",1f);
        initMediaSession(); registerNetworkMonitor();
    }

    private void registerNetworkMonitor(){
        try{
            connectivityManager=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            networkType=currentNetworkType();
            if(Build.VERSION.SDK_INT>=24){
                networkCallback=new ConnectivityManager.NetworkCallback(){
                    @Override public void onAvailable(Network network){onNetworkChanged(currentNetworkType());}
                    @Override public void onLost(Network network){onNetworkChanged("offline");}
                    @Override public void onCapabilitiesChanged(Network network,NetworkCapabilities caps){onNetworkChanged(typeFromCaps(caps));}
                };
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            }
        }catch(Exception ignored){}
    }

    private String currentNetworkType(){try{Network n=connectivityManager==null?null:connectivityManager.getActiveNetwork();NetworkCapabilities c=n==null?null:connectivityManager.getNetworkCapabilities(n);return typeFromCaps(c);}catch(Exception e){return"unknown";}}
    private String typeFromCaps(NetworkCapabilities c){if(c==null)return"offline";if(c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))return"wifi";if(c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))return"cellular";if(c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))return"ethernet";if(c.hasTransport(NetworkCapabilities.TRANSPORT_VPN))return"vpn";return"other";}
    private void onNetworkChanged(String next){
        if(next==null)next="unknown"; if(next.equals(networkType))return;
        final String old=networkType; networkType=next; networkTransitions++; lastNetworkChangeMs=System.currentTimeMillis(); saveTelemetry();
        if("offline".equals(next)){if(!userPaused&&!primaryUrl.isEmpty()){PlaybackGuardian.interrupted(this,"network");updateMediaSession(false,"İnternet bekleniyor");updateNotification("İnternet bekleniyor",true);}return;}
        if(userPaused||primaryUrl.isEmpty())return;
        if(networkRecoveryTask!=null)handler.removeCallbacks(networkRecoveryTask);
        networkRecoveryTask=()->{if(userPaused)return;boolean ok=false;try{ok=player!=null&&player.isPlaying();}catch(Exception ignored){}if(!ok){reconnectAttempts=0;playResolved(StreamFallbackManager.getPreferred(this,stationName,primaryUrl));}else{PlaybackGuardian.recovered(this);updateNotification("Ağ geçişi: "+old+" → "+networkType,true);}};
        handler.postDelayed(networkRecoveryTask,1500);
    }

    private void initMediaSession(){
        mediaSession=new MediaSession(this,"TurkRadyoSession");
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS|MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSession.Callback(){
            @Override public void onPlay(){resume();}
            @Override public void onPause(){pause(true);}
            @Override public void onStop(){stopAll();}
            @Override public void onSkipToNext(){stepQueue(1);}
            @Override public void onSkipToPrevious(){if(!previousFromHistory())stepQueue(-1);}
            @Override public void onPlayFromMediaId(String mediaId,Bundle extras){playFromMediaId(mediaId);}
        });
        mediaSession.setActive(true); updateMediaSession(false,"Hazır");
    }

    @Override public int onStartCommand(Intent in,int flags,int id){
        if(in==null)return START_STICKY; String a=in.getAction();
        if(ACTION_PLAY.equals(a)){
            String u=in.getStringExtra("url"),n=in.getStringExtra("name");
            if(u!=null&&!u.isEmpty())startStation(u,n,true);
        }else if(ACTION_PREV.equals(a)){if(!previousFromHistory())stepQueue(-1);}
        else if(ACTION_NEXT.equals(a))stepQueue(1);
        else if(ACTION_PAUSE.equals(a))pause(true);
        else if(ACTION_RESUME.equals(a))resume();
        else if(ACTION_STOP.equals(a))stopAll();
        else if(ACTION_VOLUME.equals(a)){volume=Math.max(0f,Math.min(1f,in.getFloatExtra("volume",1f)));getSharedPreferences(PREF,MODE_PRIVATE).edit().putFloat("volume",volume).apply();if(player!=null)try{player.setVolume(volume,volume);}catch(Exception ignored){}}
        else if(ACTION_GAIN.equals(a)){gainMb=in.getIntExtra("gain",0);applyGain();}
        else if(ACTION_EQ.equals(a)){int b=in.getIntExtra("band",0),l=in.getIntExtra("level",0);if(b>=0&&b<eqLevels.length){eqLevels[b]=(short)Math.max(-1500,Math.min(1500,l));applyEq();}}
        else if(ACTION_NORMALIZE.equals(a)){normalize=in.getBooleanExtra("on",false);getSharedPreferences(PREF,MODE_PRIVATE).edit().putBoolean("normalize",normalize).apply();applyGain();}
        else if(ACTION_SMOOTH.equals(a)){smooth=in.getBooleanExtra("on",true);getSharedPreferences(PREF,MODE_PRIVATE).edit().putBoolean("smooth",smooth).apply();}
        return START_STICKY;
    }

    private void playFromMediaId(String id){
        if(id==null)return;
        try{
            if(id.startsWith("q:")){playQueueIndex(Integer.parseInt(id.substring(2)));return;}
            if(id.startsWith("recent:")){int i=Integer.parseInt(id.substring(7));JSONArray a=new JSONArray(getSharedPreferences(PREF,MODE_PRIVATE).getString("recentStations","[]"));JSONObject o=a.optJSONObject(i);if(o!=null)startStation(o.optString("url"),o.optString("name","Türk Radyo"),true);}
        }catch(Exception ignored){}
    }

    private void startStation(String u,String n,boolean rememberCurrent){
        if(u==null||u.isEmpty())return;
        if(rememberCurrent)pushHistory(u);
        PlaybackGuardian.manualPlay(this);
        primaryUrl=u; stationName=(n==null||n.isEmpty())?"Türk Radyo":n; lastTrackTitle=""; lastTrackMs=0;
        getSharedPreferences(PREF,MODE_PRIVATE).edit().putString("nowTitle","").apply();
        reconnectAttempts=0; repairBusy=false; userPaused=false; resumeAfterFocus=false; livenessMisses=0;
        syncQueueIndexForUrl(u);
        playResolved(StreamFallbackManager.getPreferred(this,stationName,primaryUrl));
    }

    private void pushHistory(String incomingUrl){
        if(primaryUrl==null||primaryUrl.isEmpty()||primaryUrl.equals(incomingUrl))return;
        try{
            SharedPreferences p=getSharedPreferences(PREF,MODE_PRIVATE); JSONArray old;
            try{old=new JSONArray(p.getString(HISTORY,"[]"));}catch(Exception e){old=new JSONArray();}
            JSONArray out=new JSONArray(); JSONObject cur=new JSONObject(); cur.put("url",primaryUrl); cur.put("name",stationName); out.put(cur);
            for(int i=0;i<old.length()&&out.length()<30;i++){JSONObject x=old.optJSONObject(i);if(x!=null&&!primaryUrl.equals(x.optString("url")))out.put(x);}
            p.edit().putString(HISTORY,out.toString()).apply();
        }catch(Exception ignored){}
    }

    private boolean previousFromHistory(){
        try{
            SharedPreferences p=getSharedPreferences(PREF,MODE_PRIVATE); JSONArray a;
            try{a=new JSONArray(p.getString(HISTORY,"[]"));}catch(Exception e){a=new JSONArray();}
            JSONObject pick=null; JSONArray remain=new JSONArray();
            for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x==null)continue;String u=x.optString("url");if(pick==null&&!u.isEmpty()&&!u.equals(primaryUrl)){pick=x;continue;}remain.put(x);}
            if(pick==null){
                JSONArray recent;try{recent=new JSONArray(p.getString("recentStations","[]"));}catch(Exception e){recent=new JSONArray();}
                for(int i=0;i<recent.length();i++){JSONObject x=recent.optJSONObject(i);if(x!=null&&!x.optString("url").isEmpty()&&!x.optString("url").equals(primaryUrl)){pick=x;break;}}
            }
            if(pick==null)return false;
            p.edit().putString(HISTORY,remain.toString()).apply();
            startStation(pick.optString("url"),pick.optString("name","Türk Radyo"),false);
            return true;
        }catch(Exception e){return false;}
    }

    private void playResolved(String url){
        if(url==null||url.isEmpty())url=primaryUrl;
        streamUrl=url; playStartMs=System.currentTimeMillis(); startupMs=0; preparedAtMs=0; buffering=false; lastError=0; livenessMisses=0;
        cancelTasks(); releasePlayer(); saveTelemetry(); saveCurrent(); requestFocus(); updateMediaSession(false,"Bağlanıyor…"); startForeground(NOTIF_ID,buildNotification("Bağlanıyor…",true));
        try{
            player=new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            player.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);
            player.setOnPreparedListener(this); player.setOnErrorListener(this); player.setOnInfoListener(this);
            if(Build.VERSION.SDK_INT>=23)player.setOnTimedMetaDataAvailableListener(this);
            player.setDataSource(this,Uri.parse(streamUrl)); player.setVolume(smooth?0f:volume,smooth?0f:volume); player.prepareAsync(); armStartupWatchdog(streamUrl);
        }catch(Exception e){lastError=-1;saveTelemetry();handleFailure("Açılamadı");}
    }

    @Override public void onPrepared(MediaPlayer mp){
        startupMs=Math.max(1,System.currentTimeMillis()-playStartMs); preparedAtMs=System.currentTimeMillis(); reconnectAttempts=0; repairBusy=false; buffering=false; cancelWatchdog(); livenessMisses=0;
        try{
            mp.start(); applyGain(); applyEq(); if(smooth)fadeIn(mp);else mp.setVolume(volume,volume);
            StreamFallbackManager.markGood(this,stationName,streamUrl,startupMs); PlaybackGuardian.recovered(this); saveRecent(); saveCurrent(); saveTelemetry(); updateMediaSession(true,"Canlı yayın"); updateNotification("Canlı yayın",true); armLivenessWatchdog();
        }catch(Exception e){lastError=-2;handleFailure("Başlatılamadı");}
    }

    @Override public boolean onError(MediaPlayer mp,int what,int extra){lastError=what;buffering=false;saveTelemetry();handleFailure("Bağlantı kesildi");return true;}
    @Override public boolean onInfo(MediaPlayer mp,int what,int extra){
        if(what==MediaPlayer.MEDIA_INFO_BUFFERING_START){buffering=true;bufferCount++;livenessMisses=0;saveTelemetry();armBufferWatchdog();}
        else if(what==MediaPlayer.MEDIA_INFO_BUFFERING_END){buffering=false;cancelWatchdog();livenessMisses=0;saveTelemetry();}
        return false;
    }

    @Override public void onTimedMetaDataAvailable(MediaPlayer mp,TimedMetaData data){if(data==null)return;try{String title=extractTimedTitle(data.getMetaData());if(!title.isEmpty())recordTrack(title,"PLAYER");}catch(Exception ignored){}}
    private String extractTimedTitle(byte[] raw){if(raw==null||raw.length==0)return"";String text;try{text=new String(raw,StandardCharsets.UTF_8).replace('\u0000',' ').trim();}catch(Exception e){return"";}Matcher m=STREAM_TITLE.matcher(text);if(m.find())return cleanTrackTitle(m.group(1));int i=text.indexOf("TIT2");if(i>=0){String t=text.substring(i+4).replaceAll("[\\x00-\\x1F]+"," ").trim();int cut=t.length();for(String marker:new String[]{"TPE1","TALB","TRCK","TCON","COMM","TYER","TDRC"}){int p=t.indexOf(marker);if(p>=0&&p<cut)cut=p;}if(cut>0)return cleanTrackTitle(t.substring(0,cut));}return"";}
    private String cleanTrackTitle(String s){String x=s==null?"":s.replaceAll("\\s+"," ").trim();x=x.replaceAll("^[\\-–—|•\\s]+|[\\-–—|•\\s]+$","").trim();if(x.length()>240)x=x.substring(0,240).trim();if(x.length()<2||x.equalsIgnoreCase("unknown")||x.equalsIgnoreCase("null")||x.equals("-"))return"";return x;}
    private void recordTrack(String title,String source){title=cleanTrackTitle(title);if(title.isEmpty())return;long now=System.currentTimeMillis();if(title.equalsIgnoreCase(lastTrackTitle)&&now-lastTrackMs<90_000L)return;lastTrackTitle=title;lastTrackMs=now;try{SharedPreferences p=getSharedPreferences(PREF,MODE_PRIVATE);JSONArray old;try{old=new JSONArray(p.getString("tracks","[]"));}catch(Exception e){old=new JSONArray();}JSONArray out=new JSONArray();JSONObject n=new JSONObject();n.put("title",title);n.put("station",stationName);n.put("time",now);n.put("source",source);out.put(n);for(int i=0;i<old.length()&&out.length()<500;i++){JSONObject x=old.optJSONObject(i);if(x!=null)out.put(x);}p.edit().putString("tracks",out.toString()).putString("nowTitle",title).apply();updateMediaSession(true,title);}catch(Exception ignored){}}

    private void handleFailure(String label){
        if(userPaused)return; cancelLivenessWatchdog(); updateMediaSession(false,label); updateNotification(label,true); StreamFallbackManager.markBad(this,stationName,streamUrl,reconnectAttempts<1?60_000L:30*60_000L);
        if(reconnectAttempts<1){reconnectAttempts++;schedulePlay(primaryUrl,1200);return;} repairSameStation();
    }
    private void repairSameStation(){if(repairBusy||userPaused)return;repairBusy=true;updateNotification("Aynı radyo için kaynak aranıyor…",true);StreamFallbackManager.discoverBestAsync(this,stationName,primaryUrl,u->{repairBusy=false;if(userPaused)return;if(u!=null&&!u.isEmpty()){reconnectAttempts=0;playResolved(u);}else{repairFailures++;saveTelemetry();schedulePlay(primaryUrl,3500);}});}
    private void schedulePlay(String u,long delay){cancelReconnect();reconnectTask=()->{if(!userPaused)playResolved(u);};handler.postDelayed(reconnectTask,delay);}
    private void armStartupWatchdog(final String expected){cancelWatchdog();watchdogTask=()->{if(!userPaused&&player!=null&&startupMs==0&&expected.equals(streamUrl)){lastError=-31;saveTelemetry();handleFailure("Geç bağlantı");}};handler.postDelayed(watchdogTask,12000);}
    private void armBufferWatchdog(){cancelWatchdog();watchdogTask=()->{if(buffering&&!userPaused){lastError=-30;saveTelemetry();handleFailure("Uzun buffer");}};handler.postDelayed(watchdogTask,20000);}

    private void armLivenessWatchdog(){
        cancelLivenessWatchdog();
        livenessTask=new Runnable(){@Override public void run(){
            if(userPaused||player==null)return;
            if(buffering||"offline".equals(networkType)){livenessMisses=0;handler.postDelayed(this,10000);return;}
            boolean playing=true;try{playing=player.isPlaying();}catch(Exception e){playing=false;}
            if(playing)livenessMisses=0;else livenessMisses++;
            if(preparedAtMs>0&&System.currentTimeMillis()-preparedAtMs>=30000&&livenessMisses>=3){lastError=-33;livenessRecoveries++;saveTelemetry();cancelLivenessWatchdog();handleFailure("Oynatma durdu");return;}
            saveTelemetry(); handler.postDelayed(this,10000);
        }};
        handler.postDelayed(livenessTask,10000);
    }

    private void cancelLivenessWatchdog(){if(livenessTask!=null){handler.removeCallbacks(livenessTask);livenessTask=null;}}
    private void cancelWatchdog(){if(watchdogTask!=null){handler.removeCallbacks(watchdogTask);watchdogTask=null;}}
    private void cancelReconnect(){if(reconnectTask!=null){handler.removeCallbacks(reconnectTask);reconnectTask=null;}}
    private void cancelTasks(){cancelReconnect();cancelWatchdog();cancelLivenessWatchdog();if(networkRecoveryTask!=null){handler.removeCallbacks(networkRecoveryTask);networkRecoveryTask=null;}if(fadeTask!=null){handler.removeCallbacks(fadeTask);fadeTask=null;}}

    private void syncQueueIndexForUrl(String url){try{SharedPreferences p=getSharedPreferences(PREF,MODE_PRIVATE);JSONArray a=new JSONArray(p.getString("queue","[]"));for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&url.equals(o.optString("url"))){p.edit().putInt("queueIndex",i).apply();break;}}}catch(Exception ignored){}}
    private void stepQueue(int d){try{SharedPreferences p=getSharedPreferences(PREF,MODE_PRIVATE);JSONArray q=new JSONArray(p.getString("queue","[]"));if(q.length()==0)return;int i=p.getInt("queueIndex",0);i=(i+d)%q.length();if(i<0)i+=q.length();playQueueIndex(i);}catch(Exception ignored){}}
    private void playQueueIndex(int i){try{SharedPreferences p=getSharedPreferences(PREF,MODE_PRIVATE);JSONArray q=new JSONArray(p.getString("queue","[]"));JSONObject o=q.optJSONObject(i);if(o==null)return;String u=o.optString("url"),n=o.optString("name","Türk Radyo");if(u.isEmpty())return;p.edit().putInt("queueIndex",i).apply();startStation(u,n,true);}catch(Exception ignored){}}

    private void saveCurrent(){getSharedPreferences(PREF,MODE_PRIVATE).edit().putString("url",primaryUrl).putString("resolvedUrl",streamUrl).putString("name",stationName).apply();}
    private void saveRecent(){try{SharedPreferences p=getSharedPreferences(PREF,MODE_PRIVATE);JSONArray old;try{old=new JSONArray(p.getString("recentStations","[]"));}catch(Exception e){old=new JSONArray();}JSONArray out=new JSONArray();JSONObject n=new JSONObject();n.put("name",stationName);n.put("url",primaryUrl);out.put(n);for(int i=0;i<old.length()&&out.length()<20;i++){JSONObject x=old.optJSONObject(i);if(x!=null&&!primaryUrl.equals(x.optString("url")))out.put(x);}p.edit().putString("recentStations",out.toString()).apply();}catch(Exception ignored){}}
    private void saveTelemetry(){try{JSONObject o=new JSONObject();o.put("startupMs",startupMs);o.put("bufferCount",bufferCount);o.put("lastError",lastError);o.put("since",playStartMs);o.put("buffering",buffering);o.put("reconnectAttempts",reconnectAttempts);o.put("resolvedUrl",streamUrl);o.put("silentGuard",false);o.put("livenessGuard",true);o.put("livenessMisses",livenessMisses);o.put("livenessRecoveries",livenessRecoveries);o.put("networkType",networkType);o.put("networkTransitions",networkTransitions);o.put("lastNetworkChangeMs",lastNetworkChangeMs);o.put("repairFailures",repairFailures);o.put("serviceUptimeMs",Math.max(0,System.currentTimeMillis()-serviceStartMs));o.put("liveMs",preparedAtMs>0?Math.max(0,System.currentTimeMillis()-preparedAtMs):0);o.put("playbackGuardian",true);o.put("guardianReason",PlaybackGuardian.reason(this));getSharedPreferences(PREF,MODE_PRIVATE).edit().putString("telemetry",o.toString()).apply();}catch(Exception ignored){}}

    private void updateMediaSession(boolean playing,String subtitle){if(mediaSession==null)return;long acts=PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_PLAY_PAUSE|PlaybackState.ACTION_STOP|PlaybackState.ACTION_SKIP_TO_NEXT|PlaybackState.ACTION_SKIP_TO_PREVIOUS|PlaybackState.ACTION_PLAY_FROM_MEDIA_ID;int state=playing?PlaybackState.STATE_PLAYING:(userPaused?PlaybackState.STATE_PAUSED:PlaybackState.STATE_CONNECTING);mediaSession.setPlaybackState(new PlaybackState.Builder().setActions(acts).setState(state,PlaybackState.PLAYBACK_POSITION_UNKNOWN,playing?1f:0f).build());String now=getSharedPreferences(PREF,MODE_PRIVATE).getString("nowTitle","");mediaSession.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,stationName).putString(MediaMetadata.METADATA_KEY_ARTIST,now.isEmpty()?subtitle:now).putString(MediaMetadata.METADATA_KEY_ALBUM,"Türk Radyo").build());}
    private void pause(boolean manual){if(manual){userPaused=true;resumeAfterFocus=false;PlaybackGuardian.manualPause(this);}else PlaybackGuardian.interrupted(this,"audio_focus");cancelTasks();if(player!=null)try{if(player.isPlaying())player.pause();}catch(Exception ignored){}updateMediaSession(false,manual?"Duraklatıldı":"Geçici olarak duraklatıldı");updateNotification(manual?"Duraklatıldı":"Geçici olarak duraklatıldı",false);}
    private void resume(){PlaybackGuardian.manualPlay(this);resumeAfterFocus=false;userPaused=false;if(player!=null)try{player.start();updateMediaSession(true,"Canlı yayın");updateNotification("Canlı yayın",true);armLivenessWatchdog();}catch(Exception e){playResolved(StreamFallbackManager.getPreferred(this,stationName,primaryUrl));}else if(!primaryUrl.isEmpty())playResolved(StreamFallbackManager.getPreferred(this,stationName,primaryUrl));}
    private void autoResumeAfterFocus(){if(userPaused||!resumeAfterFocus||!PlaybackGuardian.mayAutoResume(this))return;resumeAfterFocus=false;if(player!=null)try{player.start();PlaybackGuardian.recovered(this);updateMediaSession(true,"Canlı yayın");updateNotification("Canlı yayın",true);armLivenessWatchdog();}catch(Exception e){playResolved(StreamFallbackManager.getPreferred(this,stationName,primaryUrl));}else if(!primaryUrl.isEmpty())playResolved(StreamFallbackManager.getPreferred(this,stationName,primaryUrl));}
    private void stopAll(){userPaused=true;resumeAfterFocus=false;PlaybackGuardian.manualPause(this);cancelTasks();releasePlayer();abandonFocus();if(mediaSession!=null)mediaSession.setActive(false);stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}

    private void requestFocus(){if(Build.VERSION.SDK_INT>=26){if(focusRequest!=null)audioManager.abandonAudioFocusRequest(focusRequest);focusRequest=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setOnAudioFocusChangeListener(change->{if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT||change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){resumeAfterFocus=player!=null&&!userPaused;pause(false);}else if(change==AudioManager.AUDIOFOCUS_GAIN){autoResumeAfterFocus();}else if(change==AudioManager.AUDIOFOCUS_LOSS){resumeAfterFocus=false;PlaybackGuardian.interrupted(this,"audio_focus_loss");pause(false);}}).build();audioManager.requestAudioFocus(focusRequest);}}
    private void abandonFocus(){if(Build.VERSION.SDK_INT>=26&&focusRequest!=null){audioManager.abandonAudioFocusRequest(focusRequest);focusRequest=null;}}
    private void applyGain(){if(player==null)return;try{if(enhancer!=null)enhancer.release();enhancer=new LoudnessEnhancer(player.getAudioSessionId());int t=normalize?Math.max(300,gainMb):gainMb;t=Math.max(0,Math.min(1200,t));enhancer.setTargetGain(t);enhancer.setEnabled(t>0);}catch(Exception ignored){}}
    private void applyEq(){if(player==null)return;try{if(equalizer!=null)equalizer.release();equalizer=new Equalizer(0,player.getAudioSessionId());short bands=equalizer.getNumberOfBands();short[] range=equalizer.getBandLevelRange();for(short b=0;b<bands&&b<eqLevels.length;b++)equalizer.setBandLevel(b,(short)Math.max(range[0],Math.min(range[1],eqLevels[b])));equalizer.setEnabled(true);}catch(Exception ignored){}}
    private void fadeIn(MediaPlayer mp){if(fadeTask!=null)handler.removeCallbacks(fadeTask);final int[] n={0};fadeTask=new Runnable(){public void run(){if(player!=mp)return;n[0]++;float f=Math.min(1f,n[0]/10f);try{mp.setVolume(volume*f,volume*f);}catch(Exception ignored){}if(f<1f)handler.postDelayed(this,60);}};handler.post(fadeTask);}
    private void releasePlayer(){if(equalizer!=null){try{equalizer.release();}catch(Exception ignored){}equalizer=null;}if(enhancer!=null){try{enhancer.release();}catch(Exception ignored){}enhancer=null;}if(player!=null){try{if(Build.VERSION.SDK_INT>=23)player.setOnTimedMetaDataAvailableListener(null);}catch(Exception ignored){}try{player.reset();}catch(Exception ignored){}try{player.release();}catch(Exception ignored){}player=null;}}

    private PendingIntent svc(String action,int req){return PendingIntent.getService(this,req,new Intent(this,RadioService.class).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private Notification buildNotification(String state,boolean playing){PendingIntent content=PendingIntent.getActivity(this,1,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);b.setSmallIcon(R.drawable.ic_stat_radio).setContentTitle(stationName).setContentText(state).setContentIntent(content).setOnlyAlertOnce(true).setOngoing(playing).setCategory(Notification.CATEGORY_TRANSPORT).setVisibility(Notification.VISIBILITY_PUBLIC).addAction(new Notification.Action.Builder(android.R.drawable.ic_media_previous,"Geri",svc(ACTION_PREV,10)).build()).addAction(new Notification.Action.Builder(playing?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,playing?"Duraklat":"Oynat",svc(playing?ACTION_PAUSE:ACTION_RESUME,11)).build()).addAction(new Notification.Action.Builder(android.R.drawable.ic_media_next,"İleri",svc(ACTION_NEXT,12)).build());if(Build.VERSION.SDK_INT>=21){Notification.MediaStyle s=new Notification.MediaStyle().setShowActionsInCompactView(0,1,2);if(mediaSession!=null)s.setMediaSession(mediaSession.getSessionToken());b.setStyle(s);}return b.build();}
    private void updateNotification(String text,boolean playing){if(mediaSession!=null&&!mediaSession.isActive())mediaSession.setActive(true);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIF_ID,buildNotification(text,playing));}
    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,getString(R.string.notif_channel_name),NotificationManager.IMPORTANCE_LOW);c.setDescription(getString(R.string.notif_channel_desc));c.setShowBadge(false);c.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);}}

    @Override public void onDestroy(){userPaused=true;cancelTasks();releasePlayer();abandonFocus();if(connectivityManager!=null&&networkCallback!=null)try{connectivityManager.unregisterNetworkCallback(networkCallback);}catch(Exception ignored){}if(mediaSession!=null)try{mediaSession.release();}catch(Exception ignored){}super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
