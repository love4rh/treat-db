package com.tool4us.net.http;

import java.util.List;
import java.util.Map;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;



public class RequestEx implements HttpRequest
{
    private HttpRequest                 _httpRq = null;
    private Map<String, List<String>>   _params = null;
    private ChannelHandlerContext       _ctx = null;
    
    
    public RequestEx( HttpRequest req
                    , Map<String, List<String>> params
                    , ChannelHandlerContext ctx )
    {
        _httpRq = req;
        _params = params;
        _ctx = ctx;
    }
    
    public Channel channel()
    {
        return _ctx.channel();
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
        List<String> h = this.getHeaderValues(name);
        return h == null || h.isEmpty() ? null : h.get(0);
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
        return _httpRq.method();
    }

    @Override
    public HttpRequest setMethod(HttpMethod method)
    {
        return _httpRq.setMethod(method);
    }

    @Override
    public String getUri()
    {
        return _httpRq.uri();
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
        List<String> pList = _params.get(paramName);
        
        return pList == null ? null : pList.get(0);
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
        return _httpRq.uri();
    }
}
