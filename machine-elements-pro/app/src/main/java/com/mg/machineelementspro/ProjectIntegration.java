package com.mg.machineelementspro;

import android.content.Context;

public final class ProjectIntegration {
    private ProjectIntegration(){}

    public static EngineeringProject active(Context c){
        return EngineeringProjectRepository.active(c);
    }

    public static String saveShaftBearing(Context c,SystemSelectionEngine.ShaftBearingFitResult r,double radialN,double axialN,double rpm,double targetLifeH){
        EngineeringProject p=active(c); if(p==null)return null;
        p.upsert("S_SYS1","SHAFT")
                .put("requiredDiameterMm",r.shaftRequiredMm)
                .put("preferredDiameterMm",r.shaftPreferredMm)
                .put("shaftFit",r.shaftFit)
                .put("housingFit",r.housingFit);
        p.upsert("B_SYS1","BEARING")
                .put("designation",r.bearing)
                .put("radialLoadN",radialN)
                .put("axialLoadN",axialN)
                .put("rpm",rpm)
                .put("targetLifeH",targetLifeH)
                .put("lifeHours",r.bearingLifeHours)
                .put("staticFoS",r.bearingStaticFoS)
                .put("shaft","S_SYS1");
        EngineeringProjectRepository.save(c,p);return p.name;
    }

    public static String saveBoltJoint(Context c,double diameterMm,SystemSelectionEngine.TighteningResult r){
        EngineeringProject p=active(c); if(p==null)return null;
        p.upsert("BJ_SYS1","BOLT_JOINT")
                .put("diameterMm",diameterMm)
                .put("propertyClass","10.9")
                .put("preloadMinN",r.preloadMinN)
                .put("preloadNominalN",r.nominalPreloadN)
                .put("preloadMaxN",r.preloadMaxN)
                .put("torqueMinNm",r.torqueMinNm)
                .put("torqueNominalNm",r.torqueNominalNm)
                .put("torqueMaxNm",r.torqueMaxNm);
        EngineeringProjectRepository.save(c,p);return p.name;
    }

    public static String saveProduct(Context c,int type,ProductCatalogEngine.CatalogMatch m){
        EngineeringProject p=active(c); if(p==null)return null;
        String id="PS_"+type+"_"+Math.abs((m.vendor+"|"+m.calculatedSelection).hashCode());
        p.upsert(id,"PRODUCT_SELECTION")
                .put("catalogType",type)
                .put("region",m.region)
                .put("vendor",m.vendor)
                .put("selection",m.calculatedSelection)
                .put("catalogLabel",m.catalogLabel)
                .put("url",m.url)
                .put("note",m.note);
        EngineeringProjectRepository.save(c,p);return p.name;
    }

    public static String saveDrivetrain(Context c,DrivetrainEngine.Input x,DrivetrainEngine.Result r){
        EngineeringProject p=active(c); if(p==null)return null;
        p.upsert("S_DR1","SHAFT")
                .put("diameterMm",x.shaftDiameterMm)
                .put("yieldMpa",x.shaftYieldMpa)
                .put("torqueNm",x.torqueNm)
                .put("rpm",x.rpm)
                .put("maxMomentNm",r.momentNm)
                .put("stressMpa",r.shaftStressMpa)
                .put("fos",r.shaftFos);
        p.upsert("B_DR_A","BEARING")
                .put("shaft","S_DR1").put("position","A")
                .put("capacityN",x.bearingC1N)
                .put("equivalentLoadN",r.bearing1Load)
                .put("lifeHours",r.l10h1);
        p.upsert("B_DR_B","BEARING")
                .put("shaft","S_DR1").put("position","B")
                .put("capacityN",x.bearingC2N)
                .put("equivalentLoadN",r.bearing2Load)
                .put("lifeHours",r.l10h2);
        p.upsert("C_DR1","COUPLING")
                .put("shaft","S_DR1")
                .put("designTorqueNm",x.torqueNm)
                .put("boreMm",x.shaftDiameterMm);
        p.upsert("G_DR1","GEAR_LOAD")
                .put("pitchDiameterMm",x.pitchDiameterMm)
                .put("ftN",r.ft).put("frN",r.fr).put("faN",r.fa);
        EngineeringProjectRepository.save(c,p);return p.name;
    }

    public static String saveAssemblyBoltGroup(Context c,double[] v,String resultBody,String status){
        EngineeringProject p=active(c); if(p==null)return null;
        p.upsert("BJ_ASM1","BOLT_JOINT")
                .put("module","bolt_group")
                .put("forceN",v[0]).put("momentNm",v[1]).put("pcdMm",v[2])
                .put("boltCount",v[3]).put("diameterMm",v[4]).put("yieldMpa",v[5])
                .put("propertyClass","10.9")
                .put("status",status).put("result",resultBody);
        EngineeringProjectRepository.save(c,p);return p.name;
    }
}
