package lan.dk.podcastserver.manager.worker.extractor;

import io.vavr.Tuple2;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.worker.downloader.model.DownloadingItem;
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
        assertThat(extractedValue.getUrls()).isEqualTo(List());
    }

    @Test
    public void should_return_max_compatibility_minus_one() {
        assertThat(extractor.compatibility("foo")).isEqualTo(Integer.MAX_VALUE-1);
    }

}