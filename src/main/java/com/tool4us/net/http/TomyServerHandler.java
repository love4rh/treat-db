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
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
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
import java.nio.charset.Charset;
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

import org.json.JSONObject;

import com.tool4us.common.Logs;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpVersion.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;

import static com.tool4us.common.Util.UT;



public class TomyServerHandler extends SimpleChannelInboundHandler<FullHttpRequest>
{
    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;
    
    /**
     * Request Handler를 생성하기 위한 멤버
     */
    private final TomyApiHandlerFactory _requestFac;
    
    /**
     * 생성된 Request Handler 관리 멤버.
     * 이미 생성된 것은 여기 것을 이용하고 없다면 _requestFac을 이용하여 만듦.
     */
    private Map<String, IApiHanlder>    _reqMap = null;
    
    /**
     * 최근 요청된 Request 객체
     */
    private FullHttpRequest             _request = null;
    
    private IStaticFileMap              _vPath = null;
    
    
    public TomyServerHandler(TomyApiHandlerFactory reqFac)
    {
        this(reqFac, null);
    }
    
    public TomyServerHandler(TomyApiHandlerFactory reqFac, IStaticFileMap vPathMap)
    {
        _requestFac = reqFac;
        _reqMap = new ConcurrentSkipListMap<String, IApiHanlder>();
        
        _vPath = vPathMap;
    }
    
    private IApiHanlder getHandler(String uri)
    {
        IApiHanlder reqHandle = _reqMap.get(uri);
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

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx)
    {
        ctx.flush();
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg)
    {
        this._request = msg;
        
        if( !msg.decoderResult().isSuccess() )
        {
            sendError(ctx, BAD_REQUEST);
            return;
        }
        
        // TODO 다른 메소드 지원 여부 검토
        if( !GET.equals(msg.method()) && !POST.equals(msg.method()) )
        {
            sendError(ctx, METHOD_NOT_ALLOWED);
            return;
        }

        final String uri = msg.uri();
        // final String path = 
        
        String uriPath = uri;
        TomyRequestor reqEx = null;

        if( GET.equals(msg.method()) )
        {
            QueryStringDecoder gDecoder = new QueryStringDecoder(uri);

            uriPath = gDecoder.path();
            reqEx = new TomyRequestor(_request, ctx, uriPath, gDecoder.parameters()); 
        }
        else if( POST.equals(msg.method()) )
        {
            String contentType = null;
            
            if( msg.headers().contains(HttpHeaderNames.CONTENT_TYPE) )
            {
                contentType = HttpUtil.getMimeType(msg).toString();
            }

            Logs.debug("MimeType: {}", contentType);
            
            if( "application/x-www-form-urlencoded".equals(contentType) )
            {
                Map<String, List<String>> params = new TreeMap<String, List<String>>();
                
                HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(msg);
                List<InterfaceHttpData> paramList = postDecoder.getBodyHttpDatas();

                for(InterfaceHttpData data : paramList)
                {
                    List<String> paramVal = new ArrayList<String>();
                    try
                    {
                        paramVal.add( ((Attribute)data).getValue() );
                    }
                    catch( Exception xe )
                    {
                        //
                    }
    
                    params.put(data.getName(), paramVal);
                }
                
                reqEx = new TomyRequestor(_request, ctx, uriPath, params);
            }
            else if( "application/json".equals(contentType) )
            {
                String bodyData = msg.content().toString(Charset.forName("UTF-8"));
                JSONObject jsonData = UT.parseJSON(bodyData);
                
                if( jsonData == null )
                {
                    sendError(ctx, HttpResponseStatus.FORBIDDEN);
                    return;
                }
                else
                    reqEx = new TomyRequestor(_request, ctx, uriPath, bodyData, jsonData);
            }
            else if( "text/plain".equals(contentType) )
            {
                String bodyData = msg.content().toString(Charset.forName("UTF-8"));
                reqEx = new TomyRequestor(_request, ctx, uriPath, bodyData);
            }
            else
            {
                sendError(ctx, HttpResponseStatus.FORBIDDEN);
                return;
            }
        }

        IApiHanlder reqHandle = getHandler(uriPath);

        if( reqHandle != null )
        {
            TomyResponse resEx = new TomyResponse();

            resEx.headers().set(_request.headers());
            
            try
            {
                // TODO Common Filter
                
                // resEx.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
                resEx.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
                resEx.setResultContent( reqHandle.call(reqEx, resEx) );

                sendResponse(resEx, ctx);
            }
            catch( Exception xe )
            {
                Logs.trace(xe);
                sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            }
        }
        else if( _vPath != null )
        {
            if( "/".equals(uriPath) )
            {
                uriPath += _vPath.getRootFile();
            }
            
            File sf = _vPath.getFile(uriPath);
            
            if( sf != null && sf.exists() && _vPath.isAllowed(uriPath) )
            {
                try
                {
                    sendFile(_request, ctx, sf);
                }
                catch( Exception xe )
                {
                    sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                }
            }
            else
                sendError(ctx, HttpResponseStatus.FORBIDDEN);
        }
        else
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
    }

    private void sendResponse(FullHttpResponse response, ChannelHandlerContext ctx)
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
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status)
    {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8)
        );

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
     * @see https://developer.mozilla.org/ko/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
     * @param response  HTTP response
     * @param file      file to extract content type
     */
    private static void setContentTypeHeader(HttpResponse response, File file)
    {
        String path = file.getPath().toLowerCase();
        String mimeType = UT.getMimeType( UT.getExtension(path) );
        
        // TODO ";charset=UTF-8" ??

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeType);
    }
    
    /**
     * If Keep-Alive is disabled, attaches "Connection: close" header to the response
     * and closes the connection after the response being sent.
     */
    private static void sendAndCleanupConnection(final FullHttpRequest request, ChannelHandlerContext ctx, FullHttpResponse response)
    {
        final boolean keepAlive = HttpUtil.isKeepAlive(request);
        HttpUtil.setContentLength(response, response.content().readableBytes());
        if( !keepAlive )
        {
            // We're going to close the connection as soon as the response is sent,
            // so we should also make it clear for the client.
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        } else if (request.protocolVersion().equals(HTTP_1_0)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        ChannelFuture flushPromise = ctx.writeAndFlush(response);

        if (!keepAlive) {
            // Close the connection as soon as the response is sent.
            flushPromise.addListener(ChannelFutureListener.CLOSE);
        }
    }
    
    private static void sendFile(FullHttpRequest request, ChannelHandlerContext ctx, File file) throws Exception
    {
        final boolean keepAlive = HttpUtil.isKeepAlive(request);
        
        // Cache Validation
        String ifModifiedSince = request.headers().get(HttpHeaderNames.IF_MODIFIED_SINCE);
        if( ifModifiedSince != null && !ifModifiedSince.isEmpty() )
        {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
            Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);

            // Only compare up to the second because the datetime format we send to the client
            // does not have milliseconds
            long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
            long fileLastModifiedSeconds = file.lastModified() / 1000;
            if( ifModifiedSinceDateSeconds == fileLastModifiedSeconds )
            {
                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED, Unpooled.EMPTY_BUFFER);

                dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

                Calendar time = new GregorianCalendar();
                response.headers().set(HttpHeaderNames.DATE, dateFormatter.format(time.getTime()));

                sendAndCleanupConnection(request, ctx, response);
                return;
            }
        }

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
