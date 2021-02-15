package com.tool4us.net.http;

import java.io.File;


public interface IStaticFileMap
{
    /**
     * @return root에 해당하는 파일명 반환. 보통 index.html임
     */
    String getRootFile();
    
    /**
     * 반환 가능 여부 반환.
     */
    boolean isAllowed(String uriPath);
    
    /**
     * uriPath와 일치하는 파일 반환. 없으면 null.
     */
    File getFile(String uriPath);
}
