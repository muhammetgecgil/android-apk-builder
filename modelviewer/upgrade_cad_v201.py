from pathlib import Path

p=Path('modelviewer/src/main/assets/cadviewer/cad-v199.js')
s=p.read_text(encoding='utf-8')

old="function drawView(v,x,y,w,h){const r=edgeData(v),pts=[];"
new="""function axisTriad(v,x,y){
 const L=28;let ex=[L,0],ey=[0,-L],ez=[-18,16];
 if(v.k==='top'||v.k==='bottom'){ex=[L,0];ey=[-18,16];ez=[0,-L]}
 else if(v.k==='right'||v.k==='left'){ex=[-18,16];ey=[0,-L];ez=[L,0]}
 else if(v.k==='iso'){ex=[22,11];ey=[0,-27];ez=[-22,11]}
 const ln=(d,cls)=>'<line x1="'+x+'" y1="'+y+'" x2="'+(x+d[0])+'" y2="'+(y+d[1])+'" class="'+cls+'"/>';
 const tx=(d,t)=>'<text x="'+(x+d[0]+5)+'" y="'+(y+d[1]+4)+'" class="axisTxt">'+t+'</text>';
 return '<g class="axisTriad"><circle cx="'+x+'" cy="'+y+'" r="2.8" fill="#111"/>'+ln(ex,'axX')+ln(ey,'axY')+ln(ez,'axZ')+tx(ex,'X')+tx(ey,'Y')+tx(ez,'Z')+'</g>';
}
function drawView(v,x,y,w,h){const r=edgeData(v),pts=[];"""
if old not in s:
    raise SystemExit('drawView patch point missing')
s=s.replace(old,new,1)

old="let s='<g><text x=\"'+(x+w/2)+'\" y=\"'+(y+15)+'\" class=\"ttl\">'+v.name+'</text>';"
new="let s='<g><text x=\"'+(x+w/2)+'\" y=\"'+(y+15)+'\" class=\"ttl\">'+v.name+'</text>'+axisTriad(v,x+36,y+52);"
if old not in s:
    raise SystemExit('view title patch point missing')
s=s.replace(old,new,1)

old=".note{font:14px sans-serif;fill:#222}.border{stroke:#222;stroke-width:1;fill:none}"
new=".note{font:14px sans-serif;fill:#222}.border{stroke:#222;stroke-width:1;fill:none}.axX{stroke:#d62828;stroke-width:2.4}.axY{stroke:#2a9d46;stroke-width:2.4}.axZ{stroke:#1565c0;stroke-width:2.4}.axisTxt{font:700 14px sans-serif;fill:#111;paint-order:stroke;stroke:#fff;stroke-width:3px;stroke-linejoin:round}"
if old not in s:
    raise SystemExit('style patch point missing')
s=s.replace(old,new,1)

old="window.MG_CAD_V199={version:'2.0.0',singleSheetAllViews:true,frontTopRightLeftBackBottomIso:true,hiddenEdgesDashed:true,noMeshTriangles:true,titleBlock:true,viewBasedDimensions:true,isolatedPartDrawing:true,visibleMeshesOnly:true};"
new="window.MG_CAD_V199={version:'2.0.1',singleSheetAllViews:true,frontTopRightLeftBackBottomIso:true,hiddenEdgesDashed:true,noMeshTriangles:true,titleBlock:true,viewBasedDimensions:true,isolatedPartDrawing:true,visibleMeshesOnly:true,xyzAxisTriad:true,axisTriadPerView:true};"
if old not in s:
    raise SystemExit('feature marker patch point missing')
s=s.replace(old,new,1)

p.write_text(s,encoding='utf-8')
print('v2.0.1 technical drawing XYZ axis triads added to every view')
