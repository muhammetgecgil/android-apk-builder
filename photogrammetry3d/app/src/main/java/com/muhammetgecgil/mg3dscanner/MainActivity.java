package com.muhammetgecgil.mg3dscanner;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int REQ_IMAGES = 10, REQ_VIDEO = 11;
    private final ArrayList<Bitmap> frames = new ArrayList<>();
    private TextView status;
    private LinearLayout root;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(16,17,20));
        buildUi();
    }

    private TextView text(String s, int sp) {
        TextView v = new TextView(this); v.setText(s); v.setTextColor(Color.WHITE); v.setTextSize(sp); v.setPadding(18,12,18,12); return v;
    }

    private Button button(String s) {
        Button b = new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(16); b.setMinHeight(56); return b;
    }

    private void buildUi() {
        ScrollView sc = new ScrollView(this); root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20,24,20,30); root.setBackgroundColor(Color.rgb(16,17,20)); sc.addView(root);
        TextView title=text("MG 3D SCANNER",28); title.setGravity(Gravity.CENTER); root.addView(title);
        TextView sub=text("Fotoğraf + video fotogrametri • cihaz üzerinde",14); sub.setGravity(Gravity.CENTER); sub.setTextColor(Color.LTGRAY); root.addView(sub);
        Space sp=new Space(this); root.addView(sp,new LinearLayout.LayoutParams(1,20));
        Button photos=button("📷 Fotoğrafları seç"); photos.setOnClickListener(v->pickImages()); root.addView(photos);
        Button video=button("🎥 Video seç / keyframe çıkar"); video.setOnClickListener(v->pickVideo()); root.addView(video);
        Button clear=button("🗑 Çekimi temizle"); clear.setOnClickListener(v->{frames.clear(); updateStatus("Hazır. Fotoğraf veya video ekle.");}); root.addView(clear);
        status=text("Hazır. En az iki farklı açı ekle.",16); status.setBackgroundColor(Color.rgb(35,37,43)); status.setPadding(24,24,24,24); root.addView(status,new LinearLayout.LayoutParams(-1,-2));
        Button build=button("🧊 3D MODEL OLUŞTUR"); build.setOnClickListener(v->reconstruct()); root.addView(build);
        TextView info=text("Motor: çoklu görünüm kare seçimi → köşe/patch eşleştirme → disparity/derinlik → nokta bulutu → OBJ. İyi sonuç için nesnenin çevresinde %70+ örtüşmeli, net ve aynı pozlamaya yakın görüntüler çek.",13); info.setTextColor(Color.LTGRAY); root.addView(info);
        setContentView(sc);
    }

    private void updateStatus(String s){ runOnUiThread(()->status.setText(s)); }

    private void pickImages(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("image/*"); i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,REQ_IMAGES);
    }

    private void pickVideo(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("video/*"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,REQ_VIDEO);
    }

    @Override protected void onActivityResult(int r,int c,Intent d){ super.onActivityResult(r,c,d); if(c!=RESULT_OK||d==null)return;
        new Thread(()->{ try {
            if(r==REQ_IMAGES){
                if(d.getClipData()!=null){ for(int k=0;k<d.getClipData().getItemCount();k++) addBitmap(d.getClipData().getItemAt(k).getUri()); }
                else if(d.getData()!=null) addBitmap(d.getData());
            } else if(r==REQ_VIDEO && d.getData()!=null){ extractVideoFrames(d.getData()); }
            updateStatus(frames.size()+" kare hazır. "+(frames.size()>=2?"Model oluşturabilirsin.":"En az 2 kare gerekli."));
        }catch(Exception e){updateStatus("Girdi okunamadı: "+e.getMessage());} }).start();
    }

    private void addBitmap(Uri u)throws Exception{
        Bitmap b=MediaStore.Images.Media.getBitmap(getContentResolver(),u); if(b!=null) frames.add(scale(b,960));
    }

    private Bitmap scale(Bitmap b,int max){ int w=b.getWidth(),h=b.getHeight(); if(Math.max(w,h)<=max)return b; float f=max/(float)Math.max(w,h); return Bitmap.createScaledBitmap(b,(int)(w*f),(int)(h*f),true); }

    private void extractVideoFrames(Uri u)throws Exception{
        MediaMetadataRetriever m=new MediaMetadataRetriever(); m.setDataSource(this,u); long dur=Long.parseLong(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))*1000L; int n=12;
        for(int k=0;k<n;k++){ long t=(long)((k+0.5)*dur/n); Bitmap b=m.getFrameAtTime(t,MediaMetadataRetriever.OPTION_CLOSEST_SYNC); if(b!=null && isSharp(b)) frames.add(scale(b,960)); }
        m.release();
    }

    private boolean isSharp(Bitmap b){ Bitmap s=scale(b,240); long score=0; int n=0; for(int y=2;y<s.getHeight()-2;y+=3)for(int x=2;x<s.getWidth()-2;x+=3){int c=gray(s.getPixel(x,y));int lap=Math.abs(4*c-gray(s.getPixel(x-1,y))-gray(s.getPixel(x+1,y))-gray(s.getPixel(x,y-1))-gray(s.getPixel(x,y+1)));score+=lap;n++;} return n>0 && score/n>7; }
    private int gray(int c){return (Color.red(c)*30+Color.green(c)*59+Color.blue(c)*11)/100;}

    private void reconstruct(){ if(frames.size()<2){updateStatus("En az iki farklı açı gerekli.");return;} new Thread(()->{
        try{
            updateStatus("1/4 Görüntüler hazırlanıyor…"); Bitmap a=scale(frames.get(0),640), b=scale(frames.get(1),640);
            updateStatus("2/4 Özellik ve patch eşleştirme…"); ArrayList<float[]> pts=StereoReconstructor.reconstruct(a,b);
            if(pts.size()<40){updateStatus("Yeterli ortak özellik bulunamadı ("+pts.size()+"). Daha fazla örtüşen açı çek.");return;}
            updateStatus("3/4 "+pts.size()+" adet 3B nokta üretildi…"); Uri uri=saveObj(pts);
            updateStatus("4/4 TAMAMLANDI\n"+pts.size()+" nokta\nOBJ kaydedildi: Downloads/MG3DScanner\n"+uri);
        }catch(Exception e){updateStatus("Model oluşturma hatası: "+e.getClass().getSimpleName()+" • "+e.getMessage());}
    }).start(); }

    private Uri saveObj(ArrayList<float[]> pts)throws Exception{
        ContentValues cv=new ContentValues(); cv.put(MediaStore.Downloads.DISPLAY_NAME,"MG3D_"+System.currentTimeMillis()+".obj"); cv.put(MediaStore.Downloads.MIME_TYPE,"text/plain"); cv.put(MediaStore.Downloads.RELATIVE_PATH,"Download/MG3DScanner");
        Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cv); if(u==null)throw new IOException("Dosya oluşturulamadı");
        try(Writer w=new BufferedWriter(new OutputStreamWriter(getContentResolver().openOutputStream(u)))){ w.write("# MG 3D Scanner point cloud OBJ\n"); for(float[]p:pts)w.write(String.format(Locale.US,"v %.6f %.6f %.6f\n",p[0],p[1],p[2])); }
        return u;
    }

    static class StereoReconstructor {
        static ArrayList<float[]> reconstruct(Bitmap A,Bitmap B){
            int w=Math.min(A.getWidth(),B.getWidth()),h=Math.min(A.getHeight(),B.getHeight()); A=Bitmap.createScaledBitmap(A,w,h,true);B=Bitmap.createScaledBitmap(B,w,h,true);
            int step=6, rad=3, search=Math.min(80,w/5); ArrayList<float[]> out=new ArrayList<>();
            for(int y=rad+2;y<h-rad-2;y+=step){ for(int x=rad+search+2;x<w-rad-2;x+=step){
                int gx=Math.abs(g(A,x+1,y)-g(A,x-1,y))+Math.abs(g(A,x,y+1)-g(A,x,y-1)); if(gx<45)continue;
                int bestX=-1; long best=Long.MAX_VALUE,second=Long.MAX_VALUE;
                for(int xb=Math.max(rad+1,x-search);xb<=Math.min(w-rad-2,x+12);xb+=2){ long e=0; for(int yy=-rad;yy<=rad;yy++)for(int xx=-rad;xx<=rad;xx++){int d=g(A,x+xx,y+yy)-g(B,xb+xx,y+yy);e+=Math.abs(d);} if(e<best){second=best;best=e;bestX=xb;}else if(e<second)second=e; }
                if(bestX<0||best>1800||best*100L>second*92L)continue; float disp=x-bestX; if(Math.abs(disp)<2)continue; float z=120f/Math.abs(disp); float X=(x-w/2f)*z/w; float Y=-(y-h/2f)*z/w; if(z<0.5f||z>60f)continue; out.add(new float[]{X,Y,z});
            }} return out;
        }
        static int g(Bitmap b,int x,int y){int c=b.getPixel(x,y);return (Color.red(c)*30+Color.green(c)*59+Color.blue(c)*11)/100;}
    }
}
