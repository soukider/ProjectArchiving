package com.cirb.archiver.repositories;

import com.cirb.archiver.domain.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {

	List<Provider> findByExternalTimestampLessThanEqualOrderById(Date date);
	
}
