package com.tool4us.treatdb.task;



@SuppressWarnings("serial")
public class JobException extends Exception
{
    private String      _key;
    private String      _error;


    public JobException(String key, String error)
    {
        _key = key;
        _error = error;
    }
    
    public String getKey()
    {
        return _key;
    }
    
    public String getError()
    {
        return _error;
    }
    
    @Override
    public String toString()
    {
        return "JobException - " + _error + " : " + _key;
    }
}
