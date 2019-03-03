package lan.dk.podcastserver.controller.api;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.davinkevin.podcastserver.business.PodcastBusiness;
import com.github.davinkevin.podcastserver.business.find.FindPodcastBusiness;
import com.github.davinkevin.podcastserver.business.update.UpdatePodcastBusiness;
import com.github.davinkevin.podcastserver.entity.Podcast;
import com.github.davinkevin.podcastserver.service.UrlService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;
import java.util.UUID;

/**
 * Created by kevin on 26/12/2013.
 */
@RestController
@RequestMapping("/api/podcasts")
public class PodcastController {

    private final PodcastBusiness podcastBusiness;
    private final FindPodcastBusiness findPodcastBusiness;
    private final UpdatePodcastBusiness updatePodcastBusiness;

    public PodcastController(PodcastBusiness podcastBusiness, FindPodcastBusiness findPodcastBusiness, UpdatePodcastBusiness updatePodcastBusiness) {
        this.podcastBusiness = podcastBusiness;
        this.findPodcastBusiness = findPodcastBusiness;
        this.updatePodcastBusiness = updatePodcastBusiness;
    }

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
    public Podcast patchUpdate(@RequestBody Podcast podcast, @PathVariable("id") UUID id) {
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
    public String asOpml(ServerWebExchange request) { return podcastBusiness.asOpml(UrlService.getDomainFromRequest(request).toASCIIString()); }

    @GetMapping(value="{id}/rss", produces = "application/xml; charset=utf-8")
    public String getRss(@PathVariable UUID id, @RequestParam(value="limit", required = false, defaultValue = "true") Boolean limit, ServerWebExchange request) {
        return podcastBusiness.getRss(id, limit, UrlService.getDomainFromRequest(request).toASCIIString());
    }

    @JsonView(Podcast.PodcastDetailsView.class)
    @PostMapping("fetch")
    public Podcast fetchPodcastInfoByUrl(@RequestBody(required=false) String url) {
        return findPodcastBusiness.fetchPodcastInfoByUrl(url);
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
