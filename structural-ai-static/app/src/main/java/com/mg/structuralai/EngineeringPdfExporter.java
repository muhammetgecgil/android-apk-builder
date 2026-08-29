package com.mg.structuralai;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Dependency-free engineering PDF exporter using Android PdfDocument. */
public final class EngineeringPdfExporter {
    public static final class EvidenceImage {
        public final String label;
        public final String note;
        public final Bitmap bitmap;
        public EvidenceImage(String label,String note,Bitmap bitmap){this.label=label==null?"EVIDENCE":label;this.note=note==null?"":note;this.bitmap=bitmap;}
    }

    private EngineeringPdfExporter(){}

    /** Backward-compatible single-view export. */
    public static void write(ContentResolver resolver,Uri uri,String title,String body,Bitmap viewport) throws Exception {
        List<EvidenceImage> ev=viewport==null?Collections.emptyList():Collections.singletonList(new EvidenceImage("VIEWPORT EVIDENCE","Captured application viewport",viewport));
        write(resolver,uri,title,body,ev);
    }

    /** Product report export with independently labelled visual evidence. */
    public static void write(ContentResolver resolver,Uri uri,String title,String body,List<EvidenceImage> evidence) throws Exception {
        PdfDocument pdf=new PdfDocument();
        Paint titlePaint=new Paint(Paint.ANTI_ALIAS_FLAG);titlePaint.setColor(Color.BLACK);titlePaint.setTextSize(18);titlePaint.setFakeBoldText(true);
        Paint sectionPaint=new Paint(Paint.ANTI_ALIAS_FLAG);sectionPaint.setColor(Color.BLACK);sectionPaint.setTextSize(11);sectionPaint.setFakeBoldText(true);
        Paint textPaint=new Paint(Paint.ANTI_ALIAS_FLAG);textPaint.setColor(Color.BLACK);textPaint.setTextSize(9.5f);
        Paint footerPaint=new Paint(Paint.ANTI_ALIAS_FLAG);footerPaint.setColor(Color.DKGRAY);footerPaint.setTextSize(8);
        final int w=595,h=842,margin=34;float maxWidth=w-2f*margin,lineH=12.5f;
        List<String> lines=wrapBody(textPaint,body==null?"":body,maxWidth);
        List<EvidenceImage> ev=evidence==null?Collections.emptyList():evidence;
        int pageNo=1,index=0,evidenceIndex=0;boolean first=true;
        while(index<lines.size()||evidenceIndex<ev.size()||first){
            PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(w,h,pageNo).create());Canvas c=page.getCanvas();c.drawColor(Color.WHITE);float y=margin;
            c.drawText(title==null?"STRUCTURAL AI ENGINEERING REPORT":title,margin,y+18,titlePaint);y+=34;
            c.drawText("Structural AI • v"+BuildConfig.VERSION_NAME+" • buildCode="+BuildConfig.VERSION_CODE,margin,y,footerPaint);y+=16;

            while(evidenceIndex<ev.size()){
                EvidenceImage e=ev.get(evidenceIndex);if(e==null||e.bitmap==null||e.bitmap.getWidth()<=0||e.bitmap.getHeight()<=0){evidenceIndex++;continue;}
                float iw=maxWidth,ih=Math.min(235f,iw*e.bitmap.getHeight()/Math.max(1f,e.bitmap.getWidth()));float needed=18+ih+28;
                if(y+needed>h-margin-26&&y>margin+70)break;
                c.drawText(e.label,margin,y+11,sectionPaint);y+=18;
                c.drawBitmap(e.bitmap,null,new RectF(margin,y,margin+iw,y+ih),null);y+=ih+12;
                if(!e.note.isEmpty()){List<String> noteLines=wrapBody(footerPaint,e.note,maxWidth);for(String n:noteLines){if(y>h-margin-30)break;c.drawText(n,margin,y,footerPaint);y+=10;}}
                y+=8;evidenceIndex++;
                if(y>h-margin-80)break;
            }

            while(index<lines.size()&&y<h-margin-22){c.drawText(lines.get(index++),margin,y,textPaint);y+=lineH;}
            c.drawText("Page "+pageNo+" • Structural AI v"+BuildConfig.VERSION_NAME,margin,h-18,footerPaint);pdf.finishPage(page);pageNo++;first=false;
        }
        try(OutputStream out=resolver.openOutputStream(uri)){if(out==null)throw new IllegalStateException("PDF output stream açılamadı");pdf.writeTo(out);}finally{pdf.close();}
    }

    private static List<String> wrapBody(Paint p,String body,float maxWidth){List<String> out=new ArrayList<>();String[] src=body.replace("\r","").split("\n",-1);for(String line:src){if(line.isEmpty()){out.add("");continue;}String[] words=line.split("\\s+");String cur="";for(String word:words){String next=cur.isEmpty()?word:cur+" "+word;if(p.measureText(next)<=maxWidth){cur=next;}else{if(!cur.isEmpty())out.add(cur);if(p.measureText(word)<=maxWidth){cur=word;}else{String part="";for(int i=0;i<word.length();i++){String n=part+word.charAt(i);if(p.measureText(n)>maxWidth&&!part.isEmpty()){out.add(part);part=""+word.charAt(i);}else part=n;}cur=part;}}}if(!cur.isEmpty())out.add(cur);}return out;}
}
