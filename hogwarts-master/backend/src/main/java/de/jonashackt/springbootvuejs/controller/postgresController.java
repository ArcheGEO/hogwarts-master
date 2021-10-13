package de.jonashackt.springbootvuejs.controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jonashackt.springbootvuejs.domain.gdsrecordinvalid;
import de.jonashackt.springbootvuejs.domain.gdsrecordmini;
import de.jonashackt.springbootvuejs.domain.gdsrecordview;
import de.jonashackt.springbootvuejs.domain.rulelist;

public class postgresController {
    private String url_prefix="spring.datasource.url=";
    private String user_prefix="spring.datasource.username=";
    private String password_prefix="spring.datasource.password=";
    
    //private postgresCredentials credentials = new postgresCredentials();

    private String url = "";
    private String user = "";
    private String password = "";
    private boolean FOUND_URL=false;
    private boolean FOUND_USER=false;
    private boolean FOUND_PASSWORD=false;

    private static final Logger LOG = LoggerFactory.getLogger(postgresController.class);

    private ArrayList<String> TH_organism_syn_list=new ArrayList<String>();
    private ArrayList<String> TH_ncimidList=new ArrayList<String>();
    private ArrayList<String> TH_ncitidList=new ArrayList<String>();
    private ArrayList<String> TH_found_ncitidList=new ArrayList<String>();
    
    private Connection conn;

