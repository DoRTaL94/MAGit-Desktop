package MagitExceptions;

public class ItemNotExistException extends Exception {
    public ItemNotExistException(String i_ItemId, String i_ItemType, String i_FolderIdOfItem) {
        super(String.format("%s (blob id: %s, folder id: %s) not found in xml.", i_ItemType, i_ItemId, i_FolderIdOfItem));
    }
}
