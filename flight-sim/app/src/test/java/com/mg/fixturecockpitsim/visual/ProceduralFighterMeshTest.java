package com.mg.fixturecockpitsim.visual;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ProceduralFighterMeshTest {
    @Test public void avm2CanopyCockpitPartsExistAndStayInBounds(){
        ProceduralFighterMesh.Mesh mesh=ProceduralFighterMesh.build(); assertTrue("mesh must be substantial",mesh.vertexCount()>1500);
        int canopy=0,frame=0,tub=0,seat=0,coaming=0; float minZ=Float.POSITIVE_INFINITY,maxZ=Float.NEGATIVE_INFINITY,minY=Float.POSITIVE_INFINITY,maxY=Float.NEGATIVE_INFINITY;
        for(int i=0;i<mesh.data.length;i+=7){float y=mesh.data[i+1],z=mesh.data[i+2],p=mesh.data[i+6]; assertTrue(Float.isFinite(mesh.data[i])&&Float.isFinite(y)&&Float.isFinite(z));
            if(Math.abs(p-ProceduralFighterMesh.PART_CANOPY)<.1f){canopy++;minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);minY=Math.min(minY,y);maxY=Math.max(maxY,y);} else if(Math.abs(p-ProceduralFighterMesh.PART_CANOPY_FRAME)<.1f)frame++; else if(Math.abs(p-ProceduralFighterMesh.PART_COCKPIT_TUB)<.1f)tub++; else if(Math.abs(p-ProceduralFighterMesh.PART_SEAT)<.1f)seat++; else if(Math.abs(p-ProceduralFighterMesh.PART_COAMING)<.1f)coaming++;}
        assertTrue(canopy>300&&frame>60&&tub>30&&seat>30&&coaming>20); assertTrue(maxZ-minZ>2.3f); assertTrue(maxY>1.15f&&maxY<1.45f); assertTrue(minY>.70f);
    }
    @Test public void avm3EngineAirPathPartsExist(){
        ProceduralFighterMesh.Mesh mesh=ProceduralFighterMesh.build(); int ducts=0,compressors=0,petals=0,inner=0,flame=0,core=0; float d0=99,d1=-99,f=-99,c=-99;
        for(int i=0;i<mesh.data.length;i+=7){float z=mesh.data[i+2],p=mesh.data[i+6]; if(Math.abs(p-ProceduralFighterMesh.PART_INTAKE_DUCT)<.1f){ducts++;d0=Math.min(d0,z);d1=Math.max(d1,z);} else if(Math.abs(p-ProceduralFighterMesh.PART_COMPRESSOR_FACE)<.1f)compressors++; else if(Math.abs(p-ProceduralFighterMesh.PART_NOZZLE_PETAL)<.1f)petals++; else if(Math.abs(p-ProceduralFighterMesh.PART_NOZZLE_INNER)<.1f)inner++; else if(Math.abs(p-ProceduralFighterMesh.PART_AFTERBURNER)<.1f){flame++;f=Math.max(f,z);} else if(Math.abs(p-ProceduralFighterMesh.PART_FLAME_CORE)<.1f){core++;c=Math.max(c,z);}}
        assertTrue(ducts>=90&&d1-d0>1.15f&&compressors>=180&&petals>=600&&inner>=140&&flame>=100&&f>5f&&core>=70&&c>4.6f);
    }
    @Test public void avm4AirframeProportionsStayMature(){
        AirframeShapeProfile.validate(); ProceduralFighterMesh.Mesh m=ProceduralFighterMesh.build(); float minX=99,maxX=-99,minZ=99,maxZ=-99;int skin=0;
        for(int i=0;i<m.data.length;i+=7){minX=Math.min(minX,m.data[i]);maxX=Math.max(maxX,m.data[i]);minZ=Math.min(minZ,m.data[i+2]);maxZ=Math.max(maxZ,m.data[i+2]);if(Math.abs(m.data[i+6]-ProceduralFighterMesh.PART_SKIN)<.1f)skin++;}
        float span=maxX-minX,length=maxZ-minZ; assertTrue(skin>1800&&span>9.8f&&length>11f&&span/length>.72f&&span/length<1.02f); assertTrue(AirframeShapeProfile.HALF_WIDTH[2]<.22f&&AirframeShapeProfile.HALF_WIDTH[9]>1.10f&&AirframeShapeProfile.WING_ROOT[4][0]>=5f);
    }
    @Test public void avm5UpperFuselageHasContinuousCrownWithoutBalloonBelly(){
        AirframeShapeProfile.validate();float c7=AirframeShapeProfile.upperCrown(7),c8=AirframeShapeProfile.upperCrown(8),c9=AirframeShapeProfile.upperCrown(9),c10=AirframeShapeProfile.upperCrown(10),c11=AirframeShapeProfile.upperCrown(11);
        assertTrue(c7>.75f&&c8>.84f&&c9>.86f&&c10>.80f&&c11>.67f&&c9>c10&&c10>c11); assertTrue(AirframeShapeProfile.lowerBelly(8)>-.70f&&AirframeShapeProfile.lowerBelly(9)>-.70f);
    }
    @Test public void avm5UpperSurfacePanelsAreDenseSymmetricAndAftBlended(){
        ProceduralFighterMesh.Mesh m=ProceduralFighterMesh.build();int count=0,left=0,right=0,centre=0;float minZ=99,maxZ=-99,maxY=-99;
        for(int i=0;i<m.data.length;i+=7){float x=m.data[i],y=m.data[i+1],z=m.data[i+2],p=m.data[i+6];if(Math.abs(p-ProceduralFighterMesh.PART_UPPER_PANEL)<.1f){count++;minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);maxY=Math.max(maxY,y);if(x<-.08f)left++;else if(x>.08f)right++;else centre++;}}
        assertTrue(count>300&&minZ<-1.7f&&maxZ>3f&&maxY>.88f&&centre>30&&left>100&&right>100&&Math.abs(left-right)<45);
    }
    @Test public void avm6ForebodyAndEngineShouldersBlendProgressively(){
        AirframeShapeProfile.validate();float c5=AirframeShapeProfile.upperCrown(5),c6=AirframeShapeProfile.upperCrown(6),c7=AirframeShapeProfile.upperCrown(7);assertTrue(c5<c6&&c6<c7);assertTrue(AirframeShapeProfile.HALF_WIDTH[6]>=.90f&&AirframeShapeProfile.HALF_WIDTH[7]>=1.04f);assertTrue(AirframeShapeProfile.ENGINE_Z.length>=9&&AirframeShapeProfile.ENGINE_R[0]<=.30f);for(int i=1;i<=5;i++)assertTrue(AirframeShapeProfile.ENGINE_R[i]>AirframeShapeProfile.ENGINE_R[i-1]);assertTrue(AirframeShapeProfile.ENGINE_R[5]>=.65f&&AirframeShapeProfile.engineShoulderTop(5)>=.27f&&AirframeShapeProfile.ENGINE_R[8]<AirframeShapeProfile.ENGINE_R[6]);
    }
    @Test public void avm7UpperSkinGuidesCloseTheVisualGaps(){
        AirframeShapeProfile.validate();
        assertTrue("upper blend needs dense longitudinal control",AirframeShapeProfile.UPPER_BLEND_Z.length>=12);
        assertTrue("foredeck must begin ahead of canopy",AirframeShapeProfile.UPPER_BLEND_Z[0]<-2.7f);
        assertTrue("upper blend must reach nozzle shoulder",AirframeShapeProfile.UPPER_BLEND_Z[AirframeShapeProfile.UPPER_BLEND_Z.length-1]>3.1f);
        float max=-99;for(float y:AirframeShapeProfile.UPPER_BLEND_Y)max=Math.max(max,y);assertTrue("canopy shoulders need a strong crown",max>.90f);
        for(int i=1;i<AirframeShapeProfile.ENGINE_VALLEY_Y.length;i++)assertTrue("centre valley must taper continuously",AirframeShapeProfile.ENGINE_VALLEY_Y[i]<AirframeShapeProfile.ENGINE_VALLEY_Y[i-1]);
        assertTrue("valley must terminate into aft boat-tail",AirframeShapeProfile.ENGINE_VALLEY_Z[AirframeShapeProfile.ENGINE_VALLEY_Z.length-1]>3.1f);
    }
}
