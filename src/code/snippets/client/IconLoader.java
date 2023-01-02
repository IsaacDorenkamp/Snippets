package code.snippets.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.json.simple.JSONObject;

import code.snippets.server.ProtocolException;
import code.snippets.server.Response;

import java.util.Base64;
import java.util.HashMap;

public class IconLoader {
  
  private RequestInterface reqInt;
  private String authToken;
  private HashMap<String, BufferedImage> icons;
  
  public IconLoader(RequestInterface reqInt, String authToken) {
    this.reqInt = reqInt;
    this.authToken = authToken;
    this.icons = new HashMap<String, BufferedImage>();
  }
  
  public IconLoader() {
    this(null, null);
  }
  
  public void initialize(RequestInterface reqInt, String authToken) {
    this.reqInt = reqInt;
    this.authToken = authToken;
  }
  
  public BufferedImage getIcon(String icon) {
    return icons.get(icon);
  }
  
  public void loadIcon(String icon) {
    loadIcon(icon, new Callback<BufferedImage>() {
      public void invoke(BufferedImage img) {}
      public void onFail(Exception e) {
        System.out.printf("WARNING: Failed to load icon '%s'.", icon);
      }
    });
  }
  
  public void loadIcon(String icon, Callback<BufferedImage> cbk) {
    (new Thread() {
      @SuppressWarnings("unchecked")
      @Override
      public void run() {
        if( icons.containsKey(icon) ) {
          BufferedImage img = icons.get(icon);
          cbk.invoke(img);
          return;
        }
        
        JSONObject params = new JSONObject();
        params.put("icon", icon);
        try {
          Response res = reqInt.issueCommand("get_icon", params, authToken);
          if( res.getCode() == Response.ERROR ) {
            throw new ProtocolException(res.getData()); // is this really good form? eh.
          } else {
            byte[] imgdata;
            try {
              imgdata = Base64.getDecoder().decode(res.getData());
            } catch(IllegalArgumentException iae) {
              throw new ProtocolException("Server did not return valid Base64 image data.");
            }
            
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgdata));
            icons.put(icon, img);
            cbk.invoke(img);
          }
        } catch (IOException e) {
          cbk.onFail(e);
        } catch (ProtocolException e) {
          cbk.onFail(e);
        }
      }
    }).start();
  }
}
