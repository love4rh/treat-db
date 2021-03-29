package com.tool4us.treatdb.tool;

import static com.tool4us.common.Util.UT;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;



/**
 * 세션별 데이터 핸들링 관리
 * @author TurboK
 */
public enum SessionManager
{
    SM;

	/**
	 * authCode --> User session
	 */
    private Map<String, UserSession>	_userSessions;
    
    
    private SessionManager()
    {
    	_userSessions = new ConcurrentSkipListMap<String, UserSession>();
    }
 
    public UserSession addNewSession(String userToken)
    {
    	UserSession session = new UserSession(userToken, UT.makeRandomeString(16));
    	
    	_userSessions.put(session.getAuthCode(), session);
    	
    	return session;
    }
    
    public UserSession removeSession(String authCode)
    {
    	UserSession session = _userSessions.remove(authCode);
    	
    	if( session != null )
    		session.clear();
    	
    	return session;
    }
    
    public boolean isValidAuthCode(String authCode)
    {
    	return _userSessions.containsKey(authCode);
    }

	public UserSession getSession(String authCode, String userToken)
	{
		UserSession us = _userSessions.get(authCode);

		if( us == null || !us.getUserToken().equals(userToken))
			return null;
		
		return us;
	}
}
