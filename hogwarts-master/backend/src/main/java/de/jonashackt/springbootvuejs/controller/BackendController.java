package de.jonashackt.springbootvuejs.controller;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import de.jonashackt.springbootvuejs.domain.gdsrecord;
import de.jonashackt.springbootvuejs.domain.gdsrecordview;
import de.jonashackt.springbootvuejs.domain.rulelist;
import de.jonashackt.springbootvuejs.domain.tabRecordParam;
import de.jonashackt.springbootvuejs.domain.gdsrecordinvalid;
import de.jonashackt.springbootvuejs.domain.gdsrecordmini;
import de.jonashackt.springbootvuejs.service.InterfaceGDSRecordViewService;
import de.jonashackt.springbootvuejs.service.InterfaceGDSRecordService;

@RestController
@RequestMapping("/api")
public class BackendController {

    private static final Logger LOG = LoggerFactory.getLogger(BackendController.class);

    public static final String HELLO_TEXT = "Hello from Spring Boot Backend!";
    public static final String SECURED_TEXT = "Hello from the secured resource!";
    JSoupCrawler crawler = new JSoupCrawler();
    ArrayList<String> organismList=new ArrayList<String>();
	ArrayList<String> diseaseList=new ArrayList<String>();
	ArrayList<String> anatomyList=new ArrayList<String>();
	ArrayList<String> malacardidList = new ArrayList<String>();
	ArrayList<ArrayList<String>> umlsidList = new ArrayList<ArrayList<String>>();
	ArrayList<String> generatedKeywordList = new ArrayList<String>();
	ArrayList<Integer> numValidatedRecords = new ArrayList<Integer>();
	ArrayList<ArrayList<Integer>> dlGDS;
	ArrayList<ArrayList<String>> organismDiseasePairList=new ArrayList<ArrayList<String>>();
	private FTPController ftpController=new FTPController();
	private postgresController pgController=new postgresController();
	private pythonController pyController= new pythonController();
	private Gson gson = new Gson();
	private String currFolderName="";

	@Autowired
	private InterfaceGDSRecordViewService GDSRecordViewService;

	@Autowired
    private InterfaceGDSRecordService GDSRecordService;

	@ResponseBody
    @RequestMapping(path = "/hello")
    public String sayHello() {
        LOG.info("GET called on /hello resource");
        return HELLO_TEXT;
    }

    @ResponseBody
    @RequestMapping(path="/secured", method = RequestMethod.GET)
    public String getSecured() {
        LOG.info("GET successfully called on /secured resource");
        return SECURED_TEXT;
    }
    
    private void initProcessKeyword() {
    	organismList=new ArrayList<String>();
    	diseaseList=new ArrayList<String>();
    	anatomyList=new ArrayList<String>();
    	crawler.clearKeywordSummary();
    }
    
	@ResponseBody
    @RequestMapping(path = "/processKeyword/{keyword}", method = RequestMethod.POST)
    public ArrayList<String> processKeyword(@PathVariable("keyword") String keyword) {
    	//boolean SUCCESS_organism=true, SUCCESS_disease=true;
		Long startTime=System.currentTimeMillis();
		Long endTime;
		initProcessKeyword();
    	
    	LOG.info("processKeyword keyword ="+keyword);
		String[] arr=keyword.trim().split("\\s*,\\s*");
    	ArrayList<String> keywordList = new ArrayList<String>(Arrays.asList(arr));
    	ArrayList<String> responseList=new ArrayList<String>();
    	
		String py_keywords="";
		for(int i=0; i<keywordList.size(); i++)
		{
			py_keywords=py_keywords+keywordList.get(i);
			if(i<keywordList.size()-1)
				py_keywords=py_keywords+",";
		}
		LOG.info("check for organism");
		//pyController.python_scispacy_extractDiseaseFromTitle(py_keywords);
		ArrayList<String> crawlerOrganismList=pgController.TH_checkForOrganism(keywordList);
		if(crawlerOrganismList.size()>0)
		{
			for(int i=0; i<crawlerOrganismList.size(); i++)
				organismList.add(crawlerOrganismList.get(i));
		}
		ArrayList<String> keywordListLeft=pgController.TH_getKeywordsToCheck(keywordList);
		LOG.info("keywordListLeft="+keywordListLeft.toString());
		diseaseList = pgController.TH_checkForDisease(keywordListLeft);
		
    	ArrayList<String> ncitidList = pgController.TH_getNCITIDList();
		anatomyList = pgController.TH_getAssociatedAnatomyExistingInDB(ncitidList);
		
		responseList.add("true");
    	responseList.add(getString(organismList));
    	responseList.add(getString(diseaseList));
    	responseList.add(getString(anatomyList));

        LOG.info("POST called on /processKeyword resource");
		endTime=System.currentTimeMillis();
		LOG.info("+++++++ processKeyword duration="+(endTime-startTime)/1000+" s");
		return responseList;
    }
    
    private String replaceSpaceWithPlus(String name)
    {
    	String formattedName="";
    	String[] nameArr=name.trim().split(" ");
    	for(int i=0; i<nameArr.length; i++)
    	{
    		if(i==0)
    			formattedName=nameArr[i];
    		else
    			formattedName=formattedName+"+"+nameArr[i];
    	}
    	
    	return "("+formattedName+")";
    }

    @ResponseBody
    @RequestMapping(path="/getGeoOmnibusGDS/{keyword}", method = RequestMethod.POST)
    public ArrayList<Object> getGeoOmnibusGDS(@PathVariable("keyword") String keyword) {
    	//responseList items
    	//#1="true" if successfully called and retrieved the gds values
    	//#2=size of dlGDS
    	//#3=message to be displayed on browser (List of Query [xxx organism, yyy disease] => # results)
    	//#4=tabHeader (ArrayList<String>)
    	//#4=dlGDS (ArrayList<ArrayList<String>>)
    	boolean SUCCESS_gds=true;
    	ArrayList<Object> responseList=new ArrayList<Object>();
		Long startTime=System.currentTimeMillis();
		Long endTime;
    	
    	System.out.println("getGeoOmnibusGDS organismList="+organismList.toString());
    	System.out.println("getGeoOmnibusGDS diseaseList="+diseaseList.toString());
    	generatedKeywordList=new ArrayList<String>();
		organismDiseasePairList=new ArrayList<ArrayList<String>>();
    	dlGDS=new ArrayList<ArrayList<Integer>>();
		numValidatedRecords=new ArrayList<Integer>();
    	//create pairwise mapping of organism-disease -> each pair is submitted as a search to GEOOmnibus
		if(organismList.size()>0)
		{
    		for(int i=0; i<organismList.size(); i++)
    		{
    			for(int j=0; j<diseaseList.size(); j++)
    			{
    				String organism=replaceSpaceWithPlus(organismList.get(i));
    				String disease=replaceSpaceWithPlus(diseaseList.get(j));
    				String GEOkeyword=organism+"+AND+"+disease;
    				LOG.info("GEOkeyword ="+GEOkeyword);
    				generatedKeywordList.add(GEOkeyword);
					ArrayList<String> organismDiseasePair=new ArrayList<String>();
					organismDiseasePair.add(organismList.get(i));
					organismDiseasePair.add(diseaseList.get(j));
					organismDiseasePairList.add(organismDiseasePair);
    			}
    		}
		}
		else
		{
			for(int j=0; j<diseaseList.size(); j++)
			{
				String disease=replaceSpaceWithPlus(diseaseList.get(j));
				String GEOkeyword=disease;
				LOG.info("GEOkeyword ="+GEOkeyword);
				generatedKeywordList.add(GEOkeyword);
				ArrayList<String> organismDiseasePair=new ArrayList<String>();
				organismDiseasePair.add("All organisms");
				organismDiseasePair.add(diseaseList.get(j));
				organismDiseasePairList.add(organismDiseasePair);
			}
		}
    	SUCCESS_gds=crawler.crawlForGEOOmnibusGDS(generatedKeywordList);
        LOG.info("POST successfully called on callGEOOmnibus resource");
        if(SUCCESS_gds)
        {
        	dlGDS=crawler.getGDSUidList();
        	
        	responseList.add("true");
        	responseList.add(dlGDS.size());
        	String message="";
        	ArrayList<String> tabHeader=new ArrayList<String>();
			int count=0;
			if(organismList.size()>0)
			{
				for(int i=0; i<organismList.size(); i++)
    			{
    				for(int j=0; j<diseaseList.size(); j++)
    				{
						if(count<dlGDS.size())
						{
							message=message+"\n"+"Query ["+organismList.get(i)+","+diseaseList.get(j)+"] => "+dlGDS.get(count++).size()+" GDS records";
        					tabHeader.add(organismList.get(i)+","+diseaseList.get(j));
						}
    				}
    			}
			}
			else
			{
				for(int j=0; j<diseaseList.size(); j++)
    			{
					if(count<dlGDS.size())
					{
						message=message+"\n"+"Query [All organisms,"+diseaseList.get(j)+"] => "+dlGDS.get(count++).size()+" GDS records";
        				tabHeader.add("All organisms,"+diseaseList.get(j));
					}
    			}
			}
			//GDSRecordService.deleteAll();
			//LOG.info("GDSRecordService deleteAll!");
        	responseList.add(message);
        	responseList.add(tabHeader);
        	//responseList.add(dlGDS);
			//responseList.add(organismList);
			//responseList.add(diseaseList);
        }
        else
        	responseList.add("false");
		endTime=System.currentTimeMillis();
		LOG.info("+++++++ getGeoOmnibusGDS duration="+(endTime-startTime)/1000+" s");
        LOG.info("responseList="+responseList.toString());
        return responseList;
    }
    
    private String getString(ArrayList<String> list)
    {
    	String retString="";
    	String extra="";
    	int maxDisplayItem=3;
    	int maxNum=list.size();
    	if(maxNum>maxDisplayItem)
    	{
    		maxNum=maxDisplayItem;
    		extra=", ...";
    	}
    	for(int i=0; i<maxNum; i++)
    	{
    		if(i==0)
    			retString=list.get(i);
    		else
    			retString=retString+","+list.get(i);
    	}
    	retString=retString+extra;
    	return retString;
    }

	private boolean checkOrganismMismatch(String expected, String observed)
	{
		if(expected==null || expected.length()==0)
			return false;
		else{
			if(expected!=null && observed!=null && !pgController.TH_isEquivalentOrganism(expected, observed))
				return true;
			else
				return false;
		}
	}

	//return TRUE if there is at least one common disease in expected_list and observed_list
	private boolean equivalentDisease(ArrayList<String> expected_list, ArrayList<String>observed_list)
	{
		ArrayList<String> tmp_expected = new ArrayList<String>();
		ArrayList<String> tmp_observed = new ArrayList<String>();
		for(int i=0; i<expected_list.size(); i++)
			tmp_expected.add(expected_list.get(i).toUpperCase());
		for(int i=0; i<observed_list.size(); i++)
			tmp_observed.add(observed_list.get(i).toUpperCase());
		tmp_expected.retainAll(tmp_observed);
		if(tmp_expected.size()>0)
			return true;
		else
			return false;
	}

