package code.snippets.client;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.MutableAttributeSet;

import layout.TableLayout;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import code.snippets.models.CodeSnippet;
import code.snippets.models.SnippetCategory;
import code.snippets.models.SnippetType;
import code.snippets.server.ProtocolException;
import code.snippets.server.Response;
import code.snippets.server.SnippetsServer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.HashMap;

// TODO: Need to implement more syntax highlighters :)

public class SnippetsClient extends JFrame {
  private static final long serialVersionUID = -2963954144465147519L;
  public static final double VERSION = 1.1;
  public static final String NAME = "Snippets";
  public static final Font COURIER_NEW = new Font("Courier New", Font.PLAIN, 12);
  
  private static final String SYNTAX_HIGHLIGHTERS = "/data/syntax.xml";
  protected static final int MAX_FONT_SIZE = 72;
  protected static final int MIN_FONT_SIZE = 10;
  
  private SnippetsServer localServer = null;
  private Thread serverThread = null;
  private boolean launched = false;
  
  private Socket serverConnection;
  private RequestInterface reqInt;
  private String authToken = null;
  
  // -- dynamically loaded data --
  private HashMap<String, SyntaxHighlighter> highlighters = new HashMap<>();
  private IconLoader iconLoader = new IconLoader();

  // -- application state --
  private CodeSnippet selected = null;
  private Thread fetchThread = null;
  
  // --- UI components --
  FilterList snippets;
  private JTextPane snippetView;
  private JScrollPane snippetViewScroll;
  
  private JSpinner fontSize;
  
  private FilterPanel filters;
  
  JMenuBar menuBar;
  JMenu fileMenu;
  JMenuItem exitItem;
  
  JMenu snippetsMenu;
  JMenuItem createItem;
  JMenuItem deleteItem;
  
  JMenu filterCascade;
  JMenuItem createFilterCategory;
  
  // toolbar buttons
  JButton copy;
  
