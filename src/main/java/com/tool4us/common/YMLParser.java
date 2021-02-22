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
    private Stack<State>    _stack = null;

    
    public static class State
    {
        public int _indent = 0;
        public int _status = 0; // 0: 노말, 1: 목록, 2: 멀티라인, 3: 싱글멀티라인
        public String _value = "";
    }

    
    public YMLParser()
    {
        _result = new JSONObject();
        _stack = new Stack<State>();

        _stack.push(new State());
    }
    
    public void putLineText(String lineText) throws ParseException
    {
        int indent = 0;
        while( Character.isWhitespace(lineText.charAt(indent)) )
            indent += 1;
        
        State state = _stack.peek();

        // 비거나 white space만 있는 문자열임.
        if( indent >= lineText.length() )
        {
            // 멀티라인이면 CR 추가
            if( state._status == 2 )
                state._value += "\n";
            // 싱글멀티라인이면 space 추가
            else if( state._status == 3 )
                state._value += " ";
            
            // 이외의 경우는 할 일 없음.
            return;
        }

        char ch = lineText.charAt(indent);
        
        // 주석은 값을 따로 처리할 필요가 없음.
        if( '#' == ch )
        {
            return;
        }
        else if( '-' == ch )
        {
            
        }
        
        
        //
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
            psr.putLineText(lineText);
        }
        
        in.close();

        return psr.getResult();
    }
}
