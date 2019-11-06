package components.commit.tree;

import com.fxgraph.graph.Graph;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class CommitItemFactory {
    private static final BooleanProperty clickedProperty = new SimpleBooleanProperty(false);
    private Graph m_Graph;

    public CommitItemFactory(Graph i_Graph) {
        m_Graph = i_Graph;
    }

    public static BooleanProperty GetListItemClickedProperty() { return clickedProperty; }

    public void SetGraph(Graph i_Graph) { m_Graph = i_Graph; }

    public Graph GetGraph() { return m_Graph; }

    public CommitItemController CreateCommitItem(CommitGraphNode i_GraphNode) {
        return new CommitItemController(m_Graph, i_GraphNode);
    }
}
