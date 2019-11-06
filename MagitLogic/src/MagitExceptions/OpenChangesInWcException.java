package MagitExceptions;

import java.util.List;

public class OpenChangesInWcException extends Exception {
    List<List<String>> m_WcStatus;

    public OpenChangesInWcException(List<List<String>> i_WcStatus) {
        m_WcStatus = i_WcStatus;
    }

    public List<List<String>> GetWcStatus() {
        return m_WcStatus;
    }
}
