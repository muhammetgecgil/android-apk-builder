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
async function pageFor(html='<!doctype html><body></body>',radioData=stations,catalogData=radioData){
 const page=await browser.newPage();
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

async function fullApp({main=stations,catalog=main}={}){
 const page=await pageFor(undefined,main,catalog);await page.setViewportSize({width:390,height:844});
 await page.addInitScript(()=>{
  localStorage.setItem('p2Active','1');window.sentCommands=[];
  window.nativeState={nativeRecovery:true,serviceActive:true,isPlaying:false,manualPause:true,eq:[0,0,600,0,0],normalize:true,smooth:false,volume:.7};
  window.RadioNative={command:u=>window.sentCommands.push(u),getTelemetry:()=>JSON.stringify(window.nativeState),getNowTitle:()=>'',getRecentTracks:()=>'[]',getStreamHealth:()=>'{}',getProductGuard:()=>'{}',getCatalogHealth:()=>'{}',setQueue:()=>{},getNotaTextArchive:()=>'[]'};
 });
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
 await page.locator(value==='baz'?'[data-baz-profile]':'[data-prof="'+value+'"]').click();
 await page.waitForTimeout(1600);
}

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
  await page.evaluate(()=>{localStorage.setItem('favs','{broken');localStorage.setItem('recent','null')});
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
