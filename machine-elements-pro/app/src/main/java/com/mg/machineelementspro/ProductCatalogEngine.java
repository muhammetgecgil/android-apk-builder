package com.mg.machineelementspro;

import java.util.ArrayList;
import java.util.List;

public final class ProductCatalogEngine {
    public static final class CatalogMatch {
        public final String category;
        public final String calculatedSelection;
        public final String vendor;
        public final String region;
        public final String catalogLabel;
        public final String url;
        public final String note;
        CatalogMatch(String category,String calculatedSelection,String vendor,String region,String catalogLabel,String url,String note){
            this.category=category;this.calculatedSelection=calculatedSelection;this.vendor=vendor;this.region=region;this.catalogLabel=catalogLabel;this.url=url;this.note=note;
        }
    }

    public static List<CatalogMatch> bearingMatches(double boreMm,double requiredDynamicN){
        if(boreMm<=0||requiredDynamicN<=0) throw new IllegalArgumentException("Bore ve gerekli dinamik yük pozitif olmalı.");
        SelectionCatalog.Bearing b=SelectionCatalog.selectBearing(boreMm,requiredDynamicN);
        List<CatalogMatch> out=new ArrayList<>();
        String selection="Aday rulman: "+b.code+" • bore="+b.bore+" mm • C="+Math.round(b.C)+" N";
        out.add(new CatalogMatch("Rulman",selection,"Schaeffler / FAG","Avrupa","medias Bearing Selection","https://medias.schaeffler.com/","Hesaplanan aday kodu medias içinde doğrulayın; gerçek stok ve teslim süresi ayrıca kontrol edilmelidir."));
        out.add(new CatalogMatch("Rulman",selection,"Schaeffler Türkiye","Türkiye","Schaeffler Türkiye","https://www.schaeffler.com.tr/","Türkiye tedariki için üretici/distribütör doğrulaması gerekir."));
        return out;
    }

    public static List<CatalogMatch> boltMatches(double requiredNominalMm,String propertyClass){
        if(requiredNominalMm<=0) throw new IllegalArgumentException("Gerekli nominal çap pozitif olmalı.");
        int m=EngineeringLibrary.selectMetricBolt(requiredNominalMm);
        String pc=(propertyClass==null||propertyClass.trim().isEmpty())?"10.9":propertyClass.trim();
        String selection="M"+m+" • property class "+pc;
        List<CatalogMatch> out=new ArrayList<>();
        out.add(new CatalogMatch("Civata",selection,"Würth Türkiye","Türkiye","Dijital Katalog","https://www.wurth.com.tr/tr/wuerth_tr/services/dijital_katalog/katalog_index.php","DIN/ISO, kaplama, diş boyu ve gerçek proof/load gereksinimini ürün sayfasında doğrulayın."));
        out.add(new CatalogMatch("Civata",selection,"Bossard","Avrupa","Fastening Product Solutions","https://www.bossard.com/global-en/product-solutions/","Avrupa alternatifi; ölçü ve sınıf eşleşmesi katalogda doğrulanmalıdır."));
        return out;
    }

    public static List<CatalogMatch> couplingMatches(double designTorqueNm,double shaftDiameterMm){
        if(designTorqueNm<=0||shaftDiameterMm<=0) throw new IllegalArgumentException("Tork ve mil çapı pozitif olmalı.");
        String selection="Tasarım torku ≥ "+Math.round(designTorqueNm)+" Nm • bore ≥ "+shaftDiameterMm+" mm";
        List<CatalogMatch> out=new ArrayList<>();
        out.add(new CatalogMatch("Kaplin",selection,"KTR Turkey","Türkiye","Drive Technology Catalogue","https://www.ktr.com/tr/tr/hizmetler-ve-araclar/kataloglar-ve-brosuerler/","ROTEX/servo/lamelli vb. aile seçimi hız, kaçıklık, servis faktörü ve ortam koşullarına göre katalogdan yapılmalıdır."));
        out.add(new CatalogMatch("Kaplin",selection,"KTR Europe","Avrupa","Drive Technology Catalogue","https://www.ktr.com/de/en/services-and-tools/catalogues-and-brochures/","Avrupa katalog alternatifi; seçilen size için izin verilen tork ve bore aralığını doğrulayın."));
        return out;
    }

