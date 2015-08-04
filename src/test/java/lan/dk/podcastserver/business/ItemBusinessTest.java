package lan.dk.podcastserver.business;

import com.mysema.query.types.Predicate;
import lan.dk.podcastserver.entity.Item;
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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 02/08/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class ItemBusinessTest {

    @Mock ItemDownloadManager itemDownloadManager;
    @Mock PodcastServerParameters podcastServerParameters;
    @Mock ItemRepository itemRepository;
    @Mock PodcastBusiness podcastBusiness;
    @Mock MimeTypeService mimeTypeService;
    @InjectMocks ItemBusiness itemBusiness;

    @Before
    public void beforeEach() {
        itemBusiness.setItemDownloadManager(itemDownloadManager);
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
}