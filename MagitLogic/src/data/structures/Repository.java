package data.structures;
import IO.FileUtilities;

import java.nio.file.Paths;
import java.util.Map;

public class Repository {
    private String m_LocationPath;
    private Map<String, Commit> m_Commits = null;
    private Map<String, Folder> m_Folders = null;
    private Map<String, Blob> m_Blobs = null;
    private Map<String, Branch> m_Branches = null;
    private Branch m_HeadBranch = null;

    private String m_RepoName = null;

    public String GetLocationPath() {
        return m_LocationPath;
    }

    public void SetLocationPath(String i_Path) {
        m_LocationPath = Paths.get(i_Path).toString().toLowerCase();
    }

    public Map<String, Commit> GetCommits() {
        return m_Commits;
    }

    public Map<String, Folder> GetFolders() {
        return m_Folders;
    }

    public Map<String, Blob> GetBlobs() {
        return m_Blobs;
    }

    public Map<String, Branch> GetBranches() {
        return m_Branches;
    }

    public void SetCommits(Map<String, Commit> i_Commits) {
        m_Commits = i_Commits;
    }

    public void SetFolders(Map<String, Folder> i_Folders) {
        m_Folders = i_Folders;
    }

    public void SetBlobs(Map<String, Blob> i_Blobs) {
        m_Blobs = i_Blobs;
    }

    public void SetBranches(Map<String, Branch> i_Branches) {
        m_Branches = i_Branches;
    }

    public Branch GetHeadBranch() {
        return m_HeadBranch;
    }

    public void SetHeadBranch(Branch i_HeadBranch) {
        m_HeadBranch = i_HeadBranch;
    }

    public String GetName() {
        return m_RepoName;
    }

    public void SetName(String i_RepoName) {
        this.m_RepoName = i_RepoName;

        if(m_LocationPath != null) {
            String reponamePath = Paths.get(m_LocationPath, ".magit", "reponame.txt").toString();
            FileUtilities.WriteToFile(reponamePath, i_RepoName);
        }
    }
}