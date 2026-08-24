package com.mgai.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class ModelRegistryClient {
    private ModelRegistryClient(){}

    public static String get(String url) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(5000); c.setReadTimeout(8000); c.setRequestMethod("GET");
        return read(c);
    }

    public static String post(String url,String json) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(5000); c.setReadTimeout(8000); c.setRequestMethod("POST"); c.setDoOutput(true);
        c.setRequestProperty("Content-Type","application/json");
        try(OutputStream os=c.getOutputStream()){os.write(json.getBytes("UTF-8"));}
        return read(c);
    }

    private static String read(HttpURLConnection c) throws Exception {
        int code=c.getResponseCode();
        BufferedReader br=new BufferedReader(new InputStreamReader(code>=200&&code<300?c.getInputStream():c.getErrorStream()));
        StringBuilder sb=new StringBuilder(); String line;
        while((line=br.readLine())!=null) sb.append(line).append('\n');
        br.close(); c.disconnect();
        if(code<200||code>=300) throw new Exception("HTTP "+code+": "+sb.toString().trim());
        return sb.toString().trim();
    }
}
