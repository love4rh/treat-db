package com.tool4us.common;

import org.json.JSONArray;
import org.json.JSONObject;

import static com.tool4us.common.Util.UT;

import java.util.ArrayList;
import java.util.List;



/**
 * YML 파싱을 위한 토큰 클래스
 * 
 * @author TurboK
 */
public class YMLToken
{
    public static enum Type
    {
        VALUE,      // 단순 값
        SINGLELINE, // 단일 라인 문자
        MULTILINE,  // 여러 라인 문자 
        MAPPING,    // 매핑(객체)

        LIST,       // 목록 원소
        KEY,        // 키

        QUOTES,     // 싱글 쿼테이션 시작
        QUOTED,     // 더블 쿼테이션 시작
        COMMENT,    // 코멘트
        
        ERROR,      // 파싱 오류가 있음을 알리기 위한 타입
        UNKNOWN     // 알 수 없음
    }
    
    private YMLToken    _parent = null;
    
    private int         _indent = 0;
    
    private int         _valueIndent = -1;

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
    
    private JSONObject      _mapValue;
    private JSONArray       _lstValue;
    
    // 디버깅 용도로 추가한 멤버임. 없어도 문제 없음.
    private List<YMLToken>  _children = new ArrayList<YMLToken>();


    public YMLToken(YMLToken parent, int indent)
    {
        _parent = parent;
        _indent = indent;
        _valueIndent = -1;
        _sbValue = new StringBuilder();
        
        if( parent != null )
        {
            parent.setValueIndent(indent);
            parent._children.add(this);
        }
    }
    
    @Override
    public String toString()
    {
        return String.format("KEY:{%s} TYPE:{%s} INDENT:{%d} VINDENT:{%d} VALUE:{%s} CHILD:{%d}"
            , _key, _type, _indent, _valueIndent, _sbValue.toString(), _children.size()
        );
    }
    
    public YMLToken parent()
    {
        return _parent;
    }
    
    public int indent()
    {
        return _indent;
    }
    
    /**
     * 값이 시작되는 위치 반환.
     */
    public int valueIndent()
    {
        return _valueIndent;
    }
    
    public void setValueIndent(int indent)
    {
        _valueIndent = indent;
    }

    public Type type()
    {
        return _type;
    }
    
    public YMLToken setType(Type type)
    {
        _type = type;
        
        if( _type == Type.LIST )
        {
            _lstValue = new JSONArray();
            _valueIndent = -1;
        }
        else if( _type == Type.MAPPING )
        {
            if( _mapValue == null )
                _mapValue = new JSONObject();
        }
        
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
        return _type == Type.MULTILINE || _type == Type.VALUE || _type == Type.SINGLELINE;
    }
    
    public boolean isMultiLined()
    {
        return _type == Type.MULTILINE || _type == Type.SINGLELINE;
    }
    
    /**
     * 가져야 할 값이 어떤 형태인지 정해졌는지 여부 반환
     */
    public boolean isUndefinedValue()
    {
        // TODO LIST 형태는 값이 정해진 것으로 보아야 하나?
        return _type == Type.UNKNOWN || _type == Type.KEY;
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
        if( _type == Type.UNKNOWN )
            this.setType(Type.VALUE);
        
        if( !_sbValue.toString().isEmpty() )
            _sbValue.append(" ");

        _sbValue.append(value);

        return this;
    }
    
    public String getValue()
    {
        return _sbValue.toString();
    }
    
    public boolean checkAddValue(String lineText, int indent)
    {
        if( lineText.isEmpty() && isMultiLined() )
        {
            _sbValue.append(_type == Type.MULTILINE ? '\n' : ' ');
            return true;
        }

        if( !isMultiLined() || _indent >= indent || (_valueIndent != -1 && _valueIndent < indent) )
            return false;

        if( _valueIndent != -1 )
            _sbValue.append(_type == Type.MULTILINE ? '\n' : ' ');
        else
            _valueIndent = indent;
        
        if( lineText.length() > indent )
            _sbValue.append(lineText.substring(indent));

        return true;
    }
    
    /**
     * indent에 해당하는 상위 객체까지 할당된 값을 정리하여 설정하는 작업 수행.
     * 완료되면 indent보다 크지 않은 가장 큰 경우의 YMLToken이 반환됨.
     */
    public YMLToken rollUp(int indent)
    {
        if( _parent == null )
            return null;

        Object value = null;
        
        if( isType(Type.LIST) )
        {
            value = _lstValue;
            
            /*
            if( _key != null && !_key.isEmpty() )
            {
                JSONObject obj = new JSONObject();
                obj.put(_key, value);
                value = obj;
            } // */
        }
        else if( isType(Type.MAPPING) )
        {
            value = _mapValue;
        }
        else if( isValueType() )
        {
            String s = _sbValue.toString();
            
            if( !s.isEmpty() )
            {
                if( isMultiLined() )
                {
                    value = s;
                }
                else if( "true".equals(s) )
                {
                    value = Boolean.TRUE;
                }
                else if( "false".equals(s) )
                {
                    value = Boolean.FALSE;
                }
                else
                {
                    value = UT.parseLong(s);

                    if( value == null )
                        value = UT.parseDouble(s);

                    if( value == null )
                        value = s;
                }
            }
        }

        return _parent._rollUp(indent, _key, value);
    }

    private YMLToken _rollUp(int indent, String cKey, Object value)
    {   
        // 값 설정
        if( isType(Type.LIST) )
        {
            if( _indent == -1 && value instanceof JSONArray )
                _lstValue = (JSONArray) value;
            else
                _lstValue.put(value);
        }
        else if( isType(Type.UNKNOWN) )
        {
            _type = Type.VALUE;;
            _sbValue.append(value);
        }
        else if( cKey != null )
        {   
            _mapValue.put(cKey, value != null ? value : "$null$"); // null을 표현하고 싶은데 JSONObject에서 미지원이네...
        }

        return _indent == indent ? this : rollUp(indent);
    }
    
    public YMLToken findParent(int indent)
    {
        if( _parent == null )
            return null;
        
        return _indent == indent ? this : _parent.findParent(indent);
    }
     
    public String toJson()
    {
        Object value = null;
        
        if( isType(Type.LIST) )
        {
            value = _lstValue;
        }
        else if( isType(Type.MAPPING) )
        {
            value = _mapValue;
        }
        else if( isValueType() )
        {
            value = _sbValue.toString();
        }

        return value == null ? null : value.toString();
    }
}
