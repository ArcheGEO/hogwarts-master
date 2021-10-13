package de.jonashackt.springbootvuejs.domain;

public class gdsrecordinvalid {
    private int gdsid;
    private String observedorganism;
    private String inputorganism;
    private String inputdisease;
    private String invalidreason;
    
    public gdsrecordinvalid (){

    }

    public gdsrecordinvalid (int gdsid, String observedorganism, String inputorganism, String inputdisease, 
    boolean organismmismatch, boolean diseasemismatch, boolean diseaseunverified){
        String organismMismatchDiseaseUnverified="Mismatched organism, unverified disease";
        String organismValidDiseaseMismatch="Valid organism, mismatched disease";
        String organismMismatchDiseaseValid="Mismatched organism, valid disease";
        String organismAndDiseaseMismatch="Mismatched organism and disease";

        this.gdsid = gdsid;
        this.observedorganism = observedorganism;
        this.inputorganism = inputorganism;
        this.inputdisease = inputdisease;
        this.invalidreason = "";
        if(organismmismatch && diseasemismatch)
            this.invalidreason = organismAndDiseaseMismatch;
        else{
            if(organismmismatch)
            {
                if(diseaseunverified)
                    this.invalidreason = organismMismatchDiseaseUnverified;
                if(!diseasemismatch)
                    this.invalidreason = organismMismatchDiseaseValid;
            }
            
            if(diseasemismatch && !organismmismatch)
                this.invalidreason = organismValidDiseaseMismatch;
        }
    }

    public int getGDSID(){
        return this.gdsid;
    }

    public String getInputOrganism(){
        return this.inputorganism;
    }

    public String getInputDisease(){
        return this.inputdisease;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Gdsrecordinvalid{");
        sb.append("gdsid='").append(gdsid).append('\'');
        sb.append(", observedorganism='").append(observedorganism).append('\'');
        sb.append(", inputorganism='").append(inputorganism).append('\'');
        sb.append(", inputdisease='").append(inputdisease).append('\'');
        sb.append(", invalidreason='").append(invalidreason).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
