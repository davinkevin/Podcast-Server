package lan.dk.podcastserver.service;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.downloader.Downloader;
import lan.dk.podcastserver.manager.worker.finder.Finder;
import lan.dk.podcastserver.manager.worker.selector.DownloaderSelector;
import lan.dk.podcastserver.manager.worker.selector.FinderSelector;
import lan.dk.podcastserver.manager.worker.selector.UpdaterSelector;
import lan.dk.podcastserver.manager.worker.updater.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 17/07/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class WorkerServiceTest {

    @Mock UpdaterSelector updaterSelector;
    @Mock DownloaderSelector downloaderSelector;
    @Mock ApplicationContext context;
    @Mock FinderSelector finderSelector;
    List<Updater> updaters;
    WorkerService workerService;

    @Before
    public void beforeEach() {
        updaters = Arrays.asList(new BeInSportsUpdater(), new CanalPlusUpdater(), new YoutubeUpdater());

        workerService = new WorkerService(updaterSelector, downloaderSelector, finderSelector, updaters);
        workerService.setApplicationContext(context);
    }

    @Test
    public void should_expose_all_types() {
        /* When */ Set<AbstractUpdater.Type> types = workerService.types();
        /* Then */ assertThat(types)
                        .hasSize(3)
                        .extracting("key")
                        .contains("BeInSports", "CanalPlus", "Youtube");
    }

    @Test
    public void should_get_updater_from_context() {
        /* Given */
        Class clazz = WorkerServiceTest.class;
        Updater mockUpdater = mock(Updater.class);
        when(updaterSelector.of(any())).thenReturn(clazz);
        when(context.getBean(any(String.class))).thenReturn(mockUpdater);
        Podcast podcast = new Podcast().setUrl("http://fake.url/");

        /* When */
        Updater updater = workerService.updaterOf(podcast);

        /* Then */
        assertThat(updater).isSameAs(mockUpdater);
        verify(updaterSelector, times(1)).of(eq(podcast.getUrl()));
        verify(context, times(1)).getBean(eq(clazz.getSimpleName()));
    }

    @Test
    public void should_get_downloader_by_type_from_context() {
        /* Given */
        Class clazz = WorkerServiceTest.class;
        Downloader downloader = mock(Downloader.class);
        when(downloaderSelector.of(any())).thenReturn(clazz);
        when(context.getBean(any(String.class))).thenReturn(downloader);
        when(downloader.setItem(any())).thenReturn(downloader);
        Item item = new Item().setUrl("http://fake.url/");

        /* When */
        Downloader downloaderSelected = workerService.downloaderOf(item);

        /* Then */
        assertThat(downloaderSelected).isSameAs(downloader);
        verify(downloaderSelector, times(1)).of(eq(item.getUrl()));
        verify(context, times(1)).getBean(eq(clazz.getSimpleName()));
        verify(downloader, times(1)).setItem(refEq(item));
    }

    @Test
    public void should_get_finder_by_type_from_context() {
        /* Given */
        Class clazz = WorkerServiceTest.class;
        Finder finder = mock(Finder.class);
        when(finderSelector.of(any())).thenReturn(clazz);
        when(context.getBean(any(String.class))).thenReturn(finder);

        /* When */
        Finder finderSelected = workerService.finderOf("http://an.url.com/foo");

        /* Then */
        assertThat(finderSelected).isSameAs(finder);
        verify(finderSelector, times(1)).of(eq("http://an.url.com/foo"));
        verify(context, times(1)).getBean(eq(clazz.getSimpleName()));
    }
}