package lib.turbok.hermes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


class AppMain
{
    // static final boolean SSL = System.getProperty("ssl") != null;
    // static final int PORT = Integer.parseInt(System.getProperty("port", SSL ? "8443" : "9696"));
    
    static HermesServer   serverMain = null;
    static HermesServer   sslServerMain = null;
    
    public static void main( String[] args )
    {
        Logs.initDefault(null, "Hermes");
        Logs.addConsoleLogger();

        // serverMain = new HermesServer();
        sslServerMain = new HermesServer();
        
        try
        {
            // serverMain.start(9696, false, false);
            sslServerMain.start(9697, true, false);
            
            startConsole();
        }
        catch(Exception xe)
        {
            xe.printStackTrace();
        }
    }
    
    private static void startConsole()
    {   
        String lineCmd = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        
        while( true )
        {
            System.out.print(">> ");
            
            // Getting command line.
            try
            {
                lineCmd = in.readLine();
            }
            catch(IOException e)
            {
                e.printStackTrace();
                break;
            }
            
            if( lineCmd == null )
                break;
            
            if( lineCmd.isEmpty() )
                continue;

            // 이전에 실행했던 명령 다시 실행
            if( "/".equals(lineCmd) )
            {
                //
            }
            else if( "t".equals(lineCmd) )
            {
                //
            }
            // 종료
            else if( "q".equals(lineCmd) || "quit".equals(lineCmd) )
            {
                System.out.println("exiting...");
                
                if( serverMain != null )
                {
                    serverMain.stop();
                    serverMain = null;
                }
                
                if( sslServerMain != null ) {
                	sslServerMain.stop();
                	sslServerMain = null;
                }

                System.exit(0);
            }
        }
    }
}
