package com.tool4us.treatdb.tool;

import static com.tool4us.treatdb.task.JobQueue.JQ;

import java.io.File;
import java.io.FilenameFilter;
// import java.io.FilenameFilter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.tool4us.common.Logs;



public enum PMLogBank
{
    PB;
    
    private String       _resultPath;
    
    private Map<String, PMLogData>  _analysisMap;
    
    private Map<String, Integer>    _keyStatusMap;
    private List<String[]>          _keyStatusList;


    private PMLogBank()
    {
        _analysisMap = new ConcurrentSkipListMap<String, PMLogData>();
        _keyStatusMap = new ConcurrentSkipListMap<String, Integer>();
        _keyStatusList = new CopyOnWriteArrayList<String[]>();
    }
    
    public void initialize(String resultPath, boolean deleteOld) throws Exception
    {
        _resultPath = resultPath;
   
        if( deleteOld )
        {
            File dd = new File(this._resultPath);
            for(File ff : dd.listFiles())
            {
                ff.delete();
            }
        }
    }
    
    public static String makeKey(String logPath)
    {
        String MD5 = ""; 

        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5"); 
            md.update(logPath.getBytes()); 

            byte byteData[] = md.digest();
            StringBuffer sb = new StringBuffer(); 

            for(int i = 0 ; i < byteData.length ; ++i)
            {
                sb.append(Integer.toString((byteData[i]&0xff) + 0x100, 16).substring(1));
            }
            MD5 = sb.toString();
        }
        catch( NoSuchAlgorithmException xe )
        {
            xe.printStackTrace(); 
            MD5 = null; 
        }

        return MD5;
    }

    // 로그 분석을 시작할 로그 URL입력.
    // 만들어진 것이 있으면 해당하는 hash값 반환. 없으면 null.
    // null인 경우는 일정 시간 뒤 다시 호출해야 함.
    public String pushJob(String logUrl)
    {
        String key = makeKey(logUrl);
        if( key == null )
        {
            return key;
        }
        
        File f = new File(_resultPath + File.separator + key + ".pmlog");
        
        if( f.exists() )
        {
            PMLogData pmData = new PMLogData(key, f);
            _analysisMap.put(key, pmData);

            return key;
        }
        
        JQ.pushAnalysisJob(logUrl, key);
        
        return null;
    }
    
    public void pushKeyStatus(String key, String status)
    {
        Integer idx = _keyStatusMap.get(key);
        if( idx != null  )
        {
            if( !status.equals(_keyStatusList.get(idx)[0]) )
            {
                _keyStatusMap.remove(key);
                _keyStatusList.remove((int) idx);
            }
        }
        
        synchronized( _keyStatusList )
        {
            _keyStatusList.add(new String[] { status, key });
            _keyStatusMap.put(key, _keyStatusList.size() - 1);
            
            if( _keyStatusList.size() > 1024 )
            {
                Map<String, Integer> newMap = new ConcurrentSkipListMap<String, Integer>();
                List<String[]> newList = new CopyOnWriteArrayList<String[]>();
                
                for(int i = _keyStatusList.size() - 1024; i < _keyStatusList.size(); ++i)
                {
                    String[] item = _keyStatusList.get(i);
                    newMap.put(item[1], newList.size());
                    newList.add(item);
                }
                
                _keyStatusList = newList;
                _keyStatusMap = newMap;
            }
        }
    }

    // null이면 Ok, 이상이 있는 경우는 해당 에러 문구
    public String validCheck(String logUrl)
    {
        String key = makeKey(logUrl);
        Integer idx = _keyStatusMap.get(key);
        
        return idx == null ? null : _keyStatusList.get(idx)[0];
    }
    
    public PMLogData getData(String key)
    {
        if( key == null )
            return null;

        PMLogData pl = _analysisMap.get(key);

        if( pl != null )
            pl.updateLastUsedTick();
        
        return pl;
    }
    
    public void cleanUp()
    {
        long tickLimit = (new Date()).getTime() - 12 * 60 * 60 * 1000; // 12시간 이전

        Set<String> deletedKeys = new TreeSet<String>();
        Map<String, PMLogData> newMap  = new ConcurrentSkipListMap<String, PMLogData>();

        for(Entry<String, PMLogData> elem : _analysisMap.entrySet())
        {
            if( elem.getValue().getLastUsedTick() < tickLimit )
            {
                // Delete
                elem.getValue().close();
                deletedKeys.add(elem.getKey());

                Logs.info("Delete PMLog Data: {}", elem.getKey());
            }
            else
                newMap.put(elem.getKey(), elem.getValue());
        }
        
        if( !deletedKeys.isEmpty() )
        {
            _analysisMap = newMap;
            deletePMLog(deletedKeys);
        }
    }
    
    public void deletePMLog(Set<String> deletedKeys)
    {
        if( !deletedKeys.isEmpty() )
        {
            File dd = new File(this._resultPath);
            File[] ff = dd.listFiles(new FilenameFilter()
            {
                @Override
                public boolean accept(File dir, String name)
                {
                    int pos = name.indexOf(".");

                    if( pos != -1 )
                        name = name.substring(0, pos);

                    return deletedKeys.contains(name);
                }
            });
            
            for(File f : ff)
                f.delete();
        }
    }
}
