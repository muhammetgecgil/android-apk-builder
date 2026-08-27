package com.mg.machineelementspro;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EngineeringProject {
    public final String id;
    public String name;
    public int revision;
    public long updatedAt;
    public final List<Element> elements=new ArrayList<>();

    public EngineeringProject(String id,String name){
        if(id==null||id.trim().isEmpty())throw new IllegalArgumentException("Project id required");
        this.id=id;this.name=(name==null||name.trim().isEmpty())?"Untitled":name.trim();this.revision=1;this.updatedAt=System.currentTimeMillis();
    }

    public Element upsert(String elementId,String type){
        for(Element e:elements)if(e.id.equals(elementId)){if(!e.type.equals(type))throw new IllegalArgumentException("Element type mismatch");touch();return e;}
        Element e=new Element(elementId,type);elements.add(e);touch();return e;
    }
    public Element find(String elementId){for(Element e:elements)if(e.id.equals(elementId))return e;return null;}
    public void bumpRevision(){revision++;touch();}
    private void touch(){updatedAt=System.currentTimeMillis();}

    public String encode(){
        StringBuilder b=new StringBuilder();
        b.append(enc(id)).append('|').append(enc(name)).append('|').append(revision).append('|').append(updatedAt).append('\n');
        for(Element e:elements){
            b.append("E|").append(enc(e.id)).append('|').append(enc(e.type));
            for(Map.Entry<String,String> x:e.values.entrySet())b.append('|').append(enc(x.getKey())).append('=').append(enc(x.getValue()));
            b.append('\n');
        }
        return b.toString();
    }
    public static EngineeringProject decode(String s){
        if(s==null||s.trim().isEmpty())throw new IllegalArgumentException("Empty project data");
        String[] lines=s.split("\\n");String[] h=lines[0].split("\\|",-1);
        EngineeringProject p=new EngineeringProject(dec(h[0]),dec(h[1]));p.revision=Integer.parseInt(h[2]);p.updatedAt=Long.parseLong(h[3]);p.elements.clear();
        for(int i=1;i<lines.length;i++){
            if(lines[i].trim().isEmpty())continue;String[] a=lines[i].split("\\|",-1);if(a.length<3||!"E".equals(a[0]))continue;
            Element e=new Element(dec(a[1]),dec(a[2]));
            for(int j=3;j<a.length;j++){int k=a[j].indexOf('=');if(k>0)e.values.put(dec(a[j].substring(0,k)),dec(a[j].substring(k+1)));}
            p.elements.add(e);
        }
        return p;
    }
    private static String enc(String s){return Base64.getUrlEncoder().withoutPadding().encodeToString((s==null?"":s).getBytes(StandardCharsets.UTF_8));}
    private static String dec(String s){return new String(Base64.getUrlDecoder().decode(s),StandardCharsets.UTF_8);}

    public static final class Element {
        public final String id,type;public final Map<String,String> values=new LinkedHashMap<>();
        Element(String id,String type){if(id==null||id.trim().isEmpty()||type==null||type.trim().isEmpty())throw new IllegalArgumentException("Element id/type required");this.id=id.trim();this.type=type.trim();}
        public Element put(String key,Object value){values.put(key,String.valueOf(value));return this;}
        public String get(String key){return values.get(key);}
        public double getDouble(String key){String v=values.get(key);if(v==null)throw new IllegalArgumentException("Missing "+key);return Double.parseDouble(v);}
    }
}
