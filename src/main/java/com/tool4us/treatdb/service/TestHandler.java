package com.tool4us.treatdb.service;

import java.util.Date;

import com.tool4us.net.http.TomyRequestor;
import com.tool4us.net.http.TomyResponse;
import com.tool4us.net.http.TomyApi;



@TomyApi(paths={ "/test" })
public class TestHandler extends ServiceHandler
{
    @Override
    public String call(TomyRequestor req, TomyResponse res) throws Exception
    {
        startMessage(true, req);

        return "{\"return\":\"test\", \"tick\":" + (new Date()).getTime() + "}";
    }
}
