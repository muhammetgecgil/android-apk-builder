from pathlib import Path

p=Path('app/src/v7/java/com/mg/battleship/GameActivity.java')
s=p.read_text(encoding='utf-8')

def rep(old,new,label):
    global s
    if old not in s:
        raise SystemExit('missing v17 patch target: '+label)
    s=s.replace(old,new,1)

# Restore-state guard.
rep('private long localNonce=Math.abs(new Random().nextLong()),remoteNonce=-1;',
    'private long localNonce=Math.abs(new Random().nextLong()),remoteNonce=-1;\n    private boolean restoringSavedBattle=false;','restore-field')

# First-launch tutorial.
rep('@Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences(PREF,MODE_PRIVATE);showMainMenu();}',
'''@Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences(PREF,MODE_PRIVATE);if(!prefs.getBoolean("tutorial_seen",false))showTutorial(true);else showMainMenu();}\n    @Override protected void onPause(){super.onPause();if(solo&&board!=null&&board.playing&&!board.gameOver)saveResume();}''','first-launch')

# Expand command dashboard with Continue / Settings / Help.
old_menu='''Button soloB=btn("⚔  TEK OYUNCU"),btB=btn("⌁  BLUETOOTH"),careerB=btn("★  KARİYER"),tacticalB=btn("◉  TAKTİK MOD"),fleetB=btn("▰  FİLOM"),missionB=btn("◇  GÖREVLER"),achievementB=btn("✦  BAŞARIMLAR"),statsB=btn("◎  KAYITLAR");\n        addCommandRow(root,soloB,btB);addCommandRow(root,careerB,tacticalB);addCommandRow(root,fleetB,missionB);addCommandRow(root,achievementB,statsB);'''
new_menu='''Button continueB=btn("▶  DEVAM ET"),settingsB=btn("⚙  AYARLAR"),soloB=btn("⚔  TEK OYUNCU"),btB=btn("⌁  BLUETOOTH"),careerB=btn("★  KARİYER"),tacticalB=btn("◉  TAKTİK MOD"),fleetB=btn("▰  FİLOM"),missionB=btn("◇  GÖREVLER"),achievementB=btn("✦  BAŞARIMLAR"),statsB=btn("◎  KAYITLAR"),helpB=btn("?  NASIL OYNANIR"),dummyB=btn("⚓  SÜRÜM 17");\n        continueB.setEnabled(prefs.getBoolean("resume_valid",false));dummyB.setEnabled(false);\n        addCommandRow(root,continueB,settingsB);addCommandRow(root,soloB,btB);addCommandRow(root,careerB,tacticalB);addCommandRow(root,fleetB,missionB);addCommandRow(root,achievementB,statsB);addCommandRow(root,helpB,dummyB);'''
rep(old_menu,new_menu,'dashboard-buttons')

old_actions='''soloB.setOnClickListener(v->{tacticalMode=false;showDifficulty(false);});btB.setOnClickListener(v->{tacticalMode=true;startBattle(false,false,2);});careerB.setOnClickListener(v->{tacticalMode=false;showCareer();});tacticalB.setOnClickListener(v->{tacticalMode=true;showDifficulty(false);});fleetB.setOnClickListener(v->showFleet());missionB.setOnClickListener(v->showMissions());achievementB.setOnClickListener(v->showAchievements());statsB.setOnClickListener(v->showStats());'''
new_actions='''continueB.setOnClickListener(v->restoreSavedBattle());settingsB.setOnClickListener(v->showSettings());soloB.setOnClickListener(v->{tacticalMode=false;showDifficulty(false);});btB.setOnClickListener(v->{tacticalMode=true;startBattle(false,false,2);});careerB.setOnClickListener(v->{tacticalMode=false;showCareer();});tacticalB.setOnClickListener(v->{tacticalMode=true;showDifficulty(false);});fleetB.setOnClickListener(v->showFleet());missionB.setOnClickListener(v->showMissions());achievementB.setOnClickListener(v->showAchievements());statsB.setOnClickListener(v->showStats());helpB.setOnClickListener(v->showTutorial(false));'''
rep(old_actions,new_actions,'dashboard-actions')

