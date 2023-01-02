package code.snippets.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileStorage {
  
  public static final String STORAGE_FOLDER_NAME = ".snippets";
  
  public static File getStorageFolder() throws IOException {
    String homeDir = System.getProperty("user.home");
    File storageFolder = new File(homeDir, FileStorage.STORAGE_FOLDER_NAME);
    
    if( !storageFolder.exists() ) {
      // make storage folder directories, if necessary
      storageFolder.mkdirs();
    }
    
    return storageFolder;
  }
  
  private File baseDir;
  
  public FileStorage(String path) throws FileNotFoundException {
    File f = new File(path);
    if( !f.exists() ) {
      throw new FileNotFoundException("Cannot instantiate FileStorage for non-existent location " + path);
    } else baseDir = f;
  }
  
  public FileStorage() throws FileNotFoundException, IOException {
    this(FileStorage.getStorageFolder().getAbsolutePath());
  }
  
  public File getSubdirectory(String name) throws IOException {
    File sub = new File(baseDir, name);
    if( !sub.exists() ) {
      sub.mkdir();
    }
    return sub;
  }
  
  public File getDataFile(String name) throws IOException {
    File f = new File(baseDir, name);
    if( !f.exists() ) {
      f.createNewFile();
    }
    return f;
  }
  
}
