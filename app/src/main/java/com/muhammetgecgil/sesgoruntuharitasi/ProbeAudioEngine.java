package com.muhammetgecgil.sesgoruntuharitasi;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** V8.2 reference-calibrated probe engine. S24 built-in mic is relative reference; Fyvadio USB is roaming probe. */
public final class ProbeAudioEngine {
    public static final int MODE_USB_ABSOLUTE=0,MODE_USB_MINUS_REF=1,MODE_BAND_SCAN=2;
    public static final int BAND_ALL=0,BAND_LOW=1,BAND_VOICE=2,BAND_HIGH=3;
    public static final int SAMPLE_RATE=48000;
    private static final int FRAMES=960;
    public interface Listener{void onProbe(Snapshot s);}    
    public static final class Snapshot{
        public final long timestampNs; public final float usbDbfs,refDbfs,deltaDb,usbRms01,refRms01,bandEnergy01,refNoiseFloorDb;
        public final boolean usbActive,phoneRefActive,dualLive,refStable;
        public final String usbName,refName,status;
        Snapshot(long t,float u,float r,float d,float ur,float rr,float be,float nf,boolean ua,boolean pa,boolean dl,boolean rs,String un,String rn,String st){timestampNs=t;usbDbfs=u;refDbfs=r;deltaDb=d;usbRms01=ur;refRms01=rr;bandEnergy01=be;refNoiseFloorDb=nf;usbActive=ua;phoneRefActive=pa;dualLive=dl;refStable=rs;usbName=un;refName=rn;status=st;}
    }
    private final Context context; private final Listener listener; private final AtomicBoolean running=new AtomicBoolean(false);
    private volatile int mode=MODE_USB_MINUS_REF,band=BAND_ALL; private volatile float calibratedRefDb=-55f,refNoiseFloorDb=-70f;
    private Thread thread; private AudioRecord usbRec,refRec;
    public ProbeAudioEngine(Context c,Listener l){context=c.getApplicationContext();listener=l;}
    public void setMode(int m){mode=Math.max(0,Math.min(2,m));} public int getMode(){return mode;}
    public void setBand(int b){band=Math.max(0,Math.min(3,b));} public int getBand(){return band;}
    public void setCalibratedReference(float d){calibratedRefDb=d;} public float getCalibratedReference(){return calibratedRefDb;} public float getReferenceNoiseFloor(){return refNoiseFloorDb;}
    public void start(){if(running.getAndSet(true))return;thread=new Thread(this::loop,"ProbeFusionAudio82");thread.start();}
    public void stop(){running.set(false);release();if(thread!=null){try{thread.join(500);}catch(Exception ignored){}thread=null;}}

