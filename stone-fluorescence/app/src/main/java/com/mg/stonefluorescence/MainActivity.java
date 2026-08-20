package com.mg.stonefluorescence;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_VIDEO = 42;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView result;
    private TextView detail;
    private ImageView preview;
    private ProgressBar progress;
    private Button pick;

    static class FrameStat {
        long timeUs;
        double brightness;
        double saturation;
        double redRatio;
        double greenRatio;
        double hue;
        double hotPixelRatio;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
    }

    private View buildUi() {
        int pad = dp(18);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(16,18,22));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, dp(28));
        scroll.addView(root);

        TextView title = tv("STONE GLOW ANALYZER", 24, true);
        title.setTextColor(Color.WHITE);
        root.addView(title);

        TextView sub = tv("Video içindeki sıra dışı renk değişimi, lokal parlama ve fosfor benzeri gecikmeli ışıma davranışını arar.", 15, false);
        sub.setTextColor(Color.rgb(190,198,210));
        sub.setPadding(0, dp(8), 0, dp(14));
        root.addView(sub);

        TextView warning = tv("Not: Telefon kamerası gerçek UV dalga boyunu veya mineral türünü ölçmez. Bu uygulama yalnızca görünür videodaki optik anomalileri işaretler.", 13, false);
        warning.setTextColor(Color.rgb(255,210,105));
        warning.setBackgroundColor(Color.rgb(43,39,24));
        warning.setPadding(dp(12), dp(10), dp(12), dp(10));
        root.addView(warning);

        pick = new Button(this);
        pick.setText("TAŞ VİDEOSU SEÇ VE ANALİZ ET");
        pick.setTextSize(16);
        pick.setOnClickListener(v -> chooseVideo());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(58));
        bp.setMargins(0, dp(16), 0, dp(12));
        root.addView(pick, bp);

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(44)));

        preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setBackgroundColor(Color.BLACK);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, dp(300));
        ip.setMargins(0, dp(8), 0, dp(10));
        root.addView(preview, ip);

        result = tv("Hazır", 22, true);
        result.setGravity(Gravity.CENTER_HORIZONTAL);
        result.setTextColor(Color.rgb(180,230,255));
        root.addView(result);

        detail = tv("Analiz sonucu burada görünecek.", 15, false);
        detail.setTextColor(Color.rgb(220,225,232));
        detail.setPadding(0, dp(10), 0, 0);
        root.addView(detail);
        return scroll;
    }

    private TextView tv(String text, int sp, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(sp);
        if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD);
        return v;
    }

    private void chooseVideo() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("video/*");
        startActivityForResult(i, PICK_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_VIDEO && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
            analyze(uri);
        }
    }

    private void analyze(Uri uri) {
        pick.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        result.setText("Video analiz ediliyor…");
        detail.setText("Kareler örnekleniyor; merkezdeki taş bölgesinin parlaklık ve renk davranışı karşılaştırılıyor.");

        executor.execute(() -> {
            MediaMetadataRetriever r = new MediaMetadataRetriever();
            Bitmap best = null;
            try {
                r.setDataSource(this, uri);
                String d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long durationMs = d == null ? 0 : Long.parseLong(d);
                if (durationMs < 200) throw new IllegalArgumentException("Video çok kısa.");

                int sampleCount = 24;
                List<FrameStat> stats = new ArrayList<>();
                for (int i = 0; i < sampleCount; i++) {
                    long tUs = (long)((durationMs * 1000.0) * i / (sampleCount - 1));
                    Bitmap b = r.getFrameAtTime(tUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    if (b != null) {
                        FrameStat s = stat(b);
                        s.timeUs = tUs;
                        stats.add(s);
                        b.recycle();
                    }
                }
                if (stats.size() < 6) throw new IllegalStateException("Yeterli kare okunamadı.");

                double medB = median(stats, 0);
                double medS = median(stats, 1);
                double medR = median(stats, 2);
                double medG = median(stats, 3);
                double medH = median(stats, 4);
                double medHot = median(stats, 5);

                double bestScore = -1;
                FrameStat bestStat = stats.get(0);
                double maxHueShift = 0;
                double maxColorFlip = 0;
                for (int i = 0; i < stats.size(); i++) {
                    FrameStat s = stats.get(i);
                    double brightSpike = positiveRatio(s.brightness, medB);
                    double satSpike = positiveRatio(s.saturation, medS);
                    double localGlow = positiveRatio(s.hotPixelRatio, medHot);
                    double hueShift = circularHueDistance(s.hue, medH) / 180.0;
                    double colorFlip = Math.abs((s.redRatio - s.greenRatio) - (medR - medG));
                    maxHueShift = Math.max(maxHueShift, hueShift);
                    maxColorFlip = Math.max(maxColorFlip, colorFlip);
                    double score = 34 * clamp(brightSpike/0.45) + 18 * clamp(satSpike/0.45) + 22 * clamp(hueShift/0.55) + 16 * clamp(colorFlip/0.25) + 10 * clamp(localGlow/1.2);
                    if (score > bestScore) { bestScore = score; bestStat = s; }
                }

                double persistence = persistenceScore(stats);
                bestScore = Math.min(100, bestScore + persistence * 15);
                best = r.getFrameAtTime(bestStat.timeUs, MediaMetadataRetriever.OPTION_CLOSEST);

                String level;
                if (bestScore >= 70) level = "GÜÇLÜ OPTİK ANOMALİ";
                else if (bestScore >= 45) level = "BELİRGİN ANOMALİ";
                else if (bestScore >= 25) level = "HAFİF ANOMALİ";
                else level = "OLAĞANDIŞI PARLAMA ZAYIF";

                String interpretation = String.format(Locale.US,
                        "%s\n\nAnomali skoru: %.0f/100\nRenk kayması: %.0f%%\nLokal parlama: %.0f%%\nFosfor/gecikmeli ışıma ipucu: %.0f%%\n\nYorum: %s",
                        level, bestScore, maxHueShift*100,
                        clamp(positiveRatio(bestStat.hotPixelRatio, medHot)/1.2)*100,
                        persistence*100,
                        explain(bestScore, maxHueShift, maxColorFlip, persistence));
                Bitmap finalBest = best;
                double finalScore = bestScore;
                runOnUiThread(() -> {
                    preview.setImageBitmap(finalBest);
                    result.setText(level);
                    result.setTextColor(finalScore >= 45 ? Color.rgb(255,105,95) : Color.rgb(120,230,180));
                    detail.setText(interpretation);
                    progress.setVisibility(View.GONE);
                    pick.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    result.setText("Analiz yapılamadı");
                    detail.setText(e.getMessage() == null ? "Video okunamadı." : e.getMessage());
                    progress.setVisibility(View.GONE);
                    pick.setEnabled(true);
                });
            } finally {
                try { r.release(); } catch (Exception ignored) {}
            }
        });
    }

    private FrameStat stat(Bitmap src) {
        Bitmap b = src;
        int w = b.getWidth(), h = b.getHeight();
        int x0 = (int)(w*0.15), x1 = (int)(w*0.85), y0 = (int)(h*0.15), y1 = (int)(h*0.85);
        int step = Math.max(2, Math.min(w,h)/180);
        double sumV=0, sumS=0, sumR=0, sumG=0, sumHueX=0, sumHueY=0;
        int n=0, hot=0;
        float[] hsv = new float[3];
        for (int y=y0; y<y1; y+=step) {
            for (int x=x0; x<x1; x+=step) {
                int c=b.getPixel(x,y); int r=Color.red(c), g=Color.green(c), bl=Color.blue(c);
                Color.RGBToHSV(r,g,bl,hsv);
                double v=hsv[2], s=hsv[1], rad=Math.toRadians(hsv[0]);
                sumV+=v; sumS+=s; sumR+=r/255.0; sumG+=g/255.0;
                sumHueX += Math.cos(rad)*s; sumHueY += Math.sin(rad)*s;
                if (v>0.88 && s>0.28) hot++;
                n++;
            }
        }
        FrameStat fs=new FrameStat();
        fs.brightness=sumV/n; fs.saturation=sumS/n; fs.redRatio=sumR/n; fs.greenRatio=sumG/n;
        double hue=Math.toDegrees(Math.atan2(sumHueY,sumHueX)); if(hue<0) hue+=360; fs.hue=hue;
        fs.hotPixelRatio=hot/(double)n;
        return fs;
    }

    private double median(List<FrameStat> s, int kind) {
        List<Double> a=new ArrayList<>();
        for(FrameStat f:s){
            if(kind==0)a.add(f.brightness); else if(kind==1)a.add(f.saturation); else if(kind==2)a.add(f.redRatio);
            else if(kind==3)a.add(f.greenRatio); else if(kind==4)a.add(f.hue); else a.add(f.hotPixelRatio);
        }
        Collections.sort(a); return a.get(a.size()/2);
    }

    private double persistenceScore(List<FrameStat> a) {
        int peak=0; for(int i=1;i<a.size();i++) if(a.get(i).brightness>a.get(peak).brightness) peak=i;
        if(peak>=a.size()-2) return 0;
        double p=a.get(peak).brightness, after=(a.get(peak+1).brightness+a.get(Math.min(peak+2,a.size()-1)).brightness)/2.0;
        double before=peak>0?a.get(peak-1).brightness:a.get(0).brightness;
        double rise=p-before;
        if(rise<0.06) return 0;
        return clamp((after-before)/(rise+0.0001));
    }

    private String explain(double score,double hue,double flip,double pers){
        if(score>=70 && pers>0.35) return "Videoda güçlü parlaklık/renk anomalisi ve tepe sonrasında devam eden ışıma görülüyor. Harici 365/395 nm UV ışıkla kontrollü tekrar test önerilir.";
        if(score>=45 && (hue>0.25 || flip>0.12)) return "Taş bölgesinde belirgin renk dönüşümü var. Yeşil-kırmızı gibi emisyon benzeri değişim olabilir; aynı çekimi sabit pozlama ile karşılaştırın.";
        if(score>=25) return "Bazı karelerde fark var ancak otomatik pozlama, yansıma veya beyaz dengesi de bu etkiyi oluşturabilir.";
        return "Video boyunca renk ve parlaklık davranışı büyük ölçüde kararlı. Belirgin floresans benzeri işaret bulunmadı.";
    }

    private double positiveRatio(double x,double base){ return Math.max(0,(x-base)/(Math.abs(base)+0.02)); }
    private double clamp(double x){ return Math.max(0,Math.min(1,x)); }
    private double circularHueDistance(double a,double b){ double d=Math.abs(a-b)%360; return d>180?360-d:d; }
    private int dp(int x){ return (int)(x*getResources().getDisplayMetrics().density+0.5f); }

    @Override protected void onDestroy(){ super.onDestroy(); executor.shutdownNow(); }
}
