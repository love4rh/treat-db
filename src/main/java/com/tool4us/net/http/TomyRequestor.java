package com.tool4us.net.http;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.multipart.FileUpload;

import static com.tool4us.common.Util.UT;



public class TomyRequestor implements HttpRequest
{
    private HttpRequest                 _httpRq = null;
    private ChannelHandlerContext       _ctx = null;

    private Map<String, List<String>>   _params = null;
    private Map<String, FileUpload>     _files = null;

    private long        _sTick = 0;
    private String      _traceID = null;

    private String      _uriAdj = null;
    private String      _bodyData = "";
    private String      _mimeType = null;

    
    public TomyRequestor(HttpRequest req, ChannelHandlerContext ctx)
    {
        _ctx = ctx;
        _httpRq = req;

        _sTick = UT.tickCount();
        _traceID = UT.makeRandomeString(12);
    }
    
    public void initialize(String uriPath, Map<String, List<String>> params)
    {
        _uriAdj = uriPath;

        if( params != null && !params.isEmpty() )
        {
            _params = params;
        }
    }
    
    public void putParameter(String key, String value)
    {
        if( _params == null )
        {
            _params = new TreeMap<String, List<String>>();
        }
        
        List<String> valList = _params.get(key);
        if( valList == null )
        {
            valList = new ArrayList<String>();
            _params.put(key, valList);
        }

        valList.add(value);
    }
    
    public void putParameter(String key, FileUpload file)
    {
        if( _files == null )
        {
            _files = new TreeMap<String, FileUpload>();
        }

        _files.put(key, file);
    }

    public void putBodyData(String bodyData)
    {
        _bodyData += bodyData;
    }

    public Channel channel()
    {
        return _ctx.channel();
    }
    
    public HttpRequest getHttpRequest()
    {
        return _httpRq;
    }
    
    public long getRequestedTime()
    {
        return _sTick;
    }
    
    public String getTraceID()
    {
        return _traceID;
    }
    
    public String getRemoteDescription()
    {
        if( _ctx == null )
            return "Unkwoun";
        
        return _ctx.channel().remoteAddress().toString();
    }
    
    public List<String> getHeaderValues(String name)
    {
        return _httpRq.headers().getAll(name);
    }
    
    // 여러 헤더값 중 첫 번째 것을 반환. 없으면 null.
    public String getHeaderValue(String name)
    {
        return _httpRq.headers().get(name);
    }
    
    public String getBodyData()
    {
        return _bodyData;
    }

    public String getMimeType()
    {
        if( _mimeType == null )
        {
            String contentType = _httpRq.headers().get(HttpHeaderNames.CONTENT_TYPE);
            if( UT.isValidString(contentType) )
                _mimeType = (String) HttpUtil.getMimeType(contentType);
            else
                _mimeType = "";
        }

        return _mimeType; 
    }

    @Override
    public HttpVersion getProtocolVersion()
    {
        return _httpRq.protocolVersion();
    }

    @Override
    public HttpHeaders headers()
    {
        return _httpRq.headers();
    }

    @Override
    public DecoderResult getDecoderResult()
    {
        return _httpRq.decoderResult();
    }

    @Override
    public void setDecoderResult(DecoderResult result)
    {
        _httpRq.setDecoderResult(result);
    }

    @Override
    public HttpMethod getMethod()
    {
        return this.method();
    }

    @Override
    public HttpRequest setMethod(HttpMethod method)
    {
        return _httpRq.setMethod(method);
    }

    @Override
    public String getUri()
    {
        return this.uri();
    }

    @Override
    public HttpRequest setUri(String uri)
    {
        return _httpRq.setUri(uri);
    }

    @Override
    public HttpRequest setProtocolVersion(HttpVersion version)
    {
        return _httpRq.setProtocolVersion(version);
    }
    
    public String getParameter(String paramName)
    {
        return parameter(paramName);
    }
    
    public String parameter(String paramName)
    {
        if( _params == null )
            return null;

        List<String> pList = _params.get(paramName);
        
        return pList == null ? null : pList.get(0);
    }
    
    public File parameterFile(String name) throws Exception
    {
        FileUpload upFile = _files.get(name);
        
        return upFile == null ? null : upFile.getFile();
    }

    public Map<String, List<String>> parameterMap()
    {
        return _params;
    }

    @Override
    public HttpVersion protocolVersion()
    {
        return _httpRq.protocolVersion();
    }

    @Override
    public DecoderResult decoderResult()
    {
        return _httpRq.decoderResult();
    }

    @Override
    public HttpMethod method()
    {
        return _httpRq.method();
    }

    @Override
    public String uri()
    {
        return _uriAdj != null ? _uriAdj : _httpRq.uri();
    }

    public String oneLineHeader(int limit)
    {
        StringBuilder sb = new StringBuilder();
        
        for(Entry<String, String> elem : _httpRq.headers().entries())
        {
            sb.append(elem.getKey()).append("=").append(UT.makeEllipsis(elem.getValue(), limit)).append(";");
        }

        return sb.toString();
    }
    
    public String oneLineParameter(int limit)
    {
        StringBuilder sb = new StringBuilder();
        
        if( _params != null && !_params.isEmpty() )
        {
            for(Entry<String, List<String>> elem : _params.entrySet())
            {
                List<String> valueList = elem.getValue();
                sb.append(elem.getKey()).append("=")
                  .append(valueList.isEmpty() ? "" : UT.makeEllipsis(valueList.get(0), limit))
                  .append(";");
            }
        }
        
        if( _files != null && !_files.isEmpty() )
        {
            for(Entry<String, FileUpload> elem : _files.entrySet())
            {
                FileUpload file = elem.getValue();
                sb.append(elem.getKey()).append("=File(")
                  .append(file.getFilename()).append(", ").append(file.length())
                  .append(");");
            }
        }

        return sb.toString();
    }
    
    public String oneLineBody(int limit)
    {
        return UT.isValidString(_bodyData) ? UT.makeEllipsis(UT.makeSingleLine(_bodyData), limit) : "";
    }
}
