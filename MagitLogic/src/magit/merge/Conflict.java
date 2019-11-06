package magit.merge;

import data.structures.IRepositoryFile;

public class Conflict {
    private eMergeSituation m_ConflictSituation;
    private IRepositoryFile m_Ours;
    private IRepositoryFile m_Theirs;
    private IRepositoryFile m_Ancestor;
    private String m_FullPath;
    private IRepositoryFile m_Solution;

    public Conflict(IRepositoryFile i_Ours, IRepositoryFile i_Theirs, IRepositoryFile i_Ancestor) {
        m_Ours = i_Ours;
        m_Theirs = i_Theirs;
        m_Ancestor = i_Ancestor;
    }

    public void SetConflictSituation(eMergeSituation i_ConflictSituation) {
        m_ConflictSituation = i_ConflictSituation;
    }

    public void SetFileLocation(String i_FullPath) {
        m_FullPath = i_FullPath;
    }

    public String GetFileLocation() {
        return m_FullPath;
    }

    public IRepositoryFile GetOurs() {
        return m_Ours;
    }

    public IRepositoryFile GetTheirs() {
        return m_Theirs;
    }

    public IRepositoryFile GetAncestor() {
        return m_Ancestor;
    }

    public void SetSolution(IRepositoryFile i_Solution) {
        m_Solution = i_Solution;
    }

    public void Solve() {
        m_ConflictSituation.Solve(m_FullPath, m_Solution);
    }
}
