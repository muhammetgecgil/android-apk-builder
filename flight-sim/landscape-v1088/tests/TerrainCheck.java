import com.mg.fixturecockpitsim.visual.CinematicTerrainMesh;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

public final class TerrainCheck {
    public static void main(String[] args) throws Exception {
        Path output = Paths.get(args[0]); Files.createDirectories(output);
        long start = System.nanoTime();
        for (int kind = 0; kind < 5; kind++) {
            long t = System.nanoTime();
            float[] a = CinematicTerrainMesh.build(kind);
            if (a.length % 21 != 0) throw new AssertionError("triangle alignment");
            float min = Float.POSITIVE_INFINITY, max = -min;
            for (int i = 0; i < a.length; i += 7) {
                for (int j = 0; j < 7; j++) if (!Float.isFinite(a[i+j])) throw new AssertionError("nonfinite geometry");
                min = Math.min(min, a[i+1]); max = Math.max(max, a[i+1]);
                float norm = a[i+3]*a[i+3]+a[i+4]*a[i+4]+a[i+5]*a[i+5];
                if (Math.abs(norm-1f) > .00002f || a[i+4] <= 0) throw new AssertionError("bad normal");
            }
            for (int i = 0; i < a.length; i += 21) {
                float ux=a[i+7]-a[i], uz=a[i+9]-a[i+2], vx=a[i+14]-a[i], vz=a[i+16]-a[i+2];
                if (uz*vx-ux*vz <= 0f) throw new AssertionError("degenerate or reversed triangle");
            }
            if (min < -12 || max > 45 || max-min < 2) throw new AssertionError("height bounds");
            if (!Arrays.equals(a,CinematicTerrainMesh.build(kind))) throw new AssertionError("nondeterminism");
            ByteBuffer bytes=ByteBuffer.allocate(a.length*4).order(ByteOrder.LITTLE_ENDIAN);
            bytes.asFloatBuffer().put(a); Files.write(output.resolve("terrain"+kind+".bin"),bytes.array());
            // Compact indexed WebGL representation of exactly the same vertices.
            LinkedHashMap<String,Integer> map=new LinkedHashMap<>();
            ArrayList<float[]> verts=new ArrayList<>(); int[] indices=new int[a.length/7];
            for(int i=0;i<a.length;i+=7){String key=a[i]+":"+a[i+2];Integer index=map.get(key);
                if(index==null){index=verts.size();map.put(key,index);verts.add(Arrays.copyOfRange(a,i,i+7));}indices[i/7]=index;}
            StringBuilder js=new StringBuilder("window.terrain"+kind+"={v:[");
            for(int i=0;i<verts.size();i++)for(int j=0;j<7;j++){if(i>0||j>0)js.append(',');js.append(verts.get(i)[j]);}
            js.append("],i:").append(Arrays.toString(indices)).append("};");
            Files.write(output.resolve("terrain"+kind+".js"),js.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            System.out.printf(Locale.ROOT,"kind=%d triangles=%d vertices=%d boundsY=%.3f..%.3f bytes=%d build+checkMs=%.1f%n",kind,a.length/21,verts.size(),min,max,a.length*4,(System.nanoTime()-t)/1e6);
        }
        System.out.printf(Locale.ROOT,"All geometry checks passed in %.1f ms%n",(System.nanoTime()-start)/1e6);
    }
}
