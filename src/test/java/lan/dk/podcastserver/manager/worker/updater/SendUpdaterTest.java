package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lan.dk.podcastserver.service.SignatureService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 28/06/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class SendUpdaterTest {

    public static final Item ITEM_1 = new Item().setId(1);
    public static final Item ITEM_2 = new Item().setId(2);
    public static final Item ITEM_3 = new Item().setId(3);

    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SignatureService signatureService;
    @Mock Validator validator;
    @InjectMocks SendUpdater sendUpdater;

    public static final Podcast PODCAST = new Podcast();

    @Before
    public void beforeEach() {
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

}