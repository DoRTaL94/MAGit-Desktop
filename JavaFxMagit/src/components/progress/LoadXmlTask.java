package components.progress;

import MagitExceptions.FolderInLocationAlreadyExistsException;
import MagitExceptions.RepositoryAlreadyExistsException;
import MagitExceptions.XmlErrorsException;
import com.sun.istack.internal.Nullable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import magit.Engine;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class LoadXmlTask extends Task {
    public final BooleanProperty XmlErrorProperty = new SimpleBooleanProperty(false);
    public final BooleanProperty RepositoryAlreadyExistsProperty = new SimpleBooleanProperty(false);
    public final StringProperty FolderInLocationAlreadyExistsProperty = new SimpleStringProperty("");

    private final String m_XmlPath;
    private List<String> m_XmlErrors;

    public LoadXmlTask(String i_XmlPath) {
        m_XmlPath = i_XmlPath;
    }

    @Override
    protected Object call() {
        long maxProgress = 6;
        AtomicReference<Integer> counter = new AtomicReference<>(0);
        StringProperty progressProperty = new SimpleStringProperty("");

        progressProperty.addListener((observable, oldValue, newValue) -> {
            counter.set(counter.get() + 1);
            updateMessage(newValue);
            updateProgress((long) counter.get(), maxProgress);
        });

        if(m_XmlPath != null && !m_XmlPath.isEmpty()) {
            try {
                Engine.Creator.GetInstance().LoadRepositoryFromXml(m_XmlPath, progressProperty);
            } catch (FileNotFoundException ignored) {
            } catch (RepositoryAlreadyExistsException e) {
                RepositoryAlreadyExistsProperty.set(true);
                this.cancel();
            } catch (XmlErrorsException e) {
                XmlErrorProperty.set(true);
                m_XmlErrors = e.GetErrors();
                this.cancel();
            } catch (FolderInLocationAlreadyExistsException e) {
                FolderInLocationAlreadyExistsProperty.set(e.getMessage());
                this.cancel();
            }
        }

        return null;
    }

    @Nullable
    public List<String> GetXmlError() {
        return m_XmlErrors;
    }
}
