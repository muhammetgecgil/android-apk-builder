package com.muhammetgecgil.camera3d;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.Collections;

public class MainActivity extends Activity implements SensorEventListener {
    private static final int REQ_CAMERA=70;
    private TextureView cameraView;
    private View depthOverlay;
    private CameraDevice camera;
    private CameraCaptureSession session;
    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private boolean opening=false, orientationReady=false, mode3d=true;
    private float basePitch,baseRoll,depthPx=12f;
    private TextView status;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        buildUi();
        sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
        if(sensorManager!=null) rotationSensor=sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        startCameraThread();
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.CAMERA},REQ_CAMERA);
        else tryOpen();
    }

    private void buildUi(){
        FrameLayout root=new FrameLayout(this); root.setBackgroundColor(Color.BLACK);
        cameraView=new TextureView(this);
        root.addView(cameraView,new FrameLayout.LayoutParams(-1,-1));

        depthOverlay=new View(this);
        depthOverlay.setBackgroundColor(0x1800C8FF);
        depthOverlay.setVisibility(View.VISIBLE);
        root.addView(depthOverlay,new FrameLayout.LayoutParams(-1,-1));

        cameraView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener(){
            public void onSurfaceTextureAvailable(SurfaceTexture s,int w,int h){tryOpen();}
            public void onSurfaceTextureSizeChanged(SurfaceTexture s,int w,int h){}
            public boolean onSurfaceTextureDestroyed(SurfaceTexture s){return true;}
            public void onSurfaceTextureUpdated(SurfaceTexture s){}
        });

        LinearLayout top=new LinearLayout(this); top.setOrientation(LinearLayout.VERTICAL); top.setPadding(22,18,22,18); top.setBackgroundColor(0x66000000);
        TextView title=new TextView(this); title.setText("CAMERA 3D • SAFE LIVE"); title.setTextSize(20); title.setTextColor(Color.WHITE);
        status=new TextView(this); status.setText("Kamera hazırlanıyor…"); status.setTextSize(13); status.setTextColor(0xffdddddd);
        top.addView(title); top.addView(status);
        FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP); tp.setMargins(16,40,16,0); root.addView(top,tp);

        LinearLayout controls=new LinearLayout(this); controls.setOrientation(LinearLayout.VERTICAL); controls.setPadding(22,18,22,22); controls.setBackgroundColor(0xaa111111);
        LinearLayout row=new LinearLayout(this);
        Button toggle=new Button(this); toggle.setText("3D AÇIK");
        toggle.setOnClickListener(v->{mode3d=!mode3d; toggle.setText(mode3d?"3D AÇIK":"NORMAL"); depthOverlay.setVisibility(mode3d?View.VISIBLE:View.GONE);});
        Button reset=new Button(this); reset.setText("MERKEZLE"); reset.setOnClickListener(v->{orientationReady=false; depthOverlay.setTranslationX(0); depthOverlay.setTranslationY(0);});
        row.addView(toggle,new LinearLayout.LayoutParams(0,-2,1)); row.addView(reset,new LinearLayout.LayoutParams(0,-2,1)); controls.addView(row);
        TextView dt=new TextView(this); dt.setText("3D DERİNLİK / PARALLAX"); dt.setTextColor(Color.WHITE); controls.addView(dt);
        SeekBar sb=new SeekBar(this); sb.setMax(40); sb.setProgress(12); sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){depthPx=p;} public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}}); controls.addView(sb);
        TextView hint=new TextView(this); hint.setText("Tek kamera yüzeyi kullanılır. Telefon hareketi derinlik katmanını kaydırır; Samsung Camera2 çoklu-yüzey çökmesi engellenmiştir."); hint.setTextColor(0xffcccccc); controls.addView(hint);
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM); cp.setMargins(16,0,16,40); root.addView(controls,cp);
        setContentView(root);
    }

    private void startCameraThread(){if(cameraThread!=null)return; cameraThread=new HandlerThread("camera3d"); cameraThread.start(); cameraHandler=new Handler(cameraThread.getLooper());}
    private void tryOpen(){
        if(isFinishing()||isDestroyed()||opening||camera!=null||cameraView==null||!cameraView.isAvailable())return;
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return;
        if(cameraHandler==null)startCameraThread(); openCamera();
    }
    private void openCamera(){
        try{
            CameraManager m=(CameraManager)getSystemService(Context.CAMERA_SERVICE); if(m==null)return;
            String selected=null; for(String id:m.getCameraIdList()){Integer f=m.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING); if(f!=null&&f==CameraCharacteristics.LENS_FACING_BACK){selected=id;break;}}
            if(selected==null){status.setText("Arka kamera bulunamadı");return;}
            opening=true; status.setText("Arka kamera açılıyor…");
            m.openCamera(selected,new CameraDevice.StateCallback(){
                public void onOpened(CameraDevice c){opening=false;camera=c;createPreview();}
                public void onDisconnected(CameraDevice c){opening=false;c.close();camera=null;runOnUiThread(()->status.setText("Kamera bağlantısı kesildi"));}
                public void onError(CameraDevice c,int e){opening=false;c.close();camera=null;runOnUiThread(()->status.setText("Kamera hatası: "+e));}
            },cameraHandler);
        }catch(Throwable e){opening=false;runOnUiThread(()->status.setText("Kamera açılamadı: "+e.getClass().getSimpleName()));}
    }
    private void createPreview(){
        try{
            SurfaceTexture t=cameraView.getSurfaceTexture(); if(t==null)return; t.setDefaultBufferSize(1280,720); Surface s=new Surface(t);
            CaptureRequest.Builder r=camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW); r.addTarget(s); r.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            camera.createCaptureSession(Collections.singletonList(s),new CameraCaptureSession.StateCallback(){
                public void onConfigured(CameraCaptureSession cs){session=cs;try{cs.setRepeatingRequest(r.build(),null,cameraHandler);runOnUiThread(()->status.setText("CANLI • GÜVENLİ KAMERA AKTİF"));}catch(Throwable e){runOnUiThread(()->status.setText("Önizleme hatası"));}}
                public void onConfigureFailed(CameraCaptureSession cs){runOnUiThread(()->status.setText("Kamera oturumu kurulamadı"));}
            },cameraHandler);
        }catch(Throwable e){runOnUiThread(()->status.setText("Önizleme kurulamadı: "+e.getClass().getSimpleName()));}
    }
    public void onSensorChanged(SensorEvent e){
        if(e.sensor.getType()!=Sensor.TYPE_ROTATION_VECTOR)return; float[] r=new float[9],o=new float[3]; SensorManager.getRotationMatrixFromVector(r,e.values); SensorManager.getOrientation(r,o);
        if(!orientationReady){basePitch=o[1];baseRoll=o[2];orientationReady=true;return;} if(!mode3d)return;
        float dx=clamp(o[2]-baseRoll),dy=clamp(o[1]-basePitch); depthOverlay.setTranslationX(Math.max(-depthPx,Math.min(depthPx,dx*depthPx*2))); depthOverlay.setTranslationY(Math.max(-depthPx/2,Math.min(depthPx/2,dy*depthPx)));
        float a=0.05f+Math.min(0.18f,depthPx/180f); depthOverlay.setAlpha(a);
    }
    private float clamp(float a){while(a>Math.PI)a-=2*Math.PI;while(a<-Math.PI)a+=2*Math.PI;return a;}
    public void onAccuracyChanged(Sensor s,int a){}
    protected void onResume(){super.onResume();if(sensorManager!=null&&rotationSensor!=null)sensorManager.registerListener(this,rotationSensor,SensorManager.SENSOR_DELAY_GAME);if(cameraThread==null)startCameraThread();tryOpen();}
    protected void onPause(){if(sensorManager!=null)sensorManager.unregisterListener(this);closeCamera();super.onPause();}
    private void closeCamera(){opening=false;if(session!=null){try{session.close();}catch(Throwable ignored){}session=null;}if(camera!=null){try{camera.close();}catch(Throwable ignored){}camera=null;}}
    protected void onDestroy(){closeCamera();if(cameraThread!=null){cameraThread.quitSafely();cameraThread=null;cameraHandler=null;}super.onDestroy();}
    public void onRequestPermissionsResult(int rc,String[] p,int[] g){super.onRequestPermissionsResult(rc,p,g);if(rc==REQ_CAMERA&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)tryOpen();else if(status!=null)status.setText("Kamera izni gerekli");}
}
