package com.mg.machineelementspro;

import android.content.Context;
import android.content.SharedPreferences;

public final class ProjectStore {
    private static final String PREF="machine_elements_projects";
    private ProjectStore(){}
    public static void save(Context c,String name,String mode,String text,String params){SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);p.edit().putString("name",name).putString("mode",mode).putString("text",text).putString("params",params).apply();}
    public static Snapshot load(Context c){SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);return new Snapshot(p.getString("name","Untitled"),p.getString("mode","shaft"),p.getString("text",""),p.getString("params",""));}
    public static final class Snapshot{public final String name,mode,text,params;Snapshot(String n,String m,String t,String p){name=n;mode=m;text=t;params=p;}}
}
