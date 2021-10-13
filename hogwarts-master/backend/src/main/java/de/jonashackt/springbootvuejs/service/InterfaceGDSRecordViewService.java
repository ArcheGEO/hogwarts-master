package de.jonashackt.springbootvuejs.service;

import de.jonashackt.springbootvuejs.domain.gdsrecordview;

import java.util.List;

public interface InterfaceGDSRecordViewService {

    List<gdsrecordview> findAll();
    void save(gdsrecordview r);
    void deleteAll();
}