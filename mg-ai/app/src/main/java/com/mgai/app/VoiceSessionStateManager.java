package com.mgai.app;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public final class VoiceSessionStateManager {
    public enum State { IDLE, LISTENING, TRANSCRIBING, THINKING, SPEAKING, BARGE_IN }
    public interface Listener { void onStateChanged(State state, String label, long ageMs); }

    private static final AtomicReference<State> state=new AtomicReference<>(State.IDLE);
    private static final CopyOnWriteArrayList<Listener> listeners=new CopyOnWriteArrayList<>();
    private static volatile long changedAt=System.currentTimeMillis();
    private static volatile long lastStageDurationMs=0;
    private static volatile State lastCompletedStage=State.IDLE;
    private static volatile String lastError="";

    private VoiceSessionStateManager(){}

    public static State get(){return state.get();}
    public static boolean is(State s){return state.get()==s;}
    public static long ageMs(){return Math.max(0,System.currentTimeMillis()-changedAt);}
    public static long lastStageDurationMs(){return lastStageDurationMs;}
    public static State lastCompletedStage(){return lastCompletedStage;}
    public static String lastError(){return lastError;}

    public static void addListener(Listener l){if(l!=null){listeners.addIfAbsent(l);safeNotify(l,state.get());}}
    public static void removeListener(Listener l){if(l!=null)listeners.remove(l);}

    public static synchronized void set(State next){
        if(next==null)return;
        long now=System.currentTimeMillis();
        State prev=state.get();
        if(prev!=next){
            lastStageDurationMs=Math.max(0,now-changedAt);
            lastCompletedStage=prev;
        }
        state.set(next);changedAt=now;
        notifyAllListeners(next);
    }

    public static synchronized boolean transition(State from,State to){
        if(state.get()!=from)return false;
        set(to);return true;
    }

    public static synchronized void reset(){set(State.IDLE);}
    public static synchronized void reportError(String message){
        lastError=message==null?"":message.trim();
        notifyAllListeners(state.get());
    }
    public static synchronized void clearError(){lastError="";notifyAllListeners(state.get());}

    public static String label(State s){
        if(s==null)s=State.IDLE;
        switch(s){
            case LISTENING:return "Dinliyor";
            case TRANSCRIBING:return "Yazıya çeviriyor";
            case THINKING:return "Düşünüyor";
            case SPEAKING:return "Konuşuyor";
            case BARGE_IN:return "Araya giriş algılandı";
            default:return "Hazır";
        }
    }

    public static String detail(){
        StringBuilder b=new StringBuilder();
        b.append(label(state.get())).append(" • ").append(ageMs()).append(" ms");
        if(lastCompletedStage!=State.IDLE||lastStageDurationMs>0){
            b.append("\nSon aşama: ").append(label(lastCompletedStage)).append(" • ").append(lastStageDurationMs).append(" ms");
        }
        if(!lastError.isEmpty())b.append("\nHata: ").append(lastError);
        return b.toString();
    }

    public static String summary(){return detail();}

    private static void notifyAllListeners(State s){for(Listener l:listeners)safeNotify(l,s);}
    private static void safeNotify(Listener l,State s){try{l.onStateChanged(s,label(s),ageMs());}catch(Throwable ignored){}}
}
