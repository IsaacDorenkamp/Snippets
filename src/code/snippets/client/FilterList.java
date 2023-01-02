package code.snippets.client;

import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListModel;

import code.snippets.models.CodeSnippet;
import code.snippets.models.SnippetCategory;

public class FilterList extends JList<CodeSnippet> {
  private static final long serialVersionUID = -6574385681584904236L;
  private DefaultListModel<CodeSnippet> dlm = new DefaultListModel<CodeSnippet>();
  private ArrayList<CodeSnippet> items;
  private SnippetCategory filter = null;
  
  public FilterList() {
    super();
    super.setModel(dlm);
    items = new ArrayList<>();
  }
  
  @Override
  public void setModel(ListModel<CodeSnippet> lm) {
    throw new RuntimeException("Not implemented"); // prevent setModel on FilterList since the model is managed internally
  }
  
  public void addItem(CodeSnippet cs) {
    items.add(cs);
    if( filter == null || (filter != null && filter.matches(cs)) ) {
      dlm.addElement(cs);
    }
  }
  
  public boolean removeItem(CodeSnippet cs) {
    return items.remove(cs);
  }
  
  public boolean removeItemById(int id) {
    for( int idx = 0; idx < items.size(); idx++ ) {
      CodeSnippet cs = items.get(idx);
      if( cs.id == id ) {
        items.remove(idx);
        return true;
      }
    }
    return false;
  }
  
  public void setFilter(SnippetCategory cat) {
    filter = cat;
    dlm.clear();
    if( filter == null ) {
      dlm.addAll(items);
    } else {
      for( CodeSnippet snippet : items ) {
        if( filter.matches(snippet) ) {
          dlm.addElement(snippet);
        }
      }
    }
  }
}
