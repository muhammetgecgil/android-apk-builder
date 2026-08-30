package com.mg.structuralai;

/** Fail-closed validation before a user-defined physical load case is allowed to solve. */
public final class EngineeringLoadDefinitionGate {
    public static final class Result { public final boolean pass; public final String summary; Result(boolean p,String s){pass=p;summary=s;} }
    private EngineeringLoadDefinitionGate(){}

    public static Result evaluate(PhysicalLoadDefinition load,int supportPatches,int loadPicks,LinearElasticMaterial material){
        if(load==null)return new Result(false,"LOAD DEFINITION BLOCKED: physical load definition missing");
        if(!load.finite())return new Result(false,"LOAD DEFINITION BLOCKED: non-finite load/acceleration value");
        if(supportPatches<=0)return new Result(false,"LOAD DEFINITION BLOCKED: at least one support patch is required");
        if(!load.hasAnyPhysicalLoad())return new Result(false,"LOAD DEFINITION BLOCKED: no explicit physical load defined");
        if((load.hasForce()||load.hasPressure()||load.hasMoment())&&loadPicks<=0)return new Result(false,"LOAD DEFINITION BLOCKED: force/pressure/moment requires an explicit picked load region");
        if(load.hasPressure()&&!(Double.isFinite(load.pressurePa)&&Math.abs(load.pressurePa)>0))return new Result(false,"LOAD DEFINITION BLOCKED: pressure must be finite and non-zero");
        if(load.hasAcceleration()&&(material==null||!(material.densityKgM3>0)))return new Result(false,"LOAD DEFINITION BLOCKED: acceleration/gravity requires positive density");
        if(material==null||!(material.youngPa>0)||!(material.poisson>-1.0&&material.poisson<0.5))return new Result(false,"LOAD DEFINITION BLOCKED: physically valid elastic material is required");
        return new Result(true,"LOAD DEFINITION PASS | "+load.summary()+" | supportPatches="+supportPatches+" | loadRegions="+loadPicks);
    }
}
