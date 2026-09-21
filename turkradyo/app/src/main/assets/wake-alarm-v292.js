(()=>{'use strict';
const $=s=>document.querySelector(s),esc=s=>String(s||'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
function read(){try{const raw=window.RadioNative?.getWakeAlarm?.();const s=JSON.parse(raw||localStorage.trWakeAlarm292||'{}');if(raw&&s.savedAt)localStorage.trWakeAlarm292=raw;return s}catch{return{}}}
function message(s){if(s.error)return s.error;if(s.enabled&&s.exactAllowed===false)return 'Saat kaydedildi • Alarm izni bekleniyor';return {scheduled:'Alarm açık',cancelled:'Alarm kapalı • Son seçimin korundu',fired:'Son alarm çalıştı',missed:'Alarm saati geçti • Yeniden kur',off:'Henüz alarm kurulmadı',permission:'Alarm izni bekleniyor',error:'Alarm kurulamadı'}[s.status]||'Henüz alarm kurulmadı'}
function refresh(){const box=$('#p292AlarmStatus');if(!box)return;const s=read();box.textContent=message(s);const last=$('#p292LastAlarm');if(last)last.textContent=s.savedAt?s.time+' · '+s.name:'Son kurulan alarm burada görünecek';const next=$('#p292NextAlarm');if(next)next.textContent=s.scheduled&&s.when?new Date(s.when).toLocaleString('tr-TR',{weekday:'long',day:'numeric',month:'long',hour:'2-digit',minute:'2-digit'})+(s.daily?' · Her gün':' · Bir kez'):'';const permission=$('#p292AlarmPermission');if(permission)permission.hidden=s.exactAllowed!==false;const cancel=$('#p2AlarmCancel');if(cancel)cancel.disabled=!s.enabled;}
function open(){
 const saved=read(),draft=window.trMemory?.get('trWakeDraft293',{});const state=draft&&draft.baseSavedAt===(saved.savedAt||0)?{...saved,...draft}:saved;let current=null;try{current=stations[index]}catch{}
 let station=state.url?{name:state.name,url:state.url}:{name:current?.name||'Radyo seç',url:current?._url||current?.url_resolved||current?.url||''};
 const sh=$('#sheet'),list=$('#list');if(!sh||!list)return;
 $('#sheetTitle').textContent='Radyo ile Uyan';$('#search').style.display='none';
 list.innerHTML=`<div class="p2Panel p292Alarm"><section class="p2Section p292AlarmHero"><small class="p292Eyebrow">GÜNE SENİN YAYININLA BAŞLA</small><label for="p2AlarmTime">Uyanma saati</label><div class="p292TimePicker"><span id="p292TimeValue" aria-hidden="true">${esc(/^\d{2}:\d{2}$/.test(state.time)?state.time:'07:00')}</span><svg aria-hidden="true" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M12 6v6l4 2"/></svg><input class="p2Input" id="p2AlarmTime" type="time" lang="tr" step="60" required value="${esc(/^\d{2}:\d{2}$/.test(state.time)?state.time:'07:00')}"></div><div id="p292AlarmStatus" role="status"></div><div id="p292NextAlarm"></div></section><section class="p2Section p292Saved"><small class="p292Eyebrow">SON KURULAN ALARM</small><strong id="p292LastAlarm"></strong></section><section class="p2Section p292Options"><div><small>ALARM RADYOSU</small><strong id="p292AlarmStation">${esc(station.name)}</strong></div>${current?'<button class="p2Btn" id="p292UseCurrent">Şu anki radyoyu kullan</button>':''}<label class="p292Repeat"><span>Her gün aynı saatte<small>Kapalıysa yalnızca sıradaki saatte çalar</small></span><input type="checkbox" id="p292AlarmDaily" ${state.daily?'checked':''}></label></section><div class="p292Permission" id="p292AlarmPermission" hidden><p>Radyo duraklatılmışken ve ekran kapalıyken zamanında başlaması için “Alarmlar ve hatırlatıcılar” iznini aç.</p><button class="p2Btn" id="p292AllowAlarm">Alarm iznini aç</button></div><div class="p24Actions"><button class="p2Btn" id="p2AlarmSet">ALARM KUR</button><button class="p2Btn" id="p2AlarmCancel">İPTAL ET</button></div><p class="p24Note">Seçimler hatırlanır; alarm “Alarm kur” ile etkinleşir. Radyo pasifken de seçtiğin yayın açılır. Telefonun medya sesi ve internet bağlantısı açık olmalı.</p></div>`;
 sh.classList.add('show');refresh();
 function saveDraft(baseSavedAt=read().savedAt||0){window.trMemory?.set('trWakeDraft293',{baseSavedAt,time:$('#p2AlarmTime').value,daily:$('#p292AlarmDaily').checked,name:station.name,url:station.url})}
 $('#p292AlarmDaily').addEventListener('change',()=>saveDraft());
 $('#p2AlarmTime').addEventListener('input',e=>{$('#p292TimeValue').textContent=e.target.value||'--:--';saveDraft()});
 $('#p292UseCurrent')?.addEventListener('click',()=>{station={name:current.name,url:current._url||current.url_resolved||current.url};$('#p292AlarmStation').textContent=station.name;saveDraft()});
 $('#p292AllowAlarm').onclick=()=>native('radioapp://alarmsettings');
 $('#p2AlarmSet').onclick=()=>{
  const value=$('#p2AlarmTime').value;if(!/^\d{2}:\d{2}$/.test(value)||!station.url)return toast('Saat ve radyo seç');
  const [h,m]=value.split(':').map(Number);if(h>23||m>59)return toast('Geçerli saat seç');
  if(!window.RadioNative?.setWakeAlarm)return toast('Alarmı Android uygulamasında kurabilirsin');
  try{const result=JSON.parse(window.RadioNative.setWakeAlarm(h,m,station.url,station.name,$('#p292AlarmDaily').checked));if(result.savedAt){localStorage.trWakeAlarm292=JSON.stringify(result);saveDraft(result.savedAt)}refresh();toast(result.error|| (result.scheduled?'Alarm '+value+' için kuruldu':'Saat kaydedildi; alarm iznini aç'))}catch{toast('Alarm kurulamadı; yeniden dene')}
 };
 $('#p2AlarmCancel').onclick=()=>{try{window.RadioNative?.clearWakeAlarm?.();refresh();toast('Alarm iptal edildi; son saat korundu')}catch{toast('Alarm iptal edilemedi')}};
}
window.trOpenWakeAlarm=open;
window.addEventListener('turkradyo-schedules-changed',refresh);
document.addEventListener('visibilitychange',()=>{if(!document.hidden)refresh()});
})();
