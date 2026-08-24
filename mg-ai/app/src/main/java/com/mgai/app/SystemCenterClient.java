package com.mgai.app;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SystemCenterClient {
    public static JSONObject status(String endpoint) throws Exception {
        String base=endpoint==null?"":endpoint.trim();
        while(base.endsWith("/")) base=base.substring(0,base.length()-1);
        URL u=new URL(base+"/v1/system/status");
        HttpURLConnection c=(HttpURLConnection)u.openConnection();
        c.setRequestMethod("GET"); c.setConnectTimeout(5000); c.setReadTimeout(8000);
        int code=c.getResponseCode();
        BufferedReader br=new BufferedReader(new InputStreamReader(code>=200&&code<300?c.getInputStream():c.getErrorStream()));
        StringBuilder sb=new StringBuilder(); String line; while((line=br.readLine())!=null) sb.append(line);
        if(code<200||code>=300) throw new IllegalStateException("HTTP "+code+" "+sb);
        return new JSONObject(sb.toString());
    }
}
