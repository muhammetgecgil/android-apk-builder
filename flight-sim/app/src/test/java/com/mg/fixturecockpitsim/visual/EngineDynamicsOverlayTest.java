package com.mg.fixturecockpitsim.visual;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EngineDynamicsOverlayTest {
    @Test public void solidMeshHasValidStrideAndFanPart(){
        float[] mesh=EngineDynamicsOverlay.buildSolid();
        assertTrue(mesh.length>0);
        assertEquals(0,mesh.length%7);
        assertTrue(hasPart(mesh,EngineDynamicsOverlay.ENGINE_FAN));
        assertFinite(mesh);
    }

    @Test public void transparentMeshContainsAllDynamicEffects(){
        float[] mesh=EngineDynamicsOverlay.buildTransparent();
        assertTrue(mesh.length>0);
        assertEquals(0,mesh.length%7);
        assertTrue(hasPart(mesh,EngineDynamicsOverlay.HEAT_HAZE));
        assertTrue(hasPart(mesh,EngineDynamicsOverlay.AFTERBURNER_RING));
        assertTrue(hasPart(mesh,EngineDynamicsOverlay.FAN_BLUR));
        assertFinite(mesh);
    }

    @Test public void partIdsDoNotCollide(){
        assertFalse(EngineDynamicsOverlay.ENGINE_FAN==EngineDynamicsOverlay.HEAT_HAZE);
        assertFalse(EngineDynamicsOverlay.ENGINE_FAN==EngineDynamicsOverlay.AFTERBURNER_RING);
        assertFalse(EngineDynamicsOverlay.HEAT_HAZE==EngineDynamicsOverlay.AFTERBURNER_RING);
        assertFalse(EngineDynamicsOverlay.FAN_BLUR==EngineDynamicsOverlay.AFTERBURNER_RING);
    }

    private static boolean hasPart(float[] mesh,float part){
        for(int i=6;i<mesh.length;i+=7)if(Math.abs(mesh[i]-part)<.01f)return true;
        return false;
    }

    private static void assertFinite(float[] mesh){
        for(float v:mesh)assertTrue("non-finite mesh value",Float.isFinite(v));
    }
}
