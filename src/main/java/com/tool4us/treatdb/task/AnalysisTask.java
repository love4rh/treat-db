package com.tool4us.treatdb.task;

import static com.tool4us.treatdb.AppSetting.OPT;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import com.tool4us.common.Logs;

import lib.turbok.common.ValueType;
import lib.turbok.data.Columns;
import lib.turbok.data.FileMapStore;
import lib.turbok.data.TabularDataCreator;
import lib.turbok.input.GzInputStream;
import lib.turbok.input.HTTPInputStream;
import lib.turbok.task.ITask;
import lib.turbok.util.TextFileLineReader;
import lib.turbok.util.UsefulTool;



public class AnalysisTask extends ITask
{
    private String      _url;
    private String      _key;
    
    private Set<String> _logTypes = new TreeSet<String>();
    private Set<String> _processNames = new TreeSet<String>();
    private Set<String> _contextNames = new TreeSet<String>();
    private Set<String> _msgIDs = new TreeSet<String>();
    
    public AnalysisTask(String url, String key)
    {
        _url = url;
        _key = key;
    }
    
    public String getKey()
    {
        return _key;
    }

    @Override
    public boolean isPossibleToRun()
    {
        return true;
    }

    @Override
    public String toString()
    {
        return "AL-TASK-" + _key;
    }

    @Override
    public void run() throws Exception
    {
        String prefix = OPT.resultFolder() + File.separator + _key; 
        String outFile = prefix + ".log";
        String dataFile= prefix + ".pmd";
        String resultFile = prefix + ".pmlog";
        
        FileMapStore ds = null;
        BufferedWriter out = null;
        
        try
        {
            if( !this.step1(outFile) )
            {
                throw new AnalysisJobException(_key, "empty log");
            }

            ds = this.step2(outFile, resultFile);
            
            // ds 저장 및 디멘젼 정보 저장
            if( ds != null )
                ds.store(dataFile, true);
            
            // 저장할 파일 열기
            out = new BufferedWriter( new OutputStreamWriter(new FileOutputStream(new File(resultFile)), "UTF-8") );
            
            out.write("PMLog Analyzer verion 1.0\n");

            out.write("" + (new Date()).getTime()); out.write("\n");

            out.write(_key); out.write("\n");
            out.write(_url); out.write("\n");
            out.write(dataFile); out.write("\n");

            writeCategory(out, _logTypes);
            writeCategory(out, _processNames);
            writeCategory(out, _contextNames);
            writeCategory(out, _msgIDs);
        }
        catch( Exception xe )
        {
            if( xe instanceof AnalysisJobException )
            {
                throw xe;
            }
        }
        finally
        {
            if( ds != null )
                ds.close();
            if( out != null )
                out.close();
        }
    }
    
    private void writeCategory(BufferedWriter out, Set<String> category) throws Exception
    {
        boolean filled = false;
        for(String tmpStr : category)
        {
            if( filled )
                out.write(" ");
            else
                filled = true;

            out.write(tmpStr);
        }

        out.write("\n");
    }
    
    private Object[] processLine(String lineText) throws Exception
    {
        Object[] record = new Object[11];

        int sPos = 0, ePos = 0;
        
        for(int c = 0; c < 7; ++c)
        {
            ePos = lineText.indexOf(" ", sPos);
            if( ePos == -1 )
                throw new Exception("Invalid format.");
            
            String token = lineText.substring(sPos, ePos);
            
            switch(c)
            {
            case 0: // 2014-02-28T00:00:28.249452Z
                {
                    int tmpPos = token.indexOf(".");
                    if( tmpPos == -1 )
                        throw new Exception("Invalid format.");
                    
                    String tmpStr = token.substring(0, tmpPos);
                    tmpStr = tmpStr.replace('T', ' ');
                    
                    record[0] = UsefulTool.ConvertStringToDate(tmpStr, "yyyy-MM-dd HH:mm:ss");
                    record[1] = Long.parseLong(token.substring(tmpPos + 1, token.length() - 1));
                } break;
            case 1: // [29]
                {
                    if( '[' != token.charAt(0) || ']' != token.charAt(token.length() - 1) )
                        throw new Exception("Invalid format.");

                    record[2] = Double.parseDouble(token.substring(1, token.length() - 1));
                } break;
            case 2: // Level
                if( token != null && !token.isEmpty() ) _logTypes.add(token);
                record[c + 1] = token;
                break;
            case 3: // Process Name
                if( token != null && !token.isEmpty() ) _processNames.add(token);
                record[c + 1] = token;
                break;
            case 4: // [10,20]
                {
                    if( '[' != token.charAt(0) || ']' != token.charAt(token.length() - 1) )
                        throw new Exception("Invalid format.");
                    
                    int tmpPos = token.indexOf(",");
                    
                    if( tmpPos == -1 )
                    {
                        record[5] = null;
                        record[6] = null;
                    }
                    else
                    {
                        record[5] = Long.parseLong(token.substring(1, tmpPos));
                        record[6] = Long.parseLong(token.substring(tmpPos + 1, token.length() - 1));
                    }
                } break;
            case 5: // Context Name
                if( token != null && !token.isEmpty() ) _contextNames.add(token);
                record[7] = token;
                break;
            case 6: // Msg Id
                if( token != null && !token.isEmpty() ) _msgIDs.add(token);
                record[8] = token;
                break;
            default:
                break;
            }

            sPos = ePos + 1;
        }
        
        if( sPos >= lineText.length() || '{' != lineText.charAt(sPos) )
        {
            record[9] = null;
            record[10] = lineText.substring(sPos + 1);
        }
        else
        {
            ePos = lineText.indexOf("}", sPos);
            if( ePos == - 1)
            {
                record[9] = null;
                record[10] = lineText.substring(sPos + 1);
            }
            else
            {
                // Key-Value Pair
                record[9] = lineText.substring(sPos, ePos + 1);
                
                // Free Text
                if( ePos < lineText.length() )
                    record[10] = lineText.substring(ePos + 1).trim();
                else
                    record[10] = null;
            }
        }
            
        return record;
    }
    
