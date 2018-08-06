package lan.dk.podcastserver.business;


import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.WatchList;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.WatchListRepository;
import lan.dk.podcastserver.service.JdomService;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@RunWith(MockitoJUnitRunner.class)
public class WatchListBusinessTest {

    @Mock WatchListRepository watchListRepository;
    @Mock ItemRepository itemRepository;
    @Mock JdomService jdomService;
    @InjectMocks WatchListBusiness watchListBusiness;

    @Test
    public void should_find_all() {
        /* Given */
        List<WatchList> watchLists = new ArrayList<>();
        when(watchListRepository.findAll()).thenReturn(watchLists);

        /* When */
        Set<WatchList> all = watchListBusiness.findAll();

        /* Then */
        assertThat(all).isEmpty();
        verify(watchListRepository, only()).findAll();
    }

    @Test
    public void should_find_all_playlist_with_specified_item() {
        /* Given */
        UUID id = UUID.randomUUID();
        Item item = new Item().setId(id);
        WatchList p1 = WatchList.builder().id(UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634")).name("First").build();
        WatchList p2 = WatchList.builder().id(UUID.fromString("86faa982-f462-400a-bc9b-91eb299910b6")).name("Second").build();
        Set<WatchList> watchLists = HashSet.of(p1, p2);

        when(itemRepository.findById(eq(id))).thenReturn(Optional.of(item));
        when(watchListRepository.findContainsItem(eq(item))).thenReturn(watchLists);

        /* When */
        Set<WatchList> watchListOfItem = watchListBusiness.findContainsItem(id);

        /* Then */
        assertThat(watchListOfItem).isSameAs(watchLists);
        verify(itemRepository, only()).findById(eq(id));
        verify(watchListRepository, only()).findContainsItem(eq(item));
    }

    @Test
    public void should_add_item_to_playlist() {
        /* Given */
        UUID id = UUID.randomUUID();
        Item item = new Item().setId(id).setWatchLists(HashSet.<WatchList>empty().toJavaSet());
        WatchList watchList = WatchList
                .builder()
                .id(UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634"))
                .name("First")
                .items(HashSet.<Item>empty().toJavaSet())
                .build();

        when(itemRepository.findById(eq(id))).thenReturn(Optional.of(item));
        when(watchListRepository.findById(eq(watchList.getId()))).thenReturn(Optional.of(watchList));
        when(watchListRepository.save(any(WatchList.class))).then(i -> i.getArguments()[0]);

        /* When */
        WatchList watchListOfItem = watchListBusiness.add(watchList.getId(), id);

        /* Then */
        assertThat(watchListOfItem).isSameAs(watchList);
        assertThat(watchListOfItem).hasItems(item);
        verify(itemRepository, only()).findById(eq(id));
        verify(watchListRepository, times(1)).findById(eq(watchList.getId()));
        verify(watchListRepository, times(1)).save(eq(watchList));
    }

    @Test
    public void should_remove_item_to_playlist() {
        /* Given */
        UUID id = UUID.randomUUID();
        Item item = new Item().setId(id).setWatchLists(HashSet.<WatchList>empty().toJavaSet());
        WatchList watchList = WatchList
                .builder()
                    .id(UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634"))
                    .name("First")
                    .items(HashSet.<Item>empty().toJavaSet())
                .build();

        when(itemRepository.findById(eq(id))).thenReturn(Optional.of(item));
        when(watchListRepository.findById(eq(watchList.getId()))).thenReturn(Optional.of(watchList));
        when(watchListRepository.save(any(WatchList.class))).then(i -> i.getArguments()[0]);

        /* When */
        WatchList watchListOfItem = watchListBusiness.remove(watchList.getId(), id);

        /* Then */
        assertThat(watchListOfItem).isSameAs(watchList);
        assertThat(watchListOfItem).doesNotHaveItems(item);
        verify(itemRepository, only()).findById(eq(id));
        verify(watchListRepository, times(1)).findById(eq(watchList.getId()));
        verify(watchListRepository, times(1)).save(eq(watchList));
    }

    @Test
    public void should_delete() {
        /* Given */
        UUID id = UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634");

        /* When */
        watchListBusiness.delete(id);

        /* Then */
        verify(watchListRepository, only()).deleteById(eq(id));
    }

    @Test
    public void should_save() {
        /* Given */
        WatchList watchList = WatchList
                .builder()
                    .id(UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634"))
                    .name("First")
                    .items(HashSet.<Item>empty().toJavaSet())
                .build();


        /* When */
        watchListBusiness.save(watchList);

        /* Then */
        verify(watchListRepository, only()).save(eq(watchList));
    }

    @Test
    public void should_find_one_by_id() {
        /* Given */
        WatchList watchList = WatchList
                .builder()
                    .id(UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634"))
                    .name("First")
                    .items(HashSet.<Item>empty().toJavaSet())
                .build();

        when(watchListRepository.findById(eq(watchList.getId()))).thenReturn(Optional.of(watchList));

        /* When */
        WatchList aWatchList = watchListBusiness.findOne(watchList.getId());

        /* Then */
        assertThat(aWatchList).isSameAs(watchList);
        verify(watchListRepository, only()).findById(eq(watchList.getId()));
    }

    @Test
    public void should_generate_watchlist_as_xml() {
        /* Given */
        UUID id = UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634");
        String domain = "http://localhost";
        WatchList watchList = WatchList
                .builder()
                    .id(id)
                    .name("First")
                    .items(HashSet.<Item>empty().toJavaSet())
                .build();
        when(jdomService.watchListToXml(eq(watchList), anyString())).thenReturn("anXml");
        when(watchListRepository.findById(eq(id))).thenReturn(Optional.of(watchList));

        /* When */
        String s = watchListBusiness.asRss(id, domain);

        /* Then */
        assertThat(s).isEqualTo("anXml");
        verify(watchListRepository).findById(eq(id));
        verify(jdomService).watchListToXml(same(watchList), eq(domain));
    }

    @After
    public void afterEach() {
        verifyNoMoreInteractions(watchListRepository, itemRepository);
    }

}
