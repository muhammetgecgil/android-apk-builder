package com.mgecgil.seslirehber.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.camera.view.PreviewView;
import com.mgecgil.seslirehber.core.ArCoreVisualFrameContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Reparents the existing MainActivity controls into a camera-first HUD without changing their
 * listeners or safety logic. Engineering controls remain available in a hidden drawer.
 */
public final class HudLayoutInstaller {
    private static final Map<Activity, Controller> CONTROLLERS = new WeakHashMap<>();

    private HudLayoutInstaller() {}

    public static synchronized void install(Activity activity) {
        if (activity == null || CONTROLLERS.containsKey(activity)) return;
        Controller controller = Controller.tryCreate(activity);
        if (controller != null) {
            CONTROLLERS.put(activity, controller);
            controller.start();
        }
    }

    public static synchronized void detach(Activity activity) {
        Controller controller = CONTROLLERS.remove(activity);
        if (controller != null) controller.close();
    }

    private static final class Controller {
        private final Activity activity;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final AwarenessHudView hud;
        private final ImageView arCorePreview;
        private final PreviewView preview;
        private final View developerPanel;
        private final Button developerButton;
        private Bitmap arBitmap;
        private long arTimestamp;
        private boolean closed;

        private final Runnable tick = new Runnable() {
            @Override public void run() {
                if (closed || activity.isFinishing() || activity.isDestroyed()) return;
                long now = System.currentTimeMillis();
                hud.refresh(now);
                updateArCorePreview(now);
                handler.postDelayed(this, 100L);
            }
        };

        private Controller(
                Activity activity,
                AwarenessHudView hud,
                ImageView arCorePreview,
                PreviewView preview,
                View developerPanel,
                Button developerButton) {
            this.activity = activity;
            this.hud = hud;
            this.arCorePreview = arCorePreview;
            this.preview = preview;
            this.developerPanel = developerPanel;
            this.developerButton = developerButton;
        }

        static Controller tryCreate(Activity activity) {
            View content = activity.findViewById(android.R.id.content);
            if (!(content instanceof ViewGroup contentRoot)) return null;

            PreviewView preview = findFirst(contentRoot, PreviewView.class);
            TextView status = findText(contentRoot, "Başlatılıyor", "Rehberlik durum bilgisi");
            TextView depth = findTextByPrefix(contentRoot, "Derinlik:");
            TextView voiceStatus = findTextByPrefix(contentRoot, "Ses:");
            TextView gateStatus = findTextByPrefix(contentRoot, "Gate C:");
            TextView urbanStatus = findTextByPrefix(contentRoot, "Urban Gate:");
            Button voice = findButton(contentRoot, "Sesli Komut");
            Button wake = findButtonStarts(contentRoot, "Hey Rehber");
            Button guidance = findButtonStarts(contentRoot, "Rehberliği");
            Button gate = findButtonStarts(contentRoot, "Gate C Testini");
            Button gateShare = findButtonStarts(contentRoot, "Gate C Raporunu");
            Button urban = findButtonStarts(contentRoot, "Urban Gate Testini");
            Button urbanAction = findButtonEither(contentRoot, "Senaryo:", "Sonraki:", "Urban Raporunu");
            Button stop = findButton(contentRoot, "ACİL DUR");

            if (preview == null || status == null || depth == null || voiceStatus == null
                    || voice == null || stop == null) return null;

            detach(preview); detach(status); detach(depth); detach(voiceStatus);
            detach(gateStatus); detach(urbanStatus); detach(voice); detach(wake); detach(guidance);
            detach(gate); detach(gateShare); detach(urban); detach(urbanAction); detach(stop);

            FrameLayout root = new FrameLayout(activity);
            root.setBackgroundColor(Color.BLACK);
            activity.setContentView(root);

            preview.setScaleType(PreviewView.ScaleType.FILL_CENTER);
            preview.setContentDescription("Canlı gerçek kamera görüntüsü");
            root.addView(preview, frame(-1, -1, Gravity.FILL, 0, 0, 0, 0));

            ImageView arPreview = new ImageView(activity);
            arPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            arPreview.setVisibility(View.GONE);
            arPreview.setContentDescription("ARCore canlı gerçek kamera görüntüsü");
            root.addView(arPreview, frame(-1, -1, Gravity.FILL, 0, 0, 0, 0));

            AwarenessHudView hud = new AwarenessHudView(activity);
            root.addView(hud, frame(-1, -1, Gravity.FILL, 0, 0, 0, 0));

            LinearLayout top = new LinearLayout(activity);
            top.setOrientation(LinearLayout.VERTICAL);
            top.setPadding(dp(activity, 10), dp(activity, 8), dp(activity, 10), dp(activity, 8));
            root.addView(top, frame(-1, -2, Gravity.TOP, 8, 8, 8, 0));

            styleStatus(status, activity);
            top.addView(status, new LinearLayout.LayoutParams(-1, -2));

            LinearLayout chips = new LinearLayout(activity);
            chips.setOrientation(LinearLayout.HORIZONTAL);
            chips.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(0, -2, 1f);
            chipLp.setMargins(0, dp(activity, 5), dp(activity, 4), 0);
            styleChip(depth, activity);
            styleChip(voiceStatus, activity);
            chips.addView(depth, chipLp);
            LinearLayout.LayoutParams voiceLp = new LinearLayout.LayoutParams(0, -2, 1f);
            voiceLp.setMargins(dp(activity, 4), dp(activity, 5), 0, 0);
            chips.addView(voiceStatus, voiceLp);
            top.addView(chips, new LinearLayout.LayoutParams(-1, -2));

            LinearLayout bottom = new LinearLayout(activity);
            bottom.setOrientation(LinearLayout.HORIZONTAL);
            bottom.setGravity(Gravity.CENTER);
            bottom.setPadding(dp(activity, 10), dp(activity, 6), dp(activity, 10), dp(activity, 8));
            bottom.setBackground(panelBackground(0x8A000000, dp(activity, 18)));
            root.addView(bottom, frame(-1, -2, Gravity.BOTTOM, 8, 0, 8, 10));

            voice.setText("SES");
            styleMainButton(voice, activity, 0xD91B2735);
            LinearLayout.LayoutParams mainLp = new LinearLayout.LayoutParams(0, dp(activity, 58), 1f);
            mainLp.setMargins(0, 0, dp(activity, 6), 0);
            bottom.addView(voice, mainLp);

            Button devButton = new Button(activity);
            devButton.setText("TEST …");
            devButton.setAllCaps(false);
            devButton.setTextSize(15f);
            devButton.setTextColor(Color.WHITE);
            devButton.setContentDescription("Test ve geliştirici panelini aç veya kapat");
            devButton.setBackground(panelBackground(0xD93A414A, dp(activity, 14)));
            LinearLayout.LayoutParams devLp = new LinearLayout.LayoutParams(dp(activity, 82), dp(activity, 58));
            devLp.setMargins(0, 0, dp(activity, 6), 0);
            bottom.addView(devButton, devLp);

            stop.setText("ACİL DUR");
            styleMainButton(stop, activity, 0xE5A52121);
            LinearLayout.LayoutParams stopLp = new LinearLayout.LayoutParams(0, dp(activity, 58), 1.15f);
            bottom.addView(stop, stopLp);

            ScrollView devScroll = new ScrollView(activity);
            devScroll.setFillViewport(false);
            devScroll.setVisibility(View.GONE);
            devScroll.setBackground(panelBackground(0xEE090D12, dp(activity, 18)));
            FrameLayout.LayoutParams devPanelLp = frame(-1, dp(activity, 440), Gravity.BOTTOM, 8, 0, 8, 86);
            root.addView(devScroll, devPanelLp);

            LinearLayout devColumn = new LinearLayout(activity);
            devColumn.setOrientation(LinearLayout.VERTICAL);
            devColumn.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 16));
            devScroll.addView(devColumn, new ScrollView.LayoutParams(-1, -2));

