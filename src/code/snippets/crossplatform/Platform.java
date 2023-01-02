package code.snippets.crossplatform;

public enum Platform {
  WINDOWS, MACINTOSH, UNIX;
  
  public static final Platform ACTIVE = Platform.getPlatformForName(System.getProperty("os.name"));
  
  public static Platform getPlatformForName(String name) {
    if( name.indexOf("win") >= 0 ) return WINDOWS;
    else if( name.indexOf("mac") >= 0 ) return MACINTOSH;
    else if( name.indexOf("nix") >= 0 || name.indexOf("nux") >= 0 || name.indexOf("aix") > 0 ) return UNIX;
    else return null;
  }
}
