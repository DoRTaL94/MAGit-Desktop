package components.commit.tree;

import com.fxgraph.cells.AbstractCell;
import com.fxgraph.graph.Graph;
import com.fxgraph.graph.IEdge;
import components.filetree.commit.CommitFileTree;
import components.themes.ThemesController;
import data.structures.Branch;
import data.structures.Commit;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import magit.CommitNode;
import magit.Engine;

import java.awt.Point;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class CommitGraphNode extends AbstractCell implements Comparable {
    private static final String f_DarkTheme = "resources/TreeNodeDark.css";
    private static final String f_ColorfulTheme = "resources/TreeNodeColorful.css";
    private static final String f_DefaultTheme = "resources/TreeNodeDefault.css";

    public DoubleProperty CommitDetailsXProperty;
    public DoubleProperty TreeNodeYProperty;
    private HBoxCommitDetails m_CommitDetails;
    private Rectangle m_RectangleTreeNode;
    private Pane m_PaneRectangleTreeNode;
    private CommitNode m_CommitNode;
    private List<CommitGraphNode> m_GraphNodeParents;
    private List<CommitGraphNode> m_GraphNodeChildrens;
    private int m_BranchNumber;
    private int m_IdInList;
    private String m_CurrentTheme;

    public CommitGraphNode(CommitNode i_Node) {
        m_CommitNode           = i_Node;
        m_BranchNumber         = -1;
        m_GraphNodeChildrens   = new ArrayList<>();
        m_GraphNodeParents     = new ArrayList<>();
        m_CommitDetails        = new HBoxCommitDetails();
        TreeNodeYProperty      = new SimpleDoubleProperty(0);
        CommitDetailsXProperty = new SimpleDoubleProperty(20);

        m_CommitDetails.SetDoubleClickAction(() -> new CommitFileTree(m_CommitNode));

        if(ThemesController.themeChangedProperty.get().equals("Dark")) {
            m_CurrentTheme = f_DarkTheme;
        } else if(ThemesController.themeChangedProperty.get().equals("Colorful")) {
            m_CurrentTheme = f_ColorfulTheme;
        } else {
            m_CurrentTheme = f_DefaultTheme;
        }

        initializeRectangleTreeNode();
        initializeCommitDetails();

        m_PaneRectangleTreeNode.getStylesheets().add(getClass().getResource(m_CurrentTheme).toExternalForm());
        m_PaneRectangleTreeNode.applyCss();

        ThemesController.themeChangedProperty.addListener((observable, oldValue, newValue) -> {
            if(newValue.equals("Dark")) {
                m_CurrentTheme = f_DarkTheme;
            }
            else if(newValue.equals("Colorful")) {
                m_CurrentTheme = f_ColorfulTheme;
            }
            else {
                m_CurrentTheme = f_DefaultTheme;
            }

            m_PaneRectangleTreeNode.getStylesheets().clear();
            m_PaneRectangleTreeNode.getStylesheets().add(getClass().getResource(m_CurrentTheme).toExternalForm());
            m_PaneRectangleTreeNode.applyCss();
        });
    }


    private double getTextWidth(String i_Text) {
        Text textLabelBranch = new Text(i_Text);
        textLabelBranch.setFont(Font.font("System", FontWeight.BOLD, 15));

        return textLabelBranch.getLayoutBounds().getWidth();
    }

    private void initializeCommitDetails() {
        if(this.GetPointingBranches().size() != 0) {
            for(Branch branch: this.GetPointingBranches()) {
                m_CommitDetails.AddBranchNameRectangle(branch);
            }
        }

        double labelCommitDaysAgoWidth = getTextWidth(this.GetCommit().GetLastUpdate());
        double labelCommitSha1Width = getTextWidth(this.GetSha1());
        double labelUserNameWidth = getTextWidth(this.GetCommit().GetLastChanger());
        double labelPointedCommitDescriptionWidth = getTextWidth(this.GetCommit().GetMessage());

        m_CommitDetails.GetLabelCommitDaysAgo().setPrefWidth(labelCommitDaysAgoWidth);
        m_CommitDetails.GetLabelCommitDaysAgo().setText(this.GetCommit().GetLastUpdate());
        m_CommitDetails.GetLabelCommitDaysAgo().setAlignment(Pos.CENTER);

        m_CommitDetails.GetLabelCommitSha1().setPrefWidth(labelCommitSha1Width);
        m_CommitDetails.GetLabelCommitSha1().setText(this.GetSha1());
        m_CommitDetails.GetLabelCommitSha1().setAlignment(Pos.CENTER);

        m_CommitDetails.GetLabelUserName().setPrefWidth(labelUserNameWidth);
        m_CommitDetails.GetLabelUserName().setText(this.GetCommit().GetLastChanger());
        m_CommitDetails.GetLabelUserName().setAlignment(Pos.CENTER);

        m_CommitDetails.GetLabelPointedCommitDescription().setPrefWidth(labelPointedCommitDescriptionWidth);
        m_CommitDetails.GetLabelPointedCommitDescription().setText(this.GetCommit().GetMessage());
        m_CommitDetails.GetLabelPointedCommitDescription().setAlignment(Pos.CENTER);

        m_CommitDetails.Update();

        m_CommitDetails.GetPaneSpacer().prefWidthProperty().bind(CommitDetailsXProperty);
        m_CommitDetails.GetPaneSpacer().minWidthProperty().bind(CommitDetailsXProperty);
        TreeNodeYProperty.bind(m_CommitDetails.layoutYProperty());
    }

    private void initializeRectangleTreeNode() {
        m_RectangleTreeNode = new Rectangle();
        m_RectangleTreeNode.setWidth(16);
        m_RectangleTreeNode.setHeight(16);
        m_RectangleTreeNode.setId("rectangleTreeNode");

        m_PaneRectangleTreeNode = new Pane();
        m_PaneRectangleTreeNode.getChildren().add(m_RectangleTreeNode);
        m_PaneRectangleTreeNode.setPrefWidth(16);
        m_PaneRectangleTreeNode.setPrefHeight(16);

        if(!m_CurrentTheme.isEmpty()) {
            m_PaneRectangleTreeNode.getStylesheets().clear();
            m_PaneRectangleTreeNode.getStylesheets().add(getClass().getResource(m_CurrentTheme).toExternalForm());
            m_PaneRectangleTreeNode.applyCss();
        }
    }

    public List<CommitGraphNode> GetGraphNodeParents() { return m_GraphNodeParents; }

    public void AddGraphNodeParent(CommitGraphNode i_Parent) {
        m_GraphNodeParents.add(i_Parent);
        m_GraphNodeParents.sort(CommitGraphNode::compareTo);
    }

    public int GetBranchNumber() { return m_BranchNumber; }

    public void SetBranchNumber(int i_Num) {
        m_BranchNumber = i_Num;
    }

    public List<CommitGraphNode> GetGraphNodeChildren() { return m_GraphNodeChildrens; }

    public void AddGraphNodeChild(CommitGraphNode i_GraphNode) {
        m_GraphNodeChildrens.add(i_GraphNode);
        m_GraphNodeChildrens.sort(CommitGraphNode::compareTo);
    }

    public CommitNode GetFirstParent() { return m_CommitNode.GetSecondParent(); }

    public List<CommitNode> GetChildren() { return m_CommitNode.GetChildren(); }

    public List<Branch> GetPointingBranches() { return m_CommitNode.GetPointingBranches(); }

    public String GetSha1() { return m_CommitNode.GetSha1(); }

    public CommitNode GetCommitNode() { return m_CommitNode; }

    public Commit GetCommit() { return m_CommitNode.GetCommit(); }

    public CommitNode GetSecondParent() {return m_CommitNode.GetSecondParent(); }

    @Override public Region getGraphic(Graph graph) {
        return m_PaneRectangleTreeNode;
    }

    @Override public DoubleBinding getXAnchor(Graph graph, IEdge edge) {
        final Region graphic = graph.getGraphic(this);
        return graphic.layoutXProperty().add(m_RectangleTreeNode.getWidth() / 2);
    }

    @Override public int compareTo(Object i_GraphNode) {
        CommitGraphNode nodeToCompare = (CommitGraphNode) i_GraphNode;
        long nodeToCompareTime = 0;
        long thisNodeTime = 0;

        try {
            String nodeToCompareDate = nodeToCompare.GetCommit().GetLastUpdate();
            String thisNodeDate = GetCommit().GetLastUpdate();
            nodeToCompareTime = new SimpleDateFormat(Engine.DATE_FORMAT).parse(nodeToCompareDate).getTime();
            thisNodeTime = new SimpleDateFormat(Engine.DATE_FORMAT).parse(thisNodeDate).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return (int)(nodeToCompareTime - thisNodeTime);
    }

    public Point GetLocation(Graph i_Graph) {
        return new Point((int) i_Graph.getGraphic(this).layoutXProperty().get(),
                (int) i_Graph.getGraphic(this).layoutYProperty().get());
    }

    public Region GetCommitDetails() {
        return m_CommitDetails;
    }

    public void SetRectangleTreeNodeId(String i_CssId) {
        m_RectangleTreeNode.setId(i_CssId);
    }

    public void SetIdInList(int i_NodeId) {
        m_IdInList = i_NodeId;
    }

    public int GetIdInList() { return m_IdInList; }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof CommitGraphNode)) {
            return false;
        }

        return ((CommitGraphNode) o).GetCommitNode().equals(m_CommitNode);
    }

    public void SetClickAction(Runnable i_Action) { m_CommitDetails.SetClickAction(i_Action); }
}

