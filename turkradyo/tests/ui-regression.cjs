const {test,before,after}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {execFileSync}=require('node:child_process');
let pw;
try{pw=require('playwright')}catch{pw=require(process.env.CODEX_PRIMARY_RUNTIME_NODE_MODULES+'/playwright')}
const ROOT=path.resolve(__dirname,'../..'), ASSETS='turkradyo/app/src/main/assets/';
const A='https://appassets.androidplatform.net/assets/';
function source(p){return process.env.SOURCE_REF?execFileSync('git',['show',process.env.SOURCE_REF+':'+p],{cwd:ROOT,encoding:'utf8'}):fs.readFileSync(path.join(ROOT,p),'utf8')}
const stations=[{stationuuid:'one',name:'Power Türk',url:'https://stream.test/one',bitrate:128,lastcheckok:1,tags:'pop'},
 {stationuuid:'two',name:'TRT FM',url:'https://stream.test/two',bitrate:128,lastcheckok:1,tags:'pop'}];
let browser;
before(async()=>{browser=await pw.chromium.launch({headless:true,...(process.env.CHROME_PATH?{executablePath:process.env.CHROME_PATH}:{})})});
after(async()=>{await browser?.close()});
async function pageFor(html='<!doctype html><body></body>',radioData=stations,catalogData=radioData,storageState){
 const page=await browser.newPage({storageState});
 await page.route('**/*',async route=>{
  const url=route.request().url();
  if(url===A+'fixture.html')return route.fulfill({contentType:'text/html',body:html});
  if(url.startsWith(A)){
   try{const file=new URL(url).pathname.replace('/assets/','');return route.fulfill({contentType:file.endsWith('.css')?'text/css':file.endsWith('.js')?'application/javascript':file.endsWith('.html')?'text/html':'application/json',body:source(ASSETS+file)})}catch{return route.fulfill({status:404,body:''})}
  }
  if(url.includes('api.radio-browser.info'))return route.fulfill({contentType:'application/json',body:JSON.stringify(url.includes('limit=3000')?catalogData:radioData)});
  return route.abort();
 });
 await page.goto(A+'fixture.html');
 return page;
}
async function script(page,name){await page.addScriptTag({content:source(ASSETS+name)})}

async function fullApp({main=stations,catalog=main,storageState,serviceActive=true}={}){
 const page=await pageFor(undefined,main,catalog,storageState);await page.setViewportSize({width:390,height:844});
 await page.addInitScript(serviceActive=>{
  if(localStorage.getItem('p2Active')===null)localStorage.setItem('p2Active','1');window.sentCommands=[];
  window.nativeState={nativeRecovery:true,serviceActive,isPlaying:false,manualPause:true,eq:[0,0,600,0,0],normalize:true,smooth:false,volume:.7,...JSON.parse(localStorage.testNativeAudio||'{}')};
  const command=u=>{window.sentCommands.push(u);const x=new URL(u),p=x.searchParams,t=window.nativeState;if(x.hostname==='normalize')t.normalize=p.get('on')==='1';if(x.hostname==='smooth')t.smooth=p.get('on')==='1';if(x.hostname==='vol')t.volume=Number(p.get('v'));if(x.hostname==='eq')t.eq[Number(p.get('band'))]=Number(p.get('level'));localStorage.testNativeAudio=JSON.stringify({eq:t.eq,normalize:t.normalize,smooth:t.smooth,volume:t.volume})};
  const wake=()=>JSON.parse(localStorage.testNativeWake||'{"time":"07:00","status":"off","enabled":false,"exactAllowed":true}');
  window.RadioNative={getWakeAlarm:()=>JSON.stringify(wake()),setWakeAlarm:(h,m,url,name,daily)=>{const d=new Date();d.setHours(h,m,0,0);if(d<=Date.now())d.setDate(d.getDate()+1);const exact=window.wakeExactAllowed!==false;const state={time:String(h).padStart(2,'0')+':'+String(m).padStart(2,'0'),url,name,daily,savedAt:Date.now(),when:+d,enabled:true,scheduled:exact,exactAllowed:exact,status:exact?'scheduled':'permission'};localStorage.testNativeWake=JSON.stringify(state);return JSON.stringify(state)},clearWakeAlarm:()=>{const s={...wake(),enabled:false,scheduled:false,status:'cancelled'};localStorage.testNativeWake=JSON.stringify(s);return JSON.stringify(s)},command,getTelemetry:()=>JSON.stringify(window.nativeState),getNowTitle:()=>'',getRecentTracks:()=>localStorage.testNativeTracks||'[]',getStreamHealth:()=>'{}',getProductGuard:()=>'{}',getCatalogHealth:()=>'{}',setQueue:(q,i)=>{window.lastQueue={items:JSON.parse(q),index:i}},getNotaTextArchive:()=>'[]'};
 },serviceActive);
 await page.goto(A+'index.html');
 const java=source('turkradyo/app/src/main/java/com/muhammetgecgil/turkradyo/MainActivity.java');
 const raw=java.match(/v\.evaluateJavascript\("(.*)",null\);/)[1].replaceAll('"+A+"',A);
 await page.evaluate(JSON.parse('"'+raw+'"'));await page.waitForTimeout(6500);return page;
}

const groupStation=(id,name,tags,clickcount=100)=>({stationuuid:id,name,tags,clickcount,bitrate:128,lastcheckok:1,countrycode:'TR',url:'https://stream.test/'+id});
const groupMain=[groupStation('pop-a','Pop A','pop',900),groupStation('news-a','Haber A','news',800),groupStation('rock-a','Rock A','rock',700)];
const groupCatalog=[...groupMain,groupStation('pop-b','Pop B','pop',600),groupStation('pop-c','Pop C','pop',500),groupStation('news-b','Haber B','news',400),
 {...groupStation('foreign','Foreign Pop','pop',2000),countrycode:'DE'},
 {...groupStation('broken','Broken Pop','pop',1900),lastcheckok:0},
 {...groupStation('pop-b','Pop B alternate','pop',1800),url:'https://stream.test/duplicate'},
 {...groupStation('copy','Duplicate stream','pop',1700),url:'https://stream.test/pop-c'}];
