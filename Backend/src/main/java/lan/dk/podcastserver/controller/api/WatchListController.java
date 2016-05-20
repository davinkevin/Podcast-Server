package lan.dk.podcastserver.controller.api;

import com.fasterxml.jackson.annotation.JsonView;
import lan.dk.podcastserver.business.WatchListBusiness;
import lan.dk.podcastserver.entity.WatchList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static lan.dk.podcastserver.entity.WatchList.*;

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@RestController
@RequestMapping("/api/watchlists")
public class WatchListController {

    @Autowired WatchListBusiness watchListBusiness;

    @JsonView(WatchListDetailsListView.class)
    @RequestMapping(method = RequestMethod.POST)
    public WatchList create(@RequestBody WatchList entity) {
        return watchListBusiness.save(entity);
    }

    @JsonView(Object.class)
    @RequestMapping(method = RequestMethod.GET)
    public List<WatchList> findAll() {
        return watchListBusiness.findAll();
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public WatchList findOne(@PathVariable UUID id) {
        return watchListBusiness.findOne(id);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public void delete(@PathVariable UUID id) {
        watchListBusiness.delete(id);
    }

    @JsonView(WatchListDetailsListView.class)
    @RequestMapping(value = "{id}/{itemId}", method = RequestMethod.POST)
    public WatchList add(@PathVariable UUID id, @PathVariable UUID itemId) {
        return watchListBusiness.add(id, itemId);
    }

    @JsonView(WatchListDetailsListView.class)
    @RequestMapping(value = "{id}/{itemId}", method = RequestMethod.DELETE)
    public WatchList remove(@PathVariable UUID id, @PathVariable UUID itemId) {
        return watchListBusiness.remove(id, itemId);
    }
}
