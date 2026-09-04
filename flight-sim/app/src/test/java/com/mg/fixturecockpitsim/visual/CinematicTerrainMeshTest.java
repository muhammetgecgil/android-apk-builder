package com.mg.fixturecockpitsim.visual;

import org.junit.Test;
import static org.junit.Assert.*;

public class CinematicTerrainMeshTest {
    @Test public void torosIsHighDensityRealThreeDimensionalGeometry(){
        float[] m=CinematicTerrainMesh.build(0);
        assertEquals(0,m.length%7);
        assertTrue("Taurus mesh must be substantially denser than v108",m.length/7>26000);
        float minY=Float.POSITIVE_INFINITY,maxY=Float.NEGATIVE_INFINITY;
        boolean rock=false,snow=false;
        for(int i=0;i<m.length;i+=7){
            minY=Math.min(minY,m[i+1]);maxY=Math.max(maxY,m[i+1]);
            rock|=Math.abs(m[i+6]-CinematicTerrainMesh.PART_ROCK)<.01f;
            snow|=Math.abs(m[i+6]-CinematicTerrainMesh.PART_SNOW)<.01f;
            assertTrue(Float.isFinite(m[i]));assertTrue(Float.isFinite(m[i+1]));assertTrue(Float.isFinite(m[i+2]));
        }
        assertTrue("Taurus relief must read as a mountain mass",maxY-minY>25f);
        assertTrue("rock faces required",rock);
        assertTrue("high peaks need snow material",snow);
    }

    @Test public void generatedNormalsAreUsableForTrue3dLighting(){
        float[] m=CinematicTerrainMesh.build(0);
        for(int i=0;i<m.length;i+=7*97){
            float nx=m[i+3],ny=m[i+4],nz=m[i+5];
            float len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
            assertEquals(1f,len,.03f);
        }
    }

    @Test public void everyJourneyTerrainIsDenseAndVolumetric(){
        for(int k=0;k<5;k++){
            float[] m=CinematicTerrainMesh.build(k);
            assertEquals(0,m.length%7);
            assertTrue("terrain kind "+k+" is too sparse",m.length/7>16000);
            float minY=999f,maxY=-999f;
            for(int i=0;i<m.length;i+=7){minY=Math.min(minY,m[i+1]);maxY=Math.max(maxY,m[i+1]);}
            assertTrue("terrain kind "+k+" is flat",maxY-minY>2.2f);
        }
    }
}
