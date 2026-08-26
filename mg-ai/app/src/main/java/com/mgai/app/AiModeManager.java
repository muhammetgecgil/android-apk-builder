package com.mgai.app;

import android.content.Context;
import android.content.SharedPreferences;

public final class AiModeManager {
    public enum Mode { SPEED, BALANCED, QUALITY, COOL, DEEP_REASONING }
    private static final String PREFS="mg_ai_mode";
    private static final String KEY_MODE="mode";
    private AiModeManager(){}

    public static Mode get(Context c){
        String v=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY_MODE,Mode.BALANCED.name());
        try{return Mode.valueOf(v);}catch(Throwable t){return Mode.BALANCED;}
    }
    public static void set(Context c,Mode m){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY_MODE,m.name()).apply();}
    public static String title(Mode m){switch(m){case SPEED:return "Hız";case QUALITY:return "Kalite";case COOL:return "Serin";case DEEP_REASONING:return "Uzun Muhakeme";default:return "Dengeli";}}
    public static String description(Mode m){switch(m){case SPEED:return "Düşük gecikme ve yüksek token/s öncelikli.";case QUALITY:return "Cevap doğruluğu ve görev uyumu öncelikli.";case COOL:return "Daha düşük ısı ve daha sakin işlem yükü öncelikli.";case DEEP_REASONING:return "Daha geniş context ve daha uzun cevap bütçesi öncelikli.";default:return "Kalite, hız ve ısı dengesi.";}}
    public static double qualityWeight(Mode m){switch(m){case SPEED:return 0.30;case QUALITY:return 0.80;case COOL:return 0.45;case DEEP_REASONING:return 0.75;default:return 0.65;}}
    public static double performanceWeight(Mode m){return 1.0-qualityWeight(m);}
    public static int tokenBudget(Mode m){switch(m){case SPEED:return 192;case QUALITY:return 512;case COOL:return 192;case DEEP_REASONING:return 768;default:return 384;}}
    public static int contextCap(Mode m){switch(m){case SPEED:return 2048;case QUALITY:return 4096;case COOL:return 2048;case DEEP_REASONING:return 6144;default:return 3072;}}
    public static int threadCap(Context c,Mode m){int cores=Math.max(2,Runtime.getRuntime().availableProcessors());switch(m){case SPEED:return Math.max(2,cores-2);case QUALITY:return Math.max(2,cores-2);case COOL:return Math.max(2,cores/3);case DEEP_REASONING:return Math.max(2,cores-2);default:return Math.max(2,cores/2);}}
    public static String summary(Context c){Mode m=get(c);return title(m)+" • ctx≤"+contextCap(m)+" • max "+tokenBudget(m)+" token • "+description(m);}
}
