package com.mg.structuralai;

import java.util.Map;
import java.util.WeakHashMap;

/** Binds a solved FEM result to the exact material object used for that solve without retaining results strongly. */
public final class FemMaterialRegistry {
    private static final Map<StaticFemSolver.Result,LinearElasticMaterial> MAP=new WeakHashMap<>();
    private FemMaterialRegistry(){}
    public static synchronized void bind(StaticFemSolver.Result r,LinearElasticMaterial m){if(r!=null&&m!=null)MAP.put(r,m);}
    public static synchronized LinearElasticMaterial get(StaticFemSolver.Result r){return r==null?null:MAP.get(r);}
}
