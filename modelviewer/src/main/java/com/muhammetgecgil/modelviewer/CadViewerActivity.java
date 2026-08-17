package com.muhammetgecgil.modelviewer;

import android.app.Activity;
import android.os.Bundle;
import android.net.Uri;
import android.webkit.*;
import android.content.*;
import android.graphics.Color;
import java.io.*;
import java.util.*;

public class CadViewerActivity extends Activity {
    private WebView web;
    private Uri modelUri;
    private String modelName;
    private String modelType;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.BLACK);
        modelUri = getIntent().getData();
        modelName = getIntent().getStringExtra("name");
        modelType = getIntent().getStringExtra("type");
        if (modelName == null) modelName = "CAD Model";
        if (modelType == null) modelType = "step";

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        if (android.os.Build.VERSION.SDK_INT >= 26) s.setSafeBrowsingEnabled(true);

        web.setBackgroundColor(Color.rgb(3, 8, 15));
        web.addJavascriptInterface(new Object() {
            @JavascriptInterface public void closeViewer() { runOnUiThread(() -> finish()); }
        }, "AndroidHost");

        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                try {
                    Uri u = req.getUrl();
                    if (!"mg3d.local".equalsIgnoreCase(u.getHost())) return null;
                    String p = u.getPath();
                    if ("/model.bin".equals(p)) {
                        InputStream in = getContentResolver().openInputStream(modelUri);
                        return resp("application/octet-stream", in);
                    }
                    if (p == null || "/".equals(p)) p = "/index.html";
                    String asset = "cadviewer" + p;
                    return resp(mime(asset), getAssets().open(asset));
                } catch (Throwable e) {
                    return new WebResourceResponse("text/plain", "utf-8",
                        new ByteArrayInputStream(("Load error: " + e.getMessage()).getBytes()));
                }
            }
        });
        setContentView(web);
        String url = "https://mg3d.local/index.html?type=" + Uri.encode(modelType) +
                "&name=" + Uri.encode(modelName);
        web.loadUrl(url);
    }

    private WebResourceResponse resp(String mime, InputStream in) {
        Map<String,String> h = new HashMap<>();
        h.put("Access-Control-Allow-Origin", "*");
        h.put("Cache-Control", "no-cache");
        return new WebResourceResponse(mime, "utf-8", 200, "OK", h, in);
    }
    private String mime(String p) {
        p = p.toLowerCase(Locale.ROOT);
        if (p.endsWith(".html")) return "text/html";
        if (p.endsWith(".js")) return "application/javascript";
        if (p.endsWith(".wasm")) return "application/wasm";
        if (p.endsWith(".css")) return "text/css";
        if (p.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }
    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
