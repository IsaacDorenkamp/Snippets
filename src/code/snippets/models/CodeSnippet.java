package code.snippets.models;

import code.snippets.dbinterface.Table;

import org.json.simple.JSONObject;

import code.snippets.dbinterface.Column;
import code.snippets.dbinterface.PrimaryKey;

@Table(name="code_snippets")
public class CodeSnippet {
  @Column(name="language")
  public String language;
  
  @Column(name="technology")
  public String technology;
  
  @Column(name="title")
  public String title;
  
  @Column(name="description")
  public String description;
  
  @Column(name="snippettype")
  public SnippetType type;
  
  @Column(name="source_file")
  public String source_file;
  
  @PrimaryKey
  @Column(name="id", generated=true)
  public Integer id;
  
  public CodeSnippet(String language, String technology, String title, String description, SnippetType type, String source_file) {
    this.language = language;
    this.technology = technology;
    this.title = title;
    this.description = description;
    this.type = type;
    this.source_file = source_file;
    
    this.id = null;
  }
  
  public CodeSnippet() {
    this(null, null, null, null, null, null);
  }
  
  @Override
  public String toString() {
    return String.format("CodeSnippet\n\tLanguage: %s\n\tTechnology: %s\n\tTitle: %s\n\tDescription: %s\n\tType: %s\n\tId: %d",
        language, technology, title, description, type.toString(), id);
  }
  
  // TODO - implement a method in ModelInterface to do these conversions???
  @SuppressWarnings("unchecked")
  public JSONObject toJSONObject() {
    JSONObject obj = new JSONObject();
    obj.put("language", language);
    obj.put("technology", technology);
    obj.put("title", title);
    obj.put("description", description);
    obj.put("source_file", source_file);
    obj.put("type", type.toString());
    obj.put("id", id);
    return obj;
  }
  
  public static CodeSnippet fromJSONObject(JSONObject j) {
    CodeSnippet cs = new CodeSnippet((String) j.get("language"), (String) j.get("technology"), (String) j.get("title").toString(),
        (String) j.get("description"), SnippetType.valueOf((String) j.get("type")), (String) j.get("source_file"));
    cs.id = ((Long) j.get("id")).intValue();
    return cs;
  }
}
