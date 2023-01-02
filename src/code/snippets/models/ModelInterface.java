package code.snippets.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import code.snippets.dbinterface.Table;
import code.snippets.dbinterface.WhereClause;
import code.snippets.dbinterface.Column;
import code.snippets.dbinterface.PrimaryKey;

public class ModelInterface {
  
  private static final String TABLE_NAME_REGEX = "^[A-Za-z0-9_]+$";
  private static final String COLUMN_NAME_REGEX = "^[A-Za-z0-9_]+$";
  
  private static final HashMap<Class<?>, JDBCType> TYPE_MAP = new HashMap<Class<?>, JDBCType>();
  static {
    TYPE_MAP.put(int.class, JDBCType.INTEGER);
    TYPE_MAP.put(Integer.class, JDBCType.INTEGER);
    
    TYPE_MAP.put(long.class, JDBCType.BIGINT);
    TYPE_MAP.put(Long.class, JDBCType.BIGINT);
    
    TYPE_MAP.put(short.class, JDBCType.SMALLINT);
    TYPE_MAP.put(Short.class, JDBCType.SMALLINT);
    
    TYPE_MAP.put(boolean.class, JDBCType.BOOLEAN);
    TYPE_MAP.put(Boolean.class, JDBCType.BOOLEAN);
    
    TYPE_MAP.put(byte.class, JDBCType.TINYINT);
    TYPE_MAP.put(Byte.class, JDBCType.TINYINT);
    
    TYPE_MAP.put(char.class, JDBCType.CHAR);
    TYPE_MAP.put(Character.class, JDBCType.CHAR);
    
    TYPE_MAP.put(float.class, JDBCType.FLOAT);
    TYPE_MAP.put(Float.class, JDBCType.FLOAT);
    
    TYPE_MAP.put(double.class, JDBCType.DOUBLE);
    TYPE_MAP.put(Double.class, JDBCType.DOUBLE);
    
    TYPE_MAP.put(String.class, JDBCType.VARCHAR); // TODO - how to do text type??
  }
  
  private static class ModelMapping {
    public final String table;
    public final HashMap<String, String> columnToField;
    public final HashMap<String, Class<?>> fieldToType;
    public final ArrayList<String> generatedColumns;
    public String primaryKey;
    public Class<?> primaryKeyType;
    
    public ModelMapping(String table) {
      this.table = table;
      this.columnToField = new HashMap<String, String>();
      this.fieldToType = new HashMap<String, Class<?>>();
      this.generatedColumns = new ArrayList<String>();
      this.primaryKey = null;
      this.primaryKeyType = null;
    }
    
    public void mapColumn(String col, String field, Class<?> type, boolean isPrimaryKey, boolean isGenerated) {
      columnToField.put(col, field);
      fieldToType.put(field, type);
      if( isPrimaryKey ) {
        primaryKey = col;
        primaryKeyType = type;
      }
      if( isGenerated ) {
        this.generatedColumns.add(col);
      }
    }
  }
  
  private HashMap<Class<?>, ModelMapping> mappings = new HashMap<Class<?>, ModelMapping>();
  
  private Connection conn;
  public ModelInterface(Connection conn) {
    this.conn = conn;
  }
  
