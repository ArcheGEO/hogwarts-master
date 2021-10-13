package de.jonashackt.springbootvuejs.domain;

public class rulelist {
    private boolean negation;//if true, the tab is negated
    private int resultTabIndex;//index of the tab
    private String operator;//operator will be '' if it's not assigned. Otherwise, it is AND or OR
    
    public rulelist(){

    }

    public rulelist(boolean negation, int resultTabIndex, String operator){
        this.negation = negation;
        this.resultTabIndex = resultTabIndex;
        this.operator = operator;
    }

    public boolean getNegation(){
        return negation;
    }

    public int getResultTabIndex(){
        return resultTabIndex;
    }

    public String getOperator(){
        return operator;
    }

    public String toString() {
        return "negation="+negation+" resultTabIndex="+resultTabIndex+" operator="+operator;
    }
}