async function groupApp(main=groupMain,catalog=groupCatalog){
 const page=await fullApp({main,catalog});
 await page.evaluate(data=>{
  localStorage.setItem('v5MainKeys',JSON.stringify(data.main.map(s=>s.stationuuid)));
  localStorage.setItem('v201CatalogCache',JSON.stringify({at:Date.now(),data:data.catalog}));
  const list=window.trGetStations();window.select(list.findIndex(s=>s.stationuuid===data.main[0].stationuuid),false);window.sentCommands=[];
 },{main,catalog});
 return page;
}
async function playedNames(page){return page.evaluate(()=>window.sentCommands.filter(u=>u.startsWith('radioapp://play?')).map(u=>new URL(u).searchParams.get('name')))}

async function chooseProfile(page,value){
 await page.locator('.nature-profile-pill').click();await page.waitForTimeout(250);
 if(value!=='baz')assert.equal(await page.locator('[data-prof="'+value+'"]').getAttribute('data-about-app'),null,'A selectable profile must not become the About entry');
 await page.locator(value==='baz'?'[data-baz-profile]':'[data-prof="'+value+'"]').click();
 await page.waitForTimeout(1600);
 assert.equal(await page.locator('#sheet.show').count(),0,'Profile selection must not open a feature dialog');
}

async function reopenApp(page,options={}){
 const storageState=await page.context().storageState();await page.close();
 // A fresh WebView has persistent origin storage, but no JS globals or sessionStorage.
 return fullApp({...options,storageState});
}

test('Fresh opening restores a favorite outside the main list through catalog reordering without autoplay',{timeout:45000},async()=>{
 const main=Array.from({length:80},(_,i)=>groupStation('main-'+i,'Radyo '+i,i%2?'rock':'pop',1000-i));
 const outside=groupStation('outside','Dış Pop','pop',50),catalog=[...main,outside];
 let page=await fullApp({main,catalog});try{
  await page.locator('#p2Genres').click();await page.locator('[data-p2g="Pop"]').click();await page.locator('[data-p2play="outside"]').click();
  await page.locator('#play').click();
  await page.locator('#menuBtn').click();await page.locator('#search').fill('Dış Pop');await page.locator('#list .fav').click();await page.locator('#closeSheet').click();
  const previous=await page.evaluate(()=>localStorage.trLastRadioKey);
  page=await reopenApp(page,{main:[...main].reverse(),catalog:[...catalog].reverse(),serviceActive:false});
  assert.equal(await page.locator('#now').textContent(),'Dış Pop');
  assert.equal(await page.evaluate(()=>localStorage.trLastRadioKey),previous);
  assert.equal(await page.evaluate(()=>window.trNavigation.mainStations().length),80);
  assert.deepEqual(await playedNames(page),[]);
  await page.locator('.bottom [data-nav="favorites"]').click();
  assert.match(await page.locator('#list').textContent(),/Dış Pop/);assert.equal(await page.locator('#search').inputValue(),'Dış Pop');
  await page.locator('#closeSheet').click();await page.locator('#play').click();assert.deepEqual(await playedNames(page),['Dış Pop']);
 }finally{await page.close()}
});

test('Timer preferences and alarm drafts survive a fresh opening without creating new schedules',{timeout:45000},async()=>{
 let page=await fullApp();try{
  await page.locator('#p2Sleep').click();await page.locator('#p24H').fill('2');await page.locator('#p24M').fill('17');await page.locator('#p24Fade').check();
  await page.locator('#p24Set').click();await page.locator('#p24Cancel').click();await page.locator('#closeSheet').click();
  await page.locator('#p2Alarm').click();await page.locator('#p2AlarmTime').fill('06:25');await page.locator('#p2AlarmSet').click();
  await page.locator('#p2AlarmTime').fill('08:40');await page.locator('#p292AlarmDaily').check();
  page=await reopenApp(page);
  await page.locator('#p2Sleep').click();assert.equal(await page.locator('#p24H').inputValue(),'2');assert.equal(await page.locator('#p24M').inputValue(),'17');assert.ok(await page.locator('#p24Fade').isChecked());assert.equal(await page.locator('#p24Countdown').textContent(),'--:--');
  await page.locator('#closeSheet').click();await page.locator('#p2Alarm').click();
  assert.equal(await page.locator('#p2AlarmTime').inputValue(),'08:40');assert.ok(await page.locator('#p292AlarmDaily').isChecked());assert.match(await page.locator('#p292LastAlarm').textContent(),/06:25/);
  assert.equal(await page.evaluate(()=>JSON.parse(localStorage.testNativeWake).time),'06:25','A remembered draft must not silently change the armed alarm');
  assert.deepEqual(await page.evaluate(()=>window.sentCommands.filter(u=>/radioapp:\/\/(play|resume|sleep|alarm)/.test(u))),[]);
 }finally{await page.close()}
});

