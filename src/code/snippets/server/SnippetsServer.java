package code.snippets.server;

import java.util.HashMap;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import code.snippets.dbinterface.SnippetDB;
import code.snippets.models.CodeSnippet;
import code.snippets.models.ModelInterfaceException;
import code.snippets.models.SnippetCategory;
import code.snippets.models.SnippetType;
import code.snippets.storage.FileStorage;

public class SnippetsServer extends Server {
  
  public static final int SNIPPETS_PORT = 3194;
  public static final int ICON_SIZE = 32;
  
  private SnippetDB db;
  private FileStorage fs;
  private File snippetData;
  private File icons;
  
  public SnippetsServer(int port) throws SQLException, IOException {
    super(port);
    db = new SnippetDB();
    fs = new FileStorage();
    snippetData = fs.getSubdirectory("snippets");
    icons = fs.getSubdirectory("icons");
  }
  
  public SnippetsServer() throws SQLException, IOException {
    this(SnippetsServer.SNIPPETS_PORT);
  }
  
  private static String readFileSafe(File f) {
    try {
      FileReader fr =  new FileReader(f);
      StringBuilder content = new StringBuilder();
      while( fr.ready() ) content.append((char)fr.read());
      fr.close();
      return content.toString();
    } catch(IOException ioe) { return ""; }
  }
  
  // TODO - Close sessions, allow sessions to expire after some time of inactivity
  public String createAdminSession() {
    String uuid = UUID.randomUUID().toString().replaceAll("-", "");
    ArrayList<Permissions> allPerms = new ArrayList<Permissions>();
    for( Permissions p : Permissions.values() ) allPerms.add(p);
    auth.put(uuid, allPerms);
    return uuid;
  }
  
  public String createReadOnlySession() {
    String uuid = UUID.randomUUID().toString().replaceAll("-", "");
    ArrayList<Permissions> perms = new ArrayList<Permissions>();
    perms.add(Permissions.GET);
    auth.put(uuid, perms);
    return uuid;
  }
  
  private void noauth(Response res) {
    res.setCode(Response.ERROR);
    res.setData("You do not have permission to do that!");
  }
  
  private HashMap<String, ArrayList<Permissions>> auth = new HashMap<String, ArrayList<Permissions>>();

