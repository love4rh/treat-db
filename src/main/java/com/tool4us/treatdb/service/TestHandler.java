package com.tool4us.treatdb.service;

import java.util.Date;

import com.tool4us.net.http.RequestEx;
import com.tool4us.net.http.ResponseEx;



@com.tool4us.net.http.RequestDefine(paths={ "/test" })
public class TestHandler extends ServiceHandler
{
    @Override
    public String call(RequestEx req, ResponseEx res) throws Exception
    {
        startMessage(true, req);

        return "{\"return\":\"test\", \"tick\":" + (new Date()).getTime() + "}";
    }
}
