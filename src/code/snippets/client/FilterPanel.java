package code.snippets.client;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import code.snippets.models.SnippetCategory;
import code.snippets.models.SnippetCategorySearch;

public class FilterPanel extends JPanel {
  private static final long serialVersionUID = 6403485547943166062L;
  
  private JTextField search;
  private JComboBox<SnippetCategory> cat;
  private IconLoader loader;
  
  public FilterPanel(IconLoader loader) {
    this.loader = loader;
    
    uiconfig();
  }
  
  public void addCategory(SnippetCategory c) {
    cat.addItem(c);
  }
  
  public SnippetCategory getCategory() {
    if( search.getText().isEmpty() ) return (SnippetCategory) cat.getSelectedItem();
    else return new SnippetCategorySearch((SnippetCategory) cat.getSelectedItem(), search.getText());
  }
  
  public void onChange(Runnable r) {
    cat.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent evt) {
        r.run();
      }
    });
    
    // whenever search changes, update category
    search.getDocument().addDocumentListener(new DocumentListener() {

      @Override
      public void insertUpdate(DocumentEvent e) {
        r.run();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        r.run();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {}
      
    });
  }
  
  public void removeOnChange(ActionListener al) {
    cat.removeActionListener(al);
  }
  
  private void uiconfig() {
    setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    
    search = new JTextField();
    
    cat = new JComboBox<SnippetCategory>();
    cat.setRenderer(new CategoryRenderer(loader));
    cat.addItem(SnippetCategory.NULL_CATEGORY);
    
    setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.ipadx = 3;
    gbc.ipady = 3;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new Insets(3, 3, 3, 3  );
    gbc.weighty = 1;
    gbc.weightx = 0;
    
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.LINE_END;
    add(new JLabel("Search by Title:"), gbc);
    gbc.fill = GridBagConstraints.BOTH;
    
    gbc.gridx = 1;
    gbc.gridwidth = 2;
    gbc.weightx = 1;
    
    add(search, gbc);
    
    gbc.weightx = 0;
    gbc.gridy = 1;
    gbc.gridx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.insets = new Insets(0, 3, 3, 3);
    
    add(new JLabel("Category:"), gbc);
    
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1;
    gbc.gridx = 1;
    gbc.gridwidth = 2;
    
    add(cat, gbc);
  }
}
