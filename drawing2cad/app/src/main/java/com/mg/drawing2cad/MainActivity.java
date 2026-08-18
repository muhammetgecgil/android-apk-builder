package com.mg.drawing2cad;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private Bitmap bmp;
    private ModelView view;
    private TextView status;
    private double w=100,h=60,d=20;
    private String pendingStl;
    private static final int PICK=7, SAVE=9;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(18,18,18,18);
        TextView title=new TextView(this); title.setText("MG Drawing2CAD\n2D Teknik Resim → 3D Model"); title.setTextSize(24); root.addView(title);
        LinearLayout bar=new LinearLayout(this);
        Button open=new Button(this); open.setText("Teknik Resim Aç");
        Button make=new Button(this); make.setText("Otomatik 3D");
        Button exp=new Button(this); exp.setText("STL Dışa Aktar");
        bar.addView(open,new LinearLayout.LayoutParams(0,-2,1)); bar.addView(make,new LinearLayout.LayoutParams(0,-2,1)); bar.addView(exp,new LinearLayout.LayoutParams(0,-2,1)); root.addView(bar);
        status=new TextView(this); status.setText("PNG/JPG teknik resmi yükle. Siyah teknik çizgi alanından ana gövde oranları çıkarılır."); root.addView(status);
        view=new ModelView(this); root.addView(view,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
        open.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("image/*"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,PICK);});
        make.setOnClickListener(v->analyze()); exp.setOnClickListener(v->exportStl());
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK || data==null) return;
        if(requestCode==PICK){
            try{ bmp=MediaStore.Images.Media.getBitmap(getContentResolver(),data.getData()); status.setText("Resim yüklendi: "+bmp.getWidth()+"×"+bmp.getHeight()+" px. Otomatik 3D'ye bas."); }
            catch(Exception e){ status.setText("Resim açılamadı: "+e.getMessage()); }
        } else if(requestCode==SAVE && pendingStl!=null){
            try(OutputStream os=getContentResolver().openOutputStream(data.getData())){ os.write(pendingStl.getBytes(StandardCharsets.UTF_8)); status.setText("STL kaydedildi."); }
            catch(Exception e){ status.setText("STL kaydedilemedi: "+e.getMessage()); }
        }
    }

    private void analyze(){
        if(bmp==null){Toast.makeText(this,"Önce teknik resim yükle",Toast.LENGTH_SHORT).show();return;}
        int W=bmp.getWidth(),H=bmp.getHeight(),minx=W,miny=H,maxx=0,maxy=0,count=0;
        int sx=Math.max(1,W/1200), sy=Math.max(1,H/1200);
        for(int y=0;y<H;y+=sy) for(int x=0;x<W;x+=sx){ int c=bmp.getPixel(x,y); int g=(Color.red(c)+Color.green(c)+Color.blue(c))/3; if(g<110){minx=Math.min(minx,x);maxx=Math.max(maxx,x);miny=Math.min(miny,y);maxy=Math.max(maxy,y);count++;}}
        if(count<20){status.setText("Teknik çizgi bulunamadı. Daha net/siyah-beyaz resim dene.");return;}
        double rw=Math.max(1,maxx-minx), rh=Math.max(1,maxy-miny); w=100; h=Math.max(15,100*rh/rw); d=Math.max(8,Math.min(35,h*0.28));
        view.setModel((float)w,(float)h,(float)d); status.setText(String.format("Model üretildi • yaklaşık %.1f × %.1f × %.1f birim • Parmakla döndür.",w,h,d));
    }

    private void exportStl(){
        if(view.mw<=0){Toast.makeText(this,"Önce 3D model üret",Toast.LENGTH_SHORT).show();return;}
        pendingStl=buildStl(); Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.setType("application/octet-stream"); i.putExtra(Intent.EXTRA_TITLE,"MG_Drawing2CAD_"+System.currentTimeMillis()+".stl"); startActivityForResult(i,SAVE);
    }

    private String buildStl(){
        double x=w/2,y=h/2,z=d/2; double[][]v={{-x,-y,-z},{x,-y,-z},{x,y,-z},{-x,y,-z},{-x,-y,z},{x,-y,z},{x,y,z},{-x,y,z}};
        int[][]f={{0,2,1},{0,3,2},{4,5,6},{4,6,7},{0,1,5},{0,5,4},{1,2,6},{1,6,5},{2,3,7},{2,7,6},{3,0,4},{3,4,7}};
        StringBuilder s=new StringBuilder("solid MG_Drawing2CAD\n");
        for(int[]q:f){s.append(" facet normal 0 0 0\n  outer loop\n"); for(int id:q)s.append("   vertex ").append(v[id][0]).append(' ').append(v[id][1]).append(' ').append(v[id][2]).append("\n"); s.append("  endloop\n endfacet\n");}
        return s.append("endsolid MG_Drawing2CAD\n").toString();
    }
}

class ModelView extends View {
    Paint p=new Paint(1); float mw=0,mh=0,md=0,ax=-20,ay=28,lastx,lasty;
    ModelView(android.content.Context c){super(c);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(Color.rgb(25,70,100));setBackgroundColor(Color.rgb(245,247,250));}
    void setModel(float a,float b,float c){mw=a;mh=b;md=c;invalidate();}
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){lastx=e.getX();lasty=e.getY();return true;} if(e.getAction()==MotionEvent.ACTION_MOVE){ay+=(e.getX()-lastx)*.5f;ax+=(e.getY()-lasty)*.5f;lastx=e.getX();lasty=e.getY();invalidate();return true;} return true;}
    @Override protected void onDraw(Canvas c){super.onDraw(c); if(mw<=0){p.setTextSize(38);p.setStyle(Paint.Style.FILL);p.setColor(Color.DKGRAY);c.drawText("3D model burada görünecek",40,getHeight()/2f,p);p.setStyle(Paint.Style.STROKE);p.setColor(Color.rgb(25,70,100));return;}
        float[][] pts={{-mw/2,-mh/2,-md/2},{mw/2,-mh/2,-md/2},{mw/2,mh/2,-md/2},{-mw/2,mh/2,-md/2},{-mw/2,-mh/2,md/2},{mw/2,-mh/2,md/2},{mw/2,mh/2,md/2},{-mw/2,mh/2,md/2}};
        float scale=Math.min(getWidth()/(mw*2.2f),getHeight()/(mh*2.2f)); float[]X=new float[8],Y=new float[8]; double ry=Math.toRadians(ay),rx=Math.toRadians(ax);
        for(int i=0;i<8;i++){double x=pts[i][0],y=pts[i][1],z=pts[i][2];double x1=x*Math.cos(ry)+z*Math.sin(ry),z1=-x*Math.sin(ry)+z*Math.cos(ry),y1=y*Math.cos(rx)-z1*Math.sin(rx);X[i]=(float)(getWidth()/2+x1*scale);Y[i]=(float)(getHeight()/2+y1*scale);} int[][]e={{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}}; for(int[]q:e)c.drawLine(X[q[0]],Y[q[0]],X[q[1]],Y[q[1]],p);
    }
}
