from pathlib import Path

p=Path('app/src/v7/java/com/mg/battleship/GameActivity.java')
s=p.read_text(encoding='utf-8')

def rep(old,new,label):
    global s
    if old not in s:
        raise SystemExit('missing v9 patch target: '+label)
    s=s.replace(old,new,1)

rep('private TextView status,rankText,battleHud;',
    'private TextView status,rankText,battleHud,cinematicBanner;','banner-field')
rep('private int matchShots=0,matchHits=0,enemySunkCount=0;',
    'private int matchShots=0,matchHits=0,enemySunkCount=0,hitStreak=0,bestStreak=0;\n    private int lastAiHitR=-1,lastAiHitC=-1,prevAiHitR=-1,prevAiHitC=-1;','combat-fields')
rep('TextView footer=label("v8 • Kesintisiz savaş • Batış efektleri • Canlı HUD • Gelişmiş AI",11,Color.rgb(120,145,155));',
    'TextView footer=label("v9 • CINEMATIC NAVAL WARFARE • Taktik AI • Dinamik deniz • Batış sahneleri",11,Color.rgb(120,145,155));','footer')
rep('solo=soloMode;career=careerMode;aiLevel=difficulty;connected=false;localReady=remoteReady=myTurn=false;aiTargets.clear();matchShots=matchHits=enemySunkCount=0;',
    'solo=soloMode;career=careerMode;aiLevel=difficulty;connected=false;localReady=remoteReady=myTurn=false;aiTargets.clear();matchShots=matchHits=enemySunkCount=hitStreak=bestStreak=0;lastAiHitR=lastAiHitC=prevAiHitR=prevAiHitC=-1;','reset')
rep('battleHud=label("",12,Color.rgb(255,214,92));battleHud.setVisibility(View.GONE);battleHud.setBackgroundColor(Color.rgb(7,35,47));root.addView(battleHud,new LinearLayout.LayoutParams(-1,dp(34)));\n        board=new BoardView();',
    'battleHud=label("",12,Color.rgb(255,214,92));battleHud.setVisibility(View.GONE);battleHud.setBackgroundColor(Color.rgb(7,35,47));root.addView(battleHud,new LinearLayout.LayoutParams(-1,dp(34)));\n        cinematicBanner=label("",18,Color.WHITE);cinematicBanner.setVisibility(View.GONE);cinematicBanner.setBackgroundColor(Color.rgb(83,28,18));root.addView(cinematicBanner,new LinearLayout.LayoutParams(-1,dp(42)));\n        board=new BoardView();','banner-create')
rep('private void restoreTurnPrompt(){if(board!=null&&board.playing&&!board.gameOver&&myTurn){status.setText("SIRA SENDE — vurulmamış bir hedef seç.");updateHud();}}',
'''private void restoreTurnPrompt(){if(board!=null&&board.playing&&!board.gameOver&&myTurn){status.setText("SIRA SENDE — vurulmamış bir hedef seç.");updateHud();}}\n    private void cinematic(String text,int ms){if(cinematicBanner==null)return;cinematicBanner.setText(text);cinematicBanner.setVisibility(View.VISIBLE);cinematicBanner.setAlpha(1f);cinematicBanner.animate().cancel();cinematicBanner.animate().alpha(0.92f).setDuration(120).withEndAction(()->main.postDelayed(()->{if(cinematicBanner!=null){cinematicBanner.animate().alpha(0f).setDuration(260).withEndAction(()->cinematicBanner.setVisibility(View.GONE)).start();}},Math.max(250,ms-380))).start();}\n    private void notePlayerResult(boolean hit,boolean sunk,int shipId){if(hit){hitStreak++;bestStreak=Math.max(bestStreak,hitStreak);cinematic(sunk?"⚓  "+AdvancedShipRenderer.name(shipId)+"  •  BATIRILDI":"✹  DOĞRUDAN İSABET  •  SERİ "+hitStreak,sunk?1450:650);}else{hitStreak=0;cinematic("≈  SUYA DÜŞTÜ  •  ISKA",520);}updateHud();}''','cinematic-method')
rep('battleHud.setText(turn+"   •   GEMİ: "+mine+"   •   DÜŞMAN: "+enemy+"   •   İSABET %"+String.format(java.util.Locale.US,"%.0f",acc));',
    'battleHud.setText(turn+"   •   GEMİ: "+mine+"   •   DÜŞMAN: "+enemy+"   •   İSABET %"+String.format(java.util.Locale.US,"%.0f",acc)+"   •   SERİ "+hitStreak);','hud-streak')
