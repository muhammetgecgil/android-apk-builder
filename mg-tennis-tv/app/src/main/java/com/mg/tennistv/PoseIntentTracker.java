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
import android.view.Surface;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class PoseIntentTracker {
    public interface Listener { void onPose(TennisIntentEngine.PoseSample sample); }
    private final Activity activity; private final Listener listener;
    private HandlerThread thread; private Handler handler; private CameraDevice camera; private CameraCaptureSession session; private ImageReader reader;
    private PoseDetector detector; private final AtomicBoolean busy=new AtomicBoolean(false); private int rotationDegrees=0; private volatile boolean running=false;

    public PoseIntentTracker(Activity a, Listener l){activity=a;listener=l;}

    @SuppressLint("MissingPermission") public void start(){
        if(running||activity.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return;
        try{
            PoseDetectorOptions options=new PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build();
            detector= PoseDetection.getClient(options);
            thread=new HandlerThread("MG-Tennis-Pose");thread.start();handler=new Handler(thread.getLooper());
            CameraManager cm=(CameraManager)activity.getSystemService(Context.CAMERA_SERVICE);String id=null;CameraCharacteristics chosen=null;
            for(String s:cm.getCameraIdList()){CameraCharacteristics c=cm.getCameraCharacteristics(s);Integer f=c.get(CameraCharacteristics.LENS_FACING);if(f!=null&&f==CameraCharacteristics.LENS_FACING_FRONT){id=s;chosen=c;break;}}
            if(id==null||chosen==null)return;
            Integer sensor=chosen.get(CameraCharacteristics.SENSOR_ORIENTATION);rotationDegrees=computeRotation(sensor==null?0:sensor,true);
            reader=ImageReader.newInstance(640,480,ImageFormat.YUV_420_888,2);
            reader.setOnImageAvailableListener(r->{Image im=r.acquireLatestImage();if(im==null)return;if(!busy.compareAndSet(false,true)){im.close();return;}process(im);},handler);
            running=true;cm.openCamera(id,new CameraDevice.StateCallback(){public void onOpened(CameraDevice c){camera=c;createSession();}public void onDisconnected(CameraDevice c){c.close();camera=null;}public void onError(CameraDevice c,int e){c.close();camera=null;}},handler);
        }catch(Exception e){stop();}
    }

    private int computeRotation(int sensorOrientation,boolean front){
        int r=activity.getWindowManager().getDefaultDisplay().getRotation();int d=r==Surface.ROTATION_90?90:r==Surface.ROTATION_180?180:r==Surface.ROTATION_270?270:0;
        return front?(sensorOrientation+d)%360:(sensorOrientation-d+360)%360;
    }

    private void createSession(){try{if(camera==null||reader==null)return;camera.createCaptureSession(Collections.singletonList(reader.getSurface()),new CameraCaptureSession.StateCallback(){public void onConfigured(CameraCaptureSession s){session=s;try{CaptureRequest.Builder b=camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);b.addTarget(reader.getSurface());b.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);b.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);session.setRepeatingRequest(b.build(),null,handler);}catch(Exception ignored){}}public void onConfigureFailed(CameraCaptureSession s){}},handler);}catch(Exception ignored){}}

    private void process(Image image){
        try{
            InputImage input=InputImage.fromMediaImage(image,rotationDegrees);
            detector.process(input).addOnSuccessListener(this::handlePose).addOnCompleteListener(t->{image.close();busy.set(false);});
        }catch(Exception e){image.close();busy.set(false);}
    }

    private static float nx(PoseLandmark p,int w){return p==null?.5f:clamp(p.getPosition().x/Math.max(1f,w),0f,1f);}
    private static float ny(PoseLandmark p,int h){return p==null?.5f:clamp(p.getPosition().y/Math.max(1f,h),0f,1f);}
    private void handlePose(Pose pose){
        if(listener==null||pose==null)return;int w=rotationDegrees%180==0?640:480,h=rotationDegrees%180==0?480:640;
        PoseLandmark ls=pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER),rs=pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER),lh=pose.getPoseLandmark(PoseLandmark.LEFT_HIP),rh=pose.getPoseLandmark(PoseLandmark.RIGHT_HIP);
        PoseLandmark le=pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW),re=pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW),lw=pose.getPoseLandmark(PoseLandmark.LEFT_WRIST),rw=pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
        PoseLandmark lk=pose.getPoseLandmark(PoseLandmark.LEFT_KNEE),rk=pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE),la=pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE),ra=pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE);
        if(ls==null||rs==null||lh==null||rh==null)return;
        TennisIntentEngine.PoseSample s=new TennisIntentEngine.PoseSample();s.timeMs=System.currentTimeMillis();
        float lsx=nx(ls,w),rsx=nx(rs,w),lsy=ny(ls,h),rsy=ny(rs,h),lhx=nx(lh,w),rhx=nx(rh,w),lhy=ny(lh,h),rhy=ny(rh,h);
        s.bodyX=1f-((lsx+rsx+lhx+rhx)/4f); // front-camera mirror corrected
        s.shoulderY=(lsy+rsy)/2f;s.hipY=(lhy+rhy)/2f;s.shoulderTurn=(rsx-lsx)-(rhx-lhx);
        s.rightWristX=1f-nx(rw,w);s.rightWristY=ny(rw,h);s.leftWristX=1f-nx(lw,w);s.leftWristY=ny(lw,h);
        s.rightElbowX=1f-nx(re,w);s.rightElbowY=ny(re,h);s.leftElbowX=1f-nx(le,w);s.leftElbowY=ny(le,h);
        float kneeSpread=Math.abs(nx(lk,w)-nx(rk,w));float ankleSpread=Math.abs(nx(la,w)-nx(ra,w));s.kneeBend=clamp((ankleSpread-kneeSpread)*2f,0f,1f);s.valid=true;listener.onPose(s);
    }

    public void stop(){running=false;busy.set(false);try{if(session!=null)session.close();}catch(Exception ignored){}session=null;try{if(camera!=null)camera.close();}catch(Exception ignored){}camera=null;try{if(reader!=null)reader.close();}catch(Exception ignored){}reader=null;try{if(detector!=null)detector.close();}catch(Exception ignored){}detector=null;if(thread!=null){thread.quitSafely();thread=null;}handler=null;}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
