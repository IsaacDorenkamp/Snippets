package code.snippets.server;

import org.json.simple.JSONObject;

public class Command {
  private String name;
  private JSONObject params;
  private String auth;
  
  public Command(String cmd, JSONObject params, String auth) {
    this.name = cmd;
    this.params = params;
    this.auth = auth;
  }
  
  public String getName() {
    return name;
  }
  
  public JSONObject getParameters() {
    return (JSONObject) params.clone();
  }
  
  public String getAuth() {
    return auth;
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public String toString() {
    JSONObject obj = new JSONObject();
    obj.put("command", name);
    obj.put("parameters", params);
    obj.put("auth", auth);
    return obj.toJSONString();
  }
}
