package lan.dk.podcastserver.controller.api;

import com.fasterxml.jackson.annotation.JsonView;
import javaslang.collection.Set;
import lan.dk.podcastserver.business.ItemBusiness;
import lan.dk.podcastserver.business.WatchListBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.WatchList;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.service.MultiPartFileSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import java.util.UUID;

/**
 * Created by kevin on 26/12/2013.
 */
@Slf4j
@RestController
@RequestMapping("/api/podcasts/{idPodcast}/items")
@RequiredArgsConstructor
public class ItemController {
    
    private final ItemBusiness itemBusiness;
    private final ItemDownloadManager itemDownloadManager;
    private final MultiPartFileSenderService multiPartFileSenderService;
    private final WatchListBusiness watchListBusiness;

    @GetMapping
    @JsonView(Item.ItemPodcastListView.class)
    public Page<Item> findAll(@PathVariable UUID idPodcast, Pageable pageable) {
        return itemBusiness.findByPodcast(idPodcast, pageable);
    }

    @GetMapping("{id}")
    @JsonView(Item.ItemDetailsView.class)
    public Item findById(@PathVariable UUID id) {
        return itemBusiness.findOne(id);
    }

    @PutMapping("{id}")
    @JsonView(Item.ItemDetailsView.class)
    public Item update(@RequestBody Item item, @PathVariable("id") UUID id) {
        item.setId(id);
        return itemBusiness.save(item);
    }

    @GetMapping("{id}/watchlists")
    @JsonView(Object.class)
    public Set<WatchList> getWatchListOfItem(@PathVariable("id") UUID id) {
        return watchListBusiness.findContainsItem(id);
    }

    @DeleteMapping("{id}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void delete (@PathVariable(value = "id") UUID id) {
        itemBusiness.delete(id);
    }

    @GetMapping("{id}/addtoqueue")
    public void addToDownloadList(@PathVariable("id") UUID id) {
        itemDownloadManager.addItemToQueue(id);
    }

    @GetMapping("{id}/download{ext}")
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

    @GetMapping("{id}/cover{ext}")
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


    @GetMapping("{id}/reset")
    @JsonView(Item.ItemDetailsView.class)
    public Item reset(@PathVariable UUID id) {
        return itemBusiness.reset(id);
    }

    @PostMapping("/upload")
    @JsonView(Item.ItemDetailsView.class)
    public Item uploadFile(@PathVariable UUID idPodcast, @RequestPart("file") MultipartFile file) throws IOException, ParseException, URISyntaxException {
        return itemBusiness.addItemByUpload(idPodcast, file);
    }
}
