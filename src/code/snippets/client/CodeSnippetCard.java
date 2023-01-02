package code.snippets.client;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import code.snippets.models.CodeSnippet;

public class CodeSnippetCard extends JPanel {
  private static final long serialVersionUID = 5924027493746637032L;
  
  public CodeSnippetCard(CodeSnippet cs) {
    JLabel title = new JLabel(cs.title);
    JLabel language = new JLabel(String.format("Language: %s", cs.language));
    JLabel tech = new JLabel(String.format("Technology: %s", cs.technology));

    setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    
    Font titleFont = title.getFont();
    title.setFont(new Font(titleFont.getName(), Font.PLAIN, 14));

    add(title);
    add(language);
    if( cs.technology != null ) add(tech);
    
    if( cs.description != null ) {
      JTextArea desc  = new JTextArea();
      desc.setText("    " + cs.description);
      desc.setOpaque(false);
      desc.setLineWrap(true);
    
    
    
      desc.setFont(titleFont);
      FontMetrics fm = getFontMetrics(titleFont);
    
      int lines = 1;
      StringBuilder curLine = new StringBuilder();
      for( char c : ("    " + cs.description).toCharArray() ) {
        double strWidth = fm.stringWidth(curLine.toString() + c);
        if( strWidth < 250 ) {
          curLine.append(c);
        } else {
          lines++;
          curLine.setLength(0);
        }
      }
      desc.setRows(lines);
    
      add(Box.createVerticalStrut(5));
      add(desc);
    }
  }
  
  public void select(JList<? extends CodeSnippet> list) {
    setBackground(list.getSelectionBackground());
    setForeground(list.getSelectionForeground());
  }
  
  public void deselect(JList<? extends CodeSnippet> list, int idx, Color offColor) {
    if( idx % 2 == 1 ) {
      setBackground(list.getBackground());
    } else {
      setBackground(offColor);
    }
    setForeground(list.getForeground());
  }
  
}