	//private String checkDiseaseStatus_all(String expected, ArrayList<ArrayList<String>> observed)
	private String checkDiseaseStatus(String expected, ArrayList<ArrayList<String>> observed, 
		ArrayList<String> expected_anatomy, ArrayList<String> expected_diseaseDescendents_ncit)
	{
		String MISMATCH = "MISMATCH";
		String VALID = "VALID";
		String UNVERIFIED = "UNVERIFIED";
		ArrayList<String> observed_ncit=observed.get(0);
		ArrayList<String> observed_umlsDiseaseTerm=observed.get(1);
		ArrayList<String> observed_cellline=observed.get(2);
		ArrayList<String> observed_anatomy = observed.get(3);
		//ArrayList<String> expected_anatomy = pgController.GDS_getAssociatedAnatomy(expected);
		//retrieve 2 levels down of disease associated to input disease (variable 'expected')
		//ArrayList<String> expected_diseaseDescendents_ncit = pgController.TH_getDiseaseDescendents(expected);
		
		LOG.info("[BackendController] checkDiseaseStatus expected disease =" +expected);
		LOG.info("[BackendController] checkDiseaseStatus observed_ncit disease =" +observed_ncit.toString());
		LOG.info("[BackendController] checkDiseaseStatus observed_umlsDiseaseTerm disease =" +observed_umlsDiseaseTerm.toString());
		LOG.info("[BackendController] checkDiseaseStatus expected_diseaseDescendents_ncit disease =" +expected_diseaseDescendents_ncit.toString());
						
		if(expected.contains("'"))
			expected = expected.replace("'", "&apos;");
			
		if(observed_ncit.size()>0)
		{
			//observed_ncit is not empty and does not contain the expected, so must be mismatch
			if(!observed_ncit.contains(expected.toUpperCase()) && !equivalentDisease(expected_diseaseDescendents_ncit, observed_ncit))
				return MISMATCH;
			else//observed_ncit is not empty and contains the expected, so must be valid disease
				return VALID;
		}
		else//observed_ncit is empty
		{
			if(observed_umlsDiseaseTerm.size()>0)
			{
				if(!observed_umlsDiseaseTerm.contains(expected.toUpperCase()) && !equivalentDisease(expected_diseaseDescendents_ncit, observed_umlsDiseaseTerm))
				{
					if(observed_cellline.size()>0)
					{
						if(!observed_cellline.contains(expected.toUpperCase()))
						{
							if(observed_anatomy.size()>0 && expected_anatomy.size()>0)
							{
								observed_anatomy.retainAll(expected_anatomy);
								if(observed_anatomy.size()>0)
									return VALID;
								else
									return MISMATCH;
							}
							else
								return UNVERIFIED;
								//should move on now to check using description field
						}
						else
							return VALID;
					}
					else
					{
						if(observed_anatomy.size()>0 && expected_anatomy.size()>0)
						{
							observed_anatomy.retainAll(expected_anatomy);
							if(observed_anatomy.size()>0)
								return VALID;
							else
								return MISMATCH;
						}
						else
							return UNVERIFIED;
							//should move on now to check using description field
					}
				}
				else
					return VALID;
			}
			else
			{
				if(observed_cellline.size()>0)
				{
					if(!observed_cellline.contains(expected.toUpperCase()))
					{
						if(observed_anatomy.size()>0 && expected_anatomy.size()>0)
						{
							observed_anatomy.retainAll(expected_anatomy);
							if(observed_anatomy.size()>0)
								return VALID;
							else
								return MISMATCH;
						}
						else
							return UNVERIFIED;
							//should move on now to check using description field
					}
					else
						return VALID;
				}
				else
				{
					if(observed_anatomy.size()>0 && expected_anatomy.size()>0)
					{
						observed_anatomy.retainAll(expected_anatomy);
						if(observed_anatomy.size()>0)
							return VALID;
						else
							return MISMATCH;
					}
					else
						return UNVERIFIED;
						//should move on now to check using description field
				}
			}
		}
	}

	/*private String checkDiseaseStatus_ExcludeSynonym(String expected, ArrayList<ArrayList<String>> observed, 
		ArrayList<String> expected_anatomy, ArrayList<String> expected_diseaseDescendents_ncit)
	{
		String MISMATCH = "MISMATCH";
		String VALID = "VALID";
		String UNVERIFIED = "UNVERIFIED";
		ArrayList<String> observed_ncit=observed.get(0);
		ArrayList<String> observed_umlsDiseaseTerm=observed.get(1);
		ArrayList<String> observed_cellline=observed.get(2);
		ArrayList<String> observed_anatomy = observed.get(3);
		//ArrayList<String> expected_anatomy = pgController.GDS_getAssociatedAnatomy(expected);
		
		LOG.info(observed_ncit.toString());
		LOG.info(observed_umlsDiseaseTerm.toString());
						
		
			if(observed_umlsDiseaseTerm.size()>0)
			{
				if(!observed_umlsDiseaseTerm.contains(expected.toUpperCase()))
				{
					if(observed_cellline.size()>0)
					{
						if(!observed_cellline.contains(expected.toUpperCase()))
						{
							if(observed_anatomy.size()>0 && expected_anatomy.size()>0)
							{
								observed_anatomy.retainAll(expected_anatomy);
								if(observed_anatomy.size()>0)
									return VALID;
								else
									return MISMATCH;
							}
							else
								return UNVERIFIED;
								//should move on now to check using description field
						}
						else
							return VALID;
					}
					else
					{
						if(observed_anatomy.size()>0 && expected_anatomy.size()>0)
						{
							observed_anatomy.retainAll(expected_anatomy);
							if(observed_anatomy.size()>0)
								return VALID;
							else
								return MISMATCH;
						}
						else
							return UNVERIFIED;
							//should move on now to check using description field
					}
				}
				else
					return VALID;
			}
			else
			{
				if(observed_cellline.size()>0)
				{
					if(!observed_cellline.contains(expected.toUpperCase()))
					{
						if(observed_anatomy.size()>0 && expected_anatomy.size()>0)
						{
							observed_anatomy.retainAll(expected_anatomy);
							if(observed_anatomy.size()>0)
								return VALID;
							else
								return MISMATCH;
						}
						else
							return UNVERIFIED;
							//should move on now to check using description field
					}
					else
						return VALID;
				}
				else
				{
					if(observed_anatomy.size()>0 && expected_anatomy.size()>0)
					{
						observed_anatomy.retainAll(expected_anatomy);
						if(observed_anatomy.size()>0)
							return VALID;
						else
							return MISMATCH;
					}
					else
						return UNVERIFIED;
						//should move on now to check using description field
				}
			}
		
	}*/

	/*//private String checkDiseaseStatus_ExcludeAnatomy(String expected, ArrayList<ArrayList<String>> observed)
	private String checkDiseaseStatus(String expected, ArrayList<ArrayList<String>> observed, ArrayList<String> expected_anatomy, 
		ArrayList<String> expected_diseaseDescendents_ncit)
	{
		String MISMATCH = "MISMATCH";
		String VALID = "VALID";
		String UNVERIFIED = "UNVERIFIED";
		ArrayList<String> observed_ncit=observed.get(0);
		ArrayList<String> observed_umlsDiseaseTerm=observed.get(1);
		ArrayList<String> observed_cellline=observed.get(2);
		ArrayList<String> observed_anatomy = observed.get(3);
		//ArrayList<String> expected_anatomy = pgController.GDS_getAssociatedAnatomy(expected);
		
		LOG.info(observed_ncit.toString());
		LOG.info(observed_umlsDiseaseTerm.toString());
						
		if(observed_ncit.size()>0)
		{
			//observed_ncit is not empty and does not contain the expected, so must be mismatch
			if(!observed_ncit.contains(expected.toUpperCase()))
				return MISMATCH;
			else//observed_ncit is not empty and contains the expected, so must be valid disease
				return VALID;
		}
		else//observed_ncit is empty
		{
			if(observed_umlsDiseaseTerm.size()>0)
			{
				if(!observed_umlsDiseaseTerm.contains(expected.toUpperCase()))
				{
					if(observed_cellline.size()>0)
					{
						if(!observed_cellline.contains(expected.toUpperCase()))
						{
							return UNVERIFIED;
								//should move on now to check using description field
						}
						else
							return VALID;
					}
					else
					{
						return UNVERIFIED;
							//should move on now to check using description field
					}
				}
				else
					return VALID;
			}
			else
			{
				if(observed_cellline.size()>0)
				{
					if(!observed_cellline.contains(expected.toUpperCase()))
					{
						return UNVERIFIED;
							//should move on now to check using description field
					}
					else
						return VALID;
				}
				else
				{
					return UNVERIFIED;
						//should move on now to check using description field
				}
			}
		}
	}*/

	/*//private String checkDiseaseStatus_ExcludeCellline(String expected, ArrayList<ArrayList<String>> observed)
	private String checkDiseaseStatus(String expected, ArrayList<ArrayList<String>> observed,
		ArrayList<String> expected_anatomy, ArrayList<String> expected_diseaseDescendents_ncit)
	{
		String MISMATCH = "MISMATCH";
		String VALID = "VALID";
		String UNVERIFIED = "UNVERIFIED";
		ArrayList<String> observed_ncit=observed.get(0);
		ArrayList<String> observed_umlsDiseaseTerm=observed.get(1);
		ArrayList<String> observed_cellline=observed.get(2);
		ArrayList<String> observed_anatomy = observed.get(3);
		//ArrayList<String> expected_anatomy = pgController.GDS_getAssociatedAnatomy(expected);
		
		LOG.info(observed_ncit.toString());
		LOG.info(observed_umlsDiseaseTerm.toString());
						
		if(observed_ncit.size()>0)
		{
			//observed_ncit is not empty and does not contain the expected, so must be mismatch
			if(!observed_ncit.contains(expected.toUpperCase()))
				return MISMATCH;
			else//observed_ncit is not empty and contains the expected, so must be valid disease
				return VALID;
		}
		else//observed_ncit is empty
		{
			if(observed_umlsDiseaseTerm.size()>0)
			{
				if(!observed_umlsDiseaseTerm.contains(expected.toUpperCase()))
				{
					if(observed_anatomy.size()>0 && expected_anatomy.size()>0)
					{
						observed_anatomy.retainAll(expected_anatomy);
						if(observed_anatomy.size()>0)
							return VALID;
						else
							return MISMATCH;
					}
					else
						return UNVERIFIED;
						//should move on now to check using description field
				}
				else
					return VALID;
			}
			else
			{
				if(observed_anatomy.size()>0 && expected_anatomy.size()>0)
				{
					observed_anatomy.retainAll(expected_anatomy);
					if(observed_anatomy.size()>0)
						return VALID;
					else
						return MISMATCH;
				}
				else
					return UNVERIFIED;
			}
		}
	}*/

