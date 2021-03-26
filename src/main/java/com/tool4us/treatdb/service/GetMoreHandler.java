package com.tool4us.treatdb.service;

import static com.tool4us.treatdb.AppSetting.OPT;
import static com.tool4us.common.Util.UT;
import static com.tool4us.db.DatabaseTool.DBTOOL;
import static com.tool4us.treatdb.tool.SessionManager.SM;

import org.json.JSONObject;

import com.tool4us.net.http.TomyRequestor;
import com.tool4us.net.http.TomyResponse;
import com.tool4us.net.http.ApiError;
import com.tool4us.net.http.ApiHandler;
import com.tool4us.net.http.TomyApi;



@TomyApi(paths={ "/moreData" })
public class GetMoreHandler extends ApiHandler
{
    @Override
    public String call(TomyRequestor req, TomyResponse res) throws Exception
    {
    	String userToken = req.getHeaderValue("x-user-token");
        String authCode = req.getHeaderValue("x-auth-code");
        
        if( emptyCheck(authCode, userToken) || !SM.isValidAuthCode(authCode) )
            return makeResponseJson(ApiError.InvalidAuthCode);

        String qid = req.bodyParameter("qid");
        String begin = req.bodyParameter("begin");
        String sLen = req.bodyParameter("length");

        if( emptyCheck(qid, begin, sLen) )
        	return makeResponseJson(ApiError.MissingParameter);

        JSONObject retObj = new JSONObject();
        
        // TODO

        return makeResponseJson(retObj);
    }
}
