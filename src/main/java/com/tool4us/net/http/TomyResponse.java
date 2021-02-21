package com.tool4us.net.http;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.CharsetUtil;

import static com.tool4us.common.Util.UT;
import static io.netty.buffer.Unpooled.*;



public class TomyResponse extends DefaultFullHttpResponse
{
    private ByteBuf         _content = null;
    private String          _retContent = "";

    
    public TomyResponse()
    {
        super(HTTP_1_1, OK);
    }
    
    public void setResultContent(String resultStr)
    {
        _retContent = resultStr;
        _content = copiedBuffer(resultStr, CharsetUtil.UTF_8);
    }
    
    @Override
    public ByteBuf content()
    {
        return _content;
    }
    
    @Override
    public int refCnt()
    {
        return _content.refCnt();
    }
    
    @Override
    public FullHttpResponse retain()
    {
        _content.retain();
        return this;
    }
    
    @Override
    public FullHttpResponse retain(int increment)
    {
        _content.retain(increment);
        return this;
    }
    
    @Override
    public boolean release()
    {
        return _content.release();
    }
    
    @Override
    public boolean release(int decrement)
    {
        return _content.release(decrement);
    }

    public String oneLineResponse(int limit)
    {
        return UT.makeEllipsis( UT.makeSingleLine(_retContent), limit );
    }
}
