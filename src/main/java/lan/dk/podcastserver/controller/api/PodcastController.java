package lan.dk.podcastserver.controller.api;

import com.fasterxml.jackson.annotation.JsonView;
import lan.dk.podcastserver.business.PodcastBusiness;
import lan.dk.podcastserver.business.find.FindPodcastBusiness;
import lan.dk.podcastserver.business.stats.StatsBusiness;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
import lan.dk.podcastserver.exception.PodcastNotFoundException;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lan.dk.podcastserver.utils.facade.stats.NumberOfItemByDateWrapper;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by kevin on 26/12/2013.
 */
@RestController
@RequestMapping("/api/podcast")
public class PodcastController {

    @Resource PodcastBusiness podcastBusiness;
    @Resource FindPodcastBusiness findPodcastBusiness;
    @Resource StatsBusiness statsBusiness;
    @Resource PodcastServerParameters podcastServerParameters;

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

    @JsonView(Podcast.PodcastListingView.class)
    @RequestMapping(method = RequestMethod.GET)
    public List<Podcast> findAll() {
        return podcastBusiness.findAll();
                
    }

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

    @RequestMapping(value="{id}/stats/byPubdate", method = RequestMethod.POST)
    public Set<NumberOfItemByDateWrapper> statsByPubdate(@PathVariable UUID id, @RequestBody Long numberOfMonth) {
        return statsBusiness.statByPubDate(id, numberOfMonth);
    }

    @RequestMapping(value="{id}/stats/byDownloaddate", method = RequestMethod.POST)
    public Set<NumberOfItemByDateWrapper> statsByDownloadDate(@PathVariable UUID id, @RequestBody Long numberOfMonth) {
        return statsBusiness.statsByDownloadDate(id, numberOfMonth);
    }
}
