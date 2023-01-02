package code.snippets.models;

public enum SnippetType {
  SHORT, CLASS, FUNCTION, MODULE;
  
  public static SnippetType valueOfSafe(String name) {
    try {
      return SnippetType.valueOf(name);
    } catch(IllegalArgumentException iae) { return null; }
  }
  
  public String toNiceString() {
    switch(this) {
    case SHORT:
      return "Short Snippet";
    case CLASS:
      return "Class";
    case FUNCTION:
      return "Function";
    case MODULE:
      return "Module/Complete File";
    default:
      return ""; // should be unreachable
    }
  }
}
