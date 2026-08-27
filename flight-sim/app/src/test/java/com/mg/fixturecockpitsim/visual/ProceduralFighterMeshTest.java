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

    @Test public void avm3EngineAirPathPartsExist() {
        ProceduralFighterMesh.Mesh mesh = ProceduralFighterMesh.build();
        int ducts=0, compressors=0, petals=0, inner=0, flame=0, core=0;
        float ductMinZ=Float.POSITIVE_INFINITY, ductMaxZ=Float.NEGATIVE_INFINITY;
        float flameMaxZ=Float.NEGATIVE_INFINITY, coreMaxZ=Float.NEGATIVE_INFINITY;
        for (int i=0; i<mesh.data.length; i+=7) {
            float z=mesh.data[i+2], part=mesh.data[i+6];
            if (Math.abs(part-ProceduralFighterMesh.PART_INTAKE_DUCT)<0.1f) {
                ducts++; ductMinZ=Math.min(ductMinZ,z); ductMaxZ=Math.max(ductMaxZ,z);
            } else if (Math.abs(part-ProceduralFighterMesh.PART_COMPRESSOR_FACE)<0.1f) compressors++;
            else if (Math.abs(part-ProceduralFighterMesh.PART_NOZZLE_PETAL)<0.1f) petals++;
            else if (Math.abs(part-ProceduralFighterMesh.PART_NOZZLE_INNER)<0.1f) inner++;
            else if (Math.abs(part-ProceduralFighterMesh.PART_AFTERBURNER)<0.1f) {flame++; flameMaxZ=Math.max(flameMaxZ,z);}
            else if (Math.abs(part-ProceduralFighterMesh.PART_FLAME_CORE)<0.1f) {core++; coreMaxZ=Math.max(coreMaxZ,z);}
        }
        assertTrue("intake ducts missing", ducts >= 90);
        assertTrue("S-duct path too short", ductMaxZ-ductMinZ > 1.15f);
        assertTrue("compressor faces missing", compressors >= 180);
        assertTrue("nozzle petals need thickness", petals >= 600);
        assertTrue("nozzle inner geometry missing", inner >= 140);
        assertTrue("afterburner geometry missing", flame >= 100);
        assertTrue("afterburner plume too short", flameMaxZ > 5.0f);
        assertTrue("flame core missing", core >= 70);
        assertTrue("flame core too short", coreMaxZ > 4.6f);
    }
}
