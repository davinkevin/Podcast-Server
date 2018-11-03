package lan.dk.podcastserver.controller.api;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.davinkevin.podcastserver.service.UrlService;
import io.vavr.collection.Set;
import com.github.davinkevin.podcastserver.business.WatchListBusiness;
import lan.dk.podcastserver.entity.WatchList;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

import static lan.dk.podcastserver.entity.WatchList.WatchListDetailsListView;

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@RestController
@RequestMapping("/api/watchlists")
@RequiredArgsConstructor
public class WatchListController {

    private final WatchListBusiness watchListBusiness;

    @JsonView(WatchListDetailsListView.class)
    @PostMapping
    public WatchList create(@RequestBody WatchList entity) {
        return watchListBusiness.save(entity);
    }

    @JsonView(Object.class)
    @GetMapping
    public Set<WatchList> findAll() {
        return watchListBusiness.findAll();
    }

    @GetMapping("{id}")
    public WatchList findOne(@PathVariable UUID id) {
        return watchListBusiness.findOne(id);
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable UUID id) {
        watchListBusiness.delete(id);
    }

    @JsonView(WatchListDetailsListView.class)
    @PostMapping("{id}/{itemId}")
    public WatchList add(@PathVariable UUID id, @PathVariable UUID itemId) {
        return watchListBusiness.add(id, itemId);
    }

    @JsonView(WatchListDetailsListView.class)
    @DeleteMapping("{id}/{itemId}")
    public WatchList remove(@PathVariable UUID id, @PathVariable UUID itemId) {
        return watchListBusiness.remove(id, itemId);
    }

    @GetMapping(value="{id}/rss", produces = "application/xml; charset=utf-8")
    public String asRss(@PathVariable UUID id, HttpServletRequest request) {
        return watchListBusiness.asRss(id, UrlService.getDomainFromRequest(request));
    }
}
