package com.tool4us.treatdb;

import static com.tool4us.treatdb.AppSetting.OPT;

import java.io.File;

import com.tool4us.common.Logs;
import com.tool4us.net.http.IStaticFileMap;
import com.tool4us.net.http.TomyServer;

import lib.turbok.task.TaskQueue;



/**
 * Request Handling Server
 * 
 * @author mh9.kim
 */
public class TreatPMServer
{
    private TomyServer      _serverBase = null;
    
    private TaskQueue       _taskQueue = null;
    
    
    private static class StaticFileMap implements IStaticFileMap
    {
        @Override
        public String getRootFile()
        {
            return "index.html";
        }
        
        @Override
        public boolean isAllowed(String uriPath)
        {
            // 소스 보안을 위하여 *.map 파일은 반환하지 않음.
            return !uriPath.endsWith(".map");
        }

        @Override
        public File getFile(String uriPath)
        {
            String vDir = null;
            int sPos = uriPath.indexOf("/", 1);

            if( sPos != -1 )
            {
                vDir = uriPath.substring(1, sPos);
                // vDir에 해당하는 실제 경로 찾기
                vDir = OPT.virtualDir(vDir);
            }
            
            if( vDir != null )
                uriPath = uriPath.substring(sPos);
            else
                vDir = OPT.virtualRoot();
            
            return new File(vDir + uriPath);
        }
    }
      
    public TreatPMServer()
    {
        _taskQueue = new TaskQueue(null);
    }
    
    public void start() throws Exception
    {
        if( _serverBase != null )
            return;
     
        Logs.info("Starting TreatDB Service Server at port {}.", OPT.port());

        // Secure 통신하려면 아래 주석 해제
        // System.setProperty("ssl", "true");

        _serverBase = new TomyServer("com.tool4us.treatdb.service", new StaticFileMap());
        _serverBase.start(OPT.port(), OPT.bossThreadNum(), OPT.serviceThreadNum(), 0);

        _taskQueue.startQueue(2, "TreatDB Batch");
    }
    
    public void stop()
    {
        if( _serverBase != null )
            _serverBase.shutdown();

        _serverBase = null;
        
        _taskQueue.endQueue();
    }
}
