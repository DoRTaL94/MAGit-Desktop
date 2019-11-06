package magit;

import data.structures.Branch;
import data.structures.Commit;
import org.apache.commons.codec.digest.DigestUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class CommitNode implements Comparable {
    private final CommitNode f_FirstParent;
    private final CommitNode f_SecondParent;
    private final List<CommitNode> f_Children;
    private Commit m_Commit;
    private List<Branch> m_PointingBranches;
    private List<Branch> m_OnBranches;

    public CommitNode() {
        m_OnBranches = new ArrayList<>();
        f_FirstParent = null;
        f_SecondParent = null;
        f_Children = new ArrayList<>();
        m_PointingBranches = new ArrayList<>();
        m_Commit = null;
    }

    public CommitNode(CommitNode i_FirstParent, CommitNode i_SecondParent) {
        m_OnBranches = new ArrayList<>();
        f_FirstParent = i_FirstParent;
        f_SecondParent = i_SecondParent;
        f_Children = new ArrayList<>();
        m_PointingBranches = new ArrayList<>();
        m_Commit = null;
    }



    public String GetSha1() {
        String sha1 = "";

        if(m_Commit != null) {
            return DigestUtils.sha1Hex(m_Commit.toStringForSha1());
        }

        return sha1;
    }

    public void AddChildren(CommitNode i_Child) {
        f_Children.add(i_Child);
        f_Children.sort(this::CompareCommitNodes);
    }

    private int CompareCommitNodes(CommitNode i_N1, CommitNode I_N2) {
        return i_N1.compareTo(i_N1);
    }

    public CommitNode GetFirstParent() {
        return f_FirstParent;
    }

    public CommitNode GetSecondParent() {
        return f_SecondParent;
    }

    public List<CommitNode> GetChildren() {
        return f_Children;
    }

    public Commit GetCommit() {
        return m_Commit;
    }

    public void SetCommit(Commit i_Commit) {
        m_Commit = i_Commit;
    }

    public List<Branch> GetPointingBranches() {
        return m_PointingBranches;
    }

    @Override
    public boolean equals(Object o) {
        boolean isEqual;

        if(this == o) {
            isEqual = true;
        }
        else if(!(o instanceof CommitNode)) {
            isEqual = false;
        }
        else {
            CommitNode nodeToCompare = (CommitNode) o;
            isEqual = this.GetSha1().equals(nodeToCompare.GetSha1());
        }

        return isEqual;
    }

    @Override
    public int compareTo(Object o) {
        CommitNode nodeToCompare = (CommitNode) o;

        long nodeToCompareTime = 0;
        long thisNodeTime = 0;

        try {
            nodeToCompareTime = new SimpleDateFormat(Engine.DATE_FORMAT)
                    .parse(nodeToCompare.GetCommit().GetLastUpdate()).getTime();
            thisNodeTime = new SimpleDateFormat(Engine.DATE_FORMAT)
                    .parse(this.GetCommit().GetLastUpdate()).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return (int)(thisNodeTime - nodeToCompareTime);
    }

    public List<Branch> GetOnBranches() {
        return m_OnBranches;
    }
}
