package com.mgai.app;

import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

final class RobotWorldClient {
    interface Callback { void onSuccess(String value); void onError(String error); }
    private RobotWorldClient(){}

    static void health(String base, Callback cb){ request(base,"/health","GET",null,cb); }
    static void snapshot(String base, Callback cb){ request(base,"/v1/world/snapshot","GET",null,cb); }

    static void sendPrototypeFrames(String base, long ts, Callback cb){
        try{
            JSONObject body=new JSONObject()
                    .put("parent","base_link").put("child","android_sensor")
                    .put("translation_xyz",new org.json.JSONArray().put(0).put(0).put(0))
                    .put("quaternion_xyzw",new org.json.JSONArray().put(0).put(0).put(0).put(1))
                    .put("observed_at_ms",ts).put("valid_for_ms",30000)
                    .put("confidence",0.5).put("source","android-prototype");
            request(base,"/v1/frames/transform","POST",body,cb);
        }catch(Exception e){cb.onError(e.getMessage());}
    }

    static void sendPerceptionOnlyState(String base, long ts, Callback cb){
        try{
            JSONObject body=new JSONObject()
                    .put("frame_id","base_link").put("observed_at_ms",ts)
                    .put("localization_confidence",0.0).put("mode","perception_only")
                    .put("estop","unknown").put("protective_stop",false)
                    .put("source","android-prototype");
            request(base,"/v1/robot/state","POST",body,cb);
        }catch(Exception e){cb.onError(e.getMessage());}
    }

    private static void request(String baseEndpoint,String path,String method,JSONObject body,Callback cb){
        new Thread(() -> {
            HttpURLConnection c=null;
            try{
                String base=baseEndpoint==null?"":baseEndpoint.trim(); if(base.endsWith("/"))base=base.substring(0,base.length()-1);
                c=(HttpURLConnection)new URL(base+path).openConnection(); c.setRequestMethod(method); c.setConnectTimeout(15000); c.setReadTimeout(60000);
                if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
                int status=c.getResponseCode(); InputStream s=status>=200&&status<300?c.getInputStream():c.getErrorStream();
                StringBuilder b=new StringBuilder(); if(s!=null)try(BufferedReader r=new BufferedReader(new InputStreamReader(s,StandardCharsets.UTF_8))){String line;while((line=r.readLine())!=null)b.append(line).append('\n');}
                String text=b.toString().trim(); if(status>=200&&status<300)cb.onSuccess(text);else cb.onError("HTTP "+status+": "+text);
            }catch(Exception e){cb.onError(e.getClass().getSimpleName()+": "+(e.getMessage()==null?"":e.getMessage()));}
            finally{if(c!=null)c.disconnect();}
        },"mg-robot-world-network").start();
    }
}
