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
import android.view.ViewGroup;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivityV4 extends AppCompatActivity {
    private static final int REQ_CAMERA = 10;
    private PreviewView previewView;
    private SmartOverlay overlay;
    private TextView resultText, statusText;
    private Button modeButton, penButton;
    private volatile Bitmap latestFrame;
    private volatile List<RectF> detections = new ArrayList<>();
    private boolean sameMode = false;
    private RectF sampleRect;
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService detectExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy = new AtomicBoolean(false);

    private final ObjectDetector detector = ObjectDetection.getClient(
            new ObjectDetectorOptions.Builder()
                    .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                    .enableMultipleObjects()
                    .build());

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera();
        else ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        previewView = new PreviewView(this);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        root.addView(previewView, new FrameLayout.LayoutParams(-1, -1));

        overlay = new SmartOverlay();
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(14),0,dp(14),0);
        top.setBackgroundColor(Color.argb(205,10,18,22));
        TextView title = new TextView(this);
        title.setText("Nesne Sayar AI"); title.setTextColor(Color.WHITE); title.setTextSize(22);
        top.addView(title,new LinearLayout.LayoutParams(0,dp(58),1f));
        statusText = new TextView(this);
        statusText.setText("Hazır"); statusText.setTextColor(Color.LTGRAY); statusText.setTextSize(12); statusText.setGravity(Gravity.CENTER);
        top.addView(statusText,new LinearLayout.LayoutParams(dp(155),dp(58)));
        resultText = new TextView(this);
        resultText.setText("0"); resultText.setTextColor(Color.rgb(45,245,130)); resultText.setTextSize(34); resultText.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        top.addView(resultText,new LinearLayout.LayoutParams(dp(58),dp(58)));
        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(-1,dp(58),Gravity.TOP);
        root.addView(top,topLp);

        penButton = new Button(this);
        penButton.setText("✎"); penButton.setTextSize(24); penButton.setTextColor(Color.WHITE); penButton.setBackgroundColor(Color.argb(160,45,45,45));
        FrameLayout.LayoutParams penLp = new FrameLayout.LayoutParams(dp(58),dp(58),Gravity.TOP|Gravity.START);
        penLp.leftMargin=dp(8); penLp.topMargin=dp(68); root.addView(penButton,penLp);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setBackgroundColor(Color.rgb(15,48,58));
        Button src = button("CANLI"); modeButton=button("FARKLI"); Button count=button("SAY"); Button clear=button("SİL");
        count.setBackgroundColor(Color.rgb(0,145,78));
        bottom.addView(src,new LinearLayout.LayoutParams(0,dp(60),1f));
        bottom.addView(modeButton,new LinearLayout.LayoutParams(0,dp(60),1f));
        bottom.addView(count,new LinearLayout.LayoutParams(0,dp(60),1f));
        bottom.addView(clear,new LinearLayout.LayoutParams(0,dp(60),1f));
        FrameLayout.LayoutParams bottomLp = new FrameLayout.LayoutParams(-1,dp(60),Gravity.BOTTOM);
        root.addView(bottom,bottomLp);

        ViewCompat.setOnApplyWindowInsetsListener(root,(v,i)->{
            Insets bars=i.getInsets(WindowInsetsCompat.Type.systemBars());
            topLp.topMargin=bars.top;
            penLp.topMargin=bars.top+dp(66);
            bottomLp.bottomMargin=bars.bottom+dp(10);
            top.setLayoutParams(topLp); penButton.setLayoutParams(penLp); bottom.setLayoutParams(bottomLp);
            return i;
        });
        setContentView(root);

        penButton.setOnClickListener(v->{ overlay.drawingMode=!overlay.drawingMode; penButton.setBackgroundColor(overlay.drawingMode?Color.rgb(0,145,78):Color.argb(160,45,45,45)); statusText.setText(overlay.drawingMode?"Bölgeyi çiz":"ROI hazır"); });
        src.setOnClickListener(v->Toast.makeText(this,"Canlı kamera aktif",Toast.LENGTH_SHORT).show());
        modeButton.setOnClickListener(v->{ sameMode=!sameMode; modeButton.setText(sameMode?"AYNI":"FARKLI"); sampleRect=null; overlay.sample=null; overlay.invalidate(); refreshCount(); });
        count.setOnClickListener(v->{ Bitmap f=latestFrame; if(f==null){Toast.makeText(this,"Kamera hazırlanıyor",Toast.LENGTH_SHORT).show();return;} Bitmap snap=f.copy(Bitmap.Config.ARGB_8888,false); analyze(snap); });
        clear.setOnClickListener(v->{ detections=new ArrayList<>(); sampleRect=null; overlay.boxes=Collections.emptyList(); overlay.sample=null; overlay.clearRoi(); overlay.invalidate(); resultText.setText("0"); statusText.setText("Temizlendi"); });
    }

    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setAllCaps(false);b.setBackgroundColor(Color.rgb(19,58,68));return b;}

    private void startCamera(){
        ListenableFuture<ProcessCameraProvider> f=ProcessCameraProvider.getInstance(this);
        f.addListener(()->{try{
            ProcessCameraProvider p=f.get(); Preview prev=new Preview.Builder().build(); prev.setSurfaceProvider(previewView.getSurfaceProvider());
            ImageAnalysis a=new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build();
            a.setAnalyzer(cameraExecutor,img->{try{Bitmap b=rgba(img);int rot=img.getImageInfo().getRotationDegrees();Bitmap c=rot==0?b:rotate(b,rot);latestFrame=c.copy(Bitmap.Config.ARGB_8888,false);}catch(Throwable ignored){}finally{img.close();}});
            p.unbindAll();p.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,prev,a);
        }catch(Throwable e){Toast.makeText(this,"Kamera başlatılamadı",Toast.LENGTH_LONG).show();}},ContextCompat.getMainExecutor(this));
    }

    private Bitmap rgba(ImageProxy image){ImageProxy.PlaneProxy pl=image.getPlanes()[0];ByteBuffer buf=pl.getBuffer();buf.rewind();int ps=Math.max(1,pl.getPixelStride()),rs=pl.getRowStride(),pw=rs/ps;Bitmap pad=Bitmap.createBitmap(pw,image.getHeight(),Bitmap.Config.ARGB_8888);pad.copyPixelsFromBuffer(buf);return pw==image.getWidth()?pad:Bitmap.createBitmap(pad,0,0,image.getWidth(),image.getHeight());}
    private Bitmap rotate(Bitmap s,int d){if(d==0)return s;Matrix m=new Matrix();m.postRotate(d);return Bitmap.createBitmap(s,0,0,s.getWidth(),s.getHeight(),m,true);}

    private void analyze(Bitmap source){
        if(!busy.compareAndSet(false,true))return;
        statusText.setText("AI tarıyor...");
        final List<PointF> roi = overlay.roiSnapshot();
        final int viewW=overlay.getWidth(), viewH=overlay.getHeight();
        detectExecutor.execute(()->{try{
            List<RectF> raw=new ArrayList<>();
            detect(source,0,0,source.getWidth(),source.getHeight(),raw);
            int grid=5,cw=Math.max(1,source.getWidth()/grid),ch=Math.max(1,source.getHeight()/grid),px=Math.round(cw*.24f),py=Math.round(ch*.24f);
            for(int y=0;y<grid;y++)for(int x=0;x<grid;x++){int l=Math.max(0,x*cw-px),t=Math.max(0,y*ch-py),r=Math.min(source.getWidth(),(x+1)*cw+px),b=Math.min(source.getHeight(),(y+1)*ch+py);if(r-l>80&&b-t>80)detect(source,l,t,r,b,raw);}
            List<RectF> cleaned=clean(raw,source.getWidth(),source.getHeight(),roi,viewW,viewH);
            detections=cleaned;
            runOnUiThread(()->{overlay.boxes=new ArrayList<>(cleaned);overlay.invalidate();refreshCount();statusText.setText("Doğrulanan: "+cleaned.size()+" / aday: "+raw.size());});
        }catch(Throwable e){runOnUiThread(()->statusText.setText("Algılama hatası"));}finally{busy.set(false);}});
    }

    private void detect(Bitmap src,int l,int t,int r,int b,List<RectF> out)throws Exception{
        Bitmap crop=Bitmap.createBitmap(src,l,t,r-l,b-t);
        List<DetectedObject> os=Tasks.await(detector.process(InputImage.fromBitmap(crop,0)));
        for(DetectedObject o:os){android.graphics.Rect q=o.getBoundingBox();RectF g=new RectF(q.left+l,q.top+t,q.right+l,q.bottom+t);if(g.width()>=12&&g.height()>=12)out.add(g);}
    }

    private List<RectF> clean(List<RectF> raw,int W,int H,List<PointF> roi,int viewW,int viewH){
        List<RectF> a=new ArrayList<>(); float frame=W*(float)H;
        for(RectF r:raw){
            float ar=area(r);
            if(ar<frame*.00025f||ar>frame*.72f)continue;
            if(frameVisibleRatio(r,W,H)<.92f)continue;
            if(!roi.isEmpty()&&!mostlyInsideRoi(r,W,H,roi,viewW,viewH))continue;
            a.add(new RectF(r));
        }

        // 1) Remove big group boxes only when they contain at least two substantial, separate child objects.
        List<RectF> noGroup=new ArrayList<>();
        for(RectF parent:a){
            List<RectF> substantial=new ArrayList<>();
            for(RectF child:a){
                if(parent==child)continue;
                float ratio=area(child)/area(parent);
                if(ratio>.16f&&ratio<.68f&&containment(child,parent)>.82f) substantial.add(child);
            }
            boolean group=false;
            for(int i=0;i<substantial.size()&&!group;i++)for(int j=i+1;j<substantial.size();j++){
                if(iou(substantial.get(i),substantial.get(j))<.16f){group=true;break;}
            }
            if(!group)noGroup.add(parent);
        }

        // 2) Prefer a clear outer-object box; suppress inner textures, buttons, teeth, tips and screen regions.
        noGroup.sort((x,y)->Float.compare(area(y),area(x)));
        List<RectF> keep=new ArrayList<>();
        for(RectF c:noGroup){
            boolean internal=false;
            for(RectF outer:keep){
                float ratio=area(c)/area(outer);
                if(ratio<.56f&&containment(c,outer)>.78f){internal=true;break;}
            }
            if(internal)continue;
            boolean dup=false;
            for(RectF k:keep){
                float os=overSmall(c,k),ov=iou(c,k),cd=centerDistance(c,k);
                if(ov>.24f||os>.60f||(os>.36f&&cd<.22f)){dup=true;break;}
            }
            if(!dup)keep.add(c);
        }

        // 3) Final duplicate pass: tiled detections of one silhouette collapse to one.
        List<RectF> out=new ArrayList<>();
        for(RectF c:keep){
            boolean dup=false;
            for(RectF k:out){
                float ar=Math.min(area(c),area(k))/Math.max(area(c),area(k));
                if((containment(c,k)>.72f||containment(k,c)>.72f)&&ar>.34f){dup=true;break;}
            }
            if(!dup)out.add(c);
        }
        out.sort((x,y)->{int yy=Float.compare(x.centerY(),y.centerY());return yy!=0?yy:Float.compare(x.centerX(),y.centerX());});
        return out;
    }

    private float frameVisibleRatio(RectF r,int W,int H){RectF f=new RectF(0,0,W,H);return inter(r,f)/area(r);}

    private boolean mostlyInsideRoi(RectF r,int W,int H,List<PointF> roi,int viewW,int viewH){
        if(viewW<=0||viewH<=0)return true;
        float[] xs={r.left,r.centerX(),r.right}; float[] ys={r.top,r.centerY(),r.bottom}; int inside=0,total=0;
        for(float x:xs)for(float y:ys){PointF p=imageToView(x,y,W,H,viewW,viewH);if(pointInPolygon(p.x,p.y,roi))inside++;total++;}
        PointF c=imageToView(r.centerX(),r.centerY(),W,H,viewW,viewH);
        return pointInPolygon(c.x,c.y,roi)&&inside>=6; // center + at least 2/3 of box samples must be inside ROI
    }

    private PointF imageToView(float x,float y,int W,int H,int viewW,int viewH){float sc=Math.max(viewW/(float)W,viewH/(float)H);float dx=(viewW-W*sc)/2f,dy=(viewH-H*sc)/2f;return new PointF(x*sc+dx,y*sc+dy);}

    private boolean pointInPolygon(float x,float y,List<PointF> poly){boolean c=false;int n=poly.size();if(n<3)return true;for(int i=0,j=n-1;i<n;j=i++){PointF a=poly.get(i),b=poly.get(j);if(((a.y>y)!=(b.y>y))&&(x<(b.x-a.x)*(y-a.y)/(b.y-a.y+0.00001f)+a.x))c=!c;}return c;}

    private float area(RectF r){return Math.max(1f,r.width()*r.height());}
    private float inter(RectF a,RectF b){float l=Math.max(a.left,b.left),t=Math.max(a.top,b.top),rr=Math.min(a.right,b.right),bb=Math.min(a.bottom,b.bottom);return rr>l&&bb>t?(rr-l)*(bb-t):0f;}
    private float iou(RectF a,RectF b){float i=inter(a,b);return i/(area(a)+area(b)-i);}
    private float overSmall(RectF a,RectF b){return inter(a,b)/Math.min(area(a),area(b));}
    private float containment(RectF small,RectF big){return inter(small,big)/area(small);}
    private float centerDistance(RectF a,RectF b){float dx=a.centerX()-b.centerX(),dy=a.centerY()-b.centerY();float sc=(float)Math.sqrt(Math.max(area(a),area(b)));return (float)Math.sqrt(dx*dx+dy*dy)/Math.max(1f,sc);}

    private void refreshCount(){if(!sameMode||sampleRect==null){resultText.setText(String.valueOf(detections.size()));return;}int n=0;float sa=area(sampleRect),asp=aspect(sampleRect);for(RectF r:detections){float size=Math.min(area(r),sa)/Math.max(area(r),sa);float ad=Math.abs(aspect(r)-asp);if(size>.42f&&ad<.55f)n++;}resultText.setText(String.valueOf(n));statusText.setText("AYNI mod: "+n+" benzer");}
    private float aspect(RectF r){float x=r.width()/Math.max(1f,r.height());return Math.max(x,1f/Math.max(.001f,x));}
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}

    @Override public void onRequestPermissionsResult(int r,@NonNull String[] p,@NonNull int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_CAMERA&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)startCamera();}
    @Override protected void onDestroy(){super.onDestroy();detector.close();cameraExecutor.shutdownNow();detectExecutor.shutdownNow();}

    private class SmartOverlay extends View {
        Paint box=new Paint(1),circle=new Paint(1),text=new Paint(1),sampleP=new Paint(1),roiP=new Paint(1); List<RectF> boxes=new ArrayList<>(); RectF sample; boolean drawingMode=false; private final List<PointF> roiPoints=new ArrayList<>();
        SmartOverlay(){super(MainActivityV4.this);box.setColor(Color.rgb(45,245,130));box.setStyle(Paint.Style.STROKE);box.setStrokeWidth(dp(3));circle.setColor(Color.rgb(0,165,88));text.setColor(Color.WHITE);text.setTextAlign(Paint.Align.CENTER);text.setTextSize(dp(16));text.setFakeBoldText(true);sampleP.setColor(Color.YELLOW);sampleP.setStyle(Paint.Style.STROKE);sampleP.setStrokeWidth(dp(4));roiP.setColor(Color.CYAN);roiP.setStyle(Paint.Style.STROKE);roiP.setStrokeWidth(dp(4));roiP.setStrokeJoin(Paint.Join.ROUND);roiP.setStrokeCap(Paint.Cap.ROUND);
            setOnTouchListener((v,e)->{
                if(drawingMode){
                    if(e.getAction()==MotionEvent.ACTION_DOWN){synchronized(roiPoints){roiPoints.clear();roiPoints.add(new PointF(e.getX(),e.getY()));}invalidate();return true;}
                    if(e.getAction()==MotionEvent.ACTION_MOVE){synchronized(roiPoints){roiPoints.add(new PointF(e.getX(),e.getY()));}invalidate();return true;}
                    if(e.getAction()==MotionEvent.ACTION_UP){synchronized(roiPoints){roiPoints.add(new PointF(e.getX(),e.getY()));}drawingMode=false;penButton.setBackgroundColor(Color.argb(160,45,45,45));statusText.setText("ROI hazır – SAY");invalidate();return true;}
                }
                if(sameMode&&e.getAction()==MotionEvent.ACTION_UP){RectF best=null;float ba=Float.MAX_VALUE;for(RectF r:boxes){RectF m=map(r);if(m.contains(e.getX(),e.getY())&&area(m)<ba){best=r;ba=area(m);}}if(best!=null){sampleRect=new RectF(best);sample=new RectF(best);invalidate();refreshCount();}return true;}
                return true;
            });
        }
        List<PointF> roiSnapshot(){synchronized(roiPoints){return new ArrayList<>(roiPoints);}}
        void clearRoi(){synchronized(roiPoints){roiPoints.clear();}}
        @Override protected void onDraw(Canvas c){super.onDraw(c);List<PointF> rp=roiSnapshot();if(rp.size()>1){Path p=new Path();p.moveTo(rp.get(0).x,rp.get(0).y);for(int i=1;i<rp.size();i++)p.lineTo(rp.get(i).x,rp.get(i).y);if(!drawingMode&&rp.size()>2)p.close();c.drawPath(p,roiP);}for(int i=0;i<boxes.size();i++){RectF r=map(boxes.get(i));c.drawRoundRect(r,dp(7),dp(7),box);float cx=r.centerX(),cy=Math.max(dp(82),r.top+dp(3));c.drawCircle(cx,cy,dp(16),circle);c.drawText(""+(i+1),cx,cy+dp(6),text);}if(sample!=null)c.drawRoundRect(map(sample),dp(9),dp(9),sampleP);}
        RectF map(RectF s){Bitmap f=latestFrame;if(f==null)return new RectF(s);float vw=getWidth(),vh=getHeight(),sw=f.getWidth(),sh=f.getHeight();float sc=Math.max(vw/sw,vh/sh),dx=(vw-sw*sc)/2f,dy=(vh-sh*sc)/2f;return new RectF(s.left*sc+dx,s.top*sc+dy,s.right*sc+dx,s.bottom*sc+dy);}
    }
}
