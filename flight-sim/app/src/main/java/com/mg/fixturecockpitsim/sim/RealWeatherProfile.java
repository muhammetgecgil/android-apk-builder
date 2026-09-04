package com.mg.fixturecockpitsim.sim;

/** Maps current meteorological values into stable simulator visual/sea/terrain factors. */
public final class RealWeatherProfile {
    public static final int CLEAR=0,CLOUDY=1,RAIN=2,SNOW=3,FOG=4,STORM=5;

    public final int weatherCode,kind;
    public final double temperatureC,humidity01,precipitationMm,snowfallCm;
    public final double cloud01,lowCloud01,midCloud01,highCloud01;
    public final double visibilityM,windSpeedMps,windDirectionDeg,windGustMps;
    public final boolean isDay;
    public final double localDay01;
    public final double seaRoughness01,skyHaze01,mountainHaze01,snowCover01;

    private RealWeatherProfile(int weatherCode,int kind,double temperatureC,double humidity01,
            double precipitationMm,double snowfallCm,double cloud01,double lowCloud01,double midCloud01,
            double highCloud01,double visibilityM,double windSpeedMps,double windDirectionDeg,double windGustMps,
            boolean isDay,double localDay01,double seaRoughness01,double skyHaze01,double mountainHaze01,double snowCover01){
        this.weatherCode=weatherCode;this.kind=kind;this.temperatureC=temperatureC;this.humidity01=humidity01;
        this.precipitationMm=precipitationMm;this.snowfallCm=snowfallCm;this.cloud01=cloud01;
        this.lowCloud01=lowCloud01;this.midCloud01=midCloud01;this.highCloud01=highCloud01;
        this.visibilityM=visibilityM;this.windSpeedMps=windSpeedMps;this.windDirectionDeg=windDirectionDeg;
        this.windGustMps=windGustMps;this.isDay=isDay;this.localDay01=clamp(localDay01,0,1);
        this.seaRoughness01=seaRoughness01;this.skyHaze01=skyHaze01;this.mountainHaze01=mountainHaze01;this.snowCover01=snowCover01;
    }

    public static RealWeatherProfile fromValues(int code,double tempC,double humidityPct,double precipMm,double snowCm,
            double cloudPct,double lowCloudPct,double midCloudPct,double highCloudPct,double visibilityM,
            double windMps,double windDirDeg,double gustMps,boolean isDay,double localDay01){
        int kind=kindForCode(code,precipMm,snowCm,cloudPct);
        double humidity=clamp(humidityPct/100.0,0,1),cloud=clamp(cloudPct/100.0,0,1);
        double low=clamp(lowCloudPct/100.0,0,1),mid=clamp(midCloudPct/100.0,0,1),high=clamp(highCloudPct/100.0,0,1);
        double vis=Math.max(250.0,visibilityM);
        double visHaze=clamp(1.0-vis/28000.0,0,1),fog=kind==FOG?0.55:0.0,wet=clamp(precipMm/3.0,0,1);
        double skyHaze=clamp(visHaze*.72+low*.22+humidity*.12+wet*.16+fog,0,1);
        double mountainHaze=clamp(visHaze*.88+low*.32+humidity*.10+wet*.16+fog,0,1);
        double rough=clamp(.06+Math.max(0,windMps)/16.0+Math.max(0,gustMps-windMps)/24.0+wet*.12,0,1);
        double snowCover=clamp((snowCm>0?snowCm/1.5:0)+(kind==SNOW?.42:0)+(tempC<-2&&cloud>.65?.18:0),0,1);
        return new RealWeatherProfile(code,kind,tempC,humidity,Math.max(0,precipMm),Math.max(0,snowCm),cloud,low,mid,high,vis,
                Math.max(0,windMps),wrap360(windDirDeg),Math.max(0,gustMps),isDay,localDay01,rough,skyHaze,mountainHaze,snowCover);
    }

    public static int kindForCode(int code,double precipMm,double snowCm,double cloudPct){
        if(code>=95)return STORM;
        if(code==45||code==48)return FOG;
        if((code>=71&&code<=77)||code==85||code==86||snowCm>.02)return SNOW;
        if((code>=51&&code<=67)||(code>=80&&code<=82)||precipMm>.05)return RAIN;
        if((code>=1&&code<=3)||cloudPct>=45)return CLOUDY;
        return CLEAR;
    }

    public String kindLabel(){switch(kind){case CLOUDY:return "CLOUDY";case RAIN:return "RAIN";case SNOW:return "SNOW";case FOG:return "FOG";case STORM:return "STORM";default:return "CLEAR";}}
    private static double wrap360(double v){while(v>=360)v-=360;while(v<0)v+=360;return v;}
    private static double clamp(double v,double a,double b){return Math.max(a,Math.min(b,v));}
}
