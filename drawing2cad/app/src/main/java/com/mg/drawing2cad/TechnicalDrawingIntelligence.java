package com.mg.drawing2cad;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.util.*;
import java.util.regex.*;

/** Lightweight offline technical-drawing intelligence layer.
 *  It does not invent dimensions: it reports confidence and candidates.
 */
public final class TechnicalDrawingIntelligence {
  private TechnicalDrawingIntelligence() {}

  public static final class Feature {
    public final String type;
    public final float cx, cy, size, confidence;
    public final String note;
    Feature(String t,float x,float y,float s,float c,String n){type=t;cx=x;cy=y;size=s;confidence=c;note=n;}
  }

  public static final class Report {
    public final ArrayList<Feature> features=new ArrayList<>();
    public final ArrayList<String> callouts=new ArrayList<>();
    public float geometryConfidence;
    public int darkPixelCount, componentCount, circularCandidates, longLineCandidates;
    public String summary(){
      return String.format(Locale.US,
        "Güven %.0f%% • %d geometri bileşeni • %d delik/daire adayı • %d uzun çizgi",
        geometryConfidence*100f, componentCount, circularCandidates, longLineCandidates);
    }
  }

  public static Report analyze(Bitmap src){
    Report r=new Report();
    if(src==null)return r;
    int target=520;
    float k=Math.min(1f,target/(float)Math.max(src.getWidth(),src.getHeight()));
    int w=Math.max(32,Math.round(src.getWidth()*k)), h=Math.max(32,Math.round(src.getHeight()*k));
    Bitmap b=Bitmap.createScaledBitmap(src,w,h,true);
    boolean[][] ink=new boolean[h][w];
    for(int y=0;y<h;y++)for(int x=0;x<w;x++){
      int c=b.getPixel(x,y);int g=(Color.red(c)*30+Color.green(c)*59+Color.blue(c)*11)/100;
      ink[y][x]=g<105;if(ink[y][x])r.darkPixelCount++;
    }
    boolean[][] seen=new boolean[h][w];
    int[][] d={{1,0},{-1,0},{0,1},{0,-1}};
    for(int sy=0;sy<h;sy++)for(int sx=0;sx<w;sx++)if(ink[sy][sx]&&!seen[sy][sx]){
      ArrayDeque<int[]> q=new ArrayDeque<>();q.add(new int[]{sx,sy});seen[sy][sx]=true;
      int n=0,minx=sx,maxx=sx,miny=sy,maxy=sy,border=0;
      while(!q.isEmpty()){
        int[] p=q.removeFirst();n++;int x=p[0],y=p[1];minx=Math.min(minx,x);maxx=Math.max(maxx,x);miny=Math.min(miny,y);maxy=Math.max(maxy,y);
        int neigh=0;for(int[] dd:d){int xx=x+dd[0],yy=y+dd[1];if(xx>=0&&xx<w&&yy>=0&&yy<h&&ink[yy][xx])neigh++;}
        if(neigh<=2)border++;
        for(int[] dd:d){int xx=x+dd[0],yy=y+dd[1];if(xx>=0&&xx<w&&yy>=0&&yy<h&&ink[yy][xx]&&!seen[yy][xx]){seen[yy][xx]=true;q.add(new int[]{xx,yy});}}
      }
      if(n<8)continue;r.componentCount++;
      int bw=maxx-minx+1,bh=maxy-miny+1;float aspect=bw/(float)Math.max(1,bh);float fill=n/(float)(bw*bh);
      if(bw>8&&bh>8&&aspect>.65f&&aspect<1.55f&&fill<.55f){
        float conf=.55f+Math.min(.35f,.35f*(1f-Math.abs(1f-aspect)));
        r.circularCandidates++;r.features.add(new Feature("HOLE_OR_CIRCLE",(minx+maxx)/2f/w,(miny+maxy)/2f/h,Math.max(bw,bh)/(float)Math.max(w,h),conf,"Kapalı/daire benzeri kontur"));
      }
      if((bw>w*.16f&&bh<=5)||(bh>h*.16f&&bw<=5)){
        r.longLineCandidates++;r.features.add(new Feature("LONG_LINE",(minx+maxx)/2f/w,(miny+maxy)/2f/h,Math.max(bw,bh)/(float)Math.max(w,h),.72f,"Görünür/ölçü/merkez çizgisi adayı"));
      }
    }
    float density=r.darkPixelCount/(float)(w*h);
    float compScore=Math.min(1f,r.componentCount/12f);
    float densityScore=(density>.003f&&density<.28f)?1f:.55f;
    r.geometryConfidence=Math.max(.15f,Math.min(.96f,.30f+.35f*compScore+.35f*densityScore));
    return r;
  }

  /** Parses already-extracted technical callout text. No value is fabricated. */
  public static ArrayList<String> parseCallouts(Collection<String> tokens){
    ArrayList<String> out=new ArrayList<>();if(tokens==null)return out;
    Pattern hole=Pattern.compile("(?i)(\\d+\\s*[x×]\\s*)?[ØD]\\s*\\d+(?:[.,]\\d+)?(?:\\s*THRU)?");
    Pattern radius=Pattern.compile("(?i)R\\s*\\d+(?:[.,]\\d+)?");
    Pattern thread=Pattern.compile("(?i)M\\s*\\d+(?:[.,]\\d+)?(?:\\s*[x×]\\s*\\d+(?:[.,]\\d+)?)?");
    Pattern chamfer=Pattern.compile("(?i)C\\s*\\d+(?:[.,]\\d+)?(?:\\s*[x×]\\s*45°?)?");
    Pattern tol=Pattern.compile(".*(?:±|\\+\\s*\\d|H[5-9]|[fg][5-9]).*");
    for(String raw:tokens){if(raw==null)continue;String s=raw.trim();if(hole.matcher(s).find())out.add("HOLE: "+s);else if(radius.matcher(s).find())out.add("RADIUS: "+s);else if(thread.matcher(s).find())out.add("THREAD: "+s);else if(chamfer.matcher(s).find())out.add("CHAMFER: "+s);else if(tol.matcher(s).matches())out.add("TOLERANCE: "+s);}
    return out;
  }
}