  public SnippetsClient() {
    super(String.format("%s [v%.1f]", SnippetsClient.NAME, SnippetsClient.VERSION));
    
    // configure GUI
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    uiconfig();
    
    // configure events
    WindowAdapter wa = new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent evt) {
        dispose();
        System.exit(0);
      }
    };
    addWindowListener(wa);
  }
  
  private void dispatchPutTask(JSONObject payload) {
    (new Thread() {
      @Override
      public void run() {
        JSONParser parser = new JSONParser();
        
        Response res;
        try {
          res = reqInt.issueCommand("put", payload, authToken);
        } catch (IOException e) {
          error("Failed to complete the creation request.", "I/O Error");
          return;
        } catch (ProtocolException e) {
          error("The server returned an invalid response.", "Protocol Error");
          return;
        }
        
        if( res.getCode() == Response.ERROR ) {
          error(res.getData(), "Error");
          return;
        } else {
          try {
            JSONObject obj = (JSONObject) parser.parse(res.getData());
            CodeSnippet cs = CodeSnippet.fromJSONObject(obj);
            snippets.addItem(cs);
          } catch(ParseException | ClassCastException e) {
            error("The server returned an invalid response.", "Error");
          }
        }
      }
    }).start();
  }
  
  private void dispatchCreateFilterTask(JSONObject payload) {
    (new Thread() {
      @Override
      public void run() {
        JSONParser parser = new JSONParser();
        
        Response res;
        try {
          res = reqInt.issueCommand("create_filter", payload, authToken);
        } catch (IOException e) {
          error("Failed to complete the creation request.", "I/O Error");
          return;
        } catch (ProtocolException e) {
          error("The server returned an invalid response.", "Protocol Error");
          return;
        }
        
        if( res.getCode() == Response.ERROR ) {
          error(res.getData(), "Error");
          return;
        } else {
          try {
            JSONObject obj = (JSONObject) parser.parse(res.getData());
            SnippetCategory sc = SnippetCategory.fromJSONObject(obj);
            
            iconLoader.loadIcon(sc.icon);
            
            filters.addCategory(sc);
          } catch(ParseException | ClassCastException e) {
            error("The server returned an invalid response.", "Error");
          }
        }
      }
    }).start();
  }
  
  private ImageIcon getImageIcon(String name) {
    URL url = getClass().getResource("/data/icons/" + name + ".png");
    ImageIcon ico = new ImageIcon(url);
    return ico;
  }
  
  private void uiconfig() {
    setMinimumSize(new Dimension(700, 600));
    setSize(700, 600);
    
    // configure menu bar
    menuBar = new JMenuBar();
    fileMenu = new JMenu("File");
    exitItem = new JMenuItem("Exit");
    
    exitItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int confirm = JOptionPane.showConfirmDialog(SnippetsClient.this, "Are you sure you want to exit?", "Quit",
            JOptionPane.YES_NO_OPTION);
        if( confirm == JOptionPane.OK_OPTION ) {
          System.exit(1);
        }
      }
    });
    
    fileMenu.add(exitItem);
    
    snippetsMenu = new JMenu("Snippets");
    
    createItem = new JMenuItem("Create");
    deleteItem = new JMenuItem("Delete");
    
    createItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        SnippetCreatorDialog scd = new SnippetCreatorDialog(SnippetsClient.this);
        scd.setVisible(true);
        
        if( !scd.isCancelled() ) {
          try {
            dispatchPutTask(scd.getPutPayload());
          } catch(IOException ioe) {
            error("Could not read content file.", "Error");
          }
        }
      }
    });
    
    deleteItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        int confirm = JOptionPane.showConfirmDialog(SnippetsClient.this, "Are you sure you want to delete this snippet?",
            "Delete Snippet", JOptionPane.YES_NO_OPTION);
        if( confirm == JOptionPane.OK_OPTION ) {
          deleteSnippet(selected.id);
        }
      }
    });
    
    snippetsMenu.add(createItem);
    snippetsMenu.add(deleteItem);
    
    filterCascade = new JMenu("Filters");
    
    createFilterCategory = new JMenuItem("Create Filter Category");
    filterCascade.add(createFilterCategory);
    
    createFilterCategory.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent evt) {
        CreateCategoryDialog ccd = new CreateCategoryDialog(SnippetsClient.this);
        ccd.setVisible(true);
        if( !ccd.isCancelled() ) {
          JSONObject payload = ccd.getPayload();
          dispatchCreateFilterTask(payload);
        }
      }
    });
    
    snippetsMenu.addSeparator();
    snippetsMenu.add(filterCascade);
    
    menuBar.add(fileMenu);
    menuBar.add(snippetsMenu);
    
    setJMenuBar(menuBar);
    
    // configure left pane
    JPanel leftPane = new JPanel();
    snippets = new FilterList();
    snippets.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    snippets.setCellRenderer(new CodeSnippetRenderer());
    
    filters = new FilterPanel(iconLoader);
    filters.onChange(new Runnable() {
      @Override
      public void run() {
        snippets.setFilter(filters.getCategory());
      }
    });
    
    snippets.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        CodeSnippet cs = snippets.getSelectedValue();
        if( cs != null ) {
          select(cs.id);
        } else {
          deselect();
        }
      }
    });
    
    leftPane.setLayout(new BorderLayout());
    leftPane.add(new JScrollPane(snippets), BorderLayout.CENTER);
    leftPane.add(filters, BorderLayout.NORTH);
    
    // configure right pane
    JPanel rightPane = new JPanel();
    snippetView = new JTextPane();
    snippetView.setEditable(false);
    snippetView.setFont(SnippetsClient.COURIER_NEW);
    
    JPanel noWrapPanel = new JPanel();
    noWrapPanel.setLayout(new BorderLayout());
    noWrapPanel.add(snippetView, BorderLayout.CENTER);
    
    snippetViewScroll = new JScrollPane(noWrapPanel);
    snippetViewScroll.getHorizontalScrollBar().setUnitIncrement(7);
    snippetViewScroll.getVerticalScrollBar().setUnitIncrement(15);
    
    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    
    copy = new JButton(getImageIcon("copy"));
    copy.setToolTipText("Copy Snippet");
    copy.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent evt) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(snippetView.getText()), null);
      }
    });
    toolbar.add(copy);
    toolbar.addSeparator();
    toolbar.add(Box.createRigidArea(new Dimension(10, 0)));
    
    SpinnerNumberModel fontModel = new SpinnerNumberModel(12, 10, 72, 2);
    fontSize = new JSpinner(fontModel);
    fontSize.setMaximumSize(new Dimension(40, 25));
    
    // adjust font size on scroll if control is down :)
    fontSize.addMouseWheelListener(new SpinnerScroller(fontSize, new Callback<Integer>() {
      @Override
      public void invoke(Integer ret) {
        snippetView.setFont(COURIER_NEW.deriveFont((float)ret));
      }

      @Override
      public void onFail(Exception e) {}
    }));
    
    toolbar.add(new JLabel("Font Size:"));
    toolbar.add(Box.createRigidArea(new Dimension(10, 0)));
    toolbar.add(fontSize);
    
    rightPane.setLayout(new BorderLayout());
    rightPane.add(toolbar, BorderLayout.NORTH);
    rightPane.add(snippetViewScroll, BorderLayout.CENTER);
    
    // configure master layout
    double size[][] = {
        {300, TableLayout.FILL},
        {TableLayout.FILL}
    };
    
    setLayout(new TableLayout(size));
    add(leftPane, "0, 0");
    add(rightPane, "1, 0");
    
    deselect();
  }
  
  private void setSelected(boolean b) {
    if(!b) {
      snippetView.setText("");
    }
    
    fontSize.setEnabled(b);
    deleteItem.setEnabled(b);
    copy.setEnabled(b);
  }
  
  private void deselect() {
    setSelected(false);
    fontSize.setEnabled(false);
  }
  
  private void error(String message, String title) {
    JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
  }
  
  private void success(String message, String title) {
    JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
  }
  
  private boolean select(int id) {
    setSelected(true);
    
    ListModel<CodeSnippet> m = snippets.getModel();
    for( int i = 0; i < m.getSize(); i++ ) {
      CodeSnippet cs = m.getElementAt(i);
      if( cs.id == id ) {
        // select this snippet
        selected = cs;
        onSelect();
        return true;
      }
    }
    return false;
  }
  
  private void onSelect() {
    // need to fetch
    if( fetchThread != null && fetchThread.isAlive() ) {
      fetchThread.interrupt();
    }
    fetchThread = new Thread() {
      @SuppressWarnings("unchecked")
      @Override
      public void run() {
        JSONObject params = new JSONObject();
        params.put("id", selected.id);
        Response res;
        try {
          res = reqInt.issueCommand("fetch", params, authToken);
        } catch (IOException e) {
          error("Failed to fetch code snippet source.", "I/O Error");
          return;
        } catch (ProtocolException e) {
          error("The server returned an invalid response.", "Protocol Error");
          return;
        }

        // TODO - dynamic tab->space settings
        String content = res.getData().replaceAll("\t", "  ");
        
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            // set text
            snippetView.setText("");
            MutableAttributeSet mas = snippetView.getInputAttributes();
            mas.removeAttributes(mas);
            snippetView.setText(content);
            
            // switch syntax highlighting
            updateSyntaxHighlighting();
          }
        });
      }
    };
    fetchThread.start();
  }
  
  private void updateSyntaxHighlighting() {
    // get syntax highlighter if applicable
    SyntaxHighlighter h = highlighters.get(selected.language);
    if( h == null ) return;
    
    // apply syntax highlighter to text
    h.apply(snippetView);
    
    snippetViewScroll.getVerticalScrollBar().setValue(0);
  }
  
  private void loadSyntaxHighlighters() {
    InputStream is = this.getClass().getResourceAsStream(SnippetsClient.SYNTAX_HIGHLIGHTERS);
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      error("Could not load syntax highlighters.", "Error");
      return;
    }
    
    Document doc;
    try {
      doc = builder.parse(is);
    } catch (SAXException e) {
      error("Could not load syntax highlighters - the configuration file is invalid.", "Error");
      return;
    } catch (IOException e) {
      error("Could not load syntax highlighters.", "Error");
      return;
    }
    
    NodeList languages = doc.getElementsByTagName("language");
    for( int i = 0; i < languages.getLength(); i++ ) {
      Node n = languages.item(i);
      if(n.getNodeType() == Node.ELEMENT_NODE) {
        Element e = (Element) n;
        String name = e.getAttribute("name");
        if( !name.isEmpty() ) {
          
          SyntaxHighlighter highlighter = new SyntaxHighlighter();
          
          int anonRule = 0;
          
          // load all rules
          NodeList rules = e.getElementsByTagName("rule");
          for( int j = 0; j < rules.getLength(); j++ ) {
            Node _rule = rules.item(j);
            if( _rule.getNodeType() == Node.ELEMENT_NODE ) {
              Element rule = (Element) _rule;
              String ruleName = rule.getAttribute("name");
              String color = rule.getAttribute("color");
              if( ruleName.isEmpty() ) ruleName = "anonRule" + anonRule++;
              if( color.isEmpty() || !color.matches("^#[A-Fa-f0-9]{6}$") ) color = "#000000";
              
              boolean bold = false;
              boolean italic = false;
              
              String[] style = rule.getAttribute("style").split(",");
              for( String st : style ) {
                st = st.strip();
                if( st.equals("bold") ) {
                  bold = true;
                } else if( st.equals("italic") ) {
                  italic = true;
                }
              }
              
              HighlightRule hr = new HighlightRule(ruleName, Color.decode(color), bold, italic);
              highlighter.addRule(hr);
              
              // rules for words
              NodeList words = rule.getElementsByTagName("word");
              for( int k = 0; k < words.getLength(); k++ ) {
                Node _word = words.item(k);
                if( _word.getNodeType() == Element.ELEMENT_NODE ) {
                  Element word = (Element) _word;
                  String wordText = word.getTextContent();
                  if( wordText.matches("^[A-Za-z_]+$") ) {
                    highlighter.addWord(ruleName, wordText);
                  }
                }
              }
              
              // TODO - error on duplicate comment types/string types?
              
              // rules for comments
              NodeList commentTypes = rule.getElementsByTagName("comment");
              for( int k = 0; k < commentTypes.getLength(); k++ ) {
                Node _comment = commentTypes.item(k);
                if( _comment.getNodeType() == Element.ELEMENT_NODE ) {
                  Element comment = (Element) _comment;
                  String type = comment.getAttribute("type");
                  if( type.isEmpty() ) type = SyntaxHighlighter.LINE_COMMENT;
                  String start = comment.getAttribute("start");
                  if( start.isEmpty() ) {
                    // TODO - issue warning?
                    continue;
                  }
                  String end = comment.getAttribute("end");
                  if( end.isEmpty() ) {
                    if( type.equals(SyntaxHighlighter.LINE_COMMENT) ) {
                      end = "\n";
                    } else {
                      // TODO - issue warning?
                      continue;
                    }
                  }
                  highlighter.addCommentType(ruleName, type, start, end);
                }
              }
              
              // rules for strings
              NodeList stringTypes = rule.getElementsByTagName("string");
              for( int k = 0; k < stringTypes.getLength(); k++ ) {
                Node _string = stringTypes.item(k);
                if( _string.getNodeType() == Element.ELEMENT_NODE ) {
                  Element string = (Element) _string;
                  String type = string.getAttribute("type");
                  if( type.isEmpty() ) type = SyntaxHighlighter.STRING_INLINE;
                  String marker = string.getAttribute("marker");
                  String start;
                  String end;
                  if( marker.isEmpty() ) {
                    start = string.getAttribute("start");
                    end = string.getAttribute("end");
                  } else {
                    start = marker;
                    end = marker;
                  }
                  highlighter.addStringType(ruleName, type, start, end);
                }
              }
            }
          }
          
          highlighters.put(name, highlighter);
        }
      }
    }
  }
  
  private void deleteSnippet(int id) {
    (new Thread() {
      @SuppressWarnings("unchecked")
      @Override
      public void run() {
        try {
          if( serverConnection != null && selected != null ) {
            JSONObject params = new JSONObject();
            params.put("id", id);
            Response res = reqInt.issueCommand("delete", params, authToken);
            
            if( res.getCode() == Response.ERROR ) {
              error(res.getData(), "Error");
              return;
            }
            
            // remove snippet from list model
            snippets.removeItemById(id);
            
            success(res.getData(), "Success");
          }
        } catch(IOException ioe) {
          error("Failed to delete snippet.", "I/O Error");
        } catch(ProtocolException pe) {
          error("The server returned an invalid response: " + pe.getMessage(), "Protocol Error");
        }
      }
    }).start();
  }
  
  // loads snippets AND snippet categories
  private void startSnippetLoader() {
    (new Thread() {
      @Override
      public void run() {
        JSONParser parser = new JSONParser();
        
        try {
          if( serverConnection != null ) {
            Response res = reqInt.issueCommand("get", null, authToken);
            
            String data = res.getData();
            JSONArray arr;
            try {
              arr = (JSONArray) parser.parse(data);
            } catch(ParseException | ClassCastException e) {
              throw new ProtocolException("Server returned invalid response data.");
            }
            
            for( Object _obj : arr ) {
              JSONObject obj;
              try {
                obj = (JSONObject) _obj;
              } catch (ClassCastException e) {
                throw new ProtocolException("Server returned invalid response data.");
              }
              Object lang_o  = obj.get("language");
              Object tech_o  = obj.get("technology");
              Object title_o = obj.get("title");
              Object desc_o  = obj.get("description");
              Object type_o  = obj.get("type");
              Object src_o   = obj.get("source_file");
              Object id_o    = obj.get("id");
              
              if( lang_o instanceof String && (tech_o instanceof String || tech_o == null) && title_o instanceof String &&
                  (desc_o instanceof String || desc_o == null) &&
                  (type_o instanceof String && SnippetType.valueOfSafe((String)type_o) != null) && src_o instanceof String &&
                  id_o instanceof Long) {
                snippets.addItem(CodeSnippet.fromJSONObject(obj));
              } else {
                // TODO - ignore throw exception??
              }
            }
            
            // now fetch categories
            Response catres = reqInt.issueCommand("get_filters", new JSONObject(), authToken);
            String catdata = catres.getData();
            JSONArray catarr;
            try {
              catarr = (JSONArray) parser.parse(catdata);
            } catch(ParseException | ClassCastException e) {
              throw new ProtocolException("Server returned invalid category data: " + e.toString());
            }
            
            for( Object _obj : catarr ) {
              JSONObject obj;
              try {
                obj = (JSONObject) _obj;
              } catch (ClassCastException e) {
                throw new ProtocolException("Server returned invalid response data.");
              }
              Object name_o  = obj.get("name");
              Object icon_o  = obj.get("icon");
              Object lang_o  = obj.get("language");
              Object tech_o  = obj.get("technology");
              Object type_o  = obj.get("type");
              Object id_o    = obj.get("id");
              
              if( name_o instanceof String && (tech_o instanceof String || tech_o == null) && icon_o instanceof String &&
                  (lang_o instanceof String || lang_o == null) &&
                  (type_o instanceof String && SnippetType.valueOfSafe((String)type_o) != null) &&
                  id_o instanceof Long) {
                SnippetCategory cat = SnippetCategory.fromJSONObject(obj);
                filters.addCategory(cat);
                
                // asynchronously load icon for this category
                if( cat.icon != null ) {
                  iconLoader.loadIcon(cat.icon);
                }
              } else {
                // TODO - ignore throw exception??
              }
            }
          }
        } catch(IOException ioe) {
          error("Could not fetch data from server: " + ioe.toString(), "I/O Error");
        } catch(ProtocolException pe) {
          error("Protocol Error: " + pe.getMessage(), "Protocol Error");
        }
      }
    }).start();
  }
  
  public boolean launch(boolean serveLocally) {
    if( launched ) {
      throw new RuntimeException("Already launched the application!");
    }
    
    int progress = 0;
    
    int MAX_PROGRESS = 3;
    
    ProgressDialog pd = new ProgressDialog(null, serveLocally ? MAX_PROGRESS : MAX_PROGRESS-1);
    pd.setLocationRelativeTo(null);
    pd.setVisible(true);
    pd.setProgress(progress++, serveLocally ? "Initializing local server..." : "Loading syntax highlighters...");
    
    // create local server if applicable
    if( serveLocally ) {
      try {
        localServer = new SnippetsServer();
        authToken = localServer.createAdminSession();
      } catch(IOException ioe) {
        pd.dispose();
        error("Could not instantiate storage for the Snippets database.", "Fatal Error");
        return false;
      } catch(SQLException sqle) {
        pd.dispose();
        error("Could not connect to Snippets database.", "Fatal Error");
        return false;
      }
      
      serverThread = new Thread() {
        @Override
        public void run() {
          try {
            localServer.initialize();
            synchronized(SnippetsClient.this) {
              SnippetsClient.this.notify();
            }
            localServer.run();
          } catch (IOException e) {
            synchronized(SnippetsClient.this) {
              SnippetsClient.this.notify();
            }
          }
        }
      };
      serverThread.start();

      // wait for server to start...
      synchronized(this) {
        try {
          this.wait();
        } catch (InterruptedException e) {}
      }
      
      if( !localServer.isInitialized() ) {
        return false;
      }
      
      // now connect to local server
      try {
        serverConnection = new Socket("127.0.0.1", SnippetsServer.SNIPPETS_PORT);
        reqInt = new RequestInterface(serverConnection);
      } catch (IOException e) {
        // this should not happen
        error("Could not connect to local server.", "Fatal Error");
        return false;
      }
      
      pd.setProgress(progress++, "Loading syntax highlighters...");
    } else {
      pd.setVisible(false);
      ConnectDialog cd = new ConnectDialog(this);
      
      // attempt to establish connection
      do {
        cd.setVisible(true);
        if( cd.isCancelled() ) {
          return false;
        }
        try {
          serverConnection = new Socket(cd.getServerAddress(), cd.getPort());
          reqInt = new RequestInterface(serverConnection);
          // TODO: more advanced authentication options?
          Response res = reqInt.issueCommand("authenticate", new JSONObject(), null);
          if( res.getCode() == Response.ERROR ) {
            error("Could not authenticate.", "Error");
            serverConnection = null;
            reqInt = null;
          } else {
            authToken = res.getData();
          }
        } catch (UnknownHostException e) {
          error("Unknown host.", "Error");
          serverConnection = null;
        } catch (IOException e) {
          error("Could not connect to host.", "Error");
          serverConnection = null;
        } catch (ProtocolException e) {
          error("Specified server is not a Snippets server.", "Error");
          reqInt = null;
          serverConnection = null;
        }
      } while( serverConnection == null );
      pd.setVisible(true);
      pd.setProgress(progress++, "Loading syntax highlighters...");
    }
    
    iconLoader.initialize(reqInt, authToken);
    
    // load syntax highlighters
    loadSyntaxHighlighters();
    
    pd.setProgress(progress++, "Starting asynchronous snippet loader...");
    
    startSnippetLoader();
    
    pd.dispose();
    
    setVisible(true);
    launched = true;
    return true;
  }
  
  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
        | UnsupportedLookAndFeelException e) {
      // ignore exception, just use default look n feel
      System.out.println("Info: Could not change the look and feel.");
    }
    
    boolean includeServer = JOptionPane.showConfirmDialog(null, "Do you want to run the local Snippets server?", "Initialization",
        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    
    SnippetsClient cli = new SnippetsClient();
    boolean success = cli.launch(includeServer);
    if( !success ) System.exit(1);
  }
}
