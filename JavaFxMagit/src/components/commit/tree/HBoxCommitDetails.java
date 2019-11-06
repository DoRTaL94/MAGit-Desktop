package components.commit.tree;

import components.themes.ThemesController;
import data.structures.Branch;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import main.MagitController;

import java.util.ArrayList;
import java.util.List;

public class HBoxCommitDetails extends HBox {
    private static final double HBOX_HEIGHT = 32.0;
    private static final String f_DarkTheme = "resources/Dark.css";
    private static final String f_ColorfulTheme = "resources/Colorful.css";
    private static final String f_DefaultTheme = "resources/Default.css";

    private static final String f_DarkSelectedTheme = "resources/DarkSelected.css";
    private static final String f_ColorfulSelectedTheme = "resources/ColorfulSelected.css";
    private static final String f_DefaultSelectedTheme = "resources/DefaultSelected.css";

    private Label m_LabelPointedCommitDescription;
    private Label m_LabelUserName;
    private Label m_LabelCommitTimeStamp;
    private Label m_LabelCommitSha1;
    private Pane m_PaneSpacer;
    private List<StackPane> m_StackPaneBranchNameList;
    private Runnable m_DoubleClickAction;
    private Runnable m_ClickAction;
    private String m_CurrentTheme;
    private String m_CurrentSelectedTheme;

    public HBoxCommitDetails() {
        this.setOnMouseClicked(this::CommitDetails_Click);
        CommitItemFactory.GetListItemClickedProperty().addListener(observable -> updateGraphics());

        m_StackPaneBranchNameList = new ArrayList<>();
        m_PaneSpacer = new Pane();
        m_LabelUserName = new Label();
        m_LabelPointedCommitDescription = new Label();
        m_LabelCommitTimeStamp = new Label();
        m_LabelCommitSha1 = new Label();

        this.setHeight(HBOX_HEIGHT);
        this.setPrefHeight(HBOX_HEIGHT);
        this.setMinHeight(HBOX_HEIGHT);
        this.setSpacing(10);

        if(ThemesController.themeChangedProperty.get().equals("Dark")) {
            m_CurrentTheme = f_DarkTheme;
            m_CurrentSelectedTheme = f_DarkTheme;
        } else if(ThemesController.themeChangedProperty.get().equals("Colorful")) {
            m_CurrentTheme = f_ColorfulTheme;
            m_CurrentSelectedTheme = f_ColorfulSelectedTheme;
        } else {
            m_CurrentTheme = f_DefaultTheme;
            m_CurrentSelectedTheme = f_DefaultSelectedTheme;
        }

        setStylesheet(m_CurrentTheme);

        ThemesController.themeChangedProperty.addListener((observable, oldValue, newValue) -> {
            if(newValue.equals("Dark")) {
                m_CurrentSelectedTheme = f_DarkSelectedTheme;
                m_CurrentTheme = f_DarkTheme;
            }
            else if(newValue.equals("Colorful")) {
                m_CurrentSelectedTheme = f_ColorfulSelectedTheme;
                m_CurrentTheme = f_ColorfulTheme;
            }
            else {
                m_CurrentSelectedTheme = f_DefaultSelectedTheme;
                m_CurrentTheme = f_DefaultTheme;
            }

            setStylesheet(m_CurrentTheme);
        });
    }

    private void updateGraphics() {
        if(m_CurrentTheme.equals(f_DarkTheme)) {
            setStylesheet(f_DarkTheme);
        }
        else if(m_CurrentTheme.equals(f_ColorfulTheme)) {
            setStylesheet(f_ColorfulTheme);
        }
        else {
            setStylesheet(f_DefaultTheme);
        }
    }

    private void setStylesheet(String i_CssPath) {
        this.getStylesheets().clear();
        this.getStyleClass().clear();
        this.getStyleClass().add("hBoxCommitDetails");
        this.getStylesheets().add(getClass().getResource(i_CssPath).toExternalForm());
        this.applyCss();
    }

