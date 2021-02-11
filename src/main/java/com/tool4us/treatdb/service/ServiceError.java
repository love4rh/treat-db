package com.tool4us.treatdb.service;


/**
 * 오류 정의
 * @author mh9.kim
 */
public enum ServiceError
{
	  MISSING_PARAMETER(11, "missing parameter(s)")
	, INVALID_PARAMETER(12, "Invalid parameter(s)")
	, UNKNOWN_JSONTYPE(51, "Unknown JSON format")
	, INVALID_JSONTYPE(52, "Invalid JSON")
	, NO_RESULT(1, "No Result")
	;
	
	private String		_message;
	private int			_code;
	
	private ServiceError(int code, String message)
	{
		_code = code;
		_message = message;
	}
	
	public int code()
	{
		return _code;
	}
	
	public String msg()
	{
		return _message;
	}
}
