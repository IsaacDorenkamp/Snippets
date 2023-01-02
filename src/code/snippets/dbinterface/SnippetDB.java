package code.snippets.dbinterface;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Map;

import code.snippets.models.CodeSnippet;
import code.snippets.models.ModelInterface;
import code.snippets.models.ModelInterfaceException;
import code.snippets.models.SnippetCategory;
import code.snippets.storage.FileStorage;

public class SnippetDB {
  public static final String DATABASE_FILE = "snippets.db";
  
  private static final String DATABASE_CONFIG_FILE = "/data/config.sql";
  
  public static boolean dbFileExists(String filename) throws IOException {
    return (new File(FileStorage.getStorageFolder(), filename)).exists();
  }
  
  public static String getConnectionURL(String filename) throws IOException {
    return "jdbc:sqlite:" + (new File(FileStorage.getStorageFolder(), filename)).getAbsolutePath();
  }
  
  private Connection conn;
  private ModelInterface models;
  
  protected SnippetDB(String dbFile) throws SQLException, IOException {
    boolean doConfigure = false;
    if( !SnippetDB.dbFileExists(dbFile) ) {
      (new File(FileStorage.getStorageFolder(), dbFile)).createNewFile();
      doConfigure = true;
    }
    
    conn = DriverManager.getConnection(SnippetDB.getConnectionURL(dbFile));
    
    if( doConfigure ) configureSnippetDB();
    
    models = new ModelInterface(conn);
  }
  
  public SnippetDB() throws SQLException, IOException {
    this(SnippetDB.DATABASE_FILE);
  }
  
  private void configureSnippetDB() throws SQLException, IOException {
    // load config sql file
    InputStream is = getClass().getResourceAsStream(SnippetDB.DATABASE_CONFIG_FILE);
    
    // assume ASCII encoding
    StringBuilder data = new StringBuilder();
    while( is.available() > 0 ) {
      data.append((char)is.read());
    }
    
    Statement s = conn.createStatement();
    
    String queryData = data.toString();
    String[] queries = queryData.split(";");
    for( String query : queries ) {
      if( query.isEmpty() ) continue;
      s.addBatch(query);
    }
    s.executeBatch();
  }
  
  /**
   * @returns the row ID of the inserted snippet.
   * Also sets the ID of the snippet to the returned id.
   */
  public int insertSnippet(CodeSnippet cs) throws ModelInterfaceException, SQLException {
    int id = models.insertModel(cs);
    cs.id = id;
    return id;
  }
  
  public CodeSnippet getSnippetById(int id) throws ModelInterfaceException, SQLException {
    return (CodeSnippet) models.getByPk(CodeSnippet.class, id);
  }
  
  public boolean deleteSnippetById(int id) throws ModelInterfaceException, SQLException {
    return models.deleteByPk(CodeSnippet.class, id);
  }
  
  public CodeSnippet[] getAllSnippets() throws ModelInterfaceException, SQLException {
    Object[] modelData = models.get(CodeSnippet.class);
    return (CodeSnippet[]) Arrays.copyOf(modelData, modelData.length, CodeSnippet[].class);
  }
  
  public int insertCategory(SnippetCategory sc) throws ModelInterfaceException, SQLException {
    int id = models.insertModel(sc);
    sc.id = id;
    return id;
  }
  
  public SnippetCategory[] getCategories() throws ModelInterfaceException, SQLException {
    Object[] cats = models.get(SnippetCategory.class);
    return (SnippetCategory[]) Arrays.copyOf(cats, cats.length, SnippetCategory[].class);
  }
  
  public SnippetCategory[] getCategoriesWhere(Map<String, Object> constraints) throws ModelInterfaceException, SQLException {
    Object[] cats = models.getWhere(SnippetCategory.class, constraints);
    return (SnippetCategory[]) Arrays.copyOf(cats, cats.length, SnippetCategory[].class);
  }
}
