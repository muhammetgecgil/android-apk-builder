from pathlib import Path

p=Path('app/src/v7/java/com/mg/battleship/GameActivity.java')
s=p.read_text(encoding='utf-8')

def rep(old,new,label):
    global s
    if old not in s:
        raise SystemExit('missing v15 patch target: '+label)
    s=s.replace(old,new,1)

# Add Undo button to manual deployment controls.
rep('private Button rotate,randomize,ready,sonarB,reconB;',
    'private Button rotate,randomize,undo,ready,sonarB,reconB;','undo-field')
rep('rotate=btn("Yön: Yatay");randomize=btn("Rastgele");ready=btn("Hazır");',
    'rotate=btn("Yön: Yatay");randomize=btn("Rastgele");undo=btn("Geri Al");ready=btn("Hazır");','undo-create')
rep('for(Button b:new Button[]{rotate,randomize,ready}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(54),1);lp.setMargins(dp(3),dp(4),dp(3),0);setupControls.addView(b,lp);}root.addView(setupControls);',
    'for(Button b:new Button[]{rotate,randomize,undo,ready}){b.setTextSize(13);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(54),1);lp.setMargins(dp(2),dp(4),dp(2),0);setupControls.addView(b,lp);}root.addView(setupControls);','undo-layout')
rep('randomize.setOnClickListener(v->{if(!localReady){board.randomizeOwn();status.setText("Filo konuşlandı. Hazır olduğunda savaşı başlat.");}});',
    'randomize.setOnClickListener(v->{if(!localReady){board.randomizeOwn();status.setText("Filo konuşlandı. İstersen Geri Al ile son gemiyi değiştir.");}});undo.setOnClickListener(v->{if(!localReady){if(board.undoLast()){status.setText("Son gemi geri alındı — yeniden yerleştir.");}else status.setText("Geri alınacak gemi yok.");}});','undo-action')

# Keep Undo state aligned with Ready state.
rep('localReady=true;ready.setEnabled(false);rotate.setEnabled(false);randomize.setEnabled(false);',
    'localReady=true;ready.setEnabled(false);rotate.setEnabled(false);randomize.setEnabled(false);if(undo!=null)undo.setEnabled(false);','ready-disable-undo')
rep('localReady=false;ready.setEnabled(true);rotate.setEnabled(true);randomize.setEnabled(true);status.setText("Rakip henüz bağlanmadı.");',
    'localReady=false;ready.setEnabled(true);rotate.setEnabled(true);randomize.setEnabled(true);if(undo!=null)undo.setEnabled(true);status.setText("Rakip henüz bağlanmadı.");','ready-enable-undo')

# Manual deployment undo: remove only the most recently placed ship.
rep('void place(int[][]b,int r,int c,int l,boolean h,int id){for(int i=0;i<l;i++)b[r+(h?0:i)][c+(h?i:0)]=id;}',
'''void place(int[][]b,int r,int c,int l,boolean h,int id){for(int i=0;i<l;i++)b[r+(h?0:i)][c+(h?i:0)]=id;}\n        boolean undoLast(){if(placed<=0||playing||localReady)return false;int id=placed-1;for(int r=0;r<10;r++)for(int c=0;c<10;c++)if(own[r][c]==id)own[r][c]=-1;placed--;invalidate();return true;}''','undo-method')

# Bluetooth gets tactical controls too. Sonar and Recon requests are resolved by the opponent device,
# so hidden fleet data never exists locally.
rep('if(tacticalBar!=null)tacticalBar.setVisibility(tacticalMode&&solo?View.VISIBLE:View.GONE);',
    'if(tacticalBar!=null)tacticalBar.setVisibility(tacticalMode?View.VISIBLE:View.GONE);','bt-tactical-bar')
