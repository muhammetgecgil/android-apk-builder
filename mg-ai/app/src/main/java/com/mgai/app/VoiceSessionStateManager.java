package com.mgai.app;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public final class VoiceSessionStateManager {
    public enum State { IDLE, LISTENING, TRANSCRIBING, THINKING, SPEAKING, BARGE_IN }
    public interface Listener { void onStateChanged(State state,long changedAt); }

    private static final AtomicReference<State> state=new AtomicReference<>(State.IDLE);
    private static final List<Listener> listeners=new CopyOnWriteArrayList<>();
    private static volatile long changedAt=System.currentTimeMillis();
    private VoiceSessionStateManager(){}

    public static State get(){return state.get();}
    public static long changedAt(){return changedAt;}
    public static boolean is(State s){return state.get()==s;}
    public static long ageMs(){return Math.max(0,System.currentTimeMillis()-changedAt);}

    public static void addListener(Listener l){if(l!=null){listeners.add(l);try{l.onStateChanged(state.get(),changedAt);}catch(Throwable ignored){}}}
    public static void removeListener(Listener l){if(l!=null)listeners.remove(l);}

    public static synchronized void set(State next){
        if(next==null)return;
        State prev=state.get();
        if(prev==next)return;
        state.set(next);changedAt=System.currentTimeMillis();notifyListeners(next,changedAt);
    }

    public static synchronized boolean transition(State from,State to){
        if(state.get()!=from||to==null)return false;
        state.set(to);changedAt=System.currentTimeMillis();notifyListeners(to,changedAt);return true;
    }

    public static synchronized void reset(){set(State.IDLE);}

    public static String label(){
        switch(state.get()){
            case LISTENING:return "Dinliyor";
            case TRANSCRIBING:return "Yazıya çeviriyor";
            case THINKING:return "Düşünüyor";
            case SPEAKING:return "Konuşuyor";
            case BARGE_IN:return "Araya giriş algılandı";
            default:return "Hazır";
        }
    }

    public static String summary(){return label()+" • "+ageMs()+" ms";}

    private static void notifyListeners(State s,long at){
        for(Listener l:listeners){try{l.onStateChanged(s,at);}catch(Throwable ignored){}}
    }
}
