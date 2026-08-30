(()=>{'use strict';
const $=(s,r=document)=>r.querySelector(s),$$=(s,r=document)=>[...r.querySelectorAll(s)];
function addStyle(){if($('#trHomeClean251Css'))return;const s=document.createElement('style');s.id='trHomeClean251Css';s.textContent=`
#p2ModeRow,#trCatalogHealthBtn,#trReliabilityBtn,.tr-product-more{display:none!important}
.modes.tr-product-collapsed .mode:nth-child(n+5){display:block!important}
.trSettingsTools251{display:grid;gap:8px;margin-top:8px}
.trSettingsTool251{cursor:pointer;user-select:none}
.trSettingsTool251 .num{font-size:18px;font-weight:900}
.trSettingsTool251:active{transform:scale(.992)}
`;document.head.appendChild(s)}
function cleanHome(){
  const row=$('#p2ModeRow');if(row)row.remove();
  $$('.tr-product-more,#trCatalogHealthBtn,#trReliabilityBtn').forEach(x=>x.remove());
  const modes=$('.modes');if(modes)modes.classList.remove('tr-product-collapsed');
}
function tool(id,icon,title,sub,fn){const d=document.createElement('div');d.id=id;d.className='item trSettingsTool251';d.setAttribute('role','button');d.tabIndex=0;d.innerHTML=`<div class="num">${icon}</div><div><div class="itName">${title}</div><div class="itSub">${sub}</div></div><div style="font-size:22px;color:var(--red,#ff3647);padding-right:4px">›</div>`;const go=()=>{const f=window[fn];if(typeof f==='function')f();else window.toast?.('Bu araç henüz hazır değil')};d.onclick=go;d.onkeydown=e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();go()}};return d}
function enhanceSettings(){const sh=$('#sheet'),tt=$('#sheetTitle'),li=$('#list');if(!sh||!tt||!li||!sh.classList.contains('show'))return;if((tt.textContent||'').trim().toLocaleLowerCase('tr')!=='ayarlar')return;if($('#trSettingsCatalog251',li)||$('#trSettingsReliability251',li))return;const wrap=document.createElement('div');wrap.className='trSettingsTools251';wrap.append(tool('trSettingsCatalog251','◫','Katalog Sağlık Durumu','Katalog tarama, doğrulama ve onarım raporu','openCatalogHealth'));wrap.append(tool('trSettingsReliability251','✓','Ürün Güvenilirlik Raporu','Telemetri, dayanıklılık ve kalite kapıları','openReliabilityGate'));li.appendChild(wrap)}
function run(){addStyle();cleanHome();enhanceSettings()}
function mount(){run();document.addEventListener('click',e=>{if(e.target.closest('#settingsBtn,[data-nav="settings"]'))setTimeout(enhanceSettings,0)},false);const mo=new MutationObserver(()=>run());mo.observe(document.body,{childList:true,subtree:true,attributes:true,attributeFilter:['class']});[250,600,1000,1500,2200,3200].forEach(t=>setTimeout(run,t));setTimeout(()=>{try{mo.disconnect()}catch(e){};cleanHome()},6000)}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();
})();