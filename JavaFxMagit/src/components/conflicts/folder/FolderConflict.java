package components.conflicts.folder;

import components.conflicts.IConflictDisplay;
import components.filetree.folder.FolderFileTree;
import data.structures.Folder;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import magit.merge.Conflict;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.io.File;

public class FolderConflict implements IConflictDisplay {
    private Conflict m_Conflict;
    private VBox m_Root;
    private FolderConflictController m_Controller;

    public FolderConflict(Conflict i_FolderConflict) {
        m_Conflict = i_FolderConflict;
        loadFxml();
        updateGraphics();
    }

    private void updateGraphics() {
        if(m_Conflict.GetTheirs() != null) {
            FolderFileTree folderFileTree = new FolderFileTree((Folder) m_Conflict.GetTheirs(), m_Conflict.GetFileLocation());
            folderFileTree.getRoot().getValue().SetName(new File(m_Conflict.GetFileLocation()).getName());
            VBox.setVgrow(folderFileTree, Priority.ALWAYS);
            m_Controller.GetStackPaneTheirsFolderContent().getChildren().add(folderFileTree);
        }

        if(m_Conflict.GetOurs() != null) {
            FolderFileTree folderFileTree = new FolderFileTree((Folder) m_Conflict.GetOurs(), m_Conflict.GetFileLocation());
            folderFileTree.getRoot().getValue().SetName(new File(m_Conflict.GetFileLocation()).getName());
            VBox.setVgrow(folderFileTree, Priority.ALWAYS);
            m_Controller.GetStackPaneOursFolderContent().getChildren().add(folderFileTree);
        }
    }

    private void loadFxml() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            URL url = getClass().getResource("FolderConflictUI.fxml");
            fxmlLoader.setLocation(url);
            m_Root = fxmlLoader.load(url.openStream());
            m_Controller = fxmlLoader.getController();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Parent GetGraphics() {
        return m_Root;
    }

    public void UpdateSolution() {
            m_Conflict.SetSolution(m_Controller.GetRadioButtonOurs().isSelected() ?
                    m_Conflict.GetOurs() :
                    m_Conflict.GetTheirs());
    }

    public void SetVisible(boolean i_IsVisible) {
        m_Root.setVisible(i_IsVisible);
    }

    public Conflict GetConflict() { return m_Conflict; }
}
