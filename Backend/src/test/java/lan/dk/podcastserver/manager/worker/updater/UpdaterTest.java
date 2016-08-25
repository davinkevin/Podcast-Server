package lan.dk.podcastserver.manager.worker.updater;

import com.google.common.collect.Sets;
import javaslang.Tuple3;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.SignatureService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.validation.Validator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Created by kevin on 22/06/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class UpdaterTest {

    public static final UUID ERROR_UUID = UUID.randomUUID();
    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SignatureService signatureService;
    @Mock Validator validator;
    @InjectMocks SimpleUpdater simpleUpdater;

    @Test
    public void should_not_update_because_of_same_signature() {
        /* Given */
        Podcast podcast = new Podcast();
        podcast.setId(UUID.randomUUID());
        podcast.setSignature("123456789");

        /* When */
        Tuple3<Podcast, Set<Item>, Predicate<Item>> no_change_result = simpleUpdater.update(podcast);

        /* Then */
        assertThat(no_change_result).isSameAs(Updater.NO_MODIFICATION_TUPLE);
    }

    @Test
    public void should_update_the_podcast() {
        /* Given */
        Podcast podcast = new Podcast();
        podcast.setId(UUID.randomUUID());
        podcast.setSignature("XYZ");

        /* When */
        Tuple3<Podcast, Set<Item>, Predicate<Item>> result = simpleUpdater.update(podcast);

        /* Then */
        assertThat(result).isNotSameAs(Updater.NO_MODIFICATION_TUPLE);
        assertThat(result._1()).isSameAs(podcast);
        assertThat(result._2())
                .isInstanceOf(HashSet.class)
                .hasSize(3);
        assertThat(result._3()).isNotNull();
    }

    @Test
    public void should_handle_exception_during_update() {
        /* Given */
        Podcast podcast = Podcast.builder().id(ERROR_UUID).signature("XYZ").build();

        /* When */
        Tuple3<Podcast, Set<Item>, Predicate<Item>> result = simpleUpdater.update(podcast);

        /* Then */
        assertThat(result).isSameAs(Updater.NO_MODIFICATION_TUPLE);
    }
    
    @Test
    public void should_filter_with_default_predicate() {
        /* Given */
        Podcast podcast = Podcast.builder()
                    .id(UUID.randomUUID())
                    .url("http://a.fake.url/rss.xml")
                    .items(Sets.newHashSet())
                .build();
        podcast.add(new Item().setId(UUID.fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82")));


        /* When */
        Tuple3<Podcast, Set<Item>, Predicate<Item>> result = simpleUpdater.update(podcast);

        Set<Item> collectedItem = result._2()
                .stream()
                .filter(result._3())
                .collect(toSet());

        /* Then */
        assertThat(collectedItem).hasSize(2);

    }

    @After
    public void afterEach() {
        verifyNoMoreInteractions(podcastServerParameters, signatureService, validator);
    }

    static class SimpleUpdater extends AbstractUpdater {

        @Override
        public Set<Item> getItems(Podcast podcast) {
            return Sets.newHashSet(
                    new Item().setId(UUID.fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82")),
                    new Item().setId(UUID.randomUUID()),
                    new Item().setId(UUID.randomUUID())
            );
        }

        @Override
        public String signatureOf(Podcast podcast) {
            if (podcast.getId() == ERROR_UUID) {
                throw new RuntimeException();
            }
            return "123456789";
        }

        @Override
        public Type type() {
            return null;
        }

        @Override
        public Integer compatibility(String url) {
            return -1;
        }
    }
}