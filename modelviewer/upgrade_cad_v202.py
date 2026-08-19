from pathlib import Path
p=Path('modelviewer/src/main/assets/cadviewer/cad-v199.js')
s=p.read_text(encoding='utf-8')
old="function drawView(v,x,y,w,h){const r=edgeData(v),pts=[];"
new="""function dimLineH(x1,x2,y,objY,text){const a=7;return '<g class=\"dimgrp\"><line x1=\"'+x1+'\" y1=\"'+objY+'\" x2=\"'+x1+'\" y2=\"'+y+'\" class=\"ext\"/><line x1=\"'+x2+'\" y1=\"'+objY+'\" x2=\"'+x2+'\" y2=\"'+y+'\" class=\"ext\"/><line x1=\"'+x1+'\" y1=\"'+y+'\" x2=\"'+x2+'\" y2=\"'+y+'\" class=\"dimln\"/><path d=\"M '+x1+' '+y+' l '+a+' -4 l 0 8 z\" class=\"arr\"/><path d=\"M '+x2+' '+y+' l -'+a+' -4 l 0 8 z\" class=\"arr\"/><text x=\"'+((x1+x2)/2)+'\" y=\"'+(y-5)+'\" class=\"dimtxt\">'+text+'</text></g>'}
function dimLineV(y1,y2,x,objX,text){const a=7;return '<g class=\"dimgrp\"><line x1=\"'+objX+'\" y1=\"'+y1+'\" x2=\"'+x+'\" y2=\"'+y1+'\" class=\"ext\"/><line x1=\"'+objX+'\" y1=\"'+y2+'\" x2=\"'+x+'\" y2=\"'+y2+'\" class=\"ext\"/><line x1=\"'+x+'\" y1=\"'+y1+'\" x2=\"'+x+'\" y2=\"'+y2+'\" class=\"dimln\"/><path d=\"M '+x+' '+y1+' l -4 '+a+' l 8 0 z\" class=\"arr\"/><path d=\"M '+x+' '+y2+' l -4 -'+a+' l 8 0 z\" class=\"arr\"/><text x=\"'+(x-7)+'\" y=\"'+((y1+y2)/2)+'\" class=\"dimtxt rot\">'+text+'</text></g>'}
function drawView(v,x,y,w,h){const r=edgeData(v),pts=[];"""
if old not in s: raise SystemExit('drawView patch missing')
s=s.replace(old,new,1)
old="s+='</g>';return s}"
new="""const bb2=bbox(),sz2=new THREE.Vector3();if(bb2)bb2.getSize(sz2);const left=ox+mnx*sc,right=ox+mxx*sc,top=oy-mxy*sc,bottom=oy-mny*sc;const off=20;
 if(v.k==='front'||v.k==='back'){s+=dimLineH(left,right,bottom+off,bottom,fmt(sz2.x)+' mm');s+=dimLineV(top,bottom,left-off,left,fmt(sz2.y)+' mm')}
 else if(v.k==='top'||v.k==='bottom'){s+=dimLineH(left,right,bottom+off,bottom,fmt(sz2.x)+' mm');s+=dimLineV(top,bottom,left-off,left,fmt(sz2.z)+' mm')}
 else if(v.k==='right'||v.k==='left'){s+=dimLineH(left,right,bottom+off,bottom,fmt(sz2.z)+' mm');s+=dimLineV(top,bottom,left-off,left,fmt(sz2.y)+' mm')}
 s+='</g>';return s}"""
if old not in s: raise SystemExit('return patch missing')
s=s.replace(old,new,1)
old=".axisTxt{font:700 14px sans-serif;fill:#111;paint-order:stroke;stroke:#fff;stroke-width:3px;stroke-linejoin:round}"
new=".axisTxt{font:700 14px sans-serif;fill:#111;paint-order:stroke;stroke:#fff;stroke-width:3px;stroke-linejoin:round}.dimln,.ext{stroke:#222;stroke-width:1;fill:none}.ext{stroke-width:.8}.arr{fill:#222;stroke:none}.dimtxt{font:14px sans-serif;text-anchor:middle;fill:#111;paint-order:stroke;stroke:#fff;stroke-width:3px}.dimtxt.rot{writing-mode:tb;glyph-orientation-vertical:0}"
if old not in s: raise SystemExit('style patch missing')
s=s.replace(old,new,1)
old="axisTriadPerView:true};"
new="axisTriadPerView:true,isoStyleDimensionLines:true,extensionLines:true,arrowheads:true,viewSpecificDimensionPlacement:true};"
if old not in s: raise SystemExit('marker patch missing')
s=s.replace(old,new,1)
p.write_text(s,encoding='utf-8')
print('v2.0.2 ISO-style dimension and extension lines added')
