package com.tool4us.net.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * SimpleHttpServer에서 Request를 처리하기 위한 핸들러의 속성을 지정하는 Annotation
 * 
 * @author TurboK
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestDefine
{
    /**
     * 경로 지정
     */
    String[] paths();
    
    /**
     * 지원하는 HTTP 방식 지정.
     * TODO 아직 제대로 지원 안됨.
     */
    String[] methods() default { "POST", "GET" };
}
