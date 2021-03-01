package com.tool4us.common;

import org.json.JSONObject;



/**
 * YML 파싱을 위한 토큰 클래스
 * 
 * @author TurboK
 */
public class YMLToken
{
    public static enum Type
    {
        UNKNOWN,    // 아직 모
        VALUE,      // 단일 라인
        MULTILINE,  // 여러 라인 
        LIST,       // 목록
        MAPPING     // 매핑(객체)
    }
    
    private YMLToken    _parent = null;
    
    private int         _indent = 0;

    /**
     * 0: 아직 모름
     * 1: 단순 값
     * 2: 목록 (-)
     * 3: 멀티라인 문자열 (|)
     * 5: 멀티라인 문자열 (멀티 싱글라인 >) 
     * 4: 객체
     */
    private Type    _type = Type.UNKNOWN;
    
    private String  _key = "";
    
    private StringBuilder   _sbValue;


    public YMLToken(YMLToken parent, int indent)
    {
        _parent = parent;
        _indent = indent;
        _sbValue = new StringBuilder();
    }
    
    public YMLToken parent()
    {
        return _parent;
    }
    
    public int indent()
    {
        return _indent;
    }

    public Type type()
    {
        return _type;
    }
    
    public YMLToken setType(Type type)
    {
        _type = type;
        
        //
        
        return this;
    }
    
    public boolean isType(Type type)
    {
        return _type == type;
    }
    
    /**
     * type을 수용할 수 있는 지 여부 반환 
     */
    public boolean canBeType(Type type, int indent)
    {
        return (_type == Type.UNKNOWN || isType(type)) && this._indent <= indent;
    }
    
    public boolean isValueType()
    {
        return _type == Type.MULTILINE || _type == Type.VALUE;
    }
    
    public String key()
    {
        return _key;
    }
    
    public YMLToken setKey(String key)
    {
        _key = key;
        return this;
    }
    
    public YMLToken addValue(String value)
    {
        _sbValue.append(value);
        return this;
    }
    
    public String getValue()
    {
        return _sbValue.toString();
    }
}
