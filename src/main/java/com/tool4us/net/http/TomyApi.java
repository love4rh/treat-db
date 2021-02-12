package com.tool4us.net.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * TomyServer에서 사용되는 API를 정의하기 위한 Annotation
 * 
 * @author TurboK
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TomyApi
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