rep('matchShots++;if(res==2)matchHits++;recordShot(res==2);board.flash(r,c,false,res==2);boolean sunk=res==2&&shipId>=0&&board.isAiShipSunk(shipId);',
    'matchShots++;if(res==2)matchHits++;recordShot(res==2);board.flash(r,c,false,res==2);boolean sunk=res==2&&shipId>=0&&board.isAiShipSunk(shipId);notePlayerResult(res==2,sunk,shipId);','player-cinematic')
rep('if(res==2)addNeighbors(q[0],q[1]);board.flash(q[0],q[1],true,res==2);boolean sunk=res==2&&shipId>=0&&board.isOwnShipSunk(shipId);',
    'if(res==2){prevAiHitR=lastAiHitR;prevAiHitC=lastAiHitC;lastAiHitR=q[0];lastAiHitC=q[1];addNeighbors(q[0],q[1]);addAxisTargets();cinematic("⚠  GEMİMİZ İSABET ALDI",650);}else cinematic("◎  DÜŞMAN ATIŞI ISKALADI",480);board.flash(q[0],q[1],true,res==2);boolean sunk=res==2&&shipId>=0&&board.isOwnShipSunk(shipId);','ai-cinematic-axis')
rep('private boolean valid(int r,int c){return r>=0&&r<10&&c>=0&&c<10;}\n    private void addNeighbors(int r,int c){int[][] d={{-1,0},{1,0},{0,-1},{0,1}};for(int[]x:d){int rr=r+x[0],cc=c+x[1];if(valid(rr,cc)&&!board.hit[rr][cc])aiTargets.add(new int[]{rr,cc});}}',
'''private boolean valid(int r,int c){return r>=0&&r<10&&c>=0&&c<10;}\n    private void addNeighbors(int r,int c){int[][] d={{-1,0},{1,0},{0,-1},{0,1}};for(int[]x:d){int rr=r+x[0],cc=c+x[1];if(valid(rr,cc)&&!board.hit[rr][cc])aiTargets.add(new int[]{rr,cc});}}\n    private void addAxisTargets(){if(aiLevel<2||prevAiHitR<0||lastAiHitR<0)return;if(prevAiHitR==lastAiHitR){int d=Integer.compare(lastAiHitC,prevAiHitC);int c1=lastAiHitC+d,c2=prevAiHitC-d;if(valid(lastAiHitR,c1)&&!board.hit[lastAiHitR][c1])aiTargets.add(0,new int[]{lastAiHitR,c1});if(valid(prevAiHitR,c2)&&!board.hit[prevAiHitR][c2])aiTargets.add(0,new int[]{prevAiHitR,c2});}else if(prevAiHitC==lastAiHitC){int d=Integer.compare(lastAiHitR,prevAiHitR);int r1=lastAiHitR+d,r2=prevAiHitR-d;if(valid(r1,lastAiHitC)&&!board.hit[r1][lastAiHitC])aiTargets.add(0,new int[]{r1,lastAiHitC});if(valid(r2,prevAiHitC)&&!board.hit[r2][prevAiHitC])aiTargets.add(0,new int[]{r2,prevAiHitC});}}''','axis-ai')

# Add moving sea/radar atmosphere to each grid.
rep('private void drawGrid(Canvas c,float left,float top,float cell,boolean mine){p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(6,62,84));c.drawRect(left,top,left+10*cell,top+10*cell,p);',
'''private void drawGrid(Canvas c,float left,float top,float cell,boolean mine){p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(6,62,84));c.drawRect(left,top,left+10*cell,top+10*cell,p);\n            if(playing){long tm=System.currentTimeMillis();float phase=(tm%3200L)/3200f; p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1f,cell*.025f));p.setColor(Color.argb(75,110,225,240));for(int k=0;k<5;k++){float yy=top+((k*2.15f+phase*2.0f)%10f)*cell;c.drawLine(left,yy,left+10*cell,yy,p);}float scan=left+phase*10f*cell;p.setColor(Color.argb(90,80,255,190));p.setStrokeWidth(Math.max(1f,cell*.035f));c.drawLine(scan,top,scan,top+10*cell,p);postInvalidateDelayed(90);p.setStyle(Paint.Style.FILL);}\n''','sea-motion')

p.write_text(s,encoding='utf-8')
print('v9 cinematic patch applied')
