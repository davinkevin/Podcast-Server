package lan.dk.podcastserver.manager.worker.noop;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.downloader.DownloadingItem;
import lan.dk.podcastserver.manager.worker.noop.PassThroughExtractor;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static io.vavr.API.*;

/**
 * Created by kevin on 03/12/2017
 */
public class PassThroughExtractorTest {

    private PassThroughExtractor extractor;

    @Before
    public void beforeEach() {
        extractor = new PassThroughExtractor();
    }

    @Test
    public void should_return_item_and_its_url_as_is() {
        /* GIVEN */
        Item item = Item.builder()
                .url("Foo")
                .build();

        /* WHEN  */
        DownloadingItem extractedValue = extractor.extract(item);

        /* THEN  */
        assertThat(extractedValue.getItem()).isEqualTo(item);
        assertThat(extractedValue.getUrls()).containsOnly(item.getUrl());
    }

    @Test
    public void should_return_max_compatibility_minus_one() {
        assertThat(extractor.compatibility("foo")).isEqualTo(Integer.MAX_VALUE-1);
    }

}