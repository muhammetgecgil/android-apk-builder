package com.mg.machineelementspro;

import java.util.*;

public final class BearingFamilyEngine {
 public enum Type { DEEP_GROOVE, ANGULAR_CONTACT, SPHERICAL_ROLLER, TAPERED_ROLLER, NEEDLE, ONE_WAY_SPRAG, ONE_WAY_NEEDLE, CAM_CLUTCH }
 public static final class Spec {
  public final String code,name; public final Type type; public final double d,D,B,C,C0,maxRpm,torqueNm;
  public Spec(String code,String name,Type type,double d,double D,double B,double C,double C0,double maxRpm,double torqueNm){this.code=code;this.name=name;this.type=type;this.d=d;this.D=D;this.B=B;this.C=C;this.C0=C0;this.maxRpm=maxRpm;this.torqueNm=torqueNm;}
 }
 private static final List<Spec> DATA=Arrays.asList(
  new Spec("6204-2Z","Sabit bilyalı rulman",Type.DEEP_GROOVE,20,47,14,13500,6550,17000,0),
  new Spec("7204-B","Açısal temaslı bilyalı",Type.ANGULAR_CONTACT,20,47,14,14300,7200,15000,0),
  new Spec("22205","Oynak makaralı",Type.SPHERICAL_ROLLER,25,52,18,34000,33500,9500,0),
  new Spec("30205","Konik makaralı",Type.TAPERED_ROLLER,25,52,16.25,32200,33500,11000,0),
  new Spec("NK 20/16","İğneli rulman",Type.NEEDLE,20,28,16,17900,25700,18000,0),
  new Spec("CSK 20","Sprag tip tek yönlü",Type.ONE_WAY_SPRAG,20,47,14,17200,23000,8000,62),
  new Spec("HF 2016","İğneli tek yönlü",Type.ONE_WAY_NEEDLE,20,26,16,0,0,12000,33),
  new Spec("RCB 121616","Kavramalı tek yönlü",Type.CAM_CLUTCH,12,28,16,0,0,9000,45),
  new Spec("CSK 30","Sprag tip tek yönlü",Type.ONE_WAY_SPRAG,30,62,16,28000,39000,6000,110)
 );
 public static List<Spec> all(){return new ArrayList<>(DATA);}
 public static List<Spec> filter(Type type,double bore,double requiredC,double torqueNm,double rpm){List<Spec> out=new ArrayList<>();for(Spec s:DATA){if(type!=null&&s.type!=type)continue;if(bore>0&&s.d+0.001<bore)continue;if(requiredC>0&&s.C>0&&s.C<requiredC)continue;if(torqueNm>0&&s.torqueNm>0&&s.torqueNm<torqueNm)continue;if(rpm>0&&s.maxRpm<rpm)continue;out.add(s);}return out;}
 public static String behavior(Type t){switch(t){case ONE_WAY_SPRAG:return "Sprag elemanlar bir yönde kilitlenir, ters yönde serbest döner.";case ONE_WAY_NEEDLE:return "İğneli kavrama, kompakt hacimde tek yönlü tork aktarımı sağlar.";case CAM_CLUTCH:return "Kam geometrisi ile bir yönde kavrar, diğer yönde overrunning yapar.";case ANGULAR_CONTACT:return "Kombine radyal ve eksenel yükler için temas açılı bilya geometrisi.";case SPHERICAL_ROLLER:return "Kaçıklık toleranslı, yüksek radyal yük kapasiteli çift sıra makara.";case TAPERED_ROLLER:return "Konik yuvarlanma elemanlarıyla radyal ve eksenel yük kombinasyonu.";case NEEDLE:return "Düşük kesit yüksek radyal kapasite için uzun ince makaralar.";default:return "Genel amaçlı, düşük sürtünmeli radyal bilyalı rulman.";}}
 private BearingFamilyEngine(){}
}
