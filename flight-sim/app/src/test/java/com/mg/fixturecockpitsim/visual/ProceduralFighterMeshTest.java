package com.mg.fixturecockpitsim.visual;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProceduralFighterMeshTest {
    @Test public void avm2CanopyCockpitPartsExistAndStayInBounds() {
        ProceduralFighterMesh.Mesh mesh = ProceduralFighterMesh.build();
        assertTrue("mesh must be substantial", mesh.vertexCount() > 1500);

        int canopy=0, frame=0, tub=0, seat=0, coaming=0;
        float canopyMinZ=Float.POSITIVE_INFINITY, canopyMaxZ=Float.NEGATIVE_INFINITY;
        float canopyMinY=Float.POSITIVE_INFINITY, canopyMaxY=Float.NEGATIVE_INFINITY;

        for (int i=0; i<mesh.data.length; i+=7) {
            float x=mesh.data[i], y=mesh.data[i+1], z=mesh.data[i+2], part=mesh.data[i+6];
            assertTrue("finite x", Float.isFinite(x));
            assertTrue("finite y", Float.isFinite(y));
            assertTrue("finite z", Float.isFinite(z));
            if (Math.abs(part-ProceduralFighterMesh.PART_CANOPY)<0.1f) {
                canopy++;
                canopyMinZ=Math.min(canopyMinZ,z); canopyMaxZ=Math.max(canopyMaxZ,z);
                canopyMinY=Math.min(canopyMinY,y); canopyMaxY=Math.max(canopyMaxY,y);
            } else if (Math.abs(part-ProceduralFighterMesh.PART_CANOPY_FRAME)<0.1f) frame++;
            else if (Math.abs(part-ProceduralFighterMesh.PART_COCKPIT_TUB)<0.1f) tub++;
            else if (Math.abs(part-ProceduralFighterMesh.PART_SEAT)<0.1f) seat++;
            else if (Math.abs(part-ProceduralFighterMesh.PART_COAMING)<0.1f) coaming++;
        }

        assertTrue("canopy geometry missing", canopy > 300);
        assertTrue("canopy frame geometry missing", frame > 60);
        assertTrue("cockpit tub geometry missing", tub > 30);
        assertTrue("seat geometry missing", seat > 30);
        assertTrue("coaming geometry missing", coaming > 20);

        assertTrue("canopy must be longitudinally mature", canopyMaxZ-canopyMinZ > 2.3f);
        assertTrue("canopy crown too low", canopyMaxY > 1.15f);
        assertTrue("canopy crown too tall", canopyMaxY < 1.45f);
        assertTrue("canopy base must sit on sill", canopyMinY > 0.70f);
    }
}
