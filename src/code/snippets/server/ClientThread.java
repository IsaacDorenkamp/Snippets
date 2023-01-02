package code.snippets.server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ClientThread extends Thread {
  
  private JSONParser parser = new JSONParser();
  private Server server;
  private Socket s;
  
  public ClientThread(Server server, Socket s) {
    this.server = server;
    this.s = s;
  }
  
  private Response handleSingleRequest(String requestData) {
    Response res = new Response();
    try {
      Object o = parser.parse(requestData);
      if( !(o instanceof JSONObject) ) {
        res.setCode(Response.ERROR);
        res.setData("Request must be a JSON object with keys 'command' and 'parameters', not the given:\n" + requestData);
      } else {
        JSONObject jobj = (JSONObject) o;
        if( !(jobj.containsKey("command") && jobj.containsKey("parameters")) ) {
          res.setCode(Response.ERROR);
          res.setData("Request must be a JSON object with keys 'command' and 'parameters'");
        } else {
          Object cmdObj = jobj.get("command");
          Object parametersObj = jobj.get("parameters");
          if( !(cmdObj instanceof String) ) {
            res.setCode(Response.ERROR);
            res.setData("Request key 'command' must be a string.");
          } else if( !(parametersObj instanceof JSONObject) ) {
            res.setCode(Response.ERROR);
            res.setData("Request key 'parameters' must be an object/map.");
          } else {
            String cmd = (String) cmdObj;
            JSONObject parameters = (JSONObject) parametersObj;
            String auth = null;
            
            // TODO - Handle authentication outside of handleRequest??
            
            // check for auth
            Object authObj = jobj.get("auth");
            if( authObj != null ) {
              if( authObj instanceof String ) {
                auth = (String) authObj;
              } else {
                res.setCode(Response.ERROR);
                res.setData("Request key 'auth' must be a String containing an authentication token");
              }
            }
            
            if( res.getCode() != Response.ERROR ) {
              res = server.handleRequest(cmd, parameters, auth);
            }
          }
        }
      }
    } catch(ParseException pe) {
      res.setCode(Response.ERROR);
      res.setData("Request must be a valid JSON object with keys 'command' and 'parameters'");
    }
    return res;
  }
  
  @Override
  public void run() {
    try {
      InputStreamReader isr = new InputStreamReader(s.getInputStream());
      PrintWriter writer = new PrintWriter(s.getOutputStream());
      
      while(true) {
        
        StringBuilder requestDataBuffer = new StringBuilder();
        while( !isr.ready() ) {
          try {
            Thread.sleep(25);
          } catch(InterruptedException ie) {}
        }
        while( isr.ready() ) {
          char c = (char)isr.read();
          requestDataBuffer.append(c);
        }
        
        String requestData = requestDataBuffer.toString();
        
        if( requestData.equals(Server.END) ) {
          writer.write("-");
          writer.flush();
          s.close();
          break;
        }
        
        Response r = handleSingleRequest(requestData);
        
        writer.write(r.toString());
        writer.flush();
      }
    } catch(IOException ioe) {
      // Panic Time B)
      System.out.println("ERROR: I/O broken for socket to client " + s.getInetAddress().getHostName());
    }
  }
}
