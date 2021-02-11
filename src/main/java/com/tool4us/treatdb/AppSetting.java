package com.tool4us.treatdb;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import lib.turbok.archive.ArchiveElement;
import lib.turbok.archive.XMLArchive;
import lib.turbok.util.UsefulTool;


/**
 * AppSetting manages Application Setting Values.
 * 
 * @author mh9.kim
 */
public enum AppSetting
{
    OPT;
    
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
        //
    }
    
    public void initialize(String configFile) throws Exception
    {
        _configFile = configFile;
        
        XMLArchive ar = new XMLArchive(_configFile, true);
        
        load(ar, false);
        
        ar.close();
    }
    
    public void reload() throws Exception
    {
        XMLArchive ar = new XMLArchive(_configFile, true);
        
        load(ar, true);
        
        ar.close();
    }
    
    public void load(XMLArchive ar, boolean isReload) throws Exception
    {
        ArchiveElement arElem = null;
        
        while( null != (arElem = ar.readElement()) )
        {
            String tagPath = arElem.getTagPath();
            
            if( tagPath.endsWith("/treatHD") )
            {
                    // 모두 읽은 경우임
                if( arElem.isReadFinished() )
                    break;
            }
            
            if( arElem.isReadFinished() )
            {
                if( !isReload && tagPath.endsWith("/param") )
                {
                    String keyValue = UsefulTool.concat(arElem.getParent().getTagName()
                            , "/", arElem.getAttribute("name"));

                    String value = arElem.getTextValue();
                    
                    if( keyValue.startsWith("folder") && value.startsWith("./") )
                    {
                        value = UsefulTool.GetModulePath() + File.separator + value.substring(2);
                    }
                    
                    _param.put(keyValue, value);
                }
                else if( !isReload && tagPath.endsWith("/setting/id") )
                {
                    _serverID = arElem.getTextValue();
                }
                else if( !isReload && tagPath.endsWith("/network/servicePort") )
                {
                    _port = Integer.parseInt(arElem.getTextValue());
                }
                else if( !isReload && tagPath.endsWith("/network/bossThread") )
                {
                    _bossThreadNum = Integer.parseInt(arElem.getTextValue());
                }
                else if( !isReload && tagPath.endsWith("/network/workerThread") )
                {
                    _serviceThreadNum = Integer.parseInt(arElem.getTextValue());
                }
                else if( !isReload && tagPath.endsWith("/withConsole") )
                {
                    _withConsole = "true".equals(arElem.getTextValue());
                }
                else if( !isReload && tagPath.endsWith("/logging/useFile") )
                {
                    _fileLogging = "true".equals(arElem.getTextValue());
                }
                else if( !isReload && tagPath.endsWith("/keepOlds") )
                {
                    _keepOld = "true".equals(arElem.getTextValue());
                }
            }
        }

        _temporaryFolder = parameter("folder", "temporary");

        if( _temporaryFolder == null )
            _temporaryFolder = UsefulTool.GetModulePath() + File.separator + "temporary";
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
}
