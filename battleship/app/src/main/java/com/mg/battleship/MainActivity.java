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
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
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
    private Button rotateButton, randomButton, readyButton;
    private BluetoothLink link;
    private boolean localReady = false, remoteReady = false, myTurn = false, connected = false;
    private long localNonce = Math.abs(new Random().nextLong()), remoteNonce = -1;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 42);
        } else startBluetooth();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18, 18, 18, 18);
        root.setBackgroundColor(Color.rgb(7,19,31));

        TextView title = new TextView(this);
        title.setText("AMİRAL BATTI");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0,8,0,4);
        root.addView(title, new LinearLayout.LayoutParams(-1,-2));

        status = new TextView(this);
        status.setText("Bluetooth hazırlanıyor…");
        status.setTextColor(Color.rgb(0,194,255));
        status.setTextSize(15);
        status.setGravity(Gravity.CENTER);
        status.setPadding(8,8,8,10);
        root.addView(status, new LinearLayout.LayoutParams(-1,-2));

        gameView = new GameView();
        root.addView(gameView, new LinearLayout.LayoutParams(-1,0,1f));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        rotateButton = makeButton("Döndür");
        randomButton = makeButton("Rastgele");
        readyButton = makeButton("Hazır");
        controls.addView(rotateButton, new LinearLayout.LayoutParams(0,-2,1));
        controls.addView(randomButton, new LinearLayout.LayoutParams(0,-2,1));
        controls.addView(readyButton, new LinearLayout.LayoutParams(0,-2,1));
        root.addView(controls, new LinearLayout.LayoutParams(-1,-2));
        setContentView(root);

        rotateButton.setOnClickListener(v -> { gameView.horizontal = !gameView.horizontal; gameView.invalidate(); });
        randomButton.setOnClickListener(v -> { if (!localReady) { gameView.randomizeFleet(); status.setText("Filo hazır. İstersen yerleşimi değiştir."); } });
        readyButton.setOnClickListener(v -> setReady());
    }

    private Button makeButton(String t) {
        Button b = new Button(this);
        b.setText(t);
        b.setAllCaps(false);
        b.setTextSize(14);
        return b;
    }

    @Override public void onRequestPermissionsResult(int req, String[] p, int[] g) {
        super.onRequestPermissionsResult(req,p,g);
        if (req == 42 && g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED) startBluetooth();
        else status.setText("Bluetooth izni gerekli.");
    }

    private void startBluetooth() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) { status.setText("Bu cihaz Bluetooth desteklemiyor."); return; }
        if (!adapter.isEnabled()) { status.setText("Bluetooth kapalı. Telefon ayarlarından aç."); return; }
        status.setText("Rakip aranıyor… Aynı APK diğer telefonda açık olsun.");
        link = new BluetoothLink(adapter, new BluetoothLink.Listener() {
            @Override public void onConnected(String name) { main.post(() -> {
                connected = true;
                status.setText(name + " bağlandı. Gemilerini yerleştir.");
                link.send("HELLO|" + localNonce);
            }); }
            @Override public void onMessage(String msg) { main.post(() -> handleMessage(msg)); }
            @Override public void onLost() { main.post(() -> {
                connected = false; localReady = remoteReady = false; myTurn = false;
                status.setText("Bağlantı koptu. Rakip yeniden aranıyor…");
                readyButton.setEnabled(true); rotateButton.setEnabled(true); randomButton.setEnabled(true);
                startBluetooth();
            }); }
            @Override public void onInfo(String s) { main.post(() -> { if (!connected) status.setText(s); }); }
        });
        link.start();
    }

    private void setReady() {
        if (!connected) { status.setText("Önce Bluetooth rakibi bağlansın."); return; }
        if (!gameView.hasCompleteFleet()) { status.setText("Önce tüm gemileri yerleştir veya Rastgele'ye bas."); return; }
        localReady = true;
        readyButton.setEnabled(false); rotateButton.setEnabled(false); randomButton.setEnabled(false);
        link.send("READY");
        if (remoteReady) beginGame(); else status.setText("Hazırsın. Rakip bekleniyor…");
    }

    private void beginGame() {
        myTurn = localNonce > remoteNonce;
        gameView.playing = true;
        gameView.invalidate();
        status.setText(myTurn ? "Sıra sende — rakip denize ateş et." : "Rakibin sırası…");
    }

    private void handleMessage(String msg) {
        String[] a = msg.split("\\|");
        if (a.length == 0) return;
        switch (a[0]) {
            case "HELLO":
                if (a.length > 1) try { remoteNonce = Long.parseLong(a[1]); } catch (Exception ignored) {}
                if (localReady && remoteReady) beginGame();
                break;
            case "READY":
                remoteReady = true;
                if (localReady) beginGame(); else status.setText("Rakip hazır. Gemilerini yerleştir.");
                break;
            case "SHOT":
                if (a.length >= 3) {
                    int r = Integer.parseInt(a[1]), c = Integer.parseInt(a[2]);
                    int result = gameView.receiveShot(r,c);
                    link.send("RESULT|"+r+"|"+c+"|"+result);
                    if (gameView.allShipsSunk()) {
                        link.send("GAMEOVER");
                        status.setText("Tüm gemilerin battı.");
                        myTurn = false;
                    } else {
                        myTurn = true;
                        status.setText("Sıra sende — ateş et.");
                    }
                }
                break;
            case "RESULT":
                if (a.length >= 4) {
                    int r = Integer.parseInt(a[1]), c = Integer.parseInt(a[2]), result = Integer.parseInt(a[3]);
                    gameView.enemyMarks[r][c] = result;
                    gameView.invalidate();
                    status.setText(result == 2 ? "VURDUN! Rakibin sırası…" : "Iskaladın. Rakibin sırası…");
                }
                break;
            case "GAMEOVER":
                myTurn = false;
                status.setText("KAZANDIN! Rakibin tüm gemileri battı.");
                gameView.gameOver = true;
                gameView.invalidate();
                break;
        }
    }

    private class GameView extends View {
        final int[] lengths = {5,4,3,3,2};
        final int[][] own = new int[10][10];
        final int[][] enemyMarks = new int[10][10];
        final boolean[][] hit = new boolean[10][10];
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int placed = 0;
        boolean horizontal = true, playing = false, gameOver = false;

        GameView() { super(MainActivity.this); resetBoard(); setBackgroundColor(Color.rgb(7,19,31)); }

        void resetBoard() { for (int r=0;r<10;r++) for(int c=0;c<10;c++){ own[r][c]=-1; enemyMarks[r][c]=0; hit[r][c]=false; } placed=0; invalidate(); }
        boolean hasCompleteFleet() { return placed == lengths.length; }

        void randomizeFleet() {
            resetBoard();
            for (int id=0; id<lengths.length; id++) {
                boolean ok=false;
                for (int tries=0; tries<500 && !ok; tries++) {
                    horizontal = random.nextBoolean();
                    int r=random.nextInt(10), c=random.nextInt(10);
                    if (canPlace(r,c,lengths[id],horizontal)) { place(r,c,lengths[id],horizontal,id); ok=true; }
                }
            }
            horizontal = true;
            invalidate();
        }

        boolean canPlace(int r,int c,int len,boolean h) {
            if (h && c+len>10) return false; if (!h && r+len>10) return false;
            for(int i=0;i<len;i++) if(own[r+(h?0:i)][c+(h?i:0)]!=-1) return false;
            return true;
        }
        void place(int r,int c,int len,boolean h,int id) {
            for(int i=0;i<len;i++) own[r+(h?0:i)][c+(h?i:0)] = id;
            placed = Math.max(placed,id+1);
        }

        int receiveShot(int r,int c) {
            if (r<0||r>9||c<0||c>9) return 1;
            hit[r][c]=true;
            invalidate();
            return own[r][c] >= 0 ? 2 : 1;
        }
        boolean allShipsSunk() {
            for(int r=0;r<10;r++) for(int c=0;c<10;c++) if(own[r][c]>=0 && !hit[r][c]) return false;
            return true;
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w=getWidth(), h=getHeight();
            float margin=36f;
            float gridW=Math.min(w-margin*2,(h-120)/2f);
            float cell=gridW/10f;
            float left=(w-gridW)/2f;
            float top1=45f;
            float top2=playing ? top1+gridW+85f : -10000f;

            p.setTextSize(28); p.setColor(Color.WHITE); p.setTextAlign(Paint.Align.CENTER);
            c.drawText(playing?"SENİN DENİZİN":"FİLONU YERLEŞTİR",w/2,28,p);
            drawGrid(c,left,top1,cell,true);
            if (playing) {
                p.setTextSize(28); p.setColor(Color.WHITE);
                c.drawText("RAKİP DENİZİ",w/2,top2-18,p);
                drawGrid(c,left,top2,cell,false);
            } else {
                p.setTextSize(23); p.setColor(Color.rgb(120,210,255));
                String s = placed<lengths.length ? "Sıradaki gemi: " + lengths[placed] + " kare — " + (horizontal?"yatay":"dikey") : "Tüm gemiler yerleşti";
                c.drawText(s,w/2,top1+gridW+38,p);
            }
        }

        private void drawGrid(Canvas c,float left,float top,float cell,boolean mine) {
            p.setStyle(Paint.Style.FILL);
            for(int r=0;r<10;r++) for(int col=0;col<10;col++) {
                int color=Color.rgb(15,64,90);
                if (mine && own[r][col]>=0) color=Color.rgb(95,130,145);
                if (mine && hit[r][col]) color= own[r][col]>=0 ? Color.rgb(230,65,55):Color.rgb(40,120,150);
                if (!mine && enemyMarks[r][col]==1) color=Color.rgb(40,120,150);
                if (!mine && enemyMarks[r][col]==2) color=Color.rgb(230,65,55);
                p.setColor(color);
                float x=left+col*cell, y=top+r*cell;
                c.drawRect(x+2,y+2,x+cell-2,y+cell-2,p);
            }
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(Color.rgb(90,170,205));
            c.drawRect(left,top,left+cell*10,top+cell*10,p);
            p.setStyle(Paint.Style.FILL);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction()!=MotionEvent.ACTION_UP) return true;
            float w=getWidth(), h=getHeight(), margin=36f;
            float gridW=Math.min(w-margin*2,(h-120)/2f), cell=gridW/10f, left=(w-gridW)/2f, top1=45f, top2=top1+gridW+85f;
            float x=e.getX(), y=e.getY();
            if (!playing) {
                int col=(int)((x-left)/cell), r=(int)((y-top1)/cell);
                if(r>=0&&r<10&&col>=0&&col<10&&placed<lengths.length) {
                    if(canPlace(r,col,lengths[placed],horizontal)) { place(r,col,lengths[placed],horizontal,placed); invalidate(); }
                    else status.setText("Bu konuma gemi sığmıyor.");
                }
            } else if (!gameOver && myTurn) {
                int col=(int)((x-left)/cell), r=(int)((y-top2)/cell);
                if(r>=0&&r<10&&col>=0&&col<10) {
                    if(enemyMarks[r][col]!=0) { status.setText("Bu kareye zaten ateş ettin."); return true; }
                    myTurn=false;
                    status.setText("Atış gönderildi…");
                    link.send("SHOT|"+r+"|"+col);
                }
            }
            return true;
        }
    }

    private static class BluetoothLink {
        interface Listener { void onConnected(String name); void onMessage(String msg); void onLost(); void onInfo(String s); }
        private static final UUID UUID_GAME = UUID.fromString("8f7e5ab4-2a6d-4d75-9b1f-7b62d7d98810");
        private final BluetoothAdapter adapter; private final Listener listener;
        private final ExecutorService io = Executors.newCachedThreadPool();
        private volatile BluetoothSocket socket; private volatile PrintWriter out; private volatile boolean closed=false;
        BluetoothLink(BluetoothAdapter a, Listener l){ adapter=a; listener=l; }

        void start(){
            io.execute(this::listenServer);
            io.execute(this::connectBonded);
        }
        private void listenServer(){
            try {
                BluetoothServerSocket server = adapter.listenUsingRfcommWithServiceRecord("MG Battleship",UUID_GAME);
                BluetoothSocket s = server.accept();
                server.close();
                attach(s);
            } catch(Exception ignored) { }
        }
        private void connectBonded(){
            try {
                Set<BluetoothDevice> devices = adapter.getBondedDevices();
                if(devices.isEmpty()){ listener.onInfo("Telefonları önce sistem Bluetooth ayarından eşleştir."); return; }
                for(BluetoothDevice d: devices){
                    if(socket!=null || closed) return;
                    try {
                        listener.onInfo("Rakip aranıyor: " + d.getName());
                        BluetoothSocket s=d.createRfcommSocketToServiceRecord(UUID_GAME);
                        s.connect(); attach(s); return;
                    } catch(Exception ignored) { }
                }
                listener.onInfo("Eşleştirilmiş cihazlarda açık Amiral Battı bulunamadı.");
            } catch(Exception e){ listener.onInfo("Bluetooth bağlantısı kurulamadı."); }
        }
        private synchronized void attach(BluetoothSocket s) throws Exception {
            if(socket!=null){ try{s.close();}catch(Exception ignored){} return; }
            socket=s; out=new PrintWriter(s.getOutputStream(),true);
            listener.onConnected(s.getRemoteDevice().getName());
            io.execute(() -> readLoop(s));
        }
        private void readLoop(BluetoothSocket s){
            try(BufferedReader br=new BufferedReader(new InputStreamReader(s.getInputStream()))){
                String line; while((line=br.readLine())!=null) listener.onMessage(line);
            } catch(Exception ignored) { }
            if(!closed) listener.onLost();
        }
        void send(String msg){ PrintWriter w=out; if(w!=null){ w.println(msg); w.flush(); } }
        void close(){ closed=true; try{ if(socket!=null) socket.close(); }catch(Exception ignored){} io.shutdownNow(); }
    }

    @Override protected void onDestroy(){ super.onDestroy(); if(link!=null) link.close(); }
}
