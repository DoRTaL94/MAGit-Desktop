package components.conflicts.folder;

import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class FolderConflictController {

    @FXML private VBox vBoxOurs;
    @FXML private RadioButton radioButtonOurs;
    @FXML private VBox vBoxTheirs;
    @FXML private RadioButton radioButtonTheirs;
    @FXML private StackPane stackPaneOursFolderContent;
    @FXML private StackPane stackPaneTheirsFolderContent;

    @FXML void oursSelectedAction() {
        radioButtonTheirs.setSelected(false);
    }

    @FXML void theirsSelectedAction() {
        radioButtonOurs.setSelected(false);
    }

    public StackPane GetStackPaneOursFolderContent() {
        return stackPaneOursFolderContent;
    }

    public StackPane GetStackPaneTheirsFolderContent() {
        return stackPaneTheirsFolderContent;
    }

    public VBox GetVBoxOurs() {
        return vBoxOurs;
    }

    public RadioButton GetRadioButtonOurs() {
        return radioButtonOurs;
    }

    public VBox GetVBoxTheirs() {
        return vBoxTheirs;
    }

    public RadioButton GetRadioButtonTheirs() {
        return radioButtonTheirs;
    }
}
