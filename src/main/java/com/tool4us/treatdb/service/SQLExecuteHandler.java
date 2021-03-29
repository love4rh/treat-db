package com.tool4us.treatdb.service;

import static com.tool4us.treatdb.AppSetting.OPT;
import static com.tool4us.common.Util.UT;
import static com.tool4us.treatdb.tool.SessionManager.SM;

import com.tool4us.net.http.TomyRequestor;
import com.tool4us.net.http.TomyResponse;
import com.tool4us.treatdb.task.DBTask;
import com.tool4us.treatdb.tool.UserSession;
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
        String lastQid = req.bodyParameter("lastQID");

        if( emptyCheck(dbIdx, sql) )
        	return makeResponseJson(ApiError.MissingParameter);

        String[] dbOpt = OPT.getDatabaseOption(UT.parseLong(dbIdx).intValue());
        if( dbOpt == null )
        	return makeResponseJson(ApiError.InvalidParameter);
        
        UserSession session = SM.getSession(authCode, userToken);

        if( session == null )
        	return makeResponseJson(ApiError.InvalidAuthCode);

        String jsonResult = null;
        
        try
        {
        	if( UT.isValidString(lastQid) )
        		session.removeResult(lastQid);

        	DBTask task = session.executeQuery(sql, dbOpt[1], dbOpt[2], dbOpt[3], dbOpt[4]);
        	
        	if( task == null )
        		return makeResponseJson(ApiError.NotExistsResult);
        	
        	synchronized( task )
        	{
       			task.wait(60000);
        		jsonResult = task.getInitialData();
			}
        	
        	if( jsonResult == null )
        		return makeResponseJson(ApiError.NotExistsResult); // long query
        }
        catch( Exception xe )
        {
        	return makeResponseJson(9999, xe.getMessage(), null);
        }

        return makeResponseJson(jsonResult);
    }
}
