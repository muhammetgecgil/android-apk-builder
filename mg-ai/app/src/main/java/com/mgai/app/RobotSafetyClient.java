package com.mgai.app;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

final class RobotSafetyClient {
    interface Callback { void onSuccess(String value); void onError(String error); }
    private RobotSafetyClient(){}
    static void health(String base,Callback cb){get(base,"/health",cb);}
    static void audit(String base,Callback cb){get(base,"/v1/safety/audit",cb);}
    private static void get(String baseEndpoint,String path,Callback cb){
        new Thread(()->{
            HttpURLConnection c=null;
            try{
                String base=baseEndpoint==null?"":baseEndpoint.trim();if(base.endsWith("/"))base=base.substring(0,base.length()-1);
                c=(HttpURLConnection)new URL(base+path).openConnection();c.setRequestMethod("GET");c.setConnectTimeout(12000);c.setReadTimeout(30000);
                int status=c.getResponseCode();InputStream s=status>=200&&status<300?c.getInputStream():c.getErrorStream();StringBuilder b=new StringBuilder();
                if(s!=null)try(BufferedReader r=new BufferedReader(new InputStreamReader(s,StandardCharsets.UTF_8))){String line;while((line=r.readLine())!=null)b.append(line).append('\n');}
                String text=b.toString().trim();if(status>=200&&status<300)cb.onSuccess(text);else cb.onError("HTTP "+status+": "+text);
            }catch(Exception e){cb.onError(e.getClass().getSimpleName()+": "+(e.getMessage()==null?"":e.getMessage()));}
            finally{if(c!=null)c.disconnect();}
        },"mg-robot-safety-network").start();
    }
}
