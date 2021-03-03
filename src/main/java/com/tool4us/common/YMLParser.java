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
        
        // 여러 묶음이 들어 갈 수 있으므로 MAPPING을 루트 토큰의 타입을 설정함
        _rootToken = new YMLToken(null, 0).setType(Type.MAPPING);
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
                switch( ch )
                {
                case '-':
                    tokenType = Type.LIST;
                    break;
                case ':':
                    tokenType = Type.KEY;
                    break;
                case '>':
                    tokenType = Type.SINGLELINE;
                    break;
                case '|':
                    tokenType = Type.MULTILINE;
                    break;
                default:
                    break;
                }

                if( tokenType != Type.UNKNOWN )
                {
                    p = skipSpace(text, p + 1);
                }
            }
            
            if( tokenType == Type.UNKNOWN )
            {
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
                // TODO 현재 처리 중인 토큰 정리
                _curToken.rollUp(0);
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

        while( p < lineText.length() )
        {
            Object[] tokenInfo = this.extractNextToken(lineText, p);
            Type tokenType = (Type) tokenInfo[0];
            int np = (int) tokenInfo[1];
            String infoValue = (String) tokenInfo[2];
            
            System.out.println("Indent: " + curIndent + ", Type: " + tokenType
                + ", Next Position: " + np + ", value: [" + infoValue + "]");
            
            p = np;
        }
    }
    
    public JSONObject getResult()
    {
        // TODO implementation...
        return null;
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
            , "map1: # 코멘트"
            , "# 라인 코멘트"
            , "  child0:    "
            , "  child1: ab#c"
            , "  child2: e-fc"
            , "test2:"
            , "- - - "
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
