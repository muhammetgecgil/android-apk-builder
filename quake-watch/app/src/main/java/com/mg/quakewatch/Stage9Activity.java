package com.mg.quakewatch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowInsets;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Stage9Activity extends Activity {
    private final int BG=Color.rgb(4,8,15),P=Color.rgb(12,21,35),T=Color.rgb(240,246,255),M=Color.rgb(151,170,194),C=Color.rgb(82,202,255),G=Color.rgb(74,226,162),Y=Color.rgb(255,202,101),R=Color.rgb(255,83,104),V=Color.rgb(171,129,255);
    private WebView web; private TextView status; private final Handler handler=new Handler(); private boolean alive=true;
    private final Runnable auto=new Runnable(){public void run(){if(alive){load();handler.postDelayed(this,120000);}}};

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);build();load();handler.postDelayed(auto,120000);}
    @Override protected void onDestroy(){alive=false;handler.removeCallbacks(auto);super.onDestroy();}

    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        if(Build.VERSION.SDK_INT>=20){root.setOnApplyWindowInsetsListener((v,i)->{int top=0,bot=0;if(Build.VERSION.SDK_INT>=30){top=i.getInsets(WindowInsets.Type.statusBars()).top;bot=i.getInsets(WindowInsets.Type.navigationBars()).bottom;}else{top=i.getSystemWindowInsetTop();bot=i.getSystemWindowInsetBottom();}v.setPadding(0,top,0,bot);return i;});root.requestApplyInsets();}
        TextView h=t("QUAKE WATCH • AŞAMA 9",24,T);h.setTypeface(Typeface.DEFAULT_BOLD);h.setPadding(dp(15),dp(13),dp(15),dp(2));root.addView(h);
        TextView sub=t("GLOBAL LIVE SEISMIC MAP • PROBABILISTIC HOTSPOT FORECAST",11,C);sub.setPadding(dp(15),0,dp(15),dp(4));root.addView(sub);
        status=t("● Dünya kataloğu yükleniyor...",12,G);status.setPadding(dp(15),0,dp(15),dp(8));root.addView(status);
        web=new WebView(this);WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setBuiltInZoomControls(false);web.setBackgroundColor(BG);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(6),dp(5),dp(6),dp(8));nav.setBackgroundColor(Color.rgb(9,16,27));
        Button w=btn("DÜNYA",C),tr=btn("TÜRKİYE",G),h24=btn("24 SAAT",Y),d7=btn("7 GÜN",R),ref=btn("YENİLE",V),s8=btn("AŞAMA 8",C);
        nav.addView(w,lp());nav.addView(tr,lp());nav.addView(h24,lp());nav.addView(d7,lp());nav.addView(ref,lp());nav.addView(s8,lp());root.addView(nav);
        w.setOnClickListener(v->js("modeWorld()"));tr.setOnClickListener(v->js("modeTurkey()"));h24.setOnClickListener(v->js("setHours(24)"));d7.setOnClickListener(v->js("setHours(168)"));ref.setOnClickListener(v->load());s8.setOnClickListener(v->startActivity(new Intent(this,Stage8Activity.class)));
        setContentView(root);
    }

    private void load(){status.setText("● USGS canlı dünya kataloğu güncelleniyor...");new Thread(()->{try{GlobalQuakeEngine.Report r=GlobalQuakeEngine.fetch();runOnUiThread(()->{render(r);status.setText("● "+r.summary+" • otomatik yenileme 2 dk");});}catch(Exception e){runOnUiThread(()->status.setText("● Dünya veri hatası: "+e.getMessage()));}}).start();}

    private void render(GlobalQuakeEngine.Report r){
        StringBuilder h=new StringBuilder();
        h.append("<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,user-scalable=no'><style>");
        h.append("html,body{margin:0;height:100%;overflow:hidden;background:#04080f;color:#eef4ff;font-family:Arial}#c{width:100%;height:100%;touch-action:none}.hud{position:absolute;left:8px;top:8px;right:8px;display:flex;gap:7px;pointer-events:none}.b{background:#0b1624e8;border:1px solid #29415f;border-radius:12px;padding:7px 9px;font-size:10px;box-shadow:0 8px 24px #0008}.big{font-size:14px;font-weight:bold;color:#52caff}.tip{position:absolute;display:none;z-index:9;background:#091522f2;border:1px solid #5a7da3;border-radius:11px;padding:9px;max-width:260px;font-size:11px}.leg{position:absolute;left:8px;bottom:8px;background:#0b1422e8;border:1px solid #273d59;border-radius:10px;padding:7px;font-size:9px;pointer-events:none}.stamp{position:absolute;right:8px;bottom:8px;background:#0b1422e8;border:1px solid #273d59;border-radius:10px;padding:7px;font-size:9px;pointer-events:none}</style></head><body>");
        h.append("<canvas id='c'></canvas><div class='hud'><div class='b'><div class='big' id='mode'>DÜNYA • CANLI</div><div id='stats'></div></div><div class='b'>Sürükle: kaydır<br>Pinch: zoom<br>Dokun: olay / risk hücresi</div></div><div class='leg'>Deprem: yeşil &lt;M2 • turuncu M2–4 • kırmızı ≥M4<br>Risk halkası: yalnız istatistiksel/anomali forecast</div><div class='stamp'>USGS • 7 gün</div><div id='tip' class='tip'></div>");
        h.append("<script>const E=").append(r.eventsJson).append(",H=").append(r.hotspotsJson).append(";let hours=168,mode='W',zoom=1,ox=0,oy=0,drag=false,lx=0,ly=0,moved=0;const c=document.getElementById('c'),x=c.getContext('2d'),tip=document.getElementById('tip');");
        h.append("function resize(){c.width=innerWidth*devicePixelRatio;c.height=innerHeight*devicePixelRatio;x.setTransform(devicePixelRatio,0,0,devicePixelRatio,0,0);draw()}addEventListener('resize',resize);");
        h.append("function B(){return mode==='TR'?{a:24,b:47,c:34,d:43}:{a:-180,b:180,c:-85,d:85}}function P(lon,lat){let b=B(),px=(lon-b.a)/(b.b-b.a)*innerWidth,py=(b.d-lat)/(b.d-b.c)*innerHeight;return [(px-innerWidth/2)*zoom+innerWidth/2+ox,(py-innerHeight/2)*zoom+innerHeight/2+oy]}");
        h.append("function inside(e){let b=B();return e.lon>=b.a&&e.lon<=b.b&&e.lat>=b.c&&e.lat<=b.d&&e.time>=Date.now()-hours*3600000}function col(m){return m>=4?'#ff5368':m>=2?'#ffb22f':'#4ae2a2'}");
        h.append("function grid(){x.strokeStyle='#193047';x.lineWidth=.7;for(let lon=Math.ceil(B().a/30)*30;lon<=B().b;lon+=30){let a=P(lon,B().c),b=P(lon,B().d);x.beginPath();x.moveTo(a[0],a[1]);x.lineTo(b[0],b[1]);x.stroke()}for(let lat=Math.ceil(B().c/20)*20;lat<=B().d;lat+=20){let a=P(B().a,lat),b=P(B().b,lat);x.beginPath();x.moveTo(a[0],a[1]);x.lineTo(b[0],b[1]);x.stroke()}}");
        h.append("function risks(){H.forEach((q,i)=>{if(!inside({lon:q.lon,lat:q.lat,time:Date.now()}))return;let p=P(q.lon,q.lat),r=5+q.score*.22*zoom;x.beginPath();x.arc(p[0],p[1],r,0,Math.PI*2);x.strokeStyle='rgba(255,83,104,'+(0.18+q.score/160)+')';x.lineWidth=1.2;x.stroke();x.fillStyle='rgba(255,83,104,'+(q.score/900)+')';x.fill()})}");
        h.append("function events(){let a=E.filter(inside);a.forEach(e=>{let p=P(e.lon,e.lat),r=Math.max(1.6,1.5+Math.max(0,e.mag)*.8)*Math.sqrt(zoom);x.beginPath();x.arc(p[0],p[1],r,0,Math.PI*2);x.fillStyle=col(e.mag);x.shadowColor=x.fillStyle;x.shadowBlur=e.mag>=4?9:2;x.fill();x.shadowBlur=0});document.getElementById('stats').textContent=a.length+' olay • '+hours+' saat • '+H.length+' risk hücresi'}");
        h.append("function draw(){x.clearRect(0,0,innerWidth,innerHeight);let g=x.createLinearGradient(0,0,0,innerHeight);g.addColorStop(0,'#071421');g.addColorStop(1,'#02060c');x.fillStyle=g;x.fillRect(0,0,innerWidth,innerHeight);grid();risks();events()}");
        h.append("function d(a,b,c,d){return Math.hypot(a-c,b-d)}function pick(px,py){let best=null,bd=18;E.filter(inside).forEach(e=>{let p=P(e.lon,e.lat),q=d(px,py,p[0],p[1]);if(q<bd){bd=q;best=e}});if(best){show(px,py,'<b>Deprem</b><br>M '+Number(best.mag).toFixed(1)+' • '+Number(best.depth).toFixed(1)+' km<br>'+new Date(best.time).toLocaleString()+'<br>'+best.place);return}let bh=null;br=999;H.forEach(q=>{let p=P(q.lon,q.lat),z=d(px,py,p[0],p[1]);if(z<br){br=z;bh=q}});if(bh&&br<35){show(px,py,'<b>Forecast / anomali hücresi</b><br>Skor '+Number(bh.score).toFixed(1)+'/100 • güven '+Number(bh.confidence).toFixed(0)+'%<br>24s olay '+bh.n24+' • 7g olay '+bh.n7+'<br>aktivite oranı '+Number(bh.rate).toFixed(2)+'x • Mmax '+Number(bh.maxMag).toFixed(1)+'<br><small>Kesin deprem tahmini değildir.</small>')}else tip.style.display='none'}");
        h.append("function show(a,b,s){tip.innerHTML=s;tip.style.left=Math.min(innerWidth-275,Math.max(7,a+9))+'px';tip.style.top=Math.min(innerHeight-125,Math.max(7,b+9))+'px';tip.style.display='block'}");
        h.append("c.addEventListener('pointerdown',e=>{drag=true;lx=e.clientX;ly=e.clientY;moved=0});c.addEventListener('pointermove',e=>{if(!drag)return;let dx=e.clientX-lx,dy=e.clientY-ly;ox+=dx;oy+=dy;moved+=Math.abs(dx)+Math.abs(dy);lx=e.clientX;ly=e.clientY;draw()});c.addEventListener('pointerup',e=>{if(moved<8)pick(e.clientX,e.clientY);drag=false});let ld=0;c.addEventListener('touchmove',e=>{if(e.touches.length===2){let a=e.touches[0],b=e.touches[1],q=Math.hypot(a.clientX-b.clientX,a.clientY-b.clientY);if(ld){zoom*=q/ld;zoom=Math.max(.7,Math.min(6,zoom));draw()}ld=q;e.preventDefault()}},{passive:false});c.addEventListener('touchend',()=>ld=0);");
        h.append("window.modeWorld=()=>{mode='W';zoom=1;ox=oy=0;document.getElementById('mode').textContent='DÜNYA • CANLI';draw()};window.modeTurkey=()=>{mode='TR';zoom=1;ox=oy=0;document.getElementById('mode').textContent='TÜRKİYE • DÜNYA KATALOĞU İÇİNDEN';draw()};window.setHours=n=>{hours=n;draw()};resize();</script></body></html>");
        web.loadDataWithBaseURL(null,h.toString(),"text/html","UTF-8",null);
    }
    private void js(String s){web.evaluateJavascript(s,null);}private TextView t(String s,int z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);return v;}private Button btn(String s,int c){Button b=new Button(this);b.setText(s);b.setTextColor(c);b.setTextSize(9);b.setBackgroundColor(P);return b;}private LinearLayout.LayoutParams lp(){return new LinearLayout.LayoutParams(0,dp(50),1);}private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
}
