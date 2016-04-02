package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.WatchList;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.WatchListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@Component
public class WatchListBusiness {

    private final WatchListRepository watchListRepository;
    private final ItemRepository itemRepository;

    @Autowired
    public WatchListBusiness(WatchListRepository watchListRepository, ItemRepository itemRepository) {
        this.watchListRepository = watchListRepository;
        this.itemRepository = itemRepository;
    }

    public WatchList findOne(UUID id) {
        return watchListRepository.findOne(id);
    }

    public List<WatchList> findAll() {
        return watchListRepository.findAll();
    }

    public Set<WatchList> findContainsItem(UUID itemId) {
        Item item = itemRepository.findOne(itemId);
        return watchListRepository.findContainsItem(item);
    }

    public WatchList add(UUID watchListId, UUID itemId) {
        WatchList watchList = watchListRepository.findOne(watchListId);
        Item item = itemRepository.findOne(itemId);

        return watchListRepository.save(watchList.add(item));
    }

    public WatchList remove(UUID watchListId, UUID itemId) {
        WatchList watchList = watchListRepository.findOne(watchListId);
        Item item = itemRepository.findOne(itemId);

        return watchListRepository.save(watchList.remove(item));
    }

    public void delete(UUID uuid) {
        watchListRepository.delete(uuid);
    }

    public WatchList save(WatchList watchList) {
        return watchListRepository.save(watchList);
    }
}
