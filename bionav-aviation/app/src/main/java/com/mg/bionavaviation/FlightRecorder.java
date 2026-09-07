package com.mg.bionavaviation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FlightRecorder {
    public static final class Frame {
        public final long tMs;
        public final double n, e, u, vn, ve, vu, hdg, pitch, roll, baroAlt, relBaro, vs, hSigma, vSigma, hdgSigma;
        public final String integrity;
        Frame(long tMs, FusionEngine f) {
            this.tMs = tMs;
            n=f.northM; e=f.eastM; u=f.upM; vn=f.vn; ve=f.ve; vu=f.vu;
            hdg=f.headingDeg; pitch=f.pitchDeg; roll=f.rollDeg; baroAlt=f.baroAltitudeM;
            relBaro=f.relativeBaroAltitudeM; vs=f.verticalSpeedMps;
            hSigma=f.horizontalSigmaM; vSigma=f.verticalSigmaM; hdgSigma=f.headingSigmaDeg;
            integrity=f.integrity;
        }
    }

    private final List<Frame> frames = new ArrayList<>();
    private boolean recording;
    private long lastRecordMs;
    private static final int MAX_FRAMES = 18000; // ~30 min at 10 Hz

    public void setRecording(boolean enabled) { recording = enabled; }
    public boolean isRecording() { return recording; }
    public int size() { return frames.size(); }
    public void clear() { frames.clear(); lastRecordMs = 0; }

    public void capture(FusionEngine f, long nowMs) {
        if (!recording || nowMs - lastRecordMs < 100) return;
        lastRecordMs = nowMs;
        if (frames.size() >= MAX_FRAMES) frames.remove(0);
        frames.add(new Frame(nowMs, f));
    }

    public String csv() {
        StringBuilder s = new StringBuilder("t_ms,n_m,e_m,u_m,vn_mps,ve_mps,vu_mps,hdg_deg,pitch_deg,roll_deg,baro_m,rel_baro_m,vs_mps,h_sigma_m,v_sigma_m,hdg_sigma_deg,integrity\n");
        for (Frame f : frames) {
            s.append(String.format(Locale.US,
                    "%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%s\n",
                    f.tMs,f.n,f.e,f.u,f.vn,f.ve,f.vu,f.hdg,f.pitch,f.roll,f.baroAlt,f.relBaro,f.vs,f.hSigma,f.vSigma,f.hdgSigma,f.integrity));
        }
        return s.toString();
    }
}
