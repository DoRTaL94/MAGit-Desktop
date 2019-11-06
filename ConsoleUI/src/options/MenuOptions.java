package options;

import IO.ConsoleUtils;
import MagitExceptions.*;
import javafx.beans.property.SimpleStringProperty;
import magit.IEngine;
import menus.ActionItem;
import menus.Menu;
import menus.SubMenu;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MenuOptions {
    private static List<SubMenu> m_Actions = new ArrayList<>();

    public static void CreateActions() {
        m_Actions.add(new ActionItem(" Set username", ()->{
            System.out.println("Please enter user name:");
            System.out.print(">> ");
            Scanner scanner = new Scanner(System.in);
            Menu.GetEngine().SetCurrentUserName(scanner.nextLine());
            System.out.println();
            System.out.println("Message: User name has been changed.");
            System.out.println();
        }));
        m_Actions.add(new ActionItem(" Load repository from xml", ()-> {
            boolean isSuccessfulLoad         = false;
            boolean isLoadDataFromRepository = false;
            String xmlPath                   = null;
            Scanner scanner                  = new Scanner(System.in);

            while(!isSuccessfulLoad) {
                try {
                    if(isLoadDataFromRepository) {
                        Menu.GetEngine().LoadDataFromRepository(Menu.GetEngine().GetRepositoryPath());
                        System.out.println();
                        System.out.format("Message: %s has been loaded successfully.", Menu.GetEngine().GetActiveRepository().GetName());
                        System.out.println();
                    } else {
                        if(xmlPath == null) {
                            System.out.format("Please enter full xml path:%n>> ");
                            xmlPath = scanner.nextLine();
                            System.out.println();
                        }

                        Menu.GetEngine().LoadRepositoryFromXml(xmlPath, new SimpleStringProperty(""));
                        System.out.format("Message: \"%s\" repository has been loaded successfully.", Menu.GetEngine().GetActiveRepository().GetName());
                        System.out.println();
                    }

                    System.out.println();
                    isSuccessfulLoad = true;
                }
                catch (FileNotFoundException e) {
                    System.out.println("ERROR: " + "File not found.");
                    System.out.println();
                    break;
                }
                catch (RepositoryAlreadyExistsException e) {
                    try {
                        isLoadDataFromRepository = RepoAlreadyExistsExceptionHandler(Menu.GetEngine().GetRepositoryPath());
                        System.out.println();
                    } catch (IOException ioe) {
                        System.out.println();
                        System.out.println("ERROR: " + ioe.getMessage());
                        System.out.println("Please check if all files in the directory are closed.");
                        System.out.println();
                        break;
                    }
                }
                catch (XmlErrorsException e) {
                    if(e.GetErrors() != null && e.GetErrors().size() == 1) {
                        String error = e.GetErrors().toString();
                        System.out.println("ERROR: " + error.substring(1, error.length() - 1));
                    }
                    else if(e.GetErrors() != null && e.GetErrors().size() > 1) {
                        System.out.println("ERRORS: " + e.GetErrors());
                    }
                    else {
                        System.out.println("ERROR: " + e.getMessage());
                    }

                    System.out.println();
                    break;
                } catch (FolderInLocationAlreadyExistsException e) {
                    System.out.println("ERROR: " + e.getMessage());
                    System.out.println();
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));
        m_Actions.add(new ActionItem(" Change active repository", ()->{
            System.out.println("Please enter full repository path:");
            System.out.print(">> ");

            Scanner scanner = new Scanner(System.in);
            String newRepoPath = scanner.nextLine();
            String currentRepoPath = Menu.GetEngine().GetRepositoryPath();

            try {
                if(currentRepoPath == null || !Paths.get(newRepoPath).toString().equals(Paths.get(currentRepoPath).toString())) {
                    boolean isWcClean = true;
                    int input = 0;

                    if(currentRepoPath != null) {
                        List<List<String>> wcStatus = Menu.GetEngine().GetWorkingCopyDelta();
                        isWcClean = wcStatus.get(0).size() == 0 && wcStatus.get(1).size() == 0 && wcStatus.get(2).size() == 0;

                        if (!isWcClean) {
                            System.out.println();
                            System.out.println("WARNING: Working copy has uncommitted files.");
                            System.out.println("Would you like to proceed with your request?");
                            System.out.println("1. Yes");
                            System.out.println("2. No");
                            input = ConsoleUtils.GetUserChoice();
                        }
                    }

                    System.out.println();

                    if (input == 1 || isWcClean) {
                        Menu.GetEngine().ChangeActiveRepository(newRepoPath);
                        System.out.format("Message: %s has been loaded successfully.", Menu.GetEngine().GetActiveRepository().GetName());
                        System.out.println();
                    }
                    else if(input == 2) {
                        System.out.println("Message: Request has been aborted.");
                    }
                    else {
                        System.out.println("ERROR: Invalid input! Please try again.");
                    }
                }
                else {
                    System.out.println();
                    System.out.format("Message: %s has already been loaded.", Menu.GetEngine().GetActiveRepository().GetName());
                    System.out.println();
                }
            }
            catch (NotRepositoryFolderException nrfe) {
                System.out.println("ERROR: " + nrfe.getMessage());
            }
            catch (InvalidPathException ipe) {
                System.out.println("ERROR: Path input is invalid.");
            } catch (IOException e) {
                System.out.println("ERROR: Repository is corrupted.");
            }

            System.out.println();
        }));
        m_Actions.add(new ActionItem(" Show current commit files", ()->{
            if(Menu.GetEngine().GetActiveRepository() != null) {
                List<String> commitFilesInfo = Menu.GetEngine().ShowCurrentCommitFiles();

                if(commitFilesInfo.size() != 0) {
                    printFilesDetails(commitFilesInfo);
                }
                else {
                    System.out.println("Message: Commit does not exist.");
                }
            }
            else {
                System.out.println("ERROR: Please load repository first.");
            }

            System.out.println();
        }));
        m_Actions.add(new ActionItem(" Show working copy status", ()-> {
            if(Menu.GetEngine().GetActiveRepository() != null) {
                List<List<String>> wcStatus = Menu.GetEngine().GetWorkingCopyDelta();
                List<String> deletedFile = wcStatus.get(0);
                List<String> newFiles = wcStatus.get(1);
                List<String> changedFiles = wcStatus.get(2);

                System.out.println(String.format("Repository name: %s", Menu.GetEngine().GetActiveRepository().GetName()));
                System.out.println(String.format("Repository location: %s", Menu.GetEngine().GetActiveRepository().GetLocationPath()));
                System.out.println();

                if (deletedFile.size() != 0) {
                    System.out.println(">>> These files were deleted from working copy >>>");
                    System.out.println();
                    printFilesDetails(deletedFile);
                    System.out.println();
                }

                if (newFiles.size() != 0) {
                    System.out.println(">>> These files are new files in working copy >>>");
                    System.out.println();
                    printFilesDetails(newFiles);
                    System.out.println();
                }

                if (changedFiles.size() != 0) {
                    System.out.println(">>> These files were changed in working copy >>>");
                    System.out.println();
                    printFilesDetails(changedFiles);
                    System.out.println();
                }

                if (changedFiles.size() == 0 && newFiles.size() == 0 && deletedFile.size() == 0) {
                    System.out.println("Message: Working copy is in a clean state.");
                    System.out.println();
                }
            }
            else {
                System.out.println("ERROR: Please load repository first.");
                System.out.println();
            }
        }));
        m_Actions.add(new ActionItem(" Commit", ()-> {
            if(Menu.GetEngine().GetActiveRepository() != null) {
                Scanner scanner = new Scanner(System.in);
                System.out.println("Please write commit description:");
                System.out.print(">> ");
                String description = scanner.nextLine();
                System.out.println();

                try {
                    boolean isCommitExecuted = Menu.GetEngine().Commit(description, null);

                    if (isCommitExecuted) {
                        System.out.println("Message: Commit was executed successfully.");
                    } else {
                        System.out.println("Message: Working copy didn't change.");
                    }
                }
                catch (IOException e) {
                    System.out.println();
                    System.out.println("ERROR: Please make sure to close all files associated with the repository.");
                }
                catch (EmptyWcException ewe) {
                    System.out.println("ERROR: " + ewe.getMessage());
                } catch (CommitAlreadyExistsException e) {
                    System.out.println("ERROR:" + e.getMessage());
                }
            }
            else {
                System.out.println("ERROR: Please load repository first.");
            }

            System.out.println();
        }));
        m_Actions.add(new ActionItem(" Show all branches", ()-> {
            if(Menu.GetEngine().GetActiveRepository() != null) {
                String newLine = System.lineSeparator();
                List<String> branchesInfo = Menu.GetEngine().ShowAllBranches();

                for(String branchInfo: branchesInfo) {
                    String[] parts = branchInfo.split(";");

                    if(parts.length == 4) {
                        System.out.println(">>> HEAD BRANCH <<<");
                    }

                    System.out.format("Branch name: %s%s", parts[0], newLine);
                    System.out.format("Pointed commit sha1: %s%s", parts[1], newLine);
                    System.out.format("Commit description: %s%s", parts[2], newLine);
                    System.out.println();
                }
            }
            else {
                System.out.println("ERROR: Please load repository first.");
                System.out.println();
            }
        }));
        m_Actions.add(new ActionItem(" Create new branch", ()-> {
            if(Menu.GetEngine().GetActiveRepository() != null) {
                System.out.println("Please enter branch name:");
                System.out.print(">> ");
                Scanner scanner = new Scanner(System.in);
                String branchName = scanner.nextLine();

                if (Menu.GetEngine().IsBranchNameExists(branchName)) {
                    System.out.println();
                    System.out.println("ERROR: Branch name already exists.");
                }
                else {
                    try {
                        System.out.println();
                        Menu.GetEngine().CreateNewBranch(branchName);
                        System.out.format("Message: Branch \"%s\" has been created.", branchName);
                        System.out.println();

                        List<List<String>> wcStatus = Menu.GetEngine().GetWorkingCopyDelta();
                        boolean isWcClean = wcStatus.get(0).size() == 0 &&
                                wcStatus.get(1).size() == 0 &&
                                wcStatus.get(2).size() == 0;

                        if(isWcClean) {
                            System.out.println("Do you want to execute checkout on this branch?");
                            System.out.println("1. Yes");
                            System.out.println("2. No");
                            int input = ConsoleUtils.GetUserChoice();

                            if (input == 1) {
                                Menu.GetEngine().Checkout(branchName, true);
                            } else if (input != 2) {
                                System.out.println("ERROR: Invalid input.");
                                System.out.println();
                            }
                        }
                    }
                    catch (PointedCommitEmptyException pcee) {
                        System.out.println("ERROR: " + pcee.getMessage() + " Please perform commit before creating a new branch.");
                    } catch (IOException ioe) {
                        System.out.println("ERROR: Please check if all files in the working copy are closed and try again.");
                    } catch (Exception e) {
                    }
                }
            }
            else {
                System.out.println("ERROR: Please load repository first.");
            }

            System.out.println();
        }));
        m_Actions.add(new ActionItem(" Delete branch", ()-> {
            if(Menu.GetEngine().GetActiveRepository() != null) {
                System.out.println("Please enter branch name:");
                System.out.print(">> ");
                Scanner scanner = new Scanner(System.in);
                String branchName = scanner.nextLine();

                if (!Menu.GetEngine().IsBranchNameExists(branchName)) {
                    System.out.println();
                    System.out.format("ERROR: Branch named \"%s\" is not found.", branchName);
                    System.out.println();
                }
                else {
                    if(Menu.GetEngine().GetActiveRepository().GetHeadBranch().GetName().equals(branchName)) {
                        System.out.println();
                        System.out.format("ERROR: The branch named \"%s\" is a head branch and therefore cannot be deleted.", branchName);
                        System.out.println();
                    }
                    else {
                        try {
                            Menu.GetEngine().DeleteBranch(branchName);
                            System.out.println();
                            System.out.format("Message: Branch named \"%s\" has successfully been deleted.", branchName);
                            System.out.println();
                        } catch (IOException e) {
                            System.out.println();
                            System.out.format("ERROR: Can't delete the branch named \"%s\".%sPlease make sure the file is closed and then try again.", branchName, System.lineSeparator());
                            System.out.println();
                        }
                    }
                }
            }
            else {
                System.out.println("ERROR: Please load repository first.");
            }

            System.out.println();
        }));
        m_Actions.add(new ActionItem("Checkout", ()-> {
            if(Menu.GetEngine().GetActiveRepository() != null) {
                IEngine engine = Menu.GetEngine();

                System.out.println("Please enter branch name:");
                System.out.print(">> ");
                Scanner scanner = new Scanner(System.in);
                String branchName = scanner.nextLine();
                System.out.println();

                if(engine.GetActiveRepository().GetHeadBranch().GetName().equals(branchName)) {
                    System.out.format("ERROR: \"%s\" branch is the head branch and therefore already been spread in the working copy.", branchName);
                    System.out.println();
                }
                else {
                    if(!engine.IsBranchNameExists(branchName)) {

                        System.out.format("ERROR: \"%s\" branch is not exists.", branchName);
                        System.out.println();
                    }
                    else {
                        try {
                            Menu.GetEngine().Checkout(branchName, true);
                            System.out.println("Message: Checkout was executed successfully.");
                        } catch (IOException e) {
                            System.out.println("ERROR: Please check if all files in the working copy are closed and try again.");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            else {
                System.out.println("ERROR: Please load repository first.");
            }

            System.out.println();
        }));
        m_Actions.add(new ActionItem("Show active branch history", ()-> {
            if(Menu.GetEngine().GetActiveRepository() != null) {
                List<String> activeBranchHistory = Menu.GetEngine().ShowActiveBranchHistory();
                String newLine = System.lineSeparator();
                int size = activeBranchHistory.size();

                if(size == 0) {
                    System.out.println("Message: The active repository is empty.");
                }
                else {
                    for(int i = 0; i < size; i++) {
                        String[] parts = activeBranchHistory.get(i).split(";");
                        System.out.format("Commit %d%ssha1: %s%scommit description: %s%sDate modified: %s%sCreator name: %s%s",
                                i + 1, newLine, parts[0], newLine, parts[1], newLine, parts[2], newLine, parts[3], newLine);
                        if(i + 1 != size) {
                            System.out.println("==============================");
                        }
                    }
                }
            }
            else {
                System.out.println("ERROR: Please load repository first.");
            }

            System.out.println();

        }));
        m_Actions.add(new ActionItem("Reset head branch", ()-> {
            if(Menu.GetEngine().GetActiveRepository() != null) {
                boolean isAbortRequest = false;
                boolean isSuccessfulReset = false;
                boolean isProceedWithRequest = false;
                int input;

                System.out.println("Please enter sha1 of the new pointed commit:");
                System.out.print(">> ");
                Scanner scanner = new Scanner(System.in);
                String newPointedCommitSha1 = scanner.nextLine();
                System.out.println();

                List<List<String>> wcStatus = Menu.GetEngine().GetWorkingCopyDelta();
                boolean isWcClean = wcStatus.get(0).size() == 0 &&
                        wcStatus.get(1).size() == 0 &&
                        wcStatus.get(2).size() == 0;

                while(!isAbortRequest && !isSuccessfulReset) {
                    if (isWcClean || isProceedWithRequest) {
                        try {
                            Menu.GetEngine().ResetHeadBranch(newPointedCommitSha1);
                            isSuccessfulReset = true;
                            System.out.println("Message: Reset branch has been executed successfuly.");
                            System.out.println();

                            List<String> commitFilesInfo = Menu.GetEngine().ShowCurrentCommitFiles();

                            if(commitFilesInfo.size() != 0) {
                                System.out.println("Desplaying the new pointed commit files:");
                                System.out.println();
                                printFilesDetails(commitFilesInfo);
                                System.out.println();
                            }
                        } catch (IOException e) {
                            System.out.println("ERROR: Commit not found");
                            System.out.println();
                            isAbortRequest = true;
                        } catch (Sha1LengthException e) {
                            System.out.println("ERROR: " + e.getMessage());
                            System.out.println();
                            isAbortRequest = true;
                        }
                    } else {
                        System.out.println("WARNING: Working copy has uncommitted files.");
                        System.out.println("Would you like to proceed with your request?");
                        System.out.println("1. Yes");
                        System.out.println("2. No");
                        input = ConsoleUtils.GetUserChoice();

                        if(input == 1) {
                            isProceedWithRequest = true;
                        }
                        else if(input == 2) {
                            isAbortRequest = true;
                            System.out.println();
                            System.out.println("Message: Request has been aborted.");
                        }

                        System.out.println();
                    }
                }
            }
            else {
                System.out.println("ERROR: Please load repository first.");
                System.out.println();
            }
        }));
        m_Actions.add(new ActionItem("Create repository in location", ()-> {
            boolean isSuccessfulLoad         = false;
            boolean isLoadDataFromRepository = false;

            System.out.println("Please enter repository location:");
            System.out.print(">> ");
            Scanner scanner = new Scanner(System.in);
            String repoLocation = scanner.nextLine();
            System.out.println();
            System.out.println("Please enter repository name:");
            System.out.print(">> ");
            String repoName = scanner.nextLine();
            System.out.println();

            while (!isSuccessfulLoad) {
                try {
                    if(!isLoadDataFromRepository) {
                        Menu.GetEngine().CreateRepositoryAndFiles(repoName, repoLocation);
                        System.out.format("Message: \"%s\" repository has been loaded successfully.", Menu.GetEngine().GetActiveRepository().GetName());
                        System.out.println();
                    }
                    else {
                        Menu.GetEngine().LoadDataFromRepository(Menu.GetEngine().GetRepositoryPath());
                        System.out.println();
                        System.out.format("Message: \"%s\" has been loaded successfully.", Menu.GetEngine().GetActiveRepository().GetName());
                        System.out.println();
                    }

                    System.out.println();
                    isSuccessfulLoad = true;
                }
                catch (RepositoryAlreadyExistsException e) {
                    try {
                        isLoadDataFromRepository = RepoAlreadyExistsExceptionHandler(repoLocation);
                        System.out.println();
                    }
                    catch (IOException ioe) {
                        System.out.println();
                        System.out.println("ERROR: " + ioe.getMessage());
                        System.out.println("Please check if all files in the directory are closed.");
                        System.out.println();
                    }
                }
                catch (FolderInLocationAlreadyExistsException e) {
                    System.out.println("ERROR: " + e.getMessage());
                    System.out.println();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));
        m_Actions.add(new ActionItem("Export repository to xml", ()-> {
            if(Menu.GetEngine().GetActiveRepository() != null) {
                System.out.println("Please enter xml path:");
                System.out.print(">> ");
                Scanner scanner = new Scanner(System.in);
                String xmlPath = scanner.nextLine();
                System.out.println();

                try {
                    Menu.GetEngine().ExportRepositoryToXml(xmlPath);
                    System.out.println("Message: Export executed successfully.");
                } catch (XmlErrorsException | RepositoryNotLoadedException e) {
                    System.out.println("ERROR: " + e.getMessage());
                }
            }
            else {
                System.out.println("ERROR: Please load repository first.");
            }

            System.out.println();
        }));
    }

    private static boolean RepoAlreadyExistsExceptionHandler(String i_Location) throws IOException {
        boolean isLoadDataFromRepository = false;

        System.out.format("WARNING: Repository already exists.%nHow do you wish to proceed?%n");
        System.out.println("1. Remove existing repository and create a new one.");
        System.out.println("2. Proceed with existing repository.");

        try {
            switch (ConsoleUtils.GetUserChoice()) {
                case 1:
                    File directory = new File(i_Location);
                    FileUtils.deleteDirectory(directory);
                    Menu.GetEngine().SetActiveRepository(null);
                    break;
                case 2:
                    isLoadDataFromRepository = true;
                    break;
                default:
                    System.out.println("ERROR: Invalid input.");
                    break;
            }
        } catch (NumberFormatException e) {
            System.out.println("ERROR: Input is not a number. Please try again.");
            System.out.println();
        }

        return isLoadDataFromRepository;
    }

    private static void printFilesDetails(List<String> i_FilesData) {
        String newLine = System.lineSeparator();
        int size = i_FilesData.size();

        for(int i = 0; i < size; i++) {
            String[] parts = i_FilesData.get(i).split(";");
            System.out.print(String.format("Item name: %s%sItem type: %s%sSha1: %s%sLast changer: %s%sLast update: %s%s",
                    parts[0], newLine, parts[2], newLine, parts[1], newLine, parts[3], newLine, parts[4], newLine));

            if(i + 1 != size) {
                System.out.println("==============================");
            }
        }
    }

    public static void addAction(ActionItem i_Action) {
        m_Actions.add(i_Action);
    }

    public static void SetActionsList(List<SubMenu> i_ActionsList) { m_Actions = i_ActionsList; }

    public static List<SubMenu> GetActions() { return m_Actions; }
}
