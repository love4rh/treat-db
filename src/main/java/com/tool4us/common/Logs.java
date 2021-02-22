package com.tool4us.common;

import java.io.File;
import java.io.IOException;

import lib.turbok.common.ILogging;
import lib.turbok.util.UsefulTool;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyMaxRollingFileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;



/**
 * 로깅을 위한 클래스. Log4J를 이용하여 간단히 로그를 남길 수 있도록 구현한 클래스임.
 * 각 클래스별로 logger를 새로 받아 할 필요성을 못느껴 개발하였음.(굳이 클래스명을 오픈할 필요는 없다고 생각함)
 * 기본적으로 파일에 로그를 남기며, Console에 로그를 남기려면 addConsoleLogger() 호출. 호출할 때마다 Appender가 추가되므로 한 번만 호출.
 * 파일에 남길 로그를 지정하기 위하여 initialize() 호출해야 함.
 * 
 * @author TurboK
 */
public class Logs extends ILogging
{
    private static final String _defaultPattern_ = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%-5p][%-16t] %m%n";
    
    // TODO: 코드 안정화가 된 이후에는 false로 세팅하여 성능 최적화 되도록 해야함.
	public static final boolean _isDebug = true;
	
	// 로그에 남기는 항목을 구분하기 위한 구분자.
	public static final String  _sep_ = "|";
	
	/// Singleton 유일 객체
	private static final Logs   _theOne;

	/// Main Logger 클래스
	private  Logger		        _logger = null;
	
	
	static
	{
	    _theOne = new Logs();
        _theOne._logger = Logger.getLogger( "main" );
	}

	
	/// private constructor
	private Logs() { }
	
	public static void initialize(String loggingFolder, String progName)
	{
	    initialize(loggingFolder, progName, "yyyy-MM-dd");
	}
	
	/**
	 * 기본 설정으로 Logger 클래스를 생성하는 메소드.
	 * NOTE: 기존에 설정되어 있는 Appender 클래스를 모두 제거함.
	 * @param loggingFolder    로그를 저장할 파일의 경로
	 * @param progName         로그 파일의 prefix
	 * @param rollingType      롤링할 기준. null이면 일단위로 생성. 월단위: yyyy-MM, 주단위: yyyy-ww, 일단위: yyyy-MM-dd,
	 *                         반나절 단위: yyyy-MM-dd-a, 시간단위: yyyy-MM-dd-HH, 분단위: yyyy-MM-dd-HH-mm
	 * @param errorLogging     에러만 따로 남길 지 여부 TODO
	 */
	public static void initialize(String loggingFolder, String progName, String rollingType)
	{   
	    _theOne._logger.removeAllAppenders();
	    
	    if( rollingType == null )
	        rollingType = "yyyy-MM-dd";
	            
        Layout layout = new PatternLayout(_defaultPattern_);

        String logDirStr = null;
        
        if( loggingFolder == null || loggingFolder.isEmpty() )
            logDirStr = UsefulTool.GetModulePath() + File.separator + "log";
        else
            logDirStr = loggingFolder;
        
        File logDir = new File(logDirStr);

        if( !logDir.exists() )
            logDir.mkdir();

        String fileName = logDirStr + File.separator + progName + ".log";

        try
        {
            DailyMaxRollingFileAppender rollingAppender
                    = new DailyMaxRollingFileAppender(layout, fileName, "." + rollingType);

            rollingAppender.setMaxBackupIndex(15);
            
            _theOne._logger.addAppender(rollingAppender);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
	}
	
	public static void addConsoleLogger()
	{
	    _theOne._logger.addAppender(new ConsoleAppender( new PatternLayout(_defaultPattern_) ));
	}

	/**
	 * Log4j 설정파일을 이용한 Logger 클래스 생성
	 * @param logPropFile
	 */
	public static void initWithFile(String logPropFile)
	{
        BasicConfigurator.resetConfiguration();
        PropertyConfigurator.configure(logPropFile);
            
        _theOne._logger = Logger.getLogger( "main" );
	}

	public static Logs instance()
	{
	    return _theOne;
	}

	public Logger getLogger()
	{
		return _logger;
	}
	
	private String formatMsg(Object ... args)
    {
        if( args == null || args.length == 0 )
        {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();

        for(Object o : args)
            sb.append(o == null ? "" : o.toString()).append(_sep_);

        return sb.toString();
    }
	
	private String formatMsg(String format, Object ... args)
    {
	    if( format == null || format.isEmpty())
	        return "";

	    if( args == null || args.length == 0 )
	        return format;

	    StringBuilder sb = new StringBuilder();

	    int argIdx = 0;
	    int len = format.length();

	    for(int i = 0; i < len; ++i)
	    {
	        char c = format.charAt(i);
	        
	        // parameter found
	        if( c == '{' && i + 1 < len && format.charAt(i + 1) == '}' && argIdx < args.length )
	        {
	            sb.append(args[argIdx] == null ? "" : args[argIdx].toString());
	            argIdx += 1;
	            i += 1;
	        }
	        else
	        {
	            sb.append(c);
	        }
	    }

        return sb.toString();
    }

	private void _write(int msgType, String message)
	{
		Logger logWriter = getLogger();
        
        if( logWriter == null )
            return;
                
        switch( msgType )
        {
        case ILogging.DEBUG:
            logWriter.debug(message);
            break;
        case ILogging.ERROR:
            logWriter.error(message);
            break;
        case ILogging.WARNING:
            logWriter.warn(message);
            break;
        case ILogging.FATAL:
            logWriter.fatal(message);
            break;
        default:
            logWriter.info(message);
            break;
        }
	}

    @Override
    public void write(String msg, int msgType)
    {
        write(msgType, msg);
    }

	public void write(int msgType, Object ... args)
    {
        _write(msgType, formatMsg(args));
    }
	
	public void write(int msgType, String format, Object ... args)
    {
        _write(msgType, formatMsg(format, args));
    }
	
	public static void raw(int msgType, Object ... args)
    {
        instance().write(msgType, args);
    }
	
	public static void raw(int msgType, String format, Object ... args)
    {
        instance().write(msgType, format, args);
    }
	
	public static void debug(Object ... args)
    {
        instance().write(ILogging.DEBUG, args);
    }
	
	public static void info(Object ... args)
    {
		instance().write(ILogging.NORMAL, args);
    }
    
    public static void warn(Object ... args)
    {
        instance().write(ILogging.WARNING, args);
    }
    
    public static void error(Object ... args)
    {
        instance().write(ILogging.ERROR, args);
    }
    
    public static void fatal(Object ... args)
    {
        instance().write(ILogging.FATAL, args);
    }
    
    public static void debug(String format, Object ... args)
    {
        instance().write(ILogging.DEBUG, format, args);
    }
    
    public static void info(String format, Object ... args)
    {
        instance().write(ILogging.NORMAL, format, args);
    }
    
    public static void warn(String format, Object ... args)
    {
        instance().write(ILogging.WARNING, format, args);
    }
    
    public static void error(String format, Object ... args)
    {
        instance().write(ILogging.ERROR, format, args);
    }
    
    public static void fatal(String format, Object ... args)
    {
        instance().write(ILogging.FATAL, format, args);
    }

	public static void trace(Exception xe)
	{
	    StackTraceElement[] elem = xe.getStackTrace();
        
        StringBuilder sb = new StringBuilder(elem.length * 64);
        
        sb.append(xe)
          .append(" / ")
          .append(xe.getMessage())
          ;
        
        for(int i = 0; i < elem.length; ++i)
        {
            sb.append(" << ")
              .append(elem[i]);
        }

        warn(sb.toString());
	}
}
