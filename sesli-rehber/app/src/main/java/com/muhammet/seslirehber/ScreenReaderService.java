package com.muhammet.seslirehber;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.LinkedHashSet;
import java.util.Set;

public class ScreenReaderService extends AccessibilityService {
    private static ScreenReaderService instance;

    @Override
    public void onServiceConnected() {
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        if (instance == this) {
            instance = null;
        }
        super.onDestroy();
    }

    public static String visibleText() {
        if (instance == null) {
            return null;
        }
        AccessibilityNodeInfo rootInActiveWindow = instance.getRootInActiveWindow();
        if (rootInActiveWindow == null) {
            return "Ekranda okunabilir içerik bulunamadı.";
        }
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet();
        collect(rootInActiveWindow, linkedHashSet);
        rootInActiveWindow.recycle();
        if (linkedHashSet.isEmpty()) {
            return "Ekranda okunabilir metin bulunamadı.";
        }
        StringBuilder sb = new StringBuilder();
        for (String str : linkedHashSet) {
            if (sb.length() > 0) {
                sb.append(". ");
            }
            sb.append(str);
            if (sb.length() > 1200) {
                break;
            }
        }
        return sb.toString();
    }

    private static void collect(AccessibilityNodeInfo accessibilityNodeInfo, Set<String> set) {
        if (accessibilityNodeInfo == null || !accessibilityNodeInfo.isVisibleToUser()) {
            return;
        }
        CharSequence text = accessibilityNodeInfo.getText();
        CharSequence contentDescription = accessibilityNodeInfo.getContentDescription();
        if (text != null && text.length() > 0) {
            set.add(text.toString().trim());
        }
        if (contentDescription != null && contentDescription.length() > 0) {
            set.add(contentDescription.toString().trim());
        }
        for (int i = 0; i < accessibilityNodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo child = accessibilityNodeInfo.getChild(i);
            collect(child, set);
            if (child != null) {
                child.recycle();
            }
        }
    }
}
