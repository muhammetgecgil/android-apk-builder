package com.muhammetgecgil.turkradyo;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(Color.rgb(32, 27, 42));
        w.setNavigationBarColor(Color.rgb(32, 27, 42));

        webView = new WebView(this);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString(s.getUserAgentString() + " MuhammetTurkRadyo/2.0.1");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return handleUri(request.getUrl()); }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) { return handleUri(Uri.parse(url)); }
        });
        try {
            String html = readGzipAsset("index.html.gz");
            webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
        } catch (Exception e) {
            webView.loadUrl("file:///android_asset/index.html");
        }

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    private boolean handleUri(Uri uri) {
        if (uri == null || !"radioapp".equalsIgnoreCase(uri.getScheme())) return false;
        String cmd = uri.getHost();
        if (cmd == null) return true;
        switch (cmd) {
            case "play": startRadio(RadioService.ACTION_PLAY, uri.getQueryParameter("url"), uri.getQueryParameter("name"), 0f); break;
            case "pause": startRadio(RadioService.ACTION_PAUSE, null, null, 0f); break;
            case "resume": startRadio(RadioService.ACTION_RESUME, null, null, 0f); break;
            case "stop": startRadio(RadioService.ACTION_STOP, null, null, 0f); break;
            case "vol": startRadio(RadioService.ACTION_VOLUME, null, null, parseFloat(uri.getQueryParameter("v"), 1f)); break;
            case "gain":
                Intent gain = new Intent(this, RadioService.class).setAction(RadioService.ACTION_GAIN);
                gain.putExtra("gain_mb", (int) parseFloat(uri.getQueryParameter("mb"), 0f));
                startRadioIntent(gain);
                break;
            case "open": openExternal(uri.getQueryParameter("url")); break;
            case "shazam": openShazam(); break;
            case "alarm": scheduleAlarm(uri, false); break;
            case "alarmclear": scheduleAlarm(uri, true); break;
            case "alarmsettings": openAlarmSettings(); break;
            case "sleep": scheduleSleep(uri, false); break;
            case "sleepclear": scheduleSleep(uri, true); break;
            default: break;
        }
        return true;
    }

    private void startRadio(String action, String url, String name, float value) {
        Intent i = new Intent(this, RadioService.class).setAction(action);
        if (url != null) i.putExtra("url", url);
        if (name != null) i.putExtra("name", name);
        if (RadioService.ACTION_VOLUME.equals(action)) i.putExtra("volume", value);
        startRadioIntent(i);
    }

    private void startRadioIntent(Intent i) {
        if (Build.VERSION.SDK_INT >= 26 && RadioService.ACTION_PLAY.equals(i.getAction())) startForegroundService(i);
        else startService(i);
    }

    private void openExternal(String url) {
        if (url == null || url.trim().isEmpty()) return;
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) { }
    }

    private void openShazam() {
        Intent launch = getPackageManager().getLaunchIntentForPackage("com.shazam.android");
        try {
            if (launch != null) startActivity(launch);
            else startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.shazam.com/")));
        } catch (Exception ignored) { }
    }

    private void openAlarmSettings() {
        if (Build.VERSION.SDK_INT >= 31) {
            try { startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()))); } catch (Exception ignored) { }
        }
    }

    private void scheduleAlarm(Uri uri, boolean clear) {
        int id = parseInt(uri.getQueryParameter("id"), 7400);
        Intent r = new Intent(this, AlarmReceiver.class).setAction("RADIO_ALARM_" + id);
        r.putExtra("url", uri.getQueryParameter("url"));
        r.putExtra("name", uri.getQueryParameter("name"));
        r.setData(Uri.parse("turkradyo://alarm/" + id));
        PendingIntent pi = PendingIntent.getBroadcast(this, id, r, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (clear) { am.cancel(pi); return; }
        long when = parseLong(uri.getQueryParameter("when"), System.currentTimeMillis() + 60_000L);
        try {
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        } catch (SecurityException e) {
            am.set(AlarmManager.RTC_WAKEUP, when, pi);
        }
    }

    private void scheduleSleep(Uri uri, boolean clear) {
        int id = parseInt(uri.getQueryParameter("id"), 7999);
        Intent r = new Intent(this, SleepReceiver.class).setAction("RADIO_SLEEP_" + id);
        r.setData(Uri.parse("turkradyo://sleep/" + id));
        PendingIntent pi = PendingIntent.getBroadcast(this, id, r, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (clear) { am.cancel(pi); return; }
        long when = parseLong(uri.getQueryParameter("when"), System.currentTimeMillis() + 900_000L);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
    }

    private String readGzipAsset(String name) throws Exception {
        try (InputStream raw = getAssets().open(name); GZIPInputStream gz = new GZIPInputStream(raw); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = gz.read(buf)) > 0) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static float parseFloat(String s, float d) { try { return Float.parseFloat(s); } catch (Exception e) { return d; } }
    private static int parseInt(String s, int d) { try { return Integer.parseInt(s); } catch (Exception e) { return d; } }
    private static long parseLong(String s, long d) { try { return Long.parseLong(s); } catch (Exception e) { return d; } }
}
