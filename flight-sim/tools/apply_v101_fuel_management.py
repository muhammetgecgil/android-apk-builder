from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT/'app/src/main/java/com/mg/fixturecockpitsim'
SIM=PKG/'sim'
STATE=SIM/'FlightState.java'
DYN=SIM/'FlightDynamicsEngine.java'
MISSION=SIM/'AutonomousFlightMission.java'
COCKPIT=PKG/'FighterCockpitView.java'
COCKSYS=SIM/'FighterCockpitSystemsModel.java'
TEST=ROOT/'app/src/test/java/com/mg/fixturecockpitsim/sim'
GRADLE=ROOT/'app/build.gradle'


def rep(text, old, new, label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v101 fuel patch anchor missing: {label}')
    return text.replace(old, new, 1)

# ---------------------------------------------------------------------------
# Deterministic generic twin-engine fighter fuel management model.
# Capacities are fractions of the AVM-27 initial fuel so this stays compatible
# with the existing aircraft mass model without pretending to be a real type.
# ---------------------------------------------------------------------------
(SIM/'FighterFuelSystemModel.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

/** AVM-29 generic fighter internal/external tank, transfer and imbalance model. */
public final class FighterFuelSystemModel {
    private FighterFuelSystemModel(){}

    public static final double TOTAL_CAPACITY_KG=AdvancedFlightPhysicsModel.INITIAL_FUEL_KG;
    public static final double FWD_CAP_KG=TOTAL_CAPACITY_KG*.16;
    public static final double AFT_CAP_KG=TOTAL_CAPACITY_KG*.16;
    public static final double LEFT_CAP_KG=TOTAL_CAPACITY_KG*.23;
    public static final double RIGHT_CAP_KG=TOTAL_CAPACITY_KG*.23;
    public static final double EXT_CAP_KG=TOTAL_CAPACITY_KG*.22;
    public static final double BINGO_KG=Math.max(350.0,TOTAL_CAPACITY_KG*.16);
    public static final double IMBALANCE_CAUTION_KG=Math.max(80.0,TOTAL_CAPACITY_KG*.025);
    public static final double EXTERNAL_TANK_EMPTY_MASS_KG=105.0;

    public static void reset(FlightState s){
        s.fuelForwardKg=FWD_CAP_KG;
        s.fuelAftKg=AFT_CAP_KG;
        s.fuelLeftKg=LEFT_CAP_KG;
        s.fuelRightKg=RIGHT_CAP_KG;
        s.fuelExternalKg=EXT_CAP_KG;
        s.externalTankPresent=true;
        s.fuelInitialized=true;
        publish(s,0);
    }

    public static void initializeFromTotal(FlightState s){
        double total=clamp(s.fuelKg,0,TOTAL_CAPACITY_KG);
        double k=TOTAL_CAPACITY_KG>0?total/TOTAL_CAPACITY_KG:0;
        s.fuelForwardKg=FWD_CAP_KG*k;
        s.fuelAftKg=AFT_CAP_KG*k;
        s.fuelLeftKg=LEFT_CAP_KG*k;
        s.fuelRightKg=RIGHT_CAP_KG*k;
        s.fuelExternalKg=EXT_CAP_KG*k;
        s.externalTankPresent=true;
        s.fuelInitialized=true;
        publish(s,0);
    }

    public static void update(FlightState s,double dtSec){
        if(!s.fuelInitialized)initializeFromTotal(s);
        dtSec=clamp(dtSec,0,.05);

        // External fuel is transferred first into the wing/feed tanks. Transfer
        // is symmetric and rate limited, so an external tank does not magically
        // disappear from the mass/CG model when it starts feeding the engines.
        double targetL=LEFT_CAP_KG*.88,targetR=RIGHT_CAP_KG*.88;
        double needL=Math.max(0,targetL-s.fuelLeftKg),needR=Math.max(0,targetR-s.fuelRightKg);
        double maxTransfer=2.4*dtSec;
        double xfer=Math.min(s.fuelExternalKg,Math.min(maxTransfer,needL+needR));
        if(xfer>0){
            double sum=Math.max(.001,needL+needR);
            double toL=xfer*needL/sum,toR=xfer-toL;
            s.fuelLeftKg+=toL;s.fuelRightKg+=toR;s.fuelExternalKg-=xfer;
        }

        // Forward/aft cells feed the left/right collector tanks if either side
        // falls below roughly half capacity. Aft/forward scheduling is blended
        // to keep CG motion gradual instead of creating a step change.
        double collectorNeed=Math.max(0,LEFT_CAP_KG*.52-s.fuelLeftKg)+Math.max(0,RIGHT_CAP_KG*.52-s.fuelRightKg);
        double internalTransfer=Math.min(1.65*dtSec,collectorNeed);
        if(internalTransfer>0){
            double aftShare=clamp(.46+.08*(s.cgMac-AdvancedFlightPhysicsModel.FULL_FUEL_CG_MAC)/.04,.30,.62);
            double fromAft=Math.min(s.fuelAftKg,internalTransfer*aftShare);
            double fromFwd=Math.min(s.fuelForwardKg,internalTransfer-fromAft);
            double moved=fromAft+fromFwd;s.fuelAftKg-=fromAft;s.fuelForwardKg-=fromFwd;
            double leftNeed=Math.max(.001,LEFT_CAP_KG*.52-s.fuelLeftKg),rightNeed=Math.max(.001,RIGHT_CAP_KG*.52-s.fuelRightKg),sum=leftNeed+rightNeed;
            s.fuelLeftKg+=moved*leftNeed/sum;s.fuelRightKg+=moved*rightNeed/sum;
            xfer+=moved;
        }

        // Two-engine fuel flow. Afterburner carries a large additional fuel
        // penalty; a tiny deterministic left/right flow mismatch lets the
        // imbalance logic exist as a real state rather than a cosmetic flag.
        double t=clamp(s.throttle,0,1);
        double dryFlow=.28+1.18*t*t;
        double ab=Math.max(0,(t-.80)/.20);
        double totalFlow=dryFlow+5.25*ab*ab;
        double burn=Math.min(totalFuel(s),totalFlow*dtSec);
        double bias=.012*Math.sin(s.timeSec*.37);
        double leftBurn=burn*(.5+bias),rightBurn=burn-leftBurn;
        leftBurn=drawFeed(s,true,leftBurn);
        rightBurn=drawFeed(s,false,rightBurn);
        double remaining=Math.max(0,burn-leftBurn-rightBurn);
        if(remaining>0)drawCentral(s,remaining);

        s.fuelFlowKgSec=dtSec>1e-6?burn/dtSec:0;
        publish(s,dtSec>1e-6?xfer/dtSec:0);
    }

    private static double drawFeed(FlightState s,boolean left,double amount){
        double have=left?s.fuelLeftKg:s.fuelRightKg;
        double used=Math.min(have,amount);
        if(left)s.fuelLeftKg-=used;else s.fuelRightKg-=used;
        double rem=amount-used;
        if(rem>0)used+=drawCentral(s,rem);
        return used;
    }

    private static double drawCentral(FlightState s,double amount){
        double used=0;
        double a=Math.min(s.fuelForwardKg,amount*.52);s.fuelForwardKg-=a;used+=a;amount-=a;
        double b=Math.min(s.fuelAftKg,amount);s.fuelAftKg-=b;used+=b;amount-=b;
        if(amount>0){double c=Math.min(s.fuelForwardKg,amount);s.fuelForwardKg-=c;used+=c;}
        return used;
    }

    private static void publish(FlightState s,double transferRate){
        s.fuelForwardKg=clamp(s.fuelForwardKg,0,FWD_CAP_KG);
        s.fuelAftKg=clamp(s.fuelAftKg,0,AFT_CAP_KG);
        s.fuelLeftKg=clamp(s.fuelLeftKg,0,LEFT_CAP_KG);
        s.fuelRightKg=clamp(s.fuelRightKg,0,RIGHT_CAP_KG);
        s.fuelExternalKg=clamp(s.fuelExternalKg,0,EXT_CAP_KG);
        double total=totalFuel(s);
        s.fuelKg=total;
        s.fuelFraction01=TOTAL_CAPACITY_KG>0?clamp(total/TOTAL_CAPACITY_KG,0,1):0;
        s.fuelTransferKgSec=Math.max(0,transferRate);
        s.fuelImbalanceKg=s.fuelLeftKg-s.fuelRightKg;
        s.fuelImbalanceCaution=Math.abs(s.fuelImbalanceKg)>=IMBALANCE_CAUTION_KG;
        s.bingoFuel=total<=BINGO_KG;

        double fwdFrac=FWD_CAP_KG>0?s.fuelForwardKg/FWD_CAP_KG:0;
        double aftFrac=AFT_CAP_KG>0?s.fuelAftKg/AFT_CAP_KG:0;
        double extFrac=EXT_CAP_KG>0?s.fuelExternalKg/EXT_CAP_KG:0;
        s.cgMac=clamp(AdvancedFlightPhysicsModel.FULL_FUEL_CG_MAC+.031*(aftFrac-fwdFrac)+.010*extFrac,.26,.40);
        s.massKg=AdvancedFlightPhysicsModel.DRY_MASS_KG+total+(s.externalTankPresent?EXTERNAL_TANK_EMPTY_MASS_KG:0);
        s.inertiaRollKgM2=14800+5.2*(s.fuelLeftKg+s.fuelRightKg)+1.3*(s.fuelForwardKg+s.fuelAftKg)+6.0*s.fuelExternalKg;
        s.inertiaPitchKgM2=48200+6.5*(s.fuelForwardKg+s.fuelAftKg)+3.1*(s.fuelLeftKg+s.fuelRightKg)+7.2*s.fuelExternalKg;
        s.inertiaYawKgM2=60200+5.7*(s.fuelLeftKg+s.fuelRightKg)+3.5*(s.fuelForwardKg+s.fuelAftKg)+7.6*s.fuelExternalKg;
    }

    public static double totalFuel(FlightState s){return Math.max(0,s.fuelForwardKg)+Math.max(0,s.fuelAftKg)+Math.max(0,s.fuelLeftKg)+Math.max(0,s.fuelRightKg)+Math.max(0,s.fuelExternalKg);}
    private static double clamp(double v,double a,double b){return Math.max(a,Math.min(b,v));}
}
''')

# FlightState: tank-level state shared by physics and cockpit.
s=STATE.read_text()
s=rep(s,
'    public double fuelFraction01 = 1.0;\n',
'''    public double fuelFraction01 = 1.0;
    // AVM-29 fuel management state.
    public double fuelForwardKg, fuelAftKg, fuelLeftKg, fuelRightKg, fuelExternalKg;
    public double fuelFlowKgSec, fuelTransferKgSec, fuelImbalanceKg;
    public boolean bingoFuel, fuelImbalanceCaution, externalTankPresent=true, fuelInitialized;
''',
'fuel state fields')
s=rep(s,
'        c.fuelKg=fuelKg;c.fuelFraction01=fuelFraction01;c.massKg=massKg;c.cgMac=cgMac;\n',
'''        c.fuelKg=fuelKg;c.fuelFraction01=fuelFraction01;c.massKg=massKg;c.cgMac=cgMac;
        c.fuelForwardKg=fuelForwardKg;c.fuelAftKg=fuelAftKg;c.fuelLeftKg=fuelLeftKg;c.fuelRightKg=fuelRightKg;c.fuelExternalKg=fuelExternalKg;
        c.fuelFlowKgSec=fuelFlowKgSec;c.fuelTransferKgSec=fuelTransferKgSec;c.fuelImbalanceKg=fuelImbalanceKg;
        c.bingoFuel=bingoFuel;c.fuelImbalanceCaution=fuelImbalanceCaution;c.externalTankPresent=externalTankPresent;c.fuelInitialized=fuelInitialized;
''',
'fuel state copy')
STATE.write_text(s)

# Dynamics: replace single-bucket burn with tank/transfer model and let imbalance
# create a small but real rolling tendency.
d=DYN.read_text()
d=rep(d,'        AdvancedFlightPhysicsModel.updateMassAndFuel(s,dtSec);\n','        FighterFuelSystemModel.update(s,dtSec);\n','fuel dynamics call')
d=rep(d,
'            double rollAccel=Math.toDegrees(pre.rollMomentNm/Math.max(1000.0,s.inertiaRollKgM2))+gustRollAccel;\n',
'''            double rollAccel=Math.toDegrees(pre.rollMomentNm/Math.max(1000.0,s.inertiaRollKgM2))+gustRollAccel;
            rollAccel+=clamp(s.fuelImbalanceKg/FighterFuelSystemModel.IMBALANCE_CAUTION_KG,-1.6,1.6)*2.6;
''',
'imbalance roll moment')
DYN.write_text(d)

# Mission reset: every scenario starts with a coherent tank state.
m=MISSION.read_text()
pat=r's\.fuelKg=AdvancedFlightPhysicsModel\.INITIAL_FUEL_KG;s\.fuelFraction01=1;s\.massKg=AdvancedFlightPhysicsModel\.DRY_MASS_KG\+s\.fuelKg;s\.cgMac=AdvancedFlightPhysicsModel\.FULL_FUEL_CG_MAC;\s*\n\s*s\.rollRateDegSec=s\.pitchRateDegSec=s\.yawRateDegSec=s\.sideslipDeg=0;s\.stall01=s\.spin01=0;AdvancedFlightPhysicsModel\.updateMassAndFuel\(s,0\);'
repl='FighterFuelSystemModel.reset(s);\n        s.rollRateDegSec=s.pitchRateDegSec=s.yawRateDegSec=s.sideslipDeg=0;s.stall01=s.spin01=0;'
new_m,n=re.subn(pat,repl,m,count=1)
if n==0 and 'FighterFuelSystemModel.reset(s);' not in m:
    raise SystemExit('v101 fuel patch anchor missing: mission fuel reset')
MISSION.write_text(new_m)

# Cockpit warning logic: bingo and left/right imbalance are real fuel cautions.
cs=COCKSYS.read_text()
cs=rep(cs,'        public boolean stall,spin,lowFuel,gearUnsafe,highG,masterWarning;\n','        public boolean stall,spin,lowFuel,fuelImbalance,gearUnsafe,highG,masterWarning;\n','cockpit snapshot fuel flags')
cs=rep(cs,'        o.lowFuel=s.fuelKg<520.0;\n','        o.lowFuel=s.bingoFuel;o.fuelImbalance=s.fuelImbalanceCaution;\n','cockpit fuel evaluation')
cs=rep(cs,'        o.masterWarning=o.spin||o.stall||o.gearUnsafe||o.highG||o.lowFuel;\n','        o.masterWarning=o.spin||o.stall||o.gearUnsafe||o.highG||o.lowFuel||o.fuelImbalance;\n','cockpit fuel master warning')
cs=rep(cs,'        else if(o.lowFuel)o.primaryWarning="FUEL";\n','        else if(o.fuelImbalance)o.primaryWarning="FUEL IMBAL";\n        else if(o.lowFuel)o.primaryWarning="BINGO FUEL";\n','cockpit fuel warning text')
COCKSYS.write_text(cs)

# Cockpit FUEL MFD: five separate tank levels plus transfer, flow, imbalance and bingo.
c=COCKPIT.read_text()
c=rep(c,
'    private double alt,vs,spd,hdg,roll,pitch,aoa,beta,g,mach,q,fuel,fuelFrac,cg,gear,stall,spin,throttle,thrust,speedBrake;\n',
'    private double alt,vs,spd,hdg,roll,pitch,aoa,beta,g,mach,q,fuel,fuelFrac,cg,gear,stall,spin,throttle,thrust,speedBrake,fuelFwd,fuelAft,fuelL,fuelR,fuelExt,fuelFlow,fuelXfer,fuelImbal;\n    private boolean fuelBingo,fuelImbalCaution;\n',
'cockpit fuel fields')
c=rep(c,
'fuel=s.fuelKg;fuelFrac=s.fuelFraction01;cg=s.cgMac;gear=s.gearPosition;',
'fuel=s.fuelKg;fuelFrac=s.fuelFraction01;cg=s.cgMac;fuelFwd=s.fuelForwardKg;fuelAft=s.fuelAftKg;fuelL=s.fuelLeftKg;fuelR=s.fuelRightKg;fuelExt=s.fuelExternalKg;fuelFlow=s.fuelFlowKgSec;fuelXfer=s.fuelTransferKgSec;fuelImbal=s.fuelImbalanceKg;fuelBingo=s.bingoFuel;fuelImbalCaution=s.fuelImbalanceCaution;gear=s.gearPosition;',
'cockpit fuel telemetry')
old_match=re.search(r'    private void drawFuel\(Canvas c,int w,int h,float l,float r,float t,float b\)\{.*?\}\n\n    private void drawWarnings',c,re.S)
if not old_match:
    raise SystemExit('v101 fuel patch anchor missing: drawFuel method')
new_draw=r'''    private void drawFuel(Canvas c,int w,int h,float l,float r,float t,float b){
        int green=0xff68ff9c,amber=0xffffbd45,red=0xffff6559;text.setTextAlign(Paint.Align.LEFT);text.setTextSize(w*.0106f);
        float x=l,y=t+h*.022f,line=h*.025f;
        text.setColor(green);c.drawText(String.format(Locale.US,"TOTAL %4.0f kg   FLOW %.2f kg/s",fuel,fuelFlow),x,y,text);y+=line;
        c.drawText(String.format(Locale.US,"L %4.0f   R %4.0f",fuelL,fuelR),x,y,text);y+=line;
        c.drawText(String.format(Locale.US,"FWD %4.0f AFT %4.0f",fuelFwd,fuelAft),x,y,text);y+=line;
        c.drawText(String.format(Locale.US,"EXT %4.0f   XFER %.2f",fuelExt,fuelXfer),x,y,text);y+=line;
        text.setColor(fuelImbalCaution?amber:green);c.drawText(String.format(Locale.US,"IMBAL %+4.0f kg",fuelImbal),x,y,text);y+=line;
        text.setColor(fuelBingo?red:green);c.drawText(String.format(Locale.US,"BINGO %.0f kg   %s",FighterFuelSystemModel.BINGO_KG,fuelBingo?"BINGO":"NORM"),x,y,text);y+=line;
        text.setColor(green);c.drawText(String.format(Locale.US,"CG %.1f%% MAC",cg*100),x,y,text);
        float bx=r-(r-l)*.13f,barW=(r-l)*.035f,top=t+h*.025f,bot=b-h*.008f;
        double[] vals={fuelL/FighterFuelSystemModel.LEFT_CAP_KG,fuelR/FighterFuelSystemModel.RIGHT_CAP_KG,fuelFwd/FighterFuelSystemModel.FWD_CAP_KG,fuelAft/FighterFuelSystemModel.AFT_CAP_KG,fuelExt/FighterFuelSystemModel.EXT_CAP_KG};
        for(int i=0;i<5;i++){float xx=bx+i*barW*1.45f;p.setColor(0xff13231a);c.drawRect(xx,top,xx+barW,bot,p);p.setColor(green);float yy=bot-(bot-top)*(float)Math.max(0,Math.min(1,vals[i]));c.drawRect(xx,yy,xx+barW,bot,p);}
    }

    private void drawWarnings'''
c=c[:old_match.start()]+new_draw+c[old_match.end():]
# Import model used by the fuel page.
c=rep(c,'import com.mg.fixturecockpitsim.sim.FighterCockpitSystemsModel;\n','import com.mg.fixturecockpitsim.sim.FighterCockpitSystemsModel;\nimport com.mg.fixturecockpitsim.sim.FighterFuelSystemModel;\n','cockpit fuel import')
COCKPIT.write_text(c)

# Unit tests.
TEST.mkdir(parents=True,exist_ok=True)
(TEST/'FighterFuelSystemModelTest.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

import org.junit.Test;
import static org.junit.Assert.*;

public class FighterFuelSystemModelTest {
    @Test public void resetCreatesFiveTanksAndCorrectTotal(){
        FlightState s=new FlightState();FighterFuelSystemModel.reset(s);
        assertEquals(FighterFuelSystemModel.TOTAL_CAPACITY_KG,s.fuelKg,.01);
        assertTrue(s.fuelForwardKg>0&&s.fuelAftKg>0&&s.fuelLeftKg>0&&s.fuelRightKg>0&&s.fuelExternalKg>0);
        assertTrue(s.externalTankPresent);
    }
    @Test public void externalTankTransfersIntoFeedTanks(){
        FlightState s=new FlightState();FighterFuelSystemModel.reset(s);
        s.fuelLeftKg*=.40;s.fuelRightKg*=.40;double before=s.fuelExternalKg;
        FighterFuelSystemModel.update(s,.05);
        assertTrue(s.fuelExternalKg<before);assertTrue(s.fuelTransferKgSec>0);
    }
    @Test public void highPowerBurnReducesMassAndFuel(){
        FlightState s=new FlightState();FighterFuelSystemModel.reset(s);s.throttle=1.0;double f=s.fuelKg,m=s.massKg;
        for(int i=0;i<200;i++){s.timeSec+=.05;FighterFuelSystemModel.update(s,.05);}
        assertTrue(s.fuelKg<f);assertTrue(s.massKg<m);assertTrue(s.fuelFlowKgSec>4.0);
    }
    @Test public void imbalanceAndBingoAreDetected(){
        FlightState s=new FlightState();FighterFuelSystemModel.reset(s);s.fuelLeftKg=0;s.fuelRightKg=FighterFuelSystemModel.RIGHT_CAP_KG;FighterFuelSystemModel.update(s,0);
        assertTrue(s.fuelImbalanceCaution);
        s.fuelForwardKg=s.fuelAftKg=s.fuelLeftKg=s.fuelRightKg=s.fuelExternalKg=5;FighterFuelSystemModel.update(s,0);
        assertTrue(s.bingoFuel);
    }
}
''')

# Version.
g=GRADLE.read_text()
g=rep(g,'        versionCode 100\n','        versionCode 101\n','version code')
g=rep(g,"        versionName '26.18-avm28.0-cockpit-hardpoint-realism'\n","        versionName '26.19-avm29.0-fuel-management'\n",'version name')
GRADLE.write_text(g)

print('v101 fuel management applied: five tanks, external transfer, imbalance, bingo, mass/CG/inertia and cockpit FUEL page')
