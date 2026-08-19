package com.mg.trainingassistant;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TrainingAccessibilityService extends AccessibilityService {
    private static final String PREFS = "training_assistant";
    private static final long SCAN_INTERVAL_MS = 1200L;
    private static final long MIN_CLICK_INTERVAL_MS = 2600L;
    private static final long SAME_LABEL_GUARD_MS = 7500L;
    private static final long SCROLL_INTERVAL_MS = 4500L;

    private static final List<String> NEXT_WORDS = Arrays.asList(
            "ileri", "devam", "devam et", "sonraki", "sonraki adim", "sonraki ders", "ileri git",
            "next", "continue", "next step", "next lesson", "proceed", "resume");

    private static final List<String> STRONG_STOP_WORDS = Arrays.asList(
            "quiz", "sinav", "sertifika sinavi", "degerlendirme", "exam", "assessment",
            "cevabi gonder", "cevapla", "submit answer", "check answer", "dogrula", "onayla");

    private static final List<String> BLOCKED_PACKAGES = Arrays.asList(
            "com.android.settings", "com.android.systemui", "com.google.android.permissioncontroller",
            "com.samsung.android.permissioncontroller", "com.google.android.packageinstaller");

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastClickAt = 0L;
    private long lastScrollAt = 0L;
    private long pageChangedAt = 0L;
    private String lastClickedLabel = "";
    private String lastPageSignature = "";
    private boolean scanQueued = false;

    private final Runnable watchdog = new Runnable() {
        @Override
        public void run() {
            scanNow();
            handler.postDelayed(this, SCAN_INTERVAL_MS);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        handler.removeCallbacks(watchdog);
        handler.post(watchdog);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isRunning()) return;
        CharSequence pkg = event == null ? null : event.getPackageName();
        if (pkg != null && isBlockedPackage(pkg.toString())) return;
        queueScan(180L);
    }

    @Override
    public void onInterrupt() {
        handler.removeCallbacks(watchdog);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(watchdog);
        super.onDestroy();
    }

    private boolean isRunning() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean("running", false)
                && getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean("consent", false);
    }

    private void queueScan(long delayMs) {
        if (scanQueued) return;
        scanQueued = true;
        handler.postDelayed(() -> {
            scanQueued = false;
            scanNow();
        }, delayMs);
    }

    private void scanNow() {
        if (!isRunning()) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        CharSequence pkg = root.getPackageName();
        if (pkg == null || isBlockedPackage(pkg.toString()) || getPackageName().contentEquals(pkg)) return;

        String allText = normalize(collectText(root, 0, new StringBuilder()).toString());
        String signature = buildSignature(pkg.toString(), allText);
        long now = SystemClock.uptimeMillis();

        if (!signature.equals(lastPageSignature)) {
            lastPageSignature = signature;
            pageChangedAt = now;
            lastClickedLabel = "";
        }

        if (containsAny(allText, STRONG_STOP_WORDS)) return;
        if (now - lastClickAt < MIN_CLICK_INTERVAL_MS) return;

        Candidate best = findBestCandidate(root, 0, new Candidate());
        if (best.node != null && best.score >= 40) {
            String label = normalize(nodeText(best.node));
            if (label.equals(lastClickedLabel) && now - lastClickAt < SAME_LABEL_GUARD_MS) return;

            AccessibilityNodeInfo clickable = clickableAncestor(best.node);
            if (clickable != null && clickable.isEnabled() && clickable.isVisibleToUser()) {
                boolean clicked = clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                if (clicked) {
                    lastClickAt = now;
                    lastClickedLabel = label;
                    return;
                }
            }
        }

        if (now - pageChangedAt > 1800L && now - lastScrollAt > SCROLL_INTERVAL_MS) {
            AccessibilityNodeInfo scrollable = findScrollable(root, 0);
            if (scrollable != null && scrollable.isVisibleToUser()) {
                if (scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
                    lastScrollAt = now;
                    queueScan(650L);
                }
            }
        }
    }

    private Candidate findBestCandidate(AccessibilityNodeInfo node, int depth, Candidate best) {
        if (node == null || depth > 24) return best;

        String text = normalize(nodeText(node));
        if (node.isVisibleToUser() && node.isEnabled() && matchesNext(text)) {
            int score = 40;
            if (node.isClickable()) score += 25;
            if (looksLikeButton(node)) score += 15;
            if (isExactNext(text)) score += 15;

            Rect r = new Rect();
            node.getBoundsInScreen(r);
            if (r.top > 700) score += 5;

            if (score > best.score) {
                best.node = node;
                best.score = score;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            findBestCandidate(node.getChild(i), depth + 1, best);
        }
        return best;
    }

    private AccessibilityNodeInfo clickableAncestor(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        int hops = 0;
        while (current != null && !current.isClickable() && hops < 5) {
            current = current.getParent();
            hops++;
        }
        return current;
    }

    private AccessibilityNodeInfo findScrollable(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 24) return null;
        if (node.isScrollable() && node.isEnabled() && node.isVisibleToUser()) return node;
        for (int i = node.getChildCount() - 1; i >= 0; i--) {
            AccessibilityNodeInfo found = findScrollable(node.getChild(i), depth + 1);
            if (found != null) return found;
        }
        return null;
    }

    private boolean looksLikeButton(AccessibilityNodeInfo node) {
        CharSequence cls = node.getClassName();
        if (cls == null) return false;
        String c = cls.toString().toLowerCase(Locale.ROOT);
        return c.contains("button") || c.contains("materialbutton") || c.contains("textview");
    }

    private boolean matchesNext(String text) {
        if (text.isEmpty()) return false;
        for (String word : NEXT_WORDS) {
            if (text.equals(word)
                    || text.startsWith(word + " ")
                    || text.endsWith(" " + word)
                    || text.equals(word + " >")
                    || text.equals(word + " ›")
                    || text.equals(word + " →")) return true;
        }
        return false;
    }

    private boolean isExactNext(String text) {
        for (String word : NEXT_WORDS) if (text.equals(word)) return true;
        return false;
    }

    private boolean containsAny(String text, List<String> words) {
        for (String w : words) if (text.contains(w)) return true;
        return false;
    }

    private boolean isBlockedPackage(String pkg) {
        for (String blocked : BLOCKED_PACKAGES) {
            if (pkg.equals(blocked) || pkg.startsWith(blocked + ".")) return true;
        }
        return false;
    }

    private StringBuilder collectText(AccessibilityNodeInfo node, int depth, StringBuilder out) {
        if (node == null || depth > 24 || out.length() > 16000) return out;
        String t = nodeText(node);
        if (!t.isEmpty()) out.append(' ').append(t);
        for (int i = 0; i < node.getChildCount(); i++) {
            collectText(node.getChild(i), depth + 1, out);
        }
        return out;
    }

    private String nodeText(AccessibilityNodeInfo node) {
        List<String> parts = new ArrayList<>();
        if (node.getText() != null) parts.add(node.getText().toString());
        if (node.getContentDescription() != null) parts.add(node.getContentDescription().toString());
        if (node.getHintText() != null) parts.add(node.getHintText().toString());
        return String.join(" ", parts).trim();
    }

    private String buildSignature(String pkg, String text) {
        String compact = text.length() > 900 ? text.substring(0, 900) : text;
        return pkg + "|" + compact.hashCode();
    }

    private String normalize(String s) {
        String lower = s == null ? "" : s.toLowerCase(new Locale("tr", "TR"));
        lower = lower.replace('ı', 'i');
        String n = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.replaceAll("\\s+", " ").trim();
    }

    private static class Candidate {
        AccessibilityNodeInfo node;
        int score = Integer.MIN_VALUE;
    }
}
