package com.mg.machineelementspro;

import android.content.Context;

public final class ProjectIntegration {
    private ProjectIntegration(){}

    public static EngineeringProject active(Context c){
        String id=EngineeringProjectRepository.getActiveProjectId(c);
        return id==null?null:EngineeringProjectRepository.load(c,id);
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
}
