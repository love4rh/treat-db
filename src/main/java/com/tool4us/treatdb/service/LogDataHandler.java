package com.tool4us.treatdb.service;

import static com.tool4us.treatdb.tool.PMLogBank.PB;
import static com.tool4us.treatdb.tool.SessionManager.SM;

import com.tool4us.net.http.RequestEx;
import com.tool4us.net.http.ResponseEx;
import com.tool4us.treatdb.tool.PMLogData;

import lib.turbok.common.ITabularData;
import lib.turbok.util.TabularDataTool;



/**
 * 분석 대상 로그 경로를 입력 받아 분석을 위한 기본 준비 수행.
 * 1. 경로상의 messages*를 받아와 하나의 파일로 저장
 * 2. Category 분석
 * 오래 걸릴 경우 "진행 중"임을 나타내는 시그널을 보내고,
 * 요청 시 완료된 경우에는 화면 표시를 위한 일부 데이터를 전달함.
 * 
 * @author mh9.kim
 */
@com.tool4us.net.http.RequestDefine(paths={ "/logData" })
public class LogDataHandler extends ServiceHandler
{
    @Override
    public String call(RequestEx req, ResponseEx res) throws Exception
    {
        startMessage(true, req);
        
        String dataKey = (String) req.getParameter("dataKey");
        String startStr = (String) req.getParameter("start");
        String lenStr = (String) req.getParameter("len");
        String userToken = (String) req.getParameter("userToken");

        PMLogData pl = PB.getData(dataKey);
        if( pl == null )
            return "{\"returnValue\":false, \"code\":\"ERROR\"}";
        
        ITabularData ds = SM.getDataStore(dataKey, userToken);
        if( ds == null )
            ds = pl.getDataStore();
        
        String data = TabularDataTool.genRecordsAsJson(
            ds, Long.parseLong(startStr), Long.parseLong(lenStr)
        );

        if( data == null )
            return "{\"returnValue\":false, \"code\":\"ERROR\"}";
        
        return "{\"returnValue\":true, " + data + "}";
    }
}
