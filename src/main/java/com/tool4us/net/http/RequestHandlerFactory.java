package com.tool4us.net.http;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.reflections.Reflections;



/**
 * HTTP Request Handler 클래스를 생성하기 위한 클래스
 * 
 * @author TurboK
 */
public class RequestHandlerFactory
{
    private Map<String, Class<?>>  _classMap = null;
    
    
    RequestHandlerFactory(String packageName)
    {
        initClasseMap(packageName);
    }
    
    /**
     * 지정한 패키지 내에 있는 모든 Class를 찾아 반환하는 메소드
     * @param pckgname
     * @return
     * @throws ClassNotFoundException
     */
    private void initClasseMap(String packageName)
    {
        if( _classMap != null )
            return;
        
        _classMap = new TreeMap<String, Class<?>>();
        
        Reflections reflections = new Reflections(packageName);

        // TODO 상속을 한 번 더 받아도 되는지 확인해 봐야 함.
        Set<Class<? extends IRequestHandler>> allClasses
            = reflections.getSubTypesOf(IRequestHandler.class);

        for(Class<? extends IRequestHandler> clazz : allClasses)
        {           
            RequestDefine annot = clazz.getAnnotation(RequestDefine.class);
            
            if( annot == null )
                continue;
            
            for(String path : annot.paths())
            {
                if( _classMap.containsKey(path) )
                    throw new RuntimeException("Duplicated path - " + path);
                
                _classMap.put(path, clazz);
            }
        }
    }

    public IRequestHandler getRquestClazz(String uri) throws Exception
    {
        Class<?> clazz = _classMap.get(uri);
        
        if( clazz == null )
        {
            // TODO 404인데 어떻게 할까?
            // TODO API별로 기본 메소드는 필요 없을까? 있다면 어떻게 지정해야 할까? WAS의 web.xml이 이건데...
            return null;
        }

        return (IRequestHandler) clazz.newInstance();
    }
}
