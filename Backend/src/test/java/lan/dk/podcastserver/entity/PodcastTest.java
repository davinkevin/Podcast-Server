package lan.dk.podcastserver.entity;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.UUID;

import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 15/06/15 for HackerRank problem
 */
public class PodcastTest {

    private static final ZonedDateTime NOW = ZonedDateTime.now();
    private static final Cover COVER = Cover.builder().url("ACover").build();
    private static final Podcast PODCAST = new Podcast();
    private static String PODCAST_TO_STRING;
    private UUID id;


    @Before
    public void init() {
        /* Given */
        id = UUID.randomUUID();
        PODCAST.setId(id);
        PODCAST.setTitle("PodcastDeTest");
        PODCAST.setUrl("http://nowhere.com");
        PODCAST.setSignature("ae4b93a7e8249d6be591649c936dbe7d");
        PODCAST.setType("Youtube");
        PODCAST.setLastUpdate(NOW);
        PODCAST.setCover(COVER);
        PODCAST.setDescription("A long Description");
        PODCAST.setHasToBeDeleted(true);
        PODCAST.setItems(Sets.newHashSet());
        PODCAST.setTags(Sets.newHashSet());

        PODCAST_TO_STRING = "Podcast{id="+ id +", title='PodcastDeTest', url='http://nowhere.com', signature='ae4b93a7e8249d6be591649c936dbe7d', type='Youtube', lastUpdate=%s}";
    }

    @Test
    public void should_have_all_setters_and_getters_working() {
        /* Then */
        assertThat(PODCAST)
                .hasId(id)
                .hasTitle("PodcastDeTest")
                .hasUrl("http://nowhere.com")
                .hasSignature("ae4b93a7e8249d6be591649c936dbe7d")
                .hasType("Youtube")
                .hasLastUpdate(NOW)
                .hasCover(COVER)
                .hasDescription("A long Description")
                .hasHasToBeDeleted(true)
                .hasNoItems()
                .hasNoTags();
    }
    
    @Test
    public void should_have_toString() {
        assertThat(PODCAST.toString()).isEqualTo(String.format(PODCAST_TO_STRING, NOW));
    }
    
    @Test
    public void should_have_hashcode_and_equals() {
        /* Given */
        Podcast samePodcast = new Podcast();
        samePodcast.setId(id);
        samePodcast.setLastUpdate(NOW);
        samePodcast.setSignature("ae4b93a7e8249d6be591649c936dbe7d");
        samePodcast.setTitle("PodcastDeTest");
        samePodcast.setUrl("http://nowhere.com");

        Object notPodcast = new Object();

        /* Then */
        assertThat(PODCAST).isEqualTo(PODCAST);
        assertThat(PODCAST).isNotEqualTo(notPodcast);
        assertThat(PODCAST).isEqualTo(samePodcast);
        assertThat(PODCAST.hashCode()).isNotNull();
    }
    
    @Test
    public void should_add_an_item() {
        Item itemToAdd = new Item();
        /* When */
        PODCAST.add(itemToAdd);
        /* Then */
        assertThat(itemToAdd).hasPodcast(PODCAST);
        assertThat(PODCAST).hasOnlyItems(itemToAdd);
    }

    @Test
    public void should_contains_item() {
        Item itemToAdd = new Item().setId(UUID.randomUUID());
        Item itemToAdd2 = new Item().setId(UUID.randomUUID());
        PODCAST.add(itemToAdd);

        assertThat(PODCAST.contains(itemToAdd)).isTrue();
        assertThat(PODCAST.contains(itemToAdd2)).isFalse();
    }
    
    @Test
    public void should_be_update_now() {
        /* When */
        PODCAST.lastUpdateToNow();
        /* Then */
        assertThat(PODCAST.getLastUpdate().isAfter(NOW));
    }

}