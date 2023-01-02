package code.snippets.client;

import java.awt.Color;

public class HighlightRule {
  public String name;
  public Color color;
  public boolean bold;
  public boolean italic;
  
  public HighlightRule(String name, Color color, boolean bold, boolean italic) {
    this.name = name;
    this.color = color;
    this.bold = bold;
    this.italic = italic;
  }
  
  @Override
  public boolean equals(Object other) {
    if( !(other instanceof HighlightRule) ) {
      return false;
    } else {
      HighlightRule otherRule = (HighlightRule) other;
      return name.equals(otherRule.name) && color.equals(otherRule.color) && bold == otherRule.bold && italic == otherRule.italic;
    }
  }
}
