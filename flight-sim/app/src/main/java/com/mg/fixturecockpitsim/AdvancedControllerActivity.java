package com.mg.fixturecockpitsim;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** AVM-12.1 dedicated second-phone controller: no cockpit scenery, only aircraft controls and telemetry. */
public final class AdvancedControllerActivity extends Activity {
    private static final UUID SIM_UUID=UUID.fromString("6d9b6c72-4d47-4d8e-9b58-b5e7465b4a22");
    private static final int REQ_BT=71;
    private final ExecutorService io=Executors.newCachedThreadPool();
    private final Handler txHandler=new Handler(Looper.getMainLooper());
    private final AtomicInteger sequence=new AtomicInteger();
    private BluetoothAdapter bt;
    private BluetoothSocket socket;
    private BufferedWriter writer;
    private BufferedReader reader;
    private volatile boolean running=true,connected;

    volatile float cmdRoll,cmdPitch,cmdYaw,cmdThrottle=.08f,cmdBrake;
    volatile boolean gearDown=true;
    volatile double teleAlt,teleSpeed,teleHeading,teleRoll,telePitch,teleVs,teleCross;
    volatile boolean teleGround=true,teleCrash;
    volatile long lastTelemetryMs;
    private ControllerView controllerView;

    private final Runnable txLoop=new Runnable(){@Override public void run(){
        if(!running)return;
        if(connected)sendPacket();
        if(controllerView!=null)controllerView.invalidate();
        txHandler.postDelayed(this,20);
    }};

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        bt=BluetoothAdapter.getDefaultAdapter();
        controllerView=new ControllerView();
        setContentView(controllerView);
        requestBtIfNeeded();
        txHandler.post(txLoop);
    }

    private void requestBtIfNeeded(){
        if(Build.VERSION.SDK_INT>=31&&(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED||checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED)){
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN},REQ_BT);
        }
    }

    private boolean btAllowed(){return Build.VERSION.SDK_INT<31||checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;}

    void chooseAircraft(){
        if(bt==null){toast("Bluetooth donanımı yok");return;}
        if(!btAllowed()){requestBtIfNeeded();return;}
        if(!bt.isEnabled()){startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));toast("Bluetooth'u açıp LINK'e tekrar dokun");return;}
        Set<BluetoothDevice> set=bt.getBondedDevices();
        if(set==null||set.isEmpty()){toast("Önce iki telefonu Android Bluetooth ayarından eşleştir");return;}
        ArrayList<BluetoothDevice> devices=new ArrayList<>(set);
        String[] names=new String[devices.size()];
        for(int i=0;i<devices.size();i++){BluetoothDevice d=devices.get(i);names[i]=(d.getName()==null?"Cihaz":d.getName())+"\n"+d.getAddress();}
        new AlertDialog.Builder(this).setTitle("Uçak ekranı telefonunu seç").setItems(names,(d,which)->connectTo(devices.get(which))).show();
    }

    private void connectTo(BluetoothDevice device){
        toast("Data link kuruluyor…");
        io.execute(()->{
            closeSocket();
            try{
                BluetoothSocket s=device.createRfcommSocketToServiceRecord(SIM_UUID);
                s.connect();socket=s;
                writer=new BufferedWriter(new OutputStreamWriter(s.getOutputStream(),StandardCharsets.UTF_8));
                reader=new BufferedReader(new InputStreamReader(s.getInputStream(),StandardCharsets.UTF_8));
                connected=true;
                runOnUiThread(()->toast("REMOTE FLIGHT CONTROL AKTİF"));
                readTelemetry();
            }catch(Exception e){connected=false;runOnUiThread(()->toast("Bağlantı başarısız: "+e.getClass().getSimpleName()));}
        });
    }

    private void readTelemetry(){
        try{
            String line;
            while(running&&connected&&reader!=null&&(line=reader.readLine())!=null){
                if(!line.startsWith("T3,"))continue;
                String[] a=line.split(",");
                if(a.length<12)continue;
                try{
                    teleAlt=Double.parseDouble(a[2]);teleSpeed=Double.parseDouble(a[3]);teleHeading=Double.parseDouble(a[4]);
                    teleRoll=Double.parseDouble(a[5]);telePitch=Double.parseDouble(a[6]);teleVs=Double.parseDouble(a[7]);
                    teleGround="1".equals(a[8]);teleCross=Double.parseDouble(a[9]);teleCrash="1".equals(a[10]);
                    lastTelemetryMs=System.currentTimeMillis();
                }catch(Exception ignored){}
            }
        }catch(Exception ignored){}finally{connected=false;}
    }

    private void sendPacket(){
        BufferedWriter w=writer;if(w==null)return;
        int seq=sequence.incrementAndGet();long now=System.currentTimeMillis();
        String msg=String.format(Locale.US,"V3,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%d,1\n",seq,now,cmdRoll,cmdPitch,cmdYaw,cmdThrottle,cmdBrake,gearDown?1:0);
        io.execute(()->{try{synchronized(this){if(writer!=null){writer.write(msg);writer.flush();}}}catch(Exception e){connected=false;}});
    }

    void centerAxes(){cmdRoll=cmdPitch=cmdYaw=0;}
    void toggleGear(){gearDown=!gearDown;}
    void setBrake(float b){cmdBrake=Math.max(0,Math.min(1,b));}
    void setThrottle(float t){cmdThrottle=Math.max(0,Math.min(1,t));}

    private void closeSocket(){try{if(socket!=null)socket.close();}catch(Exception ignored){}socket=null;writer=null;reader=null;connected=false;}
    @Override protected void onDestroy(){running=false;txHandler.removeCallbacksAndMessages(null);closeSocket();io.shutdownNow();super.onDestroy();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    private final class ControllerView extends View {
        private final Paint p=new Paint(3),line=new Paint(3);private final Path path=new Path();
        private final RectF linkR=new RectF(),gearR=new RectF(),brakeR=new RectF(),centerR=new RectF(),throttleR=new RectF(),rudderR=new RectF();
        private int stickPid=-1,throttlePid=-1,rudderPid=-1,brakePid=-1;
        private float stickCx,stickCy,stickRadius;

        ControllerView(){super(AdvancedControllerActivity.this);p.setTypeface(Typeface.create("sans",Typeface.NORMAL));line.setStyle(Paint.Style.STROKE);line.setStrokeCap(Paint.Cap.ROUND);setBackgroundColor(Color.rgb(4,9,13));}

        @Override protected void onDraw(Canvas c){
            int w=getWidth(),h=getHeight();
            p.setShader(new LinearGradient(0,0,w,h,new int[]{0xff07131d,0xff0d222a,0xff071116},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
            drawHeader(c,w,h);drawTelemetry(c,w,h);drawThrottle(c,w,h);drawRudder(c,w,h);drawStick(c,w,h);drawButtons(c,w,h);drawFooter(c,w,h);
            if(teleCrash)drawCrashBanner(c,w,h);
        }

        private void drawHeader(Canvas c,int w,int h){
            p.setColor(0xff9dffd0);p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(Math.max(20,w*.022f));c.drawText("ADVANCED FLIGHT CONTROLLER",w*.025f,h*.070f,p);
            p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextSize(Math.max(12,w*.011f));p.setColor(connected?0xff68f5a0:0xffffb35c);c.drawText(connected?"● DATA LINK ACTIVE / 50 Hz":"● DISCONNECTED — LINK ile uçak telefonunu seç",w*.026f,h*.112f,p);
            linkR.set(w*.82f,h*.025f,w*.975f,h*.115f);button(c,linkR,connected?"LINKED":"LINK",connected?0xff153c2e:0xff4b3320);
        }

        private void drawTelemetry(Canvas c,int w,int h){
            float x=w*.20f,y=h*.035f,ww=w*.57f,hh=h*.13f;
            p.setColor(0x80202e34);c.drawRoundRect(x,y,x+ww,y+hh,12,12,p);
            String[] labels={"ALT","SPD","HDG","ROLL","PITCH","V/S","X-TRK"};
            String[] values={String.format(Locale.US,"%.0f m",teleAlt),String.format(Locale.US,"%.0f m/s",teleSpeed),String.format(Locale.US,"%03.0f°",teleHeading),String.format(Locale.US,"%.0f°",teleRoll),String.format(Locale.US,"%.0f°",telePitch),String.format(Locale.US,"%.1f",teleVs),String.format(Locale.US,"%.0f m",teleCross)};
            float cell=ww/labels.length;
            for(int i=0;i<labels.length;i++){
                p.setTextAlign(Paint.Align.CENTER);p.setColor(0xff7897a0);p.setTextSize(Math.max(10,w*.008f));c.drawText(labels[i],x+cell*(i+.5f),y+hh*.35f,p);
                p.setColor(0xffe5f2f4);p.setTypeface(Typeface.create("monospace",Typeface.BOLD));p.setTextSize(Math.max(14,w*.012f));c.drawText(values[i],x+cell*(i+.5f),y+hh*.73f,p);p.setTypeface(Typeface.create("sans",Typeface.NORMAL));
            }
            p.setTextAlign(Paint.Align.LEFT);
        }

        private void drawThrottle(Canvas c,int w,int h){
            throttleR.set(w*.035f,h*.22f,w*.13f,h*.78f);
            p.setColor(0xff101e24);c.drawRoundRect(throttleR,18,18,p);line.setColor(0xff52646b);line.setStrokeWidth(2);c.drawRoundRect(throttleR,18,18,line);
            float y=throttleR.bottom-(throttleR.height()*cmdThrottle);
            p.setColor(0xff183540);c.drawRect(throttleR.centerX()-6,throttleR.top+22,throttleR.centerX()+6,throttleR.bottom-22,p);
            p.setColor(cmdThrottle>.92f?0xffff623f:0xff70d6ff);c.drawRoundRect(throttleR.left+10,y-18,throttleR.right-10,y+18,10,10,p);
            p.setTextAlign(Paint.Align.CENTER);p.setColor(0xffdbe8eb);p.setTextSize(Math.max(13,w*.010f));c.drawText("THROTTLE",throttleR.centerX(),throttleR.top-14,p);
            p.setTypeface(Typeface.create("monospace",Typeface.BOLD));p.setTextSize(Math.max(18,w*.014f));c.drawText(String.format(Locale.US,"%03.0f%%",cmdThrottle*100),throttleR.centerX(),throttleR.bottom+28,p);p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextAlign(Paint.Align.LEFT);
        }

        private void drawRudder(Canvas c,int w,int h){
            rudderR.set(w*.22f,h*.70f,w*.58f,h*.84f);
            p.setColor(0xff101e24);c.drawRoundRect(rudderR,18,18,p);line.setColor(0xff536870);line.setStrokeWidth(2);c.drawRoundRect(rudderR,18,18,line);
            float cx=rudderR.centerX(),knob=cx+cmdYaw*rudderR.width()*.43f;
            line.setColor(0xff789aa4);line.setStrokeWidth(4);c.drawLine(rudderR.left+18,rudderR.centerY(),rudderR.right-18,rudderR.centerY(),line);
            p.setColor(0xff71d5f8);c.drawCircle(knob,rudderR.centerY(),Math.min(rudderR.height()*.28f,18),p);
            p.setTextAlign(Paint.Align.CENTER);p.setColor(0xffdbe8eb);p.setTextSize(Math.max(12,w*.009f));c.drawText("RUDDER / NOSE-WHEEL STEERING",cx,rudderR.top-10,p);p.setTextAlign(Paint.Align.LEFT);
        }

        private void drawStick(Canvas c,int w,int h){
            stickCx=w*.77f;stickCy=h*.58f;stickRadius=Math.min(w*.175f,h*.32f);
            p.setColor(0xff101e24);c.drawCircle(stickCx,stickCy,stickRadius,p);line.setColor(0xff536a73);line.setStrokeWidth(3);c.drawCircle(stickCx,stickCy,stickRadius,line);
            line.setColor(0x705ea5b7);line.setStrokeWidth(1.5f);c.drawLine(stickCx-stickRadius,stickCy,stickCx+stickRadius,stickCy,line);c.drawLine(stickCx,stickCy-stickRadius,stickCx,stickCy+stickRadius,line);
            float kx=stickCx+cmdRoll*stickRadius*.82f,ky=stickCy-cmdPitch*stickRadius*.82f;
            p.setColor(0xff76dbff);c.drawCircle(kx,ky,Math.max(18,stickRadius*.13f),p);p.setColor(0x5576dbff);c.drawCircle(kx,ky,Math.max(30,stickRadius*.22f),p);
            p.setTextAlign(Paint.Align.CENTER);p.setColor(0xffe4f1f4);p.setTextSize(Math.max(13,w*.010f));c.drawText("PITCH / ROLL CONTROL STICK",stickCx,stickCy-stickRadius-15,p);p.setTextAlign(Paint.Align.LEFT);
        }

        private void drawButtons(Canvas c,int w,int h){
            gearR.set(w*.15f,h*.30f,w*.29f,h*.43f);brakeR.set(w*.15f,h*.48f,w*.29f,h*.62f);centerR.set(w*.15f,h*.87f,w*.32f,h*.965f);
            button(c,gearR,gearDown?"GEAR DOWN":"GEAR UP",gearDown?0xff17452d:0xff333a43);
            button(c,brakeR,cmdBrake>.5f?"WHEEL BRAKE 100%":"WHEEL BRAKE",cmdBrake>.5f?0xff6d2b22:0xff353126);
            button(c,centerR,"CENTER CONTROLS",0xff203844);
        }

        private void drawFooter(Canvas c,int w,int h){
            p.setColor(0xff72868d);p.setTextSize(Math.max(10,w*.008f));
            c.drawText("Stick bırakılınca PITCH/ROLL sıfıra döner • Rudder bırakılınca merkezler • Throttle konumunu korur • BRAKE basılı-tut",w*.035f,h*.975f,p);
            String mode=teleGround?"GROUND CONTROL / NWS":"FLIGHT CONTROL";p.setTextAlign(Paint.Align.RIGHT);p.setColor(0xff9dffd0);c.drawText(mode,w*.975f,h*.975f,p);p.setTextAlign(Paint.Align.LEFT);
        }

        private void drawCrashBanner(Canvas c,int w,int h){
            p.setColor(0xcc7a180f);c.drawRoundRect(w*.34f,h*.20f,w*.66f,h*.32f,18,18,p);p.setColor(Color.WHITE);p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(Math.max(22,w*.022f));c.drawText("AIRCRAFT CRASH",w*.5f,h*.275f,p);p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextAlign(Paint.Align.LEFT);
        }

        private void button(Canvas c,RectF r,String text,int color){
            p.setColor(color);c.drawRoundRect(r,14,14,p);line.setColor(0xff62747b);line.setStrokeWidth(2);c.drawRoundRect(r,14,14,line);p.setColor(0xffeef5f6);p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(Math.max(12,getWidth()*.010f));c.drawText(text,r.centerX(),r.centerY()+5,p);p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextAlign(Paint.Align.LEFT);
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            int action=e.getActionMasked();int index=e.getActionIndex();int pid=e.getPointerId(index);
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
                float x=e.getX(index),y=e.getY(index);
                if(linkR.contains(x,y)){chooseAircraft();return true;}
                if(gearR.contains(x,y)){toggleGear();invalidate();return true;}
                if(centerR.contains(x,y)){centerAxes();invalidate();return true;}
                if(brakeR.contains(x,y)){brakePid=pid;setBrake(1);invalidate();return true;}
                if(throttleR.contains(x,y)){throttlePid=pid;updateThrottle(y);return true;}
                float dx=x-stickCx,dy=y-stickCy;if(dx*dx+dy*dy<=stickRadius*stickRadius*1.25f){stickPid=pid;updateStick(x,y);return true;}
                if(rudderR.contains(x,y)){rudderPid=pid;updateRudder(x);return true;}
            }else if(action==MotionEvent.ACTION_MOVE){
                for(int i=0;i<e.getPointerCount();i++){
                    int id=e.getPointerId(i);float x=e.getX(i),y=e.getY(i);
                    if(id==stickPid)updateStick(x,y);else if(id==throttlePid)updateThrottle(y);else if(id==rudderPid)updateRudder(x);
                }
                return true;
            }else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP||action==MotionEvent.ACTION_CANCEL){
                if(pid==stickPid){stickPid=-1;cmdRoll=cmdPitch=0;}
                if(pid==rudderPid){rudderPid=-1;cmdYaw=0;}
                if(pid==brakePid){brakePid=-1;setBrake(0);}
                if(pid==throttlePid)throttlePid=-1;
                invalidate();return true;
            }
            return true;
        }

        private void updateStick(float x,float y){
            float dx=(x-stickCx)/(stickRadius*.82f),dy=(stickCy-y)/(stickRadius*.82f);float m=(float)Math.sqrt(dx*dx+dy*dy);if(m>1){dx/=m;dy/=m;}cmdRoll=cl(dx);cmdPitch=cl(dy);invalidate();
        }
        private void updateThrottle(float y){setThrottle((throttleR.bottom-y)/Math.max(1,throttleR.height()));invalidate();}
        private void updateRudder(float x){cmdYaw=cl((x-rudderR.centerX())/(rudderR.width()*.43f));invalidate();}
        private float cl(float v){return Math.max(-1,Math.min(1,v));}
    }
}
