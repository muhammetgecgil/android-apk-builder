from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v2300.js' not in h:
    h=h.replace('</body>','<script src="/cad-v2300.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
js=r'''(function(){
'use strict';
function ready(fn){if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',fn,{once:true});else fn();}
function tr(s){return String(s||'').toLocaleUpperCase('tr-TR')}
function f(v){if(!isFinite(v))return '-';var a=Math.abs(v);return a>=1000?v.toFixed(1):a>=100?v.toFixed(2):v.toFixed(3)}
var on=false,last=null,marks=[];
function unit(){try{return document.getElementById('unit')&&document.getElementById('unit').value||'mm'}catch(e){return'mm'}}
function factor(){var u=unit();return u==='cm'?.1:u==='m'?.001:u==='in'?1/25.4:u==='ft'?1/304.8:1}
function val(x){return f(x*factor())+' '+unit()}
function report(html){var info=document.getElementById('info');if(!info)return;info.innerHTML='<b style="color:#70e6ff">AKILLI ÖLÇÜ • v2.3</b><br>'+html;info.style.maxHeight='44vh';info.style.overflowY='auto';info.style.touchAction='pan-y'}
function rayHit(ev){try{var c=document.getElementById('c')||document.querySelector('canvas');if(!c||typeof camera==='undefined'||typeof scene==='undefined')return null;var r=c.getBoundingClientRect(),m=new THREE.Vector2(((ev.clientX-r.left)/r.width)*2-1,-((ev.clientY-r.top)/r.height)*2+1),rr=new THREE.Raycaster();rr.setFromCamera(m,camera);var objs=[];scene.traverse(function(o){if(o&&o.isMesh&&o.visible!==false)objs.push(o)});return rr.intersectObjects(objs,false)[0]||null}catch(e){return null}}
function faceNormal(hit){try{var n=hit.face.normal.clone();n.transformDirection(hit.object.matrixWorld).normalize();return n}catch(e){return new THREE.Vector3(0,0,1)}}
function axisName(n){var a=[Math.abs(n.x),Math.abs(n.y),Math.abs(n.z)],i=a.indexOf(Math.max.apply(null,a));return (i===0?'X':i===1?'Y':'Z')+(i===0?n.x>=0?'+':'-':i===1?n.y>=0?'+':'-':n.z>=0?'+':'-')}
function geom(){try{return typeof window.MG_CAD_GEOMETRY_ANALYZE==='function'?window.MG_CAD_GEOMETRY_ANALYZE():null}catch(e){return null}}
function distPointAxis(p,c,n){var v=p.clone().sub(c),ax=n.clone().normalize(),h=v.dot(ax);return {rad:v.clone().addScaledVector(ax,-h).length(),axial:h}}
function nearestFeature(p){var g=geom();if(!g||!g.ok)return null,best=null,score=1e99;g.pairs.forEach(function(x){var d=distPointAxis(p,x.a.center,x.a.normal),s=Math.abs(d.rad-x.diameter/2);if(s<score){score=s;best={kind:'cyl',diameter:x.diameter,length:x.length,axis:x.a.normal.clone(),score:s}}});g.circles.forEach(function(x){var d=distPointAxis(p,x.center,x.normal),s=Math.abs(d.rad-x.r)+Math.abs(d.axial)*.25;if(s<score){score=s;best={kind:'circle',diameter:x.diameter,radius:x.r,axis:x.normal.clone(),score:s}}});return best}
function thickness(hit,n){try{var rr=new THREE.Raycaster(),eps=Math.max(1e-4,(typeof bbox!=='undefined'&&bbox)?new THREE.Vector3().subVectors(bbox.max,bbox.min).length()*1e-6:1e-4),o=hit.point.clone().addScaledVector(n,-eps);rr.set(o,n.clone().negate());var ints=rr.intersectObject(hit.object,false).filter(function(x){return x.distance>eps*3});return ints.length?ints[0].distance:null}catch(e){return null}}
function mark(p){try{if(typeof scene==='undefined')return;var g=new THREE.SphereGeometry(.018,10,8),m=new THREE.MeshBasicMaterial({color:0xffd24a}),o=new THREE.Mesh(g,m);o.position.copy(p);scene.add(o);marks.push(o);if(marks.length>4){var q=marks.shift();scene.remove(q)}}catch(e){}}
function one(hit){var p=hit.point.clone(),n=faceNormal(hit),ft=nearestFeature(p),th=thickness(hit,n),s='<b>Seçim</b><br>Koordinat: X '+val(p.x)+' • Y '+val(p.y)+' • Z '+val(p.z)+'<br>Yüzey normali: '+axisName(n)+' ['+f(n.x)+', '+f(n.y)+', '+f(n.z)+']';if(ft){if(ft.kind==='cyl')s+='<br><b>Silindirik özellik:</b> Ø'+val(ft.diameter)+' • boy '+val(ft.length);else s+='<br><b>Dairesel özellik:</b> Ø'+val(ft.diameter)+' / R'+val(ft.radius)}if(th&&isFinite(th))s+='<br><b>Yerel kalınlık adayı:</b> '+val(th);if(last){var d=p.distanceTo(last.p),dv=p.clone().sub(last.p),ang=Math.acos(Math.min(1,Math.max(-1,n.dot(last.n))))*180/Math.PI;s+='<br><br><b>Önceki seçim ile</b><br>Mesafe: '+val(d)+' • ΔX '+val(dv.x)+' • ΔY '+val(dv.y)+' • ΔZ '+val(dv.z)+'<br>Yüzey açısı: '+f(ang)+'°'}last={p:p,n:n};mark(p);report(s)}
function handler(ev){if(!on)return;var c=document.getElementById('c')||document.querySelector('canvas');if(!c||ev.target!==c)return;var h=rayHit(ev);if(!h)return;ev.preventDefault();ev.stopImmediatePropagation();one(h)}
function toggle(b){on=!on;last=null;b.classList.toggle('on',on);b.textContent=on?'AKILLI ÖLÇÜ ✓':'AKILLI ÖLÇÜ';report(on?'Model üzerinde bir yüzeye veya kenara dokun. İkinci dokunuşta mesafe, ΔX/ΔY/ΔZ ve yüzey açısı da hesaplanır.':'Akıllı ölçüm kapatıldı.')}
function install(){if(document.getElementById('mgSmartMeasure2300'))return;var buttons=[...document.querySelectorAll('button')],ref=buttons.find(function(b){var t=tr(b.textContent);return t.includes('ÖLÇ')||t.includes('PROB')||t.includes('GEOMETRİ')});if(!ref)return;var b=document.createElement('button');b.id='mgSmartMeasure2300';b.textContent='AKILLI ÖLÇÜ';b.title='Tek dokunuş çap, radyüs, koordinat, normal ve kalınlık; iki dokunuş mesafe/açı';b.className=ref.className;b.onclick=function(e){e.preventDefault();e.stopPropagation();toggle(b)};ref.parentNode.insertBefore(b,ref.nextSibling)}
function init(){install();document.addEventListener('pointerup',handler,true);document.addEventListener('click',function(){setTimeout(install,0)},true);window.MG_CAD_V2300={version:'2.3.0',smartMeasurement:true,oneTapEngineeringMeasure:true,circleRadiusDiameter:true,cylinderDiameterLength:true,pointCoordinates:true,surfaceNormal:true,twoPointDistance:true,deltaXYZ:true,surfaceAngle:true,thicknessCandidate:true,noContinuousPolling:true};}
ready(init);
})();'''
(AS/'cad-v2300.js').write_text(js,encoding='utf-8')
print('v2.3.0 smart measurement: one-tap engineering measurement + two-point distance/angle')
