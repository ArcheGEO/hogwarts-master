package de.jonashackt.springbootvuejs.service;

import de.jonashackt.springbootvuejs.domain.gdsrecord;

import java.util.List;

public interface InterfaceGDSRecordService {

    List<gdsrecord> findAll();
    void save(gdsrecord r);
    void deleteAll();
}