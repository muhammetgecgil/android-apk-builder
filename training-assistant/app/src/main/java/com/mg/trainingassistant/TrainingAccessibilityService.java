package com.mg.trainingassistant;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityService.ScreenshotResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.HardwareBuffer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.text.Normalizer;
import java.util.*;

public class TrainingAccessibilityService extends AccessibilityService {
    private static final String PREFS="training_assistant";
    private static final long SCAN_MS=1100, OCR_MS=6000, CLICK_GUARD=2200;
    private static final List<String> PLAY=Arrays.asList("play","oynat","baslat","başlat","resume","devam oynat");
    private static final List<String> PAUSE=Arrays.asList("pause","duraklat","oynatmayi duraklat","oynatmayı duraklat");
    private static final List<String> NEXT=Arrays.asList("ileri","devam","devam et","sonraki","sonraki adim","sonraki ders","next","continue","next step","next lesson","proceed");
    private static final List<String> COMPLETE=Arrays.asList("tamamlandi","tamamlandı","completed","complete","100%","100 %","bitti","finished","lesson complete","ders tamamlandi");
    private static final List<String> STOP=Arrays.asList("quiz","sinav","sınav","exam","assessment","degerlendirme","değerlendirme","submit answer","cevabi gonder","cevabı gönder","onayla","dogrula","doğrula");
    private static final List<String> BLOCKED=Arrays.asList("com.android.settings","com.android.systemui","com.google.android.permissioncontroller","com.samsung.android.permissioncontroller","com.google.android.packageinstaller");

    private final Handler h=new Handler(Looper.getMainLooper());
    private long lastClick=0,lastOcr=0,pageStart=0,playStarted=0;
    private String pageSig="";
    private boolean ocrBusy=false;
    private SpeechRecognizer speech;
    private Intent speechIntent;

    private final Runnable loop=new Runnable(){@Override public void run(){scan();h.postDelayed(this,SCAN_MS);}};

    @Override protected void onServiceConnected(){super.onServiceConnected();h.removeCallbacks(loop);h.post(loop);startSpeech();}
    @Override public void onAccessibilityEvent(AccessibilityEvent e){if(isRunning()) h.postDelayed(this::scan,120);}
    @Override public void onInterrupt(){h.removeCallbacks(loop);stopSpeech();}
    @Override public void onDestroy(){h.removeCallbacks(loop);stopSpeech();super.onDestroy();}

    private boolean isRunning(){return getSharedPreferences(PREFS,MODE_PRIVATE).getBoolean("running",false)&&getSharedPreferences(PREFS,MODE_PRIVATE).getBoolean("consent",false);}

    private void scan(){
        if(!isRunning()) return;
        AccessibilityNodeInfo root=getRootInActiveWindow(); if(root==null) return;
        String pkg=root.getPackageName()==null?"":root.getPackageName().toString();
        if(pkg.equals(getPackageName())||blocked(pkg)) return;
        String visible=collect(root,new StringBuilder(),0).toString().replaceAll("\\s+"," ").trim();
        String n=norm(visible); if(visible.length()>20) CaptureStore.append(this,"SCREEN",visible);
        String sig=pkg+"|"+(n.length()>700?n.substring(0,700):n).hashCode();
        long now=SystemClock.uptimeMillis();
        if(!sig.equals(pageSig)){pageSig=sig;pageStart=now;playStarted=0;}
        if(contains(n,STOP)) return;
        if(now-lastOcr>OCR_MS){lastOcr=now;captureOcr();}
        if(now-lastClick<CLICK_GUARD) return;

        AccessibilityNodeInfo pause=findByWords(root,PAUSE,0);
        AccessibilityNodeInfo play=findByWords(root,PLAY,0);
        boolean completion=contains(n,COMPLETE);

        // First appearance of Play on a new lesson starts the lesson automatically.
        if(play!=null && playStarted==0){
            if(click(play)){playStarted=now;lastClick=now;return;}
        }

        // While Pause is visible, media is actively playing: never advance.
        if(pause!=null) return;

        // If Play/Replay reappears after we started playback, treat it as natural media end.
        boolean endedAfterPlay = playStarted>0 && now-playStarted>8000 && play!=null;
        boolean dwellFallback = playStarted==0 && now-pageStart>45000;
        if(completion || endedAfterPlay || dwellFallback){
            AccessibilityNodeInfo next=findByWords(root,NEXT,0);
            if(next!=null && click(next)){lastClick=now;return;}
        }
    }

