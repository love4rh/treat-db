package com.tool4us.common;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.tool4us.common.YMLToken.Type;



/**
 * Simple YML parser.
 * 프로그램 실행 옵션을 YML 형태로 지정하여 사용하기 위한 용도로 만들었음.
 * YML의 문법 중 다음만 지원함.
 * 
 * {@code
 * # this is line comment.
 * object: # this is in-line comment.
 *   id: 1
 *   name: kim
 *   
 * list:
 * - string1
 * - string2
 * 
 * objectList
 * - color: red
 *   direction: left
 * - color: blue
 *   direction: right
 * comment_multi_line: |
 *   Hello world.
 *   This is Kim.
 *   
 * comment_single_line: >
 *   Hello world.
 *   This is Kim.
 * 
 * single_quote: '
 * This is awesome.
 * '
 * 
 * double_quote: "
 * This is awesome too.
 * "
 * }
 * 
 * 목록 및 객체를 정의하기 위한 [], {}는 지원하지 않음.
 *   
 * @author TurboK
 */
public class YMLParser
{
    private static char ESCAPE_CHAR = '\\';

    private int         _lineNo = 0;

    private YMLToken    _rootToken = null;

    // 현재 처리 중인 토큰
    private YMLToken    _curToken = null;
    
    private List<YMLToken>      _documents = new ArrayList<YMLToken>();

    // 따옴표 진행 중 여부.
    private Type        _quoteOn = Type.UNKNOWN;

    
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
    private Object[] extractNextToken(final String text, int begin)
    {
        Type tokenType = Type.UNKNOWN;
        
        int p = begin;
        StringBuilder sb = new StringBuilder();

        boolean valueOn = false; // 값이 할당되었는지 여부
        
        char ch = p <= 0 ? ' ' : text.charAt(p - 1);
        boolean prevSpace = isWhitespace(text, p - 1);
        
        while( p < text.length() && tokenType == Type.UNKNOWN )
        {
            char pch = ch;
            ch = text.charAt(p);

            if( pch == ESCAPE_CHAR )
            {
                switch( ch )
                {
                case 'n': ch = '\n'; break;
                case 't': ch = '\t'; break;
                default:
                    break;
                }
            }
            else if( ch == ESCAPE_CHAR )
                continue;
            
            // 주석. 이후 문자는 무시함
            if( '#' == ch && prevSpace )
            {
                tokenType = Type.COMMENT;
                p = text.length();
            }
            else if( !valueOn && pch != ESCAPE_CHAR && (ch == '"' || ch == '\'') )
            {
                tokenType = ch == '"' ? Type.QUOTED : Type.QUOTES;
                p += 1;
            }
            else if( isWhitespace(text, p + 1) ) // 다음 문자가 스페이스라면
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
                
                if( ch == ESCAPE_CHAR )
                    ch = '\0';
            }
        }
        
        String infoValue = sb.toString();

        if( !infoValue.isEmpty() && tokenType == Type.UNKNOWN )
            tokenType = Type.VALUE;

