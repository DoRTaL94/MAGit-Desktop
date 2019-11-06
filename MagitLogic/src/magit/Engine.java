package magit;

import IO.FileUtilities;
import MagitExceptions.*;
import data.structures.*;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import magit.merge.Conflict;
import magit.merge.ConflictsManager;
import magit.merge.IMergeTask;
import magit.merge.eMergeSituation;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import data.structures.eFileType;
import puk.team.course.magit.ancestor.finder.AncestorFinder;
import resources.jaxb.schema.generated.*;
import string.StringUtilities;

public class Engine implements IEngine {
    public static final String DATE_FORMAT  = "dd.MM.yyyy-HH:mm:ss:SSS";
    public final StringProperty currentNameProperty;
    public final BooleanProperty repositoryChangedProperty;
    public final BooleanProperty remoteRepositoryClonedProperty;
    public final BooleanProperty loadedProperty;

    private final Factory f_Factory;
    private Repository m_ActiveRepository = null;
    private String m_ActiveRepositoryPath   = null;
    private String m_RemoteRepositoryLocation = "";

    // Engine is a singleton class.
    private Engine() {
        currentNameProperty = new SimpleStringProperty("Administrator");
        loadedProperty = new SimpleBooleanProperty(false);
        repositoryChangedProperty = new SimpleBooleanProperty(false);
        remoteRepositoryClonedProperty = new SimpleBooleanProperty(false);
        f_Factory = new Factory(this);
    }

    public ConflictsManager MergeBranches(Branch i_Ours, Branch i_Theirs,
                                          Consumer<Consumer<String>> i_GetCommitDescriptionAction,
                                          Runnable i_FastForwardMergeMessageToUserAction,
                                          Consumer<String> i_MergeExceptionMessageAction) {

        ConflictsManager conflictsManager = null;

        if(i_Ours == i_Theirs) {
            i_MergeExceptionMessageAction.accept("Branch can't merge with itself.");
        } else {
            boolean isFastForwardMerge;

            try {
                isFastForwardMerge = checkIfFastForwardMerge(i_Ours, i_Theirs, i_FastForwardMergeMessageToUserAction);

                if (!isFastForwardMerge) {
                    AncestorFinder ancestorFinder = new AncestorFinder(sha1 -> m_ActiveRepository.GetCommits().get(sha1));
                    String ancestorSha1 = ancestorFinder
                            .traceAncestor(i_Ours.GetPointedCommitSha1(), i_Theirs.GetPointedCommitSha1());

                    Commit ancestor = m_ActiveRepository.GetCommits().get(ancestorSha1);
                    Commit ours = m_ActiveRepository.GetCommits().get(i_Ours.GetPointedCommitSha1());
                    Commit theirs = m_ActiveRepository.GetCommits().get(i_Theirs.GetPointedCommitSha1());

                    Folder oursRootFolder = m_ActiveRepository.GetFolders().get(ours.GetRootFolderSHA1());
                    Folder theirsRootFolder = m_ActiveRepository.GetFolders().get(theirs.GetRootFolderSHA1());
                    Folder ancestorRootFolder = m_ActiveRepository.GetFolders().get(ancestor.GetRootFolderSHA1());

                    Map<String, String> oursPathToSha1Map = f_Factory.CreatePathToSha1Map(i_Ours);
                    Map<String, String> theirsPathToSha1Map = f_Factory.CreatePathToSha1Map(i_Theirs);
                    Map<String, String> ancestorPathToSha1Map = f_Factory.CreatePathToSha1MapFromCommit(ancestor);

                    List<Map<String, String>> pathToSha1Maps = new ArrayList<>();
                    pathToSha1Maps.add(oursPathToSha1Map);
                    pathToSha1Maps.add(theirsPathToSha1Map);
                    pathToSha1Maps.add(ancestorPathToSha1Map);

                    List<Conflict> conflicts = new ArrayList<>();

                    conflicts.addAll(findNewFiles(m_ActiveRepositoryPath, oursRootFolder, theirsRootFolder, ancestorRootFolder, pathToSha1Maps));
                    conflicts.addAll(findDeletedFiles(m_ActiveRepositoryPath, oursRootFolder, theirsRootFolder, ancestorRootFolder, pathToSha1Maps));
                    conflicts.addAll(findChangedFiles(m_ActiveRepositoryPath, oursRootFolder, theirsRootFolder, ancestorRootFolder, pathToSha1Maps));

                    if (conflicts.size() == 0) {
                        i_GetCommitDescriptionAction.accept(s -> handleNoConflictsInMerge(s, i_Theirs, i_MergeExceptionMessageAction));
                    } else {
                        conflictsManager = new ConflictsManager(conflicts, i_Theirs);
                        conflictsManager.SetActionToGetCommitDesctiprionFromUser(i_GetCommitDescriptionAction);
                        conflictsManager.SetErrorMessageAction(i_MergeExceptionMessageAction);
                    }
                }
            } catch (MergeException e) {
                i_MergeExceptionMessageAction.accept(e.getMessage());
            }
        }

        return conflictsManager;
    }

    private boolean checkIfFastForwardMerge(Branch i_Ours, Branch i_Theirs, Runnable i_FastForwardMergeMessageToUserAction) throws MergeException {
        Commit ours = m_ActiveRepository.GetCommits().get(i_Ours.GetPointedCommitSha1());
        Commit theirs = m_ActiveRepository.GetCommits().get(i_Theirs.GetPointedCommitSha1());

        boolean isTheirsAncestorOfOurs = checkIfOursAncestorOfTheirs(theirs, ours);
        boolean isOursAncestorOfTheirs = checkIfOursAncestorOfTheirs(ours, theirs);

        if(isTheirsAncestorOfOurs) {
            throw new MergeException("Active branch contains the selected branch for merge.");
        }
        else if(isOursAncestorOfTheirs){
            i_Ours.SetPointedCommitSha1(i_Theirs.GetPointedCommitSha1());
            new File(Paths.get(m_ActiveRepositoryPath, ".magit", "branches", i_Theirs.GetName() + ".txt").toString()).delete();
            FileUtilities.WriteToFile(Paths.get(m_ActiveRepositoryPath, ".magit", "branches", i_Ours.GetName() + ".txt").toString(),
                    i_Theirs.GetPointedCommitSha1());
            m_ActiveRepository.GetBranches().remove(i_Theirs.GetName());
            repositoryChangedProperty.set(repositoryChangedProperty.not().get());
            i_FastForwardMergeMessageToUserAction.run();
        }

        return isOursAncestorOfTheirs;
    }

    private void handleNoConflictsInMerge(String i_CommitDescription, Branch i_MergedBranch, Consumer<String> i_MergeExceptionMessageAction) {
        try {
            this.Commit(i_CommitDescription, i_MergedBranch);
        } catch (IOException | EmptyWcException | CommitAlreadyExistsException e) {
            i_MergeExceptionMessageAction.accept(e.getMessage());
        }
    }

    private boolean checkIfOursAncestorOfTheirs(Commit i_Ours, Commit i_Theirs) {
        boolean result;

        if(i_Theirs == null || i_Theirs.getFirstPrecedingSha1() == null) {
            result = false;
        }
        else if (i_Theirs == i_Ours) {
            result = true;
        }
        else {
            Commit firstPreceding = m_ActiveRepository.GetCommits().get(i_Theirs.getFirstPrecedingSha1());
            Commit secondPreceding = m_ActiveRepository.GetCommits().get(i_Theirs.getSecondPrecedingSha1());

            result = checkIfOursAncestorOfTheirs(i_Ours, firstPreceding) ||
                    checkIfOursAncestorOfTheirs(i_Ours, secondPreceding);
        }

        return result;
    }

    private List<Conflict> findChangedFiles(String i_CurrentPath, IRepositoryFile i_Ours, IRepositoryFile i_Theirs, IRepositoryFile i_Ancestor, List<Map<String, String>> i_PathToSha1Maps) {
        List<Conflict> conflicts = new ArrayList<>();
        eMergeSituation mergeSituation = findMergeSituation(i_CurrentPath, i_Ours, i_Theirs, i_Ancestor, i_PathToSha1Maps);

        if(i_Ours instanceof Blob) {
            switch (mergeSituation) {
                case SAME_NAME_DIFF_SHA1:
                case SAME_NAME_EQU_SHA1:
                case CHANGED_TO_SAME_IN_BOTH:
                case CHANGED_TO_DIFF_IN_BOTH:
                    Conflict conflict = new Conflict(i_Ours, i_Theirs, i_Ancestor);
                    conflict.SetConflictSituation(mergeSituation);
                    conflict.SetFileLocation(i_CurrentPath);
                    conflicts.add(conflict);
                    break;
                case OURS_SAME_THEIR_CHANGED:
                    mergeSituation.Solve(i_CurrentPath, i_Theirs);
                    break;
            }
        }
        else {
            conflicts.addAll(checkInsideFolder(i_CurrentPath, i_Ours, i_PathToSha1Maps, this::findChangedFiles));
        }


        return conflicts;
    }

    private List<Conflict> findDeletedFiles(String i_CurrentPath, IRepositoryFile i_Ours, IRepositoryFile i_Theirs, IRepositoryFile i_Ancestor, List<Map<String, String>> i_PathToSha1Maps) {
        List<Conflict> conflicts = new ArrayList<>();
        eMergeSituation mergeSituation = findMergeSituation(i_CurrentPath, i_Ours, i_Theirs, i_Ancestor, i_PathToSha1Maps);

        switch (mergeSituation) {
            case OURS_CHANGED_THEIRS_DELETED:
            case OURS_DELETED_THEIRS_CHANGED:
                Conflict conflict = new Conflict(i_Ours, i_Theirs, i_Ancestor);
                conflict.SetConflictSituation(mergeSituation);
                conflict.SetFileLocation(i_CurrentPath);
                conflicts.add(conflict);
                break;
            case OURS_SAME_THEIRS_DELETED:
                mergeSituation.Solve(i_CurrentPath, i_Ours);
                break;
            default:
                conflicts.addAll(checkInsideFolder(i_CurrentPath, i_Ours, i_PathToSha1Maps, this::findDeletedFiles));
                conflicts.addAll(checkInsideFolder(i_CurrentPath, i_Theirs, i_PathToSha1Maps, this::findDeletedFiles));
                break;
        }

        return conflicts;
    }

