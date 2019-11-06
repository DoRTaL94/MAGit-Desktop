package magit;

import MagitExceptions.*;
import data.structures.Branch;
import data.structures.Repository;
import javafx.beans.property.StringProperty;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public interface IEngine {
    void LoadRepositoryFromXml(String i_XmlPath, StringProperty i_ProgressProperty) throws FileNotFoundException, RepositoryAlreadyExistsException, XmlErrorsException, FolderInLocationAlreadyExistsException;
    void LoadDataFromRepository(String i_RepositoryFullPath) throws IOException;
    void ChangeActiveRepository(String i_RepositoryFullPath) throws NotRepositoryFolderException, IOException;
    List<String> ShowCurrentCommitFiles();
    List<List<String>> GetWorkingCopyDelta();
    boolean Commit(String i_Description, Branch i_SecondPrecedingIfMerge) throws IOException, EmptyWcException, CommitAlreadyExistsException;
    List<String> ShowAllBranches();
    void CreateNewBranch(String i_BranchName) throws PointedCommitEmptyException;
    void DeleteBranch(String i_BranchName) throws IOException;
    void Checkout(String i_BranchName, boolean i_IsSkipWcCheck) throws Exception;
    List<String> ShowActiveBranchHistory();
    String GetCurrentUserName();
    void SetCurrentUserName(String i_CurrentUserName);
    String GetRepositoryPath();
    void SetActiveRepository(Repository i_Repository);
    Repository GetActiveRepository();
    void CreateRepositoryAndFiles(String i_RepositoryName, String i_RepositoryLocation) throws RepositoryAlreadyExistsException, FolderInLocationAlreadyExistsException;
    void SetRepositoryPath(String i_RepositoryPath);
    void ResetHeadBranch(String i_PointedCommitSha1) throws IOException, Sha1LengthException;
    void ExportRepositoryToXml(String i_XmlPath) throws XmlErrorsException, RepositoryNotLoadedException;
    boolean IsBranchNameExists(String branchName);
    String GetRemoteRepositoryLocation();
    String ReplaceRootPath(String i_OriginalPath, String i_RootPath, int i_FromIndex);
}