        return new Object[] { tokenType, p, infoValue };
    }
    
    private Object[] doingQuote(final String text, int begin, boolean singleQuote) throws ParseException
    {
        char ending = singleQuote ? '\'' : '"';
 
        int p = begin;
        boolean closed = false;
        StringBuilder sb = new StringBuilder();

        char ch = '\0';
        while( p < text.length() && !closed )
        {
            char pch = ch;
            ch = text.charAt(p);
            
            if( pch == ESCAPE_CHAR )
            {
                switch( ch )
                {
                case 'n': ch = '\n'; break;
                case 't': ch = '\t'; break;
                default:
                    break;
                }

                sb.append(ch);
                ch = '\0';
            }
            else if( ch == ending )
            {
                p = skipSpace(text, p + 1);
                if( p < text.length() && text.charAt(p) != '#' )
                    throw makeException("only quotation mark expected", _lineNo, -1);

                closed = true;
            }
            else if( ch != ESCAPE_CHAR )
                sb.append(ch);
            
            p += 1;
        }
        
        return new Object[] { sb.toString(), closed };
    }

    public void pushLineText(final String lineText) throws ParseException
    {
        _lineNo += 1;
        
        // 따옴표 처리 중이라면
        if( _quoteOn != Type.UNKNOWN && _curToken != null )
        {
            Object[] r = doingQuote(lineText, 0, _quoteOn == Type.QUOTES);
            
            _curToken.addValue((String) r[0]);
            
            if( (Boolean) r[1] )
            {
                _curToken.setClosed(true);
                _quoteOn = Type.UNKNOWN;
            }
            
            return;
        }

        // --- 고려 해야 함.
        if( lineText.startsWith("---") )
        {
            // 새로운 블럭의 시작임
            if( _curToken != null )
            {
                _curToken.rollUp(_rootToken.indent());
                
                // 처리 중인 토큰은 다큐멘트 목록에 추가하고,
                _documents.add(_rootToken);

                // 새롭게 시작
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
        boolean keyOn = false, listOn = false, multiLine = false;

        while( p < lineText.length() )
        {
            Object[] tokenInfo = extractNextToken(lineText, p);

            int np = (int) tokenInfo[1];
            Type tokenType = (Type) tokenInfo[0];
            String infoValue = (String) tokenInfo[2];
            
            curIndent = p;
            
            //*
            System.out.printf("%d: Indent: %d, Type: %s, Next Position: %d, value: [%s], LastTokenType: %s\n"
                , _lineNo, curIndent, tokenType, np, infoValue, (_curToken == null ? "n/a" : _curToken.type())
            ); // */
            
            if( tokenType == Type.COMMENT )
            {
                p = np;
                continue;
            }
            else if( multiLine )
                throw makeException("expected a comment or a line break", _lineNo, p);

            if( tokenType == Type.KEY )
            {
                if( keyOn || _rootToken.isType(Type.VALUE) )
                    throw makeException("mapping value not allowed here", _lineNo, p);
                
                if( _rootToken.isType(Type.UNKNOWN) )
                    _rootToken.setType(Type.MAPPING);
                if( _curToken != null && _curToken.isType(Type.UNKNOWN) )
                    _curToken.setType(Type.MAPPING);

                keyOn = true;
                
                // indent에 맞는 parent를 찾아야 함.
                
                // Indent가 작아지면 새로운 항목이 시작됨을 의미함 --> 현재까지 처리된 내용 정리 필요
                if( _curToken != null && curIndent <= _curToken.indent() )
                {
                    // curIndent 보다 하나 더 위로 가야 함.
                    YMLToken tokenAtIndent = findToken(curIndent);
                    YMLToken parentToken = tokenAtIndent.parent();
                    
                    // 목록의 원소가 목록의 키명칭과 같은 indent로 정의된 경우로 이 경우는 한 단계 더 올라가야 실제 상위 객체
                    if( parentToken.indent() == tokenAtIndent.indent() ) // tokenAtIndent.isType(Type.LIST) && 
                        parentToken = parentToken.parent();

                    if( parentToken.valueIndent() != -1 && parentToken.valueIndent() != curIndent )
                        throw makeException("invalid indent found", _lineNo, p);
                    
                    if( !parentToken.canBeType(Type.MAPPING, parentToken.indent()) )
                        throw makeException("mapping value not allowed here", _lineNo, p);

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
                    YMLToken parentToken = tokenAtIndent.parent();

                    _curToken = rollUp(parentToken.indent());
                    
                    parentToken = _curToken.parent();
                    
                    if( parentToken != null && parentToken.isType(Type.LIST) && parentToken.indent() == _curToken.indent() )
                    {
                        _curToken = _curToken.rollUp(parentToken.indent());
                    }
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
            else if( tokenType == Type.MULTILINE || tokenType == Type.SINGLELINE )
            {
                if( _curToken == null )
                {
                    if( !_rootToken.isType(Type.UNKNOWN) )
                        throw makeException("unexpected token", _lineNo, p);
                    _curToken = _rootToken;
                }

                if( !_curToken.canBeType(tokenType, curIndent) )
                    throw makeException("mapping value not allowed here", _lineNo, p);

                _curToken.setType(tokenType);

                multiLine = true;
            }
            else if( tokenType == Type.QUOTED || tokenType == Type.QUOTES )
            {
                if( _curToken == null )
                {
                    if( !_rootToken.isType(Type.UNKNOWN) )
                        throw makeException("unexpected token", _lineNo, p);
                    _curToken = _rootToken;
                }

                if( !_curToken.canBeType(Type.MULTILINE, curIndent) )
                    throw makeException("mapping value not allowed here", _lineNo, p);
                
                _curToken.setType(Type.MULTILINE);
                
                Object[] r = doingQuote(lineText, np, tokenType == Type.QUOTES);
                
                _curToken.addValue((String) r[0]);
                
                if( !(Boolean) r[1] )
                    _quoteOn = tokenType;
                else
                    _curToken.setClosed(true);

                break;
            }
            else if( _curToken != null )
            {
                if( _curToken.isClosed() )
                    throw makeException("expected a comment or a line break", _lineNo, p);
                else if( _curToken.valueIndent() == -1 )
                    _curToken.setValueIndent(curIndent);
                else if( _curToken.valueIndent() > curIndent )
                    throw makeException("invalid value indent", _lineNo, p);

                _curToken.addValue(infoValue);
            }
            else if( tokenType == Type.VALUE && _rootToken.isType(Type.UNKNOWN) )
            {
                _curToken = _rootToken;
                _rootToken.setType(tokenType).addValue(infoValue);
            }

            p = np;
        }
    }
    
    private ParseException makeException(String message, int line, int column)
    {
        return new ParseException(message + " at [" + line + ", " + (column + 1) + "]", line);
    }
    
    private YMLToken rollUp(int indent) throws ParseException
    {
        _curToken = _curToken.rollUp(indent);
        if( _curToken == null )
            throw makeException("invalid indent found", _lineNo, -1);

        return _curToken;
    }
    
    private YMLToken findToken(int indent) throws ParseException
    {
        YMLToken token = _curToken.findParent(indent);
        
        if( token == null )
            throw makeException("invalid indent found", _lineNo, -1);
        
        return token;
    }
    
    public int countOfDocument()
    {
        return _documents.size();
    }
    
    public void finish()
    {
        if( _curToken != null )
        {
            // 정리하는 작업을 해야 함.
            _curToken.rollUp(_rootToken.indent());
            _curToken = null;
            
            _documents.add(_rootToken);
            
            // 다시 할 경우를 대비해서 초기화 함.
            _rootToken = new YMLToken(null, -1).setKey("$ROOT$");
        }
    }
    
    /**
     * Parsing 결과를 객체 형태로 반환.
     * @param index Document 번호
     * @return Parsing 결과에 따라, JSONObject, JSONArray, String, Long, Double일 수 있음.
     */
    public Object getDocument(int index)
    {
        finish();
        
        YMLToken token = _documents.get(index);
        return token.getValueObject();
    }

    
    public static Object toJsonObject(InputStream is, int documentNo) throws Exception
    {
        YMLParser psr = new YMLParser();
        
        BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        
        String lineText = in.readLine();
        while( lineText != null )
        {
            psr.pushLineText(lineText);
            lineText = in.readLine();
        }

        in.close();
        psr.finish();

        return psr.countOfDocument() < documentNo ? null : psr.getDocument(documentNo);
    }
    
    /**
     * YML을 파싱하여 결과를 객체 형태로 반환함. 반환되는 객체의 형태는 YMLParser.getDocument() 참고.
     * @param ymlText       파싱 대상 YML
     * @param documentNo    파싱한 YML 내 Document 번호. 파싱 결과 Document의 개수가 이 값보다 작으면 null 반환.
     * @return 파싱된 결과 객체 반환. 형태는 YMLParser.getDocument() 참고
     * @throws Exception
     */
    public static Object toJsonObject(String ymlText, int documentNo) throws Exception
    {
        return toJsonObject(new ByteArrayInputStream(ymlText.getBytes()), documentNo);
    }
    
    public static Object toJsonObject(String ymlText) throws Exception
    {
        return toJsonObject(ymlText, 0);
    }
    
    public static Object toJsonObject(File ymlFile, int documentNo) throws Exception
    {
        return toJsonObject(new FileInputStream(ymlFile), documentNo);
    }
    
    public static Object toJsonObject(File ymlFile) throws Exception
    {
        return toJsonObject(new FileInputStream(ymlFile), 0);
    }
}