    public static List<CatalogMatch> gearboxMatches(double powerKw,double inputRpm,double outputRpm,double requiredTorqueNm){
        if(powerKw<=0||inputRpm<=0||outputRpm<=0||requiredTorqueNm<=0) throw new IllegalArgumentException("Gearbox girdileri pozitif olmalı.");
        double ratio=inputRpm/outputRpm;
        String selection=String.format(java.util.Locale.US,"P=%.2f kW • i=%.2f • Treq=%.1f Nm",powerKw,ratio,requiredTorqueNm);
        List<CatalogMatch> out=new ArrayList<>();
        out.add(new CatalogMatch("Redüktör",selection,"Bonfiglioli Türkiye","Türkiye","Ürün seçimi","https://www.bonfiglioli.com/turkey/tr/","Nominal/peak tork, servis faktörü, montaj pozisyonu ve termal limit katalogda doğrulanmalıdır."));
        out.add(new CatalogMatch("Redüktör",selection,"Bonfiglioli Europe","Avrupa","Product selection","https://www.bonfiglioli.com/","Avrupa ürün ailesi eşleştirmesi; stok ve teslim süresi canlı doğrulanmalıdır."));
        return out;
    }

    public static List<CatalogMatch> beltMatches(double powerKw,double smallPulleyMm,double rpm,double requiredT1N){
        if(powerKw<=0||smallPulleyMm<=0||rpm<=0||requiredT1N<=0) throw new IllegalArgumentException("Kayış girdileri pozitif olmalı.");
        double speed=Math.PI*(smallPulleyMm/1000.0)*rpm/60.0;
        String family=powerKw<=3?"SPZ / XPZ":powerKw<=12?"SPA / XPA":powerKw<=35?"SPB / XPB":"SPC / XPC";
        String selection=String.format(java.util.Locale.US,"Aday profil ailesi: %s • P=%.2f kW • v=%.2f m/s • T1≥%.0f N",family,powerKw,speed,requiredT1N);
        List<CatalogMatch> out=new ArrayList<>();
        out.add(new CatalogMatch("Kayış",selection,"Optibelt","Türkiye","Türkiye ürün kataloğu","https://web.optibelt.com/en-tr/all-products/v-belts","Kasnak çapı, sarım açısı, servis faktörü, kayış sayısı ve seçilen profil için güç tablosunu resmi katalogda doğrulayın."));
        out.add(new CatalogMatch("Kayış",selection,"Optibelt","Avrupa","Product Finder","https://www.optibelt.com/en/microsites/product-finder/","Profil ailesi bir ön eşleştirmedir; nihai uzunluk ve kayış adedi ürün seçiciyle doğrulanmalıdır."));
        return out;
    }

    public static List<CatalogMatch> chainMatches(double powerKw,double rpm,double chainPullN,double serviceFactor){
        if(powerKw<=0||rpm<=0||chainPullN<=0||serviceFactor<=0) throw new IllegalArgumentException("Zincir girdileri pozitif olmalı.");
        double designPull=chainPullN*serviceFactor;
        String family=designPull<=4000?"08B":designPull<=7000?"10B":designPull<=11000?"12B":designPull<=18000?"16B":"20B ve üzeri";
        String selection=String.format(java.util.Locale.US,"Aday roller-chain ailesi: %s • P=%.2f kW • n=%.0f rpm • Fdesign=%.0f N",family,powerKw,rpm,designPull);
        List<CatalogMatch> out=new ArrayList<>();
        out.add(new CatalogMatch("Zincir",selection,"iwis","Türkiye / Avrupa","Sales Partner & Distributor Locator","https://www.iwis.com/en-en/about-us/locations/sales-distribution-organization","ISO/BS zincir serisi, dişli diş sayısı, hız, yağlama ve yorulma kapasitesi üretici tablosunda doğrulanmalıdır."));
        out.add(new CatalogMatch("Zincir",selection,"iwis drive systems","Avrupa","Drive chains","https://www.iwis.com/","Aday aile yalnız ön seçimdir; gerçek kopma yükü değil izin verilen çalışma yükü ve servis koşulları esas alınmalıdır."));
        return out;
    }

    private ProductCatalogEngine(){}
}
