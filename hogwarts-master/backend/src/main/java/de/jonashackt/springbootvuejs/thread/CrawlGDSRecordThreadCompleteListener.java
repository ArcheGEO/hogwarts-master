package de.jonashackt.springbootvuejs.thread;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jonashackt.springbootvuejs.domain.gdsrecordview;

public class CrawlGDSRecordThreadCompleteListener implements TaskListener{
    private static final Logger LOG = LoggerFactory.getLogger(CrawlGDSRecordThreadCompleteListener.class);
    private ArrayList<gdsrecordview> rViewList;

    public CrawlGDSRecordThreadCompleteListener()
    {
        rViewList=new ArrayList<gdsrecordview>();
    }
    
    public ArrayList<gdsrecordview> getGDSRecordList(){
        return rViewList;
    }

    @Override
    public void threadComplete(Runnable runner) {
        CrawlGDSRecordThread t=(CrawlGDSRecordThread) runner;
        LOG.info("CrawlGDSRecordThread notify completion: "+t.getName());
        ArrayList<gdsrecordview> thread_gdsrecordList=t.getGDSRecordList();
        for(int i=0; i<thread_gdsrecordList.size(); i++)
            rViewList.add(thread_gdsrecordList.get(i));
    }
}
