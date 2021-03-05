package com.tool4us.common;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.text.ParseException;

import org.json.JSONObject;

import com.tool4us.common.YMLToken.Type;



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
    private int         _lineNo = 0;
    
    private YMLToken    _rootToken = null;
    
    // 현재 처리 중인 토큰
    private YMLToken    _curToken = null;

    
    public YMLParser()
    {
        _lineNo = 0;
        _curToken = null;

        _rootToken = new YMLToken(null, -1).setKey("$ROOT$");
    }
    
    // p에 해당하는 문자가 Whitespace인지 여부 반환.
    public boolean isWhitespace(final String text, int p)
    {
        return p < 0 || p >= text.length() || Character.isWhitespace(text.charAt(p));
    }

    // start부터 시작하여 Whitespace가 아닌 위치를 찾아 반환.
    public int skipSpace(final String text, int start)
    {
        while( start < text.length() && Character.isWhitespace(text.charAt(start)) )
            start += 1;
        
        return start;
    }
    
    /**
     * 다음 토큰을 추출하여 반환함.
     * @param text  파싱할 문자열
     * @param begin 시작 위치
     * @return Object[] { tokenType, 현재까지 처리한  위치, 부가 정보 }
     */
    public Object[] extractNextToken(final String text, int begin)
    {
        Type tokenType = Type.UNKNOWN;
        
        int p = begin;
        StringBuilder sb = new StringBuilder();

        boolean valueOn = false;
        boolean prevSpace = isWhitespace(text, p - 1);
        
        while( p < text.length() && tokenType == Type.UNKNOWN )
        {
            char ch = text.charAt(p);
            boolean nextSpace = isWhitespace(text, p + 1);

            // 주석. 이후 문자는 무시함
            if( '#' == ch && prevSpace )
            {
                tokenType = Type.COMMENT;
                p = text.length();
            }
            else if( nextSpace )
            {
                if( '-' == ch && !valueOn )
                {
                    if( _curToken == null || begin <= _curToken.indent() || !_curToken.isValueType() ) 
                        tokenType = Type.LIST;
                }
                else if( ':' == ch )
                {
                    tokenType = Type.KEY;
                }
                else if( '>' == ch )
                {
                    tokenType = Type.SINGLELINE;
                }
                else if( '|' == ch )
                {
                    tokenType = Type.MULTILINE;
                }

                if( tokenType != Type.UNKNOWN )
                {
                    p = skipSpace(text, p + 1);
                    break;
                }
            }
            
            if( tokenType == Type.UNKNOWN )
            {
                valueOn = true;
                sb.append(ch);
                prevSpace = Character.isWhitespace(ch);
                p += 1;
            }
        }
        
        String infoValue = sb.toString();

        if( !infoValue.isEmpty() && tokenType == Type.UNKNOWN )
            tokenType = Type.VALUE;

        return new Object[] { tokenType, p, infoValue };
    }

    public void pushLineText(final String lineText) throws ParseException
    {
        _lineNo += 1;
        
        // --- 고려 해야 함.
        if( lineText.startsWith("---") )
        {
            // 새로운 블럭의 시작임
            if( _curToken != null )
            {
                _curToken.rollUp(_rootToken.indent());

                // TODO _rootToken을 넣어 두어야 함.
                _rootToken = new YMLToken(null, -1).setKey("$ROOT$");
            }
            _curToken = null;
            return;
        }

        int p = skipSpace(lineText, 0);
        int curIndent = p;
        
        // 값을 그냥 추가해야 하는 경우인지 체크해서 처리했다면 끝!
        if( _curToken != null && _curToken.checkAddValue(lineText, curIndent) )
            return;

        // 빈 문자열이거나 화이트 스페이스만 있는 문자열임
        if( p >= lineText.length() )
            return;

        char ch = lineText.charAt(p);
        
        // 주석 라인임 --> 무시
        if( '#' == ch )
            return;
        
        // KEY는 한 라인에 한 개
        // indent가 작아지면 상위 객체로 이동
        boolean keyOn = false;
        boolean listOn = false;

        while( p < lineText.length() )
        {
            Object[] tokenInfo = extractNextToken(lineText, p);

            int np = (int) tokenInfo[1];
            Type tokenType = (Type) tokenInfo[0];
            String infoValue = (String) tokenInfo[2];
            
            curIndent = p;
            
            System.out.println(_lineNo + ", Indent: " + curIndent + ", Type: " + tokenType
                + ", Next Position: " + np + ", value: [" + infoValue + "]");

            if( tokenType == Type.KEY )
            {
                if( keyOn )
                {
                    throw new ParseException("mapping value not allowed here at [" + _lineNo + "]", _lineNo);
                }
                
                if( _rootToken.isType(Type.UNKNOWN) )
                    _rootToken.setType(Type.MAPPING);

                keyOn = true;
                
                // indent에 맞는 parent를 찾아야 함.
                
                // Indent가 작아지면 새로운 항목이 시작됨을 의미함 --> 현재까지 처리된 내용 정리 필요
                if( _curToken != null && curIndent <= _curToken.indent() )
                {
                    // curIndent 보다 하나 더 위로 가야 함.
                    YMLToken tokenAtIndent = findToken(curIndent);
                    YMLToken parentToken = tokenAtIndent.parent();
                    
                    if( parentToken.valueIndent() != -1 && parentToken.valueIndent() != curIndent )
                        throw new ParseException("invalid indent found at [" + _lineNo + "]", _lineNo);
                    
                    if( !parentToken.canBeType(Type.MAPPING, parentToken.indent()) )
                        throw new ParseException("mapping value not allowed here at [" + _lineNo + "]", _lineNo);

                    parentToken.setType(Type.MAPPING);
                    _curToken = rollUp(parentToken.indent());
                }

                _curToken = new YMLToken(_curToken == null ? _rootToken : _curToken, listOn ? p : curIndent).setKey(infoValue);
            }
            else if( tokenType == Type.LIST )
            {
                if( _rootToken.isType(Type.UNKNOWN) )
                    _rootToken.setType(Type.LIST);

                curIndent = p;
                listOn = true;
                
                // indent에 맞는 parent를 찾아야 함.
                if( _curToken != null && !_curToken.isType(Type.UNKNOWN) && curIndent <= _curToken.indent() )
                {
                    // curIndent 보다 하나 더 위로 가야 함.
                    YMLToken tokenAtIndent = findToken(curIndent);
                    _curToken = rollUp(tokenAtIndent.parent().indent());
                }
                
                if( _curToken == null || _curToken == _rootToken )
                    _curToken = new YMLToken(_rootToken, curIndent).setType(Type.LIST);

                // 현재 토큰이 있고 LIST 형태라면 값 토큰을 하나 추가해서 이후 값을 받을 수 있도록 해야 함.
                if( _curToken != null )
                {
                    if( _curToken.isUndefinedValue() )
                        _curToken.setType(Type.LIST);

                    if( _curToken.isType(Type.LIST) )
                        _curToken = new YMLToken(_curToken, curIndent);
                }
            }
            else if( tokenType == Type.COMMENT )
            {
                // nothing to do
            }
            else if( _curToken != null )
            {
                _curToken.addValue(infoValue);
            }

            p = np;
        }
    }
    
    private YMLToken rollUp(int indent) throws ParseException
    {
        _curToken = _curToken.rollUp(indent);
        if( _curToken == null )
            throw new ParseException("invalid indent found at [" + _lineNo + "]", _lineNo);

        return _curToken;
    }
    
    private YMLToken findToken(int indent) throws ParseException
    {
        YMLToken token = _curToken.findParent(indent);
        
        if( token == null )
            throw new ParseException("invalid indent found at [" + _lineNo + "]", _lineNo);
        
        return token;
    }
    
    public YMLToken getResult()
    {
        if( _curToken != null )
        {
            // 정리하는 작업을 해야 함.
            _curToken.rollUp(_rootToken.indent());
            _curToken = null;
        }
        
        System.out.println(_rootToken.toJson());

        return _rootToken;
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

        // return psr.getResult();
        return null;
    }
    
    
    public static void main(String[] args)
    {
        YMLParser yml = new YMLParser();
        
        String[] testYml = new String[]
        {
            "---"
//            , "map1: # 코멘트"
//            , "# 라인 코멘트"
//            , "  child0:    "
//            , "  child1: ab#c"
//            , "  child2: e-fc"
            , "test0:"
            , "test1:"
            , "  id: 123"
            , "  name: hong"
            , "  desc: "
            , "  phone: 123"
            , "test2:"
            , "  - aaa"
            , "    - dfsa"
            , "  - id: 123"
            , "    name: hong"
            , "test3:"
            , " - a1"
            , " - a2"
            , "test4:"
            , "- - - a - -"
            , "     - b"
            , "  - "
        };
        
        try
        {
            for(String lineText : testYml)
            {
                yml.pushLineText(lineText);
            }
            
            yml.getResult();
        }
        catch(Exception xe)
        {
            xe.printStackTrace();
        }
    }
}
