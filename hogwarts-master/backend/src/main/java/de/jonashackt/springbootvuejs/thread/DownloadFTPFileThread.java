package de.jonashackt.springbootvuejs.thread;

import de.jonashackt.springbootvuejs.thread.NotificationThread;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadFTPFileThread extends NotificationThread
{
    private static final Logger LOG = LoggerFactory.getLogger(DownloadFTPFileThread.class);
    private String threadName;
    private int BUFFER_SIZE = 4096;
    private ArrayList<String> filepathList;
    private ArrayList<String> fullPathNameList;
    private int numDownloadedFiles=0;
    
    public DownloadFTPFileThread(int threadNum, DownloadFTPFileThreadCompleteListener listener, 
        ArrayList<String> filepathList, ArrayList<String> fullPathNameList)
    {
        this.filepathList=filepathList;
        this.fullPathNameList=fullPathNameList;
        super.addListener(listener);
        threadName="DOWNLOAD_FTPFILE_THREAD_#_"+threadNum;
        LOG.info("Initializing thread "+threadName);
    }
   
    public int getNumDownloadedFiles(){
        return numDownloadedFiles;
    }
    
    public String getName()
    {
        return threadName;
    }
    
    private void downloadFile(){
        for(int i=0; i<fullPathNameList.size(); i++)
		{
			String fileFullPathName=fullPathNameList.get(i);
			String ftpUrl=filepathList.get(i);
			LOG.info("DownloadFTPFileThread.java - fileFullPathName="+fileFullPathName);
			try {
				URL url = new URL(ftpUrl);
				URLConnection conn = url.openConnection();
				InputStream inputStream = conn.getInputStream();
	 
				FileOutputStream outputStream = new FileOutputStream(fileFullPathName);
	 
				byte[] buffer = new byte[BUFFER_SIZE];
				int bytesRead = -1;
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					outputStream.write(buffer, 0, bytesRead);
				}
	 
				outputStream.close();
				inputStream.close();
	 
                numDownloadedFiles++;
				LOG.info("DownloadFTPFileThread.java - DOWNLOADED "+ftpUrl);
			} catch (IOException ex) {
				LOG.info("DownloadFTPFileThread.java - error "+ex.getMessage());
                ex.printStackTrace();
			}
		}
    }

    @Override
    public void doWork() {
        //System.out.println("HERE:: "+threadName);
        LOG.info("doWork thread "+threadName);
        downloadFile();
    }
}
