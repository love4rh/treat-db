package com.tool4us.treatdb.service;

import static com.tool4us.treatdb.tool.PMLogBank.PB;

import com.tool4us.net.http.RequestEx;
import com.tool4us.net.http.ResponseEx;
import com.tool4us.treatdb.tool.PMLogData;

import static io.netty.handler.codec.http.HttpHeaderNames.*;



/**
 * 로그 파일 텍스트 형태로 다운로그
 * 
 * @author mh9.kim
 */
@com.tool4us.net.http.RequestDefine(paths={ "/download" })
public class DownloadHandler extends ServiceHandler
{
    @Override
    public String call(RequestEx req, ResponseEx res) throws Exception
    {
        startMessage(true, req);
        
        String dataKey = (String) req.getParameter("dataKey");

        PMLogData pl = PB.getData(dataKey);
        if( pl == null )
            return "{\"returnValue\":false, \"code\":\"ERROR\"}";
        
        res.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

        String rawText = pl.getRawLogFile();
        
        return rawText;
    }
}