test('Native sound choices win over stale alignment and all EQ bands and quality restore after reopening',{timeout:45000},async()=>{
 const hd={...stations[0],stationuuid:'one-hd',url:'https://stream.test/hd',bitrate:320},catalog=[...stations,hd];
 let page=await fullApp({catalog});try{
  await page.locator('#settingsBtn').click();await page.locator('#sNorm').click();await page.locator('#sSmooth').click();await page.locator('#closeSheet').click();
  await page.locator('#volume').evaluate(e=>{e.value='.35';e.dispatchEvent(new Event('input',{bubbles:true}))});
  await page.locator('[data-mode="eq"]').click();await page.locator('[data-preset="bass"]').click();
  await page.evaluate(()=>{for(const [band,value] of [[0,-4],[4,7]]){const el=document.querySelector('[data-eq="'+band+'"]');el.value=value;el.dispatchEvent(new Event('input',{bubbles:true}))}});await page.waitForTimeout(100);
  await page.locator('#closeSheet').click();await page.locator('#sigTools summary').click();await page.locator('#v6Heal').click();await page.locator('#v6Quality').click();await page.locator('[data-v6q="high"]').click();await page.waitForTimeout(300);
  await page.evaluate(()=>localStorage.v6Align='1');
  page=await reopenApp(page,{catalog});
  assert.equal(await page.locator('#sigTools').evaluate(e=>e.open),true);assert.equal(await page.locator('#volume').inputValue(),'0.35');
  assert.equal(await page.locator('#v6Align').evaluate(e=>e.classList.contains('on')),false);assert.equal(await page.locator('#v6Heal').evaluate(e=>e.classList.contains('on')),false);assert.equal(await page.locator('#v6Quality b').textContent(),'HQ');
  assert.equal(await page.evaluate(()=>window.trGetStations()[window.trGetIndex()]._url),'https://stream.test/hd');
  assert.deepEqual(await page.evaluate(()=>window.sentCommands),[],'Restoring preferences must not replay old commands');
  await page.locator('#settingsBtn').click();assert.equal(await page.locator('#sNorm').evaluate(e=>e.classList.contains('on')),false);assert.equal(await page.locator('#sSmooth').evaluate(e=>e.classList.contains('on')),true);await page.locator('#closeSheet').click();
  await page.locator('[data-mode="eq"]').click();assert.deepEqual(await page.locator('[data-eq]').evaluateAll(a=>a.map(e=>Number(e.value))),[-4,5,1,-1,7]);
 }finally{await page.close()}
});

test('Profile, theme choices, feature options and genre queue are remembered in a fresh app',{timeout:50000},async()=>{
 const mixed=[...stations,groupStation('rock','Rock Radyo','rock')];let page=await fullApp({main:mixed});try{
  await page.locator('[data-mode="themes"]').click();await page.locator('.nature-filter[data-f="Çiçek"]').click();await page.locator('[data-like="orchid"]').click();await page.locator('[data-off="rose"]').click();await page.locator('[data-use="orchid"]').click();await page.evaluate(()=>window.trCloseTopOverlay());
  await page.evaluate(()=>window.trOpenSlow279());await page.locator('[data-slow279-min="60"]').click();await page.locator('[data-slow279-int="deep"]').click();await page.locator('#tr279SlowAuto').click();await page.locator('#tr279SlowCap').evaluate(e=>{e.value='.55';e.dispatchEvent(new Event('input',{bubbles:true}))});await page.locator('#closeSheet').click();
  await page.evaluate(()=>window.openNotaAI());await page.locator('[data-nai-sec="120"]').click();await page.locator('[data-nai-mode="text"]').click();await page.locator('#closeSheet').click();
  await page.evaluate(()=>window.trSmartListener.open());await page.locator('[data-trtoggle="alarmGuard"]').click();await page.locator('#trSmartClose').click();
  await page.evaluate(()=>window.trOpenGenre280());await page.locator('[data-g280="Pop"]').click();await page.locator('#closeSheet').click();await chooseProfile(page,'1');
  page=await reopenApp(page,{main:mixed});
  assert.equal(await page.evaluate(()=>localStorage.trActiveProfileV281),'1');assert.equal(await page.locator('#p2UnifiedPremiumGrid263').isVisible(),false);assert.equal(await page.evaluate(()=>document.documentElement.dataset.natureTheme),'orchid');
  assert.deepEqual(new Set(await page.evaluate(()=>window.lastQueue.items.map(s=>s.name))),new Set(['Power Türk','TRT FM']));assert.equal(await page.evaluate(()=>JSON.parse(localStorage.trGenre273).active),'Pop');
  await page.locator('[data-mode="themes"]').click();assert.equal(await page.locator('.nature-filter.on').textContent(),'Çiçek');assert.ok(await page.locator('[data-like="orchid"]').evaluate(e=>e.classList.contains('liked')));assert.ok(await page.locator('[data-off="rose"]').evaluate(e=>e.classList.contains('disabled')));await page.evaluate(()=>window.trCloseTopOverlay());
  await page.evaluate(()=>window.trOpenSlow279());assert.ok(await page.locator('[data-slow279-min="60"]').evaluate(e=>e.classList.contains('on')));assert.ok(await page.locator('[data-slow279-int="deep"]').evaluate(e=>e.classList.contains('on')));assert.equal(await page.locator('#tr279SlowCap').inputValue(),'0.55');assert.equal(await page.locator('#tr279SlowAuto').textContent(),'KAPALI');await page.locator('#closeSheet').click();
  await page.evaluate(()=>window.openNotaAI());assert.ok(await page.locator('[data-nai-sec="120"]').evaluate(e=>e.classList.contains('on')));assert.ok(await page.locator('[data-nai-mode="text"]').evaluate(e=>e.classList.contains('on')));assert.equal(await page.evaluate(()=>window.trSmartListener.state().alarmGuard),false);
  assert.deepEqual(await playedNames(page),[]);
 }finally{await page.close()}
});

