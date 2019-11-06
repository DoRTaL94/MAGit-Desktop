package components.commit.tree;

import com.fxgraph.graph.Graph;
import com.fxgraph.layout.Layout;
import data.structures.Branch;
import javafx.beans.binding.Bindings;
import magit.Engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommitTreeLayout implements Layout {
    private static final int NODE_HEIGHT_FROM_TOP = 8;
    private static final int SPACE_NODE_TO_TEXT = 25;
    private static final int SPACE_LEFT_TO_NODE = 20;
    private static final int SPACE_BETWEEN_BRANCHES = 40;
    private CommitGraphNode m_Root;
    private Graph m_Graph;
    private double m_MaxXLocation;
    private Set<CommitGraphNode> m_NodesOnTheHeadBranch;
    private final Branch f_HeadBranch;

    public CommitTreeLayout(CommitGraphNode i_Root) {
        f_HeadBranch = Engine.Creator.GetInstance().GetActiveRepository().GetHeadBranch();
        m_Root = i_Root;
        m_MaxXLocation = 0;
        m_NodesOnTheHeadBranch = new HashSet<>();
    }

    @Override public void execute(Graph graph) {
        m_Graph = graph;
        initializeTreeGraphics();
    }

    private void initializeTreeGraphics() {
        // Using BFS algoritm to initialize each level at a time.
        List<CommitGraphNode> nodeList = new ArrayList<>();
        nodeList.add(m_Root);
        m_Root.SetBranchNumber(0);

        findHeadBranch(m_Root);
        updateTreeNodeGraphics(nodeList);
        updateCommitDetailsGraphics(m_Root);
    }

    private void updateCommitDetailsGraphics(CommitGraphNode i_Root) {
        if(i_Root == null) {
            return;
        }

        i_Root.CommitDetailsXProperty.set(m_MaxXLocation + SPACE_LEFT_TO_NODE);

        for(CommitGraphNode child: i_Root.GetGraphNodeChildren()) {
            updateCommitDetailsGraphics(child);
        }
    }

    private boolean findHeadBranch(CommitGraphNode i_Node) {
        boolean isHeadFound = false;

        if(i_Node != null) {
            if (isPointingBranchIsHead(i_Node)) {
                isHeadFound = true;
                m_NodesOnTheHeadBranch.add(i_Node);
            } else {
                for (CommitGraphNode child : i_Node.GetGraphNodeChildren()) {
                    isHeadFound = findHeadBranch(child);

                    if (isHeadFound) {
                        m_NodesOnTheHeadBranch.add(i_Node);
                        break;
                    }
                }
            }
        }

        return isHeadFound;
    }

    public boolean isPointingBranchIsHead(CommitGraphNode i_Node) {
        boolean result = false;

        for (Branch branch : i_Node.GetPointingBranches()) {
            if (branch == f_HeadBranch) {
                result = true;
                break;
            }
        }

        return result;
    }

    private void updateTreeNodeGraphics(List<CommitGraphNode> i_Nodes) {
        if (i_Nodes.size() == 0) {
            return;
        }

        List<CommitGraphNode> nodeList = new ArrayList<>();

        // BFS algoritm adds all the children to the list and call the function recursively.
        for (CommitGraphNode node : i_Nodes) {
            List<CommitGraphNode> nodeChildren = node.GetGraphNodeChildren();
            nodeList.addAll(nodeChildren);
            indetifyBranches(node, nodeChildren);
            setNodeGraphics(node);
        }

        updateTreeNodeGraphics(nodeList);

        if (i_Nodes.size() > 0) {
            for (int node = 0; node < i_Nodes.size(); node++) {
                CommitGraphNode currNode = i_Nodes.get(node);
                int currNodeXLocation = SPACE_LEFT_TO_NODE + currNode.GetBranchNumber() * SPACE_BETWEEN_BRANCHES;

                if (currNodeXLocation > m_MaxXLocation) {
                    m_MaxXLocation = currNodeXLocation;
                }

                updateLocationLayout(currNode, currNodeXLocation, i_Nodes);
            }
        }
    }

    public void indetifyBranches(CommitGraphNode i_Node, List<CommitGraphNode> i_NodeChildren) {
        boolean isNodeInHeadBranchTookCare = false;

        for (int nodeInd = 0; nodeInd < i_NodeChildren.size(); nodeInd++) {
//            boolean isMergedBranch = false;
//
//            for(Branch branch: i_Node.GetPointingBranches()) {
//                if(branch.GetIsMerged()) {
//                    isMergedBranch = true;
//                    break;
//                }
//            }
            if(i_NodeChildren.get(nodeInd).GetBranchNumber() == -1) {
//                if (i_Node.GetPointingBranches().size() != 0 && !isMergedBranch) {
//                    if(m_NodesOnTheHeadBranch.contains(i_NodeChildren.get(nodeInd))) {
//                        i_NodeChildren.get(nodeInd).SetBranchNumber(1 + i_Node.GetBranchNumber());
//                    }
//                    else {
//                        i_NodeChildren.get(nodeInd).SetBranchNumber(2 + branchNum + i_Node.GetBranchNumber());
//                    }
//                } else {
                    if(m_NodesOnTheHeadBranch.contains(i_NodeChildren.get(nodeInd))) {
                        i_NodeChildren.get(nodeInd).SetBranchNumber(i_Node.GetBranchNumber());
                        isNodeInHeadBranchTookCare = true;
                    }
                    else if(m_Root == i_Node) {
                        i_NodeChildren.get(nodeInd).SetBranchNumber((isNodeInHeadBranchTookCare ? nodeInd : 1 + nodeInd) + i_Node.GetBranchNumber());
                    }
                    else {
                        i_NodeChildren.get(nodeInd).SetBranchNumber(nodeInd + i_Node.GetBranchNumber());
                    }
//                }
            }

            i_NodeChildren.get(nodeInd).AddGraphNodeParent(i_Node); // used when two points are needed to create a line between parent and child.
        }
    }

    private void setNodeGraphics(CommitGraphNode i_Node) {
        boolean isHead = isPointingBranchIsHead(i_Node);
        i_Node.SetRectangleTreeNodeId(isHead ? "headBranchRectangleTreeNode" : "rectangleTreeNode");
    }

    private void updateLocationLayout(CommitGraphNode i_NodeToUpdate, final int i_NodeToUpdateXLocation, List<CommitGraphNode> i_NodesInTheSameLevel) {
        i_NodeToUpdate.CommitDetailsXProperty.set(i_NodeToUpdateXLocation + SPACE_NODE_TO_TEXT);

        m_Graph.getGraphic(i_NodeToUpdate).layoutXProperty().set(i_NodeToUpdateXLocation);
        m_Graph.getGraphic(i_NodeToUpdate).layoutYProperty().bind(Bindings.add(NODE_HEIGHT_FROM_TOP, i_NodeToUpdate.TreeNodeYProperty));
        m_Graph.getGraphic(i_NodeToUpdate).setPrefWidth(10);
        m_Graph.getGraphic(i_NodeToUpdate).setPrefHeight(10);
        //Platform.runLater(() -> updateLocationLayoutIfEdgeOnTop(i_NodesInTheSameLevel));
    }

//    private void updateLocationLayoutIfEdgeOnTop(List<CommitGraphNode> i_Nodes) {
//        for(int node = 0; node < i_Nodes.size(); node++) {
//            CommitGraphNode currNode = i_Nodes.get(node);
//            int currNodeXLocation = SPACE_LEFT_TO_NODE + currNode.GetBranchNumber() * SPACE_BETWEEN_BRANCHES;
//
//            if(currNodeXLocation < m_MaxXLocation) {
//                Line line = findNearestLine(i_Nodes, node);
//
//                if(line != null) {
//                    int newX = (int) line.CalcLineDistanceFromLeft(currNode.TreeNodeYProperty.get() + NODE_HEIGHT_FROM_TOP);
//                    currNodeXLocation = newX < currNodeXLocation ? currNodeXLocation : newX;
//                    currNode.CommitDetailsXProperty.set(currNodeXLocation + SPACE_NODE_TO_TEXT);
//                }
//            }
//        }
//    }
//
//    private Line findNearestLine(List<CommitGraphNode> i_NodesList, int i_NodeToFindNearestLine) {
//        Line result = null;
//
//        for(int node = 0; node < i_NodesList.size(); node++) {
//            if(node != i_NodeToFindNearestLine) {
//                result = findNearestLineRec(i_NodesList.get(node), i_NodesList.get(i_NodeToFindNearestLine));
//
//                if (result != null) {
//                    break;
//                }
//            }
//        }
//
//        return result;
//    }
//
//    private Line findNearestLineRec(CommitGraphNode i_NodeInOtherBranch, CommitGraphNode i_NodeToFindNearestLine) {
//        Line result = null;
//
//        if(i_NodeInOtherBranch.GetIdInList() < i_NodeToFindNearestLine.GetIdInList()) {
//            List<CommitGraphNode> parents = i_NodeInOtherBranch.GetGraphNodeParents();
//            CommitGraphNode parent = parents.get(parents.size() - 1);
//            Point parentPoint = parent.GetLocation(m_Graph);
//            Point NodeInOtherBranchPoint = i_NodeInOtherBranch.GetLocation(m_Graph);
//
//            return new Line(parentPoint, NodeInOtherBranchPoint);
//        }
//
//        for(int node = i_NodeInOtherBranch.GetGraphNodeChildren().size() - 1; node >= 0; node--) {
//            result = findNearestLineRec(i_NodeInOtherBranch.GetGraphNodeChildren().get(node), i_NodeToFindNearestLine);
//
//            if(result != null) {
//                break;
//            }
//        }
//
//        return result;
//    }
//
//
//    public static class Line {
//        private Point m_Point1;
//        private Point m_Point2;
//        private double m_Slope;
//
//        public Line(Point i_Point1, Point i_Point2) {
//            m_Point1 = i_Point1;
//            m_Point2 = i_Point2;
//
//            try {
//                m_Slope = (i_Point1.y - i_Point2.y) / (i_Point1.x - i_Point2.x);
//            } catch(ArithmeticException e) {
//                m_Slope = 0;
//            }
//        }
//
//        public Point GetPoint1() { return m_Point1; }
//
//        public Point GetPoint2() { return m_Point2; }
//
//        public double GetSlope() { return m_Slope; }
//
//        public double CalcLineDistanceFromLeft(double i_YCoord) {
//            double res;
//
//            if(m_Slope != 0) {
//                double firstFraction = i_YCoord / m_Slope;
//                double secondFraction = m_Point1.y / m_Slope;
//                res = firstFraction - secondFraction + m_Point1.x;
//            }
//            else {
//                res = m_Point1.x;
//            }
//
//            return res;
//        }
//    }
}
