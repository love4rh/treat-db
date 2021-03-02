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

        // 리스트 가능 여부
        boolean maybeList = ch == '-' && isWhitespace(lineText, p + 1);

        if( _curToken == null )
        {
            _curToken = new YMLToken(_rootToken, curIndent);

            if( maybeList )
                _curToken.setType(Type.LIST);
        }

        boolean prevSpace = true;
        int vStart = curIndent; // 값의 시작 위치
        
        while( p < lineText.length() )
        {
            ch = lineText.charAt(p);
            
            // 주석. 이후 문자는 무시함
            if( ch == '#' && prevSpace )
            {
                // TODO 현재 처리 중인 토큰을 클로즈 해야 하나?
                lineText.substring(vStart, p).trim();
                break;
            }
            
            // 다음 문자가 공백(라인 끝 포함)인지 여부
            boolean nextSpace = this.isWhitespace(lineText, p + 1);
            
            // 새로운 객체. 현재 토큰이 이 값을 수용할 수 있는지 판단해야 함.
            if( ch == ':' && nextSpace )
            {
                if( _curToken != null )
                {
                    
                }
                
                _curToken = new YMLToken(_curToken, curIndent)
                    .setKey(lineText.substring(curIndent, p).trim())
                ;
            }
            // 주석 (제일 뒤에 나오는 것만 따져야 함)
            else if( ch == '#' )
            {
                //
            }
            else
            {
                //
            }
            
            prevSpace = Character.isWhitespace(ch);
            
            p += 1;
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
