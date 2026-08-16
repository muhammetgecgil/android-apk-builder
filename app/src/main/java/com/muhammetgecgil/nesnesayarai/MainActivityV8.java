package com.muhammetgecgil.nesnesayarai;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.gms.tasks.Tasks;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.*;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivityV8 extends AppCompatActivity {
    static final int REQ=10;
    PreviewView preview; Overlay overlay; TextView result,status; Button pen,mode;
    volatile Bitmap latest; volatile List<RectF> boxes=new ArrayList<>();
    boolean same=false; RectF sample;
    final ExecutorService camExec=Executors.newSingleThreadExecutor(), detExec=Executors.newSingleThreadExecutor();
    final AtomicBoolean busy=new AtomicBoolean(false);
    final ObjectDetector detector=ObjectDetection.getClient(new ObjectDetectorOptions.Builder().setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE).enableMultipleObjects().build());

    @Override protected void onCreate(Bundle b){super.onCreate(b);ui();if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)camera();else ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},REQ);}

    void ui(){
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);
        preview=new PreviewView(this);preview.setScaleType(PreviewView.ScaleType.FILL_CENTER);root.addView(preview,new FrameLayout.LayoutParams(-1,-1));
        overlay=new Overlay();root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(14),0,dp(14),0);top.setBackgroundColor(Color.argb(205,10,18,22));
        TextView title=new TextView(this);title.setText("Nesne Sayar AI");title.setTextColor(Color.WHITE);title.setTextSize(22);top.addView(title,new LinearLayout.LayoutParams(0,dp(58),1));
        status=new TextView(this);status.setText("Fiziksel görüş hazır");status.setTextColor(Color.LTGRAY);status.setTextSize(12);status.setGravity(Gravity.CENTER);top.addView(status,new LinearLayout.LayoutParams(dp(190),dp(58)));
        result=new TextView(this);result.setText("0");result.setTextColor(Color.rgb(45,245,130));result.setTextSize(34);result.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);top.addView(result,new LinearLayout.LayoutParams(dp(58),dp(58)));
        FrameLayout.LayoutParams tlp=new FrameLayout.LayoutParams(-1,dp(58),Gravity.TOP);root.addView(top,tlp);
        pen=btn("✎");pen.setTextSize(24);FrameLayout.LayoutParams plp=new FrameLayout.LayoutParams(dp(58),dp(58),Gravity.TOP|Gravity.START);plp.leftMargin=dp(8);plp.topMargin=dp(68);root.addView(pen,plp);
        LinearLayout bot=new LinearLayout(this);Button src=btn("CANLI");mode=btn("FARKLI");Button count=btn("SAY"),clear=btn("SİL");count.setBackgroundColor(Color.rgb(0,145,78));bot.addView(src,new LinearLayout.LayoutParams(0,dp(60),1));bot.addView(mode,new LinearLayout.LayoutParams(0,dp(60),1));bot.addView(count,new LinearLayout.LayoutParams(0,dp(60),1));bot.addView(clear,new LinearLayout.LayoutParams(0,dp(60),1));FrameLayout.LayoutParams blp=new FrameLayout.LayoutParams(-1,dp(60),Gravity.BOTTOM);root.addView(bot,blp);
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,i)->{Insets x=i.getInsets(WindowInsetsCompat.Type.systemBars());tlp.topMargin=x.top;plp.topMargin=x.top+dp(66);blp.bottomMargin=x.bottom+dp(10);top.setLayoutParams(tlp);pen.setLayoutParams(plp);bot.setLayoutParams(blp);return i;});
        setContentView(root);
        pen.setOnClickListener(v->{overlay.drawMode=!overlay.drawMode;pen.setBackgroundColor(overlay.drawMode?Color.rgb(0,145,78):Color.rgb(19,58,68));status.setText(overlay.drawMode?"Bölgeyi çiz":"ROI hazır");});
        mode.setOnClickListener(v->{same=!same;mode.setText(same?"AYNI":"FARKLI");sample=null;overlay.sample=null;overlay.invalidate();refresh();});
        count.setOnClickListener(v->{Bitmap f=latest;if(f==null){Toast.makeText(this,"Kamera hazırlanıyor",Toast.LENGTH_SHORT).show();return;}analyze(f.copy(Bitmap.Config.ARGB_8888,false));});
        clear.setOnClickListener(v->{boxes=new ArrayList<>();sample=null;overlay.boxes=Collections.emptyList();overlay.sample=null;overlay.roi.clear();overlay.invalidate();result.setText("0");status.setText("Temizlendi");});
    }
    Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setAllCaps(false);b.setBackgroundColor(Color.rgb(19,58,68));return b;}

    void camera(){ListenableFuture<ProcessCameraProvider> f=ProcessCameraProvider.getInstance(this);f.addListener(()->{try{ProcessCameraProvider p=f.get();Preview pr=new Preview.Builder().build();pr.setSurfaceProvider(preview.getSurfaceProvider());ImageAnalysis a=new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build();a.setAnalyzer(camExec,img->{try{Bitmap b=rgba(img),c=rotate(b,img.getImageInfo().getRotationDegrees());latest=c.copy(Bitmap.Config.ARGB_8888,false);}catch(Throwable ignored){}finally{img.close();}});p.unbindAll();p.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,pr,a);}catch(Throwable e){Toast.makeText(this,"Kamera başlatılamadı",Toast.LENGTH_LONG).show();}},ContextCompat.getMainExecutor(this));}
    Bitmap rgba(ImageProxy im){ImageProxy.PlaneProxy p=im.getPlanes()[0];ByteBuffer bf=p.getBuffer();bf.rewind();int ps=Math.max(1,p.getPixelStride()),pw=p.getRowStride()/ps;Bitmap pad=Bitmap.createBitmap(pw,im.getHeight(),Bitmap.Config.ARGB_8888);pad.copyPixelsFromBuffer(bf);return pw==im.getWidth()?pad:Bitmap.createBitmap(pad,0,0,im.getWidth(),im.getHeight());}
    Bitmap rotate(Bitmap s,int d){if(d==0)return s;Matrix m=new Matrix();m.postRotate(d);return Bitmap.createBitmap(s,0,0,s.getWidth(),s.getHeight(),m,true);}

    void analyze(Bitmap src){
        if(!busy.compareAndSet(false,true))return;status.setText("Kontur + Fill + bileşen...");
        List<PointF> roiView=overlay.copyRoi();int vw=overlay.getWidth(),vh=overlay.getHeight();
        detExec.execute(()->{try{
            List<RectF> raw=new ArrayList<>();detect(src,0,0,src.getWidth(),src.getHeight(),raw);
            int g=3,cw=src.getWidth()/g,ch=src.getHeight()/g,px=(int)(cw*.16f),py=(int)(ch*.16f);
            for(int y=0;y<g;y++)for(int x=0;x<g;x++){int l=Math.max(0,x*cw-px),t=Math.max(0,y*ch-py),r=Math.min(src.getWidth(),(x+1)*cw+px),b=Math.min(src.getHeight(),(y+1)*ch+py);if(r-l>90&&b-t>90)detect(src,l,t,r,b,raw);}
            List<RectF> out=physicalObjects(src,raw,roiView,vw,vh);boxes=out;
            runOnUiThread(()->{overlay.boxes=new ArrayList<>(out);overlay.invalidate();refresh();status.setText("Fiziksel nesne: "+out.size()+" / AI aday: "+raw.size());});
        }catch(Throwable e){runOnUiThread(()->status.setText("Algılama hatası"));}finally{busy.set(false);}});
    }

    void detect(Bitmap src,int l,int t,int r,int b,List<RectF> out)throws Exception{Bitmap c=Bitmap.createBitmap(src,l,t,r-l,b-t);for(DetectedObject o:Tasks.await(detector.process(InputImage.fromBitmap(c,0)))){android.graphics.Rect q=o.getBoundingBox();RectF z=new RectF(q.left+l,q.top+t,q.right+l,q.bottom+t);if(z.width()>10&&z.height()>10)out.add(z);}}

    List<RectF> physicalObjects(Bitmap src,List<RectF> raw,List<PointF> roiView,int vw,int vh){
        int W=src.getWidth(),H=src.getHeight();float sc=Math.min(1f,480f/Math.max(W,H));int w=Math.max(96,Math.round(W*sc)),h=Math.max(96,Math.round(H*sc));
        Bitmap sm=Bitmap.createScaledBitmap(src,w,h,true);int[] pix=new int[w*h];sm.getPixels(pix,0,w,0,0,w,h);
        boolean[] roi=new boolean[w*h];int roiCount=0;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){boolean ok=roiView.isEmpty()||pipImage(x/sc,y/sc,W,H,roiView,vw,vh);roi[y*w+x]=ok;if(ok)roiCount++;}
        if(roiCount<150)return fallback(raw,W,H,roiView,vw,vh);
        int[] bg=estimateBackground(pix,roi,w,h);double bgY=.299*bg[0]+.587*bg[1]+.114*bg[2];
        boolean[] fg=new boolean[w*h];
        for(int y=1;y<h-1;y++)for(int x=1;x<w-1;x++){int i=y*w+x;if(!roi[i])continue;int c=pix[i],r=Color.red(c),g=Color.green(c),b=Color.blue(c);double d=Math.sqrt((r-bg[0])*(r-bg[0])+(g-bg[1])*(g-bg[1])+(b-bg[2])*(b-bg[2]));double lum=.299*r+.587*g+.114*b;int edge=colorDiff(pix[i-1],pix[i+1])+colorDiff(pix[i-w],pix[i+w]);fg[i]=(d>36&&Math.abs(lum-bgY)>8)||d>52||edge>92;}
        // Dış kontur kapatma. ROI sınırını nesne olarak kullanma.
        fg=dilate(fg,roi,w,h);fg=dilate(fg,roi,w,h);fg=erode(fg,roi,w,h);fg=erode(fg,roi,w,h);
        fillHolesFromRoiBoundary(fg,roi,w,h);
        fg=erode(fg,roi,w,h);fg=dilate(fg,roi,w,h);
        List<Comp> comps=components(fg,roi,w,h,roiCount);
        List<RectF> out=new ArrayList<>();
        int minPix=Math.max(20,(int)(roiCount*.0012f));
        for(Comp c:comps){if(c.area<minPix)continue;float frac=c.area/(float)roiCount;if(frac>.82f)continue;int bw=c.r-c.l+1,bh=c.b-c.t+1;if(bw<5||bh<5)continue;float compact=c.area/(float)(bw*bh);if(compact<.045f&&c.area<roiCount*.008f)continue;RectF rr=new RectF(c.l/sc,c.t/sc,(c.r+1)/sc,(c.b+1)/sc);if(!mostlyInsideRoi(rr,W,H,roiView,vw,vh))continue;out.add(rr);}
        out=dedup(out);
        // Eğer fiziksel maske çökmüşse yalnızca o zaman AI yedeği kullan.
        if(out.isEmpty())out=fallback(raw,W,H,roiView,vw,vh);
        // Tek dev bileşen + birbirinden uzak AI adaları varsa, dev birleşimi reddet ve ayrık AI kümelerine dön.
        if(out.size()==1&&area(out.get(0))>W*H*.18f){List<RectF> ai=fallback(raw,W,H,roiView,vw,vh);if(ai.size()>=2&&wellSeparated(ai))out=ai;}
        out.sort((a,b)->{int q=Float.compare(a.centerY(),b.centerY());return q!=0?q:Float.compare(a.centerX(),b.centerX());});return out;
    }

    int[] estimateBackground(int[] p,boolean[] roi,int w,int h){long sr=0,sg=0,sb=0;int n=0;for(int y=1;y<h-1;y++)for(int x=1;x<w-1;x++){int i=y*w+x;if(!roi[i])continue;if(isRoiBoundary(roi,x,y,w,h)){int c=p[i];sr+=Color.red(c);sg+=Color.green(c);sb+=Color.blue(c);n++;}}if(n<30){for(int i=0;i<p.length;i+=Math.max(1,p.length/700))if(roi[i]){int c=p[i];sr+=Color.red(c);sg+=Color.green(c);sb+=Color.blue(c);n++;}}return new int[]{(int)(sr/Math.max(1,n)),(int)(sg/Math.max(1,n)),(int)(sb/Math.max(1,n))};}
    boolean isRoiBoundary(boolean[] roi,int x,int y,int w,int h){int i=y*w+x;if(!roi[i])return false;if(x==0||y==0||x==w-1||y==h-1)return true;return !roi[i-1]||!roi[i+1]||!roi[i-w]||!roi[i+w];}
    int colorDiff(int a,int b){return Math.abs(Color.red(a)-Color.red(b))+Math.abs(Color.green(a)-Color.green(b))+Math.abs(Color.blue(a)-Color.blue(b));}
    boolean[] dilate(boolean[] a,boolean[] roi,int w,int h){boolean[] o=new boolean[a.length];for(int y=1;y<h-1;y++)for(int x=1;x<w-1;x++){int i=y*w+x;if(!roi[i])continue;boolean v=false;for(int dy=-1;dy<=1&&!v;dy++)for(int dx=-1;dx<=1;dx++)if(a[i+dy*w+dx]){v=true;break;}o[i]=v;}return o;}
    boolean[] erode(boolean[] a,boolean[] roi,int w,int h){boolean[] o=new boolean[a.length];for(int y=1;y<h-1;y++)for(int x=1;x<w-1;x++){int i=y*w+x;if(!roi[i]||!a[i])continue;boolean v=true;for(int dy=-1;dy<=1&&v;dy++)for(int dx=-1;dx<=1;dx++)if(!roi[i+dy*w+dx]||!a[i+dy*w+dx]){v=false;break;}o[i]=v;}return o;}

    // Kritik düzeltme: dış arka planı görüntü kenarından değil, kullanıcının ROI sınırından flood-fill et.
    void fillHolesFromRoiBoundary(boolean[] fg,boolean[] roi,int w,int h){boolean[] seen=new boolean[fg.length];int[] q=new int[fg.length];int qs=0,qe=0;for(int y=0;y<h;y++)for(int x=0;x<w;x++){int i=y*w+x;if(roi[i]&&!fg[i]&&!seen[i]&&isRoiBoundary(roi,x,y,w,h)){seen[i]=true;q[qe++]=i;}}while(qs<qe){int i=q[qs++],x=i%w,y=i/w;int[] ns={i-1,i+1,i-w,i+w};for(int j:ns){if(j<0||j>=fg.length)continue;int nx=j%w,ny=j/w;if(Math.abs(nx-x)+Math.abs(ny-y)!=1)continue;if(roi[j]&&!fg[j]&&!seen[j]){seen[j]=true;q[qe++]=j;}}}for(int i=0;i<fg.length;i++)if(roi[i]&&!fg[i]&&!seen[i])fg[i]=true;}

    List<Comp> components(boolean[] fg,boolean[] roi,int w,int h,int roiCount){int[] lab=new int[fg.length],q=new int[fg.length];List<Comp> cs=new ArrayList<>();int id=0;for(int i=0;i<fg.length;i++){if(!fg[i]||lab[i]!=0)continue;id++;int qs=0,qe=0;q[qe++]=i;lab[i]=id;Comp c=new Comp();c.l=c.r=i%w;c.t=c.b=i/w;while(qs<qe){int z=q[qs++],x=z%w,y=z/w;c.area++;c.l=Math.min(c.l,x);c.r=Math.max(c.r,x);c.t=Math.min(c.t,y);c.b=Math.max(c.b,y);for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){if(dx==0&&dy==0)continue;int nx=x+dx,ny=y+dy;if(nx<0||ny<0||nx>=w||ny>=h)continue;int n=ny*w+nx;if(fg[n]&&lab[n]==0){lab[n]=id;q[qe++]=n;}}}cs.add(c);}return cs;}
    static class Comp{int l,r,t,b,area;}

    List<RectF> dedup(List<RectF> in){in.sort((a,b)->Float.compare(area(b),area(a)));List<RectF> k=new ArrayList<>();for(RectF c:in){boolean d=false;for(RectF p:k)if(iou(c,p)>.42f||contain(c,p)>.90f||contain(p,c)>.90f){d=true;break;}if(!d)k.add(c);}return k;}
    List<RectF> fallback(List<RectF> raw,int W,int H,List<PointF> roi,int vw,int vh){List<RectF> a=new ArrayList<>();float frame=W*(float)H;for(RectF r:raw){float ar=area(r);if(ar<frame*.00025f||ar>frame*.35f)continue;if(!mostlyInsideRoi(r,W,H,roi,vw,vh))continue;a.add(new RectF(r));}a.sort((x,y)->Float.compare(area(y),area(x)));List<RectF> k=new ArrayList<>();for(RectF c:a){boolean d=false;for(RectF p:k){if(iou(c,p)>.30f||contain(c,p)>.78f||contain(p,c)>.78f){d=true;break;}}if(!d)k.add(c);}return k;}
    boolean wellSeparated(List<RectF> a){if(a.size()<2)return false;for(int i=0;i<a.size();i++)for(int j=i+1;j<a.size();j++){RectF x=a.get(i),y=a.get(j);float dx=x.centerX()-y.centerX(),dy=x.centerY()-y.centerY();float d=(float)Math.sqrt(dx*dx+dy*dy);float s=(float)Math.sqrt(Math.max(area(x),area(y)));if(d>s*.75f&&iou(x,y)<.08f)return true;}return false;}

    boolean mostlyInsideRoi(RectF r,int W,int H,List<PointF> roi,int vw,int vh){if(roi.isEmpty()||vw<=0||vh<=0)return true;float[] xs={r.left,r.centerX(),r.right},ys={r.top,r.centerY(),r.bottom};int in=0;for(float x:xs)for(float y:ys){PointF p=imageToView(x,y,W,H,vw,vh);if(pip(p.x,p.y,roi))in++;}PointF c=imageToView(r.centerX(),r.centerY(),W,H,vw,vh);return pip(c.x,c.y,roi)&&in>=6;}
    boolean pipImage(float x,float y,int W,int H,List<PointF> roi,int vw,int vh){PointF p=imageToView(x,y,W,H,vw,vh);return pip(p.x,p.y,roi);}
    PointF imageToView(float x,float y,int W,int H,int vw,int vh){float sc=Math.max(vw/(float)W,vh/(float)H),dx=(vw-W*sc)/2f,dy=(vh-H*sc)/2f;return new PointF(x*sc+dx,y*sc+dy);}
    boolean pip(float x,float y,List<PointF> poly){boolean c=false;int n=poly.size();if(n<3)return true;for(int i=0,j=n-1;i<n;j=i++){PointF a=poly.get(i),b=poly.get(j);if(((a.y>y)!=(b.y>y))&&(x<(b.x-a.x)*(y-a.y)/(b.y-a.y+0.00001f)+a.x))c=!c;}return c;}
    float area(RectF r){return Math.max(1f,r.width()*r.height());}float inter(RectF a,RectF b){float l=Math.max(a.left,b.left),t=Math.max(a.top,b.top),r=Math.min(a.right,b.right),bb=Math.min(a.bottom,b.bottom);return r>l&&bb>t?(r-l)*(bb-t):0f;}float iou(RectF a,RectF b){float i=inter(a,b);return i/(area(a)+area(b)-i);}float contain(RectF a,RectF b){return inter(a,b)/area(a);}

    void refresh(){if(!same||sample==null){result.setText(String.valueOf(boxes.size()));return;}int n=0;float sa=area(sample),asp=aspect(sample);for(RectF r:boxes){float sz=Math.min(area(r),sa)/Math.max(area(r),sa);if(sz>.42f&&Math.abs(aspect(r)-asp)<.55f)n++;}result.setText(String.valueOf(n));status.setText("AYNI: "+n);}
    float aspect(RectF r){float x=r.width()/Math.max(1f,r.height());return Math.max(x,1f/Math.max(.001f,x));}
    int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
    @Override public void onRequestPermissionsResult(int r,@NonNull String[] p,@NonNull int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)camera();}
    @Override protected void onDestroy(){super.onDestroy();detector.close();camExec.shutdownNow();detExec.shutdownNow();}

    class Overlay extends View{
        Paint box=new Paint(1),num=new Paint(1),txt=new Paint(1),rp=new Paint(1),sp=new Paint(1);List<RectF> boxes=new ArrayList<>();List<PointF> roi=new ArrayList<>();Path path=new Path();boolean drawMode=false;RectF sample;
        Overlay(){super(MainActivityV8.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);box.setColor(Color.rgb(45,245,130));box.setStyle(Paint.Style.STROKE);box.setStrokeWidth(dp(3));num.setColor(Color.rgb(0,165,88));txt.setColor(Color.WHITE);txt.setTextAlign(Paint.Align.CENTER);txt.setTextSize(dp(16));txt.setFakeBoldText(true);rp.setColor(Color.CYAN);rp.setStyle(Paint.Style.STROKE);rp.setStrokeWidth(dp(4));sp.setColor(Color.YELLOW);sp.setStyle(Paint.Style.STROKE);sp.setStrokeWidth(dp(4));setOnTouchListener((v,e)->{if(drawMode){if(e.getAction()==MotionEvent.ACTION_DOWN){roi.clear();path.reset();roi.add(new PointF(e.getX(),e.getY()));path.moveTo(e.getX(),e.getY());invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){roi.add(new PointF(e.getX(),e.getY()));path.lineTo(e.getX(),e.getY());invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_UP){roi.add(new PointF(e.getX(),e.getY()));path.close();drawMode=false;pen.setBackgroundColor(Color.rgb(19,58,68));status.setText("ROI hazır");invalidate();return true;}}else if(same&&e.getAction()==MotionEvent.ACTION_UP){RectF best=null;float ba=Float.MAX_VALUE;for(RectF r:boxes){RectF m=map(r);if(m.contains(e.getX(),e.getY())&&area(m)<ba){best=r;ba=area(m);}}if(best!=null){MainActivityV8.this.sample=new RectF(best);sample=new RectF(best);refresh();invalidate();}return true;}return true;});}
        List<PointF> copyRoi(){List<PointF> a=new ArrayList<>();for(PointF p:roi)a.add(new PointF(p.x,p.y));return a;}
        @Override protected void onDraw(Canvas c){super.onDraw(c);if(!roi.isEmpty())c.drawPath(path,rp);for(int i=0;i<boxes.size();i++){RectF r=map(boxes.get(i));c.drawRoundRect(r,dp(7),dp(7),box);float cx=r.centerX(),cy=Math.max(dp(82),r.top+dp(3));c.drawCircle(cx,cy,dp(16),num);c.drawText(""+(i+1),cx,cy+dp(6),txt);}if(sample!=null)c.drawRoundRect(map(sample),dp(9),dp(9),sp);}
        RectF map(RectF s){Bitmap f=latest;if(f==null)return new RectF(s);float vw=getWidth(),vh=getHeight(),sw=f.getWidth(),sh=f.getHeight(),sc=Math.max(vw/sw,vh/sh),dx=(vw-sw*sc)/2f,dy=(vh-sh*sc)/2f;return new RectF(s.left*sc+dx,s.top*sc+dy,s.right*sc+dx,s.bottom*sc+dy);}
    }
}