    //we read directly from application.properties file to get the url, user and password for postgres
    public postgresController() {
        
        //String filename = directory+"application.properties";
        
        //System.out.println("Current relative path is: " + filename);
        //Path root = Path.getRoot();
        //System.out.println("[postgresController] filename is: " + filename);
        
        
        //String master_directory = currentRelativePath.toAbsolutePath().toString();
        //System.out.println("[postgresController] Current absolute path is: " + master_directory);
        //String filename = master_directory + "\\hogwarts-master\\backend\\src\\main\\resources\\"+"application.properties";
        //System.out.println("[postgresController] filename is: " + filename);
        BufferedReader reader;
		try {
            URL resource = this.getClass().getResource("/application.properties");
            File f = Paths.get(resource.toURI()).toFile();
			reader = new BufferedReader(new FileReader(f));
			String line = reader.readLine();
			while (line != null) {
                if(line.contains(url_prefix))
                {
                    url=extractValue(line, url_prefix);
                    FOUND_URL=true;
                }
                if(line.contains(user_prefix))
                {
                    user=extractValue(line, user_prefix);
                    FOUND_USER=true;
                }
                if(line.contains(password_prefix))
                {
                    password=extractValue(line, password_prefix);
                    FOUND_PASSWORD=true;
                }

                if(FOUND_URL && FOUND_USER && FOUND_PASSWORD)
                    line=null;//can stop since we found all
                else
				    line = reader.readLine();// read next line
			}
			reader.close();
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
        conn = connect();
    }  
    
    //extract portion of string in s that is to the right of its substring sub_s
    private String extractValue(String s, String sub_s)
    {
        int index=s.indexOf(sub_s);
        if(index!=-1)
            return s.substring(index+sub_s.length());
        else
            return null;
    }

    public postgresController(String url, String user, String password){
        this.url=url;
        this.user=user;
        this.password=password;
    }

    private Connection connect() {
        Connection c=null;
        
        if(url==null || user==null || password==null || url.length()==0 || user.length()==0 || password.length()==0)
            System.out.println("postgresController.java ERROR with either url, user or password. Check values of these and ensure they are not null or empty.");
        else
        {
            LOG.info("url="+url);
            LOG.info("user="+user);
            LOG.info("password="+password);
            try{
                Class.forName("org.postgresql.Driver");
                c=DriverManager.getConnection(url, user, password);
            }catch(Exception e)
            {
                e.printStackTrace();
                System.err.println("connect() "+e.getClass().getName()+": "+e.getMessage());
            }
        }
        return c;
    }

    public void closeConnection(Connection conn)
    {
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /*+++++++++++++++++++++++++++++++ START GDS-related functions (GDS_xxxx) +++++++++++++++++++++++++++++*/
    public void GDS_initDBTable() {
        String SQL_CREATE_SEQ = "CREATE SEQUENCE IF NOT EXISTS seq;";
        String SQL_CREATE_VIEWSEQ = "CREATE SEQUENCE IF NOT EXISTS viewseq;";
        String SQL_CREATE_GDSRECORDVIEW = "CREATE TABLE IF NOT EXISTS gdsrecordview(gdsid INTEGER NOT NULL, organism VARCHAR(255), title VARCHAR(1000), summary VARCHAR(5000), platform VARCHAR(50), "+
            "samplenum INTEGER, fname VARCHAR(255), inputorganism VARCHAR(50) NOT NULL, inputdisease VARCHAR(50) NOT NULL, "+
            "validrecord BOOLEAN, organismmismatch BOOLEAN, diseasemismatch BOOLEAN, diseaseunverified BOOLEAN, PRIMARY KEY (gdsid, inputorganism, inputdisease));";
        String SQL_CREATE_GDSRECORD = "CREATE TABLE IF NOT EXISTS gdsrecord(gdsid INTEGER PRIMARY KEY, "+
            "organism VARCHAR(255), title VARCHAR(1000), summary VARCHAR(5000), platform VARCHAR(50), "+
            "samplenum INTEGER, fname VARCHAR(255));";
        LOG.info("postgresController.java GDS_initDBTable");
        try {
            Statement stmt = conn.createStatement();
            stmt.execute(SQL_CREATE_SEQ);
            stmt.execute(SQL_CREATE_VIEWSEQ);
            stmt.execute(SQL_CREATE_GDSRECORDVIEW);
            stmt.execute(SQL_CREATE_GDSRECORD);
            stmt.close();
        } catch (SQLException ex) {
            System.err.println( "GDS_initDBTable() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }   
    }

    public void GDS_dropcascade(String tableName) {
        String SQL = "DROP TABLE IF EXISTS "+tableName+" CASCADE;";
        LOG.info("postgresController.java GDS_dropcascade tableName = "+tableName);
        try {
            Statement stmt = conn.createStatement();
            stmt.execute(SQL);
            stmt.close();
        } catch (SQLException ex) {
            System.err.println( "GDS_dropcascade() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }   
    }

    public void GDS_resetSequence(String seqName) {
        String SQL = "ALTER SEQUENCE "+seqName+" RESTART WITH 1";
        LOG.info("postgresController.java resetting sequence of "+seqName);
        try {
            Statement stmt = conn.createStatement();
            stmt.execute(SQL);
            stmt.close();
        } catch (SQLException ex) {
            System.err.println( "GDS_resetSequence() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
    }

    public ArrayList<Integer> GDS_getGDSIDPresentInDB(ArrayList<Integer> GDSIDList) {
        String preSQL = "SELECT gdsid FROM public.gdsrecord WHERE gdsid IN ";
        String postSQL = "";
        String SQL = "";
        ArrayList<Integer> gdsList=new ArrayList<Integer>();

        for(int i=0; i<GDSIDList.size(); i++)
        {
            postSQL=postSQL +GDSIDList.get(i);
            if(i!=GDSIDList.size()-1)
                postSQL=postSQL + ",";
        }
        SQL = preSQL+"("+postSQL+");";
        LOG.info("postgresController.java GDS_getGDSIDPresentInDB SQL="+SQL);

        if(postSQL.length()>0)
        {
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(SQL);

                while ( rs.next() ) {
                    int gdsid = rs.getInt("gdsid");
                    gdsList.add(gdsid);
                }
                stmt.close();
            } catch (SQLException ex) {
                System.err.println( "getMiniRecords() "+ ex.getClass().getName()+": "+ ex.getMessage() );
            }
        }
        LOG.info("postgresController.java gdsList.size()="+gdsList.size());
        LOG.info("postgresController.java FINISH GDS_getMiniRecordsGDSIDPresentInDB");
        return gdsList;
    }
        
	private String GDS_formatStringForPostgres(String unformattedString)
	{
		String formattedString;

		formattedString=unformattedString.replaceAll("'", "''");

		return formattedString;
	}

    public ArrayList<gdsrecordview> GDS_copyGDSDetailsToView(ArrayList<Integer> GDSIDList, ArrayList<String> inputorganismList, String inputdisease)
    {
        String preSQL = "SELECT * FROM public.gdsrecord WHERE gdsid IN ";
        String postSQL = "(";
        String SQL = "";
        String write_preSQL = "INSERT INTO public.gdsrecordview(gdsid, organism, title, summary, platform, samplenum, fname,"+
        " inputorganism, inputdisease, validrecord, organismmismatch, diseasemismatch, diseaseunverified) VALUES ";
        String write_postSQL = "";
        String write_SQL = "";
        LOG.info("postgresController.java GDS_copyGDSDetailsToView");
        int counter=0;        
        ArrayList<gdsrecordview> recordviewList=new ArrayList<gdsrecordview>();

        for(int i=0; i<GDSIDList.size(); i++)
        {
            postSQL=postSQL +"\'"+GDSIDList.get(i)+"\'";
            if(i!=GDSIDList.size()-1)
                postSQL=postSQL + ",";
        }
        SQL = preSQL+postSQL+");";
        //LOG.info("postgresController.java GDS_copyGDSDetailsToView SQL="+SQL);
        try {
            //Connection conn = connect();
            //LOG.info("Opened database successfully");
            Statement stmt = conn.createStatement();
            Statement write_stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL);

            while ( rs.next() ) {
                int gdsid = rs.getInt("gdsid");
                String organism = rs.getString("organism");
                String title = GDS_formatStringForPostgres(rs.getString("title"));
                String summary = GDS_formatStringForPostgres(rs.getString("summary"));
                String platform = rs.getString("platform");
                int samplenum = rs.getInt("samplenum");
                String fname = rs.getString("fname");
                Boolean validrecord = true;
                Boolean organismmismatch = false;
                Boolean diseasemismatch = false;
                Boolean diseaseunverified = false;
                String validity="Valid";
                if(inputdisease.contains("'"))
                    inputdisease=inputdisease.replace("'", "&apos;");
                //write_postSQL=write_postSQL+"(nextval(\'viewseq\'),"+"\'"+gdsid+"\',"+"\'"+organism+"\',"
                //+"\'"+title+"\',"+"\'"+summary+"\',"+"\'"+platform+"\',"+samplenum+",\'"+fname+"\',"
                //+"\'"+inputorganismList.get(0)+"\',"+"\'"+inputdisease+"\',"+"\'"+validrecord+"\',"+organismmismatch+",\'"+diseasemismatch+",\'"+diseaseunverified+"\')";
                write_postSQL=write_postSQL+"(\'"+gdsid+"\',"+"\'"+organism+"\',"
                +"\'"+title+"\',"+"\'"+summary+"\',"+"\'"+platform+"\',"+samplenum+",\'"+fname+"\',"
                +"\'"+inputorganismList.get(0)+"\',"+"\'"+inputdisease+"\',"+validrecord+","+organismmismatch+","+diseasemismatch+","+diseaseunverified+")";
                
                if(counter!=GDSIDList.size()-1)
                    write_postSQL=write_postSQL + ",";

                gdsrecordview r=new gdsrecordview(gdsid, organism, title, summary, platform, samplenum, 
                    fname, inputorganismList.get(0), inputdisease, validrecord, organismmismatch, diseasemismatch, diseaseunverified);
				recordviewList.add(r);

                counter++;
            }
            write_SQL=write_preSQL+write_postSQL+"  ON CONFLICT DO NOTHING;";
            //LOG.info("postgresController.java copyGDSDetailsToView write_SQL="+write_SQL);
            write_stmt.executeUpdate(write_SQL);
            conn.commit();
            write_stmt.close();
            stmt.close();
            //conn.close();
        } catch (SQLException ex) {
            System.err.println( "GDS_copyGDSDetailsToView() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
        LOG.info("postgresController.java FINISH GDS_copyGDSDetailsToView recordviewList size="+recordviewList.size());
        return recordviewList;
    }

    public ArrayList<gdsrecordinvalid> GDS_getInvalidRecords() {
        ArrayList<gdsrecordinvalid> invalidRecords = new ArrayList<gdsrecordinvalid>();
        String SQL = "SELECT gdsid, organism, inputorganism, inputdisease, organismmismatch, diseasemismatch, diseaseunverified"+
        " FROM public.gdsrecordview WHERE validrecord=false;";
        LOG.info("postgresController.java getInvalidRecords");
        try {
            //Connection conn = connect();
            //LOG.info("Opened database successfully");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL);

            while ( rs.next() ) {
                int gdsid = rs.getInt("gdsid");
                String organism = rs.getString("organism");
                String inputorganism = rs.getString("inputorganism");
                String inputdisease = rs.getString("inputdisease");
                Boolean organismmismatch = rs.getBoolean("organismmismatch");
                Boolean diseasemismatch = rs.getBoolean("diseasemismatch");
                Boolean diseaseunverified = rs.getBoolean("diseaseunverified");
       
                //LOG.info("gdsid=%s, organism=%s, inputorganism=%s, inputdisease=%s, organismmismatch=%s, diseasemismatch=%s", 
                //gdsid, organism, inputorganism, inputdisease, organismmismatch, diseasemismatch);

                gdsrecordinvalid r = new gdsrecordinvalid(gdsid, organism, inputorganism, inputdisease, organismmismatch, diseasemismatch, diseaseunverified);
                invalidRecords.add(r);
             }

            stmt.close();
            //conn.close();
        } catch (SQLException ex) {
            System.err.println( "GDS_getInvalidRecords() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
        return invalidRecords;
    }

    public void GDS_setInvalidRecordsAsValid(ArrayList<gdsrecordinvalid> invalidRecords) {
        String preSQL = "UPDATE public.gdsrecordview SET validrecord=true, organismmismatch=false, diseasemismatch=false WHERE ";
        String postSQL = "";
        String SQL = "";
        LOG.info("postgresController.java GDS_setInvalidRecordsAsValid");

        for(int i=0; i<invalidRecords.size(); i++)
        {
            gdsrecordinvalid r = invalidRecords.get(i);
            postSQL=postSQL +"(inputorganism=\'"+r.getInputOrganism()+"\' and inputdisease=\'"+r.getInputDisease()
            +"\' and gdsid=\'"+r.getGDSID()+"\')";
            if(i!=invalidRecords.size()-1)
                postSQL=postSQL + " OR ";
        }
        SQL = preSQL+postSQL+";";
        LOG.info("postgresController.java GDS_setInvalidRecordsAsValid SQL="+SQL);

        try {
            //Connection conn = connect();
            //LOG.info("Opened database successfully");
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(SQL);
            stmt.close();
            conn.commit();
            //conn.close();
        } catch (SQLException ex) {
            System.err.println( "GDS_setInvalidRecordsAsValid() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
        LOG.info("postgresController.java FINISH GDS_setInvalidRecordsAsValid");
    }

    public ArrayList<gdsrecordmini> GDS_getConsolidatedValidRecords()
    {
        ArrayList<gdsrecordmini> records=new ArrayList<gdsrecordmini>();
        String SQL = "SELECT gdsid, organism, platform, samplenum, inputorganism, inputdisease, validrecord FROM public.gdsrecordview WHERE validrecord=true";
        LOG.info("postgresController.java GDS_getConsolidatedValidRecords");
        
        try {
            //Connection conn = connect();
            //LOG.info("Opened database successfully");
            Statement stmt = conn.createStatement();
            LOG.info("postgresController.java GDS_getConsolidatedValidRecords SQL="+SQL);
            ResultSet rs = stmt.executeQuery(SQL);
            while ( rs.next() ) {
                int gdsid = rs.getInt("gdsid");
                String organism = rs.getString("organism");
                String platform = rs.getString("platform");
                int samplenum = rs.getInt("samplenum");
                String inputorganism = rs.getString("inputorganism");
                String inputdisease = rs.getString("inputdisease");
                String validity = rs.getString("validrecord");
                gdsrecordmini r = new gdsrecordmini(gdsid, organism, platform, samplenum, validity, inputorganism, inputdisease);
                records.add(r);
            }
            LOG.info("records.size()="+records.size());
            stmt.close();
            //conn.close();
        } catch (SQLException ex) {
            System.err.println( "GDS_getConsolidatedValidRecords() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
        
        return records;
    }

    public ArrayList<Integer> GDS_getConsolidateValidRecords_gdsid()
    {
        ArrayList<Integer> records=new ArrayList<Integer>();
        String SQL = "SELECT DISTINCT gdsid FROM public.gdsrecordview WHERE validrecord=true";
        LOG.info("postgresController.java GDS_getConsolidateValidRecords_gdsid");
        
        try {
            //Connection conn = connect();
            //LOG.info("Opened database successfully");
            Statement stmt = conn.createStatement();
            LOG.info("postgresController.java GDS_getConsolidateValidRecords_gdsid SQL="+SQL);
            ResultSet rs = stmt.executeQuery(SQL);
            while ( rs.next() ) {
                int gdsid = rs.getInt("gdsid");
                records.add(gdsid);
            }
            LOG.info("records.size()="+records.size());
            stmt.close();
            //conn.close();
        } catch (SQLException ex) {
            System.err.println( "GDS_getConsolidateValidRecords_gdsid() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
        
        return records;
    }

    public ArrayList<ArrayList<gdsrecordmini>> GDS_getAllValidRecords(ArrayList<ArrayList<String>>organismDiseasePairList)
    {
        ArrayList<ArrayList<gdsrecordmini>> records=new ArrayList<ArrayList<gdsrecordmini>>();
        String preSQL = "SELECT gdsid, organism, platform, samplenum, inputorganism, inputdisease FROM public.gdsrecordview WHERE ";
        String postSQL = "";
        String SQL = "";
        LOG.info("postgresController.java GDS_getAllValidRecords");
        
        try {
            //Connection conn = connect();
            //LOG.info("Opened database successfully");
            Statement stmt = conn.createStatement();
            
            for(int i=0; i<organismDiseasePairList.size(); i++)
            {
                postSQL="";
                String currOrganism=organismDiseasePairList.get(i).get(0);
                String currDisease=organismDiseasePairList.get(i).get(1);
                ArrayList<gdsrecordmini> currList=new ArrayList<gdsrecordmini>();
                postSQL=postSQL+"inputorganism=\'"+currOrganism+"\' AND inputdisease=\'"+currDisease+"\' AND validrecord=true";
                SQL=preSQL+postSQL;
                LOG.info("postgresController.java GDS_getAllValidRecords SQL="+SQL);
                ResultSet rs = stmt.executeQuery(SQL);
                while ( rs.next() ) {
                    int gdsid = rs.getInt("gdsid");
                    String organism = rs.getString("organism");
                    String platform = rs.getString("platform");
                    int samplenum = rs.getInt("samplenum");
                    String inputorganism = rs.getString("inputorganism");
                    String inputdisease = rs.getString("inputdisease");
                    String validity = "Valid";//in this case, we retrieve the records directly from DB, validity will be based on validrecord boolean value
                    gdsrecordmini r = new gdsrecordmini(gdsid, organism, platform, samplenum, validity, inputorganism, inputdisease);
                    currList.add(r);
                }
                LOG.info("currList.size()="+currList.size());
                records.add(currList);
            }
            stmt.close();
            //conn.close();
        } catch (SQLException ex) {
            System.err.println( "GDS_getAllValidRecords() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
        
        return records;
    }

    public void GDS_createNewTabTempTable(String tableName, ArrayList<Integer> gdsidList)
    {
        String SQL_table = "CREATE TABLE IF NOT EXISTS "+tableName+"(gdsid integer PRIMARY KEY)";
        String SQL_gdsid = "INSERT INTO public."+tableName+"(gdsid) VALUES ";
        String SQL_gdsid_content = "";
        if(gdsidList.size()>0)
        {
            for(int i=0; i<gdsidList.size(); i++)
            {
                SQL_gdsid_content=SQL_gdsid_content+"("+gdsidList.get(i)+")";
                if(i<gdsidList.size()-1)
                    SQL_gdsid_content=SQL_gdsid_content+",";
            }
        }
        try {
            Statement stmt = conn.createStatement();
            stmt.execute(SQL_table);
            if(gdsidList.size()>0)
            {
                SQL_gdsid=SQL_gdsid+SQL_gdsid_content;
                LOG.info("postgresController.java SQL_gdsid="+SQL_gdsid);
                stmt.execute(SQL_gdsid);
            }
            stmt.close();
            //conn.close();
        } catch (SQLException ex) {
            System.err.println( "GDS_createNewTabTempTable() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
    }

    public void GDS_removeAllNewTabTempTables(ArrayList<String> tableList)
    {
        String SQL_table = "DROP TABLE IF EXISTS ";
        String tableContent = "";
        if(tableList.size()>0)
        {
            for(int i=0; i<tableList.size(); i++)
            {
                tableContent=tableContent+tableList.get(i);
                if(i<tableList.size()-1)
                    tableContent=tableContent+",";
            }
            try {
                Statement stmt = conn.createStatement();
                SQL_table=SQL_table+tableContent+" CASCADE";
                stmt.execute(SQL_table);
                stmt.close();
                //conn.close();
            } catch (SQLException ex) {
                System.err.println( "GDS_removeAllNewTabTempTables() "+ ex.getClass().getName()+": "+ ex.getMessage() );
            }
        }
    }

    public ArrayList<Integer> GDS_getNewTabGDSid(ArrayList<rulelist> rulelistArray, ArrayList<String> tempTableList, 
        ArrayList<String> negateTempTableList)
    {
        String SQL_rule="";
        ArrayList<Integer> gdsidList=new ArrayList<Integer>();
        if(rulelistArray.size()>0)
        {
            for(int i=0; i<rulelistArray.size(); i++)
            {
                rulelist rule = rulelistArray.get(i);
                boolean currNegation = rule.getNegation();
                int currValidItemIndex = rule.getResultTabIndex();
                String currOperator = rule.getOperator();
                String currRule_SQL = "(SELECT * FROM public.";
                String currRuleOp_SQL = ")";
                String currTempTableName;
                if(currNegation)
                    currTempTableName = negateTempTableList.get(currValidItemIndex);
                else
                    currTempTableName = tempTableList.get(currValidItemIndex);
                if(currOperator.compareTo("AND")==0)
                    currRuleOp_SQL=") INTERSECT ";
                if(currOperator.compareTo("OR")==0)
                    currRuleOp_SQL=") UNION ";
                currRule_SQL = currRule_SQL+currTempTableName+currRuleOp_SQL;
                SQL_rule = SQL_rule + currRule_SQL;
            }
        }
        LOG.info("[postgresCountroller.java GDS_getNewTabGDSid] SQL_rule = "+SQL_rule);
        if(SQL_rule.length()>0)
        {
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(SQL_rule);
    
                while ( rs.next() ) {
                    int gdsid = rs.getInt("gdsid");
                    if(!gdsidList.contains(gdsid))
                        gdsidList.add(gdsid);
                }
                rs.close();
                stmt.close();
            } catch (SQLException ex) {
                System.err.println( "GDS_getNewTabGDSid() "+ ex.getClass().getName()+": "+ ex.getMessage() );
            }
        }
        LOG.info("[postgresCountroller.java GDS_getNewTabGDSid] gdsidList = "+gdsidList.toString());
        return gdsidList;
    }

    public ArrayList<gdsrecordmini> GDS_getMiniRecords(ArrayList<gdsrecordinvalid> invalidRecords) {
        String preSQL = "SELECT gdsid, organism, platform, samplenum, inputorganism, inputdisease FROM public.gdsrecordview WHERE ";
        String postSQL = "";
        String SQL = "";
        LOG.info("postgresController.java GDS_getMiniRecords");
        ArrayList<gdsrecordmini> records=new ArrayList<gdsrecordmini>();

        for(int i=0; i<invalidRecords.size(); i++)
        {
            gdsrecordinvalid r = invalidRecords.get(i);
            postSQL=postSQL +"(inputorganism=\'"+r.getInputOrganism()+"\' and inputdisease=\'"+r.getInputDisease()
            +"\' and gdsid=\'"+r.getGDSID()+"\')";
            if(i!=invalidRecords.size()-1)
                postSQL=postSQL + " OR ";
        }
        SQL = preSQL+postSQL+";";
        LOG.info("postgresController.java GDS_getMiniRecords SQL="+SQL);

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL);

            while ( rs.next() ) {
                int gdsid = rs.getInt("gdsid");
                String organism = rs.getString("organism");
                String platform = rs.getString("platform");
                int samplenum = rs.getInt("samplenum");
                String inputorganism = rs.getString("inputorganism");
                String inputdisease = rs.getString("inputdisease");
                String validity = "Valid";
                LOG.info("gdsid=%s, organism=%s, inputorganism=%s, inputdisease=%s", 
                gdsid, organism, inputorganism, inputdisease);

                if(organism.compareTo(inputorganism)!=0)
                    validity="Organism Mismatch";

                gdsrecordmini r = new gdsrecordmini(gdsid, organism, platform, samplenum, validity, inputorganism, inputdisease);
                records.add(r);
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.err.println( "GDS_getMiniRecords() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
        LOG.info("postgresController.java FINISH GDS_getMiniRecords");
        return records;
    }

    public void GDS_setValidRecordsAsInvalid(ArrayList<gdsrecordmini> validRecords) {
        String preSQL = "UPDATE public.gdsrecordview SET validrecord=false, ";
        String postSQL = "";
        String SQL = "";
        LOG.info("postgresController.java GDS_setValidRecordsAsInvalid");

        try {
            for(int i=0; i<validRecords.size(); i++)
            {
                Statement stmt = conn.createStatement();
                gdsrecordmini r = validRecords.get(i);
                if(r.getValidity().compareTo("Organism Mismatch")==0 || 
                    r.getValidity().compareTo("Disease Mismatch")==0 ||
                    r.getValidity().compareTo("Organism and Disease Mismatch")==0 ||
                    r.getValidity().compareTo("Organism Mismatch, Disease Unverified")==0)
                {
                    if(r.getValidity().compareTo("Organism Mismatch")==0)
                        postSQL="organismmismatch=true, diseasemismatch=false WHERE (inputorganism=\'"+r.getInputOrganism()+"\' and inputdisease=\'"+r.getInputDisease()
                        +"\' and gdsid=\'"+r.getGDSID()+"\')";
                    else if(r.getValidity().compareTo("Disease Mismatch")==0)
                        postSQL="organismmismatch=false, diseasemismatch=true WHERE (inputorganism=\'"+r.getInputOrganism()+"\' and inputdisease=\'"+r.getInputDisease()
                        +"\' and gdsid=\'"+r.getGDSID()+"\')";
                    else if(r.getValidity().compareTo("Organism and Disease Mismatch")==0)
                        postSQL="organismmismatch=true, diseasemismatch=true WHERE (inputorganism=\'"+r.getInputOrganism()+"\' and inputdisease=\'"+r.getInputDisease()
                        +"\' and gdsid=\'"+r.getGDSID()+"\')";
                    else
                        postSQL="organismmismatch=true, diseaseunverified=true WHERE (inputorganism=\'"+r.getInputOrganism()+"\' and inputdisease=\'"+r.getInputDisease()
                        +"\' and gdsid=\'"+r.getGDSID()+"\')";
                }
                SQL = preSQL+postSQL+";";
                LOG.info("postgresController.java SQL="+SQL);
            
                stmt.executeUpdate(SQL);
                stmt.close();
            }
            conn.commit();
        } catch (SQLException ex) {
            System.err.println( "GDS_setValidRecordsAsInvalid() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
        LOG.info("postgresController.java FINISH GDS_setValidRecordsAsInvalid");
    }

    public ArrayList<String> GDS_getFilePathsToDownload(ArrayList<gdsrecordmini> records)
    {
        ArrayList<String> filepathList=new ArrayList<String>();
        String preSQL = "SELECT DISTINCT fname FROM public.gdsrecordview WHERE gdsid IN (";
        String postSQL = "";
        String SQL = "";
        LOG.info("postgresController.java GDS_getFilePathsToDownload");
        for(int i=0; i<records.size(); i++)
        {
            gdsrecordmini r = records.get(i);
            postSQL=postSQL+r.getGDSID();
            if(i<records.size()-1)
                postSQL=postSQL+",";
        }
        SQL=preSQL+postSQL+");";
        LOG.info("postgresController.java GDS_getFilePathsToDownload SQL="+SQL);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs=stmt.executeQuery(SQL);
            int counter=0;
            while ( rs.next() ) {
                String fname = rs.getString("fname");
                filepathList.add(fname);
                counter++;
            }
            LOG.info("postgresController.java counter="+counter);

            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.err.println( "GDS_getFilePathsToDownload() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
        LOG.info("postgresController.java FINISH GDS_getFilePathsToDownload");
        return filepathList;
    }

    private ArrayList<String> GDS_getDiseaseFromNCIt(int gdsid, String type)
    {
        ArrayList<String> diseaseList=new ArrayList<String>();
        String SQL = "SELECT umlsid FROM public.gds_"+type+"_mapto_umls WHERE gdsid = "+gdsid+";";
        //LOG.info("postgresController.java GDS_getDiseaseFromNCIt SQL="+SQL);
        ArrayList<String> umlsidList = new ArrayList<String>();
        ArrayList<String> ncitidList = new ArrayList<String>();
        ArrayList<String> synonymList = new ArrayList<String>();
        String preSQL1 = "SELECT ncitid FROM public.umls_mapto_ncit WHERE umlsid IN (";
        String postSQL1 = "";
        String SQL1 = "";

        String preSQL2 = "SELECT synonymname FROM public.nciterm_synonym WHERE ncitid IN (";
        String postSQL2 = "";
        String SQL2 = "";

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs=stmt.executeQuery(SQL);
            while ( rs.next() ) {
                String id = rs.getString("umlsid");
                if(umlsidList.contains(id)==false)
                    umlsidList.add(id);
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.err.println( "GDS_getDiseaseFromNCIt() error 1 "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }

        if(umlsidList.size()>0)
        {
            for(int i=0; i<umlsidList.size(); i++)
            {
                postSQL1=postSQL1+"\'"+umlsidList.get(i)+"\'";
                if(i<umlsidList.size()-1)
                    postSQL1=postSQL1+",";
            }
            SQL1=preSQL1+postSQL1+");";
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs=stmt.executeQuery(SQL1);
                while ( rs.next() ) {
                    String id = rs.getString("ncitid");
                    if(ncitidList.contains(id)==false)
                        ncitidList.add(id);
                }
                rs.close();
                stmt.close();
            } catch (SQLException ex) {
                System.err.println( "GDS_getDiseaseFromNCIt() error 2 "+ ex.getClass().getName()+": "+ ex.getMessage() );
            }

            if(ncitidList.size()>0)
            {
                for(int i=0; i<ncitidList.size(); i++)
                {
                    postSQL2=postSQL2+"\'"+ncitidList.get(i)+"\'";
                    if(i<ncitidList.size()-1)
                        postSQL2=postSQL2+",";
                }
                SQL2=preSQL2+postSQL2+");";
                try {
                    Statement stmt = conn.createStatement();
                    ResultSet rs=stmt.executeQuery(SQL2);
                    while ( rs.next() ) {
                        String synonym = rs.getString("synonymname");
                        if(synonymList.contains(synonym)==false)
                            synonymList.add(synonym);
                    }
                    rs.close();
                    stmt.close();
                } catch (SQLException ex) {
                    System.err.println( "GDS_getDiseaseFromNCIt() error 3 "+ ex.getClass().getName()+": "+ ex.getMessage() );
                }
                if(synonymList.size()>0)
                diseaseList=synonymList;
            }
        }
        LOG.info("postgresController.java GDS_getDiseaseFromNCIt diseaseList="+diseaseList.toString());
        return diseaseList;
    }

    private ArrayList<String> GDS_getDiseaseFromUMLSAndGDS(int gdsid, String type)
    {
        ArrayList<String> diseaseList=new ArrayList<String>();
        ArrayList<String> umlsnameList = new ArrayList<String>();

        String SQL1 = "SELECT umlsname FROM public.gds_"+type+"_mapto_umls WHERE gdsid = "+gdsid+";";
        //LOG.info("postgresController.java GDS_getDiseaseFromUMLSAndGDS SQL1="+SQL1);
        
        String SQL2 = "SELECT disease FROM public.gds_"+type+"_mapto_disease WHERE gdsid = "+gdsid+";";
        //LOG.info("postgresController.java GDS_getDiseaseFromUMLSAndGDS SQL2="+SQL2);
        
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs=stmt.executeQuery(SQL1);
            while ( rs.next() ) {
                String name = rs.getString("umlsname");
                if(umlsnameList.contains(name)==false)
                    umlsnameList.add(name);
            }
            rs=stmt.executeQuery(SQL2);
            while ( rs.next() ) {
                String disease = rs.getString("disease");
                if(umlsnameList.contains(disease)==false)
                    umlsnameList.add(disease);
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.err.println( "GDS_getDiseaseFromUMLSAndGDS() error 1 "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
        if(umlsnameList.size()>0)
            diseaseList=umlsnameList;
        return diseaseList;
    }

    private ArrayList<String> GDS_getDiseaseFromCellosaurus(int gdsid, String type)
    {
        ArrayList<String> diseaseList=new ArrayList<String>();
        ArrayList<String> nciterm_ncitid = new ArrayList<String>();
        String SQL1 = "SELECT gdsid FROM public.gds_"+type+"_mapto_cellosaurus WHERE gdsid = "+gdsid+";";
        String SQL2_select = "SELECT synonymname FROM public.nciterm_synonym WHERE ncitid IN (";
        String SQL2_content = "";
        String SQL2 = "";
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs=stmt.executeQuery(SQL1);
            while ( rs.next() ) {
                String ncitid = rs.getString("gdsid");
                if(nciterm_ncitid.contains(ncitid)==false)
                    nciterm_ncitid.add(ncitid);
            }

            if(nciterm_ncitid.size()>0)
            {
                for(int i=0; i<nciterm_ncitid.size(); i++)
                {
                    String ncitid = nciterm_ncitid.get(i);
                    SQL2_content = SQL2_content + "\'" + ncitid + "\'";
                    if(i<nciterm_ncitid.size()-1)
                        SQL2_content = SQL2_content + ",";
                }
                SQL2 = SQL2_select + SQL2_content + ");";
                rs=stmt.executeQuery(SQL2);
                while ( rs.next() ) {
                    String synonym = rs.getString("synonymname");
                    if(diseaseList.contains(synonym)==false)
                        diseaseList.add(synonym);
                }
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.err.println( "GDS_getDiseaseFromCellosaurus() error 1 "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
        return diseaseList;
    }

    /*private ArrayList<String> GDS_getAnatomyFromCellosaurus(int gdsid, String type)
    {
        ArrayList<String> anatomyList = new ArrayList<String>();
        ArrayList<String> cellosaurus_accession_list = new ArrayList<String>();
        ArrayList<String> ncitid_list = new ArrayList<String>();
        String SQL1 = "SELECT ac FROM public.gds_"+type+"_mapto_cellosaurus WHERE gdsid = "+gdsid+";";
        String SQL2_select = "SELECT anatomy_ncitid FROM public.cellosaurus_anatomy WHERE ac IN (";
        String SQL2_content = "";
        String SQL2 = "";
        String SQL3_select = "SELECT synonymname FROM public.nciterm_synonym WHERE ncitid IN (";
        String SQL3_content = "";
        String SQL3 = "";
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs=stmt.executeQuery(SQL1);
            while ( rs.next() ) {
                String ac = rs.getString("ac");
                if(cellosaurus_accession_list.contains(ac)==false)
                    cellosaurus_accession_list.add(ac);
            }

            if(cellosaurus_accession_list.size()>0)
            {
                for(int i=0; i<cellosaurus_accession_list.size(); i++)
                {
                    String ac = cellosaurus_accession_list.get(i);
                    SQL2_content = SQL2_content + "\'" + ac + "\'";
                    if(i<cellosaurus_accession_list.size()-1)
                        SQL2_content = SQL2_content + ",";
                }
                SQL2 = SQL2_select + SQL2_content + ");";
                rs=stmt.executeQuery(SQL2);
                while ( rs.next() ) {
                    String ncitid = rs.getString("anatomy_ncitid");
                    if(ncitid_list.contains(ncitid)==false)
                        ncitid_list.add(ncitid);
                }

                if(ncitid_list.size()>0)
                {
                    for(int i=0; i<ncitid_list.size(); i++)
                    {
                        String ncitid = ncitid_list.get(i);
                        SQL3_content = SQL3_content + "\'" + ncitid + "\'";
                        if(i<ncitid_list.size()-1)
                            SQL3_content = SQL3_content + ",";
                    }
                    SQL3 = SQL3_select + SQL3_content + ");";
                    rs=stmt.executeQuery(SQL3);
                    while ( rs.next() ) {
                        String anatomy_synonym = rs.getString("synonymname");
                        if(anatomyList.contains(anatomy_synonym)==false)
                        anatomyList.add(anatomy_synonym);
                    }
                }
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.err.println( "GDS_getAnatomyFromCellosaurus() error 1 "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
        return anatomyList;
    }*/

    private ArrayList<String> GDS_getAnatomyFromCellosaurus(int gdsid, String type)
    {
        ArrayList<String> anatomyList = new ArrayList<String>();
        ArrayList<String> cellosaurus_accession_list = new ArrayList<String>();
        ArrayList<String> synonym_list = new ArrayList<String>();
        ArrayList<String> ncitid_list = new ArrayList<String>();
        String SQL1 = "SELECT ac FROM public.gds_"+type+"_mapto_cellosaurus WHERE gdsid = "+gdsid+";";
        String SQL2_select = "SELECT anatomy_synonym FROM public.cellosaurus_anatomy WHERE ac IN (";
        String SQL2_content = "";
        String SQL2 = "";
        String SQL3_select = "SELECT ncitid FROM public.anatomy_synonym WHERE synonymname IN (";
        String SQL3_content = "";
        String SQL3 = "";
        String SQL4_select = "SELECT synonymname FROM public.anatomy_synonym WHERE ncitid IN (";
        String SQL4_content = "";
        String SQL4 = "";
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs=stmt.executeQuery(SQL1);
            while ( rs.next() ) {
                String ac = rs.getString("ac");
                if(cellosaurus_accession_list.contains(ac)==false)
                    cellosaurus_accession_list.add(ac);
            }

            if(cellosaurus_accession_list.size()>0)
            {
                for(int i=0; i<cellosaurus_accession_list.size(); i++)
                {
                    String ac = cellosaurus_accession_list.get(i);
                    SQL2_content = SQL2_content + "\'" + ac + "\'";
                    if(i<cellosaurus_accession_list.size()-1)
                        SQL2_content = SQL2_content + ",";
                }
                SQL2 = SQL2_select + SQL2_content + ");";
                rs=stmt.executeQuery(SQL2);
                while ( rs.next() ) {
                    String synonym = rs.getString("anatomy_synonym");
                    if(synonym_list.contains(synonym)==false)
                        synonym_list.add(synonym);
                }

                if(synonym_list.size()>0)
                {
                    for(int i=0; i<synonym_list.size(); i++)
                    {
                        String synonym = synonym_list.get(i);
                        SQL3_content = SQL3_content + "\'" + synonym + "\'";
                        if(i<synonym_list.size()-1)
                            SQL3_content = SQL3_content + ",";
                    }
                    SQL3 = SQL3_select + SQL3_content + ");";
                    rs=stmt.executeQuery(SQL3);
                    while ( rs.next() ) {
                        String ncitid = rs.getString("ncitid");
                        if(ncitid_list.contains(ncitid)==false)
                            ncitid_list.add(ncitid);
                    }

                    if(ncitid_list.size()>0)
                    {
                        for(int i=0; i<ncitid_list.size(); i++)
                        {
                            String ncitid = ncitid_list.get(i);
                            SQL4_content = SQL4_content + "\'" + ncitid + "\'";
                            if(i<ncitid_list.size()-1)
                                SQL4_content = SQL4_content + ",";
                        }
                        SQL4 = SQL4_select + SQL4_content + ");";
                        rs=stmt.executeQuery(SQL4);
                        while ( rs.next() ) {
                            String synonymname = rs.getString("synonymname");
                            if(anatomyList.contains(synonymname)==false)
                                anatomyList.add(synonymname);
                        }
                    }
                }
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.err.println( "GDS_getAnatomyFromCellosaurus() error 1 "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
        return anatomyList;
    }

    private ArrayList<ArrayList<String>> GDS_getAssociatedDiseaseList(int gdsid, String type)
    {
        //refer to README_processCrawledGDSData.doc for logic
        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
        ArrayList<String> diseaseFromNCItList=GDS_getDiseaseFromNCIt(gdsid, type);
        ArrayList<String> diseasenameFromUMLSAndGDSList = GDS_getDiseaseFromUMLSAndGDS(gdsid, type);
        ArrayList<String> diseaseFromCelllineList = GDS_getDiseaseFromCellosaurus(gdsid, type);
        ArrayList<String> anatomyFromCelllineList = GDS_getAnatomyFromCellosaurus(gdsid, type);

        if(diseaseFromCelllineList.size()>0)
            LOG.info("&&&&&["+gdsid+"] postgresController.java GDS_getAssociatedDiseaseList diseaseFromCelllineList="+diseaseFromCelllineList.toString());
        
        result.add(diseaseFromNCItList);
        result.add(diseasenameFromUMLSAndGDSList);
        result.add(diseaseFromCelllineList);
        result.add(anatomyFromCelllineList);
        return result;
    }

    public ArrayList<ArrayList<String>> GDS_getAssociatedDiseaseList_Title(int gdsid)
    {
        String type = "title";
        return GDS_getAssociatedDiseaseList(gdsid, type);
    }

    public ArrayList<ArrayList<String>> GDS_getAssociatedDiseaseList_Description(int gdsid)
    {
        String type = "description";
        return GDS_getAssociatedDiseaseList(gdsid, type);
    }

    public ArrayList<ArrayList<String>> GDS_getAssociatedDiseaseList_TitleDescription(int gdsid)
    {
        String type = "titledescription";
        return GDS_getAssociatedDiseaseList(gdsid, type);
    }

    public ArrayList<String> GDS_getAssociatedAnatomy(String diseasename)
    {
        ArrayList<String> anatomy_list = new ArrayList<String>();
        ArrayList<String> ncimid_list = new ArrayList<String>();
        ArrayList<String> ncitid_list = new ArrayList<String>();
        if(diseasename.contains("'"))
            diseasename = diseasename.replace("'","&apos;");
        String SQL1 = "SELECT ncimid FROM public.nciterm_synonym WHERE synonymname = \'"+diseasename.toUpperCase()+"\';";
        String SQL2_select = "SELECT anatomy_ncimid FROM public.nciterm_anatomy WHERE nciterm_ncimid IN (";
        String SQL2_content = "";
        String SQL2 = "";
        String SQL3_select = "SELECT ncitid FROM public.anatomy WHERE ncimid IN (";
        String SQL3_content = "";
        String SQL3 = "";

        String SQL4_select = "SELECT synonymname FROM public.anatomy_synonym WHERE ncitid IN (";
        String SQL4_content = "";
        String SQL4 = "";
        try {
            Statement stmt = conn.createStatement();
            LOG.info("postgresController.java GDS_getAssociatedAnatomy() SQL1="+SQL1);
            ResultSet rs=stmt.executeQuery(SQL1);
            while ( rs.next() ) {
                String ncimid = rs.getString("ncimid");
                if(ncimid_list.contains(ncimid)==false)
                    ncimid_list.add(ncimid);
            }

            if(ncimid_list.size()>0)
            {
                for(int i=0; i<ncimid_list.size(); i++)
                {
                    String ncimid = ncimid_list.get(i);
                    SQL2_content = SQL2_content + "\'" + ncimid + "\'";
                    if(i<ncimid_list.size()-1)
                        SQL2_content = SQL2_content + ",";
                }
                SQL2 = SQL2_select + SQL2_content + ");";
                LOG.info("postgresController.java GDS_getAssociatedAnatomy() SQL2="+SQL2);
                rs=stmt.executeQuery(SQL2);
                ncimid_list = new ArrayList<String>();
                while ( rs.next() ) {
                    String anatomy_ncimid = rs.getString("anatomy_ncimid");
                    if(ncimid_list.contains(anatomy_ncimid)==false)
                        ncimid_list.add(anatomy_ncimid);
                }

                if(ncimid_list.size()>0)
                {
                    for(int i=0; i<ncimid_list.size(); i++)
                    {
                        String ncimid = ncimid_list.get(i);
                        SQL3_content = SQL3_content + "\'" + ncimid + "\'";
                        if(i<ncimid_list.size()-1)
                            SQL3_content = SQL3_content + ",";
                    }
                    SQL3 = SQL3_select + SQL3_content + ");";
                    LOG.info("postgresController.java GDS_getAssociatedAnatomy() SQL3="+SQL3);
                    rs=stmt.executeQuery(SQL3);
                    while ( rs.next() ) {
                        String ncitid = rs.getString("ncitid");
                        if(ncitid_list.contains(ncitid)==false)
                            ncitid_list.add(ncitid);
                    }
                    if(ncitid_list.size()>0)
                    {
                        for(int i=0; i<ncitid_list.size(); i++)
                        {
                            String ncitid = ncitid_list.get(i);
                            SQL4_content = SQL4_content + "\'" + ncitid + "\'";
                            if(i<ncitid_list.size()-1)
                                SQL4_content = SQL4_content + ",";
                        }
                        SQL4 = SQL4_select + SQL4_content + ");";
                        LOG.info("postgresController.java GDS_getAssociatedAnatomy() SQL4="+SQL4);
                        rs=stmt.executeQuery(SQL4);
                        while ( rs.next() ) {
                            String name = rs.getString("name");
                            if(anatomy_list.contains(name)==false)
                                anatomy_list.add(name);
                        }
                    }
                }
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.err.println( "GDS_getAssociatedAnatomy() error 1 "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
        return anatomy_list;
    }
    /*+++++++++++++++++++++++++++++++ END GDS-related functions (GDS_xxxx) +++++++++++++++++++++++++++++*/


    /***************************START Thesaurus functions (TH_xxxx) *******************************/
    public ArrayList<String> TH_getDiseaseDescendents(String diseasename)
    {
        ArrayList<String> descendent_list = new ArrayList<String>();
        ArrayList<String> disease_ncitid_list = new ArrayList<String>();
        ArrayList<String> disease_ncimid_list = new ArrayList<String>();
        ArrayList<String> descendent_ncimid_list = new ArrayList<String>();
        ArrayList<String> descendent_ncitid_list = new ArrayList<String>();
        String disease_SQL = "SELECT ncitid FROM public.nciterm_synonym WHERE synonymname =\'"+diseasename.toUpperCase()+"\';";
        String disease_ncim_SQL_prefix = "SELECT ncimid FROM public.nciterm WHERE ncitid IN (";
        String disease_ncim_SQL_content = "";
        String disease_ncim_SQL = "";
        String child_lvl1_SQL_prefix = "SELECT child_ncimid FROM public.nciterm_child WHERE nciterm_ncimid IN (";
        String child_lvl1_SQL_content = "";
        String child_lvl1_SQL = "";
        String child_lvl2_SQL_prefix = "SELECT child_ncimid FROM public.nciterm_child WHERE nciterm_ncimid IN (";
        String child_lvl2_SQL_content = "";
        String child_lvl2_SQL = "";
        String descendent_ncit_SQL_prefix = "SELECT ncitid FROM public.nciterm WHERE ncimid IN (";
        String descendent_ncit_SQL_content = "";
        String descendent_ncit_SQL = "";
        String descendent_synonym_SQL_prefix = "SELECT synonymname FROM public.nciterm_synonym WHERE ncitid IN (";
        String descendent_synonym_SQL_content = "";
        String descendent_synonym_SQL = "";

        LOG.info("postgresNCIThesaurusController.java TH_getDiseaseDescendents disease_SQL="+disease_SQL);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs=stmt.executeQuery(disease_SQL);
            while ( rs.next() ) {
                String ncitid = rs.getString("ncitid");
                disease_ncitid_list.add(ncitid);
            }

            if(disease_ncitid_list.size()>0)
            {
                for(int i=0; i<disease_ncitid_list.size(); i++)
                {
                    disease_ncim_SQL_content = disease_ncim_SQL_content + "'" + disease_ncitid_list.get(i) + "'";
                    if(i<disease_ncitid_list.size()-1)
                        disease_ncim_SQL_content = disease_ncim_SQL_content + ",";
                }
                disease_ncim_SQL = disease_ncim_SQL_prefix + disease_ncim_SQL_content + ");";
                LOG.info("postgresNCIThesaurusController.java TH_getDiseaseDescendents disease_ncim_SQL="+disease_ncim_SQL);
                rs=stmt.executeQuery(disease_ncim_SQL);
                while ( rs.next() ) {
                    String ncimid = rs.getString("ncimid");
                    if(!disease_ncimid_list.contains(ncimid))
                        disease_ncimid_list.add(ncimid);
                }
                if(disease_ncimid_list.size()>0)
                {
                    for(int i=0; i<disease_ncimid_list.size(); i++)
                    {
                        child_lvl1_SQL_content = child_lvl1_SQL_content + "'" + disease_ncimid_list.get(i) + "'";
                        if(i<disease_ncimid_list.size()-1)
                            child_lvl1_SQL_content = child_lvl1_SQL_content + ",";
                    }
                    child_lvl1_SQL = child_lvl1_SQL_prefix + child_lvl1_SQL_content + ");";
                    LOG.info("postgresNCIThesaurusController.java TH_getDiseaseDescendents child_lvl1_SQL="+child_lvl1_SQL);
                    rs=stmt.executeQuery(child_lvl1_SQL);
                    while ( rs.next() ) {
                        String child_ncimid = rs.getString("child_ncimid");
                        if(!descendent_ncimid_list.contains(child_ncimid))
                            descendent_ncimid_list.add(child_ncimid);
                    }

                    if(descendent_ncimid_list.size()>0)
                    {
                        for(int i=0; i<descendent_ncimid_list.size(); i++)
                        {
                            child_lvl2_SQL_content = child_lvl2_SQL_content + "'" + descendent_ncimid_list.get(i) + "'";
                            if(i<descendent_ncimid_list.size()-1)
                                child_lvl2_SQL_content = child_lvl2_SQL_content + ",";
                        }
                        child_lvl2_SQL = child_lvl2_SQL_prefix + child_lvl2_SQL_content + ");";
                        LOG.info("postgresNCIThesaurusController.java TH_getDiseaseDescendents child_lvl2_SQL="+child_lvl2_SQL);
                        rs=stmt.executeQuery(child_lvl2_SQL);
                        while ( rs.next() ) {
                            String child_ncimid = rs.getString("child_ncimid");
                            if(!descendent_ncimid_list.contains(child_ncimid))
                                descendent_ncimid_list.add(child_ncimid);
                        }
                        if(descendent_ncimid_list.size()>0)
                        {
                            for(int i=0; i<descendent_ncimid_list.size(); i++)
                            {
                                descendent_ncit_SQL_content = descendent_ncit_SQL_content + "'" + descendent_ncimid_list.get(i) + "'";
                                if(i<descendent_ncimid_list.size()-1)
                                    descendent_ncit_SQL_content = descendent_ncit_SQL_content + ",";
                            }
                            descendent_ncit_SQL = descendent_ncit_SQL_prefix + descendent_ncit_SQL_content + ");";
                            LOG.info("postgresNCIThesaurusController.java TH_getDiseaseDescendents descendent_ncit_SQL="+descendent_ncit_SQL);
                            rs=stmt.executeQuery(descendent_ncit_SQL);
                            while ( rs.next() ) {
                                String ncitid = rs.getString("ncitid");
                                if(!descendent_ncitid_list.contains(ncitid))
                                    descendent_ncitid_list.add(ncitid);
                            }
                            if(descendent_ncitid_list.size()>0)
                            {
                                for(int i=0; i<descendent_ncitid_list.size(); i++)
                                {
                                    descendent_synonym_SQL_content = descendent_synonym_SQL_content + "'" + descendent_ncitid_list.get(i) + "'";
                                    if(i<descendent_ncitid_list.size()-1)
                                        descendent_synonym_SQL_content = descendent_synonym_SQL_content + ",";
                                }
                                descendent_synonym_SQL = descendent_synonym_SQL_prefix + descendent_synonym_SQL_content + ");";
                                LOG.info("postgresNCIThesaurusController.java TH_getDiseaseDescendents descendent_synonym_SQL="+descendent_synonym_SQL);
                                rs=stmt.executeQuery(descendent_synonym_SQL);
                                while ( rs.next() ) {
                                    String synonymname = rs.getString("synonymname");
                                    if(!descendent_list.contains(synonymname))
                                        descendent_list.add(synonymname);
                                }
                            }
                        }
                    }
                }
            }

            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.err.println( "TH_getDiseaseDescendents() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
        LOG.info("postgresNCIThesaurusController.java FINISH TH_getDiseaseDescendents");
        return descendent_list;
    }

    private ArrayList<String> TH_getPreferredNameFromNCIThesaurus(ArrayList<String> ncitidList)
    {
        ArrayList<String> preferredNameList=new ArrayList<String>();
        String preSQL = "SELECT ncitid, name FROM public.nciterm WHERE ncitid IN (";
        String postSQL = "";
        String SQL = "";
        for(int i=0; i<ncitidList.size(); i++)
        {
            String r = ncitidList.get(i);
            postSQL=postSQL+"\'"+r+"\'";
            if(i<ncitidList.size()-1)
                postSQL=postSQL+",";
        }
        SQL=preSQL+postSQL+");";
        LOG.info("postgresController.java TH_getPreferredNameFromNCIThesaurus SQL="+SQL);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs=stmt.executeQuery(SQL);

            while ( rs.next() ) {
                String keyword = rs.getString("name");
                if(keyword.contains("&apos;"))
                    keyword = keyword.replace("&apos;","'");
                preferredNameList.add(keyword);
            }

            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.err.println( "TH_getPreferredNameFromNCIThesaurus() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
        return preferredNameList;
    }

    private void TH_saveNCIMIDList(ArrayList<String> idList)
    {
        String preSQL = "SELECT ncimid FROM public.nciterm WHERE ncitid IN (";
        String postSQL = "";
        String SQL = "";
        TH_ncitidList = new ArrayList<String>();
        TH_ncimidList = new ArrayList<String>();
        for(int i=0; i<idList.size(); i++)
        {
            String r = idList.get(i);
            TH_ncitidList.add(r);
            postSQL=postSQL+"\'"+r+"\'";
            if(i<idList.size()-1)
                postSQL=postSQL+",";
        }
        SQL=preSQL+postSQL+");";
        LOG.info("postgresNCIThesaurusController.java saveNCIMIDList SQL="+SQL);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs=stmt.executeQuery(SQL);

            while ( rs.next() ) {
                String id = rs.getString("ncimid");
                TH_ncimidList.add(id);
            }

            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.err.println( "saveNCIMIDList() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
    }
    
    public ArrayList<String> TH_getNCIMIDList()
    {
        return TH_ncimidList;
    }

    public ArrayList<String> TH_getNCITIDList()
    {
        return TH_ncitidList;
    }

    public ArrayList<String> TH_checkForDisease(ArrayList<String> keywordList)
    {
        ArrayList<String> ncitidlList=new ArrayList<String>();
        String preSQL = "SELECT synonymname, ncitid FROM public.nciterm_synonym WHERE synonymname IN (";
        String postSQL = "";
        String SQL = "";
        for(int i=0; i<keywordList.size(); i++)
        {
            String r = keywordList.get(i);
            postSQL=postSQL+"\'"+r.toUpperCase()+"\'";
            if(i<keywordList.size()-1)
                postSQL=postSQL+",";
        }
        SQL=preSQL+postSQL+");";
        LOG.info("postgresNCIThesaurusController.java checkForDisease SQL="+SQL);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs=stmt.executeQuery(SQL);

            while ( rs.next() ) {
                String keyword = rs.getString("ncitid");
                ncitidlList.add(keyword);
            }

            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.err.println( "TH_checkForDisease() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
        LOG.info("TH_checkForDisease ncitidlList="+ncitidlList.toString());
        LOG.info("postgresNCIThesaurusController.java FINISH TH_checkForDisease");
        TH_saveNCIMIDList(ncitidlList);
        return TH_getPreferredNameFromNCIThesaurus(ncitidlList);
    }

    private ArrayList<String> TH_getPreferredOrganismNameFromNCIThesaurus(ArrayList<String> ncitidList)
    {
        ArrayList<String> preferredNameList=new ArrayList<String>();
        if (ncitidList.size()==0)
            return preferredNameList;

        String preSQL = "SELECT ncitid, name FROM public.taxonomy WHERE ncitid IN (";
        String postSQL = "";
        String SQL = "";
        for(int i=0; i<ncitidList.size(); i++)
        {
            String r = ncitidList.get(i);
            postSQL=postSQL+"\'"+r+"\'";
            if(i<ncitidList.size()-1)
                postSQL=postSQL+",";
        }
        SQL=preSQL+postSQL+");";
        LOG.info("postgresNCIThesaurusController.java TH_getPreferredOrganismNameFromNCIThesaurus SQL="+SQL);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs=stmt.executeQuery(SQL);

            while ( rs.next() ) {
                String keyword = rs.getString("name");
                preferredNameList.add(keyword);
            }

            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.err.println( "TH_getPreferredOrganismNameFromNCIThesaurus() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
        return preferredNameList;
    }

    public ArrayList<String> TH_checkForOrganism(ArrayList<String> keywordList)
    {
        ArrayList<String> ncitidlList=new ArrayList<String>();
        TH_organism_syn_list = new ArrayList<String>();
        String preSQL = "SELECT synonymname, ncitid FROM public.taxonomy_synonym WHERE synonymname IN (";
        String postSQL = "";
        String SQL = "";
        for(int i=0; i<keywordList.size(); i++)
        {
            String r = keywordList.get(i);
            postSQL=postSQL+"\'"+r.toUpperCase()+"\'";
            if(i<keywordList.size()-1)
                postSQL=postSQL+",";
        }
        SQL=preSQL+postSQL+");";
        LOG.info("postgresNCIThesaurusController.java TH_checkForOrganism SQL="+SQL);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs=stmt.executeQuery(SQL);

            while ( rs.next() ) {
                String keyword = rs.getString("ncitid");
                String organism_syn = rs.getString("synonymname");
                ncitidlList.add(keyword);
                TH_organism_syn_list.add(organism_syn);
            }

            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.err.println( "TH_checkForOrganism() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }

        LOG.info("postgresNCIThesaurusController.java FINISH TH_checkForOrganism");
        return TH_getPreferredOrganismNameFromNCIThesaurus(ncitidlList);
    }

    public ArrayList<String> TH_getKeywordsToCheck(ArrayList<String> keywordList)
    {
        ArrayList<String> keywordLeftToCheck=new ArrayList<String>();

        for(int i=0; i<keywordList.size(); i++)
        {
            String keyword=keywordList.get(i);
            String uppercase_keyword=keyword.toUpperCase();
            if(TH_organism_syn_list.contains(uppercase_keyword)==false)
                keywordLeftToCheck.add(keyword);
        }

        return keywordLeftToCheck;
    }

    
    private ArrayList<String> TH_retrieveAnatomyPreferredName(ArrayList<String> anatomy_ncitidlList)
    {
        ArrayList<String> anatomy_list=new ArrayList<String>();
        String preSQL = "SELECT name FROM public.anatomy WHERE ncitid IN (";
        String postSQL = "";
        String SQL = "";
        for(int i=0; i<anatomy_ncitidlList.size(); i++)
        {
            String r = anatomy_ncitidlList.get(i);
            if(r.compareTo("NONE")!=0)
            {
                postSQL=postSQL+"\'"+r+"\'";
                if(i<anatomy_ncitidlList.size()-1)
                    postSQL=postSQL+",";
            }
        }
        if(postSQL.length()>0)
            SQL=preSQL+postSQL+");";
        else
            return anatomy_list;
        //proceed to here if there are valid associated anatomy in the DB    
        LOG.info("postgresNCIThesaurusController.java TH_retrieveAnatomyPreferredName SQL="+SQL);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs=stmt.executeQuery(SQL);

            while ( rs.next() ) {
                String id = rs.getString("name");
                if(!anatomy_list.contains(id))
                    anatomy_list.add(id);
            }

            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.err.println( "TH_retrieveAnatomyPreferredName() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
        return anatomy_list;
    }
    
    public ArrayList<String> TH_getAssociatedAnatomyExistingInDB(ArrayList<String> ncitidList)
    {
        ArrayList<String> existingAnatomyNCIMList = new ArrayList<String>();
        ArrayList<String> existingAnatomyNCITList = new ArrayList<String>();
        ArrayList<String> ncimid_list = new ArrayList<String>();
        String preSQL_ncim = "SELECT ncimid FROM public.nciterm WHERE ncitid IN (";
        String postSQL_ncim = "";
        String SQL_ncim = "";
        String preSQL = "SELECT nciterm_ncimid, anatomy_ncimid FROM public.nciterm_anatomy WHERE nciterm_ncimid IN (";
        String postSQL = "";
        String SQL = "";
        String preSQL_ncitid = "SELECT ncitid FROM public.nciterm WHERE ncimid IN (";
        String postSQL_ncitid = "";
        String SQL_ncitid = "";
        String preSQL_anatomy_ncitid = "SELECT ncitid FROM public.anatomy WHERE ncimid IN (";
        String postSQL_anatomy_ncitid = "";
        String SQL_anatomy_ncitid = "";
        for(int i=0; i<ncitidList.size(); i++)
        {
            String r = ncitidList.get(i);
            postSQL_ncim=postSQL_ncim+"\'"+r+"\'";
            if(i<ncitidList.size()-1)
                postSQL_ncim=postSQL_ncim+",";
        }
        SQL_ncim=preSQL_ncim+postSQL_ncim+");";
        LOG.info("postgresNCIThesaurusController.java TH_getAssociatedAnatomyExistingInDB SQL_ncim="+SQL_ncim);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs=stmt.executeQuery(SQL_ncim);

            while ( rs.next() ) {
                String id = rs.getString("ncimid");
                if(!ncimid_list.contains(id))
                    ncimid_list.add(id);
            }
            if(ncimid_list.size()>0)
            {
                for(int i=0; i<ncimid_list.size(); i++)
                {
                    String ncimid = ncimid_list.get(i);
                    postSQL=postSQL+"\'"+ncimid+"\'";
                    if(i<ncimid_list.size()-1)
                        postSQL=postSQL+",";
                }
                SQL=preSQL+postSQL+");";
                LOG.info("postgresNCIThesaurusController.java TH_getAssociatedAnatomyExistingInDB SQL="+SQL);
                rs=stmt.executeQuery(SQL);
                ncimid_list = new ArrayList<String>();
                while ( rs.next() ) {
                    String ncimid = rs.getString("nciterm_ncimid");
                    String anatomy_ncimid = rs.getString("anatomy_ncimid");
                    if(!ncimid_list.contains(ncimid))
                        ncimid_list.add(ncimid);
                    if(!existingAnatomyNCIMList.contains(anatomy_ncimid))
                        existingAnatomyNCIMList.add(anatomy_ncimid);
                }
                LOG.info("postgresNCIThesaurusController.java TH_getAssociatedAnatomyExistingInDB ncimid_list="+ncimid_list.toString());
                LOG.info("postgresNCIThesaurusController.java TH_getAssociatedAnatomyExistingInDB existingAnatomyNCIMList="+existingAnatomyNCIMList.toString());
                
                if(ncimid_list.size()>0)
                {
                    for(int i=0; i<ncimid_list.size(); i++)
                    {
                        String ncimid = ncimid_list.get(i);
                        postSQL_ncitid=postSQL_ncitid+"\'"+ncimid+"\'";
                        if(i<ncimid_list.size()-1)
                            postSQL_ncitid=postSQL_ncitid+",";
                    }
                    SQL_ncitid=preSQL_ncitid+postSQL_ncitid+");";
                    LOG.info("postgresNCIThesaurusController.java TH_getAssociatedAnatomyExistingInDB SQL_ncitid="+SQL_ncitid);
                    rs=stmt.executeQuery(SQL_ncitid);
                    while ( rs.next() ) {
                        String ncitid = rs.getString("ncitid");
                        if(!TH_found_ncitidList.contains(ncitid))
                            TH_found_ncitidList.add(ncitid);
                    }
                } 
                if(existingAnatomyNCIMList.size()>0)
                {
                    for(int i=0; i<existingAnatomyNCIMList.size(); i++)
                    {
                        String anatomy_ncimid = existingAnatomyNCIMList.get(i);
                        postSQL_anatomy_ncitid=postSQL_anatomy_ncitid+"\'"+anatomy_ncimid+"\'";
                        if(i<existingAnatomyNCIMList.size()-1)
                            postSQL_anatomy_ncitid=postSQL_anatomy_ncitid+",";
                    }
                    SQL_anatomy_ncitid=preSQL_anatomy_ncitid+postSQL_anatomy_ncitid+");";
                    LOG.info("postgresNCIThesaurusController.java TH_getAssociatedAnatomyExistingInDB SQL_anatomy_ncitid="+SQL_anatomy_ncitid);
                    rs=stmt.executeQuery(SQL_anatomy_ncitid);
                    while ( rs.next() ) {
                        String anatomy_ncitid = rs.getString("ncitid");
                        if(!existingAnatomyNCITList.contains(anatomy_ncitid))
                            existingAnatomyNCITList.add(anatomy_ncitid);
                    }
                }    
            }

            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.err.println( "TH_getAssociatedAnatomyExistingInDB() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
        LOG.info("postgresNCIThesaurusController.java TH_getAssociatedAnatomyExistingInDB found_ncitidList="+TH_found_ncitidList.toString());
        LOG.info("postgresNCIThesaurusController.java FINISH TH_getAssociatedAnatomyExistingInDB");
        return TH_retrieveAnatomyPreferredName(existingAnatomyNCITList);
    }

    /*public ArrayList<String> TH_getNCITIDWithMissingAnatomy(ArrayList<String> original_ncitidList)
    {
        ArrayList<String> copy_ncitidList=new ArrayList<String>();

        for(int i=0; i<original_ncitidList.size(); i++)
            copy_ncitidList.add(original_ncitidList.get(i));
        copy_ncitidList.removeAll(TH_found_ncitidList);
        return copy_ncitidList;
    }*/

    /*public ArrayList<String> TH_getNCIMIDWithMissingAnatomy(ArrayList<String> remaining_ncitidList)
    {
        ArrayList<String> ncimidList=new ArrayList<String>();

        String preSQL = "SELECT ncimid FROM public.nciterm WHERE ncitid IN (";
        String postSQL = "";
        String SQL = "";
        for(int i=0; i<remaining_ncitidList.size(); i++)
        {
            String r = remaining_ncitidList.get(i);
            postSQL=postSQL+"\'"+r+"\'";
            if(i<remaining_ncitidList.size()-1)
                postSQL=postSQL+",";
        }
        SQL=preSQL+postSQL+");";
        LOG.info("postgresNCIThesaurusController.java TH_getNCIMIDWithMissingAnatomy SQL="+SQL);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs=stmt.executeQuery(SQL);

            while ( rs.next() ) {
                String id = rs.getString("ncimid");
                ncimidList.add(id);
            }

            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.err.println( "TH_getNCIMIDWithMissingAnatomy() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }

        LOG.info("postgresNCIThesaurusController.java FINISH TH_getNCIMIDWithMissingAnatomy");
        return ncimidList;
    }*/

/*    public void TH_insertAnatomyOfNCITerms(ArrayList<String> ncitidList, ArrayList<String> anatomyNameList)
    {
        ArrayList<String> toStore_nciterm_idList=new ArrayList<String>();
        ArrayList<String> toStore_anatomy_idList=new ArrayList<String>();
        for(int i=0; i<ncitidList.size(); i++)
        {
            String nciterm_id=ncitidList.get(i);
            String anatomy_name=anatomyNameList.get(i);
            String anatomy_ncitid="";
            
            //check if anatomyName inside db
            String sql="SELECT ncitid FROM public.anatomy WHERE name=\'"+anatomy_name+"\';";
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs=stmt.executeQuery(sql);
    
                while ( rs.next() ) {
                    String id = rs.getString("ncitid");
                    anatomy_ncitid=id;
                }
    
                rs.close();
                stmt.close();
            } catch (SQLException ex) {
                System.err.println( "TH_insertAnatomyOfNCITerms() "+ ex.getClass().getName()+": "+ ex.getMessage() );
            }
            if(anatomy_ncitid.length()>0)
            {
                LOG.info(":) FOUND "+anatomy_name+" for ["+nciterm_id+"]");
                toStore_nciterm_idList.add(nciterm_id);
                toStore_anatomy_idList.add(anatomy_ncitid);
            }
            else
                LOG.info("*** "+anatomy_name+" not in anatomy table ["+nciterm_id+"]");
        }
        if(toStore_nciterm_idList.size()>0)
        {
            LOG.info("toStore_nciterm_idList="+toStore_nciterm_idList.toString());
            LOG.info("toStore_anatomy_idList="+toStore_anatomy_idList.toString());
            String preSQL = "INSERT INTO public.nciterm_anatomy(nciterm_ncitid, anatomy_ncitid) VALUES ";
            String postSQL = "";
            String SQL = "";
            for(int i=0; i<toStore_nciterm_idList.size(); i++)
            {
                String r = toStore_nciterm_idList.get(i);
                String s = toStore_anatomy_idList.get(i);
                postSQL=postSQL+"(\'"+r+"\',\'"+s+"\')";
                if(i<toStore_nciterm_idList.size()-1)
                    postSQL=postSQL+",";
            }
            SQL=preSQL+postSQL+" ON CONFLICT DO NOTHING;";
            LOG.info("postgresNCIThesaurusController.java TH_insertAnatomyOfNCITerms SQL="+SQL);
            try {
                Statement stmt = conn.createStatement();
                stmt.execute(SQL);
                stmt.close();
                conn.commit();
            } catch (SQLException ex) {
                System.err.println( "TH_insertAnatomyOfNCITerms() "+ ex.getClass().getName()+": "+ ex.getMessage() );
            }
        }
    }
*/
    private ArrayList<String> TH_getNcitIdList_organismSynonym(String organismName)
    {
        ArrayList<String> ncitidList=new ArrayList<String>();
        String SQL = "SELECT ncitid FROM public.taxonomy_synonym WHERE synonymname=\'"+organismName.toUpperCase()+"\';";
        //LOG.info("postgresNCIThesaurusController.java getNcitIdList_organismSynonym SQL="+SQL);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs=stmt.executeQuery(SQL);

            while ( rs.next() ) {
                String id = rs.getString("ncitid");
                if(ncitidList.contains(id)==false)
                    ncitidList.add(id);
            }

            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.err.println( "TH_getNcitIdList_organismSynonym() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }
        return ncitidList;
    }



    public boolean TH_isEquivalentOrganism(String expectedOrganism, String observedOrganism)
    {
        ArrayList<String> ncitidList_expectedOrganism=TH_getNcitIdList_organismSynonym(expectedOrganism);
        ArrayList<String> ncitidList_observedOrganism=TH_getNcitIdList_organismSynonym(observedOrganism);
        boolean FOUND=false;

        for(int i=0; i<ncitidList_expectedOrganism.size() && !FOUND; i++)
        {
            String ncitid_expectedOrganism=ncitidList_expectedOrganism.get(i);
            if(ncitidList_observedOrganism.contains(ncitid_expectedOrganism)==true)
                return true;
        }
        
        return FOUND;
    }

    public ArrayList<String> TH_getOrganismSynonym(String organism)
    {
        ArrayList<String> ncitidList=TH_getNcitIdList_organismSynonym(organism);
        ArrayList<String> organismSynonymList=new ArrayList<String>();

        String preSQL = "SELECT synonymname FROM public.taxonomy_synonym WHERE ncitid IN (";
        String postSQL = "";
        String SQL = "";
        for(int i=0; i<ncitidList.size(); i++)
        {
            String r = ncitidList.get(i);
            postSQL=postSQL+"\'"+r+"\'";
            if(i<ncitidList.size()-1)
                postSQL=postSQL+",";
        }
        SQL=preSQL+postSQL+");";
        LOG.info("postgresNCIThesaurusController.java TH_getOrganismSynonym SQL="+SQL);
        try {
            //Connection conn = connect();
            //LOG.info("Opened database successfully");
            Statement stmt = conn.createStatement();
            ResultSet rs=stmt.executeQuery(SQL);

            while ( rs.next() ) {
                String name = rs.getString("synonymname");
                if(organismSynonymList.contains(name)==false)
                    organismSynonymList.add(name);
            }

            rs.close();
            stmt.close();
            //conn.close();
        } catch (SQLException ex) {
            System.err.println( "TH_getOrganismSynonym() "+ ex.getClass().getName()+": "+ ex.getMessage() );
        }

        return organismSynonymList;
    }
    /***************************END Thesaurus functions (TH_xxxx) ********************************/
}