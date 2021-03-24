package com.tool4us.treatdb.service;

import static com.tool4us.treatdb.AppSetting.OPT;
import static com.tool4us.common.Util.UT;
import static com.tool4us.db.DatabaseTool.DBTOOL;

import org.json.JSONObject;

import com.tool4us.net.http.TomyRequestor;
import com.tool4us.net.http.TomyResponse;
import com.tool4us.net.http.ApiError;
import com.tool4us.net.http.ApiHandler;
import com.tool4us.net.http.TomyApi;



@TomyApi(paths={ "/executeSql" })
public class SQLExecuteHandler extends ApiHandler
{
    @Override
    public String call(TomyRequestor req, TomyResponse res) throws Exception
    {
    	String userToken = req.getHeaderValue("x-user-token");
        String authCode = req.getHeaderValue("x-auth-code");
        
        if( emptyCheck(authCode, userToken) )
            return makeResponseJson(ApiError.InvalidAuthCode);
        
        String dbIdx = req.bodyParameter("dbIdx");
        String sql = req.bodyParameter("query");

        if( emptyCheck(dbIdx, sql) )
        	return makeResponseJson(ApiError.MissingParameter);
        
        String[] dbOpt = OPT.getDatabaseOption(UT.parseLong(dbIdx).intValue());
        if( dbOpt == null )
        	return makeResponseJson(ApiError.InvalidParameter);
        
        String queryID = UT.makeRandomeString(12);

        JSONObject retObj = new JSONObject();
        // dbConnInfo, sql

        // TODO assign session auth code
        retObj.put("qid", queryID);
        
        JSONObject qResult = DBTOOL.executeQuery(sql, dbOpt[1], dbOpt[2], dbOpt[3], dbOpt[4]);

        return makeResponseJson(retObj);
    }
}
