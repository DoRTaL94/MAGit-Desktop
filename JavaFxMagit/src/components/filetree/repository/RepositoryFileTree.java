package components.filetree.repository;

import IO.FileUtilities;
import components.themes.ThemesController;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import main.MagitController;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RepositoryFileTree extends TreeView<File> {
    private static final String FOLDER_ICON_PATH = "/components/filetree/resources/folder.png";
    private static final String TXT_FILE_ICON_PATH = "/components/filetree/resources/text-x-generic.png";
    private static final String f_DarkTheme = "resources/Dark.css";
    private static final String f_ColorfulTheme = "resources/Colorful.css";
    private String m_CurrentTheme;

    public RepositoryFileTree(TextArea i_TextArea) {
        setSelectedItemAction(i_TextArea);
        initializeFactory();

        if(ThemesController.themeChangedProperty.get().equals("Dark")) {
            m_CurrentTheme = f_DarkTheme;
        } else if(ThemesController.themeChangedProperty.get().equals("Colorful")) {
            m_CurrentTheme = f_ColorfulTheme;
        } else {
            m_CurrentTheme = "";
        }

        if(!m_CurrentTheme.isEmpty()) {
            this.getStylesheets().add(getClass().getResource(m_CurrentTheme).toExternalForm());
            this.applyCss();
        }

        ThemesController.themeChangedProperty.addListener((observable, oldValue, newValue) -> {
            if(newValue.equals("Dark")) {
                m_CurrentTheme = f_DarkTheme;
            }
            else if(newValue.equals("Colorful")) {
                m_CurrentTheme = f_ColorfulTheme;
            }

            if(newValue.equals("Default")) {
                m_CurrentTheme = "";
                this.getStylesheets().clear();
                this.applyCss();
            } else {
                this.getStylesheets().clear();
                this.getStylesheets().add(getClass().getResource(m_CurrentTheme).toExternalForm());
                this.applyCss();
            }
        });
    }

    private void setSelectedItemAction(TextArea i_TextArea) {
        if(i_TextArea != null) {
            this.getSelectionModel()
                    .selectedItemProperty()
                    .addListener((observable, oldValue, newValue) -> {
                        try {
                            if (!newValue.getValue().isDirectory()) {
                                i_TextArea.setDisable(false);
                                i_TextArea.setText(FileUtilities.ReadTextFromFile(newValue.getValue().toPath().toString()));
                            }
                            else {
                                i_TextArea.setDisable(true);
                                i_TextArea.setText("");
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    private void initializeFactory() {
        this.setCellFactory(e -> new TreeCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setText(item.getName());
                    setGraphic(getTreeItem().getGraphic());
                } else {
                    setText("");
                    setGraphic(null);
                }
            }
        });
    }

    public void CreateTreeFromFile(File i_File) {
        this.setRoot(CreateTreeRec(i_File));
        this.getRoot().setExpanded(true);
    }

    private TreeItem<File> CreateTreeRec(File i_File) {
        TreeItem<File> item = new TreeItem<>(i_File);
        File[] childs = i_File.listFiles();

        if (childs != null) {
            List<File> childsList = Arrays.stream(i_File.listFiles())
                .filter(f -> !f.getName().contains("magit"))
                .collect(Collectors.toList());

            for (File child : childsList) {
                item.getChildren().add(CreateTreeRec(child));
            }

            item.setGraphic(new ImageView(getClass().getResource(FOLDER_ICON_PATH).toExternalForm()));
        } else {
            item.setGraphic(new ImageView(getClass().getResource(TXT_FILE_ICON_PATH).toExternalForm()));
        }

        return item;
    }

}