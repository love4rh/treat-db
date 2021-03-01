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
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.EndOfDataDecoderException;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentSkipListMap;

import com.tool4us.common.Logs;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpVersion.*;

import static com.tool4us.common.Util.UT;



/**
 * HTTP 데이터 처리를 위한 핸들러. 다음 netty example을 참조하여 작성함.
 * @see https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/http/file
 * @see https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/http/upload
 * 
 * @author TurboK
 */
public class TomyServerHandler extends SimpleChannelInboundHandler<HttpObject>
{
    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;
    
    private static final HttpDataFactory _factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);
    
    /**
     * Request Handler를 생성하기 위한 멤버
     */
    private final TomyApiHandlerFactory _requestFac;
    
    /**
     * 생성된 Request Handler 관리 멤버.
     * 이미 생성된 것은 여기 것을 이용하고 없다면 _requestFac을 이용하여 만듦.
     */
    private Map<String, ApiHandler>    _reqMap = null;
    
    private IStaticFileMap              _vPath = null;
    
    /**
     * 상세로깅 여부. 이 값이 true라면, 헤더, 파라미터, 바디 및 반환 결과 등을 모두 남김.
     * false이면 기본 호출 이력만 남김.
     */
    private boolean                     _detailLogging = true;
    
    /**
     * 최근 요청된 Request 객체
     */
    private TomyRequestor               _request = null;
    
    private HttpPostRequestDecoder      _decoder = null;
    
    private HttpData                    _partialContent = null;
    
    
    static
    {
        DiskFileUpload.deleteOnExitTemporaryFile = true; // should delete file
                                                         // on exit (in normal
                                                         // exit)
        DiskFileUpload.baseDirectory = null; // system temp directory
        DiskAttribute.deleteOnExitTemporaryFile = true; // should delete file on
                                                        // exit (in normal exit)
        DiskAttribute.baseDirectory = null; // system temp directory
    }
    
    
    public TomyServerHandler(TomyApiHandlerFactory reqFac)
    {
        this(reqFac, null);
    }
    
    public TomyServerHandler(TomyApiHandlerFactory reqFac, IStaticFileMap vPathMap)
    {
        _requestFac = reqFac;
        _reqMap = new ConcurrentSkipListMap<String, ApiHandler>();
        
        _vPath = vPathMap;
    }
    
    private ApiHandler getHandler(String uri)
    {
        ApiHandler reqHandle = _reqMap.get(uri);
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
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        if( _decoder != null )
        {
            _decoder.cleanFiles();
        }
        super.channelInactive(ctx);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
    {
        super.channelReadComplete(ctx);
        ctx.flush();
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg)
    {
        if( msg instanceof HttpRequest )
        {
            HttpRequest request = (HttpRequest) msg;
            
            _request = new TomyRequestor(request, ctx);
            
            String uriPath = request.uri();
            QueryStringDecoder gDecoder = new QueryStringDecoder(uriPath);
            
            uriPath = gDecoder.path();
            _request.initialize(uriPath, gDecoder.parameters());
            
            Logs.info("[{}:REQ] [{}] [IP:{}] [URI:{}]", _request.getTraceID(), request.method().name(), _request.getRemoteDescription(), uriPath);

            if( !GET.equals(request.method()) )
            {
                _decoder = new HttpPostRequestDecoder(_factory, request);
            }
        }
        
        if( msg instanceof HttpContent )
        {
            HttpContent httpContent = (HttpContent) msg;
            
            // 파일 보내는 경우를 제외하고 Body Data를 저장하기 위하여 비
            if( !"multipart/form-data".equals(_request.getMimeType()) )
            {
                // TODO 문자열 중간에서 짤린 경우라면 이렇게 넣으면 안됨...
                _request.putBodyData(httpContent.content().toString(CharsetUtil.UTF_8));
            }
            
            if( _decoder != null )
            {
                try
                {
                    _decoder.offer(httpContent);
                    readHttpDataChunkByChunk();
                }
                catch( Exception xe )
                {
                    // TODO send error?
                    Logs.trace(xe);
                }
            }
            
            if( httpContent instanceof LastHttpContent )
            {                
                procResponse(ctx);   
                reset();
            }
        }
    }
    
    private void reset()
    {
        _request = null;

        // destroy the decoder to release all resources
        if( _decoder != null )
        {
            _decoder.destroy();
            _decoder = null;
        }
    }
    
    private void readHttpDataChunkByChunk() throws Exception
    {
        try
        {
            while( _decoder.hasNext() )
            {
                InterfaceHttpData data = _decoder.next();
                if( data != null )
                {
                    // check if current HttpData is a FileUpload and previously set as partial
                    if( _partialContent == data )
                    {
                        _partialContent = null;
                    }
                    
                    // new value
                    writeHttpData(data);
                }
            }
        }
        catch( EndOfDataDecoderException xe )
        {
            // end of data. exception should be ignored.
        }
        
        // Check partial decoding for a FileUpload
        InterfaceHttpData data = _decoder.currentPartialHttpData();
        if( data != null && _partialContent == null )
        {
            _partialContent = (HttpData) data;
        }
    }
    
    private void writeHttpData(InterfaceHttpData data) throws Exception
    {
        switch( data.getHttpDataType() )
        {
        case Attribute:
            {
                Attribute attribute = (Attribute) data;
                _request.putParameter(attribute.getName(), attribute.getValue());
            } break;
        case FileUpload:
            {
                FileUpload fileUpload = (FileUpload) data;
                if( fileUpload.isCompleted() )
                {
                    _request.putParameter(data.getName(), fileUpload);
                    // fileUpload.length()
                    // fileUpload.getString(fileUpload.getCharset());
                }
            } break;
        
        default:
            break;
        }
    }

    private void procResponse(ChannelHandlerContext ctx)
    {
        TomyRequestor reqEx = this._request;
        
        String uriPath = reqEx.uri();
        String traceID = reqEx.getTraceID();
        long sTick = reqEx.getRequestedTime(); // 요청시간
        long mTick = UT.tickCount(); // 요청 데이터 수령 완료 시간
        long pTick = mTick; // 요청 처리 완료 시간

        final int lenLimit = 96;
        if( _detailLogging )
        {
            Logs.info("[{}:DTL] [IP:{}] [URI:{}] [HAEDER:{}] [PARAM:{}] [BODY:{}]"
                , traceID
                , reqEx.getRemoteDescription()
                , reqEx.uri()
                , reqEx.oneLineHeader(lenLimit)
                , reqEx.oneLineParameter(lenLimit)
                , reqEx.oneLineBody(1024)
            );
        }

        TomyResponse resEx = null;
        HttpResponseStatus retStatus = HttpResponseStatus.OK;
        ApiHandler reqHandle = getHandler(uriPath);

        if( reqHandle != null )
        {
            resEx = new TomyResponse();
            resEx.headers().set(_request.headers());
            
            try
            {
                // TODO Common Filter
                
                // resEx.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
                resEx.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
                
                String retContent = reqHandle.call(reqEx, resEx);
                resEx.setResultContent( retContent );
                pTick = UT.tickCount();

                sendResponse(ctx, this._request, resEx);
            }
            catch( Exception xe )
            {
                Logs.trace(xe);
                retStatus = HttpResponseStatus.INTERNAL_SERVER_ERROR;
            }
        }
        else if( _vPath != null )
        {
            if( "/".equals(uriPath) )
            {
                uriPath += _vPath.getRootFile();
            }
            
            File staticFile = _vPath.getFile(uriPath);
            
            pTick = UT.tickCount();
            
            if( staticFile != null && staticFile.exists() && _vPath.isAllowed(uriPath) )
            {
                try
                {
                    sendFile(reqEx.getHttpRequest(), ctx, staticFile);
                }
                catch( Exception xe )
                {
                    retStatus = HttpResponseStatus.INTERNAL_SERVER_ERROR;
                }
            }
            else
                retStatus = HttpResponseStatus.NOT_FOUND;
        }
        else
            retStatus = HttpResponseStatus.NOT_FOUND;
        

        String retContent = "";
        if( retStatus != HttpResponseStatus.OK )
        {
            sendError(ctx, retStatus);
        }
        else if( resEx != null )
        {
            retStatus = resEx.status();
            retContent = resEx.oneLineResponse(1024);
        }
        
        long eTick = UT.tickCount();

        if( _detailLogging && UT.isValidString(retContent) )
        {
            Logs.info("[{}:RES] [PTIME:{}, {}, {}, {}] [STATUS:{}] [CONTENT:{}]"
                , traceID
                , mTick - sTick, pTick - mTick, eTick - pTick, eTick - sTick
                , retStatus.code()
                , retContent);
        }
        else
        {
            Logs.info("[{}:RES] PTIME:[{}, {}, {}, {}] [STATUS:{}]"
                , traceID
                , mTick - sTick, pTick - mTick, eTick - pTick, eTick - sTick
                , retStatus.code());
        }
    }

    private static void sendResponse(ChannelHandlerContext ctx, TomyRequestor request, TomyResponse response)
    {
        // Decide whether to close the connection or not.
        boolean keepAlive = HttpUtil.isKeepAlive(request);

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
        String cookieString = request.headers().get(COOKIE);
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
        ChannelFuture future = ctx.writeAndFlush(response);

        if( !keepAlive )
        {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status)
    {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HTTP_1_1, status, Unpooled.copiedBuffer(ApiHandler.makeResponseJson(status, null), CharsetUtil.UTF_8)
        );

        response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    private static void setDateAndCacheHeaders(HttpResponse response, File fileToCache)
    {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.headers().set(DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.headers().set(EXPIRES, dateFormatter.format(time.getTime()));
        response.headers().set(CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.headers().set(LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
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
        String mimeType = UT.getExtensionMimeType( UT.getExtension(path) );
        
        // TODO ";charset=UTF-8" ??

        response.headers().set(CONTENT_TYPE, mimeType);
    }
    
    /**
     * If Keep-Alive is disabled, attaches "Connection: close" header to the response
     * and closes the connection after the response being sent.
     */
    private static void sendAndCleanupConnection(final HttpRequest request, ChannelHandlerContext ctx, FullHttpResponse response)
    {
        final boolean keepAlive = HttpUtil.isKeepAlive(request);
        HttpUtil.setContentLength(response, response.content().readableBytes());
        if( !keepAlive )
        {
            // We're going to close the connection as soon as the response is sent,
            // so we should also make it clear for the client.
            response.headers().set(CONNECTION, HttpHeaderValues.CLOSE);
        } else if (request.protocolVersion().equals(HTTP_1_0)) {
            response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        ChannelFuture flushPromise = ctx.writeAndFlush(response);

        if (!keepAlive) {
            // Close the connection as soon as the response is sent.
            flushPromise.addListener(ChannelFutureListener.CLOSE);
        }
    }
    
    private static void sendFile(HttpRequest request, ChannelHandlerContext ctx, File file) throws Exception
    {
        final boolean keepAlive = HttpUtil.isKeepAlive(request);
        
        // Cache Validation
        String ifModifiedSince = request.headers().get(IF_MODIFIED_SINCE);
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
                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.NOT_MODIFIED, Unpooled.EMPTY_BUFFER);

                dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

                Calendar time = new GregorianCalendar();
                response.headers().set(DATE, dateFormatter.format(time.getTime()));

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

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK);

        HttpUtil.setContentLength(response, fileLength);

        setContentTypeHeader(response, file);
        setDateAndCacheHeaders(response, file);

        if( !keepAlive )
            response.headers().set(CONNECTION, HttpHeaderValues.CLOSE);
        else if( request.protocolVersion().equals(HTTP_1_0) )
            response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);

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
