package MagitExceptions;

public class RepositoryAlreadyExistsException extends Exception {
    public RepositoryAlreadyExistsException() {
        super("Repository is already exists.");
    }
}
