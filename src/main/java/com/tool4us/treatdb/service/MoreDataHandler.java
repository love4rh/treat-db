package com.tool4us.treatdb.service;

import static com.tool4us.common.Util.UT;
import static com.tool4us.treatdb.tool.SessionManager.SM;

import com.tool4us.net.http.TomyRequestor;
import com.tool4us.net.http.TomyResponse;
import com.tool4us.treatdb.tool.UserSession;

import lib.turbok.data.FileMapStore;
import lib.turbok.util.TabularDataTool;

import com.tool4us.net.http.ApiError;
import com.tool4us.net.http.ApiHandler;
import com.tool4us.net.http.TomyApi;



@TomyApi(paths={ "/moreData" })
public class MoreDataHandler extends ApiHandler
{
    @Override
    public String call(TomyRequestor req, TomyResponse res) throws Exception
    {
    	String userToken = req.getHeaderValue("x-user-token");
        String authCode = req.getHeaderValue("x-auth-code");
        
        if( emptyCheck(authCode, userToken) )
            return makeResponseJson(ApiError.InvalidAuthCode);

        String qid = req.bodyParameter("qid");
        String beginIdx = req.bodyParameter("beginIdx");
        String length = req.bodyParameter("length");
        

        if( emptyCheck(qid) )
        	return makeResponseJson(ApiError.MissingParameter);
        
        UserSession session = SM.getSession(authCode, userToken);

        if( session == null )
        	return makeResponseJson(ApiError.InvalidAuthCode);
        
        FileMapStore ds = session.getFetchRestul(qid);

        if( ds == null )
        	return makeResponseJson(ApiError.NotExistsResult);
        

        StringBuilder sb = new StringBuilder();
        
        sb.append("{");
        
        sb.append(TabularDataTool.genRecordsAsJson(ds, UT.parseLong(beginIdx), UT.parseLong(length)));
        sb.append(",\"recordCount\":").append(ds.getRowSize());
        sb.append(",\"fetchDone\":").append(session.isDoneResult(qid));
        
        sb.append("}");
        
        return makeResponseJson(sb.toString());
    }
}
