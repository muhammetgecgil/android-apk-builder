(()=>{'use strict';
const $=(s,r=document)=>r.querySelector(s);
function addStyle(){if($('#trHomeClean254Css'))return;const s=document.createElement('style');s.id='trHomeClean254Css';s.textContent=`
/* v2.5.4: bottom navigation belongs to page flow, with Android-system safe spacing. */
.profile2-active #p2ModeRow{display:none!important}
#trCatalogHealthBtn,#trReliabilityBtn,.tr-product-more{display:none!important}
.modes.tr-product-collapsed .mode:nth-child(n+5){display:block!important}
.bottom{position:relative!important;left:auto!important;right:auto!important;bottom:auto!important;transform:none!important;width:min(740px,calc(100% - 18px))!important;margin:20px auto calc(58px + env(safe-area-inset-bottom))!important;z-index:20!important}
.app{padding-bottom:18px!important}
.trSettingsTools254{display:grid;gap:8px;margin-top:8px}
.trSettingsTool254{cursor:pointer;user-select:none}
.trSettingsTool254 .num{font-size:18px;font-weight:900}
.trSettingsTool254:active{transform:scale(.992)}
@media(max-width:430px){.bottom{margin-top:18px!important;margin-bottom:calc(60px + env(safe-area-inset-bottom))!important}.app{padding-bottom:14px!important}}
`;document.head.appendChild(s)}
function normalizeModes(){const modes=$('.modes');if(modes)modes.classList.remove('tr-product-collapsed')}
function tool(id,icon,title,sub,fn){const d=document.createElement('div');d.id=id;d.className='item trSettingsTool254';d.setAttribute('role','button');d.tabIndex=0;d.innerHTML=`<div class="num">${icon}</div><div><div class="itName">${title}</div><div class="itSub">${sub}</div></div><div style="font-size:22px;color:var(--red,#ff3647);padding-right:4px">›</div>`;const go=()=>{const f=window[fn];if(typeof f==='function')f();else window.toast?.('Bu araç henüz hazır değil')};d.onclick=go;d.onkeydown=e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();go()}};return d}
function enhanceSettings(){const sh=$('#sheet'),tt=$('#sheetTitle'),li=$('#list');if(!sh||!tt||!li||!sh.classList.contains('show'))return;if((tt.textContent||'').trim().toLocaleLowerCase('tr')!=='ayarlar')return;if($('#trSettingsCatalog254',li)||$('#trSettingsReliability254',li))return;const wrap=document.createElement('div');wrap.className='trSettingsTools254';wrap.append(tool('trSettingsCatalog254','◫','Katalog Sağlık Durumu','Katalog tarama, doğrulama ve onarım raporu','openCatalogHealth'));wrap.append(tool('trSettingsReliability254','✓','Ürün Güvenilirlik Raporu','Telemetri, dayanıklılık ve kalite kapıları','openReliabilityGate'));li.appendChild(wrap)}
function fallbackProfileChoices(){const pm=$('#profileModal'),list=pm?.querySelector('.profile-list');if(!pm||!list||list.querySelector('[data-prof="2"]'))return;const active=localStorage.p2Active==='1';list.innerHTML=`<div class="profile-item ${!active?'active':''}" data-prof="1"><div><strong>Profil 1 • Sade</strong><small>Olgun ana ekran + doğa temaları</small></div><b>${!active?'✓':''}</b></div><div class="profile-item ${active?'active':''}" data-prof="2"><div><strong>Profil 2 • Premium</strong><small>Premium kartlar • DNA • alarm • zamanlayıcı • zapping</small></div><b>${active?'✓':''}</b></div><div class="profile-item locked"><div><strong>Profil 3 • Gelişmiş</strong><small>Henüz eklenmedi</small></div><b>Yakında</b></div>`;list.querySelectorAll('[data-prof]').forEach(x=>x.onclick=()=>{localStorage.p2Active=x.dataset.prof==='2'?'1':'0';pm.classList.remove('show');location.reload()})}
function ensureProfileAccess(){const pill=$('.nature-profile-pill');if(!pill||pill.dataset.p254ProfileFix==='1')return;pill.dataset.p254ProfileFix='1';pill.addEventListener('click',()=>{const pm=$('#profileModal');if(pm)pm.classList.add('show');setTimeout(fallbackProfileChoices,0)},true)}
function refresh(){addStyle();normalizeModes();ensureProfileAccess();fallbackProfileChoices()}
function mount(){refresh();setTimeout(refresh,550);setTimeout(refresh,1500);document.addEventListener('click',e=>{if(e.target.closest('#settingsBtn,[data-nav="settings"]'))setTimeout(enhanceSettings,0);if(e.target.closest('.nature-profile-pill'))setTimeout(fallbackProfileChoices,0)},false)}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();
})();