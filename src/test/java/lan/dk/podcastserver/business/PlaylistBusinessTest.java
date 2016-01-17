package lan.dk.podcastserver.business;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Playlist;
import lan.dk.podcastserver.entity.PlaylistAssert;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PlaylistRepository;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@RunWith(MockitoJUnitRunner.class)
public class PlaylistBusinessTest {

    @Mock PlaylistRepository playlistRepository;
    @Mock ItemRepository itemRepository;
    @InjectMocks PlaylistBusiness playlistBusiness;

    @Test
    public void should_find_all() {
        /* Given */
        List<Playlist> playlists = Lists.newArrayList();
        when(playlistRepository.findAll()).thenReturn(playlists);

        /* When */
        List<Playlist> all = playlistBusiness.findAll();

        /* Then */
        assertThat(all).isSameAs(playlists);
        verify(playlistRepository, only()).findAll();
    }

    @Test
    public void should_find_all_playlist_with_specified_item() {
        /* Given */
        Integer id = 1;
        Item item = new Item().setId(id);
        Playlist p1 = Playlist.builder().id(UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634")).name("First").build();
        Playlist p2 = Playlist.builder().id(UUID.fromString("86faa982-f462-400a-bc9b-91eb299910b6")).name("Second").build();
        Set<Playlist> playlists = Sets.newHashSet(p1, p2);

        when(itemRepository.findOne(eq(id))).thenReturn(item);
        when(playlistRepository.findContainsItem(eq(item))).thenReturn(playlists);

        /* When */
        Set<Playlist> playlistOfItem = playlistBusiness.findContainsItem(id);

        /* Then */
        assertThat(playlistOfItem).isSameAs(playlists);
        verify(itemRepository, only()).findOne(eq(id));
        verify(playlistRepository, only()).findContainsItem(eq(item));
    }

    @Test
    public void should_add_item_to_playlist() {
        /* Given */
        Integer id = 1;
        Item item = new Item().setId(id).setPlaylists(Sets.newHashSet());
        Playlist playlist = Playlist
                .builder()
                .id(UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634"))
                .name("First")
                .items(Sets.newHashSet())
                .build();

        when(itemRepository.findOne(eq(id))).thenReturn(item);
        when(playlistRepository.findOne(eq(playlist.getId()))).thenReturn(playlist);
        when(playlistRepository.save(any(Playlist.class))).then( i -> i.getArguments()[0]);

        /* When */
        Playlist playlistOfItem = playlistBusiness.add(playlist.getId(), id);

        /* Then */
        assertThat(playlistOfItem).isSameAs(playlist);
        PlaylistAssert
                .assertThat(playlistOfItem)
                .hasItems(item);
        verify(itemRepository, only()).findOne(eq(id));
        verify(playlistRepository, times(1)).findOne(eq(playlist.getId()));
        verify(playlistRepository, times(1)).save(eq(playlist));
    }

    @Test
    public void should_remove_item_to_playlist() {
        /* Given */
        Integer id = 1;
        Item item = new Item().setId(id).setPlaylists(Sets.newHashSet());
        Playlist playlist = Playlist
                .builder()
                    .id(UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634"))
                    .name("First")
                    .items(Sets.newHashSet(item))
                .build();

        when(itemRepository.findOne(eq(id))).thenReturn(item);
        when(playlistRepository.findOne(eq(playlist.getId()))).thenReturn(playlist);
        when(playlistRepository.save(any(Playlist.class))).then( i -> i.getArguments()[0]);

        /* When */
        Playlist playlistOfItem = playlistBusiness.remove(playlist.getId(), id);

        /* Then */
        assertThat(playlistOfItem).isSameAs(playlist);
        PlaylistAssert
                .assertThat(playlistOfItem)
                .doesNotHaveItems(item);
        verify(itemRepository, only()).findOne(eq(id));
        verify(playlistRepository, times(1)).findOne(eq(playlist.getId()));
        verify(playlistRepository, times(1)).save(eq(playlist));
    }

    @Test
    public void should_delete() {
        /* Given */
        UUID id = UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634");

        /* When */
        playlistBusiness.delete(id);

        /* Then */
        verify(playlistRepository, only()).delete(eq(id));
    }

    @Test
    public void should_save() {
        /* Given */
        Playlist playlist = Playlist
                .builder()
                    .id(UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634"))
                    .name("First")
                    .items(Sets.newHashSet())
                .build();


        /* When */
        playlistBusiness.save(playlist);

        /* Then */
        verify(playlistRepository, only()).save(eq(playlist));
    }

    @After
    public void afterEach() {
        verifyNoMoreInteractions(playlistRepository, itemRepository);
    }

}