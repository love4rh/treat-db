package com.tool4us.common;

import java.io.File;

import org.json.JSONArray;
import org.json.JSONObject;



/**
 * Application 구동을 위하여 필요한 옵션을 관리용 클래스.
 * YML 형태로 정의된 옵션파일(혹은 문자열)을 읽어 계층적으로 값을 가지고 있는 상태에서 지정한 값을 반환하는 기능을 수행.
 * 계층적인 값은 key1/key2/key3와 같이 '/'로 구분하여 정의함. 
 */
public class AppOptions
{
    private JSONObject      _options = null;
    
    
    public void initialize(String configFile) throws Exception
    {
        Object ymlObj = YMLParser.toJsonObject(new File(configFile));
        
        if( !(ymlObj instanceof JSONObject) )
            throw new Exception("invalid type");
        
        _options = (JSONObject) ymlObj;
    }
    
    public String toJsonString()
    {
        return _options == null ? "null" : _options.toString();
    }
    
    /**
     * 지정한 위치의 키에 해당하는 옵션 객체 반환.
     * @param keyPath parent/child/subchild 와 같이 '/'로 구분하여 옵션이 있는 위치를 지정함.
     * @return 지정한 옵션 객체. 없으면 null 반환. JSONObject, JSONArray, String, Double, Long, Boolean 가능함.
     */
    public Object getOption(String keyPath)
    {
        String[] path = keyPath.split("/");
        
        Object tmpNode = null;
        JSONObject node = _options;
        
        int idx = 0;
        
        try
        {
            while( node != null && idx < path.length)
            {
                tmpNode = node.get(path[idx]);
                if( tmpNode != null && tmpNode instanceof JSONObject )
                    node = (JSONObject) tmpNode;
                else
                    node = null;
    
                idx += 1;
            }
        }
        catch(Exception xe)
        {
            tmpNode = null;
        }

        return idx == path.length ? tmpNode : null;
    }

    /**
     * 지정한 위치의 키가 있는 지 여부 반환.
     * @param keyPath parent/child/subchild 와 같이 '/'로 구분하여 옵션이 있는 위치를 지정함.
     * @return 지정한 옵션 존재 여부
     */
    public boolean hasOption(String keyPath)
    {
        return getOption(keyPath) != null;
    }
    
    public JSONObject getAsObject(String keyPath)
    {
        return (JSONObject) getOption(keyPath);
    }
    
    public JSONArray getAsList(String keyPath)
    {
        return (JSONArray) getOption(keyPath);
    }
    
    public String getAsString(String keyPath)
    {
        return (String) getOption(keyPath);
    }
    
    public Long getAsLong(String keyPath, int defVal)
    {
        Long l = (Long) getOption(keyPath);
        return l == null ? defVal : l;
    }
    
    public Integer getAsInteger(String keyPath, int defVal)
    {
        Long l = (Long) getOption(keyPath);
        
        return l == null ? defVal : l.intValue();
    }
    
    public Boolean getAsBoolean(String keyPath, boolean defVal)
    {
        Boolean b = (Boolean) getOption(keyPath);
        return b == null ? defVal : b;
    }
    
    public Double getAsDouble(String keyPath, double defVal)
    {
        Double d = (Double) getOption(keyPath);
        return d == null ? defVal : d;
    }
}
