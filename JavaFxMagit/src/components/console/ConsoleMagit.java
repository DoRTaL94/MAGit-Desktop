package components.console;

import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import magit.Engine;
import menus.Menu;
import menus.SubMenu;
import options.MenuOptions;
import string.StringUtilities;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;
import java.util.List;

public class ConsoleMagit extends ScrollPane {
    private TextArea m_Console;
    private ConsoleMagitController m_Controller;
    private int inputLength;
    private boolean isControl = false;

    public ConsoleMagit() {
        inputLength = 0;
        m_Console = new TextArea();
        m_Controller = new ConsoleMagitController(m_Console);

        this.setPadding(new Insets(5,5,5,5));
        this.setFitToWidth(true);
        this.setFitToHeight(true);
        this.setContent(m_Console);

        initializeConsole();
        this.getStylesheets().add(getClass().getResource("Console.css").toExternalForm());
        this.applyCss();
    }

    private void initializeConsole() {
        m_Console.setOnKeyPressed(this::EnterPressedAction);
        m_Console.addEventFilter(KeyEvent.KEY_PRESSED, this::filterKeyPressed);
        m_Console.addEventFilter(KeyEvent.KEY_TYPED, this::filterKeyTyped);

        menus.MainMenu consoleMainMenu = new menus.MainMenu("M.A.Git", Engine.Creator.GetInstance());
        BuildMenu(consoleMainMenu);

        Thread consoleMagitThread = new Thread(consoleMainMenu::Show);
        consoleMagitThread.setDaemon(true);
        consoleMagitThread.start();
    }

    private static void BuildMenu(Menu i_MainMenu) {
        MenuOptions.CreateActions();
        List<SubMenu> options = MenuOptions.GetActions();

        for(SubMenu option: options) {
            i_MainMenu.AddItem(option);
        }
    }

    private void EnterPressedAction(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER)  {
            inputLength = 0;
            String text = m_Console.getText();
            List<String> lines = StringUtilities.GetLines(text);
            int linesCount = lines.size();
            String input = lines.get(linesCount - 1).substring(3);
            m_Controller.SetBuffer(input);
        }
        else if(keyEvent.getCode() == KeyCode.CONTROL || isControl) {
            isControl = true;

            if(keyEvent.getText().equals("v")) {
                isControl = false;

                try {
                    inputLength = ((String) Toolkit.getDefaultToolkit()
                            .getSystemClipboard().getData(DataFlavor.stringFlavor)).length();
                } catch (UnsupportedFlavorException | IOException e) {
                    inputLength = 0;
                }
            }

        }
        else if(!keyEvent.getText().isEmpty()) {
            inputLength++;
        }
    }

    private void filterKeyPressed(KeyEvent keyEvent){
        if(m_Console.getCaretPosition() < m_Console.getText().length() - inputLength) {
            if(keyEvent.getCode() == KeyCode.CONTROL || isControl) {
                isControl = true;

                if(keyEvent.getText().equals("c")) {
                    isControl = false;
                    StringSelection selectedString = new StringSelection(m_Console.getSelectedText());
                    Toolkit.getDefaultToolkit()
                            .getSystemClipboard().setContents(selectedString, selectedString);
                }
            }

            keyEvent.consume();
        }
        else if (keyEvent.getCode() == KeyCode.BACK_SPACE || keyEvent.getCode() == KeyCode.DELETE) {
            if(inputLength == 0) {
                keyEvent.consume();
            } else if(inputLength > 0) {
                inputLength--;
            }
        }
    }

    private void filterKeyTyped(KeyEvent keyEvent){
        if(m_Console.getCaretPosition() < m_Console.getText().length() - inputLength) {
            keyEvent.consume();
        }
    }
}
