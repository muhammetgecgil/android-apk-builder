package com.muhammetgecgil.nesnesayarai;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import java.util.*;

/**
 * V113: ground-first verification.
 * ROI is only a search mask. Candidate boxes are accepted only when the pixels
 * inside the candidate differ from the immediately surrounding floor/background.
 * Wood grain / scratches / ROI-edge fragments are rejected before counting.
 */
public class MainActivityV9 extends MainActivityV8 {

    @Override void ui() {
        super.ui();
        status.setText("V113 Zemin Ayrıştırma");
    }

    static class Cand {
        RectF box;
        PointF center;
        float score;
        float objectness;
        Cand(RectF b, PointF c, float s, float o) {
            box = new RectF(b); center = c; score = s; objectness = o;
        }
    }

    static class Stats {
        double r,g,b, edge, var;
        int n;
    }

    @Override List<Obj> solve(Bitmap src, List<RectF> raw, List<PointF> rv, int vw, int vh) {
        final int W = src.getWidth(), H = src.getHeight();
        if (rv == null || rv.size() < 3 || raw == null || raw.isEmpty()) return new ArrayList<>();

        // Work on a bounded-resolution image for deterministic mobile performance.
        float sc = Math.min(1f, 720f / Math.max(W,H));
        int w = Math.max(160, Math.round(W*sc));
        int h = Math.max(160, Math.round(H*sc));
        Bitmap sm = Bitmap.createScaledBitmap(src,w,h,true);
        int[] px = new int[w*h];
        sm.getPixels(px,0,w,0,0,w,h);

        // Step 1: merge tile duplicates only. Do NOT turn ROI into one object.
        List<RectF> boxes = new ArrayList<>();
        for (RectF r0 : raw) {
            RectF r = clip(r0,W,H);
            float af = area2(r)/(W*(float)H);
            if (af < 0.00012f || af > 0.34f) continue;
            if (!centerInRoi(r,W,H,rv,vw,vh)) continue;
            if (roiCoverage(r,W,H,rv,vw,vh) < 0.68f) continue;
            boxes.add(r);
        }
        boxes = mergeTileDuplicates(boxes);

        // Step 2: candidate-vs-local-ground test.
        List<Cand> accepted = new ArrayList<>();
        for (RectF r : boxes) {
            RectF sr = scaleRect(r,sc,w,h);
            Stats in = stats(px,w,h,sr,null);
            RectF outer = expand(sr, Math.max(5f,0.18f*Math.min(sr.width(),sr.height())),w,h);
            Stats ring = stats(px,w,h,outer,sr);
            if (in.n < 20 || ring.n < 20) continue;

            double colorDelta = Math.sqrt(sq2(in.r-ring.r)+sq2(in.g-ring.g)+sq2(in.b-ring.b));
            double edgeLift = Math.max(0, in.edge-ring.edge);
            double textureLift = Math.max(0, Math.sqrt(in.var)-Math.sqrt(ring.var));
            double border = borderContrast(px,w,h,sr);

            // True physical objects usually have at least two independent cues.
            int cues = 0;
            if (colorDelta > 15) cues++;
            if (border > 16) cues++;
            if (edgeLift > 9) cues++;
            if (textureLift > 7) cues++;

            float objectness = (float)(colorDelta*1.25 + border*1.35 + edgeLift*0.75 + textureLift*0.45);
            float af = area2(r)/(W*(float)H);
            boolean tiny = af < 0.0010f;

            // Floor grain tends to have weak color separation and no closed-border contrast.
            if (cues < 2) continue;
            if (colorDelta < 9 && border < 20) continue;
            if (tiny && objectness < 42) continue;

            PointF c = weightedCenter(px,w,h,sr,ring,sc);
            if (c == null) c = new PointF(r.centerX(),r.centerY());
            if (!point(mapToView(c.x,c.y,W,H,vw,vh).x,mapToView(c.x,c.y,W,H,vw,vh).y,rv)) continue;
            accepted.add(new Cand(r,c,objectness,objectness));
        }

        // Step 3: remove large group boxes if they contain multiple well-separated objects.
        accepted.sort((a,b)->Float.compare(area2(b.box),area2(a.box)));
        boolean[] drop = new boolean[accepted.size()];
        for (int i=0;i<accepted.size();i++) {
            Cand big=accepted.get(i);
            int children=0;
            ArrayList<PointF> centers=new ArrayList<>();
            for (int j=i+1;j<accepted.size();j++) {
                Cand small=accepted.get(j);
                float ratio=area2(small.box)/Math.max(1f,area2(big.box));
                if (ratio<0.015f || ratio>0.48f) continue;
                if (contain2(small.box,big.box)>0.78f) { children++; centers.add(small.center); }
            }
            if (children>=2 && separated(centers,big.box)) drop[i]=true;
        }

        List<Cand> noGroups=new ArrayList<>();
        for(int i=0;i<accepted.size();i++) if(!drop[i]) noGroups.add(accepted.get(i));

        // Step 4: same physical body may produce several nested ML boxes.
        // Prefer the outer physical body when centers agree; keep separated children as separate objects.
        noGroups.sort((a,b)->Float.compare(b.score,a.score));
        List<Cand> finalC=new ArrayList<>();
        for(Cand c:noGroups){
            boolean duplicate=false;
            for(int i=0;i<finalC.size();i++){
                Cand q=finalC.get(i);
                float ov=iou2(c.box,q.box);
                float d=dist2(c.center,q.center);
                float scale=.24f*(diag2(c.box)+diag2(q.box));
                float contain=Math.max(contain2(c.box,q.box),contain2(q.box,c.box));
                if(ov>0.50f || (contain>0.82f && d<scale)){
                    duplicate=true;
                    // For nested views of one body, retain the larger box if its verification is still strong.
                    if(area2(c.box)>area2(q.box) && c.score>q.score*0.72f) finalC.set(i,c);
                    break;
                }
            }
            if(!duplicate) finalC.add(c);
        }

        // Step 5: reject residual floor fragments near the ROI boundary.
        List<Obj> out=new ArrayList<>();
        for(Cand c:finalC){
            if (nearRoiBoundaryOnly(c.box,W,H,rv,vw,vh) && c.objectness < 58f) continue;
            out.add(new Obj(c.box,c.center,3));
        }

        out.sort((a,b)->{
            int yy=Float.compare(a.center.y,b.center.y);
            return yy!=0?yy:Float.compare(a.center.x,b.center.x);
        });
        return out;
    }

