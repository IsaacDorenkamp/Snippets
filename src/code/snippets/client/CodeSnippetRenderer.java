package code.snippets.client;

import code.snippets.models.CodeSnippet;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class CodeSnippetRenderer implements ListCellRenderer<CodeSnippet> {
  
  public static final Color OFF_COLOR = Color.decode("#dddddd");

  @Override
  public Component getListCellRendererComponent(JList<? extends CodeSnippet> list, CodeSnippet value, int index,
      boolean isSelected, boolean cellHasFocus) {
    CodeSnippetCard csc = new CodeSnippetCard(value);
    
    if( isSelected ) {
      csc.select(list);
    } else {
      csc.deselect(list, index, CodeSnippetRenderer.OFF_COLOR);
    }
    
    return csc;
  }

}
