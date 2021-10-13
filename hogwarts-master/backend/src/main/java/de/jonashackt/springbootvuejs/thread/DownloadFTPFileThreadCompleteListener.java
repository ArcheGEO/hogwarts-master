package de.jonashackt.springbootvuejs.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadFTPFileThreadCompleteListener implements TaskListener{
    private static final Logger LOG = LoggerFactory.getLogger(DownloadFTPFileThreadCompleteListener.class);
    private int totalNumberDownloadedFiles;

    public DownloadFTPFileThreadCompleteListener()
    {
        totalNumberDownloadedFiles=0;
    }
    
    public int getTotalNumberDownloadedFiles(){
        return totalNumberDownloadedFiles;
    }

    @Override
    public void threadComplete(Runnable runner) {
        DownloadFTPFileThread t=(DownloadFTPFileThread) runner;
        LOG.info("DownloadFTPFileThread notify completion: "+t.getName());
        this.totalNumberDownloadedFiles=this.totalNumberDownloadedFiles+t.getNumDownloadedFiles();
    }
}
