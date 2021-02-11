package com.tool4us.net.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpVersion.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;



public class TomyServerHandler extends SimpleChannelInboundHandler<Object>
{
    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;
    
    /**
     * Request Handler를 생성하기 위한 멤버
     */
    private final RequestHandlerFactory     _requestFac;
    
    /**
     * 생성된 Request Handler 관리 멤버.
     * 이미 생성된 것은 여기 것을 이용하고 없다면 _requestFac을 이용하여 만듦.
     */
    private Map<String, IRequestHandler>     _reqMap = null;
    
    /**
     * 최근 요청된 Request 객체
     */
    private HttpRequest     _request = null;
    
    private HttpPostRequestDecoder      _postDecoder = null;
    
    private IStaticFileHandler          _vPath = null;
    
    
    public TomyServerHandler(RequestHandlerFactory reqFac)
    {
        this(reqFac, null);
    }
    
    public TomyServerHandler(RequestHandlerFactory reqFac, IStaticFileHandler vPathMap)
    {
        _requestFac = reqFac;
        _reqMap = new ConcurrentSkipListMap<String, IRequestHandler>();
        
        _vPath = vPathMap;
    }
    
    private IRequestHandler getHandler(String uri)
    {
        IRequestHandler reqHandle = _reqMap.get(uri);
        if( reqHandle == null )
        {
            try
            {
                reqHandle = _requestFac.getRquestClazz(uri);
                if( reqHandle != null )
                    _reqMap.put(uri, reqHandle);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

        return reqHandle;
    }
    
    /*
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg)
    {
        System.out.println("channelRead0");

        final FullHttpResponse response = new NetOnResponse();
        // new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

        response.headers().set("custom-response-header", "Some value");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    } // */
    
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx)
    {
        ctx.flush();
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg)
    {
        if( msg instanceof HttpRequest )
        {
            _request = (HttpRequest) msg;

            if( _request.method() == HttpMethod.POST )
            {
                _postDecoder = new HttpPostRequestDecoder(_request);
            }
            else if( _request.method() == HttpMethod.OPTIONS )
            {
                //
            }
            else
                _postDecoder = null;
        }
        
        if( msg instanceof HttpContent )
        {
            if( _postDecoder != null )
                _postDecoder.offer((HttpContent) msg);
        
            if( msg instanceof LastHttpContent )
            {
                // Parameter Map 생성
                String uriPath = null;
                Map<String, List<String>> params = null;

                if( _request.method() == HttpMethod.GET )
                {
                    QueryStringDecoder gDecoder = new QueryStringDecoder(_request.uri());
                    params = gDecoder.parameters();
                    uriPath = gDecoder.path();
                }
                // POST라고 가정
                else if( _postDecoder != null )
                {
                    uriPath = _request.uri();
                    params = new TreeMap<String, List<String>>();
                    
                    List<InterfaceHttpData> paramList = _postDecoder.getBodyHttpDatas();
                    for(InterfaceHttpData data : paramList)
                    {
                        List<String> paramVal = new ArrayList<String>();
                        try
                        {
                            paramVal.add( ((Attribute)data).getValue() );
                        }
                        catch( Exception e )
                        {
                            //
                        }

                        params.put(data.getName(), paramVal);
                    }
                }
                else
                {
                    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
                    return;
                }
                
                IRequestHandler reqHandle = getHandler(uriPath);

                if( reqHandle != null )
                {
                    RequestEx reqEx = new RequestEx(_request, params, ctx);
                    ResponseEx resEx = new ResponseEx();
                    
                    resEx.headers().set(_request.headers());
                    
                    try
                    {
                        // resEx.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
                        resEx.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
                        resEx.setResultContent( reqHandle.call(reqEx, resEx) );
                        writeResponse(resEx, ctx);
                    }
                    catch( Exception e )
                    {
                        e.printStackTrace();
                        sendError(ctx, HttpResponseStatus.NOT_FOUND);
                    }
                }
                else if( _vPath != null )
                {
                    if( "/".equals(uriPath) )
                    {
                        uriPath = "/index.html";
                    }
                    
                    File sf = _vPath.getFile(uriPath);
                    
                    if( sf.exists() && !uriPath.endsWith(".map") )
                    {
                        try
                        {
                            sendFile(_request, ctx, sf);
                        }
                        catch( Exception xe )
                        {
                            sendError(ctx, HttpResponseStatus.FORBIDDEN);
                        }
                    }
                    else
                        sendError(ctx, HttpResponseStatus.FORBIDDEN);
                }
                else
                    sendError(ctx, HttpResponseStatus.NOT_FOUND);
            }
        }
    }

    private boolean writeResponse(FullHttpResponse response, ChannelHandlerContext ctx)
    {
        // Decide whether to close the connection or not.
        boolean keepAlive = HttpUtil.isKeepAlive(_request);

        if( keepAlive && response.content() != null )
        {
            // Add 'Content-Length' header only for a keep-alive connection.
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
            // Add keep alive header as per:
            // -
            // http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
            response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        
        // Encode the cookie.
        String cookieString = _request.headers().get(COOKIE);
        if( cookieString != null )
        {
            Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieString);
            if( !cookies.isEmpty() )
            {
                // Reset the cookies if necessary.
                for(Cookie cookie : cookies)
                {
                    response.headers().add(SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
                }
            }
        }

        // Write the response.
        ctx.write(response);
        
        return keepAlive;
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status)
    {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    private static void setDateAndCacheHeaders(HttpResponse response, File fileToCache)
    {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.headers().set(HttpHeaderNames.DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.headers().set(HttpHeaderNames.EXPIRES, dateFormatter.format(time.getTime()));
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.headers().set(HttpHeaderNames.LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }

    /**
     * Sets the content type header for the HTTP Response
     *
     * @param response
     *            HTTP response
     * @param file
     *            file to extract content type
     */
    private static void setContentTypeHeader(HttpResponse response, File file)
    {
        String path = file.getPath().toLowerCase();
        String mimeType = "application/octet-stream";
        
        if( path.endsWith(".png") ) {
            mimeType = "image/png";
        } else if( path.endsWith(".ico") ) {
            mimeType = "image/ico";
        } else if( path.endsWith(".html") || path.endsWith(".htm") ) {
            mimeType = "text/html"; // ";charset=UTF-8"
        } else if( path.endsWith(".js") ) {
            mimeType = "text/javascript"; // ";charset=UTF-8"
        } else if( path.endsWith(".css") ) {
            mimeType = "text/css"; // ";charset=UTF-8"
        } else if( path.endsWith(".txt") ) {
            mimeType = "text/plain"; // ";charset=UTF-8"
        } else if( path.endsWith(".mp4") ) {
            mimeType = "video/mp4"; // ";charset=UTF-8"
        }

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeType);
    }
    
    private static void sendFile(HttpRequest request, ChannelHandlerContext ctx, File file) throws Exception
    {
        boolean keepAlive = HttpUtil.isKeepAlive(request);

        RandomAccessFile raf;
        
        try
        {
            raf = new RandomAccessFile(file, "r");
        }
        catch( FileNotFoundException ignore )
        {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }
        
        long fileLength = raf.length();

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);

        HttpUtil.setContentLength(response, fileLength);
        setContentTypeHeader(response, file);
        setDateAndCacheHeaders(response, file);

        if( !keepAlive )
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        else if( request.protocolVersion().equals(HTTP_1_0) )
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

        // Write the initial line and the header.
        ctx.write(response);

        // Write the content.
        ChannelFuture sendFileFuture;
        ChannelFuture lastContentFuture;
        
        if( ctx.pipeline().get(SslHandler.class) == null )
        {
            sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());

            // Write the end marker.
            lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        }
        else
        {
            sendFileFuture = ctx.writeAndFlush(
                new HttpChunkedInput(new ChunkedFile(raf, 0, fileLength, 8192)), ctx.newProgressivePromise());

            // HttpChunkedInput will write the end marker (LastHttpContent) for us.
            lastContentFuture = sendFileFuture;
        }

        /* // Progress Check
        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total)
            {
                if( total < 0 ) // total unknown
                    System.err.println(future.channel() + " Transfer progress: " + progress);
                else
                    System.err.println(future.channel() + " Transfer progress: " + progress + " / " + total);
            }

            @Override
            public void operationComplete(ChannelProgressiveFuture future)
            {
                System.err.println(future.channel() + " Transfer complete.");
            }
        }); // */

        // Decide whether to close the connection or not.
        if( !keepAlive)
        {
            // Close the connection when the whole content is written out.
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
        cause.printStackTrace();
        ctx.close();
    }
}