  private void initializeModelClass(Class<?> clazz) throws ModelInterfaceException {
    Table table = (Table) clazz.getAnnotation(Table.class);
    String tableName;
    String t = table != null ? table.name() : "";
    if( t.isEmpty() ) {
      tableName = clazz.getSimpleName();
    } else if ( !t.matches(ModelInterface.TABLE_NAME_REGEX) ) {
      throw new ModelInterfaceException(String.format("'%s' is not a valid table name. Table names must contain"
          + " only alphanumeric characters and underscores.", t));
    } else {
      tableName = t;
    }
    
    ModelMapping mapping = new ModelMapping(tableName);
    
    Field[] fields = clazz.getDeclaredFields();
    for( Field field : fields ) {
      Column column = (Column) field.getAnnotation(Column.class);
      if( column == null ) continue;
      
      PrimaryKey pk = (PrimaryKey) field.getAnnotation(PrimaryKey.class);
      if( pk != null && mapping.primaryKey != null ) {
        throw new ModelInterfaceException(String.format("Model class '%s' defines multiple primary keys, which is invalid.",
            clazz.getSimpleName()));
      }
      
      String colName = column.name();
      // check if name is valid
      if( colName.matches(ModelInterface.COLUMN_NAME_REGEX) ) {
        mapping.mapColumn(colName, field.getName(), field.getType(), pk != null, column.generated());
      } else {
        throw new ModelInterfaceException(String.format("'%s' is not a valid column name. Column names must contain"
            + " only alphanumeric characters and underscores.", colName));
      }
    }
    
    mappings.put(clazz, mapping);
  }
  
  private Object constructModelFromResultSet(Class<?> modelType, ResultSet rs) throws ModelInterfaceException, SQLException {
    @SuppressWarnings("rawtypes")
    Constructor builder;
    try {
      builder = modelType.getConstructor();
    } catch (NoSuchMethodException e) {
      throw new ModelInterfaceException("Model classes must have a default constructor.");
    } catch (SecurityException e) {
      throw new ModelInterfaceException("Getting the model class constructor is not allowed by the security manager.");
    }
    return constructModelFromResultSet(modelType, builder, rs);
  }
  
