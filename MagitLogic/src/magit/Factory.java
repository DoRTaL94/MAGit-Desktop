package magit;

import IO.FileUtilities;
import data.structures.*;
import org.apache.commons.codec.digest.DigestUtils;
import resources.jaxb.schema.generated.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Factory {
    private final Map<String, Blob> sf_TmpBlobs = new HashMap<>();
    private final Map<String, Folder> sf_TmpFolders = new HashMap<>();
    private final IEngine f_Engine;

    public Factory(IEngine i_Engine) {
        f_Engine = i_Engine;
    }

    Repository CreateRepository(MagitRepository i_MagitRepository) {
        boolean isEmptyRepo = i_MagitRepository.getMagitCommits().getMagitSingleCommit().size() == 0;
        Repository repository = CreateRepositoryAndFiles(i_MagitRepository.getName(), i_MagitRepository.getLocation(), isEmptyRepo);

        if(!isEmptyRepo) {
            // Set lists of magit data structures.
            List<MagitSingleCommit> magitCommits = i_MagitRepository.getMagitCommits().getMagitSingleCommit();
            List<MagitSingleBranch> magitBranches = i_MagitRepository.getMagitBranches().getMagitSingleBranch();
            List<MagitBlob> magitBlobs = i_MagitRepository.getMagitBlobs().getMagitBlob();
            List<MagitSingleFolder> magitFolders = i_MagitRepository.getMagitFolders().getMagitSingleFolder();

            // Transform magit lists to maps and set maps in repository.
            repository.SetBranches(GenerateBranchesHashMap(magitBranches));
            repository.SetCommits(GenerateCommitsHashMap(magitCommits));
            repository.SetFolders(GenerateFoldersHashMap(magitFolders, GenerateMagitBlobsHashMap(magitBlobs)));
            repository.SetBlobs(GenerateBlobsHashMap(magitBlobs));

            try {
                String headBranchName = i_MagitRepository.getMagitBranches().getHead();
                CreateFilesOnSystem(headBranchName);
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        }

        return repository;
    }

    Repository CreateRepositoryAndFiles(String i_RepositoryName, String i_RepositoryLocation, boolean i_IsEmptyRepo) {
        Repository repository = new Repository();
        repository.SetName(i_RepositoryName);

        File repositoryFolder = new File(i_RepositoryLocation);
        File magit            = new File(repositoryFolder, ".magit");
        File objects          = new File(magit, "objects");
        File Branches         = new File(magit, "branches");

        repositoryFolder.mkdir();
        magit.mkdir();
        objects.mkdir();
        Branches.mkdir();

        repository.SetLocationPath(repositoryFolder.getAbsolutePath());
        f_Engine.SetRepositoryPath(repositoryFolder.getAbsolutePath());
        f_Engine.SetActiveRepository(repository);
        FileUtilities.WriteToFile(Paths.get(repository.GetLocationPath(),
                ".magit", "details.txt").toString(), String.format("%s%s%s", i_RepositoryName, System.lineSeparator(), f_Engine.GetRemoteRepositoryLocation()));

        if(i_IsEmptyRepo) {
            repository.SetBlobs(new HashMap<>());
            repository.SetFolders(new HashMap<>());
            repository.SetCommits(new HashMap<>());
            repository.SetBranches(new HashMap<>());

            Branch masterBranch = new Branch();
            masterBranch.SetIsHead(true);
            masterBranch.SetName("master");
            repository.GetBranches().put("master", masterBranch);

            String headBranchPath = Paths.get(i_RepositoryLocation, ".magit", "branches", "head.txt").toString();
            String masterBranchPath = Paths.get(i_RepositoryLocation, ".magit", "branches", "master.txt").toString();
            FileUtilities.WriteToFile(masterBranchPath, "");
            FileUtilities.WriteToFile(headBranchPath, "master");

            repository.SetHeadBranch(masterBranch);
        }

        return repository;
    }

    private void CreateFilesOnSystem(String i_HeadBranchName) throws IOException, ParseException {
        Repository repository = f_Engine.GetActiveRepository();
        Map<String, Branch> branches = f_Engine.GetActiveRepository().GetBranches();
        Map<String, Commit> commits  = f_Engine.GetActiveRepository().GetCommits();
        Set<String> sha1Set = new HashSet<>();

        for(Map.Entry<String, Branch> branch: branches.entrySet()) {
            Branch newBranch = new Branch();
            String pointedCommitSha1 = branch.getValue().GetPointedCommitSha1();
            String branchPath = Paths.get(repository.GetLocationPath(),
                    ".magit", "branches", branch.getValue().GetName().concat(".txt")).toString();
            Commit commit = commits.get(pointedCommitSha1);

            if(!sha1Set.contains(pointedCommitSha1)) {
                pointedCommitSha1 = spreadCommitAndSetNewSha1s(commit, sha1Set);
            }

            if(branch.getValue().GetName().contains("/") ||
                    branch.getValue().GetName().contains("\\")) {
                String[] parts = branch.getValue().GetName().split("/");
                String remoteRepoName;

                if(parts.length == 1) {
                    parts = branch.getValue().GetName().split(Pattern.quote("\\"));
                }

                remoteRepoName = parts[0];
                String remoteRepoFolderPath = Paths.get(repository.GetLocationPath(),
                        ".magit", "branches", remoteRepoName).toString();
                new File(remoteRepoFolderPath).mkdir();
            }

            FileUtilities.WriteToFile(branchPath, pointedCommitSha1);

            if(branch.getValue().GetName().equals(i_HeadBranchName)) {
                newBranch.SetIsHead(true);
                f_Engine.GetActiveRepository().SetHeadBranch(newBranch);
            }

            newBranch.SetPointedCommitSha1(pointedCommitSha1);
            newBranch.SetName(branch.getValue().GetName());
            branches.put(newBranch.GetName(), newBranch);
        }

        CreateWc(f_Engine.GetActiveRepository().GetHeadBranch());
    }

    void CreateWc(Branch i_HeadBranch) throws IOException {
        String repoPath = f_Engine.GetActiveRepository().GetLocationPath();
        String headBranchFilePath = Paths.get(repoPath,".magit", "branches", "head.txt").toString();
        FileUtilities.WriteToFile(headBranchFilePath, i_HeadBranch.GetName());

        String pointedCommitSha1 = i_HeadBranch.GetPointedCommitSha1();
        Commit currentCommit     = f_Engine.GetActiveRepository().GetCommits().get(pointedCommitSha1);

        CreateFolderFromCommit(currentCommit, f_Engine.GetRepositoryPath());
    }

    void CreateFolderFromCommit(Commit i_Commit, String i_Location) {
        String rootFolderSha1    = i_Commit.GetRootFolderSHA1();
        Folder rootFolder        = f_Engine.GetActiveRepository().GetFolders().get(rootFolderSha1);

        CreateWcRec(rootFolder, i_Location);
    }

    void CreateFolder(Folder i_Folder, String i_FullPath) {
        File folderFile = new File(i_FullPath);
        folderFile.mkdir();
        CreateWcRec(i_Folder, i_FullPath);
    }

    private void CreateWcRec(Folder i_RootFolder, String i_CurrentLocation) {
        List<Folder.Data> filesInFolder = i_RootFolder.GetFiles();

        for(Folder.Data file: filesInFolder) {
            if(file.GetFileType().equals(eFileType.FOLDER)) {
                Folder subFolder = f_Engine.GetActiveRepository().GetFolders().get(file.GetSHA1());
                String subFolderLocation = Paths.get(i_CurrentLocation, file.GetName()).toString();
                File folderFile = new File(subFolderLocation);
                folderFile.mkdir();

                CreateWcRec(subFolder, subFolderLocation);
            }
            else {
                Blob blob = f_Engine.GetActiveRepository().GetBlobs().get(file.GetSHA1());
                String blobPath = Paths.get(i_CurrentLocation, file.GetName()).toString();
                FileUtilities.WriteToFile(blobPath, blob.GetText());
            }
        }
    }

    public String spreadCommitAndSetNewSha1s(Commit i_Commit, Set<String> i_Sha1Set) throws IOException, ParseException {
        String firstPrecedingCommitSha1 = i_Commit.getFirstPrecedingSha1();
        String secondPrecedingCommitSha1 = i_Commit.getSecondPrecedingSha1();

        // Setting first preceding commit
        if(!firstPrecedingCommitSha1.isEmpty()) {
            Commit firstPrecedingCommit = f_Engine.GetActiveRepository().GetCommits().get(firstPrecedingCommitSha1);

            if(firstPrecedingCommit != null) {
                firstPrecedingCommitSha1 = spreadCommitAndSetNewSha1s(firstPrecedingCommit, i_Sha1Set);
                i_Commit.SetFirstPrecedingCommitSha1(firstPrecedingCommitSha1);
            }

            // Setting Second preceding commit
            if(!secondPrecedingCommitSha1.isEmpty()) {
                Commit secondPrecedingCommit = f_Engine.GetActiveRepository().GetCommits().get(secondPrecedingCommitSha1);

                if(secondPrecedingCommit != null) {
                    secondPrecedingCommitSha1 = spreadCommitAndSetNewSha1s(secondPrecedingCommit, i_Sha1Set);
                    i_Commit.SetSecondPrecedingCommitSha1(secondPrecedingCommitSha1);
                }
            }
        }

        // Setting root folder
        String rootFolderSha1 = i_Commit.GetRootFolderSHA1();
        Folder rootFolder     = f_Engine.GetActiveRepository().GetFolders().get(rootFolderSha1);
        String objectsPath    = Paths.get(f_Engine.GetActiveRepository().GetLocationPath(), ".magit", "objects").toString();
        rootFolderSha1        = spreadCommitAndSetNewSha1sRec(rootFolder, "", i_Commit.GetLastUpdate(), f_Engine.GetRepositoryPath(), false);

        f_Engine.GetActiveRepository().GetFolders().put(rootFolderSha1, rootFolder);
        i_Commit.SetRootFolderSHA1(rootFolderSha1);

        // Zip and files management
        String commitSha1 = DigestUtils.sha1Hex(i_Commit.toStringForSha1());
        String zipPath = Paths.get(objectsPath, commitSha1).toString();
        FileUtilities.ZipFile(commitSha1, i_Commit.toString(), zipPath);
        i_Sha1Set.add(commitSha1);
        File rootFolderFile = new File(Paths.get(objectsPath, rootFolderSha1).toString());
        rootFolderFile.setLastModified(new SimpleDateFormat(Engine.DATE_FORMAT).parse(i_Commit.GetLastUpdate()).getTime());

        f_Engine.GetActiveRepository().GetCommits().put(commitSha1, i_Commit);

        return commitSha1;
    }

    private String spreadCommitAndSetNewSha1sRec(IRepositoryFile i_File, String i_FileNameInFolder, String i_LastModified, String i_CurrentPath,  boolean i_IsCreateWC) throws IOException, ParseException {
        String objectsFolderPath = Paths.get(f_Engine.GetActiveRepository().GetLocationPath(), ".magit", "objects").toString();
        String Sha1;

        if(i_File instanceof Blob) {
            String blobContent = ((Blob)i_File).GetText();
            Sha1               = DigestUtils.sha1Hex(((Blob)i_File).toStringForSha1());
            String zipPath     = Paths.get(objectsFolderPath, Sha1).toString();

            if(i_IsCreateWC) {
                String blobPath = Paths.get(i_CurrentPath, i_FileNameInFolder).toString();
                FileUtilities.WriteToFile(blobPath, blobContent);
                File blobFile = new File(blobPath);
                blobFile.setLastModified(new SimpleDateFormat(Engine.DATE_FORMAT).parse(i_LastModified).getTime());
            }

            if(!isSha1Exists(Sha1, objectsFolderPath)) {
                FileUtilities.ZipFile(Sha1, blobContent, zipPath);
            }
        }
        else {
            Folder folder = (Folder)i_File;
            String folderPath = Paths.get(i_CurrentPath, i_FileNameInFolder).toString();
            File folderToCreate = new File(folderPath);

            if(!i_FileNameInFolder.equals("") && i_IsCreateWC) {
                folderToCreate.mkdir();
            }

            for(Folder.Data file: folder.GetFiles()) {
                IRepositoryFile item = file.GetFileType().equals(eFileType.BLOB) ?
                        f_Engine.GetActiveRepository().GetBlobs().get(file.GetSHA1()) :
                        f_Engine.GetActiveRepository().GetFolders().get(file.GetSHA1());
                String itemSha1 = spreadCommitAndSetNewSha1sRec(item, file.GetName(), file.GetlastUpdate(), folderPath, i_IsCreateWC);

                if(file.GetFileType().equals(eFileType.BLOB)) {
                    f_Engine.GetActiveRepository().GetBlobs().put(itemSha1,
                            f_Engine.GetActiveRepository().GetBlobs().get(file.GetSHA1()));
                }
                else {
                    f_Engine.GetActiveRepository().GetFolders().put(itemSha1,
                            f_Engine.GetActiveRepository().GetFolders().get(file.GetSHA1()));
                }

                file.SetSHA1(itemSha1);
            }

            folder.GetFiles().sort(Folder.Data::compare);
            Sha1 = DigestUtils.sha1Hex(folder.toStringForSha1(Paths.get(folderPath)));

            if(!isSha1Exists(Sha1, objectsFolderPath)) {
                String zipPath = Paths.get(objectsFolderPath, Sha1).toString();
                FileUtilities.ZipFile(Sha1, folder.toString(), zipPath);
            }

            if(!i_FileNameInFolder.equals("") && i_IsCreateWC) {
                folderToCreate.setLastModified(new SimpleDateFormat(Engine.DATE_FORMAT).parse(i_LastModified).getTime());
            }
        }

        return Sha1;
    }

    private boolean isSha1Exists(String i_Sha1, String i_ObjectsPath) {
        boolean isExists = false;
        File objectsDir = new File(i_ObjectsPath);
        File[] files = objectsDir.listFiles();

        if(files != null) {
            for (File file : files) {
                if (file.getName().equals(i_Sha1)) {
                    isExists = true;
                    break;
                }
            }
        }

        return isExists;
    }

    public Map<String, Blob> GetTmpBlobs() {
        return sf_TmpBlobs;
    }

    Map<String, Folder> GetTmpFolders() {
        return sf_TmpFolders;
    }

    Map<String, String> CreatePathToSha1Map(Branch i_Branch) {
        Map<String, String> map = new HashMap<>();
        String currentCommitSha1 = i_Branch.GetPointedCommitSha1();

        if(!currentCommitSha1.isEmpty()) {
            Commit currentCommit = f_Engine.GetActiveRepository().GetCommits().get(currentCommitSha1);
            map = CreatePathToSha1MapFromCommit(currentCommit);
        }

        return map;
    }

    Map<String, String> CreatePathToSha1MapFromCommit(Commit i_Commit) {
        HashMap<String, String> map = new HashMap<>();
        Folder rootFolder = f_Engine.GetActiveRepository().GetFolders().get(i_Commit.GetRootFolderSHA1());

        CreateCurrentCommitPathToSha1MapRec(rootFolder, f_Engine.GetRepositoryPath(), map);

        rootFolder.GetFiles().sort(Folder.Data::compare);
        String path = f_Engine.GetRepositoryPath();

        if(!f_Engine.GetRemoteRepositoryLocation().isEmpty()) {
            path = f_Engine.ReplaceRootPath(f_Engine.GetRepositoryPath(), f_Engine.GetRemoteRepositoryLocation(), 2);
        }

        String rootFolderSha1 = DigestUtils.sha1Hex(rootFolder.toStringForSha1(Paths.get(path)));
        map.put(f_Engine.GetRepositoryPath(), rootFolderSha1);

        return map;
    }

    private void CreateCurrentCommitPathToSha1MapRec(IRepositoryFile i_Item, String i_CurrentPath, Map<String, String> i_Map) {
        if(i_Item instanceof Folder) {
            List<Folder.Data> filesInFolder = ((Folder)i_Item).GetFiles();

            for(Folder.Data file: filesInFolder) {
                String newPath = Paths.get(i_CurrentPath, file.GetName()).toString();

                if(file.GetFileType().equals(eFileType.FOLDER)) {
                    Folder subFolder = f_Engine.GetActiveRepository().GetFolders().get(file.GetSHA1());
                    CreateCurrentCommitPathToSha1MapRec(subFolder, newPath, i_Map);
                    i_Map.put(newPath, file.GetSHA1());
                }
                else {
                    i_Map.put(newPath, file.GetSHA1());
                }
            }
        }

    }

    String CreateBlob(String i_Path) throws IOException {
        Blob blob = new Blob();

        blob.SetText(FileUtilities.ReadTextFromFile(i_Path));
        String sha1 = DigestUtils.sha1Hex(blob.toStringForSha1());
        sf_TmpBlobs.put(sha1, blob);

        return sha1;
    }

    String CreateFolder(String i_Path, String i_PutLastModifiedIfNew) throws IOException {
        Path folderPath = Paths.get(i_Path);

        Folder folder = new Folder();
        File folderFile = new File(i_Path);
        File[] filesInFolder = folderFile.listFiles();
        String sha1 = null;

        if(filesInFolder != null) {
            for (File file : filesInFolder) {
                boolean isFolder = file.isDirectory();

                if(!isFolder || file.listFiles().length != 0) {
                    if (isFolder) {
                        sha1 = CreateFolder(file.toPath().toString(),i_PutLastModifiedIfNew);
                    } else {
                        sha1 = CreateBlob(file.toPath().toString());
                    }

                    Folder.Data fileData = Folder.Data.Parse(file, sha1);

                    if(i_PutLastModifiedIfNew != null) {
                        fileData.SetlastUpdate(i_PutLastModifiedIfNew);
                    }

                    folder.AddFile(fileData);
                }
            }

            if (i_Path.equals(f_Engine.GetRepositoryPath())) {
                folder.SetIsRoot(true);
            }

            folder.GetFiles().sort(Folder.Data::compare);

            if(f_Engine.GetRemoteRepositoryLocation().isEmpty()) {
                sha1 = DigestUtils.sha1Hex(folder.toStringForSha1(folderPath));
            }
            else {
                sha1 = DigestUtils.sha1Hex(folder.toStringForSha1(Paths.get(f_Engine.ReplaceRootPath(i_Path,
                        f_Engine.GetRemoteRepositoryLocation(), 2))));
            }
            sf_TmpFolders.put(sha1, folder);
        }

        return sha1;
    }

    private Map<String, MagitBlob> GenerateMagitBlobsHashMap(List<MagitBlob> i_MagitBlobs) {
        return i_MagitBlobs.stream().collect(Collectors.toMap(MagitBlob::getId, Function.identity()));
    }

    private  Map<String, Folder> GenerateFoldersHashMap(List<MagitSingleFolder> i_MagitFolders, Map<String, MagitBlob> i_MagitBlobsHashMap) {
        HashMap<String, MagitSingleFolder> magitFoldersHashMap = new HashMap<>();

        for(MagitSingleFolder folder : i_MagitFolders) {
            magitFoldersHashMap.put(folder.getId(), folder);
        }

        return transformMagitFolder(magitFoldersHashMap, i_MagitBlobsHashMap);
    }

    private Map<String, Folder> transformMagitFolder(HashMap<String, MagitSingleFolder> i_MagitFoldersHashMap, Map<String, MagitBlob> i_MagitBlobsHashMap) {
        HashMap<String, Folder> foldersHashMap = new HashMap<>();

        for(Map.Entry<String, MagitSingleFolder> folder : i_MagitFoldersHashMap.entrySet()) {
            if(!foldersHashMap.containsKey(folder.getValue().getId())) {
                boolean isRoot = folder.getValue().isIsRoot();
                Folder folderToPut = new Folder();
                foldersHashMap.put(folder.getValue().getId(), folderToPut);
                folderToPut.SetIsRoot(isRoot);
            }

            for (Item item : folder.getValue().getItems().getItem()) {
                boolean isFolder = item.getType().equals("folder");
                Folder.Data folderData = new Folder.Data();

                folderData.SetSHA1(item.getId());
                folderData.SetFileType(isFolder ?
                        eFileType.FOLDER :
                        eFileType.BLOB);
                folderData.SetLastChanger(isFolder ?
                        i_MagitFoldersHashMap.get(item.getId()).getLastUpdater() :
                        i_MagitBlobsHashMap.get(item.getId()).getLastUpdater());
                folderData.SetlastUpdate(isFolder ?
                        i_MagitFoldersHashMap.get(item.getId()).getLastUpdateDate() :
                        i_MagitBlobsHashMap.get(item.getId()).getLastUpdateDate());
                folderData.SetName(isFolder ?
                        i_MagitFoldersHashMap.get(item.getId()).getName() :
                        i_MagitBlobsHashMap.get(item.getId()).getName());

                foldersHashMap.get(folder.getValue().getId()).AddFile(folderData);
            }
        }

        return foldersHashMap;
    }

    private Map<String, Blob> GenerateBlobsHashMap(List<MagitBlob> i_MagitBlobs) {
        return i_MagitBlobs.stream().collect(Collectors.toMap(MagitBlob::getId, Blob::Parse));
    }

    private Map<String, Commit> GenerateCommitsHashMap(List<MagitSingleCommit> i_MagitCommits) {
        return i_MagitCommits.stream().collect(Collectors.toMap(MagitSingleCommit::getId, Commit::Parse));
    }

    private Map<String, Branch> GenerateBranchesHashMap(List<MagitSingleBranch> i_MagitBranches) {
        return i_MagitBranches.stream().collect(Collectors.toMap(MagitSingleBranch::getName, Branch::Parse));
    }

    public void Clear() {
        sf_TmpBlobs.clear();
        sf_TmpFolders.clear();
    }
}
