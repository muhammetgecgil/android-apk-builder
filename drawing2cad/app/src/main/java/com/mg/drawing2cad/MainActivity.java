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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {
    static final int PICK=10, SAVE=11;
    Bitmap source;
    boolean[][] solid;
    MeshView mesh;
    TextView status, fileInfo;
    String pendingStl;
    Uri archiveUri;
    ArrayList<String> archiveEntries=new ArrayList<>();

    final int BG=Color.rgb(9,14,24), PANEL=Color.rgb(15,23,42), PANEL2=Color.rgb(20,30,50), LINE=Color.rgb(51,65,85), TEXT=Color.rgb(226,232,240), MUTED=Color.rgb(148,163,184), ACCENT=Color.rgb(34,211,238);

    @Override public void onCreate(Bundle b){ super.onCreate(b); getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG); buildUi(); }

    void buildUi(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);

        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(16),dp(10),dp(12),dp(10)); top.setBackgroundColor(PANEL);
        LinearLayout titles=new LinearLayout(this); titles.setOrientation(LinearLayout.VERTICAL);
        TextView title=txt("MG Drawing2CAD",20,TEXT,true); TextView sub=txt("MOBILE CAD WORKSPACE  •  v1.1",10,MUTED,false); titles.addView(title); titles.addView(sub);
        top.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView mode=txt("● 3D READY",10,ACCENT,true); mode.setPadding(dp(10),dp(6),dp(10),dp(6)); mode.setBackground(round(Color.rgb(13,38,50),dp(12))); top.addView(mode);
        root.addView(top);

        HorizontalScrollView hsv=new HorizontalScrollView(this); hsv.setHorizontalScrollBarEnabled(false); hsv.setBackgroundColor(Color.rgb(11,18,32));
        LinearLayout bar=new LinearLayout(this); bar.setPadding(dp(8),dp(7),dp(8),dp(7));
        Button open=tool("⌂  AÇ"); Button make=tool("◈  3D OLUŞTUR"); Button save=tool("⇩  STL"); Button iso=tool("◇  ISO"); Button topv=tool("▱  ÜST"); Button fit=tool("⊙  SIĞDIR");
        for(Button b:new Button[]{open,make,save,iso,topv,fit}) bar.addView(b);
        hsv.addView(bar); root.addView(hsv);

        fileInfo=txt("DOSYA: —     |     ZIP • PDF • PNG • JPG destekli",11,MUTED,false); fileInfo.setPadding(dp(14),dp(7),dp(14),dp(7)); fileInfo.setBackgroundColor(PANEL2); root.addView(fileInfo);

        FrameLayout stage=new FrameLayout(this); stage.setBackgroundColor(BG);
        mesh=new MeshView(this); stage.addView(mesh,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout rail=new LinearLayout(this); rail.setOrientation(LinearLayout.VERTICAL); rail.setPadding(dp(8),dp(10),dp(8),dp(10)); rail.setBackgroundColor(Color.argb(220,15,23,42));
        TextView tools=txt("VIEW",9,MUTED,true); tools.setGravity(Gravity.CENTER); rail.addView(tools,new LinearLayout.LayoutParams(dp(50),dp(28)));
        Button rIso=mini("ISO"), rTop=mini("TOP"), rFit=mini("FIT"), rGrid=mini("GRID");
        rail.addView(rIso); rail.addView(rTop); rail.addView(rFit); rail.addView(rGrid);
        FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(dp(66),-2,Gravity.LEFT|Gravity.TOP); rp.leftMargin=dp(8); rp.topMargin=dp(10); stage.addView(rail,rp);
        root.addView(stage,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout bottom=new LinearLayout(this); bottom.setGravity(Gravity.CENTER_VERTICAL); bottom.setPadding(dp(12),dp(8),dp(12),dp(9)); bottom.setBackgroundColor(PANEL);
        status=txt("Bir teknik resim veya ZIP paket aç. ZIP içindeki PDF/JPG/PNG dosyalarını seçebilirsin.",11,MUTED,false); bottom.addView(status,new LinearLayout.LayoutParams(0,-2,1));
        TextView hint=txt("DRAG: ORBIT   PINCH: ZOOM",9,MUTED,true); bottom.addView(hint); root.addView(bottom);
        setContentView(root);

        open.setOnClickListener(v->pick()); make.setOnClickListener(v->make3d()); save.setOnClickListener(v->save());
        iso.setOnClickListener(v->mesh.iso()); topv.setOnClickListener(v->mesh.top()); fit.setOnClickListener(v->mesh.fit());
        rIso.setOnClickListener(v->mesh.iso()); rTop.setOnClickListener(v->mesh.top()); rFit.setOnClickListener(v->mesh.fit()); rGrid.setOnClickListener(v->mesh.toggleGrid());
    }

    TextView txt(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    Button tool(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(10);b.setAllCaps(false);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(12),dp(8),dp(12),dp(8));b.setBackground(round(PANEL2,dp(8)));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(40));lp.setMargins(dp(4),0,dp(4),0);b.setLayoutParams(lp);return b;}
    Button mini(String s){Button b=new Button(this);b.setText(s);b.setTextSize(9);b.setTextColor(TEXT);b.setAllCaps(false);b.setMinHeight(0);b.setMinimumHeight(0);b.setBackground(round(Color.rgb(30,41,59),dp(7)));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(50),dp(38));lp.setMargins(0,dp(3),0,dp(3));b.setLayoutParams(lp);return b;}
    GradientDrawable round(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(r);g.setStroke(1,LINE);return g;}
    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}

    void pick(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*"); i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","application/pdf","application/zip","application/x-zip-compressed","application/octet-stream"}); startActivityForResult(i,PICK); }

    @Override protected void onActivityResult(int r,int c,Intent d){ super.onActivityResult(r,c,d); if(c!=RESULT_OK||d==null)return; try{
        if(r==PICK){ Uri u=d.getData(); String name=fileName(u); String low=name.toLowerCase(Locale.ROOT); fileInfo.setText("DOSYA: "+name);
            if(low.endsWith(".zip")||low.endsWith(".cbz")||low.endsWith(".jar")){ archiveUri=u; listArchive(u); }
            else if(low.endsWith(".pdf")){ source=renderPdf(u); loaded(name,"PDF"); }
            else { try(InputStream in=getContentResolver().openInputStream(u)){ source=BitmapFactory.decodeStream(in); } if(source==null)throw new IOException("Görüntü açılamadı"); loaded(name,"IMAGE"); }
        } else if(r==SAVE && pendingStl!=null){ try(OutputStream o=getContentResolver().openOutputStream(d.getData())){ o.write(pendingStl.getBytes("UTF-8")); } status.setText("✓ STL kaydedildi."); }
    }catch(Exception e){ status.setText("Hata: "+e.getMessage()); }}

    void loaded(String name,String type)throws IOException{if(source==null)throw new IOException("Dosya görüntüye çevrilemedi");solid=null;mesh.setBitmap(source);status.setText("✓ "+type+" yüklendi • "+source.getWidth()+"×"+source.getHeight()+" • 3D OLUŞTUR'a bas.");fileInfo.setText("DOSYA: "+name+"     |     "+type);}

    String fileName(Uri u){String n="dosya";Cursor c=null;try{c=getContentResolver().query(u,null,null,null,null);if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)n=c.getString(i);}}catch(Exception ignored){}finally{if(c!=null)c.close();}return n==null?"dosya":n;}

    void listArchive(Uri u)throws Exception{archiveEntries.clear();try(InputStream in=getContentResolver().openInputStream(u);ZipInputStream z=new ZipInputStream(new BufferedInputStream(in))){ZipEntry e;while((e=z.getNextEntry())!=null){if(e.isDirectory())continue;String n=e.getName();String l=n.toLowerCase(Locale.ROOT);if(l.endsWith(".pdf")||l.endsWith(".png")||l.endsWith(".jpg")||l.endsWith(".jpeg")||l.endsWith(".webp"))archiveEntries.add(n);}}
        if(archiveEntries.isEmpty())throw new IOException("ZIP içinde desteklenen teknik resim bulunamadı");
        status.setText("ZIP açıldı • "+archiveEntries.size()+" desteklenen dosya bulundu.");
        String[] arr=archiveEntries.toArray(new String[0]);new AlertDialog.Builder(this).setTitle("ZIP içinden teknik resim seç").setItems(arr,(dlg,which)->{try{loadArchiveEntry(arr[which]);}catch(Exception ex){status.setText("ZIP okuma hatası: "+ex.getMessage());}}).setNegativeButton("İptal",null).show();
    }

    void loadArchiveEntry(String wanted)throws Exception{File f=new File(getCacheDir(),"drawing2cad_import_"+Math.abs(wanted.hashCode())+(wanted.toLowerCase(Locale.ROOT).endsWith(".pdf")?".pdf":".img"));
        try(InputStream in=getContentResolver().openInputStream(archiveUri);ZipInputStream z=new ZipInputStream(new BufferedInputStream(in))){ZipEntry e;boolean ok=false;while((e=z.getNextEntry())!=null){if(e.getName().equals(wanted)){try(FileOutputStream o=new FileOutputStream(f)){byte[] b=new byte[16384];int n;long total=0;while((n=z.read(b))>0){total+=n;if(total>60L*1024*1024)throw new IOException("ZIP girdisi 60 MB sınırını aşıyor");o.write(b,0,n);} }ok=true;break;}}if(!ok)throw new IOException("ZIP girdisi bulunamadı");}
        if(wanted.toLowerCase(Locale.ROOT).endsWith(".pdf"))source=renderPdfFile(f);else try(InputStream in=new FileInputStream(f)){source=BitmapFactory.decodeStream(in);} loaded(wanted,"ZIP ENTRY");
    }

    Bitmap renderPdf(Uri u)throws Exception{File f=new File(getCacheDir(),"drawing2cad_direct.pdf");try(InputStream in=getContentResolver().openInputStream(u);FileOutputStream o=new FileOutputStream(f)){byte[] b=new byte[16384];int n;while((n=in.read(b))>0)o.write(b,0,n);}return renderPdfFile(f);}
    Bitmap renderPdfFile(File f)throws Exception{ParcelFileDescriptor pfd=ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer r=new PdfRenderer(pfd);if(r.getPageCount()<1){r.close();pfd.close();throw new IOException("PDF boş");}PdfRenderer.Page p=r.openPage(0);float k=Math.min(2.2f,1800f/Math.max(p.getWidth(),p.getHeight()));int w=Math.max(1,(int)(p.getWidth()*k)),h=Math.max(1,(int)(p.getHeight()*k));Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);p.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);p.close();r.close();pfd.close();return b;}

    void make3d(){ if(source==null){status.setText("Önce teknik resim veya ZIP içinden dosya aç.");return;} status.setText("Kontur analizi ve 3D katı oluşturma...");new Thread(()->{ try{ boolean[][] s=analyze(source,60,60); solid=s; int n=count(s); runOnUiThread(()->{mesh.setSolid(s);mesh.iso();status.setText("✓ 3D MODEL HAZIR • "+n+" hücre • kapalı kontur dolgu aktif");}); }catch(Exception e){runOnUiThread(()->status.setText("Analiz hatası: "+e.getMessage()));}}).start(); }

    boolean[][] analyze(Bitmap bm,int gw,int gh){Bitmap b=Bitmap.createScaledBitmap(bm,gw,gh,true);boolean[][] wall=new boolean[gh][gw];for(int y=0;y<gh;y++)for(int x=0;x<gw;x++){int c=b.getPixel(x,y);int g=(Color.red(c)*30+Color.green(c)*59+Color.blue(c)*11)/100;wall[y][x]=g<145;}boolean[][] w2=new boolean[gh][gw];for(int y=0;y<gh;y++)for(int x=0;x<gw;x++){int q=0;for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){int yy=y+dy,xx=x+dx;if(yy>=0&&yy<gh&&xx>=0&&xx<gw&&wall[yy][xx])q++;}w2[y][x]=wall[y][x]||q>=4;}boolean[][] outside=new boolean[gh][gw];ArrayDeque<int[]> q=new ArrayDeque<>();for(int x=0;x<gw;x++){seed(x,0,w2,outside,q);seed(x,gh-1,w2,outside,q);}for(int y=0;y<gh;y++){seed(0,y,w2,outside,q);seed(gw-1,y,w2,outside,q);}int[] dx={1,-1,0,0},dy={0,0,1,-1};while(!q.isEmpty()){int[] p=q.removeFirst();for(int k=0;k<4;k++){int x=p[0]+dx[k],y=p[1]+dy[k];if(x>=0&&x<gw&&y>=0&&y<gh&&!w2[y][x]&&!outside[y][x]){outside[y][x]=true;q.add(new int[]{x,y});}}}boolean[][] s=new boolean[gh][gw];for(int y=0;y<gh;y++)for(int x=0;x<gw;x++)s[y][x]=w2[y][x]||!outside[y][x];boolean[][] out=new boolean[gh][gw];for(int y=0;y<gh;y++)for(int x=0;x<gw;x++){if(!s[y][x])continue;int n=0;for(int yy=Math.max(0,y-1);yy<=Math.min(gh-1,y+1);yy++)for(int xx=Math.max(0,x-1);xx<=Math.min(gw-1,x+1);xx++)if(s[yy][xx])n++;out[y][x]=n>=2;}return out;}
    void seed(int x,int y,boolean[][] w,boolean[][] o,ArrayDeque<int[]> q){if(!w[y][x]&&!o[y][x]){o[y][x]=true;q.add(new int[]{x,y});}}
    int count(boolean[][] s){int n=0;for(boolean[] r:s)for(boolean v:r)if(v)n++;return n;}

    void save(){if(solid==null){status.setText("Önce 3D model oluştur.");return;}pendingStl=toStl(solid);Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("model/stl");i.putExtra(Intent.EXTRA_TITLE,"MG_Drawing2CAD_Model.stl");startActivityForResult(i,SAVE);}
    String toStl(boolean[][] a){StringBuilder s=new StringBuilder("solid MG_Drawing2CAD\n");int h=a.length,w=a[0].length;float z=6f;for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(a[y][x]){face(s,x,y,z,true);face(s,x,y,0,false);if(x==0||!a[y][x-1])sideX(s,x,y,0,z,false);if(x==w-1||!a[y][x+1])sideX(s,x+1,y,0,z,true);if(y==0||!a[y-1][x])sideY(s,x,y,0,z,false);if(y==h-1||!a[y+1][x])sideY(s,x,y+1,0,z,true);}s.append("endsolid MG_Drawing2CAD\n");return s.toString();}
    void tri(StringBuilder s,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz){s.append("facet normal 0 0 0\n outer loop\n");v(s,ax,ay,az);v(s,bx,by,bz);v(s,cx,cy,cz);s.append(" endloop\nendfacet\n");}
    void v(StringBuilder s,float x,float y,float z){s.append("  vertex ").append(x).append(' ').append(y).append(' ').append(z).append('\n');}
    void face(StringBuilder s,float x,float y,float z,boolean top){if(top){tri(s,x,y,z,x+1,y,z,x+1,y+1,z);tri(s,x,y,z,x+1,y+1,z,x,y+1,z);}else{tri(s,x,y,0,x+1,y+1,0,x+1,y,0);tri(s,x,y,0,x,y+1,0,x+1,y+1,0);}}
    void sideX(StringBuilder s,float x,float y,float z0,float z1,boolean pos){if(pos){tri(s,x,y,z0,x,y+1,z0,x,y+1,z1);tri(s,x,y,z0,x,y+1,z1,x,y,z1);}else{tri(s,x,y,z0,x,y+1,z1,x,y+1,z0);tri(s,x,y,z0,x,y,z1,x,y+1,z1);}}
    void sideY(StringBuilder s,float x,float y,float z0,float z1,boolean pos){if(pos){tri(s,x,y,z0,x,y,z1,x+1,y,z1);tri(s,x,y,z0,x+1,y,z1,x+1,y,z0);}else{tri(s,x,y,z0,x+1,y,z1,x,y,z1);tri(s,x,y,z0,x+1,y,z0,x+1,y,z1);}}

    static class MeshView extends View{
        Paint p=new Paint(3),edge=new Paint(3),gridPaint=new Paint(3);Bitmap bmp;boolean[][] s;float yaw=.72f,pitch=.60f,scale=7f,lx,ly,lastD;boolean drag,grid=true;
        MeshView(Context c){super(c);p.setStyle(Paint.Style.FILL);edge.setStyle(Paint.Style.STROKE);edge.setStrokeWidth(1f);edge.setColor(Color.rgb(51,65,85));gridPaint.setStrokeWidth(1f);gridPaint.setColor(Color.rgb(25,39,60));setBackgroundColor(Color.rgb(8,13,23));}
        void setBitmap(Bitmap b){bmp=b;s=null;invalidate();}void setSolid(boolean[][] a){s=a;bmp=null;fit();}
        void iso(){yaw=.72f;pitch=.60f;invalidate();}void top(){yaw=0;pitch=0;invalidate();}void fit(){scale=s==null?7f:Math.max(3f,Math.min(10f,getWidth()>0?(getWidth()*.72f/Math.max(s.length,s[0].length)):7f));invalidate();}void toggleGrid(){grid=!grid;invalidate();}
        protected void onDraw(Canvas c){super.onDraw(c);if(grid)drawGrid(c);if(s==null){if(bmp!=null){float pad=34;RectF dst=fitRect(bmp.getWidth(),bmp.getHeight(),pad,pad,getWidth()-pad,getHeight()-pad);Paint bp=new Paint(3);c.drawBitmap(bmp,null,dst,bp);Paint frame=new Paint(3);frame.setStyle(Paint.Style.STROKE);frame.setStrokeWidth(2);frame.setColor(Color.rgb(34,211,238));c.drawRect(dst,frame);}return;}c.save();c.translate(getWidth()/2f,getHeight()/2f+45);ArrayList<Cell> cells=new ArrayList<>();for(int y=0;y<s.length;y++)for(int x=0;x<s[0].length;x++)if(s[y][x])cells.add(new Cell(x-s[0].length/2f,y-s.length/2f));Collections.sort(cells,(a,b)->Float.compare(a.x+a.y,b.x+b.y));for(Cell a:cells)drawBlock(c,a.x,a.y,0,1,1,1);c.restore();}
        void drawGrid(Canvas c){int step=40;for(int x=0;x<getWidth();x+=step)c.drawLine(x,0,x,getHeight(),gridPaint);for(int y=0;y<getHeight();y+=step)c.drawLine(0,y,getWidth(),y,gridPaint);Paint ax=new Paint(3);ax.setStrokeWidth(2);ax.setColor(Color.rgb(30,70,90));c.drawLine(getWidth()/2f,0,getWidth()/2f,getHeight(),ax);c.drawLine(0,getHeight()/2f,getWidth(),getHeight()/2f,ax);}
        RectF fitRect(float iw,float ih,float l,float t,float r,float b){float aw=r-l,ah=b-t,k=Math.min(aw/iw,ah/ih),w=iw*k,h=ih*k,x=l+(aw-w)/2,y=t+(ah-h)/2;return new RectF(x,y,x+w,y+h);}
        void drawBlock(Canvas c,float x,float y,float z,float wx,float wy,float wz){float[][] v={{x,y,z},{x+wx,y,z},{x+wx,y+wy,z},{x,y+wy,z},{x,y,z+wz},{x+wx,y,z+wz},{x+wx,y+wy,z+wz},{x,y+wy,z+wz}};PointF[] q=new PointF[8];for(int i=0;i<8;i++)q[i]=proj(v[i][0],v[i][1],v[i][2]);poly(c,q,new int[]{4,5,6,7},Color.rgb(103,232,249));poly(c,q,new int[]{1,2,6,5},Color.rgb(22,163,174));poly(c,q,new int[]{2,3,7,6},Color.rgb(14,116,144));}
        PointF proj(float x,float y,float z){float cy=(float)Math.cos(yaw),sy=(float)Math.sin(yaw),cp=(float)Math.cos(pitch),sp=(float)Math.sin(pitch);float X=x*cy-y*sy,Y=x*sy+y*cy,Y2=Y*cp-z*sp;return new PointF(X*scale,Y2*scale);}
        void poly(Canvas c,PointF[] q,int[] id,int col){Path path=new Path();path.moveTo(q[id[0]].x,q[id[0]].y);for(int i=1;i<id.length;i++)path.lineTo(q[id[i]].x,q[id[i]].y);path.close();p.setColor(col);c.drawPath(path,p);c.drawPath(path,edge);}
        public boolean onTouchEvent(MotionEvent e){if(e.getPointerCount()==2){float d=(float)Math.hypot(e.getX(0)-e.getX(1),e.getY(0)-e.getY(1));if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&lastD>0){scale*=d/lastD;scale=Math.max(1.5f,Math.min(24f,scale));invalidate();}lastD=d;return true;}lastD=0;if(e.getAction()==MotionEvent.ACTION_DOWN){lx=e.getX();ly=e.getY();drag=true;return true;}if(e.getAction()==MotionEvent.ACTION_MOVE&&drag){yaw+=(e.getX()-lx)*.01f;pitch+=(e.getY()-ly)*.01f;pitch=Math.max(-1.3f,Math.min(1.3f,pitch));lx=e.getX();ly=e.getY();invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_UP)drag=false;return true;}
        static class Cell{float x,y;Cell(float a,float b){x=a;y=b;}}
    }
}
