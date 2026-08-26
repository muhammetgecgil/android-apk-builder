package com.mgecgil.seslirehber.core;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import java.nio.ByteBuffer;
import static com.mgecgil.seslirehber.core.GuidanceModels.MotionObservation;

public final class VisionMotionAnalyzer implements ImageAnalysis.Analyzer {
    public interface Listener { void onMotion(MotionObservation observation); }
    private static final int GRID_W=48, GRID_H=72;
    private final byte[] previous=new byte[GRID_W*GRID_H], current=new byte[GRID_W*GRID_H];
    private boolean havePrevious;
    private final Listener listener;
    public VisionMotionAnalyzer(Listener listener) { this.listener=listener; }
    @Override public void analyze(@NonNull ImageProxy image) {
        try {
            ImageProxy.PlaneProxy yPlane=image.getPlanes()[0]; ByteBuffer buffer=yPlane.getBuffer(); int width=image.getWidth(), height=image.getHeight(); int rowStride=yPlane.getRowStride(), pixelStride=yPlane.getPixelStride();
            for(int gy=0;gy<GRID_H;gy++){int sy=Math.min(height-1,gy*height/GRID_H);for(int gx=0;gx<GRID_W;gx++){int sx=Math.min(width-1,gx*width/GRID_W);int index=sy*rowStride+sx*pixelStride;current[gy*GRID_W+gx]=buffer.get(index);}}
            if(!havePrevious){System.arraycopy(current,0,previous,0,current.length);havePrevious=true;return;}
            int meanDiff=0;for(int i=0;i<current.length;i++)meanDiff+=Math.abs((current[i]&0xff)-(previous[i]&0xff));meanDiff/=current.length;int threshold=Math.max(16,Math.min(46,meanDiff+12));
            int changed=0;long sumX=0,sumY=0;for(int gy=0;gy<GRID_H;gy++)for(int gx=0;gx<GRID_W;gx++){int i=gy*GRID_W+gx;int d=Math.abs((current[i]&0xff)-(previous[i]&0xff));if(d>threshold){changed++;sumX+=gx;sumY+=gy;}}
            System.arraycopy(current,0,previous,0,current.length);
            float area=changed/(float)current.length;float cx=changed==0?-1f:(sumX/(float)changed)/(GRID_W-1f);float cy=changed==0?-1f:(sumY/(float)changed)/(GRID_H-1f);float confidence=Math.max(0f,Math.min(1f,(area-0.015f)/0.18f));
            listener.onMotion(new MotionObservation(area,cx,cy,confidence,System.currentTimeMillis()));
        } finally { image.close(); }
    }
}
