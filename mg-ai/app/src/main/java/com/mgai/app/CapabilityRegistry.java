package com.mgai.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CapabilityRegistry {
    public enum State { PLANNED, INTEGRATING, TESTED, ACTIVE }

    public static final class Capability {
        public final String id, name, requirementFamily, verification;
        public final State state;
        Capability(String id,String name,String requirementFamily,State state,String verification){this.id=id;this.name=name;this.requirementFamily=requirementFamily;this.state=state;this.verification=verification;}
    }

    private static final List<Capability> ITEMS;
    static {
        List<Capability> x=new ArrayList<>();
        x.add(new Capability("CAP-CORE","MG-Core / Foundation Model","CORE,TOK,EMB,ATT,PRE,CPT,IFT,PREF,MOE,SPEC",State.INTEGRATING,"Android adapter + vLLM/Qwen server contract tested; live GPU inference host pending"));
        x.add(new Capability("CAP-RSN","Reasoning & Metacognition","RTR,RSN,HYP,CRT,DBT,MET,SRCH,MCTS,PLAN,GOAL,REFL,BUD",State.TESTED,"Reasoner/Critic/Revision/Verifier service contract CI passed; live-model quality evaluation pending"));
        x.add(new Capability("CAP-VER","Truth, Verification & Calibration","VER,DIM,CONF,UNC,HAL,PROV,HON,ANS,REG,EVG",State.INTEGRATING,"Verifier role integrated; calibration, dimensional checker and release gates remain"));
        x.add(new Capability("CAP-RES","Research & Knowledge","RES,SRC,IND,CON,KAT,TMP,FRE,REV,POI,QUA",State.TESTED,"Research API contract, search adapters, source scoring, SSRF guard and Android integration tested; live hosted provider pending"));
        x.add(new Capability("CAP-MEM","Long-Term Memory","MEM,WRK,EPI,SEM,PRC,USR,SKL,FLR,RMM,FOR,MCO,LCT,PRJ,DEC",State.TESTED,"RAG/memory contract CI, pgvector schema and Android integration passed; production DB/embedding host pending"));
        x.add(new Capability("CAP-TOOL","Agents & Tools","TOOL,CODE,AGT,DYN,CAS,CNS,DIV,PERM,ACT,EVT",State.INTEGRATING,"Tool registry, permission engine, safe calculator and agent plan API added; sandbox execution and dynamic agents remain"));
        x.add(new Capability("CAP-MM","Multimodal Perception","MM,VIS,ENGV,AUD,VID,SEN,OCR,3D",State.PLANNED,"Not yet integrated"));
        x.add(new Capability("CAP-WORLD","World Model / Science / Engineering","WMD,CAU,CF,PHY,SIM,SCI,MATH,ENG,REQ,TRC,DTW,PRED,ANO",State.PLANNED,"Not yet integrated"));
        x.add(new Capability("CAP-LEARN","Learning & Self-Improvement","EXP,TEA,MTD,WEBL,CL,SYN,CUR,HEM,AL,ASI,ARS,NAS,RIM",State.PLANNED,"Not yet integrated"));
        x.add(new Capability("CAP-EVAL","Evaluation & Benchmarking","BMF,BMC,RWE,INT,CAL,RED,LAT,COST",State.PLANNED,"Not yet integrated"));
        x.add(new Capability("CAP-INFRA","Infrastructure / MLOps","API,ORC,INF,EDGE,DATA,OBS,SEC,PRIV,REL,REGY,DSR",State.INTEGRATING,"Android CI plus service contract CI active; production hosting/observability pending"));
        x.add(new Capability("CAP-PROD","Android / Web / Governance","PERS,UX,MOB,WEB,GOV,AUDT,ROL,INC,CMP,DOC",State.INTEGRATING,"Android dashboard/chat/research/memory/reasoning/tools screens integrated"));
        x.add(new Capability("CAP-ROBOT","Embodied AI / Robot Brain","EMB-001..020,R27,R28,R29,R30",State.PLANNED,"Architecture defined; no actuator control enabled"));
        x.add(new Capability("CAP-SAFE","Robotics Safety Supervisor","EMB-002..005,008..011,015..020",State.PLANNED,"Deterministic independent safety layer required before physical action"));
        x.add(new Capability("CAP-RPER","Robot Perception & World Model","RGB,DEPTH,LIDAR,IMU,ENCODER,F/T,TACTILE,SLAM,POSE",State.PLANNED,"Not yet integrated"));
        x.add(new Capability("CAP-RACT","Robot Planning / Control / Manipulation","TASK,MOTION,IK,FK,TRAJ,COLLISION,GRASP,FORCE,TELEOP",State.PLANNED,"Not yet integrated"));
        ITEMS=Collections.unmodifiableList(x);
    }
    private CapabilityRegistry(){}
    public static List<Capability> all(){return ITEMS;}
    public static int count(State s){int n=0;for(Capability c:ITEMS)if(c.state==s)n++;return n;}
    public static String summary(){return "Aktif "+count(State.ACTIVE)+" • Testli "+count(State.TESTED)+" • Entegrasyonda "+count(State.INTEGRATING)+" • Planlı "+count(State.PLANNED);}
}
