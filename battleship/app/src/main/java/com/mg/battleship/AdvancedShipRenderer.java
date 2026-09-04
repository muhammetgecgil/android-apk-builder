package com.mg.battleship;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

final class AdvancedShipRenderer {
    static final String[] NAMES={"UÇAK GEMİSİ","SAVAŞ GEMİSİ","KRUVAZÖR","DESTROYER","DENİZALTI"};
    private AdvancedShipRenderer(){}
    static String name(int id){return id>=0&&id<NAMES.length?NAMES[id]:"GEMİ";}

    static void draw(Canvas c, Paint p, float left, float top, float cell, int[][] own, int id){
        int minR=99,maxR=-1,minC=99,maxC=-1;
        for(int r=0;r<10;r++)for(int col=0;col<10;col++)if(own[r][col]==id){minR=Math.min(minR,r);maxR=Math.max(maxR,r);minC=Math.min(minC,col);maxC=Math.max(maxC,col);}
        if(maxR<0)return;
        boolean horizontal=minR==maxR;
        float x0=left+minC*cell,y0=top+minR*cell,x1=left+(maxC+1)*cell,y1=top+(maxR+1)*cell;
        float cx=(x0+x1)/2f, cy=(y0+y1)/2f;
        c.save(); if(!horizontal)c.rotate(90,cx,cy);
        float L=horizontal?(x1-x0):(y1-y0), H=cell*.84f, sx=cx-L/2f, ex=cx+L/2f, water=cy+H*.24f;
        shadow(c,p,sx,ex,water,H);
        if(id==0) carrier(c,p,sx,ex,water,H,L);
        else if(id==1) battleship(c,p,sx,ex,water,H,L);
        else if(id==2) cruiser(c,p,sx,ex,water,H,L);
        else if(id==3) destroyer(c,p,sx,ex,water,H,L);
        else submarine(c,p,sx,ex,water,H,L);
        c.restore();
    }

