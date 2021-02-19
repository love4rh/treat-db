package lib.turbok.hermes;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyMaxRollingFileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;


public class Logs
{
    public static final int DEBUG = 1;
    public static final int NORMAL = 2;
    public static final int WARNING = 3;
    public static final int ERROR = 4;
    public static final int FATAL = 5;
    
    // 로그에 남기는 항목을 구분하기 위한 구분자.
    public static final String  _sep_ = "`";
    
    /// Singleton 유일 객체
    private static Logs         _theOne = null;

    /// Main Logger 클래스
    private  Logger             _logger = null;

    
    private Logs()
    {
        //
    }
    
    public static String getModulePath()
    {
        File curDir = new File(".");
        
        String modulePath = "";
        
        try
        {
            modulePath = curDir.getCanonicalPath();
        }
        catch (IOException e)
        {
            //
        }
        
        return modulePath;
    }
    
    /**
     * 기본 설정으로 Logger 클래스를 생성하는 메소드.
     * NOTE: 기존에 설정되어 있는 Appender 클래스를 모두 제거함.
     * @param loggingFolder
     * @param progName
     * @param errorLogging     에러만 따로 남길 지 여부 TODO
     */
    public static void initDefault(String loggingFolder, String progName)
    {
        if( _theOne == null )
        {
            _theOne = new Logs();
            _theOne._logger = Logger.getLogger( "main" );
        }
        
        _theOne._logger.removeAllAppenders();
        
        String pattern = "[%d{MM-dd HH:mm:ss.SSS}] [%-5p] %m%n";
        Layout layout = new PatternLayout(pattern);

        String logDirStr = null;
        
        if( loggingFolder == null || loggingFolder.isEmpty() )
            logDirStr = getModulePath() + File.separator + "log";
        else
            logDirStr = loggingFolder;
        
        File logDir = new File(logDirStr);
        if( !logDir.exists() )
            logDir.mkdir();

        String fileName = logDirStr + File.separator + progName + ".log";

        try
        {
            DailyMaxRollingFileAppender rollingAppender
                    = new DailyMaxRollingFileAppender(layout, fileName, ".yyyy-MM-dd");

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
        String pattern = "[%d{MM-dd HH:mm:ss.SSS}] [%-5p] %m%n";
        Layout layout = new PatternLayout(pattern);

        _theOne._logger.addAppender(new ConsoleAppender(layout));
    }

    /**
     * Log4j 설정파일을 이용한 Logger 클래스 생성
     * @param logPropFile
     */
    public static void initWithFile(String logPropFile)
    {
        if( _theOne == null )
            _theOne = new Logs();
        
        BasicConfigurator.resetConfiguration();
        PropertyConfigurator.configure(logPropFile);
            
        _theOne._logger = Logger.getLogger( "main" );
    }

    public static Logs instance()
    {   
        if( _theOne == null )
        {
            _theOne = new Logs();
            _theOne._logger = Logger.getLogger( "main" );
        }

        return _theOne;
    }

    public Logger getLogger()
    {
        return _logger;
    }
    
    private String formatMsg(Object ... args)
    {
        if( args == null )
            return null;
        
        StringBuilder sb = new StringBuilder(128);
        
        // 쓰레드 정보 기록
        sb.append("[")
          .append(Thread.currentThread().getId())
          .append("]");

        sb.append("[");
        for(Object o : args)
        {
            sb.append(o == null ? "null" : o.toString())
              .append(_sep_);
        }
        sb.append("]");
        
        return sb.toString();
    }

    public void write(String message, int msgType)
    {
        _write(formatMsg(message), msgType);
    }
    
    private void _write(String message, int msgType)
    {
        Logger logWriter = getLogger();
        
        if( logWriter == null )
            return;
                
        switch( msgType )
        {
        case DEBUG:
            logWriter.debug(message);
            break;
        case ERROR:
            logWriter.error(message);
            break;
        case WARNING:
            logWriter.warn(message);
            break;
        case FATAL:
            logWriter.fatal(message);
            break;
        default:
            logWriter.info(message);
            break;
        }
    }
    
    public void write(int msgType, Object ... args)
    {
        _write(formatMsg(args), msgType);
    }
    
    public static void writeInfo(Object ... args)
    {
        instance().write(NORMAL, args);
    }
    
    public static void writeDebug(Object ... args)
    {
        instance().write(DEBUG, args);
    }

    public static void writeError(Object ... args)
    {
        instance().write(ERROR, args);
    }

    public static void writeWarning(Object ... args)
    {
        instance().write(WARNING, args);
    }
    
    public static void writeFatal(Object ... args)
    {
        instance().write(FATAL, args);
    }
    
    public static void writeTrace(Exception e)
    {
        StackTraceElement[] elem = e.getStackTrace();
        
        StringBuilder sb = new StringBuilder(elem.length * 64);
        
        sb.append(e)
          .append(" / ")
          .append(e.getMessage())
          ;
        
        for(int i = 0; i < elem.length; ++i)
        {
            sb.append(" << ")
              .append(elem[i]);
        }

        writeError(sb.toString());
    }
}
