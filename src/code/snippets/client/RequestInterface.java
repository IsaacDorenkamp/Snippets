package code.snippets.client;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.net.Socket;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import code.snippets.server.Command;
import code.snippets.server.ProtocolException;
import code.snippets.server.Response;

// TODO - pass auth token into constructor
// so that the interface can be used without
// needing an auth token passed in *every*
// damned time.
public class RequestInterface {
  
  private static final Object LOCK = new Object();
  
  private static final JSONParser PARSER = new JSONParser();
  
  private Socket s;
  public RequestInterface(Socket s) {
    this.s = s;
  }
  
  public Response issueCommand(String cmd, JSONObject params, String auth) throws IOException, ProtocolException {
    // this requires requests to be fully completed before subsequent requests may be initiated.
    // using a static LOCK will prevent separate RequestInterface instances from sending overlapping
    // requests.
    synchronized(RequestInterface.LOCK) {
      Response r = new Response();
      
      if( params == null ) params = new JSONObject();
      
      Command com = new Command(cmd, params, auth);
      
      // send command to server
      OutputStreamWriter osw = new OutputStreamWriter(s.getOutputStream());
      osw.write(com.toString());
      osw.flush();
      
      // wait for response to be ready
      InputStreamReader isr = new InputStreamReader(s.getInputStream());
      while( !isr.ready() ) {
        try {
          Thread.sleep(25);
        } catch(InterruptedException ie) {}
      }
      
      // read response
      StringBuilder responseBuilder = new StringBuilder();
      while( isr.ready() ) {
        responseBuilder.append((char)isr.read());
      }
      String response = responseBuilder.toString();
      
      Object resObj;
      try {
        resObj = RequestInterface.PARSER.parse(response);
      } catch(ParseException pe) {
        System.out.println(response);
        throw new ProtocolException("Server did not return a valid JSON response.");
      }
      
      JSONObject res;
      if( resObj instanceof JSONObject ) {
        res = (JSONObject) resObj;
      } else {
        throw new ProtocolException("Server did not return a valid JSON response.");
      }
      
      Object codeObj = res.get("code");
      Object dataObj = res.get("data");
      
      if( codeObj instanceof Long ) {
        r.setCode(((Long)codeObj).intValue());
      } else {
        throw new ProtocolException("Server did not return a valid JSON response.");
      }
      
      if( dataObj instanceof String || dataObj == null ) {
        r.setData((String)dataObj);
      } else if( dataObj instanceof JSONObject ) {
        r.setData(((JSONObject)dataObj).toJSONString());
      } else {
        throw new ProtocolException("Server did not return a valid JSON response.");
      }
      
      return r;
    }
  }
}
