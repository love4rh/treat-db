package com.tool4us.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import org.json.JSONArray;
import org.json.JSONObject;

import lib.turbok.common.ValueType;
import lib.turbok.data.Columns;
import lib.turbok.data.FileMapStore;
import lib.turbok.data.TabularDataCreator;
import lib.turbok.task.ITaskMonitor;

import static com.tool4us.common.Util.UT;



public enum DatabaseTool
{
	DBTOOL;
	
	private DatabaseTool()
	{
		//
	}
	
	public String typeStringByJDBCType(int type)
    {
        switch( type )
        {
        case Types.BIT:
        case Types.TINYINT:
        case Types.SMALLINT:
        case Types.INTEGER:
        case Types.BIGINT:
        case Types.FLOAT:
            return "Integer";
            
        case Types.REAL:
        case Types.DOUBLE:
        case Types.NUMERIC:
        case Types.DECIMAL:
            return "Real";
            
        case Types.DATE:
        case Types.TIME:
        case Types.TIMESTAMP:
            return "DateTime";
            
        case Types.BOOLEAN:
        case Types.CHAR:
        case Types.VARCHAR:
        case Types.LONGVARCHAR:
        case Types.BINARY:
        case Types.VARBINARY:
        case Types.LONGVARBINARY:
        case Types.NULL:
        case Types.OTHER:
        case Types.JAVA_OBJECT:
        case Types.DISTINCT:
        case Types.STRUCT:
        case Types.ARRAY:
        case Types.BLOB:
        case Types.CLOB:
        case Types.REF:
        case Types.DATALINK:
        case Types.ROWID:
        case Types.NCHAR:
        case Types.NVARCHAR:
        case Types.LONGNVARCHAR:
        case Types.NCLOB:
        case Types.SQLXML:
            
        default:
            return "Text";
        }
    }
	
	public String typeStringByJDBCType(String typeStr)
	{
		return typeStringByJDBCType( UT.parseLong(typeStr).intValue() ); 
	}
	
	public ValueType valueTypeByJDBCType(int type)
    {
        switch( type )
        {
        case Types.BIT:
        case Types.TINYINT:
        case Types.SMALLINT:
        case Types.INTEGER:
        case Types.BIGINT:
        case Types.FLOAT:
            return ValueType.Integer;
            
        case Types.REAL:
        case Types.DOUBLE:
        case Types.NUMERIC:
        case Types.DECIMAL:
            return ValueType.Real;
            
        case Types.DATE:
        case Types.TIME:
        case Types.TIMESTAMP:
            return ValueType.DateTime;
            
        case Types.BOOLEAN:
        case Types.CHAR:
        case Types.VARCHAR:
        case Types.LONGVARCHAR:
        case Types.BINARY:
        case Types.VARBINARY:
        case Types.LONGVARBINARY:
        case Types.NULL:
        case Types.OTHER:
        case Types.JAVA_OBJECT:
        case Types.DISTINCT:
        case Types.STRUCT:
        case Types.ARRAY:
        case Types.BLOB:
        case Types.CLOB:
        case Types.REF:
        case Types.DATALINK:
        case Types.ROWID:
        case Types.NCHAR:
        case Types.NVARCHAR:
        case Types.LONGNVARCHAR:
        case Types.NCLOB:
        case Types.SQLXML:
            
        default:
            return ValueType.Text;
        }
    }
	
	public Object getObjectFromJSON(JSONObject obj, String key)
	{
		Object retObj = null;
		
		try
		{
			retObj = obj.get(key);
		}
		catch( Exception xe )
		{
			retObj = null;
		}
		
		return retObj;
	}
	
