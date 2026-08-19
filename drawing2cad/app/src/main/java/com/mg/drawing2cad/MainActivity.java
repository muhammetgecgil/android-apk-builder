package com.mg.drawing2cad;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class MainActivity extends Activity {
  static final int PICK=10,SAVE=11;
  final int BG=Color.rgb(8,13,24),PANEL=Color.rgb(15,23,42),PANEL2=Color.rgb(22,32,52),TEXT=Color.rgb(226,232,240),MUTED=Color.rgb(148,163,184),ACC=Color.rgb(34,211,238);
  Bitmap source; boolean[][] solid; MeshView mesh; TextView status,fileInfo; String pendingStl; Uri archiveUri; ArrayList<String> entries=new ArrayList<>();

  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);buildUi();}

  void buildUi(){
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
    LinearLayout head=new LinearLayout(this);head.setPadding(dp(16),dp(10),dp(12),dp(10));head.setGravity(Gravity.CENTER_VERTICAL);head.setBackgroundColor(PANEL);
    LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);titles.addView(txt("MG Drawing2CAD",21,TEXT,true));titles.addView(txt("MOBILE CAD WORKSPACE • v1.2",10,MUTED,false));head.addView(titles,new LinearLayout.LayoutParams(0,-2,1));head.addView(txt("● CONTOUR 3D",10,ACC,true));root.addView(head);
    HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);LinearLayout bar=new LinearLayout(this);bar.setPadding(dp(8),dp(7),dp(8),dp(7));Button open=tool("⌂ AÇ"),make=tool("◈ 3D OLUŞTUR"),save=tool("⇩ STL"),iso=tool("◇ ISO"),top=tool("▱ ÜST"),fit=tool("⊙ SIĞDIR");for(Button x:new Button[]{open,make,save,iso,top,fit})bar.addView(x);hs.addView(bar);root.addView(hs);
    fileInfo=txt("DOSYA: —   |   PDF • PNG • JPG • ZIP",11,MUTED,false);fileInfo.setPadding(dp(14),dp(7),dp(14),dp(7));fileInfo.setBackgroundColor(PANEL2);root.addView(fileInfo);
    FrameLayout stage=new FrameLayout(this);mesh=new MeshView(this);stage.addView(mesh,new FrameLayout.LayoutParams(-1,-1));root.addView(stage,new LinearLayout.LayoutParams(-1,0,1));
    status=txt("Teknik resmi aç. Ana siyah kapalı kontur otomatik kırpılıp tek 3D katıya çevrilir.",11,MUTED,false);status.setPadding(dp(12),dp(9),dp(12),dp(10));status.setBackgroundColor(PANEL);root.addView(status);setContentView(root);
    open.setOnClickListener(v->pick());make.setOnClickListener(v->make3d());save.setOnClickListener(v->save());iso.setOnClickListener(v->mesh.iso());top.setOnClickListener(v->mesh.top());fit.setOnClickListener(v->mesh.fit());
  }
  TextView txt(String s,int z,int c,boolean b){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);if(b)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
  Button tool(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(10);b.setAllCaps(false);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(12),dp(8),dp(12),dp(8));GradientDrawable g=new GradientDrawable();g.setColor(PANEL2);g.setCornerRadius(dp(8));g.setStroke(1,Color.rgb(51,65,85));b.setBackground(g);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(40));lp.setMargins(dp(4),0,dp(4),0);b.setLayoutParams(lp);return b;}
  int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}

  void pick(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","application/pdf","application/zip","application/x-zip-compressed","application/octet-stream"});startActivityForResult(i,PICK);}
  @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(c!=RESULT_OK||d==null)return;try{if(r==PICK){Uri u=d.getData();String n=fileName(u),l=n.toLowerCase(Locale.ROOT);if(l.endsWith(".zip")||l.endsWith(".cbz")||l.endsWith(".jar")){archiveUri=u;listArchive(u);}else if(l.endsWith(".pdf")){source=renderPdf(u);loaded(n,"PDF");}else{try(InputStream in=getContentResolver().openInputStream(u)){source=BitmapFactory.decodeStream(in);}loaded(n,"IMAGE");}}else if(r==SAVE&&pendingStl!=null){try(OutputStream o=getContentResolver().openOutputStream(d.getData())){o.write(pendingStl.getBytes("UTF-8"));}status.setText("✓ STL kaydedildi.");}}catch(Exception e){status.setText("Hata: "+e.getMessage());}}
  void loaded(String n,String type)throws Exception{if(source==null)throw new IOException("Dosya görüntüye çevrilemedi");solid=null;mesh.setBitmap(source);fileInfo.setText("DOSYA: "+n+"   |   "+type);status.setText("✓ Teknik resim yüklendi • "+source.getWidth()+"×"+source.getHeight()+" • 3D OLUŞTUR'a bas.");}
  String fileName(Uri u){String n="dosya";Cursor c=null;try{c=getContentResolver().query(u,null,null,null,null);if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)n=c.getString(i);}}catch(Exception ignored){}finally{if(c!=null)c.close();}return n==null?"dosya":n;}

  void listArchive(Uri u)throws Exception{entries.clear();try(InputStream in=getContentResolver().openInputStream(u);ZipInputStream z=new ZipInputStream(new BufferedInputStream(in))){ZipEntry e;while((e=z.getNextEntry())!=null){if(e.isDirectory())continue;String l=e.getName().toLowerCase(Locale.ROOT);if(l.endsWith(".pdf")||l.endsWith(".png")||l.endsWith(".jpg")||l.endsWith(".jpeg")||l.endsWith(".webp"))entries.add(e.getName());}}if(entries.isEmpty())throw new IOException("ZIP içinde desteklenen teknik resim yok");String[] a=entries.toArray(new String[0]);new AlertDialog.Builder(this).setTitle("ZIP içinden teknik resim seç").setItems(a,(q,w)->{try{loadEntry(a[w]);}catch(Exception ex){status.setText("ZIP hatası: "+ex.getMessage());}}).setNegativeButton("İptal",null).show();}
  void loadEntry(String wanted)throws Exception{File f=new File(getCacheDir(),"d2c_"+Math.abs(wanted.hashCode())+(wanted.toLowerCase(Locale.ROOT).endsWith(".pdf")?".pdf":".img"));try(InputStream in=getContentResolver().openInputStream(archiveUri);ZipInputStream z=new ZipInputStream(new BufferedInputStream(in))){ZipEntry e;while((e=z.getNextEntry())!=null)if(e.getName().equals(wanted)){try(FileOutputStream o=new FileOutputStream(f)){byte[] b=new byte[16384];int n;long total=0;while((n=z.read(b))>0){total+=n;if(total>60L*1024*1024)throw new IOException("ZIP girdisi çok büyük");o.write(b,0,n);}}break;}}if(wanted.toLowerCase(Locale.ROOT).endsWith(".pdf"))source=renderPdfFile(f);else try(InputStream in=new FileInputStream(f)){source=BitmapFactory.decodeStream(in);}loaded(wanted,"ZIP");}
  Bitmap renderPdf(Uri u)throws Exception{File f=new File(getCacheDir(),"direct.pdf");try(InputStream in=getContentResolver().openInputStream(u);FileOutputStream o=new FileOutputStream(f)){byte[] b=new byte[16384];int n;while((n=in.read(b))>0)o.write(b,0,n);}return renderPdfFile(f);}
  Bitmap renderPdfFile(File f)throws Exception{ParcelFileDescriptor pfd=ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer r=new PdfRenderer(pfd);PdfRenderer.Page p=r.openPage(0);float k=Math.min(2.3f,1900f/Math.max(p.getWidth(),p.getHeight()));Bitmap b=Bitmap.createBitmap(Math.max(1,(int)(p.getWidth()*k)),Math.max(1,(int)(p.getHeight()*k)),Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);p.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);p.close();r.close();pfd.close();return b;}

  void make3d(){if(source==null){status.setText("Önce teknik resim aç.");return;}status.setText("Ana kontur aranıyor • ölçü çizgileri filtreleniyor • kapalı alan dolduruluyor...");new Thread(()->{try{boolean[][] s=analyze(source);solid=s;int n=count(s);runOnUiThread(()->{mesh.setSolid(s);mesh.iso();status.setText("✓ TEK PARÇA 3D MODEL • "+n+" hücre • otomatik kırpma + kapalı kontur");});}catch(Exception e){runOnUiThread(()->status.setText("Analiz hatası: "+e.getMessage()));}}).start();}

  boolean[][] analyze(Bitmap bm)throws Exception{
    int sw=bm.getWidth(),sh=bm.getHeight();int step=Math.max(1,Math.max(sw,sh)/1400);int minX=sw,minY=sh,maxX=-1,maxY=-1;
    for(int y=0;y<sh;y+=step)for(int x=0;x<sw;x+=step){int c=bm.getPixel(x,y);int g=(Color.red(c)*30+Color.green(c)*59+Color.blue(c)*11)/100;if(g<85){if(x<minX)minX=x;if(x>maxX)maxX=x;if(y<minY)minY=y;if(y>maxY)maxY=y;}}
    if(maxX<minX||maxY<minY)throw new IOException("Siyah ana kontur bulunamadı");int mx=Math.max(8,(maxX-minX)/35),my=Math.max(8,(maxY-minY)/35);minX=Math.max(0,minX-mx);minY=Math.max(0,minY-my);maxX=Math.min(sw-1,maxX+mx);maxY=Math.min(sh-1,maxY+my);
    Bitmap crop=Bitmap.createBitmap(bm,minX,minY,maxX-minX+1,maxY-minY+1);int cw=crop.getWidth(),ch=crop.getHeight();int gw,gh;if(cw>=ch){gw=96;gh=Math.max(24,Math.round(96f*ch/cw));}else{gh=96;gw=Math.max(24,Math.round(96f*cw/ch));}Bitmap b=Bitmap.createScaledBitmap(crop,gw,gh,true);
    boolean[][] wall=new boolean[gh][gw];for(int y=0;y<gh;y++)for(int x=0;x<gw;x++){int c=b.getPixel(x,y);int g=(Color.red(c)*30+Color.green(c)*59+Color.blue(c)*11)/100;wall[y][x]=g<105;}
    boolean[][] thick=new boolean[gh][gw];for(int y=0;y<gh;y++)for(int x=0;x<gw;x++){boolean v=false;for(int dy=-1;dy<=1&&!v;dy++)for(int dx=-1;dx<=1;dx++){int yy=y+dy,xx=x+dx;if(yy>=0&&yy<gh&&xx>=0&&xx<gw&&wall[yy][xx]){v=true;break;}}thick[y][x]=v;}
    boolean[][] out=new boolean[gh][gw];ArrayDeque<int[]> q=new ArrayDeque<>();for(int x=0;x<gw;x++){seed(x,0,thick,out,q);seed(x,gh-1,thick,out,q);}for(int y=0;y<gh;y++){seed(0,y,thick,out,q);seed(gw-1,y,thick,out,q);}int[] dx={1,-1,0,0},dy={0,0,1,-1};while(!q.isEmpty()){int[] p=q.removeFirst();for(int k=0;k<4;k++){int x=p[0]+dx[k],y=p[1]+dy[k];if(x>=0&&x<gw&&y>=0&&y<gh&&!thick[y][x]&&!out[y][x]){out[y][x]=true;q.add(new int[]{x,y});}}}
    boolean[][] s=new boolean[gh][gw];for(int y=0;y<gh;y++)for(int x=0;x<gw;x++)s[y][x]=thick[y][x]||!out[y][x];
    // keep largest connected solid region
    boolean[][] seen=new boolean[gh][gw],best=null;int bestN=0;for(int yy=0;yy<gh;yy++)for(int xx=0;xx<gw;xx++)if(s[yy][xx]&&!seen[yy][xx]){boolean[][] cur=new boolean[gh][gw];ArrayDeque<int[]> z=new ArrayDeque<>();z.add(new int[]{xx,yy});seen[yy][xx]=true;int n=0;while(!z.isEmpty()){int[] p=z.removeFirst();cur[p[1]][p[0]]=true;n++;for(int k=0;k<4;k++){int x=p[0]+dx[k],y=p[1]+dy[k];if(x>=0&&x<gw&&y>=0&&y<gh&&s[y][x]&&!seen[y][x]){seen[y][x]=true;z.add(new int[]{x,y});}}}if(n>bestN){bestN=n;best=cur;}}
    if(best==null||bestN<20)throw new IOException("Kapalı ana profil oluşturulamadı");return best;
  }
  void seed(int x,int y,boolean[][] w,boolean[][] o,ArrayDeque<int[]> q){if(!w[y][x]&&!o[y][x]){o[y][x]=true;q.add(new int[]{x,y});}}
  int count(boolean[][] s){int n=0;for(boolean[] r:s)for(boolean v:r)if(v)n++;return n;}

  void save(){if(solid==null){status.setText("Önce 3D model oluştur.");return;}pendingStl=toStl(solid);Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("model/stl");i.putExtra(Intent.EXTRA_TITLE,"MG_Drawing2CAD_v12_Model.stl");startActivityForResult(i,SAVE);}
  String toStl(boolean[][] a){StringBuilder s=new StringBuilder("solid MG_Drawing2CAD\n");int h=a.length,w=a[0].length;float z=6;for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(a[y][x]){face(s,x,y,z,true);face(s,x,y,0,false);if(x==0||!a[y][x-1])sideX(s,x,y,z,false);if(x==w-1||!a[y][x+1])sideX(s,x+1,y,z,true);if(y==0||!a[y-1][x])sideY(s,x,y,z,false);if(y==h-1||!a[y+1][x])sideY(s,x,y+1,z,true);}return s.append("endsolid MG_Drawing2CAD\n").toString();}
  void tri(StringBuilder s,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz){s.append("facet normal 0 0 0\n outer loop\n");v(s,ax,ay,az);v(s,bx,by,bz);v(s,cx,cy,cz);s.append(" endloop\nendfacet\n");}
  void v(StringBuilder s,float x,float y,float z){s.append("  vertex ").append(x).append(' ').append(y).append(' ').append(z).append('\n');}
  void face(StringBuilder s,float x,float y,float z,boolean t){if(t){tri(s,x,y,z,x+1,y,z,x+1,y+1,z);tri(s,x,y,z,x+1,y+1,z,x,y+1,z);}else{tri(s,x,y,0,x+1,y+1,0,x+1,y,0);tri(s,x,y,0,x,y+1,0,x+1,y+1,0);}}
  void sideX(StringBuilder s,float x,float y,float z,boolean p){if(p){tri(s,x,y,0,x,y+1,0,x,y+1,z);tri(s,x,y,0,x,y+1,z,x,y,z);}else{tri(s,x,y,0,x,y+1,z,x,y+1,0);tri(s,x,y,0,x,y,z,x,y+1,z);}}
  void sideY(StringBuilder s,float x,float y,float z,boolean p){if(p){tri(s,x,y,0,x,y,z,x+1,y,z);tri(s,x,y,0,x+1,y,z,x+1,y,0);}else{tri(s,x,y,0,x+1,y,z,x,y,z);tri(s,x,y,0,x+1,y,0,x+1,y,z);}}

  static class MeshView extends View{
    Paint fill=new Paint(3),edge=new Paint(3),grid=new Paint(1);Bitmap bmp;boolean[][] s;float yaw=.72f,pitch=.62f,scale=7,lx,ly,lastD;boolean drag;
    MeshView(Context c){super(c);setBackgroundColor(Color.rgb(7,12,22));edge.setStyle(Paint.Style.STROKE);edge.setStrokeWidth(.7f);edge.setColor(Color.rgb(20,80,95));grid.setColor(Color.rgb(16,31,49));}
    void setBitmap(Bitmap b){bmp=b;s=null;invalidate();}void setSolid(boolean[][] a){s=a;bmp=null;fit();}void iso(){yaw=.72f;pitch=.62f;invalidate();}void top(){yaw=0;pitch=0;invalidate();}void fit(){scale=s==null?7:Math.max(3f,Math.min(9f,Math.min(getWidth()>0?getWidth()/(float)(s[0].length+10):6,getHeight()>0?getHeight()/(float)(s.length+10):6)));invalidate();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);for(int x=0;x<getWidth();x+=32)c.drawLine(x,0,x,getHeight(),grid);for(int y=0;y<getHeight();y+=32)c.drawLine(0,y,getWidth(),y,grid);if(s==null){if(bmp!=null){float k=Math.min((getWidth()-40f)/bmp.getWidth(),(getHeight()-40f)/bmp.getHeight());float w=bmp.getWidth()*k,h=bmp.getHeight()*k;c.drawBitmap(bmp,null,new RectF((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2),fill);}return;}c.save();c.translate(getWidth()/2f,getHeight()/2f+30);int h=s.length,w=s[0].length;for(int y=0;y<h;y++){int x=0;while(x<w){while(x<w&&!s[y][x])x++;if(x>=w)break;int a=x;while(x<w&&s[y][x])x++;drawBlock(c,a-w/2f,y-h/2f,0,x-a,1,1);}}c.restore();}
    void drawBlock(Canvas c,float x,float y,float z,float wx,float wy,float wz){float[][] v={{x,y,z},{x+wx,y,z},{x+wx,y+wy,z},{x,y+wy,z},{x,y,z+wz},{x+wx,y,z+wz},{x+wx,y+wy,z+wz},{x,y+wy,z+wz}};PointF[] q=new PointF[8];for(int i=0;i<8;i++)q[i]=proj(v[i][0],v[i][1],v[i][2]);poly(c,q,new int[]{4,5,6,7},Color.rgb(70,210,230));poly(c,q,new int[]{1,2,6,5},Color.rgb(35,150,180));poly(c,q,new int[]{2,3,7,6},Color.rgb(28,110,145));}
    PointF proj(float x,float y,float z){float cy=(float)Math.cos(yaw),sy=(float)Math.sin(yaw),cp=(float)Math.cos(pitch),sp=(float)Math.sin(pitch);float X=x*cy-y*sy,Y=x*sy+y*cy;return new PointF(X*scale,(Y*cp-z*sp)*scale);}
    void poly(Canvas c,PointF[] q,int[] id,int col){Path p=new Path();p.moveTo(q[id[0]].x,q[id[0]].y);for(int i=1;i<id.length;i++)p.lineTo(q[id[i]].x,q[id[i]].y);p.close();fill.setColor(col);fill.setStyle(Paint.Style.FILL);c.drawPath(p,fill);c.drawPath(p,edge);}
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getPointerCount()==2){float d=(float)Math.hypot(e.getX(0)-e.getX(1),e.getY(0)-e.getY(1));if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&lastD>0){scale*=d/lastD;scale=Math.max(2,Math.min(18,scale));invalidate();}lastD=d;return true;}lastD=0;if(e.getAction()==MotionEvent.ACTION_DOWN){lx=e.getX();ly=e.getY();drag=true;return true;}if(e.getAction()==MotionEvent.ACTION_MOVE&&drag){yaw+=(e.getX()-lx)*.01f;pitch+=(e.getY()-ly)*.01f;pitch=Math.max(-1.25f,Math.min(1.25f,pitch));lx=e.getX();ly=e.getY();invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_UP)drag=false;return true;}
  }
}
