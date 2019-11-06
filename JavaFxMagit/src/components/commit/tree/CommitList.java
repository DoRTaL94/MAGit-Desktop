package components.commit.tree;

import javafx.scene.layout.VBox;

public class CommitList extends VBox {
    private CommitItemFactory m_Factory;

    public void SetFactory(CommitItemFactory i_Factory) {
        setSpacing(5);
        m_Factory = i_Factory;
    }

    public void AddChild(CommitGraphNode i_Child) {
        this.getChildren().add(m_Factory.CreateCommitItem(i_Child).GetCommitDetails());
    }
}
