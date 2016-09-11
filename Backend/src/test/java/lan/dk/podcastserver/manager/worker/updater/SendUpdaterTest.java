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
public class SendUpdaterTest {

    public static final Item ITEM_1 = new Item().setId(UUID.randomUUID());
    public static final Item ITEM_2 = new Item().setId(UUID.randomUUID());
    public static final Item ITEM_3 = new Item().setId(UUID.randomUUID());

    @InjectMocks SendUpdater sendUpdater;

    public static Podcast PODCAST;

    @Before
    public void beforeEach() {
        PODCAST = new Podcast();
        PODCAST.add(ITEM_1);
        PODCAST.add(ITEM_2);
        PODCAST.add(ITEM_3);
    }

    @Test
    public void should_serve_items() {
        assertThat(sendUpdater.getItems(PODCAST))
                .hasSize(3)
                .contains(ITEM_1, ITEM_2, ITEM_3);
    }

    @Test
    public void should_generate_an_empty_signature() {
        assertThat(sendUpdater.signatureOf(PODCAST))
                .isEmpty();
    }

    @Test
    public void should_reject_every_item() {
        assertThat(sendUpdater.notIn(PODCAST).test(new Item()))
                .isFalse();
    }
    
    @Test
    public void should_show_his_type() {
        AbstractUpdater.Type type = sendUpdater.type();
        assertThat(type.key()).isEqualTo("send");
        assertThat(type.name()).isEqualTo("Send");
    }

    @Test
    public void should_not_be_compatible() {
        assertThat(sendUpdater.compatibility("http://foo/bar")).isEqualTo(Integer.MAX_VALUE);
    }
}