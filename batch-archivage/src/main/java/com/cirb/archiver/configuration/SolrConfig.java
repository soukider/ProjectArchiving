package com.cirb.archiver.configuration;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.solr.core.SolrTemplate;

@Configuration
@ComponentScan(basePackages = { "com.cirb.archiver" })
@EnableJpaRepositories(basePackages = { "com.cirb.archiver.repositories" })
public class SolrConfig {

	@Value("${solr.host}")
	private String solrHost;
	
	@Value("${solr.port}")
	private int solrPort;
	
	@Bean
	public SolrClient solrClient() {
		return new HttpSolrClient.Builder("http://" + solrHost + ":" + solrPort + "/solr/").build();
	}
	
	@Bean
    public SolrTemplate solrTemplate() throws Exception {
        return new SolrTemplate(solrClient());
    }

}
