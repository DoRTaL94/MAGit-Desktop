package data.structures;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import IO.FileUtilities;
import org.apache.commons.codec.digest.DigestUtils;
import puk.team.course.magit.ancestor.finder.CommitRepresentative;
import resources.jaxb.schema.generated.MagitSingleCommit;
import resources.jaxb.schema.generated.PrecedingCommits;

public class Commit implements IRepositoryFile, CommitRepresentative {
    private String m_FirstPrecedingCommitsSHA1 = "";
    private String m_SecondPrecedingCommitsSHA1 = "";
    private String m_RootFolderSHA1 = null;
    private String m_Message = null;
    private String m_LastChanger = null;
    private String m_LastUpdate = null;

    @Override
    public String toString() {
        return String.format("%s;%s;%s;%s;%s;%s", m_RootFolderSHA1, m_FirstPrecedingCommitsSHA1, m_SecondPrecedingCommitsSHA1, m_Message, m_LastChanger, m_LastUpdate);
    }

    public String toStringForSha1() {
        return String.format("%s;%s;%s;%s", m_RootFolderSHA1, m_FirstPrecedingCommitsSHA1, m_SecondPrecedingCommitsSHA1, m_Message);
    }

    public List<String> GetPrecedingCommits() {
        List<String> precedings = new ArrayList<>();

        if(!m_FirstPrecedingCommitsSHA1.equals("")) {
            precedings.add(m_FirstPrecedingCommitsSHA1);

            if (!m_SecondPrecedingCommitsSHA1.equals("")) {
                precedings.add(m_SecondPrecedingCommitsSHA1);
            }
        }

        return precedings;
    }

    public String GetRootFolderSHA1() {
        return m_RootFolderSHA1;
    }

    public void SetRootFolderSHA1(String i_MainFolderSHA1) {
        this.m_RootFolderSHA1 = i_MainFolderSHA1;
    }

    public void SetFirstPrecedingCommitSha1(String i_PrecedingCommitSHA1) {
        m_FirstPrecedingCommitsSHA1 = i_PrecedingCommitSHA1;
    }

    public void SetSecondPrecedingCommitSha1(String i_PrecedingCommitSHA1) {
        m_SecondPrecedingCommitsSHA1 = i_PrecedingCommitSHA1;
    }

    public String GetMessage() {
        return m_Message;
    }

    public void SetMessage(String i_Message) {
        this.m_Message = i_Message;
    }

    public String GetLastChanger() {
        return m_LastChanger;
    }

    public void SetLastChanger(String i_LastChanger) {
        m_LastChanger = i_LastChanger;
    }

    public String GetLastUpdate() {
        return m_LastUpdate;
    }

    public void SetLastUpdate(String i_LastUpdate) {
        m_LastUpdate = i_LastUpdate;
    }

    public static Commit Parse(MagitSingleCommit i_MagitCommit) {
        Commit newCommit = new Commit();
        newCommit.SetMessage(i_MagitCommit.getMessage());
        newCommit.SetLastChanger(i_MagitCommit.getAuthor());
        newCommit.SetLastUpdate(i_MagitCommit.getDateOfCreation());
        newCommit.SetRootFolderSHA1(i_MagitCommit.getRootFolder().getId());

        if(i_MagitCommit.getPrecedingCommits() != null) {
            List<PrecedingCommits.PrecedingCommit> precedings = i_MagitCommit.getPrecedingCommits().getPrecedingCommit();

            if (precedings.size() != 0) {
                newCommit.SetFirstPrecedingCommitSha1(precedings.get(0).getId());

                if (precedings.size() == 2) {
                    newCommit.SetSecondPrecedingCommitSha1(precedings.get(1).getId());
                }
            }
        }

        return newCommit;
    }

    public static Commit Parse(File i_ZippedCommitFile) throws IOException {
        Commit newCommit = new Commit();

        try {
            String commitContent = FileUtilities.UnzipFile(i_ZippedCommitFile.getPath());
            String[] parts = commitContent.split(";");
            newCommit.SetRootFolderSHA1(parts[0]);

            if (!parts[1].equals("")) {
                newCommit.SetFirstPrecedingCommitSha1(parts[1]);

                if(!parts[2].equals("")) {
                    newCommit.SetSecondPrecedingCommitSha1(parts[2]);
                }
            }

            newCommit.SetMessage(parts[3]);
            newCommit.SetLastChanger(parts[4]);
            newCommit.SetLastUpdate(parts[5]);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            throw new IOException("Not a commit file.");
        }

        return newCommit;
    }

    public String getSha1() {
        return DigestUtils.sha1Hex(this.toStringForSha1());
    }

    public String getFirstPrecedingSha1() {
        return m_FirstPrecedingCommitsSHA1;
    }

    public String getSecondPrecedingSha1() {
        return m_SecondPrecedingCommitsSHA1;
    }
}