    private List<Conflict> findNewFiles(String i_CurrentPath, IRepositoryFile i_Ours, IRepositoryFile i_Theirs, IRepositoryFile i_Ancestor, List<Map<String, String>> i_PathToSha1Maps) {
        eMergeSituation mergeSituation = findMergeSituation(i_CurrentPath, i_Ours, i_Theirs, i_Ancestor, i_PathToSha1Maps);

        if(mergeSituation.equals(eMergeSituation.NEW_FILE_IN_THEIRS)) {
            mergeSituation.Solve(i_CurrentPath, i_Theirs);
        }

        return new ArrayList<>(checkInsideFolder(i_CurrentPath, i_Theirs, i_PathToSha1Maps, this::findNewFiles));
    }

    private List<Conflict> checkInsideFolder(String i_CurrentPath, IRepositoryFile i_Folder, List<Map<String, String>> i_PathToSha1Maps, IMergeTask i_MergeTask) {
        List<Conflict> conflicts = new ArrayList<>();

        if (i_Folder instanceof Folder) {
            Map<String, String> oursPathToSha1 = i_PathToSha1Maps.get(0);
            Map<String, String> theirsPathToSha1 = i_PathToSha1Maps.get(1);
            Map<String, String> ancestorPathToSha1 = i_PathToSha1Maps.get(2);

            for (Folder.Data file : ((Folder) i_Folder).GetFiles()) {
                String filePath = Paths.get(i_CurrentPath, file.GetName()).toString();
                IRepositoryFile ours = file.GetFileType().equals(eFileType.FOLDER) ?
                        m_ActiveRepository.GetFolders().get(oursPathToSha1.get(filePath)) :
                        m_ActiveRepository.GetBlobs().get(oursPathToSha1.get(filePath));
                IRepositoryFile theirs = file.GetFileType().equals(eFileType.FOLDER) ?
                        m_ActiveRepository.GetFolders().get(theirsPathToSha1.get(filePath)) :
                        m_ActiveRepository.GetBlobs().get(theirsPathToSha1.get(filePath));
                IRepositoryFile ancestor = file.GetFileType().equals(eFileType.FOLDER) ?
                        m_ActiveRepository.GetFolders().get(ancestorPathToSha1.get(filePath)) :
                        m_ActiveRepository.GetBlobs().get(ancestorPathToSha1.get(filePath));

                conflicts.addAll(i_MergeTask.ExecuteTask(filePath, ours, theirs, ancestor, i_PathToSha1Maps));
            }
        }

        return conflicts;
    }

    private eMergeSituation findMergeSituation(String i_CurrentPath, IRepositoryFile i_Ours, IRepositoryFile i_Theirs, IRepositoryFile i_Ancestor, List<Map<String, String>> i_PathToSha1Maps) {
        eMergeSituation mergeSituation;

        String oursSha1 = "";
        String theirsSha1 = "";
        String ancestorSha1 = "";

        String currentPath = m_RemoteRepositoryLocation.isEmpty() ? i_CurrentPath : ReplaceRootPath(i_CurrentPath, m_RemoteRepositoryLocation, 2);

        if(i_Ours != null) {
            oursSha1 = i_Ours instanceof Folder ? DigestUtils.sha1Hex(((Folder) i_Ours).toStringForSha1(Paths.get(currentPath))) :
                    DigestUtils.sha1Hex(((Blob) i_Ours).toStringForSha1());
        }

        if(i_Theirs != null) {
            theirsSha1 = i_Theirs instanceof Folder ? DigestUtils.sha1Hex(((Folder) i_Theirs).toStringForSha1(Paths.get(currentPath))) :
                    DigestUtils.sha1Hex(((Blob) i_Theirs).toStringForSha1());
        }

        if(i_Ancestor != null) {
            ancestorSha1 = i_Ancestor instanceof Folder ? DigestUtils.sha1Hex(((Folder) i_Ancestor).toStringForSha1(Paths.get(currentPath))) :
                    DigestUtils.sha1Hex(((Blob) i_Ancestor).toStringForSha1());
        }

        boolean isFileExistsInOurs = !oursSha1.isEmpty();
        boolean isFileExistsInTheirs = !theirsSha1.isEmpty();
        boolean isFileExistsInAncestor = !ancestorSha1.isEmpty();
        boolean isOursEqualsTheirs = oursSha1.equals(theirsSha1);
        boolean isTheirsEqualsAncestor = theirsSha1.equals(ancestorSha1);
        boolean isAncestorEqualsOurs = ancestorSha1.equals(oursSha1);

        if(!isFileExistsInOurs && isFileExistsInTheirs && !isFileExistsInAncestor) {
            mergeSituation = eMergeSituation.NEW_FILE_IN_THEIRS;
        }
        else if(!isFileExistsInOurs && isFileExistsInTheirs && isFileExistsInAncestor &&
                !isOursEqualsTheirs && !isTheirsEqualsAncestor && !isAncestorEqualsOurs) {
            mergeSituation = eMergeSituation.OURS_DELETED_THEIRS_CHANGED;
        }
//        else if(!isFileExistsInOurs && isFileExistsInTheirs && isFileExistsInAncestor &&
//                !isOursEqualsTheirs && isTheirsEqualsAncestor && !isAncestorEqualsOurs) {
//            mergeSituation = eMergeSituation.OURS_DELETED_THEIRS_SAME;
//        }
//        else if(isFileExistsInOurs && !isFileExistsInTheirs && !isFileExistsInAncestor &&
//                !isOursEqualsTheirs && !isTheirsEqualsAncestor && !isAncestorEqualsOurs) {
//            mergeSituation = eMergeSituation.NEW_FILE_IN_OURS;
//        }
        else if(isFileExistsInOurs && !isFileExistsInTheirs && isFileExistsInAncestor &&
                !isOursEqualsTheirs && !isTheirsEqualsAncestor && !isAncestorEqualsOurs) {
            mergeSituation = eMergeSituation.OURS_CHANGED_THEIRS_DELETED;
        }
        else if(isFileExistsInOurs && !isFileExistsInTheirs && isFileExistsInAncestor &&
                !isOursEqualsTheirs && !isTheirsEqualsAncestor && isAncestorEqualsOurs) {
            mergeSituation = eMergeSituation.OURS_SAME_THEIRS_DELETED;
        }
        else if (isFileExistsInOurs && isFileExistsInTheirs && !isFileExistsInAncestor &&
                isOursEqualsTheirs && !isTheirsEqualsAncestor && !isAncestorEqualsOurs) {
            mergeSituation = eMergeSituation.SAME_NAME_EQU_SHA1;
        }
        else if (isFileExistsInOurs && isFileExistsInTheirs && !isFileExistsInAncestor &&
                !isOursEqualsTheirs && !isTheirsEqualsAncestor && !isAncestorEqualsOurs) {
            mergeSituation = eMergeSituation.SAME_NAME_DIFF_SHA1;
        }
        else if (isFileExistsInOurs && isFileExistsInTheirs && isFileExistsInAncestor &&
                !isOursEqualsTheirs && !isTheirsEqualsAncestor && !isAncestorEqualsOurs) {
            mergeSituation = eMergeSituation.CHANGED_TO_DIFF_IN_BOTH;
        }
//        else if (isFileExistsInOurs && isFileExistsInTheirs && isFileExistsInAncestor &&
//                !isOursEqualsTheirs && isTheirsEqualsAncestor && !isAncestorEqualsOurs) {
//            mergeSituation = eMergeSituation.OURS_CHANGED_THEIRS_SAME;
//        }
        else if (isFileExistsInOurs && isFileExistsInTheirs && isFileExistsInAncestor &&
                isOursEqualsTheirs && !isTheirsEqualsAncestor && !isAncestorEqualsOurs) {
            mergeSituation = eMergeSituation.CHANGED_TO_SAME_IN_BOTH;
        }
//        else if (isFileExistsInOurs && isFileExistsInTheirs && isFileExistsInAncestor &&
//                isOursEqualsTheirs && isTheirsEqualsAncestor && isAncestorEqualsOurs) {
//            mergeSituation = eMergeSituation.SAME_FILE_IN_ALL;
//        }
        else if (isFileExistsInOurs && isFileExistsInTheirs && isFileExistsInAncestor &&
                !isOursEqualsTheirs && !isTheirsEqualsAncestor && isAncestorEqualsOurs) {
            mergeSituation = eMergeSituation.OURS_SAME_THEIR_CHANGED;
        }
        else {
            mergeSituation = eMergeSituation.KEEP_STATE;
        }

        return mergeSituation;
    }

    public void CreateNewFileOnSystem(IRepositoryFile i_File, String i_FullPath) {
        if(i_File instanceof Folder) {
            f_Factory.CreateFolder((Folder) i_File, i_FullPath);
        }
        else {
            FileUtilities.WriteToFile(i_FullPath, ((Blob) i_File).GetText());
        }
    }

    private boolean IsRepoAlreadyExists(String i_Location) {
        File file = new File(i_Location + "//.magit");

        return file.exists();
    }

    private boolean IsRepoFolderAlreadyExists(String i_Location) {
        File file = new File(i_Location);

        return file.exists();
    }

    @Override
    public void LoadRepositoryFromXml(String i_XmlPath, StringProperty i_ProgressProperty) throws FileNotFoundException, RepositoryAlreadyExistsException, XmlErrorsException, FolderInLocationAlreadyExistsException {
        remoteRepositoryClonedProperty.set(false);
        this.Clear();
        Path xmlPath;

        i_ProgressProperty.set("Reading xml file...");
        sleep();

        try {
            xmlPath = Paths.get(i_XmlPath);
        } catch (InvalidPathException ipe) {
            i_ProgressProperty.set("Input is not a path.");
            sleep();
            throw new XmlErrorsException("Input is not a path.");
        }

        i_ProgressProperty.set("Checking xml for errors...");

        XmlHelper xmlChecker = new XmlHelper(xmlPath);
        List<String> errors = xmlChecker.RunCheckOnXmlFile();

        if(errors.size() == 0) {
            i_ProgressProperty.set("Xml is valid.");
            sleep();
            MagitRepository magitRepository = xmlChecker.GetMagitRepository();

            i_ProgressProperty.set("Checking if repository already exists...");
            sleep();
            boolean isRepoAlreadyExits = (m_ActiveRepository != null &&
                    m_ActiveRepository.GetLocationPath().equals(magitRepository.getLocation())) ||
                    IsRepoAlreadyExists(magitRepository.getLocation());

            if (isRepoAlreadyExits) {
                i_ProgressProperty.set("Repository is already exists.");
                sleep();
                m_ActiveRepositoryPath = Paths.get(magitRepository.getLocation()).toString().toLowerCase();
                throw new RepositoryAlreadyExistsException();
            }
            else if(IsRepoFolderAlreadyExists(magitRepository.getLocation())) {
                i_ProgressProperty.set("Folder in the given location is already exists.");
                sleep();
                throw new FolderInLocationAlreadyExistsException();
            }
            else {
                loadedProperty.set(false);
                i_ProgressProperty.set("Loading xml file...");
                sleep();
                m_ActiveRepository = f_Factory.CreateRepository(magitRepository);
                i_ProgressProperty.set("Load was executed successfully.");
                sleep();
                loadedProperty.set(true);
            }
        }
        else {
            i_ProgressProperty.set("Errors found.");
            throw new XmlErrorsException(errors);
        }
    }

