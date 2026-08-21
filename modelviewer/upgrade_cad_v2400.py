from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v2400.js' not in h:
    h=h.replace('</body>','<script src="/cad-v2400.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
js=r'''(function(){
'use strict';
function ready(fn){if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',fn,{once:true});else fn();}
function tr(s){return String(s||'').toLocaleUpperCase('tr-TR')}
function f(v){if(!isFinite(v))return '-';var a=Math.abs(v);return a>=1000?v.toFixed(1):a>=100?v.toFixed(2):v.toFixed(3)}
function unit(){try{return document.getElementById('unit')&&document.getElementById('unit').value||'mm'}catch(e){return'mm'}}
function factor(){var u=unit();return u==='cm'?.1:u==='m'?.001:u==='in'?1/25.4:u==='ft'?1/304.8:1}
function val(x){return f(x*factor())+' '+unit()}
var registry=[];
function geometry(){try{return typeof window.MG_CAD_GEOMETRY_ANALYZE==='function'?window.MG_CAD_GEOMETRY_ANALYZE():null}catch(e){return null}}
function buildRegistry(){
 registry=[];var g=geometry();if(!g||!g.ok)return registry;
 (g.pairs||[]).forEach(function(p,i){registry.push({id:'CYL'+(i+1),type:'CYLINDER',diameter:p.diameter,length:p.length,axis:p.a.normal,center:p.a.center,confidence:'MESH-FIT'});});
 (g.circles||[]).forEach(function(c,i){registry.push({id:'ARC'+(i+1),type:'CIRCLE',diameter:c.diameter,radius:c.r,axis:c.normal,center:c.center,confidence:'MESH-FIT'});});
 window.MG_FEATURE_REGISTRY=registry;return registry;
}
function rayHit(ev){try{var c=document.getElementById('c')||document.querySelector('canvas');if(!c||typeof camera==='undefined'||typeof scene==='undefined')return null;var r=c.getBoundingClientRect(),m=new THREE.Vector2(((ev.clientX-r.left)/r.width)*2-1,-((ev.clientY-r.top)/r.height)*2+1),rr=new THREE.Raycaster();rr.setFromCamera(m,camera);var objs=[];scene.traverse(function(o){if(o&&o.isMesh&&o.visible!==false)objs.push(o)});return rr.intersectObjects(objs,false)[0]||null}catch(e){return null}}
function worldNormal(hit){try{var n=hit.face.normal.clone();n.transformDirection(hit.object.matrixWorld).normalize();return n}catch(e){return new THREE.Vector3(0,0,1)}}
function distAxis(p,c,n){var v=p.clone().sub(c),ax=n.clone().normalize(),h=v.dot(ax);return {r:v.clone().addScaledVector(ax,-h).length(),h:h}}
function nearestReg(p){if(!registry.length)buildRegistry();var best=null,score=1e99;registry.forEach(function(x){if(!x.center||!x.axis)return;var d=distAxis(p,x.center,x.axis),target=x.type==='CYLINDER'?x.diameter/2:x.radius,s=Math.abs(d.r-target)+Math.abs(d.h)*.12;if(s<score){score=s;best={feature:x,score:s}}});return best}
function faceLocalStats(hit){
 try{
  var g=hit.object.geometry,p=g.attributes.position,idx=g.index&&g.index.array;if(!p||!hit.face)return null;
  var ids=[hit.face.a,hit.face.b,hit.face.c],pts=ids.map(function(i){var v=new THREE.Vector3(p.getX(i),p.getY(i),p.getZ(i));return hit.object.localToWorld(v)});
  var e=[pts[0].distanceTo(pts[1]),pts[1].distanceTo(pts[2]),pts[2].distanceTo(pts[0])];
  var area=new THREE.Vector3().subVectors(pts[1],pts[0]).cross(new THREE.Vector3().subVectors(pts[2],pts[0])).length()*.5;
  return {edgeMin:Math.min.apply(null,e),edgeMax:Math.max.apply(null,e),area:area};
 }catch(e){return null}
}
function classify(hit){
 var p=hit.point.clone(),n=worldNormal(hit),near=nearestReg(p),out={point:p,normal:n,type:'PLANE',confidence:'LOCAL'};
 if(near&&near.feature){var x=near.feature;if(x.type==='CYLINDER'){out.type='CYLINDER';out.diameter=x.diameter;out.radius=x.diameter/2;out.length=x.length;out.confidence=x.confidence}else{out.type='RADIUS';out.diameter=x.diameter;out.radius=x.radius;out.confidence=x.confidence}}
 var st=faceLocalStats(hit);out.local=st;
 // Chamfer candidate: planar face normal is significantly oblique to principal axes and local triangle is not tiny.
 var ax=Math.max(Math.abs(n.x),Math.abs(n.y),Math.abs(n.z));if(out.type==='PLANE'&&ax<0.985&&st){out.type='CHAMFER_CANDIDATE';var major=Math.acos(Math.min(1,Math.max(0,ax)))*180/Math.PI;out.angle=Math.min(major,90-major);out.widthCandidate=st.edgeMin;out.confidence='MESH-APPROX';}
 return out;
}
function card(c){var s='<b>ÖZELLİK METROLOJİSİ • v2.4</b><br>';
 if(c.type==='CYLINDER')s+='Silindirik özellik: <b>Ø'+val(c.diameter)+'</b> • R'+val(c.radius)+(isFinite(c.length)?' • boy '+val(c.length):'');
 else if(c.type==='RADIUS')s+='Dairesel / fillet adayı: <b>R'+val(c.radius)+'</b> • Ø'+val(c.diameter);
 else if(c.type==='CHAMFER_CANDIDATE')s+='Pah adayı: <b>~'+val(c.widthCandidate)+'</b> • açı ~'+f(c.angle)+'°';
 else s+='Düz yüzey';
 s+='<br>Koordinat: X '+val(c.point.x)+' • Y '+val(c.point.y)+' • Z '+val(c.point.z)+'<br>Güven: '+c.confidence;
 if(c.confidence!=='ANALYTIC-BREP')s+='<br><span style="opacity:.72">Mesh tabanlı sonuç yaklaşık olabilir; STEP/IGES analitik B-Rep bulunduğunda kesin değer önceliklidir.</span>';
 return s}
function show(html){var info=document.getElementById('info');if(!info)return;info.innerHTML=html;info.style.maxHeight='46vh';info.style.overflowY='auto';info.style.touchAction='pan-y'}
function click(ev){var b=document.getElementById('mgFeatureMeasure2400');if(!b||!b.classList.contains('on'))return;var c=document.getElementById('c')||document.querySelector('canvas');if(!c||ev.target!==c)return;var h=rayHit(ev);if(!h)return;ev.preventDefault();ev.stopImmediatePropagation();show(card(classify(h)))}
function toggle(b){b.classList.toggle('on');b.textContent=b.classList.contains('on')?'ÖZELLİK ÖLÇ ✓':'ÖZELLİK ÖLÇ';if(b.classList.contains('on')){buildRegistry();show('<b>ÖZELLİK METROLOJİSİ • v2.4</b><br>Delik/mil silindirik yüzeyine, fillet/radyüs bölgesine veya pah yüzeyine dokun.')} }
function install(){if(document.getElementById('mgFeatureMeasure2400'))return;var buttons=[...document.querySelectorAll('button')],ref=buttons.find(function(b){var t=tr(b.textContent);return t.includes('AKILLI ÖLÇ')||t.includes('ÖLÇ')||t.includes('PROB')});if(!ref)return;var b=document.createElement('button');b.id='mgFeatureMeasure2400';b.textContent='ÖZELLİK ÖLÇ';b.title='Delik/mil Ø, fillet/radyüs R ve pah adayını ölç';b.className=ref.className;b.onclick=function(e){e.preventDefault();e.stopPropagation();toggle(b)};ref.parentNode.insertBefore(b,ref.nextSibling)}
function drawingAnnotations(){buildRegistry();return registry.map(function(x){return x.type==='CYLINDER'?('Ø'+f(x.diameter)):('R'+f(x.radius))})}
function init(){install();document.addEventListener('pointerup',click,true);document.addEventListener('click',function(){setTimeout(install,0)},true);window.MG_CAD_BUILD_FEATURE_REGISTRY=buildRegistry;window.MG_CAD_DRAWING_FEATURE_ANNOTATIONS=drawingAnnotations;window.MG_CAD_V2400={version:'2.4.0',featureRegistry:true,holeShaftDiameter:true,filletRadiusMeasure:true,chamferCandidateMeasure:true,drawingAnnotationSourceShared:true,meshConfidenceLabels:true,noContinuousPolling:true};}
ready(init);
})();'''
(AS/'cad-v2400.js').write_text(js,encoding='utf-8')
print('v2.4.0 feature metrology: diameter + radius/fillet + chamfer candidate + shared drawing annotations')
