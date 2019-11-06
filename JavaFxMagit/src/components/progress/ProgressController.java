package components.progress;

import components.Diff.DiffController;
import components.themes.ThemesController;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import magit.Engine;
import main.MagitController;

import java.io.IOException;
import java.net.URL;

public class ProgressController {
    @FXML private VBox vBoxLoading;
    @FXML private ProgressBar progressBar;
    @FXML private TextArea textBoxDetails;
    @FXML private Label labelPercent;

    private static final String f_DarkTheme = "/main/resources/DarkDialog.css";
    private static final String f_ColorfulTheme = "/main/resources/ColorfulDialog.css";
    private String m_CurrentTheme;
    private Stage m_Stage;
    private Scene m_Scene;
    private Task m_Task;

    public ProgressController() {
        m_Stage = new Stage();

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
                vBoxLoading.getStylesheets().clear();
                vBoxLoading.applyCss();
            } else {
                vBoxLoading.getStylesheets().clear();
                vBoxLoading.getStylesheets().add(getClass().getResource(m_CurrentTheme).toExternalForm());
                vBoxLoading.applyCss();
            }
        });
    }

    public void SetTask(Task i_Task) {
        m_Task = i_Task;

        i_Task.progressProperty().addListener((observable, oldValue, newValue) -> {
            progressBar.setProgress((double) newValue);
            labelPercent.setText(String.format("%.0f%s", ((double) newValue) * 100, "%"));
        });
        i_Task.setOnCancelled(event -> m_Stage.close());
        i_Task.messageProperty().addListener((observable, oldValue, newValue) -> addText(newValue));
        MagitController.UIRefreshedProperty.addListener((observable, oldValue, newValue) -> {
            if(newValue) {
                m_Stage.close();
            }
        });
    }

    public void Run() {
        Thread th = new Thread(m_Task);
        th.setDaemon(true);
        th.start();
        m_Stage.show();
    }

    public static ProgressController LoadFXML() {
        FXMLLoader fxmlLoader = new FXMLLoader();
        URL url = DiffController.class.getResource("/components/progress/ProgressUI.fxml");
        fxmlLoader.setLocation(url);

        try {
            fxmlLoader.load(url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fxmlLoader.getController();
    }

    @FXML public void initialize() {
        m_Scene = new Scene(vBoxLoading);
        m_Stage.setScene(m_Scene);
        m_Stage.setTitle("Loading...");
        m_Stage.getIcons().add(new Image(getClass().getResourceAsStream("/main/resources/MAGit.png")));

        if(!m_CurrentTheme.isEmpty()) {
            vBoxLoading.getStylesheets().add(getClass().getResource(m_CurrentTheme).toExternalForm());
            vBoxLoading.applyCss();
        }
    }

    public void addText(String i_Text) {
        StringBuilder sb = new StringBuilder(textBoxDetails.getText());
        sb.append(String.format("%s%s", i_Text, System.lineSeparator()));
        textBoxDetails.setText(sb.toString());
    }
}
