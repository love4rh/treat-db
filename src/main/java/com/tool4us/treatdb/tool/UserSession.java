package com.tool4us.treatdb.tool;

import static com.tool4us.common.Util.UT;
import static com.tool4us.treatdb.task.JobQueue.JQ;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

import com.tool4us.treatdb.task.DBTask;

import lib.turbok.data.FileMapStore;



public class UserSession
{
	private String		_userToken = null;
	private String		_authCode = null;
	private long		_startTick = UT.tickCount();
	
	private boolean		_valid = true;
	
	private Map<String, FileMapStore>	_resultMap = null;
	
	private Map<String, Object[]>			_resultMeta = null;
	
	
	public UserSession(String userToken, String authCode)
	{
		_userToken = userToken;
		_authCode = authCode;
		
		_resultMap = new ConcurrentSkipListMap<String, FileMapStore>();
		_resultMeta = new ConcurrentSkipListMap<String, Object[]>();
	}
	
	public String getUserToken()
	{
		return _userToken;
	}
	
	public String getAuthCode()
	{
		return _authCode;
	}
	
	public long getStartTick()
	{
		return _startTick;
	}

	public void clear()
	{
		_valid = false;
		
		for(Entry<String, FileMapStore> elem : _resultMap.entrySet())
		{
			FileMapStore ds = elem.getValue();
			ds.clearAndDelete(true);
		}

		_resultMeta.clear();
		_resultMap.clear();
	}
	
	public void removeResult(String qid)
	{
		FileMapStore ds = _resultMap.remove(qid);
		
		if( ds != null )
		{
			ds.clearAndDelete(true);
		}
		
		_resultMeta.remove(qid);
	}
	
	public boolean isValidJob(String qid)
	{
		return _resultMap.containsKey(qid);
	}
	
	public boolean isValid()
	{
		return _valid;
	}
	
	public DBTask executeQuery(String query, String driver, String server, String account, String password) throws Exception
	{
		String queryID = UT.makeRandomeString(12);
		
		return JQ.pushFetchingJob(this, queryID, query, driver, server, account, password);
	}

	public void pushTaskResult(String qid, FileMapStore resultData, long elapsedTime)
	{
		_resultMap.put(qid, resultData);
	}
	
	public void doneTaskResult(String qid, FileMapStore resultData, long elapsedTime)
	{
		_resultMeta.put(qid, new Object[] { elapsedTime } );
	}
	
	public boolean isDoneResult(String qid)
	{
		return _resultMeta.containsKey(qid);
	}

	public FileMapStore getFetchRestul(String qid)
	{
		return _resultMap.get(qid);
	}
}
