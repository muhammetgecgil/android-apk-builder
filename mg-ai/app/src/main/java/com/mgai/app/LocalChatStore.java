package com.mgai.app;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

public final class LocalChatStore {
    private static final String PREFS="mg_local_chat";
    private static final String KEY="messages";
    private static final int MAX=40;
    private LocalChatStore(){}

    public static synchronized void add(Context c,String role,String text){
        try{
            if("user".equals(role)) LocalVoiceOutput.handleCommand(c,text);
            JSONArray arr=loadArray(c);
            JSONObject o=new JSONObject();o.put("role",role);o.put("text",text);o.put("ts",System.currentTimeMillis());arr.put(o);
            while(arr.length()>MAX){JSONArray n=new JSONArray();for(int i=1;i<arr.length();i++)n.put(arr.get(i));arr=n;}
            c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,arr.toString()).apply();
            if("assistant".equals(role)) LocalVoiceOutput.speak(c,text);
        }catch(Exception ignored){}
    }
    public static synchronized String transcript(Context c,int maxTurns){
        try{
            JSONArray a=loadArray(c);StringBuilder sb=new StringBuilder();int start=Math.max(0,a.length()-Math.max(1,maxTurns*2));
            for(int i=start;i<a.length();i++){JSONObject o=a.getJSONObject(i);String r=o.optString("role","user");String t=o.optString("text","");if(t.isEmpty())continue;sb.append("user".equals(r)?"Kullanıcı: ":"MG-AI: ").append(t).append('\n');}
            return sb.toString();
        }catch(Exception e){return "";}
    }
    public static synchronized String historyText(Context c){return transcript(c,20);}
    public static synchronized void clear(Context c){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().remove(KEY).apply();}
    private static JSONArray loadArray(Context c){String s=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,"[]");try{return new JSONArray(s);}catch(Exception e){return new JSONArray();}}
}
