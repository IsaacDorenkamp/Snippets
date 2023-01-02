package code.snippets.dbinterface;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

class Pair<K, V> {
  private K key;
  private V value;
  
  public Pair(K key, V value) {
    this.key = key;
    this.value = value;
  }
  
  public K getKey() {
    return key;
  }
  
  public V getValue() {
    return value;
  }
}

public class WhereClause {
  public static enum WHERE_TYPE {
    EQUAL, IN
  }
  
  private HashMap<String, Pair<Object, WHERE_TYPE>> constraints;
  public WhereClause() {
    constraints = new HashMap<String, Pair<Object, WHERE_TYPE>>();
  }
  
  public void addConstraint(String columnName, Object columnValue, WHERE_TYPE type) {
    if( type.equals(WHERE_TYPE.IN) ) {
      Object[] objList;
      if( !(columnValue instanceof Object[] || columnValue instanceof Iterable<?>) ) {
        objList = new Object[] { columnValue };
      } else {
        if( columnValue instanceof Object[] ) {
          objList = (Object[]) columnValue;
        } else {
          Iterable<?> i = (Iterable<?>) columnValue;
          Iterator<?> it = i.iterator();
          ArrayList<Object> objArrayList = new ArrayList<Object>();
          while( it.hasNext() ) objArrayList.add(it.next());
          objList = objArrayList.toArray();
        }
      }
      constraints.put(columnName, new Pair<Object, WHERE_TYPE>(objList, type));
    } else {
      constraints.put(columnName, new Pair<Object, WHERE_TYPE>(columnValue, type));
    }
  }
  
  public PreparedStatement getPreparedStatement(Connection conn, String queryBase) throws SQLException {
    if( constraints.size() == 0 ) {
      return conn.prepareStatement(queryBase);
    } else {
      String[] pieces = new String[constraints.size()];
      HashMap<String, Integer> colToIdx = new HashMap<String, Integer>();
      int idx = 0;
      for( String constraintKey : constraints.keySet() ) {
        Pair<Object, WHERE_TYPE> constraint = constraints.get(constraintKey);
        colToIdx.put(constraintKey, idx + 1);
        pieces[idx++] = constraintKey + " " + (constraint.getValue() == WHERE_TYPE.EQUAL ? "=" : "IN") + " ?";
      }
      
      PreparedStatement ps = conn.prepareStatement(queryBase + " WHERE " + String.join(" AND ", pieces));
      for( String col : colToIdx.keySet() ) {
        ps.setObject(colToIdx.get(col), constraints.get(col).getKey());
      }
      return ps;
    }
  }
  
  private static String asString(Object obj) {
    if( obj.getClass().isArray() ) {
      Object[] objs = (Object[]) obj;
      String[] strs = new String[objs.length];
      for( int i = 0; i < objs.length; i++ ) {
        strs[i] = "\"" + objs[i].toString() + "\"";
      }
      return "(" + String.join(", ", strs) + ")";
    } else {
      return "\"" + obj.toString() + "\"";
    }
  }
  
  @Override
  public String toString() {
    String[] pieces = new String[constraints.size()];
    int idx = 0;
    for( String constraintKey : constraints.keySet() ) {
      Pair<Object, WHERE_TYPE> constraint = constraints.get(constraintKey);
      pieces[idx++] = constraintKey + " " + (constraint.getValue() == WHERE_TYPE.EQUAL ? "=" : "IN") + " " + WhereClause.asString(constraint.getKey());
    }
    
    return "WHERE " + String.join(" AND ", pieces);
  }
}
