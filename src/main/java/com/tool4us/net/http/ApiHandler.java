package com.tool4us.net.http;

import org.json.JSONObject;

import io.netty.handler.codec.http.HttpResponseStatus;

import static com.tool4us.common.Util.UT;



/**
 * Tomy Server 용 API 핸들러 베이스 클래스
 * 
 * @author TurboK
 */
public abstract class ApiHandler
{
    public abstract String call(TomyRequestor req, TomyResponse res) throws Exception;

    public static long tickCount()
    {
        return UT.tickCount();
    }

    public static String makeResponseJson(String responseText, String msgId)
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append("{");
        sb.append("\"timestamp\":").append(tickCount());

        if( UT.isValidString(msgId) )
        {
            sb.append(",\"messageId\":\"").append(msgId).append("\"");
        }
        sb.append(",\"returnCode\":").append(ApiError.Success.code());

        if( UT.isValidString(responseText) )
        {
            sb.append(",\"response\":").append(responseText);
        }

        sb.append("}");
        
        return sb.toString();
    }
    
    public static String makeResponseJson(String responseText)
    {
        return makeResponseJson(responseText, null);
    }
    
    public static String makeResponseJson(JSONObject obj, String msgId)
    {
        return makeResponseJson(obj.toString(), msgId);
    }
    
    public static String makeResponseJson(JSONObject obj)
    {
        return makeResponseJson(obj, null);
    }
    
    public static String makeResponseJson(ApiError error, String msgId)
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append("{").append("\"timestamp\":").append(tickCount());
        
        if( UT.isValidString(msgId ))
        {
            sb.append(",\"messageId\":\"").append(msgId).append("\"");
        }

        sb.append(",").append(error.toJson());

        sb.append("}");
        
        return sb.toString();
    }
    
    public static String makeResponseJson(ApiError error)
    {
        return makeResponseJson(error, null);
    }
    
    public static String makeResponseJson(int retCode, String retMsg, String msgId)
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append("{").append("\"timestamp\":").append(tickCount());
        
        if( UT.isValidString(msgId ))
        {
            sb.append(",\"messageId\":\"").append(msgId).append("\"");
        }
        sb.append(",\"returnCode\":").append(retCode)
          .append(",\"returnMessage\":\"").append(retMsg).append("\"");
        sb.append("}");
        
        return sb.toString();
    }
    
    public static String makeResponseJson(HttpResponseStatus error, String msgId)
    {
    	return makeResponseJson(error.code(), error.reasonPhrase(), msgId);
    }
    
    public static String makeResponseJson(HttpResponseStatus error)
    {
        return makeResponseJson(error, null);
    }
	
	/**
	 * 입력된 파라미터 중 null이거나 빈값인 경우가 있다면 true.
	 * @param args
	 * @return
	 */
	public static boolean emptyCheck(String ... args)
	{
		for(String s : args)
		{
			if( s == null || s.isEmpty() ) return true;
		}
		
		return false;
	}
}
