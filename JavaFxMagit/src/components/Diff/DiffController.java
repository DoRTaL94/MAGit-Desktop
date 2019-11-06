package components.Diff;

import components.themes.ThemesController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Callback;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.io.File;

public class DiffController {
    @FXML private HBox hBoxWcStatus;
    @FXML private ListView<String> listViewNewFiles;
    @FXML private ListView<String> listViewDeletedFiles;
    @FXML private ListView<String> listViewChangedFiles;

    private static final String FOLDER_ICON_PATH = "/components/filetree/resources/folder.png";
    private static final String TXT_FILE_ICON_PATH = "/components/filetree/resources/text-x-generic.png";
    private static final String f_DarkTheme = "resources/Dark.css";
    private static final String f_ColorfulTheme = "resources/Colorful.css";
    private String m_CurrentTheme;

    public DiffController() {
        m_CurrentTheme = "";

        ThemesController.themeChangedProperty.addListener((observable, oldValue, newValue) -> {
            if(newValue.equals("Dark")) {
                m_CurrentTheme = f_DarkTheme;
            }
            else if(newValue.equals("Colorful")) {
                m_CurrentTheme = f_ColorfulTheme;
            }

            if(newValue.equals("Default")) {
                m_CurrentTheme = "";
                hBoxWcStatus.getStylesheets().clear();
                hBoxWcStatus.applyCss();
            } else {
                hBoxWcStatus.getStylesheets().clear();
                hBoxWcStatus.getStylesheets().add(getClass().getResource(m_CurrentTheme).toExternalForm());
                hBoxWcStatus.applyCss();
            }
        });
    }

    public static DiffController LoadFXML() {
        FXMLLoader fxmlLoader = new FXMLLoader();
        URL url = DiffController.class.getResource("/components/Diff/DiffUI.fxml");
        fxmlLoader.setLocation(url);

        try {
            fxmlLoader.load(url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fxmlLoader.getController();
    }

    @FXML public void initialize() {
        HBox.setHgrow(hBoxWcStatus, Priority.ALWAYS);
        setListViewFactory(listViewDeletedFiles);
        setListViewFactory(listViewChangedFiles);
        setListViewFactory(listViewNewFiles);
    }

    private void setListViewFactory(ListView<String> i_ListView) {
        i_ListView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
              @Override
              public ListCell<String> call(ListView<String> list) {
                  ListCell<String> listCell = new ListCell<String>() {
                      @Override
                      public void updateItem(String item, boolean empty) {
                          super.updateItem(item, empty);
                          if (item != null) {
                              String itemName = item.split(";")[0];
                              itemName = new File(itemName).getName();
                              setText(itemName);

                              if(itemName.contains(".")){
                                  setGraphic(new ImageView(getClass().getResource(TXT_FILE_ICON_PATH).toExternalForm()));
                              }
                              else {
                                  setGraphic(new ImageView(getClass().getResource(FOLDER_ICON_PATH).toExternalForm()));
                              }
                          } else {
                              setText("");
                              setGraphic(null);
                          }
                      }
                  };

                  listCell.setOnMouseClicked(mouseEvent -> ListCell_Click(mouseEvent, listCell));

                  return listCell;
              }
          });
    }

    private void ListCell_Click(MouseEvent i_MouseEvent, ListCell<String> i_ClickedListCell) {
        if(i_MouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            if(i_MouseEvent.getClickCount() == 2) {
                try {
                    File file = new File(i_ClickedListCell.getItem().split(";")[0]);

                    if (file.exists()) {
                        try {
                            if (file.isDirectory()) {
                                Desktop.getDesktop().open(file);
                            } else {
                                Desktop.getDesktop().edit(file);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (NullPointerException ignored) {}
            }
        }
    }

    public HBox GetRoot() {
        return hBoxWcStatus;
    }

    public ListView<String> GetListViewDeletedFiles() {
        return listViewDeletedFiles;
    }

    public ListView<String> GetListViewNewFiles() {
        return listViewNewFiles;
    }

    public ListView<String> GetListViewChangedFiles() {
        return listViewChangedFiles;
    }
}
