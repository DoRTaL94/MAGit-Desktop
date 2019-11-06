package menus;

import magit.IEngine;

public class MainMenu extends Menu {

    public MainMenu(String i_Name, IEngine i_Engine) {
        super(i_Name);
        super.SetBackOrExitOption("Exit");
        SetEngine(i_Engine);
    }

    @Override
    public void Show() {
        super.Show();
        System.out.println("Goodbye!");
    }

    @Override
    public void OnClick() {}

    @Override
    public void AddItem(Menu i_ToAdd) {
        GetItems().add(i_ToAdd);
        i_ToAdd.SetEngine(GetEngine());
    }
}
