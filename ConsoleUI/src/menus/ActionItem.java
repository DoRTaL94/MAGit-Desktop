package menus;

public class ActionItem extends SubMenu {

    private IAction m_Action;

    public ActionItem(String i_Name, IAction i_Action) {
        super(i_Name);
        m_Action = i_Action;
    }

    @Override
    public void OnClick() {
        m_Action.MethodToExecuteWhenMenuWasClicked();
    }
}
