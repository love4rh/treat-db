package com.tool4us.net.example.handler;

import org.json.JSONObject;

import com.tool4us.net.http.ApiError;
import com.tool4us.net.http.ApiHandler;
import com.tool4us.net.http.TomyApi;
import com.tool4us.net.http.TomyRequestor;
import com.tool4us.net.http.TomyResponse;



/**
 * TomyServer에서 사용할 API Handler 예제.
 * TomyApi annotation으로 API의 경로를 지정함. (paths={"/hello", ...})
 * @author TurboK
 */
@TomyApi(paths={ "/hello" })
public class HelloWorldHandler extends ApiHandler
{
    @Override
    public String call(TomyRequestor req, TomyResponse res) throws Exception
    {
        // 파라미터 가져오기
        String name = req.getParameter("name");
        
        // 값 존재 여부 체크 (베이스 클래스 ApiHandler에 정의)
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
