package magit.merge;

import data.structures.IRepositoryFile;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface IMergeTask {
    List<Conflict> ExecuteTask(String i_CurrentPath, IRepositoryFile i_Ours, IRepositoryFile i_Theirs,
                            IRepositoryFile i_Ancestor, List<Map<String, String>> i_PathToSha1Maps);
}
