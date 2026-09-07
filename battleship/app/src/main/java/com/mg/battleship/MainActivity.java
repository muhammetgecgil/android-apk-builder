package com.mg.battleship;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private TextView status;
    private GameView gameView;
    private Button rotateButton, randomButton, readyButton, soloButton, bluetoothButton;
    private BluetoothLink link;
    private boolean localReady=false, remoteReady=false, myTurn=false, connected=false, soloMode=false;
    private long localNonce=Math.abs(new Random().nextLong()), remoteNonce=-1;
    private final List<int[]> aiTargets=new ArrayList<>();

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        buildUi();
        ensureBluetoothPermissionAndStart();
    }

    private int dp(int v){ return Math.round(v*getResources().getDisplayMetrics().density); }

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(4,20,31));
        int base=dp(10);
        root.setPadding(base,base,base,base+dp(8));
        root.setOnApplyWindowInsetsListener((v,insets)->{
            int bottom=0, top=0;
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
                bottom=bars.bottom; top=bars.top;
            } else {
                bottom=insets.getSystemWindowInsetBottom();
                top=insets.getSystemWindowInsetTop();
            }
            v.setPadding(base,Math.max(base,top+dp(4)),base,bottom+dp(18));
            return insets;
        });

        TextView title=new TextView(this);
        title.setText("AMİRAL BATTI"); title.setTextColor(Color.WHITE); title.setTextSize(25); title.setGravity(Gravity.CENTER);
        title.setPadding(0,dp(2),0,dp(4));
        root.addView(title,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout modes=new LinearLayout(this); modes.setOrientation(LinearLayout.HORIZONTAL); modes.setGravity(Gravity.CENTER);
        soloButton=makeButton("Tek Oyuncu"); bluetoothButton=makeButton("Bluetooth Rakip");
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(0,dp(52),1); mp.setMargins(dp(4),0,dp(4),dp(4));
        modes.addView(soloButton,mp); LinearLayout.LayoutParams mp2=new LinearLayout.LayoutParams(0,dp(52),1); mp2.setMargins(dp(4),0,dp(4),dp(4)); modes.addView(bluetoothButton,mp2);
        root.addView(modes,new LinearLayout.LayoutParams(-1,-2));

        status=new TextView(this);
        status.setText("Bluetooth rakip aranıyor… veya Tek Oyuncu'yu seç.");
        status.setTextColor(Color.rgb(0,200,255)); status.setTextSize(15); status.setGravity(Gravity.CENTER); status.setPadding(dp(4),dp(4),dp(4),dp(6));
        root.addView(status,new LinearLayout.LayoutParams(-1,-2));

        gameView=new GameView(); root.addView(gameView,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout controls=new LinearLayout(this); controls.setOrientation(LinearLayout.HORIZONTAL); controls.setGravity(Gravity.CENTER); controls.setPadding(0,dp(8),0,dp(2));
        rotateButton=makeButton("Döndür"); randomButton=makeButton("Rastgele"); readyButton=makeButton("Hazır");
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(54),1); cp.setMargins(dp(4),0,dp(4),0);
        controls.addView(rotateButton,cp); LinearLayout.LayoutParams cp2=new LinearLayout.LayoutParams(0,dp(54),1); cp2.setMargins(dp(4),0,dp(4),0); controls.addView(randomButton,cp2);
        LinearLayout.LayoutParams cp3=new LinearLayout.LayoutParams(0,dp(54),1); cp3.setMargins(dp(4),0,dp(4),0); controls.addView(readyButton,cp3);
        root.addView(controls,new LinearLayout.LayoutParams(-1,-2));
        setContentView(root); root.requestApplyInsets();

        soloButton.setOnClickListener(v->startSoloMode()); bluetoothButton.setOnClickListener(v->startBluetoothMode());
        rotateButton.setOnClickListener(v->{ if(!localReady){ gameView.horizontal=!gameView.horizontal; gameView.invalidate(); } });
        randomButton.setOnClickListener(v->{ if(!localReady){ gameView.randomizeFleet(); status.setText("Filo hazır. İstersen yerleşimi değiştir."); } });
        readyButton.setOnClickListener(v->setReady());
    }

    private Button makeButton(String t){ Button b=new Button(this); b.setText(t); b.setAllCaps(false); b.setTextSize(15); return b; }

    private void resetForMode(){
        localReady=remoteReady=myTurn=false; gameView.playing=false; gameView.gameOver=false; gameView.resetBoard();
        readyButton.setEnabled(true); rotateButton.setEnabled(true); randomButton.setEnabled(true); aiTargets.clear();
    }

    private void startSoloMode(){
        soloMode=true; if(link!=null){ link.close(); link=null; } connected=false; resetForMode(); gameView.randomizeAiFleet();
        status.setText("TEK OYUNCU — Gemilerini yerleştir. Hazır olunca başla.");
    }

    private void startBluetoothMode(){ soloMode=false; resetForMode(); status.setText("Bluetooth rakip aranıyor…"); ensureBluetoothPermissionAndStart(); }

    private void ensureBluetoothPermissionAndStart(){
        if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},42);
        else startBluetooth();
    }

    @Override public void onRequestPermissionsResult(int req,String[] p,int[] g){
        super.onRequestPermissionsResult(req,p,g);
        if(req==42 && g.length>0 && g[0]==PackageManager.PERMISSION_GRANTED){ if(!soloMode) startBluetooth(); }
        else status.setText("Bluetooth izni verilmedi. Tek Oyuncu modu kullanılabilir.");
    }

    private void startBluetooth(){
        if(soloMode) return; if(link!=null) link.close();
        BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
        if(adapter==null){ status.setText("Bluetooth yok. Tek Oyuncu kullanılabilir."); return; }
        if(!adapter.isEnabled()){ status.setText("Bluetooth kapalı. Tek Oyuncu kullanılabilir."); return; }
        status.setText("Rakip aranıyor… Aynı APK diğer telefonda açık olsun.");
        link=new BluetoothLink(adapter,new BluetoothLink.Listener(){
            @Override public void onConnected(String name){ main.post(()->{ if(soloMode)return; connected=true; status.setText(name+" bağlandı. Gemilerini yerleştir."); link.send("HELLO|"+localNonce); }); }
            @Override public void onMessage(String msg){ main.post(()->{ if(!soloMode) handleMessage(msg); }); }
            @Override public void onLost(){ main.post(()->{ if(soloMode)return; connected=false; localReady=remoteReady=myTurn=false; status.setText("Bağlantı koptu. Rakip yeniden aranıyor…"); readyButton.setEnabled(true); rotateButton.setEnabled(true); randomButton.setEnabled(true); startBluetooth(); }); }
            @Override public void onInfo(String s){ main.post(()->{ if(!connected&&!soloMode) status.setText(s); }); }
        }); link.start();
    }

    private void setReady(){
        if(!gameView.hasCompleteFleet()){ status.setText("Önce tüm gemileri yerleştir veya Rastgele'ye bas."); return; }
        localReady=true; readyButton.setEnabled(false); rotateButton.setEnabled(false); randomButton.setEnabled(false);
        if(soloMode){ gameView.playing=true; gameView.gameOver=false; myTurn=true; gameView.invalidate(); status.setText("Sıra sende — yapay zekâ filosuna ateş et."); return; }
        if(!connected){ localReady=false; readyButton.setEnabled(true); rotateButton.setEnabled(true); randomButton.setEnabled(true); status.setText("Bluetooth rakip henüz bağlanmadı."); return; }
        link.send("READY"); if(remoteReady) beginBluetoothGame(); else status.setText("Hazırsın. Rakip bekleniyor…");
    }

    private void beginBluetoothGame(){ myTurn=localNonce>remoteNonce; gameView.playing=true; gameView.invalidate(); status.setText(myTurn?"Sıra sende — rakip denize ateş et.":"Rakibin sırası…"); }

    private void handleMessage(String msg){
        String[] a=msg.split("\\|"); if(a.length==0)return;
        switch(a[0]){
            case "HELLO": if(a.length>1)try{remoteNonce=Long.parseLong(a[1]);}catch(Exception ignored){} if(localReady&&remoteReady)beginBluetoothGame(); break;
            case "READY": remoteReady=true; if(localReady)beginBluetoothGame(); else status.setText("Rakip hazır. Gemilerini yerleştir."); break;
            case "SHOT": if(a.length>=3){ int r=Integer.parseInt(a[1]),c=Integer.parseInt(a[2]); int result=gameView.receiveShot(r,c); link.send("RESULT|"+r+"|"+c+"|"+result); if(gameView.allShipsSunk()){link.send("GAMEOVER");status.setText("Tüm gemilerin battı.");myTurn=false;gameView.gameOver=true;}else{myTurn=true;status.setText("Sıra sende — ateş et.");}} break;
            case "RESULT": if(a.length>=4){int r=Integer.parseInt(a[1]),c=Integer.parseInt(a[2]),result=Integer.parseInt(a[3]);gameView.enemyMarks[r][c]=result;gameView.invalidate();status.setText(result==2?"VURDUN! Rakibin sırası…":"Iskaladın. Rakibin sırası…");} break;
            case "GAMEOVER": myTurn=false; gameView.gameOver=true; gameView.invalidate(); status.setText("KAZANDIN! Rakibin tüm gemileri battı."); break;
        }
    }

    private void soloPlayerShot(int r,int c){
        if(gameView.enemyMarks[r][c]!=0){status.setText("Bu kareye zaten ateş ettin.");return;}
        int result=gameView.receiveAiShot(r,c); gameView.enemyMarks[r][c]=result; gameView.invalidate();
        if(gameView.allAiShipsSunk()){myTurn=false;gameView.gameOver=true;status.setText("KAZANDIN! Yapay zekânın tüm gemileri battı.");return;}
        myTurn=false; status.setText(result==2?"VURDUN! Yapay zekâ hedef alıyor…":"Iskaladın. Yapay zekâ hedef alıyor…"); main.postDelayed(this::performAiShot,650);
    }

    private void performAiShot(){
        if(!soloMode||gameView.gameOver)return; int[] shot=pickAiShot(); if(shot==null)return;
        int r=shot[0],c=shot[1],result=gameView.receiveShot(r,c); if(result==2)addAiNeighbors(r,c);
        if(gameView.allShipsSunk()){gameView.gameOver=true;myTurn=false;status.setText("YAPAY ZEKÂ KAZANDI — Tüm gemilerin battı.");}
        else{myTurn=true;status.setText(result==2?"Yapay zekâ gemini VURDU! Sıra sende.":"Yapay zekâ ıskaladı. Sıra sende.");}
        gameView.invalidate();
    }

    private int[] pickAiShot(){
        while(!aiTargets.isEmpty()){int[] p=aiTargets.remove(0);if(p[0]>=0&&p[0]<10&&p[1]>=0&&p[1]<10&&!gameView.hit[p[0]][p[1]])return p;}
        List<int[]> free=new ArrayList<>(); for(int r=0;r<10;r++)for(int c=0;c<10;c++)if(!gameView.hit[r][c])free.add(new int[]{r,c});
        return free.isEmpty()?null:free.get(random.nextInt(free.size()));
    }
    private void addAiNeighbors(int r,int c){int[][]d={{-1,0},{1,0},{0,-1},{0,1}};for(int[]x:d){int nr=r+x[0],nc=c+x[1];if(nr>=0&&nr<10&&nc>=0&&nc<10&&!gameView.hit[nr][nc])aiTargets.add(new int[]{nr,nc});}}

    private class GameView extends View{
        final int[] lengths={5,4,3,3,2};
        final int[][] own=new int[10][10], aiFleet=new int[10][10], enemyMarks=new int[10][10];
        final boolean[][] hit=new boolean[10][10], aiHit=new boolean[10][10];
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); int placed=0; boolean horizontal=true,playing=false,gameOver=false;
        GameView(){super(MainActivity.this);resetBoard();setBackgroundColor(Color.rgb(4,20,31));}

        void resetBoard(){for(int r=0;r<10;r++)for(int c=0;c<10;c++){own[r][c]=-1;aiFleet[r][c]=-1;enemyMarks[r][c]=0;hit[r][c]=false;aiHit[r][c]=false;}placed=0;invalidate();}
        boolean hasCompleteFleet(){return placed==lengths.length;}
        void randomizeFleet(){for(int r=0;r<10;r++)for(int c=0;c<10;c++){own[r][c]=-1;hit[r][c]=false;enemyMarks[r][c]=0;}placed=0;for(int id=0;id<lengths.length;id++){boolean ok=false;for(int t=0;t<500&&!ok;t++){boolean h=random.nextBoolean();int r=random.nextInt(10),c=random.nextInt(10);if(canPlace(own,r,c,lengths[id],h)){place(own,r,c,lengths[id],h,id);ok=true;}}}horizontal=true;invalidate();}
        void randomizeAiFleet(){for(int r=0;r<10;r++)for(int c=0;c<10;c++){aiFleet[r][c]=-1;aiHit[r][c]=false;enemyMarks[r][c]=0;}for(int id=0;id<lengths.length;id++){boolean ok=false;for(int t=0;t<500&&!ok;t++){boolean h=random.nextBoolean();int r=random.nextInt(10),c=random.nextInt(10);if(canPlace(aiFleet,r,c,lengths[id],h)){placeRaw(aiFleet,r,c,lengths[id],h,id);ok=true;}}}invalidate();}
        boolean canPlace(int[][]b,int r,int c,int len,boolean h){if(h&&c+len>10)return false;if(!h&&r+len>10)return false;for(int i=0;i<len;i++)if(b[r+(h?0:i)][c+(h?i:0)]!=-1)return false;return true;}
        void place(int[][]b,int r,int c,int len,boolean h,int id){placeRaw(b,r,c,len,h,id);if(b==own)placed=Math.max(placed,id+1);}
        void placeRaw(int[][]b,int r,int c,int len,boolean h,int id){for(int i=0;i<len;i++)b[r+(h?0:i)][c+(h?i:0)]=id;}
        int receiveShot(int r,int c){if(r<0||r>9||c<0||c>9)return 1;hit[r][c]=true;invalidate();return own[r][c]>=0?2:1;}
        int receiveAiShot(int r,int c){aiHit[r][c]=true;return aiFleet[r][c]>=0?2:1;}
        boolean allShipsSunk(){for(int r=0;r<10;r++)for(int c=0;c<10;c++)if(own[r][c]>=0&&!hit[r][c])return false;return true;}
        boolean allAiShipsSunk(){for(int r=0;r<10;r++)for(int c=0;c<10;c++)if(aiFleet[r][c]>=0&&!aiHit[r][c])return false;return true;}

        @Override protected void onDraw(Canvas c){
            super.onDraw(c); float w=getWidth(),h=getHeight(),margin=dp(18); float maxByHeight=playing?(h-dp(125))/2f:(h-dp(80));
            float gridW=Math.min(w-margin*2,maxByHeight); gridW=Math.max(dp(220),gridW); float cell=gridW/10f,left=(w-gridW)/2f,top1=dp(28),top2=top1+gridW+dp(58);
            p.setShader(null);p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.WHITE);p.setTextSize(dp(12));
            c.drawText(playing?"SENİN DENİZİN":"FİLONU YERLEŞTİR",w/2,dp(16),p); drawGrid(c,left,top1,cell,true);
            if(playing){c.drawText(soloMode?"YAPAY ZEKÂ DENİZİ":"RAKİP DENİZİ",w/2,top2-dp(10),p);drawGrid(c,left,top2,cell,false);}
            else{p.setTextSize(dp(10));p.setColor(Color.rgb(125,215,255));String s=placed<lengths.length?"Sıradaki gemi: "+lengths[placed]+" kare — "+(horizontal?"yatay":"dikey"):"Tüm gemiler yerleşti";c.drawText(s,w/2,top1+gridW+dp(20),p);}
        }

        private void drawGrid(Canvas c,float left,float top,float cell,boolean mine){
            p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(7,71,95));c.drawRect(left,top,left+cell*10,top+cell*10,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,cell*.025f));p.setColor(Color.rgb(8,30,42));
            for(int i=0;i<=10;i++){c.drawLine(left+i*cell,top,left+i*cell,top+10*cell,p);c.drawLine(left,top+i*cell,left+10*cell,top+i*cell,p);} p.setStyle(Paint.Style.FILL);
            if(mine){for(int id=0;id<lengths.length;id++)drawShip(c,left,top,cell,id);}
            for(int r=0;r<10;r++)for(int col=0;col<10;col++){
                boolean fired=mine?hit[r][col]:enemyMarks[r][col]!=0; if(!fired)continue; boolean wasHit=mine?own[r][col]>=0:enemyMarks[r][col]==2;
                float cx=left+(col+.5f)*cell,cy=top+(r+.5f)*cell;
                p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(wasHit?Color.rgb(255,72,50):Color.rgb(185,230,245));c.drawCircle(cx,cy,cell*(wasHit?.23f:.11f),p);
                if(wasHit){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(cell*.07f);p.setColor(Color.rgb(255,190,45));c.drawCircle(cx,cy,cell*.33f,p);p.setStyle(Paint.Style.FILL);}
            }
        }

        private void drawShip(Canvas c,float left,float top,float cell,int id){
            int minR=99,maxR=-1,minC=99,maxC=-1;for(int r=0;r<10;r++)for(int col=0;col<10;col++)if(own[r][col]==id){minR=Math.min(minR,r);maxR=Math.max(maxR,r);minC=Math.min(minC,col);maxC=Math.max(maxC,col);}if(maxR<0)return;
            boolean h=minR==maxR; float x0=left+minC*cell,y0=top+minR*cell,x1=left+(maxC+1)*cell,y1=top+(maxR+1)*cell;
            float cx=(x0+x1)/2f,cy=(y0+y1)/2f; c.save(); if(!h)c.rotate(90,cx,cy);
            float L=h?(x1-x0):(y1-y0),W=cell*.72f; float sx=cx-L/2f,ex=cx+L/2f,sy=cy-W/2f,ey=cy+W/2f;
            Path hull=new Path(); hull.moveTo(sx+cell*.12f,sy+W*.18f); hull.lineTo(ex-cell*.42f,sy+W*.08f); hull.lineTo(ex,cy); hull.lineTo(ex-cell*.42f,ey-W*.08f); hull.lineTo(sx+cell*.12f,ey-W*.18f); hull.lineTo(sx,cy); hull.close();
            p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(sx,sy,ex,ey,new int[]{Color.rgb(72,83,91),Color.rgb(145,157,162),Color.rgb(58,68,76)},null,Shader.TileMode.CLAMP));c.drawPath(hull,p);p.setShader(null);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(cell*.04f);p.setColor(Color.rgb(215,225,228));c.drawPath(hull,p);p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(48,58,63));RectF deck=new RectF(sx+L*.27f,cy-W*.18f,sx+L*.70f,cy+W*.18f);c.drawRoundRect(deck,W*.10f,W*.10f,p);
            p.setColor(Color.rgb(177,187,190));RectF bridge=new RectF(sx+L*.43f,cy-W*.28f,sx+L*.58f,cy+W*.28f);c.drawRoundRect(bridge,W*.08f,W*.08f,p);
            int turrets=id==0?3:(id==1?2:1);p.setColor(Color.rgb(32,40,44));for(int t=0;t<turrets;t++){float tx=sx+L*(.18f+(t+1f)/(turrets+2f)*.62f);c.drawCircle(tx,cy,W*.12f,p);p.setStrokeWidth(W*.06f);p.setStyle(Paint.Style.STROKE);c.drawLine(tx,cy,tx+W*.28f,cy,p);p.setStyle(Paint.Style.FILL);}
            p.setColor(Color.rgb(215,225,228));p.setStrokeWidth(cell*.025f);p.setStyle(Paint.Style.STROKE);c.drawLine(sx+L*.18f,cy,sx+L*.82f,cy,p);p.setStyle(Paint.Style.FILL);
            c.restore();
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;float w=getWidth(),h=getHeight(),margin=dp(18);float maxByHeight=playing?(h-dp(125))/2f:(h-dp(80));float gridW=Math.min(w-margin*2,maxByHeight);gridW=Math.max(dp(220),gridW);float cell=gridW/10f,left=(w-gridW)/2f,top1=dp(28),top2=top1+gridW+dp(58),x=e.getX(),y=e.getY();
            if(!playing){int col=(int)((x-left)/cell),r=(int)((y-top1)/cell);if(r>=0&&r<10&&col>=0&&col<10&&placed<lengths.length){if(canPlace(own,r,col,lengths[placed],horizontal)){place(own,r,col,lengths[placed],horizontal,placed);invalidate();}else status.setText("Bu konuma gemi sığmıyor.");}}
            else if(!gameOver&&myTurn){int col=(int)((x-left)/cell),r=(int)((y-top2)/cell);if(r>=0&&r<10&&col>=0&&col<10){if(enemyMarks[r][col]!=0){status.setText("Bu kareye zaten ateş ettin.");return true;}if(soloMode)soloPlayerShot(r,col);else{myTurn=false;status.setText("Atış gönderildi…");if(link!=null)link.send("SHOT|"+r+"|"+col);}}}return true;
        }
    }

    private static class BluetoothLink{
        interface Listener{void onConnected(String name);void onMessage(String msg);void onLost();void onInfo(String s);} private static final UUID UUID_GAME=UUID.fromString("8f7e5ab4-2a6d-4d75-9b1f-7b62d7d98810");
        private final BluetoothAdapter adapter;private final Listener listener;private final ExecutorService io=Executors.newCachedThreadPool();private volatile BluetoothSocket socket;private volatile PrintWriter out;private volatile boolean closed=false;
        BluetoothLink(BluetoothAdapter a,Listener l){adapter=a;listener=l;} void start(){io.execute(this::listenServer);io.execute(this::connectBonded);} private void listenServer(){try{BluetoothServerSocket server=adapter.listenUsingRfcommWithServiceRecord("MG Battleship",UUID_GAME);BluetoothSocket s=server.accept();server.close();attach(s);}catch(Exception ignored){}}
        private void connectBonded(){try{Set<BluetoothDevice> devices=adapter.getBondedDevices();if(devices.isEmpty()){listener.onInfo("Telefonları önce sistem Bluetooth ayarından eşleştir.");return;}for(BluetoothDevice d:devices){if(socket!=null||closed)return;try{listener.onInfo("Rakip aranıyor: "+d.getName());BluetoothSocket s=d.createRfcommSocketToServiceRecord(UUID_GAME);s.connect();attach(s);return;}catch(Exception ignored){}}listener.onInfo("Eşleştirilmiş cihazlarda açık Amiral Battı bulunamadı.");}catch(Exception e){listener.onInfo("Bluetooth bağlantısı kurulamadı.");}}
        private synchronized void attach(BluetoothSocket s)throws Exception{if(socket!=null){try{s.close();}catch(Exception ignored){}return;}socket=s;out=new PrintWriter(s.getOutputStream(),true);listener.onConnected(s.getRemoteDevice().getName());io.execute(()->readLoop(s));}
        private void readLoop(BluetoothSocket s){try(BufferedReader br=new BufferedReader(new InputStreamReader(s.getInputStream()))){String line;while((line=br.readLine())!=null)listener.onMessage(line);}catch(Exception ignored){}if(!closed)listener.onLost();}
        void send(String msg){PrintWriter w=out;if(w!=null){w.println(msg);w.flush();}} void close(){closed=true;try{if(socket!=null)socket.close();}catch(Exception ignored){}io.shutdownNow();}
    }
    @Override protected void onDestroy(){super.onDestroy();if(link!=null)link.close();}
}