    List<RectF> mergeTileDuplicates(List<RectF> in){
        in.sort((a,b)->Float.compare(area2(b),area2(a)));
        List<RectF> out=new ArrayList<>();
        for(RectF c:in){
            boolean dup=false;
            for(int i=0;i<out.size();i++){
                RectF q=out.get(i);
                float ov=iou2(c,q);
                float d=(float)Math.hypot(c.centerX()-q.centerX(),c.centerY()-q.centerY());
                float sz=.17f*(diag2(c)+diag2(q));
                float ratio=Math.min(area2(c),area2(q))/Math.max(1f,Math.max(area2(c),area2(q)));
                if(ov>.58f || (d<sz && ratio>.58f)){
                    RectF u=new RectF(Math.min(c.left,q.left),Math.min(c.top,q.top),Math.max(c.right,q.right),Math.max(c.bottom,q.bottom));
                    // Avoid union-growing unrelated neighboring objects.
                    if(area2(u)<1.38f*Math.max(area2(c),area2(q))) out.set(i,u);
                    dup=true; break;
                }
            }
            if(!dup) out.add(new RectF(c));
        }
        return out;
    }

    float roiCoverage(RectF r,int W,int H,List<PointF> rv,int vw,int vh){
        int inside=0,total=0;
        for(int yy=1;yy<=5;yy++) for(int xx=1;xx<=5;xx++){
            float x=r.left+r.width()*xx/6f, y=r.top+r.height()*yy/6f; total++;
            if(pip(x,y,W,H,rv,vw,vh)) inside++;
        }
        return inside/(float)total;
    }

    boolean nearRoiBoundaryOnly(RectF r,int W,int H,List<PointF> rv,int vw,int vh){
        PointF cv=mapToView(r.centerX(),r.centerY(),W,H,vw,vh);
        float min=Float.MAX_VALUE;
        for(int i=0;i<rv.size();i++){
            PointF a=rv.get(i), b=rv.get((i+1)%rv.size());
            min=Math.min(min,segDist(cv.x,cv.y,a.x,a.y,b.x,b.y));
        }
        float rvDiag=(float)Math.hypot(vw,vh);
        return min < rvDiag*0.018f;
    }

    float segDist(float px,float py,float ax,float ay,float bx,float by){
        float dx=bx-ax,dy=by-ay,l2=dx*dx+dy*dy;
        if(l2<1e-4f)return (float)Math.hypot(px-ax,py-ay);
        float t=Math.max(0,Math.min(1,((px-ax)*dx+(py-ay)*dy)/l2));
        return (float)Math.hypot(px-(ax+t*dx),py-(ay+t*dy));
    }

    boolean separated(List<PointF> c,RectF big){
        float th=.24f*Math.min(big.width(),big.height());
        for(int i=0;i<c.size();i++)for(int j=i+1;j<c.size();j++)if(dist2(c.get(i),c.get(j))>th)return true;
        return false;
    }

