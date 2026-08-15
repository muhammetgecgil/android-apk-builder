package com.muhammetgecgil.pcbzekapro;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import java.util.*;

final class ServiceEngine {
    static String functionalAreas(List<ComponentDecoder.Item> bom){
        int power=0,logic=0,analog=0,io=0,protect=0;
        for(ComponentDecoder.Item x:bom){String s=(x.ref+" "+x.code+" "+x.type).toUpperCase();
            if(s.matches(".*(REG|7805|AMS|LM2596|XL[0-9]|DIODE|BOBIN|MOSFET).*"))power++;
            if(s.matches(".*(MCU|STM|ESP|ATMEGA|FLASH|EEPROM|U[0-9]).*"))logic++;
            if(s.matches(".*(OPAMP|LM358|TL0|ADC|DAC|SENSOR).*"))analog++;
            if(s.matches(".*(USB|CAN|LIN|UART|RS485|J[0-9]|CONN).*"))io++;
            if(s.matches(".*(FUSE|TVS|MOV|F[0-9]).*"))protect++;
        }
        return "GÜÇ/REGÜLASYON: "+power+" aday\nMANTIK/HAFIZA: "+logic+" aday\nANALOG/ÖLÇÜM: "+analog+" aday\nHABERLEŞME/GİRİŞ-ÇIKIŞ: "+io+" aday\nKORUMA: "+protect+" aday\n\nBu sınıflandırma baskı kodu ve referanslara dayanır; devre yolu doğrulaması gerekir.";
    }
    static String faultTree(String symptom){
        if(symptom.contains("Açılmıyor"))return "1) Giriş sigortası/süreklilik\n2) Ters polarite/TVS diyot kısa devre\n3) Giriş gerilimi\n4) Regülatör giriş/çıkış\n5) MCU reset ve saat\n6) Akım sınırlı besleme ile doğrulama";
        if(symptom.contains("Akım"))return "1) Enerjiyi kesin, direnç modunda besleme-GND kontrolü\n2) Akım sınırlı kaynak kullanın\n3) Isınan bölgeyi harici termal kamera ile bulun\n4) TVS, MOSFET ve seramik kondansatörleri bölerek test edin";
        if(symptom.contains("Haberleş"))return "1) Konnektör/GND\n2) Hat sonlandırma direnci\n3) TX/RX veya CANH/CANL diyot modu\n4) Transceiver beslemesi\n5) Osiloskopla fiziksel katman";
        if(symptom.contains("Isın"))return "1) Akım sınırı koyun\n2) Harici termal kamera bağlayın\n3) Regülatör/MOSFET/TVS bölgesini inceleyin\n4) Kısa devreli kondansatörü bölerek bulun";
        return "1) Belirtiyi ve besleme değerini kaydedin\n2) Görsel kontrol\n3) Güç ağacı\n4) Saat/reset\n5) Giriş-çıkış sinyal zinciri\n6) Ölçümleri servis raporuna ekleyin";
    }
    static String usbStatus(Context c){
        UsbManager m=(UsbManager)c.getSystemService(Context.USB_SERVICE);StringBuilder s=new StringBuilder();
        for(UsbDevice d:m.getDeviceList().values())s.append(d.getDeviceName()).append(" • VID ").append(d.getVendorId()).append(" PID ").append(d.getProductId()).append(" • ").append(d.getProductName()==null?"USB cihaz":d.getProductName()).append('\n');
        return s.length()==0?"USB/OTG cihazı bağlı değil. UVC mikroskop veya desteklenen termal kamera bağlandığında burada listelenir.":s.toString();
    }
    static String provisionalNetlist(List<ComponentDecoder.Item> bom){
        StringBuilder s=new StringBuilder("GÖRSEL NETLİST ÇALIŞMA LİSTESİ\n\n");
        for(ComponentDecoder.Item x:bom)s.append(x.ref).append(" [").append(x.type).append("] ").append(x.value).append(" → bağlantı uçlarını AR prob ile doğrula\n");
        s.append("\nKamera yalnız görünür yolu izleyebilir; çok katmanlı PCB iç bağlantıları ölçüm olmadan kesinleştirilemez.");
        return s.toString();
    }
}
