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

    @Test public void avm4AirframeProportionsStayMature() {
        AirframeShapeProfile.validate();
        ProceduralFighterMesh.Mesh mesh = ProceduralFighterMesh.build();
        float minX=Float.POSITIVE_INFINITY,maxX=Float.NEGATIVE_INFINITY;
        float minZ=Float.POSITIVE_INFINITY,maxZ=Float.NEGATIVE_INFINITY;
        int skin=0;
        for (int i=0;i<mesh.data.length;i+=7) {
            float x=mesh.data[i], z=mesh.data[i+2], part=mesh.data[i+6];
            minX=Math.min(minX,x); maxX=Math.max(maxX,x);
            minZ=Math.min(minZ,z); maxZ=Math.max(maxZ,z);
            if (Math.abs(part-ProceduralFighterMesh.PART_SKIN)<0.1f) skin++;
        }
        float span=maxX-minX;
        float length=maxZ-minZ;
        assertTrue("airframe skin density too low", skin > 1800);
        assertTrue("fighter span too small", span > 9.8f);
        assertTrue("fighter planform too short", length > 11.0f);
        assertTrue("fighter planform too stubby", span/length > 0.72f);
        assertTrue("fighter planform too wide", span/length < 1.02f);
        assertTrue("forebody must stay fine", AirframeShapeProfile.HALF_WIDTH[2] < 0.22f);
        assertTrue("centre body needs blended volume", AirframeShapeProfile.HALF_WIDTH[9] > 1.10f);
        assertTrue("aft engine shoulder needs volume", AirframeShapeProfile.ENGINE_R[4] >= 0.60f);
        assertTrue("wing crank needs outboard reach", AirframeShapeProfile.WING_ROOT[4][0] >= 5.0f);
    }

    @Test public void avm5UpperFuselageHasContinuousCrownWithoutBalloonBelly() {
        AirframeShapeProfile.validate();
        float c7=AirframeShapeProfile.upperCrown(7);
        float c8=AirframeShapeProfile.upperCrown(8);
        float c9=AirframeShapeProfile.upperCrown(9);
        float c10=AirframeShapeProfile.upperCrown(10);
        float c11=AirframeShapeProfile.upperCrown(11);
        assertTrue("cockpit shoulder crown too low", c7 > 0.75f);
        assertTrue("upper centre body needs stronger crown", c8 > 0.84f);
        assertTrue("dorsal high point too weak", c9 > 0.86f);
        assertTrue("spine must stay full behind cockpit", c10 > 0.80f);
        assertTrue("spine must taper gradually into engine deck", c11 > 0.67f);
        assertTrue("upper crown must taper aft", c9 > c10 && c10 > c11);
        assertTrue("centre belly too deep", AirframeShapeProfile.lowerBelly(8) > -0.70f);
        assertTrue("aft-centre belly too deep", AirframeShapeProfile.lowerBelly(9) > -0.70f);
        assertTrue("intake shoulder should meet upper body", AirframeShapeProfile.INTAKE_SHOULDER[3][1] >= 0.31f);
        assertTrue("wing root should blend into upper shoulder", AirframeShapeProfile.WING_ROOT[0][1] >= 0.21f);
    }

    @Test public void avm5UpperSurfacePanelsAreDenseSymmetricAndAftBlended() {
        ProceduralFighterMesh.Mesh mesh=ProceduralFighterMesh.build();
        int count=0,left=0,right=0,centre=0;
        float minZ=Float.POSITIVE_INFINITY,maxZ=Float.NEGATIVE_INFINITY,maxY=Float.NEGATIVE_INFINITY;
        for(int i=0;i<mesh.data.length;i+=7){
            float x=mesh.data[i],y=mesh.data[i+1],z=mesh.data[i+2],part=mesh.data[i+6];
            if(Math.abs(part-ProceduralFighterMesh.PART_UPPER_PANEL)<0.1f){
                count++; minZ=Math.min(minZ,z); maxZ=Math.max(maxZ,z); maxY=Math.max(maxY,y);
                if(x<-.08f) left++; else if(x>.08f) right++; else centre++;
            }
        }
        assertTrue("upper surface detail density too low", count > 300);
        assertTrue("upper panels must span canopy shoulder to tail root", minZ < -1.7f && maxZ > 3.0f);
        assertTrue("upper panels need visible crown", maxY > .88f);
        assertTrue("upper panels need centre spine detail", centre > 30);
        assertTrue("left upper surface detail missing", left > 100);
        assertTrue("right upper surface detail missing", right > 100);
        assertTrue("upper detail must stay bilaterally balanced", Math.abs(left-right) < 45);
    }

    @Test public void avm6ForebodyAndEngineShouldersBlendProgressively() {
        AirframeShapeProfile.validate();
        float c5=AirframeShapeProfile.upperCrown(5);
        float c6=AirframeShapeProfile.upperCrown(6);
        float c7=AirframeShapeProfile.upperCrown(7);
        assertTrue("forebody crown must rise into canopy support", c5 < c6 && c6 < c7);
        assertTrue("forward canopy shoulder too narrow", AirframeShapeProfile.HALF_WIDTH[6] >= .90f);
        assertTrue("mid canopy shoulder too narrow", AirframeShapeProfile.HALF_WIDTH[7] >= 1.04f);
        assertTrue("engine blend needs more longitudinal stations", AirframeShapeProfile.ENGINE_Z.length >= 9);
        assertTrue("engine must emerge gently from centre body", AirframeShapeProfile.ENGINE_R[0] <= .30f);
        for(int i=1;i<=5;i++) {
            assertTrue("engine shoulder should grow smoothly before peak", AirframeShapeProfile.ENGINE_R[i] > AirframeShapeProfile.ENGINE_R[i-1]);
        }
        assertTrue("engine deck peak too weak", AirframeShapeProfile.ENGINE_R[5] >= .65f);
        assertTrue("engine deck top too low", AirframeShapeProfile.engineShoulderTop(5) >= .27f);
        assertTrue("aft engine radius must taper toward nozzle", AirframeShapeProfile.ENGINE_R[8] < AirframeShapeProfile.ENGINE_R[6]);
    }
}
