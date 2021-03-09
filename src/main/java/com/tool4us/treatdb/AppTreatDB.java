package com.tool4us.treatdb;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.TimeZone;

import lib.turbok.util.UsefulTool;



public class AppTreatDB
{
    public static void main(String[] args)
    {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        String configFile = "";
        
        if( args.length == 1 )
        {
            console();
            return;
        }
        else
        {
            configFile = UsefulTool.concat( UsefulTool.GetModulePath()
                  , File.separator, "conf", File.separator, "setting.yml" );
        }
        
        System.out.println("Treat DB Sever starting...");
        
        AppMain appMain = new AppMain();
        
        try
        {
            appMain.start(configFile);
        }
        catch(Exception xe)
        {
            xe.printStackTrace();
        }
    }
    
    private static void console()
    {
        String command;
        BufferedReader bufRead = new BufferedReader(new InputStreamReader(System.in));
        
        while( true )
        {
            System.out.print(">> ");
            
            try
            {
                command = bufRead.readLine();
            }
            catch(Exception xe)
            {
                xe.printStackTrace();
                continue;
            }
            
            if( command != null )
                command = command.trim();
            
            if( command == null || command.isEmpty() )
                continue;
            
            boolean showTime = true;
            long startTick = System.currentTimeMillis();
            
            if( "q".equalsIgnoreCase(command) )
            {
                break;
            }
            else if( "t1".equalsIgnoreCase(command) )
            {
                // 
            }
            
            if( showTime )
            {
                System.out.println("Processing Time: " + (System.currentTimeMillis() - startTick) + " ms");
            }
        }
    }
}
