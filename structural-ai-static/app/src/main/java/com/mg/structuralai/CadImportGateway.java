package com.mg.structuralai;

import java.io.InputStream;
import java.util.Locale;

/** Routes mesh formats to Java parser and exact CAD formats to the native OCCT kernel. */
public final class CadImportGateway {
    public enum Format { STEP, IGES, BREP, STL, OBJ, GLTF, GLB, PLY, VRML, UNKNOWN }

    public static final class ImportedModel {
        public final MeshModel mesh;
        public final Format format;
        public final boolean exactCadSource;
        public final String assemblySummary;
        public final String metadataSummary;
        ImportedModel(MeshModel m,Format f,boolean exact,String assembly,String metadata){mesh=m;format=f;exactCadSource=exact;assemblySummary=assembly;metadataSummary=metadata;}
    }

    private CadImportGateway(){}

    public static Format detect(String name){
        String n=name==null?"":name.toLowerCase(Locale.US);
        if(n.endsWith(".step")||n.endsWith(".stp"))return Format.STEP;
        if(n.endsWith(".iges")||n.endsWith(".igs"))return Format.IGES;
        if(n.endsWith(".brep"))return Format.BREP;
        if(n.endsWith(".stl"))return Format.STL;
        if(n.endsWith(".obj"))return Format.OBJ;
        if(n.endsWith(".gltf"))return Format.GLTF;
        if(n.endsWith(".glb"))return Format.GLB;
        if(n.endsWith(".ply"))return Format.PLY;
        if(n.endsWith(".wrl")||n.endsWith(".vrml"))return Format.VRML;
        return Format.UNKNOWN;
    }

    public static ImportedModel read(String name,InputStream in){
        Format f=detect(name);
        if(f==Format.STL||f==Format.OBJ){
            MeshModel m=MeshParser.parse(name,in);
            return new ImportedModel(m,f,false,"single tessellated model","mesh format; no authoritative CAD assembly/material metadata");
        }
        if(f==Format.STEP||f==Format.IGES||f==Format.BREP){
            if(!NativeOcctBridge.isAvailable())throw new IllegalStateException("Exact "+f+" import requires the OCCT native CAD kernel; this APK build does not contain it yet.");
            throw new IllegalStateException("OCCT kernel is present but stream-to-native import adapter is not connected yet.");
        }
        throw new IllegalArgumentException("Unsupported CAD format: "+f+". P0: STEP, IGES, BREP, STL, OBJ.");
    }
}
