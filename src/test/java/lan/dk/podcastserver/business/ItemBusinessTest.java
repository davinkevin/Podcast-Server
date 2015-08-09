package lan.dk.podcastserver.business;

import com.mysema.query.types.Predicate;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.ItemAssert;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.PodcastServerParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.FileSystemUtils;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static lan.dk.podcastserver.repository.predicate.ItemPredicate.hasStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 02/08/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class ItemBusinessTest {

    String ROOT_FOLDER = "/tmp/podcast";

    @Mock ItemDownloadManager itemDownloadManager;
    @Mock PodcastServerParameters podcastServerParameters;
    @Mock ItemRepository itemRepository;
    @Mock PodcastBusiness podcastBusiness;
    @Mock MimeTypeService mimeTypeService;
    @InjectMocks ItemBusiness itemBusiness;

    @Before
    public void beforeEach() {
        itemBusiness.setItemDownloadManager(itemDownloadManager);
        FileSystemUtils.deleteRecursively(Paths.get(ROOT_FOLDER).toFile());
    }

    @Test
    public void should_find_all_by_page() {
        PageRequest pageRequest = new PageRequest(1, 3);
        PageImpl<Item> page = new PageImpl<>(new ArrayList<>());
        when(itemRepository.findAll(any(Pageable.class))).thenReturn(page);

        /* When */
        Page<Item> pageResponse = itemBusiness.findAll(pageRequest);

        /* Then */
        assertThat(pageResponse)
                .isSameAs(page);

        verify(itemRepository, times(1)).findAll(eq(pageRequest));
    }

    @Test
    public void should_find_all_by_predicate() {
        /* Given */
        List<Item> items = new ArrayList<>();
        Predicate predicate = mock(Predicate.class);
        when(itemRepository.findAll(any(Predicate.class))).thenReturn(items);

        /* When */
        Iterable<Item> itemsWithStatus = itemBusiness.findAll(predicate);

        /* Then */
        assertThat(itemsWithStatus).isSameAs(items);
        verify(itemRepository, times(1)).findAll(eq(predicate));
    }

    @Test
    public void should_save() {
        /* Given */
        Item item = new Item();
        when(itemRepository.save(any(Item.class))).thenReturn(item);
        /* When */
        Item savedItem = itemBusiness.save(item);
        /* Then */
        ItemAssert
                .assertThat(savedItem)
                .isSameAs(item);
        verify(itemRepository, times(1)).save(eq(item));
    }

    @Test
    public void should_find_by_id() {
        /* Given */
        Integer idOfItem = 33;
        Item item = new Item();
        when(itemRepository.findOne(anyInt())).thenReturn(item);
        /* When */
        Item savedItem = itemBusiness.findOne(idOfItem);
        /* Then */
        ItemAssert
                .assertThat(savedItem)
                .isSameAs(item);
        verify(itemRepository, times(1)).findOne(idOfItem);
    }

    @Test
    public void should_delete() {
        /* Given */
        Integer idOfItem = 33;
        Podcast podcast = new Podcast();
        podcast.setItems(new HashSet<>());
        Item item = new Item();
        item.setPodcast(podcast);
        podcast.getItems().add(item);

        when(itemRepository.findOne(anyInt())).thenReturn(item);

        /* When */
        itemBusiness.delete(idOfItem);
        /* Then */

        verify(itemRepository, times(1)).findOne(idOfItem);
        verify(itemDownloadManager, times(1)).removeItemFromQueueAndDownload(eq(item));
        verify(itemRepository, times(1)).delete(eq(item));
        assertThat(podcast.getItems()).isEmpty();
    }

    @Test
    public void should_reindex() throws InterruptedException {
        /* Given */
        /* When */
        itemBusiness.reindex();
        /* Then */
        verify(itemRepository, times(1)).reindex();
    }

    @Test
    public void should_reset_item() {
        /* Given */
        Integer itemId = 33;
        Item item = mock(Item.class);
        when(item.reset()).thenReturn(item);
        when(itemRepository.findOne(anyInt())).thenReturn(item);
        when(itemDownloadManager.isInDownloadingQueue(any(Item.class))).thenReturn(false);
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        /* When */
        Item resetedItem = itemBusiness.reset(itemId);

        /* Then */
        assertThat(resetedItem)
                .isSameAs(item);
        verify(itemRepository, times(1)).findOne(eq(itemId));
        verify(itemDownloadManager, times(1)).isInDownloadingQueue(eq(item));
        verify(item, times(1)).reset();
        verify(itemRepository, times(1)).save(eq(item));
    }

    @Test
    public void should_reset_a_downloading_item() {
        /* Given */
        Integer itemId = 33;
        Item item = mock(Item.class);
        when(item.reset()).thenReturn(item);
        when(itemRepository.findOne(anyInt())).thenReturn(item);
        when(itemDownloadManager.isInDownloadingQueue(any(Item.class))).thenReturn(true);

        /* When */
        Item resetedItem = itemBusiness.reset(itemId);

        /* Then */
        assertThat(resetedItem).isNull();
        verify(itemRepository, times(1)).findOne(eq(itemId));
        verify(itemDownloadManager, times(1)).isInDownloadingQueue(eq(item));
    }

    @Test
    public void should_find_by_status() {
        /* Given */
        List<Item> items = new ArrayList<>();
        when(itemRepository.findAll(any(Predicate.class))).thenReturn(items);

        /* When */
        Iterable<Item> itemsWithStatus = itemBusiness.findByStatus(Status.NOT_DOWNLOADED, Status.FINISH);

        /* Then */
        assertThat(itemsWithStatus).isSameAs(items);
        verify(itemRepository, times(1)).findAll(eq(hasStatus(Status.NOT_DOWNLOADED, Status.FINISH)));
    }
}