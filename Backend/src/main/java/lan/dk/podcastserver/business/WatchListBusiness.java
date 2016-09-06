package lan.dk.podcastserver.business;

import javaslang.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.WatchList;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.WatchListRepository;
import lan.dk.podcastserver.service.JdomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@Component
@RequiredArgsConstructor
public class WatchListBusiness {

    private final WatchListRepository watchListRepository;
    private final ItemRepository itemRepository;
    private final JdomService jdomService;

    public WatchList findOne(UUID id) {
        return Option.of(watchListRepository.findOne(id))
                .getOrElseThrow(() -> new RuntimeException("Watchlist not found"));
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

    public String asRss(UUID id, String domainFromRequest) {
        return jdomService.watchListToXml(findOne(id), domainFromRequest);
    }
}
