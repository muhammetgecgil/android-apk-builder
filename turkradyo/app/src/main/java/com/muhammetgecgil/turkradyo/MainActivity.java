package com.muhammetgecgil.turkradyo;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.InputStream;
import java.net.URLConnection;

public class MainActivity extends Activity {
    private WebView webView;
    private static final String ASSET_ORIGIN = "https://appassets.androidplatform.net/assets/";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        webView = new WebView(this);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadsImagesAutomatically(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        webView.setWebViewClient(new AppWebViewClient());
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 42);
        }
        webView.loadUrl(ASSET_ORIGIN + "index.html");
    }

    private final class AppWebViewClient extends WebViewClient {
        @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
            String u = req.getUrl().toString();
            if (u.startsWith(ASSET_ORIGIN)) {
                String rel = u.substring(ASSET_ORIGIN.length());
                try {
                    InputStream in = getAssets().open(rel);
                    String mime = URLConnection.guessContentTypeFromName(rel);
                    if (mime == null) mime = rel.endsWith(".html") ? "text/html" : rel.endsWith(".css") ? "text/css" : rel.endsWith(".js") ? "application/javascript" : "application/octet-stream";
                    return new WebResourceResponse(mime, "UTF-8", in);
                } catch (Exception ignored) { }
            }
            return super.shouldInterceptRequest(view, req);
        }

        @Override public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (url.endsWith("/index.html")) {
                view.evaluateJavascript("(function(){if(!document.getElementById('premiumCss')){var l=document.createElement('link');l.id='premiumCss';l.rel='stylesheet';l.href='https://appassets.androidplatform.net/assets/premium.css';document.head.appendChild(l)}if(!document.getElementById('premiumJs')){var s=document.createElement('script');s.id='premiumJs';s.src='https://appassets.androidplatform.net/assets/premium.js';document.body.appendChild(s)}if(!document.getElementById('privacyBtn')){var x=document.querySelector('.topBtns');if(x){var b=document.createElement('button');b.id='privacyBtn';b.className='iconBtn';b.textContent='Gizlilik';b.onclick=function(){location.href='https://appassets.androidplatform.net/assets/privacy.html'};x.appendChild(b)}}})();", null);
            }
        }

        @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
            Uri uri = req.getUrl();
            if ("radioapp".equalsIgnoreCase(uri.getScheme())) {
                handleNative(uri);
                return true;
            }
            if ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) {
                if (uri.toString().startsWith(ASSET_ORIGIN)) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) { }
                return true;
            }
            return true;
        }
    }

    private void handleNative(Uri uri) {
        String host = uri.getHost() == null ? "" : uri.getHost();
        switch (host) {
            case "play": {
                String url = uri.getQueryParameter("url");
                String name = uri.getQueryParameter("name");
                if (url == null || url.isEmpty()) return;
                getSharedPreferences("radio", MODE_PRIVATE).edit().putString("url", url).putString("name", name == null ? "Türk Radyo" : name).apply();
                Intent i = new Intent(this, RadioService.class).setAction(RadioService.ACTION_PLAY)
                        .putExtra("url", url).putExtra("name", name == null ? "Türk Radyo" : name);
                startForegroundServiceCompat(i);
                break;
            }
            case "pause": startService(new Intent(this, RadioService.class).setAction(RadioService.ACTION_PAUSE)); break;
            case "resume": startService(new Intent(this, RadioService.class).setAction(RadioService.ACTION_RESUME)); break;
            case "stop": startService(new Intent(this, RadioService.class).setAction(RadioService.ACTION_STOP)); break;
            case "vol": startService(new Intent(this, RadioService.class).setAction(RadioService.ACTION_VOLUME).putExtra("volume", parseFloat(uri.getQueryParameter("v"), 1f))); break;
            case "gain": startService(new Intent(this, RadioService.class).setAction(RadioService.ACTION_GAIN).putExtra("gain", parseInt(uri.getQueryParameter("mb"), 0))); break;
            case "shazam": openShazam(); break;
            case "open": {
                String url = uri.getQueryParameter("url");
                if (url != null) try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) { }
                break;
            }
            case "alarmsettings": openAlarmSettings(); break;
            case "alarm": scheduleAlarm(uri, false); break;
            case "alarmclear": cancelAlarm(uri); break;
            case "sleep": scheduleAlarm(uri, true); break;
            case "sleepclear": cancelAlarm(uri); break;
        }
    }

    private void startForegroundServiceCompat(Intent i) {
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }

    private void openShazam() {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage("com.shazam.android");
            if (launch != null) startActivity(launch); else startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.shazam.com/")));
        } catch (Exception ignored) { }
    }

    private void openAlarmSettings() {
        if (Build.VERSION.SDK_INT >= 31) {
            try { startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()))); } catch (Exception ignored) { }
        }
    }

    private void scheduleAlarm(Uri uri, boolean sleep) {
        long when = parseLong(uri.getQueryParameter("when"), 0L);
        int id = parseInt(uri.getQueryParameter("id"), sleep ? 7999 : 7400);
        if (when <= System.currentTimeMillis()) return;
        Intent r = new Intent(this, AlarmReceiver.class).putExtra("sleep", sleep);
        if (!sleep) {
            r.putExtra("url", uri.getQueryParameter("url"));
            r.putExtra("name", uri.getQueryParameter("name"));
        }
        PendingIntent pi = PendingIntent.getBroadcast(this, id, r, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        }
    }

    private void cancelAlarm(Uri uri) {
        int id = parseInt(uri.getQueryParameter("id"), 7400);
        PendingIntent pi = PendingIntent.getBroadcast(this, id, new Intent(this, AlarmReceiver.class), PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) ((AlarmManager)getSystemService(ALARM_SERVICE)).cancel(pi);
    }

    private static int parseInt(String s, int d) { try { return Integer.parseInt(s); } catch (Exception e) { return d; } }
    private static long parseLong(String s, long d) { try { return Long.parseLong(s); } catch (Exception e) { return d; } }
    private static float parseFloat(String s, float d) { try { return Float.parseFloat(s); } catch (Exception e) { return d; } }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
}
