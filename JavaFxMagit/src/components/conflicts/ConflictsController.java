package components.conflicts;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class ConflictsController {

    @FXML private Label labelConflictCount;
    @FXML private Label labelFilePath;
    @FXML private Button buttonBack;
    @FXML private Button buttonNext;
    @FXML private Button buttonFinish;
    @FXML private StackPane stackPaneConflictDisplay;

    private Runnable m_NextAction;
    private Runnable m_BackAction;
    private Runnable m_FinishAction;

    @FXML void backAction() {
        m_BackAction.run();
    }

    @FXML void nextAction() {
        m_NextAction.run();
    }

    @FXML void finishAction() {
        m_FinishAction.run();
    }

    public void SetNextAction(Runnable i_Action) {
        m_NextAction = i_Action;
    }

    public void SetBackAction(Runnable i_Action) {
        m_BackAction = i_Action;
    }

    public void SetFinishAction(Runnable i_Action) {
        m_FinishAction = i_Action;
    }

    public Label GetLabelConflictCount() {
        return labelConflictCount;
    }

    public Label GetLabelFilePath() {
        return labelFilePath;
    }

    public Button GetButtonBack() {
        return buttonBack;
    }

    public Button GetButtonNext() {
        return buttonNext;
    }

    public Button GetButtonFinish() {
        return buttonFinish;
    }

    public StackPane GetStackPaneConflictDisplay() {
        return stackPaneConflictDisplay;
    }
}
