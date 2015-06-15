package lan.dk.podcastserver.entity;

import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;

import static lan.dk.podcastserver.entity.ItemAssert.assertThat;

/**
 * Created by kevin on 15/06/15 for HackerRank problem
 */
public class ItemTest {

    public static Item ITEM = new Item();
    public static final Podcast PODCAST = new Podcast();
    public static final ZonedDateTime NOW = ZonedDateTime.now();
    public static final Cover COVER = new Cover("http://fakeItem.com/cover");
    public static final Cover PODCAST_COVER = new Cover("PodcastCover");

    @Before
    public void beforeEach() {
        /* Given */
        PODCAST.setTitle("Fake Podcast");
        PODCAST.setId(1);
        PODCAST.setType("Youtube");
        PODCAST.setCover(PODCAST_COVER);
        PODCAST.setHasToBeDeleted(Boolean.TRUE);


        ITEM = new Item()
                .setId(1)
                .setTitle("Fake Item")
                .setUrl("http://fakeItem.com")
                .setPodcast(PODCAST)
                .setPubdate(NOW)
                .setDescription("Fake item description")
                .setMimeType("video/mp4")
                .setLength(123456L)
                .setCover(COVER)
                .setFileName("fakeItem.mp4");

        Item.rootFolder = Paths.get("/tmp");
        Item.fileContainer = "http://podcast.dk.lan/";
    }

    
    @Test
    public void should_have_be_initialazed() {
        /* When */
        /* Then */
        assertThat(ITEM)
                .hasId(1)
                .hasTitle("Fake Item")
                .hasUrl("http://fakeItem.com")
                .hasPodcast(PODCAST)
                .hasPubdate(NOW)
                .hasDescription("Fake item description")
                .hasMimeType("video/mp4")
                .hasLength(123456L)
                .hasCover(COVER)
                .hasFileName("fakeItem.mp4")
                .hasStatus(Status.NOT_DOWNLOADED.value())
                .hasProgression(0);
    }
    
    @Test
    public void should_change_his_status() {

        /* When */ ITEM.setStatus(Status.PAUSED);
        /* Then */ assertThat(ITEM)
                .hasStatus(Status.PAUSED.value());

        /* When */ ITEM.setStatus("Finish");
        /* Then */ assertThat(ITEM)
                        .hasStatus(Status.FINISH.value());
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
        assertThat(ITEM).hasNumberOfTry(0);

        ITEM.setNumberOfTry(6);
        assertThat(ITEM).hasNumberOfTry(6);

        ITEM.addATry();
        assertThat(ITEM).hasNumberOfTry(7);
    }

    @Test
    public void should_have_a_valid_url() {
        org.assertj.core.api.Assertions.
                assertThat(ITEM.hasValidURL()).isTrue();

        PODCAST.setType("send");
        ITEM.setUrl("");
        org.assertj.core.api.Assertions.
                assertThat(ITEM.hasValidURL()).isTrue();

        PODCAST.setType("Youtube");
        ITEM.setUrl("");
        org.assertj.core.api.Assertions.
                assertThat(ITEM.hasValidURL()).isFalse();

    }

    @Test
    public void should_report_parent_podcast_id() {
        org.assertj.core.api.Assertions.
                assertThat(ITEM.getPodcastId()).isEqualTo(PODCAST.getId());

        ITEM.setPodcast(null);
        org.assertj.core.api.Assertions.
                assertThat(ITEM.getPodcastId())
                    .isNotEqualTo(PODCAST.getId())
                    .isNull();
    }
    
    @Test
    public void should_report_parent_podcast_cover() {
        org.assertj.core.api.Assertions.
                assertThat(ITEM.getCoverOfItemOrPodcast()).isSameAs(COVER);

        ITEM.setCover(null);
        org.assertj.core.api.Assertions.
                assertThat(ITEM.getCoverOfItemOrPodcast()).isSameAs(PODCAST_COVER);
    }
    
    @Test
    public void should_expose_the_API_url() {
        org.assertj.core.api.Assertions.
                assertThat(ITEM.getProxyURLWithoutExtention())
                    .isEqualTo("/api/podcast/1/items/1/download");

        org.assertj.core.api.Assertions.
                assertThat(ITEM.getProxyURL())
                    .isEqualTo("/api/podcast/1/items/1/download.mp4");
    }

    @Test
    public void should_get_the_local_path() {
        /* When */ Path localPath = ITEM.getLocalPath();
        /* Then */ org.assertj.core.api.Assertions.
                assertThat(localPath).hasFileName("fakeItem.mp4");

        /* When */ String localStringPath = ITEM.getLocalUri();
        /* Then */ org.assertj.core.api.Assertions.
                assertThat(localStringPath).isEqualTo("/tmp/Fake Podcast/fakeItem.mp4");
    }
    
    @Test
    public void should_expose_isDownloaded() {
        org.assertj.core.api.Assertions.
                assertThat(ITEM.isDownloaded()).isTrue();

        ITEM.setFileName(null);
        org.assertj.core.api.Assertions.
                assertThat(ITEM.isDownloaded()).isFalse();
    }
    
    @Test
    public void should_expose_the_full_url() {
        /* When */ String localUrl = ITEM.getLocalUrl();
        /* Then */ org.assertj.core.api.Assertions.
                assertThat(localUrl).isEqualTo("http://podcast.dk.lan/Fake Podcast/fakeItem.mp4");
    }

    @Test
    public void should_be_reset() {
        /* Given */ ITEM.setStatus(Status.FINISH);
        /* When  */ ITEM.reset();
        /* Then  */ assertThat(ITEM)
                    .hasFileName(null)
                    .hasDownloadDate(null)
                    .hasStatus(Status.NOT_DOWNLOADED.value());
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
        Item withSameId = new Item().setId(1);
        Item withSameUrl = new Item().setUrl(ITEM.getUrl());
        Item withSameName = new Item().setUrl("http://test.domain.com/toto/fakeItem.com");
        Item withSameLocalUri = new Item().setPodcast(PODCAST).setFileName("fakeItem.mp4");

        /* Then */
        assertThat(ITEM).isEqualTo(ITEM);
        assertThat(ITEM).isNotEqualTo(anObject);
        assertThat(ITEM).isEqualTo(withSameId);
        assertThat(ITEM).isEqualTo(withSameUrl);
        assertThat(ITEM).isEqualTo(withSameName);
        assertThat(ITEM).isEqualTo(withSameLocalUri);

        org.assertj.core.api.Assertions.
                assertThat(ITEM.hashCode()).isEqualTo(new Item().setUrl(ITEM.getUrl()).setPubdate(NOW).hashCode());

    }

    @Test
    public void should_toString() {
        org.assertj.core.api.Assertions.
                assertThat(ITEM.toString())
                    .isEqualTo("Item{id=1, title='Fake Item', url='http://fakeItem.com', pubdate="+ NOW +", description='Fake item description', mimeType='video/mp4', length=123456, status='NOT_DOWNLOADED', progression=0, downloaddate=null, podcast=Podcast{id=1, title='Fake Podcast', url='null', signature='null', type='Youtube', lastUpdate=null}, numberOfTry=0}");

    }

}