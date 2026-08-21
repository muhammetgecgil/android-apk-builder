package com.mg.tennistv;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import java.nio.ByteBuffer;
import java.util.*;

public class CameraMotionTracker {
    public interface Listener { void onMotion(float x, float strength, float vertical); }
    private final Activity activity;
    private final Listener listener;
    private HandlerThread thread;
    private Handler handler;
    private CameraDevice camera;
    private CameraCaptureSession session;
    private ImageReader reader;
    private byte[] prev;
    private int sampleW=0,sampleH=0;
    private volatile boolean running=false;

    public CameraMotionTracker(Activity activity, Listener listener){this.activity=activity;this.listener=listener;}

    @SuppressLint("MissingPermission")
    public void start(){
        if(running)return;
        if(activity.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return;
        try{
            thread=new HandlerThread("MG-Tennis-Vision");thread.start();handler=new Handler(thread.getLooper());
            CameraManager cm=(CameraManager)activity.getSystemService(Context.CAMERA_SERVICE);
            String id=null;
            for(String s:cm.getCameraIdList()){
                CameraCharacteristics c=cm.getCameraCharacteristics(s);
                Integer f=c.get(CameraCharacteristics.LENS_FACING);
                if(f!=null&&f==CameraCharacteristics.LENS_FACING_FRONT){id=s;break;}
            }
            if(id==null)return;
            reader=ImageReader.newInstance(320,240,ImageFormat.YUV_420_888,2);
            reader.setOnImageAvailableListener(r->{Image im=null;try{im=r.acquireLatestImage();if(im!=null)process(im);}finally{if(im!=null)im.close();}},handler);
            running=true;
            cm.openCamera(id,new CameraDevice.StateCallback(){
                @Override public void onOpened(CameraDevice c){camera=c;createSession();}
                @Override public void onDisconnected(CameraDevice c){c.close();camera=null;}
                @Override public void onError(CameraDevice c,int e){c.close();camera=null;}
            },handler);
        }catch(Exception e){stop();}
    }

    private void createSession(){
        try{
            if(camera==null||reader==null)return;
            camera.createCaptureSession(Collections.singletonList(reader.getSurface()),new CameraCaptureSession.StateCallback(){
                @Override public void onConfigured(CameraCaptureSession s){
                    session=s;
                    try{CaptureRequest.Builder b=camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);b.addTarget(reader.getSurface());b.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);b.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);session.setRepeatingRequest(b.build(),null,handler);}catch(Exception ignored){}
                }
                @Override public void onConfigureFailed(CameraCaptureSession s){}
            },handler);
        }catch(Exception ignored){}
    }

    private void process(Image image){
        Image.Plane p=image.getPlanes()[0];ByteBuffer buf=p.getBuffer();int row=p.getRowStride(),pix=p.getPixelStride();
        int w=image.getWidth(),h=image.getHeight(),step=6;int sw=w/step,sh=h/step,n=sw*sh;
        byte[] cur=new byte[n];double wx=0,wy=0,sum=0;int changed=0,k=0;
        for(int y=0;y<sh;y++)for(int x=0;x<sw;x++){
            int pos=(y*step)*row+(x*step)*pix;int v=buf.get(pos)&255;cur[k]=(byte)v;
            if(prev!=null&&prev.length==n){int d=Math.abs(v-(prev[k]&255));if(d>20){double ww=Math.min(80,d-19);sum+=ww;wx+=ww*x;wy+=ww*y;changed++;}}
            k++;
        }
        prev=cur;sampleW=sw;sampleH=sh;
        if(sum>0&&changed>8){
            float cx=(float)(wx/sum)/(Math.max(1,sw-1))*2f-1f;
            float cy=(float)(wy/sum)/(Math.max(1,sh-1))*2f-1f;
            // Front camera is mirrored for the player, so invert X to match court direction.
            cx=-cx;
            float strength=Math.min(1f,changed/(float)Math.max(1,n)*5.5f);
            if(listener!=null)listener.onMotion(cx,strength,cy);
        }
    }

    public void stop(){
        running=false;
        try{if(session!=null)session.close();}catch(Exception ignored){}session=null;
        try{if(camera!=null)camera.close();}catch(Exception ignored){}camera=null;
        try{if(reader!=null)reader.close();}catch(Exception ignored){}reader=null;
        if(thread!=null){thread.quitSafely();thread=null;}handler=null;prev=null;
    }
}
