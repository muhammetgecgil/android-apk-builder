from pathlib import Path

p=Path('app/src/v7/java/com/mg/battleship/GameActivity.java')
s=p.read_text(encoding='utf-8')

def rep(old,new,label):
    global s
    if old not in s:
        raise SystemExit('missing v11 patch target: '+label)
    s=s.replace(old,new,1)

# Imports for premium UI surfaces and canvas gradients.
rep('import android.graphics.Path;','import android.graphics.Path;\nimport android.graphics.LinearGradient;\nimport android.graphics.Shader;\nimport android.graphics.Typeface;\nimport android.graphics.drawable.GradientDrawable;','imports')

# Premium command-center background and typography.
rep('LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(3,16,25));',
'''LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{Color.rgb(2,10,18),Color.rgb(4,25,38),Color.rgb(2,12,21)});root.setBackground(bg);''','shell-bg')

rep('private TextView label(String t,int sp,int color){TextView v=new TextView(this);v.setText(t);v.setTextSize(sp);v.setTextColor(color);v.setGravity(Gravity.CENTER);v.setPadding(dp(4),dp(5),dp(4),dp(5));return v;}\n    private Button btn(String t){Button b=new Button(this);b.setText(t);b.setAllCaps(false);b.setTextSize(16);return b;}',
'''private TextView label(String t,int sp,int color){TextView v=new TextView(this);v.setText(t);v.setTextSize(sp);v.setTextColor(color);v.setGravity(Gravity.CENTER);v.setPadding(dp(4),dp(5),dp(4),dp(5));v.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return v;}\n    private GradientDrawable panel(int fill,int stroke,float radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp((int)radius));g.setStroke(dp(1),stroke);return g;}\n    private Button btn(String t){Button b=new Button(this);b.setText(t);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(Color.rgb(226,244,250));b.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));b.setGravity(Gravity.CENTER_VERTICAL);b.setPadding(dp(22),0,dp(18),0);b.setBackground(panel(Color.rgb(8,38,53),Color.rgb(39,112,137),16));b.setElevation(dp(3));return b;}''','typography-buttons')

# Main menu hero treatment.
rep('TextView title=label("AMİRAL BATTI",30,Color.WHITE);title.setPadding(0,dp(18),0,0);root.addView(title);\n        TextView sub=label("TACTICAL NAVAL COMMAND",12,Color.rgb(75,210,240));root.addView(sub);',
'''TextView title=label("AMİRAL BATTI",34,Color.WHITE);title.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));title.setLetterSpacing(.10f);title.setPadding(0,dp(24),0,dp(2));root.addView(title);\n        TextView sub=label("TACTICAL NAVAL COMMAND",12,Color.rgb(92,220,245));sub.setLetterSpacing(.18f);root.addView(sub);\n        TextView secure=label("●  NAVAL COMMAND NETWORK  •  SYSTEM READY",11,Color.rgb(124,222,185));secure.setBackground(panel(Color.rgb(5,31,42),Color.rgb(28,92,105),14));LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,dp(34));slp.setMargins(dp(18),dp(8),dp(18),dp(4));root.addView(secure,slp);''','hero')

rep('TextView footer=label("v10 • TACTICAL DAMAGE • Kritik vuruş • Savaş raporu • Sinematik deniz",11,Color.rgb(120,145,155));',
    'TextView footer=label("v11 • NAVAL COMMAND UI • Tactical Glass • Radar Grid • Cinematic Warfare",11,Color.rgb(112,151,164));','footer')

# Rank chip styling.
rep('rankText=label("⚓ "+rank()+"   •   "+xp()+" XP",16,Color.rgb(255,211,92));rankText.setPadding(0,dp(10),0,dp(14));root.addView(rankText);',
'''rankText=label("⚓  "+rank()+"   //   "+xp()+" XP",15,Color.rgb(255,218,112));rankText.setBackground(panel(Color.rgb(22,31,35),Color.rgb(112,91,43),14));LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(-1,dp(42));rlp.setMargins(dp(36),dp(8),dp(36),dp(10));root.addView(rankText,rlp);''','rank-chip')

