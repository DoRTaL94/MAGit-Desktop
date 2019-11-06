package magit.merge;

import MagitExceptions.CommitAlreadyExistsException;
import MagitExceptions.EmptyWcException;
import MagitExceptions.MergeException;
import data.structures.Branch;
import magit.Engine;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class ConflictsManager implements Iterable<Conflict> {
    private List<Conflict> m_Conflicts;
    private Branch m_MergedBranch;
    private Consumer<Consumer<String>> m_AskForCommitDescriptionAction;
    private Consumer<String> m_ErrorMessageAction;

    public ConflictsManager(List<Conflict> i_Conflicts, Branch i_MergedBranch) {
        m_Conflicts = i_Conflicts;
        m_MergedBranch = i_MergedBranch;
    }

    public Iterator<Conflict> GetConflictsIterator() { return m_Conflicts.iterator(); }

    public void SolveConflicts() {
        for(Conflict conflict: m_Conflicts) {
            conflict.Solve();
        }

        m_AskForCommitDescriptionAction.accept(this::commitWrapper);
    }

    private void commitWrapper(String i_Description) {
        try {
            Engine.Creator.GetInstance().Commit(i_Description, m_MergedBranch);
        } catch (IOException | CommitAlreadyExistsException | EmptyWcException e) {
            m_ErrorMessageAction.accept(e.getMessage());
        }
    }

    @Override
    public Iterator<Conflict> iterator() {
        return m_Conflicts.iterator();
    }

    @Override
    public void forEach(Consumer<? super Conflict> action) {
        m_Conflicts.forEach(action);
    }

    @Override
    public Spliterator<Conflict> spliterator() {
        return m_Conflicts.spliterator();
    }

    public void SetActionToGetCommitDesctiprionFromUser(Consumer<Consumer<String>> i_Action) {
        m_AskForCommitDescriptionAction = i_Action;
    }

    public void SetErrorMessageAction(Consumer<String> i_Action) {
        m_ErrorMessageAction = i_Action;
    }

    public int GetConflictsCount() { return m_Conflicts.size(); }
}
