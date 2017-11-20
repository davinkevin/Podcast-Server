package lan.dk.podcastserver.entity;

import io.vavr.collection.HashSet;
import io.vavr.control.Option;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.UUID;

import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 15/06/15 for HackerRank problem
 */
public class PodcastTest {

    private static final ZonedDateTime NOW = ZonedDateTime.now();
    private static final Cover COVER = Cover.builder().url("ACover.jpg").build();
    private static String PODCAST_TO_STRING;
    private Podcast podcast;
    private UUID id;

    @Before
    public void init() {
        Podcast.rootFolder = Paths.get("/tmp");

        /* Given */
        id = UUID.randomUUID();
        podcast = Podcast
                .builder()
                    .id(id)
                    .title("PodcastDeTest")
                    .url("http://nowhere.com")
                    .signature("ae4b93a7e8249d6be591649c936dbe7d")
                    .type("Youtube")
                    .lastUpdate(NOW)
                    .cover(COVER)
                    .description("A long Description")
                    .hasToBeDeleted(true)
                    .items(HashSet.<Item>empty().toJavaSet())
                    .tags(HashSet.<Tag>empty().toJavaSet())
                .build();

        PODCAST_TO_STRING = "Podcast{id="+ id +", title='PodcastDeTest', url='http://nowhere.com', signature='ae4b93a7e8249d6be591649c936dbe7d', type='Youtube', lastUpdate=%s}";

        FileSystemUtils.deleteRecursively(Podcast.rootFolder.resolve(podcast.getTitle()).toFile());
    }

    @Test
    public void should_have_all_setters_and_getters_working() {
        /* Then */
        assertThat(podcast)
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
        assertThat(podcast.toString()).isEqualTo(String.format(PODCAST_TO_STRING, NOW));
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
        assertThat(podcast).isEqualTo(podcast);
        assertThat(podcast).isNotEqualTo(notPodcast);
        assertThat(podcast).isEqualTo(samePodcast);
        assertThat(podcast.hashCode()).isNotNull();
    }
    
    @Test
    public void should_add_an_item() {
        Item itemToAdd = new Item();
        /* When */
        podcast.add(itemToAdd);
        /* Then */
        assertThat(itemToAdd).hasPodcast(podcast);
        assertThat(podcast).hasOnlyItems(itemToAdd);
    }

    @Test
    public void should_contains_item() {
        Item itemToAdd = new Item().setId(UUID.randomUUID());
        Item itemToAdd2 = new Item().setId(UUID.randomUUID());
        podcast.add(itemToAdd);

        assertThat(podcast.contains(itemToAdd)).isTrue();
        assertThat(podcast.contains(itemToAdd2)).isFalse();
    }
    
    @Test
    public void should_be_update_now() {
        /* When */
        podcast.lastUpdateToNow();
        /* Then */
        assertThat(podcast.getLastUpdate().isAfter(NOW));
    }

    @Test
    public void should_delete_on_post_remove() throws IOException {
        /* Given */
        Podcast.rootFolder = Paths.get("/tmp");
        podcast.setHasToBeDeleted(true);
        Path podcastFolder = Podcast.rootFolder.resolve("PodcastDeTest");
        Path coverFile = podcastFolder.resolve("cover.jpg");
        Files.createDirectory(podcastFolder);
        Files.createFile(coverFile);

        /* When */
        podcast.postRemove();

        /* Then */
        assertThat(coverFile).doesNotExist();
        assertThat(podcastFolder).doesNotExist();
    }

    @Test
    public void should_not_delete_if_podcast_is_not_auto_delete() throws IOException {
        Podcast.rootFolder = Paths.get("/tmp");
        podcast.setHasToBeDeleted(false);
        Path podcastFolder = Podcast.rootFolder.resolve("PodcastDeTest");
        Path coverFile = podcastFolder.resolve("cover.jpg");
        Files.createDirectory(podcastFolder);
        Files.createFile(coverFile);

        /* When */
        podcast.postRemove();

        /* Then */
        assertThat(coverFile).exists();
        assertThat(podcastFolder).exists();
    }

    @Test
    public void should_provide_cover_path() {
        /* GIVEN */
        /* WHEN  */
        Option<Path> coverPath = podcast.getCoverPath();

        /* THEN  */
        assertThat(coverPath).contains(Podcast.rootFolder.resolve(podcast.getTitle()).resolve("cover.jpg"));
    }

}
