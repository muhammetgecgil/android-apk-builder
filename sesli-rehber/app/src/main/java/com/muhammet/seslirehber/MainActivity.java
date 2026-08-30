package com.muhammet.seslirehber;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener, SensorEventListener {
    private static final int REQ_PERMS = 40;
    private Sensor accelerometer;
    private boolean commandMode;
    private long freeFallAt;
    private long lastFallAlert;
    private boolean listening;
    private Button mic;
    private TextView mode;
    private Runnable pendingAction;
    private SharedPreferences prefs;
    private SpeechRecognizer recognizer;
    private boolean resumed;
    private SensorManager sensorManager;
    private TextView status;
    private TextToSpeech tts;

    private final Locale f1tr = new Locale("tr", "TR");
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean wakeMode = true;
    private final RecognitionListener listener = new AnonymousClass2();

    static Handler access$000(MainActivity mainActivity) {
        return mainActivity.handler;
    }

    static boolean access$100(MainActivity mainActivity) {
        return mainActivity.resumed;
    }

    static void access$1000(MainActivity mainActivity, String str) throws NumberFormatException {
        mainActivity.handle(str);
    }

    static void access$1100(MainActivity mainActivity, long j) {
        mainActivity.vibrate(j);
    }

    static void access$1200(MainActivity mainActivity, String str) {
        mainActivity.speak(str);
    }

    static boolean access$200(MainActivity mainActivity) {
        return mainActivity.wakeMode;
    }

    static boolean access$300(MainActivity mainActivity) {
        return mainActivity.listening;
    }

    static boolean access$302(MainActivity mainActivity, boolean z) {
        mainActivity.listening = z;
        return z;
    }

    static void access$400(MainActivity mainActivity, boolean z) {
        mainActivity.startListening(z);
    }

    static TextView access$500(MainActivity mainActivity) {
        return mainActivity.status;
    }

    static Button access$600(MainActivity mainActivity) {
        return mainActivity.mic;
    }

    static void access$700(MainActivity mainActivity) {
        mainActivity.restartWake();
    }

    static Locale access$800(MainActivity mainActivity) {
        return mainActivity.f1tr;
    }

    static boolean access$900(MainActivity mainActivity) {
        return mainActivity.commandMode;
    }

    static boolean access$902(MainActivity mainActivity, boolean z) {
        mainActivity.commandMode = z;
        return z;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().addFlags(128);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        this.prefs = getSharedPreferences("guide", 0);
        buildUi();
        this.tts = new TextToSpeech(this, this);
        this.recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        this.recognizer.setRecognitionListener(this.listener);
        this.sensorManager = (SensorManager) getSystemService("sensor");
        this.accelerometer = this.sensorManager.getDefaultSensor(1);
        requestNeededPermissions();
    }

    private void buildUi() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(17);
        linearLayout.setPadding(34, 35, 34, 35);
        linearLayout.setBackgroundColor(-16777216);
        TextView textView = new TextView(this);
        textView.setText("SESLİ REHBER v3.0");
        textView.setTextColor(-1);
        textView.setTextSize(29.0f);
        textView.setGravity(17);
        textView.setContentDescription("Sesli Rehber sürüm 3.0, bağlamsal niyet anlama ve Hareket Görüş etkin");
        linearLayout.addView(textView, new LinearLayout.LayoutParams(-1, -2));
        this.mode = new TextView(this);
        this.mode.setText("HEY REHBER: AÇIK");
        this.mode.setTextColor(Color.rgb(255, 214, 0));
        this.mode.setTextSize(17.0f);
        this.mode.setGravity(17);
        this.mode.setPadding(0, 14, 0, 0);
        linearLayout.addView(this.mode, new LinearLayout.LayoutParams(-1, -2));
        this.status = new TextView(this);
        this.status.setText("Hazır\n“Hey Rehber” deyin");
        this.status.setTextColor(-1);
        this.status.setTextSize(21.0f);
        this.status.setGravity(17);
        this.status.setPadding(0, 34, 0, 28);
        linearLayout.addView(this.status, new LinearLayout.LayoutParams(-1, -2));
        this.mic = new Button(this);
        this.mic.setText("DİNLE");
        this.mic.setTextSize(28.0f);
        this.mic.setTextColor(-16777216);
        this.mic.setAllCaps(false);
        this.mic.setContentDescription("Komut vermek için iki kez dokunun");
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(1);
        gradientDrawable.setColor(Color.rgb(255, 214, 0));
        gradientDrawable.setStroke(8, -1);
        this.mic.setBackground(gradientDrawable);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(m0dp(220), m0dp(220));
        layoutParams.gravity = 17;
        this.mic.setLayoutParams(layoutParams);
        this.mic.setOnClickListener(new MainActivity$$ExternalSyntheticLambda5(this));
        this.mic.setOnLongClickListener(new MainActivity$$ExternalSyntheticLambda6(this));
        linearLayout.addView(this.mic);
        Button button = new Button(this);
        button.setText("GÜVENLİ YÜRÜYÜŞ PİLOTU");
        button.setTextSize(17.0f);
        button.setAllCaps(false);
        button.setContentDescription("Hareket Görüş kamera pilotunu aç");
        LinearLayout.LayoutParams safetyParams = new LinearLayout.LayoutParams(-1, m0dp(58));
        safetyParams.setMargins(0, m0dp(22), 0, 0);
        button.setLayoutParams(safetyParams);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchVisionPilot();
            }
        });
        linearLayout.addView(button);
        TextView textView2 = new TextView(this);
        textView2.setText("Bağlamsal niyet anlama + Hareket Görüş V14\nEksik bilgiyi sorar • Kritik işlemlerde sesli onay\nPilot kamera modu bastonun yerine geçmez");
        textView2.setTextColor(-3355444);
        textView2.setTextSize(16.0f);
        textView2.setGravity(17);
        textView2.setPadding(0, 32, 0, 0);
        linearLayout.addView(textView2, new LinearLayout.LayoutParams(-1, -2));
        setContentView(linearLayout);
    }

    void m0lambda$buildUi$0$commuhammetseslirehberMainActivity(View view) {
        this.commandMode = true;
        startListening(false);
    }

    boolean m1lambda$buildUi$1$commuhammetseslirehberMainActivity(View view) {
        speakHelp();
        return true;
    }

    private int m0dp(int i) {
        return Math.round(i * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onInit(int i) {
        if (i != 0) {
            return;
        }
        this.tts.setLanguage(this.f1tr);
        this.tts.setSpeechRate(0.93f);
        this.tts.setOnUtteranceProgressListener(new AnonymousClass1());
        speak("Sesli Rehber sürüm 3 hazır. Bağlamsal niyet anlama ve Hareket Görüş pilotu etkin.");
    }

    class AnonymousClass1 extends UtteranceProgressListener {
        AnonymousClass1() {
        }

        @Override
        public void onStart(String str) {
        }

        @Override
        public void onError(String str) {
            onDone(str);
        }

        void m11lambda$onDone$0$commuhammetseslirehberMainActivity$1() {
            if (MainActivity.access$100(MainActivity.this) && MainActivity.access$200(MainActivity.this) && !MainActivity.access$300(MainActivity.this)) {
                MainActivity.access$400(MainActivity.this, true);
            }
        }

        @Override
        public void onDone(String str) {
            MainActivity.access$000(MainActivity.this).postDelayed(new MainActivity$1$$ExternalSyntheticLambda0(this), 500L);
        }
    }

    private void requestNeededPermissions() {
        requestPermissions(new String[]{"android.permission.RECORD_AUDIO", "android.permission.READ_CONTACTS", "android.permission.CALL_PHONE", "android.permission.CAMERA", "android.permission.ACCESS_FINE_LOCATION"}, REQ_PERMS);
    }

    private void startListening(boolean z) {
        if (!this.resumed || this.listening || this.recognizer == null) {
            return;
        }
        if (this.tts != null && this.tts.isSpeaking()) {
            return;
        }
        if (checkSelfPermission("android.permission.RECORD_AUDIO") != 0) {
            requestNeededPermissions();
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            this.status.setText("Konuşma tanıma kullanılamıyor");
            return;
        }
        Intent intent = new Intent("android.speech.action.RECOGNIZE_SPEECH");
        intent.putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form");
        intent.putExtra("android.speech.extra.LANGUAGE", "tr-TR");
        intent.putExtra("android.speech.extra.MAX_RESULTS", 5);
        intent.putExtra("android.speech.extra.PARTIAL_RESULTS", false);
        intent.putExtra("android.speech.extra.PREFER_OFFLINE", true);
        this.recognizer.startListening(intent);
        this.listening = true;
        this.status.setText(z ? "“Hey Rehber” bekleniyor…" : "Dinliyorum…");
        this.mic.setText(z ? "HAZIR" : "DUR");
        if (!z) {
            vibrate(65L);
        }
    }

    class AnonymousClass2 implements RecognitionListener {
        AnonymousClass2() {
        }

        @Override
        public void onReadyForSpeech(Bundle bundle) {
        }

        @Override
        public void onBeginningOfSpeech() {
            MainActivity.access$500(MainActivity.this).setText("Sizi duyuyorum…");
        }

        @Override
        public void onRmsChanged(float f) {
        }

        @Override
        public void onBufferReceived(byte[] bArr) {
        }

        @Override
        public void onEndOfSpeech() {
            MainActivity.access$500(MainActivity.this).setText("Anlıyorum…");
        }

        void m12lambda$onError$0$commuhammetseslirehberMainActivity$2() {
            MainActivity.access$400(MainActivity.this, true);
        }

        @Override
        public void onError(int i) {
            MainActivity.access$302(MainActivity.this, false);
            MainActivity.access$600(MainActivity.this).setText("DİNLE");
            if (MainActivity.access$100(MainActivity.this) && MainActivity.access$200(MainActivity.this)) {
                MainActivity.access$000(MainActivity.this).postDelayed(new MainActivity$2$$ExternalSyntheticLambda0(this), 900L);
            }
        }

        @Override
        public void onResults(Bundle bundle) throws NumberFormatException {
            MainActivity.access$302(MainActivity.this, false);
            MainActivity.access$600(MainActivity.this).setText("DİNLE");
            ArrayList<String> stringArrayList = bundle.getStringArrayList("results_recognition");
            if (stringArrayList == null || stringArrayList.isEmpty()) {
                MainActivity.access$700(MainActivity.this);
                return;
            }
            String str = stringArrayList.get(0);
            String strTrim = str.toLowerCase(MainActivity.access$800(MainActivity.this)).trim();
            if (MainActivity.access$900(MainActivity.this)) {
                MainActivity.access$902(MainActivity.this, false);
                MainActivity.access$500(MainActivity.this).setText("Duydum: “" + str + "”");
                MainActivity.access$1000(MainActivity.this, str);
                return;
            }
            int iIndexOf = strTrim.indexOf("hey rehber");
            if (iIndexOf < 0) {
                iIndexOf = strTrim.indexOf("ey rehber");
            }
            if (iIndexOf >= 0) {
                String strTrim2 = strTrim.substring(iIndexOf + (strTrim.startsWith("hey rehber", iIndexOf) ? 10 : 9)).trim();
                MainActivity.access$1100(MainActivity.this, 90L);
                if (!strTrim2.isEmpty()) {
                    MainActivity.access$500(MainActivity.this).setText("Duydum: “" + strTrim2 + "”");
                    MainActivity.access$1000(MainActivity.this, strTrim2);
                    return;
                } else {
                    MainActivity.access$902(MainActivity.this, true);
                    MainActivity.access$1200(MainActivity.this, "Dinliyorum.");
                    return;
                }
            }
            MainActivity.access$700(MainActivity.this);
        }

        @Override
        public void onPartialResults(Bundle bundle) {
        }

        @Override
        public void onEvent(int i, Bundle bundle) {
        }
    }

    private void restartWake() {
        if (this.resumed && this.wakeMode) {
            this.handler.postDelayed(new MainActivity$$ExternalSyntheticLambda7(this), 600L);
        }
    }

    void m8lambda$restartWake$2$commuhammetseslirehberMainActivity() {
        startListening(true);
    }

    private void handle(String str) throws NumberFormatException {
        String strTrim = str.toLowerCase(this.f1tr).trim();
        if (this.pendingAction != null) {
            if (has(strTrim, "evet", "onayla", "ara")) {
                Runnable runnable = this.pendingAction;
                this.pendingAction = null;
                speak("Onaylandı.");
                runnable.run();
                return;
            }
            if (!has(strTrim, "hayır", "iptal", "vazgeç", "iyiyim")) {
                speak("Bekleyen işlem var. Evet veya iptal deyin.");
                return;
            } else {
                this.pendingAction = null;
                speak("İşlem iptal edildi. İyi olmanıza sevindim.");
                return;
            }
        }
        if (IntentEngine.hasPending() && has(strTrim, "iptal", "vazgeç", "boş ver")) {
            IntentEngine.cancelPending();
            speak("Niyet işlemi iptal edildi.");
            return;
        }
        IntentEngine.Result resultUnderstand = IntentEngine.understand(str);
        if (resultUnderstand.clarification != null) {
            this.commandMode = true;
            speak(resultUnderstand.clarification);
            return;
        }
        if (!resultUnderstand.command.equals(str)) {
            str = resultUnderstand.command;
            strTrim = str.toLowerCase(this.f1tr).trim();
            this.status.setText("Niyet: " + resultUnderstand.intent + " • Güven: yüzde " + Math.round(resultUnderstand.confidence * 100.0f));
        }
        if (has(strTrim, "yardım", "ne yapabilirsin", "komutlar")) {
            speakHelp();
            return;
        }
        if (strTrim.contains("hey rehber modunu kapat")) {
            this.wakeMode = false;
            this.mode.setText("HEY REHBER: KAPALI");
            if (this.listening) {
                this.recognizer.cancel();
            }
            speak("Hey Rehber modu kapatıldı. Ortadaki düğmeyle konuşabilirsiniz.");
            return;
        }
        if (strTrim.contains("hey rehber modunu aç")) {
            this.wakeMode = true;
            this.mode.setText("HEY REHBER: AÇIK");
            speak("Hey Rehber modu açıldı.");
            return;
        }
        if (strTrim.contains("saat")) {
            speak("Saat " + new SimpleDateFormat("HH:mm", this.f1tr).format(new Date()));
            return;
        }
        if (strTrim.contains("tarih") || strTrim.contains("bugün günlerden")) {
            speak("Bugün " + new SimpleDateFormat("d MMMM EEEE", this.f1tr).format(new Date()));
            return;
        }
        if (has(strTrim, "pil", "şarj")) {
            speak("Pil yüzde " + ((BatteryManager) getSystemService("batterymanager")).getIntProperty(4));
            return;
        }
        if (has(strTrim, "sesi yükselt", "ses aç")) {
            adjustVolume(1);
            return;
        }
        if (has(strTrim, "sesi azalt", "ses kıs")) {
            adjustVolume(-1);
            return;
        }
        if (has(strTrim, "feneri aç", "ışığı aç")) {
            setTorch(true);
            return;
        }
        if (has(strTrim, "feneri kapat", "ışığı kapat")) {
            setTorch(false);
            return;
        }
        if (has(strTrim, "ekranı oku", "ekrani oku")) {
            readScreen();
            return;
        }
        if (strTrim.contains("wifi ayar")) {
            safeStart(new Intent("android.settings.WIFI_SETTINGS"), "Wi-Fi ayarları açılıyor.");
            return;
        }
        if (strTrim.contains("bluetooth ayar")) {
            safeStart(new Intent("android.settings.BLUETOOTH_SETTINGS"), "Bluetooth ayarları açılıyor.");
            return;
        }
        if (strTrim.contains("erişilebilirlik ayar")) {
            safeStart(new Intent("android.settings.ACCESSIBILITY_SETTINGS"), "Erişilebilirlik ayarları açılıyor.");
            return;
        }
        if (has(strTrim, "güvenli yürüyüş", "hareket görüş", "önümde ne var", "çevreyi anlat")) {
            launchVisionPilot();
            return;
        }
        if (has(strTrim, "nesneyi tanı", "parayı tanı", "banknotu tanı", "kamerayla bak", "yazıyı oku")) {
            openVisualAssistant(strTrim);
            return;
        }
        if (has(strTrim, "konumumu paylaş", "neredeyim")) {
            shareLocation(strTrim.contains("paylaş"));
            return;
        }
        if (strTrim.contains("yol tarifi") || strTrim.contains("beni götür") || strTrim.endsWith(" götür")) {
            navigate(strTrim);
            return;
        }
        if (strTrim.contains("whatsapp") && has(strTrim, "mesaj", "gönder", "paylaş")) {
            sendWhatsApp(str);
            return;
        }
        if (strTrim.contains("mesaj gönder") || strTrim.contains("sms gönder")) {
            sendSms(str);
            return;
        }
        if (has(strTrim, "alarm kur", "hatırlat", "ilacımı", "ilacımı")) {
            setReminder(str);
            return;
        }
        if (has(strTrim, "dolandırıcılık kontrolü", "dolandırıcı mı", "mesajı kontrol et", "panoyu kontrol et")) {
            fraudCheck(str);
            return;
        }
        if (strTrim.startsWith("acil kişiyi ayarla")) {
            saveEmergency(strTrim);
            return;
        }
        if (has(strTrim, "yardım et", "acil durum", "112'yi ara", "112 yi ara")) {
            emergency();
            return;
        }
        if (strTrim.endsWith(" ara") || strTrim.startsWith("ara ")) {
            callContact(strTrim.replaceFirst("^ara ", "").replaceFirst(" ara$", "").trim());
            return;
        }
        if (strTrim.startsWith("internette ara ") || strTrim.startsWith("google'da ara ")) {
            String strSubstring = strTrim.substring(strTrim.indexOf("ara ") + 4);
            Intent intent = new Intent("android.intent.action.WEB_SEARCH");
            intent.putExtra("query", strSubstring);
            safeStart(intent, "İnternet araması açılıyor.");
            return;
        }
        if (strTrim.contains(" aç") || strTrim.startsWith("aç ")) {
            openApp(strTrim.replace("uygulamasını", "").replaceFirst("^aç ", "").replaceFirst(" aç$", "").trim());
        } else {
            speak("Bu komutu anlayamadım. Yardım diyerek örnek komutları dinleyebilirsiniz.");
        }
    }

    private boolean has(String str, String... strArr) {
        for (String str2 : strArr) {
            if (str.contains(str2)) {
                return true;
            }
        }
        return false;
    }

    private void speakHelp() {
        speak("Örnek komutlar: Güvenli yürüyüşü aç. Önümde ne var. Annemi ara. Ahmet'e mesaj gönder, eve geliyorum. Kadıköy'e yaya yol tarifi. Saat 9'a ilaç alarmı kur. Parayı tanı. Panoyu dolandırıcılık için kontrol et. Feneri aç. Ekranı oku. Konumumu paylaş. Yardım et.");
    }

    private void readScreen() {
        String strVisibleText = ScreenReaderService.visibleText();
        if (strVisibleText != null) {
            speak(strVisibleText);
        } else {
            speak("Ekran okuma kapalı. Ayarlardan Sesli Rehber ekran okuma hizmetini etkinleştirin.");
            safeStart(new Intent("android.settings.ACCESSIBILITY_SETTINGS"), "Erişilebilirlik ayarları açılıyor.");
        }
    }

    private void adjustVolume(int i) {
        ((AudioManager) getSystemService("audio")).adjustStreamVolume(3, i, 1);
        speak(i > 0 ? "Ses yükseltildi." : "Ses azaltıldı.");
    }

    private void setTorch(boolean z) {
        try {
            CameraManager cameraManager = (CameraManager) getSystemService("camera");
            for (String str : cameraManager.getCameraIdList()) {
                try {
                    cameraManager.setTorchMode(str, z);
                    speak(z ? "Fener açıldı." : "Fener kapatıldı.");
                    return;
                } catch (Exception e) {
                }
            }
            speak("Uygun fener bulunamadı.");
        } catch (Exception e2) {
            speak("Fener kontrol edilemedi.");
        }
    }

    private void openVisualAssistant(String str) {
        try {
            Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("googleapp://lens"));
            intent.setPackage("com.google.android.googlequicksearchbox");
            startActivity(intent);
            speak((str.contains("para") || str.contains("banknot")) ? "Banknot tanıma açılıyor. Sonucu elinizle ve mümkünse başka biriyle doğrulayın." : "Görsel tanıma açılıyor. Kamerayı nesneye doğru tutun.");
        } catch (Exception e) {
            safeStart(new Intent("android.media.action.IMAGE_CAPTURE"), "Kamera açılıyor. Görsel tanıma uygulaması bulunamadı.");
        }
    }

    private void launchVisionPilot() {
        if (checkSelfPermission("android.permission.CAMERA") != 0) {
            requestPermissions(new String[]{"android.permission.CAMERA"}, REQ_PERMS);
            speak("Hareket Görüş için kamera izni verin, sonra güvenli yürüyüş komutunu yeniden söyleyin.");
            return;
        }
        Intent intent = new Intent(this, VisionActivity.class);
        safeStart(intent, "Hareket Görüş pilotu açılıyor. Bu ekran yalnızca hareketi izler; çukur, araç veya güvenli geçiş garantisi vermez. Beyaz bastonla doğrulayın.");
    }

    private void navigate(String str) {
        String strTrim = str.replace("yol tarifi", "").replace("beni", "").replace("götür", "").replace("ver", "").trim();
        if (strTrim.isEmpty()) {
            speak("Nereye gitmek istediğinizi söyleyin.");
            return;
        }
        Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("google.navigation:q=" + Uri.encode(strTrim) + "&mode=w"));
        intent.setPackage("com.google.android.apps.maps");
        safeStart(intent, strTrim + " için yaya yol tarifi açılıyor.");
    }

    private void sendWhatsApp(String str) {
        String strTrim = str.replaceAll("(?i)whatsapp('|’)?ta?", "").replaceAll("(?i)mesaj|gönder|paylaş", "").trim();
        if (strTrim.isEmpty()) {
            strTrim = "Merhaba";
        }
        confirm("WhatsApp'ta şu mesaj paylaşılacak: " + strTrim + ". Onaylıyor musunuz?", new MainActivity$$ExternalSyntheticLambda4(this, strTrim));
    }

    void m10lambda$sendWhatsApp$3$commuhammetseslirehberMainActivity(String str) {
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("text/plain");
        intent.putExtra("android.intent.extra.TEXT", str);
        intent.setPackage("com.whatsapp");
        safeStart(intent, "WhatsApp açılıyor.");
    }

    private void sendSms(String str) {
        int i;
        String lowerCase = str.toLowerCase(this.f1tr);
        int iIndexOf = lowerCase.indexOf("mesaj gönder");
        if (iIndexOf < 0) {
            iIndexOf = lowerCase.indexOf("sms gönder");
            i = 10;
        } else {
            i = 12;
        }
        String strTrim = iIndexOf > 0 ? str.substring(0, iIndexOf).trim() : "";
        String strTrim2 = iIndexOf >= 0 ? str.substring(Math.min(str.length(), iIndexOf + i)).trim() : "";
        String strTrim3 = strTrim.replaceAll("(?i)(kişisine|kişiye)$", "").replaceAll("(?i)('e|'a|ye|ya)$", "").trim();
        if (strTrim3.isEmpty()) {
            speak("Mesaj göndereceğiniz kişinin adını da söyleyin.");
            return;
        }
        String strFindContactNumber = findContactNumber(strTrim3);
        if (strFindContactNumber == null) {
            speak(strTrim3 + " rehberde bulunamadı.");
            return;
        }
        if (strTrim2.isEmpty()) {
            strTrim2 = "Merhaba";
        }
        confirm(strTrim3 + " kişisine şu mesaj hazırlanacak: " + strTrim2 + ". Onaylıyor musunuz?", new MainActivity$$ExternalSyntheticLambda1(this, strFindContactNumber, strTrim2));
    }

    void m9lambda$sendSms$4$commuhammetseslirehberMainActivity(String str, String str2) {
        Intent intent = new Intent("android.intent.action.SENDTO", Uri.parse("smsto:" + Uri.encode(str)));
        intent.putExtra("sms_body", str2);
        safeStart(intent, "Mesaj ekranı açılıyor. Göndermeden önce son kez kontrol edin.");
    }

    private String findContactNumber(String str) {
        String string = null;
        if (checkSelfPermission("android.permission.READ_CONTACTS") != 0) {
            requestNeededPermissions();
            return null;
        }
        Cursor cursorQuery = getContentResolver().query(Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(str)), new String[]{"data1"}, null, null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    string = cursorQuery.getString(0);
                }
            } catch (Throwable th) {
                if (cursorQuery != null) {
                    try {
                        cursorQuery.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        return string;
    }

    private void callContact(String str) {
        if (str.matches("[0-9 +]+")) {
            confirm(str + " aranacak. Onaylıyor musunuz?", new MainActivity$$ExternalSyntheticLambda9(this, str));
            return;
        }
        String strFindContactNumber = findContactNumber(str);
        if (strFindContactNumber == null) {
            speak(str + " rehberde bulunamadı.");
        } else {
            confirm(str + " aranacak. Onaylıyor musunuz?", new MainActivity$$ExternalSyntheticLambda10(this, strFindContactNumber));
        }
    }

    void m2lambda$callContact$5$commuhammetseslirehberMainActivity(String str) {
        callNumber(str.replace(" ", ""));
    }

    void m3lambda$callContact$6$commuhammetseslirehberMainActivity(String str) {
        callNumber(str);
    }

    private void callNumber(String str) {
        safeStart(new Intent(checkSelfPermission("android.permission.CALL_PHONE") == 0 ? "android.intent.action.CALL" : "android.intent.action.DIAL", Uri.parse("tel:" + Uri.encode(str))), "Arama başlatılıyor.");
    }

    private void setReminder(String str) throws NumberFormatException {
        int i;
        Matcher matcher = Pattern.compile("(?:saat\\s*)?(\\d{1,2})(?:[:.]([0-5]\\d))?").matcher(str);
        if (matcher.find()) {
            int i2 = Integer.parseInt(matcher.group(1));
            if (matcher.group(2) == null) {
                i = 0;
            } else {
                i = Integer.parseInt(matcher.group(2));
            }
            if (i2 < 0 || i2 > 23) {
                speak("Hatırlatma saatini örneğin saat 9 30 diye söyleyin.");
                return;
            }
            Intent intent = new Intent("android.intent.action.SET_ALARM");
            intent.putExtra("android.intent.extra.alarm.HOUR", i2);
            intent.putExtra("android.intent.extra.alarm.MINUTES", i);
            intent.putExtra("android.intent.extra.alarm.MESSAGE", str);
            intent.putExtra("android.intent.extra.alarm.SKIP_UI", false);
            safeStart(intent, String.format(this.f1tr, "Saat %02d:%02d için alarm hazırlanıyor.", Integer.valueOf(i2), Integer.valueOf(i)));
            return;
        }
        speak("Hatırlatma saatini örneğin saat 9 30 diye söyleyin.");
    }

    private void fraudCheck(String str) {
        ClipData primaryClip;
        int iIntValue = 0;
        if (has(str.toLowerCase(this.f1tr), "panoyu", "mesajı kontrol") && (primaryClip = ((ClipboardManager) getSystemService("clipboard")).getPrimaryClip()) != null && primaryClip.getItemCount() > 0) {
            str = String.valueOf(primaryClip.getItemAt(0).coerceToText(this));
        }
        String strNormalize = normalize(str);
        LinkedHashMap<String, Integer> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put("sifre", 3);
        linkedHashMap.put("dogrulama kodu", 3);
        linkedHashMap.put("iban", 2);
        linkedHashMap.put("hemen", 1);
        linkedHashMap.put("polis", 2);
        linkedHashMap.put("savci", 2);
        linkedHashMap.put("hesabiniz kapatilacak", 3);
        linkedHashMap.put("linke tikla", 3);
        linkedHashMap.put("odul kazandiniz", 3);
        linkedHashMap.put("para gonder", 3);
        linkedHashMap.put("uzaktan baglan", 3);
        ArrayList<String> arrayList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : linkedHashMap.entrySet()) {
            if (strNormalize.contains((CharSequence) entry.getKey())) {
                iIntValue += entry.getValue().intValue();
                arrayList.add(entry.getKey());
            }
        }
        if (iIntValue >= 5) {
            speak("Yüksek dolandırıcılık riski. Para göndermeyin, şifre veya doğrulama kodu paylaşmayın. Mesajda şüpheli ifadeler var: " + join(arrayList) + ". Kurumu resmi numarasından kendiniz arayın.");
        } else if (iIntValue < 2) {
            speak("Belirgin dolandırıcılık işareti bulamadım; ancak bu kesin güvenli olduğu anlamına gelmez.");
        } else {
            speak("Bu içerik şüpheli olabilir. Tespit edilen ifadeler: " + join(arrayList) + ". Linke dokunmadan ve bilgi vermeden doğrulama yapın.");
        }
    }

    private String join(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String str : list) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(str);
        }
        return sb.toString();
    }

    private void saveEmergency(String str) {
        String str2;
        String strReplaceAll = str.replaceAll("[^0-9+]", "");
        if (strReplaceAll.length() < 10) {
            str2 = "Acil kişinin telefon numarasını rakamlarla söyleyin.";
        } else {
            this.prefs.edit().putString("emergency", strReplaceAll).apply();
            str2 = "Acil kişi numarası kaydedildi.";
        }
        speak(str2);
    }

    private void emergency() {
        String string = this.prefs.getString("emergency", "");
        if (string.isEmpty()) {
            confirm("Acil kişi kayıtlı değil. 112 aranacak. Onaylıyor musunuz?", new MainActivity$$ExternalSyntheticLambda2(this));
        } else {
            confirm("Kayıtlı acil kişi aranacak. Onaylıyor musunuz?", new MainActivity$$ExternalSyntheticLambda3(this, string));
        }
    }

    void m4lambda$emergency$7$commuhammetseslirehberMainActivity() {
        callNumber("112");
    }

    void m5lambda$emergency$8$commuhammetseslirehberMainActivity(String str) {
        callNumber(str);
    }

    private void shareLocation(boolean z) {
        if (checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") != 0) {
            requestNeededPermissions();
            return;
        }
        LocationManager locationManager = (LocationManager) getSystemService("location");
        Location location = null;
        try {
            Iterator<String> it = locationManager.getProviders(true).iterator();
            while (it.hasNext()) {
                Location lastKnownLocation = locationManager.getLastKnownLocation(it.next());
                if (lastKnownLocation != null && (location == null || lastKnownLocation.getAccuracy() < location.getAccuracy())) {
                    location = lastKnownLocation;
                }
            }
        } catch (SecurityException e) {
        }
        if (location == null) {
            speak("Güncel konum alınamadı. Konumu açıp tekrar deneyin.");
            return;
        }
        String str = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
        if (!z) {
            speak("Konumunuz haritada açılıyor.");
            safeStart(new Intent("android.intent.action.VIEW", Uri.parse(str)), "Harita açılıyor.");
        } else {
            Intent intent = new Intent("android.intent.action.SEND");
            intent.setType("text/plain");
            intent.putExtra("android.intent.extra.TEXT", "Konumum: " + str);
            safeStart(Intent.createChooser(intent, "Konumu paylaş"), "Konum paylaşım seçenekleri açılıyor.");
        }
    }

    private void openApp(String str) {
        PackageManager packageManager = getPackageManager();
        String strNormalize = normalize(str);
        for (ApplicationInfo applicationInfo : packageManager.getInstalledApplications(0)) {
            String strValueOf = String.valueOf(packageManager.getApplicationLabel(applicationInfo));
            if (normalize(strValueOf).contains(strNormalize) || strNormalize.contains(normalize(strValueOf))) {
                Intent launchIntentForPackage = packageManager.getLaunchIntentForPackage(applicationInfo.packageName);
                if (launchIntentForPackage != null) {
                    safeStart(launchIntentForPackage, strValueOf + " açılıyor.");
                    return;
                }
            }
        }
        speak(str + " uygulaması bulunamadı.");
    }

    private String normalize(String str) {
        return str.toLowerCase(this.f1tr).replace("ı", "i").replace("ş", "s").replace("ğ", "g").replace("ü", "u").replace("ö", "o").replace("ç", "c").trim();
    }

    private void confirm(String str, Runnable runnable) {
        this.pendingAction = runnable;
        this.commandMode = true;
        this.status.setText(str + "\nEvet veya iptal deyin");
        speak(str + " Evet veya iptal deyin.");
    }

    private void safeStart(Intent intent, String str) {
        try {
            startActivity(intent);
            speak(str);
        } catch (ActivityNotFoundException e) {
            speak("Bu işlemi yapacak uygulama bulunamadı.");
        } catch (Exception e2) {
            speak("İşlem açılamadı.");
        }
    }

    private void speak(String str) {
        this.status.setText(str);
        if (this.listening && this.recognizer != null) {
            this.recognizer.cancel();
            this.listening = false;
        }
        if (this.tts != null) {
            this.tts.stop();
            this.tts.speak(str, 0, null, "guide_" + System.currentTimeMillis());
        }
    }

    private void vibrate(long j) {
        Vibrator vibrator = (Vibrator) getSystemService("vibrator");
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        vibrator.vibrate(VibrationEffect.createOneShot(j, -1));
    }

    void m7lambda$onSensorChanged$9$commuhammetseslirehberMainActivity(String str) {
        if (str.isEmpty()) {
            str = "112";
        }
        callNumber(str);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() != 1) {
            return;
        }
        double dSqrt = Math.sqrt((sensorEvent.values[0] * sensorEvent.values[0]) + (sensorEvent.values[1] * sensorEvent.values[1]) + (sensorEvent.values[2] * sensorEvent.values[2]));
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (dSqrt < 3.0d) {
            this.freeFallAt = jCurrentTimeMillis;
        }
        if (this.freeFallAt <= 0 || jCurrentTimeMillis - this.freeFallAt >= 1600 || dSqrt <= 24.0d || jCurrentTimeMillis - this.lastFallAlert <= 60000) {
            return;
        }
        this.lastFallAlert = jCurrentTimeMillis;
        this.freeFallAt = 0L;
        vibrate(700L);
        confirm("Düşme olasılığı algılandı. İyi misiniz? Yardım araması için evet, iptal etmek için iyiyim deyin.", new MainActivity$$ExternalSyntheticLambda8(this, this.prefs.getString("emergency", "")));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    void m6lambda$onResume$10$commuhammetseslirehberMainActivity() {
        if (!this.wakeMode || this.listening) {
            return;
        }
        if (this.tts == null || !this.tts.isSpeaking()) {
            startListening(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.resumed = true;
        if (this.accelerometer != null) {
            this.sensorManager.registerListener(this, this.accelerometer, 3);
        }
        this.handler.postDelayed(new MainActivity$$ExternalSyntheticLambda0(this), 800L);
    }

    @Override
    protected void onPause() {
        this.resumed = false;
        this.handler.removeCallbacksAndMessages(null);
        if (this.recognizer != null) {
            this.recognizer.cancel();
        }
        this.listening = false;
        if (this.sensorManager != null) {
            this.sensorManager.unregisterListener(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (this.recognizer != null) {
            this.recognizer.destroy();
        }
        if (this.tts != null) {
            this.tts.stop();
            this.tts.shutdown();
        }
        super.onDestroy();
    }
}
