package components.conflicts.blob;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class BlobConflictController {

    @FXML private TextArea textAreaOurs;
    @FXML private TextArea textAreaTheirs;
    @FXML private TextArea textAreaAncestor;
    @FXML private TextArea textAreaFinalResult;

    public TextArea GetTextAreaOurs() {
        return textAreaOurs;
    }

    public TextArea GetTextAreaTheirs() {
        return textAreaTheirs;
    }

    public TextArea GetTextAreaAncestor() {
        return textAreaAncestor;
    }

    public TextArea GetTextAreaFinalResult() {
        return textAreaFinalResult;
    }
}
