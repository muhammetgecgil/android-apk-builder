package com.muhammetgecgil.sesgoruntuharitasi;

/** Minimal feature container used by ProbeAudioEngine. */
public final class AudioEngine {
    public static final class Snapshot {
        public final float level01, peak01, dbfs, tonal01, voice01, low01, high01, impulse01, stereoPan, stereoConfidence;
        public final long timestampNs;
        public final int activeMics, channels;
        public final boolean rawPreferred;
        public final String soundClass;
        public Snapshot(float level01,float peak01,float dbfs,float tonal01,float voice01,float low01,float high01,float impulse01,
                        float stereoPan,float stereoConfidence,long timestampNs,int activeMics,int channels,boolean rawPreferred,String soundClass){
            this.level01=level01;this.peak01=peak01;this.dbfs=dbfs;this.tonal01=tonal01;this.voice01=voice01;this.low01=low01;
            this.high01=high01;this.impulse01=impulse01;this.stereoPan=stereoPan;this.stereoConfidence=stereoConfidence;
            this.timestampNs=timestampNs;this.activeMics=activeMics;this.channels=channels;this.rawPreferred=rawPreferred;this.soundClass=soundClass;
        }
    }
    private AudioEngine(){}
}
