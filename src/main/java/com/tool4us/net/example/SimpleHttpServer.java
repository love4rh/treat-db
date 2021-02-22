package com.tool4us.net.example;

import java.io.File;

import com.tool4us.common.Logs;
import com.tool4us.net.http.IStaticFileMap;
import com.tool4us.net.http.TomyServer;



/**
 * TomyServer 클래스를 이용한 간단한 WAS & HTTP 서버 예제.
 * 핸들러는 com.tool4us.net.example.handler 패키지 내 정의되어 있으며,
 * 핸들러 생성 방법은 HelloWorldHandler 클래스 참고
 * 
 * @author TurboK
 */
public class SimpleHttpServer implements IStaticFileMap
{
    private TomyServer      _serverBase = null;

    
    public SimpleHttpServer()
    {
        //
    }
    
    public void start(int port) throws Exception
    {
        if( _serverBase != null )
            return;
        
        // com.tool4us.net.example.handler: 핸들러가 정의되어 있는 패키지.
        // Static File을 제공하기 위한 매니저(여기서는 this) 전달.
        _serverBase = new TomyServer("com.tool4us.net.example.handler", this);
        
        // Boss Thread와 Working Thread 개수를 서버 사양에 따라 조절
        _serverBase.start(port, 1, 2);
    }
    
    public void stop()
    {
        if( _serverBase != null )
        {
            _serverBase.shutdown();
            _serverBase = null;
        }
    }
    
    @Override
    public String getRootFile()
    {
        return "index.html";
    }
    
    @Override
    public boolean isAllowed(String uriPath)
    {
        // 소스 보안을 위하여 *.map 파일은 반환하지 않음.
        return !uriPath.endsWith(".map");
    }

    @Override
    public File getFile(String uriPath)
    {
        return null;
    }
    
    
    public static void main(String[] args)
    {
        // 콘솔로 로그 남기기
        Logs.addConsoleLogger();

        SimpleHttpServer simpleServer = new SimpleHttpServer();
        
        try
        {
            simpleServer.start(8888);

            System.out.println("SERVER STARTED!");
        }
        catch(Exception xe)
        {
            Logs.trace(xe);
        }
    }
}
