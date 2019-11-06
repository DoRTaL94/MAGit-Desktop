package data.structures;

import java.io.File;
import java.io.IOException;
import IO.FileUtilities;
import resources.jaxb.schema.generated.MagitBlob;

public class Blob implements IRepositoryFile {
    private String m_Text;

    public Blob() {
        m_Text = null;
    }

    public static String GetSha1FromContent(String blobContent) {
        return blobContent.replaceAll("\\s", "");
    }

    public String GetText() {
        return m_Text;
    }

    public void SetText(String i_Text) {
        m_Text = i_Text;
    }

    public static Blob Parse(MagitBlob i_MagitBlob){
        Blob newBlob = new Blob();
        newBlob.SetText(i_MagitBlob.getContent());
        return newBlob;
    }

    public static Blob Parse(File i_BlobZippedFile) throws IOException {
        Blob newBlob = new Blob();
        String blobContent = null;

        blobContent = FileUtilities.UnzipFile(i_BlobZippedFile.getPath());

        newBlob.SetText(blobContent);
        return newBlob;
    }

    @Override
    public String toString() {
        return m_Text;
    }

    public String toStringForSha1() { return m_Text.replaceAll("\\s", ""); }
}
