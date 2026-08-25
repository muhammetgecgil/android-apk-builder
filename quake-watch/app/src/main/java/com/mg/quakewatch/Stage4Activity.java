package com.mg.quakewatch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Stage4Activity extends Activity {
    private static final int BG=Color.rgb(5,9,16), PANEL=Color.rgb(12,21,35), TEXT=Color.rgb(239,245,255), MUTED=Color.rgb(151,170,194), CYAN=Color.rgb(82,202,255), GREEN=Color.rgb(74,226,162), RED=Color.rgb(255,83,104), GOLD=Color.rgb(255,202,101), PURPLE=Color.rgb(171,129,255);
    private WebView web;
    private TextView status;

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(Color.rgb(8,14,24));build();load();}

    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        if(Build.VERSION.SDK_INT>=20){root.setOnApplyWindowInsetsListener((v,in)->{int t=0,bt=0;if(Build.VERSION.SDK_INT>=30){t=in.getInsets(WindowInsets.Type.statusBars()).top;bt=in.getInsets(WindowInsets.Type.navigationBars()).bottom;}else{t=in.getSystemWindowInsetTop();bt=in.getSystemWindowInsetBottom();}v.setPadding(0,t,0,bt);return in;});root.requestApplyInsets();}
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setPadding(dp(16),dp(14),dp(16),dp(10));head.setBackground(grad(Color.rgb(13,24,42),Color.rgb(7,13,24)));
        TextView title=t("QUAKE WATCH • AŞAMA 4",24,TEXT);title.setTypeface(Typeface.DEFAULT_BOLD);head.addView(title);head.addView(t("Türkiye 3B Digital Twin • Marmara segment seçimi • olay detayları • zaman oynatıcı",11,MUTED));status=t("● Aşama 4 hazırlanıyor",12,GREEN);status.setPadding(0,dp(8),0,0);head.addView(status);root.addView(head);
        web=new WebView(this);WebSettings ws=web.getSettings();ws.setJavaScriptEnabled(true);ws.setDomStorageEnabled(true);ws.setBuiltInZoomControls(false);ws.setDisplayZoomControls(false);web.setBackgroundColor(BG);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setPadding(dp(7),dp(6),dp(7),dp(8));nav.setBackgroundColor(PANEL);
        Button tr=btn("TÜRKİYE",CYAN), mar=btn("MARMARA",RED), play=btn("▶ ZAMAN",GREEN), reset=btn("KAMERA",GOLD), back=btn("AŞAMA 3",PURPLE);
        nav.addView(tr,lp());nav.addView(mar,lp());nav.addView(play,lp());nav.addView(reset,lp());nav.addView(back,lp());root.addView(nav);
        tr.setOnClickListener(v->js("modeTR()"));mar.setOnClickListener(v->js("modeM()"));play.setOnClickListener(v->js("toggleTime()"));reset.setOnClickListener(v->js("resetCamera()"));back.setOnClickListener(v->startActivity(new Intent(this,Stage3Activity.class)));
        setContentView(root);
    }

    private void load(){
        status.setText("● Türkiye deprem kataloğu indiriliyor...");
        new Thread(()->{try{TurkeyAnalyzer.Report r=TurkeyAnalyzer.fetchAndAnalyze();runOnUiThread(()->{render(r);status.setText("● "+r.eventCount+" olay • Aşama 4 aktif");});}catch(Exception e){runOnUiThread(()->{status.setText("● Veri hatası: "+e.getMessage());web.loadData("<html><body style='background:#050910;color:white;font-family:sans-serif'>Veri yüklenemedi.</body></html>","text/html","UTF-8");});}}).start();
    }

    private void render(TurkeyAnalyzer.Report r){
        StringBuilder h=new StringBuilder();
        h.append("<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,user-scalable=no'>");
        h.append("<style>html,body{margin:0;height:100%;overflow:hidden;background:#050910;color:#eef4ff;font-family:Arial}#c{width:100%;height:100%;touch-action:none}.hud{position:absolute;left:9px;top:9px;right:9px;display:flex;gap:7px;pointer-events:none}.box{background:#0c1727e8;border:1px solid #29415f;border-radius:12px;padding:7px 9px;font-size:10px;box-shadow:0 8px 24px #0008}.big{font-size:15px;font-weight:bold;color:#52caff}.legend{position:absolute;left:9px;bottom:10px;background:#0b1422e8;border:1px solid #273d59;border-radius:10px;padding:7px;font-size:9px;pointer-events:none}.tip{position:absolute;display:none;z-index:10;background:#091522f2;border:1px solid #5a7da3;border-radius:11px;padding:9px;max-width:270px;font-size:11px;box-shadow:0 10px 30px #000a}.time{position:absolute;right:9px;bottom:10px;background:#0b1422e8;border:1px solid #273d59;border-radius:10px;padding:7px 9px;font-size:10px;pointer-events:none}</style></head><body>");
        h.append("<canvas id='c'></canvas><div class='hud'><div class='box'><div class='big' id='mode'>TÜRKİYE 3B DIGITAL TWIN</div><div id='stats'>hazırlanıyor</div></div><div class='box'>Sürükle: döndür<br>Pinch: zoom<br>Dokun: olay / fay</div></div><div class='legend'>Yeşil M&lt;2 • turuncu M2–4 • kırmızı M≥4<br>Fay: kırmızı • seçili: mor • kıyı: mavi referans</div><div class='time' id='time'>Zaman: son 168 saat</div><div id='tip' class='tip'></div>");
        h.append("<script>const events=").append(r.eventsJson).append(",faults=").append(r.faultsJson).append(";");
        h.append("let H=168,mode='TR',rx=.58,rz=-.25,zoom=1.05,drag=false,lastX=0,lastY=0,anim=false,timer=null,animH=6,selectedFault=-1;const cv=document.getElementById('c'),ctx=cv.getContext('2d'),tip=document.getElementById('tip');");
        h.append("const coast=[[26.0,40.1],[27.2,40.5],[28.8,41.0],[29.9,41.1],[31.2,41.2],[32.9,41.6],[35.0,42.0],[37.0,41.2],[39.0,41.0],[41.0,41.3],[43.0,41.5],[44.8,40.0],[44.2,38.6],[42.0,37.4],[39.0,36.4],[36.0,36.0],[34.0,36.1],[31.0,36.0],[29.0,36.4],[27.2,37.0],[26.0,38.3]];");
        h.append("function resize(){cv.width=innerWidth*devicePixelRatio;cv.height=innerHeight*devicePixelRatio;ctx.setTransform(devicePixelRatio,0,0,devicePixelRatio,0,0);draw()}addEventListener('resize',resize);");
        h.append("function bounds(){return mode==='M'?{x0:26,x1:31.5,y0:39.4,y1:41.8}:{x0:25,x1:46,y0:34,y1:43}}function col(m){return m>=4?'#ff5368':m>=2?'#ffb22f':'#4ae2a2'}");
        h.append("function proj(lon,lat,dep){let b=bounds(),x=(lon-(b.x0+b.x1)/2)/(b.x1-b.x0)*680,y=(lat-(b.y0+b.y1)/2)/(b.y1-b.y0)*360,z=-Math.min(Math.max(dep,0),180)/180*310;let cr=Math.cos(rz),sr=Math.sin(rz),x1=x*cr-y*sr,y1=x*sr+y*cr,cx=Math.cos(rx),sx=Math.sin(rx),y2=y1*cx-z*sx,z2=y1*sx+z*cx,sc=zoom*(1+z2/1600);return [innerWidth/2+x1*sc,innerHeight*.51-y2*sc,z2]}");
        h.append("function inB(lon,lat){let b=bounds();return lon>=b.x0&&lon<=b.x1&&lat>=b.y0&&lat<=b.y1}");
        h.append("function shell(){let b=bounds(),pts=[[b.x0,b.y0],[b.x1,b.y0],[b.x1,b.y1],[b.x0,b.y1]];ctx.beginPath();pts.forEach((p,i)=>{let q=proj(p[0],p[1],0);i?ctx.lineTo(q[0],q[1]):ctx.moveTo(q[0],q[1])});ctx.closePath();ctx.fillStyle='#0b1725';ctx.fill();ctx.strokeStyle='#2a455f';ctx.stroke();for(let d=30;d<=180;d+=30){ctx.beginPath();pts.forEach((p,i)=>{let q=proj(p[0],p[1],d);i?ctx.lineTo(q[0],q[1]):ctx.moveTo(q[0],q[1])});ctx.closePath();ctx.strokeStyle='#1d334888';ctx.stroke()}for(let i=1;i<6;i++){let lon=b.x0+(b.x1-b.x0)*i/6;let a=proj(lon,b.y0,0),z=proj(lon,b.y1,0);ctx.beginPath();ctx.moveTo(a[0],a[1]);ctx.lineTo(z[0],z[1]);ctx.strokeStyle='#18314666';ctx.stroke()}}");
        h.append("function drawCoast(){ctx.beginPath();let first=true;coast.forEach(p=>{if(!inB(p[0],p[1]))return;let q=proj(p[0],p[1],0);first?(ctx.moveTo(q[0],q[1]),first=false):ctx.lineTo(q[0],q[1])});ctx.strokeStyle='#2ca9ff';ctx.lineWidth=1.4;ctx.shadowColor='#2ca9ff';ctx.shadowBlur=4;ctx.stroke();ctx.shadowBlur=0}");
        h.append("function drawFaults(){faults.forEach((f,fi)=>{let ps=f.pts||[];if(ps.length<2)return;let visible=ps.some(p=>inB(p[1],p[0]));if(!visible)return;ctx.beginPath();let first=true;ps.forEach(p=>{let q=proj(p[1],p[0],0);first?(ctx.moveTo(q[0],q[1]),first=false):ctx.lineTo(q[0],q[1])});let sel=fi===selectedFault;ctx.strokeStyle=sel?'#b780ff':'#ff596c';ctx.lineWidth=sel?4:2;ctx.shadowColor=ctx.strokeStyle;ctx.shadowBlur=sel?13:7;ctx.stroke();ctx.shadowBlur=0})}");
        h.append("function filtered(){let cut=Date.now()-H*3600000;return events.filter(e=>e.time>=cut&&inB(e.lon,e.lat)).sort((a,b)=>a.depth-b.depth)}");
        h.append("function drawEvents(){let es=filtered();es.forEach(e=>{let q=proj(e.lon,e.lat,e.depth),r=Math.max(2.2,2.3+Math.max(0,e.mag)*1.25);ctx.beginPath();ctx.arc(q[0],q[1],r,0,Math.PI*2);ctx.fillStyle=col(e.mag);ctx.shadowColor=ctx.fillStyle;ctx.shadowBlur=e.mag>=4?12:4;ctx.fill();ctx.shadowBlur=0});document.getElementById('stats').innerHTML=es.length+' olay • '+H+' saat • 0–180 km kabuk'}");
        h.append("function draw(){ctx.clearRect(0,0,innerWidth,innerHeight);let g=ctx.createLinearGradient(0,0,0,innerHeight);g.addColorStop(0,'#07111e');g.addColorStop(1,'#03070d');ctx.fillStyle=g;ctx.fillRect(0,0,innerWidth,innerHeight);shell();drawCoast();drawFaults();drawEvents()}");
        h.append("function d2(ax,ay,bx,by){let x=ax-bx,y=ay-by;return x*x+y*y}function segDist(px,py,ax,ay,bx,by){let dx=bx-ax,dy=by-ay,l=dx*dx+dy*dy,t=l?((px-ax)*dx+(py-ay)*dy)/l:0;t=Math.max(0,Math.min(1,t));let x=ax+t*dx,y=ay+t*dy;return Math.sqrt(d2(px,py,x,y))}");
        h.append("function pick(x,y){let best=null,bd=24;filtered().forEach(e=>{let q=proj(e.lon,e.lat,e.depth),d=Math.sqrt(d2(x,y,q[0],q[1]));if(d<bd){bd=d;best={type:'e',e:e}}});if(best){let e=best.e;showTip(x,y,'<b>Deprem olayı</b><br>M '+Number(e.mag).toFixed(1)+' • '+Number(e.depth).toFixed(1)+' km<br>'+new Date(e.time).toLocaleString()+'<br>'+e.place+'<br>Yakın fay: '+e.fault+' • '+Number(e.faultKm).toFixed(1)+' km');return}let bf=-1,fd=16;faults.forEach((f,fi)=>{let ps=f.pts||[];for(let i=0;i<ps.length-1;i++){let a=proj(ps[i][1],ps[i][0],0),b=proj(ps[i+1][1],ps[i+1][0],0),d=segDist(x,y,a[0],a[1],b[0],b[1]);if(d<fd){fd=d;bf=fi}}});if(bf>=0){selectedFault=bf;let f=faults[bf];showTip(x,y,'<b>'+f.name+'</b><br>'+f.system+'<br>'+f.type);draw()}else{tip.style.display='none'}}");
        h.append("function showTip(x,y,html){tip.innerHTML=html;tip.style.left=Math.min(innerWidth-285,Math.max(8,x+10))+'px';tip.style.top=Math.min(innerHeight-130,Math.max(8,y+10))+'px';tip.style.display='block'}");
        h.append("cv.addEventListener('pointerdown',e=>{drag=true;lastX=e.clientX;lastY=e.clientY});cv.addEventListener('pointermove',e=>{if(!drag)return;let dx=e.clientX-lastX,dy=e.clientY-lastY;rz+=dx*.008;rx+=dy*.006;rx=Math.max(.15,Math.min(1.35,rx));lastX=e.clientX;lastY=e.clientY;draw()});cv.addEventListener('pointerup',e=>{if(Math.abs(e.clientX-lastX)<8&&Math.abs(e.clientY-lastY)<8)pick(e.clientX,e.clientY);drag=false});");
        h.append("let lastDist=0;cv.addEventListener('touchmove',e=>{if(e.touches.length===2){let a=e.touches[0],b=e.touches[1],d=Math.hypot(a.clientX-b.clientX,a.clientY-b.clientY);if(lastDist){zoom*=d/lastDist;zoom=Math.max(.5,Math.min(3.2,zoom));draw()}lastDist=d;e.preventDefault()}},{passive:false});cv.addEventListener('touchend',()=>lastDist=0);");
        h.append("window.modeTR=()=>{mode='TR';selectedFault=-1;rx=.58;rz=-.25;zoom=1.05;document.getElementById('mode').textContent='TÜRKİYE 3B DIGITAL TWIN';draw()};window.modeM=()=>{mode='M';selectedFault=-1;rx=.62;rz=-.12;zoom=1.18;document.getElementById('mode').textContent='MARMARA SEGMENT MICROSCOPE';draw()};window.resetCamera=()=>{rx=.58;rz=-.25;zoom=mode==='M'?1.18:1.05;draw()};window.toggleTime=()=>{anim=!anim;if(anim){animH=6;clearInterval(timer);timer=setInterval(()=>{H=animH;document.getElementById('time').textContent='Zaman: son '+H+' saat';animH+=6;if(animH>168)animH=6;draw()},480)}else{clearInterval(timer);H=168;document.getElementById('time').textContent='Zaman: son 168 saat';draw()}};resize();</script></body></html>");
        web.loadDataWithBaseURL("https://localhost/",h.toString(),"text/html","UTF-8",null);
    }

    private void js(String s){if(web!=null)web.evaluateJavascript("window."+s,null);}private LinearLayout.LayoutParams lp(){return new LinearLayout.LayoutParams(0,dp(52),1);}private Button btn(String s,int c){Button b=new Button(this);b.setText(s);b.setTextColor(Color.rgb(5,10,18));b.setTextSize(9);b.setTypeface(Typeface.DEFAULT_BOLD);b.setAllCaps(false);GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(12));b.setBackground(g);return b;}private TextView t(String s,int sp,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);return v;}private GradientDrawable grad(int a,int b){return new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});}private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density);}
}
