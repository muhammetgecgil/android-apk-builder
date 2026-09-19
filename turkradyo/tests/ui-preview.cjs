const fs=require('node:fs'),path=require('node:path'),{execFileSync}=require('node:child_process');
const {chromium}=require('playwright');
const ROOT=path.resolve(__dirname,'../..'),A='https://appassets.androidplatform.net/assets/';
const out=process.env.UI_CAPTURE_DIR||'/tmp/radio-ui-preview';fs.mkdirSync(out,{recursive:true});
function source(p){return process.env.SOURCE_REF?execFileSync('git',['show',process.env.SOURCE_REF+':'+p],{cwd:ROOT,encoding:'utf8'}):fs.readFileSync(path.join(ROOT,p),'utf8')}
const names=['Power Türk','TRT FM','Radyo Voyage','Slow Türk','Kral Pop','Radyo D','Joy FM','Radyo Eksen'];
const stations=names.map((name,i)=>({name,stationuuid:'preview-'+i,url:'https://stream.test/'+i,url_resolved:'https://stream.test/'+i,bitrate:128,lastcheckok:1,clickcount:3000-i*100,tags:i===2||i===3?'slow,jazz':'pop',language:'turkish',countrycode:'TR'}));
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.CHROME_PATH?{executablePath:process.env.CHROME_PATH}:{})});
 const page=await browser.newPage({viewport:{width:412,height:915},deviceScaleFactor:1});const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.route('**/*',async route=>{const url=route.request().url();if(url.startsWith(A)){try{const name=new URL(url).pathname.replace('/assets/','');return route.fulfill({contentType:name.endsWith('.css')?'text/css':name.endsWith('.js')?'application/javascript':name.endsWith('.html')?'text/html':'application/json',body:source('turkradyo/app/src/main/assets/'+name)})}catch{return route.fulfill({status:404,body:''})}}if(url.includes('api.radio-browser.info'))return route.fulfill({contentType:'application/json',body:JSON.stringify(stations)});return route.abort()});
 await page.addInitScript(()=>{localStorage.setItem('p2Active','1');window.RadioNative={getTelemetry:()=>JSON.stringify({nativeRecovery:true,serviceActive:true,isPlaying:false,manualPause:true,playerState:3,networkType:'wifi',volume:0.75,station:'Power Türk',primaryUrl:'https://stream.test/0'}),getNowTitle:()=>'',getRecentTracks:()=>'[]',getStreamHealth:()=>'{}',getProductGuard:()=>'{}',getCatalogHealth:()=>'{}',getNotaAnalysis:()=>'{"state":"IDLE"}',getNotaTextArchive:()=>'[]',setQueue:()=>{}}});
 await page.goto(A+'index.html');const java=source('turkradyo/app/src/main/java/com/muhammetgecgil/turkradyo/MainActivity.java');const raw=java.match(/v\.evaluateJavascript\("(.*)",null\);/)[1].replaceAll('"+A+"',A);await page.evaluate(JSON.parse('"'+raw+'"'));await page.waitForTimeout(6500);
 const shot=async name=>{await page.screenshot({path:path.join(out,name+'.png'),fullPage:true,animations:'disabled'})};
 await shot('profile2-home');
 const buttons=await page.locator('button').evaluateAll(a=>a.filter(e=>e.getBoundingClientRect().height>0).map(e=>({id:e.id,text:e.textContent.trim(),mode:e.dataset.mode,rect:{x:e.getBoundingClientRect().x,y:e.getBoundingClientRect().y,width:e.getBoundingClientRect().width,height:e.getBoundingClientRect().height}})));
 await page.locator('#settingsBtn').click();await page.waitForTimeout(500);await shot('settings');await page.locator('#closeSheet').click();
 await page.locator('#p2Sleep').click();await page.waitForTimeout(300);await shot('timer');await page.locator('#closeSheet').click();
 await page.locator('.nature-profile-pill').click();await page.waitForTimeout(250);await page.locator('[data-prof="1"]').click();await page.waitForTimeout(1300);await shot('profile1-home');
 await page.setViewportSize({width:360,height:800});await shot('profile1-small');
 fs.writeFileSync(path.join(out,'audit.json'),JSON.stringify({errors,buttons},null,2));await browser.close();
})().catch(e=>{console.error(e);process.exitCode=1});
