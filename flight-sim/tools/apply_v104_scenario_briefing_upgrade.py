from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
LAUNCHER=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/LauncherActivity.java'
GRADLE=ROOT/'app/build.gradle'

def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v104 scenario patch anchor missing: {label}')
    return text.replace(old,new,1)

s=LAUNCHER.read_text()
s=rep(s,
'        TextView sub=new TextView(this);sub.setText("Her görev aynı uçuş motorunu kullanır; hava, başlangıç fazı, ATC ve pist koşulları göreve göre ayarlanır.");sub.setTextColor(Color.rgb(190,194,197));sub.setTextSize(11);sub.setGravity(Gravity.CENTER);root.addView(sub,new LinearLayout.LayoutParams(-1,dp(34)));\n',
'        TextView sub=new TextView(this);sub.setText("Aynı uçuş motoru • farklı görev profili • hava/pist/ATC bağlamı • görev odaklı briefing");sub.setTextColor(Color.rgb(190,194,197));sub.setTextSize(11);sub.setGravity(Gravity.CENTER);root.addView(sub,new LinearLayout.LayoutParams(-1,dp(34)));\n',
'scenario subtitle')
old='''        addScenario(root,cockpitTarget,SCENARIO_FULL_DEMO,"1. FULL DEMO  —  Tam görev: kalkış • bulut • deniz • orbit • yaklaşma • iniş");
        addScenario(root,cockpitTarget,SCENARIO_NIGHT_LANDING,"2. NIGHT LANDING  —  RWY 27 gece yaklaşması • tam pist ışıkları • PAPI/HUD");
        addScenario(root,cockpitTarget,SCENARIO_LOW_VIS_RAIN,"3. LOW VIS / RAIN APPROACH  —  Yağmur + sis • düşük RVR • ıslak pist • ILS/PAPI");
        addScenario(root,cockpitTarget,SCENARIO_MOUNTAIN_PASS,"4. MOUNTAIN PASS  —  Dağ rotası • yüksek hızlı alçak/orta irtifa manevrası");
        addScenario(root,cockpitTarget,SCENARIO_SEA_LOW_LEVEL,"5. SEA LOW LEVEL  —  Deniz üstü alçak irtifa • yüksek hız • pull-up");
        addScenario(root,cockpitTarget,SCENARIO_EMERGENCY_RETURN,"6. EMERGENCY RETURN  —  Kısa acil geri dönüş • pist yakalama • öncelikli ATC");
'''
new='''        addScenario(root,cockpitTarget,SCENARIO_FULL_DEMO,"1. FULL DEMO  —  Kalkış • bulut geçişi • deniz alçak uçuş • orbit • yaklaşma • iniş  |  ODAK: tam sistem turu");
        addScenario(root,cockpitTarget,SCENARIO_NIGHT_LANDING,"2. NIGHT LANDING  —  RWY 27 • gece • PAPI/HUD • centerline • flare/sink-rate  |  ODAK: hassas iniş");
        addScenario(root,cockpitTarget,SCENARIO_LOW_VIS_RAIN,"3. LOW VIS / RAIN  —  Yağmur + sis • düşük RVR • ıslak pist • ILS/PAPI • frenleme  |  ODAK: kötü hava yaklaşması");
        addScenario(root,cockpitTarget,SCENARIO_MOUNTAIN_PASS,"4. MOUNTAIN PASS  —  Vadi/geçit • terrain clearance • yüksek hız • enerji/G yönetimi  |  ODAK: arazi manevrası");
        addScenario(root,cockpitTarget,SCENARIO_SEA_LOW_LEVEL,"5. SEA LOW LEVEL  —  Deniz üstü çok alçak • yüksek hız • görsel akış • pull-up  |  ODAK: alçak irtifa enerji yönetimi");
        addScenario(root,cockpitTarget,SCENARIO_EMERGENCY_RETURN,"6. EMERGENCY RETURN  —  Kısa geri dönüş • pist yakalama • öncelikli ATC • güvenli iniş  |  ODAK: acil durum toparlama");
'''
s=rep(s,old,new,'scenario labels')
s=rep(s,
'    private void addScenario(LinearLayout root,boolean cockpitTarget,int id,String label){\n        Button b=scenarioButton(label);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(59));lp.setMargins(0,dp(5),0,0);root.addView(b,lp);b.setOnClickListener(v->launchScenario(cockpitTarget,id,label));\n    }\n',
'    private void addScenario(LinearLayout root,boolean cockpitTarget,int id,String label){\n        Button b=scenarioButton(label);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(62));lp.setMargins(0,dp(4),0,0);root.addView(b,lp);b.setOnClickListener(v->launchScenario(cockpitTarget,id,label));\n    }\n',
'scenario row height')
s=rep(s,
'    private Button scenarioButton(String s){Button b=button(s);b.setTextSize(12);b.setTextColor(Color.WHITE);b.setBackgroundColor(Color.rgb(96,97,99));b.setGravity(Gravity.CENTER);return b;}\n',
'    private Button scenarioButton(String s){Button b=button(s);b.setTextSize(11);b.setTextColor(Color.WHITE);b.setBackgroundColor(Color.rgb(88,91,94));b.setGravity(Gravity.CENTER);b.setPadding(dp(10),dp(2),dp(10),dp(2));return b;}\n',
'scenario button styling')
LAUNCHER.write_text(s)

g=GRADLE.read_text()
g=re.sub(r'versionCode\s+\d+','versionCode 104',g,count=1)
g=re.sub(r'versionName\s+"[^"]+"','versionName "26.22-avm31.1-scenario-briefing-upgrade-debug"',g,count=1)
GRADLE.write_text(g)
print('v104 scenario briefing upgrade applied')
