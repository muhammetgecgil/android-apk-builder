package com.muhammetgecgil.pcbzekapro;

import androidx.camera.core.ImageProxy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

final class ResistorBandAnalyzer {
    static final class Result { final String bands,value; final int confidence; Result(String b,String v,int c){bands=b;value=v;confidence=c;} }
    private static final String[] N={"siyah","kahverengi","kırmızı","turuncu","sarı","yeşil","mavi","mor","gri","beyaz","altın"};
    private static final double[][] C={{20,20,20},{95,50,30},{190,35,35},{230,105,25},{225,205,35},{45,135,65},{45,80,175},{125,55,145},{125,125,125},{220,220,210},{180,145,55}};

    static Result analyze(ImageProxy im){
        int w=im.getWidth(),h=im.getHeight(); List<Integer> raw=new ArrayList<>();
        for(int x=w*30/100;x<w*70/100;x+=Math.max(2,w/180)){
            double[] rgb=sample(im,x,h/2,w/100,h/30); int idx=nearest(rgb);
            if(raw.isEmpty()||raw.get(raw.size()-1)!=idx) raw.add(idx);
        }
        List<Integer> b=new ArrayList<>(); for(int v:raw) if(v!=10&&(b.isEmpty()||b.get(b.size()-1)!=v)) b.add(v);
        if(b.size()>6)b=new ArrayList<>(b.subList((b.size()-6)/2,(b.size()-6)/2+6));
        if(b.size()<3)return new Result("Bant seçilemedi","Direnci yatay hizalayın; flaşsız/flaşlı tekrar deneyin",35);
        int count=Math.min(5,b.size()); StringBuilder names=new StringBuilder(); for(int i=0;i<count;i++){if(i>0)names.append("-");names.append(N[b.get(i)]);}
        int digits=count>=5?3:2; double val=0; for(int i=0;i<digits;i++)val=val*10+Math.min(9,b.get(i)); val*=Math.pow(10,Math.min(9,b.get(digits)));
        String value=(val>=1e6?fmt(val/1e6)+" MΩ":val>=1e3?fmt(val/1e3)+" kΩ":fmt(val)+" Ω")+(count>=4?" ±"+tol(b.get(count-1)):"");
        return new Result(names.toString(),value,Math.min(91,58+count*6));
    }
    private static double[] sample(ImageProxy im,int cx,int cy,int rx,int ry){ double r=0,g=0,b=0,n=0; for(int y=cy-ry;y<=cy+ry;y+=3)for(int x=cx-rx;x<=cx+rx;x+=3){double[] p=yuv(im,x,y);r+=p[0];g+=p[1];b+=p[2];n++;}return new double[]{r/n,g/n,b/n}; }
    private static double[] yuv(ImageProxy im,int x,int y){ImageProxy.PlaneProxy[] p=im.getPlanes();int Y=u(p[0],x,y);int U=u(p[1],x/2,y/2)-128;int V=u(p[2],x/2,y/2)-128;return new double[]{cl(Y+1.403*V),cl(Y-.344*U-.714*V),cl(Y+1.770*U)};}
    private static int u(ImageProxy.PlaneProxy p,int x,int y){ByteBuffer b=p.getBuffer();int i=y*p.getRowStride()+x*p.getPixelStride();return i<b.limit()?b.get(i)&255:128;}
    private static double cl(double v){return Math.max(0,Math.min(255,v));}
    private static int nearest(double[] p){int best=0;double bd=1e20;for(int i=0;i<C.length;i++){double d=0;for(int j=0;j<3;j++)d+=(p[j]-C[i][j])*(p[j]-C[i][j]);if(d<bd){bd=d;best=i;}}return best;}
    private static String tol(int i){return i==1?"1%":i==2?"2%":i==5?"0.5%":i==6?"0.25%":i==7?"0.1%":i==8?"0.05%":i==10?"5%":"10%";}
    private static String fmt(double v){return v==Math.rint(v)?String.format("%.0f",v):String.format("%.2f",v);}
}
