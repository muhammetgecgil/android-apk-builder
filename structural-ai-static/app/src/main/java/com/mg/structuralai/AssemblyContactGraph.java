package com.mg.structuralai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Conservative graph view of a multi-body assembly. Only actual touching/coincident
 * candidates are automatic load-transfer edges. Near-contact remains a geometric
 * candidate but is NOT connected until explicit closure/contact evidence exists.
 * Finite gaps never create connectivity and any suspected interference blocks
 * automatic assembly readiness.
 *
 * EdgeEvidence is intentionally UI-neutral: viewport, PDF/report and solver all
 * consume the same evaluated contact evidence instead of recomputing topology.
 */
public final class AssemblyContactGraph {
    public static final class EdgeEvidence {
        public final int bodyA,bodyB;
        public final ContactCandidateEngine.State state;
        public final boolean loadTransfer;
        public final boolean blocksReadiness;
        public final double bboxGap,bboxPenetration,sampledGap,confidence;

        EdgeEvidence(ContactCandidateEngine.Pair p,boolean transfer,boolean blocks){
            bodyA=p.bodyA;bodyB=p.bodyB;state=p.state;loadTransfer=transfer;blocksReadiness=blocks;
            bboxGap=p.bboxGap;bboxPenetration=p.bboxPenetration;sampledGap=p.sampledGap;confidence=p.confidence;
        }
        public String compact(){
            return String.format(Locale.US,"B%d-B%d %s | transfer=%s | block=%s | gap=%.6g | pen=%.6g | conf=%.2f",
                    bodyA,bodyB,state,loadTransfer,blocksReadiness,sampledGap,bboxPenetration,confidence);
        }
    }

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
        public final List<EdgeEvidence> edges;
        public final String readinessReason;
        public final String summary;

        Result(int bodies,int transferEdges,int touchingEdges,int nearEdges,int finiteGapEdges,
               int interferenceEdges,int connectedComponents,int isolatedBodies,int maxDegree,
               boolean connected,boolean hasInterference,boolean assemblyReady,List<Integer> componentByBody,
               List<EdgeEvidence> edges,String readinessReason){
            this.bodies=bodies;this.transferEdges=transferEdges;this.touchingEdges=touchingEdges;
            this.nearEdges=nearEdges;this.finiteGapEdges=finiteGapEdges;this.interferenceEdges=interferenceEdges;
            this.connectedComponents=connectedComponents;this.isolatedBodies=isolatedBodies;this.maxDegree=maxDegree;
            this.connected=connected;this.hasInterference=hasInterference;this.assemblyReady=assemblyReady;
            this.componentByBody=Collections.unmodifiableList(new ArrayList<>(componentByBody));
            this.edges=Collections.unmodifiableList(new ArrayList<>(edges));
            this.readinessReason=readinessReason;
            this.summary=String.format(Locale.US,
                    "CONTACT GRAPH | bodies=%d | transferEdges=%d | touching=%d | near=%d | finiteGap=%d | interference=%d | components=%d | isolated=%d | maxDegree=%d | connected=%s | assemblyReady=%s | reason=%s",
                    bodies,transferEdges,touchingEdges,nearEdges,finiteGapEdges,interferenceEdges,
                    connectedComponents,isolatedBodies,maxDegree,connected,assemblyReady,readinessReason);
        }
    }

    private AssemblyContactGraph(){}

    public static Result evaluate(AssemblyBodyDecomposer.Result dec,ContactCandidateEngine.Result contacts){
        int n=dec==null||dec.bodies==null?0:dec.bodies.size();
        List<List<Integer>> adj=new ArrayList<>();for(int i=0;i<n;i++)adj.add(new ArrayList<Integer>());
        List<EdgeEvidence> evidence=new ArrayList<>();
        int transfer=0,touch=0,near=0,gap=0,interference=0;
        if(contacts!=null&&contacts.pairs!=null)for(ContactCandidateEngine.Pair p:contacts.pairs){
            boolean transferCandidate=false,block=false;
            if(p.state==ContactCandidateEngine.State.TOUCHING_OR_COINCIDENT){touch++;transferCandidate=true;}
            else if(p.state==ContactCandidateEngine.State.NEAR_CONTACT){near++;}
            else if(p.state==ContactCandidateEngine.State.FINITE_GAP)gap++;
            else if(p.state==ContactCandidateEngine.State.INTERFERENCE_SUSPECTED){interference++;block=true;}
            boolean acceptedTransfer=transferCandidate&&addTransferEdge(adj,p.bodyA,p.bodyB,n);
            if(acceptedTransfer)transfer++;
            if(p.state!=ContactCandidateEngine.State.FAR)evidence.add(new EdgeEvidence(p,transferCandidate,block));
        }
        int isolated=0,maxDegree=0;for(List<Integer> a:adj){if(a.isEmpty())isolated++;maxDegree=Math.max(maxDegree,a.size());}
        int[] comp=new int[n];java.util.Arrays.fill(comp,-1);int components=0;
        for(int seed=0;seed<n;seed++)if(comp[seed]<0){ArrayDeque<Integer> q=new ArrayDeque<>();q.add(seed);comp[seed]=components;while(!q.isEmpty()){int a=q.removeFirst();for(int b:adj.get(a))if(comp[b]<0){comp[b]=components;q.addLast(b);}}components++;}
        List<Integer> compList=new ArrayList<>();for(int x:comp)compList.add(x);
        boolean connected=n>0&&components==1;
        boolean hasInterference=interference>0;
        boolean ready=n>1&&connected&&isolated==0&&!hasInterference;
        String reason;
        if(n<2)reason="NOT_AN_ASSEMBLY";
        else if(hasInterference)reason="INTERFERENCE_SUSPECTED";
        else if(isolated>0&&near>0)reason="NEAR_CONTACT_REQUIRES_CLOSURE_EVIDENCE";
        else if(isolated>0)reason="ISOLATED_BODY";
        else if(!connected)reason="DISCONNECTED_SUBASSEMBLIES";
        else reason="CONNECTED_TRANSFER_GRAPH";
        return new Result(n,transfer,touch,near,gap,interference,components,isolated,maxDegree,connected,hasInterference,ready,compList,evidence,reason);
    }

    private static boolean addTransferEdge(List<List<Integer>> adj,int a,int b,int n){
        if(a<0||b<0||a>=n||b>=n||a==b)return false;
        if(adj.get(a).contains(b))return false;
        adj.get(a).add(b);adj.get(b).add(a);return true;
    }
}
