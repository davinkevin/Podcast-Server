package lan.dk.podcastserver.controller.api;

import com.fasterxml.jackson.annotation.JsonView;
import lan.dk.podcastserver.business.PodcastBusiness;
import lan.dk.podcastserver.business.find.FindPodcastBusiness;
import lan.dk.podcastserver.business.stats.StatsBusiness;
import lan.dk.podcastserver.business.update.UpdatePodcastBusiness;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
import lan.dk.podcastserver.exception.PodcastNotFoundException;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lan.dk.podcastserver.utils.facade.stats.NumberOfItemByDateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by kevin on 26/12/2013.
 */
@Slf4j
@RestController
@RequestMapping("/api/podcast")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PodcastController {

    final PodcastBusiness podcastBusiness;
    final FindPodcastBusiness findPodcastBusiness;
    final StatsBusiness statsBusiness;
    final PodcastServerParameters podcastServerParameters;
    final UpdatePodcastBusiness updatePodcastBusiness;

    @RequestMapping(method = {RequestMethod.PUT, RequestMethod.POST})
    public Podcast create(@RequestBody Podcast podcast) {
        return podcastBusiness.create(podcast);
    }

    @JsonView(Podcast.PodcastDetailsView.class)
    @RequestMapping(value="{id}", method = RequestMethod.GET)
    public Podcast findById(@PathVariable UUID id) {
        return podcastBusiness.findOne(id);
    }

    @JsonView(Podcast.PodcastDetailsView.class)
    @RequestMapping(value="{id}", method = RequestMethod.PUT)
    public Podcast update(@RequestBody Podcast podcast, @PathVariable("id") UUID id) {
        podcast.setId(id);
        return podcastBusiness.reatachAndSave(podcast);
    }

    @JsonView(Podcast.PodcastDetailsView.class)
    @RequestMapping(value="{id}", method = RequestMethod.PATCH)
    public Podcast patchUpdate(@RequestBody Podcast podcast, @PathVariable("id") UUID id) throws PodcastNotFoundException {
        podcast.setId(id);
        Podcast patchedPodcast = podcastBusiness.patchUpdate(podcast);
        patchedPodcast.setItems(null);
        return patchedPodcast;
    }

    @RequestMapping(value="{id}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void delete (@PathVariable UUID id) {
        podcastBusiness.delete(id);
    }

    @Cacheable("podcasts")
    @JsonView(Podcast.PodcastListingView.class)
    @RequestMapping(method = RequestMethod.GET)
    public List<Podcast> findAll() { return podcastBusiness.findAll(); }

    @RequestMapping(value="{id}/rss", method = RequestMethod.GET, produces = "application/xml; charset=utf-8")
    public String getRss(@PathVariable UUID id, @RequestParam(value="limit", required = false, defaultValue = "true") Boolean limit) {
        return podcastBusiness.getRss(id, limit);
    }

    @RequestMapping(value="{id}/cover.{ext}", method = RequestMethod.GET, produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.IMAGE_GIF_VALUE})
    public FileSystemResource cover(@PathVariable UUID id) {
        return new FileSystemResource(podcastBusiness.coverOf(id).toFile());
    }

    @JsonView(Podcast.PodcastDetailsView.class)
    @RequestMapping(value="fetch", method = RequestMethod.POST)
    public Podcast fetchPodcastInfoByUrl(@RequestBody String url) throws FindPodcastNotFoundException {
        return findPodcastBusiness.fetchPodcastInfoByUrl(url);
    }

    @RequestMapping(value="{id}/stats/byPubDate", method = RequestMethod.POST)
    public Set<NumberOfItemByDateWrapper> statsByPubdate(@PathVariable UUID id, @RequestBody Long numberOfMonth) {
        return statsBusiness.statsByPubDate(id, numberOfMonth);
    }

    @RequestMapping(value="{id}/stats/byDownloadDate", method = RequestMethod.POST)
    public Set<NumberOfItemByDateWrapper> statsByDownloadDate(@PathVariable UUID id, @RequestBody Long numberOfMonth) {
        return statsBusiness.statsByDownloadDate(id, numberOfMonth);
    }

    @RequestMapping(value="{id}/stats/byCreationDate", method = RequestMethod.POST)
    public Set<NumberOfItemByDateWrapper> statsByCreationDate(@PathVariable UUID id, @RequestBody Long numberOfMonth) {
        return statsBusiness.statsByCreationDate(id, numberOfMonth);
    }

    @RequestMapping(value = "{id}/update", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void updatePodcast (@PathVariable UUID id) {
        updatePodcastBusiness.updatePodcast(id);
    }

    @RequestMapping(value = "{id}/update/force", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void updatePodcastForced (@PathVariable UUID id) {
        updatePodcastBusiness.forceUpdatePodcast(id);
    }
}
