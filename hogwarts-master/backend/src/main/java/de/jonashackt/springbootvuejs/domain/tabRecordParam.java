package de.jonashackt.springbootvuejs.domain;

import java.util.ArrayList;

public class tabRecordParam {
    ArrayList<rulelist> ruleListArray = new ArrayList<rulelist>();
    ArrayList<ArrayList<gdsrecordmini>> validItemsArray = new ArrayList<ArrayList<gdsrecordmini>>();
    ArrayList<String> validTabArray = new ArrayList<String>();

    public tabRecordParam() {

    }

    public ArrayList<rulelist> getRuleListArray(){
        return ruleListArray;
    }

    public ArrayList<ArrayList<gdsrecordmini>> getValidItemsArray(){
        return validItemsArray;
    }

    public ArrayList<String> getValidTabArray(){
        return validTabArray;
    }

    public String toString(){
        String retString="";
        if(ruleListArray.size()>0){
            for(int i=0; i<ruleListArray.size(); i++)
                retString = retString + ruleListArray.get(i).toString() +"\n";
        }
        if(validItemsArray.size()>0){
            for(int i=0; i<validItemsArray.size(); i++)
                retString = retString + validItemsArray.get(i).toString() + "\n";
        }
        retString = retString + validTabArray.toString();
        return retString;
    }
}
