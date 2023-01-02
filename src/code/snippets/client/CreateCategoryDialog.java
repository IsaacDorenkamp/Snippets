package code.snippets.client;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.filechooser.FileFilter;

import org.json.simple.JSONObject;

import code.snippets.models.SnippetType;

import javax.swing.JComboBox;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;

import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;

public class CreateCategoryDialog extends JDialog {
  private static final long serialVersionUID = 5467306213058591485L;
  
  private boolean cancelled = false;
  private JTextField name;
  private FilePicker icon;
  private JComboBox<String> language;
  private JTextField technology;
  private JComboBox<SnippetType> type;

  public CreateCategoryDialog(JFrame parent) {
    super(parent);
    uiconfig();
  }
  
  public boolean isCancelled() {
    return cancelled;
  }
  
  public boolean validateInput() {
    return !name.getText().isEmpty() && !(icon.getFile() == null);
  }
  
  private String encode(File f) {
    try {
      FileInputStream fis = new FileInputStream(f);
      byte[] bytes = fis.readAllBytes();
      fis.close();
      String b64encoded = Base64.getEncoder().encodeToString(bytes);
      return b64encoded;
    } catch(IOException ioe) {
      return null; // TODO - make this more graceful?
    }
  }
  
  @SuppressWarnings("unchecked")
  public JSONObject getPayload() {
    if( !validateInput() ) return null;
    
    JSONObject obj = new JSONObject();
    
    obj.put("name", name.getText());
    // TODO - encode icon into base64
    obj.put("icon", encode(icon.getFile()));
    obj.put("language", language.getSelectedItem());
    obj.put("technology", technology.getText());
    obj.put("type", type.getSelectedItem().toString());
    
    return obj;
  }
  
  private void uiconfig() {
    setTitle("Create Filter Category");
    setSize(300, 250);
    setResizable(false);
    setLocationRelativeTo(getParent());
    
    setModal(true);
    
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent evt) {
        cancelled = true;
      }
    });
    
    name = new JTextField();
    icon = new FilePicker();
    
    icon.setFileFilter(new FileFilter() {
      public String getDescription() {
        return "PNG Images (*.png)";
      }
      
      public boolean accept(File f) {
        if( f.isDirectory() ) {
          return true;
        } else {
          return f.getName().toLowerCase().endsWith(".png");
        }
      }
    });
    
    language = new JComboBox<String>(SnippetCreatorDialog.LANGUAGES);
    technology = new JTextField();
    type = new JComboBox<SnippetType>(SnippetType.values());
    type.setRenderer(new ListCellRenderer<SnippetType>() {
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
    
    setLayout(new GridBagLayout());
    
    GridBagConstraints gbc = new GridBagConstraints();
    
    gbc.insets = new Insets(4, 4, 4, 4);
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0;
    gbc.weighty = 1;
    gbc.anchor = GridBagConstraints.LINE_END;
    
    add(new JLabel("Name: "), gbc);
    gbc.gridy = 1;
    add(new JLabel("Icon: "), gbc);
    gbc.gridy = 2;
    add(new JLabel("Language:"), gbc);
    gbc.gridy = 3;
    add(new JLabel("Technology:"), gbc);
    gbc.gridy = 4;
    add(new JLabel("Snippet Type:"), gbc);
    
    gbc.ipadx = 3;
    gbc.ipady = 3;
    gbc.gridx = 1;
    gbc.weightx = 1;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.BOTH;
    
    add(name, gbc);
    gbc.gridy = 1;
    add(icon, gbc);
    gbc.gridy = 2;
    add(language, gbc);
    gbc.gridy = 3;
    add(technology, gbc);
    gbc.gridy = 4;
    add(type, gbc);
    
    gbc.gridx = 0;
    gbc.gridy = 5;
    gbc.gridwidth = 2;
    gbc.anchor = GridBagConstraints.CENTER;
    
    JPanel buttonPan = new JPanel();
    
    JButton cancel = new JButton("Cancel");
    JButton create = new JButton("Create");
    
    ActionListener al = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent evt) {
        Object src = evt.getSource();
        if( src.equals(cancel) ) {
          cancelled = true;
          setVisible(false);
        } else if( src.equals(create) ) {
          if( !validateInput() ) {
            name.setBackground(Color.WHITE);
            icon.clearRequired();
            
            if( name.getText().isEmpty() ) {
              name.setBackground(Color.RED);
              name.requestFocusInWindow();
            } else if( icon.getFile() == null ) {
              icon.required();
            }
          } else {
            setVisible(false);
          }
        }
      }
    };
    
    cancel.addActionListener(al);
    create.addActionListener(al);
    
    buttonPan.setLayout(new BoxLayout(buttonPan, BoxLayout.LINE_AXIS));
    buttonPan.add(Box.createHorizontalGlue());
    buttonPan.add(cancel);
    buttonPan.add(Box.createRigidArea(new Dimension(7, 0)));
    buttonPan.add(create);
    
    add(buttonPan, gbc);
  }
  
  public static void main(String[] args) {
    CreateCategoryDialog ccd = new CreateCategoryDialog(null);
    ccd.setModal(true);
    ccd.setVisible(true);
    System.exit(0);
  }
}