test('Catalog, Turkey Groups and track filters retain their choices and search text',{timeout:45000},async()=>{
 let page=await fullApp({main:groupMain,catalog:groupCatalog});try{
  await page.locator('[data-mode="all"]').click();await page.locator('[data-cat-view="main"]').click();await page.locator('[data-cat-group="Pop"]').click();await page.locator('#catalogSearch').pressSequentially('Pop');
  assert.equal(await page.locator('#catalogSearch').inputValue(),'Pop');await page.locator('#closeSheet').click();
  await page.locator('#p2Genres').click();await page.locator('[data-p2g="Haber"]').click();await page.locator('#p2Search').fill('Haber A');await page.locator('#closeSheet').click();
  await page.evaluate(()=>localStorage.testNativeTracks=JSON.stringify([{title:'Sezen Aksu',station:'Haber A',time:Date.now()},{title:'Diğer Şarkı',station:'Pop A',time:Date.now()}]));await page.locator('#p2Tracks').click();await page.locator('#tr279TrackStation').selectOption('Haber A');await page.locator('#tr279TrackSearch').fill('Sezen');
  page=await reopenApp(page,{main:groupMain,catalog:groupCatalog});
  await page.locator('[data-mode="all"]').click();assert.equal(await page.locator('[data-cat-view="main"]').evaluate(e=>e.classList.contains('active')),true);assert.equal(await page.locator('[data-cat-group="Pop"]').evaluate(e=>e.classList.contains('active')),true);assert.equal(await page.locator('#catalogSearch').inputValue(),'Pop');await page.locator('#closeSheet').click();
  await page.locator('#p2Genres').click();assert.equal(await page.locator('.p2Cat.on').textContent(),'Haber');assert.equal(await page.locator('#p2Search').inputValue(),'Haber A');assert.deepEqual(await page.locator('#p2Stations strong').allTextContents(),['Haber A']);await page.locator('#closeSheet').click();
  await page.locator('#p2Tracks').click();assert.equal(await page.locator('#tr279TrackSearch').inputValue(),'Sezen');assert.equal(await page.locator('#tr279TrackStation').inputValue(),'Haber A');assert.match(await page.locator('#tr279TrackList').textContent(),/Sezen Aksu/);
 }finally{await page.close()}
});

test('Baz stays minimal after delayed mounts, profile switches and a fresh page load',{timeout:50000},async()=>{
 const page=await fullApp();try{
  await page.setViewportSize({width:360,height:800});
  const minimal=async()=>{
   assert.equal(await page.locator('#p2UnifiedPremiumGrid263').isVisible(),false,'studio must stay hidden');
   assert.equal(await page.locator('.modes').isVisible(),false);
   assert.equal(await page.locator('.wave').isVisible(),false);
   assert.equal(await page.locator('#trBazGrid button:visible').count(),6);
   assert.equal(await page.locator('.app>*:visible').count(),3,'only header, player and shortcuts');
   for(const r of await page.locator('#trBazGrid button').evaluateAll(a=>a.map(e=>({height:e.getBoundingClientRect().height,width:e.getBoundingClientRect().width}))))assert.ok(r.height>=44&&r.height<=64&&r.width>=44);
  };
  await chooseProfile(page,'baz');await page.waitForTimeout(3100);await minimal();
  await page.locator('[data-baz="tracks"]').click();await page.waitForTimeout(150);assert.match(await page.locator('#sheetTitle').textContent(),/50/);await page.locator('#closeSheet').click();
  await page.locator('[data-baz="groups"]').click();assert.match(await page.locator('#sheetTitle').textContent(),/Türkiye/);await page.locator('#closeSheet').click();
  await chooseProfile(page,'1');assert.equal(await page.locator('#trBazGrid').isVisible(),false);
  await chooseProfile(page,'2');assert.equal(await page.locator('#p2UnifiedPremiumGrid263').isVisible(),true,await page.evaluate(()=>JSON.stringify({body:document.body.className,active:localStorage.p2Active,profile:localStorage.trActiveProfileV281,gridStyle:document.querySelector('#p2UnifiedPremiumGrid263').style.cssText,parent:document.querySelector('#p2UnifiedPremiumGrid263').parentElement.className,children:document.querySelector('#p2UnifiedPremiumGrid263').children.length})));
  await chooseProfile(page,'baz');await minimal();
  await page.reload();const java=source('turkradyo/app/src/main/java/com/muhammetgecgil/turkradyo/MainActivity.java');const raw=java.match(/v\.evaluateJavascript\("(.*)",null\);/)[1].replaceAll('"+A+"',A);await page.evaluate(JSON.parse('"'+raw+'"'));await page.waitForTimeout(6500);await minimal();
 }finally{await page.close()}
});

test('Theme palettes color page, player, cards and dialogs, including Baz theme access',{timeout:45000},async()=>{
 const page=await fullApp();try{
  const surfaces=()=>page.evaluate(()=>['body','.hero','.modes .mode','#p2UnifiedPremiumGrid263>button','#sheet .panel'].map(s=>{const c=getComputedStyle(document.querySelector(s));return c.backgroundColor+' '+c.backgroundImage}));
  const palettes=[];
  for(const id of ['morpho-blue','orchid','emerald-swallowtail']){
   await page.locator('[data-mode="themes"]').click();await page.locator('[data-use="'+id+'"]').click();await page.evaluate(()=>window.trCloseTopOverlay());await page.waitForTimeout(300);palettes.push(await surfaces());
  }
  for(let i=0;i<5;i++)assert.equal(new Set(palettes.map(p=>p[i])).size,3,'surface '+i+' must reflect all theme palettes');
  await chooseProfile(page,'baz');assert.equal((await surfaces())[0],palettes[2][0]);
  await page.locator('.nature-profile-pill').click();await page.waitForTimeout(250);await page.locator('#sigThemeShortcut').click();
  assert.equal(await page.locator('#profileModal.show').count(),0);
  await page.locator('[data-use="morpho-blue"]').click();await page.evaluate(()=>window.trCloseTopOverlay());await page.waitForTimeout(400);
  assert.equal((await surfaces())[0],palettes[0][0]);assert.equal(await page.locator('#p2UnifiedPremiumGrid263').isVisible(),false);
  assert.equal(await page.evaluate(()=>localStorage.getItem('trActiveProfileV281')),'baz');
 }finally{await page.close()}
});

test('Studio shortcuts fit compact rows without artwork overlap at narrow phone widths',{timeout:30000},async()=>{
 const page=await fullApp();try{
  for(const width of [360,412]){
   await page.setViewportSize({width,height:844});
   const cards=await page.locator('#p2UnifiedPremiumGrid263>button').evaluateAll(a=>a.map(e=>{const r=e.getBoundingClientRect(),i=e.querySelector('.p263Icon').getBoundingClientRect(),t=e.querySelector('.p263Label').getBoundingClientRect();return{height:r.height,overlap:i.left<t.right&&i.right>t.left&&i.top<t.bottom&&i.bottom>t.top,contained:t.right<=r.right&&t.bottom<=r.bottom&&i.left>=r.left}}));
   assert.equal(cards.length,8);assert.ok(cards.every(c=>c.height>=44&&c.height<=96&&!c.overlap&&c.contained),JSON.stringify(cards));
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);
  }
 }finally{await page.close()}
});

