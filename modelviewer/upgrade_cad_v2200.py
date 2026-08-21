from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v2200.js' not in h:
    h=h.replace('</body>','<script src="/cad-v2200.js"></script></body>',1)
html.write_text(h,encoding='utf-8')

js=r'''(function(){
'use strict';
function ready(fn){if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',fn,{once:true});else fn();}
function tr(s){return String(s||'').toLocaleUpperCase('tr-TR')}
function fmt(x){if(!isFinite(x))return '-';var a=Math.abs(x);return a>=1000?x.toFixed(1):a>=100?x.toFixed(2):x.toFixed(3)}
function vecKey(x,y,z,t){return Math.round(x/t)+','+Math.round(y/t)+','+Math.round(z/t)}
function edgeKey(a,b){return a<b?a+'|'+b:b+'|'+a}
function getMeshes(){
 var arr=[];
 try{if(typeof scene!=='undefined'&&scene&&scene.traverse)scene.traverse(function(o){if(o&&o.isMesh&&o.geometry&&o.visible!==false)arr.push(o)});}catch(e){}
 return arr;
}
function worldPos(o,x,y,z){var v=new THREE.Vector3(x,y,z);try{o.localToWorld(v)}catch(e){}return v}
function collectGeometry(){
 var meshes=getMeshes(), verts=[], tris=[], bbox=new THREE.Box3();
 meshes.forEach(function(o){
   var g=o.geometry;if(!g)return;var p=g.attributes&&g.attributes.position;if(!p)return;
   var idx=g.index&&g.index.array;
   var base=verts.length;
   for(var i=0;i<p.count;i++){
     var v=worldPos(o,p.getX(i),p.getY(i),p.getZ(i));verts.push(v);bbox.expandByPoint(v);
   }
   if(idx){for(var k=0;k+2<idx.length;k+=3)tris.push([base+idx[k],base+idx[k+1],base+idx[k+2]])}
   else{for(var q=0;q+2<p.count;q+=3)tris.push([base+q,base+q+1,base+q+2])}
 });
 return {meshes:meshes,verts:verts,tris:tris,bbox:bbox};
}
function analyzePlanar(data,diag){
 var groups=new Map(), curved=0, areaTotal=0, areaPlanar=0, tolN=.015, tolD=Math.max(diag*0.001,1e-6);
 data.tris.forEach(function(t){
   var a=data.verts[t[0]],b=data.verts[t[1]],c=data.verts[t[2]];if(!a||!b||!c)return;
   var ab=new THREE.Vector3().subVectors(b,a),ac=new THREE.Vector3().subVectors(c,a),n=new THREE.Vector3().crossVectors(ab,ac);
   var len=n.length();if(len<1e-12)return;var area=.5*len;areaTotal+=area;n.multiplyScalar(1/len);
   // orientation-independent normal
   if(n.x<-.0001||(Math.abs(n.x)<.0001&&n.y<-.0001)||(Math.abs(n.x)<.0001&&Math.abs(n.y)<.0001&&n.z<0))n.multiplyScalar(-1);
   var d=n.dot(a), key=Math.round(n.x/tolN)+','+Math.round(n.y/tolN)+','+Math.round(n.z/tolN)+','+Math.round(d/tolD);
   var g=groups.get(key);if(!g){g={count:0,area:0,n:n.clone(),d:d};groups.set(key,g)}g.count++;g.area+=area;
 });
 var list=[...groups.values()].filter(function(g){return g.count>=2&&g.area>areaTotal*0.002});
 list.sort(function(a,b){return b.area-a.area});
 list.forEach(function(g){areaPlanar+=g.area});
 curved=Math.max(0,areaTotal-areaPlanar);
 return {planes:list.length,majorPlanes:list.slice(0,12),areaTotal:areaTotal,areaPlanar:areaPlanar,areaCurved:curved};
}
function boundaryLoops(data,diag){
 var qtol=Math.max(diag*1e-5,1e-7), rep=new Map(), ids=[];
 data.verts.forEach(function(v,i){var k=vecKey(v.x,v.y,v.z,qtol);if(!rep.has(k))rep.set(k,rep.size);ids[i]=rep.get(k)});
 var edgeCount=new Map(), edgeEnds=new Map();
 data.tris.forEach(function(t){var a=ids[t[0]],b=ids[t[1]],c=ids[t[2]];[[a,b],[b,c],[c,a]].forEach(function(e){if(e[0]===e[1])return;var k=edgeKey(e[0],e[1]);edgeCount.set(k,(edgeCount.get(k)||0)+1);edgeEnds.set(k,e)})});
 var adj=new Map();
 edgeCount.forEach(function(c,k){if(c!==1)return;var e=edgeEnds.get(k);if(!adj.has(e[0]))adj.set(e[0],[]);if(!adj.has(e[1]))adj.set(e[1],[]);adj.get(e[0]).push(e[1]);adj.get(e[1]).push(e[0])});
 var idToV=[];rep.forEach(function(id,key){var s=key.split(',');idToV[id]=new THREE.Vector3(+s[0]*qtol,+s[1]*qtol,+s[2]*qtol)});
 var visited=new Set(), loops=[];
 adj.forEach(function(ns,start){ns.forEach(function(nxt){var ek=edgeKey(start,nxt);if(visited.has(ek))return;var loop=[start],prev=-1,cur=start,next=nxt,guard=0;while(guard++<5000){var k=edgeKey(cur,next);if(visited.has(k))break;visited.add(k);prev=cur;cur=next;loop.push(cur);if(cur===start)break;var cand=(adj.get(cur)||[]).filter(function(x){return x!==prev&&!visited.has(edgeKey(cur,x))});if(!cand.length)break;next=cand[0]}if(loop.length>=7&&loop[loop.length-1]===start)loops.push(loop.slice(0,-1))})});
 return {loops:loops,idToV:idToV};
}
function fitCircle(loop,idToV,diag){
 var pts=loop.map(function(i){return idToV[i]}).filter(Boolean);if(pts.length<6)return null;
 var c=new THREE.Vector3();pts.forEach(function(p){c.add(p)});c.multiplyScalar(1/pts.length);
 var n=new THREE.Vector3();for(var i=0;i<pts.length;i++){var a=new THREE.Vector3().subVectors(pts[i],c),b=new THREE.Vector3().subVectors(pts[(i+1)%pts.length],c);n.add(new THREE.Vector3().crossVectors(a,b))}if(n.length()<1e-10)return null;n.normalize();
 var rs=[],maxPlane=0;pts.forEach(function(p){var d=new THREE.Vector3().subVectors(p,c);var h=Math.abs(d.dot(n));maxPlane=Math.max(maxPlane,h);var proj=d.clone().addScaledVector(n,-d.dot(n));rs.push(proj.length())});
 var r=rs.reduce(function(a,b){return a+b},0)/rs.length;if(r<diag*0.0005)return null;var varr=rs.reduce(function(a,x){return a+(x-r)*(x-r)},0)/rs.length;var cv=Math.sqrt(varr)/Math.max(r,1e-12);if(cv>0.035||maxPlane>Math.max(diag*0.002,r*0.04))return null;
 return {center:c,normal:n,r:r,diameter:2*r,points:pts.length,cv:cv};
}
function detectCircular(data,diag){
 var bl=boundaryLoops(data,diag), circles=[];bl.loops.forEach(function(l){var c=fitCircle(l,bl.idToV,diag);if(c)circles.push(c)});circles.sort(function(a,b){return b.diameter-a.diameter});
 var paired=[],used=new Set();
 for(var i=0;i<circles.length;i++)for(var j=i+1;j<circles.length;j++){if(used.has(i)||used.has(j))continue;var a=circles[i],b=circles[j],rd=Math.abs(a.r-b.r)/Math.max(a.r,b.r);if(rd>.03)continue;var nd=Math.abs(a.normal.dot(b.normal));if(nd<.96)continue;var v=new THREE.Vector3().subVectors(b.center,a.center),ax=Math.abs(v.clone().normalize().dot(a.normal));if(v.length()>diag*.001&&ax>.92){paired.push({diameter:(a.diameter+b.diameter)/2,length:v.length(),a:a,b:b});used.add(i);used.add(j)}}
 return {circles:circles,pairs:paired};
}
function analyze(){
 if(typeof THREE==='undefined')return {ok:false,msg:'3B motor hazır değil'};
 var d=collectGeometry();if(!d.meshes.length||!d.tris.length)return {ok:false,msg:'Önce bir CAD modeli aç'};
 var size=new THREE.Vector3();d.bbox.getSize(size);var diag=size.length()||1;
 var p=analyzePlanar(d,diag),c=detectCircular(d,diag);
 return {ok:true,meshCount:d.meshes.length,triangles:d.tris.length,size:size,diag:diag,planes:p.planes,planarPct:p.areaTotal?100*p.areaPlanar/p.areaTotal:0,curvedPct:p.areaTotal?100*p.areaCurved/p.areaTotal:0,circles:c.circles,pairs:c.pairs};
}
function report(r){
 var info=document.getElementById('info');if(!info){alert(r.ok?'Geometri analizi tamamlandı':r.msg);return}
 if(!r.ok){info.innerHTML='<b>GEOMETRİ AKLI</b><br>'+r.msg;return}
 var holes=r.pairs.slice(0,12), singles=r.circles.filter(function(x){return !holes.some(function(h){return h.a===x||h.b===x})}).slice(0,10);
 var html='<b style="color:#70e6ff">GEOMETRİ AKLI • v2.2</b><br>'+
  'Boyut: X '+fmt(r.size.x)+' • Y '+fmt(r.size.y)+' • Z '+fmt(r.size.z)+'<br>'+
  'Mesh: '+r.meshCount+' • Üçgen: '+r.triangles.toLocaleString('tr-TR')+'<br>'+
  'Düzlemsel yüzey adayları: <b>'+r.planes+'</b> • Düz alan ~%'+r.planarPct.toFixed(1)+' • Eğrisel alan ~%'+r.curvedPct.toFixed(1)+'<br>'+
  'Dairesel kenar adayları: <b>'+r.circles.length+'</b> • Eş eksenli çift: <b>'+r.pairs.length+'</b>';
 if(holes.length){html+='<br><br><b>Olası delik / silindirik geçişler</b>';holes.forEach(function(h,i){html+='<br>'+(i+1)+') Ø'+fmt(h.diameter)+' • boy '+fmt(h.length)})}
 if(singles.length){html+='<br><br><b>Tek dairesel kenarlar</b>';singles.forEach(function(c,i){html+='<br>'+(i+1)+') Ø'+fmt(c.diameter)})}
 html+='<br><span style="opacity:.72">Not: v2.2 geometri tanıma tessellated CAD geometrisinden hesaplanır; hassas B-Rep özellikleri sonraki çekirdek adımında genişletilecektir.</span>';
 info.innerHTML=html;info.style.maxHeight='42vh';info.style.overflowY='auto';info.style.touchAction='pan-y';
}
function installButton(){
 var buttons=[...document.querySelectorAll('button')];var ref=buttons.find(function(b){return tr(b.textContent).trim()==='ANALİZ'});if(!ref||document.getElementById('mgGeometry2200'))return;
 var b=document.createElement('button');b.id='mgGeometry2200';b.textContent='GEOMETRİ';b.title='Düzlem, dairesel kenar, delik/silindir adaylarını analiz et';b.onclick=function(ev){ev.preventDefault();ev.stopPropagation();try{report(analyze())}catch(e){report({ok:false,msg:'Analiz hatası: '+(e.message||e)})}};
 b.className=ref.className;b.style.cssText=ref.style.cssText;ref.parentNode.insertBefore(b,ref.nextSibling);
}
function init(){installButton();document.addEventListener('click',function(e){var t=tr(e.target&&e.target.textContent);if(t.includes('DOSYA AÇ'))setTimeout(installButton,0)},true);window.MG_CAD_GEOMETRY_ANALYZE=analyze;window.MG_CAD_V2200={version:'2.2.0',geometryIntelligence:true,planarRegionRecognition:true,circularEdgeRecognition:true,coaxialPairRecognition:true,noContinuousPolling:true,baseline:'2.0.4'};}
ready(init);
})();'''
(AS/'cad-v2200.js').write_text(js,encoding='utf-8')
print('v2.2.0 geometry intelligence: planar regions + circular edges + coaxial feature candidates')
