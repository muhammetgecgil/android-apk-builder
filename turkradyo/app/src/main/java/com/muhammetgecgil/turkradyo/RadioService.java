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

/**
 * TurkRadyo v2.8.7 zero-jank playback architecture.
 * Player work runs on a dedicated looper; rapid channel taps are coalesced;
 * old audio stays alive until the replacement stream is READY.
 */
public class RadioService extends Service {
    public static final String ACTION_PLAY="com.muhammetgecgil.turkradyo.PLAY", ACTION_PAUSE="com.muhammetgecgil.turkradyo.PAUSE", ACTION_RESUME="com.muhammetgecgil.turkradyo.RESUME", ACTION_STOP="com.muhammetgecgil.turkradyo.STOP", ACTION_PREV="com.muhammetgecgil.turkradyo.PREV", ACTION_NEXT="com.muhammetgecgil.turkradyo.NEXT", ACTION_VOLUME="com.muhammetgecgil.turkradyo.VOLUME", ACTION_GAIN="com.muhammetgecgil.turkradyo.GAIN", ACTION_EQ="com.muhammetgecgil.turkradyo.EQ", ACTION_NORMALIZE="com.muhammetgecgil.turkradyo.NORMALIZE", ACTION_SMOOTH="com.muhammetgecgil.turkradyo.SMOOTH";

    private static final int NOTIF_ID=1201;
    private static final String CHANNEL="radio_playback";
    private static final long PLAY_DEBOUNCE_MS=110L;
    private static final long STARTUP_TIMEOUT_MS=18_000L;
    private static final long REBUFFER_TIMEOUT_MS=15_000L;
    private static final long PROGRESS_CHECK_MS=4_000L;
    private static final long FREEZE_TIMEOUT_MS=12_000L;
    private static final long STABLE_RESET_MS=15_000L;
    private static final int MAX_SAME_SOURCE_RETRIES=2;

    private HandlerThread playbackThread;
    private Handler handler;
    private final Handler mainHandler=new Handler(Looper.getMainLooper());

    private ExoPlayer player;
    private ExoPlayer candidate;
    private Player.Listener playerListener;
    private Player.Listener candidateListener;
    private LoudnessEnhancer enhancer;
    private Equalizer equalizer;
    private MediaSession mediaSession;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private Runnable pendingPlayTask, watchdogTask, progressTask, stableTask, fadeTask, reconnectTask;
    private volatile String pendingUrl="", pendingName="Türk Radyo";
    private volatile long playGeneration=0;

    private String stationName="Türk Radyo", primaryUrl="", streamUrl="", networkType="unknown", lastTrackTitle="";
    private boolean userPaused=false, buffering=false, smooth=true, normalize=false, recoveryBusy=false;
    private float volume=1f;
    private int gainMb=0, sameSourceRetries=0, reconnectAttempts=0, bufferCount=0, engineRestarts=0, freezeRecoveries=0, lastError=0;
    private long serviceStartMs=0, playStartMs=0, preparedAtMs=0, startupMs=0, lastTrackMs=0, lastProgressMs=0, lastPosition=-1;
    private final short[] eqLevels=new short[]{0,0,0,0,0};

    @Override public void onCreate(){
        super.onCreate();
        serviceStartMs=System.currentTimeMillis();
        createChannel();
        playbackThread=new HandlerThread("TurkRadyoPlayback",Process.THREAD_PRIORITY_AUDIO);
        playbackThread.start();
        handler=new Handler(playbackThread.getLooper());
        SharedPreferences p=getSharedPreferences("radio",MODE_PRIVATE);
        smooth=p.getBoolean("smooth",true);normalize=p.getBoolean("normalize",false);volume=p.getFloat("volume",1f);
        initMediaSession();
        registerNetworkMonitor();
    }