# Status/HUD glass panels.
rep('status=label(solo?"Gemilerini yerleştir. Hazır olduğunda savaşı başlat.":"Bluetooth rakip aranıyor…",14,Color.rgb(65,205,240));root.addView(status);',
'''status=label(solo?"Gemilerini yerleştir. Hazır olduğunda savaşı başlat.":"Bluetooth rakip aranıyor…",14,Color.rgb(127,222,244));status.setBackground(panel(Color.rgb(5,34,47),Color.rgb(28,94,117),12));LinearLayout.LayoutParams stlp=new LinearLayout.LayoutParams(-1,dp(42));stlp.setMargins(0,dp(4),0,dp(5));root.addView(status,stlp);''','status-panel')
rep('battleHud=label("",12,Color.rgb(255,214,92));battleHud.setVisibility(View.GONE);battleHud.setBackgroundColor(Color.rgb(7,35,47));root.addView(battleHud,new LinearLayout.LayoutParams(-1,dp(34)));',
'''battleHud=label("",12,Color.rgb(255,221,116));battleHud.setVisibility(View.GONE);battleHud.setBackground(panel(Color.rgb(7,31,42),Color.rgb(57,101,112),10));root.addView(battleHud,new LinearLayout.LayoutParams(-1,dp(36)));''','hud-panel')
rep('cinematicBanner=label("",18,Color.WHITE);cinematicBanner.setVisibility(View.GONE);cinematicBanner.setBackgroundColor(Color.rgb(83,28,18));root.addView(cinematicBanner,new LinearLayout.LayoutParams(-1,dp(42)));',
'''cinematicBanner=label("",18,Color.WHITE);cinematicBanner.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));cinematicBanner.setVisibility(View.GONE);cinematicBanner.setBackground(panel(Color.rgb(83,28,18),Color.rgb(190,76,44),10));root.addView(cinematicBanner,new LinearLayout.LayoutParams(-1,dp(44)));''','cinematic-panel')

# More space around board for A-J / 1-10 coordinates.
rep('private float[] geo(){float w=getWidth(),h=getHeight(),m=dp(3);float max=playing?(h-dp(90))/2f:h-dp(58);float gw=Math.min(w-m*2,max);float cell=gw/10,left=(w-gw)/2,top1=dp(24),top2=top1+gw+dp(45);return new float[]{gw,cell,left,top1,top2};}',
'''private float[] geo(){float w=getWidth(),h=getHeight(),m=dp(18);float max=playing?(h-dp(98))/2f:h-dp(64);float gw=Math.min(w-m*2,max);float cell=gw/10,left=(w-gw)/2,top1=dp(30),top2=top1+gw+dp(48);return new float[]{gw,cell,left,top1,top2};}''','grid-geo')

# Add ocean depth gradient, coordinate labels and luminous border.
needle='''private void drawGrid(Canvas c,float left,float top,float cell,boolean mine){p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(6,62,84));c.drawRect(left,top,left+10*cell,top+10*cell,p);'''
replacement='''private void drawGrid(Canvas c,float left,float top,float cell,boolean mine){\n            p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(left,top,left,top+10*cell,Color.rgb(8,75,98),Color.rgb(3,38,59),Shader.TileMode.CLAMP));c.drawRect(left,top,left+10*cell,top+10*cell,p);p.setShader(null);\n            p.setTextSize(Math.max(dp(8),cell*.24f));p.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.rgb(120,202,221));for(int k=0;k<10;k++)c.drawText(String.valueOf((char)('A'+k)),left+(k+.5f)*cell,top-dp(5),p);p.setTextAlign(Paint.Align.RIGHT);for(int k=0;k<10;k++)c.drawText(String.valueOf(k+1),left-dp(5),top+(k+.63f)*cell,p);p.setTextAlign(Paint.Align.CENTER);\n            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(Color.argb(150,72,206,232));c.drawRoundRect(left-dp(1),top-dp(1),left+10*cell+dp(1),top+10*cell+dp(1),dp(4),dp(4),p);p.setStyle(Paint.Style.FILL);'''
rep(needle,replacement,'grid-style')

p.write_text(s,encoding='utf-8')
print('v11 naval command UI patch applied')
