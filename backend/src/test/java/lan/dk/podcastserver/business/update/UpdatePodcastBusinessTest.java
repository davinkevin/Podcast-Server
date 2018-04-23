package lan.dk.podcastserver.business.update;


import io.vavr.Tuple;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import io.vavr.control.Try;
import lan.dk.podcastserver.business.CoverBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.selector.UpdaterSelector;
import lan.dk.podcastserver.manager.worker.Updater;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static io.vavr.API.Option;
import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 09/08/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class UpdatePodcastBusinessTest {

    private static Path rootFolder = Paths.get("/tmp/podcast/");
    private @Captor ArgumentCaptor<Item> ITEM_ARGUMENT_CAPTOR;

    private @Mock PodcastRepository podcastRepository;
    private @Mock ItemRepository itemRepository;
    private @Mock UpdaterSelector updaterSelector;
    private @Mock SimpMessagingTemplate template;
    private @Mock PodcastServerParameters podcastServerParameters;
    private @Spy ThreadPoolTaskExecutor updateExecutor = new ThreadPoolTaskExecutor();
    private @Spy ThreadPoolTaskExecutor manualExecutor = new ThreadPoolTaskExecutor();
    private @Mock Validator validator;
    private @Mock CoverBusiness coverBusiness;
    private @InjectMocks UpdatePodcastBusiness updatePodcastBusiness;

    @Before
    public void beforeEach() {
        Item.rootFolder = rootFolder;
        updatePodcastBusiness.setTimeOut(1, TimeUnit.SECONDS);
        updateExecutor.initialize();
        manualExecutor.initialize();
    }

    @Test
    public void should_delete_old_episode() {
        /* Given */
        when(itemRepository.findAllToDelete(any())).thenReturn(generateItems(3, new Podcast().setTitle("Title")));
        /* When */
        updatePodcastBusiness.deleteOldEpisode();
        /* Then */
    }

    @Test
    public void should_reset_item_with_incorrect_state() {
        /* Given */
        Set<Item> items = HashSet.of(
                Item.builder().id(UUID.randomUUID()).status(Status.STARTED).build(),
                Item.builder().id(UUID.randomUUID()).status(Status.PAUSED).build()
        );
        when(itemRepository.findByStatus(anyVararg())).thenReturn(items);

        /* When */
        updatePodcastBusiness.resetItemWithIncorrectState();

        /* Then */
        verify(itemRepository, times(2)).save(ITEM_ARGUMENT_CAPTOR.capture());
        assertThat(ITEM_ARGUMENT_CAPTOR.getAllValues())
                .are(new Condition<Item>() {
                    @Override
                    public boolean matches(Item value) {
                        return Status.NOT_DOWNLOADED == value.getStatus();
                    }
                });
    }
    
    @Test
    public void should_check_status_of_update() {
        assertThat(updatePodcastBusiness.isUpdating()).isFalse();
    }

    @Test
    public void should_update_podcasts() {
        /* Given */
        ZonedDateTime now = ZonedDateTime.now();
        Podcast podcast1 = new Podcast().setTitle("podcast1");
        Podcast podcast2 = new Podcast().setTitle("podcast2");
        Podcast podcast3 = new Podcast().setTitle("podcast3");
        Updater updater = mock(Updater.class);
        Set<Podcast> podcasts = HashSet.of(podcast1, podcast2, podcast3);
        when(podcastRepository.findByUrlIsNotNull()).thenReturn(podcasts);
        when(updaterSelector.of(anyString())).thenReturn(updater);
        when(updater.notIn(any(Podcast.class))).then(i -> {
            Podcast podcast = (Podcast) i.getArguments()[0];
            return (Predicate<Item>) item -> !podcast.contains(item);
        });
        when(updater.update(eq(podcast3))).then(i -> {
            Podcast podcast = (Podcast) i.getArguments()[0];
            return Tuple.of(podcast, generateItems(10, podcast), updater.notIn(podcast));
        });
        when(updater.update(not(eq(podcast3)))).then(i -> {
            Podcast podcast = (Podcast) i.getArguments()[0];
            return Tuple.of(podcast, HashSet.ofAll(podcast.getItems()), updater.notIn(podcast));
        });
        when(validator.validate(any(Item.class))).thenReturn(HashSet.<ConstraintViolation<Item>>empty().toJavaSet());


        /* When */
        updatePodcastBusiness.updatePodcast();
        ZonedDateTime lastFullUpdate = updatePodcastBusiness.getLastFullUpdate();

        /* Then */
        assertThat(podcast1).hasLastUpdate(null);
        assertThat(podcast2).hasLastUpdate(null);
        assertThat(podcast3.getLastUpdate())
                .isBeforeOrEqualTo(ZonedDateTime.now())
                .isAfterOrEqualTo(now);
        assertThat(lastFullUpdate).isNotNull();

        verify(podcastRepository, times(podcasts.size())).save(any(Podcast.class));
        verify(validator, times(10)).validate(any(Item.class));
    }

    @Test
    public void should_add_no_new_item_in_podcast_because_every_item_already_exists() {
        /* Given */
        Item item1 = new Item();
        Item item2 = new Item();
        Item item3 = new Item();
        Podcast podcast = new Podcast()
                .setUrl("http://an.superb.url/")
                    .add(item1)
                    .add(item2)
                    .add(item3)
                .setTitle("a title");

        Updater updater = mock(Updater.class);
        when(podcastRepository.findOne(any(UUID.class))).thenReturn(podcast);
        when(updaterSelector.of(anyString())).thenReturn(updater);
        when(updater.notIn(any(Podcast.class))).then(i -> (Predicate<Item>) item -> false);
        when(updater.update(any(Podcast.class))).then(i -> {
            Podcast podcastArgument = (Podcast) i.getArguments()[0];
            return Tuple.of(podcastArgument, generateItems(10, podcastArgument), updater.notIn(podcastArgument));
        });
        when(validator.validate(any(Item.class))).thenReturn(HashSet.<ConstraintViolation<Item>>empty().toJavaSet());

        /* When */
        updatePodcastBusiness.updatePodcast(UUID.randomUUID());

        /* Then */
        assertThat(podcast.getLastUpdate()).isNull();
    }

    @Test
    public void should_update_a_podcast() {
        /* Given */
        ZonedDateTime now = ZonedDateTime.now();
        Podcast podcast = new Podcast().setTitle("podcast1");
        Updater updater = mock(Updater.class);
        when(podcastRepository.findOne(any(UUID.class))).thenReturn(podcast);
        when(updaterSelector.of(anyString())).thenReturn(updater);
        when(updater.notIn(any(Podcast.class))).then(i -> {
            Podcast podcastArgument = (Podcast) i.getArguments()[0];
            return (Predicate<Item>) item -> !podcastArgument.contains(item);
        });
        when(updater.update(any(Podcast.class))).then(i -> {
            Podcast p = (Podcast) i.getArguments()[0];
            return Tuple.of(p, generateItems(10, p), updater.notIn(p));
        });
        when(validator.validate(any(Item.class))).thenReturn(HashSet.<ConstraintViolation<Item>>empty().toJavaSet());


        /* When */
        updatePodcastBusiness.updatePodcast(UUID.randomUUID());

        /* Then */
        assertThat(podcast.getLastUpdate())
                .isBeforeOrEqualTo(ZonedDateTime.now())
                .isAfterOrEqualTo(now);

        verify(podcastRepository, times(1)).save(eq(podcast));
        verify(validator, times(10)).validate(any(Item.class));
    }

    @Test
    public void should_not_handle_too_long_update() {
        /* Given */
        ThreadPoolTaskExecutor manualExecutor = new ThreadPoolTaskExecutor();
        updatePodcastBusiness = new UpdatePodcastBusiness(podcastRepository, itemRepository, updaterSelector, template, podcastServerParameters, updateExecutor, manualExecutor, validator, coverBusiness);
        updatePodcastBusiness.setTimeOut(1, TimeUnit.MILLISECONDS);
        manualExecutor.initialize();

        Podcast podcast1 = new Podcast().setTitle("podcast1");
        podcast1.setId(UUID.randomUUID());
        Updater updater = mock(Updater.class);
        when(podcastRepository.findOne(any(UUID.class))).thenReturn(podcast1);
        when(updaterSelector.of(anyString())).thenReturn(updater);
        when(podcastRepository.save(any(Podcast.class))).thenReturn(podcast1);
        when(updater.notIn(any(Podcast.class))).then(i -> (Predicate<Item>) item -> !i.getArgumentAt(0, Podcast.class).contains(item));
        when(updater.update(any(Podcast.class))).then(i -> {
            TimeUnit.SECONDS.sleep(15);
            Podcast podcast = i.getArgumentAt(0, Podcast.class);
            return Tuple.of(podcast, generateItems(10, podcast).toJavaSet(), updater.notIn(podcast));
        });

        /* When */
        updatePodcastBusiness.forceUpdatePodcast(UUID.randomUUID());

        /* Then */
        assertThat(podcast1).hasLastUpdate(null);

        verify(podcastRepository, times(2)).findOne(any(UUID.class));
        verify(podcastRepository, times(1)).save(any(Podcast.class));
    }

    @Test
    public void should_get_number_of_active_count() {
        /* Given */
        ThreadPoolTaskExecutor updateExecutor = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor manualExecutor = mock(ThreadPoolTaskExecutor.class);
        updatePodcastBusiness = new UpdatePodcastBusiness(podcastRepository, itemRepository, updaterSelector, template, podcastServerParameters, updateExecutor, manualExecutor, validator, coverBusiness);

        /* When */
        Integer numberOfActiveThread = updatePodcastBusiness.getUpdaterActiveCount();
        /* Then */
        assertThat(numberOfActiveThread).isEqualTo(0);
        verify(updateExecutor, times(1)).getActiveCount();
        verify(manualExecutor, times(1)).getActiveCount();
    }

    @Test
    public void should_delete_cover() {
        /* Given */
        Podcast podcast = Podcast.builder().id(UUID.randomUUID()).build();
        HashSet<Item> items = HashSet.of(
                Item.builder().title("Number1").podcast(podcast).build(),
                Item.builder().title("Number2").podcast(podcast).build(),
                Item.builder().title("Number3").podcast(podcast).build()
        );

        items
            .map(Item::getTitle)
            .forEach(t -> Try.run(() -> Files.createFile(Paths.get("/tmp/", t))));

        when(itemRepository.findAllToDelete(any(ZonedDateTime.class))).thenReturn(items);
        when(coverBusiness.getCoverPathOf(any())).then(i -> Option(Paths.get("/tmp/", i.getArgumentAt(0, Item.class).getTitle())));

        /* When */
        updatePodcastBusiness.deleteOldCover();

        /* Then */
        assertThat(Paths.get("/tmp/", "Number1")).doesNotExist();
        assertThat(Paths.get("/tmp/", "Number2")).doesNotExist();
        assertThat(Paths.get("/tmp/", "Number3")).doesNotExist();
    }

    private Set<Item> generateItems(Integer number, Podcast podcast) {
        return HashSet.rangeClosed(1, number)
                .map(i -> Item.builder().podcast(podcast).id(UUID.randomUUID()).fileName(i + ".mp3").build());
    }

}