# Tutorial, settings and local resume methods.
rep('    private void showDifficulty(boolean fromCareer){',
'''    private void showTutorial(boolean first){\n        LinearLayout root=shell();root.addView(label("AMİRAL BATTI — HIZLI EĞİTİM",25,Color.WHITE));\n        TextView t=label("1  Filonu yatay/dikey yerleştir. Geri Al ile son gemiyi değiştirebilirsin.\\n\\n2  Düşman denizinde vurulmamış bir koordinata dokun. Tur bilgisi üst HUD'da görünür.\\n\\n3  Vuruş serileri kritik ek atış kazandırabilir.\\n\\n4  Taktik Mod ve Bluetooth savaşında SONAR ile 3×3 bölgeyi tara; KEŞİF ile olası temas bul.\\n\\n5  Uygulama kapanırsa tek oyuncu savaşın otomatik kaydedilir; DEVAM ET ile geri dönersin.\\n\\nBluetooth savaşı internetsizdir; eşleşmiş iki telefon doğrudan haberleşir.",16,Color.rgb(205,230,236));t.setGravity(Gravity.LEFT);t.setPadding(dp(16),dp(12),dp(16),dp(12));root.addView(t,new LinearLayout.LayoutParams(-1,0,1));\n        Button ok=btn(first?"Anladım — Oyuna Başla":"← Komuta Merkezi");addButton(root,ok);ok.setOnClickListener(v->{prefs.edit().putBoolean("tutorial_seen",true).apply();showMainMenu();});setContentView(root);root.requestApplyInsets();\n    }\n\n    private void showSettings(){\n        LinearLayout root=shell();root.addView(label("AYARLAR",27,Color.WHITE));root.addView(label("Ayarlar yalnız bu cihazda saklanır.",13,Color.rgb(110,205,230)));\n        boolean snd=prefs.getBoolean("sound_on",true),vib=prefs.getBoolean("vibration_on",true),cin=prefs.getBoolean("cinematic_on",true);\n        Button sound=btn("Ses: "+(snd?"AÇIK":"KAPALI")),vibration=btn("Titreşim: "+(vib?"AÇIK":"KAPALI")),cinematic=btn("Sinematik Bildirim: "+(cin?"AÇIK":"KAPALI"));addButton(root,sound);addButton(root,vibration);addButton(root,cinematic);\n        sound.setOnClickListener(v->{boolean x=!prefs.getBoolean("sound_on",true);prefs.edit().putBoolean("sound_on",x).apply();sound.setText("Ses: "+(x?"AÇIK":"KAPALI"));});\n        vibration.setOnClickListener(v->{boolean x=!prefs.getBoolean("vibration_on",true);prefs.edit().putBoolean("vibration_on",x).apply();vibration.setText("Titreşim: "+(x?"AÇIK":"KAPALI"));});\n        cinematic.setOnClickListener(v->{boolean x=!prefs.getBoolean("cinematic_on",true);prefs.edit().putBoolean("cinematic_on",x).apply();cinematic.setText("Sinematik Bildirim: "+(x?"AÇIK":"KAPALI"));});\n        Button back=btn("← Komuta Merkezi");addButton(root,back);back.setOnClickListener(v->showMainMenu());setContentView(root);root.requestApplyInsets();\n    }\n\n    private String enc(int[][] a){StringBuilder b=new StringBuilder(260);for(int r=0;r<10;r++)for(int c=0;c<10;c++){if(b.length()>0)b.append(',');b.append(a[r][c]);}return b.toString();}\n    private String enc(boolean[][] a){StringBuilder b=new StringBuilder(100);for(int r=0;r<10;r++)for(int c=0;c<10;c++)b.append(a[r][c]?'1':'0');return b.toString();}\n    private void dec(String x,int[][] a){try{String[] q=x.split(",");if(q.length!=100)return;int k=0;for(int r=0;r<10;r++)for(int c=0;c<10;c++)a[r][c]=Integer.parseInt(q[k++]);}catch(Exception ignored){}}\n    private void dec(String x,boolean[][] a){if(x==null||x.length()!=100)return;int k=0;for(int r=0;r<10;r++)for(int c=0;c<10;c++)a[r][c]=x.charAt(k++)=='1';}\n    private void saveResume(){if(!solo||board==null||!board.playing||board.gameOver)return;SharedPreferences.Editor e=prefs.edit();e.putBoolean("resume_valid",true).putBoolean("resume_career",career).putBoolean("resume_tactical",tacticalMode).putBoolean("resume_turn",myTurn).putInt("resume_ai",aiLevel).putInt("resume_stage",careerStage).putInt("resume_matchShots",matchShots).putInt("resume_matchHits",matchHits).putInt("resume_enemySunk",enemySunkCount).putInt("resume_streak",hitStreak).putInt("resume_bestStreak",bestStreak).putString("resume_own",enc(board.own)).putString("resume_aiFleet",enc(board.aiFleet)).putString("resume_marks",enc(board.enemyMarks)).putString("resume_hit",enc(board.hit)).putString("resume_aiHit",enc(board.aiHit)).apply();}\n    private void restoreSavedBattle(){if(!prefs.getBoolean("resume_valid",false))return;restoringSavedBattle=true;tacticalMode=prefs.getBoolean("resume_tactical",false);careerStage=prefs.getInt("resume_stage",0);boolean savedCareer=prefs.getBoolean("resume_career",false);int savedAi=prefs.getInt("resume_ai",2);startBattle(true,savedCareer,savedAi);restoringSavedBattle=false;dec(prefs.getString("resume_own",""),board.own);dec(prefs.getString("resume_aiFleet",""),board.aiFleet);dec(prefs.getString("resume_marks",""),board.enemyMarks);dec(prefs.getString("resume_hit",""),board.hit);dec(prefs.getString("resume_aiHit",""),board.aiHit);board.placed=board.len.length;board.playing=true;localReady=true;myTurn=prefs.getBoolean("resume_turn",true);matchShots=prefs.getInt("resume_matchShots",0);matchHits=prefs.getInt("resume_matchHits",0);enemySunkCount=prefs.getInt("resume_enemySunk",0);hitStreak=prefs.getInt("resume_streak",0);bestStreak=prefs.getInt("resume_bestStreak",0);enterCombat();board.invalidate();status.setText(myTurn?"SAVAŞ GERİ YÜKLENDİ — SIRA SENDE":"SAVAŞ GERİ YÜKLENDİ — RAKİP HAMLESİ SÜRÜYOR");updateHud();if(!myTurn)main.postDelayed(this::aiShot,650);}\n\n    private void showDifficulty(boolean fromCareer){''','tutorial-settings-resume')

