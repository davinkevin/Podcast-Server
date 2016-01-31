package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Playlist;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PlaylistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@Component
public class PlaylistBusiness {

    private final PlaylistRepository playlistRepository;
    private final ItemRepository itemRepository;

    @Autowired
    public PlaylistBusiness(PlaylistRepository playlistRepository, ItemRepository itemRepository) {
        this.playlistRepository = playlistRepository;
        this.itemRepository = itemRepository;
    }

    public Playlist findOne(UUID id) {
        return playlistRepository.findOne(id);
    }

    public List<Playlist> findAll() {
        return playlistRepository.findAll();
    }

    public Set<Playlist> findContainsItem(Integer itemId) {
        Item item = itemRepository.findOne(itemId);
        return playlistRepository.findContainsItem(item);
    }

    public Playlist add(UUID playlistId, Integer itemId) {
        Playlist playlist = playlistRepository.findOne(playlistId);
        Item item = itemRepository.findOne(itemId);

        return playlistRepository.save(playlist.add(item));
    }

    public Playlist remove(UUID playlistId, Integer itemId) {
        Playlist playlist = playlistRepository.findOne(playlistId);
        Item item = itemRepository.findOne(itemId);

        return playlistRepository.save(playlist.remove(item));
    }

    public void delete(UUID uuid) {
        playlistRepository.delete(uuid);
    }

    public Playlist save(Playlist playlist) {
        return playlistRepository.save(playlist);
    }
}
