package com.cirb.archiver.repositories;

import com.cirb.archiver.domain.Consumer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ConsumerRepository extends JpaRepository<Consumer, Long> {

	List<Consumer> findByExternalTimestampLessThanEqualOrderById(Date date);

}
