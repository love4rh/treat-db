package com.tool4us.treatdb.service;

import static com.tool4us.treatdb.AppSetting.OPT;

import com.tool4us.net.http.TomyRequestor;
import com.tool4us.net.http.TomyResponse;
import com.tool4us.net.http.ApiError;
import com.tool4us.net.http.ApiHandler;
import com.tool4us.net.http.TomyApi;



@TomyApi(paths={ "/metadata" })
public class MetadataHandler extends ApiHandler
{
    @Override
    public String call(TomyRequestor req, TomyResponse res) throws Exception
    {
    	String userToken = req.getHeaderValue("x-user-token");
        String authCode = req.getHeaderValue("x-auth-code");
        
        // 값 존재 여부 체크
        if( emptyCheck(authCode, userToken) )
        {
            // 없다면 파라미터 오류 반환
            return makeResponseJson(ApiError.InvalidAuthCode);
        }

        return makeResponseJson(OPT.getMetadataAsString());
    }
}