rep('sonarB.setOnClickListener(v->{if(tacticalMode&&solo&&!sonarUsed&&board.playing&&myTurn){sonarArmed=true;status.setText("SONAR HAZIR — düşman denizinde merkez kare seç.");cinematic("◉  SONAR DARBESİ HAZIR",650);}});reconB.setOnClickListener(v->{if(tacticalMode&&solo&&!reconUsed&&board.playing&&myTurn)useRecon();});',
'''sonarB.setOnClickListener(v->{if(tacticalMode&&!sonarUsed&&board.playing&&myTurn){sonarArmed=true;status.setText("SONAR HAZIR — düşman denizinde merkez kare seç.");cinematic("◉  SONAR DARBESİ HAZIR",650);}});reconB.setOnClickListener(v->{if(tacticalMode&&!reconUsed&&board.playing&&myTurn){if(solo)useRecon();else if(connected){reconUsed=true;reconB.setEnabled(false);reconB.setText("KEŞİF KULLANILDI");link.send("RECON");status.setText("KEŞİF UÇAĞI GÖNDERİLDİ — temas bekleniyor…");}}});''','bt-tactical-actions')

# Sonar touch works in Bluetooth as well.
rep('if(tacticalMode&&solo&&sonarArmed){useSonar(r,c);return true;}',
'''if(tacticalMode&&sonarArmed){if(solo)useSonar(r,c);else if(connected){sonarUsed=true;sonarArmed=false;if(sonarB!=null){sonarB.setEnabled(false);sonarB.setText("SONAR KULLANILDI");}link.send("SONAR|"+r+"|"+c);status.setText("SONAR DARBESİ GÖNDERİLDİ — sonuç bekleniyor…");}return true;}''','bt-sonar-touch')

