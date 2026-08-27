package com.mg.structuralai;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Surface import layer. OBJ + ASCII/Binary STL. */
public final class MeshParser {
    private static final int MAX_BYTES=100*1024*1024;
    private MeshParser() {}

    public static MeshModel parse(String name, InputStream in) throws IOException {
        String n=name==null?"":name.toLowerCase(Locale.ROOT);
        if(n.endsWith(".obj")){
            MeshModel m=parseObj(in);
            m.setImportMetadata("OBJ",false,Double.NaN,"OBJ does not define an authoritative physical length unit; scale is inferred only for the 1 N parametric study");
            return m;
        }
        if(n.endsWith(".stl")){
            byte[] data=readBounded(in,MAX_BYTES);
            MeshModel m=looksBinaryStl(data)?parseBinaryStl(data):parseAsciiStl(new ByteArrayInputStream(data));
            m.setImportMetadata("STL",false,Double.NaN,"STL does not define an authoritative physical length unit; scale is inferred only for the 1 N parametric study");
            return m;
        }
        throw new IOException("Desteklenen yüzey formatları: OBJ, ASCII STL, Binary STL. STEP/IGES exact CAD yolu OCCT katmanından açılmalıdır.");
    }

    private static byte[] readBounded(InputStream in,int max) throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buf=new byte[65536]; int total=0,r;
        while((r=in.read(buf))!=-1){ total+=r; if(total>max) throw new IOException("Model 100 MB mobil import limitini aşıyor"); out.write(buf,0,r); }
        return out.toByteArray();
    }

    private static boolean looksBinaryStl(byte[] d){
        if(d.length<84) return false;
        long n=((long)d[80]&255)|(((long)d[81]&255)<<8)|(((long)d[82]&255)<<16)|(((long)d[83]&255)<<24);
        long expected=84L+50L*n;
        return n>0 && expected==d.length;
    }

    private static MeshModel parseBinaryStl(byte[] data) throws IOException{
        ByteBuffer b=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        b.position(80); long count=Integer.toUnsignedLong(b.getInt());
        if(count>2_000_000L) throw new IOException("Binary STL triangle count mobile limitini aşıyor: "+count);
        MeshModel m=new MeshModel();
        for(long f=0;f<count;f++){
            b.getFloat(); b.getFloat(); b.getFloat();
            int[] tri=new int[3];
            for(int i=0;i<3;i++){
                MeshModel.V3 v=new MeshModel.V3(b.getFloat(),b.getFloat(),b.getFloat());
                m.addVertex(v); tri[i]=m.vertices.size()-1;
            }
            b.getShort(); m.triangles.add(tri);
        }
        if(m.triangles.isEmpty()) throw new IOException("Binary STL içinde üçgen bulunamadı");
        return m;
    }

    private static MeshModel parseObj(InputStream in) throws IOException {
        MeshModel m=new MeshModel();
        BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));
        String line;
        try{
            while((line=br.readLine())!=null){
                line=line.trim();
                if(line.startsWith("v ")){
                    String[] p=line.split("\\s+");
                    if(p.length>=4) m.addVertex(new MeshModel.V3(Double.parseDouble(p[1]),Double.parseDouble(p[2]),Double.parseDouble(p[3])));
                }else if(line.startsWith("f ")){
                    String[] p=line.substring(2).trim().split("\\s+");
                    if(p.length>=3){
                        int a=index(p[0],m.vertices.size()), bb=index(p[1],m.vertices.size());
                        for(int i=2;i<p.length;i++){ int c=index(p[i],m.vertices.size()); m.triangles.add(new int[]{a,bb,c}); bb=c; }
                    }
                }
            }
        }catch(NumberFormatException|IndexOutOfBoundsException e){ throw new IOException("OBJ sayısal/yüz indeks verisi bozuk",e); }
        if(m.vertices.isEmpty()||m.triangles.isEmpty()) throw new IOException("OBJ içinde analiz edilebilir üçgen geometri bulunamadı");
        return m;
    }

    private static int index(String token,int size){
        String s=token.split("/")[0]; int i=Integer.parseInt(s); int idx=i>0?i-1:size+i;
        if(idx<0||idx>=size) throw new IndexOutOfBoundsException("OBJ face index"); return idx;
    }

    private static MeshModel parseAsciiStl(InputStream in) throws IOException {
        MeshModel m=new MeshModel();
        BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));
        String line; List<Integer> tri=new ArrayList<>(3);
        try{
            while((line=br.readLine())!=null){
                line=line.trim();
                if(line.startsWith("vertex ")){
                    String[] p=line.split("\\s+");
                    if(p.length<4) continue;
                    MeshModel.V3 v=new MeshModel.V3(Double.parseDouble(p[1]),Double.parseDouble(p[2]),Double.parseDouble(p[3]));
                    m.addVertex(v); tri.add(m.vertices.size()-1);
                    if(tri.size()==3){ m.triangles.add(new int[]{tri.get(0),tri.get(1),tri.get(2)}); tri.clear(); }
                }
            }
        }catch(NumberFormatException e){ throw new IOException("ASCII STL sayısal verisi bozuk",e); }
        if(m.triangles.isEmpty()) throw new IOException("STL içinde analiz edilebilir üçgen geometri bulunamadı");
        return m;
    }
}
