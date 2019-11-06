import magit.Engine;
import menus.MainMenu;
import menus.Menu;
import menus.SubMenu;
import options.MenuOptions;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Menu mainMenu = new MainMenu("M.A.Git", Engine.Creator.GetInstance());
        BuildMenu(mainMenu);
        mainMenu.Show();
    }

    private static void BuildMenu(Menu i_MainMenu) {
        MenuOptions.CreateActions();
        List<SubMenu> options = MenuOptions.GetActions();

        for(SubMenu option: options) {
            i_MainMenu.AddItem(option);
        }
    }
}
