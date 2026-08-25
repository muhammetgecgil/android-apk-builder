package com.mg.fixturecockpitsim.visual;

import android.content.Context;

import com.google.android.filament.Engine;
import com.google.android.filament.EntityManager;
import com.google.android.filament.Scene;
import com.google.android.filament.gltfio.AssetLoader;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.android.filament.gltfio.Gltfio;
import com.google.android.filament.gltfio.ResourceLoader;
import com.google.android.filament.gltfio.UbershaderProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Production GLB loading boundary for v19.1.
 *
 * The legacy OpenGL Jet3DView remains a fallback until fighter_v19.glb passes
 * the art acceptance gate. Once the GLB is present, this class loads it through
 * Filament gltfio, uploads resources, releases source data, and adds all model
 * entities to the supplied scene.
 */
public final class FilamentAircraftLoader implements AutoCloseable {
    static { Gltfio.init(); }

    public static final String DEFAULT_ASSET = "aircraft/fighter_v19.glb";

    private final Context context;
    private final Engine engine;
    private final Scene scene;
    private final UbershaderProvider materialProvider;
    private final AssetLoader assetLoader;
    private final ResourceLoader resourceLoader;
    private FilamentAsset asset;

    public FilamentAircraftLoader(Context context, Engine engine, Scene scene) {
        this.context = context.getApplicationContext();
        this.engine = engine;
        this.scene = scene;
        this.materialProvider = new UbershaderProvider(engine);
        this.assetLoader = new AssetLoader(engine, materialProvider, EntityManager.get());
        this.resourceLoader = new ResourceLoader(engine);
    }

    public FilamentAsset loadDefaultAircraft() throws IOException {
        return loadGlb(DEFAULT_ASSET);
    }

    public FilamentAsset loadGlb(String assetPath) throws IOException {
        unload();
        ByteBuffer data = readAsset(assetPath);
        asset = assetLoader.createAsset(data);
        if (asset == null) {
            throw new IOException("Filament could not parse GLB: " + assetPath);
        }
        resourceLoader.loadResources(asset);
        asset.releaseSourceData();
        scene.addEntities(asset.getEntities());
        return asset;
    }

    public boolean isLoaded() {
        return asset != null;
    }

    public FilamentAsset getAsset() {
        return asset;
    }

    public void unload() {
        if (asset == null) return;
        scene.removeEntities(asset.getEntities());
        assetLoader.destroyAsset(asset);
        asset = null;
    }

    private ByteBuffer readAsset(String path) throws IOException {
        try (InputStream in = context.getAssets().open(path);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[64 * 1024];
            int n;
            while ((n = in.read(chunk)) >= 0) {
                if (n > 0) out.write(chunk, 0, n);
            }
            byte[] bytes = out.toByteArray();
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());
            buffer.put(bytes).flip();
            return buffer;
        }
    }

    @Override public void close() {
        unload();
        resourceLoader.destroy();
        assetLoader.destroy();
        materialProvider.destroyMaterials();
        materialProvider.destroy();
    }
}