    private static void shadow(Canvas c,Paint p,float sx,float ex,float water,float H){
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(85,0,0,0));
        c.drawOval(new RectF(sx+H*.15f,water-H*.03f,ex-H*.08f,water+H*.20f),p);
    }
    private static void hull(Canvas c,Paint p,float sx,float ex,float water,float H,float bow,float stern,int topColor,int bottomColor){
        Path h=new Path();
        h.moveTo(sx+stern,water-H*.22f);h.lineTo(ex-bow,water-H*.22f);h.lineTo(ex,water-H*.08f);h.lineTo(ex-bow*.62f,water+H*.16f);h.lineTo(sx+stern*.65f,water+H*.18f);h.lineTo(sx,water-H*.03f);h.close();
        p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(0,water-H*.3f,0,water+H*.2f,topColor,bottomColor,Shader.TileMode.CLAMP));c.drawPath(h,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(H*.035f);p.setColor(Color.rgb(205,216,221));c.drawPath(h,p);p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(46,58,65));c.drawRect(sx+stern*.65f,water-H*.27f,ex-bow*.68f,water-H*.20f,p);
    }
    private static void turret(Canvas c,Paint p,float x,float y,float s,float barrel){
        p.setColor(Color.rgb(80,91,97));c.drawRoundRect(new RectF(x-s*.28f,y-s*.18f,x+s*.28f,y+s*.18f),s*.08f,s*.08f,p);
        p.setColor(Color.rgb(35,43,47));p.setStrokeWidth(s*.08f);p.setStyle(Paint.Style.STROKE);c.drawLine(x+s*.18f,y,x+barrel,y,p);p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(170,181,184));c.drawCircle(x,y,s*.09f,p);
    }
    private static void mast(Canvas c,Paint p,float x,float base,float H){
        p.setColor(Color.rgb(175,187,191));p.setStrokeWidth(H*.035f);p.setStyle(Paint.Style.STROKE);c.drawLine(x,base,x,base-H*.42f,p);c.drawLine(x-H*.13f,base-H*.30f,x+H*.14f,base-H*.30f,p);c.drawLine(x-H*.09f,base-H*.19f,x+H*.11f,base-H*.19f,p);p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(74,196,220));c.drawCircle(x,base-H*.42f,H*.045f,p);
    }
    private static void carrier(Canvas c,Paint p,float sx,float ex,float w,float H,float L){
        hull(c,p,sx,ex,w,H,H*.34f,H*.15f,Color.rgb(112,126,132),Color.rgb(48,58,64));
        p.setColor(Color.rgb(72,80,84));c.drawRoundRect(new RectF(sx+L*.06f,w-H*.49f,ex-L*.03f,w-H*.25f),H*.055f,H*.055f,p);
        p.setColor(Color.rgb(228,232,232));p.setStrokeWidth(H*.024f);p.setStyle(Paint.Style.STROKE);c.drawLine(sx+L*.14f,w-H*.37f,ex-L*.12f,w-H*.37f,p);p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(157,166,168));c.drawRect(sx+L*.65f,w-H*.68f,sx+L*.78f,w-H*.49f,p);mast(c,p,sx+L*.72f,w-H*.68f,H*.72f);
        p.setColor(Color.rgb(35,39,42));for(int i=0;i<3;i++){float ax=sx+L*(.22f+i*.13f);Path a=new Path();a.moveTo(ax,w-H*.43f);a.lineTo(ax+H*.18f,w-H*.37f);a.lineTo(ax,w-H*.31f);a.lineTo(ax+H*.045f,w-H*.37f);a.close();c.drawPath(a,p);}    }
    private static void battleship(Canvas c,Paint p,float sx,float ex,float w,float H,float L){
        hull(c,p,sx,ex,w,H,H*.38f,H*.18f,Color.rgb(129,142,147),Color.rgb(52,62,67));
        p.setColor(Color.rgb(111,122,126));c.drawRect(sx+L*.37f,w-H*.47f,sx+L*.67f,w-H*.22f,p);p.setColor(Color.rgb(154,166,170));c.drawRect(sx+L*.45f,w-H*.63f,sx+L*.59f,w-H*.47f,p);mast(c,p,sx+L*.53f,w-H*.63f,H*.80f);
        turret(c,p,sx+L*.17f,w-H*.31f,H*.58f,H*.45f);turret(c,p,sx+L*.29f,w-H*.30f,H*.54f,H*.41f);turret(c,p,sx+L*.79f,w-H*.29f,H*.52f,H*.40f);
        p.setColor(Color.rgb(45,49,52));c.drawRect(sx+L*.60f,w-H*.54f,sx+L*.65f,w-H*.26f,p);
    }
    private static void cruiser(Canvas c,Paint p,float sx,float ex,float w,float H,float L){
        hull(c,p,sx,ex,w,H,H*.32f,H*.14f,Color.rgb(118,133,140),Color.rgb(45,56,63));
        p.setColor(Color.rgb(133,145,149));c.drawRect(sx+L*.39f,w-H*.48f,sx+L*.64f,w-H*.22f,p);mast(c,p,sx+L*.52f,w-H*.48f,H*.72f);
        turret(c,p,sx+L*.20f,w-H*.30f,H*.43f,H*.34f);turret(c,p,sx+L*.77f,w-H*.29f,H*.40f,H*.31f);
        p.setColor(Color.rgb(52,59,63));for(int i=0;i<3;i++)c.drawRoundRect(new RectF(sx+L*(.59f+i*.035f),w-H*.41f,sx+L*(.61f+i*.035f),w-H*.28f),H*.02f,H*.02f,p);
    }
    private static void destroyer(Canvas c,Paint p,float sx,float ex,float w,float H,float L){
        hull(c,p,sx,ex,w,H,H*.27f,H*.10f,Color.rgb(104,121,129),Color.rgb(40,52,59));
        p.setColor(Color.rgb(135,148,153));c.drawRect(sx+L*.34f,w-H*.43f,sx+L*.57f,w-H*.22f,p);mast(c,p,sx+L*.45f,w-H*.43f,H*.64f);
        turret(c,p,sx+L*.16f,w-H*.29f,H*.34f,H*.29f);
        p.setColor(Color.rgb(48,54,58));for(int i=0;i<4;i++){float x=sx+L*(.61f+i*.035f);c.drawRoundRect(new RectF(x,w-H*.37f,x+H*.07f,w-H*.24f),H*.025f,H*.025f,p);} 
        p.setColor(Color.rgb(185,194,197));p.setStrokeWidth(H*.028f);p.setStyle(Paint.Style.STROKE);c.drawLine(sx+L*.70f,w-H*.42f,sx+L*.82f,w-H*.57f,p);p.setStyle(Paint.Style.FILL);
    }
    private static void submarine(Canvas c,Paint p,float sx,float ex,float w,float H,float L){
        float cy=w-H*.12f;Path body=new Path();body.moveTo(sx+H*.16f,cy-H*.14f);body.quadTo(sx,cy,sx+H*.16f,cy+H*.14f);body.lineTo(ex-H*.18f,cy+H*.14f);body.quadTo(ex,cy,ex-H*.18f,cy-H*.14f);body.close();
        p.setShader(new LinearGradient(0,cy-H*.15f,0,cy+H*.15f,Color.rgb(62,72,77),Color.rgb(18,24,28),Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);c.drawPath(body,p);p.setShader(null);
        p.setColor(Color.rgb(90,101,105));c.drawRoundRect(new RectF(sx+L*.46f,cy-H*.34f,sx+L*.60f,cy-H*.10f),H*.05f,H*.05f,p);
        p.setColor(Color.rgb(177,188,192));p.setStrokeWidth(H*.03f);p.setStyle(Paint.Style.STROKE);float px=sx+L*.53f;c.drawLine(px,cy-H*.34f,px,cy-H*.59f,p);c.drawLine(px,cy-H*.59f,px+H*.10f,cy-H*.59f,p);p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(39,47,51));Path fin=new Path();fin.moveTo(sx+L*.18f,cy);fin.lineTo(sx+L*.08f,cy-H*.28f);fin.lineTo(sx+L*.25f,cy-H*.08f);fin.close();c.drawPath(fin,p);
    }
}
