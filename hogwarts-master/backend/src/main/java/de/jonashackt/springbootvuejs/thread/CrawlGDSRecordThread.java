package de.jonashackt.springbootvuejs.thread;

import de.jonashackt.springbootvuejs.controller.JSoupCrawler;
import de.jonashackt.springbootvuejs.domain.gdsrecordview;
import de.jonashackt.springbootvuejs.thread.NotificationThread;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrawlGDSRecordThread extends NotificationThread
{
    private static final Logger LOG = LoggerFactory.getLogger(CrawlGDSRecordThread.class);
    private String threadName;
    private ArrayList<Integer> gdsIDList;
    private ArrayList<gdsrecordview> rViewList=new ArrayList<gdsrecordview>();
    private JSoupCrawler jSoupCrawler=new JSoupCrawler();
    private String inputOrganism;
    private String inputDisease;
    
    public CrawlGDSRecordThread(int threadNum, CrawlGDSRecordThreadCompleteListener listener, 
        String inputOrganism, String inputDisease, ArrayList<Integer> gdsIDList)
    {
        this.gdsIDList=gdsIDList;
        this.inputOrganism=inputOrganism;
        this.inputDisease=inputDisease;
        super.addListener(listener);
        threadName="CRAWL_GDSRECORD_THREAD_#_"+threadNum;
        LOG.info("Initializing thread "+threadName);
    }
   
    public ArrayList<gdsrecordview> getGDSRecordList(){
        return rViewList;
    }
    
    public String getName()
    {
        return threadName;
    }
    
    private void crawlRecord(){
        for(int i=0; i<gdsIDList.size(); i++)
		{
            gdsrecordview rView=jSoupCrawler.crawlForGEOOmnibusGDSItem(inputOrganism, inputDisease, gdsIDList.get(i));
            LOG.info("CrawlGDSRecordThread.java - CRAWL completed for "+gdsIDList.get(i));
            rViewList.add(rView);
		}
    }

    @Override
    public void doWork() {
        //System.out.println("HERE:: "+threadName);
        LOG.info("doWork crawlRecord thread "+threadName);
        crawlRecord();
    }
}
