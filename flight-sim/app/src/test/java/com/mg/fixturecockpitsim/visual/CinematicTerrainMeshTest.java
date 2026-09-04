package com.mg.fixturecockpitsim.visual;

import org.junit.Test;
import static org.junit.Assert.*;

public class CinematicTerrainMeshTest {
    @Test public void torosMeshIsDenseAndActuallyThreeDimensional(){
        float[] m=CinematicTerrainMesh.build(0);assertTrue(m.length>20000);float minY=999,maxY=-999;
        for(int i=0;i<m.length;i+=7){minY=Math.min(minY,m[i+1]);maxY=Math.max(maxY,m[i+1]);assertEquals(60f,m[i+6],.001f);}assertTrue(maxY-minY>8f);
    }
    @Test public void terrainKindsHaveDistinctPartIds(){
        for(int k=0;k<5;k++){float[] m=CinematicTerrainMesh.build(k);assertTrue(m.length>10000);assertEquals(60f+k,m[6],.001f);}
    }
    @Test public void undersideSealIsOpaqueClosureGeometry(){
        float[] m=AirframeUndersideSeal.build();assertTrue(m.length>=300);for(int i=0;i<m.length;i+=7)assertEquals(64f,m[i+6],.001f);
    }
}
