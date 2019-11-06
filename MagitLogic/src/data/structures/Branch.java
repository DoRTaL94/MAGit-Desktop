package data.structures;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import javafx.beans.property.BooleanProperty;
import resources.jaxb.schema.generated.MagitSingleBranch;

public class Branch {
    private static final String TXT_FILE_SUFFIX = ".txt";
    private String m_PointedCommitSha1 = "";
    private String m_Name = null;
    private boolean m_IsHead = false;
    private boolean m_IsRemote = false;
    private boolean m_IsTracking = false;
    private String m_TrakingAfter = "";
    private boolean m_IsMerged = false;

    public void SetIsRemote(boolean i_IsRemote) {
        m_IsRemote = i_IsRemote;
    }

    public boolean IsTracking() {
        return m_IsTracking;
    }

    public void SetIsTracking(boolean i_IsTracking) {
        m_IsTracking = i_IsTracking;
    }

    public String GetTrakingAfter() {
        return m_TrakingAfter;
    }

    public void SetTrakingAfter(String i_TrakingAfter) {
        m_TrakingAfter = i_TrakingAfter;
    }

    public boolean IsRemote() { return m_IsRemote; }

    public void setIsRemote(boolean i_IsRemote) {
        m_IsRemote = i_IsRemote;
    }

    public String GetPointedCommitSha1() {
        return m_PointedCommitSha1;
    }

    public void SetPointedCommitSha1(String i_PointedCommitSha1) {
        m_PointedCommitSha1 = i_PointedCommitSha1;
    }

    public String GetName() {
        return m_Name;
    }

    public void SetName(String i_Name) {
        m_Name = i_Name;
    }

    public boolean IsHead() {
        return m_IsHead;
    }

    public void SetIsHead(boolean i_IsHead){
        m_IsHead = i_IsHead;
    }

    public static Branch Parse(MagitSingleBranch i_MagitBranch) {
        Branch newBranch = new Branch();
        newBranch.SetName(i_MagitBranch.getName());
        newBranch.SetPointedCommitSha1(i_MagitBranch.getPointedCommit().getId());
        return newBranch;
    }

    public static Branch Parse(File i_BranchFile) {
        Branch newBranch = new Branch();

        try(Scanner scanner = new Scanner(i_BranchFile)) {
            String pointedCommitSha1 = "";

            if(scanner.hasNextLine()) {
                pointedCommitSha1 = scanner.nextLine();
            }

            newBranch.SetPointedCommitSha1(pointedCommitSha1);
            newBranch.SetName(i_BranchFile.getName().substring(0,
                    i_BranchFile.getName().length() - TXT_FILE_SUFFIX.length()));
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return newBranch;
    }
}