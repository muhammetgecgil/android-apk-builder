"""Targeted, anchor-checked v108.7 renderer patch. No whole-app regeneration."""
from pathlib import Path
import re, json, shutil

ROOT = Path(__file__).resolve().parent
WORK = ROOT.parent / 'work'
src = WORK / 'smali3/com/mg/fixturecockpitsim/Jet3DView$R.smali'
if not (WORK / 'Jet3DView_R.original.smali').exists():
    shutil.copyfile(src, WORK / 'Jet3DView_R.original.smali')
text = (WORK / 'Jet3DView_R.original.smali').read_text()
old_fs = (WORK / 'classes3.dex-fragment.glsl').read_text()
vs = (WORK / 'classes3.dex-vertex.glsl').read_text()
helpers = (ROOT / 'shaders/terrain_helpers.glsl').read_text()
helpers = re.sub(r'//[^\n]*', '', helpers)
helpers = re.sub(r'\s+', ' ', helpers)
start = old_fs.index('if((vP>59.5&&vP<63.5)')
end = old_fs.index('if(vP>63.5&&vP<64.5)', start)
terrain = 'if((vP>59.5&&vP<63.5)||(vP>64.5&&vP<65.5)){gl_FragColor=vec4(landscapeColor(vPos,vN,vP),1.);return;}'
fs = old_fs[:start] + terrain + old_fs[end:]
fs = fs.replace('void main(){', helpers + 'void main(){', 1)
# High precision avoids the visibly blocky procedural texture calculations on terrain.
fs = fs.replace('precision mediump float;', 'precision highp float;', 1)
fs = fs.replace('varying vec3 vN,vPos;', 'varying highp vec3 vN,vPos;', 1)
new_vs = vs.replace('varying vec3 vN,vPos;', 'varying highp vec3 vN,vPos;', 1)
assert text.count(old_fs) == 2, 'Expected constant field and inlined fragment shader'
assert text.count(vs) == 2, 'Expected constant field and inlined vertex shader'
text = text.replace(old_fs, fs).replace(vs, new_vs)
fov = 'const/high16 v24, 0x41ec0000    # 29.5f'
far = 'const/high16 v29, 0x438c0000    # 280.0f'
assert text.count(fov) == 1 and text.count(far) == 1
text = text.replace(fov, 'const/high16 v24, 0x420a0000    # 34.5f')
text = text.replace(far, 'const/high16 v29, 0x43f00000    # 480.0f')
src.write_text(text)
(ROOT / 'shaders/aircraft_and_terrain.frag').write_text(fs)
(ROOT / 'shaders/aircraft_and_terrain.vert').write_text(new_vs)
shutil.copyfile(WORK / 'terrain-smali/com/mg/fixturecockpitsim/visual/CinematicTerrainMesh.smali',
                WORK / 'smali4/com/mg/fixturecockpitsim/visual/CinematicTerrainMesh.smali')
(WORK / 'preview/shaders.js').write_text('window.VS='+json.dumps(new_vs)+';window.FS='+json.dumps(fs)+';')
print('Renderer patch: terrain shader, highp varyings, FOV 34.5 and far plane 480. Aircraft material branches preserved.')
