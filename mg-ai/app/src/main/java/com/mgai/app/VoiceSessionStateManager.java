package com.mgai.app;

import java.util.concurrent.atomic.AtomicReference;

public final class VoiceSessionStateManager {
    public enum State { IDLE, LISTENING, TRANSCRIBING, THINKING, SPEAKING, BARGE_IN }
    private static final AtomicReference<State> state=new AtomicReference<>(State.IDLE);
    private static volatile long changedAt=System.currentTimeMillis();
    private VoiceSessionStateManager(){}

    public static State get(){return state.get();}
    public static synchronized void set(State next){
        if(next==null)return;
        state.set(next);changedAt=System.currentTimeMillis();
    }
    public static boolean is(State s){return state.get()==s;}
    public static long ageMs(){return Math.max(0,System.currentTimeMillis()-changedAt);}
    public static synchronized boolean transition(State from,State to){
        if(state.get()!=from)return false;
        state.set(to);changedAt=System.currentTimeMillis();return true;
    }
    public static synchronized void reset(){set(State.IDLE);}
    public static String summary(){return state.get().name()+" • "+ageMs()+" ms";}
}
