package code.snippets.client;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class ProgressDialog extends JDialog {
  private static final long serialVersionUID = -6725510592044638353L;

  private JProgressBar monitor;
  
  private int steps;
  
  public ProgressDialog(JFrame parent, String title, int steps) {
    super(parent);
    setTitle(title);
    
    this.steps = steps;
    
    uiConfig(steps);
  }
  public ProgressDialog(JFrame parent, int steps) {
    this(parent, "Progress", steps);
  }
  
  public void setProgress(int curStep, String message) {
    if( curStep > steps ) {
      monitor.setValue(steps);
    } else if( curStep < 0 ) {
      monitor.setValue(0);
    } else {
      monitor.setValue(curStep);
    }
    if( message != null ) monitor.setString(message);
  }
  
  public void setProgress(int curStep) {
    setProgress(curStep, null);
  }
  
  private void uiConfig(int steps) {
    monitor = new JProgressBar(0, steps);
    monitor.setStringPainted(true);
    
    setSize(400, 100);
    setLayout(new BorderLayout());
    setResizable(false);
    
    JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    panel.setLayout(new BorderLayout());
    panel.add(monitor, BorderLayout.CENTER);
    
    add(panel, BorderLayout.CENTER);
  }
}
