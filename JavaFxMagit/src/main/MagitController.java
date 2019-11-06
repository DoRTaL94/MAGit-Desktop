package main;

import MagitExceptions.*;
import com.fxgraph.graph.Graph;
import components.commit.tree.CommitGraphNode;
import components.commit.tree.CommitItemFactory;
import components.commit.tree.CommitList;
import components.commit.tree.CommitTreeLayout;
import components.conflicts.ConflictDisplays;
import components.console.ConsoleMagit;
import components.dialogs.CloneRepoDialogController;
import components.filetree.repository.RepositoryFileTree;
import components.progress.LoadXmlTask;
import components.progress.ProgressController;
import components.themes.ThemesController;
import components.warnings.WarningRepoExistsController;
import components.Diff.DiffController;
import data.structures.Branch;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import magit.CommitNode;
import magit.merge.ConflictsManager;
import magit.Engine;
import org.apache.commons.io.FileUtils;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MagitController {

    @FXML private MenuItem menuItemFileExplorer;
    @FXML private MenuItem menuItemChangeUserName;
    @FXML private MenuItem menuItemExportToXml;
    @FXML private MenuItem menuItemCommit;
    @FXML private MenuItem menuItemCreateBranch;
    @FXML private MenuItem menuItemDeleteBranch;
    @FXML private MenuItem menuItemCheckoutBranch;
    @FXML private MenuItem menuItemResetHeadBranch;
    @FXML private MenuItem menuItemMergeBranches;
    @FXML private MenuItem menuItemCheckout;
    @FXML private MenuButton menuButtonWc;
    @FXML private MenuItem menuItemWcPath;
    @FXML private MenuButton menuButtonActiveBranch;
    @FXML private MenuItem menuItemActiveBranch;
    @FXML private ChoiceBox<String> choiceBoxBranches;
    @FXML private StackPane stackPaneCommitTree;
    @FXML private Pane paneCommitTree;
    @FXML private SplitPane splitPaneFileTree;
    @FXML private HBox hBoxFileTree;
    @FXML private TextArea textAreaFileTree;
    @FXML private Tab tabWcStatus;
    @FXML private Tab tabConsole;
    @FXML private Button buttonCommit;
    @FXML private Button buttonRefresh;
    @FXML private TabPane tabPaneInfo;
    @FXML private MenuItem menuItemFetch;
    @FXML private MenuItem menuItemPull;
    @FXML private MenuItem menuItemPush;
    @FXML private VBox vBoxDiff;
    @FXML private RadioButton radioButtonPreceding1;
    @FXML private RadioButton radioButtonPreceding2;
    @FXML private BorderPane borderPaneMain;

    public static final BooleanProperty UIRefreshedProperty = new SimpleBooleanProperty(false);

    private static final String f_DarkDialog = "resources/DarkDialog.css";
    private static final String f_DarkTheme = "resources/Dark.css";
    private static final String f_ColorfulDialog = "resources/ColorfulDialog.css";
    private static final String f_ColorfulTheme = "resources/Colorful.css";

    private static final String WARNING_REPO_EXIST_FXML_PATH = "/components/warnings/WarningRepoExist.fxml";
    private final ChangeListener<Boolean> f_ChangeListener1 = this::onRadioButtonPreceding1Change;
    private final ChangeListener<Boolean> f_ChangeListener2 = this::onRadioButtonPreceding2Change;
    private final Engine f_MagitEngine;

    private Stage m_MainStage;
    private BooleanProperty m_IsLoadRepoProperty;
    private BooleanProperty m_IsCreateNewProperty;
    private BooleanProperty m_IsCancelProperty;
    private RepositoryFileTree m_Repository_FileTree;
    private Graph m_CommitTreeGraph;
    private List<CommitGraphNode> m_CommitGraphNodes;
    private CommitList m_CommitList;
    private ConsoleMagit m_ConsoleMagit;
    private DiffController m_WcDiffController;
    private DiffController m_CommitDiffController;
    private int m_SelectedPrecedingDiff;
    private String m_CurrentCommitDiffSha1;
    private String m_CurrentTheme;
    private String m_CurrentDialogTheme;
    private ThemesController m_ThemesController;
    private Map<String, CommitNode> m_TreeNodes;

    public MagitController() {
        f_MagitEngine = Engine.Creator.GetInstance();
        f_MagitEngine.repositoryChangedProperty.addListener(observable -> Platform.runLater(this::refreshAction));
        f_MagitEngine.loadedProperty.addListener(observable -> Platform.runLater(this::refreshAction));

        m_CurrentDialogTheme = "";
        m_CurrentTheme = "";
        m_SelectedPrecedingDiff = 1;
        m_Repository_FileTree = new RepositoryFileTree(textAreaFileTree);
        m_CommitTreeGraph = new Graph();
        m_CommitGraphNodes = new ArrayList<>();
        m_CommitList = new CommitList();
        m_CommitList.SetFactory(new CommitItemFactory(m_CommitTreeGraph));
        m_ConsoleMagit = new ConsoleMagit();
        m_MainStage = null;
        m_WcDiffController = DiffController.LoadFXML();
        m_CommitDiffController = DiffController.LoadFXML();
        m_ThemesController = ThemesController.LoadFXML();

        ThemesController.themeChangedProperty.addListener((observable, oldValue, newValue) -> {
            if(newValue.equals("Dark")) {
                m_CurrentTheme = f_DarkTheme;
                m_CurrentDialogTheme = f_DarkDialog;
            }
            else if(newValue.equals("Colorful")){
                m_CurrentTheme = f_ColorfulTheme;
                m_CurrentDialogTheme = f_ColorfulDialog;

            }

            if(newValue.equals("Default")) {
                m_CurrentTheme = "";
                m_CurrentDialogTheme = "";
                borderPaneMain.getStylesheets().clear();
                borderPaneMain.applyCss();
            } else {
                borderPaneMain.getStylesheets().clear();
                borderPaneMain.getStylesheets().add(getClass().getResource(m_CurrentTheme).toExternalForm());
                borderPaneMain.applyCss();
            }
        });

        Platform.runLater(() -> m_MainStage.titleProperty().bind(Bindings.format("Magit - %s", f_MagitEngine.currentNameProperty)));
    }

    @FXML public void initialize() {
        stackPaneCommitTree.getChildren().add(m_CommitList);
        m_CommitList.toBack();
        tabConsole.setContent(m_ConsoleMagit);
        splitPaneFileTree.visibleProperty().bind(f_MagitEngine.loadedProperty);
        tabWcStatus.setContent(m_WcDiffController.GetRoot());
        m_WcDiffController.GetRoot().visibleProperty().bind(f_MagitEngine.loadedProperty);
        HBox CommitDiffRoot = m_CommitDiffController.GetRoot();
        VBox.setVgrow(CommitDiffRoot, Priority.ALWAYS);
        vBoxDiff.getChildren().add(CommitDiffRoot);
        vBoxDiff.setVisible(false);

        radioButtonPreceding1.selectedProperty().addListener(f_ChangeListener1);
        radioButtonPreceding2.selectedProperty().addListener(f_ChangeListener2);
        radioButtonPreceding1.selectedProperty().addListener((observable, oldValue, newValue) -> {if(newValue){radioButtonPreceding2.setSelected(false);}});
        radioButtonPreceding2.selectedProperty().addListener((observable, oldValue, newValue) -> {if(newValue){radioButtonPreceding1.setSelected(false);}});

        menuItemFileExplorer.disableProperty().bind(f_MagitEngine.loadedProperty.not());
        menuItemChangeUserName.disableProperty().bind(f_MagitEngine.loadedProperty.not());
        menuItemExportToXml.disableProperty().bind(f_MagitEngine.loadedProperty.not());
        menuItemCommit.disableProperty().bind(f_MagitEngine.loadedProperty.not());
        menuItemCreateBranch.disableProperty().bind(f_MagitEngine.loadedProperty.not());
        menuItemDeleteBranch.disableProperty().bind(f_MagitEngine.loadedProperty.not());
        menuItemCheckoutBranch.disableProperty().bind(f_MagitEngine.loadedProperty.not());
        menuItemResetHeadBranch.disableProperty().bind(f_MagitEngine.loadedProperty.not());
        menuItemMergeBranches.disableProperty().bind(f_MagitEngine.loadedProperty.not());
        menuItemCheckout.disableProperty().bind(f_MagitEngine.loadedProperty.not());
        buttonCommit.disableProperty().bind(f_MagitEngine.loadedProperty.not());
        buttonRefresh.disableProperty().bind(f_MagitEngine.loadedProperty.not());
        menuItemPush.disableProperty().bind(f_MagitEngine.remoteRepositoryClonedProperty.not());
        menuItemPull.disableProperty().bind(f_MagitEngine.remoteRepositoryClonedProperty.not());
        menuItemFetch.disableProperty().bind(f_MagitEngine.remoteRepositoryClonedProperty.not());
    }

    public void SetStage(Stage i_Stage) {
        m_MainStage = i_Stage;
    }

    @FXML void changeUserNameAction() {
        if(f_MagitEngine.GetActiveRepository() != null) {
            showTextInputDialog("Change User Name", "", "Change User Name:", f_MagitEngine::SetCurrentUserName);
        }
        else {
            alertRepositoryNotLoaded();
        }
    }

    private void showTextInputDialog(String i_Title, String i_HeaderText, String i_ContentText, Consumer<String> i_DoWhenTextEntered) {
        Dialog<String> dialog = new Dialog<>();
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().removeAll();

        if(!m_CurrentDialogTheme.isEmpty()) {
            dialogPane.getStylesheets().add(getClass().getResource(m_CurrentDialogTheme).toExternalForm());
            dialogPane.applyCss();
        }

        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.getIcons().add(new Image(getClass().getResourceAsStream("resources/MAGit.png")));

        dialog.setTitle(i_Title);
        dialog.setHeaderText(i_HeaderText);
        Label labelRepoName = new Label(i_ContentText);
        ButtonType buttonTypeOK = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(buttonTypeOK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField repoName = new TextField();
        grid.add(labelRepoName, 0, 0);
        grid.add(repoName, 1, 0);

        Node buttonOK = dialog.getDialogPane().lookupButton(buttonTypeOK);
        buttonOK.setDisable(true);

        repoName.textProperty().addListener((observable, oldValue, newValue) -> {
            buttonOK.setDisable(newValue.trim().isEmpty());
        });

        dialogPane.setContent(grid);

        Platform.runLater(repoName::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == buttonTypeOK) {
                return repoName.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(i_DoWhenTextEntered);
    }

    private void alertRepositoryNotLoaded() {
        Alert("Magit Error", "", "Repository wan't been loaded", Alert.AlertType.ERROR);
    }

    @FXML void checkoutAction() {
        if(f_MagitEngine.GetActiveRepository() != null) {
            showTextInputDialog("Checkout", "", "Please enter branch name:", branchName -> checkoutActionWrapper(branchName, false));
        }
        else {
            alertRepositoryNotLoaded();
        }
    }

    private void checkoutActionWrapper(String i_BranchName, boolean i_IsSkipWcCheck) {
        try {
            if (f_MagitEngine.GetActiveRepository().GetHeadBranch().GetName().equals(i_BranchName)) {
                Alert("Magit Error", "",
                        String.format("Branch named %s is head branch and therefore already been spreaded.", i_BranchName), Alert.AlertType.ERROR);
            } else if (!f_MagitEngine.GetActiveRepository().GetBranches().containsKey(i_BranchName)) {
                Alert("Magit Error", "",
                        String.format("Branch named %s is not exists.", i_BranchName), Alert.AlertType.ERROR);
            } else {
                f_MagitEngine.Checkout(i_BranchName, i_IsSkipWcCheck);
            }
        } catch (IOException e) {
            Alert("Directory Error", "Cannot checkout branch",
                    "Please check if all files in the working directory are closed and try again.", Alert.AlertType.ERROR);
        } catch (OpenChangesInWcException e) {
            refreshAction();
            tabPaneInfo.getSelectionModel().select(tabWcStatus);
            yesNoDialog("Checkout Warning", "Open changed in working copy", "Do you wish to proceed?",
                    () -> checkoutActionWrapper(i_BranchName, true), null);
        } catch (Exception e) {
            yesNoDialog("Checkout Warning", "Attempt to perform checkout on remote branch",
                    String.format("%s%s%s", e.getMessage(), System.lineSeparator(), "Do you wish to create a remote tracking branch?"),
                    () -> f_MagitEngine.CreateRTB(i_BranchName), null);
        }
    }

    @FXML void commitAction() {
        if(f_MagitEngine.GetActiveRepository() != null) {
            showTextInputDialog("Commit", "Performs commit on the corrent working directory",
                    "Please enter commit description:", this::commitWrapper);
        }
        else {
            alertRepositoryNotLoaded();
        }
    }

    private void commitWrapper(String i_CommitDescription) {
        try {
            boolean isCommitExecuted = f_MagitEngine.Commit(i_CommitDescription, null);

            if(isCommitExecuted) {
                Alert("Successful Operation", "",
                        "Commit was executed successfully", Alert.AlertType.INFORMATION);
            }
            else {
                Alert("Information Dialog", "",
                        "Working directory wasn't been changed since last commit.", Alert.AlertType.INFORMATION);
            }

        } catch (IOException e) {
            Alert("Directory Error", "Commit couldn't been executed",
                    "Please check if all files in the working directory are closed and try again.", Alert.AlertType.ERROR);
        } catch (EmptyWcException e) {
            Alert("Information Dialog", "", e.getMessage(), Alert.AlertType.INFORMATION);
        } catch (CommitAlreadyExistsException e) {
            exceptionHandler(e);
        }
    }

    @FXML void createBranchAction() {
        if(f_MagitEngine.GetActiveRepository() != null) {
            showTextInputDialog("Create Branch", "", "Please enter branch name:", this::createBranchWrapper);
        }
        else {
            alertRepositoryNotLoaded();
        }
    }

    private void createBranchWrapper(String i_BranchName) {
        try {
            if (!f_MagitEngine.IsBranchNameExists(i_BranchName)) {
                f_MagitEngine.CreateNewBranch(i_BranchName);
                yesNoDialog("Successful Operation", "",
                        String.format("%s%s%s","Branch has been created successfully", System.lineSeparator(), "Do you wish to perform checkout?"),
                        () -> checkoutActionWrapper(i_BranchName, false), null);
            }
            else {
                Alert("Magit Error", "",
                        String.format("Branch named %s is already exists.", i_BranchName), Alert.AlertType.ERROR);
            }
        } catch (PointedCommitEmptyException e) {
            Alert("Magit Error", e.getLocalizedMessage(),
                    " Please perform commit before creating a new branch.", Alert.AlertType.ERROR);
        }
    }

    private void yesNoDialog(String i_Title, String i_HeaderText, String i_ContentText, Runnable i_YesAction, Runnable i_NoAction) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(i_Title);
        alert.setHeaderText(i_HeaderText);
        alert.setContentText(i_ContentText);

        DialogPane dialogPane = alert.getDialogPane();

        if(!m_CurrentDialogTheme.isEmpty()) {
            dialogPane.getStylesheets().add(getClass().getResource(m_CurrentDialogTheme).toExternalForm());
            dialogPane.applyCss();
        }

        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.getIcons().add(new Image(getClass().getResourceAsStream("resources/MAGit.png")));

        alert.setOnCloseRequest(e -> stage.close());

        ButtonType buttonNo;

        if(i_NoAction == null){
            buttonNo = ButtonType.CANCEL;
        }
        else {
            buttonNo = new ButtonType("No");
        }

        alert.getButtonTypes().set(1, buttonNo);
        Optional<ButtonType> result = alert.showAndWait();

        if (result.get().equals(ButtonType.OK)){
            i_YesAction.run();
        }
        else if (result.get() == buttonNo) {
            if(i_NoAction != null) {
                i_NoAction.run();
            }
        }
    }

    @FXML void createRepoAction() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose Where To Create");
        File choice = directoryChooser.showDialog(m_MainStage);

        if (choice != null) {
            showTextInputDialog("Create Repository", "Directory name is required", "Enter directory name:", name -> createRepoActionWrapper(name, choice.toPath().toString()));
        }
    }

    public void createRepoActionWrapper(String i_DirectoryName, String i_DirectoryLocation) {
        if(!i_DirectoryName.isEmpty()) {
            showTextInputDialog("Repository Created", "Repository was created successfuly",
                    "Please enter repository name:", repoName -> createRepo(repoName, i_DirectoryName, i_DirectoryLocation));
        }
    }

    public void createRepo(String i_RepoName, String i_DirectoryName, String i_Location) {
        if(!i_RepoName.isEmpty()) {
            boolean isCreate = createRepository(i_DirectoryName, i_DirectoryName, i_Location);

            if(isCreate) {
                f_MagitEngine.GetActiveRepository().SetName(i_RepoName);
            }
        }
    }

    private boolean createRepository(String i_RepoName, String i_DirectoryName, String i_Location) {
        Stage warningStage = new Stage();
        boolean isCreate = false;
        String path = Paths.get(i_Location, i_DirectoryName).toString();

        try {
            f_MagitEngine.CreateRepositoryAndFiles(i_RepoName, path);
            isCreate = true;
        } catch (RepositoryAlreadyExistsException e) {
            repositoryAlreadyExistsHandler(path, null,
                    () -> deleteAndCreateEmptyRepo(i_RepoName, i_DirectoryName, i_Location, warningStage), warningStage);
        } catch (FolderInLocationAlreadyExistsException e) {
            alertFolderAlreadyExists(e.getMessage());
        }

        return isCreate;
    }

    private void deleteAndCreateEmptyRepo(String i_RepoName, String i_DirectoryName, String i_Location, Stage i_Stage) {
        cancelAction(i_Stage);
        String path = Paths.get(i_Location, i_DirectoryName).toString();

        try {
            FileUtils.deleteDirectory(new File(path));
            createRepo(i_RepoName ,i_DirectoryName, i_Location);
        } catch (IOException e) {
            alertDirectoryCouldntBeenRemoved(e.getMessage());
        }
    }

    @FXML void deleteBranchAction() {
        if(f_MagitEngine.GetActiveRepository() != null) {
            showTextInputDialog("Delete Branch", "", "Enter branch name:", this::deleteBranchWrapper);
        }
        else {
            alertRepositoryNotLoaded();
        }
    }

    private void deleteBranchWrapper(String i_BranchName) {
        try {
            if (!f_MagitEngine.IsBranchNameExists(i_BranchName)) {
                Alert("Magit Error", "",
                        String.format("Branch named %s is not exists.", i_BranchName), Alert.AlertType.ERROR);
            }
            else if(f_MagitEngine.GetActiveRepository().GetHeadBranch().GetName().equals(i_BranchName)) {
                Alert("Magit Error", "",
                        String.format("Branch named %s is head branch and therefore cannot be deleted.", i_BranchName), Alert.AlertType.ERROR);
            }
            else {
                f_MagitEngine.DeleteBranch(i_BranchName);
            }
        } catch (IOException e) {
            Alert("Magit Error", "",
                    String.format("Can't delete the branch named %s.%sPlease make sure the file is closed and then try again.",
                            System.lineSeparator(), i_BranchName), Alert.AlertType.ERROR);
        }
    }

    @FXML void exitAction() {
        m_MainStage.close();
    }

    @FXML void loadRepoFromXmlAction() {
        loadRepoFromXml(null);
    }

    private void loadRepoFromXml(String i_XmlLocation) {
        Stage warningStage = new Stage();
        File file = null;

        if(i_XmlLocation == null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose Xml File");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml"));
            file = fileChooser.showOpenDialog(m_MainStage);
        }

        String xmlLocation = file == null ? i_XmlLocation : file.toPath().toString();

        if(xmlLocation != null) {
            ProgressController progressController = ProgressController.LoadFXML();
            LoadXmlTask loadXmlTask = new LoadXmlTask(xmlLocation);
            progressController.SetTask(loadXmlTask);

            loadXmlTask.FolderInLocationAlreadyExistsProperty.addListener((observable, oldValue, newValue) ->
                    Platform.runLater(() -> alertFolderAlreadyExists(newValue)));
            loadXmlTask.RepositoryAlreadyExistsProperty.addListener(observable ->
                    Platform.runLater(() -> repositoryAlreadyExistsHandler(f_MagitEngine.GetRepositoryPath(), xmlLocation, () ->
                            deleteAndCreateNewRepo(f_MagitEngine.GetRepositoryPath(), xmlLocation, warningStage), warningStage)));
            loadXmlTask.XmlErrorProperty.addListener(observable ->
                    Platform.runLater(() -> xmlErrorHandler(loadXmlTask.GetXmlError())));

            progressController.Run();
        }
    }

    private void alertFolderAlreadyExists(String i_ErrorMessage) {
        Alert("Directory Error", "Directory name is taken", i_ErrorMessage, Alert.AlertType.ERROR);
    }

    private void repositoryAlreadyExistsHandler(String i_RepoLocation, String i_XmlLocation, Runnable i_DeleteAndCreateNewAction, Stage i_WarningStage) {
        FXMLLoader fxmlLoader = new FXMLLoader();
        URL url               = getClass().getResource(WARNING_REPO_EXIST_FXML_PATH);

        fxmlLoader.setLocation(url);
        m_IsCancelProperty = new SimpleBooleanProperty(false);
        m_IsCreateNewProperty = new SimpleBooleanProperty(false);
        m_IsLoadRepoProperty = new SimpleBooleanProperty(false);

        try {
            VBox root = fxmlLoader.load(url.openStream());
            WarningRepoExistsController controller = fxmlLoader.getController();

            controller.SetIsCancelProperty(m_IsCancelProperty);
            controller.SetIsCreateNewProperty(m_IsCreateNewProperty);
            controller.SetIsLoadRepoProperty(m_IsLoadRepoProperty);

            m_IsCancelProperty.addListener((observable, oldValue, newValue) ->
                    cancelAction(i_WarningStage));
            m_IsCreateNewProperty.addListener((observable, oldValue, newValue) ->
                    i_DeleteAndCreateNewAction.run());
            m_IsLoadRepoProperty.addListener((observable, oldValue, newValue) ->
                    loadExistingRepo(i_RepoLocation, i_WarningStage));

            if(!m_CurrentDialogTheme.isEmpty()) {
                root.getStylesheets().add(getClass().getResource(m_CurrentDialogTheme).toExternalForm());
                root.applyCss();
            }

            Scene mainScene = new Scene(root);
            i_WarningStage.setScene(mainScene);
            i_WarningStage.setTitle("Warning");
            i_WarningStage.getIcons().add(new Image(getClass().getResourceAsStream("resources/MAGit.png")));
            i_WarningStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadExistingRepo(String i_RepoLocation, Stage i_Stage) {
        cancelAction(i_Stage);
        loadRepository(i_RepoLocation);
    }

    private void loadRepository(String i_Location) {
        try {
            f_MagitEngine.ChangeActiveRepository(i_Location);
        } catch (IOException e) {
            alertRepositoryCorrupted(e.getMessage());
            e.printStackTrace();
        } catch (NotRepositoryFolderException e) {
            alertDirectoryIsNotMagit(e.getMessage());
        }
    }

    private void alertRepositoryCorrupted(String i_ErrorMessage) {
        Alert("Magit Error", "Magit is corrupted", i_ErrorMessage, Alert.AlertType.ERROR);
    }

    private void deleteAndCreateNewRepo(String i_RepoLocation, String i_XmlLocation, Stage i_Stage) {
        cancelAction(i_Stage);
        try {
            FileUtils.deleteDirectory(new File(i_RepoLocation));
            loadRepoFromXml(i_XmlLocation);
        } catch (IOException e) {
            alertDirectoryCouldntBeenRemoved(e.getMessage());
        }
    }

    private void Alert(String i_Title, String i_HeaderText, String i_ContentText, Alert.AlertType i_AlertType) {
        Alert alert = new Alert(i_AlertType);

        DialogPane dialogPane = alert.getDialogPane();
        if(!m_CurrentDialogTheme.isEmpty()) {
            dialogPane.getStylesheets().add(getClass().getResource(m_CurrentDialogTheme).toExternalForm());
            dialogPane.applyCss();
        }

        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.getIcons().add(new Image(getClass().getResourceAsStream("resources/MAGit.png")));

        alert.setTitle(i_Title);
        alert.setHeaderText(i_HeaderText);
        alert.setContentText(i_ContentText);

        alert.showAndWait();
    }

    private void alertDirectoryIsNotMagit(String i_ErrorMessage) {
        Alert("Magit Error", "Not a magit repository", i_ErrorMessage, Alert.AlertType.ERROR);
    }

    private void alertDirectoryCouldntBeenRemoved(String i_ErrorMessage) {
        Alert("Directory Error", "Directory couldn't been removed",
                String.format("%s%s%s", i_ErrorMessage, System.lineSeparator(),
                        "Please check if all files in the directory are closed."), Alert.AlertType.ERROR);
    }

    private void cancelAction(Stage i_Stage) {
        i_Stage.close();
    }

    private void xmlErrorHandler(List<String> i_ErrorMessages) {
        StringBuilder sb = new StringBuilder();
        i_ErrorMessages.forEach(sb::append);
        customExceptionHandler(sb.toString(),
                "Error in xml file",
                "The given xml structure is not valid",
                "Xml errors are as followed:");
    }

    private void exceptionHandler(Exception i_Exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        i_Exception.printStackTrace(pw);
        String exceptionText = sw.toString();

        customExceptionHandler(exceptionText, "Error", "An error has occured", "The exception stacktrace was:");
    }

    private void customExceptionHandler(String i_Message, String i_Title, String i_HeaderText, String i_ExceptionLabel) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(i_Title);
        alert.setHeaderText(i_HeaderText);

        Label label = new Label(i_ExceptionLabel);

        TextArea textArea = new TextArea(i_Message);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);

        alert.show();
    }

    @FXML private void refreshAction() {
        UIRefreshedProperty.set(false);

        if(f_MagitEngine.GetActiveRepository() != null) {
            clearGraphics();
            menuButtonWc.setText(f_MagitEngine.GetRepositoryPath());
            menuItemWcPath.setText(f_MagitEngine.GetRepositoryPath());

            menuButtonActiveBranch.setText(f_MagitEngine.GetActiveRepository().GetHeadBranch().GetName());
            menuItemActiveBranch.setText(f_MagitEngine.GetActiveRepository().GetHeadBranch().GetName());

            choiceBoxBranches.getItems().clear();
            choiceBoxBranches.setValue(f_MagitEngine.GetActiveRepository().GetHeadBranch().GetName());

            for (Map.Entry<String, Branch> branch : f_MagitEngine.GetActiveRepository().GetBranches().entrySet()) {
                choiceBoxBranches.getItems().add(branch.getValue().GetName());
            }

            updateFileTree();
            updateCommitTree();
            updateWcStatus();

            UIRefreshedProperty.set(true);
        }
    }

    public void clearGraphics() {
        m_CommitList.getChildren().clear();
        m_CommitTreeGraph = new Graph();
        m_CommitList.SetFactory(new CommitItemFactory(m_CommitTreeGraph));
        m_CommitGraphNodes.clear();
        paneCommitTree.getChildren().clear();
        choiceBoxBranches.getItems().clear();

        choiceBoxBranches.setValue("N/A");
        menuButtonActiveBranch.setText("N/A");
        menuButtonWc.setText("N/A");
        menuItemWcPath.setText("N/A");
    }

    private void onRadioButtonPreceding1Change(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean aBoolean1) {
        if(aBoolean1) {
            m_SelectedPrecedingDiff = 1;
            loadCommitDiff(m_CurrentCommitDiffSha1);
        }
    }

    private void onRadioButtonPreceding2Change(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean aBoolean1) {
        if(aBoolean1) {
            m_SelectedPrecedingDiff = 2;
            loadCommitDiff(m_CurrentCommitDiffSha1);
        }
    }

    private void updateCommitTree() {
        CommitNode root = buildTree();

        if(root != null) {
            CommitGraphNode graphNodeRoot = initializeCommitGraphNodes(root);
            addTreeToList();

            CommitTreeLayout treeLayout = new CommitTreeLayout(graphNodeRoot);
            m_CommitTreeGraph.layout(treeLayout);
            m_CommitTreeGraph.getModel().addCell(graphNodeRoot);
            paneCommitTree.getChildren().add(m_CommitTreeGraph.getCanvas());
            m_CommitTreeGraph.endUpdate();

            for(CommitGraphNode graphNode: m_CommitGraphNodes) {
                graphNode.SetClickAction(() -> {
                    radioButtonPreceding1.setSelected(true);
                    radioButtonPreceding2.setSelected(false);
                    loadCommitDiff(graphNode.GetSha1());
                    updateGraphicsToHighlightBranch(graphNode);
                });
            }

            Platform.runLater(() -> {
                m_CommitTreeGraph.getUseNodeGestures().set(false);
                m_CommitTreeGraph.getUseViewportGestures().set(false);
            });
        }
    }

    private void updateGraphicsToHighlightBranch(CommitGraphNode i_GraphNode) {
        for(CommitGraphNode graphNode: m_CommitGraphNodes) {
            boolean isHeadBranch = isHeadBranch(graphNode.GetPointingBranches());
            boolean isOnBranch = false;
            List<Branch> onBranches = graphNode.GetCommitNode().GetOnBranches();

            for (Branch onBranch : i_GraphNode.GetCommitNode().GetOnBranches()) {
                if(onBranches.contains(onBranch) && !isHeadBranch) {
                    isOnBranch = true;
                    break;
                }
            }
            if(isOnBranch) {
                graphNode.SetRectangleTreeNodeId("branchSelectedTreeNode");
            } else if(!isHeadBranch) {
                graphNode.SetRectangleTreeNodeId("rectangleTreeNode");
            }
        }
    }

    public boolean isHeadBranch(List<Branch> i_PointingBranches) {
        boolean isHead = false;

        for(Branch branch: i_PointingBranches) {
            if(branch.GetName().equals(f_MagitEngine.GetActiveRepository().GetHeadBranch().GetName())) {
                isHead = true;
                break;
            }
        }

        return isHead;
    }

    private void loadCommitDiff(String i_CommitSha1) {
        m_CurrentCommitDiffSha1 = i_CommitSha1;
        List<List<List<String>>> diff = f_MagitEngine.GetCommitDiff(i_CommitSha1);

        m_CommitDiffController.GetListViewNewFiles().getItems().clear();
        m_CommitDiffController.GetListViewDeletedFiles().getItems().clear();
        m_CommitDiffController.GetListViewChangedFiles().getItems().clear();

        if(diff != null) {
            vBoxDiff.setVisible(true);

            List<List<String>> precedingDiff = diff.get(m_SelectedPrecedingDiff - 1);
            radioButtonPreceding1.setDisable(false);
            radioButtonPreceding2.setDisable(diff.size() < 2);

            List<String> deletedFiles = precedingDiff.get(0);
            List<String> newFiles = precedingDiff.get(1);
            List<String> changedFiles = precedingDiff.get(2);

            for (String file : newFiles) {
                m_CommitDiffController.GetListViewNewFiles().getItems().add(file);
            }

            for (String file : deletedFiles) {
                m_CommitDiffController.GetListViewDeletedFiles().getItems().add(file);
            }

            for (String file : changedFiles) {
                m_CommitDiffController.GetListViewChangedFiles().getItems().add(file);
            }
        }
        else {
            radioButtonPreceding1.setDisable(true);
            radioButtonPreceding2.setDisable(true);
            vBoxDiff.setVisible(false);
        }
    }

    private void updateWcStatus() {
        List<List<String>> wcStatus = f_MagitEngine.GetWorkingCopyDelta();
        List<String> deletedFiles = wcStatus.get(0);
        List<String> newFiles = wcStatus.get(1);
        List<String> changedFiles = wcStatus.get(2);

        m_WcDiffController.GetListViewNewFiles().getItems().clear();
        m_WcDiffController.GetListViewDeletedFiles().getItems().clear();
        m_WcDiffController.GetListViewChangedFiles().getItems().clear();

        for(String file: newFiles) {
            m_WcDiffController.GetListViewNewFiles().getItems().add(file);
        }

        for(String file: deletedFiles) {
            m_WcDiffController.GetListViewDeletedFiles().getItems().add(file);
        }

        for(String file: changedFiles) {
            m_WcDiffController.GetListViewChangedFiles().getItems().add(file);
        }
    }

    private void addTreeToList() {
        //m_CommitGraphNodes.sort(CommitGraphNode::compareTo);
        Object[] array = m_CommitGraphNodes.toArray();
        sort(array, 0, m_CommitGraphNodes.size() - 1);
        m_CommitGraphNodes = Arrays.stream(array).map(o -> (CommitGraphNode) o).collect(Collectors.toList());

        for(int node = 0; node < m_CommitGraphNodes.size(); node++) {
            m_CommitList.AddChild(m_CommitGraphNodes.get(node));
            m_CommitGraphNodes.get(node).SetIdInList(node);
        }
    }

    private int partition(Object[] arr, int low, int high) {
        Object pivot = arr[high];
        int i = low - 1;

        for (int j = low; j < high; j++) {
            try {
                CommitGraphNode pivotNode = (CommitGraphNode) pivot;
                CommitGraphNode nodeToCheck = (CommitGraphNode) arr[j];

                long pivotTime = new SimpleDateFormat(Engine.DATE_FORMAT).parse(pivotNode.GetCommit().GetLastUpdate()).getTime();
                long nodeToCheckTime = new SimpleDateFormat(Engine.DATE_FORMAT).parse(nodeToCheck.GetCommit().GetLastUpdate()).getTime();

                if (pivotTime - nodeToCheckTime < 0) {
                    i++;
                    Object temp = arr[i];
                    arr[i] = arr[j];
                    arr[j] = temp;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        Object temp = arr[i + 1];
        arr[i + 1] = arr[high];
        arr[high] = temp;

        return i + 1;
    }

    private void sort(Object[] arr, int low, int high) {
        if (low < high) {
            int pi = partition(arr, low, high);
            sort(arr, low, pi - 1);
            sort(arr, pi + 1, high);
        }
    }

    private CommitGraphNode initializeCommitGraphNodes(CommitNode i_Root) {
        boolean isGraphNodeExists = false;
        CommitGraphNode graphNode = null;

        for(CommitGraphNode node: m_CommitGraphNodes) {
            if(node.GetCommitNode().equals(i_Root)) {
                graphNode = node;
                isGraphNodeExists = true;
                break;
            }
        }

        if(!isGraphNodeExists) {
            graphNode = new CommitGraphNode(i_Root);
            m_CommitGraphNodes.add(graphNode);

            if(i_Root.GetChildren().size() == 0) {
                return graphNode;
            }

            for(CommitNode node: i_Root.GetChildren()) {
                CommitGraphNode graphNodeChild = initializeCommitGraphNodes(node);
                graphNodeChild.GetGraphNodeParents().add(graphNode);
                graphNode.AddGraphNodeChild(graphNodeChild);
            }
        }

        return graphNode;
    }

    private CommitNode buildTree() {
        m_TreeNodes = new HashMap<>();
        CommitNode leaf = null;

        for(Map.Entry<String, Branch> branchEntry: f_MagitEngine.GetActiveRepository().GetBranches().entrySet()) {
            leaf = f_MagitEngine.BuildTree(m_TreeNodes, branchEntry.getValue());
        }

        return f_MagitEngine.FindRoot(leaf);
    }

    private void updateFileTree() {
        hBoxFileTree.getChildren().clear();
        m_Repository_FileTree = new RepositoryFileTree(textAreaFileTree);
        m_Repository_FileTree.CreateTreeFromFile(new File(f_MagitEngine.GetRepositoryPath()));
        HBox.setHgrow(m_Repository_FileTree, Priority.ALWAYS);
        hBoxFileTree.getChildren().add(m_Repository_FileTree);
    }

    @FXML private void mergeBranchesAction() {
        if(f_MagitEngine.GetActiveRepository() != null) {
            showTextInputDialog("Merge", "Merge selected branch with head branch",
                    "Enter branch name:", this::mergeBranchesActionWrapper);
        }
        else {
            alertRepositoryNotLoaded();
        }
    }

    private void mergeBranchesActionWrapper(String i_BranchName) {
        // אם שם הענף לא ריק וגם הוא קיים ברשימת ענפים
        if(!i_BranchName.isEmpty() && f_MagitEngine.GetActiveRepository().GetBranches().containsKey(i_BranchName)) {
            Branch headBranch = f_MagitEngine.GetActiveRepository().GetHeadBranch();
            Branch branchToMerge = f_MagitEngine.GetActiveRepository().GetBranches().get(i_BranchName);

            // מנהל את כל תהליך הקונפליקטים כדי שהClient לא יצטרך להתעסק עם לוגיקה
            ConflictsManager conflictsManager = f_MagitEngine.MergeBranches(headBranch, branchToMerge,
                    this::getCommitDescriptionAction, this::fastForwardMergePopup, this::mergeExceptionMessagePopup);

            if (conflictsManager != null) {
                // נציג את החלון של ניהול הקונפליקטים, נעביר לו את מנהל הקונפליקטים
                ConflictDisplays conflictDisplays = new ConflictDisplays(conflictsManager);
                conflictDisplays.Show();
            }
        } else {
            Alert("Merge Error", "Something went wrong...",
                    "Branch name doesn't exist.", Alert.AlertType.ERROR);
        }
    }

    private void mergeExceptionMessagePopup(String i_Message) {
        Alert("Merge Exception", "Something went wrong...",
                i_Message, Alert.AlertType.ERROR);
    }

    private void fastForwardMergePopup() {
        Alert("Merge Information", "Merge was executed successfully.",
                "A fast forward merge was executed.", Alert.AlertType.INFORMATION);
    }

    private void getCommitDescriptionAction(Consumer<String> i_SetCommitDescriptionAction) {
        showTextInputDialog("Commit", "Performs commit on the corrent working directory",
                "Please enter commit description:", i_SetCommitDescriptionAction);
    }

    @FXML private void openFileExplorerAction() {
        if(f_MagitEngine.GetActiveRepository() != null) {
            try {
                Desktop.getDesktop().open(new File(f_MagitEngine.GetRepositoryPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML private void openRepositoryAction() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File directory = directoryChooser.showDialog(m_MainStage);

        if(directory != null) {
            String directoryPath = directory.getAbsolutePath();
            loadRepository(directoryPath);
        }
    }

    @FXML private void exportToXmlAction() {
        if(f_MagitEngine.GetActiveRepository() != null) {
            showTextInputDialog("Export Repository To Xml", "", "Please enter xml full path:", this::exportRepositoryToXmlActionWrapper);
        }
        else {
            alertRepositoryNotLoaded();
        }
    }

    private void exportRepositoryToXmlActionWrapper(String i_XmlFullPath) {
        try {
            f_MagitEngine.ExportRepositoryToXml(i_XmlFullPath);
        } catch (XmlErrorsException | RepositoryNotLoadedException e) {
            exceptionHandler(e);
        }
    }

    @FXML private void resetHeadBranchAction() {
        if(f_MagitEngine.GetActiveRepository() != null) {
            showTextInputDialog("Reset Head Branch", "", "Please enter commit sha1:", this::resetHeadBranchActionWrapper);
        }
        else {
            alertRepositoryNotLoaded();
        }
    }

    private void resetHeadBranchActionWrapper(String i_CommitSha1) {
        try {
            f_MagitEngine.ResetHeadBranch(i_CommitSha1);
        } catch (IOException e) {
            Alert("Magit Error", "Cannot reset head branch", "Input is not a commit sha1.", Alert.AlertType.ERROR);
        } catch (Sha1LengthException e) {
            Alert("Magit Error", "Cannot reset head branch",
                    "Commit sha1 length supposed to be 40 characters length.", Alert.AlertType.ERROR);
        }
    }

    @FXML private void cloneAction() {
        CloneRepoDialogController cloneDialogController = CloneRepoDialogController.LoadFxml();
        cloneDialogController.SetOkAction(() -> cloneActionWrapper(cloneDialogController));
        cloneDialogController.ShowDialog();
    }

    private void cloneActionWrapper(CloneRepoDialogController i_CloneDialogController) {
        String repoName = i_CloneDialogController.GetTextFieldRepoName().getText();
        String localRepoLocation  = i_CloneDialogController.GetTextFieldLocalRepoPath().getText();
        String remoteRepoLocation = i_CloneDialogController.GetTextFieldRemoteRepoPath().getText();

        try {
            f_MagitEngine.Clone(repoName, localRepoLocation, remoteRepoLocation);
        } catch (IOException e) {
            exceptionHandler(e);
        } catch (CollaborationException e) {
            Alert("Clone Error", "Clone failed", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML private void pushAction() {
        try {
            f_MagitEngine.Push();
            Alert("Collaboration Message", "Successful operation", "Push was executed successfully.", Alert.AlertType.INFORMATION);
        } catch (CollaborationException e) {
            Alert("Collaboration Error", "Something went wrong...", e.getMessage(), Alert.AlertType.ERROR);
        } catch (IOException e) {
            exceptionHandler(e);
        }
    }

    @FXML private void pullAction() {
        try {
            f_MagitEngine.Pull();
            Alert("Collaboration Message", "Successful operation", "Pull was executed successfully.", Alert.AlertType.INFORMATION);
        } catch (OpenChangesInWcException e) {
            Alert("Collaboration Error", "Something went wrong...", "Remote repository working directory is not clean.", Alert.AlertType.ERROR);
        } catch (CollaborationException e) {
            Alert("Collaboration Error", "Something went wrong...", e.getMessage(), Alert.AlertType.ERROR);
        } catch (Exception e) {
            exceptionHandler(e);
        }
    }

    @FXML private void fetchAction() {
        f_MagitEngine.Fetch();
        Alert("Collaboration Message", "Successful operation", "Fetch was executed successfully.", Alert.AlertType.INFORMATION);
    }

    @FXML public void changeThemeAction() {
        m_ThemesController.Show();
    }
}
