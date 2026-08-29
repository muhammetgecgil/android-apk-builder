package com.mg.structuralai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** UI-neutral visual model for assembly QA overlays. Engineering decisions remain in AssemblyContactGraph. */
public final class AssemblyOverlayModel {
    public enum EdgeStyle { TOUCH, NEAR, GAP, INTERFERENCE }

    public static final class BodyNode {
        public final int bodyId;
        public final MeshModel.V3 center;
        public final int componentId;
        public final boolean isolated;
        BodyNode(int id,MeshModel.V3 c,int comp,boolean iso){bodyId=id;center=c;componentId=comp;isolated=iso;}
        public String label(){return "B"+bodyId;}
    }

    public static final class Edge {
        public final int bodyA,bodyB;
        public final EdgeStyle style;
        public final boolean loadTransfer;
        public final boolean blocksReadiness;
        public final double gap,penetration,confidence;
        Edge(AssemblyContactGraph.EdgeEvidence e){
            bodyA=e.bodyA;bodyB=e.bodyB;loadTransfer=e.loadTransfer;blocksReadiness=e.blocksReadiness;
            gap=e.sampledGap;penetration=e.bboxPenetration;confidence=e.confidence;
            if(e.state==ContactCandidateEngine.State.INTERFERENCE_SUSPECTED)style=EdgeStyle.INTERFERENCE;
            else if(e.state==ContactCandidateEngine.State.TOUCHING_OR_COINCIDENT)style=EdgeStyle.TOUCH;
            else if(e.state==ContactCandidateEngine.State.NEAR_CONTACT)style=EdgeStyle.NEAR;
            else style=EdgeStyle.GAP;
        }
        public String label(){return String.format(Locale.US,"B%d-B%d %s",bodyA,bodyB,style);}
    }

    public final List<BodyNode> bodies;
    public final List<Edge> edges;
    public final boolean assemblyReady;
    public final String readinessReason;
    public final String badgeText;

    private AssemblyOverlayModel(List<BodyNode> b,List<Edge> e,AssemblyContactGraph.Result g){
        bodies=Collections.unmodifiableList(b);edges=Collections.unmodifiableList(e);
        assemblyReady=g!=null&&g.assemblyReady;
        readinessReason=g==null?"NO_GRAPH":g.readinessReason;
        badgeText=assemblyReady?"ASSEMBLY READY":"ASSEMBLY BLOCKED • "+readinessReason;
    }

    public static AssemblyOverlayModel build(AssemblyBodyDecomposer.Result dec,AssemblyContactGraph.Result graph){
        List<BodyNode> bn=new ArrayList<>();
        if(dec!=null&&dec.bodies!=null){
            int[] degree=new int[dec.bodies.size()];
            if(graph!=null)for(AssemblyContactGraph.EdgeEvidence e:graph.edges)if(e.loadTransfer){if(e.bodyA>=0&&e.bodyA<degree.length)degree[e.bodyA]++;if(e.bodyB>=0&&e.bodyB<degree.length)degree[e.bodyB]++;}
            for(AssemblyBodyDecomposer.Body b:dec.bodies){
                MeshModel.V3 c=new MeshModel.V3((b.minX+b.maxX)/2.0,(b.minY+b.maxY)/2.0,(b.minZ+b.maxZ)/2.0);
                int comp=graph!=null&&b.id<graph.componentByBody.size()?graph.componentByBody.get(b.id):-1;
                boolean isolated=b.id>=0&&b.id<degree.length&&degree[b.id]==0;
                bn.add(new BodyNode(b.id,c,comp,isolated));
            }
        }
        List<Edge> ee=new ArrayList<>();if(graph!=null)for(AssemblyContactGraph.EdgeEvidence e:graph.edges)ee.add(new Edge(e));
        return new AssemblyOverlayModel(bn,ee,graph);
    }
}