            TextView devTitle = new TextView(activity);
            devTitle.setText("Test / Geliştirici");
            devTitle.setTextColor(Color.WHITE);
            devTitle.setTextSize(20f);
            devTitle.setGravity(Gravity.CENTER_VERTICAL);
            devTitle.setPadding(dp(activity, 6), dp(activity, 4), dp(activity, 6), dp(activity, 8));
            devColumn.addView(devTitle, new LinearLayout.LayoutParams(-1, -2));

            if (gateStatus != null) { styleDeveloperStatus(gateStatus, activity); devColumn.addView(gateStatus); }
            if (urbanStatus != null) { styleDeveloperStatus(urbanStatus, activity); devColumn.addView(urbanStatus); }
            addDeveloperButton(devColumn, wake, activity);
            addDeveloperButton(devColumn, guidance, activity);
            addDeveloperButton(devColumn, gate, activity);
            addDeveloperButton(devColumn, gateShare, activity);
            addDeveloperButton(devColumn, urban, activity);
            addDeveloperButton(devColumn, urbanAction, activity);

            Button close = new Button(activity);
            close.setText("Paneli Kapat");
            close.setAllCaps(false);
            close.setContentDescription("Test panelini kapat");
            addDeveloperButton(devColumn, close, activity);

            View.OnClickListener toggle = v -> {
                boolean opening = devScroll.getVisibility() != View.VISIBLE;
                devScroll.setVisibility(opening ? View.VISIBLE : View.GONE);
                devButton.setText(opening ? "KAPAT" : "TEST …");
                if (opening) devTitle.announceForAccessibility("Test ve geliştirici paneli açıldı");
                else devButton.announceForAccessibility("Test ve geliştirici paneli kapatıldı");
            };
            devButton.setOnClickListener(toggle);
            close.setOnClickListener(toggle);

