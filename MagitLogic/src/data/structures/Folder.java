package data.structures;

import IO.FileUtilities;
import magit.Engine;
import org.apache.commons.codec.digest.DigestUtils;
import string.StringUtilities;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Folder implements IRepositoryFile {
    private final List<Data> m_Files = new ArrayList<>();
    private boolean m_IsRoot = false;

    public final List<Data> GetFiles() {
        return m_Files;
    }

    public void AddFile(Data i_Data) {
        m_Files.add(i_Data);
        m_Files.sort(Folder.Data::compare);
    }

    public boolean IsRoot() {
        return m_IsRoot;
    }

    public void SetIsRoot(boolean i_IsRoot) {
        m_IsRoot = i_IsRoot;
    }

    public String toStringForSha1(Path i_FolderPath) {
        StringBuilder sb = new StringBuilder();
        int filesCount = m_Files.size();

        for(int data = 0; data < filesCount; data++) {
            sb.append(Paths.get(i_FolderPath.toString(), m_Files.get(data).toStringForSha1()).toString().toLowerCase());

            if(data < filesCount - 1) {
                sb.append(System.lineSeparator());
            }
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int filesCount = m_Files.size();

        for(int i = 0; i < filesCount; i++) {
            sb.append(m_Files.get(i).toString());

            if(i+1 < filesCount){
                sb.append(System.lineSeparator());
            }
        }

        return sb.toString();
    }

    public static Folder Parse(File i_FolderZippedFile) throws IOException {
        Folder newFolder = new Folder();
        String rootFolderContent;

        rootFolderContent = FileUtilities.UnzipFile(i_FolderZippedFile.getPath());

        List<String> rootFolderLines = StringUtilities.GetLines(rootFolderContent);

        for(String line: rootFolderLines) {
            String[] parts = line.split(";");
            Folder.Data fileInFolderData = new Folder.Data();
            fileInFolderData.SetName(parts[0]);
            fileInFolderData.SetSHA1(parts[1]);
            fileInFolderData.SetFileType(parts[2].equals("blob")? eFileType.BLOB : eFileType.FOLDER);
            fileInFolderData.SetLastChanger(parts[3]);
            fileInFolderData.SetlastUpdate(parts[4]);
            newFolder.AddFile(fileInFolderData);
        }

        return newFolder;
    }

    public static class Data {
        private String m_Name;
        private String m_SHA1;
        private eFileType m_FileType;
        private String m_LastChanger;
        private String m_LastUpdate;

        @Override
        public String toString(){
            return String.format("%s;%s;%s;%s;%s", m_Name, m_SHA1, m_FileType.toString().toLowerCase(), m_LastChanger, m_LastUpdate);
        }

        public String toStringForSha1() {
            return String.format("%s;%s;%s", m_Name, m_SHA1, m_FileType.toString().toLowerCase());
        }

        public String GetName() {
            return m_Name;
        }

        public void SetName(String i_Name) {
            m_Name = i_Name;
        }

        public String GetSHA1() {
            return m_SHA1;
        }

        public void SetSHA1(String i_SHA1) {
            m_SHA1 = i_SHA1;
        }

        public eFileType GetFileType() {
            return m_FileType;
        }

        public void SetFileType(eFileType i_FileType) {
            m_FileType = i_FileType;
        }

        public String GetLastChanger() {
            return m_LastChanger;
        }

        public void SetLastChanger(String i_LastChanger) {
            m_LastChanger = i_LastChanger;
        }

        public String GetlastUpdate() {
            return m_LastUpdate;
        }

        public void SetlastUpdate(String i_lastUpdate) {
            m_LastUpdate = i_lastUpdate;
        }

        public int compare(Data i_Data) {
            return m_Name.compareTo(i_Data.GetName());
        }

        public static Folder.Data Parse(File i_File, String i_Sha1) {
            Folder.Data data = new Folder.Data();
            boolean isFolder = i_File.isDirectory() ? true : false;

            data.SetFileType(isFolder ? eFileType.FOLDER : eFileType.BLOB);
            data.SetName(i_File.getName());
            data.SetlastUpdate(new SimpleDateFormat(Engine.DATE_FORMAT).format(new Date(i_File.lastModified())));
            data.SetLastChanger(Engine.Creator.GetInstance().GetCurrentUserName());
            data.SetSHA1(i_Sha1);

            return data;
        }
    }
}
