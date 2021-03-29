package com.tool4us.treatdb.task;

import static com.tool4us.common.Util.UT;
import static com.tool4us.db.DatabaseTool.DBTOOL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import com.tool4us.common.Logs;
import com.tool4us.treatdb.tool.UserSession;

import lib.turbok.common.ValueType;
import lib.turbok.data.Columns;
import lib.turbok.data.FileMapStore;
import lib.turbok.data.TabularDataCreator;
import lib.turbok.task.ITask;
import lib.turbok.util.TabularDataTool;



/**
 * doing DB job instead
 * @author TurboK
 */
public class DBTask extends ITask
{
	private String		_qid;
	private UserSession	_session;
	
	private String		_query;
	private String		_driver;
	private String		_server;
	private String		_account;
	private String		_password;
	
	private Exception	_exception = null;
	private String		_initialData = null;
	
	private FileMapStore	_resultData = null;
    
    public DBTask(UserSession session, String qid
    	, String query, String driver, String server, String account, String password)
    {
    	_session = session;
        _qid = qid;
        
        _query = query;
        _driver = driver;
        _server = server;
        _account = account;
        _password = password;
    }
    
    public String getKey()
    {
    	return _qid;
    }

    @Override
    public boolean isPossibleToRun()
    {
        return true;
    }

    @Override
    public String toString()
    {
        return "DB-TASK-" + _qid;
    }

    @Override
    public void run() throws Exception
    {
    	long sTick = UT.tickCount();
    	
    	Class.forName(_driver);
	    Connection conn = DriverManager.getConnection(_server, _account, _password);

        Statement stmt = conn.createStatement();
        boolean isMySQL = _driver.contains("mysql");
        
        if( isMySQL )
            stmt.setFetchSize(Integer.MIN_VALUE);

        long insRow = 0;
        ResultSet rs = null;
        
        _initialData = null;

        try
        {
            rs = stmt.executeQuery(_query);
            rs.setFetchSize(2048);    // CHECK 이 값이 너무 크면 이상한 오류가 남.
            
            ResultSetMetaData rsMeta = rs.getMetaData();
                    
            int columnSize = rsMeta.getColumnCount();
            Columns columns = new Columns();
            
            for(int c = 1; c <= columnSize; ++c)
            {
                String columnName = rsMeta.getColumnName(c);
                ValueType vType = DBTOOL.valueTypeByJDBCType(rsMeta.getColumnType(c));
                
                columns.addColumn(columnName, vType);
            }
            
            _resultData = TabularDataCreator.newTabularData(columns, 1024);
            _session.pushTaskResult(_qid, _resultData, UT.tickCount() - sTick);

            boolean hasNext = rs.next();
            while( _session.isValid() && hasNext )
            {
                for(int c = 1; c <= columnSize; ++c)
                {
                	_resultData.setCell(c - 1, insRow, rs.getString(c));
                }
                
                ++insRow;
                hasNext = rs.next();
                
                if( hasNext && insRow == 1024 )
                {
                	_initialData = makeData(insRow, !hasNext);
                	synchronized( this )
                	{
                		this.notifyAll();
                	}
                }
            }
            
            if( _initialData == null )
            {
            	_initialData = makeData(insRow, true);
            	synchronized( this )
            	{
            		this.notifyAll();
            	}
            }

            Logs.info("QID:[{}], COLUMN:[{}], RECORD:[{}], PROCTIME:[{} ms]", _qid, columnSize, insRow, UT.tickCount() - sTick);
            _session.doneTaskResult(_qid, _resultData, UT.tickCount() - sTick);
        }
        catch(Exception xe)
        {
        	_exception = xe;
        	xe.printStackTrace();

        	synchronized( this )
        	{
        		this.notifyAll();
        	}
        }
        finally
        {
            if( rs != null )
                rs.close();
            if( conn != null )
                conn.close();
        }
    }
    
    private String makeData(long row, boolean fetchDone) throws Exception
    {
    	StringBuilder sb = new StringBuilder();

    	sb.append("{");
    	
    	sb.append("\"qid\":\"").append(_qid).append("\"");
    	sb.append(",\"fetchDone\":").append(fetchDone);
    	sb.append(",").append(TabularDataTool.genMetaAsJson(_resultData, false));
    	sb.append(",").append(TabularDataTool.genRecordsAsJson(_resultData, 0, row));
    	
    	sb.append("}");
    	
    	return sb.toString();
    }

	public String getInitialData() throws Exception
	{
		if( _exception != null )
			throw _exception;

		return _initialData;
	}
}
