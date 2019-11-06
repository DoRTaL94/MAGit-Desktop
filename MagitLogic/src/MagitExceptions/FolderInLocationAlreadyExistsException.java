package MagitExceptions;

public class FolderInLocationAlreadyExistsException extends Exception {
    public FolderInLocationAlreadyExistsException() {
        super("Folder in the given location is already exists.");
    }
}
