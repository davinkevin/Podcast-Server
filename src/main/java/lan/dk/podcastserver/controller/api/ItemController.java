package lan.dk.podcastserver.controller.api;

import com.fasterxml.jackson.annotation.JsonView;
import lan.dk.podcastserver.business.ItemBusiness;
import lan.dk.podcastserver.business.WatchListBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.WatchList;
import lan.dk.podcastserver.exception.PodcastNotFoundException;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.service.MultiPartFileSenderService;
import lan.dk.podcastserver.utils.facade.PageRequestFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Set;

/**
 * Created by kevin on 26/12/2013.
 */
@Slf4j
@RestController
@RequestMapping("/api/podcast/{idPodcast}/items")
public class ItemController {
    
    @Resource ItemBusiness itemBusiness;
    @Resource ItemDownloadManager itemDownloadManager;
    @Resource MultiPartFileSenderService multiPartFileSenderService;
    @Autowired WatchListBusiness watchListBusiness;

    @RequestMapping(method = RequestMethod.POST)
    @JsonView(Item.ItemPodcastListView.class)
    public Page<Item> findAll(@PathVariable Integer idPodcast, @RequestBody PageRequestFacade pageRequestFacade) {
        return itemBusiness.findByPodcast(idPodcast, pageRequestFacade.toPageRequest());
    }

    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.GET)
    @JsonView(Item.ItemDetailsView.class)
    public Item findById(@PathVariable int id) {
        return itemBusiness.findOne(id);
    }

    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.PUT)
    @JsonView(Item.ItemDetailsView.class)
    public Item update(@RequestBody Item item, @PathVariable(value = "id") int id) {
        item.setId(id);
        return itemBusiness.save(item);
    }

    @RequestMapping(value="{id:[\\d]+}/watchlists", method = RequestMethod.GET)
    @JsonView(Object.class)
    public Set<WatchList> getWatchListOfItem(@PathVariable(value = "id") int id) {
        return watchListBusiness.findContainsItem(id);
    }

    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void delete (@PathVariable(value = "id") int id) {
        itemBusiness.delete(id);
    }

    @RequestMapping(value="{id:[\\d]+}/addtoqueue", method = RequestMethod.GET)
    public void addToDownloadList(@PathVariable(value = "id") int id) {
        itemDownloadManager.addItemToQueue(id);
    }

    @RequestMapping(value="{id:[\\d]+}/download{ext}", method = RequestMethod.GET)
    public void getEpisodeFile(@PathVariable Integer id, HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.debug("Download du fichier d'item {}", id);
        Item item = itemBusiness.findOne(id);
        if (item.isDownloaded()) {
            log.debug("Récupération en local de l'item {} au chemin {}", id, item.getLocalUri());
            multiPartFileSenderService.fromPath(item.getLocalPath())
                    .with(request)
                    .with(response)
                .serveResource();
        } else {
            response.sendRedirect(item.getUrl());
        }
    }

    @RequestMapping(value = "{id:[\\d]+}/reset", method = RequestMethod.GET)
    @JsonView(Item.ItemDetailsView.class)
    public Item reset(@PathVariable Integer id) {
        return itemBusiness.reset(id);
    }

    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    @JsonView(Item.ItemDetailsView.class)
    public Item uploadFile(@PathVariable Integer idPodcast, @RequestPart("file") MultipartFile file) throws PodcastNotFoundException, IOException, ParseException, URISyntaxException {
        return itemBusiness.addItemByUpload(idPodcast, file);
    }
}
