package code.snippets.client;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import code.snippets.server.SnippetsServer;

import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.awt.Dimension;
import java.awt.GridBagConstraints;

public class ConnectDialog extends JDialog {
  private static final long serialVersionUID = 7716459767391801714L;
  
  private JRadioButton local;
  private JRadioButton remote;
  
  private JTextField remoteServer;
  private JSpinner port;
  
  private boolean cancelled = false;

  public ConnectDialog(JFrame parent) {
    super(parent);
    setTitle("Connect to Snippets Server");
    setModal(true);
    uiconfig();
  }
  
  public String getServerAddress() {
    if( local.isSelected() ) {
      return "127.0.0.1";
    } else {
      return remoteServer.getText();
    }
  }
  
  public Integer getPort() {
    return (Integer)port.getValue();
  }
  
  public boolean isCancelled() {
    return cancelled;
  }
  
  private void uiconfig() {
    setSize(300, 200);
    setLocationRelativeTo(getParent());
    
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent evt) {
        cancelled = true;
        setVisible(false);
      }
    });
    
    // host panel
    JPanel hostSelect = new JPanel();
    local = new JRadioButton();
    remote = new JRadioButton();
    
    remoteServer = new JTextField();
    remoteServer.setEnabled(false);
    
    Action updateTextField = new Action() {

      @Override
      public void actionPerformed(ActionEvent e) {
        remoteServer.setEnabled(remote.isSelected());
      }

      @Override
      public Object getValue(String key) {
        return null;
      }

      @Override
      public void putValue(String key, Object value) {}

      @Override
      public void setEnabled(boolean b) {}

      @Override
      public boolean isEnabled() {
        return true;
      }

      @Override
      public void addPropertyChangeListener(PropertyChangeListener listener) {}

      @Override
      public void removePropertyChangeListener(PropertyChangeListener listener) {}
      
    };
    
    local.setAction(updateTextField);
    remote.setAction(updateTextField);
    
    local.setSelected(true);
    
    hostSelect.setLayout(new GridBagLayout());
    hostSelect.setBorder(BorderFactory.createTitledBorder("Host"));
    
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weighty = 1;
    gbc.fill = GridBagConstraints.BOTH;
    
    hostSelect.add(local, gbc);
    gbc.gridy = 1;
    hostSelect.add(remote, gbc);
    gbc.gridy = 0;
    gbc.gridx = 1;
    gbc.weightx = 1;
    hostSelect.add(new JLabel("Local Server"), gbc);
    gbc.gridy = 1;
    hostSelect.add(remoteServer, gbc);
    
    ButtonGroup hostOptions = new ButtonGroup();
    hostOptions.add(local);
    hostOptions.add(remote);
    
    // button panel
    JButton cancel = new JButton("Cancel");
    JButton connect = new JButton("Connect");
    JPanel buttonPan = new JPanel();
    buttonPan.setLayout(new BoxLayout(buttonPan, BoxLayout.LINE_AXIS));
    buttonPan.add(Box.createHorizontalGlue());
    buttonPan.add(cancel);
    buttonPan.add(Box.createRigidArea(new Dimension(5, 0)));
    buttonPan.add(connect);
    
    cancel.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent evt) {
        cancelled = true;
        setVisible(false);
      }
    });
    
    connect.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent evt) {
        cancelled = false;
        setVisible(false);
      }
    });
    
    // master layout
    setLayout(new GridBagLayout());
    
    GridBagConstraints gbc2 = new GridBagConstraints();
    gbc2.gridx = 0;
    gbc2.gridy = 0;
    gbc2.insets = new Insets(5, 5, 5, 5);
    gbc2.weighty = 1;
    gbc2.weightx = 1;
    gbc2.fill = GridBagConstraints.BOTH;
    gbc2.gridwidth = 2;
    
    add(hostSelect, gbc2);
    
    SpinnerNumberModel port_model = new SpinnerNumberModel(SnippetsServer.SNIPPETS_PORT, 0, 65535, 1);
    
    port = new JSpinner(port_model);
    JSpinner.NumberEditor num = new JSpinner.NumberEditor(port, "#");
    num.getTextField().setHorizontalAlignment(JTextField.CENTER);
    port.setEditor(num);
    port.addMouseWheelListener(new SpinnerScroller(port));
    
    gbc2.gridwidth = 1;
    gbc2.weightx = 0;
    gbc2.gridy = 1;
    gbc2.weighty = 0;
    add(new JLabel("Port:"), gbc2);
    gbc2.gridx = 1;
    gbc2.fill = GridBagConstraints.NONE;
    gbc2.anchor = GridBagConstraints.LINE_START;
    gbc2.ipadx = 5;
    gbc2.ipady = 5;
    add(port, gbc2);
    gbc2.fill = GridBagConstraints.BOTH;
    
    gbc2.gridx = 0;
    gbc2.gridy = 2;
    gbc2.gridwidth = 2;
    add(buttonPan, gbc2);
  }
  
  public static void main(String[] args) {
    ConnectDialog cd = new ConnectDialog(null);
    cd.setVisible(true);
    System.exit(0);
  }
}
