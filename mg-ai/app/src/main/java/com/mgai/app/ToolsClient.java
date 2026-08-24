package com.mgai.app;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

final class ToolsClient {
    interface Callback { void onSuccess(JSONObject value); void onError(String message); }
    private ToolsClient() {}

    static void calculator(String base, String expr, Callback cb) {
        try {
            JSONObject body = new JSONObject();
            body.put("expression", expr);
            JSONArray perms = new JSONArray(); perms.put("execute");
            body.put("permissions", perms);
            post(base, "/v1/tools/calculator", body, cb);
        } catch (Exception e) { cb.onError(e.getMessage()); }
    }

    static void plan(String base, String goal, Callback cb) {
        try {
            JSONObject body = new JSONObject(); body.put("goal", goal);
            JSONArray tools = new JSONArray(); tools.put("calculator").put("memory_query").put("research").put("python_sandbox");
            JSONArray perms = new JSONArray(); perms.put("execute").put("read").put("network");
            body.put("available_tools", tools); body.put("permissions", perms);
            post(base, "/v1/agent/plan", body, cb);
        } catch (Exception e) { cb.onError(e.getMessage()); }
    }

    private static void post(String base, String path, JSONObject body, Callback cb) {
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                String b = base == null ? "" : base.trim(); if (b.endsWith("/")) b=b.substring(0,b.length()-1);
                c=(HttpURLConnection)new URL(b+path).openConnection(); c.setRequestMethod("POST"); c.setDoOutput(true);
                c.setConnectTimeout(30000); c.setReadTimeout(120000); c.setRequestProperty("Content-Type","application/json; charset=utf-8");
                try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
                int status=c.getResponseCode(); InputStream in=status>=200&&status<300?c.getInputStream():c.getErrorStream();
                String text=read(in); if(status<200||status>=300){cb.onError("HTTP "+status+": "+text);return;} cb.onSuccess(new JSONObject(text));
            } catch(Exception e){cb.onError(e.getClass().getSimpleName()+": "+(e.getMessage()==null?"":e.getMessage()));}
            finally{if(c!=null)c.disconnect();}
        },"mg-tools-network").start();
    }
    private static String read(InputStream in) throws Exception { if(in==null)return""; StringBuilder b=new StringBuilder(); try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String l; while((l=r.readLine())!=null)b.append(l).append('\n');} return b.toString().trim(); }
}
