package lan.dk.podcastserver.config;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManager;

import static org.hibernate.search.jpa.Search.getFullTextEntityManager;

/**
 * Created by kevin on 05/09/15 for Podcast Server
 */
@Configuration
public class FullTextSearchConfig {

    @Bean
    @Autowired
    public FullTextEntityManager fullTextEntityManager(EntityManager entityManager) {
        return getFullTextEntityManager(entityManager);
    }
}
