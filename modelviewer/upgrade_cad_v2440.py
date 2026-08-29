from pathlib import Path

AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v2440.js' not in h:
    h=h.replace('</body>','<script src="/cad-v2440.js"></script></body>',1)
html.write_text(h,encoding='utf-8')

js=r'''(function(){
'use strict';
function ready(fn){if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',fn,{once:true});else fn();}
let snapMode='free';
function tr(s){return String(s||'').trim().toLocaleUpperCase('tr-TR')}
function toast(msg){let t=document.getElementById('mgSnapToast2440');if(!t){t=document.createElement('div');t.id='mgSnapToast2440';t.style.cssText='position:fixed;left:50%;top:74px;transform:translateX(-50%);z-index:130;background:rgba(4,18,31,.97);border:1px solid #39b8ff;border-radius:10px;padding:10px 16px;color:#eef9ff;font-weight:800;box-shadow:0 6px 22px #0009;pointer-events:none;transition:.2s';document.body.appendChild(t)}t.textContent=msg;t.style.opacity='1';clearTimeout(t._tm);t._tm=setTimeout(()=>t.style.opacity='0',2000)}
function label(m){return m==='vertex'?'KÖŞE':m==='edge'?'KENAR ORTA':m==='face'?'YÜZ MERKEZ':m==='hole'?'DELİK EKSENİ':'SERBEST NOKTA'}
function activePick(){try{return typeof pivotPick!=='undefined'&&pivotPick}catch(e){return false}}
function beginPick(){try{if(!activePick()&&typeof togglePivotPick==='function')togglePivotPick()}catch(e){} }
function setMode(m){snapMode=m;document.querySelectorAll('.mgSnap2440').forEach(b=>b.classList.toggle('on',b.dataset.mode===m));const info=document.getElementById('pivotInfo');if(info)info.innerHTML='<b>'+label(m)+'</b><br>Model üzerinde uygun geometrinin üzerine dokun.';beginPick();toast(label(m)+' yakalama aktif')}
function installUI(){const pb=document.getElementById('pivotB');if(!pb)return false;let box=document.getElementById('mgPivotSnap2440');if(!box){box=document.createElement('div');box.id='mgPivotSnap2440';box.style.cssText='margin-top:7px;border-top:1px solid #173c60;padding-top:7px';box.innerHTML='<div class="small" style="color:#8bd8ff;font-weight:800;margin-bottom:5px">PİVOT YAKALAMA</div><div class="row" id="mgSnapRow2440"></div><div class="small" id="mgSnapHint2440">Serbest yüzey noktası seçilir.</div>';const host=(document.getElementById('pivotInfo')||pb.parentElement);host.insertAdjacentElement('afterend',box);const row=box.querySelector('#mgSnapRow2440');[['free','SERBEST'],['vertex','KÖŞE'],['edge','KENAR ORTA'],['face','YÜZ MERKEZ'],['hole','DELİK EKSENİ']].forEach(x=>{const b=document.createElement('button');b.type='button';b.className='mgSnap2440';b.dataset.mode=x[0];b.textContent=x[1];b.style.flex='1 1 92px';b.onclick=e=>{e.preventDefault();e.stopPropagation();setMode(x[0])};row.appendChild(b)});}
 box.querySelectorAll('.mgSnap2440').forEach(b=>b.classList.toggle('on',b.dataset.mode===snapMode));return true}
function triWorld(hit){const o=hit.object,g=o.geometry,face=hit.face;if(!g||!g.attributes||!g.attributes.position||!face)return null;const p=g.attributes.position;const a=new THREE.Vector3().fromBufferAttribute(p,face.a),b=new THREE.Vector3().fromBufferAttribute(p,face.b),c=new THREE.Vector3().fromBufferAttribute(p,face.c);o.localToWorld(a);o.localToWorld(b);o.localToWorld(c);return[a,b,c]}
function nearestPoint(arr,p){let best=arr[0],d=Infinity;for(const q of arr){const dd=q.distanceToSquared(p);if(dd<d){d=dd;best=q}}return best.clone()}
function snapVertex(hit){const t=triWorld(hit);return t?nearestPoint(t,hit.point):hit.point.clone()}
function snapEdge(hit){const t=triWorld(hit);if(!t)return hit.point.clone();const mids=[t[0].clone().add(t[1]).multiplyScalar(.5),t[1].clone().add(t[2]).multiplyScalar(.5),t[2].clone().add(t[0]).multiplyScalar(.5)];return nearestPoint(mids,hit.point)}
function snapFace(hit){const t=triWorld(hit);if(!t)return hit.point.clone();return t[0].clone().add(t[1]).add(t[2]).multiplyScalar(1/3)}
function solve3(A,b){const m=[A[0].slice().concat(b[0]),A[1].slice().concat(b[1]),A[2].slice().concat(b[2])];for(let i=0;i<3;i++){let k=i;for(let r=i+1;r<3;r++)if(Math.abs(m[r][i])>Math.abs(m[k][i]))k=r;if(Math.abs(m[k][i])<1e-10)return null;[m[i],m[k]]=[m[k],m[i]];const q=m[i][i];for(let c=i;c<4;c++)m[i][c]/=q;for(let r=0;r<3;r++)if(r!==i){const f=m[r][i];for(let c=i;c<4;c++)m[r][c]-=f*m[i][c]}}return[m[0][3],m[1][3],m[2][3]]}
function holeAxisPoint(hit){
 const o=hit.object,g=o.geometry,pos=g&&g.attributes&&g.attributes.position,nor=g&&g.attributes&&g.attributes.normal;if(!pos||!nor)return null;
 const maxD=Math.max((typeof baseDims!=='undefined'&&baseDims.x)||1,(typeof baseDims!=='undefined'&&baseDims.y)||1,(typeof baseDims!=='undefined'&&baseDims.z)||1,1),R=maxD*.14,R2=R*R;
 const nm=new THREE.Matrix3().getNormalMatrix(o.matrixWorld),pts=[],ns=[];const stride=Math.max(1,Math.floor(pos.count/12000));
 for(let i=0;i<pos.count;i+=stride){const p=new THREE.Vector3().fromBufferAttribute(pos,i);o.localToWorld(p);if(p.distanceToSquared(hit.point)>R2)continue;const n=new THREE.Vector3().fromBufferAttribute(nor,i).applyMatrix3(nm).normalize();pts.push(p);ns.push(n);if(pts.length>1800)break}
 if(pts.length<12)return null;
 let axis=null,best=0;const lim=Math.min(ns.length,70);for(let i=0;i<lim;i+=2)for(let j=i+3;j<lim;j+=3){const c=new THREE.Vector3().crossVectors(ns[i],ns[j]);const l=c.lengthSq();if(l>best){best=l;axis=c.normalize()}}
 if(!axis||best<.03)return null;
 const n0=ns[0];if(Math.abs(axis.dot(n0))>.45)return null;
 let u=new THREE.Vector3(1,0,0);if(Math.abs(u.dot(axis))>.85)u.set(0,1,0);u.addScaledVector(axis,-u.dot(axis)).normalize();const v=new THREE.Vector3().crossVectors(axis,u).normalize();
 let Sx=0,Sy=0,Sxx=0,Syy=0,Sxy=0,Sxxx=0,Syyy=0,Sxyy=0,Sxxy=0,N=0;
 for(const p of pts){const d=p.clone().sub(hit.point),x=d.dot(u),y=d.dot(v);Sx+=x;Sy+=y;Sxx+=x*x;Syy+=y*y;Sxy+=x*y;Sxxx+=x*x*x;Syyy+=y*y*y;Sxyy+=x*y*y;Sxxy+=x*x*y;N++}
 const sol=solve3([[2*Sxx,2*Sxy,2*Sx],[2*Sxy,2*Syy,2*Sy],[2*Sx,2*Sy,2*N]],[Sxxx+Sxyy,Sxxy+Syyy,Sxx+Syy]);if(!sol)return null;
 const cx=sol[0],cy=sol[1];if(!isFinite(cx)||!isFinite(cy)||Math.hypot(cx,cy)>R*1.4)return null;
 return hit.point.clone().addScaledVector(u,cx).addScaledVector(v,cy)
}
function finish(p,mode){try{if(typeof pivotPick!=='undefined')pivotPick=false;controls.enableRotate=true;if(typeof window.setPivot==='function')window.setPivot(p);else if(typeof setPivot==='function')setPivot(p);const info=document.getElementById('pivotInfo');if(info)info.innerHTML='<b>✓ '+label(mode)+' seçildi</b><br>Dönüş merkezi yakalanan geometridedir.';toast('✓ '+label(mode)+' pivot olarak seçildi')}catch(e){console.warn(e)}}
function installCapture(){const c=document.getElementById('c');if(!c||c.dataset.mgSnap2440)return;c.dataset.mgSnap2440='1';c.addEventListener('click',function(ev){if(snapMode==='free'||!activePick())return;let hit=[];try{hit=pointer(ev)}catch(e){}if(!hit||!hit.length)return;ev.preventDefault();ev.stopImmediatePropagation();let p=null;if(snapMode==='vertex')p=snapVertex(hit[0]);else if(snapMode==='edge')p=snapEdge(hit[0]);else if(snapMode==='face')p=snapFace(hit[0]);else if(snapMode==='hole')p=holeAxisPoint(hit[0]);if(!p){toast('Delik ekseni bulunamadı — silindirik iç yüzeye dokun');return}finish(p,snapMode)},true)}
function init(){installUI();installCapture();const mo=new MutationObserver(()=>{installUI();installCapture()});mo.observe(document.body,{childList:true,subtree:true});window.MG_CAD_V2440={version:'2.4.4',pivotSnapVertex:true,pivotSnapEdgeMidpoint:true,pivotSnapFaceCenter:true,pivotHoleAxisFit:true,precisionPivotModes:true};}
ready(init);
})();'''
(AS/'cad-v2440.js').write_text(js,encoding='utf-8')
print('v2.4.4: precision pivot snap - vertex, edge midpoint, face center, fitted hole axis')