            activity.getWindow().setStatusBarColor(Color.BLACK);
            activity.getWindow().setNavigationBarColor(Color.BLACK);
            return new Controller(activity, hud, arPreview, preview, devScroll, devButton);
        }

        void start() {
            handler.removeCallbacks(tick);
            handler.post(tick);
        }

        void close() {
            closed = true;
            handler.removeCallbacksAndMessages(null);
            if (arBitmap != null) {
                try { arBitmap.recycle(); } catch (Throwable ignored) {}
                arBitmap = null;
            }
        }

        private void updateArCorePreview(long nowMs) {
            ArCoreVisualFrameContext.Frame frame = ArCoreVisualFrameContext.latest(nowMs);
            if (frame == null) {
                arCorePreview.setVisibility(View.GONE);
                return;
            }
            if (frame.timestampMs() == arTimestamp) {
                arCorePreview.setVisibility(View.VISIBLE);
                return;
            }
            int[] pixels = frame.argb();
            if (pixels.length < frame.width() * frame.height()) return;
            Bitmap next = Bitmap.createBitmap(frame.width(), frame.height(), Bitmap.Config.ARGB_8888);
            next.setPixels(pixels, 0, frame.width(), 0, 0, frame.width(), frame.height());
            Bitmap old = arBitmap;
            arBitmap = next;
            arTimestamp = frame.timestampMs();
            arCorePreview.setImageBitmap(next);
            arCorePreview.setVisibility(View.VISIBLE);
            if (old != null) try { old.recycle(); } catch (Throwable ignored) {}
        }
    }

    private static void styleStatus(TextView view, Activity activity) {
        view.setTextColor(Color.WHITE);
        view.setTextSize(17f);
        view.setMaxLines(2);
        view.setEllipsize(TextUtils.TruncateAt.END);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), dp(activity, 8));
        view.setMinHeight(0);
        view.setBackground(panelBackground(0xB3000000, dp(activity, 14)));
    }

    private static void styleChip(TextView view, Activity activity) {
        view.setTextColor(Color.WHITE);
        view.setTextSize(12.5f);
        view.setSingleLine(true);
        view.setEllipsize(TextUtils.TruncateAt.END);
        view.setPadding(dp(activity, 8), dp(activity, 5), dp(activity, 8), dp(activity, 5));
        view.setMinHeight(0);
        view.setBackground(panelBackground(0x99080D13, dp(activity, 11)));
    }

    private static void styleDeveloperStatus(TextView view, Activity activity) {
        view.setTextColor(Color.LTGRAY);
        view.setTextSize(14f);
        view.setMaxLines(3);
        view.setPadding(dp(activity, 6), dp(activity, 6), dp(activity, 6), dp(activity, 6));
        view.setMinHeight(0);
    }

    private static void styleMainButton(Button button, Activity activity, int color) {
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(18f);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setBackground(panelBackground(color, dp(activity, 14)));
    }

    private static void addDeveloperButton(LinearLayout parent, Button button, Activity activity) {
        if (button == null) return;
        button.setTextSize(17f);
        button.setMinHeight(dp(activity, 54));
        button.setBackground(panelBackground(0xE5262D35, dp(activity, 12)));
        button.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(activity, 56));
        lp.setMargins(0, dp(activity, 5), 0, 0);
        parent.addView(button, lp);
    }

    private static GradientDrawable panelBackground(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        d.setStroke(1, Color.argb(80, 255, 255, 255));
        return d;
    }

    private static FrameLayout.LayoutParams frame(
            int width,
            int height,
            int gravity,
            int left,
            int top,
            int right,
            int bottom) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height, gravity);
        lp.setMargins(left, top, right, bottom);
        return lp;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static void detach(View view) {
        if (view == null) return;
        if (view.getParent() instanceof ViewGroup parent) parent.removeView(view);
    }

    private static Button findButton(View root, String exact) {
        for (Button b : all(root, Button.class)) {
            if (exact.equals(text(b))) return b;
        }
        return null;
    }

    private static Button findButtonStarts(View root, String prefix) {
        for (Button b : all(root, Button.class)) {
            if (text(b).startsWith(prefix)) return b;
        }
        return null;
    }

    private static Button findButtonEither(View root, String... prefixes) {
        for (Button b : all(root, Button.class)) {
            String t = text(b);
            for (String prefix : prefixes) if (t.startsWith(prefix)) return b;
        }
        return null;
    }

    private static TextView findTextByPrefix(View root, String prefix) {
        for (TextView t : all(root, TextView.class)) {
            if (!(t instanceof Button) && text(t).startsWith(prefix)) return t;
        }
        return null;
    }

    private static TextView findText(View root, String textPrefix, String description) {
        for (TextView t : all(root, TextView.class)) {
            if (t instanceof Button) continue;
            String content = t.getContentDescription() == null ? "" : t.getContentDescription().toString();
            if (text(t).startsWith(textPrefix) || description.equals(content)) return t;
        }
        return null;
    }

    private static String text(TextView view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }

    private static <T extends View> T findFirst(View root, Class<T> cls) {
        List<T> values = all(root, cls);
        return values.isEmpty() ? null : values.get(0);
    }

    private static <T extends View> List<T> all(View root, Class<T> cls) {
        List<T> out = new ArrayList<>();
        collect(root, cls, out);
        return out;
    }

    private static <T extends View> void collect(View view, Class<T> cls, List<T> out) {
        if (cls.isInstance(view)) out.add(cls.cast(view));
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) collect(group.getChildAt(i), cls, out);
        }
    }
}
