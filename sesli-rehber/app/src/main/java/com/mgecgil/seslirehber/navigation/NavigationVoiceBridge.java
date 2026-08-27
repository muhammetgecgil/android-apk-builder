package com.mgecgil.seslirehber.navigation;

/** Temporary voice hand-off used while navigation is asking a confirmation/cancel question. */
public final class NavigationVoiceBridge {
    public interface Handler { boolean handle(String rawText); }
    private static volatile Handler handler;
    private NavigationVoiceBridge() {}

    public static void install(Handler value) { handler = value; }
    public static void clear(Handler value) { if (handler == value) handler = null; }

    public static boolean tryHandle(String rawText) {
        Handler h = handler;
        if (h == null) return false;
        try { return h.handle(rawText); }
        catch (Throwable ignored) { return false; }
    }
}
