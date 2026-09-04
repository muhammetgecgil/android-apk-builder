package com.mg.fixturecockpitsim.visual;

import org.junit.Test;
import static org.junit.Assert.*;

public class CinematicTerrainMeshTest {
    @Test public void torosMeshIsDenseAndDeeplyThreeDimensional(){
        float[] m=CinematicTerrainMesh.build(0);
        assertTrue(m.length>150000);
        float minY=999,maxY=-999,minNy=1,maxNy=-1;
        for(int i=0;i<m.length;i+=7){
            minY=Math.min(minY,m[i+1]);maxY=Math.max(maxY,m[i+1]);
            minNy=Math.min(minNy,m[i+4]);maxNy=Math.max(maxNy,m[i+4]);
            assertEquals(60f,m[i+6],.001f);
            float n=(float)Math.sqrt(m[i+3]*m[i+3]+m[i+4]*m[i+4]+m[i+5]*m[i+5]);
            assertEquals(1f,n,.012f);
        }
        assertTrue(maxY-minY>24f);
        assertTrue(maxNy-minNy>.12f);
    }

    @Test public void everyRouteTerrainIsDenseAndHasRealRelief(){
        for(int k=0;k<5;k++){
            float[] m=CinematicTerrainMesh.build(k);assertTrue(m.length>150000);
            float expected=k==4?65f:60f+k,minY=999,maxY=-999;
            for(int i=0;i<m.length;i+=7){minY=Math.min(minY,m[i+1]);maxY=Math.max(maxY,m[i+1]);assertEquals(expected,m[i+6],.001f);}
            assertTrue("kind="+k,maxY-minY>4f);
        }
    }

    @Test public void torosNormalsVaryAcrossRidgesAndValleys(){
        float[] m=CinematicTerrainMesh.build(0);float first=m[4];boolean varied=false;
        for(int i=7;i<m.length;i+=7)if(Math.abs(m[i+4]-first)>.08f){varied=true;break;}
        assertTrue(varied);
    }
}
