package com.muhammetgecgil.pcbzekapro;

import java.util.*;

final class OfflineComponentDb {
    private static final Map<String,String> DB=new LinkedHashMap<>();
    static {DB.put("LM358","Çift op-amp • 3–32 V • DIP/SOIC-8");DB.put("NE555","Zamanlayıcı IC • DIP/SOIC-8");DB.put("AMS1117","LDO regülatör • SOT-223 • çıkış son eke göre");DB.put("SS14","Schottky diyot • 1 A • 40 V • SMA");DB.put("1N4148","Hızlı sinyal diyodu");DB.put("AO3400","N-kanal MOSFET • SOT-23");DB.put("2N7002","N-kanal MOSFET • SOT-23");DB.put("BC817","NPN transistör • SOT-23");DB.put("TL431","Ayarlanabilir şönt referans");DB.put("PC817","Optokuplör • 4 pin");DB.put("ULN2003","7 kanal Darlington sürücü");DB.put("CH340","USB-UART dönüştürücü");}
    static String lookup(String code){String u=code==null?"":code.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]","");if(u.length()<2)return "Önce bir parça kodu okutun.";for(Map.Entry<String,String>e:DB.entrySet())if(u.contains(e.getKey())||e.getKey().contains(u))return e.getKey()+"\n"+e.getValue()+"\nÇevrimdışı kayıt; paket ve datasheet ile doğrulayın.";return "Çevrimdışı veritabanında kesin eşleşme yok. İnternet datasheet aramasını veya ÖĞRET modunu kullanın.";}
    static String stats(){return "Çevrimdışı çekirdek kayıt: "+DB.size()+"\nDirenç/bobin/SMD kod çözücü ayrıca çevrimdışı çalışır.";}
}
