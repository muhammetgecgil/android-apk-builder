package com.mg.fixturecockpitsim.visual;

import org.junit.Test;
import static org.junit.Assert.*;

public class True3DWorldMeshTest {
    @Test public void allWorldMeshesContainTriangleGeometry(){
        check(True3DWorldMesh.sea(),1000);
        check(True3DWorldMesh.coastLand(),1000);
        check(True3DWorldMesh.shoreline(),300);
        check(True3DWorldMesh.islands(),1000);
        check(True3DWorldMesh.mountains(),1000);
        check(True3DWorldMesh.snowCaps(),200);
    }

    @Test public void islandsAndMountainsHaveRealVerticalRelief(){
        assertTrue(verticalRange(True3DWorldMesh.islands())>3.0f);
        assertTrue(verticalRange(True3DWorldMesh.mountains())>9.0f);
        assertTrue(verticalRange(True3DWorldMesh.shoreline())>.20f);
    }

    @Test public void seaIsNotAFlatSinglePlane(){
        assertTrue(verticalRange(True3DWorldMesh.sea())>.10f);
    }

    private static void check(float[] d,int minFloats){
        assertNotNull(d);assertTrue(d.length>minFloats);assertEquals(0,d.length%7);
        for(int i=0;i<d.length;i+=7){
            for(int k=0;k<7;k++)assertTrue(Float.isFinite(d[i+k]));
            assertEquals(True3DWorldMesh.PART_WORLD,d[i+6],.001f);
            float nl=(float)Math.sqrt(d[i+3]*d[i+3]+d[i+4]*d[i+4]+d[i+5]*d[i+5]);
            assertTrue(nl>.95f&&nl<1.05f);
        }
    }

    private static float verticalRange(float[] d){
        float lo=Float.POSITIVE_INFINITY,hi=Float.NEGATIVE_INFINITY;
        for(int i=0;i<d.length;i+=7){lo=Math.min(lo,d[i+1]);hi=Math.max(hi,d[i+1]);}
        return hi-lo;
    }
}
