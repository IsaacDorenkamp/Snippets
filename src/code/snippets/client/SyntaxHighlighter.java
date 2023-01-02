package code.snippets.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class SyntaxHighlighter {
  public static final String LINE_COMMENT = "line";
  public static final String INLINE_COMMENT = "inline";
  
  public static final String STRING_LINE = "line";
  public static final String STRING_INLINE = "inline";
  
  private HashMap<String, HighlightRule> rules;
  private HashMap<String, String> wordToRule;
  private HashMap<RegionType, String> commentTypeToRule;
  private HashMap<RegionType, String> stringTypeToRule;
  
  private HashMap<String, RegionType> stringStartToEnd;
  private HashMap<String, RegionType> commentStartToEnd;
  
  public SyntaxHighlighter() {
    rules = new HashMap<>();
    wordToRule = new HashMap<>();
    commentTypeToRule = new HashMap<>();
    stringTypeToRule = new HashMap<>();
    commentStartToEnd = new HashMap<>();
    stringStartToEnd = new HashMap<>();
  }
  
  public void addRule(HighlightRule rule) {
    rules.put(rule.name, rule);
  }
  
  public void addWord(String ruleName, String word) {
    assert rules.containsKey(ruleName) : "Cannot add word to non-existent rule";
    wordToRule.put(word, ruleName);
  }
  
  
  // TODO - enforce mutual exclusivity between comment start/string start char sequences?
  public void addCommentType(String ruleName, String commentType, String start, String end) {
    assert rules.containsKey(ruleName) : "Cannot add comment to non-existent rule";
    RegionType rt = new RegionType(start, end);
    commentTypeToRule.put(rt, ruleName);
    commentStartToEnd.put(start, rt);
  }
  
  public void addStringType(String ruleName, String stringType, String start, String end) {
    assert rules.containsKey(ruleName) : "Cannot add string type to non-existent rule";
    RegionType rt = new RegionType(start, end);
    stringTypeToRule.put(rt, ruleName);
    stringStartToEnd.put(start, rt);
  }
  
  private ArrayList<HighlightRegion> preliminaryStage(String source) {
    String inString = null;
    String inComment = null;
    
    int stringStart = -1;
    int commentStart = -1;
    int regularStart = 0;
    
    ArrayList<HighlightRegion> regions = new ArrayList<>();
    
    for( int idx = 0; idx < source.length(); idx++ ) {
      // if we are already in a string or a comment, we can
      // use look-behind instead of look-ahead because we
      // are only testing one candidate (the specified string
      // or comment end marker). We have to use look-ahead for
      // the start of these, however.
      if( inString != null ) {
        String stringEnd = stringStartToEnd.get(inString).getEnd();
        int startIdx = idx - (stringEnd.length() - 1);
        if( startIdx >= 0 ) {
          String lastNChars = source.substring(startIdx, idx+1);
          if( lastNChars.equals(stringEnd) ) {
            HighlightRule rule = rules.get(stringTypeToRule.get(stringStartToEnd.get(inString)));
            regions.add(new HighlightRegion(inString + source.substring(stringStart, startIdx) + stringEnd,
                HighlightRegion.HighlightReason.STRING,rule));
            inString = null;
            stringStart = -1;
            regularStart = idx + 1;
          }
        }
      } else if( inComment != null ) {
        String commentEnd = commentStartToEnd.get(inComment).getEnd();
        int startIdx = idx - (commentEnd.length() - 1);
        if( startIdx >= 0 ) {
          String lastNChars = source.substring(startIdx, idx+1);
          if( lastNChars.equals(commentEnd) ) {
            HighlightRule rule = rules.get(commentTypeToRule.get(commentStartToEnd.get(inComment)));
            regions.add(new HighlightRegion(inComment + source.substring(commentStart, startIdx) + commentEnd,
                HighlightRegion.HighlightReason.COMMENT, rule));
            inComment = null;
            commentStart = -1;
            regularStart = idx + 1;
          }
        }
      } else {
        // check if there is an indication to start a string
        ArrayList<String> candidates = new ArrayList<>();
        for( String starter : stringStartToEnd.keySet() ) {
          if( starter.charAt(0) == source.charAt(idx) ) {
            candidates.add(starter);
          }
        }
        candidates.sort(new Comparator<String>() {
          @Override
          public int compare(String a, String b) {
            if( a.equals(b) ) return 0;
            else return b.length() - a.length();
          }
        });
        
        String chosen = null;
        for( String candidate : candidates ) {
          // look ahead time!
          if( candidate.length() == 1 ) {
            chosen = candidate;
            break;
          } else {
            boolean consistent = true;
            for( int i = 1; i < candidate.length(); i++ ) {
              if( idx + i >= source.length() ) break; // it's definitely not this one!
              else {
                if( source.charAt(idx+i) != candidate.charAt(i) ) {
                  consistent = false;
                  break;
                }
              }
            }
            if( consistent ) {
              // this is the one
              chosen = candidate;
              break;
            }
          }
        }
        
        if( chosen != null ) {
          // add block of regular text
          String regular = source.substring(regularStart, idx);
          regions.add(new HighlightRegion(regular, HighlightRegion.HighlightReason.OTHER, null));
          inString = chosen;
          stringStart = idx + chosen.length();
          idx += chosen.length() - 1; // skip over the start
          continue;
        }
        
        // now for comments
        candidates = new ArrayList<>();
        for( String starter : commentStartToEnd.keySet() ) {
          if( starter.charAt(0) == source.charAt(idx) ) {
            candidates.add(starter);
          }
        }
        candidates.sort(new Comparator<String>() {
          @Override
          public int compare(String a, String b) {
            if( a.equals(b) ) return 0;
            else return b.length() - a.length();
          }
        });
        
        chosen = null;
        for( String candidate : candidates ) {
          // look ahead time!
          if( candidate.length() == 1 ) {
            chosen = candidate;
            break;
          } else {
            boolean consistent = true;
            for( int i = 1; i < candidate.length(); i++ ) {
              if( idx + i >= source.length() ) break; // it's definitely not this one!
              else {
                if( source.charAt(idx+i) != candidate.charAt(i) ) {
                  consistent = false;
                  break;
                }
              }
            }
            if( consistent ) {
              // this is the one
              chosen = candidate;
              break;
            }
          }
        }
        
        if( chosen != null ) {
          // add block of regular text
          String regular = source.substring(regularStart, idx);
          regions.add(new HighlightRegion(regular, HighlightRegion.HighlightReason.OTHER, null));
          inComment = chosen;
          commentStart = idx + chosen.length();
          idx += chosen.length() - 1; // skip over the start
          continue;
        }
      }
    }
    
    if(inString != null) {
      regions.add(new HighlightRegion(source.substring(stringStart, source.length()), HighlightRegion.HighlightReason.STRING,
          rules.get(stringTypeToRule.get(stringStartToEnd.get(inString)))));
    } else if(inComment != null) {
      regions.add(new HighlightRegion(source.substring(commentStart, source.length()), HighlightRegion.HighlightReason.COMMENT,
          rules.get(commentTypeToRule.get(commentStartToEnd.get(inComment)))));
    } else {
      regions.add(new HighlightRegion(source.substring(regularStart, source.length()), HighlightRegion.HighlightReason.OTHER,
          null));
    }
    
    return regions;
  }
  
  public void apply(JTextPane textPane) {
    StyledDocument doc = textPane.getStyledDocument();
    String content = textPane.getText();
    
    textPane.setText("");
    
    int style = 0;
    
    ArrayList<HighlightRegion> regions = preliminaryStage(content);
    for( HighlightRegion r : regions ) {
      HighlightRule rule = r.getRule();
      Style s = doc.addStyle("style" + (style++), null);
      if( rule != null ) {
        StyleConstants.setForeground(s, rule.color);
        StyleConstants.setBold(s, rule.bold);
        StyleConstants.setItalic(s, rule.italic);
      }
    }
    
    ArrayList<HighlightRegion> secondary = new ArrayList<>();
    for( HighlightRegion region : regions ) {
      HighlightRegion.HighlightReason reason = region.getReason();
      if( reason == HighlightRegion.HighlightReason.COMMENT || reason == HighlightRegion.HighlightReason.STRING) {
        // comments and strings are handled in the preliminary stage, these regions pass through untouched
        secondary.add(region);
      } else {
        // this is where real highlighting needs to happen
      
        String regionContent = region.getContent();
        ArrayList<String> pieces = new ArrayList<String>();
        boolean inAlphaToken = false;
        StringBuilder piece = new StringBuilder();
        for( char c : regionContent.toCharArray() ) {
          String cs = String.valueOf(c);
          if( inAlphaToken ) {
            if( !cs.matches("^[a-zA-Z]$") ) {
              if( piece.length() > 0 ) {
                pieces.add(piece.toString());
                piece.setLength(0);
              }
              piece.append(c);
              inAlphaToken = false;
            } else {
              piece.append(c);
            }
          } else {
            if( cs.matches("^[a-zA-Z]$") ) {
              if( piece.length() > 0 ) {
                pieces.add(piece.toString());
                piece.setLength(0);
              }
              piece.append(c);
              inAlphaToken = true;
            } else {
              piece.append(c);
            }
          }
        }
        if( piece.length() > 0 ) {
          pieces.add(piece.toString());
        }
        
        // now for the easy part - applying highlighting rules
        for( String section : pieces ) {
          if( section.matches("^[A-Za-z]+$") && wordToRule.containsKey(section) ) {
            String ruleName = wordToRule.get(section);
            HighlightRule rule = rules.get(ruleName);
            if( doc.getStyle(ruleName) == null ) {
              Style s = doc.addStyle(ruleName, null);
              StyleConstants.setForeground(s, rule.color);
              StyleConstants.setBold(s, rule.bold);
              StyleConstants.setItalic(s, rule.italic);
            }
            secondary.add(new HighlightRegion(section, HighlightRegion.HighlightReason.OTHER, rule));
          } else {
            secondary.add(new HighlightRegion(section, HighlightRegion.HighlightReason.OTHER, null));
          }
        }
      }
    }
    
    // now, place the regions into the document
    int anonStyle = 0;
    for( HighlightRegion hr : secondary ) {
      String regionText = hr.getContent();
      String ruleName = wordToRule.get(regionText);
      Style s = null;
      if( ruleName != null ) {
        s = doc.getStyle(ruleName);
      } else {
        HighlightRule rule = hr.getRule();
        if( rule != null ) {
          s = doc.addStyle("style" + (anonStyle++), null);
          StyleConstants.setForeground(s, rule.color);
          StyleConstants.setBold(s, rule.bold);
          StyleConstants.setItalic(s, rule.italic);
        }
      }
      try {
        doc.insertString(doc.getLength(), regionText, s);
        
        // despite the fact that the textPane isn't editable,
        // there is still a caret position which affects scroll
        // position. We need the caret position to be at the top
        // so that the scroll pane will be scrolled to the top.
        textPane.setCaretPosition(0);
      } catch(BadLocationException e) {}
    }
  }
  
  @Override
  public String toString() {
    StringBuilder res = new StringBuilder();
    HashMap<String, ArrayList<String>> ruleToWords = new HashMap<>();
    for( String word : wordToRule.keySet() ) {
      String rule = wordToRule.get(word);
      if(ruleToWords.containsKey(rule)) {
        ArrayList<String> words = ruleToWords.get(rule);
        words.add(word);
      } else {
        ArrayList<String> words = new ArrayList<>();
        words.add(word);
        ruleToWords.put(rule, words);
      }
    }
    
    boolean first = true;
    for( String rule : ruleToWords.keySet() ) {
      res.append((!first ? "\n" : "") + rule);
      first = false;
      for( String word : ruleToWords.get(rule) ) {
        res.append("\n\t" + word);
      }
    }
    
    return res.toString();
  }
}
