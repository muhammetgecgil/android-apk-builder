package com.muhammetgecgil.pcbzekapro;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Size;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    private enum Mode { OCR, BOARD, BANDS }
    private PreviewView preview; private TextView status,result,zoom,summary;
    private Camera camera; private ExecutorService executor; private ScaleGestureDetector scaler;
    private final TextRecognizer recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    private final AtomicBoolean requested=new AtomicBoolean(false),busy=new AtomicBoolean(false);
    private final List<ComponentDecoder.Item> bom=new ArrayList<>();
    private Mode mode=Mode.BOARD; private boolean torch; private String lastCode=""; private int scanNo=1;
    private final ActivityResultLauncher<String> permission=registerForActivityResult(new ActivityResultContracts.RequestPermission(),ok->{if(ok)startCamera();else status.setText("Kamera izni gerekli • Ayarlar > Uygulamalar > PCB Zekâ Pro > İzinler");});

    @Override protected void onCreate(Bundle b){super.onCreate(b);executor=Executors.newSingleThreadExecutor();buildUi();if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)startCamera();else permission.launch(Manifest.permission.CAMERA);}
    private void buildUi(){
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(0xFF071017);
        preview=new PreviewView(this);preview.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);preview.setScaleType(PreviewView.ScaleType.FILL_CENTER);root.addView(preview,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.VERTICAL);top.setPadding(dp(14),dp(10),dp(14),dp(8));top.setBackgroundColor(0xD9071017);
        top.addView(label("PCB ZEKÂ PRO v2 • MUHAMMET GEÇGİL",17,Color.WHITE));status=label("Kamera hazırlanıyor…",12,0xFF65FFF5);top.addView(status);summary=label("BOM: 0 • OCR hazır • R/C/L/D/Q/U",12,0xFFFFD166);top.addView(summary);root.addView(top,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
        LinearLayout bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.VERTICAL);bottom.setPadding(dp(10),dp(8),dp(10),dp(18));bottom.setBackgroundColor(0xEB071017);
        zoom=label("Zoom 1.0× • dokunarak netleştir",12,Color.WHITE);bottom.addView(zoom);SeekBar zb=new SeekBar(this);zb.setMax(100);bottom.addView(zb,new LinearLayout.LayoutParams(-1,dp(34)));
        HorizontalScrollView hs=new HorizontalScrollView(this);LinearLayout buttons=new LinearLayout(this);
        Button board=button("KARTI TARA"),bands=button("DİRENÇ RENK"),ocr=button("YAZILARI OKU"),flash=button("FLAŞ"),bomBtn=button("BOM"),data=button("DATASHEET"),fault=button("ARIZA"),share=button("KAYDET/PAYLAŞ"),clear=button("TEMİZLE");
        for(Button x:new Button[]{board,bands,ocr,flash,bomBtn,data,fault,share,clear})buttons.addView(x,new LinearLayout.LayoutParams(dp(132),dp(48)));hs.addView(buttons);bottom.addView(hs);
        result=label("Kartı sabitleyin. Tüm kart için KARTI TARA; direnç için elemanı yatay ortalayıp DİRENÇ RENK seçin.",13,Color.WHITE);result.setMaxLines(7);result.setPadding(0,dp(6),0,0);bottom.addView(result);
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);bp.setMargins(dp(6),0,dp(6),dp(6));root.addView(bottom,bp);setContentView(root);
        board.setOnClickListener(v->request(Mode.BOARD,"Tüm kart OCR + BOM analizi"));bands.setOnClickListener(v->request(Mode.BANDS,"Renk bantları analiz ediliyor"));ocr.setOnClickListener(v->request(Mode.OCR,"Yazı ve parça kodları okunuyor"));
        flash.setOnClickListener(v->{if(camera!=null&&camera.getCameraInfo().hasFlashUnit()){torch=!torch;camera.getCameraControl().enableTorch(torch);flash.setText(torch?"FLAŞ AÇIK":"FLAŞ");}});
        bomBtn.setOnClickListener(v->showBom());data.setOnClickListener(v->datasheet());fault.setOnClickListener(v->faultReport());share.setOnClickListener(v->shareCsv());clear.setOnClickListener(v->{bom.clear();scanNo=1;lastCode="";refresh();result.setText("Sonuçlar temizlendi.");});
        zb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean from){if(from)setZoom(p/100f);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
        scaler=new ScaleGestureDetector(this,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){if(camera==null)return false;float z=camera.getCameraInfo().getZoomState().getValue().getZoomRatio();camera.getCameraControl().setZoomRatio(z*d.getScaleFactor());updateZoom();return true;}});preview.setOnTouchListener(this::touch);
    }
    private void request(Mode m,String s){mode=m;requested.set(true);status.setText(s+" • net kare bekleniyor…");}
    private boolean touch(View v,MotionEvent e){scaler.onTouchEvent(e);if(e.getAction()==MotionEvent.ACTION_UP&&camera!=null&&!scaler.isInProgress()){MeteringPoint p=preview.getMeteringPointFactory().createPoint(e.getX(),e.getY());camera.getCameraControl().startFocusAndMetering(new FocusMeteringAction.Builder(p,FocusMeteringAction.FLAG_AF|FocusMeteringAction.FLAG_AE).setAutoCancelDuration(5,TimeUnit.SECONDS).build());status.setText("AF/AE hedefe kilitlendi");}return true;}
    private void startCamera(){ListenableFuture<ProcessCameraProvider> f=ProcessCameraProvider.getInstance(this);f.addListener(()->{try{ProcessCameraProvider provider=f.get();Preview pv=new Preview.Builder().setTargetResolution(new Size(1920,1080)).build();pv.setSurfaceProvider(preview.getSurfaceProvider());ImageAnalysis ia=new ImageAnalysis.Builder().setTargetResolution(new Size(1920,1080)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();ia.setAnalyzer(executor,this::analyze);provider.unbindAll();camera=provider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,pv,ia);camera.getCameraControl().setLinearZoom(0);runOnUiThread(()->status.setText("Kamera açık • 1080p analiz • dokunarak netleştir"));}catch(Exception e){runOnUiThread(()->status.setText("Kamera açılamadı: "+e.getMessage()));}},ContextCompat.getMainExecutor(this));}
    private void analyze(@NonNull ImageProxy im){
        if(!requested.compareAndSet(true,false)||!busy.compareAndSet(false,true)){im.close();return;}
        if(mode==Mode.BANDS){ResistorBandAnalyzer.Result r=ResistorBandAnalyzer.analyze(im);runOnUiThread(()->{result.setText("RENK BANTLARI\n"+r.bands+"\nDeğer: "+r.value+" • Güven %"+r.confidence);bom.add(new ComponentDecoder.Item("R?"+(scanNo++),r.bands,"Direnç",r.value,r.confidence,"Kamera renk bandı"));refresh();status.setText("Direnç analizi tamamlandı");});busy.set(false);im.close();return;}
        if(im.getImage()==null){busy.set(false);im.close();return;}InputImage input=InputImage.fromMediaImage(im.getImage(),im.getImageInfo().getRotationDegrees());
        recognizer.process(input).addOnSuccessListener(this::textDone).addOnFailureListener(e->runOnUiThread(()->status.setText("OCR hatası: "+e.getMessage()))).addOnCompleteListener(t->{busy.set(false);im.close();});
    }
    private void textDone(Text text){String raw=text.getText().trim();List<ComponentDecoder.Item> found=ComponentDecoder.decodeText(raw);if(!found.isEmpty()){for(ComponentDecoder.Item x:found)if(!contains(x.ref,x.code))bom.add(x);lastCode=found.get(0).code;}runOnUiThread(()->{StringBuilder s=new StringBuilder();for(ComponentDecoder.Item x:found)s.append(x.line()).append('\n');if(s.length()==0)s.append("Kod bulunamadı. Yaklaştırın, hedefe dokunup tekrar deneyin.");result.setText(s.toString().trim());status.setText("Analiz tamamlandı • "+found.size()+" aday • "+text.getTextBlocks().size()+" OCR alanı");refresh();});}
    private boolean contains(String ref,String code){for(ComponentDecoder.Item x:bom)if(x.ref.equals(ref)&&x.code.equals(code))return true;return false;}
    private void refresh(){int r=0,c=0,l=0,d=0,q=0,u=0;for(ComponentDecoder.Item x:bom){if(x.ref.startsWith("R"))r++;else if(x.ref.startsWith("C"))c++;else if(x.ref.startsWith("L")||x.ref.startsWith("FB"))l++;else if(x.ref.startsWith("D"))d++;else if(x.ref.startsWith("Q"))q++;else if(x.ref.startsWith("U"))u++;}summary.setText("BOM: "+bom.size()+" • R"+r+" C"+c+" L/FB"+l+" D"+d+" Q"+q+" U"+u);}
    private void showBom(){StringBuilder s=new StringBuilder("REF • TÜR • KOD → DEĞER • GÜVEN\n\n");for(ComponentDecoder.Item x:bom)s.append(x.line()).append("\n");if(bom.isEmpty())s.append("Henüz eleman yok. KARTI TARA ile başlayın.");dialog("BOM / MALZEME LİSTESİ",s.toString());}
    private void datasheet(){String q=lastCode.isEmpty()?"electronic component marking datasheet":lastCode+" datasheet pdf pinout";startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+URLEncoder.encode(q,StandardCharsets.UTF_8))));}
    private void faultReport(){StringBuilder s=new StringBuilder("GÖRSEL KONTROL PLANI\n\n1. Yanık, çatlak, şişme, oksit ve lehim köprüsü için kartı farklı açılarda tarayın.\n2. Düşük güvenli değerleri multimetre/LCR ile doğrulayın.\n3. IC kodunu paket ve pin sayısıyla doğrulayın.\n4. İşaretsiz bobin/kondansatör kameradan ölçülemez.\n\n");for(ComponentDecoder.Item x:bom)if(x.confidence<80||x.value.contains("gerekli"))s.append("⚠ ").append(x.line()).append("\n");dialog("ARIZA / KONTROL ET",s.toString());}
    private void shareCsv(){StringBuilder s=new StringBuilder("Ref,Tur,Kod,Deger,Guven,Not\n");for(ComponentDecoder.Item x:bom)s.append(x.csv()).append('\n');Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/csv");i.putExtra(Intent.EXTRA_SUBJECT,"PCB Zekâ Pro BOM");i.putExtra(Intent.EXTRA_TEXT,s.toString());startActivity(Intent.createChooser(i,"BOM kaydet veya paylaş"));}
    private void dialog(String title,String text){ScrollView sv=new ScrollView(this);TextView t=label(text,14,Color.WHITE);t.setPadding(dp(18),dp(10),dp(18),dp(10));sv.addView(t);new AlertDialog.Builder(this).setTitle(title).setView(sv).setPositiveButton("GERİ",null).show();}
    private void setZoom(float z){if(camera!=null){camera.getCameraControl().setLinearZoom(Math.max(0,Math.min(1,z)));preview.postDelayed(this::updateZoom,80);}}
    private void updateZoom(){if(camera!=null&&camera.getCameraInfo().getZoomState().getValue()!=null)zoom.setText(String.format(Locale.US,"Zoom %.1f× • dokunarak netleştir",camera.getCameraInfo().getZoomState().getValue().getZoomRatio()));}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(11);b.setTextColor(Color.WHITE);return b;}private TextView label(String s,int z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);return v;}private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override protected void onDestroy(){recognizer.close();executor.shutdown();super.onDestroy();}
}