    PointF weightedCenter(int[] px,int w,int h,RectF r,Stats bg,float invScale){
        double sx=0,sy=0,sw=0;
        int l=Math.max(1,(int)r.left),t=Math.max(1,(int)r.top),rr=Math.min(w-2,(int)r.right),bb=Math.min(h-2,(int)r.bottom);
        for(int y=t;y<=bb;y+=2)for(int x=l;x<=rr;x+=2){
            int c=px[y*w+x];
            double d=Math.sqrt(sq2(Color.red(c)-bg.r)+sq2(Color.green(c)-bg.g)+sq2(Color.blue(c)-bg.b));
            if(d<11)continue;
            double wt=Math.min(80,d);
            sx+=x*wt;sy+=y*wt;sw+=wt;
        }
        if(sw<1)return null;
        // invScale argument is actually image scale; convert small-image coords to source coords.
        return new PointF((float)(sx/sw/invScale),(float)(sy/sw/invScale));
    }

    Stats stats(int[] px,int w,int h,RectF outer,RectF hole){
        Stats s=new Stats();
        double sr=0,sg=0,sb=0,s2=0,se=0;
        int l=Math.max(1,(int)Math.floor(outer.left)),t=Math.max(1,(int)Math.floor(outer.top));
        int r=Math.min(w-2,(int)Math.ceil(outer.right)),b=Math.min(h-2,(int)Math.ceil(outer.bottom));
        for(int y=t;y<=b;y+=2)for(int x=l;x<=r;x+=2){
            if(hole!=null && x>=hole.left&&x<=hole.right&&y>=hole.top&&y<=hole.bottom)continue;
            int c=px[y*w+x],R=Color.red(c),G=Color.green(c),B=Color.blue(c);
            double lum=.299*R+.587*G+.114*B;
            sr+=R;sg+=G;sb+=B;s2+=lum*lum;
            se+=(rgbDiff(px[y*w+x-1],px[y*w+x+1])+rgbDiff(px[(y-1)*w+x],px[(y+1)*w+x]))*.5;
            s.n++;
        }
        if(s.n>0){s.r=sr/s.n;s.g=sg/s.n;s.b=sb/s.n;double mean=.299*s.r+.587*s.g+.114*s.b;s.var=Math.max(0,s2/s.n-mean*mean);s.edge=se/s.n;}
        return s;
    }

    double borderContrast(int[] px,int w,int h,RectF r){
        int l=Math.max(2,(int)r.left),t=Math.max(2,(int)r.top),rr=Math.min(w-3,(int)r.right),bb=Math.min(h-3,(int)r.bottom);
        if(rr-l<4||bb-t<4)return 0;
        double sum=0;int n=0;
        for(int x=l;x<=rr;x+=3){sum+=rgbDiff(px[t*w+x],px[(t-2)*w+x]);sum+=rgbDiff(px[bb*w+x],px[(bb+2)*w+x]);n+=2;}
        for(int y=t;y<=bb;y+=3){sum+=rgbDiff(px[y*w+l],px[y*w+l-2]);sum+=rgbDiff(px[y*w+rr],px[y*w+rr+2]);n+=2;}
        return n==0?0:sum/n;
    }

    RectF scaleRect(RectF r,float sc,int w,int h){return new RectF(clamp(r.left*sc,0,w-1),clamp(r.top*sc,0,h-1),clamp(r.right*sc,1,w),clamp(r.bottom*sc,1,h));}
    RectF expand(RectF r,float p,int w,int h){return new RectF(clamp(r.left-p,0,w-1),clamp(r.top-p,0,h-1),clamp(r.right+p,1,w),clamp(r.bottom+p,1,h));}
    RectF clip(RectF r,int W,int H){return new RectF(clamp(r.left,0,W-1),clamp(r.top,0,H-1),clamp(r.right,1,W),clamp(r.bottom,1,H));}
    float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
    float area2(RectF r){return Math.max(0,r.width())*Math.max(0,r.height());}
    float diag2(RectF r){return (float)Math.hypot(r.width(),r.height());}
    float iou2(RectF a,RectF b){float l=Math.max(a.left,b.left),t=Math.max(a.top,b.top),r=Math.min(a.right,b.right),bt=Math.min(a.bottom,b.bottom);float in=Math.max(0,r-l)*Math.max(0,bt-t);return in/Math.max(1f,area2(a)+area2(b)-in);}
    float contain2(RectF a,RectF b){float l=Math.max(a.left,b.left),t=Math.max(a.top,b.top),r=Math.min(a.right,b.right),bt=Math.min(a.bottom,b.bottom);float in=Math.max(0,r-l)*Math.max(0,bt-t);return in/Math.max(1f,area2(a));}
    float dist2(PointF a,PointF b){return (float)Math.hypot(a.x-b.x,a.y-b.y);}
    double sq2(double x){return x*x;}
    int rgbDiff(int a,int b){return (Math.abs(Color.red(a)-Color.red(b))+Math.abs(Color.green(a)-Color.green(b))+Math.abs(Color.blue(a)-Color.blue(b)))/3;}
}
