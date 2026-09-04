package com.mg.structuralai;

import java.util.*;

/** Solver-ready linearized contact constraints. Current safe subset: bonded/tied node pairs. */
public final class ContactConstraintSet {
    public enum Kind { BONDED_TIE, NO_SEPARATION_NORMAL, FRICTIONLESS_NORMAL, UNRESOLVED }
    public static final class Pair {
        public final int nodeA,nodeB; public final Kind kind; public final MeshModel.V3 normal; public final double gapM,confidence;
        Pair(int a,int b,Kind k,MeshModel.V3 n,double g,double c){nodeA=a;nodeB=b;kind=k;normal=n;gapM=g;confidence=c;}
    }
    public final List<Pair> pairs=new ArrayList<>();
    public void add(int a,int b,Kind k,MeshModel.V3 n,double gapM,double confidence){pairs.add(new Pair(a,b,k,n,gapM,confidence));}
    public int bondedCount(){int n=0;for(Pair p:pairs)if(p.kind==Kind.BONDED_TIE)n++;return n;}
    public int unresolvedCount(){int n=0;for(Pair p:pairs)if(p.kind==Kind.UNRESOLVED)n++;return n;}
}
