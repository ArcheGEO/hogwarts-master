package de.jonashackt.springbootvuejs.service;

import de.jonashackt.springbootvuejs.domain.gdsrecord;
import de.jonashackt.springbootvuejs.repository.gdsrecordRepository;
import de.jonashackt.springbootvuejs.controller.JSoupCrawler;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;

@Service
public class gdsrecordService implements InterfaceGDSRecordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(gdsrecordService.class);
    JSoupCrawler crawler = new JSoupCrawler();

    @Autowired
    private gdsrecordRepository repository;

    public List<gdsrecord> findAll() {

    	List<gdsrecord> gds = (List<gdsrecord>) repository.findAll();

        return gds;
    }

    public void save(gdsrecord r) {
        repository.save(r);
    }

    public void deleteAll() {
        repository.deleteAll();
    }
}