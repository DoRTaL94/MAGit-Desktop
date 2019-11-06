package MagitExceptions;

import java.util.List;

public class XmlErrorsException extends Exception {
    List<String> m_Errors = null;

    public XmlErrorsException(String i_Message) {
        super(i_Message);
    }

    public XmlErrorsException(List<String> i_ErrorsList) {
        m_Errors = i_ErrorsList;
    }

    public List<String> GetErrors() {
        return m_Errors;
    }
}