	/*@ResponseBody
	@RequestMapping(path="/populateDBWithGDS", method=RequestMethod.GET)
    //remember to return in JSON format because vue expects the data in JSON format
	//public String populateDBWithGDS_ExcludeOrganism() {
	public String populateDBWithGDS() {
		Long startTime=System.currentTimeMillis();
		Long endTime;
		LOG.info("backendcountroller.java - populateDBWithGDS ");
		GDSRecordViewService.deleteAll();
		LOG.info("backendcountroller.java - deleteAll ");
		pgController.GDS_resetSequence("viewseq");
		LOG.info("backendcountroller.java - resetSequence ");
		gdsrecord r;
		gdsrecordview rView;
		gdsrecordmini rMini;
		ArrayList<ArrayList<gdsrecordmini>> responseList=new ArrayList<ArrayList<gdsrecordmini>>();
		int counter=0;
		for(int i=0; i<organismList.size(); i++)
		{
			ArrayList<String> organismSynonymList=new ArrayList<String>();
			organismSynonymList=pgController.TH_getOrganismSynonym(organismList.get(i));
			LOG.info("backendcountroller.java - organismSynonymList="+organismSynonymList.toString());
			String currExpectedOrganism=organismList.get(i);
			String currObservedOrganism="";
			for(int j=0; j<diseaseList.size(); j++)
			{
				//LOG.info("currExpectedOrganism="+currExpectedOrganism);
				//System.out.println("currOrganism="+currOrganism);
				String currExpectedDisease = diseaseList.get(j);
				ArrayList<String> expected_anatomy = pgController.GDS_getAssociatedAnatomy(currExpectedDisease);
				//retrieve 2 levels down of disease associated to input disease (variable 'expected')
				ArrayList<String> expected_diseaseDescendents_ncit = pgController.TH_getDiseaseDescendents(currExpectedDisease);
		
				ArrayList<ArrayList<String>> currObservedDiseaseArrayList_title = new ArrayList<ArrayList<String>>();
				ArrayList<ArrayList<String>> currObservedDiseaseArrayList_description = new ArrayList<ArrayList<String>>();
				ArrayList<gdsrecordmini> responseItem=new ArrayList<gdsrecordmini>();
				ArrayList<gdsrecordview> rViewList=new ArrayList<gdsrecordview>();
				ArrayList<Integer> currGDSList=dlGDS.get(counter++);
				//check and obtain currGDSList.get(k) from postgres DB if available
				ArrayList<Integer> foundGDS=pgController.GDS_getGDSIDPresentInDB(currGDSList);
				LOG.info("backendcountroller.java - foundGDS.size()="+foundGDS.size());
				currGDSList.removeAll(foundGDS);
				LOG.info("backendcountroller.java - currGDSList.size()="+currGDSList.size());
				LOG.info("backendcountroller.java - currGDSList="+currGDSList.toString());

				if(foundGDS.size()>0)
				{
					rViewList=pgController.GDS_copyGDSDetailsToView(foundGDS, organismSynonymList, currExpectedDisease);
					LOG.info("backendcountroller.java - responseItem.size()="+responseItem.size());
				}
				if(currGDSList.size()>0)
				{
					ArrayList<gdsrecordview> rViewList_additional=crawler.crawlForGEOOmnibusGDSItemList(currExpectedOrganism, currExpectedDisease, currGDSList);
					for(int k=0; k<rViewList_additional.size(); k++)
					{
						rView=rViewList_additional.get(k);
						rViewList.add(rView);
						r = new gdsrecord(rView.getGdsid(), rView.getOrganism(), rView.getTitle(), rView.getSummary(), rView.getPlatform(), rView.getSampleNum(), rView.getFName());
						GDSRecordService.save(r);
					}
				}	
				if(rViewList.size()>0)
				{
					currObservedDiseaseArrayList_title=new ArrayList<ArrayList<String>>();
					currObservedDiseaseArrayList_description = new ArrayList<ArrayList<String>>();
					for(int k=0; k<rViewList.size(); k++)
					{
						String validity="Valid";
						rView=rViewList.get(k);
						//String title=rView.getTitle();
						//LOG.info("rView="+rView.getGdsid());
						//pyController.python_scispacy_extractDiseaseFromTitle(title);
						currObservedOrganism=rView.getOrganism();
						currObservedDiseaseArrayList_title=pgController.GDS_getAssociatedDiseaseList_Title(rView.getGdsid());
						currObservedDiseaseArrayList_description=pgController.GDS_getAssociatedDiseaseList_Description(rView.getGdsid());
						//LOG.info("currObservedDiseaseList="+currObservedDiseaseList.toString());
						//LOG.info("currExpectedOrganism="+currExpectedOrganism);
						//LOG.info("currObservedOrganism="+currObservedOrganism);
						//if(currExpectedOrganism!=null && currObservedOrganism!=null && currExpectedOrganism.compareTo(currObservedOrganism)!=0)
						LOG.info(rView.getGdsid()+"-------------------------------");
						Boolean organismMismatch = 	checkOrganismMismatch(currExpectedOrganism, currObservedOrganism);
						String diseaseStatus_title = checkDiseaseStatus(currExpectedDisease, currObservedDiseaseArrayList_title, expected_anatomy, expected_diseaseDescendents_ncit);
						String diseaseStatus_description = checkDiseaseStatus(currExpectedDisease, currObservedDiseaseArrayList_description, expected_anatomy, expected_diseaseDescendents_ncit);
						if(diseaseStatus_title.compareTo("VALID")==0 || 
							(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("VALID")==0))
						//if(diseaseStatus_title.compareTo("VALID")==0 || diseaseStatus_description.compareTo("VALID")==0)
						{
							LOG.info(":):):) ["+rView.getGdsid()+"] SET AS VALID!");
							validity="Organism Unverified, Disease Valid";
							rMini=new gdsrecordmini(rView.getGdsid(), rView.getOrganism(), rView.getPlatform(), rView.getSampleNum(), validity, organismList.get(i), diseaseList.get(j));
							responseItem.add(rMini);
						}
						else if(diseaseStatus_title.compareTo("MISMATCH")==0 ||
							(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("MISMATCH")==0))
						{
							LOG.info("**** ["+rView.getGdsid()+"] SET AS INVALID DUE TO DISEASE MISMATCH!");
							rView.setValidRecord(false);	
							rView.setDiseaseMismatch(true);
							validity="Organism Unverified, Disease Mismatch";
						}
						else
						//if(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("UNVERIFIED")==0)
						{
							LOG.info("++++ ["+rView.getGdsid()+"] SET AS VALID DUE TO ORG VALID, DISEASE UNVERIFIED!");
							rView.setDiseaseUnverified(true);
							validity="Organism Unverified, Disease Unverified";
							rMini=new gdsrecordmini(rView.getGdsid(), rView.getOrganism(), rView.getPlatform(), rView.getSampleNum(), validity, organismList.get(i), diseaseList.get(j));
							responseItem.add(rMini);
						}
						GDSRecordViewService.save(rView);
					}
				}
				responseList.add(responseItem);
				numValidatedRecords.add(responseItem.size());
			}
		}
		endTime=System.currentTimeMillis();
		LOG.info("+++++++ populateDBWithGDS duration="+(endTime-startTime)/1000+" s");
		LOG.info("JSON:" + gson.toJson(responseList));
		return gson.toJson(responseList);
	}*/

	private ArrayList<ArrayList<gdsrecordmini>> populateDBWithGDS_forOneOrMoreOrganism()
	{
		ArrayList<ArrayList<gdsrecordmini>> responseList=new ArrayList<ArrayList<gdsrecordmini>>();
		gdsrecord r;
		gdsrecordview rView;
		gdsrecordmini rMini;
		int counter=0;
		

		for(int i=0; i<organismList.size(); i++)
		{
			ArrayList<String> organismSynonymList=new ArrayList<String>();
			organismSynonymList=pgController.TH_getOrganismSynonym(organismList.get(i));
			LOG.info("backendcountroller.java - organismSynonymList="+organismSynonymList.toString());
			String currExpectedOrganism=organismList.get(i);
			String currObservedOrganism="";
			for(int j=0; j<diseaseList.size(); j++)
			{
				LOG.info("**** populateDBWithGDS_forOneOrMoreOrganism");
				//LOG.info("currExpectedOrganism="+currExpectedOrganism);
				//System.out.println("currOrganism="+currOrganism);
				String currExpectedDisease = diseaseList.get(j);
				ArrayList<String> expected_anatomy = pgController.GDS_getAssociatedAnatomy(currExpectedDisease);
				//retrieve 2 levels down of disease associated to input disease (variable 'expected')
				ArrayList<String> expected_diseaseDescendents_ncit = pgController.TH_getDiseaseDescendents(currExpectedDisease);
		
				ArrayList<ArrayList<String>> currObservedDiseaseArrayList_title = new ArrayList<ArrayList<String>>();
				ArrayList<ArrayList<String>> currObservedDiseaseArrayList_description = new ArrayList<ArrayList<String>>();
				ArrayList<gdsrecordmini> responseItem=new ArrayList<gdsrecordmini>();
				ArrayList<gdsrecordview> rViewList=new ArrayList<gdsrecordview>();
				ArrayList<Integer> currGDSList=dlGDS.get(counter++);
				//check and obtain currGDSList.get(k) from postgres DB if available
				ArrayList<Integer> foundGDS=pgController.GDS_getGDSIDPresentInDB(currGDSList);
				LOG.info("backendcountroller.java - foundGDS.size()="+foundGDS.size());
				currGDSList.removeAll(foundGDS);
				LOG.info("backendcountroller.java - currGDSList.size()="+currGDSList.size());
				LOG.info("backendcountroller.java - currGDSList="+currGDSList.toString());

				if(foundGDS.size()>0)
				{
					rViewList=pgController.GDS_copyGDSDetailsToView(foundGDS, organismSynonymList, currExpectedDisease);
					LOG.info("backendcountroller.java - responseItem.size()="+responseItem.size());
				}
				if(currGDSList.size()>0)
				{
					ArrayList<gdsrecordview> rViewList_additional=crawler.crawlForGEOOmnibusGDSItemList(currExpectedOrganism, currExpectedDisease, currGDSList);
					for(int k=0; k<rViewList_additional.size(); k++)
					{
						rView=rViewList_additional.get(k);
						rViewList.add(rView);
						r = new gdsrecord(rView.getGdsid(), rView.getOrganism(), rView.getTitle(), rView.getSummary(), rView.getPlatform(), rView.getSampleNum(), rView.getFName());
						GDSRecordService.save(r);
					}
				}	
				if(rViewList.size()>0)
				{
					currObservedDiseaseArrayList_title=new ArrayList<ArrayList<String>>();
					currObservedDiseaseArrayList_description = new ArrayList<ArrayList<String>>();
					for(int k=0; k<rViewList.size(); k++)
					{
						String validity="Valid";
						rView=rViewList.get(k);
						//String title=rView.getTitle();
						//LOG.info("rView="+rView.getGdsid());
						//pyController.python_scispacy_extractDiseaseFromTitle(title);
						currObservedOrganism=rView.getOrganism();
						currObservedDiseaseArrayList_title=pgController.GDS_getAssociatedDiseaseList_Title(rView.getGdsid());
						currObservedDiseaseArrayList_description=pgController.GDS_getAssociatedDiseaseList_Description(rView.getGdsid());
						//LOG.info("currObservedDiseaseList="+currObservedDiseaseList.toString());
						//LOG.info("currExpectedOrganism="+currExpectedOrganism);
						//LOG.info("currObservedOrganism="+currObservedOrganism);
						//if(currExpectedOrganism!=null && currObservedOrganism!=null && currExpectedOrganism.compareTo(currObservedOrganism)!=0)
						LOG.info(rView.getGdsid()+"-------------------------------");
						Boolean organismMismatch = 	checkOrganismMismatch(currExpectedOrganism, currObservedOrganism);
						String diseaseStatus_title = checkDiseaseStatus(currExpectedDisease, currObservedDiseaseArrayList_title, expected_anatomy, expected_diseaseDescendents_ncit);
						String diseaseStatus_description = checkDiseaseStatus(currExpectedDisease, currObservedDiseaseArrayList_description, expected_anatomy, expected_diseaseDescendents_ncit);
						if(organismMismatch)
						{
							LOG.info("organism FAILLL 1 or more org---------------- ");
							rView.setValidRecord(false);	
							rView.setOrganismMismatch(true);
							if(diseaseStatus_title.compareTo("VALID")==0 || 
								(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("VALID")==0))
							//if(diseaseStatus_title.compareTo("VALID")==0 || diseaseStatus_description.compareTo("VALID")==0)
							{
								LOG.info("---- ["+rView.getGdsid()+"] SET AS INVALID DUE TO ORG MISMATCH!");
								validity="Organism Mismatch, Disease Valid";
							}
							else if(diseaseStatus_title.compareTo("MISMATCH")==0 ||
								(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("MISMATCH")==0))
							{
								LOG.info("!!!! ["+rView.getGdsid()+"] SET AS INVALID DUE TO ORG and DISEASE MISMATCH!");
								rView.setDiseaseMismatch(true);
								validity="Organism and Disease Mismatch";
							}
							else 
							//(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("UNVERIFIED")==0)
							{
								LOG.info("???? ["+rView.getGdsid()+"] SET AS INVALID DUE TO ORG MISMATCH, DISEASE UNVERIFIED!");
								rView.setDiseaseUnverified(true);
								validity="Organism Mismatch, Disease Unverified";
							}
						}
						else
						{
							LOG.info("organism PASSSSS 1 or more org+++++++");
							if(diseaseStatus_title.compareTo("VALID")==0 || 
								(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("VALID")==0))
							//if(diseaseStatus_title.compareTo("VALID")==0 || diseaseStatus_description.compareTo("VALID")==0)
							{
								LOG.info(":):):) ["+rView.getGdsid()+"] SET AS VALID!");
								validity="Valid";
								rMini=new gdsrecordmini(rView.getGdsid(), rView.getOrganism(), rView.getPlatform(), rView.getSampleNum(), validity, organismList.get(i), diseaseList.get(j));
								responseItem.add(rMini);
							}
							else if(diseaseStatus_title.compareTo("MISMATCH")==0 ||
								(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("MISMATCH")==0))
							{
								LOG.info("**** ["+rView.getGdsid()+"] SET AS INVALID DUE TO DISEASE MISMATCH!");
								rView.setValidRecord(false);	
								rView.setDiseaseMismatch(true);
								validity="Organism Valid, Disease Mismatch";
							}
							else
							//if(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("UNVERIFIED")==0)
							{
								LOG.info("++++ ["+rView.getGdsid()+"] SET AS VALID DUE TO ORG VALID, DISEASE UNVERIFIED!");
								rView.setDiseaseUnverified(true);
								validity="Organism Valid, Disease Unverified";
								rMini=new gdsrecordmini(rView.getGdsid(), rView.getOrganism(), rView.getPlatform(), rView.getSampleNum(), validity, organismList.get(i), diseaseList.get(j));
								responseItem.add(rMini);
							}
						}
						GDSRecordViewService.save(rView);
					}
				}
				responseList.add(responseItem);
				numValidatedRecords.add(responseItem.size());
			}
		}
		return responseList;
	}

