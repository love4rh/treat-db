package com.tool4us.treatdb.service;

import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import com.tool4us.common.Logs;
import com.tool4us.net.http.IApiHanlder;
import com.tool4us.net.http.TomyRequestor;

import lib.turbok.util.UsefulTool;



public abstract class ServiceHandler implements IApiHanlder
{
    public long tickCount()
    {
        return (new Date()).getTime();
    }
    
	public void startMessage(boolean printParam, TomyRequestor request)
    {
        StringBuilder sb = new StringBuilder();
        
        String uri = request.getUri();
        int pos = uri.indexOf("?");
        
        sb.append("$REQ$ ")
          .append(request.getRemoteDescription())
          .append(" | ");
        
        if( pos == -1 || pos > 128 )
        	sb.append(uri);
        else
            sb.append(uri.substring(0, pos));

        if( printParam )
        {
            sb.append(" | ");
            for(Entry<String, List<String> > param : request.parameterMap().entrySet())
            {
                String paramName = param.getKey();

                sb.append("&")
                  .append(paramName)
                  .append("=")
                  .append(param.getValue().get(0));
            }
        }

        Logs.info(sb.toString());
    }
	
	/**
	 * 입력된 파라미터 중 null이거나 빈값인 경우가 있다면 true.
	 * @param args
	 * @return
	 */
	public boolean emptyCheck(String ... args)
	{
		for(String s : args)
		{
			if( s == null || s.isEmpty() ) return true;
		}
		
		return false;
	}
	
	/**
	 * 발생한 오류를 클라이언트에 알림
	 * @param error
	 * @return
	 */
	public String resultForError(ServiceError error)
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("{\"code\":").append(error.code())
		  .append(",\"message\":\"").append(error.msg()).append("\"")
		  .append(",\"result\":{}")
		  .append("}");
		
		return sb.toString();
	}
	
	public String resultForError(String errorMsg)
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("{\"code\":").append(9999)
		  .append(",\"message\":\"").append(errorMsg.replace("\"", "'").replace("\n", " ")).append("\"")
		  .append(",\"result\":{}")
		  .append("}");
		
		return sb.toString();
	}
	
	/**
	 * 결과코드를 붙여 반환하기 위한 메소드
	 * @param result
	 * @return
	 */
	public String resultWithCode(String result)
	{
		StringBuilder sb = new StringBuilder();
		
		// {"error_code":"609","error_message":"Invalid Parameter"}
		
		int code = 0;
		String message = "";
		
		int pos = result.indexOf("\"error_code\"");
		if( pos != -1 )
		{
			pos = result.indexOf("\"", pos + 13);
			code = Integer.parseInt( result.substring(pos + 1, result.indexOf("\"", pos + 1)) );
			
			pos = result.indexOf("\"error_message\"");
			if( pos != -1 )
			{
				pos = result.indexOf("\"", pos + 16);
				message = result.substring(pos + 1, result.indexOf("\"", pos + 1));
			}
		}

		sb.append("{\"code\":").append(code)
		  .append(",\"message\":\"").append(UsefulTool.NVL(message, "")).append("\"")
		  .append(",\"result\":").append(UsefulTool.NVL(result, "{}"))
		  .append("}");
		
		return sb.toString();
	}
}
