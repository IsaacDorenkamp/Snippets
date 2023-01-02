package code.snippets.models;

import code.snippets.dbinterface.Table;

import org.json.simple.JSONObject;

import code.snippets.dbinterface.Column;
import code.snippets.dbinterface.PrimaryKey;

@Table(name="categories")
public class SnippetCategory {
  
  public static final SnippetCategory NULL_CATEGORY = new SnippetCategory("All Snippets");
  
  @Column(name="name")
  public String name;
  
  @Column(name="icon")
  public String icon;
  
  @Column(name="language")
  public String language;
  
  @Column(name="technology")
  public String technology;
  
  @Column(name="snippettype")
  public SnippetType type;
  
  @PrimaryKey
  @Column(name="id")
  public Integer id;
  
  public SnippetCategory(String name, String icon, String language, String technology, SnippetType type) {
    this.name = name;
    this.icon = icon;
    this.language = language;
    this.technology = technology;
    this.type = type;
    this.id = null;
  }
  
  public SnippetCategory(String name) {
    this(name, null, null, null, null);
  }
  
  public SnippetCategory() {
    this(null);
  }
  
  public boolean matches(CodeSnippet cs) {
    if( (language == null || (language != null && language.equals(cs.language))) && (technology == null || 
        (technology != null && technology.equals(cs.technology))) && (type == null || (type != null && type.equals(cs.type))) ) {
      return true;
    } else return false;
  }
  
  @SuppressWarnings("unchecked")
  public JSONObject toJSONObject() {
    JSONObject out = new JSONObject();
    out.put("name", name);
    out.put("icon", icon);
    out.put("language", language);
    out.put("technology", technology);
    out.put("type", type.toString());
    out.put("id", id);
    return out;
  }
  
  public static SnippetCategory fromJSONObject(JSONObject obj) {
    String name = (String)obj.get("name");
    if( name.isEmpty() ) name = null;
    
    String language = (String)obj.get("language");
    if( language.isEmpty() ) language = null;
    
    String technology = (String)obj.get("technology");
    if( technology.isEmpty() ) technology = null;
    
    SnippetCategory cat = new SnippetCategory(name, (String)obj.get("icon"), language, technology,
        SnippetType.valueOfSafe((String)obj.get("type")));
    cat.id = ((Long)obj.get("id")).intValue();
    return cat;
  }
  
  @Override
  public String toString() {
    return name;
  }
}
