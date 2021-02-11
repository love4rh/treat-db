package com.tool4us.net.http;

import com.tool4us.common.Logs;
import com.tool4us.net.LoggingHandlerEx;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.stream.ChunkedWriteHandler;



/**
 * Simple WAS and HTTP Server.
 * 
 * @author TurboK
 */
public class TomyServer
{
    private int 				_port = 8080;

    private EventLoopGroup 		_bossGroup = null;
    private EventLoopGroup 		_workerGroup = null;
    private ServerBootstrap 	_bootstrap = null;
    private ChannelFuture       _bootChannel = null;
    
    private RequestHandlerFactory	_handlerFactory = null;
    
    /** 로그 남길지 여부. 0: 안 남김, 1: 호출된 함수 목록만 남김, 9: 주고 받은 데이터도 남기기 */
    private int                 _loggingType = 0;
    
    private IStaticFileHandler  _vRoot = null;

    
    public TomyServer(String handlerPackage)
    {
        this(handlerPackage, null);
    }
    
    /**
     * @param handlerPackage    요청에 대한 핸들러가 모여 있는 패키지 이름
     */
    public TomyServer(String handlerPackage, IStaticFileHandler vRoot)
    {
        // TODO CHECK NetOnSetting 호출 필요?
        _handlerFactory = new RequestHandlerFactory(handlerPackage);
        _vRoot = vRoot;
    }
    
    public boolean isRunning()
    {
    	return _bossGroup != null;
    }

    /**
     * 서버 실행.
     * 
     * @param port          	포트번호
     * @param bossThreadNum 	Listening Thread 개수
     * @param workThreadNum 	작업 Thread 개수
     * @throws Exception
     */
    public void start(int port, int bossThreadNum, int workThreadNum) throws Exception
    {
        this.start(port, bossThreadNum, workThreadNum, 0);
    }

    /**
     * 서버 실행.
     * 
     * @param port              포트번호
     * @param bossThreadNum     Listening Thread 개수
     * @param workThreadNum     작업 Thread 개수
     * @param loggingType       로그 남길지 여부. 0: 안 남김, 1: 호출된 함수 목록만 남김, 9: 주고 받은 데이터도 남기기
     * @throws Exception
     */
    public void start(int port, int bossThreadNum, int workThreadNum, int loggingType) throws Exception
    {
        final boolean SSL = System.getProperty("ssl") != null;

        final SslContext sslCtx;
        
        if( SSL )
        {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        }
        else
            sslCtx = null;
        
    	_port = port;
        
        // Configure the server.
    	_bossGroup = new NioEventLoopGroup(bossThreadNum);
    	_workerGroup = workThreadNum <= 0 ? new NioEventLoopGroup()
    									  : new NioEventLoopGroup(workThreadNum);

        try
        {
            _bootstrap = new ServerBootstrap();

            _bootstrap.group(_bossGroup, _workerGroup)
	            .channel(NioServerSocketChannel.class);
            
            if( loggingType > 0 )
            {
                _loggingType = loggingType;
                _bootstrap.handler( new LoggingHandlerEx(Logs.DEBUG, _loggingType >= 9) );
            }

            _bootstrap.option(ChannelOption.SO_BACKLOG, 100)
	            // .option(ChannelOption.SO_KEEPALIVE, true)
	            // .option(ChannelOption.ALLOCATOR, new PooledByteBufAllocator())
	            .childHandler( new HttpServerInitializer(_handlerFactory, sslCtx, loggingType, _vRoot) );
            
            // Start the server.
            _bootChannel = _bootstrap.bind(_port).sync(); // .channel().closeFuture().sync();
        }
        catch( Exception e )
        {
        	shutdown();
        	throw e;
        }
    }
    
    // Shut down all event loops to terminate all threads.
    public void shutdown()
    {
    	if( _bossGroup == null )
    		return;

    	_bossGroup.shutdownGracefully();
        _workerGroup.shutdownGracefully();
        
        _bossGroup = null;
        _workerGroup = null;
        _bootChannel = null;
        _bootstrap = null;
    }
    
    /**
     * 서버가 끝날 때까지 대기하는 메소드
     * 
     * @throws Exception
     */
    public void sync() throws Exception
    {
        if( _bootChannel == null )
            return;
        
        _bootChannel.channel().closeFuture().sync();
    }
    
    public RequestHandlerFactory getHandlerFactory()
    {
        return _handlerFactory;
    }
}


/**
 * netty example에서 가져옴.
 * https://github.com/netty/netty/blob/master/example/src/main/java/io/netty/example/http/upload/HttpUploadServerInitializer.java
 */
class HttpServerInitializer extends ChannelInitializer<SocketChannel>
{
    private final SslContext                _sslCtx;
    private final RequestHandlerFactory     _requestFac;
    private IStaticFileHandler      _vRoot;
    
    private int     _loggingType = 0;

    
    public HttpServerInitializer(RequestHandlerFactory reqFac, SslContext sslCtx, int loggingType)
    {
        this(reqFac, sslCtx, loggingType, null);
    }
    
    public HttpServerInitializer(RequestHandlerFactory reqFac, SslContext sslCtx, int loggingType, IStaticFileHandler vRoot)
    {
        _sslCtx = sslCtx;
        _requestFac = reqFac;
        _loggingType = loggingType;
        _vRoot = vRoot;
    }

    @Override
    public void initChannel(SocketChannel ch)
    {
        ChannelPipeline pl = ch.pipeline();

        if( _sslCtx != null )
        {
            pl.addLast( _sslCtx.newHandler(ch.alloc()) );
        }
        
        if( _loggingType > 0 )
        {
            pl.addLast( new LoggingHandlerEx(Logs.DEBUG, _loggingType >= 9) );
        }
        
        CorsConfig corsConfig = CorsConfigBuilder
            .forAnyOrigin()
            .allowNullOrigin()
            .allowCredentials()
            .allowedRequestMethods(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.OPTIONS)
            //.allowedRequestHeaders("x-auth-code", "Content-Type")
            //.preflightResponseHeader("Access-Control-Allow-Headers", "Access-Control-Allow-Origin")
            .build();

        pl.addLast(new HttpServerCodec())
          .addLast(new HttpObjectAggregator(65536))
          .addLast(new ChunkedWriteHandler())
          .addLast(new CorsHandler(corsConfig))
          .addLast(new TomyServerHandler(_requestFac, _vRoot))
        ;
    }
}
