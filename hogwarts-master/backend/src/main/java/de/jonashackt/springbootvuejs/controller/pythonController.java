package de.jonashackt.springbootvuejs.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class pythonController {
    private static final Logger LOG = LoggerFactory.getLogger(pythonController.class);

    public pythonController(){

    }

    public void python_scispacy_extractDiseaseFromTitle(String title){
        try{
            String s=null;
            String command = "python D:\\workspace\\hogwarts-master\\run_scispacy.py \""+title+"\"";
            LOG.info("command="+command);
            Process p=Runtime.getRuntime().exec(command);
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while((s=in.readLine())!=null)
                LOG.info(s);
        }
        catch(IOException ie) {
            ie.printStackTrace();
        }
    }

    public void python_setupPython()
    {
        try{
            String s=null;
            Path path = FileSystems.getDefault().getPath("").toAbsolutePath();
            LOG.info("Current path="+path.toString());
            Process p=Runtime.getRuntime().exec("pip install scispacy");
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while((s=in.readLine())!=null)
                LOG.info(s);
            p=Runtime.getRuntime().exec("pip install https://s3-us-west-2.amazonaws.com/ai2-s2-scispacy/releases/v0.4.0/en_core_sci_lg-0.4.0.tar.gz");
            in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while((s=in.readLine())!=null)
                LOG.info(s);
            p=Runtime.getRuntime().exec("pip install https://s3-us-west-2.amazonaws.com/ai2-s2-scispacy/releases/v0.4.0/en_ner_bc5cdr_md-0.4.0.tar.gz");
            in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while((s=in.readLine())!=null)
                LOG.info(s);
        }
        catch(IOException ie) {
            ie.printStackTrace();
        }
    }

    
}
