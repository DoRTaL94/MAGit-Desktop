package MagitExceptions;

public class EmptyWcException extends Exception {
    public EmptyWcException() {
        super("Working directory is empty.");
    }
}