    private void loop(){
        if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){running.set(false);return;}
        AudioManager am=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);AudioDeviceInfo usb=findUsb(am),builtIn=findBuiltIn(am);
        String usbName=usb==null?"USB bulunamadı":String.valueOf(usb.getProductName()),refName=builtIn==null?"Telefon mic bulunamadı":String.valueOf(builtIn.getProductName());
        if(usb==null){emit(-120,-120,0,0,0,0,-120,false,false,false,false,usbName,refName,"USB mikrofon bağlı değil");running.set(false);return;}
        boolean refCal=false;
        if(builtIn!=null){Cal c=measureReference(builtIn);if(c.valid){calibratedRefDb=c.level;refNoiseFloorDb=c.floor;refCal=true;}}
        usbRec=build(usb);refRec=build(builtIn);boolean usbOk=initialized(usbRec),refOk=initialized(refRec);
        if(!usbOk){release();emit(-120,calibratedRefDb,0,0,0,0,refNoiseFloorDb,false,refCal,false,refCal,usbName,refName,"USB AudioRecord açılamadı");running.set(false);return;}
        try{usbRec.startRecording();}catch(Exception e){release();running.set(false);return;}
        boolean dual=false;if(refOk){try{refRec.startRecording();dual=refRec.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING;}catch(Exception ignored){}}
        dual=dual&&routeMatches(usbRec,usb)&&routeMatches(refRec,builtIn);if(!dual&&refRec!=null){try{refRec.stop();}catch(Exception ignored){}try{refRec.release();}catch(Exception ignored){}refRec=null;}
        short[] ub=new short[FRAMES],rb=new short[FRAMES];float smoothRef=calibratedRefDb,smoothDelta=0;boolean first=true;
        while(running.get()){
            int un;try{un=usbRec.read(ub,0,ub.length,AudioRecord.READ_BLOCKING);}catch(Exception e){break;}if(un<=0)continue;
            int rn=0;if(dual&&refRec!=null){try{rn=refRec.read(rb,0,rb.length,AudioRecord.READ_NON_BLOCKING);}catch(Exception ignored){}}
            Features uf=features(ub,un,band),rf=rn>32?features(rb,rn,BAND_ALL):null;
            float rawRef=dual&&rf!=null?rf.dbfs:calibratedRefDb;
            float jump=Math.abs(rawRef-smoothRef);float alpha=jump>6f?.035f:.12f;smoothRef=smoothRef*(1-alpha)+rawRef*alpha;
            boolean stable=jump<8f;
            float rawDelta=clamp(uf.dbfs-smoothRef,-30,30);if(first){smoothDelta=rawDelta;first=false;}else smoothDelta=.78f*smoothDelta+.22f*rawDelta;
            float mapEnergy=mode==MODE_USB_ABSOLUTE?uf.rms01:mode==MODE_USB_MINUS_REF?clamp01((smoothDelta+2f)/24f):uf.band01;
            String st=(dual?"DUAL LIVE":"REF CAL")+" • S24 REF "+String.format(Locale.US,"%.1f dBFS",smoothRef)+" • FLOOR "+String.format(Locale.US,"%.1f",refNoiseFloorDb)+(stable?" • STABLE":" • GAIN BEKLE");
            emit(uf.dbfs,smoothRef,smoothDelta,uf.rms01,rf==null?0:rf.rms01,mapEnergy,refNoiseFloorDb,true,dual||refCal,dual,stable,usbName,refName,st);
        }
        release();running.set(false);
    }

    private static final class Cal{final float level,floor;final boolean valid;Cal(float l,float f,boolean v){level=l;floor=f;valid=v;}}
    private Cal measureReference(AudioDeviceInfo d){AudioRecord r=build(d);if(!initialized(r)){try{if(r!=null)r.release();}catch(Exception ignored){}return new Cal(-120,-120,false);}try{r.startRecording();short[] b=new short[FRAMES];ArrayList<Float> vals=new ArrayList<>();long until=System.nanoTime()+3_000_000_000L;while(System.nanoTime()<until&&vals.size()<180){int n=r.read(b,0,b.length,AudioRecord.READ_BLOCKING);if(n>32)vals.add(features(b,n,BAND_ALL).dbfs);}if(vals.size()<12)return new Cal(-120,-120,false);Collections.sort(vals);int lo=Math.max(1,vals.size()/10),hi=Math.min(vals.size()-1,vals.size()-vals.size()/10);double s=0;for(int i=lo;i<hi;i++)s+=vals.get(i);float trimmed=(float)(s/Math.max(1,hi-lo));float floor=vals.get(Math.max(0,(int)(vals.size()*.20f)-1));return new Cal(trimmed,floor,true);}catch(Exception e){return new Cal(-120,-120,false);}finally{try{r.stop();}catch(Exception ignored){}try{r.release();}catch(Exception ignored){}}}

    public static AudioEngine.Snapshot asFusionSnapshot(Snapshot s,int b){if(s==null)return null;float l=clamp01(s.bandEnergy01),low=b==BAND_LOW?l:l*.3f,voice=b==BAND_VOICE?l:l*.3f,high=b==BAND_HIGH?l:l*.3f;return new AudioEngine.Snapshot(l,l,s.usbDbfs,0,voice,low,high,0,0,0,s.timestampNs,s.phoneRefActive?2:1,1,true,s.dualLive?"USB-REF":"USB PROBE");}
    private AudioRecord build(AudioDeviceInfo p){if(p==null)return null;try{int min=AudioRecord.getMinBufferSize(SAMPLE_RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);AudioFormat f=new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build();AudioRecord r=new AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.UNPROCESSED).setAudioFormat(f).setBufferSizeInBytes(Math.max(min,FRAMES*8)).build();r.setPreferredDevice(p);return r;}catch(Exception e){try{int min=AudioRecord.getMinBufferSize(SAMPLE_RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);AudioRecord r=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,SAMPLE_RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(min,FRAMES*8));r.setPreferredDevice(p);return r;}catch(Exception x){return null;}}}
    private static boolean initialized(AudioRecord r){return r!=null&&r.getState()==AudioRecord.STATE_INITIALIZED;} private static boolean routeMatches(AudioRecord r,AudioDeviceInfo q){try{AudioDeviceInfo d=r.getRoutedDevice();return d!=null&&q!=null&&d.getId()==q.getId();}catch(Exception e){return false;}}
    private static AudioDeviceInfo findUsb(AudioManager a){for(AudioDeviceInfo d:a.getDevices(AudioManager.GET_DEVICES_INPUTS)){int t=d.getType();if(t==AudioDeviceInfo.TYPE_USB_DEVICE||t==AudioDeviceInfo.TYPE_USB_HEADSET||t==AudioDeviceInfo.TYPE_USB_ACCESSORY)return d;}return null;} private static AudioDeviceInfo findBuiltIn(AudioManager a){for(AudioDeviceInfo d:a.getDevices(AudioManager.GET_DEVICES_INPUTS))if(d.getType()==AudioDeviceInfo.TYPE_BUILTIN_MIC)return d;return null;}
    private static final class Features{float dbfs,rms01,band01;Features(float d,float r,float b){dbfs=d;rms01=r;band01=b;}}
    private static Features features(short[] x,int n,int band){double s=0;for(int i=0;i<n;i++){double v=x[i]/32768.0;s+=v*v;}double rms=Math.sqrt(s/Math.max(1,n));float db=(float)(20*Math.log10(Math.max(1e-7,rms)));int[] f={100,200,400,800,1200,2000,3500,6000,10000};int lo=0,hi=f.length;if(band==BAND_LOW){hi=3;}else if(band==BAND_VOICE){lo=2;hi=7;}else if(band==BAND_HIGH){lo=6;}double e=0;for(int i=lo;i<hi;i++)e+=goertzel(x,n,f[i]);e/=Math.max(1,hi-lo);return new Features(db,clamp01((float)rms*9f),clamp01((float)Math.sqrt(e)/6000f));}
    private static double goertzel(short[] x,int n,int hz){double w=2*Math.PI*hz/SAMPLE_RATE,c=2*Math.cos(w),s0,s1=0,s2=0;for(int i=0;i<n;i++){s0=x[i]+c*s1-s2;s2=s1;s1=s0;}return s1*s1+s2*s2-c*s1*s2;}
    private void emit(float u,float r,float d,float ur,float rr,float be,float nf,boolean ua,boolean pa,boolean dl,boolean rs,String un,String rn,String st){if(listener!=null)listener.onProbe(new Snapshot(System.nanoTime(),u,r,d,ur,rr,be,nf,ua,pa,dl,rs,un,rn,st));}
    private void release(){if(usbRec!=null){try{usbRec.stop();}catch(Exception ignored){}try{usbRec.release();}catch(Exception ignored){}usbRec=null;}if(refRec!=null){try{refRec.stop();}catch(Exception ignored){}try{refRec.release();}catch(Exception ignored){}refRec=null;}}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}private static float clamp01(float v){return clamp(v,0,1);}
}
