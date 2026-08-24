package com.mgai.app;

import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

final class TrainingClient {
    interface Callback { void onSuccess(String value); void onError(String error); }
    private TrainingClient() {}
    static void health(String base, Callback cb){ request(base,"/health","GET",null,cb); }
    static void createJob(String base,String baseModel,String datasetId,Callback cb){
        try{
            JSONObject body=new JSONObject().put("base_model",baseModel).put("dataset_id",datasetId).put("method","lora_sft");
            request(base,"/v1/training/jobs","POST",body,cb);
        }catch(Exception e){cb.onError(e.getMessage());}
    }
    static void listJobs(String base,Callback cb){ request(base,"/v1/training/jobs","GET",null,cb); }
    static void checkpoints(String base,Callback cb){ request(base,"/v1/training/checkpoints","GET",null,cb); }
    private static void request(String base,String path,String method,JSONObject body,Callback cb){
        new Thread(() -> {
            HttpURLConnection c=null;
            try{
                String b=base==null?"":base.trim(); if(b.endsWith("/"))b=b.substring(0,b.length()-1);
                c=(HttpURLConnection)new URL(b+path).openConnection(); c.setRequestMethod(method); c.setConnectTimeout(15000); c.setReadTimeout(120000);
                if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
                int status=c.getResponseCode(); InputStream s=status>=200&&status<300?c.getInputStream():c.getErrorStream(); StringBuilder out=new StringBuilder();
                if(s!=null)try(BufferedReader r=new BufferedReader(new InputStreamReader(s,StandardCharsets.UTF_8))){String line;while((line=r.readLine())!=null)out.append(line).append('\n');}
                String text=out.toString().trim(); if(status>=200&&status<300)cb.onSuccess(text); else cb.onError("HTTP "+status+": "+text);
            }catch(Exception e){cb.onError(e.getClass().getSimpleName()+": "+(e.getMessage()==null?"":e.getMessage()));}
            finally{if(c!=null)c.disconnect();}
        },"mg-training-network").start();
    }
}
