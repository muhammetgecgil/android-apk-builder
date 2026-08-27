package com.mg.structuralai;

import java.util.*;

/** Curated engineering reference library. Reference entries are not design-certification evidence. */
public final class MaterialLibrary {
    public static final class Entry {
        public final String id,family,condition,reference;
        public final LinearElasticMaterial material;
        Entry(String id,String family,String condition,LinearElasticMaterial material,String reference){this.id=id;this.family=family;this.condition=condition;this.material=material;this.reference=reference;}
        public String summary(){return id+" | "+family+" | "+condition+" | E="+(material.youngPa/1e9)+" GPa | nu="+material.poisson+" | rho="+material.densityKgM3+" kg/m3 | Sy="+(material.yieldPa/1e6)+" MPa | Su="+(material.ultimatePa/1e6)+" MPa | evidence="+material.evidenceLevel;}
    }
    private static final LinkedHashMap<String,Entry> ENTRIES=new LinkedHashMap<>();
    static {
        add("STEEL_S355_REF","Structural steel","reference nominal",210e9,0.30,7850,355e6,470e6,"Reference nominal values; verify governing product standard/certificate before design release");
        add("AISI_304_REF","Stainless steel","annealed reference",193e9,0.29,8000,215e6,505e6,"Reference nominal values; verify heat/product certificate before design release");
        add("AL_6061_T6_REF","Aluminium","6061-T6 reference",68.9e9,0.33,2700,276e6,310e6,"Reference nominal values; verify material specification and lot certificate before design release");
        add("AL_7075_T6_REF","Aluminium","7075-T6 reference",71.7e9,0.33,2810,503e6,572e6,"Reference nominal values; verify material specification and lot certificate before design release");
        add("TI_6AL4V_REF","Titanium","Ti-6Al-4V reference",113.8e9,0.342,4430,880e6,950e6,"Reference nominal values; verify material specification/condition before design release");
    }
    private MaterialLibrary(){}
    private static void add(String id,String family,String condition,double e,double nu,double rho,double sy,double su,String ref){
        LinearElasticMaterial m=new LinearElasticMaterial(id,e,nu,rho,sy,su,ref,LinearElasticMaterial.EvidenceLevel.REFERENCE);
        ENTRIES.put(id,new Entry(id,family,condition,m,ref));
    }
    public static List<Entry> all(){return Collections.unmodifiableList(new ArrayList<>(ENTRIES.values()));}
    public static Entry get(String id){return ENTRIES.get(id);}
    public static LinearElasticMaterial userCertified(String name,double e,double nu,double rho,double sy,double su,String certificateOrStandard){
        if(certificateOrStandard==null||certificateOrStandard.trim().length()<4)throw new IllegalArgumentException("Material certificate/standard provenance required");
        return new LinearElasticMaterial(name,e,nu,rho,sy,su,certificateOrStandard,LinearElasticMaterial.EvidenceLevel.USER_CERTIFIED);
    }
}
