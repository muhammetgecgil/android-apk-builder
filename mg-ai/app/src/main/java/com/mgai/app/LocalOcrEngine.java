package com.mgai.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class LocalOcrEngine {
    private static final int MAX_PDF_PAGES=30;
    private LocalOcrEngine(){}

    public static String recognizeImage(Context c, Uri uri) throws Exception {
        ImageDecoder.Source src=ImageDecoder.createSource(c.getContentResolver(),uri);
        Bitmap bmp=ImageDecoder.decodeBitmap(src,(decoder,info,s)->decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE));
        return recognizeBitmap(bmp);
    }

    public static String recognizeScannedPdf(Context c, Uri uri) throws Exception {
        StringBuilder out=new StringBuilder();
        try(ParcelFileDescriptor pfd=c.getContentResolver().openFileDescriptor(uri,"r")){
            if(pfd==null)throw new IllegalArgumentException("pdf_open_failed");
            try(PdfRenderer renderer=new PdfRenderer(pfd)){
                int count=Math.min(renderer.getPageCount(),MAX_PDF_PAGES);
                for(int i=0;i<count;i++){
                    try(PdfRenderer.Page page=renderer.openPage(i)){
                        int width=Math.max(1080,page.getWidth()*2);
                        int height=Math.max(1440,page.getHeight()*2);
                        Bitmap bmp=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
                        page.render(bmp,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        String text=recognizeBitmap(bmp).trim();
                        bmp.recycle();
                        if(!text.isEmpty())out.append("\n[Sayfa ").append(i+1).append("]\n").append(text).append('\n');
                    }
                }
            }
        }
        String text=out.toString().trim();
        if(text.length()<10)throw new IllegalArgumentException("OCR ile okunabilir metin bulunamadı.");
        return text;
    }

    private static String recognizeBitmap(Bitmap bmp) throws Exception {
        TextRecognizer recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        CountDownLatch latch=new CountDownLatch(1);
        AtomicReference<String> value=new AtomicReference<>("");
        AtomicReference<Exception> error=new AtomicReference<>();
        recognizer.process(InputImage.fromBitmap(bmp,0))
                .addOnSuccessListener((Text result)->{value.set(result.getText());latch.countDown();})
                .addOnFailureListener(e->{error.set(e instanceof Exception?(Exception)e:new Exception(e));latch.countDown();});
        if(!latch.await(60, TimeUnit.SECONDS))throw new IllegalStateException("OCR timeout");
        recognizer.close();
        if(error.get()!=null)throw error.get();
        return value.get()==null?"":value.get();
    }
}