    private void captureOcr(){
        if(ocrBusy || Build.VERSION.SDK_INT<30 || !isRunning()) return;
        ocrBusy=true;
        takeScreenshot(0,getMainExecutor(),new TakeScreenshotCallback(){
            @Override public void onSuccess(ScreenshotResult r){
                HardwareBuffer hb=r.getHardwareBuffer();
                Bitmap b=null;
                try{ b=Bitmap.wrapHardwareBuffer(hb,r.getColorSpace()); }
                catch(Exception ignored){}
                if(b==null){hb.close();ocrBusy=false;return;}
                Bitmap copy=b.copy(Bitmap.Config.ARGB_8888,false); hb.close();
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(InputImage.fromBitmap(copy,0))
                        .addOnSuccessListener(t->{CaptureStore.append(TrainingAccessibilityService.this,"OCR",t.getText());copy.recycle();ocrBusy=false;})
                        .addOnFailureListener(e->{copy.recycle();ocrBusy=false;});
            }
            @Override public void onFailure(int errorCode){ocrBusy=false;}
        });
    }

    private void startSpeech(){
        if(!SpeechRecognizer.isRecognitionAvailable(this)||checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) return;
        if(speech==null){
            speech=SpeechRecognizer.createSpeechRecognizer(this);
            speech.setRecognitionListener(new RecognitionListener(){
                @Override public void onReadyForSpeech(Bundle p){} @Override public void onBeginningOfSpeech(){} @Override public void onRmsChanged(float r){} @Override public void onBufferReceived(byte[] b){} @Override public void onEndOfSpeech(){}
                @Override public void onError(int e){h.postDelayed(TrainingAccessibilityService.this::restartSpeech,900);}
                @Override public void onResults(Bundle b){saveSpeech(b);h.postDelayed(TrainingAccessibilityService.this::restartSpeech,500);}
                @Override public void onPartialResults(Bundle b){saveSpeech(b);}
                @Override public void onEvent(int e,Bundle b){}
            });
            speechIntent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"tr-TR");
            speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);
            speechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);
        }
        try{speech.startListening(speechIntent);}catch(Exception ignored){}
    }
    private void restartSpeech(){if(isRunning()){try{if(speech!=null)speech.cancel();}catch(Exception ignored){} startSpeech();}}
    private void saveSpeech(Bundle b){if(b==null)return;ArrayList<String> a=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(a!=null&&!a.isEmpty())CaptureStore.append(this,"STT",a.get(0));}
    private void stopSpeech(){if(speech!=null){try{speech.cancel();speech.destroy();}catch(Exception ignored){}speech=null;}}

    private AccessibilityNodeInfo findByWords(AccessibilityNodeInfo node,List<String> words,int depth){
        if(node==null||depth>25)return null;
        String t=norm(nodeText(node));
        if(node.isVisibleToUser()&&node.isEnabled()&&matches(t,words))return node;
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo f=findByWords(node.getChild(i),words,depth+1);if(f!=null)return f;}
        return null;
    }
    private boolean click(AccessibilityNodeInfo n){AccessibilityNodeInfo c=n;int i=0;while(c!=null&&!c.isClickable()&&i++<6)c=c.getParent();return c!=null&&c.isEnabled()&&c.isVisibleToUser()&&c.performAction(AccessibilityNodeInfo.ACTION_CLICK);}
    private boolean matches(String t,List<String>w){if(t.isEmpty())return false;for(String x:w){String q=norm(x);if(t.equals(q)||t.startsWith(q+" ")||t.endsWith(" "+q))return true;}return false;}
    private boolean contains(String t,List<String>w){for(String x:w)if(t.contains(norm(x)))return true;return false;}
    private boolean blocked(String p){for(String b:BLOCKED)if(p.equals(b)||p.startsWith(b+"."))return true;return false;}
    private StringBuilder collect(AccessibilityNodeInfo n,StringBuilder o,int d){if(n==null||d>25||o.length()>20000)return o;String t=nodeText(n);if(!t.isEmpty())o.append(' ').append(t);for(int i=0;i<n.getChildCount();i++)collect(n.getChild(i),o,d+1);return o;}
    private String nodeText(AccessibilityNodeInfo n){List<String>p=new ArrayList<>();if(n.getText()!=null)p.add(n.getText().toString());if(n.getContentDescription()!=null)p.add(n.getContentDescription().toString());if(n.getHintText()!=null)p.add(n.getHintText().toString());return String.join(" ",p).trim();}
    private String norm(String s){String l=s==null?"":s.toLowerCase(new Locale("tr","TR")).replace('ı','i');return Normalizer.normalize(l,Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("\\s+"," ").trim();}
}
