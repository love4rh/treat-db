package lib.turbok.hermes;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListMap;


/**
 * 사용자 통합 관리
 * TODO 사용자 GUID 생성 방법에 대해 조금 더 고민해야 함.
 * 1. 해시값을 이용하면 직접 입력하기가 힘들므로 사용성에 불편함이 있으며,
 * 2. 4 ~ 5 숫자로 할 경우 다른 사용자가 실수 혹은 의도적으로 입력할 수 있어 보안상 문제가 됨.
 * 3. 숫자로 하되 시퀀스하게 채번하지 말고 랜덤으로 하는 것이 좋을까?
 * 3번 형식으로 우선 해보자 
 * @author TurboK
 */
public enum HermesClientManager
{
    CM;
    
    /**
     * 4자리 숫자로 맞추고자 하면 9999 - 1로 설정하여야 함.
     */
    private static final int    _clientLimit = 99999;
    
    private BitSet      _guid = new BitSet(_clientLimit - 1);
    private Random      _random = new Random();
    
    private Map<String, HermesClientHandler>    _clientMap = null;
    
    private HermesClientManager()
    {
        _clientMap = new ConcurrentSkipListMap<String, HermesClientHandler>();
    }
    
    public String makeGUID(String seed)
    {
        try
        {
            MessageDigest sh = MessageDigest.getInstance("SHA-256");
            
            seed += "|" + System.currentTimeMillis();
            
            sh.update(seed.getBytes());
            byte byteData[] = sh.digest();
            StringBuffer sb = new StringBuffer(byteData.length);
            
            for(int i = 0; i < byteData.length; ++i)
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            
            return sb.toString();
        }
        catch( NoSuchAlgorithmException xe )
        {
            xe.printStackTrace();
        }
        
        return seed;
    }
    
    private int popID()
    {
        synchronized(_guid)
        {
            int id = -1;
            int tryCount = 0;
            
            while(++tryCount < 4)
            {
                int tmpId = _random.nextInt(_clientLimit);
                if( !_guid.get(tmpId) )
                {
                    id = tmpId;
                    break;
                }
            }
            
            if( id == -1 )
            {
                id = _guid.nextClearBit(0);
            }
            
            _guid.set(id, true);
            
            return id + 1;
        }
    }
    
    private void pushID(int id)
    {
        synchronized(_guid)
        {
            _guid.set(id - 1, false);
        }
    }
    
    //
    public String push(HermesClientHandler clientHandler)
    {
        int id = popID();
        String guid = String.format("%05d", id);    // makeGUID(clientHandler.getRmoteAddress());
        
        _clientMap.put(guid, clientHandler);
        
        return guid;
    }
    
    public void pop(String guid)
    {
        int id = Integer.parseInt(guid);

        _clientMap.remove(guid);
        
        pushID(id);
    }
    
    public HermesClientHandler get(String guid)
    {
        return _clientMap.get(guid);
    }
}
