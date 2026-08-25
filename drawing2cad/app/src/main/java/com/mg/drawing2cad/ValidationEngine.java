package com.mg.drawing2cad;

import java.util.*;

public final class ValidationEngine {
  public static final class Result{
    public final int confidence;public final int quality;public final ArrayList<String> warnings;
    Result(int c,int q,ArrayList<String>w){confidence=c;quality=q;warnings=w;}
    public String compact(){return "GÜVEN %"+confidence+" • KALİTE %"+quality+(warnings.isEmpty()?" • DOĞRULANDI":" • "+warnings.size()+" UYARI");}
  }
  public static Result validate3d(int triangles,float[] b){ArrayList<String>w=new ArrayList<>();int c=96,q=95;if(triangles<12){w.add("Çok az üçgen");c-=25;q-=20;}if(b==null||b.length<6){w.add("Bounding box yok");c-=20;q-=20;}else{float x=b[3]-b[0],y=b[4]-b[1],z=b[5]-b[2];if(x<=0||y<=0||z<=0){w.add("Sıfır boyutlu eksen");c-=30;q-=30;}float max=Math.max(x,Math.max(y,z)),min=Math.min(x,Math.min(y,z));if(max>0&&min/max<0.0001f){w.add("Aşırı ince geometri");q-=10;}}
    return new Result(clamp(c),clamp(q),w);
  }
  public static Result validate2d(int width,int height,boolean hasFront,boolean hasTop,boolean hasRight){ArrayList<String>w=new ArrayList<>();int c=98,q=96;if(width<600||height<600){w.add("Düşük çözünürlük");c-=15;q-=15;}if(!hasFront){w.add("Ön görünüş belirsiz");c-=25;}if(!hasTop){w.add("Üst görünüş belirsiz");c-=25;}if(!hasRight){w.add("Sağ görünüş belirsiz");c-=25;}return new Result(clamp(c),clamp(q),w);}
  static int clamp(int x){return Math.max(0,Math.min(100,x));}
  private ValidationEngine(){}
}
