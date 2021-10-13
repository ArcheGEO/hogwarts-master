package de.jonashackt.springbootvuejs.domain;

import java.util.Objects;
import javax.persistence.*;

//CREATE TABLE gdsrecord(id serial PRIMARY KEY, organism VARCHAR(255), title VARCHAR(1000), summary VARCHAR(5000), platform VARCHAR(50), sampleNum INTEGER, fName VARCHAR(255));

@Entity
@Table(name = "gdsrecord")
public class gdsrecord {

    /*@Id
    @SequenceGenerator(name="gds_sequence",sequenceName="seq", initialValue=1, allocationSize=1)
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="gds_sequence")
    private long id;*/
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

    public gdsrecord() {
    }

    public gdsrecord(int gdsid, String organism, String title, String summary, 
    String platform, int samplenum, String fName) {

        this.gdsid = gdsid;
        this.organism = organism;
        this.title = title;
        this.summary = summary;
        this.platform = platform;
        this.samplenum = samplenum;
        this.fname = fName;
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
        final gdsrecord other = (gdsrecord) obj;
        if ((!Objects.equals(this.organism, other.organism)) ||
            (!Objects.equals(this.title, other.title)) ||
            (!Objects.equals(this.summary, other.summary)) ||
            (!Objects.equals(this.platform, other.platform)) ||
            (!Objects.equals(this.samplenum, other.samplenum)) ||
            (!Objects.equals(this.fname, other.fname))) {
            return false;
        }
        return Objects.equals(this.gdsid, other.gdsid);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GDSRecord{");
        sb.append("gdsid=").append(gdsid);
        sb.append(", organism=").append(organism);
        sb.append(", title=").append(title);
        sb.append(", summary=").append(summary);
        sb.append(", platform=").append(platform);
        sb.append(", samplenum=").append(samplenum);
        sb.append(", fname='").append(fname).append('\'');
        sb.append('}');
        return sb.toString();
    }
}