    // 하나로 묶은 로그에서 카테고리 등을 추출하는 단계
    private FileMapStore step2(String logFile, String resultFile) throws Exception
    {
        // Timestamp, microseconds, MonotonicTime, Level, Process Name, Process ID, Thread ID
        // Context Name, MSGID, KeyValue, Free Text
        Columns columns = new Columns();
        
        columns.addColumn("Timestamp", ValueType.DateTime); // 0
        columns.addColumn("Micro-seconds", ValueType.Integer);
        columns.addColumn("Monotonic Time", ValueType.Real);
        columns.addColumn("Level", ValueType.Text);
        columns.addColumn("Process Name", ValueType.Text);
        // columns.addColumn("Process ID", ValueType.Integer);
        // columns.addColumn("Thread ID", ValueType.Integer);
        columns.addColumn("Context Name", ValueType.Text); // 7 --> 5
        columns.addColumn("MSGID", ValueType.Text);
        columns.addColumn("Key-Value Pair", ValueType.Text);
        columns.addColumn("Free Text", ValueType.Text);

        // 읽을 파일 열기
        TextFileLineReader in = new TextFileLineReader(logFile, "UTF-8");
        
        FileMapStore store = TabularDataCreator.newTabularData(columns, 0);

        boolean saveStart = false;
        
        String tmpLine = null;
        String lineText = null;
        int lineNo = 0;
        
        try
        {
            // com.webos.app.homeconnect / hd-service
            // 2014-02-28T00:00:28.249452Z [29] user.info mvpd-service [10,20] MVPD-SERVICE INFO {"K":"V"} _commandservice_Thread:268] Started!! 
            while( null != (tmpLine = in.getNextLine()) || lineText != null )
            {
                lineNo += 1;

                try
                {
                    if( tmpLine != null )
                        Integer.parseInt(tmpLine.substring(0, 4));
                    
                    if( lineText != null )
                    {
                        Object[] record = this.processLine(lineText);
                        
                        /*// TODO 여러 줄로 나눠져 있는 JSON 하나로 합치기
                        if( "com.webos.app.homeconnect".equals(record[7]) || "hd-service".equals(record[7]) )
                        {
                            // record[10]
                        } // */
                        
                        long insRow = store.getRowSize();
                        for(int c = 0; c < 5; ++c)
                            store.setCell(c, insRow, record[c]);
                        
                        for(int c = 5; c < (int) store.getColumnSize(); ++c)
                            store.setCell(c, insRow, record[c + 2]); // process id, threa id 제외
                    }
                        
                    lineText = tmpLine;
                }
                catch(Exception xe)
                {
                    // Exception이 나면 2014-02- 이런 식으로 시작하지 않는 줄이 잘린 경우임.
                    if( saveStart )
                        lineText += tmpLine;
                    else
                        continue;
                }

                saveStart = true;
            }
            
            Logs.info("{} --> {} processed", _url, lineNo);
        }
        catch(Exception e)
        {
            store.clearAndDelete(true);
            throw e;
        }
        finally
        {
            in.close();
        }
        
        return store;
    }
    
    // PM 로그를 사이트에서 읽어 하나로 저장하는 단계
    private boolean step1(String outFile) throws Exception
    {
        final String[] names = new String[] {
            "messages.4.gz", "messages.3.gz", "messages.2.gz", "messages.1.gz", "messages.0.gz", "messages"
        };

        boolean hasLog = false;
        InputStream in = null;
        byte[] buf = new byte[8192];
        
        OutputStream outStream = null;
        
        for(String name : names)
        {
            try
            {
                if( name.endsWith(".gz") )
                {
                    in = new GzInputStream(new HTTPInputStream(_url + "/" + name));
                }
                else
                {
                    in = new HTTPInputStream(_url + "/" + name);
                }
                
                int readLen = in.read(buf);
                
                if( readLen > 0 && outStream == null )
                {
                    outStream = new FileOutputStream(new File(outFile));
                }

                while( readLen > 0 )
                {
                    outStream.write(buf, 0, readLen);
                    readLen = in.read(buf);
                    hasLog = true;
                }
            }
            catch( Exception xe )
            {
                // xe.printStackTrace();
            }
            finally
            {
                if( in != null )
                    in.close();
            }
        }
        
        if( outStream != null )
            outStream.close();
        
        return hasLog;
    }
}
