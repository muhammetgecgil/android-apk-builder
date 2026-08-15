package com.muhammetgecgil.pcbzekapro;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ComponentDecoder {
    static final class Item {
        final String ref, code, type, value, note;
        final int confidence;
        Item(String ref, String code, String type, String value, int confidence, String note) {
            this.ref=ref; this.code=code; this.type=type; this.value=value;
            this.confidence=confidence; this.note=note;
        }
        String line() { return ref+" • "+type+" • "+code+" → "+value+" • Güven %"+confidence+(note.isEmpty()?"":" • "+note); }
        String csv() { return q(ref)+","+q(type)+","+q(code)+","+q(value)+","+confidence+","+q(note); }
        private static String q(String s){ return "\""+s.replace("\"","\"\"")+"\""; }
    }

    private static final Pattern REF_CODE=Pattern.compile("(?i)\\b([RCLDQUTJF]|FB)\\s*([0-9]{1,4})\\s*[:=-]?\\s*([A-Z0-9.]{2,14})\\b");
    private static final Pattern CODE=Pattern.compile("(?i)\\b(?:[0-9]{3,4}|[0-9]{1,2}[RKM][0-9]{1,2}|R[0-9]{1,3}|[0-9]{2}[A-Z])\\b");
    private static final int[] E96={100,102,105,107,110,113,115,118,121,124,127,130,133,137,140,143,147,150,154,158,162,165,169,174,178,182,187,191,196,200,205,210,215,221,226,232,237,243,249,255,261,267,274,280,287,294,301,309,316,324,332,340,348,357,365,374,383,392,402,412,422,432,442,453,464,475,487,499,511,523,536,549,562,576,590,604,619,634,649,665,681,698,715,732,750,768,787,806,825,845,866,887,909,931,953,976};

    static List<Item> decodeText(String text) {
        List<Item> out=new ArrayList<>(); Set<String> seen=new LinkedHashSet<>();
        String normalized=text.toUpperCase(Locale.ROOT).replace('\n',' ');
        Matcher paired=REF_CODE.matcher(normalized);
        while(paired.find()) add(out,seen,paired.group(1)+paired.group(2),paired.group(3),true);
        Matcher loose=CODE.matcher(normalized);
        int n=1;
        while(loose.find()) {
            String code=loose.group();
            if(!seen.contains(code)) add(out,seen,"?"+(n++),code,false);
        }
        if(out.isEmpty() && !normalized.trim().isEmpty()) {
            String first=normalized.trim().split("\\s+")[0];
            out.add(new Item("U?",first,"IC / aktif eleman","Kod bulundu; datasheet araması gerekli",68,"Paket ve pin sayısıyla doğrulayın"));
        }
        return out;
    }

    private static void add(List<Item> out, Set<String> seen, String ref, String code, boolean paired) {
        if(!seen.add(code)) return;
        char kind=Character.toUpperCase(ref.charAt(0));
        if(kind=='R') out.add(new Item(ref,code,"Direnç",decodeResistor(code),paired?94:82,"SMD kodu"));
        else if(kind=='L' || ref.toUpperCase(Locale.ROOT).startsWith("FB")) out.add(new Item(ref,code,kind=='L'?"Bobin":"Ferrit boncuk",decodeInductor(code),paired?93:80,"İşaret kodu"));
        else if(kind=='C') out.add(new Item(ref,code,"Kondansatör",decodeCap(code),paired?88:72,"Baskı kodu; gerilim sınıfını doğrulayın"));
        else out.add(new Item(ref,code,typeName(kind),"Kod: "+code,paired?87:68,"Datasheet ile doğrulayın"));
    }

    static String decodeResistor(String raw) {
        String s=raw.toUpperCase(Locale.ROOT);
        if(s.matches("R\\d+")) return format(Double.parseDouble(s.substring(1)))+"Ω";
        if(s.matches("\\d+[RKM]\\d+")) {
            char m=s.replaceAll("[0-9]","").charAt(0); String[] p=s.split("[RKM]");
            double v=Double.parseDouble(p[0]+"."+p[1])*(m=='K'?1e3:m=='M'?1e6:1);
            return format(v)+"Ω";
        }
        if(s.matches("\\d{3,4}")) {
            int digits=s.length()-1; double base=Double.parseDouble(s.substring(0,digits));
            return format(base*Math.pow(10,Character.digit(s.charAt(digits),10)))+"Ω";
        }
        if(s.matches("\\d{2}[A-Z]")) {
            int idx=Integer.parseInt(s.substring(0,2)); int mul=e96Multiplier(s.charAt(2));
            if(idx>=1&&idx<=96&&mul>=0) return format(E96[idx-1]/100.0*Math.pow(10,mul))+"Ω (EIA-96)";
        }
        return "Değer belirsiz — ölçüm gerekli";
    }

    static String decodeInductor(String raw) {
        String s=raw.toUpperCase(Locale.ROOT);
        if(s.matches("\\d+[R]\\d+")) { String[] p=s.split("R"); return trim(Double.parseDouble(p[0]+"."+p[1]))+" µH"; }
        if(s.matches("\\d{3}")) { double v=Double.parseDouble(s.substring(0,2))*Math.pow(10,Character.digit(s.charAt(2),10)); return formatInd(v); }
        if(s.matches("\\d{4}")) { double v=Double.parseDouble(s.substring(0,3))*Math.pow(10,Character.digit(s.charAt(3),10)); return formatInd(v); }
        return "İşaret yok/belirsiz — LCR ölçümü gerekli";
    }

    private static String decodeCap(String s){
        if(s.matches("\\d{3}")){ double pf=Double.parseDouble(s.substring(0,2))*Math.pow(10,Character.digit(s.charAt(2),10)); return pf>=1e6?trim(pf/1e6)+" µF":pf>=1e3?trim(pf/1e3)+" nF":trim(pf)+" pF"; }
        return "Kod: "+s+" — datasheet/ölçüm gerekli";
    }
    private static String typeName(char c){ switch(c){case'D':return"Diyot";case'Q':return"Transistör/MOSFET";case'U':return"Entegre";case'F':return"Sigorta";case'J':return"Konnektör";case'T':return"Trafo";default:return"Elektronik eleman";} }
    private static int e96Multiplier(char c){ String k="ZYXABCDEF"; return k.indexOf(c)-3; }
    private static String format(double v){ if(v>=1e6)return trim(v/1e6)+" M";if(v>=1e3)return trim(v/1e3)+" k";return trim(v)+" "; }
    private static String formatInd(double uh){ return uh>=1000?trim(uh/1000)+" mH":trim(uh)+" µH"; }
    private static String trim(double d){ return String.format(Locale.US,d==Math.rint(d)?"%.0f":"%.2f",d); }
}
