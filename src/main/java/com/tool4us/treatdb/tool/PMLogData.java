package com.tool4us.treatdb.tool;

import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import com.tool4us.common.Logs;

import lib.turbok.common.ITabularData;
import lib.turbok.data.FileMapStore;
import lib.turbok.util.TabularDataTool;
import lib.turbok.util.TextFileLineReader;



public class PMLogData
{
    private String          _key;
    private String          _source;
    private String          _pmdFilePath;
    
    private Long            _createdTick;
    private String          _logTypes;
    private String          _processNames;
    private String          _contextNames;
    // private String          _msgIDs;

    private String          _contextColors;
    private String          _processColors;
    
    private Map<String, String> _procColorMap;
    private Map<String, String> _ctxColorMap;
    
    // Process, Context의 컬러맵을 JSON 배열 형태로 가지고 있는 멤버
    // ex. [[process colors...], [context colors...]]
    private String          _colorMapJson;
    
    private FileMapStore    _ds;
    
    private Long            _lastUsedTick;

    
    public PMLogData(String key, File metaFile)
    {
        _key = key;
        initialize(metaFile);
    }
    
    static public long collapseToLong(String s)
    {
        long l = 1;

        for(byte b : s.getBytes())
            l *= b;

        return l;
    }
    
    static public Object[] stringsToColors(String[] strList)
    {
        Map<String, String> colorMap = new TreeMap<String, String>();
        
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < strList.length; ++i)
        {
            if( i > 0 ) sb.append(" ");

            long seed = collapseToLong(strList[i]);
            Random rnd = new Random(seed);
            String color = Long.toHexString((long) Math.floor(rnd.nextDouble() * 16777215.0));
            
            sb.append( color );
            colorMap.put(strList[i], color);
        }
        
        return new Object[] { sb.toString(), colorMap };
    }

    @SuppressWarnings({ "unused", "unchecked" })
    private void initialize(File metaFile)
    {
        TextFileLineReader in = null;
        
        try
        {
            in = new TextFileLineReader(metaFile, "UTF-8");
            
            String version = in.getNextLine(); // version
            
            _createdTick = Long.parseLong(in.getNextLine());
            _lastUsedTick = _createdTick;
            
            String tmpKey = in.getNextLine();

            if( !_key.equals(tmpKey) )
            {
                Logs.warn("DataKey mismatched: {} and {}", _key, tmpKey);
            }
            
            _source = in.getNextLine(); // url
            _pmdFilePath = in.getNextLine();
            
            _ds = FileMapStore.newInstance(_pmdFilePath);
            
            _logTypes = in.getNextLine();
            _processNames = in.getNextLine();
            _contextNames = in.getNextLine();
            tmpKey = in.getNextLine(); // _msgIDs

            Object[] clr = stringsToColors(_contextNames.split(" "));
            _contextColors = (String) clr[0];
            
            _ctxColorMap = (Map<String, String>) clr[1];
            
            clr = stringsToColors(_processNames.split(" "));

            _processColors = (String) clr[0];
            _procColorMap = (Map<String, String>) clr[1];

            _colorMapJson = makeLineColorMap(_ds);
        }
        catch( Exception xe )
        {
            xe.printStackTrace();
        }
        finally
        {
            if( in != null )
                in.close();
        }
    }
    
    public void close()
    {
        if( _ds != null )
        {
            _ds.close();
        }
    }

    public String makeLineColorMap(ITabularData ds) throws Exception
    {
        // ColorMap 만들기
        StringBuilder sb1 = new StringBuilder(), sb2 = new StringBuilder();
        sb1.append("[["); sb2.append("[");
        for(long r = 0; r < ds.getRowSize(); ++r)
        {
            if( r > 0 )
            {
                sb1.append(","); sb2.append(",");
            }
            
            Object v = ds.getCell(4, r);
            sb1.append("\"").append( v == null ? "000000" : _procColorMap.get(v) ).append("\"");
            
            v = ds.getCell(5, r);
            sb2.append("\"").append( v == null ? "000000" : _ctxColorMap.get(v) ).append("\"");
        }
        sb2.append("]");
        sb1.append("],").append(sb2.toString()).append("]");
        
        return sb1.toString();
    }
    
    public String toJson() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append("\"dataKey\":\"").append(_key).append("\"");
        sb.append(",\"source\":\"").append(_source).append("\"");
        sb.append(",\"tick\":").append(_createdTick);
        sb.append(",\"category\":{");
        sb.append("\"logTypes\":\"").append(_logTypes).append("\"");
        sb.append(",\"processNames\":\"").append(_processNames).append("\"");
        sb.append(",\"contextNames\":\"").append(_contextNames).append("\"");
        sb.append(",\"contextColors\":\"").append(_contextColors).append("\"");
        sb.append(",\"processColors\":\"").append(_processColors).append("\"");
        sb.append("}");
        sb.append(",\"lineColorMap\":").append(_colorMapJson);

        sb.append(",\"initData\":{");
        sb.append(TabularDataTool.genMetaAsJson(_ds, true));
        sb.append("}");
        
        return sb.toString();
    }

    public String getDataAsJson(long start, long len) 
    {
        try
        {
            return TabularDataTool.genRecordsAsJson(_ds, start, len);
        }
        catch( Exception xe )
        {
            xe.printStackTrace();
        }
        
        return null;
    }
    
    public ITabularData getDataStore()
    {
        return _ds;
    }
    
    public void updateLastUsedTick()
    {
        this._lastUsedTick = (new Date()).getTime();
    }
    
    public long getLastUsedTick()
    {
        return _lastUsedTick;
    }
    
    public String getRawLogFile() throws Exception
    {
        String fileName = _pmdFilePath.substring(0, _pmdFilePath.length() - 3) + "log";

        StringBuilder sb = new StringBuilder();
        TextFileLineReader in = new TextFileLineReader(fileName, "UTF-8");
        
        String lineText = in.getNextLine();
        while( lineText != null )
        {
            sb.append(lineText).append("\n");
            lineText = in.getNextLine();
        }
        
        in.close();
        
        return sb.toString();
    }
}
