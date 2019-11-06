package MagitExceptions;

public class CommitAlreadyExistsException extends Exception {
    public CommitAlreadyExistsException(String i_CommitSha1) {
        super(String.format("Commit already exists. You could reset head branch to: %s.",i_CommitSha1));
    }
}
