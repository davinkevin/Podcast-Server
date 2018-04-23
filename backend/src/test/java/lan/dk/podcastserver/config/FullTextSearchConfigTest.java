package lan.dk.podcastserver.config;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.junit.Test;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Created by kevin on 11/09/15 for Podcast Server
 */
public class FullTextSearchConfigTest {

    @Test
    public void should_be_a_configuration_class() {
        assertThat(FullTextSearchConfig.class).hasAnnotation(Configuration.class);
    }
    
    @Test
    public void should_generate_a_full_text_entity_manager() {
        /* Given */
        EntityManager em = mock(EntityManager.class);
        FullTextSearchConfig fullTextSearchConfig = new FullTextSearchConfig();

        /* When */
        FullTextEntityManager fullTextEntityManager = fullTextSearchConfig.fullTextEntityManager(em);

        /* Then */
        assertThat(fullTextEntityManager).isInstanceOf(FullTextEntityManager.class);
    }
}
