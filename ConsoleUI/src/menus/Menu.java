package menus;

import IO.*;
import magit.IEngine;
import java.util.ArrayList;
import java.util.List;

public abstract class Menu {
    private static IEngine m_Engine;
    private final List<Menu> m_Items;
    private String m_Name;
    private boolean m_IsBackOrExit;
    private String m_BackOrExitOption;
    private int m_UserChoice;

    public Menu(String i_Name){
        m_Name = i_Name;
        m_Items = new ArrayList<>();
        m_IsBackOrExit = false;
        m_BackOrExitOption = null;
        m_UserChoice = -1;
    }

    public abstract void OnClick();

    public void Show() {
        int i;
        List<Menu> items = GetItems();

        while(!IsBackOrExit()) {
            System.out.format("%s \\ User: %s \\ Repository: %s >%s",
                GetName(),
                    m_Engine.GetCurrentUserName(),
                    m_Engine.GetActiveRepository() == null ? "N/A" : m_Engine.GetActiveRepository().GetName(),
                System.lineSeparator());

            for(i = 0; i< items.size(); i++) {
                System.out.format("%d. %s%n", i + 1, items.get(i).GetName());
            }

            System.out.format("%d. %s%n", i + 1, m_BackOrExitOption);

            try {
                m_UserChoice = ConsoleUtils.GetUserChoice();
                System.out.println();

                if (m_UserChoice == i + 1) {
                    SetIsBackOrExit(true);
                } else if (m_UserChoice > 0 && m_UserChoice <= i) {
                    items.get(m_UserChoice - 1).OnClick();
                } else {
                    System.out.format("ERROR: Expected input is a number between %d and %d. Please try again.", 1, i + 1);
                    System.out.println();
                    System.out.println();
                }
            } catch (NumberFormatException nfe) {
                System.out.println();
                System.out.println("ERROR: Input is not a number! Please try again.");
                System.out.println();
            }
        }
    }

    public final List<Menu> GetItems() {
        return m_Items;
    }

    public void AddItem(Menu i_ToAdd){
        m_Items.add(i_ToAdd);
    }

    public String GetName() {
        return m_Name;
    }

    public boolean IsBackOrExit() {
        return m_IsBackOrExit;
    }

    public void SetIsBackOrExit(boolean i_IsBackOrExit) {
        m_IsBackOrExit = i_IsBackOrExit;
    }

    public void SetBackOrExitOption(String i_Option) {
        m_BackOrExitOption = i_Option;
    }

    public static IEngine GetEngine() {
        return m_Engine;
    }

    public static void SetEngine(IEngine i_Engine) {
        m_Engine = i_Engine;
    }

    private void checkIfInputExists() {
        while(m_UserChoice == -1) {

        }

        this.notifyAll();
    }
}
