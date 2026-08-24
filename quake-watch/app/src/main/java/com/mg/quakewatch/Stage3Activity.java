package com.mg.quakewatch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowInsets;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

public class Stage3Activity extends Activity {
    private static final int BG=Color.rgb(5,9,16), PANEL=Color.rgb(13,22,36), TEXT=Color.rgb(238,244,255), MUTED=Color.rgb(145,167,194), CYAN=Color.rgb(82,202,255), GREEN=Color.rgb(74,226,162), RED=Color.rgb(255,83,104), GOLD=Color.rgb(255,202,101), PURPLE=Color.rgb(171,129,255);
    private WebView twin;
    private TextView status, windowLabel;
    private TurkeyAnalyzer.Report report;
    private int hours=168;

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(Color.rgb(8,14,24));build();load();}

    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        if(Build.VERSION.SDK_INT>=20){root.setOnApplyWindowInsetsListener((v,in)->{int t=0,bt=0;if(Build.VERSION.SDK_INT>=30){t=in.getInsets(WindowInsets.Type.statusBars()).top;bt=in.getInsets(WindowInsets.Type.navigationBars()).bottom;}else{t=in.getSystemWindowInsetTop();bt=in.getSystemWindowInsetBottom();}v.setPadding(0,t,0,bt);return in;});root.requestApplyInsets();}
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setPadding(dp(16),dp(16),dp(16),dp(12));head.setBackground(grad(Color.rgb(12,23,40),Color.rgb(7,13,24)));
        TextView title=t("QUAKE WATCH • AŞAMA 3",24,TEXT);title.setTypeface(Typeface.DEFAULT_BOLD);head.addView(title);head.addView(t("Türkiye Digital Twin • 3B kabuk • fay segmentleri • Marmara Microscope • göç animasyonu",11,MUTED));status=t("● Digital Twin hazırlanıyor",12,GREEN);status.setPadding(0,dp(9),0,0);head.addView(status);root.addView(head);

        LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.VERTICAL);controls.setPadding(dp(12),dp(10),dp(12),dp(8));controls.setBackgroundColor(PANEL);
        windowLabel=t("Zaman penceresi: son 168 saat",12,TEXT);controls.addView(windowLabel);SeekBar sb=new SeekBar(this);sb.setMax(167);sb.setProgress(167);controls.addView(sb);root.addView(controls);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean from){hours=p+1;windowLabel.setText("Zaman penceresi: son "+hours+" saat");js("setHours("+hours+")");}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});

        twin=new WebView(this);WebSettings ws=twin.getSettings();ws.setJavaScriptEnabled(true);ws.setDomStorageEnabled(true);ws.setBuiltInZoomControls(false);ws.setDisplayZoomControls(false);twin.setBackgroundColor(BG);root.addView(twin,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setPadding(dp(8),dp(7),dp(8),dp(9));nav.setBackgroundColor(Color.rgb(9,16,27));
        Button tr=btn("TÜRKİYE 3B",CYAN), mar=btn("MARMARA",RED), play=btn("▶ GÖÇ",GREEN), prev=btn("AŞAMA 2",GOLD), radar=btn("RADAR",PURPLE);
        nav.addView(tr,lp());nav.addView(mar,lp());nav.addView(play,lp());nav.addView(prev,lp());nav.addView(radar,lp());root.addView(nav);
        tr.setOnClickListener(v->js("modeTR()"));mar.setOnClickListener(v->js("modeMarmara()"));play.setOnClickListener(v->js("toggleMigration()"));prev.setOnClickListener(v->startActivity(new Intent(this,Stage2Activity.class)));radar.setOnClickListener(v->startActivity(new Intent(this,ResearchConsoleActivity.class)));
        setContentView(root);
    }

    private void load(){status.setText("● Türkiye kataloğu indiriliyor...");new Thread(()->{try{report=TurkeyAnalyzer.fetchAndAnalyze();runOnUiThread(()->{render(report);status.setText("● "+report.eventCount+" olay • Digital Twin aktif");});}catch(Exception e){runOnUiThread(()->{status.setText("● Veri hatası: "+e.getMessage());twin.loadData("<html><body style='background:#050910;color:white;font-family:sans-serif'>Digital Twin verisi yüklenemedi.</body></html>","text/html","UTF-8");});}}).start();}

    private void render(TurkeyAnalyzer.Report r){
        String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,user-scalable=no'><style>html,body{margin:0;height:100%;overflow:hidden;background:#050910;color:#eef4ff;font-family:Arial}#c{width:100%;height:100%;touch-action:none}.hud{position:absolute;left:10px;top:10px;right:10px;display:flex;gap:8px;pointer-events:none}.box{background:#0c1727dd;border:1px solid #29415f;border-radius:12px;padding:8px 10px;font-size:11px;box-shadow:0 8px 30px #0008}.big{font-size:16px;font-weight:bold;color:#52caff}.legend{position:absolute;bottom:12px;left:10px;background:#0b1422dd;border:1px solid #273d59;border-radius:10px;padding:8px;font-size:10px}.tip{position:absolute;display:none;background:#0b1525ee;border:1px solid #4a6688;border-radius:10px;padding:8px;max-width:250px;font-size:11px;pointer-events:none}</style></head><body><canvas id='c'></canvas><div class='hud'><div class='box'><div class='big' id='mode'>TÜRKİYE DIGITAL TWIN</div><div id='stats'>hazırlanıyor</div></div><div class='box'>Sürükle: döndür<br>Pinch: zoom<br>Dokun: olay/segment</div></div><div class='legend'>Derinlik: aşağı doğru • yeşil M&lt;2 • turuncu M2–4 • kırmızı M≥4<br>Faylar: kırmızı ışıklı çizgi • mor: seçili Marmara segmentleri</div><div id='tip' class='tip'></div><script>const events="+r.eventsJson+",faults="+r.faultsJson+";let H=168,mode='TR',rx=.58,rz=-.25,zoom=1.05,drag=false,lastX=0,lastY=0,anim=false,timer=null,animH=6;const cv=document.getElementById('c'),ctx=cv.getContext('2d');function resize(){cv.width=innerWidth*devicePixelRatio;cv.height=innerHeight*devicePixelRatio;ctx.setTransform(devicePixelRatio,0,0,devicePixelRatio,0,0);draw()}addEventListener('resize',resize);function col(m){return m>=4?'#ff5368':m>=2?'#ffb22f':'#4ae2a2'}function bounds(){return mode==='M'?{x0:26,x1:31.4,y0:39.5,y1:41.7}:{x0:25,x1:46,y0:34,y1:43}}function proj(lon,lat,dep){let b=bounds(),x=(lon-(b.x0+b.x1)/2)/(b.x1-b.x0)*680,y=(lat-(b.y0+b.y1)/2)/(b.y1-b.y0)*360,z=-Math.min(dep,160)/160*300;let cr=Math.cos(rz),sr=Math.sin(rz),x1=x*cr-y*sr,y1=x*sr+y*cr;let cx=Math.cos(rx),sx=Math.sin(rx),y2=y1*cx-z*sx,z2=y1*sx+z*cx;let sc=zoom*(1+z2/1500);return [innerWidth/2+x1*sc,innerHeight*.52-y2*sc,z2]}
function ground(){let b=bounds(),pts=[[b.x0,b.y0],[b.x1,b.y0],[b.x1,b.y1],[b.x0,b.y1]];ctx.beginPath();pts.forEach((p,i)=>{let q=proj(p[0],p[1],0);i?ctx.lineTo(q[0],q[1]):ctx.moveTo(q[0],q[1])});ctx.closePath();ctx.fillStyle='#0a1523';ctx.fill();ctx.strokeStyle='#28415c';ctx.lineWidth=1;ctx.stroke();for(let d=40;d<=160;d+=40){ctx.beginPath();pts.forEach((p,i)=>{let q=proj(p[0],p[1],d);i?ctx.lineTo(q[0],q[1]):ctx.moveTo(q[0],q[1])});ctx.closePath();ctx.strokeStyle='#20354b88';ctx.stroke()}}
function inB(lon,lat){let b=bounds();return lon>=b.x0&&lon<=b.x1&&lat>=b.y0&&lat<=b.y1}function drawFaults(){ctx.save();faults.forEach(f=>{let ps=f.pts||[];if(!ps.length)return;let any=ps.some(p=>inB(p[1],p[0]));if(!any)return;ctx.beginPath();let first=true;ps.forEach(p=>{let q=proj(p[1],p[0],0);if(first){ctx.moveTo(q[0],q[1]);first=false}else ctx.lineTo(q[0],q[1])});let marm=/Marmara|Ganos|Kumburgaz|Çınarcık|Tekirdağ/i.test(f.name||'');ctx.strokeStyle=marm&&mode==='M'?'#b780ff':'#ff596c';ctx.shadowColor=ctx.strokeStyle;ctx.shadowBlur=8;ctx.lineWidth=marm&&mode==='M'?3.5:2;ctx.stroke();ctx.shadowBlur=0});ctx.restore()}
function filtered(){let cut=Date.now()-H*3600000;return events.filter(e=>e.time>=cut&&inB(e.lon,e.lat)).sort((a,b)=>a.depth-b.depth)}function drawEvents(){let es=filtered();es.forEach(e=>{let q=proj(e.lon,e.lat,e.depth);let r=Math.max(2,2.2+Math.max(0,e.mag)*1.3);ctx.beginPath();ctx.arc(q[0],q[1],r,0,Math.PI*2);ctx.fillStyle=col(e.mag);ctx.shadowColor=ctx.fillStyle;ctx.shadowBlur=e.mag>=4?12:4;ctx.fill();ctx.shadowBlur=0});document.getElementById('stats').innerHTML=es.length+' olay • '+H+' saat • 0–160 km kabuk görünümü'}function drawMigration(){if(!anim)return;let es=filtered().sort((a,b)=>a.time-b.time);if(es.length<2)return;let tail=es.slice(Math.max(0,es.length-18));ctx.beginPath();tail.forEach((e,i)=>{let q=proj(e.lon,e.lat,Math.min(e.depth,80));i?ctx.lineTo(q[0],q[1]):ctx.moveTo(q[0],q[1])});ctx.strokeStyle='#52caff';ctx.lineWidth=2;ctx.setLineDash([5,5]);ctx.stroke();ctx.setLineDash([])}function draw(){ctx.clearRect(0,0,innerWidth,innerHeight);let g=ctx.createLinearGradient(0,0,0,innerHeight);g.addColorStop(0,'#07111e');g.addColorStop(1,'#03070d');ctx.fillStyle=g;ctx.fillRect(0,0,innerWidth,innerHeight);ground();drawFaults();drawEvents();drawMigration()}
cv.addEventListener('pointerdown',e=>{drag=true;lastX=e.clientX;lastY=e.clientY});cv.addEventListener('pointermove',e=>{if(!drag)return;rz+=(e.clientX-lastX)*.008;rx+=(e.clientY-lastY)*.006;rx=Math.max(.15,Math.min(1.35,rx));lastX=e.clientX;lastY=e.clientY;draw()});addEventListener('pointerup',()=>drag=false);let lastDist=0;cv.addEventListener('touchmove',e=>{if(e.touches.length===2){let a=e.touches[0],b=e.touches[1],d=Math.hypot(a.clientX-b.clientX,a.clientY-b.clientY);if(lastDist){zoom*=d/lastDist;zoom=Math.max(.55,Math.min(2.8,zoom));draw()}lastDist=d;e.preventDefault()}},{passive:false});cv.addEventListener('touchend',()=>lastDist=0);cv.addEventListener('wheel',e=>{zoom*=e.deltaY<0?1.08:.92;zoom=Math.max(.55,Math.min(2.8,zoom));draw();e.preventDefault()},{passive:false});window.setHours=x=>{H=x;draw()};window.modeTR=()=>{mode='TR';rx=.58;rz=-.25;zoom=1.05;document.getElementById('mode').textContent='TÜRKİYE DIGITAL TWIN';draw()};window.modeMarmara=()=>{mode='M';rx=.62;rz=-.12;zoom=1.18;document.getElementById('mode').textContent='MARMARA MICROSCOPE';draw()};window.toggleMigration=()=>{anim=!anim;if(anim){animH=6;clearInterval(timer);timer=setInterval(()=>{H=animH;animH+=6;if(animH>168)animH=6;draw()},550)}else{clearInterval(timer);H=168;draw()}};resize();</script></body></html>";
        twin.loadDataWithBaseURL("https://localhost/",html,"text/html","UTF-8",null);
    }

    private void js(String s){if(twin!=null)twin.evaluateJavascript("window."+s,null);}private LinearLayout.LayoutParams lp(){return new LinearLayout.LayoutParams(0,dp(54),1);}private Button btn(String s,int c){Button b=new Button(this);b.setText(s);b.setTextColor(Color.rgb(5,10,18));b.setTextSize(9);b.setTypeface(Typeface.DEFAULT_BOLD);b.setAllCaps(false);GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(13));b.setBackground(g);return b;}private TextView t(String s,int sp,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);return v;}private GradientDrawable grad(int a,int b){return new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});}private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density);}
}