test('Redesigned dialogs contain keyboard focus and return to the invoking control',{timeout:30000},async()=>{
 const page=await fullApp();try{
  await page.locator('#menuBtn').click();await page.waitForTimeout(160);
  assert.equal(await page.locator('#closeSheet').evaluate(e=>e===document.activeElement),true);
  await page.keyboard.press('Shift+Tab');
  assert.equal(await page.locator('#sheet').evaluate(e=>e.contains(document.activeElement)),true);
  await page.locator('#closeSheet').click();await page.waitForTimeout(160);
  assert.equal(await page.locator('#menuBtn').evaluate(e=>e===document.activeElement),true);
  await page.locator('#settingsBtn').click();await page.waitForTimeout(160);
  assert.equal(await page.locator('#sNorm').getAttribute('role'),'switch');
  assert.equal(await page.locator('#sNorm').getAttribute('aria-checked'),'true');
  await page.locator('#sNorm').click();await page.waitForTimeout(160);
  assert.equal(await page.locator('#sNorm').getAttribute('aria-checked'),'false');
 }finally{await page.close()}
});

test('Compact layout keeps both transport rows and expanded broadcast tools usable',{timeout:30000},async()=>{
 const page=await fullApp();try{
  await page.setViewportSize({width:360,height:800});
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);
  for(const id of ['prev','play','next','prev2','shazam','next2']){
   const r=await page.locator('#'+id).boundingBox();assert.ok(r.width>=44&&r.height>=44,id+' touch target');
  }
  await page.locator('#sigTools summary').click();
  await page.locator('#v6Quality').click();await page.waitForFunction(()=>/Kalite/.test(document.querySelector('#sheetTitle').textContent));
  await page.locator('#closeSheet').click();
  await page.locator('.nature-profile-pill').click();await page.waitForTimeout(250);await page.locator('[data-prof="1"]').click();await page.waitForTimeout(3000);
  await page.locator('#settingsBtn').click();assert.match(await page.locator('#sheetTitle').textContent(),/Ayar/);
 }finally{await page.close()}
});

test('Upper buttons follow the main list; lower buttons share Turkey Groups classification',{timeout:30000},async()=>{
 const page=await groupApp();try{
  await page.locator('#next').click();assert.equal(await page.locator('#now').textContent(),'Haber A');
  await page.locator('#p2Genres').click();await page.locator('[data-p2g="Haber"]').click();
  const names=await page.locator('#p2Stations strong').allTextContents();assert.deepEqual(new Set(names),new Set(['Haber A','Haber B']));
  await page.locator('#closeSheet').click();
  await page.locator('#next2').click();await page.waitForFunction(()=>document.querySelector('#now').textContent==='Haber B');
  assert.equal(await page.locator('#next2>span').textContent(),'HABER GRUBU');
  await page.locator('#prev2').click();await page.waitForFunction(()=>document.querySelector('#now').textContent==='Haber A');
  await page.locator('#next').click();assert.equal(await page.locator('#now').textContent(),'Rock A');
  assert.deepEqual(await playedNames(page),['Haber A','Haber B','Haber A','Rock A']);
  assert.deepEqual(await page.evaluate(()=>JSON.parse(localStorage.v5MainKeys)),groupMain.map(s=>s.stationuuid));
 }finally{await page.close()}
});

test('Cached group navigation works offline, wraps and skips duplicate, foreign and broken streams',{timeout:30000},async()=>{
 const page=await groupApp();try{
  await page.route('**/*api.radio-browser.info/**',route=>route.abort());
  for(const name of ['Pop B','Pop C','Pop A']){await page.locator('#next2').click();await page.waitForFunction(n=>document.querySelector('#now').textContent===n,name)}
  await page.locator('#prev2').click();await page.waitForFunction(()=>document.querySelector('#now').textContent==='Pop C');
  assert.deepEqual(await playedNames(page),['Pop B','Pop C','Pop A','Pop C']);
  await page.locator('#next').click();assert.equal(await page.locator('#now').textContent(),'Haber A');
 }finally{await page.close()}
});

test('Legacy genre mode cannot duplicate lower navigation or hijack upper main-list buttons',{timeout:30000},async()=>{
 const page=await groupApp();try{
  await page.evaluate(()=>localStorage.setItem('trGenre273',JSON.stringify({active:'Rock',pos:0,pool:[{name:'Wrong rock',_url:'https://wrong.test/rock'}]})));
  await page.locator('#next2').click();await page.waitForFunction(()=>document.querySelector('#now').textContent==='Pop B');
  await page.locator('#next').click();assert.equal(await page.locator('#now').textContent(),'Haber A');
  assert.equal(await page.evaluate(()=>JSON.parse(localStorage.trGenre273).active),'');
  assert.deepEqual(await playedNames(page),['Pop B','Haber A']);
 }finally{await page.close()}
});

