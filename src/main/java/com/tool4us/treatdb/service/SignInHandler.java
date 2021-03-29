package com.tool4us.treatdb.service;

// import static com.tool4us.treatdb.AppSetting.OPT;
import static com.tool4us.treatdb.tool.SessionManager.SM;

import org.json.JSONObject;

import com.tool4us.net.http.TomyRequestor;
import com.tool4us.net.http.TomyResponse;
import com.tool4us.treatdb.tool.UserSession;
import com.tool4us.net.http.ApiError;
import com.tool4us.net.http.ApiHandler;
import com.tool4us.net.http.TomyApi;



@TomyApi(paths={ "/checkAuthority" })
public class SignInHandler extends ApiHandler
{
    @Override
    public String call(TomyRequestor req, TomyResponse res) throws Exception
    {
    	String userToken = req.getHeaderValue("x-user-token");
        String userID = req.getParameter("userID");
        String password = req.getParameter("password");
        
        if( emptyCheck(userID, password) )
        {
            return makeResponseJson(ApiError.MissingParameter);
        }
        
        // TODO check validity of userID & password
        if( !("ibs".equals(userID) && "1111".equals(password))
        	&& !("admin".equals(userID) && "1234".equals(password)) )
        {
        	return makeResponseJson(ApiError.InvalidAuthCode);
        }
        
        JSONObject retObj = new JSONObject();
        UserSession session = SM.addNewSession(userToken);

        // TODO assign session auth code
        retObj.put("authCode", session.getAuthCode());
        retObj.put("tickCount", session.getStartTick());

        return makeResponseJson(retObj);
    }
}
