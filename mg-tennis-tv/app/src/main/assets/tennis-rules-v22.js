(function(){
'use strict';
// MG Tennis TV v2.2 — deterministic tennis rules layer.
let match={setsP:0,setsO:0,setGamesP:0,setGamesO:0,server:'opponent',firstServer:'opponent',serveNumber:1,pointLive:false,tiebreak:false,tbP:0,tbO:0,matchOver:false};
let serveTimer=null,lastPointAt=0;
function status(t){const e=document.getElementById('status');if(e)e.textContent=t}
function scoreEl(){return document.getElementById('scoreMain')}
function subEl(){return document.getElementById('scoreSub')}
function pLabel(a,b){if(match.tiebreak)return String(a);if(a<3)return['0','15','30'][a];if(a===3&&b<=3)return'40';if(a>=3&&b>=3){if(a===b)return'40';if(a===b+1)return'AD';return'40'}return'40'}
function renderScore(){
  const s=scoreEl(),sub=subEl();if(!s||!sub)return;
  const lp=match.tiebreak?match.tbP:pointsP, lo=match.tiebreak?match.tbO:pointsO;
  s.textContent=match.setsP+'S '+match.setGamesP+' · '+pLabel(lp,lo)+' — '+pLabel(lo,lp)+' · '+match.setGamesO+' '+match.setsO+'S';
  sub.textContent=(match.server==='player'?'● SERVİS SENDE':'● SERVİS RAKİPTE')+' • '+(match.tiebreak?'TIE-BREAK':'GERÇEK TENİS KURALI')+' • '+(match.serveNumber===2?'2. SERVİS':'1. SERVİS');
}
function clearServeTimer(){if(serveTimer){clearTimeout(serveTimer);serveTimer=null}}
function placeForServe(){
  ballVel.set(0,0,0);ballSpin.set(0,0,0);bounceNear=bounceFar=0;pendingLocal=pendingRemote=null;ballTrail=[];
  if(match.server==='player'){
    serving=true; ball.position.set(playerX-.20,EYE_H-.18,playerZ-.58);status(match.serveNumber===1?'Servis sende • topu at ve vur':'İkinci servis • topu oyuna sok');
  }else{
    serving=false; ball.position.set(opponentX+.18,1.78,opponentZ+.55);status(match.serveNumber===1?'Rakip servis hazırlanıyor…':'Rakibin ikinci servisi…');scheduleAiServe();
  }
  renderScore();
}
function scheduleAiServe(){clearServeTimer();serveTimer=setTimeout(aiServe,850+Math.random()*550)}
function aiServe(){
  if(match.matchOver||match.server!=='opponent'||match.pointLive)return;
  const deuce=((pointsP+pointsO)%2===0), targetX=deuce?-2.0:2.0, T=1.18+Math.random()*.16, g=9.81, targetZ=6.0;
  ball.position.set(opponentX+(deuce?.35:-.35),1.92,opponentZ+.50);
  const vx=(targetX-ball.position.x)/T, vz=(targetZ-ball.position.z)/T, vy=(BALL_R-ball.position.y+.5*g*T*T)/T;
  ballVel.set(vx,vy,vz);ballSpin.set(-19,(deuce?-1:1)*5,0);lastHitter='opponent';opponentSwingPhase=.001;match.pointLive=true;bounceNear=bounceFar=0;
  status('Rakip servis attı • topu izle ve karşıla');
}
function fault(server){
  if(match.serveNumber===1){match.serveNumber=2;match.pointLive=false;status((server==='player'?'Hata':'Rakip hata')+' • ikinci servis');setTimeout(placeForServe,650)}
  else{match.serveNumber=1;match.pointLive=false;award(server==='player'?'opponent':'player','Çift hata')}
}
function gameWon(w){
  if(w==='player')match.setGamesP++;else match.setGamesO++;
  pointsP=pointsO=0;match.serveNumber=1;match.pointLive=false;
  if(match.tiebreak){match.tiebreak=false;match.tbP=match.tbO=0}
  const gp=match.setGamesP,go=match.setGamesO;
  if((gp>=6||go>=6)&&Math.abs(gp-go)>=2){setWon(gp>go?'player':'opponent');return}
  if(gp===6&&go===6){match.tiebreak=true;match.tbP=match.tbO=0}
  match.server=match.server==='player'?'opponent':'player';setTimeout(placeForServe,950);renderScore();
}
function setWon(w){
  if(w==='player')match.setsP++;else match.setsO++;
  match.setGamesP=match.setGamesO=0;pointsP=pointsO=0;match.tbP=match.tbO=0;match.tiebreak=false;
  if(match.setsP>=2||match.setsO>=2){match.matchOver=true;clearServeTimer();ballVel.set(0,0,0);status(match.setsP>match.setsO?'MAÇ SENİN!':'MAÇ RAKİBİN');renderScore();return}
  match.server=match.server==='player'?'opponent':'player';setTimeout(placeForServe,1200);renderScore();
}
function award(w,reason){
  const now=performance.now();if(now-lastPointAt<350)return;lastPointAt=now;match.pointLive=false;ballVel.set(0,0,0);
  if(match.tiebreak){if(w==='player')match.tbP++;else match.tbO++;const a=match.tbP,b=match.tbO;if((a>=7||b>=7)&&Math.abs(a-b)>=2){const winner=a>b?'player':'opponent';match.setGamesP=winner==='player'?7:6;match.setGamesO=winner==='opponent'?7:6;setWon(winner);return}const total=a+b;if(total>0&&(total===1||total%2===1))match.server=match.server==='player'?'opponent':'player';status((reason?reason+' • ':'')+(w==='player'?'Sayı senin':'Sayı rakibin'));setTimeout(placeForServe,850);renderScore();return}
  if(w==='player')pointsP++;else pointsO++;
  if((pointsP>=4||pointsO>=4)&&Math.abs(pointsP-pointsO)>=2){status((reason?reason+' • ':'')+(w==='player'?'Game senin':'Game rakibin'));gameWon(w);return}
  status((reason?reason+' • ':'')+(w==='player'?'Sayı senin':'Sayı rakibin'));match.serveNumber=1;setTimeout(placeForServe,850);renderScore();
}
// Override legacy point/reset/score functions.
window.pointTo=function(w){award(w,'')};
window.updateScore=renderScore;
window.resetPoint=function(){match.pointLive=false;match.serveNumber=1;placeForServe()};
// Local player serve still uses the original swing, but marks point live.
const oldServe=window.serve;
window.serve=function(power,dir){if(match.server!=='player'||match.matchOver)return;if(oldServe)oldServe(power,dir);match.pointLive=true;match.serveNumber=1;status('Servis oyunda • ralli')};
// Rule monitor: out, double bounce and serve faults. Uses singles court width 8.23 m.
let prevY=1,prevVy=0,pointStart=0;
setInterval(()=>{
  if(!ball||match.matchOver)return;
  if(match.pointLive&&pointStart===0)pointStart=performance.now();if(!match.pointLive)pointStart=0;
  const x=Math.abs(ball.position.x),z=Math.abs(ball.position.z),y=ball.position.y;
  const landed=prevY>BALL_R+.02&&y<=BALL_R+.06&&prevVy<0;
  if(match.pointLive&&landed){
    const singlesOut=x>4.115||Math.abs(z)>HALF_L+.08;
    if(singlesOut){const loser=lastHitter;award(loser==='player'?'opponent':'player','OUT');prevY=y;prevVy=ballVel.y;return}
    // Service must land in opposite service box.
    if(pointStart&&performance.now()-pointStart<1600){
      if(lastHitter===match.server){const serviceDepth=Math.abs(z)<=6.40+.12, correctHalf=(match.server==='player'?z<0:z>0);if(!(serviceDepth&&correctHalf)){fault(match.server);prevY=y;prevVy=ballVel.y;return}}
    }
    if(z>0){bounceNear++;if(bounceNear>=2)award('opponent','İki sekme')}else{bounceFar++;if(bounceFar>=2)award('player','İki sekme')}
  }
  // Ball that dies into/under net after a hit.
  if(match.pointLive&&Math.abs(z)<.22&&y<.93&&Math.abs(ballVel.z)<6){award(lastHitter==='player'?'opponent':'player','NET')}
  prevY=y;prevVy=ballVel.y;
},25);
window.MGTennisRules={match,award,fault,reset:()=>{match={setsP:0,setsO:0,setGamesP:0,setGamesO:0,server:'opponent',firstServer:'opponent',serveNumber:1,pointLive:false,tiebreak:false,tbP:0,tbO:0,matchOver:false};pointsP=pointsO=gamesP=gamesO=0;placeForServe()}};
setTimeout(()=>{pointsP=pointsO=gamesP=gamesO=0;match.server='opponent';placeForServe();status('Maç başladı • rakip ilk serviste')},1800);
})();
