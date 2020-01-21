package com.cirb.archiver.repositories;

import com.cirb.archiver.domain.SolrArchive;
import org.springframework.data.solr.repository.SolrCrudRepository;

public interface SolrArchiveRepository extends SolrCrudRepository<SolrArchive, Long> {

}