  // TODO - be able to synchronize multiple clients, i.e. when one client creates/deletes a snippet,
  // the others are somehow informed of this and update their models accordingly
  @Override
  @SuppressWarnings("unchecked")
  public Response handleRequest(String command, JSONObject params, String authToken) {
    Response res = new Response();
    
    ArrayList<Permissions> perms = auth.get(authToken);
    if( perms == null ) {
      perms = new ArrayList<Permissions>();
    }
    
    if( command.equals("get") ) {
      if( perms.contains(Permissions.GET) ) {
        try {
          CodeSnippet[] snippets = db.getAllSnippets();
          JSONArray array = new JSONArray();
          
          for( CodeSnippet cs : snippets ) array.add(cs.toJSONObject());
          
          res.setCode(Response.SUCCESS);
          res.setData(array.toJSONString());
        } catch(ModelInterfaceException mie) {
          res.setCode(Response.ERROR);
          res.setData("There was an error constructing the model instances.");
        } catch(SQLException sqle) {
          res.setCode(Response.ERROR);
          res.setData("There was an error fetching the snippet data from the DB.");
        }
      } else {
        noauth(res);
      }
    } else if( command.equals("fetch") ) {
      if( perms.contains(Permissions.GET) ) {
        Object idObj = params.get("id");
        int id;
        if( idObj instanceof Long ) {
          id = ((Long)idObj).intValue();
        } else {
          res.setCode(Response.ERROR);
          res.setData("ID parameter must be an integer.");
          return res;
        }
        
        // fetch snippet content from the snippet id
        try {
          CodeSnippet cs = db.getSnippetById(id);
          String src = cs.source_file;
          File srcFile = new File(snippetData, src);
          if( srcFile.exists() ) {
            // TODO - some errors should not be ignored (like file access errors). These should be addressed for the sake of
            // good form. However, the way this code is written, there should be no issues with this - all the files needed
            // should be accessible unless access has been deliberately manipulated outside of this software.
            String content = SnippetsServer.readFileSafe(srcFile);
            res.setCode(Response.SUCCESS);
            res.setData(content);
          } else {
            res.setCode(Response.ERROR);
            res.setData("The source file for the snippet was not found.");
          }
        } catch(ModelInterfaceException mie) {
          res.setCode(Response.ERROR);
          res.setData("There was an error constructing the model instances.");
        } catch(SQLException sqle) {
          res.setCode(Response.ERROR);
          res.setData("There was an error getting the snippet data from the database.");
        }
      } else {
        noauth(res);
      }
    } else if( command.equals("put") ) {
      if( perms.contains(Permissions.PUT) ) {
        // first, try to construct code snippet from the parameters
        Object lang_obj  = params.get("language");
        Object tech_obj  = params.get("technology");
        Object title_obj = params.get("title");
        Object desc_obj  = params.get("description");
        Object type_obj  = params.get("type");
        Object cont_obj  = params.get("content");
        
        if( lang_obj instanceof String && (tech_obj instanceof String || tech_obj == null) && (title_obj instanceof String)
            && (desc_obj instanceof String || desc_obj == null) && (type_obj instanceof String) && cont_obj instanceof String ) {
          String language  = (String) lang_obj;
          String tech      = tech_obj == null ? null : (String) tech_obj;
          String title     = (String) title_obj;
          String desc      = desc_obj == null ? null : (String) desc_obj;
          SnippetType type = SnippetType.valueOf((String)type_obj);
          String content   = (String) cont_obj;
          
          // first, try to save content to file
          String fname = UUID.randomUUID().toString().replaceAll("-", "");
          File snippetFile = new File(snippetData, fname);
          try {
            snippetFile.createNewFile();
            FileWriter fw = new FileWriter(snippetFile);
            fw.write(content);
            fw.close();
          } catch(IOException ioe) {
            res.setCode(Response.ERROR);
            res.setData("Could not create snippet file.");
            return res;
          }
          
          CodeSnippet snippet = new CodeSnippet(language, tech, title, desc, type, fname);
          try {
            db.insertSnippet(snippet);
            res.setData(snippet.toJSONObject().toJSONString()); // will include new snippet ID
          } catch (ModelInterfaceException e) {
            res.setCode(Response.ERROR);
            res.setData("Failed to extract data from model class instance.");
          } catch (SQLException e) {
            res.setCode(Response.ERROR);
            res.setData("There was an error when attempting to insert the model into the database.");
          }
        }
        
      } else {
        noauth(res);
      }
    } else if( command.equals("delete") ) {
      if( perms.contains(Permissions.DELETE) ) {
        Object idObj = params.get("id");
        int id;
        if( idObj instanceof Long ) {
          id = ((Long)idObj).intValue();
        } else {
          res.setCode(Response.ERROR);
          res.setData("'id' must be a whole number.");
          return res;
        }
        
        try {
          CodeSnippet cs = db.getSnippetById(id);
          File f = new File(snippetData, cs.source_file);
          f.delete();
          boolean success = db.deleteSnippetById(id);
          if( success ) {
            res.setCode(Response.SUCCESS);
            res.setData("Deleted snippet.");
          } else {
            
          }
        } catch(ModelInterfaceException e) {
          res.setCode(Response.ERROR);
          res.setData("Internal server error");
        } catch(SQLException e) {
          res.setCode(Response.ERROR);
          res.setData("Error deleting model from the database.");
        }
      } else {
        noauth(res);
      }
    } else if( command.equals("create_filter") ) {
      if( perms.contains(Permissions.PUT_FILTERS) ) {
        Object name = params.get("name");
        Object icon = params.get("icon");
        Object language = params.get("language");
        Object technology = params.get("technology");
        Object type = params.get("type");
        
        if( name instanceof String && icon instanceof String && (language == null || language instanceof String) &&
            (technology == null || technology instanceof String) &&
            (type instanceof String && SnippetType.valueOfSafe((String)type) != null) ) {
          // check if an equivalent filter already exists
          try {
            HashMap<String, Object> where = new HashMap<String, Object>();
            where.put("name", (String)name);
            where.put("language", (String)language);
            where.put("snippettype", SnippetType.valueOf((String)type));
            SnippetCategory[] existing = db.getCategoriesWhere(where);
            if( existing.length >= 1 ) {
              res.setCode(Response.ERROR);
              res.setData("An identical filter already exists!");
              return res;
            }
          } catch(ModelInterfaceException mie) {
            res.setCode(Response.ERROR);
            res.setData("Model Interface could not instntiate or populate the model class instance.");
            return res;
          } catch(SQLException sqle) {
            res.setCode(Response.ERROR);
            res.setData("Could not get data from the database.");
            return res;
          }
          
          // convert icon data string to ImageIcon
          byte[] data;
          String fileName;
          try {
            data = Base64.getDecoder().decode((String)icon);
          } catch(IllegalArgumentException iae) {
            res.setCode(Response.ERROR);
            res.setData("Icon data is not a base64 encoded image.");
            return res;
          }
          try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
            if( img == null ) {
              res.setCode(Response.ERROR);
              res.setData("Icon data is not a base64 encoded image.");
              return res;
            } else {
              // size constraints
              if( img.getWidth() != SnippetsServer.ICON_SIZE || img.getHeight() != SnippetsServer.ICON_SIZE ) {
                res.setCode(Response.ERROR);
                res.setData(String.format("Icons must be %dpx x %dpx.", SnippetsServer.ICON_SIZE,
                    SnippetsServer.ICON_SIZE));
                return res;
              } else {
                // write to memory
                fileName = UUID.randomUUID().toString().replaceAll("-", "");
                File outputFile = new File(icons, fileName);
                
                FileOutputStream fos = new FileOutputStream(outputFile);
                fos.write(data);
                fos.close();
              }
            }
          } catch (IOException e) {
            res.setCode(Response.ERROR);
            res.setData("Could not store the icon.");
            return res;
          }
          
          // TODO - Test insert category! Connect with category creator dialog on client!
          // then have functionality for fetching/displaying categories on client!
          
          // now store the filter Category!
          SnippetCategory cat = new SnippetCategory((String)name, fileName, (String)language, (String)technology,
              SnippetType.valueOf((String)type));
          try {
            db.insertCategory(cat);
            res.setCode(Response.SUCCESS);
            res.setData(cat.toJSONObject().toJSONString());
          } catch(ModelInterfaceException | SQLException e) {
            res.setCode(Response.ERROR);
            res.setData("Could not insert filter category into database: " + e.toString());
          }
        } else {
          res.setCode(Response.ERROR);
          res.setData("Invalid payload.");
        }
      } else {
        noauth(res);
      }
    } else if( command.equals("get_filters") ) {
      try {
        SnippetCategory[] sc = db.getCategories();
        JSONArray arr = new JSONArray();
        for( SnippetCategory cat : sc ) arr.add(cat.toJSONObject());
        
        res.setCode(Response.SUCCESS);
        res.setData(arr.toJSONString());
      } catch(ModelInterfaceException | SQLException e) {
        res.setCode(Response.ERROR);
        res.setData("Could not fetch data.");
      }
    } else if( command.equals("get_icon") ) {
      Object icon_o = params.get("icon");
      if( icon_o instanceof String ) {
        String icon = (String) icon_o;
        File iconFile = new File(icons, icon);
        try {
          FileInputStream fis = new FileInputStream(iconFile);
          byte[] data = fis.readAllBytes();
          fis.close();
          String enc = Base64.getEncoder().encodeToString(data);
          res.setCode(Response.SUCCESS);
          res.setData(enc);
        } catch(IOException ioe) {
          res.setCode(Response.ERROR);
          res.setData("Could not get icon.");
        }
      } else {
        res.setCode(Response.ERROR);
        res.setData("get_icon request must include an 'icon' field of type String specifying the icon to get.");
      }
    } else if( command.equals("authenticate") ) {
      // TODO: Implement "plug-n-play" authentication methods
      // For now, just return a "read-only" permissions set
      String token = createReadOnlySession();
      res.setCode(Response.SUCCESS);
      res.setData(token);
    } else {
      res.setCode(Response.ERROR);
      res.setData(String.format("Invalid command '%s.'", command));
    }
    
    return res;
  }
  
  public static void main(String[] args) {
    SnippetsServer server;
    try {
      System.out.println("Creating server instance...");
      server = new SnippetsServer();
      System.out.println("Initializing server...");
      server.initialize();
      System.out.println("Creating admin session...");
      String uuid = server.createAdminSession();
      System.out.println("Admin session token: " + uuid);
      server.run();
    } catch (SQLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
