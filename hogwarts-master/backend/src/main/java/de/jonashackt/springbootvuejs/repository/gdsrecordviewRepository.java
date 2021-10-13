package de.jonashackt.springbootvuejs.repository;

import de.jonashackt.springbootvuejs.domain.gdsrecordview;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface gdsrecordviewRepository extends CrudRepository<gdsrecordview, Long> {
	
}