test('A delayed group catalog cannot override a later main-list selection or pause',{timeout:30000},async()=>{
 const page=await groupApp();try{
  await page.evaluate(()=>{localStorage.removeItem('v201CatalogCache');window.radioFetchJson=()=>new Promise(r=>window.completeGroupCatalog=r)});
  await page.locator('#next2').click();await page.waitForFunction(()=>typeof window.completeGroupCatalog==='function');
  await page.locator('#next').click();
  await page.evaluate(data=>window.completeGroupCatalog(data),groupCatalog);await page.waitForTimeout(150);
  assert.equal(await page.locator('#now').textContent(),'Haber A');assert.deepEqual(await playedNames(page),['Haber A']);
  await page.evaluate(()=>{localStorage.removeItem('v201CatalogCache');window.completeGroupCatalog=null;window.sentCommands=[]});
  await page.locator('#next2').click();await page.waitForFunction(()=>typeof window.completeGroupCatalog==='function');
  await page.locator('#play').click();
  await page.evaluate(data=>window.completeGroupCatalog(data),groupCatalog);await page.waitForTimeout(150);
  assert.equal(await page.locator('#now').textContent(),'Haber A');assert.deepEqual(await playedNames(page),[]);
  await page.evaluate(()=>{localStorage.removeItem('v201CatalogCache');window.select(window.trGetStations().findIndex(s=>s.stationuuid==='pop-a'),false);window.completeGroupCatalog=null;window.sentCommands=[]});
  await page.locator('#next2').click();await page.waitForFunction(()=>typeof window.completeGroupCatalog==='function');
  await page.locator('#prev2').click();
  await page.evaluate(data=>window.completeGroupCatalog(data),groupCatalog);
  await page.waitForFunction(()=>document.querySelector('#now').textContent==='Pop C');
  assert.deepEqual(await playedNames(page),['Pop C']);
 }finally{await page.close()}
});

test('A single-station or unknown group preserves playback instead of choosing another genre',{timeout:30000},async()=>{
 const main=[groupStation('solo','Solo Jazz','jazz'),groupStation('unknown','Etiketsiz Radyo','')];
 const page=await groupApp(main,[...main,...groupMain]);try{
  await page.locator('#next2').click();await page.waitForTimeout(100);
  assert.equal(await page.locator('#now').textContent(),'Solo Jazz');assert.match(await page.locator('#toast').textContent(),/başka uygun radyo/);
  await page.evaluate(()=>window.select(window.trGetStations().findIndex(s=>s.stationuuid==='unknown'),false));
  await page.locator('#prev2').click();assert.equal(await page.locator('#now').textContent(),'Etiketsiz Radyo');
  assert.match(await page.locator('#toast').textContent(),/türü belirlenemedi/);assert.deepEqual(await playedNames(page),[]);
 }finally{await page.close()}
});

test('Mobile favorites and search select a station through the native command bridge',{timeout:30000},async()=>{
 const page=await fullApp();try{
  await page.locator('#menuBtn').click();await page.locator('#search').fill('TRT');
  assert.equal(await page.locator('#list .item').count(),1);
  await page.locator('#list .fav').click();await page.locator('#closeSheet').click();
  await page.locator('.bottom [data-nav="favorites"]').click();
  assert.match(await page.locator('#list').textContent(),/TRT FM/);
  await page.locator('#list .item').first().click();
  assert.ok((await page.evaluate(()=>window.sentCommands)).some(u=>u.startsWith('radioapp://play?')&&decodeURIComponent(u).includes('TRT FM')));
 }finally{await page.close()}
});

test('EQ presets send all five bands and settings show stored audio preferences',{timeout:30000},async()=>{
 const page=await fullApp();try{
  await page.locator('[data-mode="eq"]').click();
  assert.equal(await page.locator('[data-eq="2"]').inputValue(),'6');
  await page.locator('[data-preset="bass"]').click();
  const commands=await page.evaluate(()=>window.sentCommands.filter(u=>u.startsWith('radioapp://eq?')));
  assert.equal(commands.length,5);
  assert.deepEqual(commands.map(u=>Number(new URL(u).searchParams.get('level'))),[800,500,100,-100,-200]);
  await page.locator('#closeSheet').click();await page.locator('#settingsBtn').click();
  assert.ok(await page.locator('#sNorm').evaluate(e=>e.classList.contains('on')));
  assert.ok(!(await page.locator('#sSmooth').evaluate(e=>e.classList.contains('on'))));
 }finally{await page.close()}
});

test('Sleep timer passes fade to Android and cancellation clears the countdown',{timeout:30000},async()=>{
 const page=await fullApp();try{
  await page.locator('#p2Sleep').click();await page.locator('#p24Fade').check();
  await page.locator('.p24Quick [data-q="15"]').click();
  const commands=await page.evaluate(()=>window.sentCommands);
  assert.ok(commands.some(u=>u.startsWith('radioapp://sleep?')&&new URL(u).searchParams.get('fade')==='1'));
  assert.notEqual(await page.locator('#p24Countdown').textContent(),'--:--');
  await page.locator('#p24Cancel').click();
  assert.equal(await page.locator('#p24Countdown').textContent(),'--:--');
  assert.ok((await page.evaluate(()=>window.sentCommands)).includes('radioapp://sleepclear?id=8299'));
 }finally{await page.close()}
});

test('Wake alarm remembers last time and station after closing, cancellation and reload',{timeout:40000},async()=>{
 const page=await fullApp();try{
  await page.setViewportSize({width:360,height:800});await page.locator('#p2Alarm').click();await page.locator('#p2AlarmTime').fill('09:35');assert.equal(await page.locator('#p292TimeValue').textContent(),'09:35');assert.ok(await page.locator('.p292Saved .p292Eyebrow').isVisible());await page.locator('#p292AlarmDaily').check();await page.locator('#p2AlarmSet').click();
  assert.match(await page.locator('#p292AlarmStatus').textContent(),/Alarm açık/);assert.match(await page.locator('#p292LastAlarm').textContent(),/09:35.*Power Türk/);
  await page.locator('#closeSheet').click();await page.evaluate(()=>select(1,false));await page.locator('#p2Alarm').click();
  assert.equal(await page.locator('#p2AlarmTime').inputValue(),'09:35');assert.equal(await page.locator('#p292AlarmStation').textContent(),'Power Türk');assert.ok(await page.locator('#p292AlarmDaily').isChecked());
  await page.locator('#p2AlarmCancel').click();assert.match(await page.locator('#p292AlarmStatus').textContent(),/Alarm kapalı/);assert.equal(await page.locator('#p2AlarmTime').inputValue(),'09:35');
  await page.reload();const java=source('turkradyo/app/src/main/java/com/muhammetgecgil/turkradyo/MainActivity.java');const raw=java.match(/v\.evaluateJavascript\("(.*)",null\);/)[1].replaceAll('"+A+"',A);await page.evaluate(JSON.parse('"'+raw+'"'));await page.waitForTimeout(6500);
  await page.locator('#p2Alarm').click();assert.equal(await page.locator('#p2AlarmTime').inputValue(),'09:35');assert.match(await page.locator('#p292LastAlarm').textContent(),/Power Türk/);
  assert.deepEqual(await page.evaluate(()=>window.sentCommands.filter(u=>u.startsWith('radioapp://play'))),[],'Setting an alarm must not start radio immediately');
 }finally{await page.close()}
});

