package MagitExceptions;

public class RepositoryNotLoadedException extends Exception {
    public RepositoryNotLoadedException() {
        super("Repository wasn't been loaded.");
    }
}
