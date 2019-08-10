package lan.dk.podcastserver.controller.api;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.davinkevin.podcastserver.business.PodcastBusiness;
import com.github.davinkevin.podcastserver.business.find.FindPodcastBusiness;
import com.github.davinkevin.podcastserver.business.update.UpdatePodcastBusiness;
import com.github.davinkevin.podcastserver.entity.Podcast;
import com.github.davinkevin.podcastserver.service.UrlService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;

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


    @JsonView(Podcast.PodcastDetailsView.class)
    @PostMapping("fetch")
    public Podcast fetchPodcastInfoByUrl(@RequestBody(required=false) String url) {
        return findPodcastBusiness.fetchPodcastInfoByUrl(url);
    }
}
