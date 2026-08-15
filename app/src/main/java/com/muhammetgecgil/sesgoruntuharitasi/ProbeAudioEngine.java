package com.muhammetgecgil.sesgoruntuharitasi;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import java.util.concurrent.atomic.AtomicBoolean;

/** V7 Probe Fusion audio core. */
public final class ProbeAudioEngine {
    public static final int MODE_USB_ABSOLUTE = 0;
    public static final int MODE_USB_MINUS_REF = 1;
    public static final int MODE_BAND_SCAN = 2;
    public static final int BAND_ALL = 0, BAND_LOW = 1, BAND_VOICE = 2, BAND_HIGH = 3;
    public static final int SAMPLE_RATE = 48000;
    private static final int FRAMES = 960;

    public interface Listener { void onProbe(Snapshot s); }
    public static final class Snapshot {
        public final long timestampNs;
        public final float usbDbfs, refDbfs, deltaDb;
        public final float usbRms01, refRms01, bandEnergy01;
        public final boolean usbActive, phoneRefActive, dualLive;
        public final String usbName, refName, status;
        Snapshot(long t,float u,float r,float d,float ur,float rr,float be,
                 boolean ua,boolean pa,boolean dl,String un,String rn,String st){
            timestampNs=t;usbDbfs=u;refDbfs=r;deltaDb=d;usbRms01=ur;refRms01=rr;bandEnergy01=be;
            usbActive=ua;phoneRefActive=pa;dualLive=dl;usbName=un;refName=rn;status=st;
        }
    }

    private final Context context; private final Listener listener;
    private final AtomicBoolean running=new AtomicBoolean(false);
    private volatile int mode=MODE_USB_ABSOLUTE, band=BAND_ALL;
    private volatile float calibratedRefDb=-55f;
    private Thread thread; private AudioRecord usbRec, refRec;

    public ProbeAudioEngine(Context c,Listener l){context=c.getApplicationContext();listener=l;}
    public void setMode(int m){mode=Math.max(0,Math.min(2,m));}
    public int getMode(){return mode;}
    public void setBand(int b){band=Math.max(0,Math.min(3,b));}
    public int getBand(){return band;}
    public void setCalibratedReference(float db){calibratedRefDb=db;}
    public float getCalibratedReference(){return calibratedRefDb;}

    public void start(){ if(running.getAndSet(true))return; thread=new Thread(this::loop,"ProbeFusionAudio");thread.start(); }
    public void stop(){ running.set(false); release(); if(thread!=null){try{thread.join(500);}catch(Exception ignored){}thread=null;} }

    private void loop(){
        if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){running.set(false);return;}
        AudioManager am=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo usb=findUsb(am), builtIn=findBuiltIn(am);
        String usbName=usb==null?"USB bulunamadı":String.valueOf(usb.getProductName());
        String refName=builtIn==null?"Telefon mic bulunamadı":String.valueOf(builtIn.getProductName());
        if(usb==null){ emit(-120,-120,0,0,0,0,false,false,false,usbName,refName,"USB mikrofon bağlı değil");running.set(false);return; }

        boolean refCalibrated=false;
        if (builtIn != null) {
            float measured = measureReference(builtIn);
            if (measured > -110f) { calibratedRefDb = measured; refCalibrated=true; }
        }

        usbRec=build(usb); boolean usbOk=initialized(usbRec);
        refRec=build(builtIn); boolean refOk=initialized(refRec);
        if(!usbOk){release();emit(-120,-120,0,0,0,0,false,refCalibrated,false,usbName,refName,"USB AudioRecord açılamadı");running.set(false);return;}

        try{usbRec.startRecording();}catch(Exception e){release();running.set(false);return;}
        boolean dual=false;
        if(refOk){
            try{refRec.startRecording();dual=refRec.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING;}catch(Exception ignored){dual=false;}
        }
        boolean usbRouted=routeMatches(usbRec,usb);
        boolean refRouted=dual && routeMatches(refRec,builtIn);
        dual=usbRouted && refRouted;
        if(!dual && refRec!=null){try{refRec.stop();}catch(Exception ignored){}try{refRec.release();}catch(Exception ignored){}refRec=null;}

