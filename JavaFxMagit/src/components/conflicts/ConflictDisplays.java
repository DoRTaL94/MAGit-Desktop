package components.conflicts;

import components.conflicts.blob.BlobConflict;
import components.conflicts.folder.FolderConflict;
import components.themes.ThemesController;
import data.structures.Folder;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import magit.merge.Conflict;
import magit.merge.ConflictsManager;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ConflictDisplays {
    private VBox m_Root;
    private ConflictsController m_Controller;
    private List<IConflictDisplay> m_ConflictDisplays;
    private ConflictsManager m_ConflictsManager;
    private int m_CurrentDisplayIndex;
    private Stage m_Stage;
    private static final String f_DarkTheme = "resources/Dark.css";
    private static final String f_ColorfulTheme = "resources/Colorful.css";
    private String m_CurrentTheme;

    public ConflictDisplays(ConflictsManager i_ConflictsManager) {
        m_ConflictDisplays = new ArrayList<>();
        m_ConflictsManager = i_ConflictsManager;
        loadFxml();
        updateConflictDisplays();
        initializeButtonsAction();

        if(ThemesController.themeChangedProperty.get().equals("Dark")) {
            m_CurrentTheme = f_DarkTheme;
        } else if(ThemesController.themeChangedProperty.get().equals("Colorful")) {
            m_CurrentTheme = f_ColorfulTheme;
        } else {
            m_CurrentTheme = "";
        }

        if(!m_CurrentTheme.isEmpty()) {
            m_Root.getStylesheets().add(getClass().getResource(m_CurrentTheme).toExternalForm());
            m_Root.applyCss();
        }

        ThemesController.themeChangedProperty.addListener((observable, oldValue, newValue) -> {
            if(newValue.equals("Dark")) {
                m_CurrentTheme = f_DarkTheme;
            }
            else if(newValue.equals("Colorful")) {
                m_CurrentTheme = f_ColorfulTheme;
            }

            if(newValue.equals("Default")) {
                m_CurrentTheme = "";
                m_Root.getStylesheets().clear();
                m_Root.applyCss();
            } else {
                m_Root.getStylesheets().clear();
                m_Root.getStylesheets().add(getClass().getResource(m_CurrentTheme).toExternalForm());
                m_Root.applyCss();
            }
        });
    }

    private void initializeButtonsAction() {
        m_Controller.SetBackAction(this::PrevDisplay);
        m_Controller.SetNextAction(this::NextDisplay);
        m_Controller.SetFinishAction(() -> {
            m_ConflictDisplays.forEach(IConflictDisplay::UpdateSolution);
            m_ConflictsManager.SolveConflicts();
            m_Stage.close();
        });
    }

    private void updateConflictDisplays() {
        for(Conflict conflict : m_ConflictsManager) {
            IConflictDisplay conflictDisplay = null;
            if(conflict.GetOurs() != null) {
                conflictDisplay = conflict.GetOurs() instanceof Folder ?
                        new FolderConflict(conflict):
                        new BlobConflict(conflict);
            }
            else {
                conflictDisplay = conflict.GetTheirs() instanceof Folder ?
                        new FolderConflict(conflict):
                        new BlobConflict(conflict);
            }

            conflictDisplay.SetVisible(false);
            m_Controller.GetStackPaneConflictDisplay().getChildren().add(conflictDisplay.GetGraphics());
            m_ConflictDisplays.add(conflictDisplay);
        }

        if(m_ConflictDisplays.size() != 0) {
            m_ConflictDisplays.get(0).SetVisible(true);
            m_CurrentDisplayIndex = 0;

            m_Controller.GetButtonFinish().setDisable(m_ConflictDisplays.size() != 1);
            m_Controller.GetButtonNext().setDisable(m_ConflictDisplays.size() < 2);
            m_Controller.GetLabelConflictCount().setText(String.format("%s/%s", m_CurrentDisplayIndex + 1, m_ConflictDisplays.size()));
            m_Controller.GetLabelFilePath().setText(String.format("File path: %s", m_ConflictDisplays.get(0).GetConflict().GetFileLocation()));
        }
    }

    private void loadFxml() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            URL url = getClass().getResource("/components/conflicts/ConflictUI.fxml");
            fxmlLoader.setLocation(url);
            m_Root = fxmlLoader.load(url.openStream());
            m_Controller = fxmlLoader.getController();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Show() {
        m_Stage = new Stage();
        Scene mainScene = new Scene(m_Root);
        m_Stage.setScene(mainScene);
        m_Stage.setTitle("Conflict Manager");
        m_Stage.getIcons().add(new Image(getClass().getResourceAsStream("/main/resources/MAGit.png")));
        m_Stage.showAndWait();
    }

    public boolean NextDisplay() {
        boolean hasNext;

        if(hasNext = HasNext()) {
            m_ConflictDisplays.get(m_CurrentDisplayIndex++).SetVisible(false);
            m_ConflictDisplays.get(m_CurrentDisplayIndex).SetVisible(true);
            m_Controller.GetLabelConflictCount().setText(String.format("%s/%s", m_CurrentDisplayIndex + 1, m_ConflictDisplays.size()));
            m_Controller.GetLabelFilePath().setText(String.format("File path: %s", m_ConflictDisplays.get(m_CurrentDisplayIndex).GetConflict().GetFileLocation()));
        }

        m_Controller.GetButtonBack().setDisable(!HasPrev());
        m_Controller.GetButtonNext().setDisable(!HasNext());
        m_Controller.GetButtonFinish().setDisable(HasNext());

        return hasNext;
    }

    public boolean PrevDisplay() {
        boolean hasPrev;

        if(hasPrev = HasPrev()) {
            m_ConflictDisplays.get(m_CurrentDisplayIndex--).SetVisible(false);
            m_ConflictDisplays.get(m_CurrentDisplayIndex).SetVisible(true);
            m_Controller.GetLabelConflictCount().setText(String.format("%s/%s", m_CurrentDisplayIndex + 1, m_ConflictDisplays.size()));
            m_Controller.GetLabelFilePath().setText(String.format("File path: %s", m_ConflictDisplays.get(m_CurrentDisplayIndex).GetConflict().GetFileLocation()));
        }

        m_Controller.GetButtonBack().setDisable(!HasPrev());
        m_Controller.GetButtonNext().setDisable(!HasNext());

        return hasPrev;
    }

    public boolean HasNext() {
        return m_CurrentDisplayIndex < m_ConflictDisplays.size() - 1;
    }

    public boolean HasPrev() {
        return m_CurrentDisplayIndex > 0;
    }
}
