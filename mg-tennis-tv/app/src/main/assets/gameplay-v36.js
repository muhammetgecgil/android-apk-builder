(function(){'use strict';
function easyAssist(){try{if(!window.MGDifficulty||!ballVel||MGDifficulty.current.key!=='easy')return;if(window.lastHitter==='player'&&ballVel.z<-.2){ballVel.x*=.72;ballVel.z=Math.max(-10.2,Math.min(-7.1,ballVel.z));if(ballVel.y<1.45)ballVel.y=1.45;const x=ball.position.x;if(Math.abs(x)>3.8)ballVel.x+=(-x)*.12}}catch(e){}}
function fixSeats(){try{if(!window.scene)return;scene.traverse(o=>{const n=(o.name||'').toLowerCase();if(!/seat|chair|tribune|bleacher|koltuk/.test(n))return;const wp=new THREE.Vector3();o.getWorldPosition(wp);if(wp.x<-.5){o.lookAt(new THREE.Vector3(0,wp.y,-1));}})}catch(e){}}
let done=false;function tick(){easyAssist();if(!done){fixSeats();done=true;setTimeout(()=>{done=false},2500)}requestAnimationFrame(tick)}
requestAnimationFrame(tick);
})();