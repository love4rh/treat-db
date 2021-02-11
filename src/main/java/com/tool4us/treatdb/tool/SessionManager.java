package com.tool4us.treatdb.tool;

import static com.tool4us.treatdb.tool.PMLogBank.PB;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
// import java.util.concurrent.CopyOnWriteArrayList;

import lib.turbok.common.ITabularData;
import lib.turbok.data.RowIndexer;
import lib.turbok.data.RowIndexerManager;
import lib.turbok.data.RowOperatedStore;



/**
 * 세션별 데이터 핸들링 관리
 * 
 * @author mh9.kim
 */
public enum SessionManager
{
    SM;
    
    // User Token --> Category 별 선택 현황. 아무것도 없으면 전체 선택임.
    private Map<String, ArrayList<Set<String>>>   _categoryMap;
    
    private Map<String, RowIndexer> _selectorMap;
    
    private Map<String, RowOperatedStore> _dataStoreMap;
    
    
    private SessionManager()
    {
        _categoryMap = new ConcurrentSkipListMap<String, ArrayList<Set<String>>>();
        _selectorMap = new ConcurrentSkipListMap<String, RowIndexer>();
        _dataStoreMap = new ConcurrentSkipListMap<String, RowOperatedStore>();

        // _keyStatusList = new CopyOnWriteArrayList<String[]>();
    }
    
    public void prepareUserData(String userToken)
    {
        this.removeUserData(userToken);
    
        // logType, process, context
        ArrayList<Set<String>> filter = new ArrayList<Set<String>>();

        filter.add(null);
        filter.add(null);
        filter.add(null);

        _categoryMap.put(userToken, filter);
    }

    public void removeUserData(String userToken)
    {
        RowIndexer rowIdx = _selectorMap.remove(userToken);

        if( rowIdx != null )
            rowIdx.closeAndDelete();
        
        RowOperatedStore ds = _dataStoreMap.remove(userToken);
        if( ds != null )
            ds.clear();
        
        _categoryMap.remove(userToken);
    }
    
    public boolean setCategoryFilter(String userToken, String dataKey, int idx, String selected) throws Exception
    {
        ArrayList<Set<String>> filter = _categoryMap.get(userToken);
        if( filter == null )
            prepareUserData(userToken);

        filter = _categoryMap.get(userToken);
        if( filter == null )
            return false;
        
        Set<String> tmpSet = new TreeSet<String>();
        for(String key : selected.split(" "))
            tmpSet.add(key);
        
        filter.set(idx, tmpSet);
        
        RowIndexer rowIdx = getRowIndexer(dataKey, userToken);
        
        PMLogData pl = PB.getData(dataKey);
        if( pl == null )
            return false;
            
        ITabularData ds = pl.getDataStore();
        if( ds == null )
            return false;
        
        if( rowIdx == null )
        {
            rowIdx = (RowIndexer) RowIndexerManager.newIndexer(ds);
            _selectorMap.put(userToken, rowIdx);
        }

        rowIdx.initIndex();
        
        long realRowNum = 0;
        for(long row = 0; row < ds.getRowSize(); ++row)
        {
            boolean isOk = true;
            for(int c = 0; isOk && c < 3; ++c)
            {
                Set<String> cond = filter.get(c);
                isOk = cond == null || cond.contains((String) ds.getCell(3 + c, row));
            }

            if( isOk )
                rowIdx.setIndex(realRowNum++, row);
        }
        
        rowIdx.shrinkRowSize(realRowNum);

        _dataStoreMap.put(userToken, new RowOperatedStore(ds, rowIdx));
        
        return true;
    }

    // TODO dataKey 고려해야 할까?
    public RowIndexer getRowIndexer(String dataKey, String userToken)
    {
        return _selectorMap.get(userToken);
    }
    
    public ITabularData getDataStore(String dataKey, String userToken)
    {
        return _dataStoreMap.get(userToken);
    }
}
