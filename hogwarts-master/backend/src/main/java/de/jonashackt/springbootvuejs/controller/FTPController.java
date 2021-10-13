package de.jonashackt.springbootvuejs.controller;

import java.io.File;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jonashackt.springbootvuejs.domain.gdsrecordmini;
import de.jonashackt.springbootvuejs.thread.DownloadFTPFileThread;
import de.jonashackt.springbootvuejs.thread.DownloadFTPFileThreadCompleteListener;

public class FTPController {
    private static final Logger LOG = LoggerFactory.getLogger(FTPController.class);

    public FTPController(){

    }
    
    public String download(ArrayList<gdsrecordmini> recordsToDownload, postgresController pgController, String currFolderName){
        Long startTime=System.currentTimeMillis();
        Long endTime;
		String msg="";
        DownloadFTPFileThreadCompleteListener threadListener=new DownloadFTPFileThreadCompleteListener();
        ArrayList<Thread> threadList=new ArrayList<Thread>();
        int np=Runtime.getRuntime().availableProcessors();
        if(recordsToDownload.size()<np)
            np=recordsToDownload.size();
        //int np=1;
		LOG.info("FTPController.java - np="+np);
		
		String downloadLocation=System.getProperty("user.home")+File.separator+"Downloads";
		File f = new File(downloadLocation);
		if (!f.exists())
			f.mkdir();
		LOG.info("FTPController.java - downloadLocation="+downloadLocation);
		ArrayList<String> filepathList_all=new ArrayList<String>();
		ArrayList<String> filepathList_missing=new ArrayList<String>();
		ArrayList<String> fileFullPathName_missing=new ArrayList<String>();
		ArrayList<ArrayList<String>> threadload_filepathList_missing=new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<String>> threadload_fileFullPathName_missing=new ArrayList<ArrayList<String>>();

		//check if download folder if present
		downloadLocation=downloadLocation+File.separator+currFolderName;
		LOG.info("FTPController.java - downloadLocation="+downloadLocation);
		f = new File(downloadLocation);
		if (!f.exists())
			f.mkdir();
		
		//retrieve filepaths from DB
		LOG.info("FTPController.java - recordsToDownload.size()="+recordsToDownload.size());
		filepathList_all=pgController.GDS_getFilePathsToDownload(recordsToDownload);//get filepath from DB
		LOG.info("FTPController.java - filepathList_all="+filepathList_all.toString());

		//check if file present
		for(int i=0; i<filepathList_all.size(); i++)
		{
			String ftpUrl=filepathList_all.get(i);
			String filename=extractFilenameFromFTPUrl(ftpUrl);
			String fileFullPathName=downloadLocation+File.separator+filename;
			f = new File(fileFullPathName);
			if (!f.exists())
			{
				fileFullPathName_missing.add(fileFullPathName);
				filepathList_missing.add(ftpUrl);
			}
		}

		//split file download load among the threads
		threadload_filepathList_missing=getThreadLoad(np, filepathList_missing);
		threadload_fileFullPathName_missing=getThreadLoad(np, fileFullPathName_missing);
		
        for(int i=0; i<threadload_filepathList_missing.size(); i++)
            LOG.info("backendcountroller.java - thread "+i+" ="+threadload_filepathList_missing.get(i).toString());

		for(int i=0; i<np; i++)
        {
            DownloadFTPFileThread t=new DownloadFTPFileThread(i, threadListener, 
				threadload_filepathList_missing.get(i), threadload_fileFullPathName_missing.get(i));
            Thread gpThread=new Thread(t, t.getName());
            gpThread.start();
            threadList.add(gpThread);
        }
        try {
            for(int i=0; i<np; i++)
            {
                threadList.get(i).join();
				LOG.info("FTPController.java - "+threadList.get(i).getName()+" join");
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        boolean SOME_THREAD_STILL_ALIVE=false;
        for(int i=0; i<threadList.size(); i++)
        {
            if(threadList.get(i).isAlive())
            {
                SOME_THREAD_STILL_ALIVE=true;
                i=threadList.size();
            }
        }
        while(SOME_THREAD_STILL_ALIVE)
        {
            try {
                Thread.sleep(10);
                for(int i=0; i<threadList.size(); i++)
                {
                    if(threadList.get(i).isAlive())
                    {
                        SOME_THREAD_STILL_ALIVE=true;
                        i=threadList.size();
                    }
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
		//finish up everything
		int numDownload=threadListener.getTotalNumberDownloadedFiles();
		msg=numDownload+" SOFT files downloaded to "+downloadLocation;
		if(numDownload<filepathList_missing.size())
			msg=msg+".\n"+(filepathList_missing.size()-numDownload)+
			" files did not complete download due to network issues.\n Please click DOWNLOAD SOFT button again to retry on these files.";
					
		LOG.info("FTPController.java - all download files threads are DEAD!!! can proceed");
		endTime=System.currentTimeMillis();
        LOG.info("++++++++++++ FTPController.java download - startTime="+startTime+" endTime="+endTime+" "+
			" duration="+(endTime-startTime)/1000+" s");

        return msg;
    }

    private String extractFilenameFromFTPUrl(String url)
	{
		String fName="";
		String delimiter="/";
		int delimiter_lastIndex=url.lastIndexOf(delimiter);
		LOG.info("backendcountroller.java - extractFilenameFromFTPUrl delimiter_lastIndex="+delimiter_lastIndex);
		if(delimiter_lastIndex!=-1)
			fName=url.substring(delimiter_lastIndex+1);
		return fName;
	}

	private ArrayList<ArrayList<String>> getThreadLoad(int numThreads, ArrayList<String> strArrayList)
    {
        ArrayList<ArrayList<String>> thread_strArrayList=new ArrayList<ArrayList<String>>();
        int numBucketsWithExtras=strArrayList.size()%numThreads;
        int defaultSize=(strArrayList.size()-numBucketsWithExtras)/numThreads;
        int size;
        int count=0;
        LOG.info("FTPController.java - getThreadLoad numBucketsWithExtras="+numBucketsWithExtras);
        LOG.info("FTPController.java - getThreadLoad defaultSize="+defaultSize);
        for(int i=0; i<numThreads; i++)
        {
            if(i<numBucketsWithExtras)
                size=defaultSize+1;
            else
                size=defaultSize;
            
            LOG.info("FTPController.java - getThreadLoad thread "+i+" => size="+size);
            ArrayList<String> thisThread_strArrayList=new ArrayList<String>();
            for(int j=0; j<size; j++)
				thisThread_strArrayList.add(strArrayList.get(count+j));
			thread_strArrayList.add(thisThread_strArrayList);
            LOG.info("FTPController.java - getThreadLoad thread "+i+" thread_strArrayList="+thread_strArrayList.toString());
            count=count+size;
            LOG.info("FTPController.java - getThreadLoad count "+count);
        }
        return thread_strArrayList;
    }
}
