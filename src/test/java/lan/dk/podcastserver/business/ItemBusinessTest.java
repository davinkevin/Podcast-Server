package lan.dk.podcastserver.business;

import com.mysema.query.types.Predicate;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.ItemAssert;
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
import java.util.List;

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
}