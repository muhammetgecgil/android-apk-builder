package com.mg.drawing2cad;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    static final int PICK=10, SAVE=11;
    Bitmap source;
    boolean[][] solid;
    MeshView mesh;
    TextView status;
    String pendingStl;

    @Override public void onCreate(Bundle b){ super.onCreate(b); buildUi(); }

    void buildUi(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(20,22,20,18); root.setBackgroundColor(Color.rgb(248,250,252));
        TextView title=new TextView(this); title.setText("MG Drawing2CAD"); title.setTextSize(25); title.setTextColor(Color.rgb(15,23,42)); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); root.addView(title);
        TextView sub=new TextView(this); sub.setText("2D teknik resim → 3D katı profil • v1.0"); sub.setTextSize(14); sub.setTextColor(Color.rgb(71,85,105)); sub.setPadding(0,2,0,12); root.addView(sub);
        LinearLayout buttons=new LinearLayout(this); buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button pick=button("RESİM SEÇ"); Button make=button("3D OLUŞTUR"); Button save=button("STL KAYDET");
        buttons.addView(pick,new LinearLayout.LayoutParams(0,-2,1)); buttons.addView(make,new LinearLayout.LayoutParams(0,-2,1)); buttons.addView(save,new LinearLayout.LayoutParams(0,-2,1)); root.addView(buttons);
        status=new TextView(this); status.setText("PNG/JPG teknik resmi seç. Kapalı konturlar katı gövdeye çevrilir."); status.setTextSize(14); status.setTextColor(Color.rgb(51,65,85)); status.setPadding(0,12,0,10); root.addView(status);
        mesh=new MeshView(this); root.addView(mesh,new LinearLayout.LayoutParams(-1,0,1));
        TextView foot=new TextView(this); foot.setText("Parmakla sürükle: döndür • İki parmak: ölçek"); foot.setTextSize(12); foot.setGravity(Gravity.CENTER); foot.setTextColor(Color.rgb(100,116,139)); root.addView(foot);
        setContentView(root);
        pick.setOnClickListener(v->pick()); make.setOnClickListener(v->make3d()); save.setOnClickListener(v->save());
    }

    Button button(String s){ Button b=new Button(this); b.setText(s); b.setTextSize(11); return b; }

    void pick(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("image/*"); startActivityForResult(i,PICK); }

    @Override protected void onActivityResult(int r,int c,Intent d){ super.onActivityResult(r,c,d); if(c!=RESULT_OK||d==null)return; try{
        if(r==PICK){ Uri u=d.getData(); try(InputStream in=getContentResolver().openInputStream(u)){ source=BitmapFactory.decodeStream(in); } if(source==null)throw new IOException("Görüntü açılamadı"); status.setText("Resim yüklendi: "+source.getWidth()+" × "+source.getHeight()+". 3D OLUŞTUR'a bas."); mesh.setBitmap(source); }
        else if(r==SAVE && pendingStl!=null){ try(OutputStream o=getContentResolver().openOutputStream(d.getData())){ o.write(pendingStl.getBytes("UTF-8")); } status.setText("STL kaydedildi."); }
    }catch(Exception e){ status.setText("Hata: "+e.getMessage()); }}

    void make3d(){ if(source==null){status.setText("Önce teknik resim seç.");return;} new Thread(()->{ try{ boolean[][] s=analyze(source,56,56); solid=s; int n=count(s); runOnUiThread(()->{mesh.setSolid(s); status.setText("3D model hazır • "+n+" hücre • kapalı kontur dolgu aktif");}); }catch(Exception e){runOnUiThread(()->status.setText("Analiz hatası: "+e.getMessage()));}}).start(); }

    boolean[][] analyze(Bitmap bm,int gw,int gh){
        Bitmap b=Bitmap.createScaledBitmap(bm,gw,gh,true); boolean[][] wall=new boolean[gh][gw];
        for(int y=0;y<gh;y++)for(int x=0;x<gw;x++){ int c=b.getPixel(x,y); int g=(Color.red(c)*30+Color.green(c)*59+Color.blue(c)*11)/100; wall[y][x]=g<150; }
        // close small gaps
        boolean[][] w2=new boolean[gh][gw]; for(int y=0;y<gh;y++)for(int x=0;x<gw;x++){int q=0;for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){int yy=y+dy,xx=x+dx;if(yy>=0&&yy<gh&&xx>=0&&xx<gw&&wall[yy][xx])q++;}w2[y][x]=wall[y][x]||q>=4;}
        boolean[][] outside=new boolean[gh][gw]; ArrayDeque<int[]> q=new ArrayDeque<>();
        for(int x=0;x<gw;x++){seed(x,0,w2,outside,q);seed(x,gh-1,w2,outside,q);} for(int y=0;y<gh;y++){seed(0,y,w2,outside,q);seed(gw-1,y,w2,outside,q);}
        int[] dx={1,-1,0,0},dy={0,0,1,-1}; while(!q.isEmpty()){int[] p=q.removeFirst();for(int k=0;k<4;k++){int x=p[0]+dx[k],y=p[1]+dy[k];if(x>=0&&x<gw&&y>=0&&y<gh&&!w2[y][x]&&!outside[y][x]){outside[y][x]=true;q.add(new int[]{x,y});}}}
        boolean[][] s=new boolean[gh][gw]; for(int y=0;y<gh;y++)for(int x=0;x<gw;x++)s[y][x]=w2[y][x]||!outside[y][x];
        // remove tiny noise components indirectly by requiring local support
        boolean[][] out=new boolean[gh][gw]; for(int y=0;y<gh;y++)for(int x=0;x<gw;x++){if(!s[y][x])continue;int n=0;for(int yy=Math.max(0,y-1);yy<=Math.min(gh-1,y+1);yy++)for(int xx=Math.max(0,x-1);xx<=Math.min(gw-1,x+1);xx++)if(s[yy][xx])n++;out[y][x]=n>=2;}
        return out;
    }
    void seed(int x,int y,boolean[][] w,boolean[][] o,ArrayDeque<int[]> q){if(!w[y][x]&&!o[y][x]){o[y][x]=true;q.add(new int[]{x,y});}}
    int count(boolean[][] s){int n=0;for(boolean[] r:s)for(boolean v:r)if(v)n++;return n;}

    void save(){ if(solid==null){status.setText("Önce 3D model oluştur.");return;} pendingStl=toStl(solid); Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("model/stl");i.putExtra(Intent.EXTRA_TITLE,"MG_Drawing2CAD_Model.stl");startActivityForResult(i,SAVE); }

    String toStl(boolean[][] a){StringBuilder s=new StringBuilder("solid MG_Drawing2CAD\n");int h=a.length,w=a[0].length;float z=6f;for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(a[y][x]){face(s,x,y,z,true);face(s,x,y,0,false);if(x==0||!a[y][x-1])sideX(s,x,y,0,z,false);if(x==w-1||!a[y][x+1])sideX(s,x+1,y,0,z,true);if(y==0||!a[y-1][x])sideY(s,x,y,0,z,false);if(y==h-1||!a[y+1][x])sideY(s,x,y+1,0,z,true);}s.append("endsolid MG_Drawing2CAD\n");return s.toString();}
    void tri(StringBuilder s,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz){s.append("facet normal 0 0 0\n outer loop\n");v(s,ax,ay,az);v(s,bx,by,bz);v(s,cx,cy,cz);s.append(" endloop\nendfacet\n");}
    void v(StringBuilder s,float x,float y,float z){s.append("  vertex ").append(x).append(' ').append(y).append(' ').append(z).append('\n');}
    void face(StringBuilder s,float x,float y,float z,boolean top){if(top){tri(s,x,y,z,x+1,y,z,x+1,y+1,z);tri(s,x,y,z,x+1,y+1,z,x,y+1,z);}else{tri(s,x,y,0,x+1,y+1,0,x+1,y,0);tri(s,x,y,0,x,y+1,0,x+1,y+1,0);}}
    void sideX(StringBuilder s,float x,float y,float z0,float z1,boolean pos){if(pos){tri(s,x,y,z0,x,y+1,z0,x,y+1,z1);tri(s,x,y,z0,x,y+1,z1,x,y,z1);}else{tri(s,x,y,z0,x,y+1,z1,x,y+1,z0);tri(s,x,y,z0,x,y,z1,x,y+1,z1);}}
    void sideY(StringBuilder s,float x,float y,float z0,float z1,boolean pos){if(pos){tri(s,x,y,z0,x,y,z1,x+1,y,z1);tri(s,x,y,z0,x+1,y,z1,x+1,y,z0);}else{tri(s,x,y,z0,x+1,y,z1,x,y,z1);tri(s,x,y,z0,x+1,y,z0,x+1,y,z1);}}

    static class MeshView extends View {
        Paint p=new Paint(3), edge=new Paint(3); Bitmap bmp; boolean[][] s; float yaw=.75f,pitch=.65f,scale=7f; float lx,ly; boolean drag;
        MeshView(Context c){super(c);p.setStyle(Paint.Style.FILL);edge.setStyle(Paint.Style.STROKE);edge.setStrokeWidth(1f);edge.setColor(Color.rgb(30,41,59));setBackgroundColor(Color.WHITE);}
        void setBitmap(Bitmap b){bmp=b;s=null;invalidate();} void setSolid(boolean[][] a){s=a;bmp=null;invalidate();}
        protected void onDraw(Canvas c){super.onDraw(c);if(s==null){if(bmp!=null){RectF dst=new RectF(20,20,getWidth()-20,getHeight()-20);c.drawBitmap(bmp,null,dst,p);}return;} c.save();c.translate(getWidth()/2f,getHeight()/2f+70);ArrayList<Cell> cells=new ArrayList<>();for(int y=0;y<s.length;y++)for(int x=0;x<s[0].length;x++)if(s[y][x])cells.add(new Cell(x-s[0].length/2f,y-s.length/2f));Collections.sort(cells,(a,b)->Float.compare(a.x+a.y,b.x+b.y));for(Cell a:cells)drawBlock(c,a.x,a.y,0,1,1,1);c.restore();}
        void drawBlock(Canvas c,float x,float y,float z,float wx,float wy,float wz){float[][] v={{x,y,z},{x+wx,y,z},{x+wx,y+wy,z},{x,y+wy,z},{x,y,z+wz},{x+wx,y,z+wz},{x+wx,y+wy,z+wz},{x,y+wy,z+wz}};PointF[] q=new PointF[8];for(int i=0;i<8;i++)q[i]=proj(v[i][0],v[i][1],v[i][2]);poly(c,q,new int[]{4,5,6,7},Color.rgb(203,213,225));poly(c,q,new int[]{1,2,6,5},Color.rgb(148,163,184));poly(c,q,new int[]{2,3,7,6},Color.rgb(100,116,139));}
        PointF proj(float x,float y,float z){float cy=(float)Math.cos(yaw),sy=(float)Math.sin(yaw),cp=(float)Math.cos(pitch),sp=(float)Math.sin(pitch);float X=x*cy-y*sy,Y=x*sy+y*cy;float Y2=Y*cp-z*sp,Z2=Y*sp+z*cp;return new PointF(X*scale,Y2*scale);}
        void poly(Canvas c,PointF[] q,int[] id,int col){Path path=new Path();path.moveTo(q[id[0]].x,q[id[0]].y);for(int i=1;i<id.length;i++)path.lineTo(q[id[i]].x,q[id[i]].y);path.close();p.setColor(col);c.drawPath(path,p);c.drawPath(path,edge);}
        public boolean onTouchEvent(android.view.MotionEvent e){if(e.getPointerCount()==2){float d=(float)Math.hypot(e.getX(0)-e.getX(1),e.getY(0)-e.getY(1));if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&lastD>0){scale*=d/lastD;scale=Math.max(2f,Math.min(20f,scale));invalidate();}lastD=d;return true;}lastD=0;if(e.getAction()==MotionEvent.ACTION_DOWN){lx=e.getX();ly=e.getY();drag=true;return true;}if(e.getAction()==MotionEvent.ACTION_MOVE&&drag){yaw+=(e.getX()-lx)*.01f;pitch+=(e.getY()-ly)*.01f;pitch=Math.max(-1.3f,Math.min(1.3f,pitch));lx=e.getX();ly=e.getY();invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_UP)drag=false;return true;}float lastD;
        static class Cell{float x,y;Cell(float a,float b){x=a;y=b;}}
    }
}
