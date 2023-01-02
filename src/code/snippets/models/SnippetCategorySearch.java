package code.snippets.models;

public class SnippetCategorySearch extends SnippetCategory {
  private String search;
  
  public SnippetCategorySearch(SnippetCategory cat, String search) {
    super(cat.name, cat.icon, cat.language, cat.technology, cat.type);
    this.search = search;
  }
  
  public SnippetCategorySearch(String search) {
    this(SnippetCategory.NULL_CATEGORY, search);
  }
  
  @Override
  public boolean matches(CodeSnippet snippet) {
    return super.matches(snippet) && snippet.title.matches(String.format("^(?i).*%s.*$", search));
  }
}
