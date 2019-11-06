package magit.merge;

import IO.FileUtilities;
import data.structures.Blob;
import data.structures.Folder;
import data.structures.IRepositoryFile;
import magit.Engine;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public enum eMergeSituation {
    NEW_FILE_IN_THEIRS {
        public void Solve(String i_FullPath, IRepositoryFile i_Solution) {
            Engine.Creator.GetInstance().CreateNewFileOnSystem(i_Solution, i_FullPath);
        }
    },

    // CONFLICT
    OURS_DELETED_THEIRS_CHANGED {
        public void Solve(String i_FullPath, IRepositoryFile i_Solution) {
            if(i_Solution != null) {
                Engine.Creator.GetInstance().CreateNewFileOnSystem(i_Solution, i_FullPath);
            }
        }
    },

    //OURS_DELETED_THEIRS_SAME {
    //    void Solve(String i_FullPath, IRepositoryFile i_Solution) {}
    //},

    //NEW_FILE_IN_OURS {
    //    void Solve(String i_FullPath, IRepositoryFile i_Solution) {}
    //},

    // CONFLICT
    SAME_NAME_EQU_SHA1 {
        public void Solve(String i_FullPath, IRepositoryFile i_Solution) {
            Engine.Creator.GetInstance().DeleteFile(i_Solution, i_FullPath, true);
        }
    },

    // CONFLICT
    SAME_NAME_DIFF_SHA1 {
        public void Solve(String i_FullPath, IRepositoryFile i_Solution) {
            Engine.Creator.GetInstance().DeleteFile(i_Solution, i_FullPath, true);
        }
    },

    // CONFLICT
    OURS_CHANGED_THEIRS_DELETED {
        public void Solve(String i_FullPath, IRepositoryFile i_Solution) {
            if(i_Solution == null) {
                Engine.Creator.GetInstance().DeleteFile(i_Solution, i_FullPath, false);
            }
        }
    },

    OURS_SAME_THEIRS_DELETED {
        public void Solve(String i_FullPath, IRepositoryFile i_Solution) {
            Engine.Creator.GetInstance().DeleteFile(i_Solution, i_FullPath, false);
        }
    },

    // CONFLICT
    CHANGED_TO_DIFF_IN_BOTH {
        public void Solve(String i_FullPath, IRepositoryFile i_Solution) {
            Engine.Creator.GetInstance().DeleteFile(i_Solution, i_FullPath, true);
        }
    },

    //OURS_CHANGED_THEIRS_SAME {
    //    void Solve(String i_FullPath, IRepositoryFile i_Solution) {}
    //},

    // CONFLICT
    CHANGED_TO_SAME_IN_BOTH {
        public void Solve(String i_FullPath, IRepositoryFile i_Solution) {
            if(!(i_Solution instanceof Folder)) {
                Engine.Creator.GetInstance().DeleteFile(i_Solution, i_FullPath, true);
            }
        }
    },

    //SAME_FILE_IN_ALL {
    //    void Solve(String i_FullPath, IRepositoryFile i_Solution) {}
    //},

    OURS_SAME_THEIR_CHANGED {
        public void Solve(String i_FullPath, IRepositoryFile i_Solution) {
            Engine.Creator.GetInstance().DeleteFile(i_Solution, i_FullPath, true);
        }
    },

    KEEP_STATE {
        public void Solve(String i_FullPath, IRepositoryFile i_Solution) {}
    };

    public abstract void Solve(String i_FullPath, IRepositoryFile i_Solution);
}
