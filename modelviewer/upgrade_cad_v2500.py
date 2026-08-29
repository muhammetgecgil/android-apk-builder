from pathlib import Path

ROOT=Path('modelviewer/src/main')
AS=ROOT/'assets/cadviewer'
manifest=ROOT/'AndroidManifest.xml'

# Prevent Activity recreation when device orientation/screen geometry changes.
m=manifest.read_text(encoding='utf-8')
for activity in ['.CadViewerActivity','.MainActivity']:
    needle=f'android:name="{activity}"'
    pos=m.find(needle)
    if pos>=0:
        end=m.find('/>',pos)
        if end<0: end=m.find('>',pos)
        seg=m[pos:end]
        if 'android:configChanges=' not in seg:
            m=m[:end]+' android:configChanges="orientation|screenSize|keyboardHidden|smallestScreenSize"'+m[end:]
manifest.write_text(m,encoding='utf-8')

html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v2500.js' not in h:
    h=h.replace('</body>','<script src="/cad-v2500.js"></script></body>',1)
html.write_text(h,encoding='utf-8')

js=r'''(function(){
'use strict';
function ready(fn){if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',fn,{once:true});else fn();}
function toast(msg){let t=document.getElementById('mgToast2500');if(!t){t=document.createElement('div');t.id='mgToast2500';t.style.cssText='position:fixed;left:50%;top:74px;transform:translateX(-50%);z-index:150;background:rgba(4,18,31,.97);border:1px solid #39b8ff;border-radius:10px;padding:10px 16px;color:#eef9ff;font-weight:800;box-shadow:0 6px 22px #0009;pointer-events:none;transition:.2s';document.body.appendChild(t)}t.textContent=msg;t.style.opacity='1';clearTimeout(t._tm);t._tm=setTimeout(()=>t.style.opacity='0',1800)}
function v3(a){return a&&a.length===3&&a.every(Number.isFinite)?new THREE.Vector3(a[0],a[1],a[2]):null}
const key='mgcad:view:'+location.search;
function saveView(){try{if(!window.camera||!window.controls)return;const p=camera.position,t=controls.target,u=camera.up;sessionStorage.setItem(key,JSON.stringify({p:[p.x,p.y,p.z],t:[t.x,t.y,t.z],u:[u.x,u.y,u.z],z:camera.zoom||1,ts:Date.now()}))}catch(e){}}
function restoreView(){try{const x=JSON.parse(sessionStorage.getItem(key)||'null');if(!x)return false;const p=v3(x.p),t=v3(x.t),u=v3(x.u);if(!p||!t||!u)return false;camera.position.copy(p);controls.target.copy(t);camera.up.copy(u);if(Number.isFinite(x.z))camera.zoom=x.z;camera.updateProjectionMatrix();camera.lookAt(t);controls.update();return true}catch(e){return false}}
let saveTimer=0;
function queueSave(){clearTimeout(saveTimer);saveTimer=setTimeout(saveView,180)}

let undo=[],redo=[],internal=false;
function clonePoint(p){return p&&p.isVector3?p.clone():null}
function currentPivot(){try{return controls&&controls.target?controls.target.clone():null}catch(e){return null}}
function installPivotHistory(){if(typeof window.setPivot!=='function'||window.setPivot._mg2500)return false;const oldSet=window.setPivot,oldReset=window.resetPivot;function wrapped(p){if(!internal){const c=currentPivot();if(c){undo.push(c);if(undo.length>30)undo.shift();redo=[];updateHistoryButtons()}}oldSet(p);saveView()}wrapped._mg2500=true;window.setPivot=wrapped;if(typeof oldReset==='function')window.resetPivot=function(){if(!internal){const c=currentPivot();if(c){undo.push(c);if(undo.length>30)undo.shift();redo=[]}}oldReset();updateHistoryButtons();saveView()};return true}
function applyHistory(p){if(!p)return;internal=true;try{window.setPivot(p)}finally{internal=false}updateHistoryButtons();saveView()}
function pivotUndo(){const cur=currentPivot();if(!undo.length){toast('Geri alınacak pivot yok');return}if(cur)redo.push(cur);applyHistory(undo.pop());toast('Pivot geri alındı')}
function pivotRedo(){const cur=currentPivot();if(!redo.length){toast('İleri alınacak pivot yok');return}if(cur)undo.push(cur);applyHistory(redo.pop());toast('Pivot yeniden uygulandı')}
function updateHistoryButtons(){const a=document.getElementById('mgPivotUndo2500'),b=document.getElementById('mgPivotRedo2500');if(a)a.disabled=!undo.length;if(b)b.disabled=!redo.length}
function num(id){const e=document.getElementById(id),n=e?Number(String(e.value).replace(',','.')):NaN;return Number.isFinite(n)?n:null}
function coordinatePivot(){const x=num('mgPX2500'),y=num('mgPY2500'),z=num('mgPZ2500');if(x===null||y===null||z===null){toast('X, Y, Z değerlerini kontrol et');return}if(typeof window.setPivot!=='function'){toast('Pivot motoru hazır değil');return}window.setPivot(new THREE.Vector3(x,y,z));toast('✓ Koordinat pivotu uygulandı')}
function fillCurrent(){const p=currentPivot();if(!p)return;[['mgPX2500',p.x],['mgPY2500',p.y],['mgPZ2500',p.z]].forEach(([id,v])=>{const e=document.getElementById(id);if(e)e.value=Number(v).toFixed(3)})}
function installAdvancedUI(){const pb=document.getElementById('pivotB');if(!pb)return false;if(document.getElementById('mgPivotAdvanced2500'))return true;const host=document.getElementById('mgPivotSnap2440')||document.getElementById('pivotInfo')||pb.parentElement;const box=document.createElement('div');box.id='mgPivotAdvanced2500';box.style.cssText='margin-top:8px;border-top:1px solid #173c60;padding-top:8px';box.innerHTML='<div class="small" style="color:#8bd8ff;font-weight:800;margin-bottom:5px">HASSAS PİVOT</div><div class="row"><button id="mgPivotUndo2500">↶ GERİ</button><button id="mgPivotRedo2500">↷ İLERİ</button><button id="mgPivotFill2500">MEVCUT XYZ</button></div><div class="row"><input id="mgPX2500" inputmode="decimal" placeholder="X" style="min-width:0;width:30%"><input id="mgPY2500" inputmode="decimal" placeholder="Y" style="min-width:0;width:30%"><input id="mgPZ2500" inputmode="decimal" placeholder="Z" style="min-width:0;width:30%"></div><div class="row"><button id="mgPivotApply2500">XYZ PİVOT UYGULA</button></div><div class="small">Pivot geçmişi 30 adım tutulur. Ekran yönü değişse de kamera ve model görünümü korunur.</div>';host.insertAdjacentElement('afterend',box);document.getElementById('mgPivotUndo2500').onclick=e=>{e.preventDefault();pivotUndo()};document.getElementById('mgPivotRedo2500').onclick=e=>{e.preventDefault();pivotRedo()};document.getElementById('mgPivotFill2500').onclick=e=>{e.preventDefault();fillCurrent()};document.getElementById('mgPivotApply2500').onclick=e=>{e.preventDefault();coordinatePivot()};updateHistoryButtons();return true}
function restoreWhenReady(){let tries=0;const t=setInterval(()=>{tries++;try{if(window.camera&&window.controls&&window.group&&group.children&&group.children.length){clearInterval(t);if(restoreView())toast('Görünüm geri yüklendi')}}catch(e){}if(tries>60)clearInterval(t)},250)}
function init(){installPivotHistory();installAdvancedUI();const mo=new MutationObserver(()=>{installPivotHistory();installAdvancedUI()});mo.observe(document.body,{childList:true,subtree:true});try{controls.addEventListener('change',queueSave)}catch(e){}window.addEventListener('pagehide',saveView);document.addEventListener('visibilitychange',()=>{if(document.hidden)saveView()});window.addEventListener('orientationchange',()=>{saveView();setTimeout(()=>{try{resize();restoreView()}catch(e){}},250)});window.addEventListener('resize',()=>{queueSave()},{passive:true});restoreWhenReady();window.MG_CAD_V2500={version:'2.5.0',orientationStable:true,cameraStatePersistence:true,pivotUndoRedo:true,pivotCoordinateEntry:true,pivotHistory30:true};}
ready(init);
})();'''
(AS/'cad-v2500.js').write_text(js,encoding='utf-8')
print('v2.5.0: orientation-stable CAD view + persistent camera + pivot undo/redo + XYZ pivot entry')
