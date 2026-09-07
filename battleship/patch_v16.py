from pathlib import Path

p=Path('app/src/v7/java/com/mg/battleship/GameActivity.java')
s=p.read_text(encoding='utf-8')

def rep(old,new,label):
    global s
    if old not in s:
        raise SystemExit('missing v16 patch target: '+label)
    s=s.replace(old,new,1)

# Replace cramped 4-button single row with a responsive 2x2 deployment control grid.
old='''setupControls=new LinearLayout(this);setupControls.setOrientation(LinearLayout.HORIZONTAL);setupControls.setGravity(Gravity.CENTER);\n        rotate=btn("Yön: Yatay");randomize=btn("Rastgele");undo=btn("Geri Al");ready=btn("Hazır");\n        for(Button b:new Button[]{rotate,randomize,undo,ready}){b.setTextSize(13);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(54),1);lp.setMargins(dp(2),dp(4),dp(2),0);setupControls.addView(b,lp);}root.addView(setupControls);'''
new='''setupControls=new LinearLayout(this);setupControls.setOrientation(LinearLayout.VERTICAL);setupControls.setGravity(Gravity.CENTER);\n        rotate=btn("Yön: Yatay");randomize=btn("Rastgele");undo=btn("Geri Al");ready=btn("Hazır");\n        LinearLayout setupRow1=new LinearLayout(this);setupRow1.setOrientation(LinearLayout.HORIZONTAL);setupRow1.setGravity(Gravity.CENTER);\n        LinearLayout setupRow2=new LinearLayout(this);setupRow2.setOrientation(LinearLayout.HORIZONTAL);setupRow2.setGravity(Gravity.CENTER);\n        for(Button b:new Button[]{rotate,randomize,undo,ready}){b.setTextSize(15);b.setGravity(Gravity.CENTER);b.setMaxLines(1);b.setSingleLine(true);b.setPadding(dp(8),0,dp(8),0);if(Build.VERSION.SDK_INT>=26)b.setAutoSizeTextTypeUniformWithConfiguration(11,16,1,android.util.TypedValue.COMPLEX_UNIT_SP);}\n        for(Button b:new Button[]{rotate,randomize}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(54),1);lp.setMargins(dp(4),dp(3),dp(4),dp(3));setupRow1.addView(b,lp);}\n        for(Button b:new Button[]{undo,ready}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(54),1);lp.setMargins(dp(4),dp(3),dp(4),dp(3));setupRow2.addView(b,lp);}\n        setupControls.addView(setupRow1,new LinearLayout.LayoutParams(-1,dp(60)));setupControls.addView(setupRow2,new LinearLayout.LayoutParams(-1,dp(60)));root.addView(setupControls);'''
rep(old,new,'responsive-setup-grid')

# Keep direction label compact so it never wraps on narrow displays.
rep('rotate.setOnClickListener(v->{if(!localReady){board.horizontal=!board.horizontal;rotate.setText(board.horizontal?"Yön: Yatay":"Yön: Dikey");board.invalidate();}});',
    'rotate.setOnClickListener(v->{if(!localReady){board.horizontal=!board.horizontal;rotate.setText(board.horizontal?"Yön: Yatay":"Yön: Dikey");rotate.setContentDescription(board.horizontal?"Gemi yönü yatay":"Gemi yönü dikey");board.invalidate();}});','direction-label')

# v16 footer identity.
rep('TextView footer=label("v15 • BLUETOOTH PARITY • Undo Deployment • Sonar • Recon • Critical Turn",11,Color.rgb(112,151,164));',
    'TextView footer=label("v16 • RESPONSIVE CONTROLS • Bluetooth Parity • Undo • Tactical Combat",11,Color.rgb(112,151,164));','footer')

p.write_text(s,encoding='utf-8')
print('v16 responsive deployment controls patch applied')
