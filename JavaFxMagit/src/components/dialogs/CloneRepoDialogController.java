package components.dialogs;

import components.themes.ThemesController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class CloneRepoDialogController {

    @FXML private HBox hBoxHeader;
    @FXML private GridPane gridPaneInputs;
    @FXML private TextField textFieldRepoName;
    @FXML private TextField textFieldLocalRepoPath;
    @FXML private TextField textFieldRemoteRepoPath;
    @FXML private Button buttonOk;
    @FXML private Button buttonCancel;
    @FXML private VBox vBoxRoot;

    private static final String f_DarkTheme = "resources/Dark.css";
    private static final String f_ColorfulTheme = "resources/Colorful.css";
    private String m_CurrentTheme;
    private Runnable m_OkAction;
    private Stage m_Stage;

    public CloneRepoDialogController() {
        if(ThemesController.themeChangedProperty.get().equals("Dark")) {
            m_CurrentTheme = f_DarkTheme;
        } else if(ThemesController.themeChangedProperty.get().equals("Colorful")) {
            m_CurrentTheme = f_ColorfulTheme;
        } else {
            m_CurrentTheme = "";
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
                vBoxRoot.getStylesheets().clear();
                vBoxRoot.applyCss();
            } else {
                vBoxRoot.getStylesheets().clear();
                vBoxRoot.getStylesheets().add(getClass().getResource(m_CurrentTheme).toExternalForm());
                vBoxRoot.applyCss();
            }
        });
    }

    public HBox gethBoxHeader() {
        return hBoxHeader;
    }

    public static CloneRepoDialogController LoadFxml() {
        FXMLLoader fxmlLoader = new FXMLLoader();
        URL url = CloneRepoDialogController.class.getResource("CloneRepoDialog.fxml");
        fxmlLoader.setLocation(url);

        try {
            fxmlLoader.load(url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fxmlLoader.getController();
    }

    @FXML public void initialize() {
        if(!m_CurrentTheme.isEmpty()) {
            vBoxRoot.getStylesheets().add(getClass().getResource(m_CurrentTheme).toExternalForm());
            vBoxRoot.applyCss();
        }
    }

    public void ShowDialog() {
        m_Stage = new Stage();
        Scene mainScene = new Scene(vBoxRoot);
        m_Stage.setScene(mainScene);
        m_Stage.setTitle("Clone Repository");
        m_Stage.getIcons().add(new Image(getClass().getResourceAsStream("/main/resources/MAGit.png")));
        m_Stage.show();
    }

    @FXML void cancelAction() {
        m_Stage.close();
    }

    @FXML void okAction() {
        m_OkAction.run();
        m_Stage.close();
    }

    public void SetOkAction(Runnable i_OkAction) {
        m_OkAction = i_OkAction;
    }

    public TextField GetTextFieldRepoName() {
        return textFieldRepoName;
    }

    public TextField GetTextFieldLocalRepoPath() {
        return textFieldLocalRepoPath;
    }

    public TextField GetTextFieldRemoteRepoPath() {
        return textFieldRemoteRepoPath;
    }
}