# New solo battle starts clean; restore path keeps the existing save until loaded.
rep('solo=soloMode;career=careerMode;aiLevel=difficulty;connected=false;localReady=remoteReady=myTurn=false;aiTargets.clear();sonarUsed=reconUsed=sonarArmed=false;threatText="TEHDİT: —";',
    'solo=soloMode;career=careerMode;aiLevel=difficulty;connected=false;localReady=remoteReady=myTurn=false;if(solo&&!restoringSavedBattle)prefs.edit().putBoolean("resume_valid",false).apply();aiTargets.clear();sonarUsed=reconUsed=sonarArmed=false;threatText="TEHDİT: —";','new-battle-clear-save')

# Clear resume on battle completion.
rep('private void finishMatch(boolean win){',
    'private void finishMatch(boolean win){\n        prefs.edit().putBoolean("resume_valid",false).apply();','clear-resume-on-finish')

# Respect cinematic setting.
rep('private void cinematic(String text,int ms){if(cinematicBanner==null)return;',
    'private void cinematic(String text,int ms){if(cinematicBanner==null||!prefs.getBoolean("cinematic_on",true))return;','cinematic-setting')

# Saving through HUD updates gives turn-safe checkpoints after every meaningful action.
rep('battleHud.setText(turn+"  •  GEMİ "+mine+"  •  DÜŞMAN "+enemy+"\\nHASAR %"+dmg+"  •  İSABET %"+String.format(java.util.Locale.US,"%.0f",acc)+"  •  SERİ ×"+hitStreak+"  •  "+threatText+"  •  "+alarm);',
    'battleHud.setText(turn+"  •  GEMİ "+mine+"  •  DÜŞMAN "+enemy+"\\nHASAR %"+dmg+"  •  İSABET %"+String.format(java.util.Locale.US,"%.0f",acc)+"  •  SERİ ×"+hitStreak+"  •  "+threatText+"  •  "+alarm);if(solo&&!board.gameOver)saveResume();','autosave-hud')

