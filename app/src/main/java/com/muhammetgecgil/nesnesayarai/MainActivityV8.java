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
 PreviewView preview; Overlay overlay; TextView result,status; Button mode,pen;
 volatile Bitmap latest; volatile List<Obj> objects=new ArrayList<>(); boolean same=false;
 final ExecutorService cam=Executors.newSingleThreadExecutor(),work=Executors.newSingleThreadExecutor();
 final AtomicBoolean busy=new AtomicBoolean(false);
 final ObjectDetector detector=ObjectDetection.getClient(new ObjectDetectorOptions.Builder().setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE).enableMultipleObjects().build());

 @Override protected void onCreate(Bundle b){super.onCreate(b);ui();if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)camera();else ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},REQ);}

 void ui(){
  FrameLayout root=new FrameLayout(this);preview=new PreviewView(this);preview.setScaleType(PreviewView.ScaleType.FILL_CENTER);root.addView(preview,new FrameLayout.LayoutParams(-1,-1));overlay=new Overlay();root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
  LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(14),0,dp(14),0);top.setBackgroundColor(Color.argb(215,10,18,22));
  TextView title=new TextView(this);title.setText("Nesne Sayar AI");title.setTextColor(Color.WHITE);title.setTextSize(22);top.addView(title,new LinearLayout.LayoutParams(0,dp(58),1));
  status=new TextView(this);status.setText("V112 Kesin ROI");status.setTextColor(Color.LTGRAY);status.setTextSize(12);status.setGravity(Gravity.CENTER);top.addView(status,new LinearLayout.LayoutParams(dp(190),dp(58)));
  result=new TextView(this);result.setText("0");result.setTextColor(Color.rgb(45,245,130));result.setTextSize(34);result.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);top.addView(result,new LinearLayout.LayoutParams(dp(58),dp(58)));
  FrameLayout.LayoutParams tlp=new FrameLayout.LayoutParams(-1,dp(58),Gravity.TOP);root.addView(top,tlp);
  pen=btn("✎");pen.setTextSize(24);pen.setBackgroundColor(Color.rgb(0,145,78));FrameLayout.LayoutParams plp=new FrameLayout.LayoutParams(dp(58),dp(58),Gravity.TOP|Gravity.START);plp.leftMargin=dp(8);plp.topMargin=dp(68);root.addView(pen,plp);
  LinearLayout bot=new LinearLayout(this);Button src=btn("CANLI");mode=btn("FARKLI");Button count=btn("SAY"),clear=btn("SİL");count.setBackgroundColor(Color.rgb(0,145,78));for(Button x:new Button[]{src,mode,count,clear})bot.addView(x,new LinearLayout.LayoutParams(0,dp(60),1));FrameLayout.LayoutParams blp=new FrameLayout.LayoutParams(-1,dp(60),Gravity.BOTTOM);root.addView(bot,blp);
  ViewCompat.setOnApplyWindowInsetsListener(root,(v,i)->{Insets z=i.getInsets(WindowInsetsCompat.Type.systemBars());tlp.topMargin=z.top;plp.topMargin=z.top+dp(66);blp.bottomMargin=z.bottom+dp(10);top.setLayoutParams(tlp);pen.setLayoutParams(plp);bot.setLayoutParams(blp);return i;});
  setContentView(root); overlay.drawMode=true;
  pen.setOnClickListener(v->{overlay.drawMode=true;overlay.clearRoi();objects=new ArrayList<>();overlay.objects=Collections.emptyList();result.setText("0");status.setText("Yeni alanı çiz");overlay.invalidate();});
  mode.setOnClickListener(v->{same=!same;mode.setText(same?"AYNI":"FARKLI");});
  count.setOnClickListener(v->{Bitmap f=latest;if(f==null)return;if(overlay.roi.size()<3){status.setText("Önce turkuaz alanı çiz");return;}analyze(f.copy(Bitmap.Config.ARGB_8888,false));});
  clear.setOnClickListener(v->{objects=new ArrayList<>();overlay.objects=Collections.emptyList();overlay.clearRoi();overlay.drawMode=true;overlay.invalidate();result.setText("0");status.setText("Alanı çiz");});
 }

 Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setAllCaps(false);b.setBackgroundColor(Color.rgb(19,58,68));return b;}

 void camera(){ListenableFuture<ProcessCameraProvider> f=ProcessCameraProvider.getInstance(this);f.addListener(()->{try{ProcessCameraProvider p=f.get();Preview pr=new Preview.Builder().build();pr.setSurfaceProvider(preview.getSurfaceProvider());ImageAnalysis a=new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build();a.setAnalyzer(cam,img->{try{Bitmap b=rgba(img);latest=rotate(b,img.getImageInfo().getRotationDegrees());}catch(Throwable ignored){}finally{img.close();}});p.unbindAll();p.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,pr,a);}catch(Throwable ignored){}},ContextCompat.getMainExecutor(this));}
 Bitmap rgba(ImageProxy im){ImageProxy.PlaneProxy p=im.getPlanes()[0];ByteBuffer bf=p.getBuffer();bf.rewind();int ps=Math.max(1,p.getPixelStride()),pw=p.getRowStride()/ps;Bitmap pad=Bitmap.createBitmap(pw,im.getHeight(),Bitmap.Config.ARGB_8888);pad.copyPixelsFromBuffer(bf);return pw==im.getWidth()?pad:Bitmap.createBitmap(pad,0,0,im.getWidth(),im.getHeight());}
 Bitmap rotate(Bitmap s,int d){if(d==0)return s;Matrix m=new Matrix();m.postRotate(d);return Bitmap.createBitmap(s,0,0,s.getWidth(),s.getHeight(),m,true);}

 void analyze(Bitmap src){if(!busy.compareAndSet(false,true))return;List<PointF> rv=overlay.copyRoi();int vw=overlay.getWidth(),vh=overlay.getHeight();status.setText("ROI içinde fiziksel nesneler taranıyor...");work.execute(()->{try{
   List<RectF> raw=new ArrayList<>();detect(src,0,0,src.getWidth(),src.getHeight(),raw);
   int g=5,cw=Math.max(1,src.getWidth()/g),ch=Math.max(1,src.getHeight()/g),pad=Math.min(cw,ch)/8;
   for(int y=0;y<g;y++)for(int x=0;x<g;x++){int l=Math.max(0,x*cw-pad),t=Math.max(0,y*ch-pad),r=Math.min(src.getWidth(),(x+1)*cw+pad),b=Math.min(src.getHeight(),(y+1)*ch+pad);detect(src,l,t,r,b,raw);}
   List<Obj> out=solve(src,raw,rv,vw,vh);objects=out;runOnUiThread(()->{overlay.objects=new ArrayList<>(out);overlay.invalidate();result.setText(""+out.size());status.setText("Fiziksel nesne: "+out.size()+" / AI aday: "+raw.size());});
  }catch(Throwable e){runOnUiThread(()->status.setText("Algılama hatası"));}finally{busy.set(false);}});}

 void detect(Bitmap s,int l,int t,int r,int b,List<RectF> out)throws Exception{if(r-l<36||b-t<36)return;Bitmap c=Bitmap.createBitmap(s,l,t,r-l,b-t);for(DetectedObject o:Tasks.await(detector.process(InputImage.fromBitmap(c,0)))){android.graphics.Rect q=o.getBoundingBox();RectF z=new RectF(q.left+l,q.top+t,q.right+l,q.bottom+t);if(z.width()>12&&z.height()>12)out.add(z);}}

 List<Obj> solve(Bitmap src,List<RectF> raw,List<PointF> rv,int vw,int vh){int W=src.getWidth(),H=src.getHeight();float sc=Math.min(1f,560f/Math.max(W,H));int w=Math.max(120,Math.round(W*sc)),h=Math.max(120,Math.round(H*sc));Bitmap sm=Bitmap.createScaledBitmap(src,w,h,true);int[] px=new int[w*h];sm.getPixels(px,0,w,0,0,w,h);
  boolean[] roi=new boolean[w*h];int rc=0;for(int y=0;y<h;y++)for(int x=0;x<w;x++){boolean ok=pip(x/sc,y/sc,W,H,rv,vw,vh);roi[y*w+x]=ok;if(ok)rc++;}
  int[] bg=background(px,roi,w,h);boolean[] fg=new boolean[w*h];for(int y=1;y<h-1;y++)for(int x=1;x<w-1;x++){int i=y*w+x;if(!roi[i])continue;int c=px[i],rr=Color.red(c),gg=Color.green(c),bb=Color.blue(c);double cd=Math.sqrt(sq(rr-bg[0])+sq(gg-bg[1])+sq(bb-bg[2]));int ed=(diff(px[i-1],px[i+1])+diff(px[i-w],px[i+w]))/2;fg[i]=cd>42||ed>72;}
  fg=open1(fg,roi,w,h);List<Comp> comps=components(fg,w,h);List<Obj> physical=new ArrayList<>();int minPix=Math.max(35,rc/3500);
  for(Comp c:comps){if(c.n<minPix)continue;RectF r=new RectF(c.l/sc,c.t/sc,(c.r+1)/sc,(c.b+1)/sc);float af=area(r)/(W*(float)H);if(af<.00018f||af>.42f)continue;if(!centerInRoi(r,W,H,rv,vw,vh))continue;float density=c.n/Math.max(1f,(c.r-c.l+1f)*(c.b-c.t+1f));if(density<.10f)continue;physical.add(new Obj(r,new PointF(c.sx/(float)c.n/sc,c.sy/(float)c.n/sc),2));}

  List<RectF> ai=cleanAI(raw,W,H,rv,vw,vh);List<Obj> supported=new ArrayList<>();
  for(RectF a:ai){float support=maskSupport(a,fg,sc,w,h);if(support<.10f)continue;PointF cen=new PointF(a.centerX(),a.centerY());Obj best=null;float bestIo=0;for(Obj q:physical){float ov=iou(a,q.box);if(ov>bestIo){bestIo=ov;best=q;}}if(best!=null&&bestIo>.10f){best.aiHits++;}else supported.add(new Obj(a,cen,1));}
  for(Obj q:physical)if(q.aiHits>0||area(q.box)/(W*(float)H)>.0012f)supported.add(q);

  supported=dedupObjects(supported);
  // Büyük grup kutusu içinde birden çok bağımsız aday varsa grup kutusunu sil.
  List<Obj> out=new ArrayList<>();for(int i=0;i<supported.size();i++){Obj a=supported.get(i);int child=0;for(int j=0;j<supported.size();j++)if(i!=j){Obj b=supported.get(j);float ar=area(b.box)/Math.max(1f,area(a.box));if(ar>.02f&&ar<.45f&&contain(b.box,a.box)>.80f)child++;}if(child<2)out.add(a);}
  out=dedupObjects(out);out.sort((a,b)->{int z=Float.compare(a.center.y,b.center.y);return z!=0?z:Float.compare(a.center.x,b.center.x);});return out;
 }

 float maskSupport(RectF a,boolean[] fg,float sc,int w,int h){int l=Math.max(0,(int)(a.left*sc)),t=Math.max(0,(int)(a.top*sc)),r=Math.min(w-1,(int)(a.right*sc)),b=Math.min(h-1,(int)(a.bottom*sc));int n=0,k=0;for(int y=t;y<=b;y+=2)for(int x=l;x<=r;x+=2){n++;if(fg[y*w+x])k++;}return n==0?0:k/(float)n;}
 List<RectF> cleanAI(List<RectF> raw,int W,int H,List<PointF> rv,int vw,int vh){List<RectF> a=new ArrayList<>();for(RectF r:raw){float af=area(r)/(W*(float)H);if(af<.00015f||af>.38f)continue;if(!centerInRoi(r,W,H,rv,vw,vh))continue;a.add(new RectF(r));}a.sort((x,y)->Float.compare(area(x),area(y)));List<RectF> k=new ArrayList<>();for(RectF c:a){boolean dup=false;for(RectF q:k)if(iou(c,q)>.55f||(contain(c,q)>.92f&&Math.min(area(c),area(q))/Math.max(area(c),area(q))>.55f)){dup=true;break;}if(!dup)k.add(c);}return k;}
 List<Obj> dedupObjects(List<Obj> a){a.sort((x,y)->Float.compare(area(y.box),area(x.box)));List<Obj> k=new ArrayList<>();for(Obj c:a){boolean dup=false;for(Obj q:k){float d=dist(c.center,q.center),sz=.30f*(diag(c.box)+diag(q.box));if(iou(c.box,q.box)>.48f||(d<sz&&contain(c.box,q.box)>.55f)){dup=true;if(c.aiHits>q.aiHits){q.box=c.box;q.center=c.center;q.aiHits=c.aiHits;}break;}}if(!dup)k.add(c);}return k;}

 int[] background(int[] p,boolean[] roi,int w,int h){ArrayList<Integer> rs=new ArrayList<>(),gs=new ArrayList<>(),bs=new ArrayList<>();for(int y=1;y<h-1;y++)for(int x=1;x<w-1;x++){int i=y*w+x;if(!roi[i])continue;if(!roi[i-1]||!roi[i+1]||!roi[i-w]||!roi[i+w]){rs.add(Color.red(p[i]));gs.add(Color.green(p[i]));bs.add(Color.blue(p[i]));}}if(rs.size()<20)for(int i=0;i<p.length;i+=Math.max(1,p.length/900))if(roi[i]){rs.add(Color.red(p[i]));gs.add(Color.green(p[i]));bs.add(Color.blue(p[i]));}Collections.sort(rs);Collections.sort(gs);Collections.sort(bs);if(rs.isEmpty())return new int[]{128,128,128};int n=rs.size()/2;return new int[]{rs.get(n),gs.get(n),bs.get(n)};}
 boolean[] open1(boolean[] a,boolean[] roi,int w,int h){return dilate(erode(a,roi,w,h),roi,w,h);}
 boolean[] dilate(boolean[] a,boolean[] roi,int w,int h){boolean[] o=new boolean[a.length];for(int y=1;y<h-1;y++)for(int x=1;x<w-1;x++){int i=y*w+x;if(!roi[i])continue;for(int dy=-1;dy<=1&&!o[i];dy++)for(int dx=-1;dx<=1;dx++)if(a[i+dy*w+dx]){o[i]=true;break;}}return o;}
 boolean[] erode(boolean[] a,boolean[] roi,int w,int h){boolean[] o=new boolean[a.length];for(int y=1;y<h-1;y++)for(int x=1;x<w-1;x++){int i=y*w+x;if(!roi[i]||!a[i])continue;boolean ok=true;for(int dy=-1;dy<=1&&ok;dy++)for(int dx=-1;dx<=1;dx++)if(!roi[i+dy*w+dx]||!a[i+dy*w+dx]){ok=false;break;}o[i]=ok;}return o;}
 List<Comp> components(boolean[] f,int w,int h){int[] lab=new int[f.length],q=new int[f.length];List<Comp> out=new ArrayList<>();int id=0;for(int s=0;s<f.length;s++){if(!f[s]||lab[s]!=0)continue;id++;int qs=0,qe=0;q[qe++]=s;lab[s]=id;Comp c=new Comp();c.l=c.r=s%w;c.t=c.b=s/w;while(qs<qe){int z=q[qs++],x=z%w,y=z/w;c.n++;c.sx+=x;c.sy+=y;c.l=Math.min(c.l,x);c.r=Math.max(c.r,x);c.t=Math.min(c.t,y);c.b=Math.max(c.b,y);for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){if(dx==0&&dy==0)continue;int nx=x+dx,ny=y+dy;if(nx<0||ny<0||nx>=w||ny>=h)continue;int ni=ny*w+nx;if(f[ni]&&lab[ni]==0){lab[ni]=id;q[qe++]=ni;}}}out.add(c);}return out;}
 static class Comp{int l,r,t,b,n;long sx,sy;}
 static class Obj{RectF box;PointF center;int aiHits;Obj(RectF b,PointF c,int h){box=new RectF(b);center=c;aiHits=h;}}

 boolean centerInRoi(RectF r,int W,int H,List<PointF> poly,int vw,int vh){PointF c=mapToView(r.centerX(),r.centerY(),W,H,vw,vh);return point(c.x,c.y,poly);}
 boolean pip(float x,float y,int W,int H,List<PointF> poly,int vw,int vh){PointF p=mapToView(x,y,W,H,vw,vh);return point(p.x,p.y,poly);}
 PointF mapToView(float x,float y,int W,int H,int vw,int vh){float sc=Math.max(vw/(float)W,vh/(float)H),dx=(vw-W*sc)/2f,dy=(vh-H*sc)/2f;return new PointF(x*sc+dx,y*sc+dy);}
 boolean point(float x,float y,List<PointF> p){boolean c=false;for(int i=0,j=p.size()-1;i<p.size();j=i++)if(((p.get(i).y>y)!=(p.get(j).y>y))&&(x<(p.get(j).x-p.get(i).x)*(y-p.get(i).y)/(p.get(j).y-p.get(i).y+.0001f)+p.get(i).x))c=!c;return c;}
 float area(RectF r){return Math.max(0,r.width())*Math.max(0,r.height());}float iou(RectF a,RectF b){float l=Math.max(a.left,b.left),t=Math.max(a.top,b.top),r=Math.min(a.right,b.right),d=Math.min(a.bottom,b.bottom),in=Math.max(0,r-l)*Math.max(0,d-t);return in/(area(a)+area(b)-in+.001f);}float contain(RectF a,RectF b){float l=Math.max(a.left,b.left),t=Math.max(a.top,b.top),r=Math.min(a.right,b.right),d=Math.min(a.bottom,b.bottom),in=Math.max(0,r-l)*Math.max(0,d-t);return in/(area(a)+.001f);}float diag(RectF r){return (float)Math.hypot(r.width(),r.height());}float dist(PointF a,PointF b){return (float)Math.hypot(a.x-b.x,a.y-b.y);}double sq(double x){return x*x;}int diff(int a,int b){return Math.abs(Color.red(a)-Color.red(b))+Math.abs(Color.green(a)-Color.green(b))+Math.abs(Color.blue(a)-Color.blue(b));}
 void refresh(){result.setText(""+objects.size());}
 int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
 @Override public void onRequestPermissionsResult(int r,@NonNull String[] p,@NonNull int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)camera();}

 class Overlay extends View{
  Paint num=new Paint(1),txt=new Paint(1),rp=new Paint(1);List<Obj> objects=new ArrayList<>();boolean drawMode=true;List<PointF> roi=new ArrayList<>();Path path=new Path();
  Overlay(){super(MainActivityV8.this);num.setColor(Color.rgb(0,165,88));txt.setColor(Color.WHITE);txt.setTextAlign(Paint.Align.CENTER);txt.setTextSize(dp(16));rp.setColor(Color.CYAN);rp.setStyle(Paint.Style.STROKE);rp.setStrokeWidth(dp(4));setOnTouchListener((v,e)->{if(drawMode){if(e.getAction()==MotionEvent.ACTION_DOWN){roi.clear();path.reset();roi.add(new PointF(e.getX(),e.getY()));path.moveTo(e.getX(),e.getY());invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){roi.add(new PointF(e.getX(),e.getY()));path.lineTo(e.getX(),e.getY());invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_UP){roi.add(new PointF(e.getX(),e.getY()));path.close();drawMode=false;status.setText("ROI kilitli — SAY'a bas");invalidate();return true;}}return true;});}
  List<PointF> copyRoi(){List<PointF> x=new ArrayList<>();for(PointF p:roi)x.add(new PointF(p.x,p.y));return x;}void clearRoi(){roi.clear();path.reset();}
  @Override protected void onDraw(Canvas c){super.onDraw(c);if(!roi.isEmpty())c.drawPath(path,rp);for(int i=0;i<objects.size();i++){PointF v=map(objects.get(i).center);float cx=v.x,cy=v.y;c.drawCircle(cx,cy,dp(16),num);c.drawText(""+(i+1),cx,cy+dp(6),txt);}}
  PointF map(PointF s){Bitmap f=latest;if(f==null)return new PointF(s.x,s.y);float vw=getWidth(),vh=getHeight(),sw=f.getWidth(),sh=f.getHeight(),sc=Math.max(vw/sw,vh/sh),dx=(vw-sw*sc)/2f,dy=(vh-sh*sc)/2f;return new PointF(s.x*sc+dx,s.y*sc+dy);}
 }
}
