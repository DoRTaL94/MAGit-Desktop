package magit;

import resources.jaxb.schema.generated.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class XmlHelper {
    private final static String JAXB_XML_GAME_PACKAGE_NAME = "resources.jaxb.schema.generated";
    private MagitRepository m_MagitRepository;
    private String m_XmlPath;

    XmlHelper(Path i_XmlPath) {
        m_XmlPath = i_XmlPath.toString();
        m_MagitRepository = null;
    }

    MagitRepository GetMagitRepository() {
        return m_MagitRepository;
    }

    boolean IsValidXmlPath() {
        return m_XmlPath.endsWith(".xml");
    }

    List<String> RunCheckOnXmlFile() throws FileNotFoundException {
        List<String> errors;

        if (IsValidXmlPath()) {
            errors = LoadRepositoryFromXml();
        }
        else {
            errors = new ArrayList<>();
            errors.add("Xml file should end with \".xml\".");
        }

        return errors;
    }

    private MagitRepository LoadMagitRepository(InputStream i_XmlInputStream) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(JAXB_XML_GAME_PACKAGE_NAME);
        Unmarshaller u = jc.createUnmarshaller();
        return (MagitRepository)u.unmarshal(i_XmlInputStream);
    }

    private List<String> LoadRepositoryFromXml() throws FileNotFoundException {
        List<String> errors = new ArrayList<>();

        try {
            InputStream inputStream = new FileInputStream(m_XmlPath);
            m_MagitRepository = LoadMagitRepository(inputStream);
            errors.addAll(CheckErrorsInRepo(m_MagitRepository));
            errors.addAll(CheckDuplicateKeyInMagitRepo(m_MagitRepository));
        }
        catch (JAXBException e) {
            e.printStackTrace();
        }

        return errors;
    }

    private List<String> CheckDuplicateKeyInMagitRepo(MagitRepository i_MagitRepository) {
        List<String> errors = new ArrayList<>();

        errors.addAll(CheckDuplicateKeyInMagitBlobs(i_MagitRepository));
        errors.addAll(CheckDuplicateKeyInMagitFolders(i_MagitRepository));
        errors.addAll(CheckDuplicateKeyInMagitCommits(i_MagitRepository));
        errors.addAll(CheckDuplicateKeyInMagitBranches(i_MagitRepository));

        return errors;
    }

    private List<String> CheckDuplicateKeyInMagitBranches(MagitRepository i_MagitRepository) {
        List<MagitSingleBranch> branches = i_MagitRepository.getMagitBranches().getMagitSingleBranch();
        List<String> errors = new ArrayList<>();

        for(int i = 0; i < branches.size(); i++) {
            for (int j = i; j < branches.size(); j++) {
                if (i != j && branches.get(i).getName().equals(branches.get(j).getName())) {
                    errors.add(String.format("%sThere are several branches with same name (Name: %s)", System.lineSeparator(), branches.get(i).getName()));
                    removeDuplicates(branches, branches.get(i), j, (b1, b2) -> ((MagitSingleBranch)b1).getName().equals(((MagitSingleBranch)b2).getName()));
                }
            }
        }
        return errors;
    }

    private List<String> CheckDuplicateKeyInMagitCommits(MagitRepository i_MagitRepository) {
        List<MagitSingleCommit> commits = i_MagitRepository.getMagitCommits().getMagitSingleCommit();
        List<String> errors = new ArrayList<>();

        for(int i = 0; i < commits.size(); i++) {
            for(int j = i; j < commits.size(); j++) {
                if(i != j && commits.get(i).getId().equals(commits.get(j).getId())) {
                    errors.add(String.format("There are several commits id with same value (Id: %s)", commits.get(i).getId()));
                    removeDuplicates(commits, commits.get(i), j, (c1, c2) -> ((MagitSingleCommit)c1).getId().equals(((MagitSingleCommit)c2).getId()));
                }
            }
        }

        return errors;
    }

    private List<String> CheckDuplicateKeyInMagitFolders(MagitRepository i_MagitRepository) {
        List<MagitSingleFolder> folders = i_MagitRepository.getMagitFolders().getMagitSingleFolder();
        List<String> errors = new ArrayList<>();

        for(int i = 0; i < folders.size(); i++) {
            for(int j = i; j < folders.size(); j++) {
                if(i != j && folders.get(i).getId().equals(folders.get(j).getId())) {
                    errors.add(String.format("There are several folders id with same value (Id: %s)", folders.get(i).getId()));
                    removeDuplicates(folders, folders.get(i), j, (f1, f2) -> ((MagitSingleFolder)f1).getId().equals(((MagitSingleFolder)f2).getId()));
                }
            }
        }

        return errors;
    }

    private List<String> CheckDuplicateKeyInMagitBlobs(MagitRepository i_MagitRepository) {
        List<MagitBlob> blobs = i_MagitRepository.getMagitBlobs().getMagitBlob();
        List<String> errors = new ArrayList<>();

        for(int i = 0; i < blobs.size(); i++) {
            for(int j = i; j < blobs.size(); j++) {
                if(i != j && blobs.get(i).getId().equals(blobs.get(j).getId())) {
                    errors.add(String.format("There are several blobs id with same value (Id: %s)", blobs.get(i).getId()));
                    removeDuplicates(blobs, blobs.get(i), j, (b1, b2) -> ((MagitBlob)b1).getId().equals(((MagitBlob)b2).getId()));
                }
            }
        }

        return errors;
    }

    private void removeDuplicates(List<?> i_List, Object i_ToCompare, int i_StartIndex, ICompare<Object, Object, Boolean> i_IsRomove) {
        for(int i = i_StartIndex; i < i_List.size(); i++) {
            if(i_IsRomove.compare(i_ToCompare, i_List.get(i))) {
                i_List.remove(i);
            }
        }
    }

    private List<String> CheckErrorsInRepo(MagitRepository i_Repository) {
        boolean isHeadNameExistsInBranchList = false;
        List<String> errors                  = new ArrayList<>();

        MagitBranches magitBranches          = i_Repository.getMagitBranches();
        List<MagitSingleBranch> branches     = magitBranches.getMagitSingleBranch();
        String headBranchName                = magitBranches.getHead();

        for(MagitSingleBranch branch: branches) {
            if(headBranchName.equals(branch.getName())) {
                isHeadNameExistsInBranchList = true;
            }

            errors.addAll(CheckErrorsInBranch(branch, i_Repository));
        }

        MagitCommits magitCommits = i_Repository.getMagitCommits();
        List<MagitSingleCommit> commits = magitCommits.getMagitSingleCommit();

        for(MagitSingleCommit commit: commits) {
            errors.addAll(CheckErrorInCommit(commit, i_Repository));
        }

        if (!isHeadNameExistsInBranchList) {
            errors.add(String.format("%sHead branch (Name: %s) doesn't exist in branches list", System.lineSeparator(), headBranchName));
        }

        return errors;
    }

    private List<String> CheckErrorsInBranch(MagitSingleBranch branch, MagitRepository i_Repository) {
        List<String> errors             = new ArrayList<>();
        String pointedCommitId          = branch.getPointedCommit().getId();
        MagitSingleCommit pointedCommit = GetCommitById(pointedCommitId, i_Repository);

        if(i_Repository.getMagitCommits().getMagitSingleCommit().size() != 0) {
            if (pointedCommit == null) {
                errors.add(String.format("%sBranch (Name: %s) pointing to commit (Id: %s) that not exists", System.lineSeparator(), branch.getName(), branch.getPointedCommit().getId()));
            }

        }

        return errors;
    }

    private List<String> CheckErrorInCommit(MagitSingleCommit i_Commits, MagitRepository i_Repository) {
        List<String> errors             = new ArrayList<>();

        PrecedingCommits precedingCommitsObject = i_Commits.getPrecedingCommits();

        if(precedingCommitsObject != null) {
            List<PrecedingCommits.PrecedingCommit> precedingCommits = precedingCommitsObject.getPrecedingCommit();

            if(precedingCommits.size() != 0) {
                String precedingCommitSha1 = precedingCommits.get(0).getId();
                MagitSingleCommit precedingCommit = GetCommitById(precedingCommitSha1, i_Repository);

                if (precedingCommit == null) {
                    errors.add(String.format("%sCommit (Id: %s) pointing to preceding commit (Id: %s) that not exists", System.lineSeparator(), i_Commits.getId(), precedingCommitSha1));
                }
            }
        }

        String rootFolderId          = i_Commits.getRootFolder().getId();
        MagitSingleFolder rootFolder = GetFolderById(rootFolderId, i_Repository);

        if (rootFolder == null) {
            errors.add(String.format("%sCommit (Id: %s) pointing to folder (Id: %s) that not exists", System.lineSeparator(), i_Commits.getId(), i_Commits.getRootFolder().getId()));
        }
        else {
            if (!rootFolder.isIsRoot()) {
                errors.add(String.format("%sCommit (Id: %s) is not pointing to root folder (Id: %s)", System.lineSeparator(), i_Commits.getId(), rootFolder.getId()));
            }

            errors.addAll(CheckFolderForErrors(rootFolder, i_Repository));
        }

        return errors;
    }

    private List<String> CheckFolderForErrors(MagitSingleFolder rootFolder, MagitRepository i_Repository) {
        List<String> errors = new ArrayList<>();

        if(rootFolder.getItems() == null) {
            errors.add(String.format("Folder (Id: %s) is empty.%s", rootFolder.getId(), System.lineSeparator()));
        }
        else {
            List<Item> folderItems = rootFolder.getItems().getItem();

            for (Item item : folderItems) {
                if (item.getType().equals("folder")) {
                    MagitSingleFolder folder = GetFolderById(item.getId(), i_Repository);

                    if (folder == null) {
                        errors.add(String.format("%sFolder (Id: %s) not exists", System.lineSeparator(), item.getId()));
                    } else {
                        if(folder.getId().equals(rootFolder.getId())) {
                            errors.add(String.format("%sFolder (Id: %s) is pointing to itself", System.lineSeparator(), rootFolder.getId()));
                        }
                        else {
                            errors.addAll(CheckFolderForErrors(folder, i_Repository));
                        }
                    }
                } else {
                    if (GetBlobById(item.getId(), i_Repository) == null) {
                        errors.add(String.format("%sBlob (Id: %s, Folder id: %s) not exists", System.lineSeparator(), item.getId(), rootFolder.getId()));
                    }
                }
            }
        }

        return errors;
    }

    private MagitBlob GetBlobById(String i_BlobId, MagitRepository i_repository) {
        List<MagitBlob> blobs = i_repository.getMagitBlobs().getMagitBlob();
        MagitBlob blobToReturn = null;

        for(MagitBlob blob: blobs) {
            if(blob.getId().equals(i_BlobId)) {
                blobToReturn = blob;
                break;
            }
        }

        return blobToReturn;
    }

    private MagitSingleFolder GetFolderById(String rootFolderId, MagitRepository i_repository) {
        List<MagitSingleFolder> folders = i_repository.getMagitFolders().getMagitSingleFolder();
        MagitSingleFolder rootFolder = null;

        for(MagitSingleFolder folder : folders) {
            if(folder.getId().equals(rootFolderId)) {
                rootFolder = folder;
                break;
            }
        }

        return rootFolder;
    }

    private MagitSingleCommit GetCommitById(String pointedCommitId, MagitRepository i_Repository) {
        List<MagitSingleCommit> commits = i_Repository.getMagitCommits().getMagitSingleCommit();
        MagitSingleCommit commitToReturn = null;

        for(MagitSingleCommit commit: commits) {
            if(commit.getId().equals(pointedCommitId)) {
                commitToReturn = commit;
                break;
            }
        }

        return commitToReturn;
    }

    public void MagitRepositoryToXml(MagitRepository i_MagitRepo) {
        try {
            File file = new File(m_XmlPath);
            JAXBContext jaxbContext = JAXBContext.newInstance(JAXB_XML_GAME_PACKAGE_NAME);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(i_MagitRepo, file);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}
