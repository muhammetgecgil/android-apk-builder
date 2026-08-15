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

public class MainActivityV6 extends AppCompatActivity {
  static final int REQ=10;
  PreviewView preview; Overlay overlay; TextView result,status; Button mode,pen;
  volatile Bitmap latest; volatile List<RectF> boxes=new ArrayList<>();
  boolean same=false; RectF sample;
  ExecutorService camExec=Executors.newSingleThreadExecutor(), detExec=Executors.newSingleThreadExecutor();
  AtomicBoolean busy=new AtomicBoolean(false);
  ObjectDetector detector=ObjectDetection.getClient(new ObjectDetectorOptions.Builder().setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE).enableMultipleObjects().build());

  @Override protected void onCreate(Bundle b){super.onCreate(b);ui(); if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)camera(); else ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},REQ);}

  void ui(){
    FrameLayout root=new FrameLayout(this); root.setBackgroundColor(Color.BLACK);
    preview=new PreviewView(this); preview.setScaleType(PreviewView.ScaleType.FILL_CENTER); root.addView(preview,new FrameLayout.LayoutParams(-1,-1));
    overlay=new Overlay(); root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
    LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(14),0,dp(14),0); top.setBackgroundColor(Color.argb(205,10,18,22));
    TextView title=new TextView(this); title.setText("Nesne Sayar AI"); title.setTextColor(Color.WHITE); title.setTextSize(22); top.addView(title,new LinearLayout.LayoutParams(0,dp(58),1));
    status=new TextView(this); status.setText("Hazır"); status.setTextColor(Color.LTGRAY); status.setTextSize(12); status.setGravity(Gravity.CENTER); top.addView(status,new LinearLayout.LayoutParams(dp(165),dp(58)));
    result=new TextView(this); result.setText("0"); result.setTextColor(Color.rgb(45,245,130)); result.setTextSize(34); result.setGravity(Gravity.END|Gravity.CENTER_VERTICAL); top.addView(result,new LinearLayout.LayoutParams(dp(58),dp(58)));
    FrameLayout.LayoutParams tlp=new FrameLayout.LayoutParams(-1,dp(58),Gravity.TOP); root.addView(top,tlp);
    pen=btn("✎"); pen.setTextSize(24); FrameLayout.LayoutParams plp=new FrameLayout.LayoutParams(dp(58),dp(58),Gravity.TOP|Gravity.START); plp.leftMargin=dp(8); plp.topMargin=dp(68); root.addView(pen,plp);
    LinearLayout bot=new LinearLayout(this); Button src=btn("CANLI"); mode=btn("FARKLI"); Button count=btn("SAY"),clear=btn("SİL"); count.setBackgroundColor(Color.rgb(0,145,78)); bot.addView(src,new LinearLayout.LayoutParams(0,dp(60),1));bot.addView(mode,new LinearLayout.LayoutParams(0,dp(60),1));bot.addView(count,new LinearLayout.LayoutParams(0,dp(60),1));bot.addView(clear,new LinearLayout.LayoutParams(0,dp(60),1)); FrameLayout.LayoutParams blp=new FrameLayout.LayoutParams(-1,dp(60),Gravity.BOTTOM); root.addView(bot,blp);
    ViewCompat.setOnApplyWindowInsetsListener(root,(v,i)->{Insets x=i.getInsets(WindowInsetsCompat.Type.systemBars()); tlp.topMargin=x.top; plp.topMargin=x.top+dp(66); blp.bottomMargin=x.bottom+dp(10); top.setLayoutParams(tlp);pen.setLayoutParams(plp);bot.setLayoutParams(blp);return i;});
    setContentView(root);
    pen.setOnClickListener(v->{overlay.drawMode=!overlay.drawMode; pen.setBackgroundColor(overlay.drawMode?Color.rgb(0,145,78):Color.rgb(19,58,68)); status.setText(overlay.drawMode?"Bölgeyi çiz":"ROI hazır");});
    mode.setOnClickListener(v->{same=!same;mode.setText(same?"AYNI":"FARKLI");sample=null;overlay.sample=null;overlay.invalidate();refresh();});
    count.setOnClickListener(v->{Bitmap f=latest;if(f==null){Toast.makeText(this,"Kamera hazırlanıyor",Toast.LENGTH_SHORT).show();return;} analyze(f.copy(Bitmap.Config.ARGB_8888,false));});
    clear.setOnClickListener(v->{boxes=new ArrayList<>();sample=null;overlay.boxes=Collections.emptyList();overlay.sample=null;overlay.roi.clear();overlay.invalidate();result.setText("0");status.setText("Temizlendi");});
  }
  Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setAllCaps(false);b.setBackgroundColor(Color.rgb(19,58,68));return b;}

  void camera(){ListenableFuture<ProcessCameraProvider> f=ProcessCameraProvider.getInstance(this);f.addListener(()->{try{ProcessCameraProvider p=f.get();Preview pr=new Preview.Builder().build();pr.setSurfaceProvider(preview.getSurfaceProvider());ImageAnalysis a=new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build();a.setAnalyzer(camExec,img->{try{Bitmap b=rgba(img),c=rotate(b,img.getImageInfo().getRotationDegrees());latest=c.copy(Bitmap.Config.ARGB_8888,false);}catch(Throwable ignored){}finally{img.close();}});p.unbindAll();p.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,pr,a);}catch(Throwable e){Toast.makeText(this,"Kamera başlatılamadı",Toast.LENGTH_LONG).show();}},ContextCompat.getMainExecutor(this));}
  Bitmap rgba(ImageProxy im){ImageProxy.PlaneProxy p=im.getPlanes()[0];ByteBuffer bf=p.getBuffer();bf.rewind();int ps=Math.max(1,p.getPixelStride()),pw=p.getRowStride()/ps;Bitmap pad=Bitmap.createBitmap(pw,im.getHeight(),Bitmap.Config.ARGB_8888);pad.copyPixelsFromBuffer(bf);return pw==im.getWidth()?pad:Bitmap.createBitmap(pad,0,0,im.getWidth(),im.getHeight());}
  Bitmap rotate(Bitmap s,int d){if(d==0)return s;Matrix m=new Matrix();m.postRotate(d);return Bitmap.createBitmap(s,0,0,s.getWidth(),s.getHeight(),m,true);}

  void analyze(Bitmap src){if(!busy.compareAndSet(false,true))return;status.setText("AI tarıyor...");List<PointF> roi=overlay.copyRoi();int vw=overlay.getWidth(),vh=overlay.getHeight();detExec.execute(()->{try{List<RectF> raw=new ArrayList<>();detect(src,0,0,src.getWidth(),src.getHeight(),raw);int g=6,cw=src.getWidth()/g,ch=src.getHeight()/g,px=(int)(cw*.25f),py=(int)(ch*.25f);for(int y=0;y<g;y++)for(int x=0;x<g;x++){int l=Math.max(0,x*cw-px),t=Math.max(0,y*ch-py),r=Math.min(src.getWidth(),(x+1)*cw+px),b=Math.min(src.getHeight(),(y+1)*ch+py);if(r-l>72&&b-t>72)detect(src,l,t,r,b,raw);}List<RectF> out=clean(raw,src.getWidth(),src.getHeight(),roi,vw,vh);boxes=out;runOnUiThread(()->{overlay.boxes=new ArrayList<>(out);overlay.invalidate();refresh();status.setText("Doğrulanan: "+out.size()+" / aday: "+raw.size());});}catch(Throwable e){runOnUiThread(()->status.setText("Algılama hatası"));}finally{busy.set(false);}});}
  void detect(Bitmap src,int l,int t,int r,int b,List<RectF> out)throws Exception{Bitmap c=Bitmap.createBitmap(src,l,t,r-l,b-t);for(DetectedObject o:Tasks.await(detector.process(InputImage.fromBitmap(c,0)))){android.graphics.Rect q=o.getBoundingBox();RectF z=new RectF(q.left+l,q.top+t,q.right+l,q.bottom+t);if(z.width()>10&&z.height()>10)out.add(z);}}

  List<RectF> clean(List<RectF> raw,int W,int H,List<PointF> roi,int vw,int vh){
    float frame=W*(float)H;List<RectF> cand=new ArrayList<>();
    for(RectF r:raw){float a=area(r);if(a<frame*.00016f||a>frame*.52f)continue;if(visible(r,W,H)<.94f)continue;if(!roi.isEmpty()&&!insideRoi(r,W,H,roi,vw,vh))continue;cand.add(new RectF(r));}
    // 1. Consensus clustering: tiled detections of the SAME object collapse only when overlap is strong.
    cand.sort((a,b)->Float.compare(area(b),area(a)));List<RectF> clusters=new ArrayList<>();
    for(RectF c:cand){int hit=-1;float best=0;for(int i=0;i<clusters.size();i++){RectF k=clusters.get(i);float s=Math.max(iou(c,k),overSmall(c,k)*.72f);float sr=Math.min(area(c),area(k))/Math.max(area(c),area(k));if(sr>.28f&&s>best&&(iou(c,k)>.34f||overSmall(c,k)>.78f)){best=s;hit=i;}}if(hit<0)clusters.add(c);else clusters.set(hit,weightedUnion(clusters.get(hit),c));}
    // 2. Remove giant group boxes when they contain two or more spatially separate credible objects.
    List<RectF> noGroups=new ArrayList<>();
    for(RectF p:clusters){List<RectF> kids=new ArrayList<>();for(RectF q:clusters){if(p==q)continue;float rr=area(q)/area(p);if(rr>.045f&&rr<.55f&&contain(q,p)>.86f)kids.add(q);}boolean group=false;for(int i=0;i<kids.size()&&!group;i++)for(int j=i+1;j<kids.size();j++){RectF a=kids.get(i),b=kids.get(j);if(iou(a,b)<.08f&&centerSep(a,b,p)>.24f){group=true;break;}}if(!group)noGroups.add(p);}
    // 3. Interior-pattern suppression ONLY when the outer box is a close geometric parent, not a scene group.
    noGroups.sort((a,b)->Float.compare(area(b),area(a)));List<RectF> keep=new ArrayList<>();
    for(RectF c:noGroups){boolean inner=false;for(RectF p:keep){float rr=area(c)/area(p);if(rr<.42f&&contain(c,p)>.93f&&centerNorm(c,p)<.30f){inner=true;break;}}if(!inner)keep.add(c);}
    // 4. Final duplicate pass, conservative so separate nearby objects survive.
    List<RectF> out=new ArrayList<>();for(RectF c:keep){boolean dup=false;for(RectF k:out){float sr=Math.min(area(c),area(k))/Math.max(area(c),area(k));if(sr>.36f&&(iou(c,k)>.48f||overSmall(c,k)>.88f)){dup=true;break;}}if(!dup)out.add(c);}out.sort((a,b)->{int y=Float.compare(a.centerY(),b.centerY());return y!=0?y:Float.compare(a.centerX(),b.centerX());});return out;
  }
  RectF weightedUnion(RectF a,RectF b){return new RectF(Math.min(a.left,b.left),Math.min(a.top,b.top),Math.max(a.right,b.right),Math.max(a.bottom,b.bottom));}
  float centerSep(RectF a,RectF b,RectF p){float dx=a.centerX()-b.centerX(),dy=a.centerY()-b.centerY();return (float)Math.sqrt(dx*dx+dy*dy)/(float)Math.sqrt(area(p));}
  float centerNorm(RectF a,RectF b){float dx=a.centerX()-b.centerX(),dy=a.centerY()-b.centerY();return (float)Math.sqrt(dx*dx+dy*dy)/(float)Math.sqrt(Math.max(area(a),area(b)));}
  float visible(RectF r,int W,int H){return inter(r,new RectF(0,0,W,H))/area(r);} float area(RectF r){return Math.max(1,r.width()*r.height());} float inter(RectF a,RectF b){float l=Math.max(a.left,b.left),t=Math.max(a.top,b.top),r=Math.min(a.right,b.right),d=Math.min(a.bottom,b.bottom);return r>l&&d>t?(r-l)*(d-t):0;} float iou(RectF a,RectF b){float i=inter(a,b);return i/(area(a)+area(b)-i);} float overSmall(RectF a,RectF b){return inter(a,b)/Math.min(area(a),area(b));} float contain(RectF a,RectF b){return inter(a,b)/area(a);}
  boolean insideRoi(RectF r,int W,int H,List<PointF> poly,int vw,int vh){int in=0;float[] xs={r.left,r.left+r.width()*.25f,r.centerX(),r.left+r.width()*.75f,r.right},ys={r.top,r.top+r.height()*.25f,r.centerY(),r.top+r.height()*.75f,r.bottom};for(float x:xs)for(float y:ys){PointF p=toView(x,y,W,H,vw,vh);if(pip(p.x,p.y,poly))in++;}PointF c=toView(r.centerX(),r.centerY(),W,H,vw,vh);return pip(c.x,c.y,poly)&&in>=16;}
  PointF toView(float x,float y,int W,int H,int vw,int vh){float s=Math.max(vw/(float)W,vh/(float)H),dx=(vw-W*s)/2f,dy=(vh-H*s)/2f;return new PointF(x*s+dx,y*s+dy);} boolean pip(float x,float y,List<PointF> p){if(p.size()<3)return true;boolean c=false;for(int i=0,j=p.size()-1;i<p.size();j=i++){PointF a=p.get(i),b=p.get(j);if(((a.y>y)!=(b.y>y))&&(x<(b.x-a.x)*(y-a.y)/(b.y-a.y+.00001f)+a.x))c=!c;}return c;}

  void refresh(){if(!same||sample==null){result.setText(String.valueOf(boxes.size()));return;}int n=0;for(RectF r:boxes){float ar=Math.min(area(r),area(sample))/Math.max(area(r),area(sample));float asp=Math.min(r.width()/r.height(),sample.width()/sample.height())/Math.max(r.width()/r.height(),sample.width()/sample.height());if(ar>.38f&&asp>.55f)n++;}result.setText(String.valueOf(n));}
  int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
  @Override public void onRequestPermissionsResult(int r,@NonNull String[] p,@NonNull int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)camera();}

  class Overlay extends View{
    Paint boxP=new Paint(1),numP=new Paint(1),roiP=new Paint(1),sampleP=new Paint(1); List<RectF> boxes=Collections.emptyList(); List<PointF> roi=new ArrayList<>();boolean drawMode=false;RectF sample;
    Overlay(){super(MainActivityV6.this);boxP.setStyle(Paint.Style.STROKE);boxP.setStrokeWidth(dp(3));boxP.setColor(Color.rgb(35,245,135));roiP.setStyle(Paint.Style.STROKE);roiP.setStrokeWidth(dp(4));roiP.setColor(Color.CYAN);sampleP.setStyle(Paint.Style.STROKE);sampleP.setStrokeWidth(dp(4));sampleP.setColor(Color.YELLOW);numP.setColor(Color.rgb(0,180,105));}
    List<PointF> copyRoi(){List<PointF>x=new ArrayList<>();for(PointF p:roi)x.add(new PointF(p.x,p.y));return x;}
    @Override protected void onDraw(Canvas c){super.onDraw(c);if(roi.size()>1){Path p=new Path();p.moveTo(roi.get(0).x,roi.get(0).y);for(int i=1;i<roi.size();i++)p.lineTo(roi.get(i).x,roi.get(i).y);if(!drawMode)p.close();c.drawPath(p,roiP);}for(int i=0;i<boxes.size();i++){RectF r=toScreen(boxes.get(i));c.drawRect(r,boxP);float rad=dp(22);c.drawCircle(r.centerX(),Math.max(rad,r.top),rad,numP);Paint t=new Paint(1);t.setColor(Color.WHITE);t.setTextAlign(Paint.Align.CENTER);t.setTextSize(dp(18));c.drawText(String.valueOf(i+1),r.centerX(),Math.max(rad,r.top)+dp(6),t);}if(sample!=null)c.drawRect(toScreen(sample),sampleP);}
    RectF toScreen(RectF r){Bitmap f=latest;if(f==null)return new RectF(r);float s=Math.max(getWidth()/(float)f.getWidth(),getHeight()/(float)f.getHeight()),dx=(getWidth()-f.getWidth()*s)/2f,dy=(getHeight()-f.getHeight()*s)/2f;return new RectF(r.left*s+dx,r.top*s+dy,r.right*s+dx,r.bottom*s+dy);}
    @Override public boolean onTouchEvent(MotionEvent e){if(drawMode){if(e.getAction()==MotionEvent.ACTION_DOWN){roi.clear();roi.add(new PointF(e.getX(),e.getY()));invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){roi.add(new PointF(e.getX(),e.getY()));invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_UP){roi.add(new PointF(e.getX(),e.getY()));drawMode=false;pen.setBackgroundColor(Color.rgb(19,58,68));status.setText("ROI hazır");invalidate();return true;}}else if(same&&e.getAction()==MotionEvent.ACTION_UP){for(RectF r:boxes){RectF s=toScreen(r);if(s.contains(e.getX(),e.getY())){MainActivityV6.this.sample=new RectF(r);sample=new RectF(r);refresh();invalidate();return true;}}}return true;}
  }
}
