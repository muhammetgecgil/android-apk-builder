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

public class MainActivityV7 extends AppCompatActivity {
    static final int REQ=10;
    PreviewView preview; Overlay overlay; TextView result,status; Button mode,pen;
    volatile Bitmap latest; volatile List<RectF> boxes=new ArrayList<>();
    boolean same=false; RectF sample;
    final ExecutorService camExec=Executors.newSingleThreadExecutor();
    final ExecutorService detExec=Executors.newSingleThreadExecutor();
    final AtomicBoolean busy=new AtomicBoolean(false);
    final ObjectDetector detector=ObjectDetection.getClient(new ObjectDetectorOptions.Builder().setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE).enableMultipleObjects().build());

    @Override protected void onCreate(Bundle b){super.onCreate(b);ui();if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)camera();else ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},REQ);}

    void ui(){
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);
        preview=new PreviewView(this);preview.setScaleType(PreviewView.ScaleType.FILL_CENTER);root.addView(preview,new FrameLayout.LayoutParams(-1,-1));
        overlay=new Overlay();root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(14),0,dp(14),0);top.setBackgroundColor(Color.argb(205,10,18,22));
        TextView title=new TextView(this);title.setText("Nesne Sayar AI");title.setTextColor(Color.WHITE);title.setTextSize(22);top.addView(title,new LinearLayout.LayoutParams(0,dp(58),1));
        status=new TextView(this);status.setText("Fill motoru hazır");status.setTextColor(Color.LTGRAY);status.setTextSize(12);status.setGravity(Gravity.CENTER);top.addView(status,new LinearLayout.LayoutParams(dp(180),dp(58)));
        result=new TextView(this);result.setText("0");result.setTextColor(Color.rgb(45,245,130));result.setTextSize(34);result.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);top.addView(result,new LinearLayout.LayoutParams(dp(58),dp(58)));
        FrameLayout.LayoutParams tlp=new FrameLayout.LayoutParams(-1,dp(58),Gravity.TOP);root.addView(top,tlp);
        pen=btn("✎");pen.setTextSize(24);FrameLayout.LayoutParams plp=new FrameLayout.LayoutParams(dp(58),dp(58),Gravity.TOP|Gravity.START);plp.leftMargin=dp(8);plp.topMargin=dp(68);root.addView(pen,plp);
        LinearLayout bot=new LinearLayout(this);Button src=btn("CANLI");mode=btn("FARKLI");Button count=btn("SAY"),clear=btn("SİL");count.setBackgroundColor(Color.rgb(0,145,78));bot.addView(src,new LinearLayout.LayoutParams(0,dp(60),1));bot.addView(mode,new LinearLayout.LayoutParams(0,dp(60),1));bot.addView(count,new LinearLayout.LayoutParams(0,dp(60),1));bot.addView(clear,new LinearLayout.LayoutParams(0,dp(60),1));FrameLayout.LayoutParams blp=new FrameLayout.LayoutParams(-1,dp(60),Gravity.BOTTOM);root.addView(bot,blp);
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,i)->{Insets x=i.getInsets(WindowInsetsCompat.Type.systemBars());tlp.topMargin=x.top;plp.topMargin=x.top+dp(66);blp.bottomMargin=x.bottom+dp(10);top.setLayoutParams(tlp);pen.setLayoutParams(plp);bot.setLayoutParams(blp);return i;});
        setContentView(root);
        pen.setOnClickListener(v->{overlay.drawMode=!overlay.drawMode;pen.setBackgroundColor(overlay.drawMode?Color.rgb(0,145,78):Color.rgb(19,58,68));status.setText(overlay.drawMode?"Bölgeyi çiz":"ROI kilitli");});
        mode.setOnClickListener(v->{same=!same;mode.setText(same?"AYNI":"FARKLI");sample=null;overlay.sample=null;overlay.invalidate();refresh();});
        count.setOnClickListener(v->{Bitmap f=latest;if(f==null){Toast.makeText(this,"Kamera hazırlanıyor",Toast.LENGTH_SHORT).show();return;}analyze(f.copy(Bitmap.Config.ARGB_8888,false));});
        clear.setOnClickListener(v->{boxes=new ArrayList<>();sample=null;overlay.boxes=Collections.emptyList();overlay.sample=null;overlay.roi.clear();overlay.invalidate();result.setText("0");status.setText("Temizlendi");});
    }
    Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setAllCaps(false);b.setBackgroundColor(Color.rgb(19,58,68));return b;}

    void camera(){ListenableFuture<ProcessCameraProvider> f=ProcessCameraProvider.getInstance(this);f.addListener(()->{try{ProcessCameraProvider p=f.get();Preview pr=new Preview.Builder().build();pr.setSurfaceProvider(preview.getSurfaceProvider());ImageAnalysis a=new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build();a.setAnalyzer(camExec,img->{try{Bitmap b=rgba(img),c=rotate(b,img.getImageInfo().getRotationDegrees());latest=c.copy(Bitmap.Config.ARGB_8888,false);}catch(Throwable ignored){}finally{img.close();}});p.unbindAll();p.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,pr,a);}catch(Throwable e){Toast.makeText(this,"Kamera başlatılamadı",Toast.LENGTH_LONG).show();}},ContextCompat.getMainExecutor(this));}
    Bitmap rgba(ImageProxy im){ImageProxy.PlaneProxy p=im.getPlanes()[0];ByteBuffer bf=p.getBuffer();bf.rewind();int ps=Math.max(1,p.getPixelStride()),pw=p.getRowStride()/ps;Bitmap pad=Bitmap.createBitmap(pw,im.getHeight(),Bitmap.Config.ARGB_8888);pad.copyPixelsFromBuffer(bf);return pw==im.getWidth()?pad:Bitmap.createBitmap(pad,0,0,im.getWidth(),im.getHeight());}
    Bitmap rotate(Bitmap s,int d){if(d==0)return s;Matrix m=new Matrix();m.postRotate(d);return Bitmap.createBitmap(s,0,0,s.getWidth(),s.getHeight(),m,true);}

    void analyze(Bitmap src){
        if(!busy.compareAndSet(false,true))return;status.setText("Dış sınır + Fill...");
        List<PointF> roiView=overlay.copyRoi();int vw=overlay.getWidth(),vh=overlay.getHeight();
        detExec.execute(()->{try{
            List<RectF> raw=new ArrayList<>();detect(src,0,0,src.getWidth(),src.getHeight(),raw);
            int g=5,cw=src.getWidth()/g,ch=src.getHeight()/g,px=(int)(cw*.22f),py=(int)(ch*.22f);
            for(int y=0;y<g;y++)for(int x=0;x<g;x++){int l=Math.max(0,x*cw-px),t=Math.max(0,y*ch-py),r=Math.min(src.getWidth(),(x+1)*cw+px),b=Math.min(src.getHeight(),(y+1)*ch+py);if(r-l>80&&b-t>80)detect(src,l,t,r,b,raw);}
            List<RectF> out=paintFillObjects(src,raw,roiView,vw,vh);boxes=out;
            runOnUiThread(()->{overlay.boxes=new ArrayList<>(out);overlay.invalidate();refresh();status.setText("Fiziksel nesne: "+out.size()+" / AI aday: "+raw.size());});
        }catch(Throwable e){runOnUiThread(()->status.setText("Algılama hatası"));}finally{busy.set(false);}});
    }

    void detect(Bitmap src,int l,int t,int r,int b,List<RectF> out)throws Exception{Bitmap c=Bitmap.createBitmap(src,l,t,r-l,b-t);for(DetectedObject o:Tasks.await(detector.process(InputImage.fromBitmap(c,0)))){android.graphics.Rect q=o.getBoundingBox();RectF z=new RectF(q.left+l,q.top+t,q.right+l,q.bottom+t);if(z.width()>10&&z.height()>10)out.add(z);}}

    List<RectF> paintFillObjects(Bitmap src,List<RectF> raw,List<PointF> roiView,int vw,int vh){
        int W=src.getWidth(),H=src.getHeight();float sc=Math.min(1f,420f/Math.max(W,H));int w=Math.max(80,Math.round(W*sc)),h=Math.max(80,Math.round(H*sc));
        Bitmap sm=Bitmap.createScaledBitmap(src,w,h,true);int[] pix=new int[w*h];sm.getPixels(pix,0,w,0,0,w,h);
        boolean[] roi=new boolean[w*h];int roiCount=0;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){boolean ok=roiView.isEmpty()||pipImage(x/sc,y/sc,W,H,roiView,vw,vh);roi[y*w+x]=ok;if(ok)roiCount++;}
        if(roiCount<100)return conservativeBoxes(raw,W,H,roiView,vw,vh);

        int[] bg=estimateBackground(pix,roi,w,h);double bgY=.299*bg[0]+.587*bg[1]+.114*bg[2];
        boolean[] fg=new boolean[w*h];
        for(int y=1;y<h-1;y++)for(int x=1;x<w-1;x++){int i=y*w+x;if(!roi[i])continue;int c=pix[i],r=Color.red(c),g=Color.green(c),b=Color.blue(c);double d=Math.sqrt((r-bg[0])*(r-bg[0])+(g-bg[1])*(g-bg[1])+(b-bg[2])*(b-bg[2]));double lum=.299*r+.587*g+.114*b;int gx=colorDiff(pix[i-1],pix[i+1]),gy=colorDiff(pix[i-w],pix[i+w]);boolean edge=gx+gy>72;fg[i]=(d>34&&Math.abs(lum-bgY)>9)||d>48||edge;}
        // Paint-like close/fill: bridge broken contours, then fill internal holes.
        for(int k=0;k<3;k++)fg=dilate(fg,roi,w,h);for(int k=0;k<2;k++)fg=erode(fg,roi,w,h);
        fillHoles(fg,roi,w,h);
        fg=erode(fg,roi,w,h);fg=dilate(fg,roi,w,h);

        Seg seg=components(fg,roi,w,h,roiCount);
        List<RectF> physical=new ArrayList<>();
        for(Comp cp:seg.comps){RectF r=new RectF(cp.l/sc,cp.t/sc,(cp.r+1)/sc,(cp.b+1)/sc);if(r.width()<12||r.height()<12)continue;float af=area(r)/(W*(float)H);if(af>.48f)continue;int support=0;for(RectF a:raw){if(a.centerX()>=r.left&&a.centerX()<=r.right&&a.centerY()>=r.top&&a.centerY()<=r.bottom)support++;else if(iou(a,r)>.10f)support++;}float compact=cp.area/Math.max(1f,(cp.r-cp.l+1f)*(cp.b-cp.t+1f));if(support>0||cp.area>roiCount*.010f&&compact>.16f)physical.add(r);}

        // Merge only filled regions that practically describe the same outer silhouette.
        physical.sort((a,b)->Float.compare(area(b),area(a)));List<RectF> keep=new ArrayList<>();
        for(RectF c:physical){boolean dup=false;for(RectF k:keep){if(iou(c,k)>.38f||contain(c,k)>.88f||contain(k,c)>.88f){dup=true;break;}}if(!dup)keep.add(c);}
        if(keep.isEmpty())keep=conservativeBoxes(raw,W,H,roiView,vw,vh);
        keep.sort((a,b)->{int q=Float.compare(a.centerY(),b.centerY());return q!=0?q:Float.compare(a.centerX(),b.centerX());});return keep;
    }

    int[] estimateBackground(int[] p,boolean[] roi,int w,int h){long sr=0,sg=0,sb=0;int n=0;for(int y=1;y<h-1;y++)for(int x=1;x<w-1;x++){int i=y*w+x;if(!roi[i])continue;boolean boundary=!roi[i-1]||!roi[i+1]||!roi[i-w]||!roi[i+w]||x<4||y<4||x>w-5||y>h-5;if(boundary){int c=p[i];sr+=Color.red(c);sg+=Color.green(c);sb+=Color.blue(c);n++;}}if(n<20){for(int i=0;i<p.length;i+=Math.max(1,p.length/500)){if(roi[i]){int c=p[i];sr+=Color.red(c);sg+=Color.green(c);sb+=Color.blue(c);n++;}}}return new int[]{(int)(sr/Math.max(1,n)),(int)(sg/Math.max(1,n)),(int)(sb/Math.max(1,n))};}
    int colorDiff(int a,int b){return Math.abs(Color.red(a)-Color.red(b))+Math.abs(Color.green(a)-Color.green(b))+Math.abs(Color.blue(a)-Color.blue(b));}
    boolean[] dilate(boolean[] a,boolean[] roi,int w,int h){boolean[] o=new boolean[a.length];for(int y=1;y<h-1;y++)for(int x=1;x<w-1;x++){int i=y*w+x;if(!roi[i])continue;boolean v=false;for(int dy=-1;dy<=1&&!v;dy++)for(int dx=-1;dx<=1;dx++)if(a[i+dy*w+dx]){v=true;break;}o[i]=v;}return o;}
    boolean[] erode(boolean[] a,boolean[] roi,int w,int h){boolean[] o=new boolean[a.length];for(int y=1;y<h-1;y++)for(int x=1;x<w-1;x++){int i=y*w+x;if(!roi[i]||!a[i])continue;boolean v=true;for(int dy=-1;dy<=1&&v;dy++)for(int dx=-1;dx<=1;dx++)if(!roi[i+dy*w+dx]||!a[i+dy*w+dx]){v=false;break;}o[i]=v;}return o;}
    void fillHoles(boolean[] fg,boolean[] roi,int w,int h){boolean[] seen=new boolean[fg.length];int[] q=new int[fg.length];int qs=0,qe=0;for(int x=0;x<w;x++){int a=x,b=(h-1)*w+x;if(roi[a]&&!fg[a]&&!seen[a]){seen[a]=true;q[qe++]=a;}if(roi[b]&&!fg[b]&&!seen[b]){seen[b]=true;q[qe++]=b;}}for(int y=0;y<h;y++){int a=y*w,b=y*w+w-1;if(roi[a]&&!fg[a]&&!seen[a]){seen[a]=true;q[qe++]=a;}if(roi[b]&&!fg[b]&&!seen[b]){seen[b]=true;q[qe++]=b;}}while(qs<qe){int i=q[qs++],x=i%w,y=i/w;int[] ns={i-1,i+1,i-w,i+w};for(int j:ns){if(j<0||j>=fg.length)continue;int nx=j%w,ny=j/w;if(Math.abs(nx-x)+Math.abs(ny-y)!=1)continue;if(roi[j]&&!fg[j]&&!seen[j]){seen[j]=true;q[qe++]=j;}}}for(int i=0;i<fg.length;i++)if(roi[i]&&!fg[i]&&!seen[i])fg[i]=true;}

    Seg components(boolean[] fg,boolean[] roi,int w,int h,int roiCount){int[] lab=new int[fg.length];List<Comp> cs=new ArrayList<>();int id=0;int[] q=new int[fg.length];for(int i=0;i<fg.length;i++){if(!fg[i]||lab[i]!=0)continue;id++;int qs=0,qe=0;q[qe++]=i;lab[i]=id;Comp c=new Comp();c.l=c.r=i%w;c.t=c.b=i/w;while(qs<qe){int z=q[qs++],x=z%w,y=z/w;c.area++;c.l=Math.min(c.l,x);c.r=Math.max(c.r,x);c.t=Math.min(c.t,y);c.b=Math.max(c.b,y);for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){if(dx==0&&dy==0)continue;int nx=x+dx,ny=y+dy;if(nx<0||ny<0||nx>=w||ny>=h)continue;int j=ny*w+nx;if(fg[j]&&lab[j]==0){lab[j]=id;q[qe++]=j;}}}if(c.area>Math.max(25,roiCount*.0015f))cs.add(c);}return new Seg(lab,cs);}
    static class Comp{int l,t,r,b,area;} static class Seg{int[] lab;List<Comp> comps;Seg(int[]l,List<Comp>c){lab=l;comps=c;}}

    List<RectF> conservativeBoxes(List<RectF> raw,int W,int H,List<PointF> roi,int vw,int vh){List<RectF>a=new ArrayList<>();float frame=W*(float)H;for(RectF r:raw){if(area(r)<frame*.0002f||area(r)>frame*.45f)continue;if(!roi.isEmpty()&&!insideRoi(r,W,H,roi,vw,vh))continue;a.add(new RectF(r));}a.sort((x,y)->Float.compare(area(y),area(x)));List<RectF>out=new ArrayList<>();for(RectF c:a){boolean d=false;for(RectF k:out)if(iou(c,k)>.36f||overSmall(c,k)>.80f){d=true;break;}if(!d)out.add(c);}return out;}

    boolean pipImage(float ix,float iy,int W,int H,List<PointF> poly,int vw,int vh){PointF p=toView(ix,iy,W,H,vw,vh);return pip(p.x,p.y,poly);}boolean insideRoi(RectF r,int W,int H,List<PointF> poly,int vw,int vh){PointF c=toView(r.centerX(),r.centerY(),W,H,vw,vh);return pip(c.x,c.y,poly);}PointF toView(float x,float y,int W,int H,int vw,int vh){float s=Math.max(vw/(float)W,vh/(float)H),dx=(vw-W*s)/2f,dy=(vh-H*s)/2f;return new PointF(x*s+dx,y*s+dy);}boolean pip(float x,float y,List<PointF> p){if(p.size()<3)return true;boolean c=false;for(int i=0,j=p.size()-1;i<p.size();j=i++){PointF a=p.get(i),b=p.get(j);if(((a.y>y)!=(b.y>y))&&(x<(b.x-a.x)*(y-a.y)/(b.y-a.y+.00001f)+a.x))c=!c;}return c;}
    float area(RectF r){return Math.max(1,r.width()*r.height());}float inter(RectF a,RectF b){float l=Math.max(a.left,b.left),t=Math.max(a.top,b.top),rr=Math.min(a.right,b.right),bb=Math.min(a.bottom,b.bottom);return rr>l&&bb>t?(rr-l)*(bb-t):0;}float iou(RectF a,RectF b){float i=inter(a,b);return i/(area(a)+area(b)-i);}float contain(RectF a,RectF b){return inter(a,b)/area(a);}float overSmall(RectF a,RectF b){return inter(a,b)/Math.min(area(a),area(b));}

    void refresh(){if(!same||sample==null){result.setText(String.valueOf(boxes.size()));return;}int n=0;for(RectF r:boxes){float ar=Math.min(area(r),area(sample))/Math.max(area(r),area(sample));float ra=r.width()/Math.max(1,r.height()),rb=sample.width()/Math.max(1,sample.height());float asp=Math.min(ra,rb)/Math.max(ra,rb);if(ar>.38f&&asp>.55f)n++;}result.setText(String.valueOf(n));}
    int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
    @Override public void onRequestPermissionsResult(int r,@NonNull String[] p,@NonNull int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)camera();}

    class Overlay extends View{
        Paint boxP=new Paint(1),numP=new Paint(1),roiP=new Paint(1),sampleP=new Paint(1);List<RectF> boxes=Collections.emptyList();List<PointF> roi=new ArrayList<>();boolean drawMode=false;RectF sample;
        Overlay(){super(MainActivityV7.this);boxP.setStyle(Paint.Style.STROKE);boxP.setStrokeWidth(dp(3));boxP.setColor(Color.rgb(35,245,135));roiP.setStyle(Paint.Style.STROKE);roiP.setStrokeWidth(dp(4));roiP.setColor(Color.CYAN);sampleP.setStyle(Paint.Style.STROKE);sampleP.setStrokeWidth(dp(4));sampleP.setColor(Color.YELLOW);numP.setColor(Color.rgb(0,180,105));}
        List<PointF> copyRoi(){List<PointF>x=new ArrayList<>();for(PointF p:roi)x.add(new PointF(p.x,p.y));return x;}
        @Override protected void onDraw(Canvas c){super.onDraw(c);if(roi.size()>1){Path p=new Path();p.moveTo(roi.get(0).x,roi.get(0).y);for(int i=1;i<roi.size();i++)p.lineTo(roi.get(i).x,roi.get(i).y);if(!drawMode)p.close();c.drawPath(p,roiP);}for(int i=0;i<boxes.size();i++){RectF r=toScreen(boxes.get(i));c.drawRect(r,boxP);float rad=dp(22);c.drawCircle(r.centerX(),Math.max(rad,r.top),rad,numP);Paint t=new Paint(1);t.setColor(Color.WHITE);t.setTextAlign(Paint.Align.CENTER);t.setTextSize(dp(18));c.drawText(String.valueOf(i+1),r.centerX(),Math.max(rad,r.top)+dp(6),t);}if(sample!=null)c.drawRect(toScreen(sample),sampleP);}
        RectF toScreen(RectF r){Bitmap f=latest;if(f==null)return new RectF(r);float s=Math.max(getWidth()/(float)f.getWidth(),getHeight()/(float)f.getHeight()),dx=(getWidth()-f.getWidth()*s)/2f,dy=(getHeight()-f.getHeight()*s)/2f;return new RectF(r.left*s+dx,r.top*s+dy,r.right*s+dx,r.bottom*s+dy);}
        @Override public boolean onTouchEvent(android.view.MotionEvent e){if(drawMode){if(e.getAction()==MotionEvent.ACTION_DOWN){roi.clear();roi.add(new PointF(e.getX(),e.getY()));invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){PointF last=roi.get(roi.size()-1);float dx=e.getX()-last.x,dy=e.getY()-last.y;if(dx*dx+dy*dy>16){roi.add(new PointF(e.getX(),e.getY()));invalidate();}return true;}if(e.getAction()==MotionEvent.ACTION_UP){roi.add(new PointF(e.getX(),e.getY()));drawMode=false;pen.setBackgroundColor(Color.rgb(19,58,68));status.setText("ROI hazır");invalidate();return true;}}else if(same&&e.getAction()==MotionEvent.ACTION_DOWN){for(RectF b:boxes){RectF s=toScreen(b);if(s.contains(e.getX(),e.getY())){sample=new RectF(b);MainActivityV7.this.sample=new RectF(b);invalidate();refresh();return true;}}}return true;}
    }
}
