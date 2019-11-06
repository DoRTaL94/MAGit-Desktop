package MagitExceptions;

public class Sha1LengthException extends Exception {
    public Sha1LengthException() {
        super("Sha1 length should be 40 characters.");
    }
}
