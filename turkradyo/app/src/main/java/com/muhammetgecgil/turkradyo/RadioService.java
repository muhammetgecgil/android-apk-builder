package com.muhammetgecgil.turkradyo;

import android.app.*;
import android.content.*;
import android.media.MediaMetadata;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.*;
import android.os.*;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.PlaybackException;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import org.json.JSONArray;
import org.json.JSONObject;

/** TurkRadyo playback service - v2.8.7 freeze recovery. */
public class RadioService extends Service {
    public static final String ACTION_PLAY="com.muhammetgecgil.turkradyo.PLAY", ACTION_PAUSE="com.muhammetgecgil.turkradyo.PAUSE", ACTION_RESUME="com.muhammetgecgil.turkradyo.RESUME", ACTION_STOP="com.muhammetgecgil.turkradyo.STOP", ACTION_PREV="com.muhammetgecgil.turkradyo.PREV", ACTION_NEXT="com.muhammetgecgil.turkradyo.NEXT", ACTION_VOLUME="com.muhammetgecgil.turkradyo.VOLUME", ACTION_GAIN="com.muhammetgecgil.turkradyo.GAIN", ACTION_EQ="com.muhammetgecgil.turkradyo.EQ", ACTION_NORMALIZE="com.muhammetgecgil.turkradyo.NORMALIZE", ACTION_SMOOTH="com.muhammetgecgil.turkradyo.SMOOTH";
    private static final int NOTIF_ID=1201;
    private static final String CHANNEL="radio_playback";
    private static final long STARTUP_TIMEOUT_MS=20_000L;
    private static final long REBUFFER_TIMEOUT_MS=18_000L;
    private static final long NETWORK_SETTLE_MS=6_000L;
    private static final long STABLE_RESET_MS=15_000L;
    private static final long PROGRESS_CHECK_MS=5_000L;
    private static final long FREEZE_TIMEOUT_MS=15_000L;
    private static final long READY_STALL_TIMEOUT_MS=10_000L;
    private static final int MAX_SAME_SOURCE_RETRIES=2;

    private ExoPlayer player;
    private Player.Listener playerListener;
    private LoudnessEnhancer enhancer;
    private Equalizer equalizer;
    private MediaSession mediaSession;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Runnable reconnectTask, watchdogTask, fadeTask, networkRecoveryTask, stableTask, progressTask;

    private String stationName="Türk Radyo", primaryUrl="", streamUrl="", networkType="unknown", lastTrackTitle="";
    private boolean userPaused=false, buffering=false, smooth=true, normalize=false, repairBusy=false, recoveryBusy=false;
    private float volume=1f;
    private int gainMb=0, reconnectAttempts=0, sameSourceRetries=0, bufferCount=0, lastError=0, networkTransitions=0, repairFailures=0, engineRestarts=0, freezeRecoveries=0;
    private long playStartMs=0, startupMs=0, preparedAtMs=0, serviceStartMs=0, lastNetworkChangeMs=0, lastTrackMs=0, lastRecoveryMs=0, lastBufferStartMs=0, lastPlaybackPosition=-1, lastProgressMs=0;
    private final short[] eqLevels=new short[]{0,0,0,0,0};

    @Override public void onCreate(){
        super.onCreate();serviceStartMs=System.currentTimeMillis();createChannel();
        SharedPreferences p=getSharedPreferences("radio",MODE_PRIVATE);
        smooth=p.getBoolean("smooth",true);normalize=p.getBoolean("normalize",false);volume=p.getFloat("volume",1f);
        initMediaSession();registerNetworkMonitor();
    }

