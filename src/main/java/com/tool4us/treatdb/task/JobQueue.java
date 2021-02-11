package com.tool4us.treatdb.task;

import static com.tool4us.treatdb.tool.PMLogBank.PB;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import com.tool4us.common.Logs;

import lib.turbok.task.ITask;
import lib.turbok.task.ITaskMonitor;
import lib.turbok.task.TaskQueue;



public enum JobQueue implements ITaskMonitor
{
    JQ;
    
    private TaskQueue   _taskMgr = null;
    
    private Map<String, Integer> _working = null;

    private JobQueue()
    {
        _taskMgr = new TaskQueue(this);
        _working = new ConcurrentSkipListMap<String, Integer>();
    }
    
    public void begin()
    {
        _taskMgr.startQueue(2, "pmlog-anal-jobq");
    }
    
    public void end()
    {
        _taskMgr.endQueue();
    }

    public void pushAnalysisJob(String logUrl, String key)
    {
        if( _working.containsKey(key) ) // 작업중임
        {
            Logs.info("Working now: {}", logUrl);
            return;
        }
        
        _working.put(key, 1);
        _taskMgr.pushTask( new AnalysisTask(logUrl, key) );
    }

    @Override
    public boolean isContinuing(ITask task)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void stopTask(ITask task)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void OnStartTask(ITask task)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean OnProgress(ITask task, long progress)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void OnEndTask(ITask task)
    {
        if( task instanceof AnalysisTask )
        {
            String key = ((AnalysisTask) task).getKey();
            _working.remove(key);
        }
    }

    @Override
    public void OnErrorRaised(ITask task, Throwable e)
    {
        if( e instanceof AnalysisJobException )
        {
            AnalysisJobException xe = (AnalysisJobException) e;
            
            PB.pushKeyStatus(xe.getKey(), xe.getError());
            _working.remove(xe.getKey());

            Logs.warn("Error occured: {}", task.toString());
            Logs.trace(xe);
        }
    }
}
