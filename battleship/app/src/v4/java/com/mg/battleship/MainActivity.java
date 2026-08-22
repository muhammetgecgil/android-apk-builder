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
import android.graphics.Path;
import android.graphics.RectF;
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
    private final List<int[]> aiTargets = new ArrayList<>();
    private TextView status;
    private GameView gameView;
    private Button rotateButton, randomButton, readyButton;
    private BluetoothLink link;
    private boolean soloMode=false, connected=false, localReady=false, remoteReady=false, myTurn=false;
    private long localNonce=Math.abs(new Random().nextLong()), remoteNonce=-1;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        buildUi();
        ensureBluetooth();
    }

    private int dp(int v){ return Math.round(v*getResources().getDisplayMetrics().density); }

    private Button button(String s){
        Button b=new Button(this);
        b.setText(s); b.setAllCaps(false); b.setTextSize(15);
        return b;
    }

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(4,20,31));
        int base=dp(10);
        root.setPadding(base,base,base,base+dp(8));
        root.setOnApplyWindowInsetsListener((v,insets)->{
            int bottom=0,top=0;
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
                bottom=bars.bottom; top=bars.top;
            }else{
                bottom=insets.getSystemWindowInsetBottom();
                top=insets.getSystemWindowInsetTop();
            }
            v.setPadding(base,Math.max(base,top+dp(4)),base,bottom+dp(18));
            return insets;
        });

        TextView title=new TextView(this);
        title.setText("AMİRAL BATTI");
        title.setTextColor(Color.WHITE); title.setTextSize(25); title.setGravity(Gravity.CENTER);
        root.addView(title,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout modes=new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        Button solo=button("Tek Oyuncu"), bt=button("Bluetooth Rakip");
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(0,dp(52),1); mp.setMargins(dp(4),dp(4),dp(4),dp(4));
        modes.addView(solo,mp);
        LinearLayout.LayoutParams mp2=new LinearLayout.LayoutParams(0,dp(52),1); mp2.setMargins(dp(4),dp(4),dp(4),dp(4));
        modes.addView(bt,mp2);
        root.addView(modes);

        status=new TextView(this);
        status.setText("Bluetooth rakip aranıyor… veya Tek Oyuncu'yu seç.");
        status.setTextColor(Color.rgb(0,200,255)); status.setTextSize(15); status.setGravity(Gravity.CENTER);
        status.setPadding(dp(4),dp(4),dp(4),dp(6));
        root.addView(status,new LinearLayout.LayoutParams(-1,-2));

        gameView=new GameView();
        root.addView(gameView,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout controls=new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setPadding(0,dp(8),0,dp(2));
        rotateButton=button("Döndür"); randomButton=button("Rastgele"); readyButton=button("Hazır");
        for(Button b:new Button[]{rotateButton,randomButton,readyButton}){
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(54),1);
            cp.setMargins(dp(4),0,dp(4),0);
            controls.addView(b,cp);
        }
        root.addView(controls);
        setContentView(root);
        root.requestApplyInsets();

        solo.setOnClickListener(v->startSolo());
        bt.setOnClickListener(v->startBluetoothMode());
        rotateButton.setOnClickListener(v->{ if(!localReady){ gameView.horizontal=!gameView.horizontal; gameView.invalidate(); }});
        randomButton.setOnClickListener(v->{ if(!localReady){ gameView.randomizeFleet(); status.setText("Filo hazır. İstersen yerleşimi değiştir."); }});
        readyButton.setOnClickListener(v->setReady());
    }

    private void resetGame(){
        localReady=remoteReady=myTurn=false;
        gameView.playing=false; gameView.gameOver=false; gameView.resetBoard();
        readyButton.setEnabled(true); rotateButton.setEnabled(true); randomButton.setEnabled(true);
        aiTargets.clear();
    }

    private void startSolo(){
        soloMode=true;
        if(link!=null){ link.close(); link=null; }
        connected=false; resetGame(); gameView.randomizeAiFleet();
        status.setText("TEK OYUNCU — Gemilerini yerleştir. Hazır olunca başla.");
    }

    private void startBluetoothMode(){
        soloMode=false; resetGame(); status.setText("Bluetooth rakip aranıyor…"); ensureBluetooth();
    }

    private void ensureBluetooth(){
        if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},42);
        else startBluetooth();
    }

    @Override public void onRequestPermissionsResult(int req,String[] p,int[] g){
        super.onRequestPermissionsResult(req,p,g);
        if(req==42 && g.length>0 && g[0]==PackageManager.PERMISSION_GRANTED){ if(!soloMode) startBluetooth(); }
        else status.setText("Bluetooth izni verilmedi. Tek Oyuncu kullanılabilir.");
    }

    private void startBluetooth(){
        if(soloMode)return;
        if(link!=null)link.close();
        BluetoothAdapter a=BluetoothAdapter.getDefaultAdapter();
        if(a==null){ status.setText("Bluetooth yok. Tek Oyuncu kullanılabilir."); return; }
        if(!a.isEnabled()){ status.setText("Bluetooth kapalı. Tek Oyuncu kullanılabilir."); return; }
        status.setText("Rakip aranıyor… Aynı APK diğer telefonda açık olsun.");
        link=new BluetoothLink(a,new BluetoothLink.Listener(){
            public void onConnected(String n){ main.post(()->{ if(soloMode)return; connected=true; status.setText(n+" bağlandı. Gemilerini yerleştir."); link.send("HELLO|"+localNonce); });}
            public void onMessage(String m){ main.post(()->{ if(!soloMode)handleMessage(m); });}
            public void onLost(){ main.post(()->{ if(soloMode)return; connected=false; localReady=remoteReady=myTurn=false; status.setText("Bağlantı koptu. Yeniden aranıyor…"); startBluetooth(); });}
            public void onInfo(String s){ main.post(()->{ if(!connected&&!soloMode)status.setText(s); });}
        });
        link.start();
    }

    private void setReady(){
        if(!gameView.complete()){ status.setText("Önce tüm gemileri yerleştir veya Rastgele'ye bas."); return; }
        localReady=true; readyButton.setEnabled(false); rotateButton.setEnabled(false); randomButton.setEnabled(false);
        if(soloMode){
            gameView.playing=true; myTurn=true; gameView.invalidate();
            status.setText("Sıra sende — yapay zekâ filosuna ateş et."); return;
        }
        if(!connected){
            localReady=false; readyButton.setEnabled(true); rotateButton.setEnabled(true); randomButton.setEnabled(true);
            status.setText("Bluetooth rakip henüz bağlanmadı."); return;
        }
        link.send("READY");
        if(remoteReady)beginBluetooth(); else status.setText("Hazırsın. Rakip bekleniyor…");
    }

    private void beginBluetooth(){
        myTurn=localNonce>remoteNonce;
        gameView.playing=true; gameView.invalidate();
        status.setText(myTurn?"Sıra sende — ateş et.":"Rakibin sırası…");
    }

    private void handleMessage(String msg){
        String[] a=msg.split("\\|");
        if(a.length==0)return;
        try{
            switch(a[0]){
                case "HELLO":
                    if(a.length>1)remoteNonce=Long.parseLong(a[1]);
                    if(localReady&&remoteReady)beginBluetooth();
                    break;
                case "READY":
                    remoteReady=true;
                    if(localReady)beginBluetooth(); else status.setText("Rakip hazır. Gemilerini yerleştir.");
                    break;
                case "SHOT":{
                    int r=Integer.parseInt(a[1]),c=Integer.parseInt(a[2]),res=gameView.receiveShot(r,c);
                    link.send("RESULT|"+r+"|"+c+"|"+res);
                    if(gameView.allOwnSunk()){ link.send("GAMEOVER"); gameView.gameOver=true; myTurn=false; status.setText("Tüm gemilerin battı."); }
                    else{ myTurn=true; status.setText("Sıra sende — ateş et."); }
                    break;
                }
                case "RESULT":{
                    int r=Integer.parseInt(a[1]),c=Integer.parseInt(a[2]),res=Integer.parseInt(a[3]);
                    gameView.enemyMarks[r][c]=res; gameView.invalidate();
                    status.setText(res==2?"VURDUN! Rakibin sırası…":"Iskaladın. Rakibin sırası…");
                    break;
                }
                case "GAMEOVER":
                    gameView.gameOver=true; myTurn=false; gameView.invalidate(); status.setText("KAZANDIN!");
                    break;
            }
        }catch(Exception ignored){}
    }

    private void playerSoloShot(int r,int c){
        if(gameView.enemyMarks[r][c]!=0){ status.setText("Bu kareye zaten ateş ettin."); return; }
        int res=gameView.receiveAiShot(r,c); gameView.enemyMarks[r][c]=res; gameView.invalidate();
        if(gameView.allAiSunk()){ gameView.gameOver=true; myTurn=false; status.setText("KAZANDIN! Yapay zekânın filosu battı."); return; }
        myTurn=false; status.setText(res==2?"VURDUN! Yapay zekâ hedef alıyor…":"Iskaladın. Yapay zekâ hedef alıyor…");
        main.postDelayed(this::aiShot,600);
    }

    private void aiShot(){
        if(!soloMode||gameView.gameOver)return;
        int[] s=pickAi(); if(s==null)return;
        int res=gameView.receiveShot(s[0],s[1]);
        if(res==2)addNeighbors(s[0],s[1]);
        if(gameView.allOwnSunk()){ gameView.gameOver=true; myTurn=false; status.setText("YAPAY ZEKÂ KAZANDI."); }
        else{ myTurn=true; status.setText(res==2?"Yapay zekâ gemini VURDU! Sıra sende.":"Yapay zekâ ıskaladı. Sıra sende."); }
        gameView.invalidate();
    }

    private int[] pickAi(){
        while(!aiTargets.isEmpty()){
            int[] p=aiTargets.remove(0);
            if(p[0]>=0&&p[0]<10&&p[1]>=0&&p[1]<10&&!gameView.hit[p[0]][p[1]])return p;
        }
        List<int[]> free=new ArrayList<>();
        for(int r=0;r<10;r++)for(int c=0;c<10;c++)if(!gameView.hit[r][c])free.add(new int[]{r,c});
        return free.isEmpty()?null:free.get(random.nextInt(free.size()));
    }

    private void addNeighbors(int r,int c){
        int[][] d={{-1,0},{1,0},{0,-1},{0,1}};
        for(int[] q:d){int rr=r+q[0],cc=c+q[1];if(rr>=0&&rr<10&&cc>=0&&cc<10&&!gameView.hit[rr][cc])aiTargets.add(new int[]{rr,cc});}
    }

    private class GameView extends View{
        final int[] lengths={5,4,3,3,2};
        final String[] names={"UÇAK GEMİSİ","SAVAŞ GEMİSİ","KRUVAZÖR","DESTROYER","DENİZALTI"};
        final int[][] own=new int[10][10], aiFleet=new int[10][10], enemyMarks=new int[10][10];
        final boolean[][] hit=new boolean[10][10], aiHit=new boolean[10][10];
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        int placed=0; boolean horizontal=true,playing=false,gameOver=false;

        GameView(){ super(MainActivity.this); resetBoard(); setBackgroundColor(Color.rgb(4,20,31)); }

        void resetBoard(){
            for(int r=0;r<10;r++)for(int c=0;c<10;c++){own[r][c]=-1;aiFleet[r][c]=-1;enemyMarks[r][c]=0;hit[r][c]=false;aiHit[r][c]=false;}
            placed=0; invalidate();
        }
        boolean complete(){ return placed==lengths.length; }

        boolean canPlace(int[][] b,int r,int c,int len,boolean h){
            if(h&&c+len>10)return false; if(!h&&r+len>10)return false;
            for(int i=0;i<len;i++)if(b[r+(h?0:i)][c+(h?i:0)]!=-1)return false;
            return true;
        }
        void rawPlace(int[][] b,int r,int c,int len,boolean h,int id){
            for(int i=0;i<len;i++)b[r+(h?0:i)][c+(h?i:0)]=id;
        }
        void placeOwn(int r,int c,boolean h,int id){ rawPlace(own,r,c,lengths[id],h,id); placed=Math.max(placed,id+1); }

        void randomizeFleet(){
            for(int r=0;r<10;r++)for(int c=0;c<10;c++){own[r][c]=-1;hit[r][c]=false;enemyMarks[r][c]=0;}
            placed=0;
            for(int id=0;id<lengths.length;id++)for(int tries=0;tries<800;tries++){
                boolean h=random.nextBoolean();int r=random.nextInt(10),c=random.nextInt(10);
                if(canPlace(own,r,c,lengths[id],h)){placeOwn(r,c,h,id);break;}
            }
            horizontal=true; invalidate();
        }

        void randomizeAiFleet(){
            for(int r=0;r<10;r++)for(int c=0;c<10;c++){aiFleet[r][c]=-1;aiHit[r][c]=false;enemyMarks[r][c]=0;}
            for(int id=0;id<lengths.length;id++)for(int tries=0;tries<800;tries++){
                boolean h=random.nextBoolean();int r=random.nextInt(10),c=random.nextInt(10);
                if(canPlace(aiFleet,r,c,lengths[id],h)){rawPlace(aiFleet,r,c,lengths[id],h,id);break;}
            }
        }

        int receiveShot(int r,int c){hit[r][c]=true;return own[r][c]>=0?2:1;}
        int receiveAiShot(int r,int c){aiHit[r][c]=true;return aiFleet[r][c]>=0?2:1;}
        boolean allOwnSunk(){for(int r=0;r<10;r++)for(int c=0;c<10;c++)if(own[r][c]>=0&&!hit[r][c])return false;return true;}
        boolean allAiSunk(){for(int r=0;r<10;r++)for(int c=0;c<10;c++)if(aiFleet[r][c]>=0&&!aiHit[r][c])return false;return true;}

        private float[] geom(){
            float w=getWidth(),h=getHeight(),margin=dp(18);
            float max=playing?(h-dp(125))/2f:(h-dp(80));
            float gw=Math.min(w-margin*2,max); gw=Math.max(dp(220),gw);
            float cell=gw/10f,left=(w-gw)/2f,top1=dp(28),top2=top1+gw+dp(58);
            return new float[]{gw,cell,left,top1,top2};
        }

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float[] g=geom(); float gw=g[0],cell=g[1],left=g[2],top1=g[3],top2=g[4];
            p.setTextAlign(Paint.Align.CENTER);p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);p.setTextSize(dp(12));
            c.drawText(playing?"SENİN DENİZİN":"FİLONU YERLEŞTİR",getWidth()/2f,dp(16),p);
            drawGrid(c,left,top1,cell,true);
            if(playing){
                c.drawText(soloMode?"YAPAY ZEKÂ DENİZİ":"RAKİP DENİZİ",getWidth()/2f,top2-dp(10),p);
                drawGrid(c,left,top2,cell,false);
            }else{
                p.setTextSize(dp(10));p.setColor(Color.rgb(125,215,255));
                String s=placed<lengths.length?names[placed]+" — "+lengths[placed]+" kare — "+(horizontal?"yatay":"dikey"):"Tüm gemiler yerleşti";
                c.drawText(s,getWidth()/2f,top1+gw+dp(20),p);
            }
        }

        private void drawGrid(Canvas c,float left,float top,float cell,boolean mine){
            p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(7,71,95));c.drawRect(left,top,left+10*cell,top+10*cell,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,cell*.025f));p.setColor(Color.rgb(8,30,42));
            for(int i=0;i<=10;i++){c.drawLine(left+i*cell,top,left+i*cell,top+10*cell,p);c.drawLine(left,top+i*cell,left+10*cell,top+i*cell,p);}
            p.setStyle(Paint.Style.FILL);
            if(mine)for(int id=0;id<lengths.length;id++)drawShipSide(c,left,top,cell,id);
            for(int r=0;r<10;r++)for(int col=0;col<10;col++){
                boolean fired=mine?hit[r][col]:enemyMarks[r][col]!=0;if(!fired)continue;
                boolean good=mine?own[r][col]>=0:enemyMarks[r][col]==2;
                float cx=left+(col+.5f)*cell,cy=top+(r+.5f)*cell;
                p.setColor(good?Color.rgb(255,70,45):Color.rgb(210,240,250));
                c.drawCircle(cx,cy,cell*(good?.23f:.11f),p);
                if(good){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(cell*.06f);p.setColor(Color.rgb(255,190,45));c.drawCircle(cx,cy,cell*.32f,p);p.setStyle(Paint.Style.FILL);}
            }
        }

        private void drawShipSide(Canvas c,float left,float top,float cell,int id){
            int minR=99,maxR=-1,minC=99,maxC=-1;
            for(int r=0;r<10;r++)for(int col=0;col<10;col++)if(own[r][col]==id){
                minR=Math.min(minR,r);maxR=Math.max(maxR,r);minC=Math.min(minC,col);maxC=Math.max(maxC,col);
            }
            if(maxR<0)return;
            boolean h=minR==maxR;
            float x0=left+minC*cell,y0=top+minR*cell,x1=left+(maxC+1)*cell,y1=top+(maxR+1)*cell;
            float cx=(x0+x1)/2f,cy=(y0+y1)/2f,L=h?(x1-x0):(y1-y0),H=cell*.78f;
            c.save(); if(!h)c.rotate(90,cx,cy);
            float sx=cx-L/2f,ex=cx+L/2f,water=cy+H*.24f;

            Path hull=new Path();
            hull.moveTo(sx+L*.03f,water-H*.20f);
            hull.lineTo(ex-L*.10f,water-H*.20f);
            hull.lineTo(ex,water-H*.05f);
            hull.lineTo(ex-L*.08f,water+H*.20f);
            hull.lineTo(sx+L*.10f,water+H*.20f);
            hull.lineTo(sx,water+H*.05f);
            hull.close();
            p.setColor(id==4?Color.rgb(38,48,52):Color.rgb(95,108,116));p.setStyle(Paint.Style.FILL);c.drawPath(hull,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(cell*.035f);p.setColor(Color.rgb(205,220,225));c.drawPath(hull,p);p.setStyle(Paint.Style.FILL);

            if(id==0) drawCarrier(c,sx,ex,water,H,cell);
            else if(id==1) drawBattleship(c,sx,ex,water,H,cell);
            else if(id==2) drawCruiser(c,sx,ex,water,H,cell);
            else if(id==3) drawDestroyer(c,sx,ex,water,H,cell);
            else drawSubmarine(c,sx,ex,water,H,cell);

            c.restore();
        }

        private void drawCarrier(Canvas c,float sx,float ex,float water,float H,float cell){
            float L=ex-sx;
            p.setColor(Color.rgb(62,72,77));c.drawRect(sx+L*.05f,water-H*.34f,ex-L*.06f,water-H*.21f,p);
            p.setColor(Color.rgb(170,182,186));c.drawRect(sx+L*.62f,water-H*.55f,sx+L*.76f,water-H*.34f,p);
            p.setColor(Color.rgb(65,78,83));c.drawRect(sx+L*.66f,water-H*.72f,sx+L*.72f,water-H*.55f,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(cell*.025f);p.setColor(Color.WHITE);
            c.drawLine(sx+L*.12f,water-H*.275f,ex-L*.10f,water-H*.275f,p);
            for(int i=0;i<3;i++){float x=sx+L*(.24f+i*.13f);c.drawLine(x,water-H*.38f,x+cell*.18f,water-H*.31f,p);c.drawLine(x+cell*.09f,water-H*.35f,x+cell*.08f,water-H*.25f,p);}
            p.setStyle(Paint.Style.FILL);
        }
        private void drawBattleship(Canvas c,float sx,float ex,float water,float H,float cell){
            float L=ex-sx;
            p.setColor(Color.rgb(155,166,170));c.drawRect(sx+L*.40f,water-H*.50f,sx+L*.60f,water-H*.20f,p);
            p.setColor(Color.rgb(55,65,70));
            turret(c,sx+L*.18f,water-H*.26f,cell,true); turret(c,sx+L*.32f,water-H*.27f,cell,true); turret(c,sx+L*.76f,water-H*.26f,cell,false);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(cell*.035f);p.setColor(Color.rgb(210,220,223));
            c.drawLine(sx+L*.50f,water-H*.50f,sx+L*.50f,water-H*.72f,p);c.drawLine(sx+L*.44f,water-H*.64f,sx+L*.56f,water-H*.64f,p);p.setStyle(Paint.Style.FILL);
        }
        private void drawCruiser(Canvas c,float sx,float ex,float water,float H,float cell){
            float L=ex-sx;
            p.setColor(Color.rgb(150,162,166));c.drawRect(sx+L*.42f,water-H*.43f,sx+L*.58f,water-H*.20f,p);
            turret(c,sx+L*.20f,water-H*.25f,cell,true);turret(c,sx+L*.75f,water-H*.25f,cell,false);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(cell*.03f);p.setColor(Color.WHITE);
            c.drawLine(sx+L*.50f,water-H*.43f,sx+L*.50f,water-H*.68f,p);c.drawLine(sx+L*.44f,water-H*.58f,sx+L*.57f,water-H*.58f,p);p.setStyle(Paint.Style.FILL);
        }
        private void drawDestroyer(Canvas c,float sx,float ex,float water,float H,float cell){
            float L=ex-sx;
            p.setColor(Color.rgb(160,172,176));c.drawRect(sx+L*.43f,water-H*.39f,sx+L*.56f,water-H*.20f,p);
            turret(c,sx+L*.18f,water-H*.24f,cell,true);
            p.setColor(Color.rgb(45,55,60));
            for(int i=0;i<3;i++)c.drawRect(sx+L*(.62f+i*.045f),water-H*.31f,sx+L*(.65f+i*.045f),water-H*.20f,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(cell*.025f);p.setColor(Color.WHITE);
            c.drawLine(sx+L*.49f,water-H*.39f,sx+L*.49f,water-H*.65f,p);c.drawLine(sx+L*.43f,water-H*.55f,sx+L*.55f,water-H*.55f,p);p.setStyle(Paint.Style.FILL);
        }
        private void drawSubmarine(Canvas c,float sx,float ex,float water,float H,float cell){
            float L=ex-sx,mid=water-H*.03f;
            RectF body=new RectF(sx+L*.05f,mid-H*.18f,ex-L*.05f,mid+H*.18f);
            p.setColor(Color.rgb(38,48,52));c.drawOval(body,p);
            p.setColor(Color.rgb(90,100,105));c.drawRect(sx+L*.43f,mid-H*.31f,sx+L*.56f,mid-H*.10f,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(cell*.025f);p.setColor(Color.rgb(210,220,223));
            c.drawLine(sx+L*.50f,mid-H*.31f,sx+L*.50f,mid-H*.50f,p);c.drawLine(sx+L*.50f,mid-H*.50f,sx+L*.57f,mid-H*.50f,p);p.setStyle(Paint.Style.FILL);
        }
        private void turret(Canvas c,float x,float y,float cell,boolean right){
            p.setColor(Color.rgb(55,65,70));c.drawCircle(x,y,cell*.11f,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(cell*.045f);p.setColor(Color.rgb(45,52,56));
            float d=right?cell*.32f:-cell*.32f;c.drawLine(x,y,x+d,y,p);c.drawLine(x,y+cell*.05f,x+d,y+cell*.05f,p);p.setStyle(Paint.Style.FILL);
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;
            float[] g=geom();float cell=g[1],left=g[2],top1=g[3],top2=g[4],x=e.getX(),y=e.getY();
            if(!playing){
                int col=(int)((x-left)/cell),r=(int)((y-top1)/cell);
                if(r>=0&&r<10&&col>=0&&col<10&&placed<lengths.length){
                    if(canPlace(own,r,col,lengths[placed],horizontal)){placeOwn(r,col,horizontal,placed);invalidate();}
                    else status.setText("Bu konuma gemi sığmıyor.");
                }
            }else if(!gameOver&&myTurn){
                int col=(int)((x-left)/cell),r=(int)((y-top2)/cell);
                if(r>=0&&r<10&&col>=0&&col<10){
                    if(enemyMarks[r][col]!=0){status.setText("Bu kareye zaten ateş ettin.");return true;}
                    if(soloMode)playerSoloShot(r,col);
                    else{myTurn=false;status.setText("Atış gönderildi…");if(link!=null)link.send("SHOT|"+r+"|"+col);}
                }
            }
            return true;
        }
    }

    private static class BluetoothLink{
        interface Listener{void onConnected(String n);void onMessage(String m);void onLost();void onInfo(String s);}
        private static final UUID ID=UUID.fromString("8f7e5ab4-2a6d-4d75-9b1f-7b62d7d98810");
        private final BluetoothAdapter a; private final Listener l; private final ExecutorService io=Executors.newCachedThreadPool();
        private volatile BluetoothSocket socket; private volatile PrintWriter out; private volatile boolean closed=false;
        BluetoothLink(BluetoothAdapter a,Listener l){this.a=a;this.l=l;}
        void start(){io.execute(this::server);io.execute(this::connect);}
        void server(){try{BluetoothServerSocket s=a.listenUsingRfcommWithServiceRecord("MG Battleship",ID);BluetoothSocket x=s.accept();s.close();attach(x);}catch(Exception ignored){}}
        void connect(){try{Set<BluetoothDevice> ds=a.getBondedDevices();if(ds.isEmpty()){l.onInfo("Telefonları önce sistem Bluetooth ayarından eşleştir.");return;}for(BluetoothDevice d:ds){if(socket!=null||closed)return;try{BluetoothSocket x=d.createRfcommSocketToServiceRecord(ID);x.connect();attach(x);return;}catch(Exception ignored){}}l.onInfo("Eşleştirilmiş cihazlarda açık Amiral Battı bulunamadı.");}catch(Exception e){l.onInfo("Bluetooth bağlantısı kurulamadı.");}}
        synchronized void attach(BluetoothSocket s)throws Exception{if(socket!=null){s.close();return;}socket=s;out=new PrintWriter(s.getOutputStream(),true);l.onConnected(s.getRemoteDevice().getName());io.execute(()->read(s));}
        void read(BluetoothSocket s){try(BufferedReader br=new BufferedReader(new InputStreamReader(s.getInputStream()))){String line;while((line=br.readLine())!=null)l.onMessage(line);}catch(Exception ignored){}if(!closed)l.onLost();}
        void send(String m){PrintWriter w=out;if(w!=null){w.println(m);w.flush();}}
        void close(){closed=true;try{if(socket!=null)socket.close();}catch(Exception ignored){}io.shutdownNow();}
    }

    @Override protected void onDestroy(){super.onDestroy();if(link!=null)link.close();}
}
