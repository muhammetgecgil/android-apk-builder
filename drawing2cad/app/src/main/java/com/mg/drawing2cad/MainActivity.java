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
  final int BG=Color.rgb(7,12,22), PANEL=Color.rgb(15,23,42), PANEL2=Color.rgb(22,32,52), TEXT=Color.rgb(226,232,240), MUTED=Color.rgb(148,163,184), ACC=Color.rgb(34,211,238);
  Bitmap source; boolean[][] solid; MeshView mesh; TextView status,fileInfo,thickText; String pendingStl; Uri archiveUri; ArrayList<String> entries=new ArrayList<>(); int zHeight=8;

  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);buildUi();}

  void buildUi(){
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
    LinearLayout head=new LinearLayout(this);head.setPadding(dp(16),dp(10),dp(12),dp(10));head.setGravity(Gravity.CENTER_VERTICAL);head.setBackgroundColor(PANEL);
    LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);titles.addView(txt("MG Drawing2CAD",21,TEXT,true));titles.addView(txt("TECHNICAL DRAWING CAD • v1.4",10,MUTED,false));head.addView(titles,new LinearLayout.LayoutParams(0,-2,1));head.addView(txt("● ISO DRAWING MODE",9,ACC,true));root.addView(head);

    HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);LinearLayout bar=new LinearLayout(this);bar.setPadding(dp(8),dp(7),dp(8),dp(7));
    Button open=tool("⌂ AÇ"),make=tool("◈ 3D OLUŞTUR"),save=tool("⇩ STL"),iso=tool("◇ ISO"),top=tool("▱ ÜST"),fit=tool("⊙ SIĞDIR"),grid=tool("# GRID");
    for(Button x:new Button[]{open,make,save,iso,top,fit,grid})bar.addView(x);hs.addView(bar);root.addView(hs);

    LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(12),dp(5),dp(12),dp(5));info.setBackgroundColor(PANEL2);
    info.addView(txt("TEKNİK RESİM FİLTRESİ: ölçü • uzatma • eksen • yazı → geometri dışı",10,ACC,true));
    LinearLayout settings=new LinearLayout(this);settings.setGravity(Gravity.CENTER_VERTICAL);thickText=txt("KALINLIK: 8",10,TEXT,true);settings.addView(thickText,new LinearLayout.LayoutParams(dp(110),-2));SeekBar thick=new SeekBar(this);thick.setMax(23);thick.setProgress(6);settings.addView(thick,new LinearLayout.LayoutParams(0,dp(34),1));info.addView(settings);root.addView(info);
    thick.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){zHeight=p+2;thickText.setText("KALINLIK: "+zHeight);if(solid!=null)mesh.setSolid(solid,zHeight);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});

    fileInfo=txt("DOSYA: —   |   PDF • PNG • JPG • ZIP",11,MUTED,false);fileInfo.setPadding(dp(14),dp(7),dp(14),dp(7));fileInfo.setBackgroundColor(Color.rgb(17,27,46));root.addView(fileInfo);
    FrameLayout stage=new FrameLayout(this);mesh=new MeshView(this);stage.addView(mesh,new FrameLayout.LayoutParams(-1,-1));root.addView(stage,new LinearLayout.LayoutParams(-1,0,1));
    status=txt("Ölçülendirilmiş teknik resmi aç. Ana görünüş otomatik seçilir; ölçü çizgileri 3D geometriye katılmaz.",11,MUTED,false);status.setPadding(dp(12),dp(9),dp(12),dp(10));status.setBackgroundColor(PANEL);root.addView(status);setContentView(root);

    open.setOnClickListener(v->pick());make.setOnClickListener(v->make3d());save.setOnClickListener(v->save());iso.setOnClickListener(v->mesh.iso());top.setOnClickListener(v->mesh.top());fit.setOnClickListener(v->mesh.fit());grid.setOnClickListener(v->mesh.toggleGrid());
  }

  TextView txt(String s,int z,int c,boolean b){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);if(b)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
  Button tool(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(10);b.setAllCaps(false);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(12),dp(8),dp(12),dp(8));GradientDrawable g=new GradientDrawable();g.setColor(PANEL2);g.setCornerRadius(dp(8));g.setStroke(1,Color.rgb(51,65,85));b.setBackground(g);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(40));lp.setMargins(dp(4),0,dp(4),0);b.setLayoutParams(lp);return b;}
  int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}

  void pick(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","application/pdf","application/zip","application/x-zip-compressed","application/octet-stream"});startActivityForResult(i,PICK);}
  @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(c!=RESULT_OK||d==null)return;try{if(r==PICK){Uri u=d.getData();String n=fileName(u),l=n.toLowerCase(Locale.ROOT);if(l.endsWith(".zip")||l.endsWith(".cbz")||l.endsWith(".jar")){archiveUri=u;listArchive(u);}else if(l.endsWith(".pdf")){source=renderPdf(u);loaded(n,"PDF");}else{try(InputStream in=getContentResolver().openInputStream(u)){source=BitmapFactory.decodeStream(in);}loaded(n,"IMAGE");}}else if(r==SAVE&&pendingStl!=null){try(OutputStream o=getContentResolver().openOutputStream(d.getData())){o.write(pendingStl.getBytes("UTF-8"));}status.setText("✓ STL kaydedildi.");}}catch(Exception e){status.setText("Hata: "+e.getMessage());}}
  void loaded(String n,String type)throws Exception{if(source==null)throw new IOException("Dosya görüntüye çevrilemedi");solid=null;mesh.setBitmap(source);fileInfo.setText("DOSYA: "+n+"   |   "+type);status.setText("✓ Teknik resim yüklendi • ölçü/yazı filtresi hazır • 3D OLUŞTUR'a bas.");}
  String fileName(Uri u){String n="dosya";Cursor c=null;try{c=getContentResolver().query(u,null,null,null,null);if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)n=c.getString(i);}}catch(Exception ignored){}finally{if(c!=null)c.close();}return n==null?"dosya":n;}

  void listArchive(Uri u)throws Exception{entries.clear();try(InputStream in=getContentResolver().openInputStream(u);ZipInputStream z=new ZipInputStream(new BufferedInputStream(in))){ZipEntry e;while((e=z.getNextEntry())!=null){if(e.isDirectory())continue;String l=e.getName().toLowerCase(Locale.ROOT);if(l.endsWith(".pdf")||l.endsWith(".png")||l.endsWith(".jpg")||l.endsWith(".jpeg")||l.endsWith(".webp"))entries.add(e.getName());}}if(entries.isEmpty())throw new IOException("ZIP içinde desteklenen teknik resim yok");String[] a=entries.toArray(new String[0]);new AlertDialog.Builder(this).setTitle("ZIP içinden teknik resim seç").setItems(a,(q,w)->{try{loadEntry(a[w]);}catch(Exception ex){status.setText("ZIP hatası: "+ex.getMessage());}}).setNegativeButton("İptal",null).show();}
  void loadEntry(String wanted)throws Exception{File f=new File(getCacheDir(),"d2c_"+Math.abs(wanted.hashCode())+(wanted.toLowerCase(Locale.ROOT).endsWith(".pdf")?".pdf":".img"));boolean found=false;try(InputStream in=getContentResolver().openInputStream(archiveUri);ZipInputStream z=new ZipInputStream(new BufferedInputStream(in))){ZipEntry e;while((e=z.getNextEntry())!=null)if(e.getName().equals(wanted)){try(FileOutputStream o=new FileOutputStream(f)){byte[] b=new byte[16384];int n;long total=0;while((n=z.read(b))>0){total+=n;if(total>80L*1024*1024)throw new IOException("ZIP girdisi çok büyük");o.write(b,0,n);}}found=true;break;}}if(!found)throw new IOException("ZIP girdisi bulunamadı");if(wanted.toLowerCase(Locale.ROOT).endsWith(".pdf"))source=renderPdfFile(f);else try(InputStream in=new FileInputStream(f)){source=BitmapFactory.decodeStream(in);}loaded(wanted,"ZIP");}
  Bitmap renderPdf(Uri u)throws Exception{File f=new File(getCacheDir(),"direct.pdf");try(InputStream in=getContentResolver().openInputStream(u);FileOutputStream o=new FileOutputStream(f)){byte[] b=new byte[16384];int n;while((n=in.read(b))>0)o.write(b,0,n);}return renderPdfFile(f);}
  Bitmap renderPdfFile(File f)throws Exception{ParcelFileDescriptor pfd=ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer r=new PdfRenderer(pfd);if(r.getPageCount()<1){r.close();pfd.close();throw new IOException("PDF boş");}PdfRenderer.Page p=r.openPage(0);float k=Math.min(2.8f,2300f/Math.max(p.getWidth(),p.getHeight()));Bitmap b=Bitmap.createBitmap(Math.max(1,(int)(p.getWidth()*k)),Math.max(1,(int)(p.getHeight()*k)),Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);p.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);p.close();r.close();pfd.close();return b;}

  void make3d(){if(source==null){status.setText("Önce teknik resim aç.");return;}status.setText("TEKNİK RESİM ANALİZİ: ana görünüş • çizgi kalınlığı • ölçü/yazı filtresi • delik/slot...");new Thread(()->{try{AnalyzeResult a=analyzeTechnicalDrawing(source);solid=a.mask;runOnUiThread(()->{mesh.setSolid(a.mask,zHeight);mesh.iso();status.setText("✓ 3D HAZIR • "+a.holes+" delik/slot • "+a.noise+" ölçü/yazı bileşeni elendi • kalınlık "+zHeight);});}catch(Exception e){runOnUiThread(()->status.setText("Analiz hatası: "+e.getMessage()));}}).start();}

  static class Comp{ArrayList<int[]> pts=new ArrayList<>();int minX=99999,minY=99999,maxX=-1,maxY=-1;void add(int x,int y){pts.add(new int[]{x,y});minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);}int size(){return pts.size();}int bw(){return maxX-minX+1;}int bh(){return maxY-minY+1;}int area(){return bw()*bh();}}
  static class AnalyzeResult{boolean[][] mask;int holes,noise;AnalyzeResult(boolean[][] m,int h,int n){mask=m;holes=h;noise=n;}}

  AnalyzeResult analyzeTechnicalDrawing(Bitmap bm)throws Exception{
    Bitmap page=scaleTo(bm,180,180);int h=page.getHeight(),w=page.getWidth();
    boolean[][] raw=new boolean[h][w];for(int y=0;y<h;y++)for(int x=0;x<w;x++)raw[y][x]=gray(page.getPixel(x,y))<145;
    boolean[][] strong=new boolean[h][w];for(int y=0;y<h;y++)for(int x=0;x<w;x++){int g=gray(page.getPixel(x,y));int n=localCount(raw,x,y,2);strong[y][x]=(g<70)||(g<120&&n>=7);}
    ArrayList<Comp> strongComps=components(strong);Comp main=null;double best=-1;int pageCx=w/2,pageCy=h/2;
    for(Comp c:strongComps){if(c.size()<12)continue;int mn=Math.min(c.bw(),c.bh()),mx=Math.max(c.bw(),c.bh());if(mn<=1&&mx>14)continue;double centerPenalty=Math.hypot((c.minX+c.maxX)/2.0-pageCx,(c.minY+c.maxY)/2.0-pageCy);double score=c.size()*6.0+c.area()*0.8-centerPenalty*0.35;if(score>best){best=score;main=c;}}
    if(main==null)throw new IOException("Ana görünüş bulunamadı. Çizgi kalitesi veya kontrast yetersiz.");

    int pad=Math.max(5,Math.max(main.bw(),main.bh())/12);int x0=Math.max(0,main.minX-pad),y0=Math.max(0,main.minY-pad),x1=Math.min(w-1,main.maxX+pad),y1=Math.min(h-1,main.maxY+pad);
    Bitmap roi=Bitmap.createBitmap(page,x0,y0,x1-x0+1,y1-y0+1);roi=scaleTo(roi,120,120);h=roi.getHeight();w=roi.getWidth();
    raw=new boolean[h][w];for(int y=0;y<h;y++)for(int x=0;x<w;x++)raw[y][x]=gray(roi.getPixel(x,y))<155;

    ArrayList<Comp> all=components(raw);boolean[][] clean=new boolean[h][w];int noise=0;Comp outer=null;double outerScore=-1;
    for(Comp c:all){int mn=Math.min(c.bw(),c.bh()),mx=Math.max(c.bw(),c.bh());boolean thin=(mn<=1&&mx>10);boolean tiny=(c.area()<22||c.size()<8);if(thin||tiny){noise++;continue;}double s=c.size()*5.0+c.area();if(s>outerScore){outerScore=s;outer=c;}for(int[] p:c.pts)clean[p[1]][p[0]]=true;}
    if(outer==null)throw new IOException("Ana parça konturu seçilemedi.");

    boolean[][] outerWall=maskOf(h,w,outer);outerWall=dilate(outerWall,1);boolean[][] body=fillInside(outerWall);if(count(body)<60)throw new IOException("Dış profil kapalı değil veya ölçü çizgileri profile karışıyor.");

    // İç boşlukları ayrı kapalı kontur olarak seç. Küçük yazı halkalarını reddet.
    int holes=0;for(Comp c:all){if(c==outer)continue;if(c.size()<10||c.bw()<4||c.bh()<4)continue;int cx=(c.minX+c.maxX)/2,cy=(c.minY+c.maxY)/2;if(cx<0||cy<0||cx>=w||cy>=h||!body[cy][cx])continue;if(c.area()>outer.area()*0.35)continue;double compact=(double)c.size()/Math.max(1,c.area());if(compact<0.08)continue;boolean[][] loop=dilate(maskOf(h,w,c),1);boolean[][] enclosed=fillInside(loop);int inside=count(enclosed)-count(loop);if(inside<8)continue;for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(enclosed[y][x])body[y][x]=false;holes++;}

    body=largestSolid(body);body=majority(body);if(count(body)<50)throw new IOException("Geçerli tek parça 3D gövde üretilemedi.");return new AnalyzeResult(body,holes,noise);
  }

  Bitmap scaleTo(Bitmap b,int mw,int mh){float k=Math.min((float)mw/b.getWidth(),(float)mh/b.getHeight());k=Math.min(1f,k);return Bitmap.createScaledBitmap(b,Math.max(1,Math.round(b.getWidth()*k)),Math.max(1,Math.round(b.getHeight()*k)),true);}
  int gray(int c){return (Color.red(c)*30+Color.green(c)*59+Color.blue(c)*11)/100;}
  int localCount(boolean[][] a,int x,int y,int r){int h=a.length,w=a[0].length,n=0;for(int yy=Math.max(0,y-r);yy<=Math.min(h-1,y+r);yy++)for(int xx=Math.max(0,x-r);xx<=Math.min(w-1,x+r);xx++)if(a[yy][xx])n++;return n;}
  ArrayList<Comp> components(boolean[][] a){int h=a.length,w=a[0].length;boolean[][] seen=new boolean[h][w];ArrayList<Comp> list=new ArrayList<>();int[] dx={1,-1,0,0,1,1,-1,-1},dy={0,0,1,-1,1,-1,1,-1};for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(a[y][x]&&!seen[y][x]){Comp c=new Comp();ArrayDeque<int[]> q=new ArrayDeque<>();q.add(new int[]{x,y});seen[y][x]=true;while(!q.isEmpty()){int[] p=q.removeFirst();c.add(p[0],p[1]);for(int k=0;k<8;k++){int xx=p[0]+dx[k],yy=p[1]+dy[k];if(xx>=0&&xx<w&&yy>=0&&yy<h&&a[yy][xx]&&!seen[yy][xx]){seen[yy][xx]=true;q.add(new int[]{xx,yy});}}}list.add(c);}return list;}
  boolean[][] maskOf(int h,int w,Comp c){boolean[][] m=new boolean[h][w];for(int[] p:c.pts)m[p[1]][p[0]]=true;return m;}
  boolean[][] dilate(boolean[][] a,int r){int h=a.length,w=a[0].length;boolean[][] o=new boolean[h][w];for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(a[y][x])for(int yy=Math.max(0,y-r);yy<=Math.min(h-1,y+r);yy++)for(int xx=Math.max(0,x-r);xx<=Math.min(w-1,x+r);xx++)o[yy][xx]=true;return o;}
  boolean[][] fillInside(boolean[][] wall){int h=wall.length,w=wall[0].length;boolean[][] outside=new boolean[h][w];ArrayDeque<int[]> q=new ArrayDeque<>();for(int x=0;x<w;x++){seed(x,0,wall,outside,q);seed(x,h-1,wall,outside,q);}for(int y=0;y<h;y++){seed(0,y,wall,outside,q);seed(w-1,y,wall,outside,q);}int[] dx={1,-1,0,0},dy={0,0,1,-1};while(!q.isEmpty()){int[] p=q.removeFirst();for(int k=0;k<4;k++){int x=p[0]+dx[k],y=p[1]+dy[k];if(x>=0&&x<w&&y>=0&&y<h&&!wall[y][x]&&!outside[y][x]){outside[y][x]=true;q.add(new int[]{x,y});}}}boolean[][] f=new boolean[h][w];for(int y=0;y<h;y++)for(int x=0;x<w;x++)f[y][x]=wall[y][x]||!outside[y][x];return f;}
  void seed(int x,int y,boolean[][] wall,boolean[][] out,ArrayDeque<int[]> q){if(!wall[y][x]&&!out[y][x]){out[y][x]=true;q.add(new int[]{x,y});}}
  boolean[][] largestSolid(boolean[][] a){ArrayList<Comp> cs=components(a);if(cs.isEmpty())return a;Comp best=cs.get(0);for(Comp c:cs)if(c.size()>best.size())best=c;return maskOf(a.length,a[0].length,best);}
  boolean[][] majority(boolean[][] a){int h=a.length,w=a[0].length;boolean[][] o=new boolean[h][w];for(int y=0;y<h;y++)for(int x=0;x<w;x++){int n=localCount(a,x,y,1);o[y][x]=a[y][x]?(n>=3):(n>=7);}return o;}
  int count(boolean[][] a){int n=0;for(boolean[] r:a)for(boolean v:r)if(v)n++;return n;}

  void save(){if(solid==null){status.setText("Önce 3D model oluştur.");return;}pendingStl=toStl(solid,zHeight);Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("model/stl");i.putExtra(Intent.EXTRA_TITLE,"MG_Drawing2CAD_v14_Model.stl");startActivityForResult(i,SAVE);}
  String toStl(boolean[][] a,float z){StringBuilder s=new StringBuilder("solid MG_Drawing2CAD\n");int h=a.length,w=a[0].length;for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(a[y][x]){face(s,x,y,z,true);face(s,x,y,z,false);if(x==0||!a[y][x-1])sideX(s,x,y,z,false);if(x==w-1||!a[y][x+1])sideX(s,x+1,y,z,true);if(y==0||!a[y-1][x])sideY(s,x,y,z,false);if(y==h-1||!a[y+1][x])sideY(s,x,y+1,z,true);}return s.append("endsolid MG_Drawing2CAD\n").toString();}
  void tri(StringBuilder s,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz){s.append("facet normal 0 0 0\n outer loop\n");v(s,ax,ay,az);v(s,bx,by,bz);v(s,cx,cy,cz);s.append(" endloop\nendfacet\n");}
  void v(StringBuilder s,float x,float y,float z){s.append("  vertex ").append(x).append(' ').append(y).append(' ').append(z).append('\n');}
  void face(StringBuilder s,float x,float y,float z,boolean top){if(top){tri(s,x,y,z,x+1,y,z,x+1,y+1,z);tri(s,x,y,z,x+1,y+1,z,x,y+1,z);}else{tri(s,x,y,0,x+1,y+1,0,x+1,y,0);tri(s,x,y,0,x,y+1,0,x+1,y+1,0);}}
  void sideX(StringBuilder s,float x,float y,float z,boolean pos){if(pos){tri(s,x,y,0,x,y+1,0,x,y+1,z);tri(s,x,y,0,x,y+1,z,x,y,z);}else{tri(s,x,y,0,x,y+1,z,x,y+1,0);tri(s,x,y,0,x,y,z,x,y+1,z);}}
  void sideY(StringBuilder s,float x,float y,float z,boolean pos){if(pos){tri(s,x,y,0,x,y,z,x+1,y,z);tri(s,x,y,0,x+1,y,z,x+1,y,0);}else{tri(s,x,y,0,x+1,y,z,x,y,z);tri(s,x,y,0,x+1,y,0,x+1,y,z);}}

  static class MeshView extends View{
    Paint fill=new Paint(3),edge=new Paint(3),gridP=new Paint(1);Bitmap bmp;boolean[][] s;float yaw=.72f,pitch=.62f,scale=6f,lastX,lastY,lastD;boolean drag,grid=true;int z=8;
    MeshView(Context c){super(c);setBackgroundColor(Color.rgb(7,12,22));edge.setStyle(Paint.Style.STROKE);edge.setStrokeWidth(1);edge.setColor(Color.rgb(8,47,73));gridP.setColor(Color.rgb(18,34,55));}
    void setBitmap(Bitmap b){bmp=b;s=null;invalidate();}void setSolid(boolean[][] m,int zz){s=m;z=zz;bmp=null;fit();}void iso(){yaw=.72f;pitch=.62f;invalidate();}void top(){yaw=0;pitch=0;invalidate();}void fit(){if(s!=null)scale=Math.max(2f,Math.min(9f,Math.min(getWidth()/Math.max(1f,s[0].length*1.45f),getHeight()/Math.max(1f,s.length*1.45f))));invalidate();}void toggleGrid(){grid=!grid;invalidate();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);if(grid)drawGrid(c);if(s==null){if(bmp!=null){float k=Math.min((getWidth()-30f)/bmp.getWidth(),(getHeight()-30f)/bmp.getHeight());float ww=bmp.getWidth()*k,hh=bmp.getHeight()*k;RectF r=new RectF((getWidth()-ww)/2,(getHeight()-hh)/2,(getWidth()+ww)/2,(getHeight()+hh)/2);c.drawBitmap(bmp,null,r,fill);}return;}c.save();c.translate(getWidth()/2f,getHeight()/2f);ArrayList<Cell> cells=new ArrayList<>();for(int y=0;y<s.length;y++)for(int x=0;x<s[0].length;x++)if(s[y][x])cells.add(new Cell(x-s[0].length/2f,y-s.length/2f));Collections.sort(cells,(a,b)->Float.compare(a.x+a.y,b.x+b.y));for(Cell q:cells)cube(c,q.x,q.y);c.restore();}
    void drawGrid(Canvas c){for(int x=0;x<getWidth();x+=24)c.drawLine(x,0,x,getHeight(),gridP);for(int y=0;y<getHeight();y+=24)c.drawLine(0,y,getWidth(),y,gridP);}
    void cube(Canvas c,float x,float y){float h3=Math.max(1f,z/5f);float[][] v={{x,y,0},{x+1,y,0},{x+1,y+1,0},{x,y+1,0},{x,y,h3},{x+1,y,h3},{x+1,y+1,h3},{x,y+1,h3}};PointF[] q=new PointF[8];for(int i=0;i<8;i++)q[i]=proj(v[i][0],v[i][1],v[i][2]);poly(c,q,new int[]{4,5,6,7},Color.rgb(103,232,249));poly(c,q,new int[]{1,2,6,5},Color.rgb(34,211,238));poly(c,q,new int[]{2,3,7,6},Color.rgb(14,165,233));}
    PointF proj(float x,float y,float z){float cy=(float)Math.cos(yaw),sy=(float)Math.sin(yaw),cp=(float)Math.cos(pitch),sp=(float)Math.sin(pitch);float X=x*cy-y*sy,Y=x*sy+y*cy;float Y2=Y*cp-z*sp;return new PointF(X*scale,Y2*scale);}
    void poly(Canvas c,PointF[] q,int[] id,int col){Path p=new Path();p.moveTo(q[id[0]].x,q[id[0]].y);for(int i=1;i<id.length;i++)p.lineTo(q[id[i]].x,q[id[i]].y);p.close();fill.setStyle(Paint.Style.FILL);fill.setColor(col);c.drawPath(p,fill);c.drawPath(p,edge);}
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getPointerCount()==2){float d=(float)Math.hypot(e.getX(0)-e.getX(1),e.getY(0)-e.getY(1));if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&lastD>0){scale*=d/lastD;scale=Math.max(2,Math.min(18,scale));invalidate();}lastD=d;return true;}lastD=0;if(e.getAction()==MotionEvent.ACTION_DOWN){lastX=e.getX();lastY=e.getY();drag=true;return true;}if(e.getAction()==MotionEvent.ACTION_MOVE&&drag){yaw+=(e.getX()-lastX)*.01f;pitch+=(e.getY()-lastY)*.01f;pitch=Math.max(-1.25f,Math.min(1.25f,pitch));lastX=e.getX();lastY=e.getY();invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_UP)drag=false;return true;}
    static class Cell{float x,y;Cell(float a,float b){x=a;y=b;}}
  }
}
