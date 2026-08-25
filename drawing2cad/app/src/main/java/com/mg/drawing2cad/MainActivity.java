package com.mg.drawing2cad;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

public class MainActivity extends Activity {
    static final int PICK=100, SAVE_PDF=101, SAVE_DXF=102, SAVE_STL=103;
    static final int MODE_2D3D=0, MODE_3D2D=1;
    final int BG=Color.rgb(3,12,22), PANEL=Color.rgb(6,23,39), PANEL2=Color.rgb(9,34,57), TEXT=Color.rgb(234,244,252), MUTED=Color.rgb(146,170,190), CYAN=Color.rgb(55,205,255), GREEN=Color.rgb(65,220,120);

    int mode=MODE_2D3D;
    Bitmap source2d;
    boolean[][][] voxels;
    ArrayList<Tri> mesh=new ArrayList<>();
    CadView cad;
    TextView status, fileInfo, modeInfo;
    Uri archiveUri;
    ArrayList<String> archiveEntries=new ArrayList<>();

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        buildUi();
    }

    void buildUi(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL); header.setPadding(dp(12),dp(7),dp(10),dp(7)); header.setBackgroundColor(PANEL);
        LinearLayout titles=new LinearLayout(this); titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(txt("MG CAD Studio",20,CYAN,true)); titles.addView(txt("BIDIRECTIONAL CAD • v2.0",9,MUTED,true));
        header.addView(titles,new LinearLayout.LayoutParams(dp(210),-2));
        Button m23=button("2D → 3D"), m32=button("3D → 2D"); header.addView(m23); header.addView(m32);
        modeInfo=txt("  TEKNİK RESİM → 3D MODEL",11,GREEN,true); header.addView(modeInfo,new LinearLayout.LayoutParams(0,-2,1));
        Button open=button("DOSYA AÇ"), create=button("OLUŞTUR"), pdf=button("TEKNİK RESİM PDF"), dxf=button("DXF"), stl=button("STL");
        for(Button b1:new Button[]{open,create,pdf,dxf,stl}) header.addView(b1);
        root.addView(header,new LinearLayout.LayoutParams(-1,dp(58)));

        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(8),dp(4),dp(8),dp(4));bar.setBackgroundColor(Color.rgb(5,18,31));
        Button iso=small("ISO"), front=small("ÖN"), top=small("ÜST"), right=small("SAĞ"), left=small("SOL"), bottom=small("ALT"), fit=small("SIĞDIR"), wire=small("TEL KAFES");
        for(Button b1:new Button[]{iso,front,top,right,left,bottom,fit,wire})bar.addView(b1);
        fileInfo=txt("Dosya seçilmedi",10,MUTED,false);bar.addView(fileInfo,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(bar,new LinearLayout.LayoutParams(-1,dp(48)));

        cad=new CadView(this); root.addView(cad,new LinearLayout.LayoutParams(-1,0,1));
        status=txt("2D→3D: ön/sağ/üst görünüşlü teknik resim aç. 3D→2D: STL veya OBJ model aç.",10,MUTED,false);status.setPadding(dp(12),dp(8),dp(12),dp(8));status.setBackgroundColor(PANEL);root.addView(status,new LinearLayout.LayoutParams(-1,dp(40)));
        setContentView(root);

        m23.setOnClickListener(v->setMode(MODE_2D3D)); m32.setOnClickListener(v->setMode(MODE_3D2D));
        open.setOnClickListener(v->pick()); create.setOnClickListener(v->createAction()); pdf.setOnClickListener(v->saveTechnicalPdf()); dxf.setOnClickListener(v->saveDxf()); stl.setOnClickListener(v->saveStl());
        iso.setOnClickListener(v->cad.setView(0)); front.setOnClickListener(v->cad.setView(1)); top.setOnClickListener(v->cad.setView(2)); right.setOnClickListener(v->cad.setView(3)); left.setOnClickListener(v->cad.setView(4)); bottom.setOnClickListener(v->cad.setView(5)); fit.setOnClickListener(v->cad.fit()); wire.setOnClickListener(v->{cad.wire=!cad.wire;cad.invalidate();});
    }

    void setMode(int m){
        mode=m;
        if(m==MODE_2D3D){modeInfo.setText("  TEKNİK RESİM → 3D MODEL");status.setText("2D→3D modu: PDF/PNG/JPG/ZIP içinden çok görünüşlü teknik resmi aç.");}
        else{modeInfo.setText("  3D MODEL → TEKNİK RESİM");status.setText("3D→2D modu: STL veya OBJ aç; ön/üst/sağ/izometrik teknik resim üret.");}
    }

    void pick(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");
        if(mode==MODE_2D3D)i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","application/pdf","application/zip","application/x-zip-compressed","application/octet-stream"});
        else i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"model/stl","application/sla","application/octet-stream","text/plain","model/obj"});
        startActivityForResult(i,PICK);
    }

    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data); if(res!=RESULT_OK||data==null||data.getData()==null)return;
        try{
            Uri u=data.getData();
            if(req==PICK){ if(mode==MODE_2D3D)load2d(u); else load3d(u); }
            else if(req==SAVE_PDF){try(OutputStream o=getContentResolver().openOutputStream(u)){writeTechnicalPdf(o);}status.setText("✓ Teknik resim PDF kaydedildi.");}
            else if(req==SAVE_DXF){try(OutputStream o=getContentResolver().openOutputStream(u)){o.write(buildDxf().getBytes(StandardCharsets.UTF_8));}status.setText("✓ Teknik resim DXF kaydedildi.");}
            else if(req==SAVE_STL){try(OutputStream o=getContentResolver().openOutputStream(u)){o.write(buildStl().getBytes(StandardCharsets.UTF_8));}status.setText("✓ STL kaydedildi.");}
        }catch(Exception e){status.setText("Hata: "+e.getMessage());}
    }

    void load2d(Uri u)throws Exception{
        String name=fileName(u), low=name.toLowerCase(Locale.ROOT); fileInfo.setText(name);
        if(low.endsWith(".zip")||low.endsWith(".cbz")){archiveUri=u;listArchive(u);return;}
        if(low.endsWith(".pdf"))source2d=renderPdf(u); else try(InputStream in=getContentResolver().openInputStream(u)){source2d=BitmapFactory.decodeStream(in);}
        if(source2d==null)throw new IOException("2D dosya görüntüye çevrilemedi"); voxels=null;mesh.clear();cad.setBitmap(source2d);status.setText("✓ Teknik resim yüklendi. OLUŞTUR ile 3D rekonstrüksiyonu başlat.");
    }

    void listArchive(Uri u)throws Exception{
        archiveEntries.clear();try(InputStream in=getContentResolver().openInputStream(u);ZipInputStream z=new ZipInputStream(new BufferedInputStream(in))){ZipEntry e;while((e=z.getNextEntry())!=null){if(e.isDirectory())continue;String n=e.getName().toLowerCase(Locale.ROOT);if(n.endsWith(".pdf")||n.endsWith(".png")||n.endsWith(".jpg")||n.endsWith(".jpeg")||n.endsWith(".webp"))archiveEntries.add(e.getName());}}
        if(archiveEntries.isEmpty())throw new IOException("ZIP içinde desteklenen teknik resim yok");String[] a=archiveEntries.toArray(new String[0]);new AlertDialog.Builder(this).setTitle("Teknik resim seç").setItems(a,(d,w)->{try{loadArchiveEntry(a[w]);}catch(Exception ex){status.setText("ZIP hatası: "+ex.getMessage());}}).show();
    }

    void loadArchiveEntry(String wanted)throws Exception{
        File f=new File(getCacheDir(),"mgcad_"+Math.abs(wanted.hashCode())+(wanted.toLowerCase(Locale.ROOT).endsWith(".pdf")?".pdf":".img"));boolean found=false;
        try(InputStream in=getContentResolver().openInputStream(archiveUri);ZipInputStream z=new ZipInputStream(new BufferedInputStream(in))){ZipEntry e;while((e=z.getNextEntry())!=null){if(e.getName().equals(wanted)){try(FileOutputStream o=new FileOutputStream(f)){byte[] b=new byte[16384];int n;long total=0;while((n=z.read(b))>0){total+=n;if(total>100L*1024*1024)throw new IOException("ZIP girdisi çok büyük");o.write(b,0,n);}}found=true;break;}}}
        if(!found)throw new IOException("ZIP girdisi bulunamadı"); if(wanted.toLowerCase(Locale.ROOT).endsWith(".pdf"))source2d=renderPdfFile(f);else try(InputStream in=new FileInputStream(f)){source2d=BitmapFactory.decodeStream(in);}fileInfo.setText(wanted);voxels=null;mesh.clear();cad.setBitmap(source2d);status.setText("✓ ZIP içinden teknik resim yüklendi.");
    }

    Bitmap renderPdf(Uri u)throws Exception{File f=new File(getCacheDir(),"source.pdf");try(InputStream in=getContentResolver().openInputStream(u);FileOutputStream o=new FileOutputStream(f)){byte[] b=new byte[16384];int n;while((n=in.read(b))>0)o.write(b,0,n);}return renderPdfFile(f);}
    Bitmap renderPdfFile(File f)throws Exception{ParcelFileDescriptor pfd=ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer r=new PdfRenderer(pfd);if(r.getPageCount()<1)throw new IOException("PDF boş");PdfRenderer.Page p=r.openPage(0);float k=Math.min(3f,2400f/Math.max(p.getWidth(),p.getHeight()));Bitmap b=Bitmap.createBitmap(Math.max(1,(int)(p.getWidth()*k)),Math.max(1,(int)(p.getHeight()*k)),Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);p.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);p.close();r.close();pfd.close();return b;}

    void load3d(Uri u)throws Exception{
        String name=fileName(u),low=name.toLowerCase(Locale.ROOT);byte[] data=readAll(u,120L*1024*1024);ArrayList<Tri> parsed;
        if(low.endsWith(".obj"))parsed=parseObj(new String(data,StandardCharsets.UTF_8)); else parsed=parseStl(data);
        if(parsed.isEmpty())throw new IOException("3D geometri okunamadı");mesh=parsed;voxels=null;source2d=null;cad.setMesh(mesh);fileInfo.setText(name+" • "+mesh.size()+" üçgen");float[] b=bounds(mesh);status.setText(String.format(Locale.US,"✓ 3D model yüklendi • %.2f × %.2f × %.2f • TEKNİK RESİM PDF veya DXF ile çıktı al.",b[3]-b[0],b[4]-b[1],b[5]-b[2]));
    }

    byte[] readAll(Uri u,long max)throws Exception{try(InputStream in=getContentResolver().openInputStream(u);ByteArrayOutputStream o=new ByteArrayOutputStream()){byte[] b=new byte[32768];int n;long t=0;while((n=in.read(b))>0){t+=n;if(t>max)throw new IOException("Dosya boyutu sınırı aşıldı");o.write(b,0,n);}return o.toByteArray();}}

    ArrayList<Tri> parseObj(String s){ArrayList<Vec> vs=new ArrayList<>();ArrayList<Tri> ts=new ArrayList<>();for(String ln:s.split("\\r?\\n")){ln=ln.trim();try{if(ln.startsWith("v ")){String[] q=ln.split("\\s+");vs.add(new Vec(Float.parseFloat(q[1]),Float.parseFloat(q[2]),Float.parseFloat(q[3])));}else if(ln.startsWith("f ")){String[] q=ln.split("\\s+");int[] ids=new int[q.length-1];for(int i=1;i<q.length;i++){String z=q[i].split("/")[0];int id=Integer.parseInt(z);ids[i-1]=id<0?vs.size()+id:id-1;}for(int i=1;i<ids.length-1;i++)ts.add(new Tri(vs.get(ids[0]),vs.get(ids[i]),vs.get(ids[i+1])));}}catch(Exception ignored){}}return ts;}

    ArrayList<Tri> parseStl(byte[] d)throws Exception{
        if(d.length<84)throw new IOException("STL çok küçük");
        int n=ByteBuffer.wrap(d,80,4).order(ByteOrder.LITTLE_ENDIAN).getInt();long expected=84L+50L*n;if(n>0&&n<5_000_000&&expected<=d.length){ArrayList<Tri> t=new ArrayList<>();ByteBuffer b=ByteBuffer.wrap(d).order(ByteOrder.LITTLE_ENDIAN);b.position(84);for(int i=0;i<n;i++){b.position(b.position()+12);Vec a=new Vec(b.getFloat(),b.getFloat(),b.getFloat()),c=new Vec(b.getFloat(),b.getFloat(),b.getFloat()),e=new Vec(b.getFloat(),b.getFloat(),b.getFloat());t.add(new Tri(a,c,e));b.getShort();}return t;}
        String s=new String(d,StandardCharsets.US_ASCII);ArrayList<Vec> v=new ArrayList<>();ArrayList<Tri> t=new ArrayList<>();for(String ln:s.split("\\r?\\n")){ln=ln.trim();if(ln.startsWith("vertex ")){String[] q=ln.split("\\s+");if(q.length>=4){try{v.add(new Vec(Float.parseFloat(q[1]),Float.parseFloat(q[2]),Float.parseFloat(q[3])));if(v.size()==3){t.add(new Tri(v.get(0),v.get(1),v.get(2)));v.clear();}}catch(Exception ignored){}}}}return t;
    }

    void createAction(){
        if(mode==MODE_3D2D){if(mesh.isEmpty()){status.setText("Önce STL/OBJ 3D model aç.");return;}saveTechnicalPdf();return;}
        if(source2d==null){status.setText("Önce teknik resim aç.");return;}status.setText("Ön + üst + sağ görünüş ayrılıyor • silüetler eşleştiriliyor • 3D hacim hesaplanıyor...");new Thread(()->{try{boolean[][][] v=reconstruct(source2d);voxels=v;mesh.clear();runOnUiThread(()->{cad.setVoxels(v);cad.setView(0);status.setText("✓ 2D → 3D MODEL HAZIR • "+count3(v)+" voxel");});}catch(Exception e){runOnUiThread(()->status.setText("Rekonstrüksiyon hatası: "+e.getMessage()));}}).start();
    }

    boolean[][][] reconstruct(Bitmap bm)throws Exception{
        int W=bm.getWidth(),H=bm.getHeight();int topCut=(int)(H*.07f),bottomCut=(int)(H*.07f),usable=H-topCut-bottomCut,midX=W/2,midY=topCut+usable/2;
        Bitmap front=Bitmap.createBitmap(bm,0,topCut,midX,usable/2), right=Bitmap.createBitmap(bm,midX,topCut,W-midX,usable/2), top=Bitmap.createBitmap(bm,0,midY,midX,H-bottomCut-midY);
        Profile pf=profile(front), pr=profile(right), pt=profile(top);if(pf==null||pr==null||pt==null)throw new IOException("Ön/üst/sağ ana görünüşlerden biri bulunamadı");
        int nx=84,nz=Math.max(18,Math.min(72,Math.round(nx*pf.aspectH))),ny=Math.max(18,Math.min(70,Math.round(nx*pt.aspectH)));
        boolean[][] fm=resampleMask(pf.mask,nx,nz),tm=resampleMask(pt.mask,nx,ny),rm=resampleMask(pr.mask,ny,nz);
        boolean[][][] v=new boolean[nx][ny][nz];for(int x=0;x<nx;x++)for(int y=0;y<ny;y++)for(int z=0;z<nz;z++)v[x][y][z]=fm[nz-1-z][x]&&tm[ny-1-y][x]&&rm[nz-1-z][y];
        v=largest3(v);v=smooth3(v);if(count3(v)<100)throw new IOException("Görünüşlerin 3D kesişimi yetersiz");return v;
    }

    static class Profile{boolean[][] mask;float aspectH;Profile(boolean[][]m,float a){mask=m;aspectH=a;}}
    Profile profile(Bitmap src){
        int tw=180,th=Math.max(80,Math.round(180f*src.getHeight()/src.getWidth()));Bitmap s=Bitmap.createScaledBitmap(src,tw,th,true);boolean[][] d=new boolean[th][tw];for(int y=0;y<th;y++)for(int x=0;x<tw;x++)d[y][x]=gray(s.getPixel(x,y))<95;
        Comp best=largestComp(d);if(best==null||best.pts.size()<30)return null;int mx=2,my=2,w=Math.max(1,best.maxX-best.minX+1),h=Math.max(1,best.maxY-best.minY+1);int x0=Math.max(0,best.minX-mx),y0=Math.max(0,best.minY-my),x1=Math.min(tw-1,best.maxX+mx),y1=Math.min(th-1,best.maxY+my);boolean[][] wall=new boolean[y1-y0+1][x1-x0+1];for(int[] p:best.pts)wall[p[1]-y0][p[0]-x0]=true;wall=dilate(wall,1);boolean[][] outside=floodOutside(wall),solid=new boolean[wall.length][wall[0].length];for(int y=0;y<wall.length;y++)for(int x=0;x<wall[0].length;x++)solid[y][x]=wall[y][x]||!outside[y][x];return new Profile(solid,solid.length/(float)solid[0].length);
    }

    static class Comp{ArrayList<int[]>pts=new ArrayList<>();int minX=9999,minY=9999,maxX=-1,maxY=-1;void add(int x,int y){pts.add(new int[]{x,y});minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);}}
    Comp largestComp(boolean[][] a){int h=a.length,w=a[0].length;boolean[][] seen=new boolean[h][w];Comp best=null;int[][] dd={{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(a[y][x]&&!seen[y][x]){Comp c=new Comp();ArrayDeque<int[]>q=new ArrayDeque<>();q.add(new int[]{x,y});seen[y][x]=true;while(!q.isEmpty()){int[]p=q.removeFirst();c.add(p[0],p[1]);for(int[]d:dd){int xx=p[0]+d[0],yy=p[1]+d[1];if(xx>=0&&xx<w&&yy>=0&&yy<h&&a[yy][xx]&&!seen[yy][xx]){seen[yy][xx]=true;q.add(new int[]{xx,yy});}}}if(best==null||c.pts.size()>best.pts.size())best=c;}return best;}
    boolean[][] resampleMask(boolean[][]a,int w,int h){Bitmap b=Bitmap.createBitmap(a[0].length,a.length,Bitmap.Config.ARGB_8888);for(int y=0;y<a.length;y++)for(int x=0;x<a[0].length;x++)b.setPixel(x,y,a[y][x]?Color.BLACK:Color.WHITE);Bitmap s=Bitmap.createScaledBitmap(b,w,h,false);boolean[][]o=new boolean[h][w];for(int y=0;y<h;y++)for(int x=0;x<w;x++)o[y][x]=gray(s.getPixel(x,y))<128;return o;}
    int gray(int c){return (Color.red(c)*30+Color.green(c)*59+Color.blue(c)*11)/100;}
    boolean[][] dilate(boolean[][]a,int r){int h=a.length,w=a[0].length;boolean[][]o=new boolean[h][w];for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(a[y][x])for(int yy=Math.max(0,y-r);yy<=Math.min(h-1,y+r);yy++)for(int xx=Math.max(0,x-r);xx<=Math.min(w-1,x+r);xx++)o[yy][xx]=true;return o;}
    boolean[][] floodOutside(boolean[][]wall){int h=wall.length,w=wall[0].length;boolean[][]o=new boolean[h][w];ArrayDeque<int[]>q=new ArrayDeque<>();for(int x=0;x<w;x++){seed(x,0,wall,o,q);seed(x,h-1,wall,o,q);}for(int y=0;y<h;y++){seed(0,y,wall,o,q);seed(w-1,y,wall,o,q);}int[][]d={{1,0},{-1,0},{0,1},{0,-1}};while(!q.isEmpty()){int[]p=q.removeFirst();for(int[]z:d){int x=p[0]+z[0],y=p[1]+z[1];if(x>=0&&x<w&&y>=0&&y<h&&!wall[y][x]&&!o[y][x]){o[y][x]=true;q.add(new int[]{x,y});}}}return o;}
    void seed(int x,int y,boolean[][]w,boolean[][]o,ArrayDeque<int[]>q){if(!w[y][x]&&!o[y][x]){o[y][x]=true;q.add(new int[]{x,y});}}
    boolean[][][] largest3(boolean[][][]a){int nx=a.length,ny=a[0].length,nz=a[0][0].length;boolean[][][]seen=new boolean[nx][ny][nz],best=null;int bestN=0;int[][]d={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};for(int x=0;x<nx;x++)for(int y=0;y<ny;y++)for(int z=0;z<nz;z++)if(a[x][y][z]&&!seen[x][y][z]){ArrayList<int[]>pts=new ArrayList<>();ArrayDeque<int[]>q=new ArrayDeque<>();q.add(new int[]{x,y,z});seen[x][y][z]=true;while(!q.isEmpty()){int[]p=q.removeFirst();pts.add(p);for(int[]e:d){int xx=p[0]+e[0],yy=p[1]+e[1],zz=p[2]+e[2];if(xx>=0&&xx<nx&&yy>=0&&yy<ny&&zz>=0&&zz<nz&&a[xx][yy][zz]&&!seen[xx][yy][zz]){seen[xx][yy][zz]=true;q.add(new int[]{xx,yy,zz});}}}if(pts.size()>bestN){bestN=pts.size();best=new boolean[nx][ny][nz];for(int[]p:pts)best[p[0]][p[1]][p[2]]=true;}}return best==null?a:best;}
    boolean[][][] smooth3(boolean[][][]a){int nx=a.length,ny=a[0].length,nz=a[0][0].length;boolean[][][]o=new boolean[nx][ny][nz];for(int x=0;x<nx;x++)for(int y=0;y<ny;y++)for(int z=0;z<nz;z++){int n=0;for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++){int xx=x+dx,yy=y+dy,zz=z+dz;if(xx>=0&&xx<nx&&yy>=0&&yy<ny&&zz>=0&&zz<nz&&a[xx][yy][zz])n++;}o[x][y][z]=a[x][y][z]?n>=5:n>=18;}return o;}
    int count3(boolean[][][]a){if(a==null)return 0;int n=0;for(boolean[][]x:a)for(boolean[]y:x)for(boolean z:y)if(z)n++;return n;}

    void saveTechnicalPdf(){if(mesh.isEmpty()){status.setText("3D→2D için önce STL/OBJ model aç.");return;}Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");i.putExtra(Intent.EXTRA_TITLE,"MG_CAD_Technical_Drawing.pdf");startActivityForResult(i,SAVE_PDF);}
    void saveDxf(){if(mesh.isEmpty()){status.setText("DXF için önce 3D model aç.");return;}Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/dxf");i.putExtra(Intent.EXTRA_TITLE,"MG_CAD_Technical_Drawing.dxf");startActivityForResult(i,SAVE_DXF);}
    void saveStl(){if(voxels==null&&mesh.isEmpty()){status.setText("Kaydedilecek 3D model yok.");return;}Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("model/stl");i.putExtra(Intent.EXTRA_TITLE,"MG_CAD_Model.stl");startActivityForResult(i,SAVE_STL);}

    void writeTechnicalPdf(OutputStream out)throws Exception{
        PdfDocument doc=new PdfDocument();PdfDocument.PageInfo pi=new PdfDocument.PageInfo.Builder(1191,842,1).create();PdfDocument.Page page=doc.startPage(pi);Canvas c=page.getCanvas();c.drawColor(Color.WHITE);Paint p=new Paint(3);p.setColor(Color.BLACK);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f);Paint tx=new Paint(3);tx.setColor(Color.BLACK);tx.setTextSize(14);tx.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));
        c.drawRect(20,20,1171,822,p);c.drawText("MG CAD STUDIO — AUTOMATIC TECHNICAL DRAWING",35,45,tx);float[] b=bounds(mesh);Paint sub=new Paint(3);sub.setColor(Color.DKGRAY);sub.setTextSize(11);c.drawText(String.format(Locale.US,"Overall: X %.3f   Y %.3f   Z %.3f   |   Units: model units",b[3]-b[0],b[4]-b[1],b[5]-b[2]),35,65,sub);
        RectF f=new RectF(40,90,560,370),r=new RectF(620,90,1135,370),t=new RectF(40,420,560,740),iso=new RectF(620,420,1135,740);drawProjection(c,mesh,f,0,p);drawProjection(c,mesh,r,2,p);drawProjection(c,mesh,t,1,p);drawProjection(c,mesh,iso,3,p);c.drawText("FRONT",45,385,tx);c.drawText("RIGHT",625,385,tx);c.drawText("TOP",45,755,tx);c.drawText("ISOMETRIC",625,755,tx);
        drawDimension(c,f,b[3]-b[0],b[5]-b[2],sub,p);drawDimension(c,r,b[4]-b[1],b[5]-b[2],sub,p);drawDimension(c,t,b[3]-b[0],b[4]-b[1],sub,p);
        c.drawRect(620,775,1135,815,p);c.drawText("PART: Imported 3D model     SCALE: AUTO     PROJECTION: ORTHOGRAPHIC     REV: A",630,800,sub);doc.finishPage(page);doc.writeTo(out);doc.close();
    }

    void drawDimension(Canvas c,RectF box,float w,float h,Paint tx,Paint p){float y=box.bottom-8;c.drawLine(box.left+20,y,box.right-20,y,p);c.drawText(String.format(Locale.US,"%.3f",w),box.centerX()-20,y-4,tx);c.save();c.rotate(-90,box.left+8,box.centerY());c.drawText(String.format(Locale.US,"%.3f",h),box.left+8,box.centerY(),tx);c.restore();}

    void drawProjection(Canvas c,ArrayList<Tri> ts,RectF box,int view,Paint p){float[] ext=projectBounds(ts,view);float sx=(box.width()-35)/Math.max(.0001f,ext[2]-ext[0]),sy=(box.height()-35)/Math.max(.0001f,ext[3]-ext[1]),s=Math.min(sx,sy);float ox=box.centerX()-(ext[0]+ext[2])*.5f*s,oy=box.centerY()+(ext[1]+ext[3])*.5f*s;c.drawRect(box,p);for(Tri tr:ts){PointF a=proj(tr.a,view),b=proj(tr.b,view),d=proj(tr.c,view);c.drawLine(ox+a.x*s,oy-a.y*s,ox+b.x*s,oy-b.y*s,p);c.drawLine(ox+b.x*s,oy-b.y*s,ox+d.x*s,oy-d.y*s,p);c.drawLine(ox+d.x*s,oy-d.y*s,ox+a.x*s,oy-a.y*s,p);}}
    float[] projectBounds(ArrayList<Tri>ts,int v){float minX=Float.MAX_VALUE,minY=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,maxY=-Float.MAX_VALUE;for(Tri t:ts)for(Vec q:new Vec[]{t.a,t.b,t.c}){PointF p=proj(q,v);minX=Math.min(minX,p.x);maxX=Math.max(maxX,p.x);minY=Math.min(minY,p.y);maxY=Math.max(maxY,p.y);}return new float[]{minX,minY,maxX,maxY};}
    PointF proj(Vec q,int v){if(v==0)return new PointF(q.x,q.z);if(v==1)return new PointF(q.x,q.y);if(v==2)return new PointF(q.y,q.z);return new PointF((q.x-q.y)*.866f,q.z+(q.x+q.y)*.5f);}

    String buildDxf(){StringBuilder s=new StringBuilder("0\nSECTION\n2\nHEADER\n0\nENDSEC\n0\nSECTION\n2\nENTITIES\n");float[] b=bounds(mesh);appendDxfView(s,0,0,0);appendDxfView(s,2,(b[3]-b[0])*1.35f,0);appendDxfView(s,1,0,-(b[5]-b[2])*1.55f);appendDxfView(s,3,(b[3]-b[0])*1.35f,-(b[5]-b[2])*1.55f);s.append("0\nENDSEC\n0\nEOF\n");return s.toString();}
    void appendDxfView(StringBuilder s,int v,float ox,float oy){for(Tri t:mesh){Vec[]q={t.a,t.b,t.c,t.a};for(int i=0;i<3;i++){PointF a=proj(q[i],v),b=proj(q[i+1],v);s.append("0\nLINE\n8\nOBJECT\n10\n").append(a.x+ox).append("\n20\n").append(a.y+oy).append("\n30\n0\n11\n").append(b.x+ox).append("\n21\n").append(b.y+oy).append("\n31\n0\n");}}}

    String buildStl(){if(!mesh.isEmpty()){StringBuilder s=new StringBuilder("solid MG_CAD\n");for(Tri t:mesh)stlTri(s,t.a,t.b,t.c);return s.append("endsolid MG_CAD\n").toString();}StringBuilder s=new StringBuilder("solid MG_CAD_VOXEL\n");int nx=voxels.length,ny=voxels[0].length,nz=voxels[0][0].length;for(int x=0;x<nx;x++)for(int y=0;y<ny;y++)for(int z=0;z<nz;z++)if(voxels[x][y][z]){if(x==0||!voxels[x-1][y][z])quad(s,new Vec(x,y,z),new Vec(x,y+1,z),new Vec(x,y+1,z+1),new Vec(x,y,z+1));if(x==nx-1||!voxels[x+1][y][z])quad(s,new Vec(x+1,y,z),new Vec(x+1,y,z+1),new Vec(x+1,y+1,z+1),new Vec(x+1,y+1,z));if(y==0||!voxels[x][y-1][z])quad(s,new Vec(x,y,z),new Vec(x,y,z+1),new Vec(x+1,y,z+1),new Vec(x+1,y,z));if(y==ny-1||!voxels[x][y+1][z])quad(s,new Vec(x,y+1,z),new Vec(x+1,y+1,z),new Vec(x+1,y+1,z+1),new Vec(x,y+1,z+1));if(z==0||!voxels[x][y][z-1])quad(s,new Vec(x,y,z),new Vec(x+1,y,z),new Vec(x+1,y+1,z),new Vec(x,y+1,z));if(z==nz-1||!voxels[x][y][z+1])quad(s,new Vec(x,y,z+1),new Vec(x,y+1,z+1),new Vec(x+1,y+1,z+1),new Vec(x+1,y,z+1));}return s.append("endsolid MG_CAD_VOXEL\n").toString();}
    void quad(StringBuilder s,Vec a,Vec b,Vec c,Vec d){stlTri(s,a,b,c);stlTri(s,a,c,d);}void stlTri(StringBuilder s,Vec a,Vec b,Vec c){s.append("facet normal 0 0 0\n outer loop\n");for(Vec v:new Vec[]{a,b,c})s.append("  vertex ").append(v.x).append(' ').append(v.y).append(' ').append(v.z).append('\n');s.append(" endloop\nendfacet\n");}

    float[] bounds(ArrayList<Tri>ts){float[]b={Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE};for(Tri t:ts)for(Vec v:new Vec[]{t.a,t.b,t.c}){b[0]=Math.min(b[0],v.x);b[1]=Math.min(b[1],v.y);b[2]=Math.min(b[2],v.z);b[3]=Math.max(b[3],v.x);b[4]=Math.max(b[4],v.y);b[5]=Math.max(b[5],v.z);}return b;}

    String fileName(Uri u){String n="dosya";Cursor c=null;try{c=getContentResolver().query(u,null,null,null,null);if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)n=c.getString(i);}}catch(Exception ignored){}finally{if(c!=null)c.close();}return n==null?"dosya":n;}
    TextView txt(String s,int z,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(10);b.setTextColor(TEXT);b.setAllCaps(false);b.setPadding(dp(10),0,dp(10),0);b.setMinHeight(0);b.setMinimumHeight(0);round(b,PANEL2,Color.rgb(25,86,125),9);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(42));lp.setMargins(dp(3),0,dp(3),0);b.setLayoutParams(lp);return b;}
    Button small(String s){Button b=button(s);b.setTextSize(9);return b;}void round(View v,int fill,int stroke,int r){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setStroke(1,stroke);g.setCornerRadius(dp(r));v.setBackground(g);}int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}

    static class Vec{float x,y,z;Vec(float a,float b,float c){x=a;y=b;z=c;}}
    static class Tri{Vec a,b,c;Tri(Vec x,Vec y,Vec z){a=x;b=y;c=z;}}

    class CadView extends View{
        Paint p=new Paint(3),grid=new Paint(3);Bitmap bm;ArrayList<Tri>m=new ArrayList<>();boolean[][][]v;int view=0;boolean wire=false;float yaw=.65f,pitch=.45f,zoom=1f,panX=0,panY=0,lastX,lastY,lastDist;boolean drag=false;
        CadView(Context c){super(c);setBackgroundColor(BG);grid.setColor(Color.rgb(15,55,80));grid.setStrokeWidth(1);}
        void setBitmap(Bitmap b){bm=b;m.clear();v=null;invalidate();}void setMesh(ArrayList<Tri>x){m=x;bm=null;v=null;fit();}void setVoxels(boolean[][][]x){v=x;bm=null;m.clear();fit();}void setView(int q){view=q;if(q==0){yaw=.65f;pitch=.45f;}invalidate();}void fit(){zoom=1;panX=panY=0;invalidate();}
        @Override protected void onDraw(Canvas c){super.onDraw(c);for(int x=0;x<getWidth();x+=dp(28))c.drawLine(x,0,x,getHeight(),grid);for(int y=0;y<getHeight();y+=dp(28))c.drawLine(0,y,getWidth(),y,grid);if(bm!=null){float k=Math.min(getWidth()/(float)bm.getWidth(),getHeight()/(float)bm.getHeight());float w=bm.getWidth()*k,h=bm.getHeight()*k;c.drawBitmap(bm,null,new RectF((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2),p);return;}if(!m.isEmpty())drawMesh(c);else if(v!=null)drawVoxels(c);}
        void drawMesh(Canvas c){float[]bb=bounds(m);float cx=(bb[0]+bb[3])/2,cy=(bb[1]+bb[4])/2,cz=(bb[2]+bb[5])/2,max=Math.max(bb[3]-bb[0],Math.max(bb[4]-bb[1],bb[5]-bb[2]));float s=Math.min(getWidth(),getHeight())*.62f/Math.max(.001f,max)*zoom;ArrayList<DrawTri>ds=new ArrayList<>();for(Tri t:m){P3 a=transform(t.a,cx,cy,cz),b=transform(t.b,cx,cy,cz),d=transform(t.c,cx,cy,cz);ds.add(new DrawTri(a,b,d,(a.z+b.z+d.z)/3));}Collections.sort(ds,(a,b)->Float.compare(a.depth,b.depth));for(DrawTri t:ds){Path q=new Path();q.moveTo(getWidth()/2+panX+t.a.x*s,getHeight()/2+panY-t.a.y*s);q.lineTo(getWidth()/2+panX+t.b.x*s,getHeight()/2+panY-t.b.y*s);q.lineTo(getWidth()/2+panX+t.c.x*s,getHeight()/2+panY-t.c.y*s);q.close();if(!wire){p.setStyle(Paint.Style.FILL);int shade=(int)Math.max(70,Math.min(210,145-t.depth*4));p.setColor(Color.rgb(40,shade,Math.min(240,shade+35)));c.drawPath(q,p);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(wire?1.2f:.45f);p.setColor(wire?CYAN:Color.rgb(13,83,108));c.drawPath(q,p);}}
        P3 transform(Vec q,float cx,float cy,float cz){float x=q.x-cx,y=q.y-cy,z=q.z-cz;if(view==1)return new P3(x,z,y);if(view==2)return new P3(x,y,z);if(view==3)return new P3(y,z,x);if(view==4)return new P3(-y,z,-x);if(view==5)return new P3(x,-y,-z);float ca=(float)Math.cos(yaw),sa=(float)Math.sin(yaw),cp=(float)Math.cos(pitch),sp=(float)Math.sin(pitch);float x1=ca*x-sa*y,y1=sa*x+ca*y,z1=z;return new P3(x1,cp*z1-sp*y1,sp*z1+cp*y1);}
        void drawVoxels(Canvas c){int nx=v.length,ny=v[0].length,nz=v[0][0].length;float s=Math.min(getWidth(),getHeight())/(float)(Math.max(nx,Math.max(ny,nz))*2.1f)*zoom,ox=getWidth()/2+panX,oy=getHeight()*.68f+panY;p.setStyle(Paint.Style.FILL);for(int z=0;z<nz;z++)for(int y=ny-1;y>=0;y--)for(int x=0;x<nx;x++)if(v[x][y][z]&&(z==nz-1||!v[x][y][z+1]||x==nx-1||!v[x+1][y][z]||y==0||!v[x][y-1][z])){float px=ox+(x-y)*s*.86f,py=oy-(x+y)*s*.45f-z*s;p.setColor(Color.rgb(49,181,215));c.drawRect(px-s*.42f,py-s*.42f,px+s*.42f,py+s*.42f,p);}}
        @Override public boolean onTouchEvent(MotionEvent e){if(e.getPointerCount()>=2){float dx=e.getX(0)-e.getX(1),dy=e.getY(0)-e.getY(1),d=(float)Math.hypot(dx,dy),mx=(e.getX(0)+e.getX(1))/2,my=(e.getY(0)+e.getY(1))/2;if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&lastDist>0){zoom*=d/lastDist;zoom=Math.max(.2f,Math.min(8f,zoom));panX+=(mx-lastX);panY+=(my-lastY);invalidate();}lastDist=d;lastX=mx;lastY=my;return true;}lastDist=0;if(e.getAction()==MotionEvent.ACTION_DOWN){lastX=e.getX();lastY=e.getY();drag=true;return true;}if(e.getAction()==MotionEvent.ACTION_MOVE&&drag&&view==0){yaw+=(e.getX()-lastX)*.008f;pitch+=(e.getY()-lastY)*.008f;pitch=Math.max(-1.4f,Math.min(1.4f,pitch));lastX=e.getX();lastY=e.getY();invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_UP)drag=false;return true;}
    }
    static class P3{float x,y,z;P3(float a,float b,float c){x=a;y=b;z=c;}}
    static class DrawTri{P3 a,b,c;float depth;DrawTri(P3 x,P3 y,P3 z,float d){a=x;b=y;c=z;depth=d;}}
}
