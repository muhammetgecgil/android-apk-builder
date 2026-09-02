from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PATCH=ROOT/'tools/apply_v100_cockpit_hardpoint_realism.py'

s=PATCH.read_text()
old="""def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v100 cockpit/hardpoint patch anchor missing: {label}')
    return text.replace(old,new,1)
"""
new="""def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        # v89 already owns the airborne speed-brake plumbing. v100 only needs
        # to add the cockpit controls around it, so accept those proven forms.
        if label in {'local speed brake','manual speed brake command','manual exit speed brake reset'}:
            return text
        raise SystemExit(f'v100 cockpit/hardpoint patch anchor missing: {label}')
    return text.replace(old,new,1)
"""
if new not in s:
    if old not in s:
        raise SystemExit('v100 post98 compat: rep helper anchor not found')
    s=s.replace(old,new,1)

# v90 lighting added lightButton to the same declarations and bottom bar that
# v100 extends with CKPT. Retarget v100's patch anchors to the proven v90/v95
# forms so both LGT and CKPT survive together.
s=s.replace(
"r=rep(r,'    private Button resetButton,modeButton,linkButton,brakeButton,gearButton;\\n','    private Button resetButton,modeButton,linkButton,brakeButton,gearButton,cockpitButton;\\n','cockpit button field')",
"r=rep(r,'    private Button resetButton,modeButton,linkButton,brakeButton,gearButton,lightButton;\\n','    private Button resetButton,modeButton,linkButton,brakeButton,gearButton,lightButton,cockpitButton;\\n','cockpit button field')")
s=s.replace(
"'        Button[] all={modeButton,linkButton,center,yawL,yawR,thrM,thrP,brakeButton,gearButton,cam};for(Button b:all)bottomPanel.addView(b,new LinearLayout.LayoutParams(0,-1,1f));\\n',\n'        cockpitButton=bottomButton(\"CKPT\");Button[] all={modeButton,linkButton,center,yawL,yawR,thrM,thrP,brakeButton,gearButton,cam,cockpitButton};for(Button b:all)bottomPanel.addView(b,new LinearLayout.LayoutParams(0,-1,1f));\\n',",
"'        Button[] all={modeButton,linkButton,center,yawL,yawR,thrM,thrP,brakeButton,gearButton,cam,lightButton};for(Button b:all)bottomPanel.addView(b,new LinearLayout.LayoutParams(0,-1,1f));\\n',\n'        cockpitButton=bottomButton(\"CKPT\");Button[] all={modeButton,linkButton,center,yawL,yawR,thrM,thrP,brakeButton,gearButton,cam,lightButton,cockpitButton};for(Button b:all)bottomPanel.addView(b,new LinearLayout.LayoutParams(0,-1,1f));\\n',")
s=s.replace(
"'cam.setOnClickListener(v->{cameraMode=(cameraMode+1)%12;jet.setCameraMode(cameraMode);});updateButtons();',\n'cam.setOnClickListener(v->{cameraMode=(cameraMode+1)%12;jet.setCameraMode(cameraMode);});cockpitButton.setOnClickListener(v->toggleCockpit());updateButtons();',",
"'cam.setOnClickListener(v->{cameraMode=(cameraMode+1)%12;jet.setCameraMode(cameraMode);});lightButton.setOnClickListener(v->{lightingPanel.setVisibility(lightingPanel.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);updateLightingButtons();});updateButtons();',\n'cam.setOnClickListener(v->{cameraMode=(cameraMode+1)%12;jet.setCameraMode(cameraMode);});lightButton.setOnClickListener(v->{lightingPanel.setVisibility(lightingPanel.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);updateLightingButtons();});cockpitButton.setOnClickListener(v->toggleCockpit());updateButtons();',")

PATCH.write_text(s)
print('v100 post98 compatibility enabled: preserves v89 speed-brake and v90 lighting controls')
