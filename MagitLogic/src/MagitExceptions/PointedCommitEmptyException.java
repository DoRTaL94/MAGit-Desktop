package MagitExceptions;

public class PointedCommitEmptyException extends Exception {
    public PointedCommitEmptyException() {
        super("Head branch pointed commit is empty.");
    }
}
