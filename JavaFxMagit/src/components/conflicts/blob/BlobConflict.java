package components.conflicts.blob;

import components.conflicts.IConflictDisplay;
import data.structures.Blob;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import magit.merge.Conflict;
import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

public class BlobConflict implements IConflictDisplay {
    private Conflict m_Conflict;
    private HBox m_Root;
    private BlobConflictController m_Controller;

    public BlobConflict(Conflict i_BlobConflict) {
        m_Conflict = i_BlobConflict;
        loadFxml();
        updateGraphics();
    }

    private void updateGraphics() {
        String ancestorContent = "";
        String oursContent = "";
        String theirsContent = "";

        if(m_Conflict.GetAncestor() != null) {
            ancestorContent = ((Blob) m_Conflict.GetAncestor()).GetText();
        }

        if(m_Conflict.GetOurs() != null) {
            oursContent = ((Blob) m_Conflict.GetOurs()).GetText();
        }

        if(m_Conflict.GetTheirs() != null) {
            theirsContent = ((Blob) m_Conflict.GetTheirs()).GetText();
        }

        m_Controller.GetTextAreaAncestor().setText(ancestorContent);
        m_Controller.GetTextAreaOurs().setText(oursContent);
        m_Controller.GetTextAreaTheirs().setText(theirsContent);
    }

    private void loadFxml() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            URL url = getClass().getResource("BlobConflictUI.fxml");
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
        Blob blob = new Blob();
        blob.SetText(m_Controller.GetTextAreaFinalResult().getText());
        m_Conflict.SetSolution(blob);
    }

    public void SetVisible(boolean i_IsVisible) {
        m_Root.setVisible(i_IsVisible);
    }

    public Conflict GetConflict() { return m_Conflict; }
}