  private Object constructModelFromResultSet(Class<?> modelType, @SuppressWarnings("rawtypes") Constructor builder, ResultSet rs)
    throws ModelInterfaceException, SQLException {
    ModelMapping mapping = mappings.get(modelType);
    HashMap<String, Integer> columnsToIndex = new HashMap<String, Integer>();
    ResultSetMetaData rsmd = rs.getMetaData();
    for( int idx = 1; idx <= rsmd.getColumnCount(); idx++ ) {
      columnsToIndex.put(rsmd.getColumnName(idx), idx);
    }
    return constructModelFromResultSet(modelType, builder, rs, columnsToIndex, mapping);
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private Object constructModelFromResultSet(Class<?> modelType, Constructor builder, ResultSet rs,
      HashMap<String, Integer> columnsToIndex, ModelMapping mapping) throws ModelInterfaceException, SQLException {
    Object model = null;
    try {
      model = builder.newInstance();
    } catch (InstantiationException e) {
      throw new ModelInterfaceException("Unable to instantiate the model class: " + e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ModelInterfaceException("Default constructor for model class is not visible!");
    } catch (IllegalArgumentException e) { /* unreachable */ } catch (InvocationTargetException e) {
      throw new ModelInterfaceException("An error occurred instantiating the model class: " + e.getMessage());
    }
    
    for( String column : mapping.columnToField.keySet() ) {
      Integer colIdx = columnsToIndex.get(column);
      if( colIdx == null ) {
        throw new ModelInterfaceException(String.format("Model class specifies a column '%s' that does not"
            + " exist in the database table.", column));
      }
      
      String fieldName = mapping.columnToField.get(column);
      Class<?> dataType = mapping.fieldToType.get(fieldName);
      
      Class<?> fromDb = dataType;
      if( fromDb.isEnum() ) fromDb = String.class;
      
      JDBCType sqlType = ModelInterface.TYPE_MAP.get(fromDb);
      
      if( sqlType == null ) {
        throw new ModelInterfaceException(String.format("Model class specifies a column '%s' with an unsupported"
            + " type '%s.'", column, dataType.getName()));
      }
      
      Field f;
      try {
        f = modelType.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        /* this should never happen - the field name was directly fetched via reflection */
        throw new ModelInterfaceException("Fatal unexpected error: " + e.getMessage());
      } catch (SecurityException e) {
        /* this would already have occurred, no need to do anything (unreachable) */
        throw new ModelInterfaceException("Fatal unexpected error: " + e.getMessage());
      }
      try {
        Object o = rs.getObject(colIdx);
        if( dataType.isEnum() ) o = Enum.valueOf((Class<Enum>)dataType, o.toString());
        f.set(model, o);
      } catch (IllegalArgumentException e) {
        throw new ModelInterfaceException(String.format("Error putting data into model class: %s", e.getMessage()));
      } catch (IllegalAccessException e) {
        throw new ModelInterfaceException(String.format("Model class maps the column '%s' to the field '%s,'"
            + " which is not visible.", column, fieldName));
      }
    }
    
    return model;
  }
  
  public Object[] get(Class<?> modelType, String queryPostfix) throws ModelInterfaceException, SQLException {
    @SuppressWarnings("rawtypes")
    Constructor builder;
    try {
      builder = modelType.getConstructor();
    } catch (NoSuchMethodException e) {
      throw new ModelInterfaceException("Model classes must have a default constructor.");
    } catch (SecurityException e) {
      throw new ModelInterfaceException("Getting the model class constructor is not allowed by the security manager.");
    }
    
    if( !mappings.containsKey(modelType) ) {
      initializeModelClass(modelType);
    }
    
    ModelMapping mapping = mappings.get(modelType);
    String query = String.format("SELECT * FROM %s" + (queryPostfix != null ? " " + queryPostfix : ""), mapping.table);
    Statement s = conn.createStatement();
    s.execute(query);
    ResultSet rs = s.getResultSet();
    ArrayList<Object> models = new ArrayList<Object>();
    
    HashMap<String, Integer> columnsToIndex = new HashMap<String, Integer>();
    ResultSetMetaData rsmd = rs.getMetaData();
    for( int idx = 1; idx <= rsmd.getColumnCount(); idx++ ) {
      columnsToIndex.put(rsmd.getColumnName(idx), idx);
    }
    
    while( rs.next() ) {
      Object model = constructModelFromResultSet(modelType, builder, rs, columnsToIndex, mapping);
      models.add(model);
    }
    return models.toArray();
  }
  
  public Object[] getWhere(Class<?> modelType, Map<String, Object> where) throws ModelInterfaceException, SQLException {
    @SuppressWarnings("rawtypes")
    Constructor builder;
    try {
      builder = modelType.getConstructor();
    } catch (NoSuchMethodException e) {
      throw new ModelInterfaceException("Model classes must have a default constructor.");
    } catch (SecurityException e) {
      throw new ModelInterfaceException("Getting the model class constructor is not allowed by the security manager.");
    }
    
    if( !mappings.containsKey(modelType) ) {
      initializeModelClass(modelType);
    }
    
    ModelMapping mapping = mappings.get(modelType);
    
    WhereClause wc = new WhereClause();
    
    // TODO - automatically detect whether to use "equals" or "in" depending on if object is an array or not
    for( String key : where.keySet() ) {
      wc.addConstraint(key, where.get(key), WhereClause.WHERE_TYPE.EQUAL);
    }
    
    PreparedStatement stmt = wc.getPreparedStatement(conn, String.format("SELECT * FROM %s", mapping.table));
    stmt.execute();
    ResultSet rs = stmt.getResultSet();
    
    HashMap<String, Integer> columnsToIndex = new HashMap<String, Integer>();
    ResultSetMetaData rsmd = rs.getMetaData();
    for( int idx = 1; idx <= rsmd.getColumnCount(); idx++ ) {
      columnsToIndex.put(rsmd.getColumnName(idx), idx);
    }
    
    ArrayList<Object> models = new ArrayList<Object>();
    while( rs.next() ) {
      models.add(constructModelFromResultSet(modelType, builder, rs, columnsToIndex, mapping));
    }
    
    return models.toArray();
  }
  
  public Object[] get(Class<?> modelType) throws ModelInterfaceException, SQLException {
    return get(modelType, null);
  }
  
  public Object getByPk(Class<?> modelType, Object pk) throws ModelInterfaceException, SQLException {
    if( !mappings.containsKey(modelType) ) {
      initializeModelClass(modelType);
    }
    
    ModelMapping mapping = mappings.get(modelType);
    
    JDBCType pkType = ModelInterface.TYPE_MAP.get(mapping.primaryKeyType);
    
    if( pkType == null ) {
      throw new ModelInterfaceException("The primary key is of an unsupported type.");
    }
    
    String query = String.format("SELECT * FROM %s WHERE %s = ?", mapping.table, mapping.primaryKey);
    PreparedStatement ps = conn.prepareStatement(query);
    ps.setObject(1, pk);
    ps.execute();
    ResultSet rs = ps.getResultSet();
    
    if( !rs.next() ) {
      return null;
    } else {
      return constructModelFromResultSet(modelType, rs);
    }
  }
  
  public boolean deleteByPk(Class<?> modelType, Object pk) throws ModelInterfaceException, SQLException {
    if( !mappings.containsKey(modelType) ) {
      initializeModelClass(modelType);
    }
    
    ModelMapping mapping = mappings.get(modelType);
    
    JDBCType pkType = ModelInterface.TYPE_MAP.get(mapping.primaryKeyType);
    
    if( pkType == null ) {
      throw new ModelInterfaceException("The primary key is of an unsupported type.");
    }
    
    String query = String.format("DELETE FROM %s WHERE %s = ?", mapping.table, mapping.primaryKey);
    PreparedStatement ps = conn.prepareStatement(query);
    ps.setObject(1, pk);
    int row = ps.executeUpdate();
    return row > 0;
  }
  
  public Integer insertModel(Object m) throws ModelInterfaceException, SQLException {
    Class<?> type = m.getClass();
    
    if( !mappings.containsKey(type) ) {
      initializeModelClass(type);
    }
    
    HashMap<String, Object> params = new HashMap<String, Object>();
    ModelMapping mapping = mappings.get(type);
    for( String column : mapping.columnToField.keySet() ) {
      String fieldName = mapping.columnToField.get(column);
      Field field;
      try {
        field = type.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        throw new ModelInterfaceException("Unexpected exception: field not found in target type");
      } catch (SecurityException e) {
        throw new ModelInterfaceException("Getting fields from the model class is not allowed by the security manager.");
      }
      try {
        params.put(column, field.get(m));
      } catch (IllegalArgumentException e) {
        throw new ModelInterfaceException("Unexpected exception: Could not get field value from model.");
      } catch (IllegalAccessException e) {
        throw new ModelInterfaceException(String.format("Field '%s' in model class '%s' is not visible.", fieldName,
            type.getSimpleName()));
      }
    }
    
    // get non-generated columns
    
    String[] insertColumns = new String[mapping.columnToField.size() - mapping.generatedColumns.size()];
    int cIdx = 0;
    for( String column : mapping.columnToField.keySet() ) {
      if( !mapping.generatedColumns.contains(column) ) insertColumns[cIdx++] = column;
    }
    
    String[] qs = new String[insertColumns.length];
    for( int i = 0; i < qs.length; i++ ) qs[i] = "?";
    String qstring = "(" + String.join(", ", qs) + ")";
    
    String query = "INSERT INTO " + mapping.table + " (" + String.join(", ", insertColumns) + ") VALUES " + qstring + ";";
    PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
    
    for( int idx = 0; idx < insertColumns.length; idx++ ) {
      ps.setObject(idx+1, params.get(insertColumns[idx]));
    }
    
    ps.executeUpdate();
    
    ResultSet rs = ps.getGeneratedKeys();
    if(rs.next()) {
      return ((Long)rs.getLong(1)).intValue();
    } else {
      return null;
    }
  }
}