    private void registerNetworkMonitor(){
        try{
            connectivityManager=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);networkType=currentNetworkType();
            if(Build.VERSION.SDK_INT>=24){
                networkCallback=new ConnectivityManager.NetworkCallback(){
                    @Override public void onAvailable(Network network){onNetworkChanged(currentNetworkType());}
                    @Override public void onLost(Network network){onNetworkChanged(currentNetworkType());}
                    @Override public void onCapabilitiesChanged(Network network,NetworkCapabilities caps){onNetworkChanged(typeFromCaps(caps));}
                };connectivityManager.registerDefaultNetworkCallback(networkCallback);
            }
        }catch(Exception ignored){}
    }
    private String currentNetworkType(){try{Network n=connectivityManager==null?null:connectivityManager.getActiveNetwork();return typeFromCaps(n==null?null:connectivityManager.getNetworkCapabilities(n));}catch(Exception e){return"unknown";}}
    private String typeFromCaps(NetworkCapabilities c){if(c==null)return"offline";if(c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))return"wifi";if(c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))return"cellular";if(c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))return"ethernet";if(c.hasTransport(NetworkCapabilities.TRANSPORT_VPN))return"vpn";return"other";}
    private void onNetworkChanged(String next){
        if(next==null)next="unknown";if(next.equals(networkType))return;final String old=networkType, nn=next;networkType=nn;networkTransitions++;lastNetworkChangeMs=System.currentTimeMillis();saveTelemetry();
        if("offline".equals(nn)){if(!userPaused&&!primaryUrl.isEmpty()){PlaybackGuardian.interrupted(this,"network");updateMediaSession(false,"İnternet bekleniyor");updateNotification("İnternet bekleniyor",true);}return;}
        if(userPaused||primaryUrl.isEmpty())return;if(networkRecoveryTask!=null)handler.removeCallbacks(networkRecoveryTask);
        networkRecoveryTask=()->{if(userPaused||player==null)return;int state;boolean pwr;try{state=player.getPlaybackState();pwr=player.getPlayWhenReady();}catch(Exception e){state=Player.STATE_IDLE;pwr=false;}if(state==Player.STATE_IDLE||state==Player.STATE_ENDED||!pwr){sameSourceRetries=0;schedulePlay(streamUrl.isEmpty()?StreamFallbackManager.getPreferred(this,stationName,primaryUrl):streamUrl,250);}else{updateNotification("Ağ geçişi: "+old+" → "+nn,true);saveTelemetry();}};
        handler.postDelayed(networkRecoveryTask,NETWORK_SETTLE_MS);
    }

    private void initMediaSession(){
        mediaSession=new MediaSession(this,"TurkRadyoSession");mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS|MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSession.Callback(){@Override public void onPlay(){resume();}@Override public void onPause(){pause(true);}@Override public void onStop(){stopAll();}@Override public void onSkipToNext(){stepQueue(1);}@Override public void onSkipToPrevious(){stepQueue(-1);}@Override public void onPlayFromMediaId(String mediaId,Bundle extras){playFromMediaId(mediaId);}});
        mediaSession.setActive(true);updateMediaSession(false,"Hazır");
    }

    @Override public int onStartCommand(Intent in,int flags,int id){
        if(in==null)return START_STICKY;String a=in.getAction();
        if(ACTION_PLAY.equals(a)){
            String u=in.getStringExtra("url"),n=in.getStringExtra("name");if(u!=null&&!u.isEmpty()){
                if(!userPaused&&u.equals(primaryUrl)&&player!=null){try{int st=player.getPlaybackState();if(st!=Player.STATE_IDLE&&st!=Player.STATE_ENDED){if(player.isPlaying()){return START_STICKY;}if(player.getPlayWhenReady()&&player.getPlaybackSuppressionReason()==Player.PLAYBACK_SUPPRESSION_REASON_NONE&&System.currentTimeMillis()-lastProgressMs>READY_STALL_TIMEOUT_MS){lastError=-34;handleFailure("Oynatma dondu");return START_STICKY;}player.play();armProgressWatchdog();return START_STICKY;}}catch(Exception ignored){}}
                PlaybackGuardian.manualPlay(this);primaryUrl=u;stationName=(n==null||n.isEmpty())?"Türk Radyo":n;lastTrackTitle="";lastTrackMs=0;getSharedPreferences("radio",MODE_PRIVATE).edit().putString("nowTitle","").apply();reconnectAttempts=0;sameSourceRetries=0;repairBusy=false;userPaused=false;syncQueueIndexForUrl(u);playResolved(StreamFallbackManager.getPreferred(this,stationName,primaryUrl));
            }
        }else if(ACTION_PREV.equals(a))stepQueue(-1);else if(ACTION_NEXT.equals(a))stepQueue(1);else if(ACTION_PAUSE.equals(a))pause(true);else if(ACTION_RESUME.equals(a))resume();else if(ACTION_STOP.equals(a))stopAll();
        else if(ACTION_VOLUME.equals(a)){volume=Math.max(0f,Math.min(1f,in.getFloatExtra("volume",1f)));getSharedPreferences("radio",MODE_PRIVATE).edit().putFloat("volume",volume).apply();if(player!=null)try{player.setVolume(volume);}catch(Exception ignored){}}
        else if(ACTION_GAIN.equals(a)){gainMb=in.getIntExtra("gain",0);applyGain();}
        else if(ACTION_EQ.equals(a)){int b=in.getIntExtra("band",0),l=in.getIntExtra("level",0);if(b>=0&&b<eqLevels.length){eqLevels[b]=(short)Math.max(-1500,Math.min(1500,l));applyEq();}}
        else if(ACTION_NORMALIZE.equals(a)){normalize=in.getBooleanExtra("on",false);getSharedPreferences("radio",MODE_PRIVATE).edit().putBoolean("normalize",normalize).apply();applyGain();}
        else if(ACTION_SMOOTH.equals(a)){smooth=in.getBooleanExtra("on",true);getSharedPreferences("radio",MODE_PRIVATE).edit().putBoolean("smooth",smooth).apply();}
        return START_STICKY;
    }

    private void playFromMediaId(String id){if(id==null)return;try{if(id.startsWith("q:")){playQueueIndex(Integer.parseInt(id.substring(2)));return;}if(id.startsWith("recent:")){int i=Integer.parseInt(id.substring(7));JSONArray a=new JSONArray(getSharedPreferences("radio",MODE_PRIVATE).getString("recentStations","[]"));JSONObject o=a.optJSONObject(i);if(o!=null)startStation(o.optString("url"),o.optString("name","Türk Radyo"));}}catch(Exception ignored){}}
    private void startStation(String u,String n){if(u==null||u.isEmpty())return;PlaybackGuardian.manualPlay(this);primaryUrl=u;stationName=(n==null||n.isEmpty())?"Türk Radyo":n;lastTrackTitle="";lastTrackMs=0;getSharedPreferences("radio",MODE_PRIVATE).edit().putString("nowTitle","").apply();reconnectAttempts=0;sameSourceRetries=0;repairBusy=false;userPaused=false;playResolved(StreamFallbackManager.getPreferred(this,stationName,primaryUrl));}

    private ExoPlayer buildPlayer(){
        DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setUserAgent("TurkRadyo/2.8.7").setConnectTimeoutMs(8_000).setReadTimeoutMs(12_000).setAllowCrossProtocolRedirects(true);
        DefaultMediaSourceFactory msf=new DefaultMediaSourceFactory(http);
        DefaultLoadControl lc=new DefaultLoadControl.Builder().setBufferDurationsMs(10_000,45_000,1_000,2_500).setPrioritizeTimeOverSizeThresholds(true).build();
        ExoPlayer ep=new ExoPlayer.Builder(this).setMediaSourceFactory(msf).setLoadControl(lc).build();
        androidx.media3.common.AudioAttributes attrs=new androidx.media3.common.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build();
        ep.setAudioAttributes(attrs,true);ep.setHandleAudioBecomingNoisy(true);ep.setWakeMode(C.WAKE_MODE_NETWORK);return ep;
    }

    private void playResolved(String url){
        if(url==null||url.isEmpty())url=primaryUrl;streamUrl=url;playStartMs=System.currentTimeMillis();startupMs=0;preparedAtMs=0;buffering=false;lastError=0;lastBufferStartMs=0;recoveryBusy=false;lastPlaybackPosition=-1;lastProgressMs=System.currentTimeMillis();
        cancelTasks();releasePlayer();saveTelemetry();saveCurrent();updateMediaSession(false,"Bağlanıyor…");startForeground(NOTIF_ID,buildNotification("Bağlanıyor…",true));
        try{
            final ExoPlayer ep=buildPlayer();player=ep;
            playerListener=new Player.Listener(){
                @Override public void onPlaybackStateChanged(int state){if(ep!=player)return;if(state==Player.STATE_BUFFERING){if(!buffering){bufferCount++;lastBufferStartMs=System.currentTimeMillis();}buffering=true;saveTelemetry();armBufferWatchdog();updateMediaSession(false,preparedAtMs>0?"Yayın tamponlanıyor…":"Bağlanıyor…");}else if(state==Player.STATE_READY){buffering=false;lastBufferStartMs=0;cancelWatchdog();if(startupMs<=0)startupMs=Math.max(1,System.currentTimeMillis()-playStartMs);if(preparedAtMs<=0)preparedAtMs=System.currentTimeMillis();StreamFallbackManager.markGood(RadioService.this,stationName,streamUrl,startupMs);saveRecent();saveCurrent();saveTelemetry();if(!userPaused)ep.play();armProgressWatchdog();}else if(state==Player.STATE_ENDED&&!userPaused){lastError=-43;saveTelemetry();handleFailure("Yayın sona erdi");}}
                @Override public void onIsPlayingChanged(boolean isPlaying){if(ep!=player)return;if(isPlaying){buffering=false;lastBufferStartMs=0;cancelWatchdog();lastPlaybackPosition=safePosition(ep);lastProgressMs=System.currentTimeMillis();updateMediaSession(true,"Canlı yayın");updateNotification("Canlı yayın",true);armStableReset();armProgressWatchdog();}else if(!userPaused&&ep.getPlaybackState()==Player.STATE_READY){updateMediaSession(false,"Ses odağı bekleniyor");armProgressWatchdog();}}
                @Override public void onPlayerError(PlaybackException error){if(ep!=player)return;lastError=error==null?-40:error.errorCode;buffering=false;saveTelemetry();handleFailure("Bağlantı kesildi");}
                @Override public void onMediaMetadataChanged(androidx.media3.common.MediaMetadata metadata){if(ep!=player||metadata==null)return;String t=metadata.title==null?"":metadata.title.toString(),ar=metadata.artist==null?"":metadata.artist.toString();if(!ar.isEmpty()&&!t.isEmpty()&&!t.toLowerCase().contains(ar.toLowerCase()))t=ar+" - "+t;if(!t.isEmpty()&&!t.equalsIgnoreCase(stationName))recordTrack(t,"MEDIA3");}
                @Override public void onAudioSessionIdChanged(int id){if(ep!=player)return;applyGain();applyEq();}
            };
            ep.addListener(playerListener);ep.setMediaItem(MediaItem.fromUri(Uri.parse(streamUrl)));ep.setVolume(smooth?0f:volume);ep.setPlayWhenReady(true);ep.prepare();if(smooth)fadeIn(ep);armStartupWatchdog(streamUrl);
        }catch(Exception e){lastError=-1;saveTelemetry();handleFailure("Açılamadı");}
    }

    private long safePosition(ExoPlayer ep){try{return ep==null?-1:ep.getCurrentPosition();}catch(Exception e){return-1;}}
    private void armProgressWatchdog(){
        cancelProgressWatchdog();if(userPaused||player==null)return;if(lastProgressMs<=0)lastProgressMs=System.currentTimeMillis();
        progressTask=new Runnable(){@Override public void run(){if(userPaused||player==null){progressTask=null;return;}try{long now=System.currentTimeMillis();int state=player.getPlaybackState();boolean pwr=player.getPlayWhenReady(),playing=player.isPlaying();int suppression=player.getPlaybackSuppressionReason();long pos=player.getCurrentPosition();if(state==Player.STATE_READY&&pwr){if(playing){if(lastPlaybackPosition<0||Math.abs(pos-lastPlaybackPosition)>=250){lastPlaybackPosition=pos;lastProgressMs=now;}else if(now-lastProgressMs>=FREEZE_TIMEOUT_MS&&!"offline".equals(networkType)){freezeRecoveries++;lastError=-32;saveTelemetry();handleFailure("Yayın dondu, yeniden bağlanıyor");progressTask=null;return;}}else if(suppression==Player.PLAYBACK_SUPPRESSION_REASON_NONE){if(now-lastProgressMs>=READY_STALL_TIMEOUT_MS&&!"offline".equals(networkType)){freezeRecoveries++;lastError=-33;saveTelemetry();handleFailure("Oynatma takıldı, yeniden bağlanıyor");progressTask=null;return;}}else{lastProgressMs=now;lastPlaybackPosition=pos;}}else if(state!=Player.STATE_BUFFERING){lastProgressMs=now;lastPlaybackPosition=pos;}saveTelemetry();}catch(Exception ignored){}if(progressTask!=null)handler.postDelayed(this,PROGRESS_CHECK_MS);}};
        handler.postDelayed(progressTask,PROGRESS_CHECK_MS);
    }

    private String cleanTrackTitle(String s){String x=s==null?"":s.replaceAll("\\s+"," ").trim();x=x.replaceAll("^[\\-–—|•\\s]+|[\\-–—|•\\s]+$","").trim();if(x.length()>240)x=x.substring(0,240).trim();if(x.length()<2||x.equalsIgnoreCase("unknown")||x.equalsIgnoreCase("null")||x.equals("-"))return"";return x;}
    private void recordTrack(String title,String source){title=cleanTrackTitle(title);if(title.isEmpty())return;long now=System.currentTimeMillis();if(title.equalsIgnoreCase(lastTrackTitle)&&now-lastTrackMs<90_000L)return;lastTrackTitle=title;lastTrackMs=now;try{SharedPreferences p=getSharedPreferences("radio",MODE_PRIVATE);JSONArray old;try{old=new JSONArray(p.getString("tracks","[]"));}catch(Exception e){old=new JSONArray();}JSONArray out=new JSONArray();JSONObject n=new JSONObject();n.put("title",title);n.put("station",stationName);n.put("time",now);n.put("source",source);out.put(n);for(int i=0;i<old.length()&&out.length()<50;i++){JSONObject x=old.optJSONObject(i);if(x!=null)out.put(x);}p.edit().putString("tracks",out.toString()).putString("nowTitle",title).apply();updateMediaSession(player!=null&&player.isPlaying(),title);}catch(Exception ignored){}}

    private void handleFailure(String label){
        if(userPaused)return;long now=System.currentTimeMillis();if(recoveryBusy&&now-lastRecoveryMs<1200L)return;recoveryBusy=true;lastRecoveryMs=now;cancelWatchdog();cancelStableReset();cancelProgressWatchdog();updateMediaSession(false,label);updateNotification(label,true);if("offline".equals(networkType)){recoveryBusy=false;return;}
        if(sameSourceRetries<MAX_SAME_SOURCE_RETRIES){sameSourceRetries++;engineRestarts++;long delay=sameSourceRetries==1?700L:1600L;schedulePlay(streamUrl.isEmpty()?primaryUrl:streamUrl,delay);return;}
        StreamFallbackManager.markBad(this,stationName,streamUrl,5*60_000L);if(reconnectAttempts<1){reconnectAttempts++;String next=StreamFallbackManager.getPreferred(this,stationName,primaryUrl);schedulePlay(next,1000L);return;}repairSameStation();
    }
    private void repairSameStation(){if(repairBusy||userPaused)return;repairBusy=true;updateNotification("Aynı radyo için sağlam kaynak aranıyor…",true);StreamFallbackManager.discoverBestAsync(this,stationName,primaryUrl,u->{repairBusy=false;recoveryBusy=false;if(userPaused)return;if(u!=null&&!u.isEmpty()){reconnectAttempts=0;sameSourceRetries=0;playResolved(u);}else{repairFailures++;saveTelemetry();sameSourceRetries=0;schedulePlay(primaryUrl,3000L);}});}
    private void schedulePlay(String u,long delay){cancelReconnect();reconnectTask=()->{recoveryBusy=false;if(!userPaused)playResolved(u);};handler.postDelayed(reconnectTask,delay);}

    private void armStartupWatchdog(final String expected){cancelWatchdog();watchdogTask=()->{if(userPaused||player==null||!expected.equals(streamUrl))return;int state;try{state=player.getPlaybackState();}catch(Exception e){state=Player.STATE_IDLE;}if(state!=Player.STATE_READY){lastError=-31;saveTelemetry();handleFailure("Geç bağlantı");}};handler.postDelayed(watchdogTask,STARTUP_TIMEOUT_MS);}
    private void armBufferWatchdog(){cancelWatchdog();watchdogTask=()->{if(userPaused||player==null)return;int state;try{state=player.getPlaybackState();}catch(Exception e){state=Player.STATE_IDLE;}if(buffering&&state==Player.STATE_BUFFERING){lastError=-30;saveTelemetry();handleFailure("Uzun buffer");}};handler.postDelayed(watchdogTask,REBUFFER_TIMEOUT_MS);}
    private void armStableReset(){cancelStableReset();stableTask=()->{if(userPaused||player==null)return;try{if(player.isPlaying()){sameSourceRetries=0;reconnectAttempts=0;recoveryBusy=false;if(PlaybackGuardian.mayAutoResume(this))PlaybackGuardian.recovered(this);saveTelemetry();}}catch(Exception ignored){}};handler.postDelayed(stableTask,STABLE_RESET_MS);}
    private void cancelStableReset(){if(stableTask!=null){handler.removeCallbacks(stableTask);stableTask=null;}}
    private void cancelWatchdog(){if(watchdogTask!=null){handler.removeCallbacks(watchdogTask);watchdogTask=null;}}
    private void cancelReconnect(){if(reconnectTask!=null){handler.removeCallbacks(reconnectTask);reconnectTask=null;}}
    private void cancelProgressWatchdog(){if(progressTask!=null){handler.removeCallbacks(progressTask);progressTask=null;}}
    private void cancelTasks(){cancelReconnect();cancelWatchdog();cancelStableReset();cancelProgressWatchdog();if(networkRecoveryTask!=null){handler.removeCallbacks(networkRecoveryTask);networkRecoveryTask=null;}if(fadeTask!=null){handler.removeCallbacks(fadeTask);fadeTask=null;}}

    private void syncQueueIndexForUrl(String url){try{SharedPreferences p=getSharedPreferences("radio",MODE_PRIVATE);JSONArray a=new JSONArray(p.getString("queue","[]"));for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&url.equals(o.optString("url"))){p.edit().putInt("queueIndex",i).apply();break;}}}catch(Exception ignored){}}
    private void stepQueue(int d){try{SharedPreferences p=getSharedPreferences("radio",MODE_PRIVATE);JSONArray q=new JSONArray(p.getString("queue","[]"));if(q.length()==0)return;int i=p.getInt("queueIndex",0);i=(i+d)%q.length();if(i<0)i+=q.length();playQueueIndex(i);}catch(Exception ignored){}}
    private void playQueueIndex(int i){try{SharedPreferences p=getSharedPreferences("radio",MODE_PRIVATE);JSONArray q=new JSONArray(p.getString("queue","[]"));JSONObject o=q.optJSONObject(i);if(o==null)return;String u=o.optString("url"),n=o.optString("name","Türk Radyo");if(u.isEmpty())return;p.edit().putInt("queueIndex",i).apply();startStation(u,n);}catch(Exception ignored){}}
    private void saveCurrent(){getSharedPreferences("radio",MODE_PRIVATE).edit().putString("url",primaryUrl).putString("resolvedUrl",streamUrl).putString("name",stationName).apply();}
    private void saveRecent(){try{SharedPreferences p=getSharedPreferences("radio",MODE_PRIVATE);JSONArray old;try{old=new JSONArray(p.getString("recentStations","[]"));}catch(Exception e){old=new JSONArray();}JSONArray out=new JSONArray();JSONObject n=new JSONObject();n.put("name",stationName);n.put("url",primaryUrl);out.put(n);for(int i=0;i<old.length()&&out.length()<20;i++){JSONObject x=old.optJSONObject(i);if(x!=null&&!primaryUrl.equals(x.optString("url")))out.put(x);}p.edit().putString("recentStations",out.toString()).apply();}catch(Exception ignored){}}

    private void saveTelemetry(){try{JSONObject o=new JSONObject();o.put("engine","media3-exoplayer-1.11.0");o.put("startupMs",startupMs);o.put("bufferCount",bufferCount);o.put("lastError",lastError);o.put("since",playStartMs);o.put("buffering",buffering);o.put("reconnectAttempts",reconnectAttempts);o.put("sameSourceRetries",sameSourceRetries);o.put("engineRestarts",engineRestarts);o.put("freezeRecoveries",freezeRecoveries);o.put("lastProgressMs",lastProgressMs);o.put("resolvedUrl",streamUrl);o.put("networkType",networkType);o.put("networkTransitions",networkTransitions);o.put("lastNetworkChangeMs",lastNetworkChangeMs);o.put("repairFailures",repairFailures);o.put("serviceUptimeMs",Math.max(0,System.currentTimeMillis()-serviceStartMs));o.put("liveMs",preparedAtMs>0?Math.max(0,System.currentTimeMillis()-preparedAtMs):0);o.put("playbackGuardian",true);o.put("guardianReason",PlaybackGuardian.reason(this));if(player!=null){try{o.put("playerState",player.getPlaybackState());o.put("isPlaying",player.isPlaying());o.put("playWhenReady",player.getPlayWhenReady());o.put("positionMs",player.getCurrentPosition());o.put("suppression",player.getPlaybackSuppressionReason());}catch(Exception ignored){}}getSharedPreferences("radio",MODE_PRIVATE).edit().putString("telemetry",o.toString()).apply();}catch(Exception ignored){}}

    private void updateMediaSession(boolean playing,String subtitle){if(mediaSession==null)return;long acts=PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_PLAY_PAUSE|PlaybackState.ACTION_STOP|PlaybackState.ACTION_SKIP_TO_NEXT|PlaybackState.ACTION_SKIP_TO_PREVIOUS|PlaybackState.ACTION_PLAY_FROM_MEDIA_ID;int state=playing?PlaybackState.STATE_PLAYING:(userPaused?PlaybackState.STATE_PAUSED:PlaybackState.STATE_CONNECTING);mediaSession.setPlaybackState(new PlaybackState.Builder().setActions(acts).setState(state,PlaybackState.PLAYBACK_POSITION_UNKNOWN,playing?1f:0f).build());String now=getSharedPreferences("radio",MODE_PRIVATE).getString("nowTitle","");mediaSession.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,stationName).putString(MediaMetadata.METADATA_KEY_ARTIST,now.isEmpty()?subtitle:now).putString(MediaMetadata.METADATA_KEY_ALBUM,"Türk Radyo").build());}
    private void pause(boolean manual){if(manual){userPaused=true;PlaybackGuardian.manualPause(this);}cancelTasks();if(player!=null)try{player.pause();}catch(Exception ignored){}updateMediaSession(false,manual?"Duraklatıldı":"Geçici olarak duraklatıldı");updateNotification(manual?"Duraklatıldı":"Geçici olarak duraklatıldı",false);}
    private void resume(){PlaybackGuardian.manualPlay(this);userPaused=false;if(player!=null){try{int state=player.getPlaybackState();if(state==Player.STATE_IDLE||state==Player.STATE_ENDED){playResolved(streamUrl.isEmpty()?StreamFallbackManager.getPreferred(this,stationName,primaryUrl):streamUrl);return;}lastProgressMs=System.currentTimeMillis();lastPlaybackPosition=safePosition(player);player.play();armProgressWatchdog();updateMediaSession(true,"Canlı yayın");updateNotification("Canlı yayın",true);}catch(Exception e){playResolved(StreamFallbackManager.getPreferred(this,stationName,primaryUrl));}}else if(!primaryUrl.isEmpty())playResolved(StreamFallbackManager.getPreferred(this,stationName,primaryUrl));}
    private void stopAll(){userPaused=true;PlaybackGuardian.manualPause(this);cancelTasks();releasePlayer();if(mediaSession!=null)mediaSession.setActive(false);stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}

    private int audioSessionId(){try{return player==null?0:player.getAudioSessionId();}catch(Exception e){return 0;}}
    private void applyGain(){int sid=audioSessionId();if(sid<=0)return;try{if(enhancer!=null)enhancer.release();enhancer=new LoudnessEnhancer(sid);int t=normalize?Math.max(300,gainMb):gainMb;t=Math.max(0,Math.min(1200,t));enhancer.setTargetGain(t);enhancer.setEnabled(t>0);}catch(Exception ignored){}}
    private void applyEq(){int sid=audioSessionId();if(sid<=0)return;try{if(equalizer!=null)equalizer.release();equalizer=new Equalizer(0,sid);short bands=equalizer.getNumberOfBands();short[] range=equalizer.getBandLevelRange();for(short b=0;b<bands&&b<eqLevels.length;b++)equalizer.setBandLevel(b,(short)Math.max(range[0],Math.min(range[1],eqLevels[b])));equalizer.setEnabled(true);}catch(Exception ignored){}}
    private void fadeIn(ExoPlayer ep){if(fadeTask!=null)handler.removeCallbacks(fadeTask);final int[] n={0};fadeTask=new Runnable(){@Override public void run(){if(player!=ep)return;n[0]++;float f=Math.min(1f,n[0]/10f);try{ep.setVolume(volume*f);}catch(Exception ignored){}if(f<1f)handler.postDelayed(this,60);}};handler.post(fadeTask);}
    private void releasePlayer(){cancelProgressWatchdog();if(equalizer!=null){try{equalizer.release();}catch(Exception ignored){}equalizer=null;}if(enhancer!=null){try{enhancer.release();}catch(Exception ignored){}enhancer=null;}if(player!=null){try{if(playerListener!=null)player.removeListener(playerListener);}catch(Exception ignored){}try{player.release();}catch(Exception ignored){}player=null;playerListener=null;}}

    private PendingIntent svc(String action,int req){return PendingIntent.getService(this,req,new Intent(this,RadioService.class).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private Notification buildNotification(String state,boolean playing){PendingIntent content=PendingIntent.getActivity(this,1,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);b.setSmallIcon(R.drawable.ic_stat_radio).setContentTitle(stationName).setContentText(state).setContentIntent(content).setOnlyAlertOnce(true).setOngoing(playing).setCategory(Notification.CATEGORY_TRANSPORT).setVisibility(Notification.VISIBILITY_PUBLIC).addAction(new Notification.Action.Builder(android.R.drawable.ic_media_previous,"Geri",svc(ACTION_PREV,10)).build()).addAction(new Notification.Action.Builder(playing?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,playing?"Duraklat":"Oynat",svc(playing?ACTION_PAUSE:ACTION_RESUME,11)).build()).addAction(new Notification.Action.Builder(android.R.drawable.ic_media_next,"İleri",svc(ACTION_NEXT,12)).build());if(Build.VERSION.SDK_INT>=21){Notification.MediaStyle s=new Notification.MediaStyle().setShowActionsInCompactView(0,1,2);if(mediaSession!=null)s.setMediaSession(mediaSession.getSessionToken());b.setStyle(s);}return b.build();}
    private void updateNotification(String text,boolean playing){if(mediaSession!=null&&!mediaSession.isActive())mediaSession.setActive(true);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIF_ID,buildNotification(text,playing));}
    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,getString(R.string.notif_channel_name),NotificationManager.IMPORTANCE_LOW);c.setDescription(getString(R.string.notif_channel_desc));c.setShowBadge(false);c.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);}}

    @Override public void onDestroy(){userPaused=true;cancelTasks();releasePlayer();if(connectivityManager!=null&&networkCallback!=null)try{connectivityManager.unregisterNetworkCallback(networkCallback);}catch(Exception ignored){}if(mediaSession!=null){try{mediaSession.release();}catch(Exception ignored){}}super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
