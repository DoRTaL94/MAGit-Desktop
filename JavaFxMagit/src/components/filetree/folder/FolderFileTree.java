package components.filetree.folder;

import components.filetree.commit.CommitFileTreeController;
import data.structures.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import magit.CommitNode;
import magit.Engine;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

public class FolderFileTree extends TreeView<Folder.Data> {
    private static final String FOLDER_ICON_PATH = "/components/filetree/resources/folder.png";
    private static final String TXT_FILE_ICON_PATH = "/components/filetree/resources/text-x-generic.png";
    private final Repository f_ActiveRepository =  Engine.Creator.GetInstance().GetActiveRepository();
    private final Folder m_Folder;
    private final String m_Location;

    public FolderFileTree(Folder i_Folder, String i_Location) {
        m_Location = i_Location;
        m_Folder = i_Folder;
        initializeFactory();
        CreateTree();
    }

    private void initializeFactory() {
        this.setCellFactory(e -> new TreeCell<Folder.Data>() {
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

    private void CreateTree() {
        Folder.Data rootFolderData = new Folder.Data();
        rootFolderData.SetSHA1(DigestUtils.sha1Hex(m_Folder.toStringForSha1(Paths.get(m_Location))));
        rootFolderData.SetName(f_ActiveRepository.GetName());
        rootFolderData.SetFileType(eFileType.FOLDER);

        this.setRoot(CreateTreeRec(rootFolderData));
        this.getRoot().setExpanded(true);
    }

    private TreeItem<Folder.Data> CreateTreeRec(Folder.Data i_File) {
        TreeItem<Folder.Data> item = new TreeItem<>(i_File);

        if(i_File.GetFileType().equals(eFileType.FOLDER)) {
            Folder root = f_ActiveRepository.GetFolders().get(i_File.GetSHA1());
            List<Folder.Data> files = root.GetFiles();

            for(Folder.Data file: files) {
                item.getChildren().add(CreateTreeRec(file));
            }

            item.setGraphic(new ImageView(getClass().getResource(FOLDER_ICON_PATH).toExternalForm()));
        }
        else {
            item.setGraphic(new ImageView(getClass().getResource(TXT_FILE_ICON_PATH).toExternalForm()));
        }

        return item;
    }
}