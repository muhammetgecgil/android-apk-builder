package com.muhammetgecgil.pcbzekapro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import com.google.mlkit.vision.text.Text;
import java.util.ArrayList;
import java.util.List;

final class ComponentOverlayView extends View {
    private final Paint box=new Paint(),label=new Paint();
    private final List<Text.TextBlock> blocks=new ArrayList<>();
    private int imageW=1080,imageH=1920;
    ComponentOverlayView(Context c){super(c);box.setStyle(Paint.Style.STROKE);box.setStrokeWidth(4);box.setColor(0xFF4DEEEA);label.setColor(Color.WHITE);label.setTextSize(28);label.setStyle(Paint.Style.FILL);setWillNotDraw(false);}
    void update(Text t,int w,int h){blocks.clear();blocks.addAll(t.getTextBlocks());imageW=Math.max(1,w);imageH=Math.max(1,h);postInvalidate();}
    void clear(){blocks.clear();postInvalidate();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float sx=getWidth()/(float)imageW,sy=getHeight()/(float)imageH;for(Text.TextBlock b:blocks){Rect r=b.getBoundingBox();if(r==null)continue;float l=r.left*sx,t=r.top*sy,rr=r.right*sx,bb=r.bottom*sy;box.setColor(colorFor(b.getText()));c.drawRoundRect(l,t,rr,bb,10,10,box);String s=b.getText().replace('\n',' ');if(s.length()>16)s=s.substring(0,16);c.drawText(s,l,Math.max(28,t-6),label);}}
    private int colorFor(String s){String u=s.toUpperCase();if(u.matches(".*\\bR\\d+.*"))return 0xFFFFC857;if(u.matches(".*\\bC\\d+.*"))return 0xFF4CC9F0;if(u.matches(".*\\bL\\d+.*"))return 0xFF80ED99;if(u.matches(".*\\bU\\d+.*"))return 0xFFFF5D8F;if(u.matches(".*\\b[QD]\\d+.*"))return 0xFFC77DFF;return 0xFF4DEEEA;}
}
