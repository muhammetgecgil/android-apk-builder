package com.mgai.app;

import android.content.Context;
import java.util.Locale;

public final class AiOrchestrator {
    public enum Route { DIRECT, MEMORY, DOCUMENT_RAG, DEEP_REASONING, CAUTIOUS }

    public static final class Plan {
        public final Route route;
        public final int maxTokens;
        public final float temperature;
        public final int memoryItems;
        public final int documentItems;
        public final String reason;
        Plan(Route route,int maxTokens,float temperature,int memoryItems,int documentItems,String reason){
            this.route=route;this.maxTokens=maxTokens;this.temperature=temperature;this.memoryItems=memoryItems;this.documentItems=documentItems;this.reason=reason;
        }
        public String label(){
            switch(route){
                case MEMORY:return "HAFIZA";
                case DOCUMENT_RAG:return "BELGE / RAG";
                case DEEP_REASONING:return "UZUN MUHAKEME";
                case CAUTIOUS:return "TEMKİNLİ DOĞRULAMA";
                default:return "DOĞRUDAN";
            }
        }
        public String summary(){return label()+" • max "+maxTokens+" token • temp "+temperature+" • "+reason;}
    }

    private AiOrchestrator(){}

    public static Plan plan(Context c,String prompt,AdaptivePerformanceManager.Profile perf){
        String q=norm(prompt);
        int base=Math.max(96,perf.maxTokens);
        boolean memory=containsAny(q,"hatirla","hatırlıyor","daha once","daha önce","benim hakkimda","benim hakkımda","proje:","gecen sefer","geçen sefer");
        boolean docs=containsAny(q,"belge","dokuman","doküman","pdf","dosya","rapor","teknik resim","ocr","ekledigim","eklediğim","dokumanda","dokümanda");
        boolean deep=containsAny(q,"hesapla","analiz et","karsilastir","karşılaştır","neden","tasarla","muhendislik","mühendislik","adim adim","adım adım","optimize","kok neden","kök neden","senaryo","risk","dogrula","doğrula") || q.length()>420;
        boolean cautious=containsAny(q,"emin misin","kaynak","kanıt","kanit","kesin","guvenli","güvenli","kritik","standart","sertifika","uygunluk","failure","emniyet");

        if(docs){return new Plan(Route.DOCUMENT_RAG,Math.min(768,Math.max(base,384)),0.35f,6,6,"yerel belge bağlamı öncelikli");}
        if(memory){return new Plan(Route.MEMORY,Math.min(512,Math.max(base,256)),0.45f,10,2,"uzun süreli hafıza öncelikli");}
        if(cautious){return new Plan(Route.CAUTIOUS,Math.min(768,Math.max(base,384)),0.20f,6,4,"iddiaları sınırlı ve belirsizliği açık cevapla");}
        if(deep){return new Plan(Route.DEEP_REASONING,Math.min(896,Math.max(base,512)),0.35f,6,4,"daha geniş bağlam ve uzun muhakeme");}
        return new Plan(Route.DIRECT,Math.min(384,base),0.65f,4,2,"hızlı yerel cevap");
    }

    public static String buildPrompt(Context c,String user,Plan plan){
        String mem=LocalLongTermMemory.relevantContext(c,user,plan.memoryItems);
        String docs=LocalDocumentStore.retrieve(c,user,plan.documentItems);
        String history=LocalChatStore.transcript(c,8);
        StringBuilder s=new StringBuilder();
        s.append("Sen MG-AI adlı, telefonda yerel çalışan kişisel yapay zekasın. Türkçe sorulara doğal ve açık Türkçe cevap ver.\n");
        s.append("Orchestrator rotası: ").append(plan.label()).append(".\n");
        s.append("Kural: bilmediğin bilgiyi uydurma; belge veya hafıza bağlamını yalnız gerçekten ilgiliyse kullan. Kullanıcının isteğini doğrudan yerine getir.\n");
        if(plan.route==Route.DEEP_REASONING)s.append("Bu görev muhakeme gerektiriyor. Son cevabı vermeden önce problemi alt parçalara ayır, çelişkileri kontrol et ve sayısal sonuçları mümkünse ikinci kez doğrula. Gizli düşünce zincirini yazma; yalnız gerekli hesap/adım özetlerini göster.\n");
        if(plan.route==Route.CAUTIOUS)s.append("Bu görev temkinli doğrulama gerektiriyor. Varsayım, belirsizlik ve kanıt eksiklerini açıkça ayır; kesin olmayan şeyi kesinmiş gibi sunma.\n");
        if(plan.route==Route.DOCUMENT_RAG)s.append("Belge sorusunda önce aşağıdaki yerel belge parçalarını kullan; cevap belgede yoksa bunu açıkça söyle.\n");
        if(plan.route==Route.MEMORY)s.append("Kullanıcı hafızası sorusunda aşağıdaki uzun süreli hafızayı önceliklendir; kayıt yoksa uydurma.\n");
        s.append("\nUZUN SÜRELİ HAFIZA:\n").append(mem.isEmpty()?"(ilgili kayıt yok)":mem);
        s.append("\n\nYEREL BELGE/OCR BAĞLAMI:\n").append(docs.isEmpty()?"(ilgili belge parçası yok)":docs);
        s.append("\n\nSON KONUŞMA:\n").append(history);
        s.append("\nMG-AI:");
        return s.toString();
    }

    private static String norm(String s){return s==null?"":s.toLowerCase(new Locale("tr","TR")).trim();}
    private static boolean containsAny(String s,String... xs){for(String x:xs)if(s.contains(x))return true;return false;}
}
