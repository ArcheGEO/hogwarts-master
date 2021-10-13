package de.jonashackt.springbootvuejs.domain;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSearchPage {
	private ArrayList<String> gdsidList=new ArrayList<String>();
	private ArrayList<String> organismList=new ArrayList<String>();
	private ArrayList<String> diseaseList=new ArrayList<String>();
	
	private static final Logger LOG = LoggerFactory.getLogger(WebSearchPage.class);

	public WebSearchPage(){

	}
	
	public WebSearchPage(ArrayList<String> gdsidList, ArrayList<String> organismList, ArrayList<String> diseaseList)
	{
		this.gdsidList = gdsidList;
		this.organismList = organismList;
		this.diseaseList = diseaseList;
	}

	public ArrayList<String> getGDSIDList()
	{
		return gdsidList;
	}

	public void setGDSIDList(ArrayList<String> gdsidList)
	{
		this.gdsidList = gdsidList;
	}

	public ArrayList<String> getOrganismList()
	{
		return organismList;
	}

	public void setOrganismList(ArrayList<String> organismList)
	{
		this.organismList = organismList;
	}

	public ArrayList<String> getDiseaseList()
	{
		return diseaseList;
	}

	public void setDiseaseList(ArrayList<String> diseaseList)
	{
		this.diseaseList = diseaseList;
	}

	public String toString()
	{
		final StringBuilder sb = new StringBuilder("WebSearchPage{");
        sb.append("gdsidList='").append(gdsidList.toString()).append('\'');
        sb.append(", organismList='").append(organismList.toString()).append('\'');
		sb.append(", diseaseList='").append(diseaseList.toString()).append('\'');
        sb.append('}');
        return sb.toString();
	}
}