test('Wake permission is actionable and a pending alarm is never shown as armed',{timeout:30000},async()=>{
 const page=await fullApp();try{
  await page.evaluate(()=>window.wakeExactAllowed=false);await page.locator('#p2Alarm').click();await page.locator('#p2AlarmTime').fill('06:20');await page.locator('#p2AlarmSet').click();
  assert.match(await page.locator('#p292AlarmStatus').textContent(),/izni|izin/i);assert.equal(await page.locator('#p292NextAlarm').textContent(),'');assert.ok(await page.locator('#p292AlarmPermission').isVisible());
  await page.locator('#p292AllowAlarm').click();assert.ok((await page.evaluate(()=>window.sentCommands)).includes('radioapp://alarmsettings'));
  await page.evaluate(()=>{const s=JSON.parse(localStorage.testNativeWake);Object.assign(s,{scheduled:true,exactAllowed:true,status:'scheduled'});localStorage.testNativeWake=JSON.stringify(s);window.dispatchEvent(new Event('turkradyo-schedules-changed'))});
  assert.match(await page.locator('#p292AlarmStatus').textContent(),/Alarm açık/);assert.equal(await page.locator('#p292AlarmPermission').isVisible(),false);assert.equal(await page.locator('#p2AlarmTime').inputValue(),'06:20');
 }finally{await page.close()}
});

test('Sleep countdown reads native state after reopening, respects expiry and reports scheduling errors',{timeout:30000},async()=>{
 const page=await fullApp();try{
  await page.evaluate(()=>{window.timerState={when:Date.now()+61_000,fade:true};window.RadioNative.getSleepTimer=()=>JSON.stringify(window.timerState);window.RadioNative.setSleepTimer=(n,fade)=>{if(window.timerFail)return '{"error":"Zamanlayıcı başlatılamadı"}';window.timerState={when:Date.now()+n*60000,fade};return JSON.stringify(window.timerState)};window.RadioNative.clearSleepTimer=()=>JSON.stringify(window.timerState={when:0,fade:false});localStorage.p24Timer=JSON.stringify({when:Date.now()+900000,pendingUntil:Date.now()+900000})});
  await page.locator('#p2Sleep').click();assert.match(await page.locator('#p24Countdown').textContent(),/^01:0[01]$/);assert.ok(await page.locator('#p24Fade').isChecked());
  await page.locator('.p24Quick [data-q="30"]').click();await page.locator('#closeSheet').click();await page.locator('#p2Sleep').click();assert.match(await page.locator('#p24Countdown').textContent(),/^(30:00|29:5\d)$/);
  await page.evaluate(()=>window.timerState.when=Date.now()-1);await page.waitForTimeout(1100);assert.equal(await page.locator('#p24Countdown').textContent(),'--:--');
  await page.evaluate(()=>window.timerFail=true);await page.locator('.p24Quick [data-q="15"]').click();assert.match(await page.locator('#toast').textContent(),/başlatılamadı/);assert.equal(await page.locator('#p24Countdown').textContent(),'--:--');
  await page.locator('#p24Cancel').click();assert.equal(await page.evaluate(()=>window.timerState.when),0);
 }finally{await page.close()}
});

test('Theme reset clears inline colors and back closes the theme overlay',{timeout:30000},async()=>{
 const page=await fullApp();try{
  await page.locator('[data-mode="themes"]').click();await page.locator('[data-use="morpho-blue"]').click();
  assert.equal(await page.evaluate(()=>document.documentElement.style.getPropertyValue('--red')),'#23a9ff');
  await page.locator('#natureReset').click();
  assert.equal(await page.evaluate(()=>document.documentElement.style.getPropertyValue('--red')),'');
  assert.ok(await page.evaluate(()=>window.trCloseTopOverlay()));
  assert.equal(await page.locator('#natureThemeModal.show').count(),0);
 }finally{await page.close()}
});

test('Playback labels follow real pause and buffering states without restarting playback',{timeout:30000},async()=>{
 const page=await fullApp();try{
  assert.equal(await page.locator('.tr-live-text').textContent(),'DURAKLATILDI');
  assert.match(await page.locator('#v9Health').textContent(),/DURAKLATILDI/);
  await page.evaluate(()=>Object.assign(window.nativeState,{manualPause:false,buffering:true}));
  await page.waitForFunction(()=>document.body.dataset.playback==='buffering');
  assert.equal(await page.locator('.tr-live-text').textContent(),'BAĞLANIYOR');
  assert.deepEqual(await page.evaluate(()=>window.sentCommands.filter(u=>/radioapp:\/\/(play|resume)\?/.test(u))),[]);
 }finally{await page.close()}
});

test('Slow Mod stops mutating an unchanged label',{timeout:15000},async()=>{
 const page=await pageFor('<body><button id="v12Smart"><b>☾</b>SLOW MOD</button></body>');
 try{
  await script(page,'profile2-slow-pro-v18-7.js');
  await page.waitForTimeout(350);
  await page.evaluate(()=>{window.changes=0;new MutationObserver(x=>window.changes+=x.length).observe(document.body,{subtree:true,characterData:true,childList:true})});
  await page.waitForTimeout(350);
  assert.equal(await page.evaluate(()=>window.changes),0);
 }finally{await page.close()}
});

