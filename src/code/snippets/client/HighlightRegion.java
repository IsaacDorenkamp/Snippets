package code.snippets.client;

public class HighlightRegion {
  
  public static enum HighlightReason {
    STRING, COMMENT, OTHER
  }
  
  private String content;
  private HighlightReason reason;
  private HighlightRule rule;
  
  public HighlightRegion(String content, HighlightReason reason, HighlightRule rule) {
    this.content = content;
    this.reason = reason;
    this.rule = rule;
  }
  
  public String getContent() {
    return content;
  }
  
  public HighlightReason getReason() {
    return reason;
  }
  
  public HighlightRule getRule() {
    return rule;
  }
}