	private ArrayList<ArrayList<gdsrecordmini>> populateDBWithGDS_forNoOrganism()
	{
		ArrayList<ArrayList<gdsrecordmini>> responseList=new ArrayList<ArrayList<gdsrecordmini>>();
		gdsrecord r;
		gdsrecordview rView;
		gdsrecordmini rMini;
		int counter=0;
			String currExpectedOrganism="";
			String currObservedOrganism="";
			ArrayList<String> organismSynonymList=new ArrayList<String>();
			organismSynonymList.add("All organisms");
			for(int j=0; j<diseaseList.size(); j++)
			{
				LOG.info("&&&&&&& populateDBWithGDS_forNoOrganism");
				//LOG.info("currExpectedOrganism="+currExpectedOrganism);
				//System.out.println("currOrganism="+currOrganism);
				String currExpectedDisease = diseaseList.get(j);
				ArrayList<String> expected_anatomy = pgController.GDS_getAssociatedAnatomy(currExpectedDisease);
				//retrieve 2 levels down of disease associated to input disease (variable 'expected')
				ArrayList<String> expected_diseaseDescendents_ncit = pgController.TH_getDiseaseDescendents(currExpectedDisease);
		
				ArrayList<ArrayList<String>> currObservedDiseaseArrayList_title = new ArrayList<ArrayList<String>>();
				ArrayList<ArrayList<String>> currObservedDiseaseArrayList_description = new ArrayList<ArrayList<String>>();
				ArrayList<gdsrecordmini> responseItem=new ArrayList<gdsrecordmini>();
				ArrayList<gdsrecordview> rViewList=new ArrayList<gdsrecordview>();
				ArrayList<Integer> currGDSList=dlGDS.get(counter++);
				//check and obtain currGDSList.get(k) from postgres DB if available
				ArrayList<Integer> foundGDS=pgController.GDS_getGDSIDPresentInDB(currGDSList);
				LOG.info("backendcountroller.java - foundGDS.size()="+foundGDS.size());
				currGDSList.removeAll(foundGDS);
				LOG.info("backendcountroller.java - currGDSList.size()="+currGDSList.size());
				LOG.info("backendcountroller.java - currGDSList="+currGDSList.toString());

				if(foundGDS.size()>0)
				{
					rViewList=pgController.GDS_copyGDSDetailsToView(foundGDS, organismSynonymList, currExpectedDisease);
					LOG.info("backendcountroller.java - responseItem.size()="+responseItem.size());
				}
				if(currGDSList.size()>0)
				{
					ArrayList<gdsrecordview> rViewList_additional=crawler.crawlForGEOOmnibusGDSItemList("", currExpectedDisease, currGDSList);
					for(int k=0; k<rViewList_additional.size(); k++)
					{
						rView=rViewList_additional.get(k);
						rViewList.add(rView);
						r = new gdsrecord(rView.getGdsid(), rView.getOrganism(), rView.getTitle(), rView.getSummary(), rView.getPlatform(), rView.getSampleNum(), rView.getFName());
						GDSRecordService.save(r);
					}
				}	
				if(rViewList.size()>0)
				{
					currObservedDiseaseArrayList_title=new ArrayList<ArrayList<String>>();
					currObservedDiseaseArrayList_description = new ArrayList<ArrayList<String>>();
					for(int k=0; k<rViewList.size(); k++)
					{
						String validity="Valid";
						rView=rViewList.get(k);
						//String title=rView.getTitle();
						//LOG.info("rView="+rView.getGdsid());
						//pyController.python_scispacy_extractDiseaseFromTitle(title);
						currObservedOrganism=rView.getOrganism();
						currObservedDiseaseArrayList_title=pgController.GDS_getAssociatedDiseaseList_Title(rView.getGdsid());
						currObservedDiseaseArrayList_description=pgController.GDS_getAssociatedDiseaseList_Description(rView.getGdsid());
						//LOG.info("currObservedDiseaseList="+currObservedDiseaseList.toString());
						//LOG.info("currExpectedOrganism="+currExpectedOrganism);
						//LOG.info("currObservedOrganism="+currObservedOrganism);
						//if(currExpectedOrganism!=null && currObservedOrganism!=null && currExpectedOrganism.compareTo(currObservedOrganism)!=0)
						LOG.info(rView.getGdsid()+"-------------------------------");
						Boolean organismMismatch = 	checkOrganismMismatch(currExpectedOrganism, currObservedOrganism);
						LOG.info("currExpectedOrganism="+ currExpectedOrganism);
						LOG.info("currObservedOrganism="+ currObservedOrganism);
						LOG.info("organismMismatch="+ organismMismatch);
						String diseaseStatus_title = checkDiseaseStatus(currExpectedDisease, currObservedDiseaseArrayList_title, expected_anatomy, expected_diseaseDescendents_ncit);
						String diseaseStatus_description = checkDiseaseStatus(currExpectedDisease, currObservedDiseaseArrayList_description, expected_anatomy, expected_diseaseDescendents_ncit);
						if(organismMismatch)
						{
							LOG.info("organism FAILLL no org---------------- ");
							rView.setValidRecord(false);	
							rView.setOrganismMismatch(true);
							if(diseaseStatus_title.compareTo("VALID")==0 || 
								(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("VALID")==0))
							//if(diseaseStatus_title.compareTo("VALID")==0 || diseaseStatus_description.compareTo("VALID")==0)
							{
								LOG.info("---- ["+rView.getGdsid()+"] SET AS INVALID DUE TO ORG MISMATCH!");
								validity="Organism Mismatch, Disease Valid";
							}
							else if(diseaseStatus_title.compareTo("MISMATCH")==0 ||
								(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("MISMATCH")==0))
							{
								LOG.info("!!!! ["+rView.getGdsid()+"] SET AS INVALID DUE TO ORG and DISEASE MISMATCH!");
								rView.setDiseaseMismatch(true);
								validity="Organism and Disease Mismatch";
							}
							else 
							//(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("UNVERIFIED")==0)
							{
								LOG.info("???? ["+rView.getGdsid()+"] SET AS INVALID DUE TO ORG MISMATCH, DISEASE UNVERIFIED!");
								rView.setDiseaseUnverified(true);
								validity="Organism Mismatch, Disease Unverified";
							}
						}
						else
						{
							LOG.info("organism PASSSSS no org+++++++");
							if(diseaseStatus_title.compareTo("VALID")==0 || 
								(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("VALID")==0))
							//if(diseaseStatus_title.compareTo("VALID")==0 || diseaseStatus_description.compareTo("VALID")==0)
							{
								LOG.info(":):):) ["+rView.getGdsid()+"] SET AS VALID!");
								validity="Valid";
								rMini=new gdsrecordmini(rView.getGdsid(), rView.getOrganism(), rView.getPlatform(), rView.getSampleNum(), validity, "", diseaseList.get(j));
								responseItem.add(rMini);
							}
							else if(diseaseStatus_title.compareTo("MISMATCH")==0 ||
								(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("MISMATCH")==0))
							{
								LOG.info("**** ["+rView.getGdsid()+"] SET AS INVALID DUE TO DISEASE MISMATCH!");
								rView.setValidRecord(false);	
								rView.setDiseaseMismatch(true);
								validity="Organism Valid, Disease Mismatch";
							}
							else
							//if(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("UNVERIFIED")==0)
							{
								LOG.info("++++ ["+rView.getGdsid()+"] SET AS VALID DUE TO ORG VALID, DISEASE UNVERIFIED!");
								rView.setDiseaseUnverified(true);
								validity="Organism Valid, Disease Unverified";
								rMini=new gdsrecordmini(rView.getGdsid(), rView.getOrganism(), rView.getPlatform(), rView.getSampleNum(), validity, "", diseaseList.get(j));
								responseItem.add(rMini);
							}
						}
						GDSRecordViewService.save(rView);
					}
				}
				responseList.add(responseItem);
				numValidatedRecords.add(responseItem.size());
			}
		
		return responseList;
	}

