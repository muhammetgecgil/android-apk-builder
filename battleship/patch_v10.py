from pathlib import Path

p=Path('app/src/v7/java/com/mg/battleship/GameActivity.java')
s=p.read_text(encoding='utf-8')

def rep(old,new,label):
    global s
    if old not in s:
        raise SystemExit('missing v10 patch target: '+label)
    s=s.replace(old,new,1)

rep('private int matchShots=0,matchHits=0,enemySunkCount=0,hitStreak=0,bestStreak=0;\n    private int lastAiHitR=-1,lastAiHitC=-1,prevAiHitR=-1,prevAiHitC=-1;',
    'private int matchShots=0,matchHits=0,enemySunkCount=0,hitStreak=0,bestStreak=0;\n    private long matchStartTime=0;\n    private int lastAiHitR=-1,lastAiHitC=-1,prevAiHitR=-1,prevAiHitC=-1;','fields')

rep('TextView footer=label("v9 • CINEMATIC NAVAL WARFARE • Taktik AI • Dinamik deniz • Batış sahneleri",11,Color.rgb(120,145,155));',
    'TextView footer=label("v10 • TACTICAL DAMAGE • Kritik vuruş • Savaş raporu • Sinematik deniz",11,Color.rgb(120,145,155));','footer')

rep('solo=soloMode;career=careerMode;aiLevel=difficulty;connected=false;localReady=remoteReady=myTurn=false;aiTargets.clear();matchShots=matchHits=enemySunkCount=hitStreak=bestStreak=0;lastAiHitR=lastAiHitC=prevAiHitR=prevAiHitC=-1;',
    'solo=soloMode;career=careerMode;aiLevel=difficulty;connected=false;localReady=remoteReady=myTurn=false;aiTargets.clear();matchShots=matchHits=enemySunkCount=hitStreak=bestStreak=0;matchStartTime=System.currentTimeMillis();lastAiHitR=lastAiHitC=prevAiHitR=prevAiHitC=-1;','starttime')

rep('battleHud.setText(turn+"   •   GEMİ: "+mine+"   •   DÜŞMAN: "+enemy+"   •   İSABET %"+String.format(java.util.Locale.US,"%.0f",acc)+"   •   SERİ "+hitStreak);',
    'int dmg=board.ownDamagePercent();battleHud.setText(turn+"   •   GEMİ: "+mine+"   •   DÜŞMAN: "+enemy+"   •   HASAR %"+dmg+"   •   İSABET %"+String.format(java.util.Locale.US,"%.0f",acc)+"   •   SERİ "+hitStreak);','huddamage')

rep('private void notePlayerResult(boolean hit,boolean sunk,int shipId){if(hit){hitStreak++;bestStreak=Math.max(bestStreak,hitStreak);cinematic(sunk?"⚓  "+AdvancedShipRenderer.name(shipId)+"  •  BATIRILDI":"✹  DOĞRUDAN İSABET  •  SERİ "+hitStreak,sunk?1450:650);}else{hitStreak=0;cinematic("≈  SUYA DÜŞTÜ  •  ISKA",520);}updateHud();}',
'''private void notePlayerResult(boolean hit,boolean sunk,int shipId){if(hit){hitStreak++;bestStreak=Math.max(bestStreak,hitStreak);if(sunk)cinematic("⚓  "+AdvancedShipRenderer.name(shipId)+"  •  BATIRILDI",1550);else if(hitStreak>=3)cinematic("⚠  KRİTİK İSABET SERİSİ  ×"+hitStreak,900);else cinematic("✹  DOĞRUDAN İSABET  •  SERİ "+hitStreak,650);}else{hitStreak=0;cinematic("≈  SUYA DÜŞTÜ  •  ISKA",520);}updateHud();}''','critical')

old='''root.addView(label("Rütbe: "+rank()+"\\nXP: "+xp()+"\\nGalibiyet: "+wins()+"\\nİsabet: "+hits()+" / "+shots(),19,Color.WHITE),new LinearLayout.LayoutParams(-1,0,1));'''
new='''long sec=Math.max(1,(System.currentTimeMillis()-matchStartTime)/1000);float ma=matchShots==0?0f:100f*matchHits/matchShots;String report="Rütbe: "+rank()+"\\nXP: "+xp()+"\\n\\nBU MUHAREBE\\nAtış: "+matchShots+"   İsabet: "+matchHits+"   İsabet %"+String.format(java.util.Locale.US,"%.1f",ma)+"\\nEn iyi seri: "+bestStreak+"   Batırılan: "+enemySunkCount+"\\nSüre: "+(sec/60)+" dk "+(sec%60)+" sn\\n\\nKARİYER\\nGalibiyet: "+wins()+"   Toplam isabet: "+hits()+" / "+shots();root.addView(label(report,18,Color.WHITE),new LinearLayout.LayoutParams(-1,0,1));'''
rep(old,new,'report')

rep('boolean allOwnSunk(){return aliveOwnShips()==0;}boolean allAiSunk(){return aliveAiShips()==0;}\n        void startSinking',
'''int ownDamagePercent(){int total=0,damaged=0;for(int r=0;r<10;r++)for(int c=0;c<10;c++)if(own[r][c]>=0){total++;if(hit[r][c])damaged++;}return total==0?0:Math.round(100f*damaged/total);}\n        boolean allOwnSunk(){return aliveOwnShips()==0;}boolean allAiSunk(){return aliveAiShips()==0;}\n        void startSinking''','damage-method')

# Add animated smoke/fire above damaged friendly ship cells.
needle='''if(good){p.setColor(Color.rgb(255,78,40));c.drawCircle(cx,cy,cell*.24f,p);p.setColor(Color.rgb(255,210,90));c.drawCircle(cx,cy,cell*.10f,p);}else{'''
replacement='''if(good){p.setColor(Color.rgb(255,78,40));c.drawCircle(cx,cy,cell*.24f,p);p.setColor(Color.rgb(255,210,90));c.drawCircle(cx,cy,cell*.10f,p);if(mine){float pulse=.5f+.5f*(float)Math.sin(System.currentTimeMillis()/150.0+r*1.7+col);p.setColor(Color.argb(110,35,35,35));c.drawCircle(cx+cell*.10f,cy-cell*(.25f+.08f*pulse),cell*(.15f+.05f*pulse),p);p.setColor(Color.argb(150,255,105,25));c.drawCircle(cx-cell*.06f,cy-cell*.12f,cell*(.08f+.03f*pulse),p);postInvalidateDelayed(100);}}else{'''
rep(needle,replacement,'damagefx')

p.write_text(s,encoding='utf-8')
print('v10 tactical damage patch applied')
