package lan.dk.podcastserver.controller.api;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.davinkevin.podcastserver.business.PodcastBusiness;
import com.github.davinkevin.podcastserver.entity.Podcast;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Created by kevin on 26/12/2013.
 */
@RestController
@RequestMapping("/api/podcasts")
public class PodcastController {

    private final PodcastBusiness podcastBusiness;

    public PodcastController(PodcastBusiness podcastBusiness) {
        this.podcastBusiness = podcastBusiness;
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
}
