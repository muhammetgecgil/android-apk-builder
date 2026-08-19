package com.mg.trainingassistant;

import android.accessibilityservice.AccessibilityService;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TrainingAccessibilityService extends AccessibilityService {
    private static final List<String> NEXT_WORDS = Arrays.asList(
            "ileri", "devam", "sonraki", "next", "continue", "devam et", "sonraki adim", "sonraki ders");
    private static final List<String> STOP_WORDS = Arrays.asList(
            "quiz", "sinav", "test", "degerlendirme", "soru", "cevap", "onayla", "dogrula", "sertifika sinavi");
    private long lastClickAt = 0L;
    private String lastClickedText = "";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!getSharedPreferences("training_assistant", MODE_PRIVATE).getBoolean("running", false)) return;
        if (SystemClock.uptimeMillis() - lastClickAt < 2500) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        String allText = normalize(collectText(root, 0, new StringBuilder()).toString());
        if (containsAny(allText, STOP_WORDS)) return;

        AccessibilityNodeInfo candidate = findNextButton(root, 0);
        if (candidate == null) return;

        String label = normalize(nodeText(candidate));
        if (label.equals(lastClickedText) && SystemClock.uptimeMillis() - lastClickAt < 8000) return;

        AccessibilityNodeInfo clickable = candidate;
        while (clickable != null && !clickable.isClickable()) clickable = clickable.getParent();
        if (clickable != null && clickable.isEnabled() && clickable.isVisibleToUser()) {
            if (clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                lastClickAt = SystemClock.uptimeMillis();
                lastClickedText = label;
            }
        }
    }

    @Override
    public void onInterrupt() { }

    private AccessibilityNodeInfo findNextButton(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 18) return null;
        String text = normalize(nodeText(node));
        if (node.isVisibleToUser() && node.isEnabled() && matchesNext(text)) return node;

        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo found = findNextButton(node.getChild(i), depth + 1);
            if (found != null) return found;
        }
        return null;
    }

    private boolean matchesNext(String text) {
        if (text.isEmpty()) return false;
        for (String word : NEXT_WORDS) {
            if (text.equals(word) || text.startsWith(word + " ") || text.endsWith(" " + word)) return true;
        }
        return false;
    }

    private boolean containsAny(String text, List<String> words) {
        for (String w : words) if (text.contains(w)) return true;
        return false;
    }

    private StringBuilder collectText(AccessibilityNodeInfo node, int depth, StringBuilder out) {
        if (node == null || depth > 18 || out.length() > 12000) return out;
        String t = nodeText(node);
        if (!t.isEmpty()) out.append(' ').append(t);
        for (int i = 0; i < node.getChildCount(); i++) collectText(node.getChild(i), depth + 1, out);
        return out;
    }

    private String nodeText(AccessibilityNodeInfo node) {
        List<String> parts = new ArrayList<>();
        if (node.getText() != null) parts.add(node.getText().toString());
        if (node.getContentDescription() != null) parts.add(node.getContentDescription().toString());
        return String.join(" ", parts).trim();
    }

    private String normalize(String s) {
        String lower = s == null ? "" : s.toLowerCase(new Locale("tr", "TR"));
        lower = lower.replace('ı', 'i');
        String n = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.replaceAll("\\s+", " ").trim();
    }
}