        short[] ub=new short[FRAMES], rb=new short[FRAMES];
        while(running.get()){
            int un; try{un=usbRec.read(ub,0,ub.length,AudioRecord.READ_BLOCKING);}catch(Exception e){break;}
            if(un<=0)continue;
            int rn=0;if(dual&&refRec!=null){try{rn=refRec.read(rb,0,rb.length,AudioRecord.READ_NON_BLOCKING);}catch(Exception ignored){rn=0;}}
            Features uf=features(ub,un,band); Features rf=rn>32?features(rb,rn,BAND_ALL):null;
            float refDb=dual&&rf!=null?rf.dbfs:calibratedRefDb;
            float delta=clamp(uf.dbfs-refDb,-30f,30f);
            float mapEnergy;
            if(mode==MODE_USB_ABSOLUTE) mapEnergy=uf.rms01;
            else if(mode==MODE_USB_MINUS_REF) mapEnergy=clamp01((delta+3f)/24f);
            else mapEnergy=uf.band01;
            boolean refAvailable=dual||refCalibrated;
            String st=dual?"DUAL LIVE: USB + telefon referans":refCalibrated?"USB LIVE • telefon referansı kalibre":"USB LIVE • telefon referansı yok";
            emit(uf.dbfs,refDb,delta,uf.rms01,rf==null?0:rf.rms01,mapEnergy,true,refAvailable,dual,usbName,refName,st);
        }
        release();running.set(false);
    }

    public static AudioEngine.Snapshot asFusionSnapshot(Snapshot s, int band) {
        if (s == null) return null;
        float level = clamp01(s.bandEnergy01);
        float low = band == BAND_LOW ? level : level * .30f;
        float voice = band == BAND_VOICE ? level : level * .30f;
        float high = band == BAND_HIGH ? level : level * .30f;
        String cls = s.dualLive ? "USB-REF" : "USB PROBE";
        return new AudioEngine.Snapshot(level, level, s.usbDbfs, 0f, voice, low, high, 0f,
                0f, 0f, s.timestampNs, s.phoneRefActive ? 2 : 1, 1, true, cls);
    }

    private float measureReference(AudioDeviceInfo builtIn) {
        AudioRecord r = build(builtIn);
        if (!initialized(r)) { try { if(r!=null)r.release(); } catch(Exception ignored){} return -120f; }
        try {
            r.startRecording();
            short[] b = new short[FRAMES];
            double sumDb = 0; int blocks = 0;
            long until = System.nanoTime() + 650_000_000L;
            while (System.nanoTime() < until && blocks < 32) {
                int n = r.read(b,0,b.length,AudioRecord.READ_BLOCKING);
                if (n > 32) { sumDb += features(b,n,BAND_ALL).dbfs; blocks++; }
            }
            return blocks > 0 ? (float)(sumDb/blocks) : -120f;
        } catch(Exception e) { return -120f; }
        finally { try { r.stop(); } catch(Exception ignored){} try { r.release(); } catch(Exception ignored){} }
    }

    private AudioRecord build(AudioDeviceInfo preferred){
        if(preferred==null)return null;
        try{
            int min=AudioRecord.getMinBufferSize(SAMPLE_RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
            AudioFormat fmt=new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build();
            AudioRecord r=new AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.UNPROCESSED).setAudioFormat(fmt).setBufferSizeInBytes(Math.max(min,FRAMES*8)).build();
            r.setPreferredDevice(preferred);return r;
        }catch(Exception e){
            try{
                int min=AudioRecord.getMinBufferSize(SAMPLE_RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
                AudioRecord r=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,SAMPLE_RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(min,FRAMES*8));
                r.setPreferredDevice(preferred);return r;
            }catch(Exception ignored){return null;}
        }
    }
    private static boolean initialized(AudioRecord r){return r!=null&&r.getState()==AudioRecord.STATE_INITIALIZED;}
    private static boolean routeMatches(AudioRecord r,AudioDeviceInfo requested){try{AudioDeviceInfo d=r.getRoutedDevice();return d!=null&&requested!=null&&d.getId()==requested.getId();}catch(Exception e){return false;}}
    private static AudioDeviceInfo findUsb(AudioManager am){for(AudioDeviceInfo d:am.getDevices(AudioManager.GET_DEVICES_INPUTS)){int t=d.getType();if(t==AudioDeviceInfo.TYPE_USB_DEVICE||t==AudioDeviceInfo.TYPE_USB_HEADSET||t==AudioDeviceInfo.TYPE_USB_ACCESSORY)return d;}return null;}
    private static AudioDeviceInfo findBuiltIn(AudioManager am){for(AudioDeviceInfo d:am.getDevices(AudioManager.GET_DEVICES_INPUTS))if(d.getType()==AudioDeviceInfo.TYPE_BUILTIN_MIC)return d;return null;}

    private static final class Features{float dbfs,rms01,band01;Features(float d,float r,float b){dbfs=d;rms01=r;band01=b;}}
    private static Features features(short[] x,int n,int band){
        double sum=0;for(int i=0;i<n;i++){double v=x[i]/32768.0;sum+=v*v;}double rms=Math.sqrt(sum/Math.max(1,n));float db=(float)(20*Math.log10(Math.max(1e-7,rms)));
        int[] all={100,200,400,800,1200,2000,3500,6000,10000};
        int lo=0,hi=all.length;if(band==BAND_LOW){lo=0;hi=3;}else if(band==BAND_VOICE){lo=2;hi=7;}else if(band==BAND_HIGH){lo=6;hi=all.length;}
        double e=0;for(int i=lo;i<hi;i++)e+=goertzel(x,n,all[i]);e/=Math.max(1,hi-lo);
        float b=clamp01((float)Math.sqrt(e)/6000f);return new Features(db,clamp01((float)rms*9f),b);
    }
    private static double goertzel(short[] x,int n,int hz){double w=2*Math.PI*hz/SAMPLE_RATE,coeff=2*Math.cos(w),s0,s1=0,s2=0;for(int i=0;i<n;i++){s0=x[i]+coeff*s1-s2;s2=s1;s1=s0;}return s1*s1+s2*s2-coeff*s1*s2;}
    private void emit(float u,float r,float d,float ur,float rr,float be,boolean ua,boolean pa,boolean dl,String un,String rn,String st){if(listener!=null)listener.onProbe(new Snapshot(System.nanoTime(),u,r,d,ur,rr,be,ua,pa,dl,un,rn,st));}
    private void release(){if(usbRec!=null){try{usbRec.stop();}catch(Exception ignored){}try{usbRec.release();}catch(Exception ignored){}usbRec=null;}if(refRec!=null){try{refRec.stop();}catch(Exception ignored){}try{refRec.release();}catch(Exception ignored){}refRec=null;}}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));} private static float clamp01(float v){return clamp(v,0,1);}
}
