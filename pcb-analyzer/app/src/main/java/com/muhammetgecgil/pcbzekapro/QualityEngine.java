package com.muhammetgecgil.pcbzekapro;

import androidx.camera.core.ImageProxy;
import java.nio.ByteBuffer;
import java.util.*;

final class QualityEngine {
    static final class Score { final int sharpness,exposure,total; final String advice; Score(int s,int e,int t,String a){sharpness=s;exposure=e;total=t;advice=a;} }
    static Score score(ImageProxy im){
        ImageProxy.PlaneProxy p=im.getPlanes()[0];ByteBuffer b=p.getBuffer();int w=im.getWidth(),h=im.getHeight(),row=p.getRowStride();long sum=0,grad=0,n=0;
        for(int y=h/4;y<h*3/4;y+=8)for(int x=w/4;x<w*3/4;x+=8){int i=y*row+x;if(i>=b.limit())continue;int v=b.get(i)&255;sum+=v;n++;if(x+8<w&&i+8<b.limit())grad+=Math.abs(v-(b.get(i+8)&255));}
        int mean=n==0?0:(int)(sum/n),g=n==0?0:(int)(grad/n);int sharp=Math.min(100,g*6),exp=Math.max(0,100-Math.abs(mean-128)*100/128),total=(sharp*65+exp*35)/100;
        String a=mean>210?"Flaş yansıması/fazla pozlama: açıyı değiştirin":mean<45?"Görüntü karanlık: ışık veya flaş kullanın":sharp<45?"Netlik düşük: hedefe dokunun ve telefonu sabitleyin":"Görüntü analiz için uygun";
        return new Score(sharp,exp,total,a);
    }
    static String consensus(String raw){
        Map<String,Integer> count=new LinkedHashMap<>();for(String line:raw.toUpperCase(Locale.ROOT).split("\\s+"))if(line.length()>=2&&line.length()<=16&&line.matches("[A-Z0-9.RKM-]+"))count.put(line,count.getOrDefault(line,0)+1);
        StringBuilder s=new StringBuilder();for(Map.Entry<String,Integer>e:count.entrySet())if(e.getValue()>=2)s.append(e.getKey()).append(' ');return s.length()==0?raw:s.toString();
    }
}
