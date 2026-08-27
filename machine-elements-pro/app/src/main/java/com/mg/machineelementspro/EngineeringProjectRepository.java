package com.mg.machineelementspro;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class EngineeringProjectRepository {
    private static final String PREF="mep_engineering_projects_v2";
    private static final String INDEX="project_ids";
    private static final String ACTIVE="active_project_id";
    private EngineeringProjectRepository(){}

    public static void save(Context c,EngineeringProject p){
        SharedPreferences sp=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        Set<String> ids=new HashSet<>(sp.getStringSet(INDEX,Collections.emptySet()));ids.add(p.id);
        sp.edit().putString("p_"+p.id,p.encode()).putStringSet(INDEX,ids).apply();
    }
    public static EngineeringProject load(Context c,String id){
        String raw=c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString("p_"+id,null);
        return raw==null?null:EngineeringProject.decode(raw);
    }
    public static List<EngineeringProject> list(Context c){
        SharedPreferences sp=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);List<EngineeringProject> out=new ArrayList<>();
        for(String id:sp.getStringSet(INDEX,Collections.emptySet())){String raw=sp.getString("p_"+id,null);if(raw!=null)try{out.add(EngineeringProject.decode(raw));}catch(Exception ignored){}}
        Collections.sort(out,(a,b)->Long.compare(b.updatedAt,a.updatedAt));return out;
    }
    public static void setActive(Context c,String id){c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString(ACTIVE,id).apply();}
    public static String activeId(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(ACTIVE,"");}
    public static EngineeringProject active(Context c){String id=activeId(c);return id.isEmpty()?null:load(c,id);}
    public static void delete(Context c,String id){
        SharedPreferences sp=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);Set<String> ids=new HashSet<>(sp.getStringSet(INDEX,Collections.emptySet()));ids.remove(id);
        SharedPreferences.Editor e=sp.edit().remove("p_"+id).putStringSet(INDEX,ids);if(id.equals(sp.getString(ACTIVE,"")))e.remove(ACTIVE);e.apply();
    }
}