	public JSONArray getMetadata(String driver, String server, String account, String password) throws Exception
	{
        Connection conn = null;
        JSONArray objList = new JSONArray();
        
        try
        {
            Class.forName(driver);
            conn = DriverManager.getConnection(server, account, password);
            DatabaseMetaData meta = conn.getMetaData();
            
            ResultSet resultSet = meta.getTables(null, null, null, new String[]{ "TABLE", "VIEW", "ALIAS", "SYNONYM" });

            /*
        	ResultSetMetaData md = resultSet.getMetaData();
        	for(int i = 1; i <= md.getColumnCount(); ++i)
        	{
        		System.out.printf("%d. %s\n", i, md.getColumnName(i));
        	}
        	System.out.println("----------------------------------");
        	// */
        	
            while( resultSet.next() )
            {
            	JSONObject tblObj = new JSONObject();
            	
            	String tableName = resultSet.getString("TABLE_NAME");
            	String tableType = resultSet.getString("TABLE_TYPE");
            	String tableDesc= resultSet.getString("REMARKS");
            	String tableScheme = resultSet.getString("TABLE_SCHEM");
            	if( tableScheme == null )
            		tableScheme = resultSet.getString("TABLE_CAT");
            	
            	tblObj.put("name", tableName);
            	tblObj.put("type", tableType);
            	tblObj.put("scheme", UT.NVL(tableScheme, "unknown"));
            	tblObj.put("description", UT.NVL(tableDesc, " "));

            	JSONArray colList = new JSONArray();
            	ResultSet columns = meta.getColumns(null, null, tableName, null);
            	
            	tblObj.put("columns", colList);
            	
            	while( columns.next() )
            	{
            		JSONObject colObj = new JSONObject();
            		
            		colObj.put("name", columns.getString("COLUMN_NAME"));
            		colObj.put("type", typeStringByJDBCType(columns.getString("DATA_TYPE")));
            		// colObj.put("jdbcType", UT.parseLong(columns.getString("DATA_TYPE")));
            		// colObj.put("size", UT.parseLong(columns.getString("COLUMN_SIZE")));
            		colObj.put("nullable", "YES".equals(columns.getString("IS_NULLABLE")));
            		colObj.put("description", UT.NVL(columns.getString("REMARKS"), " "));

            		colList.put(colObj);
            	}
            	
            	objList.put(tblObj);                
            }
        }
        catch(Exception xe)
        {
            throw xe;
        }
        finally
        {
            if( conn != null )
                try
                {
                    conn.close();
                }
                catch( SQLException xe )
                {
                    xe.printStackTrace();
                }
        }
        
        return objList;
    }
	
	public JSONObject executeQuery(String query, String driver, String server, String account, String password) throws Exception
	{
	    Class.forName(driver);
	    Connection conn = DriverManager.getConnection(server, account, password);

        Statement stmt = conn.createStatement();
        
        boolean isMySQL = driver.contains("mysql");
        
        if( isMySQL )
            stmt.setFetchSize(Integer.MIN_VALUE);

        ResultSet rs = null;
        
        long insRow = 0;
        FileMapStore resultData = null;
        
        try
        {
            rs = stmt.executeQuery(query);
            rs.setFetchSize(2048);    // CHECK 이 값이 너무 크면 이상한 오류가 남.
            
            ResultSetMetaData rsMeta = rs.getMetaData();
                    
            int columnSize = rsMeta.getColumnCount();
            Columns columns = new Columns();
            
            for(int c = 1; c <= columnSize; ++c)
            {
                String columnName = rsMeta.getColumnName(c);
                ValueType vType = valueTypeByJDBCType(rsMeta.getColumnType(c));
                
                columns.addColumn(columnName, vType);
            }
            
            resultData = TabularDataCreator.newTabularData(columns, 1024);

            while( rs.next() )
            {
                for(int c = 1; c <= columnSize; ++c)
                {
                    resultData.setCell(c - 1, insRow, rs.getString(c));
                }
                
                ++insRow;
            }
        }
        catch(Exception xe)
        {
            throw xe;
        }
        finally
        {
            if( rs != null )
                rs.close();
            if( conn != null )
                conn.close();
        }
	    
	    return null;
	}
}