    @ResponseBody
	@RequestMapping(path="/populateDBWithGDS", method=RequestMethod.GET)
    //remember to return in JSON format because vue expects the data in JSON format
	//public String populateDBWithGDS_TS() {
	public String populateDBWithGDS() {
		Long startTime=System.currentTimeMillis();
		Long endTime;
		LOG.info("backendcountroller.java - populateDBWithGDS ");
		GDSRecordViewService.deleteAll();
		LOG.info("backendcountroller.java - deleteAll ");
		pgController.GDS_resetSequence("viewseq");
		LOG.info("backendcountroller.java - resetSequence ");
		ArrayList<ArrayList<gdsrecordmini>> responseList=new ArrayList<ArrayList<gdsrecordmini>>();
		
		if(organismList.size()>0)
			responseList=populateDBWithGDS_forOneOrMoreOrganism();
		else
			responseList=populateDBWithGDS_forNoOrganism();

		endTime=System.currentTimeMillis();
		LOG.info("+++++++ populateDBWithGDS duration="+(endTime-startTime)/1000+" s");
		LOG.info("JSON:" + gson.toJson(responseList));
		return gson.toJson(responseList);
	}


/*@ResponseBody
@RequestMapping(path="/populateDBWithGDS", method=RequestMethod.GET)
//remember to return in JSON format because vue expects the data in JSON format
public String populateDBWithGDS_T() {
	Long startTime=System.currentTimeMillis();
	Long endTime;
	LOG.info("backendcountroller.java - populateDBWithGDS ");
	GDSRecordViewService.deleteAll();
	LOG.info("backendcountroller.java - deleteAll ");
	pgController.GDS_resetSequence("viewseq");
	LOG.info("backendcountroller.java - resetSequence ");
	gdsrecord r;
	gdsrecordview rView;
	gdsrecordmini rMini;
	ArrayList<ArrayList<gdsrecordmini>> responseList=new ArrayList<ArrayList<gdsrecordmini>>();
	int counter=0;
	for(int i=0; i<organismList.size(); i++)
	{
		ArrayList<String> organismSynonymList=new ArrayList<String>();
		organismSynonymList=pgController.TH_getOrganismSynonym(organismList.get(i));
		LOG.info("backendcountroller.java - organismSynonymList="+organismSynonymList.toString());
		String currExpectedOrganism=organismList.get(i);
		String currObservedOrganism="";
		for(int j=0; j<diseaseList.size(); j++)
		{
			//LOG.info("currExpectedOrganism="+currExpectedOrganism);
			//System.out.println("currOrganism="+currOrganism);
			String currExpectedDisease = diseaseList.get(j);
			ArrayList<String> expected_anatomy = pgController.GDS_getAssociatedAnatomy(currExpectedDisease);
			//retrieve 2 levels down of disease associated to input disease (variable 'expected')
			ArrayList<String> expected_diseaseDescendents_ncit = pgController.TH_getDiseaseDescendents(currExpectedDisease);
		
			ArrayList<ArrayList<String>> currObservedDiseaseArrayList_title = new ArrayList<ArrayList<String>>();
			ArrayList<ArrayList<String>> currObservedDiseaseArrayList_description = new ArrayList<ArrayList<String>>();
			ArrayList<gdsrecordmini> responseItem=new ArrayList<gdsrecordmini>();
			ArrayList<gdsrecordview> rViewList=new ArrayList<gdsrecordview>();
			ArrayList<Integer> currGDSList=dlGDS.get(counter++);
			//check and obtain currGDSList.get(k) from postgres DB if available
			ArrayList<Integer> foundGDS=pgController.GDS_getGDSIDPresentInDB(currGDSList);
			LOG.info("backendcountroller.java - foundGDS.size()="+foundGDS.size());
			currGDSList.removeAll(foundGDS);
			LOG.info("backendcountroller.java - currGDSList.size()="+currGDSList.size());
			LOG.info("backendcountroller.java - currGDSList="+currGDSList.toString());

			if(foundGDS.size()>0)
			{
				rViewList=pgController.GDS_copyGDSDetailsToView(foundGDS, organismSynonymList, currExpectedDisease);
				LOG.info("backendcountroller.java - responseItem.size()="+responseItem.size());
			}
			if(currGDSList.size()>0)
			{
				ArrayList<gdsrecordview> rViewList_additional=crawler.crawlForGEOOmnibusGDSItemList(currExpectedOrganism, currExpectedDisease, currGDSList);
				for(int k=0; k<rViewList_additional.size(); k++)
				{
					rView=rViewList_additional.get(k);
					rViewList.add(rView);
					r = new gdsrecord(rView.getGdsid(), rView.getOrganism(), rView.getTitle(), rView.getSummary(), rView.getPlatform(), rView.getSampleNum(), rView.getFName());
					GDSRecordService.save(r);
				}
			}	
			if(rViewList.size()>0)
			{
				currObservedDiseaseArrayList_title=new ArrayList<ArrayList<String>>();
				currObservedDiseaseArrayList_description = new ArrayList<ArrayList<String>>();
				for(int k=0; k<rViewList.size(); k++)
				{
					String validity="Valid";
					rView=rViewList.get(k);
					//String title=rView.getTitle();
					//LOG.info("rView="+rView.getGdsid());
					//pyController.python_scispacy_extractDiseaseFromTitle(title);
					currObservedOrganism=rView.getOrganism();
					currObservedDiseaseArrayList_title=pgController.GDS_getAssociatedDiseaseList_Title(rView.getGdsid());
					currObservedDiseaseArrayList_description=pgController.GDS_getAssociatedDiseaseList_Description(rView.getGdsid());
					//LOG.info("currObservedDiseaseList="+currObservedDiseaseList.toString());
					//LOG.info("currExpectedOrganism="+currExpectedOrganism);
					//LOG.info("currObservedOrganism="+currObservedOrganism);
					//if(currExpectedOrganism!=null && currObservedOrganism!=null && currExpectedOrganism.compareTo(currObservedOrganism)!=0)
					LOG.info(rView.getGdsid()+"-------------------------------");
					Boolean organismMismatch = 	checkOrganismMismatch(currExpectedOrganism, currObservedOrganism);
					String diseaseStatus_title = checkDiseaseStatus(currExpectedDisease, currObservedDiseaseArrayList_title, expected_anatomy, expected_diseaseDescendents_ncit);
					String diseaseStatus_description = checkDiseaseStatus(currExpectedDisease, currObservedDiseaseArrayList_description, expected_anatomy, expected_diseaseDescendents_ncit);
					if(organismMismatch)
					{
						rView.setValidRecord(false);	
						rView.setOrganismMismatch(true);
						if(diseaseStatus_title.compareTo("VALID")==0)
						//if(diseaseStatus_title.compareTo("VALID")==0 || diseaseStatus_description.compareTo("VALID")==0)
						{
							LOG.info("---- ["+rView.getGdsid()+"] SET AS INVALID DUE TO ORG MISMATCH!");
							validity="Organism Mismatch, Disease Valid";
						}
						else if(diseaseStatus_title.compareTo("MISMATCH")==0)
						{
							LOG.info("!!!! ["+rView.getGdsid()+"] SET AS INVALID DUE TO ORG and DISEASE MISMATCH!");
							rView.setDiseaseMismatch(true);
							validity="Organism and Disease Mismatch";
						}
						else 
						//(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("UNVERIFIED")==0)
						{
							LOG.info("???? ["+rView.getGdsid()+"] SET AS INVALID DUE TO ORG MISMATCH, DISEASE UNVERIFIED!");
							rView.setDiseaseUnverified(true);
							validity="Organism Mismatch, Disease Unverified";
						}
					}
					else
					{
						if(diseaseStatus_title.compareTo("VALID")==0)
						//if(diseaseStatus_title.compareTo("VALID")==0 || diseaseStatus_description.compareTo("VALID")==0)
						{
							LOG.info(":):):) ["+rView.getGdsid()+"] SET AS VALID!");
							validity="Valid";
							rMini=new gdsrecordmini(rView.getGdsid(), rView.getOrganism(), rView.getPlatform(), rView.getSampleNum(), validity, organismList.get(i), diseaseList.get(j));
							responseItem.add(rMini);
						}
						else if(diseaseStatus_title.compareTo("MISMATCH")==0)
						{
							LOG.info("**** ["+rView.getGdsid()+"] SET AS INVALID DUE TO DISEASE MISMATCH!");
							rView.setValidRecord(false);	
							rView.setDiseaseMismatch(true);
							validity="Organism Valid, Disease Mismatch";
						}
						else
						//if(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("UNVERIFIED")==0)
						{
							LOG.info("++++ ["+rView.getGdsid()+"] SET AS VALID DUE TO ORG VALID, DISEASE UNVERIFIED!");
							rView.setDiseaseUnverified(true);
							validity="Organism Valid, Disease Unverified";
							rMini=new gdsrecordmini(rView.getGdsid(), rView.getOrganism(), rView.getPlatform(), rView.getSampleNum(), validity, organismList.get(i), diseaseList.get(j));
							responseItem.add(rMini);
						}
					}
					GDSRecordViewService.save(rView);
				}
			}
			responseList.add(responseItem);
			numValidatedRecords.add(responseItem.size());
		}
	}
	endTime=System.currentTimeMillis();
	LOG.info("+++++++ populateDBWithGDS duration="+(endTime-startTime)/1000+" s");
	LOG.info("JSON:" + gson.toJson(responseList));
	return gson.toJson(responseList);
}*/

/*@ResponseBody
@RequestMapping(path="/populateDBWithGDS", method=RequestMethod.GET)
//remember to return in JSON format because vue expects the data in JSON format
public String populateDBWithGDS_S() {
	Long startTime=System.currentTimeMillis();
	Long endTime;
	LOG.info("backendcountroller.java - populateDBWithGDS ");
	GDSRecordViewService.deleteAll();
	LOG.info("backendcountroller.java - deleteAll ");
	pgController.GDS_resetSequence("viewseq");
	LOG.info("backendcountroller.java - resetSequence ");
	gdsrecord r;
	gdsrecordview rView;
	gdsrecordmini rMini;
	ArrayList<ArrayList<gdsrecordmini>> responseList=new ArrayList<ArrayList<gdsrecordmini>>();
	int counter=0;
	for(int i=0; i<organismList.size(); i++)
	{
		ArrayList<String> organismSynonymList=new ArrayList<String>();
		organismSynonymList=pgController.TH_getOrganismSynonym(organismList.get(i));
		LOG.info("backendcountroller.java - organismSynonymList="+organismSynonymList.toString());
		String currExpectedOrganism=organismList.get(i);
		String currObservedOrganism="";
		for(int j=0; j<diseaseList.size(); j++)
		{
			//LOG.info("currExpectedOrganism="+currExpectedOrganism);
			//System.out.println("currOrganism="+currOrganism);
			String currExpectedDisease = diseaseList.get(j);
			ArrayList<String> expected_anatomy = pgController.GDS_getAssociatedAnatomy(currExpectedDisease);
			//retrieve 2 levels down of disease associated to input disease (variable 'expected')
			ArrayList<String> expected_diseaseDescendents_ncit = pgController.TH_getDiseaseDescendents(currExpectedDisease);
		
			ArrayList<ArrayList<String>> currObservedDiseaseArrayList_title = new ArrayList<ArrayList<String>>();
			ArrayList<ArrayList<String>> currObservedDiseaseArrayList_description = new ArrayList<ArrayList<String>>();
			ArrayList<gdsrecordmini> responseItem=new ArrayList<gdsrecordmini>();
			ArrayList<gdsrecordview> rViewList=new ArrayList<gdsrecordview>();
			ArrayList<Integer> currGDSList=dlGDS.get(counter++);
			//check and obtain currGDSList.get(k) from postgres DB if available
			ArrayList<Integer> foundGDS=pgController.GDS_getGDSIDPresentInDB(currGDSList);
			LOG.info("backendcountroller.java - foundGDS.size()="+foundGDS.size());
			currGDSList.removeAll(foundGDS);
			LOG.info("backendcountroller.java - currGDSList.size()="+currGDSList.size());
			LOG.info("backendcountroller.java - currGDSList="+currGDSList.toString());

			if(foundGDS.size()>0)
			{
				rViewList=pgController.GDS_copyGDSDetailsToView(foundGDS, organismSynonymList, currExpectedDisease);
				LOG.info("backendcountroller.java - responseItem.size()="+responseItem.size());
			}
			if(currGDSList.size()>0)
			{
				ArrayList<gdsrecordview> rViewList_additional=crawler.crawlForGEOOmnibusGDSItemList(currExpectedOrganism, currExpectedDisease, currGDSList);
				for(int k=0; k<rViewList_additional.size(); k++)
				{
					rView=rViewList_additional.get(k);
					rViewList.add(rView);
					r = new gdsrecord(rView.getGdsid(), rView.getOrganism(), rView.getTitle(), rView.getSummary(), rView.getPlatform(), rView.getSampleNum(), rView.getFName());
					GDSRecordService.save(r);
				}
			}	
			if(rViewList.size()>0)
			{
				currObservedDiseaseArrayList_title=new ArrayList<ArrayList<String>>();
				currObservedDiseaseArrayList_description = new ArrayList<ArrayList<String>>();
				for(int k=0; k<rViewList.size(); k++)
				{
					String validity="Valid";
					rView=rViewList.get(k);
					//String title=rView.getTitle();
					//LOG.info("rView="+rView.getGdsid());
					//pyController.python_scispacy_extractDiseaseFromTitle(title);
					currObservedOrganism=rView.getOrganism();
					currObservedDiseaseArrayList_title=pgController.GDS_getAssociatedDiseaseList_Title(rView.getGdsid());
					currObservedDiseaseArrayList_description=pgController.GDS_getAssociatedDiseaseList_Description(rView.getGdsid());
					//LOG.info("currObservedDiseaseList="+currObservedDiseaseList.toString());
					//LOG.info("currExpectedOrganism="+currExpectedOrganism);
					//LOG.info("currObservedOrganism="+currObservedOrganism);
					//if(currExpectedOrganism!=null && currObservedOrganism!=null && currExpectedOrganism.compareTo(currObservedOrganism)!=0)
					LOG.info(rView.getGdsid()+"-------------------------------");
					Boolean organismMismatch = 	checkOrganismMismatch(currExpectedOrganism, currObservedOrganism);
					String diseaseStatus_title = checkDiseaseStatus(currExpectedDisease, currObservedDiseaseArrayList_title, expected_anatomy, expected_diseaseDescendents_ncit);
					String diseaseStatus_description = checkDiseaseStatus(currExpectedDisease, currObservedDiseaseArrayList_description, expected_anatomy, expected_diseaseDescendents_ncit);
					if(organismMismatch)
					{
						rView.setValidRecord(false);	
						rView.setOrganismMismatch(true);
						if(diseaseStatus_description.compareTo("VALID")==0)
						//if(diseaseStatus_title.compareTo("VALID")==0 || diseaseStatus_description.compareTo("VALID")==0)
						{
							LOG.info("---- ["+rView.getGdsid()+"] SET AS INVALID DUE TO ORG MISMATCH!");
							validity="Organism Mismatch, Disease Valid";
						}
						else if(diseaseStatus_description.compareTo("MISMATCH")==0)
						{
							LOG.info("!!!! ["+rView.getGdsid()+"] SET AS INVALID DUE TO ORG and DISEASE MISMATCH!");
							rView.setDiseaseMismatch(true);
							validity="Organism and Disease Mismatch";
						}
						else 
						//(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("UNVERIFIED")==0)
						{
							LOG.info("???? ["+rView.getGdsid()+"] SET AS INVALID DUE TO ORG MISMATCH, DISEASE UNVERIFIED!");
							rView.setDiseaseUnverified(true);
							validity="Organism Mismatch, Disease Unverified";
						}
					}
					else
					{
						if(diseaseStatus_description.compareTo("VALID")==0)
						//if(diseaseStatus_title.compareTo("VALID")==0 || diseaseStatus_description.compareTo("VALID")==0)
						{
							LOG.info(":):):) ["+rView.getGdsid()+"] SET AS VALID!");
							validity="Valid";
							rMini=new gdsrecordmini(rView.getGdsid(), rView.getOrganism(), rView.getPlatform(), rView.getSampleNum(), validity, organismList.get(i), diseaseList.get(j));
							responseItem.add(rMini);
						}
						else if(diseaseStatus_description.compareTo("MISMATCH")==0)
						{
							LOG.info("**** ["+rView.getGdsid()+"] SET AS INVALID DUE TO DISEASE MISMATCH!");
							rView.setValidRecord(false);	
							rView.setDiseaseMismatch(true);
							validity="Organism Valid, Disease Mismatch";
						}
						else
						//if(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("UNVERIFIED")==0)
						{
							LOG.info("++++ ["+rView.getGdsid()+"] SET AS VALID DUE TO ORG VALID, DISEASE UNVERIFIED!");
							rView.setDiseaseUnverified(true);
							validity="Organism Valid, Disease Unverified";
							rMini=new gdsrecordmini(rView.getGdsid(), rView.getOrganism(), rView.getPlatform(), rView.getSampleNum(), validity, organismList.get(i), diseaseList.get(j));
							responseItem.add(rMini);
						}
					}
					GDSRecordViewService.save(rView);
				}
			}
			responseList.add(responseItem);
			numValidatedRecords.add(responseItem.size());
		}
	}
	endTime=System.currentTimeMillis();
	LOG.info("+++++++ populateDBWithGDS duration="+(endTime-startTime)/1000+" s");
	LOG.info("JSON:" + gson.toJson(responseList));
	return gson.toJson(responseList);
}*/


/*@ResponseBody
@RequestMapping(path="/populateDBWithGDS", method=RequestMethod.GET)
//remember to return in JSON format because vue expects the data in JSON format
//public String populateDBWithGDS_All() {
public String populateDBWithGDS() {
	Long startTime=System.currentTimeMillis();
	Long endTime;
	LOG.info("backendcountroller.java - populateDBWithGDS ");
	GDSRecordViewService.deleteAll();
	LOG.info("backendcountroller.java - deleteAll ");
	pgController.GDS_resetSequence("viewseq");
	LOG.info("backendcountroller.java - resetSequence ");
	gdsrecord r;
	gdsrecordview rView;
	gdsrecordmini rMini;
	ArrayList<ArrayList<gdsrecordmini>> responseList=new ArrayList<ArrayList<gdsrecordmini>>();
	int counter=0;
	for(int i=0; i<organismList.size(); i++)
	{
		ArrayList<String> organismSynonymList=new ArrayList<String>();
		organismSynonymList=pgController.TH_getOrganismSynonym(organismList.get(i));
		LOG.info("backendcountroller.java - organismSynonymList="+organismSynonymList.toString());
		String currExpectedOrganism=organismList.get(i);
		String currObservedOrganism="";
		for(int j=0; j<diseaseList.size(); j++)
		{
			//LOG.info("currExpectedOrganism="+currExpectedOrganism);
			//System.out.println("currOrganism="+currOrganism);
			String currExpectedDisease = diseaseList.get(j);
			ArrayList<String> expected_anatomy = pgController.GDS_getAssociatedAnatomy(currExpectedDisease);
			//retrieve 2 levels down of disease associated to input disease (variable 'expected')
			ArrayList<String> expected_diseaseDescendents_ncit = pgController.TH_getDiseaseDescendents(currExpectedDisease);
		
			ArrayList<ArrayList<String>> currObservedDiseaseArrayList_title = new ArrayList<ArrayList<String>>();
			ArrayList<ArrayList<String>> currObservedDiseaseArrayList_description = new ArrayList<ArrayList<String>>();
			ArrayList<gdsrecordmini> responseItem=new ArrayList<gdsrecordmini>();
			ArrayList<gdsrecordview> rViewList=new ArrayList<gdsrecordview>();
			ArrayList<Integer> currGDSList=dlGDS.get(counter++);
			//check and obtain currGDSList.get(k) from postgres DB if available
			ArrayList<Integer> foundGDS=pgController.GDS_getGDSIDPresentInDB(currGDSList);
			LOG.info("backendcountroller.java - foundGDS.size()="+foundGDS.size());
			currGDSList.removeAll(foundGDS);
			LOG.info("backendcountroller.java - currGDSList.size()="+currGDSList.size());
			LOG.info("backendcountroller.java - currGDSList="+currGDSList.toString());

			if(foundGDS.size()>0)
			{
				rViewList=pgController.GDS_copyGDSDetailsToView(foundGDS, organismSynonymList, currExpectedDisease);
				LOG.info("backendcountroller.java - responseItem.size()="+responseItem.size());
			}
			if(currGDSList.size()>0)
			{
				ArrayList<gdsrecordview> rViewList_additional=crawler.crawlForGEOOmnibusGDSItemList(currExpectedOrganism, currExpectedDisease, currGDSList);
				for(int k=0; k<rViewList_additional.size(); k++)
				{
					rView=rViewList_additional.get(k);
					rViewList.add(rView);
					r = new gdsrecord(rView.getGdsid(), rView.getOrganism(), rView.getTitle(), rView.getSummary(), rView.getPlatform(), rView.getSampleNum(), rView.getFName());
					GDSRecordService.save(r);
				}
			}	
			if(rViewList.size()>0)
			{
				currObservedDiseaseArrayList_title=new ArrayList<ArrayList<String>>();
				currObservedDiseaseArrayList_description = new ArrayList<ArrayList<String>>();
				for(int k=0; k<rViewList.size(); k++)
				{
					String validity="Valid";
					rView=rViewList.get(k);
					//String title=rView.getTitle();
					//LOG.info("rView="+rView.getGdsid());
					//pyController.python_scispacy_extractDiseaseFromTitle(title);
					currObservedOrganism=rView.getOrganism();
					currObservedDiseaseArrayList_title=pgController.GDS_getAssociatedDiseaseList_Title(rView.getGdsid());
					//currObservedDiseaseArrayList_description=pgController.GDS_getAssociatedDiseaseList_Description(rView.getGdsid());
					currObservedDiseaseArrayList_description=pgController.GDS_getAssociatedDiseaseList_TitleDescription(rView.getGdsid());
					//LOG.info("currObservedDiseaseList="+currObservedDiseaseList.toString());
					//LOG.info("currExpectedOrganism="+currExpectedOrganism);
					//LOG.info("currObservedOrganism="+currObservedOrganism);
					//if(currExpectedOrganism!=null && currObservedOrganism!=null && currExpectedOrganism.compareTo(currObservedOrganism)!=0)
					LOG.info(rView.getGdsid()+"-------------------------------");
					Boolean organismMismatch = 	checkOrganismMismatch(currExpectedOrganism, currObservedOrganism);
					String diseaseStatus_title = checkDiseaseStatus(currExpectedDisease, currObservedDiseaseArrayList_title, expected_anatomy, expected_diseaseDescendents_ncit);
					String diseaseStatus_description = checkDiseaseStatus(currExpectedDisease, currObservedDiseaseArrayList_description, expected_anatomy, expected_diseaseDescendents_ncit);
					if(organismMismatch)
					{
						rView.setValidRecord(false);	
						rView.setOrganismMismatch(true);
						if(diseaseStatus_description.compareTo("VALID")==0)
						//if(diseaseStatus_title.compareTo("VALID")==0 || diseaseStatus_description.compareTo("VALID")==0)
						{
							LOG.info("---- ["+rView.getGdsid()+"] SET AS INVALID DUE TO ORG MISMATCH!");
							validity="Organism Mismatch, Disease Valid";
						}
						else if(diseaseStatus_description.compareTo("MISMATCH")==0)
						{
							LOG.info("!!!! ["+rView.getGdsid()+"] SET AS INVALID DUE TO ORG and DISEASE MISMATCH!");
							rView.setDiseaseMismatch(true);
							validity="Organism and Disease Mismatch";
						}
						else 
						//(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("UNVERIFIED")==0)
						{
							LOG.info("???? ["+rView.getGdsid()+"] SET AS INVALID DUE TO ORG MISMATCH, DISEASE UNVERIFIED!");
							rView.setDiseaseUnverified(true);
							validity="Organism Mismatch, Disease Unverified";
						}
					}
					else
					{
						if(diseaseStatus_description.compareTo("VALID")==0)
						//if(diseaseStatus_title.compareTo("VALID")==0 || diseaseStatus_description.compareTo("VALID")==0)
						{
							LOG.info(":):):) ["+rView.getGdsid()+"] SET AS VALID!");
							validity="Valid";
							rMini=new gdsrecordmini(rView.getGdsid(), rView.getOrganism(), rView.getPlatform(), rView.getSampleNum(), validity, organismList.get(i), diseaseList.get(j));
							responseItem.add(rMini);
						}
						else if(diseaseStatus_description.compareTo("MISMATCH")==0)
						{
							LOG.info("**** ["+rView.getGdsid()+"] SET AS INVALID DUE TO DISEASE MISMATCH!");
							rView.setValidRecord(false);	
							rView.setDiseaseMismatch(true);
							validity="Organism Valid, Disease Mismatch";
						}
						else
						//if(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("UNVERIFIED")==0)
						{
							LOG.info("++++ ["+rView.getGdsid()+"] SET AS VALID DUE TO ORG VALID, DISEASE UNVERIFIED!");
							rView.setDiseaseUnverified(true);
							validity="Organism Valid, Disease Unverified";
							rMini=new gdsrecordmini(rView.getGdsid(), rView.getOrganism(), rView.getPlatform(), rView.getSampleNum(), validity, organismList.get(i), diseaseList.get(j));
							responseItem.add(rMini);
						}
					}
					GDSRecordViewService.save(rView);
				}
			}
			responseList.add(responseItem);
			numValidatedRecords.add(responseItem.size());
		}
	}
	endTime=System.currentTimeMillis();
	LOG.info("+++++++ populateDBWithGDS duration="+(endTime-startTime)/1000+" s");
	LOG.info("JSON:" + gson.toJson(responseList));
	return gson.toJson(responseList);
}*/

/*@ResponseBody
	@RequestMapping(path="/populateDBWithGDS", method=RequestMethod.GET)
    //remember to return in JSON format because vue expects the data in JSON format
	public String populateDBWithGDS_ST() {
		Long startTime=System.currentTimeMillis();
		Long endTime;
		LOG.info("backendcountroller.java - populateDBWithGDS ");
		GDSRecordViewService.deleteAll();
		LOG.info("backendcountroller.java - deleteAll ");
		pgController.GDS_resetSequence("viewseq");
		LOG.info("backendcountroller.java - resetSequence ");
		gdsrecord r;
		gdsrecordview rView;
		gdsrecordmini rMini;
		ArrayList<ArrayList<gdsrecordmini>> responseList=new ArrayList<ArrayList<gdsrecordmini>>();
		int counter=0;
		for(int i=0; i<organismList.size(); i++)
		{
			ArrayList<String> organismSynonymList=new ArrayList<String>();
			organismSynonymList=pgController.TH_getOrganismSynonym(organismList.get(i));
			LOG.info("backendcountroller.java - organismSynonymList="+organismSynonymList.toString());
			String currExpectedOrganism=organismList.get(i);
			String currObservedOrganism="";
			for(int j=0; j<diseaseList.size(); j++)
			{
				//LOG.info("currExpectedOrganism="+currExpectedOrganism);
				//System.out.println("currOrganism="+currOrganism);
				String currExpectedDisease = diseaseList.get(j);
				ArrayList<String> expected_anatomy = pgController.GDS_getAssociatedAnatomy(currExpectedDisease);
				//retrieve 2 levels down of disease associated to input disease (variable 'expected')
				ArrayList<String> expected_diseaseDescendents_ncit = pgController.TH_getDiseaseDescendents(currExpectedDisease);
		
				ArrayList<ArrayList<String>> currObservedDiseaseArrayList_title = new ArrayList<ArrayList<String>>();
				ArrayList<ArrayList<String>> currObservedDiseaseArrayList_description = new ArrayList<ArrayList<String>>();
				ArrayList<gdsrecordmini> responseItem=new ArrayList<gdsrecordmini>();
				ArrayList<gdsrecordview> rViewList=new ArrayList<gdsrecordview>();
				ArrayList<Integer> currGDSList=dlGDS.get(counter++);
				//check and obtain currGDSList.get(k) from postgres DB if available
				ArrayList<Integer> foundGDS=pgController.GDS_getGDSIDPresentInDB(currGDSList);
				LOG.info("backendcountroller.java - foundGDS.size()="+foundGDS.size());
				currGDSList.removeAll(foundGDS);
				LOG.info("backendcountroller.java - currGDSList.size()="+currGDSList.size());
				LOG.info("backendcountroller.java - currGDSList="+currGDSList.toString());

				if(foundGDS.size()>0)
				{
					rViewList=pgController.GDS_copyGDSDetailsToView(foundGDS, organismSynonymList, currExpectedDisease);
					LOG.info("backendcountroller.java - responseItem.size()="+responseItem.size());
				}
				if(currGDSList.size()>0)
				{
					ArrayList<gdsrecordview> rViewList_additional=crawler.crawlForGEOOmnibusGDSItemList(currExpectedOrganism, currExpectedDisease, currGDSList);
					for(int k=0; k<rViewList_additional.size(); k++)
					{
						rView=rViewList_additional.get(k);
						rViewList.add(rView);
						r = new gdsrecord(rView.getGdsid(), rView.getOrganism(), rView.getTitle(), rView.getSummary(), rView.getPlatform(), rView.getSampleNum(), rView.getFName());
						GDSRecordService.save(r);
					}
				}	
				if(rViewList.size()>0)
				{
					currObservedDiseaseArrayList_title=new ArrayList<ArrayList<String>>();
					currObservedDiseaseArrayList_description = new ArrayList<ArrayList<String>>();
					for(int k=0; k<rViewList.size(); k++)
					{
						String validity="Valid";
						rView=rViewList.get(k);
						//String title=rView.getTitle();
						//LOG.info("rView="+rView.getGdsid());
						//pyController.python_scispacy_extractDiseaseFromTitle(title);
						currObservedOrganism=rView.getOrganism();
						currObservedDiseaseArrayList_title=pgController.GDS_getAssociatedDiseaseList_Title(rView.getGdsid());
						currObservedDiseaseArrayList_description=pgController.GDS_getAssociatedDiseaseList_Description(rView.getGdsid());
						//LOG.info("currObservedDiseaseList="+currObservedDiseaseList.toString());
						//LOG.info("currExpectedOrganism="+currExpectedOrganism);
						//LOG.info("currObservedOrganism="+currObservedOrganism);
						//if(currExpectedOrganism!=null && currObservedOrganism!=null && currExpectedOrganism.compareTo(currObservedOrganism)!=0)
						LOG.info(rView.getGdsid()+"-------------------------------");
						Boolean organismMismatch = 	checkOrganismMismatch(currExpectedOrganism, currObservedOrganism);
						String diseaseStatus_title = checkDiseaseStatus(currExpectedDisease, currObservedDiseaseArrayList_title, expected_anatomy, expected_diseaseDescendents_ncit);
						String diseaseStatus_description = checkDiseaseStatus(currExpectedDisease, currObservedDiseaseArrayList_description, expected_anatomy, expected_diseaseDescendents_ncit);
						if(organismMismatch)
						{
							rView.setValidRecord(false);	
							rView.setOrganismMismatch(true);
							if(diseaseStatus_description.compareTo("VALID")==0 || 
								(diseaseStatus_description.compareTo("UNVERIFIED")==0 && diseaseStatus_title.compareTo("VALID")==0))
							//if(diseaseStatus_title.compareTo("VALID")==0 || diseaseStatus_description.compareTo("VALID")==0)
							{
								LOG.info("---- ["+rView.getGdsid()+"] SET AS INVALID DUE TO ORG MISMATCH!");
								validity="Organism Mismatch, Disease Valid";
							}
							else if(diseaseStatus_description.compareTo("MISMATCH")==0 ||
								(diseaseStatus_description.compareTo("UNVERIFIED")==0 && diseaseStatus_title.compareTo("MISMATCH")==0))
							{
								LOG.info("!!!! ["+rView.getGdsid()+"] SET AS INVALID DUE TO ORG and DISEASE MISMATCH!");
								rView.setDiseaseMismatch(true);
								validity="Organism and Disease Mismatch";
							}
							else 
							//(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("UNVERIFIED")==0)
							{
								LOG.info("???? ["+rView.getGdsid()+"] SET AS INVALID DUE TO ORG MISMATCH, DISEASE UNVERIFIED!");
								rView.setDiseaseUnverified(true);
								validity="Organism Mismatch, Disease Unverified";
							}
						}
						else
						{
							if(diseaseStatus_description.compareTo("VALID")==0 || 
								(diseaseStatus_description.compareTo("UNVERIFIED")==0 && diseaseStatus_title.compareTo("VALID")==0))
							//if(diseaseStatus_title.compareTo("VALID")==0 || diseaseStatus_description.compareTo("VALID")==0)
							{
								LOG.info(":):):) ["+rView.getGdsid()+"] SET AS VALID!");
								validity="Valid";
								rMini=new gdsrecordmini(rView.getGdsid(), rView.getOrganism(), rView.getPlatform(), rView.getSampleNum(), validity, organismList.get(i), diseaseList.get(j));
								responseItem.add(rMini);
							}
							else if(diseaseStatus_description.compareTo("MISMATCH")==0 ||
								(diseaseStatus_description.compareTo("UNVERIFIED")==0 && diseaseStatus_title.compareTo("MISMATCH")==0))
							{
								LOG.info("**** ["+rView.getGdsid()+"] SET AS INVALID DUE TO DISEASE MISMATCH!");
								rView.setValidRecord(false);	
								rView.setDiseaseMismatch(true);
								validity="Organism Valid, Disease Mismatch";
							}
							else
							//if(diseaseStatus_title.compareTo("UNVERIFIED")==0 && diseaseStatus_description.compareTo("UNVERIFIED")==0)
							{
								LOG.info("++++ ["+rView.getGdsid()+"] SET AS VALID DUE TO ORG VALID, DISEASE UNVERIFIED!");
								rView.setDiseaseUnverified(true);
								validity="Organism Valid, Disease Unverified";
								rMini=new gdsrecordmini(rView.getGdsid(), rView.getOrganism(), rView.getPlatform(), rView.getSampleNum(), validity, organismList.get(i), diseaseList.get(j));
								responseItem.add(rMini);
							}
						}
						GDSRecordViewService.save(rView);
					}
				}
				responseList.add(responseItem);
				numValidatedRecords.add(responseItem.size());
			}
		}
		endTime=System.currentTimeMillis();
		LOG.info("+++++++ populateDBWithGDS duration="+(endTime-startTime)/1000+" s");
		LOG.info("JSON:" + gson.toJson(responseList));
		return gson.toJson(responseList);
}*/

