package lib.turbok.hermes;

import java.io.File;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;



/**
 * 
 * @author TurboK
 */
public class HermesServer
{
    private EventLoopGroup      _bossGroup = null;
    private EventLoopGroup      _workerGroup = null;
    private ServerBootstrap     _bootstrap = null;
    private Channel             _bootChannel = null;
    
    
    public void start(int port, boolean SSL, boolean holdOn)
            throws CertificateException, SSLException, InterruptedException
    {
        final SslContext sslCtx;
        
        if( SSL )
        {
        	String appPath = Logs.getModulePath();
            sslCtx = SslContextBuilder.forServer(new File(appPath + File.separator + "hermes.pem")
            		, new File(appPath + File.separator + "hermes.key")).build();
        }
        else
        {
            sslCtx = null;
        }
        
        _bossGroup = new NioEventLoopGroup(1);
        _workerGroup = new NioEventLoopGroup();
        
        _bootstrap = new ServerBootstrap();
        _bootstrap.group(_bossGroup, _workerGroup)
            .channel(NioServerSocketChannel.class)
            // .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new HermesServerInitializer(sslCtx));
        
        _bootChannel = _bootstrap.bind(port).sync().channel();
        
        System.out.println("Open your web browser and navigate to " +
                           (SSL ? "https" : "http")
                           + "://127.0.0.1:"
                           + port
                           + '/');
        
        if( holdOn )
        {
            _bootChannel.closeFuture().sync();
        }
    }
    
    public void stop()
    {
        if( _workerGroup == null )
            return;

        if( _bossGroup != null )
            _bossGroup.shutdownGracefully();

        _workerGroup.shutdownGracefully();
        
        _bossGroup = null;
        _workerGroup = null;
        _bootChannel = null;
        _bootstrap = null;
    }
}
