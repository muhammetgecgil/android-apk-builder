/* MGtürk Signature: presentation only. Existing nodes retain their event handlers. */
(()=>{'use strict';
const $=s=>document.querySelector(s),A='https://appassets.androidplatform.net/assets/';
const paths={
 heart:'<path d="M20 5c-3-3-6-1-8 1-2-2-5-4-8-1-5 5 8 15 8 15S25 10 20 5z"/>',
 grid:'<rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2"/>',
 genre:'<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="3"/><path d="M12 3v3m9 6h-3m-6 9v-3m-9-6h3"/>',
 star:'<path d="m12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5z"/>',
 zap:'<path d="m13 2-8 12h6l-1 8 9-13h-6z"/>',
 clock:'<circle cx="12" cy="12" r="9"/><path d="M12 6v6l4 2"/>',
 back:'<path d="m8 4-5 5 5 5M3 9h11a6 6 0 1 1 0 12"/>',
 top:'<path d="m3 6 3 13h12l3-13-6 5-3-8-3 8zM7 22h10"/>',
 note:'<path d="M9 17V5l10-2v12"/><ellipse cx="6" cy="17" rx="3" ry="2.5"/><ellipse cx="16" cy="15" rx="3" ry="2.5"/>',
 theme:'<circle cx="12" cy="12" r="9"/><path d="M12 3a9 9 0 0 1 0 18z" fill="currentColor" stroke="none"/>',
 eq:'<path d="M5 3v7m0 4v7m7-18v12m0 4v2m7-18v3m0 4v11M2 10h6m1 5h6m1-9h6"/>',
 radio:'<rect x="3" y="6" width="18" height="15" rx="3"/><path d="m5 6 13-4M7 10h10M7 14h2"/><circle cx="16" cy="16" r="2"/>'
};
const svg=n=>'<svg class="sig-icon" viewBox="0 0 24 24" aria-hidden="true">'+(paths[n]||paths.radio)+'</svg>';
function setText(e,v){if(e&&e.textContent!==v)e.textContent=v}
function attr(e,k,v){if(e&&e.getAttribute(k)!==v)e.setAttribute(k,v)}
function title(id,label,before,p2=false){if(!before)return;let e=$('#'+id);if(!e){e=document.createElement('h2');e.id=id;e.className='sig-section-title'+(p2?' sig-p2-only':'');e.textContent=label}if(e.nextElementSibling!==before)before.before(e)}
function copyCard(el,title,caption,art){
 if(!el)return;
 const old=el.querySelector('.sig-copy');
 if(old&&old.dataset.title===title)return;
 const b=document.createElement('b');b.className='sig-mini-art';b.innerHTML=svg(art);
 const copy=document.createElement('span');copy.className='sig-copy';copy.dataset.title=title;
 const t=document.createElement('strong');t.textContent=title;const c=document.createElement('small');c.textContent=caption;
 copy.append(t,c);el.replaceChildren(b,copy);
}
function mount(){
 document.documentElement.id='trSignature';
 document.body.classList.add('tr-signature');
 const top=$('.top'),hero=$('.hero');if(!top||!hero)return;
 if(!$('.sig-brand')){const brand=document.createElement('div');brand.className='sig-brand';brand.innerHTML='<span>MGtürk<span class="sig-brand-dot">.</span></span><small>RADYO</small>';$('#settingsBtn')?.before(brand)}
 let status=$('.sig-status');if(!status){status=document.createElement('div');status.className='sig-status';hero.prepend(status)}
 for(const el of [$('.live'),$('.nature-profile-pill')])if(el&&el.parentNode!==status)status.appendChild(el);
 const wave=$('.wave');
 if(wave&&!$('.sig-dial-art')){
  const art=document.createElement('div');art.className='sig-dial-art';art.setAttribute('aria-hidden','true');
  art.innerHTML='<svg viewBox="0 0 200 200"><circle class="sig-dial-track" cx="100" cy="100" r="86"/><path class="sig-dial-accent" d="M39.2 160.8a86 86 0 1 1 121.6 0"/><g class="sig-waveform" fill="currentColor">'+[12,24,38,56,34,70,48,30,54,38,20].map((h,i)=>'<rect x="'+(48+i*10)+'" y="'+(100-h/2)+'" width="3" height="'+h+'" rx="1.5"/>').join('')+'</g><circle cx="100" cy="174" r="2" fill="currentColor"/><path class="sig-dial-ticks" d="M100 5v7M5 100h7M188 100h7M33 33l5 5M162 38l5-5"/></svg>';
  wave.appendChild(art);
 }
 attr($('.logo'),'aria-hidden','true');
 const now=$('#now');attr(now,'role','heading');attr(now,'aria-level','1');
 const quick=$('.tr-product-quick');
 setText($('.tr-product-title'),'Kütüphanen');
 if(quick){const m={favorites:['Favoriler','heart'],all:['Radyolar','radio'],genre:['Tür modu','genre'],smart:['Akıllı radyo','star']};quick.querySelectorAll('button').forEach(b=>{const d=m[b.dataset.q];if(d)copyCard(b,d[0],'',d[1])})}
 const modes=$('.modes');title('sigExploreTitle','Keşfet & kişiselleştir',modes);
 if(modes){
  const modeCopy={favorites:['Akıllı zapping','Yeni bir istasyon keşfet','zap'],recent:['Son dinlenenler','Kaldığın yerden devam','clock'],top:['Top 20','Popüler istasyonlar','top'],themes:['Temalar','Sana ait bir atmosfer','theme'],eq:['Eşitleyici','Sesi kendine göre ayarla','eq'],all:['Tüm radyolar','Türkiye kataloğunu aç','grid']};
  modes.querySelectorAll('.mode').forEach(b=>{const d=b.hasAttribute('data-last-radio')?['Son radyo','Önceki istasyona dön','back']:b.hasAttribute('data-nota-ai')?['NOTA AI','Deneysel nota analizi','note']:modeCopy[b.dataset.mode];if(d)copyCard(b,...d)});
 }
 const grid=$('#p2UnifiedPremiumGrid263');title('sigStudioTitle','Dinleme stüdyosu',grid,true);
 const bazCopy={zap:['Zapping','zap'],favorites:['Favoriler','heart'],back:['Önceki radyo','back'],recent:['Son dinlenenler','clock'],tracks:['Son 50','note'],groups:['Türkiye grupları','grid']};
 document.querySelectorAll('#trBazGrid [data-baz]').forEach(b=>{const d=bazCopy[b.dataset.baz];if(!d)return;const icon=b.querySelector('.trBazIcon');if(icon&&!icon.querySelector('.sig-icon'))icon.innerHTML=svg(d[1]);setText(b.querySelector('strong'),d[0]);attr(b,'aria-label',d[0])});
 const picker=$('#profileModal .nature-shell');
 if(picker){
  setText(picker.querySelector('.nature-sub'),'Tema tüm profillerde korunur');
  setText(picker.querySelector('[data-prof="1"] strong'),'Profil 1 • Standart');
  setText(picker.querySelector('[data-prof="1"] small'),'Radyo ve keşif kısayolları');
  setText(picker.querySelector('[data-prof="2"] strong'),'Profil 2 • Tüm özellikler');
  setText(picker.querySelector('[data-prof="2"] small'),'Kompakt dinleme stüdyosu ve gelişmiş araçlar');
  if(!$('#sigThemeShortcut')){const b=document.createElement('button');b.id='sigThemeShortcut';b.dataset.sigThemes='1';b.innerHTML=svg('theme')+'<span>Temalar</span>';picker.appendChild(b)}
 }
 setText($('#natureThemeModal .nature-sub'),'Tüm profiller • 50 renk paleti');
 setText($('#natureThemeModal .nature-foot'),'Seçtiğin tema arka plana, kartlara ve özellik panellerine uygulanır. Profil değiştirsen de aynı tema korunur.');
 const captions={v12Similar:'Dinlediğine benzer',v12Genres:'Türlere göre keşfet',v12Smart:'Yavaşla, dinlemeye devam et',p2DNA:'Bağlantını yakından tanı',p2Tracks:'Şarkı geçmişin',p2Alarm:'Güne radyoyla başla',p2Sleep:'Süreyi sen belirle',p2Genres:'Türkiye’yi tür tür keşfet'};
 Object.entries(captions).forEach(([id,caption])=>{const e=$('#'+id);attr(e?.querySelector('.p263Label'),'data-caption',caption);if(e)attr(e,'aria-label',(e.querySelector('.p263Label')?.textContent||'')+' — '+caption)});
 const tools=$('.radio-tools-v6');
 if(tools){let box=$('#sigTools');if(!box){box=document.createElement('details');box.id='sigTools';box.className='sig-tools';box.innerHTML='<summary><span>Yayın araçları<small>Ses, kalite ve bağlantı</small></span><span class="sig-chevron" aria-hidden="true">⌄</span></summary>';($('.mini')||modes)?.before(box)}if(tools.parentNode!==box)box.appendChild(tools)}
 document.querySelectorAll('.bottom [data-nav]').forEach(b=>{attr(b,'aria-label',({home:'Ana sayfa',discover:'Keşfet',favorites:'Favoriler',settings:'Ayarlar'})[b.dataset.nav]);attr(b,'aria-current',b.classList.contains('active')?'page':'false')});
 decoratePanel();
}
function decoratePanel(){
 const s=$('#sheet');if(s?.classList.contains('show')){
  const search=$('#search');if(search?.style.display!=='none')attr(search,'placeholder','İstasyon veya tür ara…');
  document.querySelectorAll('#list .item[data-i]').forEach(e=>{attr(e,'tabindex','0');attr(e,'role','button');attr(e,'aria-label',(e.querySelector('.itName')?.textContent||'Radyo')+' dinle');attr(e.querySelector('.fav'),'aria-label','Favoriyi değiştir')});
 }
 document.querySelectorAll('.p2Switch').forEach(b=>{attr(b,'role','switch');attr(b,'aria-checked',String(b.classList.contains('on')));attr(b,'aria-label',b.closest('.p2SettingRow')?.querySelector('strong')?.textContent||'Ses ayarı')});
}
let opener=null;
function topOverlay(){return [...document.querySelectorAll('.sheet.show,.nature-modal.show')].at(-1)}
function settleFocus(){const o=topOverlay();if(!o){if(opener?.isConnected){opener.focus({preventScroll:true});opener=null}return}if(!o.contains(document.activeElement)){const close=o.querySelector('#closeSheet,[data-close],[data-pclose]');close?.focus({preventScroll:true})}}
function boot(){
 if(!$('#signatureV290Css')){const css=document.createElement('link');css.id='signatureV290Css';css.rel='stylesheet';css.href=A+'signature-v290.css?v=291';document.head.appendChild(css)}
 [0,400,1300,3000,5500].forEach(ms=>setTimeout(mount,ms));
 window.addEventListener('click',e=>{
  const b=e.target.closest('button,[role="button"]');if(!b)return;
  if(b.id==='sigThemeShortcut')$('#profileModal')?.classList.remove('show');
  if(!topOverlay())opener=b;
  setTimeout(()=>{decoratePanel();settleFocus()},100);
  if(b.closest('[data-prof],.nature-profile-pill,[data-use],#natureReset'))[300,1100,2700].forEach(ms=>setTimeout(mount,ms));
 },true);
 const closeOverlay=window.trCloseTopOverlay;
 if(closeOverlay)window.trCloseTopOverlay=()=>{const closed=closeOverlay();if(closed)requestAnimationFrame(settleFocus);return closed};
 document.addEventListener('keydown',e=>{
  if((e.key==='Enter'||e.key===' ')&&e.target.matches('.item[data-i]')){e.preventDefault();e.target.click();return}
  if(e.key!=='Tab')return;const o=topOverlay();if(!o)return;
  const nodes=[...o.querySelectorAll('button,input,select,textarea,[tabindex="0"]')].filter(n=>!n.disabled&&n.getClientRects().length);
  if(!nodes.length)return;const first=nodes[0],last=nodes.at(-1);
  if(e.shiftKey&&(document.activeElement===first||!o.contains(document.activeElement))){e.preventDefault();last.focus()}else if(!e.shiftKey&&(document.activeElement===last||!o.contains(document.activeElement))){e.preventDefault();first.focus()}
 });
 window.addEventListener('turkradyo-theme-synced',()=>setTimeout(mount,0));
 window.addEventListener('turkradyo-profile-changed',()=>setTimeout(mount,0));
 window.addEventListener('pageshow',()=>setTimeout(mount,0));
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
