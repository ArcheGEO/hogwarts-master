package de.jonashackt.springbootvuejs.repository;

import de.jonashackt.springbootvuejs.domain.gdsrecord;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface gdsrecordRepository extends CrudRepository<gdsrecord, Long> {
	
}