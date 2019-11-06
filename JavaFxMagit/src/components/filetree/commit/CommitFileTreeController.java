package components.filetree.commit;

import components.themes.ThemesController;
import data.structures.Folder;
import data.structures.Repository;
import data.structures.eFileType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import magit.Engine;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import javafx.scene.control.Label;

public class CommitFileTreeController {

    @FXML private Label labelCommitSha1;
    @FXML private Button buttonCopyCommitSha1;
    @FXML private HBox hBoxCommitFiles;
    @FXML private TextArea textAreaCommitFileContent;
    @FXML private Label labelFileSha1;
    @FXML private Button buttonCopyFileSha1;
    @FXML private Label labelLastUpdateDate;
    @FXML private Label labelLastUpdater;
    @FXML private VBox vBoxRoot;

    private static final String CLIPBOARD_IMAGE_PATH = "/components/filetree/commit/resources/clipboard.png";
    private static final double sf_CopyButtonOpacity = 0.6;
    private static final double sf_ClipboardImgSize = 20;

    private final Repository f_ActiveRepository =  Engine.Creator.GetInstance().GetActiveRepository();

    @FXML public void initialize() {
        buttonCopyFileSha1.setGraphic(createGraphics());
        buttonCopyFileSha1.setOpacity(sf_CopyButtonOpacity);
        buttonCopyCommitSha1.setGraphic(createGraphics());
        buttonCopyCommitSha1.setOpacity(sf_CopyButtonOpacity);
    }

    private ImageView createGraphics() {
        ImageView clipboardImg = new ImageView(CLIPBOARD_IMAGE_PATH);

        clipboardImg.setFitHeight(sf_ClipboardImgSize);
        clipboardImg.setFitWidth(sf_ClipboardImgSize);

        return clipboardImg;
    }

    public void SetCommitFilesTree(CommitFileTree i_CommitFileTree) {
        setCommitFilesTreeSelectedItemAction(i_CommitFileTree);
        setCommitFilesTreeFactory(i_CommitFileTree);
        setTreeView(i_CommitFileTree);
    }

    private void setCommitFilesTreeSelectedItemAction(CommitFileTree i_CommitFileTree) {
        i_CommitFileTree.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (!newValue.getValue().GetFileType().equals(eFileType.FOLDER)) {
                        textAreaCommitFileContent.setDisable(false);
                        SetTextAreaText(f_ActiveRepository.GetBlobs().get(newValue.getValue().GetSHA1()).GetText());
                    }
                    else {
                        textAreaCommitFileContent.setDisable(true);
                        textAreaCommitFileContent.setText("");
                    }

                    SetLabelLastUpdater(newValue.getValue().GetLastChanger());
                    SetLabelSha1(newValue.getValue().GetSHA1());
                    SetLabelUpdateDate(newValue.getValue().GetlastUpdate());
                });
    }

    private void setCommitFilesTreeFactory(CommitFileTree i_CommitFileTree) {
        i_CommitFileTree.setCellFactory(e -> new TreeCell<Folder.Data>() {
            @Override
            protected void updateItem(Folder.Data item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setText(item.GetName());
                    setGraphic(getTreeItem().getGraphic());
                } else {
                    setText("");
                    setGraphic(null);
                }
            }
        });
    }

    public void SetTextAreaText(String i_Text) {
        textAreaCommitFileContent.setText(i_Text);
    }

    private void setTreeView(TreeView<Folder.Data> i_Root) {
        HBox.setHgrow(i_Root, Priority.ALWAYS);
        hBoxCommitFiles.getChildren().add(i_Root);
    }

    public void SetLabelCommitSha1(String i_CommitSha1) { labelCommitSha1.setText(i_CommitSha1); }

    public void SetLabelSha1(String i_Text) {
        labelFileSha1.setText(i_Text);
    }

    public void SetLabelUpdateDate(String i_Date) {
        labelLastUpdateDate.setText(i_Date);
    }

    public void SetLabelLastUpdater(String i_LastUpdater) {
        labelLastUpdater.setText(i_LastUpdater);
    }

    @FXML private void copyFileSha1ToClipboardAction() {
        StringSelection selectedString = new StringSelection(labelFileSha1.getText());
        Toolkit.getDefaultToolkit()
                .getSystemClipboard().setContents(selectedString, selectedString);
    }

    @FXML private void copyCommitSha1ToClipboardAction() {
        StringSelection selectedString = new StringSelection(labelCommitSha1.getText());
        Toolkit.getDefaultToolkit()
                .getSystemClipboard().setContents(selectedString, selectedString);
    }
}

