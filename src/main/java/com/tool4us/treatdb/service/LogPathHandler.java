package com.tool4us.treatdb.service;

import static com.tool4us.treatdb.tool.PMLogBank.PB;
import static com.tool4us.treatdb.tool.SessionManager.SM;

import com.tool4us.common.Logs;
import com.tool4us.net.http.RequestEx;
import com.tool4us.net.http.ResponseEx;
import com.tool4us.treatdb.tool.PMLogData;



/**
 * 분석 대상 로그 경로를 입력 받아 분석을 위한 기본 준비 수행.
 * 1. 경로상의 messages*를 받아와 하나의 파일로 저장
 * 2. Category 분석
 * 오래 걸릴 경우 "진행 중"임을 나타내는 시그널을 보내고,
 * 요청 시 완료된 경우에는 화면 표시를 위한 일부 데이터를 전달함.
 * 
 * @author mh9.kim
 */
@com.tool4us.net.http.RequestDefine(paths={ "/inputLogPath" })
public class LogPathHandler extends ServiceHandler
{
    @Override
    public String call(RequestEx req, ResponseEx res) throws Exception
    {
        long tick = tickCount();
        
        startMessage(true, req);
        
        // 1. http://dob.lge.com/~defectobserver/blackbox/viewer.html?blackbox_idx=822625
        // 2. http://dob.lge.com/~defectobserver/blackbox/logs/dob_pmlog_820493 
        // 3. http://dob.lge.com/~defectobserver/blackbox/logs/dob_pmlog_820493/log/
        // http://dob.lge.com/~defectobserver/blackbox/logs/dob_pmlog_816240/log/
        String path = (String) req.getParameter("path");
        String userToken = (String) req.getParameter("userToken");
        
        if( userToken == null || userToken.isEmpty() || path == null || path.isEmpty() )
            return "{\"code\":\"ERROR\"}";

        if( path.endsWith("/") )
            path = path.substring(0, path.length() - 1);
        
        int pos = path.indexOf("blackbox_idx=");
        if( pos != -1 )
        {
            String id = path.substring(pos + 13);
            try
            {
                Long.parseLong(id);
                path = "http://dob.lge.com/~defectobserver/blackbox/logs/dob_pmlog_" + id + "/log";
            }
            catch( Exception xe )
            {
                System.out.println("maybe normal type");
            }
        }

        if( !path.endsWith("/log") )
        {
            path = path + "/log";
        }
        
        String status = PB.validCheck(path);
        
        if( status != null )
            return "{\"code\":\"ERROR\"}";

        String logKey = PB.pushJob( path );

        if( logKey == null )
            return "{\"returnValue\":false, \"code\":\"WORKING\"}";
        
        PMLogData pl = PB.getData(logKey);
        if( pl == null )
            return "{\"code\":\"ERROR\"}";
        
        SM.prepareUserData(userToken);
        
        Logs.info("Processing Time: {}", tickCount() - tick);

        return "{\"returnValue\":true, \"code\":\"DONE\"," + pl.toJson() + "}";
    }
}
