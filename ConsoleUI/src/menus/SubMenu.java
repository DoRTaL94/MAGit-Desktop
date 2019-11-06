package menus;

public class SubMenu extends Menu {

    public SubMenu(String i_Name) {
        super(i_Name);
        super.SetBackOrExitOption("Back");
    }

    @Override
    public void OnClick() {
        this.Show();
    }
}
