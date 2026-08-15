from pathlib import Path
import re

root = Path(__file__).resolve().parent
main_p = root / 'app/src/main/java/com/mg/veinassist/MainActivityV29.java'
an_p = root / 'app/src/main/java/com/mg/veinassist/VeinAnalyzerV29.java'
build_p = root / 'app/build.gradle'
manifest_p = root / 'app/src/main/AndroidManifest.xml'

main = main_p.read_text(encoding='utf-8')
an = an_p.read_text(encoding='utf-8')

# 1) Real 1..200 sensitivity range.
main = main.replace('if (sensitivity < 99) sensitivity++;', 'if (sensitivity < 200) sensitivity++;')
an = an.replace('void setSensitivity(int s) { sensitivity = Math.max(1, Math.min(99, s)); }',
                'void setSensitivity(int s) { sensitivity = Math.max(1, Math.min(200, s)); }')

# 2) Sensitivity 100..200 must actually lower the detector threshold.
an = an.replace('float threshold = Math.max(5.9f, 8.75f - sensitivity * 0.026f);',
                'float threshold = Math.max(2.35f, 9.05f - sensitivity * 0.0335f);')

# 3) Critical skin-mask fix: veins are darker/bluer than skin, therefore the centre pixel
# must not itself be required to be skin.  A dense neighbourhood inside the largest skin
# component defines the limb interior, filling narrow vascular corridors while excluding background.
an = an.replace('innerSkin[i] = borderSafe && skin[i] && skinDensity[i] >= 0.955f;',
                'innerSkin[i] = borderSafe && skinDensity[i] >= 0.68f;')

# 4) More permissive directional evidence; final long-component filtering remains conservative.
an = an.replace('if (!innerSkin[i] || base < threshold * 0.30f) continue;',
                'if (!innerSkin[i] || base < threshold * 0.20f) continue;')
an = an.replace('if (raw[i1] > threshold * 0.24f) lineStrong++;',
                'if (raw[i1] > threshold * 0.17f) lineStrong++;')
an = an.replace('if (raw[i2] > threshold * 0.24f) lineStrong++;',
                'if (raw[i2] > threshold * 0.17f) lineStrong++;')
an = an.replace('if (lineSkin < 7 || lineStrong < 5) continue;',
                'if (lineSkin < 6 || lineStrong < 4) continue;')
an = an.replace('float score = base * 0.56f + lineMean * 0.34f + across * 0.22f;',
                'float score = base * 0.50f + lineMean * 0.40f + across * 0.24f;')

# 5) Let weak but persistent vein segments join strong seeds.
an = an.replace('if (s > threshold * 0.68f) persistence[i] = (byte)Math.min(12, persistence[i] + 1);',
                'if (s > threshold * 0.50f) persistence[i] = (byte)Math.min(12, persistence[i] + 1);')
an = an.replace('strong[i] = temporal[i] > threshold * 1.08f && persistence[i] >= 2;',
                'strong[i] = temporal[i] > threshold * 0.92f && persistence[i] >= 2;')
an = an.replace('weak[i] = temporal[i] > threshold * 0.66f && persistence[i] >= 2;',
                'weak[i] = temporal[i] > threshold * 0.46f && persistence[i] >= 2;')

