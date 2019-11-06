package components.warnings;

import javafx.beans.property.BooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import javax.swing.text.StyledEditorKit;

public class WarningRepoExistsController {

    @FXML private Button buttonExistingRepo;
    @FXML private Button buttonDeleteAndCreateNew;
    @FXML private Button buttonCancelRepoWarning;

    private BooleanProperty m_IsLoadRepoProperty;
    private BooleanProperty m_IsCreateNewProperty;
    private BooleanProperty m_IsCancelProperty;

    @FXML void cancelAction(ActionEvent event) {
        m_IsCancelProperty.set(!m_IsCancelProperty.get());
    }

    @FXML void deleteAndCreateNewRepoAction(ActionEvent event) {
        m_IsCreateNewProperty.set(!m_IsCreateNewProperty.get());
    }

    @FXML void loadExistingRepoAction(ActionEvent event) {
        m_IsLoadRepoProperty.set(!m_IsCancelProperty.get());
    }

    public void SetIsCancelProperty(BooleanProperty i_IsCancelProperty) {
        m_IsCancelProperty = i_IsCancelProperty;
    }

    public void SetIsLoadRepoProperty(BooleanProperty i_IsLoadRepoProperty) {
        m_IsLoadRepoProperty = i_IsLoadRepoProperty;
    }

    public void SetIsCreateNewProperty(BooleanProperty i_IsCreateNewProperty) {
        m_IsCreateNewProperty = i_IsCreateNewProperty;
    }
}
