package components.commit.tree;

import com.fxgraph.edges.Edge;
import com.fxgraph.graph.Graph;
import com.fxgraph.graph.Model;
import javafx.scene.layout.Region;

public class CommitItemController {
    private Graph m_Graph;
    private CommitGraphNode m_GraphNode;

    public CommitItemController(Graph i_Graph, CommitGraphNode i_GraphNode) {
        m_Graph = i_Graph;
        m_GraphNode = i_GraphNode;
        updateGraph();
    }

    public Region GetCommitDetails() {
        return m_GraphNode.GetCommitDetails();
    }

    private void updateGraph() {
        final Model model = m_Graph.getModel();

        for(CommitGraphNode graphNode: m_GraphNode.GetGraphNodeChildren()) {
            if(!model.getAddedCells().contains(graphNode)) {
                model.addCell(graphNode);
            }

            model.addEdge(new Edge(m_GraphNode, graphNode));
        }
    }
}