from pathlib import Path
import runpy

root = Path(__file__).resolve().parent
runpy.run_path(str(root / 'upgrade_v35.py'), run_name='__main__')

main_p = root / 'app/src/main/java/com/mg/veinassist/MainActivityV29.java'
build_p = root / 'app/build.gradle'
manifest_p = root / 'app/src/main/AndroidManifest.xml'

main = main_p.read_text(encoding='utf-8')

start = main.index('    private void buildUi() {')
end = main.index('    private void updateSensitivity() {', start)

new_ui = '''    private void buildUi() {
        // v3.6: camera is the full-screen background. Header and controls are overlays.
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        imageFrame = new FrameLayout(this);
        imageFrame.setBackgroundColor(Color.rgb(0, 4, 8));

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

        // Transparent top HUD; camera remains visible behind it.
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(10), dp(10), dp(9), dp(8));
        header.setBackgroundColor(Color.argb(118, 2, 8, 13));
        header.addView(text("MG VEINASSIST CLINICAL RESEARCH", CYAN, 16, true));
        statusText = text("BOLGE AUTO   GORUNTU --   CIZGI --   TEN --", Color.rgb(190, 205, 215), 10, false);
        header.addView(statusText);
        modeLabel = text("KLINISYEN DOGRULAMASI GEREKLI", WARN, 10, false);
        header.addView(modeLabel);

        FrameLayout.LayoutParams headerLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(112));
        headerLp.gravity = Gravity.TOP;
        headerLp.setMargins(dp(3), dp(3), dp(3), 0);
        root.addView(header, headerLp);

        // Existing controls stay in the same order, now floating transparently over the camera.
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(dp(7), dp(4), dp(7), dp(4));
        controls.setBackgroundColor(Color.argb(32, 0, 0, 0));

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

        // Raise controls clear of Samsung/Android soft navigation area.
        FrameLayout.LayoutParams controlsLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(220));
        controlsLp.gravity = Gravity.BOTTOM;
        controlsLp.setMargins(dp(2), 0, dp(2), dp(54));
        root.addView(controls, controlsLp);

        TextView footer = text("EGITIM VE ARASTIRMA - TIBBI CIHAZ DEGIL", WARN, 9, false);
        footer.setGravity(Gravity.CENTER);
        footer.setBackgroundColor(Color.argb(95, 0, 0, 0));
        FrameLayout.LayoutParams footerLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(26));
        footerLp.gravity = Gravity.BOTTOM;
        footerLp.setMargins(dp(8), 0, dp(8), dp(24));
        root.addView(footer, footerLp);

        setContentView(root);

        bindSensitivityHold(minus, -1);
        bindSensitivityHold(plus, +1);

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
main = main[:start] + new_ui + main[end:]

# Semi-transparent button fills; keep the exact cyan/navy palette.
main = main.replace(
    'b.setBackground(makeBackground(PANEL, BORDER, 1));',
    'b.setBackground(makeBackground(Color.argb(155, 5, 16, 26), BORDER, 1));',
    1)
main = main.replace(
    'b.setBackground(makeBackground(selected ? CYAN_DARK : PANEL, selected ? CYAN : BORDER, selected ? 2 : 1));',
    'b.setBackground(makeBackground(selected ? Color.argb(205, 0, 88, 94) : Color.argb(155, 5, 16, 26), selected ? CYAN : BORDER, selected ? 2 : 1));',
    1)

main_p.write_text(main, encoding='utf-8')

build = build_p.read_text(encoding='utf-8')
build = build.replace("applicationId 'com.mg.veinassist.continuity35'", "applicationId 'com.mg.veinassist.fullscreen36'", 1)
build = build.replace('versionCode 35', 'versionCode 36', 1)
build = build.replace("versionName '3.5-skin-path-continuity-hold'", "versionName '3.6-fullscreen-overlay'", 1)
build_p.write_text(build, encoding='utf-8')

manifest = manifest_p.read_text(encoding='utf-8')
manifest = manifest.replace('MG VeinAssist v3.5 Skin Path', 'MG VeinAssist v3.6 Fullscreen', 1)
manifest_p.write_text(manifest, encoding='utf-8')

print('MG VeinAssist v3.6 full-screen camera + transparent controls patch applied')
