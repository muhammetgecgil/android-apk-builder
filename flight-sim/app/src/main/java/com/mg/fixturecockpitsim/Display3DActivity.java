package com.mg.fixturecockpitsim;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Display3DActivity extends Activity {
    private static final UUID SIM_UUID = UUID.fromString("6d9b6c72-4d47-4d8e-9b58-b5e7465b4a22");
    private static final int REQ_BT = 61;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private volatile boolean running = true, connected;
    private volatile long lastPacketMs;
    private volatile float roll, pitch, yaw, throttle = 0.62f, linkHz;
    private volatile int lastSeq, drops;
    private volatile long previousRxMs;
    private BluetoothAdapter bt;
    private BluetoothServerSocket server;
    private BluetoothSocket socket;
    private BufferedWriter writer;
    private Jet3DView jetView;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        bt = BluetoothAdapter.getDefaultAdapter();
        jetView = new Jet3DView(this);
        FrameLayout root = new FrameLayout(this);
        root.addView(jetView, new FrameLayout.LayoutParams(-1,-1));
        Button back = new Button(this); back.setText("MOD"); back.setAllCaps(false);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(dp(96),dp(48), Gravity.TOP|Gravity.RIGHT); bp.setMargins(0,10,10,0); root.addView(back,bp);
        back.setOnClickListener(v -> finish());
        setContentView(root);
        requestBtThenStart();
    }

    private void requestBtThenStart(){
        if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN},REQ_BT);
        } else startServer();
    }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==REQ_BT && grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) startServer();
        else Toast.makeText(this,"Bluetooth izni gerekli",Toast.LENGTH_LONG).show();
    }

    private void startServer(){
        if(bt==null){Toast.makeText(this,"Bluetooth donanımı yok",Toast.LENGTH_LONG).show();return;}
        if(!bt.isEnabled()){startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));Toast.makeText(this,"Bluetooth'u aç, sonra bu moda tekrar gir",Toast.LENGTH_LONG).show();return;}
        io.execute(() -> {
            while(running){
                try{
                    server=bt.listenUsingRfcommWithServiceRecord("FixtureCockpit3D",SIM_UUID);
                    runOnUiThread(() -> Toast.makeText(this,"3D uçak ekranı hazır — pilot bağlantısı bekleniyor",Toast.LENGTH_SHORT).show());
                    socket=server.accept(); connected=true; lastPacketMs=System.currentTimeMillis();
                    writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8));
                    BufferedReader r=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
                    String line;
                    while(running && (line=r.readLine())!=null){
                        String[] a=line.split(","); if(a.length<7 || !"V2".equals(a[0])) continue;
                        try{
                            int seq=Integer.parseInt(a[1]); float nr=Float.parseFloat(a[3]),np=Float.parseFloat(a[4]),ny=Float.parseFloat(a[5]),nt=Float.parseFloat(a[6]); long now=System.currentTimeMillis();
                            if(lastSeq>0 && seq>lastSeq+1)drops+=seq-lastSeq-1; lastSeq=seq;
                            if(previousRxMs>0){float dt=Math.max(1,now-previousRxMs);linkHz=linkHz+(1000f/dt-linkHz)*0.15f;}previousRxMs=now;
                            roll=approach(roll,nr,7.5f);pitch=approach(pitch,np,5f);yaw=angleLerp(yaw,ny,0.24f);throttle+=(nt-throttle)*0.20f;lastPacketMs=now;
                            jetView.setTelemetry(roll,pitch,yaw,throttle,linkHz,drops,true);
                            synchronized(this){writer.write("A,"+seq+"\n");writer.flush();}
                        }catch(Exception ignored){}
                    }
                }catch(Exception ignored){connected=false;}
                finally{closeLink();}
            }
        });
    }

    private static float approach(float c,float t,float step){float d=t-c;if(d>step)d=step;if(d<-step)d=-step;return c+d;}
    private static float angleLerp(float a,float b,float k){float d=b-a;while(d>180)d-=360;while(d<-180)d+=360;return a+d*k;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void closeLink(){try{if(socket!=null)socket.close();}catch(Exception ignored){}try{if(server!=null)server.close();}catch(Exception ignored){}socket=null;server=null;writer=null;connected=false;if(jetView!=null)jetView.setTelemetry(roll,pitch,yaw,throttle,linkHz,drops,false);}
    @Override protected void onPause(){super.onPause();jetView.onPause();}
    @Override protected void onResume(){super.onResume();jetView.onResume();}
    @Override protected void onDestroy(){running=false;closeLink();io.shutdownNow();super.onDestroy();}
}
