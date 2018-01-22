package lan.dk.podcastserver.manager.worker.extractor;

import io.vavr.Tuple2;
import lan.dk.podcastserver.entity.Item;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 03/12/2017
 */
public class NoOpExtractorTest {

    private NoOpExtractor extractor;

    @Before
    public void beforeEach() {
        extractor = new NoOpExtractor();
    }

    @Test
    public void should_return_item_and_its_url_as_is() {
        /* GIVEN */
        Item item = Item.builder()
                .url("Foo")
                .build();

        /* WHEN  */
        Tuple2<Item, String> extractedValue = extractor.extract(item);

        /* THEN  */
        assertThat(extractedValue._1()).isEqualTo(item);
        assertThat(extractedValue._2()).isEqualTo(item.getUrl());
    }

    @Test
    public void should_return_max_compatibility() {
        assertThat(extractor.compatibility("foo")).isEqualTo(Integer.MAX_VALUE);
    }

}