package code.snippets.client;

public class RegionType {
  private String start;
  private String end;
  public RegionType(String start, String end) {
    this.start = start;
    this.end = end;
  }
  
  public String getStart() {
    return start;
  }
  
  public String getEnd() {
    return end;
  }
  
  @Override
  public boolean equals(Object o) {
    if( o instanceof RegionType ) {
      RegionType c = (RegionType) o;
      return c.getStart().equals(start) && c.getEnd().equals(end);
    } else return false;
  }
}
