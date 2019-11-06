package components.conflicts;

import javafx.scene.Parent;
import magit.merge.Conflict;

public interface IConflictDisplay {
    void UpdateSolution();
    void SetVisible(boolean i_IsVisible);
    Parent GetGraphics();
    Conflict GetConflict();
}
