package lan.dk.podcastserver.entity;

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
public class ItemTest {

    private static final UUID PODCAST_ID = UUID.randomUUID();
    private static Item ITEM = new Item();
    private static Podcast PODCAST = new Podcast().setId(UUID.randomUUID());
    private static final ZonedDateTime NOW = ZonedDateTime.now();
    private static final Cover COVER = Cover.builder().url("http://fakeItem.com/cover.png").build();
    private static final Cover PODCAST_COVER = Cover.builder().url("PodcastCover").build();
    private static final UUID ID = UUID.randomUUID();

    @Before
    public void beforeEach() throws IOException {
        /* Given */
        PODCAST = Podcast.builder()
                .title("Fake Podcast")
                .id(PODCAST_ID)
                .type("Youtube")
                .cover(PODCAST_COVER)
                .hasToBeDeleted(Boolean.TRUE)
                .build();


        ITEM = new Item()
                .setId(ID)
                .setTitle("Fake Item")
                .setUrl("http://fakeItem.com")
                .setPodcast(PODCAST)
                .setPubDate(NOW)
                .setDescription("Fake item description")
                .setMimeType("video/mp4")
                .setLength(123456L)
                .setCover(COVER)
                .setFileName("fakeItem.mp4");

        Item.rootFolder = Paths.get("/tmp/podcast");

        FileSystemUtils.deleteRecursively(Item.rootFolder.toFile());
        Files.createDirectories(ITEM.getLocalPath().getParent());
        Files.createFile(ITEM.getLocalPath());
    }

    @Test
    public void should_have_be_initialazed() {
        /* When */
        /* Then */
        assertThat(ITEM)
                .hasId(ID)
                .hasTitle("Fake Item")
                .hasUrl("http://fakeItem.com")
                .hasPodcast(PODCAST)
                .hasPubDate(NOW)
                .hasDescription("Fake item description")
                .hasMimeType("video/mp4")
                .hasLength(123456L)
                .hasCover(COVER)
                .hasFileName("fakeItem.mp4")
                .hasStatus(Status.NOT_DOWNLOADED)
                .hasProgression(0);
    }

    @Test
    public void should_change_his_status() {

        /* When */ ITEM.setStatus(Status.PAUSED);
        /* Then */ assertThat(ITEM)
                .hasStatus(Status.PAUSED);

        /* When */ ITEM.setStatus(Status.FINISH);
        /* Then */ assertThat(ITEM)
                .hasStatus(Status.FINISH);
    }

    @Test
    public void should_advance_in_progression() {
        /* When */ ITEM.setProgression(50);
        /* Then */ assertThat(ITEM).hasProgression(50);
    }

    @Test
    public void should_set_the_downloaddate() {
        /* Given */ ZonedDateTime downloaddate = ZonedDateTime.now();
        /* When */  ITEM.setDownloadDate(downloaddate);
        /* Then */  assertThat(ITEM).hasDownloadDate(downloaddate);
    }

    @Test
    public void should_increment_the_number_of_retry() {
        assertThat(ITEM).hasNumberOfFail(0);

        ITEM.setNumberOfFail(6);
        assertThat(ITEM).hasNumberOfFail(6);

        ITEM.addATry();
        assertThat(ITEM).hasNumberOfFail(7);
    }

    @Test
    public void should_have_a_valid_url() {
        assertThat(ITEM.hasValidURL()).isTrue();

        PODCAST.setType("upload");
        ITEM.setUrl("");
        assertThat(ITEM.hasValidURL()).isTrue();

        PODCAST.setType("Youtube");
        ITEM.setUrl("");
        assertThat(ITEM.hasValidURL()).isFalse();

    }

    @Test
    public void should_report_parent_podcast_id() {
        assertThat(ITEM.getPodcastId()).isEqualTo(PODCAST.getId());

        ITEM.setPodcast(null);
        assertThat(ITEM.getPodcastId())
                .isNotEqualTo(PODCAST.getId())
                .isNull();
    }

