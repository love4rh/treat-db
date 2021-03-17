package com.tool4us.treatdb;

import static com.tool4us.treatdb.AppSetting.OPT;
import static com.tool4us.treatdb.tool.PMLogBank.PB;
import static com.tool4us.treatdb.task.JobQueue.JQ;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import com.tool4us.common.Logs;

import lib.turbok.util.DataFileManager;
import lib.turbok.util.UsefulTool;

import static com.tool4us.common.Util.UT;



/**
 * Application Main class
 * 
 * @author TurboK
 */
public class AppMain
{
    private TreatPMServer   _serviceServer = null;
    
    private BatchJobs       _batchJob = null;
    
    
    public AppMain()
    {
        //
    }
    
    public void start(String configFile) throws Exception
    {
        // kill -15 처리
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                Logs.info("Respository Server: Shutting down");
                
                try
                {
                    stopSerrver();
                }
                catch( Exception xe )
                {
                    Logs.trace(xe);
                }
                
                Logs.info("Respository Server: Shutdown completed.");
            }
        });
        
        // 설정파일 읽기
        OPT.initialize(configFile);
        
        if( OPT.loggingFile() )
        {
            Logs.initialize(UsefulTool.concat(UsefulTool.GetModulePath(), File.separator, "log") , "treatDB");
            Logs.addConsoleLogger();
        }
        else
        {
            Logs.instance();
            Logs.addConsoleLogger();
        }

        initialize();

        // TODO CHECK
        // NetOnSetting.C.initialize(OPT.temporaryFolder(), false, Logs.instance());

        _serviceServer = new TreatPMServer();
        _serviceServer.start();
        
        if( OPT.withConsole() )
        {
            this.console();
        }
    }
    
    private void initialize() throws Exception
    {
        JQ.begin();
        PB.initialize(OPT.resultFolder(), !OPT.isKeepOldMade());
        
        DataFileManager.deleteTempFiles(-1);

        _batchJob = new BatchJobs();
        _batchJob.start();
    }

    /**
     * enter CLI mode.
     */
    public void console()
    {
        String command;
        BufferedReader bufRead = new BufferedReader(new InputStreamReader(System.in));
        
        while( true )
        {
            System.out.print(">> ");
            
            try
            {
                command = bufRead.readLine();
            }
            catch(Exception xe)
            {
                xe.printStackTrace();
                continue;
            }
            
            if( command != null )
                command = command.trim();
            
            if( command == null || command.isEmpty() )
                continue;
            
            boolean showTime = true;
            long startTick = System.currentTimeMillis();
            
            if( "q".equalsIgnoreCase(command) )
            {
                stopSerrver();
                break;
            }
            else if( "t1".equalsIgnoreCase(command) )
            {
                test();
            }
            
            if( showTime )
            {
                System.out.println("Processing Time: " + (System.currentTimeMillis() - startTick) + " ms");
            }
        }
    }
    
    public void stopSerrver()
    {
        JQ.end();
        _serviceServer.stop();
        
        if( _batchJob != null )
            _batchJob.end();
    }
    
    public String getTypeFromJDBCTypes(String typeStr)
    {
        switch( UT.parseLong(typeStr).intValue() )
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
    
    public void test()
    {
        // https://www.progress.com/blogs/jdbc-tutorial-extracting-database-metadata-via-jdbc-driver 참고

        Connection conn = null;
        
        try
        {
            // name, driver, server, account, password
            String[] dbOpt = OPT.getDatabaseOption(0);

            Class.forName(dbOpt[1]);
            conn = DriverManager.getConnection(dbOpt[2], dbOpt[3], dbOpt[4]);
            DatabaseMetaData meta = conn.getMetaData();
            
            ResultSet resultSet = meta.getTables(null, null, null, new String[]{ "TABLE", "VIEW", "ALIAS", "SYNONYM" });
            System.out.println("Printing TABLE_TYPE \"TABLE\" ");
            while( resultSet.next() )
            {
            	String tableName = resultSet.getString("TABLE_NAME");
            	
            	System.out.println("==================================");
            	System.out.println(tableName);
            	System.out.println("----------------------------------");

            	ResultSet columns = meta.getColumns(null, null, tableName, null);
            	while( columns.next() )
            	{
            		System.out.printf("[%s] [Type: %s] [Size: %s] [Digits: %s] [Nullable: %s] [AutoInc: %s]\n"
            			, columns.getString("COLUMN_NAME")
            			, getTypeFromJDBCTypes(columns.getString("DATA_TYPE"))
            			, columns.getString("COLUMN_SIZE")
            			, columns.getString("DECIMAL_DIGITS")
            			, columns.getString("IS_NULLABLE")
            			, columns.getString("IS_AUTOINCREMENT")
            		);
            	}

                
            }
        }
        catch(Exception xe)
        {
            xe.printStackTrace();
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
    }

}
