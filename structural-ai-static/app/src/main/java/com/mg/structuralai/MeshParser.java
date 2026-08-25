package com.mg.structuralai;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class MeshParser {
    private MeshParser() {}

    public static MeshModel parse(String name, InputStream in) throws IOException {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (n.endsWith(".obj")) return parseObj(in);
        if (n.endsWith(".stl")) return parseAsciiStl(in);
        throw new IOException("Şimdilik OBJ ve ASCII STL destekleniyor.");
    }

    private static MeshModel parseObj(InputStream in) throws IOException {
        MeshModel m = new MeshModel();
        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("v ")) {
                String[] p = line.split("\\s+");
                if (p.length >= 4) m.addVertex(new MeshModel.V3(Double.parseDouble(p[1]),Double.parseDouble(p[2]),Double.parseDouble(p[3])));
            } else if (line.startsWith("f ")) {
                String[] p = line.substring(2).trim().split("\\s+");
                if (p.length >= 3) {
                    int a=index(p[0],m.vertices.size()), b=index(p[1],m.vertices.size());
                    for(int i=2;i<p.length;i++){
                        int c=index(p[i],m.vertices.size());
                        m.triangles.add(new int[]{a,b,c});
                        b=c;
                    }
                }
            }
        }
        if (m.vertices.isEmpty()) throw new IOException("OBJ içinde geometri bulunamadı.");
        return m;
    }

    private static int index(String token, int size){
        String s=token.split("/")[0];
        int i=Integer.parseInt(s);
        return i>0 ? i-1 : size+i;
    }

    private static MeshModel parseAsciiStl(InputStream in) throws IOException {
        MeshModel m = new MeshModel();
        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line; List<Integer> tri = new ArrayList<>(3);
        while((line=br.readLine())!=null){
            line=line.trim();
            if(line.startsWith("vertex ")){
                String[] p=line.split("\\s+");
                MeshModel.V3 v=new MeshModel.V3(Double.parseDouble(p[1]),Double.parseDouble(p[2]),Double.parseDouble(p[3]));
                m.addVertex(v); tri.add(m.vertices.size()-1);
                if(tri.size()==3){ m.triangles.add(new int[]{tri.get(0),tri.get(1),tri.get(2)}); tri.clear(); }
            }
        }
        if(m.vertices.isEmpty()) throw new IOException("STL içinde geometri bulunamadı veya binary STL olabilir.");
        return m;
    }
}