    private void CommitDetails_Click(MouseEvent mouseEvent) {
        if(mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            if(mouseEvent.getClickCount() == 1) {
                CommitItemFactory.GetListItemClickedProperty().set(CommitItemFactory.GetListItemClickedProperty().not().get());
                setStylesheet(m_CurrentSelectedTheme);
                if(m_ClickAction != null) {
                    m_ClickAction.run();
                }
            }
            else if(mouseEvent.getClickCount() == 2) {
                if (m_DoubleClickAction != null) {
                    m_DoubleClickAction.run();
                }
            }
        }
    }

    public void SetDoubleClickAction(Runnable i_Action) {
        m_DoubleClickAction = i_Action;
    }

    public void SetClickAction(Runnable i_Action) {
        m_ClickAction = i_Action;
    }

    public void AddBranchNameRectangle(Branch i_Branch) {
        Text textLabelBranch = new Text(i_Branch.GetName());
        textLabelBranch.setFont(Font.font("System", FontWeight.BOLD, 15));
        double labelBranchNameWidth = textLabelBranch.getLayoutBounds().getWidth();

        StackPane stackPaneBranchName = new StackPane();
        Rectangle rectBranchName = new Rectangle();
        Label labelBranch = new Label();

        labelBranch.setText(i_Branch.GetName());
        labelBranch.setPrefWidth(labelBranchNameWidth);

        stackPaneBranchName.minWidthProperty().bind(rectBranchName.widthProperty());
        stackPaneBranchName.maxWidthProperty().bind(rectBranchName.widthProperty());
        stackPaneBranchName.setPrefHeight(HBOX_HEIGHT);

        if(i_Branch.IsTracking()) {
            rectBranchName.getStyleClass().add("rectBranchNameRTB");
        } else {
            rectBranchName.getStyleClass().add("rectBranchName");
        }

        rectBranchName.setHeight(HBOX_HEIGHT);
        rectBranchName.widthProperty().bind(Bindings.add(30, labelBranch.prefWidthProperty()));

        labelBranch.getStyleClass().add("labelBranchName");
        labelBranch.setPrefHeight(HBOX_HEIGHT);
        labelBranch.minWidthProperty().bind(labelBranch.prefWidthProperty());
        labelBranch.maxWidthProperty().bind(labelBranch.prefWidthProperty());

        stackPaneBranchName.getChildren().add(rectBranchName);
        stackPaneBranchName.getChildren().add(labelBranch);

        m_StackPaneBranchNameList.add(stackPaneBranchName);
    }

    public void Update() {
        m_PaneSpacer.setPrefWidth(0);
        m_PaneSpacer.setPrefHeight(HBOX_HEIGHT);

        setLabelStyle(m_LabelUserName);
        setLabelStyle(m_LabelPointedCommitDescription);
        setLabelStyle(m_LabelCommitTimeStamp);
        setLabelStyle(m_LabelCommitSha1);

        this.getChildren().add(m_PaneSpacer);

        for(StackPane stackPane: m_StackPaneBranchNameList) {
            this.getChildren().add(stackPane);
        }

        this.getChildren().add(m_LabelPointedCommitDescription);
        this.getChildren().add(m_LabelUserName);
        this.getChildren().add(m_LabelCommitTimeStamp);
        this.getChildren().add(m_LabelCommitSha1);
    }

    private void setLabelStyle(Label i_Label) {
        i_Label.getStyleClass().add("CommitDetail");
        i_Label.setPrefHeight(HBOX_HEIGHT);
        i_Label.minWidthProperty().bind(i_Label.prefWidthProperty());
        i_Label.maxWidthProperty().bind(i_Label.prefWidthProperty());
    }

    public List<StackPane> GetBranchNameRectangleList() {
        return m_StackPaneBranchNameList;
    }

    public Label GetLabelPointedCommitDescription() {
        return m_LabelPointedCommitDescription;
    }

    public Label GetLabelUserName() {
        return m_LabelUserName;
    }

    public Label GetLabelCommitDaysAgo() {
        return m_LabelCommitTimeStamp;
    }

    public Label GetLabelCommitSha1() {
        return m_LabelCommitSha1;
    }

    public Pane GetPaneSpacer() {
        return m_PaneSpacer;
    }
}
