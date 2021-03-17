package com.tool4us.treatdb;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.tool4us.common.AppOptions;

import lib.turbok.util.UsefulTool;



/**
 * AppSetting manages Application Setting Values.
 * 
 * @author TurboK
 */
public enum AppSetting
{
    OPT;
    
    private AppOptions  _options = null;
    
    private String  _serverID = null;
    
    /**
     * Listening Port for Web-Service
     */
    private int     _port = 8888;
    
    private int     _bossThreadNum = 1;
    
    private int     _serviceThreadNum = 4;
    
    /**
     * 0: Information Level
     * 1: Debug Level
     * 2: Debug Level + Network Debug Level
     */
    private String      _configFile = null;

    private String      _temporaryFolder = null;

    private boolean     _withConsole = false;
    
    private boolean     _fileLogging = true;
    
    // 이전에 만들어진 PMLog 파일 유지 여부
    private boolean     _keepOld = true;
    
    private Map<String, String>     _param = new TreeMap<String, String>();

    
    private AppSetting()
    {
        _options = new AppOptions();
    }
    
    public void initialize(String configFile) throws Exception
    {
        _configFile = configFile;
        
        reload();
    }
    
    public void reload() throws Exception
    {
        _options.initialize(_configFile);
        load();
    }
    
    private void load() throws Exception
    {
        String[] pathName = new String[] { "folder/temporary", "folder/vroot" };

        for(String key : pathName)
        {
            String value = _options.getAsString(key);
            
            if( value == null )
                continue;
            
            if( value.startsWith("./") )
            {
                value = UsefulTool.GetModulePath() + File.separator + value.substring(2);
            }
            
            _param.put(key, value);
        }
        
        _serverID = _options.getAsString("setting/id");
        _port = _options.getAsInteger("network/port", 8080);
        _bossThreadNum = _options.getAsInteger("network/bossThread", 1);
        _serviceThreadNum = _options.getAsInteger("/network/workerThread", 4);
        _withConsole = _options.getAsBoolean("setting/withConsole", false);
        _fileLogging = _options.getAsBoolean("logging/useFile", true);

        _temporaryFolder = parameter("folder", "temporary");

        if( _temporaryFolder == null )
            _temporaryFolder = UsefulTool.GetModulePath() + File.separator + "temporary";

        // System.out.println(_options.toJsonString());
    }
    
    public int port()
    {
        return _port;
    }
    
    public int bossThreadNum()
    {
        return _bossThreadNum;
    }
    
    public int serviceThreadNum()
    {
        return _serviceThreadNum;
    }
    
    public String id()
    {
        return _serverID;
    }
    
    private String parameter(String category, String type)
    {
        return _param.get(UsefulTool.concat(category, "/", type));
    }

    public String temporaryFolder()
    {
        return _temporaryFolder;
    }

    public boolean withConsole()
    {
        return _withConsole;
    }
    
    public boolean loggingFile()
    {
        return _fileLogging;
    }
    
    public String resultFolder()
    {
        return this.parameter("folder", "result");
    }
    
    public String virtualRoot()
    {
        return this.parameter("folder", "vroot");
    }

    public String virtualDir(String vDir)
    {
        return this.parameter("folder", vDir);
    }

    public boolean isKeepOldMade()
    {
        return this._keepOld;
    }
    
    public int sizeOfDatabase()
    {
        JSONArray dbList = _options.getAsList("database");
        
        return dbList == null ? 0 : dbList.length();
    }
    
    public String[] getDatabaseOption(int idx)
    {
        JSONArray dbList = _options.getAsList("database");
        
        if( dbList == null )
            return null;
        
        JSONObject dbOpt = (JSONObject) dbList.get(idx);
        
        return new String[]
        {
              dbOpt.getString("name")
            , "com.mysql.jdbc.Driver"
            , dbOpt.getString("server")
            , dbOpt.getString("account")
            , dbOpt.getString("password")
        };
    }
}
