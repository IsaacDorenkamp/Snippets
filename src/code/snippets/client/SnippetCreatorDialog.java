package code.snippets.client;

import code.snippets.models.SnippetType;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;

import org.json.simple.JSONObject;

public class SnippetCreatorDialog extends JDialog {
  
  private static final long serialVersionUID = 291818656484504004L;

  // this may be temporary, may make dynamic in the future
  public static final String[] LANGUAGES = {
      "Java", "Python", "JavaScript", "HTML", "CSS", "PHP", "Perl", "C/C++", "C#", "Objective-C", "Ruby", "Swift"
  };
  
  private JComboBox<String> language;
  private JTextField technology; // may make this a combo box in the future, dynamically changing depending on the language
  private JTextField title;
  private JTextArea description;
  private JComboBox<SnippetType> snippetType;
  private FilePicker file;
  
  private JButton ok;
  private JButton cancel;
  
  private boolean cancelled = false;
  
  public SnippetCreatorDialog(JFrame parent) {
    super(parent, true);
    setTitle("Create Snippet");
    setSize(300, 350);
    setLocationRelativeTo(parent);
    uiconfig();
  }
  
  private boolean validateInput() {
    return !title.getText().isBlank() && file.getFile() != null && file.getFile().exists();
  }
  
  public boolean isCancelled() {
    return cancelled;
  }
  
  @SuppressWarnings("unchecked")
  public JSONObject getPutPayload() throws IOException {
    if( !validateInput() ) return null;
    JSONObject payload = new JSONObject();
    payload.put("language", language.getSelectedItem());
    payload.put("technology", technology.getText().isBlank() ? null : technology.getText());
    payload.put("title", title.getText());
    payload.put("description", description.getText().isBlank() ? null : description.getText());
    payload.put("type", snippetType.getSelectedItem().toString());
    
    // read the selected file
    FileReader fr = new FileReader(file.getFile());
    StringBuilder content = new StringBuilder();
    while( fr.ready() ) {
      content.append((char)fr.read());
    }
    fr.close();
    
    payload.put("content", content.toString());
    return payload;
  }
  
  private void uiconfig() {
    
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent evt) {
        cancelled = true;
      }
    });
    
    language = new JComboBox<String>(SnippetCreatorDialog.LANGUAGES);
    technology = new JTextField();
    title = new JTextField();
    description = new JTextArea();
    description.setFont(technology.getFont());
    description.setLineWrap(true);
    snippetType = new JComboBox<SnippetType>(SnippetType.values());
    file = new FilePicker();
   
    
    // TODO - copy pasted to CreateCategoryDialog.java, should I instead create a class? yes, but I'm lazy for now
    snippetType.setRenderer(new ListCellRenderer<SnippetType>() {
      @Override
      public Component getListCellRendererComponent(JList<? extends SnippetType> list, SnippetType value, int index, boolean isSelected,
          boolean cellHasFocus) {
        JLabel l = new JLabel(value.toNiceString());
        if( index == -1 ) {
          l.setOpaque(false);
        } else if( isSelected ) {
          l.setOpaque(true);
          l.setBackground(list.getSelectionBackground());
          l.setForeground(list.getSelectionForeground());
        } else {
          l.setOpaque(true);
          l.setBackground(list.getBackground());
          l.setForeground(list.getForeground());
        }
        return l;
      }
     
    });
   
    cancel = new JButton("Cancel");
    cancel.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        cancelled = true;
        setVisible(false);
      }
    });
   
    ok = new JButton("Create");
    ok.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        if( validateInput() ) {
          setVisible(false);
        } else {
          title.setBackground(Color.WHITE);
          file.clearRequired();
          
          if( title.getText().isEmpty() ) {
            title.setBackground(Color.decode("#ff6961"));
            title.requestFocusInWindow();
          } else {
            file.required();
          }
        }
      }
    });
   
    setLayout(new GridBagLayout());
   
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0;
    gbc.weighty = 0;
    gbc.anchor = GridBagConstraints.LINE_END;
    add(new JLabel("Language:"), gbc);
    gbc.gridy = 1;
    add(new JLabel("Technology:"), gbc);
    gbc.gridy = 2;
    add(new JLabel("Title:"), gbc);
    gbc.gridy = 3;
    gbc.gridheight = 2;
    gbc.weighty = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Description:"), gbc);
    gbc.anchor = GridBagConstraints.LINE_END;
    gbc.weighty = 0;
    gbc.gridheight = 1;
    gbc.gridy = 4;
    add(new JPanel(), gbc);
    gbc.gridy = 5;
    add(new JLabel("Type: "), gbc);
    gbc.gridy = 6;
    add(new JLabel("Content: "), gbc);
    
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.weightx = 1;
    gbc.ipadx = 3;
    gbc.ipady = 3;
    gbc.fill = GridBagConstraints.BOTH;
    
    add(language, gbc);
    gbc.gridy = 1;
    add(technology, gbc);
    gbc.gridy = 2;
    add(title, gbc);
    gbc.gridy = 3;
    gbc.gridheight = 2;
    gbc.weighty = 1;
    add(description, gbc);
    gbc.weighty = 0;
    gbc.gridheight = 1;
    gbc.gridy = 5;
    add(snippetType, gbc);
    gbc.gridy = 6;
    add(file, gbc);
    
    gbc.gridx = 0;
    gbc.gridy = 7;
    gbc.gridwidth = 2;
    
    JPanel buttonPan = new JPanel();
    buttonPan.setLayout(new BoxLayout(buttonPan, BoxLayout.LINE_AXIS));
    buttonPan.add(Box.createHorizontalGlue());
    buttonPan.add(cancel);
    buttonPan.add(Box.createRigidArea(new Dimension(10, 0)));
    buttonPan.add(ok);
    
    add(buttonPan, gbc);
  }
  
  public static void main(String[] args) {
    SnippetCreatorDialog scd = new SnippetCreatorDialog(null);
    scd.setVisible(true);
    System.exit(0);
  }
}
