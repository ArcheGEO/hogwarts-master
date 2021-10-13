package de.jonashackt.springbootvuejs.domain;

import java.util.Objects;
import javax.persistence.*;

//CREATE TABLE gdsrecord(id serial PRIMARY KEY, organism VARCHAR(255), title VARCHAR(1000), summary VARCHAR(5000), platform VARCHAR(50), sampleNum INTEGER, fName VARCHAR(255));

@Entity
@IdClass(gdsrecordviewId.class)
@Table(name = "gdsrecordview")
public class gdsrecordview {

    @Id
    @Column(name="gdsid")
    private int gdsid;

    @Column(name="organism")
    private String organism;

    @Column(name="title")
    private String title;

    @Column(name="summary")
    private String summary;

    @Column(name="platform")
    private String platform;

    @Column(name="samplenum")
    private int samplenum;

    @Column(name="fname")
    private String fname;

    @Id
    @Column(name="inputorganism")
    private String inputorganism;

    @Id
    @Column(name="inputdisease")
    private String inputdisease;

    @Column(name="validrecord")
    private boolean validrecord;

    @Column(name="organismmismatch")
    private boolean organismmismatch;
    
    @Column(name="diseasemismatch")
    private boolean diseasemismatch;

    @Column(name="diseaseunverified")
    private boolean diseaseunverified;

    public gdsrecordview() {
    }

    public gdsrecordview(int gdsid, String organism, String title, String summary, 
    String platform, int samplenum, String fName, String inputorganism, String inputdisease, 
    boolean validrecord, boolean organismmismatch, boolean diseasemismatch, boolean diseaseunverified) {
        this.gdsid = gdsid;
        this.organism = organism;
        this.title = title;
        this.summary = summary;
        this.platform = platform;
        this.samplenum = samplenum;
        this.fname = fName;
        this.inputorganism = inputorganism;
        this.inputdisease = inputdisease;
        this.validrecord = validrecord;
        this.organismmismatch = organismmismatch;
        this.diseasemismatch = diseasemismatch;
        this.diseaseunverified = diseaseunverified;
    }

    public int getGdsid() {
        return gdsid;
    }

    public void setGdsid(int gdsid) {
        this.gdsid = gdsid;
    }

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public int getSampleNum() {
        return samplenum;
    }

    public void setSampleNum(int sampleNum) {
        this.samplenum = sampleNum;
    }

    public String getFName() {
        return fname;
    }

    public void setFName(String fName) {
        this.fname = fName;
    }

    public boolean getValidRecord() {
        return validrecord;
    }

    public void setValidRecord(boolean validrecord) {
        this.validrecord = validrecord;
    }

    public boolean getOrganismMismatch() {
        return organismmismatch;
    }

    public void setOrganismMismatch(boolean organismmismatch) {
        this.organismmismatch = organismmismatch;
    }

    public boolean getDiseaseMismatch() {
        return diseasemismatch;
    }

    public void setDiseaseMismatch(boolean diseasemismatch) {
        this.diseasemismatch = diseasemismatch;
    }

    public boolean getDiseaseUnverified() {
        return diseaseunverified;
    }

    public void setDiseaseUnverified(boolean diseaseunverified) {
        this.diseaseunverified = diseaseunverified;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final gdsrecordview other = (gdsrecordview) obj;
        if ((!Objects.equals(this.gdsid, other.gdsid)) ||
            (!Objects.equals(this.organism, other.organism)) ||
            (!Objects.equals(this.title, other.title)) ||
            (!Objects.equals(this.summary, other.summary)) ||
            (!Objects.equals(this.platform, other.platform)) ||
            (!Objects.equals(this.samplenum, other.samplenum)) ||
            (!Objects.equals(this.fname, other.fname)) ||
            (!Objects.equals(this.inputorganism, other.inputorganism)) ||
            (!Objects.equals(this.inputdisease, other.inputdisease))) {
            return false;
        }
        else return true;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GDSRecordView{");
        sb.append(", organism=").append(organism);
        sb.append(", title=").append(title);
        sb.append(", summary=").append(summary);
        sb.append(", platform=").append(platform);
        sb.append(", samplenum=").append(samplenum);
        sb.append(", fname='").append(fname);
        sb.append(", inputorganism='").append(inputorganism);
        sb.append(", inputdisease='").append(inputdisease);
        sb.append(", validrecord='").append(validrecord);
        sb.append(", organismmismatch='").append(organismmismatch);
        sb.append(", diseasemismatch='").append(diseasemismatch);
        sb.append(", diseaseunverified='").append(diseaseunverified).append('\'');
        sb.append('}');
        return sb.toString();
    }
}