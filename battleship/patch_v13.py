from pathlib import Path

p=Path('app/src/v7/java/com/mg/battleship/GameActivity.java')
s=p.read_text(encoding='utf-8')

def rep(old,new,label):
    global s
    if old not in s:
        raise SystemExit('missing v13 patch target: '+label)
    s=s.replace(old,new,1)

rep('private boolean solo=false,career=false,connected=false,localReady=false,remoteReady=false,myTurn=false,tacticalMode=false,sonarUsed=false,reconUsed=false,sonarArmed=false;',
    'private boolean solo=false,career=false,connected=false,localReady=false,remoteReady=false,myTurn=false,tacticalMode=false,sonarUsed=false,reconUsed=false,sonarArmed=false;\n    private String threatText="TEHDİT: —";','threat-field')

rep('aiTargets.clear();sonarUsed=reconUsed=sonarArmed=false;matchShots=matchHits=enemySunkCount=hitStreak=bestStreak=0;',
    'aiTargets.clear();sonarUsed=reconUsed=sonarArmed=false;threatText="TEHDİT: —";matchShots=matchHits=enemySunkCount=hitStreak=bestStreak=0;','reset-threat')

rep('battleHud=label("",12,Color.rgb(255,221,116));battleHud.setVisibility(View.GONE);battleHud.setBackground(panel(Color.rgb(7,31,42),Color.rgb(57,101,112),10));root.addView(battleHud,new LinearLayout.LayoutParams(-1,dp(36)));',
'''battleHud=label("",11,Color.rgb(255,221,116));battleHud.setVisibility(View.GONE);battleHud.setBackgroundColor(Color.TRANSPARENT);battleHud.setMaxLines(2);battleHud.setSingleLine(false);battleHud.setGravity(Gravity.CENTER);battleHud.setPadding(dp(2),dp(2),dp(2),dp(2));root.addView(battleHud,new LinearLayout.LayoutParams(-1,dp(48)));''','adaptive-hud')

rep('int dmg=board.ownDamagePercent();battleHud.setText(turn+"   •   GEMİ: "+mine+"   •   DÜŞMAN: "+enemy+"   •   HASAR %"+dmg+"   •   İSABET %"+String.format(java.util.Locale.US,"%.0f",acc)+"   •   SERİ "+hitStreak);',
r'''int dmg=board.ownDamagePercent();String alarm=dmg>=70?"KIRMIZI ALARM":(dmg>=40?"HASAR UYARISI":"SİSTEM NORMAL");battleHud.setText(turn+"  •  GEMİ "+mine+"  •  DÜŞMAN "+enemy+"\nHASAR %"+dmg+"  •  İSABET %"+String.format(java.util.Locale.US,"%.0f",acc)+"  •  SERİ ×"+hitStreak+"  •  "+threatText+"  •  "+alarm);''','hud-two-lines')

rep('c.drawText(solo?"DÜŞMAN FİLOSU":"RAKİP DENİZİ",getWidth()/2f,t2-dp(8),p);drawGrid(c,left,t2,cell,false);',
    'c.drawText(solo?"DÜŞMAN FİLOSU":"RAKİP DENİZİ",getWidth()/2f,t2-dp(20),p);drawGrid(c,left,t2,cell,false);','enemy-title-spacing')

needle='''if(!sunk)status.setText(res==2?"VURUŞ!  •  "+DIFF[aiLevel]+" HEDEFLİYOR…":"ISKALANDI  •  "+DIFF[aiLevel]+" HEDEFLİYOR…");updateHud();\n        main.postDelayed(this::aiShot,sunk?1050:(aiLevel>=3?520:720));'''
replacement='''if(!sunk)status.setText(res==2?"VURUŞ!  •  "+DIFF[aiLevel]+" HEDEFLİYOR…":"ISKALANDI  •  "+DIFF[aiLevel]+" HEDEFLİYOR…");\n        if(res==2&&!sunk&&hitStreak>0&&hitStreak%3==0){myTurn=true;status.setText("KRİTİK TAKTİK ÜSTÜNLÜK — EK ATIŞ HAKKI");cinematic("⚡  ATIŞ ÜSTÜNLÜĞÜ  •  EK HEDEF",1200);updateHud();return;}updateHud();\n        main.postDelayed(this::aiShot,sunk?1050:(aiLevel>=3?520:720));'''
rep(needle,replacement,'critical-extra-shot')

rep('if(!solo||board.gameOver||myTurn)return;int[] q=pickAi();if(q==null){myTurn=true;restoreTurnPrompt();return;}int shipId=board.own[q[0]][q[1]];',
'''if(!solo||board.gameOver||myTurn)return;int[] q=pickAi();if(q==null){myTurn=true;restoreTurnPrompt();return;}String ns=q[0]<5?"KUZEY":"GÜNEY";String ew=q[1]<5?"BATI":"DOĞU";threatText="TEHDİT: "+ns+"-"+ew;int shipId=board.own[q[0]][q[1]];''','threat-direction')

rep('if(aiLevel>=3){if((r+c)%2==0)free.add(new int[]{r,c});}else free.add(new int[]{r,c});',
'''if(aiLevel>=4){boolean parity=(r+c)%2==0;boolean pressure=lastAiHitR>=0&&(Math.abs(r-lastAiHitR)+Math.abs(c-lastAiHitC)<=4);if(parity||pressure)free.add(new int[]{r,c});}else if(aiLevel>=3){if((r+c)%2==0)free.add(new int[]{r,c});}else free.add(new int[]{r,c});''','adaptive-ai')

rep('TextView footer=label("v12 • FLAGSHIP COMMAND CENTER • Sonar • Recon • Missions • Achievements",11,Color.rgb(112,151,164));',
    'TextView footer=label("v13 • COMBAT COMMAND • Adaptive HUD • Critical Turn • Threat Vector • Tactical AI",11,Color.rgb(112,151,164));','footer')

p.write_text(s,encoding='utf-8')
print('v13 combat command patch applied')