    private void sleep() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException ignored) {}
    }

    @Override
    public void LoadDataFromRepository(String i_RepositoryFullPath) throws IOException {
        this.Clear();
        String repoDetails = FileUtilities.ReadTextFromFile(Paths.get(i_RepositoryFullPath, ".magit", "details.txt").toString());
        List<String> repoDetailsList = StringUtilities.GetLines(repoDetails);

        m_RemoteRepositoryLocation = repoDetailsList.size() == 2 ? repoDetailsList.get(1) : "";
        m_ActiveRepository = new Repository();
        m_ActiveRepository.SetName(repoDetailsList.get(0));
        m_ActiveRepository.SetBranches(new HashMap<>());
        m_ActiveRepository.SetCommits(new HashMap<>());
        m_ActiveRepository.SetFolders(new HashMap<>());
        m_ActiveRepository.SetBlobs(new HashMap<>());
        m_ActiveRepository.SetLocationPath(Paths.get(i_RepositoryFullPath).toString());
        m_ActiveRepositoryPath = m_ActiveRepository.GetLocationPath();

        File branchesDirectory = new File(Paths.get(i_RepositoryFullPath, ".magit", "branches").toString());
        File[] branches = branchesDirectory.listFiles();

        if(branches != null) {
            List<String> localBranches = Arrays.stream(branches)
                    .map(f -> f.toPath().toString())
                    .filter(s -> !s.endsWith("head.txt") && s.contains(".txt"))
                    .collect(Collectors.toList());

            AtomicReference<String> remoteRepoName = new AtomicReference<>();
            List<String> remoteBranches = getRemoteDirectoryContentPaths(branches, remoteRepoName);

            for (String branchPath : localBranches) {
                loadBranch(branchPath);
            }

            if(remoteBranches != null) {
                for (String remoteBranchPath : remoteBranches) {
                    loadRemoteBranch(remoteBranchPath, remoteRepoName.get());
                }

                remoteRepositoryClonedProperty.set(true);
            }

            File headFile = new File(Paths.get(i_RepositoryFullPath, ".magit", "branches", "head.txt").toString());

            try (Scanner scanner = new Scanner(headFile)) {
                String headBranchName = scanner.nextLine();
                Branch headBranch = m_ActiveRepository.GetBranches().get(headBranchName);
                headBranch.SetIsHead(true);
                m_ActiveRepository.SetHeadBranch(headBranch);
            }
        }
    }

    private void loadRemoteBranch(String i_RemoteBranchPath, String i_RemoteRepoName) throws IOException {
        File branchFile = new File(i_RemoteBranchPath);
        Branch branch = Branch.Parse(branchFile);

        if(m_ActiveRepository.GetBranches().containsKey(branch.GetName())) {
            m_ActiveRepository.GetBranches().get(branch.GetName()).SetIsTracking(true);
            m_ActiveRepository.GetBranches().get(branch.GetName()).SetTrakingAfter(i_RemoteRepoName + "/" + branch.GetName());
        }

        branch.SetName(i_RemoteRepoName + "/" + branch.GetName());
        branch.SetIsRemote(true);
        m_ActiveRepository.GetBranches().put(branch.GetName(), branch);

        if(branch.GetPointedCommitSha1() != null && !branch.GetPointedCommitSha1().isEmpty()) {
            loadCommitFile(branch.GetPointedCommitSha1());
        }
    }

    private List<String> getRemoteDirectoryContentPaths(File[] i_BranchesDirectoryFiles, AtomicReference<String> i_RemoteRepositoryName) {
        List<String> result = null;
        File[] remoteBranches = null;

        for(File file: i_BranchesDirectoryFiles) {
            if(file.isDirectory()) {
                remoteBranches = file.listFiles();
                i_RemoteRepositoryName.set(file.getName());
                break;
            }
        }

        if(remoteBranches != null) {
            result = Arrays.stream(remoteBranches)
                    .map(f -> f.toPath().toString())
                    .filter(s -> !s.endsWith("head.txt"))
                    .collect(Collectors.toList());
        }

        return result;
    }

    @Override
    public void ChangeActiveRepository(String i_RepositoryFullPath) throws NotRepositoryFolderException, InvalidPathException, IOException {
        File repo = new File(Paths.get(i_RepositoryFullPath, ".magit").toString());

        if(repo.exists()) {
            loadedProperty.set(false);
            remoteRepositoryClonedProperty.set(false);
            LoadDataFromRepository(i_RepositoryFullPath);
            loadedProperty.set(true);
        }
        else {
            throw new NotRepositoryFolderException(String.format("The folder named \"%s\" is not a magit repository.", i_RepositoryFullPath));
        }
    }

    @Override
    public boolean Commit(String i_Description, Branch i_SecondPrecedingIfMerge) throws IOException, EmptyWcException, CommitAlreadyExistsException {
        boolean isCommitExecuted = false;

        AtomicReference<String> lastChangerRef = new AtomicReference<>();
        Map<String, String> pathToSha1Map = f_Factory.CreatePathToSha1Map(m_ActiveRepository.GetHeadBranch());
        Folder folder = walkInFolder(m_ActiveRepositoryPath, pathToSha1Map, lastChangerRef, false);

        if(folder != null) {
            String folderPath = m_RemoteRepositoryLocation.isEmpty() ? m_ActiveRepositoryPath : m_RemoteRepositoryLocation;
            String folderSha1 = DigestUtils.sha1Hex(folder.toStringForSha1(Paths.get(folderPath)));
            boolean isFolderAlreadyExists = m_ActiveRepository.GetFolders().containsKey(folderSha1);

            if(!isFolderAlreadyExists || i_SecondPrecedingIfMerge != null) {
                if (!isFolderAlreadyExists) {
                    m_ActiveRepository.GetFolders().put(folderSha1, folder);
                }

                isCommitExecuted = true;
                Commit commit    = new Commit();
                commit.SetMessage(i_Description);
                commit.SetRootFolderSHA1(folderSha1);
                commit.SetFirstPrecedingCommitSha1(m_ActiveRepository.GetHeadBranch().GetPointedCommitSha1());
                commit.SetLastChanger(currentNameProperty.get());
                commit.SetLastUpdate(new SimpleDateFormat(DATE_FORMAT).format(new Date(System.currentTimeMillis())));

                if(i_SecondPrecedingIfMerge != null) {
                    commit.SetSecondPrecedingCommitSha1(i_SecondPrecedingIfMerge.GetPointedCommitSha1());
                    new File(Paths.get(m_ActiveRepositoryPath,
                            ".magit", "branches", i_SecondPrecedingIfMerge.GetName() + ".txt").toString()).delete();
                    m_ActiveRepository.GetBranches().remove(i_SecondPrecedingIfMerge.GetName());
                }

                String commitSha1 = DigestUtils.sha1Hex(commit.toStringForSha1());

                if (!m_ActiveRepository.GetCommits().containsKey(commitSha1)) {
                    String pointedBranchInHeadPath = Paths.get(m_ActiveRepositoryPath,
                            ".magit", "branches", m_ActiveRepository.GetHeadBranch().GetName() + ".txt").toString();
                    m_ActiveRepository.GetCommits().put(commitSha1, commit);
                    m_ActiveRepository.GetHeadBranch().SetPointedCommitSha1(commitSha1);


                    FileUtilities.WriteToFile(pointedBranchInHeadPath, commitSha1);
                    FileUtilities.ZipFile(folderSha1, folder.toString(), Paths.get(m_ActiveRepositoryPath,
                            ".magit", "objects", folderSha1).toString());
                    FileUtilities.ZipFile(commitSha1, commit.toString(), Paths.get(m_ActiveRepositoryPath,
                            ".magit", "objects", commitSha1).toString());

                    repositoryChangedProperty.set(repositoryChangedProperty.not().get());
                } else {
                    throw new CommitAlreadyExistsException(commitSha1);
                }
            }
        } else {
            throw new EmptyWcException();
        }

        return isCommitExecuted;
    }

    private Folder walkInFolder(String i_ParentPath, Map<String, String> i_PathToSha1Map, AtomicReference<String> ref_LastChanger, boolean i_IsNewItems) throws IOException {
        Folder folder = null;

        // convert path to file and gets the content of the folder
        File parentFolderFile              = new File(i_ParentPath);
        File[] filesInParentFolder         = parentFolderFile.listFiles();
        List<File> filesInParentFolderList = null;

        if(filesInParentFolder != null) {
            filesInParentFolderList = Arrays.stream(filesInParentFolder)
                    .filter(f -> !f.getName().contains(".magit"))
                    .collect(Collectors.toList());
        }

        if (filesInParentFolderList != null && filesInParentFolderList.size() != 0) {
            long maxLastModified = 0;

            folder = new Folder();

            for (File file : filesInParentFolderList) {
                String returnedFileSha1FromPathToSha1Map = i_PathToSha1Map.get(file.getAbsolutePath());
                boolean isNewItem = returnedFileSha1FromPathToSha1Map == null;
                boolean isFolder = file.isDirectory();
                String sha1;

                if(isFolder) {
                    sha1 = checkDelta(file, i_PathToSha1Map, ref_LastChanger, isNewItem);
                } else {
                    sha1 = checkDelta(file, i_PathToSha1Map, ref_LastChanger, i_IsNewItems);

                    if(isNewItem) {
                        ref_LastChanger.set(currentNameProperty.get());
                        file.setLastModified(System.currentTimeMillis());
                    } else {
                        String parentFolderSha1 = i_PathToSha1Map.get(i_ParentPath);
                        Folder parentFolder = m_ActiveRepository.GetFolders().get(parentFolderSha1);
                        Folder.Data oldItemData = getDataBySha1(parentFolder, returnedFileSha1FromPathToSha1Map);
                        boolean isChanged = !sha1.equals(oldItemData.GetSHA1());

                        if(isChanged) {
                            ref_LastChanger.set(currentNameProperty.get());
                            file.setLastModified(System.currentTimeMillis());
                        } else {
                            ref_LastChanger.set(oldItemData.GetLastChanger());
                        }
                    }
                }

                if (sha1 != null) {
                    Folder.Data itemDataToAdd = Folder.Data.Parse(file, sha1);
                    itemDataToAdd.SetLastChanger(ref_LastChanger.get());

                    if (file.lastModified() > maxLastModified) {
                        maxLastModified = file.lastModified();
                    }

                    folder.AddFile(itemDataToAdd);
                }
            }

            folder.SetIsRoot(i_ParentPath.equals(m_ActiveRepositoryPath));
            folder.GetFiles().sort(Folder.Data::compare);
            parentFolderFile.setLastModified(maxLastModified);
        }

        return folder;
    }

    private String checkDelta(File i_FileToCheckDelta, Map<String, String> i_PathToSha1Map, AtomicReference<String> ref_LastChanger, boolean i_IsNewItems) throws IOException {
        String sha1;
        String filePath = i_FileToCheckDelta.toPath().toString();

        if(!i_FileToCheckDelta.isDirectory()) {
            String blobContent = FileUtilities.ReadTextFromFile(filePath);
            sha1 = DigestUtils.sha1Hex(Blob.GetSha1FromContent(blobContent));

            if(!m_ActiveRepository.GetBlobs().containsKey(sha1)) {
                Blob blob = new Blob();
                blob.SetText(blobContent);
                m_ActiveRepository.GetBlobs().put(sha1, blob);
                FileUtilities.ZipFile(sha1, blobContent, Paths.get(m_ActiveRepositoryPath,".magit", "objects", sha1).toString());
            }
        }
        else {
            Folder newFolder = walkInFolder(filePath, i_PathToSha1Map, ref_LastChanger, i_IsNewItems);

            if(newFolder != null) {
                String currentPath = i_FileToCheckDelta.toPath().toString();

                if(!m_RemoteRepositoryLocation.isEmpty()) {
                    currentPath = ReplaceRootPath(i_FileToCheckDelta.toPath().toString(), m_RemoteRepositoryLocation, 2);
                }

                sha1 = DigestUtils.sha1Hex(newFolder.toStringForSha1(Paths.get(currentPath)));

                if (!m_ActiveRepository.GetFolders().containsKey(sha1)) {
                    m_ActiveRepository.GetFolders().put(sha1, newFolder);
                    FileUtilities.ZipFile(sha1, newFolder.toString(), Paths.get(m_ActiveRepositoryPath, ".magit", "objects", sha1).toString());
                }
            }
            else {
                sha1 = null;
            }
        }

        return sha1;
    }

    public String ReplaceRootPath(String i_OriginalPath, String i_RootPath, int i_FromIndex) {
        String[] pathParts = i_OriginalPath.split(Pattern.quote("\\"));
        StringBuilder sb = new StringBuilder(i_RootPath);

        for(int i = i_FromIndex; i < pathParts.length; i++) {
            sb.append("\\");
            sb.append(pathParts[i]);
        }

        return sb.toString();
    }

    private Folder.Data getDataByName(Folder i_ParentFolder, eFileType i_FileType, String i_Name) {
        Folder.Data toReturn = null;

        if(i_ParentFolder != null) {
            List<Folder.Data> folderData = i_ParentFolder.GetFiles();

            for (Folder.Data data : folderData) {
                if (data.GetName().equals(i_Name) && data.GetFileType().equals(i_FileType)) {
                    toReturn = data;
                    break;
                }
            }
        }
        return toReturn;
    }

    private Folder.Data getDataBySha1(Folder i_ParentFolder, String i_Sha1) {
        Folder.Data toReturn = null;

        if(i_ParentFolder != null && i_Sha1 != null) {
            List<Folder.Data> folderData = i_ParentFolder.GetFiles();

            for (Folder.Data data : folderData) {
                if (data.GetSHA1().equals(i_Sha1)) {
                    toReturn = data;
                    break;
                }
            }
        }

        return toReturn;
    }

    @Override
    public List<String> ShowCurrentCommitFiles() {
        List<String> commitFilesInfo = new ArrayList<>();

        String pointedCommitSha1 = m_ActiveRepository.GetHeadBranch().GetPointedCommitSha1();

        if(pointedCommitSha1 != null && !pointedCommitSha1.isEmpty()) {
            Commit pointedCommit = m_ActiveRepository.GetCommits().get(pointedCommitSha1);
            String rootFolderSha1 = pointedCommit.GetRootFolderSHA1();
            Folder rootFolder = m_ActiveRepository.GetFolders().get(rootFolderSha1);

            List<Folder.Data> folderItems = rootFolder.GetFiles();

            for (Folder.Data item : folderItems) {
                commitFilesInfo.addAll(folderDataToString(item, m_ActiveRepositoryPath));
            }
        }

        return commitFilesInfo;
    }

    public List<List<List<String>>> GetCommitDiff(String i_CommitSha1) {
        List<List<List<String>>> wcStatus = null;
        Commit commit = m_ActiveRepository.GetCommits().get(i_CommitSha1);

        if(!commit.getFirstPrecedingSha1().isEmpty()) {
            wcStatus = new ArrayList<>();
            List<List<String>> diffFirstPreceding = new ArrayList<>();
            wcStatus.add(diffFirstPreceding);

            findDiffBetweenCommits(i_CommitSha1, commit.getFirstPrecedingSha1(), diffFirstPreceding);

            if(!commit.getSecondPrecedingSha1().isEmpty()) {
                List<List<String>> diffSecondPreceding = new ArrayList<>();
                wcStatus.add(diffSecondPreceding);

                findDiffBetweenCommits(i_CommitSha1, commit.getSecondPrecedingSha1(), diffSecondPreceding);
            }
        }

        return wcStatus;
    }

    private void findDiffBetweenCommits(String i_CurrentCommitSha1, String i_PrecedingSha1, List<List<String>> i_Diff) {
        Commit currentCommit = m_ActiveRepository.GetCommits().get(i_CurrentCommitSha1);
        Map<String, String> currentCommitPathToSha1 = f_Factory.CreatePathToSha1MapFromCommit(currentCommit);
        Commit firstPreceding = m_ActiveRepository.GetCommits().get(i_PrecedingSha1);
        Map<String, String> precedingCommitPathToSha1 = f_Factory.CreatePathToSha1MapFromCommit(firstPreceding);

        List<String> deletedItems = new ArrayList<>();
        List<String> newItems = new ArrayList<>();
        List<String> changedItems = new ArrayList<>();

        i_Diff.add(deletedItems);
        i_Diff.add(newItems);
        i_Diff.add(changedItems);

        getNewDiff(newItems, currentCommitPathToSha1, precedingCommitPathToSha1);
        getChangedDiff(changedItems, currentCommitPathToSha1, precedingCommitPathToSha1);
        getDeletedDiff(deletedItems, currentCommitPathToSha1, precedingCommitPathToSha1);
    }

    private void getDeletedDiff(List<String> i_DeletedItems, Map<String, String> i_CurrentCommitPathToSha1, Map<String, String> i_PrecedingCommitPathToSha1) {
        for(Map.Entry<String, String> mapEntry: i_PrecedingCommitPathToSha1.entrySet()) {
            if(!i_CurrentCommitPathToSha1.containsKey(mapEntry.getKey()) && !mapEntry.getKey().equals(m_ActiveRepositoryPath)) {
                i_DeletedItems.add(String.format("%s;%s", mapEntry.getKey(), mapEntry.getValue()));
            }
        }
    }

    private void getChangedDiff(List<String> i_ChangedItems, Map<String, String> i_CurrentCommitPathToSha1, Map<String, String> i_PrecedingCommitPathToSha1) {
        for(Map.Entry<String, String> mapEntry: i_PrecedingCommitPathToSha1.entrySet()) {
            if(i_CurrentCommitPathToSha1.containsKey(mapEntry.getKey()) && !mapEntry.getKey().equals(m_ActiveRepositoryPath)) {
                String precedingItemSha1 = mapEntry.getValue();
                String currentCommitItemSha1 = i_CurrentCommitPathToSha1.get(mapEntry.getKey());
                boolean isItemsDiff = !precedingItemSha1.equals(currentCommitItemSha1);

                if(isItemsDiff) {
                    i_ChangedItems.add(String.format("%s;%s", mapEntry.getKey(), mapEntry.getValue()));
                }
            }
        }
    }

    private void getNewDiff(List<String> i_NewItems, Map<String, String> i_CurrentCommitPathToSha1, Map<String, String> i_PrecedingCommitPathToSha1) {
        for(Map.Entry<String, String> mapEntry: i_CurrentCommitPathToSha1.entrySet()) {
            if(!i_PrecedingCommitPathToSha1.containsKey(mapEntry.getKey()) && !mapEntry.getKey().equals(m_ActiveRepositoryPath)) {
                i_NewItems.add(String.format("%s;%s", mapEntry.getKey(), mapEntry.getValue()));
            }
        }
    }

    @Override
    public List<List<String>> GetWorkingCopyDelta() {
        String pointedCommitSha1 = m_ActiveRepository.GetHeadBranch().GetPointedCommitSha1();
        List<List<String>> wcStatus = new ArrayList<>();

        Map<String, String> pathToSha1Map = f_Factory.CreatePathToSha1Map(m_ActiveRepository.GetHeadBranch());
        List<String> deletedItems = new ArrayList<>();
        List<String> newItems = new ArrayList<>();
        List<String> changedItems = new ArrayList<>();

        wcStatus.add(deletedItems);
        wcStatus.add(newItems);
        wcStatus.add(changedItems);

        if(!pointedCommitSha1.isEmpty()) {
            try {
                getNewItems(pathToSha1Map, m_ActiveRepositoryPath, newItems);
                Commit currentCommit = m_ActiveRepository.GetCommits().get(pointedCommitSha1);
                String rootFolderSha1 = currentCommit.GetRootFolderSHA1();

                if (rootFolderSha1 != null) {
                    Folder currentRootFolder = m_ActiveRepository.GetFolders().get(rootFolderSha1);
                    getDeletedItems(currentRootFolder, m_ActiveRepositoryPath, deletedItems);
                }

                getChangedItems(pathToSha1Map, m_ActiveRepositoryPath, changedItems);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return wcStatus;
    }

    private void getDeletedItems(IRepositoryFile i_CurrentFile, String i_CurrentPath, List<String> i_DeletedItems) {
        if(i_CurrentFile instanceof Folder) {
            List<Folder.Data> itemsInCurrentFolder = ((Folder)i_CurrentFile).GetFiles();

            for(Folder.Data item: itemsInCurrentFolder) {
                String path = Paths.get(i_CurrentPath, item.GetName()).toString();
                File file = new File(path);

                if(item.GetFileType().equals(eFileType.FOLDER)){
                    Folder subFolder = m_ActiveRepository.GetFolders().get(item.GetSHA1());

                    if(file.exists()) {
                        getDeletedItems(subFolder, path, i_DeletedItems);
                    }
                    else {
                        i_DeletedItems.addAll(folderDataToStringList(subFolder.GetFiles(), path));
                        i_DeletedItems.add(itemDataToString(item, i_CurrentPath));
                    }
                }
                else {
                    if(!file.exists()){
                        i_DeletedItems.add(itemDataToString(item, i_CurrentPath));
                    }
                }
            }
        }
    }

    private void getChangedItems(Map<String, String> i_PathToSha1Map, String i_CurrentPath, List<String> i_ChangedItems) throws IOException {
        File folder = new File(i_CurrentPath);
        File[] filesInFolder = folder.listFiles();

        if(filesInFolder != null) {
            List<File> filesInFolderList = Arrays.stream(filesInFolder).filter(f -> !f.getName().contains(".magit")).collect(Collectors.toList());
            String sha1;

            for (File file : filesInFolderList) {
                String path = file.toPath().toString();

                if (i_PathToSha1Map.containsKey(path)) {
                    if (file.isDirectory()) {
                        sha1 = f_Factory.CreateFolder(path, null);

                        if (!m_ActiveRepository.GetFolders().containsKey(sha1)) {

                            Folder.Data folderData = Folder.Data.Parse(file, sha1);
                            i_ChangedItems.add(itemDataToString(folderData, i_CurrentPath));
                            getChangedItems(i_PathToSha1Map, path, i_ChangedItems);
                        }
                    } else {
                        sha1 = f_Factory.CreateBlob(path);

                        if (!m_ActiveRepository.GetBlobs().containsKey(sha1)) {
                            Folder.Data folderData = Folder.Data.Parse(file, sha1);
                            i_ChangedItems.add(itemDataToString(folderData, i_CurrentPath));
                        }
                    }
                }
            }
        }
    }

    private void getNewItems(Map<String, String> i_PathToSha1Map, String i_CurrentPath, List<String> i_NewItems) throws IOException {
        File folder = new File(i_CurrentPath);
        File[] filesInFolder = folder.listFiles();

        if(filesInFolder != null) {
            List<File> filesInFolderList = Arrays.stream(filesInFolder)
                    .filter(f -> !f.getName().contains(".magit"))
                    .collect(Collectors.toList());
            String sha1;

            for (File file : filesInFolderList) {
                String path = file.toPath().toString();
                if (!i_PathToSha1Map.containsKey(path)) {
                    if (file.isDirectory()) {
                        sha1 = f_Factory.CreateFolder(path, new SimpleDateFormat(DATE_FORMAT).format(new Date(file.lastModified())));
                        List<String> newItems = folderDataToStringList(f_Factory.GetTmpFolders().get(sha1).GetFiles(), path);
                        i_NewItems.addAll(newItems);

                        // empty folder is not considered new item
                        if(newItems.size() != 0) {
                            Folder.Data folderData = Folder.Data.Parse(file, sha1);
                            String folderDataString = itemDataToString(folderData, i_CurrentPath);
                            i_NewItems.add(folderDataString);
                        }
                    } else {
                        sha1 = f_Factory.CreateBlob(path);
                        Folder.Data blobData = Folder.Data.Parse(file, sha1);
                        i_NewItems.add(itemDataToString(blobData, i_CurrentPath));
                    }
                } else {
                    if (file.isDirectory()) {
                        getNewItems(i_PathToSha1Map, path, i_NewItems);
                    }
                }
            }
        }
    }

    private List<String> folderDataToStringList(List<Folder.Data> i_FolderItemsData, String i_CurrentPath) {
        List<String> result = new ArrayList<>();

        for(Folder.Data item: i_FolderItemsData) {
            result.addAll(folderDataToString(item, i_CurrentPath));
        }

        return result;
    }

    private List<String> folderDataToString(Folder.Data i_ItemData, String i_CurrentPath) {
        List<String> result = new ArrayList<>();

        if(i_ItemData.GetFileType().equals(eFileType.FOLDER)) {
            Folder folder = m_ActiveRepository.GetFolders().get(i_ItemData.GetSHA1());

            if(folder == null) {
                folder = f_Factory.GetTmpFolders().get(i_ItemData.GetSHA1());
            }

            if(folder != null) {
                List<Folder.Data> folderData = folder.GetFiles();
                result.add(itemDataToString(i_ItemData, i_CurrentPath));

                for (Folder.Data item : folderData) {
                    String itemPath = Paths.get(i_CurrentPath, i_ItemData.GetName()).toString();

                    if (item.GetFileType().equals(eFileType.FOLDER)) {
                        result.addAll(folderDataToString(item, itemPath));
                    } else {
                        result.add(itemDataToString(item, itemPath));
                    }
                }
            }
        }
        else {
            result.add(itemDataToString(i_ItemData, i_CurrentPath));
        }

        return result;
    }

    private String itemDataToString(Folder.Data i_Data, String i_CurrentPath) {
        StringBuilder sb = new StringBuilder();
        String[] parts = i_Data.toString().split(";");
        parts[0] = Paths.get(i_CurrentPath, parts[0]).toString();

        for (String part : parts) {
            sb.append(part);
            sb.append(";");
        }

        return sb.toString();
    }

    @Override
    public List<String> ShowAllBranches() {
        List<String> branchesInfo = new ArrayList<>();
        Map<String, Branch> branchesMap = m_ActiveRepository.GetBranches();

        for(Map.Entry<String, Branch> branch: branchesMap.entrySet()) {
            StringBuilder sb = new StringBuilder();

            String pointedCommitSha1 = branch.getValue().GetPointedCommitSha1();
            boolean isPointedCommitSha1Init = pointedCommitSha1 != null && !pointedCommitSha1.isEmpty();
            String commitMessage = isPointedCommitSha1Init ? m_ActiveRepository.GetCommits().get(branch.getValue().GetPointedCommitSha1()).GetMessage() : "-";

            sb.append(branch.getValue().GetName());
            sb.append(";");
            sb.append(isPointedCommitSha1Init ? pointedCommitSha1 : "-");
            sb.append(";");
            sb.append(commitMessage);

            if(branch.getValue().IsHead()) {
                sb.append(";");
                sb.append("Head branch");
            }

            branchesInfo.add(sb.toString());
        }

        return branchesInfo;
    }

    public boolean IsBranchNameExists(String i_BranchName) {
        return m_ActiveRepository.GetBranches().containsKey(i_BranchName);
    }

    @Override
    public void CreateNewBranch(String i_BranchName) throws PointedCommitEmptyException {
        Branch newBranch = new Branch();
        newBranch.SetName(i_BranchName);
        String headBranchPointedCommitSha1 = m_ActiveRepository.GetHeadBranch().GetPointedCommitSha1();

        if(headBranchPointedCommitSha1 != null) {
            newBranch.SetPointedCommitSha1(headBranchPointedCommitSha1);
        }
        else {
            throw new PointedCommitEmptyException();
        }

        String branchPath = Paths.get(m_ActiveRepositoryPath, ".magit", "branches", newBranch.GetName() + ".txt").toString();
        FileUtilities.WriteToFile(branchPath, headBranchPointedCommitSha1);
        m_ActiveRepository.GetBranches().put(i_BranchName, newBranch);

        repositoryChangedProperty.set(repositoryChangedProperty.not().get());
    }

    @Override
    public void DeleteBranch(String i_BranchName) throws IOException {
        Files.delete(Paths.get(m_ActiveRepositoryPath, ".magit", "branches", i_BranchName + ".txt"));
        m_ActiveRepository.GetBranches().remove(i_BranchName);
        repositoryChangedProperty.set(repositoryChangedProperty.not().get());
    }

    public void CreateRTB(String i_BranchName) {
        String rtbName = i_BranchName.split("/")[1];
        String pointedCommit = m_ActiveRepository.GetBranches().get(i_BranchName).GetPointedCommitSha1();

        String rtbLocation = Paths.get(m_ActiveRepositoryPath, ".magit", "branches", rtbName + ".txt").toString();
        FileUtilities.WriteToFile(rtbLocation, pointedCommit);
        Branch rtbBranch = new Branch();
        rtbBranch.SetPointedCommitSha1(pointedCommit);
        rtbBranch.SetTrakingAfter(i_BranchName);
        rtbBranch.SetIsTracking(true);
        rtbBranch.SetName(rtbName);
        m_ActiveRepository.GetBranches().put(rtbName, rtbBranch);
        repositoryChangedProperty.set(repositoryChangedProperty.not().get());
    }

    @Override
    public void Checkout(String i_BranchName, boolean i_IsSkipWcCheck) throws Exception {
        if(!i_BranchName.contains("/")) {
            List<List<String>> wcStatus = null;
            List<String> deletedFiles = null;
            List<String> newFiles = null;
            List<String> changedFiles = null;

            if (!i_IsSkipWcCheck) {
                wcStatus = GetWorkingCopyDelta();
                deletedFiles = wcStatus.get(0);
                newFiles = wcStatus.get(1);
                changedFiles = wcStatus.get(2);
            }

            if (i_IsSkipWcCheck || deletedFiles.size() == 0 && newFiles.size() == 0 && changedFiles.size() == 0) {
                Branch branch = m_ActiveRepository.GetBranches().get(i_BranchName);
                m_ActiveRepository.GetHeadBranch().SetIsHead(false);
                branch.SetIsHead(true);
                m_ActiveRepository.SetHeadBranch(branch);

                cleanWc();
                f_Factory.CreateWc(branch);

                repositoryChangedProperty.set(repositoryChangedProperty.not().get());
            } else {
                throw new OpenChangesInWcException(wcStatus);
            }
        } else {
            throw new Exception("Checkout is illigal on remote branches.");
        }
    }

    public void CreateFolderFromCommit(Commit i_Commit, String i_Location) {
        f_Factory.CreateFolderFromCommit(i_Commit, i_Location);
    }

    private void cleanWc() throws IOException {
        File rootFolder = new File(m_ActiveRepositoryPath);
        File[] filesInRootFolder = rootFolder.listFiles();

        if(filesInRootFolder != null) {
            List<File> filesInRootFolderList = Arrays.stream(filesInRootFolder).filter(f -> !f.getName().contains(".magit")).collect(Collectors.toList());

            for (File file : filesInRootFolderList) {
                if (file.isDirectory()) {
                    FileUtils.deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        throw new IOException();
                    }
                }
            }
        }
    }

    public void DeleteFile(IRepositoryFile i_File, String i_FullPath, boolean i_IsCreateNew) {
        if(i_File instanceof Folder || new File(i_FullPath).isDirectory()) {
            try {
                FileUtils.deleteDirectory(new File(i_FullPath));
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(i_IsCreateNew) {
                Engine.Creator.GetInstance().CreateNewFileOnSystem(i_File, i_FullPath);
            }
        }
        else {
            new File(i_FullPath).delete();

            if(i_IsCreateNew) {
                FileUtilities.WriteToFile(i_FullPath, ((Blob) i_File).GetText());
            }
        }
    }

    @Override
    public List<String> ShowActiveBranchHistory() {
        List<String> history = new ArrayList<>();
        Branch activeBranch = m_ActiveRepository.GetHeadBranch();
        Commit commit = m_ActiveRepository.GetCommits().get(activeBranch.GetPointedCommitSha1());

        ShowActiveBranchHistoryRec(commit, activeBranch.GetPointedCommitSha1(), history);

        return history;
    }

    private void ShowActiveBranchHistoryRec(Commit i_Commit, String i_Sha1, List<String> i_History) {
        if(i_Commit == null) {
            return;
        }
        else {
            String precedingCommitSha1 = i_Commit.getFirstPrecedingSha1();
            ShowActiveBranchHistoryRec(m_ActiveRepository.GetCommits().get(precedingCommitSha1), precedingCommitSha1, i_History);

            StringBuilder sb = new StringBuilder();
            sb.append(i_Sha1);
            sb.append(";");
            sb.append(i_Commit.GetMessage());
            sb.append(";");
            sb.append(i_Commit.GetLastUpdate());
            sb.append(";");
            sb.append(i_Commit.GetLastChanger());

            i_History.add(sb.toString());
        }
    }

    @Override
    public Repository GetActiveRepository() {
        return m_ActiveRepository;
    }

    @Override
    public String GetRepositoryPath() {
        return m_ActiveRepositoryPath;
    }

    @Override
    public void SetRepositoryPath(String i_RepositoryPath) {
        m_ActiveRepositoryPath = i_RepositoryPath == null ? null : Paths.get(i_RepositoryPath).toString().toLowerCase();
    }

    @Override
    public void SetActiveRepository(Repository i_Repository) {
        m_ActiveRepository = i_Repository;
    }

    @Override
    public void SetCurrentUserName(String i_CurrentUserName) {
        currentNameProperty.set(i_CurrentUserName);
    }

    @Override
    public String GetCurrentUserName() {
        return currentNameProperty.get();
    }

    @Override
    public void CreateRepositoryAndFiles(String i_RepositoryName, String i_RepositoryLocation) throws RepositoryAlreadyExistsException, FolderInLocationAlreadyExistsException {
        this.Clear();
        File repositoryFolder = new File(i_RepositoryLocation);
        File magit            = new File(repositoryFolder, ".magit");

        if(magit.exists()) {
            throw new RepositoryAlreadyExistsException();
        }
        else if(repositoryFolder.exists() && repositoryFolder.listFiles() != null && repositoryFolder.listFiles().length != 0) {
            throw new FolderInLocationAlreadyExistsException();
        }
        else {
            loadedProperty.set(false);
            f_Factory.CreateRepositoryAndFiles(i_RepositoryName, i_RepositoryLocation, true);
            loadedProperty.set(true);
        }
    }

    @Override
    public void ResetHeadBranch(String i_PointedCommitSha1) throws IOException, Sha1LengthException {
        if(i_PointedCommitSha1.length() != 40) {
            throw new Sha1LengthException();
        }

        if(!m_ActiveRepository.GetCommits().containsKey(i_PointedCommitSha1)) {
            loadCommitFile(i_PointedCommitSha1);
        }

        FileUtilities.WriteToFile(Paths.get(m_ActiveRepositoryPath,
                ".magit", "branches", m_ActiveRepository.GetHeadBranch().GetName() + ".txt").toString(), i_PointedCommitSha1);
        m_ActiveRepository.GetHeadBranch().SetPointedCommitSha1(i_PointedCommitSha1);

        cleanWc();
        f_Factory.CreateWc(m_ActiveRepository.GetHeadBranch());

        repositoryChangedProperty.set(repositoryChangedProperty.not().get());
    }

    private void loadBranch(String i_BranchPath) throws IOException {
        File branchFile = new File(i_BranchPath);
        Branch branch = Branch.Parse(branchFile);
        m_ActiveRepository.GetBranches().put(branch.GetName(), branch);

        if(branch.GetPointedCommitSha1() != null && !branch.GetPointedCommitSha1().isEmpty()) {
            loadCommitFile(branch.GetPointedCommitSha1());
        }
    }

    private void loadCommitFile(String i_CommitSha1) throws IOException {
        String objectsFolderPath = Paths.get(this.GetRepositoryPath(), ".magit", "objects").toString();
        Commit commit = Commit.Parse(new File(Paths.get(objectsFolderPath, i_CommitSha1).toString()));

        if(!m_ActiveRepository.GetCommits().containsKey(i_CommitSha1)) {
            m_ActiveRepository.GetCommits().put(i_CommitSha1, commit);

            if (!commit.getFirstPrecedingSha1().isEmpty()) {
                boolean isFirstPrecedingExists = m_ActiveRepository.GetCommits().containsKey(commit.getFirstPrecedingSha1());

                if(!isFirstPrecedingExists) {
                    loadCommitFile(commit.getFirstPrecedingSha1());
                }

                if (!commit.getSecondPrecedingSha1().isEmpty()) {
                    boolean isSecondPrecedingExists = m_ActiveRepository.GetCommits().containsKey(commit.getSecondPrecedingSha1());

                    if(!isSecondPrecedingExists) {
                        loadCommitFile(commit.getSecondPrecedingSha1());
                    }
                }
            }
        }

        if(commit.GetRootFolderSHA1() != null && !commit.GetRootFolderSHA1().isEmpty()) {
            loadFolderFile(commit.GetRootFolderSHA1());
            m_ActiveRepository.GetFolders().get(commit.GetRootFolderSHA1()).SetIsRoot(true);
        }
    }

    private void loadFolderFile(String i_FolderSha1) throws IOException {
        String objectsFolderPath = Paths.get(this.GetRepositoryPath(), ".magit", "objects").toString();
        Folder folder = Folder.Parse(new File(Paths.get(objectsFolderPath, i_FolderSha1).toString()));
        m_ActiveRepository.GetFolders().put(i_FolderSha1, folder);

        for(Folder.Data item: folder.GetFiles()) {
            if(item.GetSHA1() != null && !item.GetSHA1().isEmpty()) {
                if (item.GetFileType().equals(eFileType.FOLDER)) {
                    loadFolderFile(item.GetSHA1());
                } else {
                    loadBlobFile(item.GetSHA1());
                }
            }
        }
    }

    private void loadBlobFile(String i_BlobSHA1) throws IOException {
        String objectsFolderPath = Paths.get(this.GetRepositoryPath(), ".magit", "objects").toString();
        Blob blob = Blob.Parse(new File(Paths.get(objectsFolderPath, i_BlobSHA1).toString()));
        m_ActiveRepository.GetBlobs().put(i_BlobSHA1, blob);
    }

    @Override
    public void ExportRepositoryToXml(String i_XmlPath) throws XmlErrorsException, RepositoryNotLoadedException {
        Path xmlPath;

        try {
            xmlPath = Paths.get(i_XmlPath);
        } catch (InvalidPathException ipe) {
            throw new XmlErrorsException("Input is not a path.");
        }

        XmlHelper xmlHelper = new XmlHelper(xmlPath);

        if(xmlHelper.IsValidXmlPath()) {
            if(m_ActiveRepository != null) {
                MagitRepository magitRepository = new MagitRepository();
                magitRepository.setLocation(m_ActiveRepositoryPath);
                magitRepository.setName(m_ActiveRepository.GetName());

                if (m_ActiveRepository.GetBranches().size() != 0) {
                    createMagitBranches(magitRepository);
                }

                if (m_ActiveRepository.GetCommits().size() != 0) {
                    createMagitCommits(magitRepository);
                }

                xmlHelper.MagitRepositoryToXml(magitRepository);
            }
            else {
                throw new RepositoryNotLoadedException();
            }
        }
        else {
            throw new XmlErrorsException("Xml file should end with \".xml\".");
        }
    }

    private void createMagitBranches(MagitRepository i_MagitRepository) {
        Map<String, Branch> branches = m_ActiveRepository.GetBranches();
        i_MagitRepository.setMagitBranches(new MagitBranches());
        List<MagitSingleBranch> magitBranches = i_MagitRepository.getMagitBranches().getMagitSingleBranch();

        for(Map.Entry<String, Branch> branchEntry: branches.entrySet()) {
            MagitSingleBranch magitBranch = new MagitSingleBranch();
            MagitSingleBranch.PointedCommit pointedCommit = new MagitSingleBranch.PointedCommit();

            pointedCommit.setId(branchEntry.getValue().GetPointedCommitSha1());
            magitBranch.setPointedCommit(pointedCommit);
            magitBranch.setTrackingAfter(branchEntry.getValue().GetTrakingAfter());
            magitBranch.setTracking(branchEntry.getValue().IsTracking());
            magitBranch.setName(branchEntry.getValue().GetName());
            magitBranch.setIsRemote(branchEntry.getValue().IsRemote());

            if(branchEntry.getValue().IsHead()) {
                i_MagitRepository.getMagitBranches().setHead(magitBranch.getName());
            }

            magitBranches.add(magitBranch);
        }
    }

    private void createMagitCommits(MagitRepository i_MagitRepository) {
        Map<String, Commit> commits = m_ActiveRepository.GetCommits();

        i_MagitRepository.setMagitCommits(new MagitCommits());
        i_MagitRepository.setMagitFolders(new MagitFolders());
        i_MagitRepository.setMagitBlobs(new MagitBlobs());
        Set<String> sha1TrackerSet = new HashSet<>();

        List<MagitSingleCommit> magitCommits = i_MagitRepository.getMagitCommits().getMagitSingleCommit();
        Set<String> rootFoldersTracker = new HashSet<>();

        for(Map.Entry<String, Commit> commitEntry: commits.entrySet()) {
            if(commitEntry.getKey().length() == 40) {
                MagitSingleCommit magitCommit = new MagitSingleCommit();
                PrecedingCommits precedingCommits = new PrecedingCommits();
                List<PrecedingCommits.PrecedingCommit> magitPrecedingCommits = precedingCommits.getPrecedingCommit();

                for(String precedingCommitSha1: commitEntry.getValue().GetPrecedingCommits()) {
                    PrecedingCommits.PrecedingCommit magitPrecedingCommit = new PrecedingCommits.PrecedingCommit();
                    magitPrecedingCommit.setId(precedingCommitSha1);
                    magitPrecedingCommits.add(magitPrecedingCommit);
                }

                RootFolder magitRootFolder = new RootFolder();
                magitRootFolder.setId(commitEntry.getValue().GetRootFolderSHA1());
                magitCommit.setRootFolder(magitRootFolder);

                magitCommit.setPrecedingCommits(precedingCommits);
                magitCommit.setMessage(commitEntry.getValue().GetMessage());
                magitCommit.setId(commitEntry.getKey());
                magitCommit.setDateOfCreation(commitEntry.getValue().GetLastUpdate());
                magitCommit.setAuthor(commitEntry.getValue().GetLastChanger());

                magitCommits.add(magitCommit);

                //////////////////////////////////////////////////////////////////////////////////

                if(!rootFoldersTracker.contains(commitEntry.getValue().GetRootFolderSHA1())) {
                    rootFoldersTracker.add(commitEntry.getValue().GetRootFolderSHA1());
                    MagitSingleFolder magitFolder = new MagitSingleFolder();
                    magitFolder.setName(null);
                    magitFolder.setLastUpdater(commitEntry.getValue().GetLastChanger());
                    magitFolder.setLastUpdateDate(commitEntry.getValue().GetLastUpdate());
                    magitFolder.setIsRoot(true);
                    magitFolder.setId(commitEntry.getValue().GetRootFolderSHA1());

                    createMagitFolders(i_MagitRepository, magitFolder, sha1TrackerSet);
                }
            }
        }
    }

    private void createMagitFolders(MagitRepository i_MagitRepository, MagitSingleFolder i_MagitFolder, Set<String> i_Sha1TrackerSet) {
        List<MagitSingleFolder> magitFolders = i_MagitRepository.getMagitFolders().getMagitSingleFolder();
        magitFolders.add(i_MagitFolder);
        i_Sha1TrackerSet.add(i_MagitFolder.getId());

        MagitSingleFolder.Items items = new MagitSingleFolder.Items();
        List<Item> itemsList = items.getItem();
        i_MagitFolder.setItems(items);

        Folder folder = m_ActiveRepository.GetFolders().get(i_MagitFolder.getId());

        for(Folder.Data itemData: folder.GetFiles()) {
            Item item = new Item();
            item.setType(itemData.GetFileType().toString().toLowerCase());
            item.setId(itemData.GetSHA1());
            itemsList.add(item);

            if(!i_Sha1TrackerSet.contains(itemData.GetSHA1())) {
                if (itemData.GetFileType().equals(eFileType.FOLDER)) {
                    MagitSingleFolder magitSubFolder = new MagitSingleFolder();
                    magitSubFolder.setId(itemData.GetSHA1());
                    magitSubFolder.setIsRoot(false);
                    magitSubFolder.setLastUpdateDate(itemData.GetlastUpdate());
                    magitSubFolder.setLastUpdater(itemData.GetLastChanger());
                    magitSubFolder.setName(itemData.GetName());

                    createMagitFolders(i_MagitRepository, magitSubFolder, i_Sha1TrackerSet);
                }
                else {
                    MagitBlob magitBlob = new MagitBlob();
                    magitBlob.setName(itemData.GetName());
                    magitBlob.setLastUpdater(itemData.GetLastChanger());
                    magitBlob.setLastUpdateDate(itemData.GetlastUpdate());
                    magitBlob.setId(itemData.GetSHA1());

                    createMagitBlob(i_MagitRepository, magitBlob, i_Sha1TrackerSet);
                }
            }
        }
    }

    private void createMagitBlob(MagitRepository i_MagitRepository, MagitBlob i_MagitBlob, Set<String> i_Sha1TrackerSet) {
        List<MagitBlob> magitBlobs = i_MagitRepository.getMagitBlobs().getMagitBlob();
        String blobContent = m_ActiveRepository.GetBlobs().get(i_MagitBlob.getId()).GetText();
        i_MagitBlob.setContent(blobContent);
        magitBlobs.add(i_MagitBlob);
        i_Sha1TrackerSet.add(i_MagitBlob.getId());
    }

    public void Clear() {
        m_ActiveRepository = null;
        m_ActiveRepositoryPath = null;
        m_RemoteRepositoryLocation = "";
        currentNameProperty.set("Administrator");
        repositoryChangedProperty.set(repositoryChangedProperty.not().get());
        f_Factory.Clear();
    }

    public CommitNode BuildTree(Map<String, CommitNode> i_TreeNodes, Branch i_BranchToAdd) {
        Commit pointedCommit = m_ActiveRepository.GetCommits().get(i_BranchToAdd.GetPointedCommitSha1());
        return pointedCommit == null ? null : buildTreeRec(pointedCommit, i_BranchToAdd, i_BranchToAdd, i_TreeNodes);
    }

    private CommitNode buildTreeRec(Commit i_CommitToNode, Branch i_PointingBranch, Branch i_OnBranch, Map<String, CommitNode> i_TreeNodes) {
        CommitNode node = null;
        String firstPrecedingSha1 = i_CommitToNode.getFirstPrecedingSha1();
        String secondPrecedingSha1 = i_CommitToNode.getSecondPrecedingSha1();

        if(!firstPrecedingSha1.isEmpty() && !i_TreeNodes.containsKey(i_CommitToNode.getSha1())) {
            Commit firstPreceding = m_ActiveRepository.GetCommits().get(firstPrecedingSha1);
            CommitNode firstPrecedingNode = buildTreeRec(firstPreceding, null, i_OnBranch, i_TreeNodes);
            CommitNode secondPrecedingNode = null;

            if(!secondPrecedingSha1.isEmpty()) {
                Commit secondPreceding = m_ActiveRepository.GetCommits().get(secondPrecedingSha1);
                secondPrecedingNode = buildTreeRec(secondPreceding, null, i_OnBranch, i_TreeNodes);
            }

            node = createCommitNode(i_CommitToNode, firstPrecedingNode, secondPrecedingNode, i_PointingBranch);
            i_TreeNodes.put(node.GetSha1(), node);
            firstPrecedingNode.AddChildren(node);

            if(secondPrecedingNode != null) {
                secondPrecedingNode.AddChildren(node);
            }
        }

        if(node == null && i_TreeNodes.containsKey(i_CommitToNode.getSha1())) {
            node = i_TreeNodes.get(i_CommitToNode.getSha1());

            if(i_PointingBranch != null) {
                node.GetPointingBranches().add(i_PointingBranch);
            }

            setOnBranch(node.GetFirstParent(), i_OnBranch);
            setOnBranch(node.GetSecondParent(), i_OnBranch);
        }
        else if(node == null) {
            node = createCommitNode(i_CommitToNode, null, null, i_PointingBranch);
            i_TreeNodes.put(node.GetSha1(), node);
        }

        node.GetOnBranches().add(i_OnBranch);

        return node;
    }

    private void setOnBranch(CommitNode i_Node, Branch i_OnBranch) {
        if(i_Node == null) {
            return;
        }

        i_Node.GetOnBranches().add(i_OnBranch);
        setOnBranch(i_Node.GetFirstParent(), i_OnBranch);
        setOnBranch(i_Node.GetSecondParent(), i_OnBranch);
    }

    private CommitNode createCommitNode(Commit i_CommitToNode, CommitNode i_FirstPrecedingNode, CommitNode i_SecondPrecedingNode, Branch i_PointingBranch) {
        CommitNode result = new CommitNode(i_FirstPrecedingNode, i_SecondPrecedingNode);
        result.SetCommit(i_CommitToNode);

        if(i_PointingBranch != null) {
            result.GetPointingBranches().add(i_PointingBranch);
        }

        return result;
    }

    public CommitNode FindRoot(CommitNode i_Leaf) {
        if(i_Leaf == null || i_Leaf.GetFirstParent() == null) {
            return i_Leaf;
        }

        return FindRoot(i_Leaf.GetFirstParent());
    }

    public void Clone(String i_LocalRepositoryName, String i_LocalRepositoryFullPath, String i_RemoteRepositoryFullPath) throws IOException, CollaborationException {
        if(!new File(i_LocalRepositoryFullPath).exists()) {
            m_RemoteRepositoryLocation = Paths.get(i_RemoteRepositoryFullPath).toString().toLowerCase();

            File remoteRepoFile = new File(i_RemoteRepositoryFullPath);
            if(remoteRepoFile.exists()) {
                if (Arrays.stream(Objects.requireNonNull(remoteRepoFile.listFiles()))
                        .anyMatch(f -> f.getName().contains("magit"))) {
                    File localRepoFile = new File(i_LocalRepositoryFullPath);
                    File branchesDirectory = new File(Paths.get(i_LocalRepositoryFullPath, ".magit", "branches").toString());

                    FileUtils.copyDirectory(remoteRepoFile, localRepoFile);
                    FileUtilities.WriteToFile(Paths.get(i_LocalRepositoryFullPath, ".magit", "details.txt").toString(), String.format(
                            "%s%s%s", i_LocalRepositoryName, System.lineSeparator(), m_RemoteRepositoryLocation
                    ));

                    File[] branchesFiles = branchesDirectory.listFiles();
                    File remoteBranchesDirectory = new File(Paths.get(branchesDirectory.toPath().toString(),
                            remoteRepoFile.getName()).toString());
                    remoteBranchesDirectory.mkdir();

                    String headBranchName = FileUtilities.ReadTextFromFile(Paths.get(branchesDirectory.toPath().toString(), "head.txt").toString());

                    for (File branchFile : branchesFiles) {
                        if (!branchFile.getName().contains("head") && !branchFile.getName().contains(headBranchName)) {
                            FileUtils.copyFile(branchFile, new File(Paths.get(remoteBranchesDirectory.toPath().toString(), branchFile.getName()).toString()));
                            branchFile.delete();
                        }

                        if (branchFile.getName().contains(headBranchName)) {
                            FileUtils.copyFile(branchFile, new File(Paths.get(remoteBranchesDirectory.toPath().toString(), branchFile.getName()).toString()));
                        }
                    }

                    loadedProperty.set(false);
                    LoadDataFromRepository(i_LocalRepositoryFullPath);
                    loadedProperty.set(true);
                } else {
                    throw new CollaborationException("Remote directory is not a magit repository.");
                }
            } else {
                throw new CollaborationException("Remote directory not exists.");
            }
        } else {
            throw new CollaborationException("Directory with the same name already exists in the given location.");
        }
    }

    public void Fetch() {
        String remoteBranchesDirPath = Paths.get(m_RemoteRepositoryLocation, ".magit", "branches").toString();
        String remoteObjectsDirPath = Paths.get(m_RemoteRepositoryLocation, ".magit", "objects").toString();
        String localRemoteBranchesDirPath = Paths.get(m_ActiveRepositoryPath, ".magit", "branches", new File(m_RemoteRepositoryLocation).getName()).toString();
        String localObjectsDirPath = Paths.get(m_ActiveRepositoryPath, ".magit", "objects").toString();

        try {
            FileUtils.copyDirectory(new File(remoteBranchesDirPath), new File(localRemoteBranchesDirPath));
            FileUtils.copyDirectory(new File(remoteObjectsDirPath), new File(localObjectsDirPath));
            LoadDataFromRepository(m_ActiveRepositoryPath);

            repositoryChangedProperty.set(repositoryChangedProperty.not().get());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Push() throws CollaborationException, IOException {
        if(m_ActiveRepository.GetHeadBranch().IsTracking()) {
            if(isPushNeeded()) {
                String localRepoPath = m_ActiveRepositoryPath;
                String localHeadBranchName = m_ActiveRepository.GetHeadBranch().GetName();

                LoadDataFromRepository(m_RemoteRepositoryLocation);
                boolean isRemoteWcClean = isWcClean();

                if (isRemoteWcClean) {
                    String remotePointedCommitSha1 = m_ActiveRepository.GetBranches().get(localHeadBranchName).GetPointedCommitSha1();
                    LoadDataFromRepository(localRepoPath);
                    Branch trackedBranch = m_ActiveRepository.GetBranches().get(m_ActiveRepository.GetHeadBranch().GetTrakingAfter());
                    String trackedBranchPointedCommitSha1 = trackedBranch.GetPointedCommitSha1();

                    if (remotePointedCommitSha1.equals(trackedBranchPointedCommitSha1)) {
                        String headBranchPointedCommitSha1 = m_ActiveRepository.GetHeadBranch().GetPointedCommitSha1();

                        String remoteBranchPath = Paths.get(m_RemoteRepositoryLocation,
                                ".magit", "branches", localHeadBranchName + ".txt").toString();
                        String trackedBranchPath = Paths.get(m_ActiveRepositoryPath,
                                ".magit", "branches", new File(m_RemoteRepositoryLocation).getName(),
                                localHeadBranchName + ".txt").toString();

                        FileUtilities.WriteToFile(remoteBranchPath, headBranchPointedCommitSha1);
                        FileUtilities.WriteToFile(trackedBranchPath, headBranchPointedCommitSha1);
                        trackedBranch.SetPointedCommitSha1(headBranchPointedCommitSha1);

                        copyCommitsFilesFromBranch(m_ActiveRepository.GetHeadBranch(), m_RemoteRepositoryLocation, m_ActiveRepositoryPath);
                        copyWcToRemote();

                        repositoryChangedProperty.set(repositoryChangedProperty.not().get());

                    } else {
                        throw new CollaborationException("Remote branch is not pointing to the same commit as local branch.");
                    }

                } else {
                    throw new CollaborationException("Remote repository working directory is not clean.");
                }
            }
        } else {
            throw new CollaborationException("Head branch is not a remote tracking branch.");
        }
    }

    private void copyWcToRemote() throws IOException {
        File remoteRepo = new File(m_RemoteRepositoryLocation);
        File localRepo = new File(m_ActiveRepositoryPath);

        File[] filesInRemote = remoteRepo.listFiles();
        File[] filesInLocal = localRepo.listFiles();

        if(filesInRemote != null) {
            List<File> filterdFilesInRemote = Arrays.stream(filesInRemote).filter(f -> !f.getName().contains(".magit")).collect(Collectors.toList());

            for(File file: filesInRemote) {
                file.delete();
            }
        }

        if(filesInLocal != null) {
            List<File> filterdFilesInRemote = Arrays.stream(filesInLocal).filter(f -> !f.getName().contains(".magit")).collect(Collectors.toList());

            for(File file: filterdFilesInRemote) {
                if(file.isDirectory()) {
                    FileUtils.copyDirectory(file, new File(Paths.get(m_RemoteRepositoryLocation, file.getName()).toString()));
                }
                else {
                    FileUtils.copyFile(file, new File(ReplaceRootPath(file.toPath().toString(), m_RemoteRepositoryLocation, 2)));
                }
            }
        }
    }

    private void copyCommitsFilesFromBranch(Branch i_Branch, String i_Dest, String i_Src) throws IOException {
        copyCommitFile(i_Branch.GetPointedCommitSha1(), i_Dest, i_Src);
    }

    private void copyCommitFile(String i_CommitSha1, String i_Dest, String i_Src) throws IOException {
        if(i_CommitSha1.isEmpty()) {
            return;
        }

        String srcContent = copyFile(i_CommitSha1, i_Dest, i_Src);

        String[] details = srcContent.split(";");

        copyFolderFile(details[0], i_Dest, i_Src);
        copyCommitFile(details[1], i_Dest, i_Src);
        copyCommitFile(details[2], i_Dest, i_Src);
    }

    private void copyFolderFile(String i_FolderSha1, String i_Dest, String i_Src) throws IOException {
        String srcContent = copyFile(i_FolderSha1, i_Dest, i_Src);
        List<String> filesData = StringUtilities.GetLines(srcContent);

        for(String data: filesData) {
            String[] dataParts = data.split(";");

            if(dataParts.length >= 3 && dataParts[2].equals("folder")) {
                copyFolderFile(dataParts[1], i_Dest, i_Src);
            }
            else {
                copyFile(dataParts[1], i_Dest, i_Src);
            }
        }
    }

    private String copyFile(String i_Sha1, String i_Dest, String i_Src) throws IOException {
        String srcFilePath = Paths.get(i_Src, ".magit", "objects", i_Sha1).toString();
        String destFilePath = Paths.get(i_Dest, ".magit", "objects", i_Sha1).toString();

        String srcContent = FileUtilities.UnzipFile(srcFilePath);
        FileUtilities.ZipFile(i_Sha1, srcContent, destFilePath);

        return srcContent;
    }

    public void Pull() throws OpenChangesInWcException, CollaborationException, Exception {
        Branch localHeadBranch = m_ActiveRepository.GetHeadBranch();

        if(localHeadBranch.IsTracking()) {
            if(isWcClean()) {
                if(!isPushNeeded()) {
                    String remoteHeadBranchPath = Paths.get(m_RemoteRepositoryLocation, ".magit", "branches", localHeadBranch.GetName() + ".txt").toString();
                    String localHeadBranchPath = Paths.get(m_ActiveRepositoryPath, ".magit", "branches", localHeadBranch.GetName() + ".txt").toString();
                    String trackedBranchByHeadBranch = Paths.get(m_ActiveRepositoryPath, ".magit", "branches", new File(m_RemoteRepositoryLocation).getName(), localHeadBranch.GetName() + ".txt").toString();

                    try {
                        String remotePointedCommit = FileUtilities.ReadTextFromFile(remoteHeadBranchPath);
                        FileUtilities.WriteToFile(localHeadBranchPath, remotePointedCommit);
                        FileUtilities.WriteToFile(trackedBranchByHeadBranch, remotePointedCommit);

                        Branch temp = new Branch();
                        temp.SetPointedCommitSha1(FileUtilities.ReadTextFromFile(remoteHeadBranchPath));

                        this.copyCommitsFilesFromBranch(temp, m_ActiveRepositoryPath, m_RemoteRepositoryLocation);
                        this.LoadDataFromRepository(m_ActiveRepositoryPath);
                        this.Checkout(m_ActiveRepository.GetHeadBranch().GetName(), true);

                        repositoryChangedProperty.set(repositoryChangedProperty.not().get());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    throw new CollaborationException("Push operation is necessary before pull execution.");
                }
            }
            else {
                throw new OpenChangesInWcException(null);
            }
        }
        else {
            throw new CollaborationException("Head branch is not remote tracking branch.");
        }
    }

    private boolean isPushNeeded() {
        String currentCommitSha1 = m_ActiveRepository.GetHeadBranch().GetPointedCommitSha1();
        String commitFilePathInRemote = Paths.get(m_RemoteRepositoryLocation, ".magit", "objects", currentCommitSha1).toString();
        File commitFileInRemote = new File(commitFilePathInRemote);

        return !commitFileInRemote.exists();
    }

    public boolean isWcClean() {
        List<List<String>> wcStatus = GetWorkingCopyDelta();

        List<String> deletedFiles = wcStatus.get(0);
        List<String> newFiles = wcStatus.get(1);
        List<String> changedFiles = wcStatus.get(2);

        return deletedFiles.size() == 0 && newFiles.size() == 0 && changedFiles.size() == 0;
    }

    public String GetRemoteRepositoryLocation() { return m_RemoteRepositoryLocation; }

    public static class Creator {
        private static Engine m_Instance = null;
        private static final Object m_Lock = new Object();

        private Creator() {
        }

        public static Engine GetInstance() {
            if (m_Instance == null) {
                synchronized (m_Lock) {
                    if (m_Instance == null) {
                        try {
                            Constructor[] constructors = Engine.class.getDeclaredConstructors();
                            for (Constructor constructor : constructors) {
                                if (!constructor.isAccessible()) {
                                    constructor.setAccessible(true);
                                    m_Instance = (Engine) constructor.newInstance();
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            return m_Instance;
        }
    }
}