test('Quality badges settle without triggering their own observer',{timeout:15000},async()=>{
 const page=await pageFor('<body><div class="modes"></div><div id="list"><div class="item"><b class="itName">TRT FM</b><small class="itSub">Canlı</small></div></div></body>');
 try{
  await script(page,'smart-listener-v1.js');await page.waitForTimeout(350);
  await page.evaluate(()=>{window.changes=0;new MutationObserver(x=>window.changes+=x.length).observe(document.getElementById('list'),{childList:true,subtree:true,characterData:true})});
  await page.waitForTimeout(500);
  assert.equal(await page.evaluate(()=>window.changes),0);
  assert.equal(await page.locator('.trChannelQuality').count(),1);
 }finally{await page.close()}
});

test('Native recovery and manual pause cannot trigger a second JavaScript player',{timeout:15000},async()=>{
 const page=await pageFor('<body><b id="now">Power Türk</b><div id="freq"></div></body>');
 try{
  await page.evaluate(s=>{window.stations=s.map(x=>({...x,_url:x.url}));window.index=0;window.paused=true;window.playCalls=0;window.play=()=>window.playCalls++;window.RadioNative={getTelemetry:()=>JSON.stringify({nativeRecovery:true,manualPause:true,lastError:2001,bufferCount:4})}},stations);
  await page.route('**/*api.radio-browser.info/**',route=>route.fulfill({contentType:'application/json',body:JSON.stringify([{...stations[0],url:'https://stream.test/alternate'}])}));
  await script(page,'profile1-radio-ui-v9.js');await page.waitForTimeout(2900);
  assert.equal(await page.evaluate(()=>window.playCalls),0);
 }finally{await page.close()}
});

test('Manual repair result is discarded after selecting a different station',{timeout:15000},async()=>{
 const page=await pageFor('<body><div class="actions"></div></body>');
 try{
  await page.evaluate(s=>{
   window.stations=s.map(x=>({...x,_url:x.url}));window.index=0;window.paused=false;window.playCalls=0;window.play=()=>window.playCalls++;
   window.toast=()=>{};window.pendingRepair=null;
   const result={...s[0],url:'https://stream.test/repaired'};
   window.radioFetchJson=url=>url.includes('/byname/')?new Promise(r=>window.pendingRepair=()=>r([result])):Promise.resolve(s);
   window.fetch=url=>window.radioFetchJson(url).then(data=>({ok:true,json:async()=>data}));
  },stations);
  await script(page,'profile1-radio-ui-v6.js');await page.locator('#v6Repair').click();
  await page.evaluate(()=>{window.index=1;window.pendingRepair()});await page.waitForTimeout(150);
  assert.equal(await page.evaluate(()=>window.playCalls),0);
  assert.equal(await page.evaluate(()=>window.stations[0]._url),'https://stream.test/one');
 }finally{await page.close()}
});

test('Damaged preferences do not prevent catalog and play controls from loading',{timeout:15000},async()=>{
 const page=await pageFor();
 try{
  await page.evaluate(()=>{localStorage.setItem('favs','{broken');localStorage.setItem('recent','null');localStorage.setItem('trLastStation293','{broken');localStorage.setItem('v6Overrides','null')});
  await page.goto(A+'index.html');
  await page.waitForFunction(()=>document.getElementById('now')?.textContent==='Power Türk');
  await page.locator('#next').click();
  assert.equal(await page.locator('#now').textContent(),'TRT FM');
 }finally{await page.close()}
});

test('Catalog requests time out while reading the response body',{timeout:15000},async()=>{
 const page=await pageFor();
 try{
  await page.goto(A+'index.html');
  const result=await page.evaluate(async()=>{
   window.fetch=async(url,opts)=>({ok:true,json:()=>new Promise((resolve,reject)=>opts.signal.addEventListener('abort',()=>reject(new Error('aborted'))))});
   const start=performance.now();try{await radioFetchJson('/slow-body',40);return false}catch{return performance.now()-start<1000}
  });
  assert.equal(result,true);
 }finally{await page.close()}
});

test('Offline opening reuses the cached station catalog',{timeout:15000},async()=>{
 const page=await pageFor();
 try{
  await page.evaluate(s=>localStorage.setItem('trCatalogCache',JSON.stringify(s)),stations);
  await page.route('**/*api.radio-browser.info/**',route=>route.abort());
  await page.goto(A+'index.html');
  await page.waitForFunction(()=>document.getElementById('now')?.textContent==='Power Türk');
  assert.equal(await page.locator('#now').textContent(),'Power Türk');
 }finally{await page.close()}
});

test('Full Android asset bootstrap keeps settings and next controls responsive',{timeout:30000},async()=>{
 const page=await pageFor();const errors=[];page.on('pageerror',e=>errors.push(e.message));
 try{
  await page.addInitScript(()=>{window.RadioNative={getTelemetry:()=>'{"nativeRecovery":true}',getNowTitle:()=>'',getRecentTracks:()=> '[]',getStreamHealth:()=> '{}',getProductGuard:()=> '{}',getCatalogHealth:()=> '{}',setQueue:()=>{}}});
  await page.goto(A+'index.html');
  const java=source('turkradyo/app/src/main/java/com/muhammetgecgil/turkradyo/MainActivity.java');
  const raw=java.match(/v\.evaluateJavascript\("(.*)",null\);/)[1].replaceAll('"+A+"',A);
  await page.evaluate(JSON.parse('"'+raw+'"'));
  await page.waitForTimeout(6500);
  await page.locator('#settingsBtn').click();await page.waitForTimeout(700);
  assert.match(await page.locator('#sheetTitle').textContent(),/Ayar/i);
  await page.locator('#closeSheet').click();
  await page.locator('#next').click();
  assert.ok(await page.locator('#now').textContent());
  assert.deepEqual(errors,[]);
 }finally{await page.close()}
});
