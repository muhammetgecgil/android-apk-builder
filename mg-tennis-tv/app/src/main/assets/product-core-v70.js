(function(){'use strict';
window.MGPC1=window.MGPC1||{};
const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));let current=1;
function activeGame(){return !document.getElementById('mg41')&&document.body.dataset.mgProductMode&&document.body.dataset.mgProductMode!=='racket'}
function incomingFromOpponent(){try{if(!window.ball||!window.ballVel)return false;const h=String(typeof lastHitter!=='undefined'?lastHitter:'');return h==='opponent'&&ballVel.z>0.05}catch(e){return false}}
function targetScale(){try{if(!activeGame()||!window.ball)return 1;if(!incomingFromOpponent())return 1;const z=Number(ball.position.z)||0;/* player is near +9.55: progressively enlarge opponent ball as it approaches */const progress=clamp((z+1.5)/10.5,0,1);let s=1+progress*0.82;/* extra readability in final reaction zone */if(z>5.6)s+=clamp((z-5.6)/3.6,0,1)*0.28;return clamp(s,1,2.08)}catch(e){return 1}}
function tick(){try{if(window.ball&&ball.scale){const t=targetScale();current+= (t-current)*(t>current?.20:.13);ball.scale.setScalar(current);MGPC1.incomingBallScale=Number(current.toFixed(2));MGPC1.incomingBallVisual='DYNAMIC_APPROACH_SCALE'}}catch(e){}requestAnimationFrame(tick)}
MGPC1.version='7.0.0';MGPC1.productTrack='INCOMING_BALL_VISIBILITY';requestAnimationFrame(tick);
})();