    private void initMediaSession(){
        mediaSession=new MediaSession(this,"TurkRadyoSession");
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS|MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSession.Callback(){
            @Override public void onPlay(){post(RadioService.this::resumeInternal);}
            @Override public void onPause(){post(()->pauseInternal(true));}
            @Override public void onStop(){post(RadioService.this::stopInternal);}
            @Override public void onSkipToNext(){post(()->stepQueue(1));}
            @Override public void onSkipToPrevious(){post(()->stepQueue(-1));}
            @Override public void onPlayFromMediaId(String mediaId,Bundle extras){post(()->playFromMediaId(mediaId));}
        });
        mediaSession.setActive(true);
        updateMediaSession(false,"Hazır");
    }

    private void registerNetworkMonitor(){
        try{
            connectivityManager=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            networkType=currentNetworkType();
            if(Build.VERSION.SDK_INT>=24){
                networkCallback=new ConnectivityManager.NetworkCallback(){
                    @Override public void onAvailable(Network n){post(()->networkChanged(currentNetworkType()));}
                    @Override public void onLost(Network n){post(()->networkChanged(currentNetworkType()));}
                    @Override public void onCapabilitiesChanged(Network n,NetworkCapabilities c){String t=typeFromCaps(c);post(()->networkChanged(t));}
                };
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            }
        }catch(Exception ignored){}
    }

    private String currentNetworkType(){try{Network n=connectivityManager==null?null:connectivityManager.getActiveNetwork();return typeFromCaps(n==null?null:connectivityManager.getNetworkCapabilities(n));}catch(Exception e){return"unknown";}}
    private String typeFromCaps(NetworkCapabilities c){if(c==null)return"offline";if(c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))return"wifi";if(c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))return"cellular";if(c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))return"ethernet";if(c.hasTransport(NetworkCapabilities.TRANSPORT_VPN))return"vpn";return"other";}
    private void networkChanged(String next){
        if(next==null)next="unknown";if(next.equals(networkType))return;networkType=next;saveTelemetry();
        if("offline".equals(next)){PlaybackGuardian.interrupted(this,"network");updateMediaSession(false,"İnternet bekleniyor");updateNotification("İnternet bekleniyor",player!=null&&!userPaused);return;}
        if(!userPaused&&player!=null){try{int s=player.getPlaybackState();if(s==Player.STATE_IDLE||s==Player.STATE_ENDED)recover("Ağ geri geldi");}catch(Exception ignored){}}
    }

    @Override public int onStartCommand(Intent in,int flags,int id){
        if(in==null)return START_STICKY;
        String a=in.getAction();
        if(ACTION_PLAY.equals(a)){
            String u=in.getStringExtra("url"),n=in.getStringExtra("name");
            if(u!=null&&!u.isEmpty()){
                startForeground(NOTIF_ID,buildNotification("Hazırlanıyor…",true));
                pendingUrl=u;pendingName=(n==null||n.isEmpty())?"Türk Radyo":n;
                long gen=++playGeneration;
                post(()->{
                    if(pendingPlayTask!=null)handler.removeCallbacks(pendingPlayTask);
                    pendingPlayTask=()->{if(gen==playGeneration)requestStation(pendingUrl,pendingName);};
                    handler.postDelayed(pendingPlayTask,PLAY_DEBOUNCE_MS);
                });
            }
            return START_STICKY;
        }
        post(()->handleAction(in));
        return START_STICKY;
    }

    private void handleAction(Intent in){
        String a=in.getAction();
        if(ACTION_PREV.equals(a))stepQueue(-1);
        else if(ACTION_NEXT.equals(a))stepQueue(1);
        else if(ACTION_PAUSE.equals(a))pauseInternal(true);
        else if(ACTION_RESUME.equals(a))resumeInternal();
        else if(ACTION_STOP.equals(a))stopInternal();
        else if(ACTION_VOLUME.equals(a)){volume=Math.max(0f,Math.min(1f,in.getFloatExtra("volume",1f)));getSharedPreferences("radio",MODE_PRIVATE).edit().putFloat("volume",volume).apply();if(player!=null)try{player.setVolume(volume);}catch(Exception ignored){}}
        else if(ACTION_GAIN.equals(a)){gainMb=in.getIntExtra("gain",0);applyGain();}
        else if(ACTION_EQ.equals(a)){int b=in.getIntExtra("band",0),l=in.getIntExtra("level",0);if(b>=0&&b<eqLevels.length){eqLevels[b]=(short)Math.max(-1500,Math.min(1500,l));applyEq();}}
        else if(ACTION_NORMALIZE.equals(a)){normalize=in.getBooleanExtra("on",false);getSharedPreferences("radio",MODE_PRIVATE).edit().putBoolean("normalize",normalize).apply();applyGain();}
        else if(ACTION_SMOOTH.equals(a)){smooth=in.getBooleanExtra("on",true);getSharedPreferences("radio",MODE_PRIVATE).edit().putBoolean("smooth",smooth).apply();}
    }

    private void requestStation(String url,String name){
        if(url==null||url.isEmpty())return;
        if(!userPaused&&url.equals(primaryUrl)&&player!=null){
            try{if(player.getPlaybackState()!=Player.STATE_IDLE&&player.getPlaybackState()!=Player.STATE_ENDED){player.play();armProgressWatchdog();return;}}catch(Exception ignored){}
        }
        PlaybackGuardian.manualPlay(this);
        userPaused=false;recoveryBusy=false;sameSourceRetries=0;reconnectAttempts=0;
        primaryUrl=url;stationName=name;lastTrackTitle="";lastTrackMs=0;
        getSharedPreferences("radio",MODE_PRIVATE).edit().putString("nowTitle","").apply();
        syncQueueIndexForUrl(url);
        String resolved=StreamFallbackManager.getPreferred(this,stationName,primaryUrl);
        seamlessSwitch(resolved,playGeneration);
    }

    private ExoPlayer newPlayer(){
        DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setUserAgent("TurkRadyo/2.8.7").setConnectTimeoutMs(7_000).setReadTimeoutMs(11_000).setAllowCrossProtocolRedirects(true);
        DefaultMediaSourceFactory msf=new DefaultMediaSourceFactory(http);
        DefaultLoadControl lc=new DefaultLoadControl.Builder().setBufferDurationsMs(8_000,40_000,800,1800).setPrioritizeTimeOverSizeThresholds(true).build();
        ExoPlayer ep=new ExoPlayer.Builder(this).setLooper(playbackThread.getLooper()).setMediaSourceFactory(msf).setLoadControl(lc).build();
        androidx.media3.common.AudioAttributes attrs=new androidx.media3.common.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build();
        ep.setAudioAttributes(attrs,true);ep.setHandleAudioBecomingNoisy(true);ep.setWakeMode(C.WAKE_MODE_NETWORK);return ep;
    }

    private void seamlessSwitch(String url,long generation){
        if(url==null||url.isEmpty())url=primaryUrl;
        final String target=url;
        releaseCandidate();
        playStartMs=System.currentTimeMillis();startupMs=0;
        updateMediaSession(player!=null&&player.isPlaying(),"Yeni kanal hazırlanıyor…");
        updateNotification("Yeni kanal hazırlanıyor…",true);
        final ExoPlayer next=newPlayer();candidate=next;
        candidateListener=new Player.Listener(){
            @Override public void onPlaybackStateChanged(int state){
                if(candidate!=next||generation!=playGeneration)return;
                if(state==Player.STATE_READY){handoff(next,target,generation);}
                else if(state==Player.STATE_BUFFERING){bufferCount++;}
                else if(state==Player.STATE_ENDED){candidateFailed(next,target,generation,-43);}
            }
            @Override public void onPlayerError(PlaybackException e){candidateFailed(next,target,generation,e==null?-40:e.errorCode);}
            @Override public void onMediaMetadataChanged(androidx.media3.common.MediaMetadata m){if(candidate==next)recordMetadata(m);}
        };
        next.addListener(candidateListener);
        next.setMediaItem(MediaItem.fromUri(Uri.parse(target)));
        next.setVolume(0f);
        next.setPlayWhenReady(true);
        next.prepare();
        armCandidateTimeout(next,target,generation);
    }

    private void handoff(ExoPlayer next,String target,long generation){
        if(candidate!=next||generation!=playGeneration)return;
        cancelWatchdog();
        ExoPlayer old=player;Player.Listener oldListener=playerListener;
        candidate=null;candidateListener=null;
        player=next;streamUrl=target;preparedAtMs=System.currentTimeMillis();startupMs=Math.max(1,preparedAtMs-playStartMs);buffering=false;lastPosition=safePosition(next);lastProgressMs=System.currentTimeMillis();
        playerListener=activeListener(next,generation);
        next.addListener(playerListener);
        saveCurrent();saveRecent();StreamFallbackManager.markGood(this,stationName,target,startupMs);saveTelemetry();
        applyGain();applyEq();
        crossfade(old,oldListener,next);
        updateMediaSession(true,"Canlı yayın");updateNotification("Canlı yayın",true);armProgressWatchdog();armStableReset();
    }

    private Player.Listener activeListener(final ExoPlayer ep,final long generation){
        return new Player.Listener(){
            @Override public void onPlaybackStateChanged(int state){
                if(player!=ep||generation!=playGeneration)return;
                if(state==Player.STATE_BUFFERING){buffering=true;bufferCount++;armBufferWatchdog(ep,generation);updateMediaSession(false,"Yayın tamponlanıyor…");}
                else if(state==Player.STATE_READY){buffering=false;cancelWatchdog();if(!userPaused)ep.play();}
                else if(state==Player.STATE_ENDED&&!userPaused)recover("Yayın sona erdi");
            }
            @Override public void onIsPlayingChanged(boolean playing){if(player!=ep)return;if(playing){buffering=false;lastPosition=safePosition(ep);lastProgressMs=System.currentTimeMillis();updateMediaSession(true,"Canlı yayın");updateNotification("Canlı yayın",true);armProgressWatchdog();}}
            @Override public void onPlayerError(PlaybackException e){if(player!=ep)return;lastError=e==null?-40:e.errorCode;recover("Bağlantı kesildi");}
            @Override public void onMediaMetadataChanged(androidx.media3.common.MediaMetadata m){if(player==ep)recordMetadata(m);}
            @Override public void onAudioSessionIdChanged(int id){if(player==ep){applyGain();applyEq();}}
        };
    }

    private void candidateFailed(ExoPlayer next,String target,long generation,int error){
        if(candidate!=next||generation!=playGeneration)return;lastError=error;releaseCandidate();
        if(player!=null&&player.isPlaying()){updateNotification("Eski yayın korunuyor",true);repairCandidate(generation);}
        else recover("Yeni kanal açılamadı");
    }

    private void repairCandidate(long generation){
        if(generation!=playGeneration)return;
        String alt=StreamFallbackManager.getPreferred(this,stationName,primaryUrl);
        if(alt!=null&&!alt.isEmpty()&&!alt.equals(streamUrl)){handler.postDelayed(()->{if(generation==playGeneration)seamlessSwitch(alt,generation);},500);}
        else handler.postDelayed(()->{if(generation==playGeneration)seamlessSwitch(primaryUrl,generation);},900);
    }

    private void armCandidateTimeout(ExoPlayer expected,String target,long generation){
        cancelWatchdog();watchdogTask=()->{if(candidate==expected&&generation==playGeneration){lastError=-31;candidateFailed(expected,target,generation,-31);}};handler.postDelayed(watchdogTask,STARTUP_TIMEOUT_MS);
    }

    private void armBufferWatchdog(ExoPlayer expected,long generation){
        cancelWatchdog();watchdogTask=()->{if(player==expected&&generation==playGeneration&&buffering&&!userPaused){lastError=-30;recover("Uzun buffer");}};handler.postDelayed(watchdogTask,REBUFFER_TIMEOUT_MS);
    }

    private void armProgressWatchdog(){
        cancelProgress();if(player==null||userPaused)return;
        progressTask=new Runnable(){@Override public void run(){if(player==null||userPaused){progressTask=null;return;}try{long now=System.currentTimeMillis();int s=player.getPlaybackState();if(s==Player.STATE_READY&&player.getPlayWhenReady()&&player.getPlaybackSuppressionReason()==Player.PLAYBACK_SUPPRESSION_REASON_NONE){long p=player.getCurrentPosition();if(lastPosition<0||Math.abs(p-lastPosition)>=200){lastPosition=p;lastProgressMs=now;}else if(now-lastProgressMs>=FREEZE_TIMEOUT_MS){freezeRecoveries++;lastError=-32;recover("Yayın takıldı");progressTask=null;return;}}}catch(Exception ignored){}if(progressTask!=null)handler.postDelayed(this,PROGRESS_CHECK_MS);}};
        handler.postDelayed(progressTask,PROGRESS_CHECK_MS);
    }

    private void recover(String why){
        if(userPaused||recoveryBusy||"offline".equals(networkType))return;recoveryBusy=true;cancelWatchdog();cancelProgress();
        updateNotification(why+" • toparlanıyor",true);
        if(sameSourceRetries<MAX_SAME_SOURCE_RETRIES){sameSourceRetries++;engineRestarts++;long g=playGeneration;handler.postDelayed(()->{recoveryBusy=false;if(g==playGeneration&&!userPaused)seamlessSwitch(streamUrl.isEmpty()?primaryUrl:streamUrl,g);},sameSourceRetries==1?350:900);return;}
        sameSourceRetries=0;reconnectAttempts++;
        StreamFallbackManager.markBad(this,stationName,streamUrl,3*60_000L);
        long g=playGeneration;String alt=StreamFallbackManager.getPreferred(this,stationName,primaryUrl);
        handler.postDelayed(()->{recoveryBusy=false;if(g==playGeneration&&!userPaused)seamlessSwitch((alt==null||alt.isEmpty())?primaryUrl:alt,g);},700);
    }

    private void crossfade(ExoPlayer old,Player.Listener oldListener,ExoPlayer next){
        if(fadeTask!=null)handler.removeCallbacks(fadeTask);
        if(old==null||old==next){next.setVolume(volume);return;}
        final int[] n={0};fadeTask=new Runnable(){@Override public void run(){n[0]++;float f=Math.min(1f,n[0]/5f);try{next.setVolume(volume*f);old.setVolume(volume*(1f-f));}catch(Exception ignored){}if(f<1f)handler.postDelayed(this,40);else{releaseOne(old,oldListener);fadeTask=null;}}};handler.post(fadeTask);
    }

    private void pauseInternal(boolean manual){if(manual){userPaused=true;PlaybackGuardian.manualPause(this);}cancelProgress();cancelWatchdog();releaseCandidate();if(player!=null)try{player.pause();}catch(Exception ignored){}updateMediaSession(false,"Duraklatıldı");updateNotification("Duraklatıldı",false);}
    private void resumeInternal(){PlaybackGuardian.manualPlay(this);userPaused=false;if(player!=null){try{player.play();lastPosition=safePosition(player);lastProgressMs=System.currentTimeMillis();armProgressWatchdog();updateMediaSession(true,"Canlı yayın");updateNotification("Canlı yayın",true);return;}catch(Exception ignored){}}if(!primaryUrl.isEmpty())seamlessSwitch(streamUrl.isEmpty()?primaryUrl:streamUrl,playGeneration);}
    private void stopInternal(){userPaused=true;PlaybackGuardian.manualPause(this);cancelAll();releaseCandidate();releaseActive();if(mediaSession!=null)mediaSession.setActive(false);mainHandler.post(()->{stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();});}

    private void playFromMediaId(String id){if(id==null)return;try{if(id.startsWith("q:")){playQueueIndex(Integer.parseInt(id.substring(2)));return;}if(id.startsWith("recent:")){int i=Integer.parseInt(id.substring(7));JSONArray a=new JSONArray(getSharedPreferences("radio",MODE_PRIVATE).getString("recentStations","[]"));JSONObject o=a.optJSONObject(i);if(o!=null)requestStation(o.optString("url"),o.optString("name","Türk Radyo"));}}catch(Exception ignored){}}
    private void stepQueue(int d){try{SharedPreferences p=getSharedPreferences("radio",MODE_PRIVATE);JSONArray q=new JSONArray(p.getString("queue","[]"));if(q.length()==0)return;int i=p.getInt("queueIndex",0);i=(i+d)%q.length();if(i<0)i+=q.length();playQueueIndex(i);}catch(Exception ignored){}}
    private void playQueueIndex(int i){try{SharedPreferences p=getSharedPreferences("radio",MODE_PRIVATE);JSONArray q=new JSONArray(p.getString("queue","[]"));JSONObject o=q.optJSONObject(i);if(o==null)return;String u=o.optString("url"),n=o.optString("name","Türk Radyo");if(u.isEmpty())return;p.edit().putInt("queueIndex",i).apply();pendingUrl=u;pendingName=n;playGeneration++;requestStation(u,n);}catch(Exception ignored){}}
    private void syncQueueIndexForUrl(String url){try{SharedPreferences p=getSharedPreferences("radio",MODE_PRIVATE);JSONArray a=new JSONArray(p.getString("queue","[]"));for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&url.equals(o.optString("url"))){p.edit().putInt("queueIndex",i).apply();break;}}}catch(Exception ignored){}}

    private void recordMetadata(androidx.media3.common.MediaMetadata m){if(m==null)return;String t=m.title==null?"":m.title.toString(),ar=m.artist==null?"":m.artist.toString();if(!ar.isEmpty()&&!t.isEmpty()&&!t.toLowerCase().contains(ar.toLowerCase()))t=ar+" - "+t;recordTrack(t,"MEDIA3");}
    private String clean(String s){String x=s==null?"":s.replaceAll("\\s+"," ").trim();if(x.length()>240)x=x.substring(0,240);return x;}
    private void recordTrack(String title,String source){title=clean(title);if(title.length()<2||title.equalsIgnoreCase(stationName))return;long now=System.currentTimeMillis();if(title.equalsIgnoreCase(lastTrackTitle)&&now-lastTrackMs<90_000L)return;lastTrackTitle=title;lastTrackMs=now;try{SharedPreferences p=getSharedPreferences("radio",MODE_PRIVATE);JSONArray old;try{old=new JSONArray(p.getString("tracks","[]"));}catch(Exception e){old=new JSONArray();}JSONArray out=new JSONArray();JSONObject n=new JSONObject();n.put("title",title);n.put("station",stationName);n.put("time",now);n.put("source",source);out.put(n);for(int i=0;i<old.length()&&out.length()<50;i++){JSONObject x=old.optJSONObject(i);if(x!=null)out.put(x);}p.edit().putString("tracks",out.toString()).putString("nowTitle",title).apply();updateMediaSession(player!=null&&player.isPlaying(),title);}catch(Exception ignored){}}

    private void saveCurrent(){getSharedPreferences("radio",MODE_PRIVATE).edit().putString("url",primaryUrl).putString("resolvedUrl",streamUrl).putString("name",stationName).apply();}
    private void saveRecent(){try{SharedPreferences p=getSharedPreferences("radio",MODE_PRIVATE);JSONArray old;try{old=new JSONArray(p.getString("recentStations","[]"));}catch(Exception e){old=new JSONArray();}JSONArray out=new JSONArray();JSONObject n=new JSONObject();n.put("name",stationName);n.put("url",primaryUrl);out.put(n);for(int i=0;i<old.length()&&out.length()<20;i++){JSONObject x=old.optJSONObject(i);if(x!=null&&!primaryUrl.equals(x.optString("url")))out.put(x);}p.edit().putString("recentStations",out.toString()).apply();}catch(Exception ignored){}}
    private void saveTelemetry(){try{JSONObject o=new JSONObject();o.put("engine","media3-exoplayer-1.11.0-dedicated-thread");o.put("startupMs",startupMs);o.put("bufferCount",bufferCount);o.put("lastError",lastError);o.put("sameSourceRetries",sameSourceRetries);o.put("engineRestarts",engineRestarts);o.put("freezeRecoveries",freezeRecoveries);o.put("resolvedUrl",streamUrl);o.put("networkType",networkType);o.put("serviceUptimeMs",System.currentTimeMillis()-serviceStartMs);o.put("playbackThread",true);o.put("seamlessHandoff",true);if(player!=null){o.put("playerState",player.getPlaybackState());o.put("isPlaying",player.isPlaying());o.put("positionMs",player.getCurrentPosition());}getSharedPreferences("radio",MODE_PRIVATE).edit().putString("telemetry",o.toString()).apply();}catch(Exception ignored){}}

    private void armStableReset(){cancelStable();stableTask=()->{if(player!=null&&!userPaused)try{if(player.isPlaying()){sameSourceRetries=0;reconnectAttempts=0;recoveryBusy=false;PlaybackGuardian.recovered(this);saveTelemetry();}}catch(Exception ignored){};handler.postDelayed(stableTask,STABLE_RESET_MS);}
    private long safePosition(ExoPlayer ep){try{return ep==null?-1:ep.getCurrentPosition();}catch(Exception e){return-1;}}
    private void cancelWatchdog(){if(watchdogTask!=null){handler.removeCallbacks(watchdogTask);watchdogTask=null;}}
    private void cancelProgress(){if(progressTask!=null){handler.removeCallbacks(progressTask);progressTask=null;}}
    private void cancelStable(){if(stableTask!=null){handler.removeCallbacks(stableTask);stableTask=null;}}
    private void cancelAll(){cancelWatchdog();cancelProgress();cancelStable();if(reconnectTask!=null){handler.removeCallbacks(reconnectTask);reconnectTask=null;}if(fadeTask!=null){handler.removeCallbacks(fadeTask);fadeTask=null;}if(pendingPlayTask!=null){handler.removeCallbacks(pendingPlayTask);pendingPlayTask=null;}}

    private void releaseCandidate(){cancelWatchdog();if(candidate!=null){try{if(candidateListener!=null)candidate.removeListener(candidateListener);}catch(Exception ignored){}try{candidate.release();}catch(Exception ignored){}candidate=null;candidateListener=null;}}
    private void releaseOne(ExoPlayer ep,Player.Listener l){if(ep==null)return;try{if(l!=null)ep.removeListener(l);}catch(Exception ignored){}try{ep.release();}catch(Exception ignored){}}
    private void releaseActive(){cancelProgress();if(equalizer!=null){try{equalizer.release();}catch(Exception ignored){}equalizer=null;}if(enhancer!=null){try{enhancer.release();}catch(Exception ignored){}enhancer=null;}ExoPlayer p=player;Player.Listener l=playerListener;player=null;playerListener=null;releaseOne(p,l);}

    private int audioSessionId(){try{return player==null?0:player.getAudioSessionId();}catch(Exception e){return 0;}}
    private void applyGain(){int sid=audioSessionId();if(sid<=0)return;try{if(enhancer!=null)enhancer.release();enhancer=new LoudnessEnhancer(sid);int t=normalize?Math.max(300,gainMb):gainMb;t=Math.max(0,Math.min(1200,t));enhancer.setTargetGain(t);enhancer.setEnabled(t>0);}catch(Exception ignored){}}
    private void applyEq(){int sid=audioSessionId();if(sid<=0)return;try{if(equalizer!=null)equalizer.release();equalizer=new Equalizer(0,sid);short bands=equalizer.getNumberOfBands();short[] range=equalizer.getBandLevelRange();for(short b=0;b<bands&&b<eqLevels.length;b++)equalizer.setBandLevel(b,(short)Math.max(range[0],Math.min(range[1],eqLevels[b])));equalizer.setEnabled(true);}catch(Exception ignored){}}

    private void updateMediaSession(boolean playing,String subtitle){if(mediaSession==null)return;long acts=PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_PLAY_PAUSE|PlaybackState.ACTION_STOP|PlaybackState.ACTION_SKIP_TO_NEXT|PlaybackState.ACTION_SKIP_TO_PREVIOUS|PlaybackState.ACTION_PLAY_FROM_MEDIA_ID;int state=playing?PlaybackState.STATE_PLAYING:(userPaused?PlaybackState.STATE_PAUSED:PlaybackState.STATE_CONNECTING);mediaSession.setPlaybackState(new PlaybackState.Builder().setActions(acts).setState(state,PlaybackState.PLAYBACK_POSITION_UNKNOWN,playing?1f:0f).build());String now=getSharedPreferences("radio",MODE_PRIVATE).getString("nowTitle","");mediaSession.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,stationName).putString(MediaMetadata.METADATA_KEY_ARTIST,now.isEmpty()?subtitle:now).putString(MediaMetadata.METADATA_KEY_ALBUM,"Türk Radyo").build());}
    private PendingIntent svc(String action,int req){return PendingIntent.getService(this,req,new Intent(this,RadioService.class).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private Notification buildNotification(String state,boolean playing){PendingIntent content=PendingIntent.getActivity(this,1,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);b.setSmallIcon(R.drawable.ic_stat_radio).setContentTitle(stationName).setContentText(state).setContentIntent(content).setOnlyAlertOnce(true).setOngoing(playing).setCategory(Notification.CATEGORY_TRANSPORT).setVisibility(Notification.VISIBILITY_PUBLIC).addAction(new Notification.Action.Builder(android.R.drawable.ic_media_previous,"Geri",svc(ACTION_PREV,10)).build()).addAction(new Notification.Action.Builder(playing?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,playing?"Duraklat":"Oynat",svc(playing?ACTION_PAUSE:ACTION_RESUME,11)).build()).addAction(new Notification.Action.Builder(android.R.drawable.ic_media_next,"İleri",svc(ACTION_NEXT,12)).build());if(Build.VERSION.SDK_INT>=21){Notification.MediaStyle s=new Notification.MediaStyle().setShowActionsInCompactView(0,1,2);if(mediaSession!=null)s.setMediaSession(mediaSession.getSessionToken());b.setStyle(s);}return b.build();}
    private void updateNotification(String text,boolean playing){Notification n=buildNotification(text,playing);mainHandler.post(()->{if(mediaSession!=null&&!mediaSession.isActive())mediaSession.setActive(true);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIF_ID,n);});}
    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,getString(R.string.notif_channel_name),NotificationManager.IMPORTANCE_LOW);c.setDescription(getString(R.string.notif_channel_desc));c.setShowBadge(false);c.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);}}
    private void post(Runnable r){Handler h=handler;if(h!=null)h.post(r);}

    @Override public void onDestroy(){
        if(connectivityManager!=null&&networkCallback!=null)try{connectivityManager.unregisterNetworkCallback(networkCallback);}catch(Exception ignored){}
        Handler h=handler;if(h!=null)h.post(()->{userPaused=true;cancelAll();releaseCandidate();releaseActive();if(playbackThread!=null)playbackThread.quitSafely();});
        if(mediaSession!=null){try{mediaSession.release();}catch(Exception ignored){}}
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent i){return null;}
}
