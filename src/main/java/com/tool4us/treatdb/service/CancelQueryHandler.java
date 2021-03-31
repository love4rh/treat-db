package com.tool4us.treatdb.service;

import static com.tool4us.treatdb.tool.SessionManager.SM;

import com.tool4us.net.http.TomyRequestor;
import com.tool4us.net.http.TomyResponse;
import com.tool4us.treatdb.tool.UserSession;

import com.tool4us.net.http.ApiError;
import com.tool4us.net.http.ApiHandler;
import com.tool4us.net.http.TomyApi;



/**
 * cancel querying job
 * 
 * @author TurboK
 */
@TomyApi(paths={ "/cancelQuery" })
public class CancelQueryHandler extends ApiHandler
{
    @Override
    public String call(TomyRequestor req, TomyResponse res) throws Exception
    {
    	String userToken = req.getHeaderValue("x-user-token");
        String authCode = req.getHeaderValue("x-auth-code");
        
        if( emptyCheck(authCode, userToken) )
            return makeResponseJson(ApiError.InvalidAuthCode);

        String qid = req.bodyParameter("qid");
        
        if( emptyCheck(qid) )
        	return makeResponseJson(ApiError.MissingParameter);
        
        UserSession session = SM.getSession(authCode, userToken);

        if( session == null )
        	return makeResponseJson(ApiError.InvalidAuthCode);
        
        session.removeResult(qid);
        
        return makeResponseJson(ApiError.Success);
    }
}
