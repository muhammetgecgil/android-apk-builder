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
import android.graphics.RadialGradient;
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

/**
 * AVM-12.3 ultra-premium second-phone flight controller.
 * A dedicated HOTAS-like station with telemetry, throttle detents, NWS/rudder and precision stick.
 */
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
        for(int i=0;i<devices.size();i++){
            BluetoothDevice d=devices.get(i);
            names[i]=(d.getName()==null?"Cihaz":d.getName())+"\n"+d.getAddress();
        }
        new AlertDialog.Builder(this).setTitle("Uçuş ekranı telefonunu seç").setItems(names,(d,which)->connectTo(devices.get(which))).show();
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
                runOnUiThread(()->toast("FLIGHT CONTROL LINK ACTIVE"));
                readTelemetry();
            }catch(Exception e){
                connected=false;
                runOnUiThread(()->toast("Bağlantı başarısız: "+e.getClass().getSimpleName()));
            }
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
        io.execute(()->{
            try{
                synchronized(this){if(writer!=null){writer.write(msg);writer.flush();}}
            }catch(Exception e){connected=false;}
        });
    }

    void centerAxes(){cmdRoll=cmdPitch=cmdYaw=0;}
    void toggleGear(){gearDown=!gearDown;}
    void setBrake(float b){cmdBrake=Math.max(0,Math.min(1,b));}
    void setThrottle(float t){cmdThrottle=Math.max(0,Math.min(1,t));}

    private void closeSocket(){
        try{if(socket!=null)socket.close();}catch(Exception ignored){}
        socket=null;writer=null;reader=null;connected=false;
    }

    @Override protected void onDestroy(){
        running=false;txHandler.removeCallbacksAndMessages(null);closeSocket();io.shutdownNow();super.onDestroy();
    }

    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    private final class ControllerView extends View {
        private final Paint p=new Paint(3),line=new Paint(3),glow=new Paint(3);
        private final Path path=new Path();
        private final RectF linkR=new RectF(),gearR=new RectF(),brakeR=new RectF(),centerR=new RectF();
        private final RectF throttleR=new RectF(),rudderR=new RectF(),idleR=new RectF(),milR=new RectF(),maxR=new RectF();
        private int stickPid=-1,throttlePid=-1,rudderPid=-1,brakePid=-1;
        private float stickCx,stickCy,stickRadius;

        ControllerView(){
            super(AdvancedControllerActivity.this);
            p.setTypeface(Typeface.create("sans",Typeface.NORMAL));
            line.setStyle(Paint.Style.STROKE);line.setStrokeCap(Paint.Cap.ROUND);
            setBackgroundColor(Color.rgb(2,7,11));
        }

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            int w=getWidth(),h=getHeight();
            drawBackground(c,w,h);
            drawHeader(c,w,h);
            drawTelemetryDeck(c,w,h);
            drawThrottleModule(c,w,h);
            drawSystemsModule(c,w,h);
            drawRudderModule(c,w,h);
            drawStickModule(c,w,h);
            drawFooter(c,w,h);
            if(teleCrash)drawCrashBanner(c,w,h);
        }

        private void drawBackground(Canvas c,int w,int h){
            p.setShader(new LinearGradient(0,0,w,h,new int[]{0xff02070b,0xff07171f,0xff061117,0xff02070b},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p);p.setShader(null);

            // Very subtle technical grid / panel seams.
            line.setColor(0x121cc8df);line.setStrokeWidth(1);
            float step=Math.max(36,w*.045f);
            for(float x=0;x<w;x+=step)c.drawLine(x,0,x,h,line);
            for(float y=0;y<h;y+=step)c.drawLine(0,y,w,y,line);

            p.setShader(new RadialGradient(w*.73f,h*.53f,w*.40f,new int[]{0x222f8fa8,0x070e4552,0x00000000},null,Shader.TileMode.CLAMP));
            c.drawCircle(w*.73f,h*.53f,w*.40f,p);p.setShader(null);

            p.setColor(0x54020a0f);c.drawRect(0,0,w,h*.145f,p);
            line.setColor(0x4b63d9ef);line.setStrokeWidth(1.2f);c.drawLine(0,h*.145f,w,h*.145f,line);
        }

        private void drawHeader(Canvas c,int w,int h){
            p.setTypeface(Typeface.create("sans",Typeface.BOLD));
            p.setTextSize(Math.max(19,w*.0195f));p.setColor(0xffdffaff);
            c.drawText("FCS",w*.022f,h*.055f,p);
            p.setTextSize(Math.max(13,w*.0115f));p.setColor(0xff75dff4);
            c.drawText("REMOTE FLIGHT STATION",w*.074f,h*.055f,p);

            long age=lastTelemetryMs==0?9999:System.currentTimeMillis()-lastTelemetryMs;
            boolean fresh=connected&&age<700;
            p.setTypeface(Typeface.create("monospace",Typeface.BOLD));
            p.setTextSize(Math.max(10,w*.0084f));p.setColor(fresh?0xff7dffb2:connected?0xffffd276:0xffff976e);
            String status=fresh?"● DATALINK 50 Hz / NOMINAL":connected?"● LINKED / TELEMETRY WAIT":"● OFFLINE / LINK REQUIRED";
            c.drawText(status,w*.022f,h*.095f,p);
            p.setTypeface(Typeface.create("sans",Typeface.NORMAL));

            // Link quality bars.
            float bx=w*.305f,by=h*.083f,bw=w*.006f;
            int bars=fresh?4:connected?2:0;
            for(int i=0;i<4;i++){
                float bh=h*(.010f+.007f*i);
                p.setColor(i<bars?0xff71efab:0xff24343a);
                c.drawRoundRect(bx+i*bw*1.45f,by-bh,bx+i*bw*1.45f+bw,by,2,2,p);
            }

            linkR.set(w*.835f,h*.022f,w*.974f,h*.112f);
            premiumButton(c,linkR,connected?"LINKED":"LINK",connected?0xff123b30:0xff49301f,connected?0xff72f7ae:0xffffb46b);
        }

        private void drawTelemetryDeck(Canvas c,int w,int h){
            float left=w*.345f,top=h*.018f,right=w*.805f,bottom=h*.125f;
            panel(c,left,top,right,bottom,0x8c0d2028,0x625bc3da);
            String[] labels={"ALT","SPD","HDG","ROLL","PITCH","V/S","X-TRK"};
            String[] values={
                    String.format(Locale.US,"%.0f m",teleAlt),String.format(Locale.US,"%.0f m/s",teleSpeed),String.format(Locale.US,"%03.0f°",teleHeading),
                    String.format(Locale.US,"%+.0f°",teleRoll),String.format(Locale.US,"%+.0f°",telePitch),String.format(Locale.US,"%+.1f",teleVs),String.format(Locale.US,"%+.0f m",teleCross)
            };
            float cell=(right-left)/labels.length;
            for(int i=0;i<labels.length;i++){
                float cx=left+cell*(i+.5f);
                p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans",Typeface.BOLD));
                p.setTextSize(Math.max(8,w*.0065f));p.setColor(0xff668893);c.drawText(labels[i],cx,top+(bottom-top)*.31f,p);
                p.setTypeface(Typeface.create("monospace",Typeface.BOLD));p.setTextSize(Math.max(12,w*.0102f));p.setColor(0xffe7fbff);c.drawText(values[i],cx,top+(bottom-top)*.73f,p);
                if(i<labels.length-1){line.setColor(0x354b6c75);line.setStrokeWidth(1);c.drawLine(left+cell*(i+1),top+8,left+cell*(i+1),bottom-8,line);}
            }
            p.setTextAlign(Paint.Align.LEFT);p.setTypeface(Typeface.create("sans",Typeface.NORMAL));
        }

        private void drawThrottleModule(Canvas c,int w,int h){
            float px0=w*.018f,py0=h*.175f,px1=w*.155f,py1=h*.885f;
            panel(c,px0,py0,px1,py1,0x9b09161c,0x665f8793);
            p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);p.setColor(0xffd8f5fb);p.setTextSize(Math.max(11,w*.009f));
            c.drawText("THROTTLE",(px0+px1)*.5f,py0+h*.038f,p);
            p.setTypeface(Typeface.create("monospace",Typeface.BOLD));p.setTextSize(Math.max(17,w*.014f));
            p.setColor(cmdThrottle>.92f?0xffff8d68:0xff75e4ff);c.drawText(String.format(Locale.US,"%03.0f%%",cmdThrottle*100),(px0+px1)*.5f,py0+h*.084f,p);

            throttleR.set(px0+w*.032f,py0+h*.112f,px1-w*.032f,py1-h*.145f);
            float railX=throttleR.centerX();
            // Slot / rails.
            p.setColor(0xff061016);c.drawRoundRect(railX-w*.012f,throttleR.top,railX+w*.012f,throttleR.bottom,10,10,p);
            line.setColor(0x705d8791);line.setStrokeWidth(1.5f);c.drawRoundRect(railX-w*.012f,throttleR.top,railX+w*.012f,throttleR.bottom,10,10,line);

            for(int i=0;i<=10;i++){
                float q=i/10f,y=throttleR.bottom-throttleR.height()*q;
                line.setColor(i==9?0xa0ff7e64:0x70627e87);line.setStrokeWidth(i%5==0?2:1);
                c.drawLine(railX+w*.019f,y,railX+w*(i%5==0?.031f:.026f),y,line);
            }
            float milY=throttleR.bottom-throttleR.height()*.88f;
            line.setColor(0xb4ffca6e);line.setStrokeWidth(2);c.drawLine(throttleR.left-w*.005f,milY,throttleR.right+w*.005f,milY,line);
            p.setTextAlign(Paint.Align.RIGHT);p.setTextSize(Math.max(8,w*.0065f));p.setColor(0xffb8995f);c.drawText("MIL",throttleR.left-w*.006f,milY+4,p);
            p.setColor(0xffe37b66);c.drawText("AB",throttleR.left-w*.006f,throttleR.top+6,p);

            float y=throttleR.bottom-throttleR.height()*cmdThrottle;
            float handleW=w*.061f,handleH=h*.060f;
            p.setShader(new LinearGradient(railX-handleW*.5f,y-handleH*.5f,railX+handleW*.5f,y+handleH*.5f,new int[]{0xff1b3944,0xff62d8f4,0xff1a4959},null,Shader.TileMode.CLAMP));
            c.drawRoundRect(railX-handleW*.5f,y-handleH*.5f,railX+handleW*.5f,y+handleH*.5f,12,12,p);p.setShader(null);
            line.setColor(0xff9deeff);line.setStrokeWidth(2);c.drawRoundRect(railX-handleW*.5f,y-handleH*.5f,railX+handleW*.5f,y+handleH*.5f,12,12,line);
            line.setColor(0x80ffffff);line.setStrokeWidth(1.2f);for(int i=-2;i<=2;i++)c.drawLine(railX+i*handleW*.12f,y-handleH*.22f,railX+i*handleW*.12f,y+handleH*.22f,line);

            float by=py1-h*.105f,bh=h*.052f,gap=w*.004f,bw=(px1-px0-w*.018f-gap*2)/3f;
            idleR.set(px0+w*.009f,by,px0+w*.009f+bw,by+bh);milR.set(idleR.right+gap,by,idleR.right+gap+bw,by+bh);maxR.set(milR.right+gap,by,milR.right+gap+bw,by+bh);
            miniButton(c,idleR,"IDLE",0xff17313a);miniButton(c,milR,"MIL",0xff423821);miniButton(c,maxR,"MAX",0xff4a2822);
            p.setTextAlign(Paint.Align.LEFT);
        }

        private void drawSystemsModule(Canvas c,int w,int h){
            float x0=w*.172f,y0=h*.205f,x1=w*.335f,y1=h*.665f;
            panel(c,x0,y0,x1,y1,0x970a171d,0x5c607f89);
            p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(Math.max(10,w*.008f));p.setColor(0xff8eb6c1);c.drawText("AIRCRAFT SYSTEMS",(x0+x1)*.5f,y0+h*.038f,p);

            gearR.set(x0+w*.018f,y0+h*.075f,x1-w*.018f,y0+h*.205f);
            brakeR.set(x0+w*.018f,y0+h*.232f,x1-w*.018f,y0+h*.370f);
            premiumButton(c,gearR,gearDown?"GEAR  DOWN":"GEAR  UP",gearDown?0xff123e2a:0xff242e36,gearDown?0xff77f0a5:0xffb7cad1);
            premiumButton(c,brakeR,cmdBrake>.5f?"WHEEL BRAKE  •  ON":"WHEEL BRAKE",cmdBrake>.5f?0xff5a211c:0xff2f2920,cmdBrake>.5f?0xffff9178:0xffffd080);

            // compact annunciators
            float ay=y1-h*.060f;
            annunciator(c,x0+w*.020f,ay,"GEAR",gearDown?0xff72f0a4:0xff60757c);
            annunciator(c,x0+w*.073f,ay,"BRK",cmdBrake>.5f?0xffff8b73:0xff60757c);
            annunciator(c,x0+w*.126f,ay,"NWS",teleGround?0xff72dff6:0xff60757c);
            p.setTextAlign(Paint.Align.LEFT);
        }

        private void drawRudderModule(Canvas c,int w,int h){
            float x0=w*.175f,y0=h*.695f,x1=w*.555f,y1=h*.855f;
            panel(c,x0,y0,x1,y1,0x9809141a,0x5663828d);
            rudderR.set(x0+w*.020f,y0+h*.055f,x1-w*.020f,y1-h*.025f);
            float cx=rudderR.centerX(),cy=rudderR.centerY();
            p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(Math.max(9,w*.0075f));p.setColor(0xff9dbac3);c.drawText("RUDDER / NOSE-WHEEL STEERING",cx,y0+h*.036f,p);

            line.setColor(0xff314a53);line.setStrokeWidth(8);c.drawLine(rudderR.left,cy,rudderR.right,cy,line);
            line.setColor(0xff88d8e8);line.setStrokeWidth(2);c.drawLine(rudderR.left,cy,rudderR.right,cy,line);
            for(int i=-4;i<=4;i++){
                float x=cx+i*rudderR.width()/8f;line.setColor(i==0?0xffd4f7ff:0xff526d76);line.setStrokeWidth(i==0?2:1);c.drawLine(x,cy-h*.018f,x,cy+h*.018f,line);
            }
            float knob=cx+cmdYaw*rudderR.width()*.48f;
            glow.setShader(new RadialGradient(knob,cy,w*.025f,new int[]{0x9978e7ff,0x2278e7ff,0x0078e7ff},null,Shader.TileMode.CLAMP));c.drawCircle(knob,cy,w*.025f,glow);glow.setShader(null);
            p.setColor(0xff7be6ff);c.drawCircle(knob,cy,Math.max(9,w*.007f),p);
            p.setTypeface(Typeface.create("monospace",Typeface.BOLD));p.setTextSize(Math.max(10,w*.008f));p.setColor(0xffdffaff);c.drawText(String.format(Locale.US,"%+.0f%%",cmdYaw*100),cx,y1-h*.012f,p);
            p.setTextAlign(Paint.Align.LEFT);
        }

        private void drawStickModule(Canvas c,int w,int h){
            float x0=w*.575f,y0=h*.178f,x1=w*.982f,y1=h*.885f;
            panel(c,x0,y0,x1,y1,0x84081217,0x58648690);
            stickCx=(x0+x1)*.5f;stickCy=y0+(y1-y0)*.545f;stickRadius=Math.min((x1-x0)*.405f,(y1-y0)*.39f);

            p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(Math.max(11,w*.009f));p.setColor(0xffd5f4fa);c.drawText("FLIGHT CONTROL STICK",stickCx,y0+h*.045f,p);
            p.setTypeface(Typeface.create("monospace",Typeface.BOLD));p.setTextSize(Math.max(9,w*.0075f));p.setColor(0xff78c9d9);c.drawText(String.format(Locale.US,"PITCH %+.0f%%     ROLL %+.0f%%",cmdPitch*100,cmdRoll*100),stickCx,y0+h*.080f,p);

            // outer glow and gimbal rings
            glow.setShader(new RadialGradient(stickCx,stickCy,stickRadius*1.08f,new int[]{0x162edbf4,0x0c1f6b7c,0x00000000},null,Shader.TileMode.CLAMP));c.drawCircle(stickCx,stickCy,stickRadius*1.10f,glow);glow.setShader(null);
            p.setColor(0xff07141a);c.drawCircle(stickCx,stickCy,stickRadius,p);
            line.setStyle(Paint.Style.STROKE);line.setColor(0xff4d6b74);line.setStrokeWidth(2);c.drawCircle(stickCx,stickCy,stickRadius,line);
            line.setColor(0x71446a76);line.setStrokeWidth(1);c.drawCircle(stickCx,stickCy,stickRadius*.72f,line);c.drawCircle(stickCx,stickCy,stickRadius*.35f,line);
            line.setColor(0x7c5e8c99);line.setStrokeWidth(1.3f);c.drawLine(stickCx-stickRadius,stickCy,stickCx+stickRadius,stickCy,line);c.drawLine(stickCx,stickCy-stickRadius,stickCx,stickCy+stickRadius,line);

            // dead-zone box/cross
            float dz=stickRadius*.11f;line.setColor(0x9a91e9f5);line.setStrokeWidth(1.4f);c.drawRoundRect(stickCx-dz,stickCy-dz,stickCx+dz,stickCy+dz,5,5,line);
            p.setTextSize(Math.max(8,w*.006f));p.setColor(0xff5b7b84);c.drawText("DEAD ZONE",stickCx,stickCy+dz+h*.025f,p);

            // limit labels
            p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(Math.max(8,w*.0065f));p.setColor(0xff647f88);
            c.drawText("PITCH +",stickCx,stickCy-stickRadius-h*.018f,p);c.drawText("PITCH −",stickCx,stickCy+stickRadius+h*.030f,p);
            p.setTextAlign(Paint.Align.LEFT);c.drawText("ROLL −",stickCx-stickRadius-w*.010f,stickCy-8,p);p.setTextAlign(Paint.Align.RIGHT);c.drawText("ROLL +",stickCx+stickRadius+w*.010f,stickCy-8,p);

            float kx=stickCx+cmdRoll*stickRadius*.82f,ky=stickCy-cmdPitch*stickRadius*.82f;
            glow.setShader(new RadialGradient(kx,ky,stickRadius*.20f,new int[]{0xbb7de8ff,0x337de8ff,0x007de8ff},null,Shader.TileMode.CLAMP));c.drawCircle(kx,ky,stickRadius*.20f,glow);glow.setShader(null);
            p.setShader(new RadialGradient(kx-stickRadius*.035f,ky-stickRadius*.035f,stickRadius*.14f,new int[]{0xffe9fbff,0xff76dbf5,0xff28778d},null,Shader.TileMode.CLAMP));c.drawCircle(kx,ky,Math.max(18,stickRadius*.105f),p);p.setShader(null);
            line.setColor(0xffbaf5ff);line.setStrokeWidth(1.6f);c.drawCircle(kx,ky,Math.max(18,stickRadius*.105f),line);

            centerR.set(x0+w*.055f,y1-h*.085f,x1-w*.055f,y1-h*.022f);
            premiumButton(c,centerR,"CENTER / TRIM RESET",0xff17303a,0xff88deef);
            p.setTextAlign(Paint.Align.LEFT);
        }

        private void drawFooter(Canvas c,int w,int h){
            p.setColor(0xa5071116);c.drawRect(0,h*.912f,w,h,p);
            line.setColor(0x4459bfd2);line.setStrokeWidth(1);c.drawLine(0,h*.912f,w,h*.912f,line);

            String mode=teleCrash?"IMPACT / RESET AIRCRAFT":teleGround?"GROUND CONTROL • NWS ACTIVE":"FLIGHT CONTROL • FCS ACTIVE";
            p.setTypeface(Typeface.create("monospace",Typeface.BOLD));p.setTextSize(Math.max(9,w*.0075f));p.setColor(teleCrash?0xffff8b75:0xff7df2ae);c.drawText(mode,w*.020f,h*.951f,p);

            p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextSize(Math.max(8,w*.0065f));p.setColor(0xff6d8992);
            c.drawText("Stick ve rudder bırakılınca merkezlenir • Throttle konumunu korur • Brake basılı tutulur",w*.245f,h*.951f,p);

            p.setTextAlign(Paint.Align.RIGHT);p.setTypeface(Typeface.create("monospace",Typeface.BOLD));p.setColor(connected?0xff79f0aa:0xffffa36e);
            c.drawText(connected?"REMOTE MASTER":"NO DATALINK",w*.978f,h*.951f,p);p.setTextAlign(Paint.Align.LEFT);
        }

        private void drawCrashBanner(Canvas c,int w,int h){
            RectF r=new RectF(w*.32f,h*.16f,w*.68f,h*.285f);
            p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,new int[]{0xd9811710,0xe8440e0b},null,Shader.TileMode.CLAMP));c.drawRoundRect(r,18,18,p);p.setShader(null);
            line.setColor(0xffff8f78);line.setStrokeWidth(2);c.drawRoundRect(r,18,18,line);
            p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setColor(Color.WHITE);p.setTextSize(Math.max(19,w*.016f));c.drawText("AIRCRAFT IMPACT",r.centerX(),r.top+r.height()*.48f,p);
            p.setTextSize(Math.max(10,w*.008f));p.setColor(0xffffc8bd);c.drawText("Flight controls inhibited until aircraft reset",r.centerX(),r.top+r.height()*.75f,p);p.setTextAlign(Paint.Align.LEFT);
        }

        private void panel(Canvas c,float l,float t,float r,float b,int fillColor,int strokeColor){
            p.setColor(0x25000000);c.drawRoundRect(l+5,t+7,r+5,b+7,18,18,p);
            p.setColor(fillColor);c.drawRoundRect(l,t,r,b,18,18,p);
            line.setColor(strokeColor);line.setStrokeWidth(1.2f);c.drawRoundRect(l,t,r,b,18,18,line);
            line.setColor(0x25ffffff);line.setStrokeWidth(1);c.drawLine(l+18,t+1,r-18,t+1,line);
        }

        private void premiumButton(Canvas c,RectF r,String text,int fillColor,int accent){
            p.setColor(0x48000000);c.drawRoundRect(r.left+4,r.top+5,r.right+4,r.bottom+5,13,13,p);
            p.setShader(new LinearGradient(r.left,r.top,r.left,r.bottom,new int[]{lighten(fillColor,22),fillColor,darken(fillColor,18)},null,Shader.TileMode.CLAMP));c.drawRoundRect(r,13,13,p);p.setShader(null);
            line.setColor(accent);line.setStrokeWidth(1.4f);c.drawRoundRect(r,13,13,line);
            p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(Math.max(10,getWidth()*.008f));p.setColor(0xffedf9fb);c.drawText(text,r.centerX(),r.centerY()+5,p);p.setTextAlign(Paint.Align.LEFT);
        }

        private void miniButton(Canvas c,RectF r,String text,int color){
            p.setColor(color);c.drawRoundRect(r,8,8,p);line.setColor(0x8a7698a2);line.setStrokeWidth(1);c.drawRoundRect(r,8,8,line);
            p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(Math.max(8,getWidth()*.0062f));p.setColor(0xffd9edf1);c.drawText(text,r.centerX(),r.centerY()+4,p);p.setTextAlign(Paint.Align.LEFT);
        }

        private void annunciator(Canvas c,float x,float y,String text,int color){
            float w=getWidth()*.040f,h=getHeight()*.035f;
            RectF r=new RectF(x,y,x+w,y+h);p.setColor(0xff071015);c.drawRoundRect(r,5,5,p);line.setColor(0x50637f89);line.setStrokeWidth(1);c.drawRoundRect(r,5,5,line);
            p.setColor(color);c.drawCircle(r.left+w*.18f,r.centerY(),Math.max(3,getWidth()*.0024f),p);
            p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("monospace",Typeface.BOLD));p.setTextSize(Math.max(7,getWidth()*.0058f));p.setColor(0xffc6dade);c.drawText(text,r.left+w*.61f,r.centerY()+3,p);p.setTextAlign(Paint.Align.LEFT);
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            int action=e.getActionMasked();int idx=e.getActionIndex();int pid=e.getPointerId(idx);float x=e.getX(idx),y=e.getY(idx);
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
                if(linkR.contains(x,y)){chooseAircraft();invalidate();return true;}
                if(gearR.contains(x,y)){toggleGear();invalidate();return true;}
                if(centerR.contains(x,y)){centerAxes();invalidate();return true;}
                if(idleR.contains(x,y)){setThrottle(.06f);invalidate();return true;}
                if(milR.contains(x,y)){setThrottle(.88f);invalidate();return true;}
                if(maxR.contains(x,y)){setThrottle(1f);invalidate();return true;}
                if(brakeR.contains(x,y)){brakePid=pid;setBrake(1);invalidate();return true;}
                if(throttleR.contains(x,y)){throttlePid=pid;updateThrottle(y);return true;}
                float dx=x-stickCx,dy=y-stickCy;
                if(dx*dx+dy*dy<=stickRadius*stickRadius*1.30f){stickPid=pid;updateStick(x,y);return true;}
                if(rudderR.contains(x,y)){rudderPid=pid;updateRudder(x);return true;}
            }else if(action==MotionEvent.ACTION_MOVE){
                for(int i=0;i<e.getPointerCount();i++){
                    int id=e.getPointerId(i);float mx=e.getX(i),my=e.getY(i);
                    if(id==stickPid)updateStick(mx,my);else if(id==throttlePid)updateThrottle(my);else if(id==rudderPid)updateRudder(mx);
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
            float dx=(x-stickCx)/(stickRadius*.82f),dy=(stickCy-y)/(stickRadius*.82f);
            float m=(float)Math.sqrt(dx*dx+dy*dy);if(m>1){dx/=m;dy/=m;}
            // small software dead-zone for steady neutral flight
            if(Math.abs(dx)<.035f)dx=0;if(Math.abs(dy)<.035f)dy=0;
            cmdRoll=cl(dx);cmdPitch=cl(dy);invalidate();
        }

        private void updateThrottle(float y){
            float q=(throttleR.bottom-y)/Math.max(1,throttleR.height());
            // gentle detent around MIL power
            if(Math.abs(q-.88f)<.025f)q=.88f;
            setThrottle(q);invalidate();
        }

        private void updateRudder(float x){
            float q=(x-rudderR.centerX())/(rudderR.width()*.48f);if(Math.abs(q)<.035f)q=0;cmdYaw=cl(q);invalidate();
        }

        private int lighten(int c,int a){return Color.rgb(Math.min(255,Color.red(c)+a),Math.min(255,Color.green(c)+a),Math.min(255,Color.blue(c)+a));}
        private int darken(int c,int a){return Color.rgb(Math.max(0,Color.red(c)-a),Math.max(0,Color.green(c)-a),Math.max(0,Color.blue(c)-a));}
        private float cl(float v){return Math.max(-1,Math.min(1,v));}
    }
}
