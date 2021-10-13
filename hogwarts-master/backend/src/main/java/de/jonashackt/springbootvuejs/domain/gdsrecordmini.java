package de.jonashackt.springbootvuejs.domain;

public class gdsrecordmini {
    private int gdsid;
    private String organism;
    private String platform;
    private int samplenum;
    private String validity;
    private String inputorganism;
    private String inputdisease;
    
    public gdsrecordmini (){

    }

    public gdsrecordmini (int gdsid, String organism, String platform, int samplenum, String validity, String inputorganism, String inputdisease){
        this.gdsid = gdsid;
        this.organism = organism;
        this.platform = platform;
        this.samplenum = samplenum;
        this.validity = validity;
        this.inputorganism = inputorganism;
        this.inputdisease = inputdisease;
    }

    public int getGDSID()
    {
        return this.gdsid;
    }

    public String getInputOrganism()
    {
        return this.inputorganism;
    }

    public String getInputDisease()
    {
        return this.inputdisease;
    }

    public String getValidity()
    {
        return this.validity;
    }

    public String getPlatform()
    {
        return this.platform;
    }

    public int getSampleNum()
    {
        return this.samplenum;
    }

    public String getOrganism()
    {
        return this.organism;
    }

    public void setInputDisease(String inputdisease)
    {
        this.inputdisease = inputdisease;
    }

    public void setInputOrganism(String inputorganism)
    {
        this.inputorganism = inputorganism;
    }

    public String toString() {
        return "gdsid="+gdsid+" organism="+organism+" platform="+platform+" samplenum="+samplenum
        +" validity="+validity+" inputorganism="+inputorganism+" inputdisease="+inputdisease;
    }
}
