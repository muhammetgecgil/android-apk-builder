from pathlib import Path
import runpy

p=Path(__file__).with_name('apply_v96_surface_material_realism_v2.py')
s=p.read_text()
a=s.find("if 'vP>49.5&&vP<50.5' not in j:")
b=s.find('JET.write_text(j)',a)
if a<0 or b<a: raise SystemExit('v96 v3: v2 shader block not found')
block=r'''if 'vP>49.5&&vP<50.5' not in j:
    marker='                "else if(vP>21.5&&vP<22.5){'
    i=j.find(marker)
    require(i>=0,'new material shader insertion')
    newcases='                "else if(vP>49.5&&vP<50.5){float grain=(hash(floor(vPos*34.))-0.5)*.018;base=vec3(.205,.216,.224)+vec3(grain);rough=.70;metal=.005;ao=.97;}else if(vP>50.5&&vP<51.5){base=vec3(.072,.079,.086);rough=.64;metal=.018;ao=.91;}else if(vP>51.5&&vP<52.5){base=vec3(.105,.114,.122);rough=.58;metal=.08;ao=.88;}else if(vP>52.5&&vP<53.5){base=vec3(.175,.185,.195);rough=.30;metal=.84;ao=.92;}"+\n'
    j=j[:i]+newcases+j[i:]
'''
p.write_text(s[:a]+block+s[b:])
runpy.run_path(str(p),run_name='__main__')
