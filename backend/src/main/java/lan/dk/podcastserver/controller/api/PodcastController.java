package lan.dk.podcastserver.controller.api;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.davinkevin.podcastserver.service.UrlService;
import io.vavr.collection.Set;
import com.github.davinkevin.podcastserver.business.PodcastBusiness;
import com.github.davinkevin.podcastserver.business.find.FindPodcastBusiness;
import com.github.davinkevin.podcastserver.business.stats.NumberOfItemByDateWrapper;
import com.github.davinkevin.podcastserver.business.stats.StatsBusiness;
import com.github.davinkevin.podcastserver.business.update.UpdatePodcastBusiness;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.PodcastNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Created by kevin on 26/12/2013.
 */
@Slf4j
@RestController
@RequestMapping("/api/podcasts")
@RequiredArgsConstructor
public class PodcastController {

    private final PodcastBusiness podcastBusiness;
    private final FindPodcastBusiness findPodcastBusiness;
    private final StatsBusiness statsBusiness;
    private final UpdatePodcastBusiness updatePodcastBusiness;

    @RequestMapping(method = {RequestMethod.PUT, RequestMethod.POST})
    public Podcast create(@RequestBody Podcast podcast) {
        return podcastBusiness.create(podcast);
    }

    @JsonView(Podcast.PodcastDetailsView.class)
    @GetMapping("{id}")
    public Podcast findById(@PathVariable UUID id) {
        return podcastBusiness.findOne(id);
    }

    @JsonView(Podcast.PodcastDetailsView.class)
    @PutMapping("{id}")
    public Podcast update(@RequestBody Podcast podcast, @PathVariable("id") UUID id) {
        podcast.setId(id);
        return podcastBusiness.reatachAndSave(podcast);
    }

    @JsonView(Podcast.PodcastDetailsView.class)
    @PatchMapping("{id}")
    public Podcast patchUpdate(@RequestBody Podcast podcast, @PathVariable("id") UUID id) throws PodcastNotFoundException {
        podcast.setId(id);
        Podcast patchedPodcast = podcastBusiness.patchUpdate(podcast);
        patchedPodcast.setItems(null);
        return patchedPodcast;
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete (@PathVariable UUID id) {
        podcastBusiness.delete(id);
    }

    @GetMapping
    @Cacheable("podcasts")
    @JsonView(Podcast.PodcastListingView.class)
    public List<Podcast> findAll() { return podcastBusiness.findAll(); }

    @GetMapping(value="/opml", produces = "application/xml; charset=utf-8")
    public String asOpml(HttpServletRequest request) { return podcastBusiness.asOpml(UrlService.getDomainFromRequest(request)); }

    @GetMapping(value="{id}/rss", produces = "application/xml; charset=utf-8")
    public String getRss(@PathVariable UUID id, @RequestParam(value="limit", required = false, defaultValue = "true") Boolean limit, HttpServletRequest request) {
        return podcastBusiness.getRss(id, limit, UrlService.getDomainFromRequest(request));
    }

    @GetMapping(value="{id}/cover.{ext}", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.IMAGE_GIF_VALUE})
    public ResponseEntity<?> cover(@PathVariable UUID id) throws IOException {
        Path cover = podcastBusiness.coverOf(id);

        if (Files.notExists(cover))
            return ResponseEntity.notFound().build();

       return ResponseEntity.ok()
               .lastModified(Files.getLastModifiedTime(cover).toMillis())
               .body(new FileSystemResource(cover.toFile()));
    }

    @JsonView(Podcast.PodcastDetailsView.class)
    @PostMapping("fetch")
    public Podcast fetchPodcastInfoByUrl(@RequestBody(required=false) String url) {
        return findPodcastBusiness.fetchPodcastInfoByUrl(url);
    }

    @PostMapping(value="{id}/stats/byPubDate")
    public Set<NumberOfItemByDateWrapper> statsByPubdate(@PathVariable UUID id, @RequestBody Long numberOfMonth) {
        return statsBusiness.statsByPubDate(id, numberOfMonth);
    }

    @PostMapping("{id}/stats/byDownloadDate")
    public Set<NumberOfItemByDateWrapper> statsByDownloadDate(@PathVariable UUID id, @RequestBody Long numberOfMonth) {
        return statsBusiness.statsByDownloadDate(id, numberOfMonth);
    }

    @PostMapping("{id}/stats/byCreationDate")
    public Set<NumberOfItemByDateWrapper> statsByCreationDate(@PathVariable UUID id, @RequestBody Long numberOfMonth) {
        return statsBusiness.statsByCreationDate(id, numberOfMonth);
    }

    @GetMapping("{id}/update")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePodcast (@PathVariable UUID id) {
        updatePodcastBusiness.updatePodcast(id);
    }

    @GetMapping("{id}/update/force")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePodcastForced (@PathVariable UUID id) {
        updatePodcastBusiness.forceUpdatePodcast(id);
    }
}
