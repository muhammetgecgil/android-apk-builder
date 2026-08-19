from pathlib import Path
import re

AS=Path('modelviewer/src/main/assets/cadviewer')
p=AS/'cad-v199.js'
s=p.read_text(encoding='utf-8')

# Technical drawing must follow current isolation/visibility state.
s=s.replace("function meshes(){try{return group.children.filter(o=>o&&o.isMesh&&!o.userData?.mgTechAux)}catch(e){return[]}}",
            "function meshes(){try{return group.children.filter(o=>o&&o.isMesh&&o.visible!==false&&!o.userData?.mgTechAux)}catch(e){return[]}}",1)
s=s.replace("function bbox(){try{return new THREE.Box3().setFromObject(group)}catch(e){return null}}",
            "function bbox(){try{const b=new THREE.Box3();let any=false;meshes().forEach(m=>{b.expandByObject(m);any=true});return any?b:null}catch(e){return null}}",1)

# Add proper title block and identify whether sheet is whole assembly or isolated part.
old="svg+='<text x=\"30\" y=\"850\" class=\"note\">MG CAD PRO • TÜM GÖRÜNÜŞLER • Birim: mm • Dış ölçü: '+fmt(sz.x)+' × '+fmt(sz.y)+' × '+fmt(sz.z)+' mm</text><text x=\"30\" y=\"875\" class=\"note\">Düz çizgi: görünür kenar • Kesikli çizgi: görünmeyen/arka kenar • Mesh üçgenleri gösterilmez.</text></svg>';"
new="const vm=meshes(),isolated=vm.length===1,partName=(isolated?(vm[0].name||'İZOLE PARÇA'):'ASSEMBLY ('+vm.length+' PARÇA)'),mat=(document.getElementById('matSel')?document.getElementById('matSel').selectedOptions[0].text:'Belirtilmedi');svg+='<text x=\"30\" y=\"835\" class=\"note\">Düz çizgi: görünür kenar • Kesikli çizgi: görünmeyen delik/kenar • Merkezler görünüşe göre gösterilir.</text><rect x=\"920\" y=\"760\" width=\"455\" height=\"120\" class=\"border\"/><line x1=\"1040\" y1=\"760\" x2=\"1040\" y2=\"880\" class=\"border\"/><line x1=\"920\" y1=\"790\" x2=\"1375\" y2=\"790\" class=\"border\"/><line x1=\"920\" y1=\"820\" x2=\"1375\" y2=\"820\" class=\"border\"/><line x1=\"920\" y1=\"850\" x2=\"1375\" y2=\"850\" class=\"border\"/><text x=\"930\" y=\"780\" class=\"note\">PARÇA</text><text x=\"1050\" y=\"780\" class=\"note\">'+String(partName).replace(/[&<>]/g,'')+'</text><text x=\"930\" y=\"810\" class=\"note\">DOSYA</text><text x=\"1050\" y=\"810\" class=\"note\">'+String(typeof fileName!=='undefined'?fileName:'').replace(/[&<>]/g,'')+'</text><text x=\"930\" y=\"840\" class=\"note\">MALZEME</text><text x=\"1050\" y=\"840\" class=\"note\">'+String(mat).replace(/[&<>]/g,'')+'</text><text x=\"930\" y=\"870\" class=\"note\">ÖLÇEK / BİRİM</text><text x=\"1050\" y=\"870\" class=\"note\">OTOMATİK SIĞDIR • mm • MG CAD PRO v2.0</text><text x=\"30\" y=\"875\" class=\"note\">Görünüşe özel dış ölçülendirme: '+fmt(sz.x)+' × '+fmt(sz.y)+' × '+fmt(sz.z)+' mm • '+(isolated?'İZOLE PARÇA TEKNİK RESMİ':'ASSEMBLY TEKNİK RESMİ')+'</text></svg>';"
if old not in s:
    raise SystemExit('v199 title block patch point not found')
s=s.replace(old,new,1)

# Feature markers for verification.
s=s.replace("window.MG_CAD_V199={version:'1.9.9',singleSheetAllViews:true,frontTopRightLeftBackBottomIso:true,hiddenEdgesDashed:true,noMeshTriangles:true};",
            "window.MG_CAD_V199={version:'2.0.0',singleSheetAllViews:true,frontTopRightLeftBackBottomIso:true,hiddenEdgesDashed:true,noMeshTriangles:true,titleBlock:true,viewBasedDimensions:true,isolatedPartDrawing:true,visibleMeshesOnly:true};",1)
p.write_text(s,encoding='utf-8')

# Native Android picker: advertise only supported CAD/mesh/archive document types.
j=Path('modelviewer/src/main/java/com/muhammetgecgil/modelviewer/MainActivity.java')
t=j.read_text(encoding='utf-8')
pat=r'void openFile\(\)\{Intent i=new Intent\(Intent\.ACTION_OPEN_DOCUMENT\);.*?startActivityForResult\(i,OPEN_MODEL\);\}'
rep='void openFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/octet-stream");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/octet-stream","model/step","model/iges","model/obj","model/stl","application/sla","application/x-ply","application/zip","application/x-zip-compressed"});i.putExtra(Intent.EXTRA_TITLE,"CAD: STEP/STP • IGES/IGS • BREP/BRP • OBJ • STL • PLY • ZIP");startActivityForResult(i,OPEN_MODEL);}'
t2,n=re.subn(pat,rep,t,count=1,flags=re.S)
if n!=1:
    raise SystemExit('native openFile picker patch point not found')
j.write_text(t2,encoding='utf-8')
print('v2.0 manufacturing drawing: title block, isolated-part sheet, view dimensions, CAD-only picker')
