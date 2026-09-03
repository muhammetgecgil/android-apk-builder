package com.mg.machineelementspro;

import java.util.ArrayList;
import java.util.List;

public final class DesignReviewEngine {
    private DesignReviewEngine(){}

    public enum Severity { CRITICAL, WARNING, OK, MISSING }

    public static final class Finding {
        public final Severity severity;
        public final String elementId,type,title,detail;
        public Finding(Severity severity,String elementId,String type,String title,String detail){
            this.severity=severity;this.elementId=elementId;this.type=type;this.title=title;this.detail=detail;
        }
    }

    public static final class Review {
        public final int critical,warning,ok,missing;
        public final double minFos,minBearingLifeH;
        public final List<Finding> findings;
        Review(int c,int w,int o,int m,double minFos,double minBearingLifeH,List<Finding> f){
            critical=c;warning=w;ok=o;missing=m;this.minFos=minFos;this.minBearingLifeH=minBearingLifeH;findings=f;
        }
        public String overall(){return critical>0?"KRİTİK":warning>0||missing>0?"İNCELEME GEREKLİ":"UYGUN";}
    }

    public static Review review(EngineeringProject p){
        if(p==null)throw new IllegalArgumentException("Project required");
        List<Finding> out=new ArrayList<>();
        double minFos=Double.POSITIVE_INFINITY,minLife=Double.POSITIVE_INFINITY;
        int c=0,w=0,o=0,m=0; boolean hasProduct=false;
        for(EngineeringProject.Element e:p.elements){
            if("PRODUCT_SELECTION".equals(e.type))hasProduct=true;
            Double fos=firstDouble(e,"fos","shaftFos","staticFoS","gearSafetyFactor");
            if(fos!=null){minFos=Math.min(minFos,fos);Finding f;
                if(fos<1.0)f=new Finding(Severity.CRITICAL,e.id,e.type,"Emniyet katsayısı kritik","FoS="+fmt(fos));
                else if(fos<1.5)f=new Finding(Severity.WARNING,e.id,e.type,"Emniyet katsayısı sınırda","FoS="+fmt(fos));
                else f=new Finding(Severity.OK,e.id,e.type,"Emniyet katsayısı uygun","FoS="+fmt(fos));
                out.add(f); if(f.severity==Severity.CRITICAL)c++;else if(f.severity==Severity.WARNING)w++;else o++;
            }
            Double life=firstDouble(e,"lifeHours","l10h","l10h1","l10h2");
            if(life!=null){minLife=Math.min(minLife,life);Finding f;
                if(life<1000)f=new Finding(Severity.CRITICAL,e.id,e.type,"Rulman ömrü kritik","L10h="+fmt(life)+" h");
                else if(life<3000)f=new Finding(Severity.WARNING,e.id,e.type,"Rulman ömrü düşük","L10h="+fmt(life)+" h");
                else f=new Finding(Severity.OK,e.id,e.type,"Rulman ömrü uygun","L10h="+fmt(life)+" h");
                out.add(f); if(f.severity==Severity.CRITICAL)c++;else if(f.severity==Severity.WARNING)w++;else o++;
            }
            if("BOLT_JOINT".equals(e.type) && e.get("torqueNominalNm")==null && e.get("diameterMm")!=null){out.add(new Finding(Severity.MISSING,e.id,e.type,"Sıkma torku eksik","Bolt joint var fakat nominal tightening torque kaydı yok."));m++;}
            if("BEARING".equals(e.type) && e.get("designation")==null){out.add(new Finding(Severity.MISSING,e.id,e.type,"Rulman ürün kodu eksik","Bearing hesabı var fakat designation/ürün seçimi yok."));m++;}
        }
        if(!hasProduct){out.add(new Finding(Severity.MISSING,"PROJECT","PROJECT","Ürün seçimi eksik","Projede Türkiye/Avrupa PRODUCT_SELECTION kaydı bulunmuyor."));m++;}
        if(p.elements.isEmpty()){out.add(new Finding(Severity.MISSING,"PROJECT","PROJECT","Proje boş","Design Review için bağlı mühendislik elemanı yok."));m++;}
        return new Review(c,w,o,m,minFos==Double.POSITIVE_INFINITY?Double.NaN:minFos,minLife==Double.POSITIVE_INFINITY?Double.NaN:minLife,out);
    }

    private static Double firstDouble(EngineeringProject.Element e,String...keys){for(String k:keys){String v=e.get(k);if(v!=null)try{return Double.parseDouble(v);}catch(Exception ignored){}}return null;}
    private static String fmt(double x){return String.format(java.util.Locale.US,"%.4g",x);}
}
