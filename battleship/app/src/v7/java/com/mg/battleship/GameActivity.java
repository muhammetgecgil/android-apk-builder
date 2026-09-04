package com.mg.battleship;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
import android.widget.ScrollView;
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

public class GameActivity extends Activity {
    private static final String PREF="admiral_v7";
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Random rnd=new Random();
    private final List<int[]> aiTargets=new ArrayList<>();
    private SharedPreferences prefs;
    private TextView status,rankText;
    private BoardView board;
    private Button rotate,randomize,ready;
    private BluetoothLink link;
    private boolean solo=false,career=false,connected=false,localReady=false,remoteReady=false,myTurn=false;
    private long localNonce=Math.abs(new Random().nextLong()),remoteNonce=-1;
    private int aiLevel=2,careerStage=0;

    private final String[] REGIONS={"EGE DENİZİ","AKDENİZ","ATLANTİK","KUZEY DENİZİ","PASİFİK"};
    private final String[] DIFF={"ACEMİ","DENİZCİ","KAPTAN","AMİRAL","BÜYÜK AMİRAL"};
    private final String[] RANKS={"DENİZCİ","ASTSUBAY","TEĞMEN","YÜZBAŞI","AMİRAL","FİLO AMİRALİ"};

    @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences(PREF,MODE_PRIVATE);showMainMenu();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}

    private LinearLayout shell(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(3,16,25));
        int p=dp(12);root.setPadding(p,p,p,p+dp(8));
        root.setOnApplyWindowInsetsListener((v,i)->{int bot=0,top=0;if(Build.VERSION.SDK_INT>=30){android.graphics.Insets in=i.getInsets(WindowInsets.Type.systemBars());bot=in.bottom;top=in.top;}else{bot=i.getSystemWindowInsetBottom();top=i.getSystemWindowInsetTop();}v.setPadding(p,Math.max(p,top+dp(4)),p,bot+dp(14));return i;});
        return root;
    }
    private TextView label(String t,int sp,int color){TextView v=new TextView(this);v.setText(t);v.setTextSize(sp);v.setTextColor(color);v.setGravity(Gravity.CENTER);v.setPadding(dp(4),dp(5),dp(4),dp(5));return v;}
    private Button btn(String t){Button b=new Button(this);b.setText(t);b.setAllCaps(false);b.setTextSize(16);return b;}
    private void addButton(LinearLayout root,Button b){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56));lp.setMargins(dp(8),dp(5),dp(8),dp(5));root.addView(b,lp);}

    private int wins(){return prefs.getInt("wins",0);} private int losses(){return prefs.getInt("losses",0);} private int shots(){return prefs.getInt("shots",0);} private int hits(){return prefs.getInt("hits",0);} private int xp(){return prefs.getInt("xp",0);} 
    private int rankIndex(){int x=xp();if(x>=3500)return 5;if(x>=2200)return 4;if(x>=1200)return 3;if(x>=600)return 2;if(x>=250)return 1;return 0;}
    private String rank(){return RANKS[rankIndex()];}

    private void showMainMenu(){
        closeLink();
        LinearLayout root=shell();
        TextView title=label("AMİRAL BATTI",30,Color.WHITE);title.setPadding(0,dp(18),0,0);root.addView(title);
        TextView sub=label("TACTICAL NAVAL COMMAND",12,Color.rgb(75,210,240));root.addView(sub);
        rankText=label("⚓ "+rank()+"   •   "+xp()+" XP",16,Color.rgb(255,211,92));rankText.setPadding(0,dp(10),0,dp(14));root.addView(rankText);
        Button soloB=btn("⚔  TEK OYUNCU"),btB=btn("⌁  BLUETOOTH SAVAŞI"),careerB=btn("★  DENİZ HARBİ KARİYERİ"),fleetB=btn("▰  FİLOM"),statsB=btn("◎  MUHAREBE KAYDI");
        addButton(root,soloB);addButton(root,btB);addButton(root,careerB);addButton(root,fleetB);addButton(root,statsB);
        TextView footer=label("v7 • Sinematik savaş • Kariyer • Rütbe • Gelişmiş AI",11,Color.rgb(120,145,155));LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,0,1);footer.setGravity(Gravity.CENTER|Gravity.BOTTOM);root.addView(footer,fp);
        setContentView(root);root.requestApplyInsets();
        soloB.setOnClickListener(v->showDifficulty(false));btB.setOnClickListener(v->startBattle(false,false,2));careerB.setOnClickListener(v->showCareer());fleetB.setOnClickListener(v->showFleet());statsB.setOnClickListener(v->showStats());
    }

    private void showDifficulty(boolean fromCareer){
        LinearLayout root=shell();root.addView(label("YAPAY ZEKÂ ZORLUĞU",24,Color.WHITE));root.addView(label("Rakip hile yapmaz; yalnızca taktik seviyesi değişir.",13,Color.rgb(110,205,230)));
        for(int i=0;i<DIFF.length;i++){final int x=i;Button b=btn(DIFF[i]+(i==4?"  ★":""));addButton(root,b);b.setOnClickListener(v->startBattle(true,fromCareer,x));}
        Button back=btn("← Ana Menü");addButton(root,back);back.setOnClickListener(v->showMainMenu());setContentView(root);root.requestApplyInsets();
    }

    private void showCareer(){
        LinearLayout root=shell();root.addView(label("DENİZ HARBİ KARİYERİ",24,Color.WHITE));int unlocked=prefs.getInt("career",0);
        root.addView(label("Bölge ilerlemesi: "+Math.min(unlocked+1,REGIONS.length)+" / "+REGIONS.length,14,Color.rgb(80,205,235)));
        for(int i=0;i<REGIONS.length;i++){final int x=i;boolean open=i<=unlocked;Button b=btn((open?"⚓ ":"🔒 ")+REGIONS[i]+"  •  "+DIFF[Math.min(4,i)]);b.setEnabled(open);addButton(root,b);if(open)b.setOnClickListener(v->{careerStage=x;startBattle(true,true,Math.min(4,x));});}
        Button back=btn("← Ana Menü");addButton(root,back);back.setOnClickListener(v->showMainMenu());setContentView(root);root.requestApplyInsets();
    }

    private void showFleet(){
        LinearLayout root=shell();root.addView(label("FİLOM",26,Color.WHITE));root.addView(label("Sınıflar görsel koleksiyondur; güç avantajı vermez.",13,Color.rgb(100,205,230)));
        String[] info={"UÇAK GEMİSİ  •  5 bölüm  •  Ana Filo Gemisi","SAVAŞ GEMİSİ  •  4 bölüm  •  Ağır Zırhlı","KRUVAZÖR  •  3 bölüm  •  Dengeli","DESTROYER  •  3 bölüm  •  Hızlı Taarruz","DENİZALTI  •  2 bölüm  •  Gizli Avcı"};
        for(String s:info){TextView v=label(s,16,Color.LTGRAY);v.setBackgroundColor(Color.rgb(8,36,49));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(58));lp.setMargins(dp(4),dp(5),dp(4),dp(5));root.addView(v,lp);}Button back=btn("← Ana Menü");addButton(root,back);back.setOnClickListener(v->showMainMenu());setContentView(root);root.requestApplyInsets();
    }

    private void showStats(){
        LinearLayout root=shell();root.addView(label("MUHAREBE KAYDI",26,Color.WHITE));int s=shots(),h=hits();float acc=s==0?0:100f*h/s;
        root.addView(label("RÜTBE  •  "+rank()+"\n\n"+wins()+" GALİBİYET    "+losses()+" MAĞLUBİYET\n\n"+s+" ATIŞ    "+h+" İSABET\n\n%"+String.format(java.util.Locale.US,"%.1f",acc)+" İSABET ORANI\n\n"+xp()+" TOPLAM XP",20,Color.rgb(200,225,230)),new LinearLayout.LayoutParams(-1,0,1));
        Button back=btn("← Ana Menü");addButton(root,back);back.setOnClickListener(v->showMainMenu());setContentView(root);root.requestApplyInsets();
    }

    private void startBattle(boolean soloMode,boolean careerMode,int difficulty){
        solo=soloMode;career=careerMode;aiLevel=difficulty;connected=false;localReady=remoteReady=myTurn=false;aiTargets.clear();
        LinearLayout root=shell();
        TextView top=label(career?REGIONS[careerStage]:(solo?"TEK OYUNCU • "+DIFF[aiLevel]:"BLUETOOTH SAVAŞI"),18,Color.WHITE);root.addView(top);
        status=label(solo?"Gemilerini yerleştir. Hazır olduğunda savaşı başlat.":"Bluetooth rakip aranıyor…",14,Color.rgb(65,205,240));root.addView(status);
        board=new BoardView();root.addView(board,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.HORIZONTAL);controls.setGravity(Gravity.CENTER);
        rotate=btn("Yön: Yatay");randomize=btn("Rastgele");ready=btn("Hazır");
        for(Button b:new Button[]{rotate,randomize,ready}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(54),1);lp.setMargins(dp(3),dp(4),dp(3),0);controls.addView(b,lp);}root.addView(controls);
        setContentView(root);root.requestApplyInsets();
        rotate.setOnClickListener(v->{if(!localReady){board.horizontal=!board.horizontal;rotate.setText(board.horizontal?"Yön: Yatay":"Yön: Dikey");board.invalidate();}});
        randomize.setOnClickListener(v->{if(!localReady){board.randomizeOwn();status.setText("Filo konuşlandı. Hazır olduğunda savaşı başlat.");}});
        ready.setOnClickListener(v->setReady());
        if(solo){board.randomizeAi();}else ensureBluetooth();
    }

    private void setReady(){
        if(!board.complete()){status.setText("Önce tüm filoyu yerleştir veya Rastgele'ye bas.");return;}localReady=true;ready.setEnabled(false);rotate.setEnabled(false);randomize.setEnabled(false);
        if(solo){board.playing=true;myTurn=true;status.setText("HEDEFLEME SERBEST — Sıra sende.");board.invalidate();return;}
        if(!connected){localReady=false;ready.setEnabled(true);rotate.setEnabled(true);randomize.setEnabled(true);status.setText("Rakip henüz bağlanmadı.");return;}link.send("READY");if(remoteReady)beginBluetooth();else status.setText("Filo hazır. Rakip bekleniyor…");
    }
    private void beginBluetooth(){myTurn=localNonce>remoteNonce;board.playing=true;board.invalidate();status.setText(myTurn?"Sıra sende — hedef seç.":"Rakibin hamlesi bekleniyor…");}

    private void recordShot(boolean hit){prefs.edit().putInt("shots",shots()+1).putInt("hits",hits()+(hit?1:0)).apply();}
    private void finishMatch(boolean win){
        int newXp=xp()+(win?120+aiLevel*35:25);SharedPreferences.Editor e=prefs.edit().putInt(win?"wins":"losses",(win?wins():losses())+1).putInt("xp",newXp);
        if(win&&career){int u=prefs.getInt("career",0);if(careerStage==u&&u<REGIONS.length-1)e.putInt("career",u+1);}e.apply();
        status.setText(win?"ZAFER!  +"+(120+aiLevel*35)+" XP":"FİLO KAYBEDİLDİ  +25 XP");
        main.postDelayed(()->showDebrief(win),1100);
    }
    private void showDebrief(boolean win){
        LinearLayout root=shell();root.addView(label(win?"MUHAREBE ZAFERİ":"MUHAREBE SONU",28,win?Color.rgb(255,215,85):Color.LTGRAY));
        root.addView(label("Rütbe: "+rank()+"\nXP: "+xp()+"\nGalibiyet: "+wins()+"\nİsabet: "+hits()+" / "+shots(),19,Color.WHITE),new LinearLayout.LayoutParams(-1,0,1));
        Button rematch=btn("Tekrar Savaş"),menu=btn("Ana Menü");addButton(root,rematch);addButton(root,menu);rematch.setOnClickListener(v->startBattle(solo,career,aiLevel));menu.setOnClickListener(v->showMainMenu());setContentView(root);root.requestApplyInsets();
    }

    private void playerShot(int r,int c){if(board.enemyMarks[r][c]!=0){status.setText("Bu koordinat zaten vuruldu.");return;}int res=board.receiveAiShot(r,c);board.enemyMarks[r][c]=res;recordShot(res==2);board.flash(r,c,false,res==2);if(board.allAiSunk()){board.gameOver=true;myTurn=false;finishMatch(true);return;}myTurn=false;status.setText(res==2?"VURUŞ! Yapay zekâ karşılık veriyor…":"ISKALANDI — Yapay zekâ karşılık veriyor…");main.postDelayed(this::aiShot,aiLevel>=3?420:650);}
    private void aiShot(){if(!solo||board.gameOver)return;int[] q=pickAi();if(q==null)return;int res=board.receiveOwnShot(q[0],q[1]);if(res==2)addNeighbors(q[0],q[1]);board.flash(q[0],q[1],true,res==2);if(board.allOwnSunk()){board.gameOver=true;myTurn=false;finishMatch(false);}else{myTurn=true;status.setText(res==2?"Gemimiz VURULDU! Sıra sende.":"Rakip ıskaladı. Sıra sende.");}}
    private int[] pickAi(){
        while(!aiTargets.isEmpty()){int[] p=aiTargets.remove(0);if(valid(p[0],p[1])&&!board.hit[p[0]][p[1]])return p;}
        List<int[]> free=new ArrayList<>();for(int r=0;r<10;r++)for(int c=0;c<10;c++)if(!board.hit[r][c]){
            if(aiLevel>=3){if((r+c)%2==0)free.add(new int[]{r,c});}else free.add(new int[]{r,c});}
        if(free.isEmpty())for(int r=0;r<10;r++)for(int c=0;c<10;c++)if(!board.hit[r][c])free.add(new int[]{r,c});return free.isEmpty()?null:free.get(rnd.nextInt(free.size()));
    }
    private boolean valid(int r,int c){return r>=0&&r<10&&c>=0&&c<10;}
    private void addNeighbors(int r,int c){int[][] d={{-1,0},{1,0},{0,-1},{0,1}};for(int[]x:d){int rr=r+x[0],cc=c+x[1];if(valid(rr,cc)&&!board.hit[rr][cc])aiTargets.add(new int[]{rr,cc});}}

    private void ensureBluetooth(){if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},42);else startBluetooth();}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==42&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)startBluetooth();else if(status!=null)status.setText("Bluetooth izni gerekli.");}
    private void startBluetooth(){BluetoothAdapter a=BluetoothAdapter.getDefaultAdapter();if(a==null||!a.isEnabled()){status.setText("Bluetooth kapalı veya kullanılamıyor.");return;}closeLink();link=new BluetoothLink(a,new BluetoothLink.Listener(){public void onConnected(String n){main.post(()->{connected=true;status.setText(n+" bağlandı — filonu hazırla.");link.send("HELLO|"+localNonce);});}public void onMessage(String m){main.post(()->handleBt(m));}public void onLost(){main.post(()->{connected=false;status.setText("Bağlantı koptu. Rakip yeniden aranıyor…");startBluetooth();});}public void onInfo(String s){main.post(()->{if(!connected)status.setText(s);});}});link.start();}
    private void handleBt(String m){try{String[] a=m.split("\\|");switch(a[0]){case"HELLO":remoteNonce=Long.parseLong(a[1]);break;case"READY":remoteReady=true;if(localReady)beginBluetooth();else status.setText("Rakip hazır — filonu yerleştir.");break;case"SHOT":{int r=Integer.parseInt(a[1]),c=Integer.parseInt(a[2]),res=board.receiveOwnShot(r,c);link.send("RESULT|"+r+"|"+c+"|"+res);board.flash(r,c,true,res==2);if(board.allOwnSunk()){link.send("GAMEOVER");board.gameOver=true;finishMatch(false);}else{myTurn=true;status.setText("Sıra sende — hedef seç.");}break;}case"RESULT":{int r=Integer.parseInt(a[1]),c=Integer.parseInt(a[2]),res=Integer.parseInt(a[3]);board.enemyMarks[r][c]=res;recordShot(res==2);if(res==2)BattleEffects.hit(this);else BattleEffects.miss(this);board.flash(r,c,false,res==2);status.setText(res==2?"VURUŞ! Rakibin sırası…":"ISKALANDI — Rakibin sırası…");break;}case"GAMEOVER":board.gameOver=true;finishMatch(true);break;}}catch(Exception ignored){}}
    private void closeLink(){if(link!=null){link.close();link=null;}}
    @Override public void onBackPressed(){showMainMenu();}

    private class BoardView extends View{
        final int[] len={5,4,3,3,2};final int[][] own=new int[10][10],aiFleet=new int[10][10],enemyMarks=new int[10][10];final boolean[][] hit=new boolean[10][10],aiHit=new boolean[10][10];final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);int placed=0;boolean horizontal=true,playing=false,gameOver=false;int flashR=-1,flashC=-1;boolean flashMine=false,flashHit=false;long flashUntil=0;
        BoardView(){super(GameActivity.this);reset();setBackgroundColor(Color.rgb(3,16,25));}
        void reset(){for(int r=0;r<10;r++)for(int c=0;c<10;c++){own[r][c]=-1;aiFleet[r][c]=-1;enemyMarks[r][c]=0;hit[r][c]=false;aiHit[r][c]=false;}placed=0;}
        boolean complete(){return placed==len.length;}
        boolean can(int[][]b,int r,int c,int l,boolean h){if(h&&c+l>10)return false;if(!h&&r+l>10)return false;for(int i=0;i<l;i++)if(b[r+(h?0:i)][c+(h?i:0)]!=-1)return false;return true;}
        void place(int[][]b,int r,int c,int l,boolean h,int id){for(int i=0;i<l;i++)b[r+(h?0:i)][c+(h?i:0)]=id;}
        void randomizeOwn(){for(int r=0;r<10;r++)for(int c=0;c<10;c++){own[r][c]=-1;hit[r][c]=false;enemyMarks[r][c]=0;}placed=0;for(int id=0;id<len.length;id++)for(int t=0;t<1000;t++){boolean h=rnd.nextBoolean();int r=rnd.nextInt(10),c=rnd.nextInt(10);if(can(own,r,c,len[id],h)){place(own,r,c,len[id],h,id);placed++;break;}}invalidate();}
        void randomizeAi(){for(int r=0;r<10;r++)for(int c=0;c<10;c++){aiFleet[r][c]=-1;aiHit[r][c]=false;enemyMarks[r][c]=0;}for(int id=0;id<len.length;id++)for(int t=0;t<1000;t++){boolean h=rnd.nextBoolean();int r=rnd.nextInt(10),c=rnd.nextInt(10);if(can(aiFleet,r,c,len[id],h)){place(aiFleet,r,c,len[id],h,id);break;}}}
        int receiveOwnShot(int r,int c){hit[r][c]=true;int res=own[r][c]>=0?2:1;if(res==2)BattleEffects.hit(GameActivity.this);else BattleEffects.miss(GameActivity.this);invalidate();return res;}
        int receiveAiShot(int r,int c){aiHit[r][c]=true;int res=aiFleet[r][c]>=0?2:1;if(res==2)BattleEffects.hit(GameActivity.this);else BattleEffects.miss(GameActivity.this);return res;}
        boolean allOwnSunk(){for(int r=0;r<10;r++)for(int c=0;c<10;c++)if(own[r][c]>=0&&!hit[r][c])return false;return true;}boolean allAiSunk(){for(int r=0;r<10;r++)for(int c=0;c<10;c++)if(aiFleet[r][c]>=0&&!aiHit[r][c])return false;return true;}
        void flash(int r,int c,boolean mine,boolean wasHit){flashR=r;flashC=c;flashMine=mine;flashHit=wasHit;flashUntil=System.currentTimeMillis()+500;invalidate();main.postDelayed(this::invalidate,520);}
        private float[] geo(){float w=getWidth(),h=getHeight(),m=dp(3);float max=playing?(h-dp(90))/2f:h-dp(58);float gw=Math.min(w-m*2,max);float cell=gw/10,left=(w-gw)/2,top1=dp(24),top2=top1+gw+dp(45);return new float[]{gw,cell,left,top1,top2};}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float[]g=geo();float gw=g[0],cell=g[1],left=g[2],t1=g[3],t2=g[4];p.setTextAlign(Paint.Align.CENTER);p.setTextSize(dp(11));p.setColor(Color.WHITE);p.setStyle(Paint.Style.FILL);c.drawText(playing?"SENİN DENİZİN":"FİLONU KONUŞLANDIR",getWidth()/2f,dp(14),p);drawGrid(c,left,t1,cell,true);if(playing){c.drawText(solo?"DÜŞMAN FİLOSU":"RAKİP DENİZİ",getWidth()/2f,t2-dp(8),p);drawGrid(c,left,t2,cell,false);}else{p.setColor(Color.rgb(90,210,235));p.setTextSize(dp(10));String s=placed<len.length?AdvancedShipRenderer.name(placed)+" • "+len[placed]+" bölüm • "+(horizontal?"YATAY":"DİKEY"):"FİLO HAZIR";c.drawText(s,getWidth()/2f,t1+gw+dp(18),p);}}
        private void drawGrid(Canvas c,float left,float top,float cell,boolean mine){p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(6,62,84));c.drawRect(left,top,left+10*cell,top+10*cell,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,cell*.025f));p.setColor(Color.rgb(11,30,40));for(int i=0;i<=10;i++){c.drawLine(left+i*cell,top,left+i*cell,top+10*cell,p);c.drawLine(left,top+i*cell,left+10*cell,top+i*cell,p);}p.setStyle(Paint.Style.FILL);if(mine)for(int id=0;id<len.length;id++)AdvancedShipRenderer.draw(c,p,left,top,cell,own,id);for(int r=0;r<10;r++)for(int col=0;col<10;col++){boolean fired=mine?hit[r][col]:enemyMarks[r][col]!=0;if(!fired)continue;boolean good=mine?own[r][col]>=0:enemyMarks[r][col]==2;float cx=left+(col+.5f)*cell,cy=top+(r+.5f)*cell;if(good){p.setColor(Color.rgb(255,78,40));c.drawCircle(cx,cy,cell*.24f,p);p.setColor(Color.rgb(255,210,90));c.drawCircle(cx,cy,cell*.10f,p);}else{p.setColor(Color.rgb(155,225,245));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(cell*.08f);c.drawCircle(cx,cy,cell*.18f,p);p.setStyle(Paint.Style.FILL);}}if(System.currentTimeMillis()<flashUntil&&flashMine==mine){float cx=left+(flashC+.5f)*cell,cy=top+(flashR+.5f)*cell;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(cell*.10f);p.setColor(flashHit?Color.YELLOW:Color.WHITE);c.drawCircle(cx,cy,cell*.40f,p);p.setStyle(Paint.Style.FILL);}}
        @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float[]g=geo();float cell=g[1],left=g[2],t1=g[3],t2=g[4];if(!playing&&!localReady){int c=(int)((e.getX()-left)/cell),r=(int)((e.getY()-t1)/cell);if(valid(r,c)&&placed<len.length&&can(own,r,c,len[placed],horizontal)){place(own,r,c,len[placed],horizontal,placed);placed++;invalidate();}return true;}if(playing&&myTurn&&!gameOver){int c=(int)((e.getX()-left)/cell),r=(int)((e.getY()-t2)/cell);if(valid(r,c)){myTurn=false;if(solo)playerShot(r,c);else if(connected){link.send("SHOT|"+r+"|"+c);status.setText("Atış gönderildi — sonuç bekleniyor…");}}}return true;}
    }

    private static class BluetoothLink{
        interface Listener{void onConnected(String n);void onMessage(String m);void onLost();void onInfo(String s);}private static final UUID ID=UUID.fromString("b50c4c7e-3941-4bb3-9af1-b8eebf30a3c4");private final BluetoothAdapter a;private final Listener l;private final ExecutorService ex=Executors.newCachedThreadPool();private volatile boolean run=true;private BluetoothSocket s;private PrintWriter out;BluetoothLink(BluetoothAdapter a,Listener l){this.a=a;this.l=l;}void start(){ex.execute(this::server);ex.execute(this::clients);}private void server(){try(BluetoothServerSocket ss=a.listenUsingRfcommWithServiceRecord("AmiralBatti",ID)){l.onInfo("Rakip bekleniyor…");while(run&&s==null){BluetoothSocket x=ss.accept();attach(x);}}catch(Exception ignored){}}private void clients(){try{Thread.sleep(450);Set<BluetoothDevice>d=a.getBondedDevices();for(BluetoothDevice x:d){if(!run||s!=null)return;try{BluetoothSocket q=x.createRfcommSocketToServiceRecord(ID);a.cancelDiscovery();q.connect();attach(q);return;}catch(Exception ignored){}}}catch(Exception ignored){}}private synchronized void attach(BluetoothSocket q){if(!run||s!=null){try{q.close();}catch(Exception ignored){}return;}try{s=q;out=new PrintWriter(q.getOutputStream(),true);l.onConnected(q.getRemoteDevice().getName());ex.execute(()->read(q));}catch(Exception e){l.onLost();}}private void read(BluetoothSocket q){try{BufferedReader r=new BufferedReader(new InputStreamReader(q.getInputStream()));String line;while(run&&(line=r.readLine())!=null)l.onMessage(line);}catch(Exception ignored){}if(run)l.onLost();}void send(String m){PrintWriter w=out;if(w!=null){w.println(m);w.flush();}}void close(){run=false;try{if(s!=null)s.close();}catch(Exception ignored){}ex.shutdownNow();}
    }
}
