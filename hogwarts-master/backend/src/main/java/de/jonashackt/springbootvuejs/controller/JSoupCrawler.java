package de.jonashackt.springbootvuejs.controller;

import de.jonashackt.springbootvuejs.domain.gdsrecordview;
import de.jonashackt.springbootvuejs.thread.CrawlGDSRecordThread;
import de.jonashackt.springbootvuejs.thread.CrawlGDSRecordThreadCompleteListener;

import java.io.IOException;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSoupCrawler{  
	private static final Logger LOG = LoggerFactory.getLogger(BackendController.class);
	private static String NOTFOUND = "No items found - Taxonomy - NCBI";
	private static String TAXONOMY_TITLE_SUFFIX = " - Taxonomy - NCBI";
	private boolean TIMEOUT=false;
	private static ArrayList<String> keywordsLeftToCheck;
	private ArrayList<String> organismList;
	private ArrayList<String> diseaseList;
	private ArrayList<String> anatomyList;
	private ArrayList<ArrayList<Integer>> gdsUidList;	
	private ArrayList<ArrayList<String>> umlsidList;
	private ArrayList<String> malacardidList;
	private ArrayList<String> malacarddiseaseList;
	
	
	public JSoupCrawler() {
		clearKeywordSummary();
    }

	public ArrayList<String> getMalacardidList()
	{
		return malacardidList;
	}

	public ArrayList<ArrayList<String>> getUMLSidList()
	{
		return umlsidList;
	}
	
	public ArrayList<ArrayList<Integer>> getGDSUidList()
	{
		return gdsUidList;
	}
	
	public ArrayList<String> getKeywordsToCheck()
	{
		return keywordsLeftToCheck;
	}
	
	public ArrayList<String> getOrganismArrayList()
	{
		return organismList;
	}
	
	public ArrayList<String> getDiseaseArrayList()
	{
		return diseaseList;
	}
	
	public ArrayList<String> getAnatomyArrayList()
	{
		return anatomyList;
	}
	
	public void clearKeywordSummary()
	{
		keywordsLeftToCheck=new ArrayList<String>();
		organismList=new ArrayList<String>();
		diseaseList=new ArrayList<String>();
		anatomyList=new ArrayList<String>();
		gdsUidList = new ArrayList<ArrayList<Integer>>();	
		umlsidList = new ArrayList<ArrayList<String>>();
		malacardidList = new ArrayList<String>();
		malacarddiseaseList = new ArrayList<String>();
	}
	
	/*public boolean crawlForOrganism(ArrayList<String> keywordList){  
		TIMEOUT=false;
		for(int i=0; i<keywordList.size(); i++)
			keywordsLeftToCheck.add(keywordList.get(i));
		
		//System.out.println("keywordList = "+keywordList.toString());
		for(int i=0; i<keywordList.size(); i++)
		{
			String keyword=keywordList.get(i);
			Document doc=new Document("");
			try {
				doc = Jsoup.connect("https://www.ncbi.nlm.nih.gov/taxonomy/?term="+keyword+"%5BCommon+Name%5D").timeout(20000).get();
				String title = doc.title();
				System.out.println("title="+title);
				if(title.compareTo(NOTFOUND)!=0)//potential organism found, extract it
				{
					int titleSuffixIndex=title.indexOf(TAXONOMY_TITLE_SUFFIX);
					if(titleSuffixIndex!=-1)
					{
						//LOG.info("Found valid organism " + keywordList.get(i));
						keywordsLeftToCheck.remove(keyword);
						Elements links = doc.select("p[class=\"title\"]");
						for(int j=0; j<links.size(); j++)
						{
							Elements href=links.get(j).select("a[href]");
							String hrefText=href.get(0).attr("href");
							String prefix="id=";
							int ncbi_uid=-1;
							LOG.info("organism hrefText=" + hrefText);
							int prefixIndex=hrefText.indexOf(prefix);
							if(prefixIndex!=-1)
								ncbi_uid=Integer.parseInt(hrefText.substring(prefixIndex+prefix.length()));
							LOG.info("organism ncbi_uid=" + ncbi_uid);
							organismList.add(links.get(j).text());
						}
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Timeout for crawl at NCBI taxonomy!");
				TIMEOUT=true;
			}
		}
		LOG.info("organismList: " + organismList.toString());
		LOG.info("keywordsLeftToCheck: " + keywordsLeftToCheck.toString());
		return true;
	} 
*/
	//perform minimal check to retrieve disease id for checking into DB if the disease retails have been crawled previously
	//and stored in database
	/*public boolean crawlForDisease(ArrayList<String> keywordList) {
		TIMEOUT=false;
		
		for(int i=0; i<keywordList.size(); i++)
		{
			String tempKeyword=keywordList.get(i), keyword="";
			if(tempKeyword.contains(" ")==true)
			{
				String[] idArr=tempKeyword.trim().split(" ");
				for(int a=0; a<idArr.length; a++)
				{
					if(a==0)
						keyword=idArr[a];
					else
						keyword=keyword+"%20"+idArr[a];
				}
			}
			else
				keyword=tempKeyword;
			Document doc=new Document("");
			try {
				//System.out.println("https://www.omim.org/search?index=entry&start=1&limit=10&sort=score+desc%2C+prefix_sort+desc&search="+keyword);
				//https://hpo.jax.org/app/browse/search?q=ovarian%20cancer&navFilter=term
				//System.out.println("https://www.ebi.ac.uk/ols/search?q=lung+cancer&groupField=iri&start=0&ontology=ncit");
				//doc = Jsoup.connect("https://hpo.jax.org/app/browse/search?q="+keyword+"&navFilter=term").timeout(200000).get();
				//doc = Jsoup.connect("https://www.ebi.ac.uk/ols/search?q=lung+cancer&groupField=iri&start=0&ontology=ncit").timeout(200000).get();

				System.out.println("https://www.malacards.org/search/results?query=lung+cancer");
				doc = Jsoup.connect("https://www.malacards.org/search/results?query=lung+cancer").timeout(200000).get();

				String title = doc.title();
				System.out.println("title="+title);
				Elements IDLinks = doc.select("mat-row[class=mat-row cdk-row ng-star-inserted]");
				String idLink, id="", disease="";
				if(IDLinks.size()>0)
				{
					idLink=IDLinks.get(0).id();
					System.out.println("idLink="+idLink);
					String[] idArr=idLink.trim().split("\\|");
					id=idArr[0];
					malacardidList.add(id);
					disease=idArr[2];
					malacarddiseaseList.add(disease);
					String formattedDisease="";
					String[] diseaseArr=disease.trim().split("_");
					for(int d=0; d<diseaseArr.length; d++)
					{
						if(d==0)
							formattedDisease=diseaseArr[d];
						else
							formattedDisease=formattedDisease+" "+diseaseArr[d];
					}
					diseaseList.add(formattedDisease);
					System.out.println("id="+id+" disease="+disease);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Timeout for crawl at HPO disease query keyword (minimal)!");
				LOG.info("Timeout for crawl at HPO disease query keyword (minimal)!");
				TIMEOUT=true;
			}
		}
		LOG.info("diseaseList: " + diseaseList.toString());
		return true;
	}*/

	/*public boolean crawlForAnatomy(ArrayList<String> keywordList) {
		TIMEOUT=false;
		
		for(int i=0; i<malacarddiseaseList.size(); i++)
		{
			String disease = malacarddiseaseList.get(i);
			String id = malacardidList.get(i);
			System.out.println("id="+id+" disease="+disease);
				
			Document docMala=new Document("");
			try {
				//https://www.malacards.org/card/breast_cancer?search=BRS047
				docMala = Jsoup.connect("https://www.malacards.org/card/"+disease+"?search="+id).timeout(200000).get();
				//docMala = Jsoup.connect("https://www.malacards.org/card/"+disease+"?search="+id).timeout(1000).get(); //for simulating timeout
				Elements extLinks = docMala.select("div[id^=ExternalId_item_]");
				System.out.println("extLinks num="+extLinks.size());
				for(int e=0; e<extLinks.size(); e++)
				{
					//get anatomy info via NCIT repository
					Elements l=extLinks.get(e).select("b[title=\"National Cancer Institute Thesaurus\"]");
					if(l.size()>0)
						getAnatomyFromNCIT(extLinks, e);
					//get related UMLS concept for the disease
					l=extLinks.get(e).select("b[title=\"Unified Medical Language System\"]");
					if(l.size()>0)
						getSynonymsConceptFromUMLS(extLinks, e);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Timeout for crawl at OMIM disease query keyword!");
				LOG.info("Timeout for crawl at OMIM disease query keyword!");
				TIMEOUT=true;
			}
		}
		LOG.info("anatomyList: " + anatomyList.toString());
		return true;
	}
	*/

	/*public ArrayList<String> crawlForAnatomy(String ncimid) {
		ArrayList<String> primaryAnatomyList=new ArrayList<String>();
		ArrayList<String> associatedAnatomyList=new ArrayList<String>();
		ArrayList<String> anatomyList=new ArrayList<String>();
		Document docNCIMeta=new Document("");
		try {
			docNCIMeta = Jsoup.connect("https://ncim.nci.nih.gov/ncimbrowser/pages/concept_details.jsf?dictionary=NCI%20Metathesaurus&code="+ncimid+"&type=relationship#Other").timeout(200000).get();
			LOG.info("[JSoupCrawler.java - crawlForAnatomy] ncimid="+ncimid);
			Elements content = docNCIMeta.select("div[class=\"tabTableContentContainer\"]");
			Element dataTable = null;
			Boolean FOUND = false;
			if(content.size()>0)
			{
				LOG.info("content size="+content.size());
				Elements content_children = content.get(0).children();
				LOG.info("content_children size="+content_children.size());
				Elements span = content.select("table[class=\"datatable_960\"]");
				LOG.info("datatable_960 span size="+span.size());
				LOG.info("span content="+span.get(5).text());
				//LOG.info("content size="+content.size());
				//LOG.info("content text="+content.get(0).text());
				//Elements content_children = content.get(0).children();
				//LOG.info("content_children size="+content_children.size());
				//for(int i=0; i<content_children.size() && !FOUND; i++)
				//{
				//	//LOG.info(i+" content_children=["+content_children.get(i).text()+"] "+content_children.get(i).data()+
				//	//	"%%%"+content_children.get(i).className()+"%%%"+content_children.get(i).cssSelector());
				//	if(content_children.get(i).text().compareTo("Other Relationships:")==0)
				//	{
				//		LOG.info(i+"FOUND content_children=["+content_children.get(i).text().substring(0,100)+"] ");
				//		dataTable = content_children.get(i+1);
				//		FOUND=true;
				//	}
				//}
				//LOG.info("FOUND="+FOUND);
				//Elements anatomy = dataTable.getElementsByTag("td");
				//LOG.info("anatomy size="+anatomy.size());
				//for(int c=0; c<anatomy.size(); c++)
				//{
				//	if(anatomy.get(c).text().compareTo("Disease_Has_Primary_Anatomic_Site")==0)
				//		primaryAnatomyList.add(anatomy.get(c+1).text());
				//	if(anatomy.get(c).text().compareTo("Disease_Has_Associated_Anatomic_Site")==0)
				//		associatedAnatomyList.add(anatomy.get(c+1).text());
				//	//LOG.info(c+" anatomy=["+anatomy.get(c).text()+"] "+anatomy.get(c).data()+
				//	//" "+anatomy.get(c).className()+" "+anatomy.get(c).cssSelector());
				//}
			}
			LOG.info("primaryAnatomyList="+primaryAnatomyList.toString());
			LOG.info("associatedAnatomyList="+associatedAnatomyList.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Timeout for crawl at NCI Metathesaurus for anatomy!");
			LOG.info("Timeout for crawl at NCI Metathesaurus for anatomy!");
			TIMEOUT=true;
		}
		if(primaryAnatomyList.size()>0)
			anatomyList=primaryAnatomyList;
		else
			anatomyList=associatedAnatomyList;
		LOG.info("anatomyList: " + anatomyList.toString());
		return anatomyList;
	}
*/

	private void getSynonymsConceptFromUMLS(Elements extLinks, int eleIndex) throws IOException{
		System.out.println("Found UMLS!! At "+extLinks.get(eleIndex).id());
		Elements span=extLinks.get(eleIndex).select("span");
		System.out.println("Found span = "+span.size());
		ArrayList<String> umlsid=new ArrayList<String>();
		if(span.size()>1)
		{
			for(int i=0; i<span.size(); i++)
			{
				if(i!=0 && i!=span.size()-1)
				{
					String currRecord = span.get(i).text();
					System.out.println("currRecord => "+currRecord);
					String[] splitStr = currRecord.trim().split("\\s+");
					for(int j=0; j<splitStr.length; j++)
					{
						umlsid.add(splitStr[j]);
						System.out.println("span => "+splitStr[j]);
					}
				}
			}
			umlsidList.add(umlsid);
		}
	}

	private void getAnatomyFromNCIT(Elements extLinks, int eleIndex) throws IOException
	{
		Document docNCIT=new Document("");
		String ncitID="";
		String associatedAnatomicSite="";
		String primaryAnatomicSite="";
		String DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE="Disease_Has_Associated_Anatomic_Site";
		String DISEASE_HAS_PRIMARY_ANATOMIC_SITE="Disease_Has_Primary_Anatomic_Site";
		String DISEASE_DISORDER_OR_FINDING="Disease, Disorder or Finding";
		
			
		System.out.println("Found NCIT!! At "+extLinks.get(eleIndex).id());
		Elements href=extLinks.get(eleIndex).select("a[href]");
		System.out.println("Found href = "+href.size());
		if(href.size()>1)
		{
			ncitID=href.get(1).text();
			//https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=NCI_Thesaurus&ns=ncit&type=relationship&code=C4872
			docNCIT = Jsoup.connect("https://ncit.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=NCI_Thesaurus&ns=ncit&type=relationship&code="+ncitID).timeout(200000).get();
			Elements cells = docNCIT.select("td[class=\"dataCellText\"]");
			System.out.println("cells num="+cells.size());
			
			for(int c=0; c<cells.size(); c++)
			{
				String cellText=cells.get(c).text();
				if(cellText.contains(DISEASE_HAS_PRIMARY_ANATOMIC_SITE)==true)
					primaryAnatomicSite=cells.get(c+1).text();
				if(cellText.contains(DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE)==true)
					associatedAnatomicSite=cells.get(c+1).text();
													
				//this is the stop tag....can stop searching
				if(cellText.contains(DISEASE_DISORDER_OR_FINDING)==true)
					c=cells.size()+1;
			}
			System.out.println("primaryAnatomicSite="+primaryAnatomicSite+" associatedAnatomicSite="+associatedAnatomicSite);
			if(primaryAnatomicSite!="")
				anatomyList.add(primaryAnatomicSite);
			else
			{
				if(associatedAnatomicSite!="")
					anatomyList.add(associatedAnatomicSite);
			}
		}
	}

	public boolean crawlForGEOOmnibusGDS(ArrayList<String> keywordList){
		TIMEOUT=false;
		Document doc=new Document("");
		
		for(int i=0; i<keywordList.size(); i++)
		{
			ArrayList<Integer> uidList=new ArrayList<Integer>();
			//ArrayList<String> uidList=new ArrayList<String>();
			try {
				//https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=gds&term=(breast+cancer)+AND+(human)+AND+gds[Filter]+&retmax=5000
				doc = Jsoup.connect("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=gds&term="+keywordList.get(i)+"+AND+gds[Filter]+&retmax=5000").timeout(20000).get();
				String title = doc.title();
				//System.out.println("title="+title);
				Elements resultCount=doc.select("Id");
				//System.out.println(resultCount.size());
				if(resultCount.size()>0)
				{
					for(int r=0; r<resultCount.size(); r++)
						uidList.add(Integer.parseInt(resultCount.get(r).text()));
				}
				gdsUidList.add(uidList);	
				//System.out.println(uidList.toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//System.out.println("Timeout for crawl at GEO Omnibus GDS!");
				TIMEOUT=true;
			}
		}
		return true;
	} 

	private String extractPlatformInfo(String s)
	{
		//E.g. of s:
		//Platform: GPL10558: Illumina HumanHT-12 V4.0 expression beadchip

		String delimiter=":";
		String platform="";

		int delimiter_index=s.indexOf(delimiter);
		if(delimiter_index!=-1)//can find the first :
		{
			s=s.substring(delimiter_index+1);
			//System.out.println("platform s=["+s+"]");
			delimiter_index=s.indexOf(delimiter);
			//System.out.println("delimiter_index=["+delimiter_index+"]");
			if(delimiter_index!=-1)
			{
				platform=s.substring(0, delimiter_index).trim();
				//System.out.println("platform=["+platform+"]");
			}
		}
		//System.out.println("platform=["+platform+"]");
		return platform;
	}

	private String extractAllFromDelimiter(String s, boolean LASTDELIMITER)
	{
		//E.g. of s:
		//[samplenum will extract all from last delimiter] Reference Series: GSE829  Sample count: 7
		//[title and summary extract all from first delimiter] Title: Spermatogonial stem cell activity in testis laminin binding germ cells (MG-U74A)
		//default is extraction from first delimiter

		String delimiter=":";
		String extractedDetails="";
		//System.out.println("s =["+s+"]");
		int delimiter_index;
		if(LASTDELIMITER)
		{
			delimiter_index=s.lastIndexOf(delimiter);
			//System.out.println("LASTDELIMITER delimiter_index =["+delimiter_index+"]");
		}
		else
		{
			delimiter_index=s.indexOf(delimiter);
			//System.out.println("!LASTDELIMITER delimiter_index =["+delimiter_index+"]");
		}
		if(delimiter_index!=-1)//can find the first :
		{
			extractedDetails=s.substring(delimiter_index+1).trim();
			//System.out.println("extractedDetails =["+extractedDetails+"]");
		}
		//System.out.println("extractedDetails=["+extractedDetails+"]");
		return extractedDetails;
	}

	private ArrayList<ArrayList<Integer>> getThreadLoad(int numThreads, ArrayList<Integer> intArrayList)
    {
        ArrayList<ArrayList<Integer>> thread_intArrayList=new ArrayList<ArrayList<Integer>>();
        int numBucketsWithExtras=intArrayList.size()%numThreads;
        int defaultSize=(intArrayList.size()-numBucketsWithExtras)/numThreads;
        int size;
        int count=0;
        for(int i=0; i<numThreads; i++)
        {
            if(i<numBucketsWithExtras)
                size=defaultSize+1;
            else
                size=defaultSize;
            ArrayList<Integer> thisThread_intArrayList=new ArrayList<Integer>();
            for(int j=0; j<size; j++)
				thisThread_intArrayList.add(intArrayList.get(count+j));
			thread_intArrayList.add(thisThread_intArrayList);
            count=count+size;
        }
        return thread_intArrayList;
    }

	public ArrayList<gdsrecordview> crawlForGEOOmnibusGDSItemList(String inputorganism, String inputdisease, ArrayList<Integer> gdsIDList){
		ArrayList<gdsrecordview> rList=new ArrayList<gdsrecordview>(); 
		Long startTime=System.currentTimeMillis();
		CrawlGDSRecordThreadCompleteListener threadListener=new CrawlGDSRecordThreadCompleteListener();
        ArrayList<Thread> threadList=new ArrayList<Thread>();
        int np=Runtime.getRuntime().availableProcessors();
		LOG.info("JSoupCrawler.java - np="+np);
		ArrayList<ArrayList<Integer>> threadload_gdsIDList=new ArrayList<ArrayList<Integer>>();
		
		//split file download load among the threads
		threadload_gdsIDList=getThreadLoad(np, gdsIDList);
		
		for(int i=0; i<np; i++)
        {
            CrawlGDSRecordThread t=new CrawlGDSRecordThread(i, threadListener, 
				inputorganism, inputdisease, threadload_gdsIDList.get(i));
            Thread gpThread=new Thread(t, t.getName());
            gpThread.start();
            threadList.add(gpThread);
        }
        try {
            for(int i=0; i<np; i++)
            {
                threadList.get(i).join();
				LOG.info("JSoupCrawler.java - "+threadList.get(i).getName()+" join");
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
		rList=threadListener.getGDSRecordList();
		return rList;
	} 

    public gdsrecordview crawlForGEOOmnibusGDSItem(String inputorganism, String inputdisease, int gdsID){
		TIMEOUT=false;
		Document doc=new Document("");
		int TITLE_LINE=1;
		int SUMMARY_LINE=2;
		int PLATFORM_LINE=4;
		int SAMPLENUM_LINE=6;
		String organism="", filepath="", samplenum="", platform="", title="", summary="";
		gdsrecordview r=new gdsrecordview(); 

		try {
			//PrintStream out = new PrintStream(new FileOutputStream("output_"+gdsID+".txt"));
			//System.setOut(out);
		    
			//https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=gds&term=(breast+cancer)+AND+(human)+AND+gds[Filter]+&retmax=5000
			doc = Jsoup.connect("https://www.ncbi.nlm.nih.gov/sites/GDSbrowser?acc=GDS"+gdsID).timeout(20000).get();
			//get organism
			Elements resultCount=doc.select("td[class=\"species\"]");
			if(resultCount.size()>0)
			{
				//LOG.info(resultCount.get(0).text());
				//System.out.println("ORGANISM");
				//System.out.println(resultCount.get(0).text());
				organism=resultCount.get(0).text();
			}
			resultCount=doc.select("table[id=\"gds_details\"]"); 
			Elements rows=resultCount.get(0).select("tr");
			if(rows.size()>0)
			{
				//row index 1 contains title
				//System.out.println("TITLE");
				//System.out.println(rows.get(TITLE_LINE).text());
				title=extractAllFromDelimiter(rows.get(TITLE_LINE).text(), false);
				//at the moment, title comes appended with "Cluster Analysis Download DataSet full SOFT file DataSet SOFT file Series family SOFT file Series family MINiML file Annotation SOFT file"
				String titleAppendixToRemove="Cluster Analysis Download DataSet full SOFT file DataSet SOFT file Series family SOFT file Series family MINiML file Annotation SOFT file";
				int titleAppendixIndex=title.indexOf(titleAppendixToRemove);
				if(titleAppendixIndex!=-1)
					title=title.substring(0,titleAppendixIndex);
				//row index 2 contains summary
				//System.out.println("SUMMARY");
				//System.out.println(rows.get(SUMMARY_LINE).text());
				summary=extractAllFromDelimiter(rows.get(SUMMARY_LINE).text(), false);
				//row index 4 or 5 contains platform info
				if(rows.size()==9)
				{
					PLATFORM_LINE=5;
					SAMPLENUM_LINE=7;
				}
				else if(rows.size()==7)
				{
					SAMPLENUM_LINE=5;
				}
				//System.out.println("PLATFORM");
				//System.out.println(rows.get(PLATFORM_LINE).text());
				platform=extractPlatformInfo(rows.get(PLATFORM_LINE).text());
				//System.out.println(platform);
				//row index 6 contains samplenum info
				//System.out.println("SAMPLENUM");
				//System.out.println(rows.get(SAMPLENUM_LINE).text());
				samplenum=extractAllFromDelimiter(rows.get(SAMPLENUM_LINE).text(), true);
				//System.out.println(samplenum);
			}
			//extract file name information
			resultCount=doc.select("td[id=\"detailsRightCol\"]"); 
			rows=resultCount.get(0).select("li");
			if(rows.size()>0)
			{
				Elements href=rows.get(0).select("a");
				filepath=href.attr("href");
			}
			//gdsItemDetailsList.add(gdsItemDetails);
			r=new gdsrecordview(gdsID, organism, title, summary, platform, Integer.parseInt(samplenum), filepath, inputorganism, inputdisease, true, false, false, false);
			LOG.info("r="+r.toString());
			//System.out.println("r="+r.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//System.out.println("Timeout for crawl at GEO Omnibus GDS item!");
			TIMEOUT=true;
		}
	//}
		return r;
	} 
}  

