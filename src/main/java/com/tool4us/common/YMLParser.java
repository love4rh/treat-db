package com.tool4us.common;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Stack;

import org.json.JSONObject;



/**
 * Simple YML parser.
 * YML의 문법 중 다음만 지원함. 프로그램 실행 옵션을 YML 형태로 지정 시 사용 
 * 
 * # this is comment.
 * name: kim
 * job: designer
 * married: true
 * age: 20
 * 
 * fruit_list:
 * - apple
 * - banana
 * 
 * object:
 *   name: kim
 *   job: developer
 * 
 * object_list:
 * - color: red
 *   direction: left
 * - color: blue
 *   direction: right
 *
 * comment_multi_line: |
 *   Hello world.
 *   This is Kim.
 *   
 * comment_single_line: >
 *   Hello world.
 *   This is Kim.
 *   
 * @author TurboK
 */
public class YMLParser
{
    private JSONObject      _result = null;
    
    // 이전 단게의 상태 저장. [indent, element 상태(목록, 멀티라인, 싱글멀티라인), 현재까지 추가한 값]
    private Stack<Token>    _stack = null;

    
    public static class Token
    {
        public int _indent = 0;

        /**
         * 0: 아직 모름
         * 1: 단순 값
         * 2: 목록 (-)
         * 3: 멀티라인 문자열 (|)
         * 5: 멀티라인 문자열 (멀티 싱글라인 >) 
         * 4: 객체
         */
        private int     _type = 0;

        private String  _key = "";
        private String  _value = "";
        
        public Token()
        {
            _indent = 0;
            _type = 0;
            _value = "";
        }

        public Token(String key, int indent, int type)
        {
            _key= key;
            _indent = indent;
            _type = type;
        }
        
        public Token setIndent(int indent)
        {
            _indent = indent;
            return this;
        }
        
        public Token setType(int type)
        {
            _type = type;
            return this;
        }
        
        public boolean isMultiLine()
        {
            return _type == 3;
        }
        
        public boolean isMultiSingleLine()
        {
            return _type == 4;
        }
    }

    
    public YMLParser()
    {
        _result = new JSONObject();
        _stack = new Stack<Token>();
    }
    
    // p에 해당하는 문자가 Whitespace인지 여부 반환.
    public boolean isWhitespace(final String text, int p)
    {
        return p <= text.length() || Character.isWhitespace(text.charAt(p));
    }

    // start부터 시작하여 Whitespace가 아닌 위치를 찾아 반환.
    public int skipSpace(final String text, int start)
    {
        while( start < text.length() && Character.isWhitespace(text.charAt(start)) )
            start += 1;
        
        return start;
    }

    public void pushLineText(final String lineText) throws ParseException
    {
        // --- 고려 해야 함.
        if( lineText.startsWith("---") )
        {
            // 새로운 블럭의 시작임
            _stack = new Stack<Token>();
            return;
        }

        int p = skipSpace(lineText, 0);

        int curIndent = p;
        Token curToken = _stack.isEmpty() ? null : _stack.peek();

        // 빈 문자열이거나 화이트 스페이스만 있는 문자열임
        if( p >= lineText.length() )
        {
            if( curToken != null )
            {
                
            }
            // |(CR) 나 >(공백) 로 멀티 라인이라면 추가하고
            // 키가 정의된 것이 있다면 null 값 입력
            // 값이 와야 하는데 없다면 에러 
            // 이외의 경우는 무시
            
            return;
        }
        
        char ch = lineText.charAt(p);
        StringBuilder sbKey = new StringBuilder();

        while( p < lineText.length() )
        {
            // 다음 문자가 공백(라인 끝 포함)인지 여부
            boolean nextSpace = this.isWhitespace(lineText, p + 1);
            
            ch = lineText.charAt(p);
            
            // 특정 상황을 나타내는 문자 뒤는 화이트 스페이스가 옮
            
            // 새로운 키 (Object)
            if( nextSpace && ch == ':' )
            {
                //
            }
            else if( nextSpace && ch == '-' )
            {
                //
            }
            else
            {
                //
            }
            
            p += 1;
        }
        
        // 
        // 0: not defined
        // 1: key-value
        int prevToken = 0;
        int prevIndent = 0;
        
        // prevIndent < indent --> 새로운 key 등장 필요
        
        // 주석 --> # ~
        // 새로운 키의 시작 --> foo:
        // 목록 --> -
        // 문자열만 존재
        
        Token state = null;
        
        // 스택에 아무것도 없는 경우는 처음 객체가 필요한 경우임.
        if( _stack.isEmpty() )
        {
            
        }
        
    }
    
    public JSONObject getResult()
    {
        return _result;
    }

    public static JSONObject toJsonObject(String ymlText) throws Exception
    {
        YMLParser psr = new YMLParser();
        
        BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(ymlText.getBytes()), "UTF-8"));
        
        String lineText = in.readLine();
        while( lineText != null )
        {
            psr.pushLineText(lineText);
            lineText = in.readLine();
        }

        in.close();

        return psr.getResult();
    }
    
    
    public static void main(String[] args)
    {
        YMLParser yml = new YMLParser();
        
        String[] testYml = new String[]
        {
              "---"
            , "te,:st: aaa: "
            , ""
            , "test2:"
            , "- "
            , "  - aaa"
            , "    - dfsa"
        };
        
        try
        {
            for(String lineText : testYml)
            {
                yml.pushLineText(lineText);
            }
        }
        catch(Exception xe)
        {
            xe.printStackTrace();
        }
    }
}
