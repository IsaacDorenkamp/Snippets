package code.snippets.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

public class FilePicker extends JPanel {
  private static final long serialVersionUID = -5227569615186525207L;
  
  private JTextField file;
  private JButton pick;
  private JFileChooser jfc;
  
  public FilePicker() {
    super();
    uiconfig();
  }
  
  public void setFileFilter(FileFilter ff) {
    jfc.setFileFilter(ff);
  }
  
  public File getFile() {
    return jfc.getSelectedFile();
  }
  
  public void required() {
    file.setBackground(Color.decode("#ff6961"));
    file.setText("Please select a file");
    pick.requestFocusInWindow();
  }
  
  public void clearRequired() {
    file.setBackground(getBackground());
  }
  
  private void uiconfig() {
    setLayout(new BorderLayout());
    
    file = new JTextField();
    file.setEditable(false);
    
    pick = new JButton("...");
    jfc = new JFileChooser();
    jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    
    pick.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        jfc.showOpenDialog(null);
        File selected = jfc.getSelectedFile();
        if( selected != null ) file.setText(selected.getAbsolutePath());
        else file.setText("");
      }
    });
    
    add(file, BorderLayout.CENTER);
    add(pick, BorderLayout.EAST);
  }
}