# Better Bluetooth reconnect messaging; still no internet.
rep('public void onLost(){main.post(()->{connected=false;status.setText("Bağlantı koptu. Rakip yeniden aranıyor…");startBluetooth();});}',
    'public void onLost(){main.post(()->{connected=false;if(status!=null)status.setText("BLUETOOTH BAĞLANTISI KOPTU — aynı rakibe yeniden bağlanılıyor…");main.postDelayed(()->{if(!connected)startBluetooth();},700);});}','bt-reconnect')

# v17 footer.
rep('TextView footer=label("v16 • RESPONSIVE CONTROLS • Bluetooth Parity • Undo • Tactical Combat",11,Color.rgb(112,151,164));',
    'TextView footer=label("v17 • RELEASE CANDIDATE • Auto Save • Settings • Tutorial • Bluetooth Recovery",11,Color.rgb(112,151,164));','footer')

p.write_text(s,encoding='utf-8')
print('v17 release candidate patch applied')

# BattleEffects obeys local sound/vibration settings.
ep=Path('app/src/v4/java/com/mg/battleship/BattleEffects.java')
e=ep.read_text(encoding='utf-8')
e=e.replace('''    static void hit(Context c){\n        vibrate(c,110,210);\n        new Thread(BattleEffects::playExplosion,"battle-explosion").start();\n    }''','''    static void hit(Context c){\n        if(vibrationOn(c))vibrate(c,110,210);\n        if(soundOn(c))new Thread(BattleEffects::playExplosion,"battle-explosion").start();\n    }''')
e=e.replace('''    static void miss(Context c){\n        vibrate(c,24,65);\n        new Thread(BattleEffects::playSplash,"battle-water").start();\n    }''','''    static void miss(Context c){\n        if(vibrationOn(c))vibrate(c,24,65);\n        if(soundOn(c))new Thread(BattleEffects::playSplash,"battle-water").start();\n    }''')
e=e.replace('''    static void sunk(Context c){\n        vibratePattern(c);\n        new Thread(BattleEffects::playSinking,"battle-sinking").start();\n    }''','''    static void sunk(Context c){\n        if(vibrationOn(c))vibratePattern(c);\n        if(soundOn(c))new Thread(BattleEffects::playSinking,"battle-sinking").start();\n    }\n\n    private static boolean soundOn(Context c){return c.getSharedPreferences("admiral_v7",Context.MODE_PRIVATE).getBoolean("sound_on",true);}\n    private static boolean vibrationOn(Context c){return c.getSharedPreferences("admiral_v7",Context.MODE_PRIVATE).getBoolean("vibration_on",true);}''')
ep.write_text(e,encoding='utf-8')
print('v17 battle effect settings applied')
