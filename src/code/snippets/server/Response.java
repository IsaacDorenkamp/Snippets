package code.snippets.server;

import org.json.simple.JSONObject;

public class Response {
  
  public static final int SUCCESS = 0;
  public static final int ERROR = 1;
  
  private String responseData;
  private int responseCode;
  
  public Response(String data, int code) {
    this.responseData = data;
    this.responseCode = code;
  }
  
  public Response(String data) {
    this(data, Response.SUCCESS);
  }
  
  public Response() {
    this(null);
  }
  
  public void setData(String data) {
    responseData = data;
  }
  
  public void setCode(int code) {
    responseCode = code;
  }
  
  public String getData() {
    return responseData;
  }
  
  public int getCode() {
    return responseCode;
  }
  
  @SuppressWarnings("unchecked")
  public JSONObject getJSONObject() {
    JSONObject obj = new JSONObject();
    obj.put("code", responseCode);
    obj.put("data", responseData);
    return obj;
  }
  
  @Override
  public String toString() {
    return getJSONObject().toJSONString();
  }
}
