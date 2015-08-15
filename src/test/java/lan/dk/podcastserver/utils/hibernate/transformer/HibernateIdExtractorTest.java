package lan.dk.podcastserver.utils.hibernate.transformer;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 15/07/15 for Podcast Server
 */
public class HibernateIdExtractorTest {
    
    @Test
    public void should_extract_id() {
        Object[] objects = {1};
        String[] aliases = {"id"};
        assertThat(new HibernateIdExtractor().transformTuple(objects, aliases))
                .isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_not_revert_extraction() {
        List<String> collection = Arrays.asList("Elem1", "Elem2");
        assertThat(new HibernateIdExtractor().transformList(collection))
                .isNotEmpty()
                .hasSize(collection.size())
                .containsAll(collection);
    }

}