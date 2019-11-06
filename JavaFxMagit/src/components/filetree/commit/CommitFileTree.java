package components.filetree.commit;

import components.themes.ThemesController;
import data.structures.Commit;
import data.structures.Folder;
import data.structures.Repository;
import data.structures.eFileType;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import magit.CommitNode;
import magit.Engine;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class CommitFileTree extends TreeView<Folder.Data> {
    private static final String FOLDER_ICON_PATH = "/components/filetree/resources/folder.png";
    private static final String TXT_FILE_ICON_PATH = "/components/filetree/resources/text-x-generic.png";
    private final Repository f_ActiveRepository =  Engine.Creator.GetInstance().GetActiveRepository();
    private CommitNode m_CommitNode;
    private static final String f_DarkTheme = "resources/Dark.css";
    private static final String f_ColorfulTheme = "resources/Colorful.css";
    private String m_CurrentTheme;
    private VBox m_Root;

    public CommitFileTree(CommitNode i_CommitNode) {
        if(ThemesController.themeChangedProperty.get().equals("Dark")) {
            m_CurrentTheme = f_DarkTheme;
        } else if(ThemesController.themeChangedProperty.get().equals("Colorful")) {
            m_CurrentTheme = f_ColorfulTheme;
        } else {
            m_CurrentTheme = "";
        }

        if(!m_CurrentTheme.isEmpty()) {
            this.getStylesheets().add(getClass().getResource(m_CurrentTheme).toExternalForm());
            this.applyCss();
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

        m_CommitNode = i_CommitNode;
        CreateTree(i_CommitNode.GetCommit());
        loadFXML();
    }

    private void loadFXML() {
        Stage commitFilesStage = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader();
        URL url = getClass().getResource("/components/filetree/commit/CommitFileTreeUI.fxml");
        fxmlLoader.setLocation(url);

        try {
            m_Root = fxmlLoader.load(url.openStream());
            Scene mainScene = new Scene(m_Root);
            commitFilesStage.setScene(mainScene);
            commitFilesStage.setTitle("Commit Files");
            commitFilesStage.getIcons().add(new Image(getClass().getResourceAsStream("/main/resources/MAGit.png")));
            commitFilesStage.show();
            ((CommitFileTreeController)fxmlLoader.getController()).SetCommitFilesTree(this);
            ((CommitFileTreeController)fxmlLoader.getController()).SetLabelCommitSha1(m_CommitNode.GetSha1());

            m_Root.getStylesheets().add(getClass().getResource(m_CurrentTheme).toExternalForm());
            m_Root.applyCss();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void CreateTree(Commit i_Commit) {
        Folder.Data rootFolderData = new Folder.Data();
        rootFolderData.SetSHA1(i_Commit.GetRootFolderSHA1());
        rootFolderData.SetlastUpdate(i_Commit.GetLastUpdate());
        rootFolderData.SetLastChanger(i_Commit.GetLastChanger());
        rootFolderData.SetName(f_ActiveRepository.GetName());
        rootFolderData.SetFileType(eFileType.FOLDER);

        this.setRoot(CreateTreeRec(rootFolderData));
        this.getRoot().setExpanded(true);
    }

    public TreeItem<Folder.Data> CreateTreeRec(Folder.Data i_File) {
        TreeItem<Folder.Data> item = new TreeItem<>(i_File);

        if(i_File.GetFileType().equals(eFileType.FOLDER)) {
            Folder root = f_ActiveRepository.GetFolders().get(i_File.GetSHA1());
            List<Folder.Data> files = root.GetFiles();

            for(Folder.Data file: files) {
                item.getChildren().add(CreateTreeRec(file));
            }

            item.setGraphic(new ImageView(getClass().getResource(FOLDER_ICON_PATH).toExternalForm()));
        }
        else {
            item.setGraphic(new ImageView(getClass().getResource(TXT_FILE_ICON_PATH).toExternalForm()));
        }

        return item;
    }
}