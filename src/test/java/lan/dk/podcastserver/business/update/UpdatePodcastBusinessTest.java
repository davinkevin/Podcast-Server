package lan.dk.podcastserver.business.update;

import lan.dk.podcastserver.business.PodcastBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.PodcastAssert;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.worker.selector.UpdaterSelector;
import lan.dk.podcastserver.manager.worker.updater.Updater;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lan.dk.podcastserver.utils.facade.UpdateTuple;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import javax.validation.Validator;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 09/08/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class UpdatePodcastBusinessTest {

    public static Path rootFolder = Paths.get("/tmp/podcast/");
    @Captor ArgumentCaptor<Item> ITEM_ARGUMENT_CAPTOR;

    @Mock PodcastBusiness podcastBusiness;
    @Mock ItemRepository itemRepository;
    @Mock UpdaterSelector updaterSelector;
    @Mock SimpMessagingTemplate template;
    @Mock PodcastServerParameters podcastServerParameters;
    @Spy TaskExecutor updateExecutor = new SyncTaskExecutor();
    @Spy TaskExecutor manualExecutor = new SyncTaskExecutor();
    @Mock Validator validator;
    @InjectMocks UpdatePodcastBusiness updatePodcastBusiness;

    @Before
    public void beforeEach() {
        Item.rootFolder = rootFolder;
        updatePodcastBusiness.setTimeOut(1, TimeUnit.SECONDS);
    }

    @Test
    public void should_do_di() {
        assertThat(updatePodcastBusiness.podcastBusiness).isNotNull();
        assertThat(updatePodcastBusiness.itemRepository).isNotNull();
        assertThat(updatePodcastBusiness.updaterSelector).isNotNull();
        assertThat(updatePodcastBusiness.template).isNotNull();
        assertThat(updatePodcastBusiness.updateExecutor).isNotNull();
        assertThat(updatePodcastBusiness.manualExecutor).isNotNull();
        assertThat(updatePodcastBusiness.validator).isNotNull();
    }

    @Test
    public void should_delete_old_episode() {
        /* Given */
        when(itemRepository.findAllToDelete(any())).thenReturn(generateItemsOfPodcast(3, new Podcast().setTitle("Title")));
        /* When */
        updatePodcastBusiness.deleteOldEpisode();
        /* Then */
    }

    private List<Item> generateItemsOfPodcast(Integer numberOfItem, Podcast podcast) {
        return IntStream
                .rangeClosed(1, numberOfItem)
                .mapToObj(i -> new Item().setPodcast(podcast).setId(UUID.randomUUID()).setFileName(i + ".mp3"))
                .collect(toList());
    }

    @Test
    public void should_reset_item_with_incorrect_state() {
        /* Given */
        List<Item> items = Arrays.asList(
                new Item().setStatus(Status.STARTED),
                new Item().setStatus(Status.PAUSED)
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
        List<Podcast> podcasts = Arrays.asList(podcast1, podcast2, podcast3);
        when(podcastBusiness.findByUrlIsNotNull()).thenReturn(podcasts);
        when(updaterSelector.of(anyString())).thenReturn(updater);
        when(updater.notIn(any(Podcast.class))).then(i -> {
            Podcast podcast = (Podcast) i.getArguments()[0];
            return (Predicate<Item>) item -> !podcast.contains(item);
        });
        when(updater.update(eq(podcast3))).then(i -> {
            Podcast podcast = (Podcast) i.getArguments()[0];
            return UpdateTuple.of(podcast, generateSetOfItem(10, podcast), updater.notIn(podcast));
        });
        when(updater.update(not(eq(podcast3)))).then(i -> {
            Podcast podcast = (Podcast) i.getArguments()[0];
            return UpdateTuple.of(podcast, podcast.getItems(), updater.notIn(podcast));
        });
        when(validator.validate(any(Item.class))).thenReturn(new HashSet<>());


        /* When */
        updatePodcastBusiness.updatePodcast();

        /* Then */
        PodcastAssert
                .assertThat(podcast1)
                .hasLastUpdate(null);
        PodcastAssert
                .assertThat(podcast2)
                .hasLastUpdate(null);
        assertThat(podcast3.getLastUpdate())
                .isBeforeOrEqualTo(ZonedDateTime.now())
                .isAfterOrEqualTo(now);

        verify(podcastBusiness, times(podcasts.size())).save(any(Podcast.class));
        verify(validator, times(10)).validate(any(Item.class));
    }

    @Test
    public void should_add_no_new_item_in_podcast_becase_every_item_already_exists() {
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
        when(podcastBusiness.findOne(any(UUID.class))).thenReturn(podcast);
        when(updaterSelector.of(anyString())).thenReturn(updater);
        when(updater.notIn(any(Podcast.class))).then(i -> (Predicate<Item>) item -> false);
        when(updater.update(any(Podcast.class))).then(i -> {
            Podcast podcastArgument = (Podcast) i.getArguments()[0];
            return UpdateTuple.of(podcastArgument, generateSetOfItem(10, podcastArgument), updater.notIn(podcastArgument));
        });
        when(validator.validate(any(Item.class))).thenReturn(new HashSet<>());

        /* When */
        updatePodcastBusiness.updatePodcast(UUID.randomUUID());

        /* Then */
        assertThat(podcast.getLastUpdate())
                .isNull();
    }

    @Test
    public void should_update_a_podcast() {
        /* Given */
        ZonedDateTime now = ZonedDateTime.now();
        Podcast podcast = new Podcast().setTitle("podcast1");
        Updater updater = mock(Updater.class);
        when(podcastBusiness.findOne(any(UUID.class))).thenReturn(podcast);
        when(updaterSelector.of(anyString())).thenReturn(updater);
        when(updater.notIn(any(Podcast.class))).then(i -> {
            Podcast podcastArgument = (Podcast) i.getArguments()[0];
            return (Predicate<Item>) item -> !podcastArgument.contains(item);
        });
        when(updater.update(any(Podcast.class))).then(i -> {
            Podcast podcastArgument = (Podcast) i.getArguments()[0];
            return UpdateTuple.of(podcastArgument, generateSetOfItem(10, podcastArgument), updater.notIn(podcastArgument));
        });
        when(validator.validate(any(Item.class))).thenReturn(new HashSet<>());


        /* When */
        updatePodcastBusiness.updatePodcast(UUID.randomUUID());

        /* Then */
        assertThat(podcast.getLastUpdate())
                .isBeforeOrEqualTo(ZonedDateTime.now())
                .isAfterOrEqualTo(now);

        verify(podcastBusiness, times(1)).save(eq(podcast));
        verify(validator, times(10)).validate(any(Item.class));
    }

    @Test
    public void should_not_handle_too_long_update() {
        /* Given */
        updatePodcastBusiness = new UpdatePodcastBusiness(podcastBusiness, itemRepository, updaterSelector, template, podcastServerParameters, updateExecutor, new SimpleAsyncTaskExecutor("test-update"), validator);
        updatePodcastBusiness.setTimeOut(1, TimeUnit.SECONDS);

        Podcast podcast1 = new Podcast().setTitle("podcast1");
        podcast1.setId(UUID.randomUUID());
        Updater updater = mock(Updater.class);
        when(podcastBusiness.findOne(any(UUID.class))).thenReturn(podcast1);
        when(updaterSelector.of(anyString())).thenReturn(updater);
        when(podcastBusiness.save(any(Podcast.class))).thenReturn(podcast1);
        when(updater.notIn(any(Podcast.class))).then(i -> {
            Podcast podcast = (Podcast) i.getArguments()[0];
            return (Predicate<Item>) item -> !podcast.contains(item);
        });
        when(updater.update(any(Podcast.class))).then(i -> {
            TimeUnit.SECONDS.sleep(15);
            Podcast podcast = (Podcast) i.getArguments()[0];
            return UpdateTuple.of(podcast, generateSetOfItem(10, podcast), updater.notIn(podcast));
        });

        /* When */
        updatePodcastBusiness.forceUpdatePodcast(UUID.randomUUID());

        /* Then */
        PodcastAssert
                .assertThat(podcast1)
                .hasLastUpdate(null);

        verify(podcastBusiness, times(2)).findOne(any(UUID.class));
        verify(podcastBusiness, times(1)).save(any(Podcast.class));
    }


    private Set<Item> generateSetOfItem(Integer numberOfItem, Podcast podcast) {
        return IntStream
                .rangeClosed(1, numberOfItem)
                .mapToObj(i -> new Item().setPodcast(podcast).setId(UUID.randomUUID()).setFileName(i + ".mp3"))
                .collect(toSet());
    }
}