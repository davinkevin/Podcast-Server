package lan.dk.podcastserver.business;

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.WatchList;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.WatchListRepository;
import com.github.davinkevin.podcastserver.service.JdomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
        return Option.ofOptional(watchListRepository.findById(id))
                .getOrElseThrow(() -> new RuntimeException("Watchlist not found"));
    }

    public Set<WatchList> findAll() {
        return HashSet.ofAll(watchListRepository.findAll());
    }

    public Set<WatchList> findContainsItem(UUID itemId) {
        return Option.ofOptional(itemRepository.findById(itemId))
                .map(watchListRepository::findContainsItem)
                .getOrElse(HashSet.empty());
    }

    public WatchList add(UUID watchListId, UUID itemId) {
        WatchList watchList = findOne(watchListId);
        Item item = Option.ofOptional(itemRepository.findById(itemId)).getOrElseThrow(() -> new Error("Item with ID "+ itemId +" not found"));

        return watchListRepository.save(watchList.add(item));
    }

    public WatchList remove(UUID watchListId, UUID itemId) {
        WatchList watchList = findOne(watchListId);
        Item item = Option.ofOptional(itemRepository.findById(itemId)).getOrElseThrow(() -> new Error("Item with ID "+ itemId +" not found"));

        return watchListRepository.save(watchList.remove(item));
    }

    public void delete(UUID uuid) {
        watchListRepository.deleteById(uuid);
    }

    public WatchList save(WatchList watchList) {
        return watchListRepository.save(watchList);
    }

    public String asRss(UUID id, String domainFromRequest) {
        return jdomService.watchListToXml(findOne(id), domainFromRequest);
    }
}
