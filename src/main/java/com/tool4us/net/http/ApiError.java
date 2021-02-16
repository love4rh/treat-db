package com.tool4us.net.http;


/**
 * Definition of errors that can be occurred.
 * @author TurboK
 */
public enum ApiError
{
    Success(0),
    Failed(1, "failed"),

    InvalidHeader(1001, "invalid header"),
    InvalidParameter(1002, "invalid parameter"),
    InvalidBody(1003, "invalid body"),
    
    MissingParameter(1004, "missing parameter"),
    MissingHeader(1005, "missing header"),
    
    InvalidAuthCode(2001, "invalid auth code"),
    InvalidEULA(2002, "invalid user agreements"),

    NotExistsResult(3001, "not exists the result"),
    OverCapacityLimit(3002, "over capacity limit"),

    NeedRerequest(9001, "need to request again"),

    ServerError(9999, "Server Error")
    ;
    
    
    private String      _msg = null;
    private int         _code = 0;

    private ApiError(int code)
    {
        this(code, "");
    }
    
    private ApiError(int code, String msg)
    {
        _code = code;
        _msg = msg;
    }
    
    public String toJson()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append("\"returnCode\":").append(_code);

        if( _msg != null && !_msg.isEmpty() )
        {
          sb.append(", \"returnMessage\":\"").append(_msg).append("\"");
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString()
    {
        return _msg;
    }
    
    public int code()
    {
        return _code;
    }
}