# Bluetooth turn protocol: receiver waits for PASS/KEEP after RESULT. This lets the same
# critical 3-hit extra-shot mechanic work fairly in Bluetooth without leaking fleet state.
old_bt='''case"SHOT":{int r=Integer.parseInt(a[1]),c=Integer.parseInt(a[2]),shipId=board.own[r][c],res=board.receiveOwnShot(r,c);boolean sunk=res==2&&shipId>=0&&board.isOwnShipSunk(shipId);link.send("RESULT|"+r+"|"+c+"|"+res+"|"+shipId+"|"+(sunk?1:0));board.flash(r,c,true,res==2);if(sunk){board.startSinking(shipId,true);main.postDelayed(()->BattleEffects.sunk(this),140);}if(board.allOwnSunk()){link.send("GAMEOVER");board.gameOver=true;updateHud();finishMatch(false);}else{myTurn=true;status.setText(sunk?AdvancedShipRenderer.name(shipId)+" GEMİMİZ BATTI!  •  SIRA SENDE":"SIRA SENDE — hedef seç.");updateHud();}break;}case"RESULT":{int r=Integer.parseInt(a[1]),c=Integer.parseInt(a[2]),res=Integer.parseInt(a[3]);board.enemyMarks[r][c]=res;matchShots++;if(res==2)matchHits++;recordShot(res==2);if(res==2)BattleEffects.hit(this);else BattleEffects.miss(this);board.flash(r,c,false,res==2);boolean sunk=a.length>5&&"1".equals(a[5]);int shipId=a.length>4?Integer.parseInt(a[4]):-1;if(sunk){enemySunkCount++;main.postDelayed(()->BattleEffects.sunk(this),140);status.setText((shipId>=0?AdvancedShipRenderer.name(shipId):"DÜŞMAN GEMİSİ")+" BATIRILDI!  •  Rakibin sırası…");}else status.setText(res==2?"VURUŞ! Rakibin sırası…":"ISKALANDI — Rakibin sırası…");updateHud();break;}case"GAMEOVER":board.gameOver=true;finishMatch(true);break;'''
new_bt='''case"SHOT":{int r=Integer.parseInt(a[1]),c=Integer.parseInt(a[2]),shipId=board.own[r][c],res=board.receiveOwnShot(r,c);boolean sunk=res==2&&shipId>=0&&board.isOwnShipSunk(shipId);link.send("RESULT|"+r+"|"+c+"|"+res+"|"+shipId+"|"+(sunk?1:0));board.flash(r,c,true,res==2);if(res==2)cinematic("⚠  GEMİMİZ İSABET ALDI",650);else cinematic("◎  RAKİP ATIŞI ISKALADI",480);if(sunk){board.startSinking(shipId,true);main.postDelayed(()->BattleEffects.sunk(this),140);}if(board.allOwnSunk()){link.send("GAMEOVER");board.gameOver=true;updateHud();finishMatch(false);}else{myTurn=false;status.setText(sunk?AdvancedShipRenderer.name(shipId)+" GEMİMİZ BATTI — rakip tur kararı bekleniyor…":"ATIŞ SONUCU GÖNDERİLDİ — rakip tur kararı bekleniyor…");updateHud();}break;}case"RESULT":{int r=Integer.parseInt(a[1]),c=Integer.parseInt(a[2]),res=Integer.parseInt(a[3]);board.enemyMarks[r][c]=res;matchShots++;if(res==2)matchHits++;recordShot(res==2);if(res==2)BattleEffects.hit(this);else BattleEffects.miss(this);board.flash(r,c,false,res==2);boolean sunk=a.length>5&&"1".equals(a[5]);int shipId=a.length>4?Integer.parseInt(a[4]):-1;if(sunk)enemySunkCount++;notePlayerResult(res==2,sunk,shipId);if(sunk){main.postDelayed(()->BattleEffects.sunk(this),140);status.setText((shipId>=0?AdvancedShipRenderer.name(shipId):"DÜŞMAN GEMİSİ")+" BATIRILDI!");}boolean extra=res==2&&!sunk&&hitStreak>0&&hitStreak%3==0;if(extra){myTurn=true;link.send("KEEP");status.setText("KRİTİK TAKTİK ÜSTÜNLÜK — EK ATIŞ HAKKI");cinematic("⚡  ATIŞ ÜSTÜNLÜĞÜ  •  EK HEDEF",1200);}else{myTurn=false;link.send("PASS");if(!sunk)status.setText(res==2?"VURUŞ! Rakibin sırası…":"ISKALANDI — Rakibin sırası…");}updateHud();break;}case"PASS":myTurn=true;status.setText("SIRA SENDE — hedef seç.");updateHud();break;case"KEEP":myTurn=false;status.setText("RAKİP KRİTİK ÜSTÜNLÜK KAZANDI — yeniden ateş edecek.");updateHud();break;case"SONAR":{int r=Integer.parseInt(a[1]),c=Integer.parseInt(a[2]),count=0;for(int rr=Math.max(0,r-1);rr<=Math.min(9,r+1);rr++)for(int cc=Math.max(0,c-1);cc<=Math.min(9,c+1);cc++)if(board.own[rr][cc]>=0)count++;link.send("SONAR_RESULT|"+count);break;}case"SONAR_RESULT":{int count=Integer.parseInt(a[1]);cinematic("◉  SONAR: 3×3 BÖLGEDE "+count+" GÖVDE İZİ",1250);status.setText("Sonar taraması tamamlandı — sıran devam ediyor.");break;}case"RECON":{List<int[]> q=new ArrayList<>();for(int r=0;r<10;r++)for(int c=0;c<10;c++)if(board.own[r][c]>=0&&!board.hit[r][c])q.add(new int[]{r,c});if(q.isEmpty())link.send("RECON_RESULT|-1|-1");else{int[] z=q.get(rnd.nextInt(q.size()));link.send("RECON_RESULT|"+z[0]+"|"+z[1]);}break;}case"RECON_RESULT":{int r=Integer.parseInt(a[1]),c=Integer.parseInt(a[2]);if(r>=0){board.intelR=r;board.intelC=c;board.intelUntil=System.currentTimeMillis()+2600;board.invalidate();cinematic("⌖  KEŞİF TEMASI — "+(char)('A'+c)+(r+1),1500);status.setText("Keşif olası düşman bölümünü işaretledi — sıran devam ediyor.");}else cinematic("⌖  KEŞİF: YENİ HEDEF YOK",700);break;}case"GAMEOVER":board.gameOver=true;finishMatch(true);break;'''
rep(old_bt,new_bt,'bt-full-parity')

# Bluetooth launches Tactical Mode too, instead of forcing classic-only mode.
rep('btB.setOnClickListener(v->{tacticalMode=false;startBattle(false,false,2);});',
    'btB.setOnClickListener(v->{tacticalMode=true;startBattle(false,false,2);});','bt-enable-tactical')

# v15 footer identity.
rep('TextView footer=label("v13 • COMBAT COMMAND • Adaptive HUD • Critical Turn • Threat Vector • Tactical AI",11,Color.rgb(112,151,164));',
    'TextView footer=label("v15 • BLUETOOTH PARITY • Undo Deployment • Sonar • Recon • Critical Turn",11,Color.rgb(112,151,164));','footer')

p.write_text(s,encoding='utf-8')
print('v15 bluetooth parity + undo patch applied')
