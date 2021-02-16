package com.tool4us.treatdb.service;

import org.json.JSONObject;

import com.tool4us.net.http.TomyRequestor;
import com.tool4us.net.http.TomyResponse;
import com.tool4us.net.http.ApiError;
import com.tool4us.net.http.ApiHandler;
import com.tool4us.net.http.TomyApi;



@TomyApi(paths={ "/test" })
public class TestHandler extends ApiHandler
{
    @Override
    public String call(TomyRequestor req, TomyResponse res) throws Exception
    {
        // 파라미터 가져오기
        String name = req.getParameter("name");
        
        // 값 존재 여부 체크
        if( emptyCheck(name) )
        {
            // 없다면 파라미터 오류 반환
            return makeResponseJson(ApiError.MissingParameter);
        }
        
        // 있다면 정상 루틴 실행
        JSONObject retObj = new JSONObject();

        retObj.put("greetings", "Hello " + name + "!!");
        
        return makeResponseJson(retObj);
    }
}
