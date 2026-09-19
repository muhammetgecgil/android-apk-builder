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
async function pageFor(html='<!doctype html><body></body>'){
 const page=await browser.newPage();
 await page.route('**/*',async route=>{
  const url=route.request().url();
  if(url===A+'fixture.html')return route.fulfill({contentType:'text/html',body:html});
  if(url.startsWith(A)){
   try{const file=new URL(url).pathname.replace('/assets/','');return route.fulfill({contentType:file.endsWith('.css')?'text/css':file.endsWith('.js')?'application/javascript':file.endsWith('.html')?'text/html':'application/json',body:source(ASSETS+file)})}catch{return route.fulfill({status:404,body:''})}
  }
  if(url.includes('api.radio-browser.info'))return route.fulfill({contentType:'application/json',body:JSON.stringify(stations)});
  return route.abort();
 });
 await page.goto(A+'fixture.html');
 return page;
}
async function script(page,name){await page.addScriptTag({content:source(ASSETS+name)})}

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
