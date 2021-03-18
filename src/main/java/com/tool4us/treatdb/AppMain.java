package com.tool4us.treatdb;

import static com.tool4us.treatdb.AppSetting.OPT;
import static com.tool4us.treatdb.tool.PMLogBank.PB;
import static com.tool4us.treatdb.task.JobQueue.JQ;
import static com.tool4us.db.DatabaseTool.DBTOOL;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.json.JSONObject;

import com.tool4us.common.Logs;

import lib.turbok.util.DataFileManager;
import lib.turbok.util.UsefulTool;



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
    
    public void test()
    {
        try
        {
            // name, driver, server, account, password
            String[] dbOpt = OPT.getDatabaseOption(0);
            JSONObject dbMeta = DBTOOL.getMetadata(dbOpt[1], dbOpt[2], dbOpt[3], dbOpt[4]);
            Logs.info(dbMeta.toString());
        }
        catch(Exception xe)
        {
            xe.printStackTrace();
        }
    }

}
