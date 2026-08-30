package com.muhammetgecgil.haber;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private final String[] categories = {
            "Gündem","Bilim","Yapay Zekâ","Savaş","Arkeoloji","Sanat","Havacılık","Uzay",
            "Enerji","Otomotiv","Robotik","Teknoloji","Savunma","Ekonomi","Dünya","Türkiye",
            "Sağlık","Çevre","İklim","Tarih","Eğitim","Mühendislik","Elektronik","Siber Güvenlik"
    };
    private EditText query;
    private WebView web;
    private ProgressBar progress;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        requestNotificationPermission();
        AiNewsReceiver.schedule(this);
        boolean openAi = getIntent() != null && getIntent().getBooleanExtra("open_ai", false);
        query.setText(openAi ? "Yapay Zekâ" : "Gündem");
        search(false);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 77);
        }
    }

    private void buildUi() {
        int p = dp(12);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p,p,p,p);
        root.setBackgroundColor(0xFFF7F3EA);

        TextView title = new TextView(this);
        title.setText("Muhammet Haber");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setTextColor(0xFF1D1A17);
        title.setPadding(0,dp(4),0,dp(8));
        root.addView(title,new LinearLayout.LayoutParams(-1,-2));

        TextView sub = new TextView(this);
        sub.setText("24 kategori • API anahtarı yok • günlük AI bildirimi 09:00");
        sub.setGravity(Gravity.CENTER_HORIZONTAL);
        sub.setTextColor(0xFF6F6255);
        sub.setPadding(0,0,0,dp(8));
        root.addView(sub,new LinearLayout.LayoutParams(-1,-2));

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        for (String c : categories) {
            Button b = new Button(this);
            b.setAllCaps(false); b.setText(c); b.setTextSize(12);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2,dp(43));
            bp.setMargins(dp(2),0,dp(2),0);
            chips.addView(b,bp);
            b.setOnClickListener(v -> { query.setText(c); search(false); });
        }
        hsv.addView(chips);
        root.addView(hsv,new LinearLayout.LayoutParams(-1,dp(48)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        query = new EditText(this);
        query.setSingleLine(true);
        query.setHint("Bir konu ara: KAAN, AI, uzay...");
        row.addView(query,new LinearLayout.LayoutParams(0,dp(52),1f));
        Button find = new Button(this); find.setText("ARA"); find.setOnClickListener(v -> search(false));
        row.addView(find,new LinearLayout.LayoutParams(dp(72),dp(52)));
        Button digest = new Button(this); digest.setText("DERLE"); digest.setOnClickListener(v -> search(true));
        row.addView(digest,new LinearLayout.LayoutParams(dp(86),dp(52)));
        root.addView(row,new LinearLayout.LayoutParams(-1,-2));

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(dp(34),dp(34));
        pp.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(progress,pp);

        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(false);
        web.getSettings().setDomStorageEnabled(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                openExternal(request.getUrl().toString());
                return true;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                openExternal(url);
                return true;
            }
        });
        root.addView(web,new LinearLayout.LayoutParams(-1,0,1f));
        setContentView(root);
    }

    private void openExternal(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) { Toast.makeText(this,"Bağlantı açılamadı",Toast.LENGTH_SHORT).show(); }
    }

    private void search(boolean compile) {
        String q = query.getText().toString().trim();
        if (TextUtils.isEmpty(q)) { Toast.makeText(this,"Bir konu yazın",Toast.LENGTH_SHORT).show(); return; }
        progress.setVisibility(View.VISIBLE);
        web.loadDataWithBaseURL(null,page("<h1>"+(compile?"Derleniyor":"Haberler aranıyor")+"</h1><p class='lead'>"+esc(q)+" için güncel kaynaklara bağlanılıyor.</p>"),"text/html","UTF-8",null);
        new Thread(() -> {
            try {
                List<NewsUtils.Article> items = NewsUtils.fetch(q, 25);
                String html = compile ? compileHtml(q, items) : listHtml(q, items);
                runOnUiThread(() -> { progress.setVisibility(View.GONE); web.loadDataWithBaseURL(null,html,"text/html","UTF-8",null); });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    web.loadDataWithBaseURL(null,errorHtml(q,e),"text/html","UTF-8",null);
                });
            }
        }).start();
    }

    private String listHtml(String q, List<NewsUtils.Article> a) {
        StringBuilder b = new StringBuilder("<h1>"+esc(q)+" haberleri</h1><div class='meta'>Google News RSS • yedek kaynak etkin</div>");
        if (a.isEmpty()) b.append("<div class='card'>Sonuç bulunamadı.</div>");
        for (NewsUtils.Article x : a) {
            b.append("<div class='card'><a href='").append(attr(x.url)).append("'><b>").append(esc(x.title)).append("</b></a>")
                    .append("<div class='meta left'>").append(esc(x.source)).append(" • ").append(esc(x.date)).append("</div></div>");
        }
        return page(b.toString());
    }

    private String compileHtml(String q, List<NewsUtils.Article> a) {
        if (a.isEmpty()) return page("<h1>"+esc(q)+"</h1><div class='card'>Derleme için güncel kaynak bulunamadı.</div>");

        Map<String,Integer> freq = new HashMap<>();
        for (NewsUtils.Article x : a) {
            for (String w : x.title.toLowerCase(new Locale("tr","TR")).split("\\s+")) {
                w = w.replaceAll("[^a-zA-ZçÇğĞıİöÖşŞüÜ0-9]","");
                if (w.length() >= 5 && !stop(w)) freq.put(w,freq.getOrDefault(w,0)+1);
            }
        }
        List<Map.Entry<String,Integer>> keys = new ArrayList<>(freq.entrySet());
        keys.sort((x,y) -> Integer.compare(y.getValue(),x.getValue()));
        StringBuilder themes = new StringBuilder();
        for (int i=0;i<Math.min(5,keys.size());i++) { if (i>0) themes.append(", "); themes.append(keys.get(i).getKey()); }

        StringBuilder body = new StringBuilder();
        body.append("<p class='lead'>Son güncel haber akışı birlikte değerlendirildiğinde <b>").append(esc(q)).append("</b> başlığında ")
                .append(themes.length()>0 ? esc(themes.toString())+" temaları öne çıkıyor. " : "birkaç ana gelişme öne çıkıyor. ")
                .append("Aşağıdaki metin, tekrar eden başlıkları ayıklayıp gelişmeleri tek okuma akışında toplar.</p>");

        int used=0;
        for (NewsUtils.Article x : a) {
            if (used >= 7) break;
            String s = normalize(x.title);
            if (s.length() < 25) continue;
            body.append("<p>").append(esc(s)).append(".</p>");
            used++;
        }
        body.append("<p>Kaynaklar farklı ayrıntılar veya farklı zaman bilgileri verebilir. Sayı, tarih, resmî açıklama ve kritik gelişmeler için aşağıdaki özgün bağlantıları açarak kontrol etmek en güvenilir yöntemdir.</p>");

        StringBuilder src = new StringBuilder("<ul>");
        for (int i=0;i<Math.min(10,a.size());i++) {
            NewsUtils.Article x=a.get(i);
            src.append("<li><a href='").append(attr(x.url)).append("'>").append(esc(x.title)).append("</a>");
            if (!x.source.isEmpty()) src.append(" — ").append(esc(x.source));
            src.append("</li>");
        }
        src.append("</ul>");

        String date = new SimpleDateFormat("dd MMMM yyyy HH:mm",new Locale("tr","TR")).format(new Date());
        return page("<article><h1>"+esc(q)+": güncel derleme</h1><div class='meta'>"+date+" • "+a.size()+" haber tarandı</div><div class='rule'></div>"+body+"<h2>Kaynaklar</h2><div class='sources'>"+src+"</div></article>");
    }

    private String errorHtml(String q, Exception e) {
        String msg = e == null || e.getMessage() == null ? "İnternet bağlantısını kontrol edin." : e.getMessage();
        return page("<h1>"+esc(q)+"</h1><div class='card'><b>Haber alınamadı</b><br>"+esc(msg)+"<br><br>Bu sürüm Google News RSS başarısız olursa ikinci haber kaynağını otomatik dener.</div>");
    }

    private String page(String body) {
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>"+
                "body{margin:0;background:#f7f3ea;color:#28231f;font-family:Georgia,'Times New Roman',serif;padding:18px}"+
                "article{max-width:850px;margin:0 auto;background:#fffdf8;padding:28px 24px 42px;border-radius:18px;box-shadow:0 4px 18px #00000012}"+
                "h1{font-size:28px;line-height:1.25;text-align:center;margin:4px 0 8px}h2{font-size:19px;margin-top:32px;border-left:4px solid #a77a43;padding-left:10px}"+
                ".meta{font:13px sans-serif;color:#746b62;text-align:center;margin:7px 0 18px}.left{text-align:left;margin:8px 0 0}"+
                ".rule{width:72px;height:2px;background:#c4aa86;margin:0 auto 25px}.lead:first-letter{float:left;font-size:50px;line-height:.9;padding:4px 7px 0 0;color:#8b5a2b;font-weight:bold}"+
                "p{font-size:18px;line-height:1.9;text-align:justify;text-indent:1.45em;margin:0 0 20px}.lead{text-indent:0}"+
                ".card{background:#fffdf8;border-radius:15px;padding:17px;margin:12px 0;box-shadow:0 3px 12px #00000010;font:16px/1.5 sans-serif}"+
                ".sources{background:#f2eadf;padding:14px 16px;border-radius:14px;font:14px/1.6 sans-serif}a{color:#5b3b19;text-decoration:none}li{margin:9px 0}</style></head><body>"+body+"</body></html>";
    }

    private boolean stop(String w) {
        String s=" haber son dakika bugün hakkında için olan olarak sonra önce yeni daha göre kadar ile bir bu ve veya çok tüm şimdi ancak yine " ;
        return s.contains(" "+w+" ");
    }
    private String normalize(String t) {
        String s = t.replaceAll("\\s+[|–—-]\\s+[^|–—-]{2,80}$","").trim();
        if (s.endsWith(".")) s=s.substring(0,s.length()-1);
        if (s.length()>1) s=Character.toUpperCase(s.charAt(0))+s.substring(1);
        return s;
    }
    private String esc(String s) { if (s==null) return ""; return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;"); }
    private String attr(String s) { return esc(s).replace("'","&#39;"); }
    private int dp(int v) { return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
}