	@ResponseBody
	@RequestMapping(path="/getNumValidatedRecords", method=RequestMethod.GET)
    //remember to return in JSON format because vue expects the data in JSON format
	public String getNumValidatedRecords() {
		String responseMessage="";
		LOG.info("backendcountroller.java - getNumValidatedRecords ");
		int counter=0;
		if(organismList.size()>0)
		{
			for(int i=0; i<organismList.size(); i++)
			{
				for(int j=0; j<diseaseList.size(); j++)
					responseMessage=responseMessage+"\n"+numValidatedRecords.get(counter++)+" validated GDS records for ["+organismList.get(i)+","+diseaseList.get(j)+"]";
			}
		}
		else
		{
			for(int j=0; j<diseaseList.size(); j++)
				responseMessage=responseMessage+"\n"+numValidatedRecords.get(counter++)+" validated GDS records for [All organisms,"+diseaseList.get(j)+"]";
		}
		return responseMessage;
	}

	@ResponseBody
	@RequestMapping(path="/getInvalidRecords", method=RequestMethod.GET)
    //remember to return in JSON format because vue expects the data in JSON format
	public String getInvalidRecords() {
		ArrayList<gdsrecordinvalid> responseList=new ArrayList<gdsrecordinvalid>();
		LOG.info("backendcountroller.java - getInvalidRecords ");
		responseList=pgController.GDS_getInvalidRecords();
		
		LOG.info("JSON:" + gson.toJson(responseList));
		return gson.toJson(responseList);
	}



