package code.snippets.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.json.simple.JSONObject;
public abstract class Server {
  
  public static final String END = "-";
  public static final int DEFAULT_MAX_CONNECTIONS = 5;
  
  protected int port;
  protected int maxConnections;
  protected volatile boolean running = true;
  
  private ServerSocket ss = null;
  private boolean initialized = false;
  
  public Server(int port, int maxConnections) {
    this.port = port;
    this.maxConnections = maxConnections;
  }
  
  public Server(int port) {
    this(port, Server.DEFAULT_MAX_CONNECTIONS);
  }
  
  public abstract Response handleRequest(String command, JSONObject params, String authToken);
  
  public void initialize() throws IOException {
    if( ss == null ) {
      ss = new ServerSocket(port);
      initialized = true;
    }
  }
  
  public boolean isInitialized() {
    return initialized;
  }
  
  public void run() throws IOException {
    if( ss == null ) {
      throw new IOException("Must initialize server first!"); // TODO - custom exception for this?
    }
    
    while(running) {
      // TODO - put a limit on connections!!
      
      Socket s = ss.accept(); 
      System.out.println("ACCEPTED: " + s.getInetAddress().toString());
      ClientThread ct = new ClientThread(this, s);
      ct.start();
    }
    ss.close();
  }
}
