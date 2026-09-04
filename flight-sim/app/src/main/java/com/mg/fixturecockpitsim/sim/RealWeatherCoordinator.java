package com.mg.fixturecockpitsim.sim;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lightweight process-wide current-weather client for the simulated aircraft position. */
public final class RealWeatherCoordinator {
    private static final long REFRESH_MS=10*60*1000L, RETRY_MS=90*1000L;
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private static volatile double latitude=40.0,longitude=29.0,lastLat=999,lastLon=999;
    private static volatile long nextFetchMs;
    private static volatile boolean fetching,stale;
    private static volatile RealWeatherProfile profile;

    private RealWeatherCoordinator(){}

    public static void setLocation(double lat,double lon){
        if(!Double.isFinite(lat)||!Double.isFinite(lon)||lat<-90||lat>90||lon<-180||lon>180)return;
        latitude=lat;longitude=lon;
        if(Math.abs(lat-lastLat)>.30||Math.abs(lon-lastLon)>.30)nextFetchMs=0;
    }

    public static RealWeatherProfile getProfile(){return profile;}
    public static boolean isStale(){return stale;}
    public static boolean hasLiveProfile(){return profile!=null;}

    public static String statusLabel(){
        RealWeatherProfile p=profile;if(p==null)return "OFFLINE WX";
        return String.format(Locale.US,"%s %.0f°C / %s / CLD %.0f%% / WIND %.0f m/s",stale?"CACHED WX":"LIVE WX",p.temperatureC,p.kindLabel(),p.cloud01*100,p.windSpeedMps);
    }

    /** Meteorological direction is wind-from; sign is enough for the simulator crosswind convention. */
    public static float crosswindMps(double runwayHeadingDeg){
        RealWeatherProfile p=profile;if(p==null)return 0;
        return (float)(p.windSpeedMps*Math.sin(Math.toRadians(p.windDirectionDeg-runwayHeadingDeg)));
    }

    public static void requestIfNeeded(){
        long now=System.currentTimeMillis();if(fetching||now<nextFetchMs)return;
        fetching=true;final double lat=latitude,lon=longitude;lastLat=lat;lastLon=lon;nextFetchMs=now+REFRESH_MS;
        IO.execute(()->fetch(lat,lon));
    }

    private static void fetch(double lat,double lon){
        HttpURLConnection con=null;
        try{
            String vars="temperature_2m,relative_humidity_2m,precipitation,rain,snowfall,weather_code,cloud_cover,cloud_cover_low,cloud_cover_mid,cloud_cover_high,visibility,wind_speed_10m,wind_direction_10m,wind_gusts_10m,is_day";
            String u=String.format(Locale.US,"https://api.open-meteo.com/v1/forecast?latitude=%.5f&longitude=%.5f&current=%s&wind_speed_unit=ms&timezone=auto",lat,lon,vars);
            con=(HttpURLConnection)new URL(u).openConnection();con.setConnectTimeout(5500);con.setReadTimeout(5500);con.setRequestProperty("Accept","application/json");
            int code=con.getResponseCode();if(code<200||code>=300)throw new Exception("HTTP "+code);
            BufferedReader br=new BufferedReader(new InputStreamReader(con.getInputStream(),StandardCharsets.UTF_8));StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);br.close();
            JSONObject cur=new JSONObject(sb.toString()).getJSONObject("current");
            double wind=cur.optDouble("wind_speed_10m",2);
            profile=RealWeatherProfile.fromValues(cur.optInt("weather_code",0),cur.optDouble("temperature_2m",18),cur.optDouble("relative_humidity_2m",55),
                    cur.optDouble("precipitation",0),cur.optDouble("snowfall",0),cur.optDouble("cloud_cover",20),cur.optDouble("cloud_cover_low",0),
                    cur.optDouble("cloud_cover_mid",0),cur.optDouble("cloud_cover_high",0),cur.optDouble("visibility",30000),wind,
                    cur.optDouble("wind_direction_10m",270),cur.optDouble("wind_gusts_10m",wind),cur.optInt("is_day",1)!=0,parseLocalDay01(cur.optString("time","12:00")));
            stale=false;nextFetchMs=System.currentTimeMillis()+REFRESH_MS;
        }catch(Exception ignored){stale=profile!=null;nextFetchMs=System.currentTimeMillis()+RETRY_MS;}
        finally{fetching=false;if(con!=null)con.disconnect();}
    }

    private static double parseLocalDay01(String s){
        try{int t=s.indexOf('T');String hm=t>=0?s.substring(t+1):s;int h=Integer.parseInt(hm.substring(0,2)),m=Integer.parseInt(hm.substring(3,5));return (h+m/60.0)/24.0;}
        catch(Exception e){return .5;}
    }
}
