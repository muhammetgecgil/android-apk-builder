package com.muhammetgecgil.haber;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    private final String[] categories = {
            "Gündem","Bilim","Yapay Zekâ","Savaş","Arkeoloji","Sanat","Havacılık","Uzay",
            "Enerji","Otomotiv","Robotik","Teknoloji","Savunma","Ekonomi","Dünya","Türkiye",
            "Sağlık","Çevre","İklim","Tarih","Eğitim","Mühendislik","Elektronik","Siber Güvenlik"
    };
    private EditText query;
    private WebView web;
    private ProgressBar progress;
    private String selected = "Gündem";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        search(false);
    }

    private void buildUi() {
        int p = dp(14);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p,p,p,p);
        root.setBackgroundColor(0xFFF7F3EA);

        TextView title = new TextView(this);
        title.setText("Muhammet Haber");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setTextColor(0xFF1D1A17);
        title.setPadding(0,dp(4),0,dp(10));
        root.addView(title,new LinearLayout.LayoutParams(-1,-2));

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        for (String c: categories) {
            Button b = new Button(this);
            b.setAllCaps(false);
            b.setText(c);
            b.setTextSize(13);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2,dp(44));
            bp.setMargins(dp(3),0,dp(3),0);
            chips.addView(b,bp);
            b.setOnClickListener(v -> { selected = c; query.setText(c); search(false); });
        }
        hsv.addView(chips);
        root.addView(hsv,new LinearLayout.LayoutParams(-1,dp(50)));

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        query = new EditText(this);
        query.setSingleLine(true);
        query.setHint("Bir konu ara: KAAN, yapay zekâ, uzay...");
        query.setText(selected);
        searchRow.addView(query,new LinearLayout.LayoutParams(0,dp(52),1f));

        Button find = new Button(this); find.setText("ARA");
        find.setOnClickListener(v -> search(false));
        searchRow.addView(find,new LinearLayout.LayoutParams(dp(74),dp(52)));
        Button compile = new Button(this); compile.setText("DERLE");
        compile.setOnClickListener(v -> search(true));
        searchRow.addView(compile,new LinearLayout.LayoutParams(dp(86),dp(52)));
        root.addView(searchRow,new LinearLayout.LayoutParams(-1,-2));

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(dp(36),dp(36));
        pp.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(progress,pp);

        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(false);
        web.getSettings().setDomStorageEnabled(false);
        web.getSettings().setBuiltInZoomControls(false);
        web.setWebViewClient(new WebViewClient());
        root.addView(web,new LinearLayout.LayoutParams(-1,0,1f));
        setContentView(root);
    }

    private void search(boolean compile) {
        String q = query.getText().toString().trim();
        if (TextUtils.isEmpty(q)) { Toast.makeText(this,"Bir konu yazın",Toast.LENGTH_SHORT).show(); return; }
        progress.setVisibility(View.VISIBLE);
        web.loadDataWithBaseURL(null, loadingHtml(q,compile), "text/html","UTF-8",null);
        new Thread(() -> {
            try {
                List<Article> articles = fetchGdelt(q);
                String html = compile ? compileHtml(q,articles) : listHtml(q,articles);
                runOnUiThread(() -> { progress.setVisibility(View.GONE); web.loadDataWithBaseURL(null,html,"text/html","UTF-8",null); });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    String h = errorHtml(q,e.getMessage());
                    web.loadDataWithBaseURL(null,h,"text/html","UTF-8",null);
                });
            }
        }).start();
    }

    private List<Article> fetchGdelt(String q) throws Exception {
        String url = "https://api.gdeltproject.org/api/v2/doc/doc?query=" + URLEncoder.encode(q, "UTF-8") +
                "&mode=artlist&maxrecords=30&format=json&sort=datedesc";
        HttpURLConnection c = (HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(12000); c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent","MuhammetHaber/1.0 Android");
        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(); String line;
        while ((line=br.readLine())!=null) sb.append(line);
        br.close();
        JSONObject root = new JSONObject(sb.toString());
        JSONArray arr = root.optJSONArray("articles");
        List<Article> out = new ArrayList<>();
        if (arr == null) return out;
        Set<String> seen = new HashSet<>();
        for (int i=0;i<arr.length();i++) {
            JSONObject o = arr.optJSONObject(i); if (o==null) continue;
            String t = clean(o.optString("title"));
            String u = o.optString("url");
            if (TextUtils.isEmpty(t) || TextUtils.isEmpty(u)) continue;
            String key = t.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9çğıöşü ]","");
            if (!seen.add(key)) continue;
            Article a = new Article(); a.title=t; a.url=u; a.domain=o.optString("domain"); a.date=o.optString("seendate");
            out.add(a); if (out.size()>=20) break;
        }
        return out;
    }

    private String compileHtml(String q, List<Article> a) {
        if (a.isEmpty()) return errorHtml(q,"Bu konu için yeterli güncel kaynak bulunamadı.");
        List<String> facts = new ArrayList<>();
        Map<String,Integer> freq = new HashMap<>();
        for (Article x:a) {
            String t=x.title;
            facts.add(t);
            for (String w:t.toLowerCase(new Locale("tr","TR")).split("\\s+")) {
                w=w.replaceAll("[^a-zA-ZçÇğĞıİöÖşŞüÜ0-9]","");
                if (w.length()<5 || stop(w)) continue;
                freq.put(w,freq.getOrDefault(w,0)+1);
            }
        }
        List<Map.Entry<String,Integer>> keys = new ArrayList<>(freq.entrySet());
        keys.sort((x,y)->Integer.compare(y.getValue(),x.getValue()));
        StringBuilder themes = new StringBuilder();
        for (int i=0;i<Math.min(6,keys.size());i++) { if (i>0) themes.append(", "); themes.append(keys.get(i).getKey()); }

        String intro = "Son güncel kaynaklar birlikte değerlendirildiğinde " + esc(q) + " başlığında öne çıkan gelişmeler " +
                (themes.length()>0 ? themes + " ekseninde yoğunlaşıyor. " : "birkaç ana başlıkta toplanıyor. ") +
                "Aşağıdaki metin, aynı olayın tekrar eden başlıklarını ayıklayarak ortak çerçeveyi bağımsız bir haber diliyle özetler.";

        StringBuilder body = new StringBuilder();
        int used=0;
        for (String f:facts) {
            if (used>=7) break;
            String s = normalizeHeadline(f);
            if (s.length()<28) continue;
            body.append("<p>").append(esc(s)).append(".</p>"); used++;
        }
        String conclusion = "Kaynaklar arasında ayrıntı ve vurgu farklılıkları bulunabileceği için, özellikle sayı, tarih ve resmî açıklama içeren gelişmelerde aşağıdaki asıl haber bağlantılarının kontrol edilmesi önerilir.";

        StringBuilder src = new StringBuilder("<ul>");
        for (int i=0;i<Math.min(10,a.size());i++) {
            Article x=a.get(i);
            src.append("<li><a href=\"").append(escAttr(x.url)).append("\">").append(esc(x.title)).append("</a>");
            if (!TextUtils.isEmpty(x.domain)) src.append(" <span>— ").append(esc(x.domain)).append("</span>");
            src.append("</li>");
        }
        src.append("</ul>");

        return page("<article><h1>"+esc(q)+": güncel derleme</h1>"+
                "<div class='meta'>"+new SimpleDateFormat("dd MMMM yyyy HH:mm",new Locale("tr","TR")).format(new Date())+" • "+a.size()+" kaynak tarandı</div>"+
                "<div class='rule'></div><p class='lead'>"+intro+"</p>"+body+"<p>"+conclusion+"</p>"+
                "<h2>Kaynaklar</h2><div class='sources'>"+src+"</div></article>");
    }

    private String listHtml(String q,List<Article> a) {
        StringBuilder b=new StringBuilder("<h1>"+esc(q)+" haberleri</h1><div class='meta'>En yeni sonuçlar</div>");
        if (a.isEmpty()) b.append("<div class='card'>Sonuç bulunamadı.</div>");
        for (Article x:a) b.append("<div class='card'><a href=\"").append(escAttr(x.url)).append("\"><b>").append(esc(x.title)).append("</b></a><div class='meta'>").append(esc(x.domain)).append(" ").append(esc(x.date)).append("</div></div>");
        return page(b.toString());
    }

    private String page(String body) {
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>"+
                "body{margin:0;background:#f7f3ea;color:#28231f;font-family:Georgia,'Times New Roman',serif;padding:18px;}"+
                "article{max-width:850px;margin:0 auto;background:#fffdf8;padding:26px 22px 38px;border-radius:18px;box-shadow:0 4px 18px #00000012}"+
                "h1{font-size:29px;line-height:1.25;text-align:center;margin:4px 0 8px;color:#1e1b18}h2{font-size:19px;margin-top:30px;border-left:4px solid #a77a43;padding-left:10px}.meta{text-align:center;font:13px sans-serif;color:#746b62;margin:6px 0 18px}.rule{width:70px;height:2px;background:#c4aa86;margin:0 auto 24px}.lead:first-letter{float:left;font-size:50px;line-height:.9;padding:4px 7px 0 0;color:#8b5a2b;font-weight:bold}p{font-size:18px;line-height:1.85;text-align:justify;text-indent:1.45em;margin:0 0 18px}.lead{text-indent:0}.sources{background:#f2eadf;padding:13px 15px;border-radius:14px;font:14px/1.55 sans-serif}.sources a,.card a{color:#5b3b19;text-decoration:none}.card{background:#fffdf8;border-radius:14px;padding:16px;margin:12px 0;box-shadow:0 3px 12px #00000010;font:16px/1.45 sans-serif}.card .meta{text-align:left;margin:8px 0 0}ul{padding-left:20px}li{margin:8px 0}</style></head><body>"+body+"</body></html>";
    }

    private String loadingHtml(String q, boolean compile) { return page("<h1>"+(compile?"Derleniyor":"Haberler aranıyor")+"</h1><p class='lead'>"+esc(q)+" için güncel kaynaklar taranıyor.</p>"); }
    private String errorHtml(String q,String m) { return page("<h1>"+esc(q)+"</h1><div class='card'><b>İşlem tamamlanamadı</b><br>"+esc(m==null?"Ağ bağlantısını kontrol edin.":m)+"</div>"); }
    private String normalizeHeadline(String t) {
        t=t.replaceAll("\\s+[|–—-]\\s+[^|–—-]{2,60}$","").trim();
        if (t.endsWith(".")) t=t.substring(0,t.length()-1);
        if (t.length()>1) t=Character.toUpperCase(t.charAt(0))+t.substring(1);
        return t;
    }
    private boolean stop(String w) {
        String s="haber son dakika bugün hakkında için olan olarak sonra önce yeni daha göre kadar ile bir bu ve veya da de mi mı mu mü çok son tüm şimdi işte ancak yine";
        return (" "+s+" ").contains(" "+w+" ");
    }
    private String clean(String s){ return s==null?"":s.replaceAll("\\s+"," ").trim(); }
    private String esc(String s){ if(s==null)return ""; return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;"); }
    private String escAttr(String s){ return esc(s).replace("'","&#39;"); }
    private int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
    static class Article { String title,url,domain,date; }
}