    @Test
    public void should_report_parent_podcast_cover() {
        assertThat(ITEM.getCoverOfItemOrPodcast())
                .isEqualTo(Cover.builder().url("/api/podcasts/" + PODCAST_ID + "/items/" + ID + "/cover.png").build());

        ITEM.setCover(null);
        assertThat(ITEM.getCoverOfItemOrPodcast()).isSameAs(PODCAST_COVER);
    }

    @Test
    public void should_expose_the_API_url() {
        assertThat(ITEM.getProxyURLWithoutExtention())
                .isEqualTo(String.format("/api/podcasts/%s/items/%s/" + "Fake_Item", PODCAST_ID, ID));

        assertThat(ITEM.getProxyURL())
                .isEqualTo(String.format("/api/podcasts/%s/items/%s/Fake_Item.mp4", PODCAST_ID, ID));
    }

    @Test
    public void should_get_the_local_path() {
        /* When */ Path localPath = ITEM.getLocalPath();
        /* Then */ assertThat(localPath).hasFileName("fakeItem.mp4");

        /* When */ String localStringPath = ITEM.getLocalUri();
        /* Then */ assertThat(localStringPath).isEqualTo("/tmp/podcast/Fake Podcast/fakeItem.mp4");
    }

    @Test
    public void should_expose_isDownloaded() {
        assertThat(ITEM.isDownloaded()).isTrue();

        ITEM.setFileName(null);
        assertThat(ITEM.isDownloaded()).isFalse();
    }

    @Test
    public void should_be_reset() {
        /* Given */ ITEM.setStatus(Status.FINISH);
        /* When  */ ITEM.reset();
        /* Then  */ assertThat(ITEM).hasFileName(null)
                .hasDownloadDate(null)
                .hasStatus(Status.NOT_DOWNLOADED);
    }

    @Test
    public void should_change_the_local_uri() {
        /* When */ ITEM.setLocalUri("http://www.google.fr/mavideo.mp4");
        /* Then */ assertThat(ITEM).hasFileName("mavideo.mp4");
    }

    @Test
    public void should_equals_and_hashcode() {
        /* Given */
        Object anObject = new Object();
        Item withSameId = new Item().setId(ID);
        Item withSameUrl = new Item().setUrl(ITEM.getUrl());
        Item withSameName = new Item().setUrl("http://test.domain.com/toto/fakeItem.com");
        /*Item withSameLocalUri = new Item().setPodcast(PODCAST).setFileName("fakeItem.mp4");*/

        /* Then */
        assertThat(ITEM)
            .isEqualTo(ITEM)
            .isNotEqualTo(anObject)
            .isEqualTo(withSameId)
            .isEqualTo(withSameUrl)
            .isNotEqualTo(withSameName)
            /*.isEqualTo(withSameLocalUri)*/;

        assertThat(ITEM.hashCode()).isEqualTo(new Item().setUrl(ITEM.getUrl()).setPubDate(NOW).hashCode());

    }

    @Test
    public void should_toString() {
        assertThat(ITEM.toString())
                .isEqualTo("Item{id="+ ID +", title='Fake Item', url='http://fakeItem.com', pubDate="+ NOW +", description='Fake item description', mimeType='video/mp4', length=123456, status='NOT_DOWNLOADED', progression=0, downloaddate=null, podcast=Podcast{id="+PODCAST_ID+", title='Fake Podcast', url='null', signature='null', type='Youtube', lastUpdate=null}, numberOfTry=0}");

    }

    @Test
    public void should_delete() {
        /* Given */ ITEM.setStatus(Status.FINISH);
        /* When  */ ITEM.deleteDownloadedFile();
        /* Then  */ assertThat(ITEM).hasFileName(null)
                .hasStatus(Status.DELETED);
    }

    @Test
    public void should_preremove() throws IOException {
        /* Given */
        Path fileToDelete = ITEM.getLocalPath();

        /* When */
        ITEM.preRemove();

        /* Then */
        assertThat(fileToDelete).doesNotExist();
    }

    @Test
    public void should_generate_exception_on_deletion() {
        /* Given */
        Item.rootFolder = Paths.get("/");
        PODCAST.setTitle("sbin");
        ITEM.setFileName("fsck");

        /* When */  ITEM.deleteDownloadedFile();
    }

}
