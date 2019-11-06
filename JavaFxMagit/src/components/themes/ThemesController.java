package components.themes;

import components.Diff.DiffController;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import main.MagitController;

import java.io.IOException;
import java.net.URL;

public class ThemesController {
    @FXML private RadioButton radioButtonDark;
    @FXML private RadioButton radioButtonColor;
    @FXML private RadioButton radioButtonDefault;
    @FXML private Button buttonOk;
    @FXML private Button buttonCancel;
    @FXML private VBox vBoxRoot;

    private static final String f_DarkTheme = "/main/resources/DarkDialog.css";
    private static final String f_ColorfulTheme = "/main/resources/ColorfulDialog.css";
    private String m_CurrentTheme;
    public static final StringProperty themeChangedProperty = new SimpleStringProperty("");;
    private Stage m_Stage;
    Scene m_Scene;

    public ThemesController() {
        m_Stage = new Stage();
        m_CurrentTheme = "";

        themeChangedProperty.addListener((observable, oldValue, newValue) -> {
            if(newValue.equals("Dark")) {
                m_CurrentTheme = f_DarkTheme;
            }
            else if(newValue.equals("Colorful")) {
                m_CurrentTheme = f_ColorfulTheme;
            }

            if(newValue.equals("Colorful")) {
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

    public static ThemesController LoadFXML() {
        FXMLLoader fxmlLoader = new FXMLLoader();
        URL url = DiffController.class.getResource("/components/themes/ThemesUI.fxml");
        fxmlLoader.setLocation(url);

        try {
            fxmlLoader.load(url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fxmlLoader.getController();
    }

    @FXML public void initialize() {
        radioButtonColor.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue) {
                radioButtonDark.setSelected(false);
                radioButtonDefault.setSelected(false);
            }

            buttonOk.setDisable(!radioButtonDark.isSelected() && !radioButtonColor.isSelected() && !radioButtonDefault.isSelected());
        });

        radioButtonDark.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue) {
                radioButtonColor.setSelected(false);
                radioButtonDefault.setSelected(false);
            }

            buttonOk.setDisable(!radioButtonDark.isSelected() && !radioButtonColor.isSelected() && !radioButtonDefault.isSelected());
        });

        radioButtonDefault.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue) {
                radioButtonColor.setSelected(false);
                radioButtonDark.setSelected(false);
            }

            buttonOk.setDisable(!radioButtonDark.isSelected() && !radioButtonColor.isSelected() && !radioButtonDefault.isSelected());
        });

        m_Scene = new Scene(vBoxRoot);
        m_Stage.setScene(m_Scene);
        m_Stage.setTitle("Change Theme");
        m_Stage.getIcons().add(new Image(getClass().getResourceAsStream("/main/resources/MAGit.png")));
    }

    public void Show() {
        m_Stage.showAndWait();
    }

    @FXML public void okAction() {
        String theme;

        if(radioButtonDark.isSelected()) {
            theme = "Dark";
        }
        else if(radioButtonColor.isSelected()) {
            theme = "Colorful";
        }
        else {
            theme = "Default";
        }

        themeChangedProperty.set(theme);
        m_Stage.close();
    }

    @FXML public void cancelAction() {
        m_Stage.close();
    }
}
