package de.jonashackt.springbootvuejs.domain;

import java.io.Serializable;

public class gdsrecordviewId implements Serializable{
    private int gdsid;
    private String inputorganism;
    private String inputdisease;

    // default constructor
    public gdsrecordviewId(){

    }
    
    public gdsrecordviewId(int gdsid, String inputorganism, String inputdisease) {
        this.gdsid = gdsid;
        this.inputorganism = inputorganism;
        this.inputdisease = inputdisease;
    }

    // equals() and hashCode()
}
