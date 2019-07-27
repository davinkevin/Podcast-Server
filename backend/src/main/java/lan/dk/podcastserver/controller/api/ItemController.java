package lan.dk.podcastserver.controller.api;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.davinkevin.podcastserver.business.ItemBusiness;
import com.github.davinkevin.podcastserver.business.WatchListBusiness;
import com.github.davinkevin.podcastserver.entity.Item;
import com.github.davinkevin.podcastserver.entity.WatchList;
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager;
import io.vavr.collection.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.UUID;

/**
 * Created by kevin on 26/12/2013.
 */
@RestController
@RequestMapping("/api/podcasts/{idPodcast}/items")
public class ItemController {

    private final ItemBusiness itemBusiness;
    private final ItemDownloadManager itemDownloadManager;
    private final WatchListBusiness watchListBusiness;

    public ItemController(ItemBusiness itemBusiness, ItemDownloadManager itemDownloadManager, WatchListBusiness watchListBusiness) {
        this.itemBusiness = itemBusiness;
        this.itemDownloadManager = itemDownloadManager;
        this.watchListBusiness = watchListBusiness;
    }

    @GetMapping("{id}/watchlists")
    @JsonView(Object.class)
    public Set<WatchList> getWatchListOfItem(@PathVariable("id") UUID id) {
        return watchListBusiness.findContainsItem(id);
    }

    @DeleteMapping("{id}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable(value = "id") UUID id) {
        itemBusiness.delete(id);
    }

    @GetMapping("{id}/addtoqueue")
    public void addToDownloadList(@PathVariable("id") UUID id) {
        itemDownloadManager.addItemToQueue(id);
    }
}
