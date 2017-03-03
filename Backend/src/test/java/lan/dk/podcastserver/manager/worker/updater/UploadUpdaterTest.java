package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 28/06/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class UploadUpdaterTest {

    private Podcast podcast;
    private final Item item1 = new Item().setId(UUID.randomUUID());
    private final Item item2 = new Item().setId(UUID.randomUUID());
    private final Item item3 = new Item().setId(UUID.randomUUID());

    private @InjectMocks UploadUpdater uploadUpdater;


    @Before
    public void beforeEach() {
        podcast = new Podcast();
        podcast.add(item1);
        podcast.add(item2);
        podcast.add(item3);
    }

    @Test
    public void should_serve_items() {
        assertThat(uploadUpdater.getItems(podcast))
                .hasSize(3)
                .contains(item1, item2, item3);
    }

    @Test
    public void should_generate_an_empty_signature() {
        assertThat(uploadUpdater.signatureOf(podcast))
                .isEmpty();
    }

    @Test
    public void should_reject_every_item() {
        assertThat(uploadUpdater.notIn(podcast).test(new Item()))
                .isFalse();
    }
    
    @Test
    public void should_show_his_type() {
        AbstractUpdater.Type type = uploadUpdater.type();
        assertThat(type.key()).isEqualTo("upload");
        assertThat(type.name()).isEqualTo("Upload");
    }

    @Test
    public void should_not_be_compatible() {
        assertThat(uploadUpdater.compatibility("http://foo/bar")).isEqualTo(Integer.MAX_VALUE);
    }
}