	@ResponseBody
	@RequestMapping(path="/setInvalidRecordsAsValid", method=RequestMethod.POST)
    //remember to return in JSON format because vue expects the data in JSON format
	public String setInvalidRecordsAsValid(@RequestBody String jsonRecordList) {
		LOG.info("backendcountroller.java - setInvalidRecordsAsValid ");

		ArrayList<gdsrecordinvalid> invalidRecordToMakeValidList=gson.fromJson(jsonRecordList, new TypeToken<ArrayList<gdsrecordinvalid>>(){}.getType());
		pgController.GDS_setInvalidRecordsAsValid(invalidRecordToMakeValidList);//update postgres tables
		LOG.info("backendcountroller.java - sent setInvalidRecordsAsValid to postgresController.java ");
		ArrayList<ArrayList<gdsrecordmini>> responseList=pgController.GDS_getAllValidRecords(organismDiseasePairList);
		LOG.info("backendcountroller.java - retrieve all valid records from postgresController.java ");
		LOG.info("JSON:" + gson.toJson(responseList));
		return gson.toJson(responseList);
	}


	@ResponseBody
	@RequestMapping(path="/setValidRecordsAsInvalid", method=RequestMethod.POST)
    //remember to return in JSON format because vue expects the data in JSON format
	public String setValidRecordsAsInvalid(@RequestBody String jsonRecordList) {
		LOG.info("backendcountroller.java - setValidRecordsAsInvalid ");

		ArrayList<gdsrecordmini> validRecordToMakeInvalidList=gson.fromJson(jsonRecordList, new TypeToken<ArrayList<gdsrecordmini>>(){}.getType());
		LOG.info("backendcountroller.java - validRecordToMakeInvalidList.size()="+validRecordToMakeInvalidList.size());
		pgController.GDS_setValidRecordsAsInvalid(validRecordToMakeInvalidList);//update postgres tables
		LOG.info("backendcountroller.java - sent setValidRecordsAsInvalid to postgresController.java ");
		ArrayList<gdsrecordinvalid> responseList=pgController.GDS_getInvalidRecords();
		LOG.info("backendcountroller.java - retrieve all valid records from postgresController.java ");
		LOG.info("JSON:" + gson.toJson(responseList));
		return gson.toJson(responseList);
	}

