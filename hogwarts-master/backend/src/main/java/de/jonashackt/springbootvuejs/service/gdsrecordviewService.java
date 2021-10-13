package de.jonashackt.springbootvuejs.service;

import de.jonashackt.springbootvuejs.domain.gdsrecordview;
import de.jonashackt.springbootvuejs.repository.gdsrecordviewRepository;
import de.jonashackt.springbootvuejs.controller.JSoupCrawler;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class gdsrecordviewService implements InterfaceGDSRecordViewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(gdsrecordviewService.class);
    JSoupCrawler crawler = new JSoupCrawler();

    @Autowired
    private gdsrecordviewRepository repository;

    public List<gdsrecordview> findAll() {

    	List<gdsrecordview> gds = (List<gdsrecordview>) repository.findAll();

        return gds;
    }

    public void save(gdsrecordview r) {
        repository.save(r);
    }

    public void deleteAll() {
        LOGGER.info("gdsrecordviewService.java - deleteAll ");
        repository.deleteAll();
    }
}