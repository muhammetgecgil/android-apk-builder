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

public class Stage5Activity extends Activity {
    private static final int BG=Color.rgb(5,9,16), PANEL=Color.rgb(12,21,35), TEXT=Color.rgb(239,245,255), MUTED=Color.rgb(151,170,194), CYAN=Color.rgb(82,202,255), GREEN=Color.rgb(74,226,162), RED=Color.rgb(255,83,104), GOLD=Color.rgb(255,202,101), PURPLE=Color.rgb(171,129,255);
    private WebView web;
    private TextView status;

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(Color.rgb(8,14,24));build();load();}

    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        if(Build.VERSION.SDK_INT>=20){root.setOnApplyWindowInsetsListener((v,in)->{int t=0,bt=0;if(Build.VERSION.SDK_INT>=30){t=in.getInsets(WindowInsets.Type.statusBars()).top;bt=in.getInsets(WindowInsets.Type.navigationBars()).bottom;}else{t=in.getSystemWindowInsetTop();bt=in.getSystemWindowInsetBottom();}v.setPadding(0,t,0,bt);return in;});root.requestApplyInsets();}
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setPadding(dp(16),dp(13),dp(16),dp(10));head.setBackground(grad(Color.rgb(13,24,42),Color.rgb(7,13,24)));
        TextView title=t("QUAKE WATCH • AŞAMA 5",24,TEXT);title.setTypeface(Typeface.DEFAULT_BOLD);head.addView(title);
        head.addView(t("Marmara Hypocenter Lab • segment bazlı kesit • fay boyunca mesafe • göç yönü",11,MUTED));
        status=t("● Aşama 5 hazırlanıyor",12,GREEN);status.setPadding(0,dp(8),0,0);head.addView(status);root.addView(head);
        web=new WebView(this);WebSettings ws=web.getSettings();ws.setJavaScriptEnabled(true);ws.setDomStorageEnabled(true);ws.setBuiltInZoomControls(false);ws.setDisplayZoomControls(false);web.setBackgroundColor(BG);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setPadding(dp(6),dp(6),dp(6),dp(8));nav.setBackgroundColor(PANEL);
        Button map=btn("3B",CYAN), section=btn("KESİT",RED), along=btn("FAY KM",GREEN), time=btn("ZAMAN",GOLD), back=btn("AŞAMA 4",PURPLE);
        nav.addView(map,lp());nav.addView(section,lp());nav.addView(along,lp());nav.addView(time,lp());nav.addView(back,lp());root.addView(nav);
        map.setOnClickListener(v->js("setMode('map')"));section.setOnClickListener(v->js("setMode('section')"));along.setOnClickListener(v->js("setMode('along')"));time.setOnClickListener(v->js("setMode('time')"));back.setOnClickListener(v->startActivity(new Intent(this,Stage4Activity.class)));
        setContentView(root);
    }

    private void load(){
        status.setText("● Marmara kataloğu ve segmentleri hazırlanıyor...");
        new Thread(()->{try{TurkeyAnalyzer.Report r=TurkeyAnalyzer.fetchAndAnalyze();runOnUiThread(()->{render(r);status.setText("● "+r.eventCount+" Türkiye olayı • Marmara Lab aktif");});}catch(Exception e){runOnUiThread(()->{status.setText("● Veri hatası: "+e.getMessage());web.loadData("<html><body style='background:#050910;color:white;font-family:sans-serif'>Veri yüklenemedi.</body></html>","text/html","UTF-8");});}}).start();
    }

    private void render(TurkeyAnalyzer.Report r){
        StringBuilder h=new StringBuilder();
        h.append("<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,user-scalable=no'>");
        h.append("<style>html,body{margin:0;height:100%;overflow:hidden;background:#050910;color:#eef4ff;font-family:Arial}#c{width:100%;height:100%;touch-action:none}.hud{position:absolute;left:8px;top:8px;right:8px;display:flex;gap:7px;pointer-events:none}.box{background:#0c1727e8;border:1px solid #29415f;border-radius:12px;padding:7px 9px;font-size:10px}.big{font-size:15px;font-weight:bold;color:#52caff}.legend{position:absolute;left:8px;bottom:9px;background:#0b1422e8;border:1px solid #273d59;border-radius:10px;padding:7px;font-size:9px;pointer-events:none}.info{position:absolute;right:8px;bottom:9px;background:#0b1422e8;border:1px solid #273d59;border-radius:10px;padding:7px 9px;font-size:9px;max-width:48%;pointer-events:none}</style></head><body>");
        h.append("<canvas id='c'></canvas><div class='hud'><div class='box'><div class='big' id='title'>MARMARA 3B HİPOSANTR BULUTU</div><div id='stats'>hazırlanıyor</div></div><div class='box'>Dokun: segment seç<br>Sürükle: 3B döndür<br>Pinch: zoom</div></div><div class='legend'>M&lt;2 yeşil • M2–4 turuncu • M≥4 kırmızı<br>Seçili segment mor • ok: göç yönü</div><div class='info' id='info'>Segment seçilmedi</div>");
        h.append("<script>const events=").append(r.eventsJson).append(";");
        h.append("const segs=[{n:'Ganos',a:[26.55,40.48],b:[27.45,40.62]},{n:'Tekirdağ',a:[27.45,40.62],b:[28.18,40.72]},{n:'Orta Marmara',a:[28.18,40.72],b:[28.72,40.82]},{n:'Kumburgaz',a:[28.72,40.82],b:[29.18,40.88]},{n:'Çınarcık',a:[29.18,40.88],b:[29.82,40.72]},{n:'İzmit',a:[29.82,40.72],b:[30.55,40.75]}];");
        h.append("let mode='map',sel=3,rx=.62,rz=-.12,zoom=1.18,drag=false,lx=0,ly=0;const cv=document.getElementById('c'),ctx=cv.getContext('2d'),stats=document.getElementById('stats'),info=document.getElementById('info'),title=document.getElementById('title');");
        h.append("function col(m){return m>=4?'#ff5368':m>=2?'#ffb22f':'#4ae2a2'}function marm(){return events.filter(e=>e.lon>=26.2&&e.lon<=30.8&&e.lat>=39.8&&e.lat<=41.5)}");
        h.append("function kmLon(lat){return 111.32*Math.cos(lat*Math.PI/180)}function segMetrics(e,s){let lat0=(s.a[1]+s.b[1])*.5,kx=kmLon(lat0),x=e.lon*kx,y=e.lat*110.57,ax=s.a[0]*kx,ay=s.a[1]*110.57,bx=s.b[0]*kx,by=s.b[1]*110.57,dx=bx-ax,dy=by-ay,L=Math.hypot(dx,dy)||1,t=((x-ax)*dx+(y-ay)*dy)/(L*L),along=Math.max(0,Math.min(1,t))*L,cross=((x-ax)*(-dy)+(y-ay)*dx)/L;return [along,cross,L]}");
        h.append("function nearestSeg(e){let bi=0,bd=1e9;segs.forEach((s,i)=>{let m=segMetrics(e,s),d=Math.abs(m[1]);if(d<bd){bd=d;bi=i}});return [bi,bd]}");
        h.append("function proj(lon,lat,dep){let x=(lon-28.5)/4.6*720,y=(lat-40.65)/1.8*360,z=-Math.min(Math.max(dep,0),100)/100*300,cr=Math.cos(rz),sr=Math.sin(rz),x1=x*cr-y*sr,y1=x*sr+y*cr,cx=Math.cos(rx),sx=Math.sin(rx),y2=y1*cx-z*sx,z2=y1*sx+z*cx,sc=zoom*(1+z2/1500);return [innerWidth/2+x1*sc,innerHeight*.52-y2*sc,z2]}");
        h.append("function resize(){cv.width=innerWidth*devicePixelRatio;cv.height=innerHeight*devicePixelRatio;ctx.setTransform(devicePixelRatio,0,0,devicePixelRatio,0,0);draw()}addEventListener('resize',resize);");
        h.append("function bg(){let g=ctx.createLinearGradient(0,0,0,innerHeight);g.addColorStop(0,'#07111e');g.addColorStop(1,'#03070d');ctx.fillStyle=g;ctx.fillRect(0,0,innerWidth,innerHeight)}");
        h.append("function drawMap(){let es=marm();segs.forEach((s,i)=>{let a=proj(s.a[0],s.a[1],0),b=proj(s.b[0],s.b[1],0);ctx.beginPath();ctx.moveTo(a[0],a[1]);ctx.lineTo(b[0],b[1]);ctx.strokeStyle=i===sel?'#b780ff':'#ff596c';ctx.lineWidth=i===sel?4:2;ctx.shadowColor=ctx.strokeStyle;ctx.shadowBlur=i===sel?12:6;ctx.stroke();ctx.shadowBlur=0});es.forEach(e=>{let q=proj(e.lon,e.lat,e.depth),r=Math.max(2,2+Math.max(0,e.mag)*1.25);ctx.beginPath();ctx.arc(q[0],q[1],r,0,Math.PI*2);ctx.fillStyle=col(e.mag);ctx.fill()});let m=segmentEvents(),old=m.filter(e=>Date.now()-e.time>24*3600000),neu=m.filter(e=>Date.now()-e.time<=24*3600000);if(old.length&&neu.length){let oa=centroid(old),na=centroid(neu),a=proj(oa[0],oa[1],8),b=proj(na[0],na[1],8);arrow(a,b)}stats.textContent=es.length+' Marmara olayı • 0–100 km derinlik';segmentInfo(m)}");
        h.append("function segmentEvents(){return marm().filter(e=>{let n=nearestSeg(e);return n[0]===sel&&n[1]<=35})}function centroid(es){let x=0,y=0;es.forEach(e=>{x+=e.lon;y+=e.lat});return [x/es.length,y/es.length]}function arrow(a,b){ctx.strokeStyle='#52caff';ctx.fillStyle='#52caff';ctx.lineWidth=2;ctx.beginPath();ctx.moveTo(a[0],a[1]);ctx.lineTo(b[0],b[1]);ctx.stroke();let ang=Math.atan2(b[1]-a[1],b[0]-a[0]);ctx.beginPath();ctx.moveTo(b[0],b[1]);ctx.lineTo(b[0]-10*Math.cos(ang-.45),b[1]-10*Math.sin(ang-.45));ctx.lineTo(b[0]-10*Math.cos(ang+.45),b[1]-10*Math.sin(ang+.45));ctx.closePath();ctx.fill()}");
        h.append("function axes(x0,y0,w,h,xlab,ylab){ctx.strokeStyle='#49617c';ctx.lineWidth=1;ctx.strokeRect(x0,y0,w,h);ctx.fillStyle='#93a8c1';ctx.font='11px Arial';ctx.fillText(xlab,x0+w/2-35,y0+h+17);ctx.save();ctx.translate(x0-28,y0+h/2+30);ctx.rotate(-Math.PI/2);ctx.fillText(ylab,0,0);ctx.restore()}");
        h.append("function drawSection(){let es=segmentEvents(),s=segs[sel],x0=42,y0=65,w=innerWidth-62,h=innerHeight-115;axes(x0,y0,w,h,'Faya dik mesafe (km)','Derinlik (km)');es.forEach(e=>{let m=segMetrics(e,s),x=x0+w/2+(m[1]/50)*(w/2),y=y0+Math.min(e.depth,80)/80*h;ctx.beginPath();ctx.arc(x,y,Math.max(2,2+e.mag),0,Math.PI*2);ctx.fillStyle=col(e.mag);ctx.fill()});stats.textContent=s.n+' • '+es.length+' olay • ±50 km kesit';segmentInfo(es)}");
        h.append("function drawAlong(){let es=segmentEvents(),s=segs[sel],L=segMetrics({lon:s.a[0],lat:s.a[1]},s)[2],x0=42,y0=65,w=innerWidth-62,h=innerHeight-115;axes(x0,y0,w,h,'Fay boyunca mesafe (km)','Derinlik (km)');es.forEach(e=>{let m=segMetrics(e,s),x=x0+(m[0]/L)*w,y=y0+Math.min(e.depth,80)/80*h;ctx.beginPath();ctx.arc(x,y,Math.max(2,2+e.mag),0,Math.PI*2);ctx.fillStyle=col(e.mag);ctx.fill()});stats.textContent=s.n+' • '+Math.round(L)+' km segment ekseni • '+es.length+' olay';segmentInfo(es)}");
        h.append("function drawTime(){let es=segmentEvents().sort((a,b)=>a.time-b.time),x0=42,y0=65,w=innerWidth-62,h=innerHeight-115,min=Date.now()-7*86400000;axes(x0,y0,w,h,'Son 7 gün','Fay boyunca km');let s=segs[sel],L=segMetrics({lon:s.a[0],lat:s.a[1]},s)[2];es.forEach(e=>{let m=segMetrics(e,s),x=x0+Math.max(0,Math.min(1,(e.time-min)/(7*86400000)))*w,y=y0+(m[0]/L)*h;ctx.beginPath();ctx.arc(x,y,Math.max(2,2+e.mag),0,Math.PI*2);ctx.fillStyle=col(e.mag);ctx.fill()});stats.textContent=s.n+' • zaman-göç diyagramı • '+es.length+' olay';segmentInfo(es)}");
        h.append("function segmentInfo(es){let s=segs[sel],n24=0,max=-9,dep=0;es.forEach(e=>{if(Date.now()-e.time<=86400000)n24++;max=Math.max(max,e.mag);dep+=e.depth});info.innerHTML='<b>'+s.n+'</b><br>35 km koridor: '+es.length+' olay<br>Son 24s: '+n24+' • Mmax '+(max>-8?max.toFixed(1):'-')+'<br>Ort. derinlik: '+(es.length?(dep/es.length).toFixed(1):'-')+' km'}");
        h.append("function draw(){bg();if(mode==='map')drawMap();else if(mode==='section')drawSection();else if(mode==='along')drawAlong();else drawTime()}");
        h.append("function pickSeg(x,y){if(mode!=='map')return;let bd=22,bi=-1;segs.forEach((s,i)=>{let a=proj(s.a[0],s.a[1],0),b=proj(s.b[0],s.b[1],0),dx=b[0]-a[0],dy=b[1]-a[1],l=dx*dx+dy*dy,t=l?((x-a[0])*dx+(y-a[1])*dy)/l:0;t=Math.max(0,Math.min(1,t));let px=a[0]+t*dx,py=a[1]+t*dy,d=Math.hypot(x-px,y-py);if(d<bd){bd=d;bi=i}});if(bi>=0){sel=bi;draw()}}");
        h.append("cv.addEventListener('pointerdown',e=>{drag=true;lx=e.clientX;ly=e.clientY});cv.addEventListener('pointermove',e=>{if(!drag||mode!=='map')return;let dx=e.clientX-lx,dy=e.clientY-ly;rz+=dx*.008;rx+=dy*.006;rx=Math.max(.15,Math.min(1.35,rx));lx=e.clientX;ly=e.clientY;draw()});cv.addEventListener('pointerup',e=>{if(Math.abs(e.clientX-lx)<8&&Math.abs(e.clientY-ly)<8)pickSeg(e.clientX,e.clientY);drag=false});let ld=0;cv.addEventListener('touchmove',e=>{if(e.touches.length===2&&mode==='map'){let a=e.touches[0],b=e.touches[1],d=Math.hypot(a.clientX-b.clientX,a.clientY-b.clientY);if(ld){zoom*=d/ld;zoom=Math.max(.55,Math.min(3,zoom));draw()}ld=d;e.preventDefault()}},{passive:false});cv.addEventListener('touchend',()=>ld=0);");
        h.append("window.setMode=m=>{mode=m;title.textContent=m==='map'?'MARMARA 3B HİPOSANTR BULUTU':m==='section'?'FAYA DİK DERİNLİK KESİTİ':m==='along'?'FAY BOYUNCA HİPOSANTR DAĞILIMI':'ZAMAN • GÖÇ DİYAGRAMI';draw()};resize();</script></body></html>");
        web.loadDataWithBaseURL("https://localhost/",h.toString(),"text/html","UTF-8",null);
    }

    private void js(String s){if(web!=null)web.evaluateJavascript("window."+s,null);}private LinearLayout.LayoutParams lp(){return new LinearLayout.LayoutParams(0,dp(54),1);}private Button btn(String s,int c){Button b=new Button(this);b.setText(s);b.setTextColor(Color.rgb(5,10,18));b.setTextSize(9);b.setTypeface(Typeface.DEFAULT_BOLD);b.setAllCaps(false);GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(13));b.setBackground(g);return b;}private TextView t(String s,int sp,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);return v;}private GradientDrawable grad(int a,int b){return new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});}private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density);}
}