# 6) Replace 1-2px gap bridge with directional 2-7px continuity bridge.
an = an.replace('bridgeShortGaps();', 'bridgeDirectionalGaps(threshold);')
start = an.index('    /** Fill only short 1-2 px gaps with vessel evidence on opposite sides. */')
end = an.index('    private int filterContinuousComponents', start)
bridge_method = r'''    /**
     * Connects short interruptions along the same vessel direction.  The gap must stay
     * inside the learned limb/skin region and contain at least weak vessel evidence.
     * This is deliberately directional so isolated spots are not simply dilated together.
     */
    private void bridgeDirectionalGaps(float threshold) {
        System.arraycopy(candidate, 0, bridge, 0, N);

        for (int y = 8; y < H - 8; y++) {
            for (int x = 8; x < W - 8; x++) {
                int p = y * W + x;
                if (!candidate[p] || !innerSkin[p]) continue;
                int d0 = bestDir[p] & 0xff;

                for (int dk = -1; dk <= 1; dk++) {
                    int k = (d0 + dk + 8) % 8;
                    int dx = sign(offX[k][0]);
                    int dy = sign(offY[k][0]);
                    if (dx == 0 && dy == 0) continue;

                    for (int signDir : new int[]{-1, 1}) {
                        int ex = dx * signDir;
                        int ey = dy * signDir;
                        int hitDist = 0;
                        int hit = -1;
                        float evidence = 0f;
                        int evidenceN = 0;
                        boolean allSkin = true;

                        for (int dist = 2; dist <= 7; dist++) {
                            int nx = x + ex * dist;
                            int ny = y + ey * dist;
                            int q = ny * W + nx;
                            if (!innerSkin[q]) { allSkin = false; break; }
                            evidence += Math.max(raw[q], continuous[q] * 0.55f);
                            evidenceN++;
                            if (candidate[q] && dirDiff(k, bestDir[q] & 0xff) <= 2) {
                                hit = q;
                                hitDist = dist;
                                break;
                            }
                        }

                        if (!allSkin || hit < 0 || hitDist < 3) continue;
                        float meanEvidence = evidence / Math.max(1, evidenceN);
                        if (meanEvidence < threshold * 0.13f) continue;

                        float endScore = Math.min(temporal[p], temporal[hit]);
                        for (int dist = 1; dist < hitDist; dist++) {
                            int nx = x + ex * dist;
                            int ny = y + ey * dist;
                            int q = ny * W + nx;
                            if (!innerSkin[q]) break;
                            bridge[q] = true;
                            bestDir[q] = (byte)k;
                            temporal[q] = Math.max(temporal[q], endScore * 0.70f);
                        }
                    }
                }
            }
        }
        System.arraycopy(bridge, 0, candidate, 0, N);
    }

'''
an = an[:start] + bridge_method + an[end:]

# 7) Component filtering now favours long continuous/curved vessels and scales with threshold.
an = an.replace('boolean longEnough = span >= 13;', 'boolean longEnough = span >= 11;')
an = an.replace('boolean sparseCurve = span >= 22 && fill <= 0.58f;',
                'boolean sparseCurve = span >= 18 && fill <= 0.68f;')
an = an.replace('boolean elongated = elongation >= 1.55f && fill <= 0.76f;',
                'boolean elongated = elongation >= 1.35f && fill <= 0.80f;')
an = an.replace('boolean widthOk = width <= 16.0f;', 'boolean widthOk = width <= 18.0f;')
an = an.replace('boolean scoreOk = meanScore >= 4.8f;',
                'boolean scoreOk = meanScore >= Math.max(2.15f, thresholdForComponent());')

# Insert helper before thickenKeptOnePixel.
needle = '    /** Adds one-pixel cross-vessel thickness, but only inside the learned skin. */'
helper = '''    private float thresholdForComponent() {\n        float t = Math.max(2.35f, 9.05f - sensitivity * 0.0335f);\n        return t * 0.52f;\n    }\n\n'''
an = an.replace(needle, helper + needle)

# 8) Slightly stronger but still weak anatomy direction prior; never creates a line by itself.
an = an.replace('if (deg <= 24f) return 1.24f;', 'if (deg <= 24f) return 1.34f;')
an = an.replace('if (deg <= 45f) return 1.08f;', 'if (deg <= 45f) return 1.12f;')
an = an.replace('if (deg >= 70f) return 0.70f;', 'if (deg >= 70f) return 0.62f;')

