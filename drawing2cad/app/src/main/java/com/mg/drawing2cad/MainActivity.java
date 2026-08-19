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
  Bitmap source; VoxelView view; TextView status,fileInfo; Uri archiveUri; ArrayList<String> entries=new ArrayList<>(); boolean[][][] voxels; String pendingStl;

  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);buildUi();}

  void buildUi(){
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
    LinearLayout head=new LinearLayout(this);head.setPadding(dp(16),dp(10),dp(12),dp(10));head.setGravity(Gravity.CENTER_VERTICAL);head.setBackgroundColor(PANEL);
    LinearLayout tt=new LinearLayout(this);tt.setOrientation(LinearLayout.VERTICAL);tt.addView(txt("MG Drawing2CAD",21,TEXT,true));tt.addView(txt("MULTI-VIEW CAD • v1.5",10,MUTED,false));head.addView(tt,new LinearLayout.LayoutParams(0,-2,1));head.addView(txt("● FRONT + TOP + RIGHT",9,ACC,true));root.addView(head);

    HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);LinearLayout bar=new LinearLayout(this);bar.setPadding(dp(8),dp(7),dp(8),dp(7));
    Button open=tool("⌂ AÇ"), make=tool("◈ 3D OLUŞTUR"), save=tool("⇩ STL"), iso=tool("◇ ISO"), front=tool("FRONT"), top=tool("TOP"), right=tool("RIGHT"), fit=tool("⊙ SIĞDIR");
    for(Button x:new Button[]{open,make,save,iso,front,top,right,fit})bar.addView(x);hs.addView(bar);root.addView(hs);

    fileInfo=txt("DOSYA: — | Tek sayfa: FRONT sol-üst • RIGHT sağ-üst • TOP sol-alt • ISO sağ-alt",10,MUTED,false);fileInfo.setPadding(dp(12),dp(7),dp(12),dp(7));fileInfo.setBackgroundColor(PANEL2);root.addView(fileInfo);
    FrameLayout stage=new FrameLayout(this);view=new VoxelView(this);stage.addView(view,new FrameLayout.LayoutParams(-1,-1));root.addView(stage,new LinearLayout.LayoutParams(-1,0,1));
    status=txt("Ölçülendirilmiş çok görünüşlü teknik resmi aç. Siyah geometri okunur; açık gri ölçü/eksen çizgileri filtrelenir.",10,MUTED,false);status.setPadding(dp(12),dp(9),dp(12),dp(10));status.setBackgroundColor(PANEL);root.addView(status);setContentView(root);

    open.setOnClickListener(v->pick());make.setOnClickListener(v->make3d());save.setOnClickListener(v->save());iso.setOnClickListener(v->view.iso());front.setOnClickListener(v->view.front());top.setOnClickListener(v->view.top());right.setOnClickListener(v->view.right());fit.setOnClickListener(v->view.fit());
  }

  TextView txt(String s,int z,int c,boolean b){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);if(b)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
  Button tool(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(9);b.setAllCaps(false);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(11),dp(7),dp(11),dp(7));GradientDrawable g=new GradientDrawable();g.setColor(PANEL2);g.setCornerRadius(dp(8));g.setStroke(1,Color.rgb(51,65,85));b.setBackground(g);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(40));lp.setMargins(dp(4),0,dp(4),0);b.setLayoutParams(lp);return b;}
  int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}

  void pick(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","application/pdf","application/zip","application/x-zip-compressed","application/octet-stream"});startActivityForResult(i,PICK);}
  @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(c!=RESULT_OK||d==null)return;try{if(r==PICK){Uri u=d.getData();String n=fileName(u),l=n.toLowerCase(Locale.ROOT);if(l.endsWith(".zip")||l.endsWith(".cbz")||l.endsWith(".jar")){archiveUri=u;listArchive(u);}else if(l.endsWith(".pdf")){source=renderPdf(u);loaded(n,"PDF");}else{try(InputStream in=getContentResolver().openInputStream(u)){source=BitmapFactory.decodeStream(in);}loaded(n,"IMAGE");}}else if(r==SAVE&&pendingStl!=null){try(OutputStream o=getContentResolver().openOutputStream(d.getData())){o.write(pendingStl.getBytes("UTF-8"));}status.setText("✓ STL kaydedildi.");}}catch(Exception e){status.setText("Hata: "+e.getMessage());}}
  void loaded(String n,String type)throws Exception{if(source==null)throw new IOException("Dosya görüntüye çevrilemedi");voxels=null;view.setBitmap(source);fileInfo.setText("DOSYA: "+n+" | "+type+" | 4 görünüş düzeni");status.setText("✓ Sayfa yüklendi. FRONT/TOP/RIGHT görünüşlerinden 3D hacim için 3D OLUŞTUR'a bas.");}
  String fileName(Uri u){String n="dosya";Cursor c=null;try{c=getContentResolver().query(u,null,null,null,null);if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)n=c.getString(i);}}catch(Exception ignored){}finally{if(c!=null)c.close();}return n==null?"dosya":n;}

  void listArchive(Uri u)throws Exception{entries.clear();try(InputStream in=getContentResolver().openInputStream(u);ZipInputStream z=new ZipInputStream(new BufferedInputStream(in))){ZipEntry e;while((e=z.getNextEntry())!=null){if(e.isDirectory())continue;String l=e.getName().toLowerCase(Locale.ROOT);if(l.endsWith(".pdf")||l.endsWith(".png")||l.endsWith(".jpg")||l.endsWith(".jpeg")||l.endsWith(".webp"))entries.add(e.getName());}}if(entries.isEmpty())throw new IOException("ZIP içinde desteklenen çizim yok");String[] a=entries.toArray(new String[0]);new AlertDialog.Builder(this).setTitle("ZIP içinden teknik resim seç").setItems(a,(q,w)->{try{loadEntry(a[w]);}catch(Exception ex){status.setText("ZIP hatası: "+ex.getMessage());}}).setNegativeButton("İptal",null).show();}
  void loadEntry(String wanted)throws Exception{File f=new File(getCacheDir(),"d2c15_"+Math.abs(wanted.hashCode())+(wanted.toLowerCase(Locale.ROOT).endsWith(".pdf")?".pdf":".img"));boolean found=false;try(InputStream in=getContentResolver().openInputStream(archiveUri);ZipInputStream z=new ZipInputStream(new BufferedInputStream(in))){ZipEntry e;while((e=z.getNextEntry())!=null)if(e.getName().equals(wanted)){try(FileOutputStream o=new FileOutputStream(f)){byte[] b=new byte[16384];int n;long total=0;while((n=z.read(b))>0){total+=n;if(total>80L*1024*1024)throw new IOException("ZIP girdisi çok büyük");o.write(b,0,n);}}found=true;break;}}if(!found)throw new IOException("ZIP girdisi bulunamadı");if(wanted.toLowerCase(Locale.ROOT).endsWith(".pdf"))source=renderPdfFile(f);else try(InputStream in=new FileInputStream(f)){source=BitmapFactory.decodeStream(in);}loaded(wanted,"ZIP");}
  Bitmap renderPdf(Uri u)throws Exception{File f=new File(getCacheDir(),"mv.pdf");try(InputStream in=getContentResolver().openInputStream(u);FileOutputStream o=new FileOutputStream(f)){byte[] b=new byte[16384];int n;while((n=in.read(b))>0)o.write(b,0,n);}return renderPdfFile(f);}
  Bitmap renderPdfFile(File f)throws Exception{ParcelFileDescriptor pfd=ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer r=new PdfRenderer(pfd);if(r.getPageCount()<1)throw new IOException("PDF boş");PdfRenderer.Page p=r.openPage(0);float k=Math.min(2.8f,2200f/Math.max(p.getWidth(),p.getHeight()));Bitmap b=Bitmap.createBitmap(Math.max(1,(int)(p.getWidth()*k)),Math.max(1,(int)(p.getHeight()*k)),Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);p.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);p.close();r.close();pfd.close();return b;}

  void make3d(){if(source==null){status.setText("Önce teknik resim aç.");return;}status.setText("3 görünüş ayrılıyor • ölçü çizgileri filtreleniyor • silüetler eşleştiriliyor • 3D visual-hull hesaplanıyor...");new Thread(()->{try{MVResult r=reconstruct(source);voxels=r.v;runOnUiThread(()->{view.setVoxels(r.v);view.iso();status.setText("✓ MULTI-VIEW 3D HAZIR • "+r.nx+"×"+r.ny+"×"+r.nz+" voxel • "+count3(r.v)+" dolu hücre");});}catch(Exception e){runOnUiThread(()->status.setText("Analiz hatası: "+e.getMessage()));}}).start();}

  static class MVResult{boolean[][][] v;int nx,ny,nz;MVResult(boolean[][][] a,int x,int y,int z){v=a;nx=x;ny=y;nz=z;}}
  static class Box{int x0,y0,x1,y1;Box(int a,int b,int c,int d){x0=a;y0=b;x1=c;y1=d;}int w(){return x1-x0+1;}int h(){return y1-y0+1;}}

  MVResult reconstruct(Bitmap bm)throws Exception{
    int W=bm.getWidth(),H=bm.getHeight();int topCut=(int)(H*.08f),bottomCut=(int)(H*.08f),usableH=H-topCut-bottomCut,midY=topCut+usableH/2,midX=W/2;
    Bitmap front=Bitmap.createBitmap(bm,0,topCut,midX,usableH/2);
    Bitmap right=Bitmap.createBitmap(bm,midX,topCut,W-midX,usableH/2);
    Bitmap top=Bitmap.createBitmap(bm,0,midY,midX,H-bottomCut-midY);
    Box bf=findGeometry(front), br=findGeometry(right), bt=findGeometry(top);
    if(bf==null||br==null||bt==null)throw new IOException("FRONT / RIGHT / TOP ana geometrilerinden biri bulunamadı. 4 görünüş sayfa düzenini kullan.");
    int nx=60;int nz=Math.max(16,Math.min(52,Math.round(nx*(bf.h()/(float)bf.w()))));int ny=Math.max(16,Math.min(48,Math.round(nx*(bt.h()/(float)bt.w()))));
    boolean[][] fm=profileMask(front,bf,nx,nz,true);
    boolean[][] tm=profileMask(top,bt,nx,ny,true);
    boolean[][] rm=profileMask(right,br,ny,nz,false);
    boolean[][][] v=new boolean[nx][ny][nz];
    for(int x=0;x<nx;x++)for(int y=0;y<ny;y++)for(int z=0;z<nz;z++)v[x][y][z]=fm[nz-1-z][x]&&tm[ny-1-y][x]&&rm[nz-1-z][y];
    v=largest3(v);if(count3(v)<80)throw new IOException("3 görünüş birbiriyle yeterince kesişmedi. Görünüş ölçeklerini ve hizalamayı kontrol et.");return new MVResult(v,nx,ny,nz);
  }

  Box findGeometry(Bitmap b){int w=b.getWidth(),h=b.getHeight();int minX=w,minY=h,maxX=-1,maxY=-1;int step=Math.max(1,Math.max(w,h)/1200);for(int y=0;y<h;y+=step)for(int x=0;x<w;x+=step){if(gray(b.getPixel(x,y))<95){minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);}}if(maxX<0)return null;int mx=Math.max(4,(maxX-minX)/80),my=Math.max(4,(maxY-minY)/80);return new Box(Math.max(0,minX-mx),Math.max(0,minY-my),Math.min(w-1,maxX+mx),Math.min(h-1,maxY+my));}

  boolean[][] profileMask(Bitmap src,Box b,int outW,int outH,boolean subtractSmallHoles){Bitmap crop=Bitmap.createBitmap(src,b.x0,b.y0,b.w(),b.h());Bitmap s=Bitmap.createScaledBitmap(crop,outW,outH,true);boolean[][] wall=new boolean[outH][outW];for(int y=0;y<outH;y++)for(int x=0;x<outW;x++)wall[y][x]=gray(s.getPixel(x,y))<115;wall=dilate(wall,1);boolean[][] outside=floodOutside(wall);boolean[][] solid=new boolean[outH][outW];for(int y=0;y<outH;y++)for(int x=0;x<outW;x++)solid[y][x]=wall[y][x]||!outside[y][x];if(subtractSmallHoles){boolean[][] seen=new boolean[outH][outW];int maxHole=Math.max(12,(outW*outH)/22);for(int y=0;y<outH;y++)for(int x=0;x<outW;x++)if(!wall[y][x]&&!outside[y][x]&&!seen[y][x]){ArrayList<int[]> pts=new ArrayList<>();ArrayDeque<int[]> q=new ArrayDeque<>();q.add(new int[]{x,y});seen[y][x]=true;while(!q.isEmpty()){int[] p=q.removeFirst();pts.add(p);int[][] d={{1,0},{-1,0},{0,1},{0,-1}};for(int[] dd:d){int xx=p[0]+dd[0],yy=p[1]+dd[1];if(xx>=0&&xx<outW&&yy>=0&&yy<outH&&!wall[yy][xx]&&!outside[yy][xx]&&!seen[yy][xx]){seen[yy][xx]=true;q.add(new int[]{xx,yy});}}}if(pts.size()>=5&&pts.size()<=maxHole)for(int[] p:pts)solid[p[1]][p[0]]=false;}}
    return solid;}

  int gray(int c){return (Color.red(c)*30+Color.green(c)*59+Color.blue(c)*11)/100;}
  boolean[][] dilate(boolean[][] a,int r){int h=a.length,w=a[0].length;boolean[][] o=new boolean[h][w];for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(a[y][x])for(int yy=Math.max(0,y-r);yy<=Math.min(h-1,y+r);yy++)for(int xx=Math.max(0,x-r);xx<=Math.min(w-1,x+r);xx++)o[yy][xx]=true;return o;}
  boolean[][] floodOutside(boolean[][] wall){int h=wall.length,w=wall[0].length;boolean[][] o=new boolean[h][w];ArrayDeque<int[]> q=new ArrayDeque<>();for(int x=0;x<w;x++){seed2(x,0,wall,o,q);seed2(x,h-1,wall,o,q);}for(int y=0;y<h;y++){seed2(0,y,wall,o,q);seed2(w-1,y,wall,o,q);}int[][] d={{1,0},{-1,0},{0,1},{0,-1}};while(!q.isEmpty()){int[] p=q.removeFirst();for(int[] dd:d){int x=p[0]+dd[0],y=p[1]+dd[1];if(x>=0&&x<w&&y>=0&&y<h&&!wall[y][x]&&!o[y][x]){o[y][x]=true;q.add(new int[]{x,y});}}}return o;}
  void seed2(int x,int y,boolean[][] w,boolean[][] o,ArrayDeque<int[]> q){if(!w[y][x]&&!o[y][x]){o[y][x]=true;q.add(new int[]{x,y});}}

  boolean[][][] largest3(boolean[][][] a){int nx=a.length,ny=a[0].length,nz=a[0][0].length;boolean[][][] seen=new boolean[nx][ny][nz],best=null;int bestN=0;int[][] d={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};for(int x=0;x<nx;x++)for(int y=0;y<ny;y++)for(int z=0;z<nz;z++)if(a[x][y][z]&&!seen[x][y][z]){ArrayList<int[]> pts=new ArrayList<>();ArrayDeque<int[]> q=new ArrayDeque<>();q.add(new int[]{x,y,z});seen[x][y][z]=true;while(!q.isEmpty()){int[] p=q.removeFirst();pts.add(p);for(int[] dd:d){int xx=p[0]+dd[0],yy=p[1]+dd[1],zz=p[2]+dd[2];if(xx>=0&&xx<nx&&yy>=0&&yy<ny&&zz>=0&&zz<nz&&a[xx][yy][zz]&&!seen[xx][yy][zz]){seen[xx][yy][zz]=true;q.add(new int[]{xx,yy,zz});}}}if(pts.size()>bestN){bestN=pts.size();best=new boolean[nx][ny][nz];for(int[] p:pts)best[p[0]][p[1]][p[2]]=true;}}return best==null?a:best;}
  int count3(boolean[][][] a){if(a==null)return 0;int n=0;for(int x=0;x<a.length;x++)for(int y=0;y<a[0].length;y++)for(int z=0;z<a[0][0].length;z++)if(a[x][y][z])n++;return n;}

  void save(){if(voxels==null){status.setText("Önce 3D model oluştur.");return;}pendingStl=toStl(voxels);Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("model/stl");i.putExtra(Intent.EXTRA_TITLE,"MG_Drawing2CAD_MultiView_v15.stl");startActivityForResult(i,SAVE);}
  String toStl(boolean[][][] a){StringBuilder s=new StringBuilder("solid MG_Drawing2CAD_MultiView\n");int nx=a.length,ny=a[0].length,nz=a[0][0].length;for(int x=0;x<nx;x++)for(int y=0;y<ny;y++)for(int z=0;z<nz;z++)if(a[x][y][z]){if(x==0||!a[x-1][y][z])quad(s,x,y,z,x,y+1,z,x,y+1,z+1,x,y,z+1);if(x==nx-1||!a[x+1][y][z])quad(s,x+1,y,z,x+1,y,z+1,x+1,y+1,z+1,x+1,y+1,z);if(y==0||!a[x][y-1][z])quad(s,x,y,z,x,y,z+1,x+1,y,z+1,x+1,y,z);if(y==ny-1||!a[x][y+1][z])quad(s,x,y+1,z,x+1,y+1,z,x+1,y+1,z+1,x,y+1,z+1);if(z==0||!a[x][y][z-1])quad(s,x,y,z,x+1,y,z,x+1,y+1,z,x,y+1,z);if(z==nz-1||!a[x][y][z+1])quad(s,x,y,z+1,x,y+1,z+1,x+1,y+1,z+1,x+1,y,z+1);}return s.append("endsolid MG_Drawing2CAD_MultiView\n").toString();}
  void quad(StringBuilder s,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float dx,float dy,float dz){tri(s,ax,ay,az,bx,by,bz,cx,cy,cz);tri(s,ax,ay,az,cx,cy,cz,dx,dy,dz);}
  void tri(StringBuilder s,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz){s.append("facet normal 0 0 0\n outer loop\n");vtx(s,ax,ay,az);vtx(s,bx,by,bz);vtx(s,cx,cy,cz);s.append(" endloop\nendfacet\n");}
  void vtx(StringBuilder s,float x,float y,float z){s.append("  vertex ").append(x).append(' ').append(y).append(' ').append(z).append('\n');}

  class VoxelView extends View{
    Paint p=new Paint(3),edge=new Paint(3),grid=new Paint(3);Bitmap bm;boolean[][][] v;float scale=7f;int mode=0;
    VoxelView(Context c){super(c);p.setColor(Color.rgb(74,197,230));edge.setStyle(Paint.Style.STROKE);edge.setStrokeWidth(1);edge.setColor(Color.rgb(8,73,91));grid.setColor(Color.rgb(16,31,49));grid.setStrokeWidth(1);setBackgroundColor(BG);}
    void setBitmap(Bitmap b){bm=b;v=null;invalidate();}void setVoxels(boolean[][][] a){v=a;bm=null;fit();}
    void iso(){mode=0;invalidate();}void front(){mode=1;invalidate();}void top(){mode=2;invalidate();}void right(){mode=3;invalidate();}void fit(){if(v!=null){int m=Math.max(v.length,Math.max(v[0].length,v[0][0].length));scale=Math.max(2.2f,Math.min(10f,Math.min(getWidth(),getHeight())/(float)(m*2.1f)));}invalidate();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);for(int x=0;x<getWidth();x+=dp(24))c.drawLine(x,0,x,getHeight(),grid);for(int y=0;y<getHeight();y+=dp(24))c.drawLine(0,y,getWidth(),y,grid);if(bm!=null){RectF dst=fitRect(bm,getWidth(),getHeight());c.drawBitmap(bm,null,dst,p);return;}if(v==null)return;if(mode==1)drawFront(c);else if(mode==2)drawTop(c);else if(mode==3)drawRight(c);else drawIso(c);}
    RectF fitRect(Bitmap b,int W,int H){float k=Math.min(W/(float)b.getWidth(),H/(float)b.getHeight());float w=b.getWidth()*k,h=b.getHeight()*k;return new RectF((W-w)/2,(H-h)/2,(W+w)/2,(H+h)/2);}
    void drawFront(Canvas c){int nx=v.length,ny=v[0].length,nz=v[0][0].length;float s=Math.min(getWidth()/(nx+4f),getHeight()/(nz+4f)),ox=(getWidth()-nx*s)/2,oy=(getHeight()-nz*s)/2;p.setColor(Color.rgb(64,190,224));for(int x=0;x<nx;x++)for(int z=0;z<nz;z++){boolean any=false;for(int y=0;y<ny;y++)if(v[x][y][z]){any=true;break;}if(any)c.drawRect(ox+x*s,oy+(nz-1-z)*s,ox+(x+1)*s,oy+(nz-z)*s,p);}}
    void drawTop(Canvas c){int nx=v.length,ny=v[0].length,nz=v[0][0].length;float s=Math.min(getWidth()/(nx+4f),getHeight()/(ny+4f)),ox=(getWidth()-nx*s)/2,oy=(getHeight()-ny*s)/2;p.setColor(Color.rgb(64,190,224));for(int x=0;x<nx;x++)for(int y=0;y<ny;y++){boolean any=false;for(int z=0;z<nz;z++)if(v[x][y][z]){any=true;break;}if(any)c.drawRect(ox+x*s,oy+(ny-1-y)*s,ox+(x+1)*s,oy+(ny-y)*s,p);}}
    void drawRight(Canvas c){int nx=v.length,ny=v[0].length,nz=v[0][0].length;float s=Math.min(getWidth()/(ny+4f),getHeight()/(nz+4f)),ox=(getWidth()-ny*s)/2,oy=(getHeight()-nz*s)/2;p.setColor(Color.rgb(64,190,224));for(int y=0;y<ny;y++)for(int z=0;z<nz;z++){boolean any=false;for(int x=0;x<nx;x++)if(v[x][y][z]){any=true;break;}if(any)c.drawRect(ox+y*s,oy+(nz-1-z)*s,ox+(y+1)*s,oy+(nz-z)*s,p);}}
    void drawIso(Canvas c){int nx=v.length,ny=v[0].length,nz=v[0][0].length;float sx=scale,sy=scale*.52f,sz=scale;float ox=getWidth()/2f,oy=getHeight()*.70f;for(int z=0;z<nz;z++)for(int y=ny-1;y>=0;y--)for(int x=0;x<nx;x++)if(v[x][y][z]){float px=ox+(x-y)*sx*.86f,py=oy-(x+y)*sy-z*sz;if(z==nz-1||!v[x][y][z+1])faceTop(c,px,py,sx,sy,sz);if(x==nx-1||!v[x+1][y][z])faceR(c,px,py,sx,sy,sz);if(y==0||!v[x][y-1][z])faceL(c,px,py,sx,sy,sz);}}
    void poly(Canvas c,float[] xs,float[] ys,int col){Path path=new Path();path.moveTo(xs[0],ys[0]);for(int i=1;i<xs.length;i++)path.lineTo(xs[i],ys[i]);path.close();p.setColor(col);c.drawPath(path,p);c.drawPath(path,edge);}
    void faceTop(Canvas c,float x,float y,float sx,float sy,float sz){poly(c,new float[]{x,x+sx*.86f,x,x-sx*.86f},new float[]{y-sz,y-sz-sy,y-sz-2*sy,y-sz-sy},Color.rgb(96,222,244));}
    void faceR(Canvas c,float x,float y,float sx,float sy,float sz){poly(c,new float[]{x,x+sx*.86f,x+sx*.86f,x},new float[]{y,y-sy,y-sy-sz,y-sz},Color.rgb(49,176,211));}
    void faceL(Canvas c,float x,float y,float sx,float sy,float sz){poly(c,new float[]{x,x-sx*.86f,x-sx*.86f,x},new float[]{y,y-sy,y-sy-sz,y-sz},Color.rgb(37,135,170));}
  }
}
