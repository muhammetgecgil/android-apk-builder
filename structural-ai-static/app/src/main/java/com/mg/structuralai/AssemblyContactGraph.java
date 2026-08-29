package com.mg.structuralai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Conservative graph view of a multi-body assembly. Only touching/coincident
 * and near-contact candidates are considered possible load-transfer edges.
 * Finite gaps never create connectivity and any suspected interference blocks
 * automatic assembly readiness.
 */
public final class AssemblyContactGraph {
    public static final class Result {
        public final int bodies;
        public final int transferEdges;
        public final int touchingEdges;
        public final int nearEdges;
        public final int finiteGapEdges;
        public final int interferenceEdges;
        public final int connectedComponents;
        public final int isolatedBodies;
        public final int maxDegree;
        public final boolean connected;
        public final boolean hasInterference;
        public final boolean assemblyReady;
        public final List<Integer> componentByBody;
        public final String summary;

        Result(int bodies,int transferEdges,int touchingEdges,int nearEdges,int finiteGapEdges,
               int interferenceEdges,int connectedComponents,int isolatedBodies,int maxDegree,
               boolean connected,boolean hasInterference,boolean assemblyReady,List<Integer> componentByBody){
            this.bodies=bodies;this.transferEdges=transferEdges;this.touchingEdges=touchingEdges;
            this.nearEdges=nearEdges;this.finiteGapEdges=finiteGapEdges;this.interferenceEdges=interferenceEdges;
            this.connectedComponents=connectedComponents;this.isolatedBodies=isolatedBodies;this.maxDegree=maxDegree;
            this.connected=connected;this.hasInterference=hasInterference;this.assemblyReady=assemblyReady;
            this.componentByBody=Collections.unmodifiableList(new ArrayList<>(componentByBody));
            this.summary=String.format(Locale.US,
                    "CONTACT GRAPH | bodies=%d | transferEdges=%d | touching=%d | near=%d | finiteGap=%d | interference=%d | components=%d | isolated=%d | maxDegree=%d | connected=%s | assemblyReady=%s",
                    bodies,transferEdges,touchingEdges,nearEdges,finiteGapEdges,interferenceEdges,
                    connectedComponents,isolatedBodies,maxDegree,connected,assemblyReady);
        }
    }

    private AssemblyContactGraph(){}

    public static Result evaluate(AssemblyBodyDecomposer.Result dec,ContactCandidateEngine.Result contacts){
        int n=dec==null||dec.bodies==null?0:dec.bodies.size();
        List<List<Integer>> adj=new ArrayList<>();for(int i=0;i<n;i++)adj.add(new ArrayList<Integer>());
        int transfer=0,touch=0,near=0,gap=0,interference=0;
        if(contacts!=null&&contacts.pairs!=null)for(ContactCandidateEngine.Pair p:contacts.pairs){
            if(p.state==ContactCandidateEngine.State.TOUCHING_OR_COINCIDENT){touch++;if(addTransferEdge(adj,p.bodyA,p.bodyB,n))transfer++;}
            else if(p.state==ContactCandidateEngine.State.NEAR_CONTACT){near++;if(addTransferEdge(adj,p.bodyA,p.bodyB,n))transfer++;}
            else if(p.state==ContactCandidateEngine.State.FINITE_GAP)gap++;
            else if(p.state==ContactCandidateEngine.State.INTERFERENCE_SUSPECTED)interference++;
        }
        int isolated=0,maxDegree=0;for(List<Integer> a:adj){if(a.isEmpty())isolated++;maxDegree=Math.max(maxDegree,a.size());}
        int[] comp=new int[n];java.util.Arrays.fill(comp,-1);int components=0;
        for(int seed=0;seed<n;seed++)if(comp[seed]<0){ArrayDeque<Integer> q=new ArrayDeque<>();q.add(seed);comp[seed]=components;while(!q.isEmpty()){int a=q.removeFirst();for(int b:adj.get(a))if(comp[b]<0){comp[b]=components;q.addLast(b);}}components++;}
        List<Integer> compList=new ArrayList<>();for(int x:comp)compList.add(x);
        boolean connected=n>0&&components==1;
        boolean hasInterference=interference>0;
        boolean ready=n>1&&connected&&isolated==0&&!hasInterference;
        return new Result(n,transfer,touch,near,gap,interference,components,isolated,maxDegree,connected,hasInterference,ready,compList);
    }

    private static boolean addTransferEdge(List<List<Integer>> adj,int a,int b,int n){
        if(a<0||b<0||a>=n||b>=n||a==b)return false;
        if(adj.get(a).contains(b))return false;
        adj.get(a).add(b);adj.get(b).add(a);return true;
    }
}
