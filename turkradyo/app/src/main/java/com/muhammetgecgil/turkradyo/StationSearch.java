package com.muhammetgecgil.turkradyo;

import java.text.Normalizer;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Name matching for Android Auto voice search; never changes the saved queue. */
final class StationSearch {
    private StationSearch(){}
    static int find(JSONArray queue,String query){
        String wanted=normalize(query);
        if(wanted.isEmpty())return -1;
        int partial=-1;
        for(int i=0;i<queue.length();i++){
            JSONObject station=queue.optJSONObject(i);
            if(station==null||station.optString("url").isEmpty())continue;
            String name=normalize(station.optString("name"));
            if(name.equals(wanted))return i;
            if(partial<0&&name.contains(wanted))partial=i;
        }
        return partial;
    }
    private static String normalize(String value){
        return Normalizer.normalize(value==null?"":value,Normalizer.Form.NFD)
                .replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).replace('ı','i')
                .replaceAll("[^a-z0-9]+"," ").trim().replaceAll("\\s+"," ");
    }
}
