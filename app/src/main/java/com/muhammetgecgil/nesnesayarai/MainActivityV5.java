package com.muhammetgecgil.nesnesayarai;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
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
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivityV5 extends AppCompatActivity {
    private static final int REQ_CAMERA=10;
    private PreviewView previewView;
    private SmartOverlay overlay;
    private TextView resultText,statusText;
    private Button modeButton,penButton;
    private volatile Bitmap latestFrame;
    private volatile List<RectF> detections=new ArrayList<>();
    private boolean sameMode=false;
    private RectF sampleRect;
    private final ExecutorService cameraExecutor=Executors.newSingleThreadExecutor();
    private final ExecutorService detectExecutor=Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy=new AtomicBoolean(false);

    private final ObjectDetector detector=ObjectDetection.getClient(new ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects().build());

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);buildUi();
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)startCamera();
        else ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},REQ_CAMERA);
    }

    private void buildUi(){
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);
        previewView=new PreviewView(this);previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);root.addView(previewView,new FrameLayout.LayoutParams(-1,-1));
        overlay=new SmartOverlay();root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(14),0,dp(14),0);top.setBackgroundColor(Color.argb(205,10,18,22));
        TextView title=new TextView(this);title.setText("Nesne Sayar AI");title.setTextColor(Color.WHITE);title.setTextSize(22);top.addView(title,new LinearLayout.LayoutParams(0,dp(58),1));
        statusText=new TextView(this);statusText.setText("Hazır");statusText.setTextColor(Color.LTGRAY);statusText.setTextSize(12);statusText.setGravity(Gravity.CENTER);top.addView(statusText,new LinearLayout.LayoutParams(dp(165),dp(58)));
        resultText=new TextView(this);resultText.setText("0");resultText.setTextColor(Color.rgb(45,245,130));resultText.setTextSize(34);resultText.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);top.addView(resultText,new LinearLayout.LayoutParams(dp(58),dp(58)));
        FrameLayout.LayoutParams topLp=new FrameLayout.LayoutParams(-1,dp(58),Gravity.TOP);root.addView(top,topLp);

        penButton=new Button(this);penButton.setText("✎");penButton.setTextSize(24);penButton.setTextColor(Color.WHITE);penButton.setBackgroundColor(Color.argb(160,45,45,45));
        FrameLayout.LayoutParams penLp=new FrameLayout.LayoutParams(dp(58),dp(58),Gravity.TOP|Gravity.START);penLp.leftMargin=dp(8);penLp.topMargin=dp(68);root.addView(penButton,penLp);

        LinearLayout bottom=new LinearLayout(this);bottom.setBackgroundColor(Color.rgb(15,48,58));
        Button src=button("CANLI");modeButton=button("FARKLI");Button count=button("SAY");Button clear=button("SİL");count.setBackgroundColor(Color.rgb(0,145,78));
        bottom.addView(src,new LinearLayout.LayoutParams(0,dp(60),1));bottom.addView(modeButton,new LinearLayout.LayoutParams(0,dp(60),1));bottom.addView(count,new LinearLayout.LayoutParams(0,dp(60),1));bottom.addView(clear,new LinearLayout.LayoutParams(0,dp(60),1));
        FrameLayout.LayoutParams bottomLp=new FrameLayout.LayoutParams(-1,dp(60),Gravity.BOTTOM);root.addView(bottom,bottomLp);

        ViewCompat.setOnApplyWindowInsetsListener(root,(v,i)->{Insets bars=i.getInsets(WindowInsetsCompat.Type.systemBars());topLp.topMargin=bars.top;penLp.topMargin=bars.top+dp(66);bottomLp.bottomMargin=bars.bottom+dp(10);top.setLayoutParams(topLp);penButton.setLayoutParams(penLp);bottom.setLayoutParams(bottomLp);return i;});
        setContentView(root);

        penButton.setOnClickListener(v->{overlay.drawingMode=!overlay.drawingMode;penButton.setBackgroundColor(overlay.drawingMode?Color.rgb(0,145,78):Color.argb(160,45,45,45));statusText.setText(overlay.drawingMode?"Bölgeyi çiz":"ROI hazır");});
        src.setOnClickListener(v->Toast.makeText(this,"Canlı kamera aktif",Toast.LENGTH_SHORT).show());
        modeButton.setOnClickListener(v->{sameMode=!sameMode;modeButton.setText(sameMode?"AYNI":"FARKLI");sampleRect=null;overlay.sample=null;overlay.invalidate();refreshCount();});
        count.setOnClickListener(v->{Bitmap f=latestFrame;if(f==null){Toast.makeText(this,"Kamera hazırlanıyor",Toast.LENGTH_SHORT).show();return;}analyze(f.copy(Bitmap.Config.ARGB_8888,false));});
        clear.setOnClickListener(v->{detections=new ArrayList<>();sampleRect=null;overlay.boxes=Collections.emptyList();overlay.sample=null;overlay.clearRoi();overlay.invalidate();resultText.setText("0");statusText.setText("Temizlendi");});
    }

    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setAllCaps(false);b.setBackgroundColor(Color.rgb(19,58,68));return b;}

    private void startCamera(){ListenableFuture<ProcessCameraProvider> f=ProcessCameraProvider.getInstance(this);f.addListener(()->{try{ProcessCameraProvider p=f.get();Preview prev=new Preview.Builder().build();prev.setSurfaceProvider(previewView.getSurfaceProvider());ImageAnalysis a=new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build();a.setAnalyzer(cameraExecutor,img->{try{Bitmap b=rgba(img);int rot=img.getImageInfo().getRotationDegrees();Bitmap c=rot==0?b:rotate(b,rot);latestFrame=c.copy(Bitmap.Config.ARGB_8888,false);}catch(Throwable ignored){}finally{img.close();}});p.unbindAll();p.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,prev,a);}catch(Throwable e){Toast.makeText(this,"Kamera başlatılamadı",Toast.LENGTH_LONG).show();}},ContextCompat.getMainExecutor(this));}

    private Bitmap rgba(ImageProxy image){ImageProxy.PlaneProxy pl=image.getPlanes()[0];ByteBuffer buf=pl.getBuffer();buf.rewind();int ps=Math.max(1,pl.getPixelStride()),rs=pl.getRowStride(),pw=rs/ps;Bitmap pad=Bitmap.createBitmap(pw,image.getHeight(),Bitmap.Config.ARGB_8888);pad.copyPixelsFromBuffer(buf);return pw==image.getWidth()?pad:Bitmap.createBitmap(pad,0,0,image.getWidth(),image.getHeight());}
    private Bitmap rotate(Bitmap s,int d){if(d==0)return s;Matrix m=new Matrix();m.postRotate(d);return Bitmap.createBitmap(s,0,0,s.getWidth(),s.getHeight(),m,true);}

    private void analyze(Bitmap source){
        if(!busy.compareAndSet(false,true))return;statusText.setText("AI tarıyor...");
        final List<PointF> roi=overlay.roiSnapshot();final int viewW=overlay.getWidth(),viewH=overlay.getHeight();
        detectExecutor.execute(()->{try{
            List<RectF> raw=new ArrayList<>();detect(source,0,0,source.getWidth(),source.getHeight(),raw);
            int grid=6,cw=Math.max(1,source.getWidth()/grid),ch=Math.max(1,source.getHeight()/grid),px=Math.round(cw*.28f),py=Math.round(ch*.28f);
            for(int y=0;y<grid;y++)for(int x=0;x<grid;x++){int l=Math.max(0,x*cw-px),t=Math.max(0,y*ch-py),r=Math.min(source.getWidth(),(x+1)*cw+px),b=Math.min(source.getHeight(),(y+1)*ch+py);if(r-l>72&&b-t>72)detect(source,l,t,r,b,raw);}
            List<RectF> cleaned=clean(raw,source.getWidth(),source.getHeight(),roi,viewW,viewH);detections=cleaned;
            runOnUiThread(()->{overlay.boxes=new ArrayList<>(cleaned);overlay.invalidate();refreshCount();statusText.setText("Doğrulanan: "+cleaned.size()+" / aday: "+raw.size());});
        }catch(Throwable e){runOnUiThread(()->statusText.setText("Algılama hatası"));}finally{busy.set(false);}});
    }

    private void detect(Bitmap src,int l,int t,int r,int b,List<RectF> out)throws Exception{Bitmap crop=Bitmap.createBitmap(src,l,t,r-l,b-t);List<DetectedObject> os=Tasks.await(detector.process(InputImage.fromBitmap(crop,0)));for(DetectedObject o:os){android.graphics.Rect q=o.getBoundingBox();RectF g=new RectF(q.left+l,q.top+t,q.right+l,q.bottom+t);if(g.width()>=10&&g.height()>=10)out.add(g);}}

    private List<RectF> clean(List<RectF> raw,int W,int H,List<PointF> roi,int viewW,int viewH){
        float frame=W*(float)H;List<RectF> a=new ArrayList<>();
        for(RectF r:raw){float ar=area(r);if(ar<frame*.00018f||ar>frame*.58f)continue;if(frameVisibleRatio(r,W,H)<.94f)continue;if(!roi.isEmpty()&&!mostlyInsideRoi(r,W,H,roi,viewW,viewH))continue;a.add(new RectF(r));}

        // First collapse obvious duplicates from tiled scans.
        a.sort((x,y)->Float.compare(area(y),area(x)));List<RectF> dedup=new ArrayList<>();
        for(RectF c:a){boolean dup=false;for(RectF k:dedup){if(iou(c,k)>.30f||overSmall(c,k)>.66f){dup=true;break;}}if(!dup)dedup.add(c);}

        // Physical-object fusion: pieces that are aligned, touching/overlapping and together form one elongated or compact silhouette are merged.
        boolean changed=true;int guard=0;
        while(changed&&guard++<8){changed=false;outer:for(int i=0;i<dedup.size();i++)for(int j=i+1;j<dedup.size();j++){
            RectF x=dedup.get(i),y=dedup.get(j);if(shouldFuse(x,y)){
                RectF u=union(x,y);dedup.remove(j);dedup.remove(i);dedup.add(u);changed=true;break outer;
            }
        }}

        // Suppress interior patterns/components when an enclosing body exists.
        dedup.sort((x,y)->Float.compare(area(y),area(x)));List<RectF> outerBodies=new ArrayList<>();
        for(RectF c:dedup){boolean inner=false;for(RectF body:outerBodies){float ratio=area(c)/area(body);if(ratio<.72f&&containment(c,body)>.74f){inner=true;break;}}if(!inner)outerBodies.add(c);}

        // Reject giant group boxes that span multiple separated bodies.
        List<RectF> out=new ArrayList<>();
        for(RectF p:outerBodies){int children=0;for(RectF q:outerBodies){if(p==q)continue;float rr=area(q)/area(p);if(rr>.12f&&rr<.62f&&containment(q,p)>.70f)children++;}if(children<2)out.add(p);}

        out.sort((x,y)->{int yy=Float.compare(x.centerY(),y.centerY());return yy!=0?yy:Float.compare(x.centerX(),y.centerX());});return out;
    }

    private boolean shouldFuse(RectF a,RectF b){
        if(iou(a,b)>.18f||overSmall(a,b)>.44f)return true;
        float gapX=Math.max(0,Math.max(a.left,b.left)-Math.min(a.right,b.right));
        float gapY=Math.max(0,Math.max(a.top,b.top)-Math.min(a.bottom,b.bottom));
        float minW=Math.min(a.width(),b.width()),minH=Math.min(a.height(),b.height());
        float xOverlap=Math.max(0,Math.min(a.right,b.right)-Math.max(a.left,b.left))/Math.max(1,minW);
        float yOverlap=Math.max(0,Math.min(a.bottom,b.bottom)-Math.max(a.top,b.top))/Math.max(1,minH);
        boolean verticalChain=xOverlap>.42f&&gapY<Math.max(12f,minH*.28f);
        boolean horizontalChain=yOverlap>.42f&&gapX<Math.max(12f,minW*.28f);
        float sizeRatio=Math.min(area(a),area(b))/Math.max(area(a),area(b));
        return sizeRatio>.16f&&(verticalChain||horizontalChain);
    }

    private RectF union(RectF a,RectF b){return new RectF(Math.min(a.left,b.left),Math.min(a.top,b.top),Math.max(a.right,b.right),Math.max(a.bottom,b.bottom));}
    private float frameVisibleRatio(RectF r,int W,int H){return inter(r,new RectF(0,0,W,H))/area(r);}
    private boolean mostlyInsideRoi(RectF r,int W,int H,List<PointF> roi,int viewW,int viewH){if(viewW<=0||viewH<=0)return true;float[] xs={r.left,r.left+r.width()*.25f,r.centerX(),r.left+r.width()*.75f,r.right};float[] ys={r.top,r.top+r.height()*.25f,r.centerY(),r.top+r.height()*.75f,r.bottom};int inside=0,total=0;for(float x:xs)for(float y:ys){PointF p=imageToView(x,y,W,H,viewW,viewH);if(pointInPolygon(p.x,p.y,roi))inside++;total++;}PointF c=imageToView(r.centerX(),r.centerY(),W,H,viewW,viewH);return pointInPolygon(c.x,c.y,roi)&&inside>=18;}
    private PointF imageToView(float x,float y,int W,int H,int viewW,int viewH){float sc=Math.max(viewW/(float)W,viewH/(float)H);float dx=(viewW-W*sc)/2f,dy=(viewH-H*sc)/2f;return new PointF(x*sc+dx,y*sc+dy);}
    private boolean pointInPolygon(float x,float y,List<PointF> poly){boolean c=false;int n=poly.size();if(n<3)return true;for(int i=0,j=n-1;i<n;j=i++){PointF a=poly.get(i),b=poly.get(j);if(((a.y>y)!=(b.y>y))&&(x<(b.x-a.x)*(y-a.y)/(b.y-a.y+0.00001f)+a.x))c=!c;}return c;}
    private float area(RectF r){return Math.max(1f,r.width()*r.height());}
    private float inter(RectF a,RectF b){float l=Math.max(a.left,b.left),t=Math.max(a.top,b.top),rr=Math.min(a.right,b.right),bb=Math.min(a.bottom,b.bottom);return rr>l&&bb>t?(rr-l)*(bb-t):0f;}
    private float iou(RectF a,RectF b){float i=inter(a,b);return i/(area(a)+area(b)-i);}
    private float overSmall(RectF a,RectF b){return inter(a,b)/Math.min(area(a),area(b));}
    private float containment(RectF small,RectF big){return inter(small,big)/area(small);}

    private void refreshCount(){
        if(!sameMode||sampleRect==null){resultText.setText(String.valueOf(detections.size()));return;}
        int n=0;float sa=area(sampleRect),asp=aspect(sampleRect);for(RectF r:detections){float size=Math.min(area(r),sa)/Math.max(area(r),sa);float ad=Math.abs(aspect(r)-asp);if(size>.40f&&ad<.55f)n++;}
        resultText.setText(String.valueOf(n));statusText.setText("AYNI mod: "+n+" benzer");
    }
    private float aspect(RectF r){float x=r.width()/Math.max(1f,r.height());return Math.max(x,1f/Math.max(.001f,x));}
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}

    @Override public void onRequestPermissionsResult(int r,@NonNull String[] p,@NonNull int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_CAMERA&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)startCamera();}
    @Override protected void onDestroy(){super.onDestroy();detector.close();cameraExecutor.shutdownNow();detectExecutor.shutdownNow();}

    private class SmartOverlay extends View{
        Paint box=new Paint(1),circle=new Paint(1),text=new Paint(1),roiPaint=new Paint(1),samplePaint=new Paint(1);List<RectF> boxes=new ArrayList<>();RectF sample;boolean drawingMode=false;List<PointF> roiPoints=new ArrayList<>();Path roiPath=new Path();
        SmartOverlay(){super(MainActivityV5.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);box.setColor(Color.rgb(45,245,130));box.setStyle(Paint.Style.STROKE);box.setStrokeWidth(dp(3));circle.setColor(Color.rgb(0,165,88));text.setColor(Color.WHITE);text.setTextAlign(Paint.Align.CENTER);text.setTextSize(dp(16));text.setFakeBoldText(true);roiPaint.setColor(Color.CYAN);roiPaint.setStyle(Paint.Style.STROKE);roiPaint.setStrokeWidth(dp(4));samplePaint.setColor(Color.YELLOW);samplePaint.setStyle(Paint.Style.STROKE);samplePaint.setStrokeWidth(dp(4));
            setOnTouchListener((v,e)->{if(drawingMode){if(e.getAction()==MotionEvent.ACTION_DOWN){roiPoints.clear();roiPath.reset();roiPoints.add(new PointF(e.getX(),e.getY()));roiPath.moveTo(e.getX(),e.getY());invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){roiPoints.add(new PointF(e.getX(),e.getY()));roiPath.lineTo(e.getX(),e.getY());invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_UP){roiPoints.add(new PointF(e.getX(),e.getY()));roiPath.close();drawingMode=false;penButton.setBackgroundColor(Color.argb(160,45,45,45));statusText.setText("ROI hazır");invalidate();return true;}}else if(sameMode&&e.getAction()==MotionEvent.ACTION_UP){RectF best=null;float ba=Float.MAX_VALUE;for(RectF r:boxes){RectF m=map(r);if(m.contains(e.getX(),e.getY())&&area(m)<ba){best=r;ba=area(m);}}if(best!=null){sampleRect=new RectF(best);sample=new RectF(best);invalidate();refreshCount();}return true;}return true;});}
        List<PointF> roiSnapshot(){List<PointF> x=new ArrayList<>();for(PointF p:roiPoints)x.add(new PointF(p.x,p.y));return x;}
        void clearRoi(){roiPoints.clear();roiPath.reset();drawingMode=false;}
        @Override protected void onDraw(Canvas c){super.onDraw(c);if(!roiPoints.isEmpty())c.drawPath(roiPath,roiPaint);for(int i=0;i<boxes.size();i++){RectF r=map(boxes.get(i));c.drawRoundRect(r,dp(7),dp(7),box);float cx=r.centerX(),cy=Math.max(dp(82),r.top+dp(3));c.drawCircle(cx,cy,dp(16),circle);c.drawText(""+(i+1),cx,cy+dp(6),text);}if(sample!=null)c.drawRoundRect(map(sample),dp(9),dp(9),samplePaint);}
        RectF map(RectF s){Bitmap f=latestFrame;if(f==null)return new RectF(s);float vw=getWidth(),vh=getHeight(),sw=f.getWidth(),sh=f.getHeight(),sc=Math.max(vw/sw,vh/sh),dx=(vw-sw*sc)/2f,dy=(vh-sh*sc)/2f;return new RectF(s.left*sc+dx,s.top*sc+dy,s.right*sc+dx,s.bottom*sc+dy);}
    }
}
