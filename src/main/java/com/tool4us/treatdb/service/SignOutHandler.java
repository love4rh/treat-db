package com.tool4us.treatdb.service;

import static com.tool4us.treatdb.tool.SessionManager.SM;

import com.tool4us.net.http.TomyRequestor;
import com.tool4us.net.http.TomyResponse;
import com.tool4us.net.http.ApiError;
import com.tool4us.net.http.ApiHandler;
import com.tool4us.net.http.TomyApi;



@TomyApi(paths={ "/signOut" })
public class SignOutHandler extends ApiHandler
{
    @Override
    public String call(TomyRequestor req, TomyResponse res) throws Exception
    {
    	String authCode = req.getHeaderValue("x-auth-code");
    	
        if( emptyCheck(authCode) )
        {
            return makeResponseJson(ApiError.MissingHeader);
        }
        
        SM.removeSession(authCode);

        return makeResponseJson(ApiError.Success);
    }
}
