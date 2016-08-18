package lan.dk.podcastserver.controller.api;

import com.fasterxml.jackson.annotation.JsonView;
import lan.dk.podcastserver.business.ItemBusiness;
import lan.dk.podcastserver.business.WatchListBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.WatchList;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.service.MultiPartFileSenderService;
import lan.dk.podcastserver.utils.facade.PageRequestFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Set;
import java.util.UUID;

/**
 * Created by kevin on 26/12/2013.
 */
@Slf4j
@RestController
@RequestMapping("/api/podcast/{idPodcast}/items")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ItemController {
    
    final ItemBusiness itemBusiness;
    final ItemDownloadManager itemDownloadManager;
    final MultiPartFileSenderService multiPartFileSenderService;
    final WatchListBusiness watchListBusiness;

    @RequestMapping(method = RequestMethod.POST)
    @JsonView(Item.ItemPodcastListView.class)
    public Page<Item> findAll(@PathVariable UUID idPodcast, @RequestBody PageRequestFacade pageRequestFacade) {
        return itemBusiness.findByPodcast(idPodcast, pageRequestFacade.toPageRequest());
    }

    @RequestMapping(value="{id}", method = RequestMethod.GET)
    @JsonView(Item.ItemDetailsView.class)
    public Item findById(@PathVariable UUID id) {
        return itemBusiness.findOne(id);
    }

    @RequestMapping(value="{id}", method = RequestMethod.PUT)
    @JsonView(Item.ItemDetailsView.class)
    public Item update(@RequestBody Item item, @PathVariable("id") UUID id) {
        item.setId(id);
        return itemBusiness.save(item);
    }

    @RequestMapping(value="{id}/watchlists", method = RequestMethod.GET)
    @JsonView(Object.class)
    public Set<WatchList> getWatchListOfItem(@PathVariable("id") UUID id) {
        return watchListBusiness.findContainsItem(id);
    }

    @RequestMapping(value="{id}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void delete (@PathVariable(value = "id") UUID id) {
        itemBusiness.delete(id);
    }

    @RequestMapping(value="{id}/addtoqueue", method = RequestMethod.GET)
    public void addToDownloadList(@PathVariable("id") UUID id) {
        itemDownloadManager.addItemToQueue(id);
    }

    @RequestMapping(value="{id}/download{ext}", method = RequestMethod.GET)
    public void getEpisodeFile(@PathVariable UUID id, HttpServletRequest request, HttpServletResponse response) throws Exception {
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

    @RequestMapping(value="{id}/cover{ext}", method = RequestMethod.GET)
    public ResponseEntity<?> getCover(@PathVariable UUID id) throws Exception {
        Item item = itemBusiness.findOne(id);
        Path cover = item.getCoverPath();

        if (Files.notExists(cover))
            return ResponseEntity
                    .ok(new UrlResource(item.getCover().getUrl()));

        return ResponseEntity.ok()
                .lastModified(Files.getLastModifiedTime(cover).toMillis())
                .body(new FileSystemResource(cover.toFile()));
    }


    @RequestMapping(value = "{id}/reset", method = RequestMethod.GET)
    @JsonView(Item.ItemDetailsView.class)
    public Item reset(@PathVariable UUID id) {
        return itemBusiness.reset(id);
    }

    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    @JsonView(Item.ItemDetailsView.class)
    public Item uploadFile(@PathVariable UUID idPodcast, @RequestPart("file") MultipartFile file) throws IOException, ParseException, URISyntaxException {
        return itemBusiness.addItemByUpload(idPodcast, file);
    }
}