# 9) Full-screen camera behind controls. Header/buttons preserve the same palette but become translucent.
build_ui_start = main.index('    private void buildUi() {')
build_ui_end = main.index('    private void updateSensitivity() {', build_ui_start)
new_build_ui = r'''    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        imageFrame = new FrameLayout(this);
        imageFrame.setBackgroundColor(Color.BLACK);
        textureView = new TextureView(this);
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        imageFrame.addView(textureView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        effectView = new ImageView(this);
        effectView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        effectView.setVisibility(View.GONE);
        imageFrame.addView(effectView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        overlayView = new OverlayView();
        imageFrame.addView(overlayView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(imageFrame, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(9), dp(8), dp(8), dp(7));
        header.setBackgroundColor(Color.argb(178, 2, 8, 13));
        header.addView(text("MG VEINASSIST CLINICAL RESEARCH", CYAN, 16, true));
        statusText = text("BOLGE AUTO   GORUNTU --   CIZGI --   TEN --", Color.rgb(200, 213, 224), 10, false);
        header.addView(statusText);
        modeLabel = text("KLINISYEN DOGRULAMASI GEREKLI", WARN, 10, false);
        header.addView(modeLabel);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(128), Gravity.TOP);
        root.addView(header, hp);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(dp(7), dp(3), dp(7), dp(3));
        controls.setBackgroundColor(Color.TRANSPARENT);

        liveButton = button("CANLI");
        atlasButton = button("ATLAS");
        maskButton = button("MASKE");
        controls.addView(row(liveButton, atlasButton, maskButton));

        Button minus = button("-");
        sensitivityButton = button("HASSASIYET " + sensitivity);
        Button plus = button("+");
        controls.addView(row(minus, sensitivityButton, plus));

        Button regionMinus = button("BOLGE -");
        autoButton = button("AUTO");
        Button regionPlus = button("BOLGE +");
        controls.addView(row(regionMinus, autoButton, regionPlus));

        freezeButton = button("DONDUR");
        thermalButton = button("TERMAL MOD");
        controls.addView(row(freezeButton, thermalButton));

        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(224), Gravity.BOTTOM);
        cp.bottomMargin = dp(28);
        root.addView(controls, cp);

        TextView footer = text("EGITIM VE ARASTIRMA - TIBBI CIHAZ DEGIL", WARN, 9, false);
        footer.setGravity(Gravity.CENTER);
        footer.setBackgroundColor(Color.argb(72, 0, 0, 0));
        root.addView(footer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(28), Gravity.BOTTOM));

        setContentView(root);

        minus.setOnClickListener(v -> {
            if (sensitivity > 1) sensitivity--;
            updateSensitivity();
        });
        plus.setOnClickListener(v -> {
            if (sensitivity < 200) sensitivity++;
            updateSensitivity();
        });

        liveButton.setOnClickListener(v -> setMode(VeinAnalyzerV29.MODE_LIVE, false));
        atlasButton.setOnClickListener(v -> setMode(VeinAnalyzerV29.MODE_LIVE, true));
        maskButton.setOnClickListener(v -> setMode(VeinAnalyzerV29.MODE_MASK, atlasPrior));
        thermalButton.setOnClickListener(v -> setMode(VeinAnalyzerV29.MODE_THERMAL, false));

        regionMinus.setOnClickListener(v -> {
            regionIndex = (regionIndex - 1 + regions.length) % regions.length;
            region = regions[regionIndex];
            autoButton.setText(region);
            analyzer.setRegion(region);
            updateHeader(null);
        });
        regionPlus.setOnClickListener(v -> {
            regionIndex = (regionIndex + 1) % regions.length;
            region = regions[regionIndex];
            autoButton.setText(region);
            analyzer.setRegion(region);
            updateHeader(null);
        });
        autoButton.setOnClickListener(v -> {
            regionIndex = 0;
            region = "AUTO";
            autoButton.setText("AUTO");
            analyzer.setRegion(region);
            updateHeader(null);
        });
        freezeButton.setOnClickListener(v -> toggleFreeze());

        scaleDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override public boolean onScale(ScaleGestureDetector detector) {
                        float next = zoomRatio * detector.getScaleFactor();
                        setZoom(Math.max(1.0f, Math.min(maxZoomRatio, next)));
                        return true;
                    }
                });
        gestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onSingleTapUp(MotionEvent e) {
                        triggerSmartFocus(e.getX(), e.getY());
                        return true;
                    }
                });
        imageFrame.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);
            return true;
        });
        updateButtonStates();
    }

'''
main = main[:build_ui_start] + new_build_ui + main[build_ui_end:]

# Semi-transparent buttons and selected state: gaps show the live camera.
main = main.replace('b.setBackground(makeBackground(PANEL, BORDER, 1));',
                    'b.setBackground(makeBackground(Color.argb(92, 5, 16, 26), BORDER, 1));')
main = main.replace('b.setBackground(makeBackground(selected ? CYAN_DARK : PANEL,\n                selected ? CYAN : BORDER, selected ? 2 : 1));',
                    'b.setBackground(makeBackground(selected ? Color.argb(155, 0, 88, 94) : Color.argb(92, 5, 16, 26),\n                selected ? CYAN : BORDER, selected ? 2 : 1));')

main_p.write_text(main, encoding='utf-8')
an_p.write_text(an, encoding='utf-8')

build = build_p.read_text(encoding='utf-8')
build = build.replace("applicationId 'com.mg.veinassist.stable30'", "applicationId 'com.mg.veinassist.stable31'")
build = build.replace('versionCode 30', 'versionCode 31')
build = build.replace("versionName '3.0-skin-continuity-anatomy'", "versionName '3.1-continuity-200-transparent'")
build_p.write_text(build, encoding='utf-8')

manifest = manifest_p.read_text(encoding='utf-8')
manifest = manifest.replace('MG VeinAssist v3.0 Stable', 'MG VeinAssist v3.1 Continuity')
manifest_p.write_text(manifest, encoding='utf-8')

print('MG VeinAssist v3.1 patch applied')
