package com.tool4us.treatdb.tool;

import static com.tool4us.common.Util.UT;



public class UserSession
{
	private String		_userToken = null;
	private String		_authCode = null;
	private long		_startTick = UT.tickCount();
	
	
	public UserSession(String userToken, String authCode)
	{
		_userToken = userToken;
		_authCode = authCode;
	}
	
	public String getUserToken()
	{
		return _userToken;
	}
	
	public String getAuthCode()
	{
		return _authCode;
	}
	
	public long getStartTick()
	{
		return _startTick;
	}
	
}