	/*@ResponseBody
	@RequestMapping(path="/getNewTabRecords_backup", method=RequestMethod.POST)
    //remember to return in JSON format because vue expects the data in JSON format
	//TODO1: for each rule, create a tmp table in postgres
	//TODO2: create the SQL statement to filter out relevant records using INTERSECT (for AND) and UNION (for OR) with SELECT statements
	//TODO3: extract the gdsrecordmini for TODO2. may need to combine the organism and disease information check extracted records is correctly displayed
	//TODO4: for vue, create a new array (tabheader and validItems) to store the details for the combined results

	public String getNewTabRecords_backup(@RequestBody String jsonValidItemsArray) {
		LOG.info("backendcountroller.java - getNewTabRecords ");
		ArrayList<gdsrecordmini> newTabRecords = new ArrayList<gdsrecordmini>();
		ArrayList<ArrayList<gdsrecordmini>> validItemsArray = gson.fromJson(jsonValidItemsArray, new TypeToken<ArrayList<ArrayList<gdsrecordmini>>>(){}.getType());

		//create an overall records for all valid tabs
		ArrayList<gdsrecordmini> consolidateValidRecords = pgController.GDS_getConsolidatedValidRecords();
		ArrayList<Integer> consolidateValidRecords_gdsid = pgController.GDS_getConsolidateValidRecords_gdsid();

		//pull out the corresponding validItems gdsid for each rule in rulelistArray
		ArrayList<ArrayList<Integer>> rulelistArray_validItemsGDSid = new ArrayList<ArrayList<Integer>>();
		ArrayList<ArrayList<Integer>> rulelistArray_negate_validItemsGDSid = new ArrayList<ArrayList<Integer>>();
		ArrayList<ArrayList<String>> rulelistArray_inputOrganismDisease = new ArrayList<ArrayList<String>>();
		for(int i=0; i<rulelistArray.size(); i++)
		{
			int index = rulelistArray.get(i).getResultTabIndex();
			ArrayList<gdsrecordmini> currValidItems = validItemsArray.get(index);
			ArrayList<Integer> currValidItemsGDSid = new ArrayList<Integer>();
			ArrayList<String> currInputOrganismDisease = new ArrayList<String>();
			ArrayList<Integer> differences = new ArrayList<Integer>(consolidateValidRecords_gdsid);
			String currInputOrganism = "";
			String currInputDisease = "";
			
			if(currValidItems.size()>0)
			{
				currInputOrganism = currValidItems.get(0).getInputOrganism();
				currInputDisease = currValidItems.get(0).getInputDisease();
				for(int j=0; j<currValidItems.size(); j++)
					currValidItemsGDSid.add(currValidItems.get(j).getGDSID());
			}

			currInputOrganismDisease.add(currInputOrganism);
			currInputOrganismDisease.add(currInputDisease);
			rulelistArray_inputOrganismDisease.add(currInputOrganismDisease);
			rulelistArray_validItemsGDSid.add(currValidItemsGDSid);
			differences.removeAll(currValidItemsGDSid);
			rulelistArray_negate_validItemsGDSid.add(differences);
		}
		createTempTables(rulelistArray_validItemsGDSid, rulelistArray_negate_validItemsGDSid);
		return gson.toJson(newTabRecords);
	}

	private void createTempTables(ArrayList<ArrayList<Integer>> validGDSid_2DArray, ArrayList<ArrayList<Integer>> negateValidGDSid_2DArray)
	{

	}
*/
	private String createTempTableForNewTab(int index, ArrayList<Integer> gdsidList, boolean isNegated)
	{
		String negate="";
		if(isNegated)
			negate = "negate_";
		String tempTableName = negate+"tmp_"+index;
		pgController.GDS_createNewTabTempTable(tempTableName, gdsidList);
		return tempTableName;
	}

	private ArrayList<Integer> getAllIndex(int obj, ArrayList<gdsrecordmini> objArray)
	{
		ArrayList<Integer> indexList = new ArrayList<Integer>();
		if(objArray.size()>0)
		{
			for(int i=0; i<objArray.size(); i++)
			{
				gdsrecordmini r = objArray.get(i);
				if(r.getGDSID()==obj)
					indexList.add(i);
			}
		}
		return indexList;
	}

	@ResponseBody
	@RequestMapping(path="/getNewTabRecords", method=RequestMethod.POST)
	public String getNewTabRecords(@RequestBody String jsonParams) {
		LOG.info("backendcountroller.java - getNewTabRecords ");
		ArrayList<gdsrecordmini> newTabRecords = new ArrayList<gdsrecordmini>();
		tabRecordParam params = gson.fromJson(jsonParams, new TypeToken<tabRecordParam>(){}.getType());
		ArrayList<rulelist> rulelistArray = params.getRuleListArray();
		ArrayList<ArrayList<gdsrecordmini>> validitemsArray = params.getValidItemsArray();
		ArrayList<String> validtabArray = params.getValidTabArray(); //tell us what the original valid result tabs are. Only those are selectable for combination results view
		ArrayList<String> tempTableList = new ArrayList<String>();
		ArrayList<String> negateTempTableList = new ArrayList<String>();
		ArrayList<Integer> newTabGDSid = new ArrayList<Integer>();
		//create an overall records for all valid tabs
		//ArrayList<gdsrecordmini> consolidateValidRecords = pgController.GDS_getConsolidatedValidRecords();
		ArrayList<gdsrecordmini> consolidateValidRecords = pgController.GDS_getConsolidatedValidRecords();
		ArrayList<Integer> consolidateValidRecords_gdsid = pgController.GDS_getConsolidateValidRecords_gdsid();
		//get number of validtabs
		int firstNTabs = validtabArray.size();
		//pull out firstNTabs validItems and corresponding negate items for storing as temp tables into postgreSQL
		for(int i=0; i<firstNTabs; i++)
		{
			ArrayList<gdsrecordmini> currValidItems = validitemsArray.get(i);
			ArrayList<Integer> currValidItemsGDSid = new ArrayList<Integer>();
			ArrayList<Integer> differences = new ArrayList<Integer>(consolidateValidRecords_gdsid);
			
			if(currValidItems.size()>0)
			{
				for(int j=0; j<currValidItems.size(); j++)
					currValidItemsGDSid.add(currValidItems.get(j).getGDSID());
			}
			differences.removeAll(currValidItemsGDSid);
			//create temp tables to store currValidItemsGDSid and differences
			String tempTableName=createTempTableForNewTab(i,currValidItemsGDSid,false);
			String negateTempTableName=createTempTableForNewTab(i,differences,true);
			if(tempTableList.contains(tempTableName)==false)
				tempTableList.add(tempTableName);
			if(negateTempTableList.contains(negateTempTableName)==false)
				negateTempTableList.add(negateTempTableName);
		}
		newTabGDSid=pgController.GDS_getNewTabGDSid(rulelistArray, tempTableList, negateTempTableList);
		for(int i=0; i<newTabGDSid.size(); i++)
		{
			int gdsid = newTabGDSid.get(i);
			String platform = "";
			int samplenum = -1;
			String validity = "";
			String organism = "";
			String inputorganism = "";
			String inputdisease = "";
        	ArrayList<Integer> allIndices = getAllIndex(gdsid, consolidateValidRecords);
			LOG.info("backendcountroller.java - gdsid = "+gdsid);
			LOG.info("backendcountroller.java - allIndices = "+allIndices.toString());
			if(allIndices.size()>0)
			{
				ArrayList<String> organismList=new ArrayList<String>();
				ArrayList<String> diseaseList=new ArrayList<String>();
				
				for(int j=0; j<allIndices.size(); j++)
				{
					gdsrecordmini r = consolidateValidRecords.get(allIndices.get(j));
					LOG.info("backendcountroller.java - r = "+r.toString());
					String currInputOrganism = r.getInputOrganism();
					String currInputDisease = r.getInputDisease();
					if(!organismList.contains(currInputOrganism))
						organismList.add(currInputOrganism);
					if(!diseaseList.contains(currInputDisease))
						diseaseList.add(currInputDisease);
					if(j == 0)
					{
						platform = r.getPlatform();
						samplenum = r.getSampleNum();
						validity = r.getValidity();
						organism = r.getOrganism();
					}
				}
				for(int j=0; j<organismList.size(); j++)
				{
					inputorganism=inputorganism + organismList.get(j);
					if(j<organismList.size()-1)
						inputorganism=inputorganism +",";
				}
				for(int j=0; j<diseaseList.size(); j++)
				{
					inputdisease=inputdisease + diseaseList.get(j);
					if(j<diseaseList.size()-1)
						inputdisease=inputdisease +",";
				}
			}
			LOG.info("backendcountroller.java [getNewTabRecords]- inputdisease = "+inputdisease);
			LOG.info("backendcountroller.java [getNewTabRecords]- inputorganism = "+inputorganism);
			gdsrecordmini newR=new gdsrecordmini(gdsid, organism, platform, samplenum, validity, inputorganism, inputdisease);
			newTabRecords.add(newR);
		}

		//delete all temp tables
		pgController.GDS_removeAllNewTabTempTables(tempTableList);
		pgController.GDS_removeAllNewTabTempTables(negateTempTableList);
		return gson.toJson(newTabRecords);
	}
	/*private String getOS() {
		String os = System.getProperty("os.name").toLowerCase();
	
		if(os.indexOf("mac") >= 0){
		   return "MAC";
		}
		else if(os.indexOf("win") >= 0){
		   return "WIN";
		}
		else if(os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0){
		   return "LINUX/UNIX";
		}
		else if(os.indexOf("sunos") >= 0){
		   return "SOLARIS";
		}
		return os;
	}*/
	
	@ResponseBody
	@RequestMapping(path="/setDownloadFoldername", method=RequestMethod.POST)
    //we will store the downloaded SOFT files according to organism-disease (i.e., <human,lung cancer>)
	//suppose the selected records are from the <human,lung cancer> tab, then the files are downloaded into the folder for <human, lung cancer>
	//** Note: each call to downloadSOFT will be from a single valid tab (i.e., all records selected have same organism-disease)
	public void setDownloadFoldername(@RequestBody String foldername) {
		LOG.info("backendcountroller.java - setDownloadFoldername = ["+foldername+"]");
		String foldernameDecoded="";
		try {
			foldernameDecoded=URLDecoder.decode(foldername, StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException e) {
			// not going to happen - value came from JDK's own StandardCharsets
		}
		foldernameDecoded=foldernameDecoded.replaceAll("=", "");
		foldernameDecoded=foldernameDecoded.replaceAll(" ", "");
		foldernameDecoded=foldernameDecoded.replaceAll(",", "_");
		LOG.info("backendcountroller.java - setDownloadFoldername after replaceAll = ["+foldernameDecoded+"]");
		this.currFolderName=foldernameDecoded;
	}


	@ResponseBody
	@RequestMapping(path="/downloadSOFT", method=RequestMethod.POST)
    //we will store the downloaded SOFT files according to organism-disease (i.e., <human,lung cancer>)
	//suppose the selected records are from the <human,lung cancer> tab, then the files are downloaded into the folder for <human, lung cancer>
	//** Note: each call to downloadSOFT will be from a single valid tab (i.e., all records selected have same organism-disease)
	public String downloadSOFT(@RequestBody String jsonRecordList) {
		LOG.info("backendcountroller.java - downloadSOFT ");
		ArrayList<gdsrecordmini> recordsToDownload=gson.fromJson(jsonRecordList, new TypeToken<ArrayList<gdsrecordmini>>(){}.getType());
		LOG.info("backendcountroller.java - downloadSOFT size = "+recordsToDownload.size());
		String status_msg=ftpController.download(recordsToDownload, pgController, currFolderName);
		LOG.info("backendcountroller.java - status_msg="+status_msg);
		return status_msg;
		//this.sendMessages(status_msg);
		//this.messageTemplate.convertAndSend("/download/status", msg);	
	}

	@ResponseBody
	@RequestMapping(path="/setupPython", method=RequestMethod.POST)
    public String setupPython() {
		String status_msg = "NLP pipeline set up successfully.";
		LOG.info("backendcountroller.java - setupPython ");
		pyController.python_setupPython();

		return status_msg;
	}

	@ResponseBody
	@RequestMapping(path="/initDBTable", method=RequestMethod.POST)
    public String initDBTable() {
		String status_msg = "DB initialization complete.";
		LOG.info("backendcountroller.java - initDBTable ");
		pgController.GDS_initDBTable();
		return status_msg;
	}
}
