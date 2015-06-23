package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lan.dk.podcastserver.service.SignatureService;
import lan.dk.podcastserver.utils.facade.UpdateTuple;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.validation.Validator;
import java.util.Set;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Created by kevin on 22/06/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class AbstractUpdaterTest {

    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SignatureService signatureService;
    @Mock Validator validator;
    @InjectMocks SimpleUpdater simpleUpdater;

    @Test
    public void should_not_update_because_of_same_signature() {
        /* Given */
        Podcast podcast = new Podcast();
        podcast.setId(1);
        podcast.setSignature("123456789");

        /* When */ UpdateTuple<Podcast, Set<Item>, Predicate<Item>> no_change_result = simpleUpdater.update(podcast);
        /* Then */ assertThat(no_change_result).isSameAs(Updater.NO_MODIFICATION_TUPLE);
    }

    @Test
    public void should_update_the_podcast() {
        /* Given */
        Podcast podcast = new Podcast();
        podcast.setId(1);
        podcast.setSignature("XYZ");

        /* When */ UpdateTuple<Podcast, Set<Item>, Predicate<Item>> result = simpleUpdater.update(podcast);
        /* Then */
        assertThat(result).isNotSameAs(Updater.NO_MODIFICATION_TUPLE);
        assertThat(result.first()).isSameAs(podcast);
        assertThat(result.second()).isNull();
        assertThat(result.third()).isNotNull();
    }

    @Test
    public void should_handle_exception_during_update() {
        /* Given */
        Podcast podcast = new Podcast();
        podcast.setId(99);
        podcast.setSignature("XYZ");

        /* When */ UpdateTuple<Podcast, Set<Item>, Predicate<Item>> result = simpleUpdater.update(podcast);
        /* Then */ assertThat(result).isSameAs(Updater.NO_MODIFICATION_TUPLE);
    }

    @After
    public void afterEach() {
        verifyNoMoreInteractions(podcastServerParameters, signatureService, validator);
    }

    static class SimpleUpdater extends AbstractUpdater {

        @Override
        public Set<Item> getItems(Podcast podcast) {
            return null;
        }

        @Override
        public String signatureOf(Podcast podcast) {
            if (podcast.getId() == 99) {
                throw new RuntimeException();
            }
            return "123456789";
        }

        @Override
        public Type type() {
            return null;
        }
    }
}