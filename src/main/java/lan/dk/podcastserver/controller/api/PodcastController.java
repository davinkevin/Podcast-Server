package lan.dk.podcastserver.controller.api;

import com.fasterxml.jackson.annotation.JsonView;
import lan.dk.podcastserver.business.FindPodcastBusiness;
import lan.dk.podcastserver.business.PodcastBusiness;
import lan.dk.podcastserver.business.stats.StatsBusiness;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
import lan.dk.podcastserver.exception.PodcastNotFoundException;
import lan.dk.podcastserver.utils.facade.stats.NumberOfItemByDateWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

/**
 * Created by kevin on 26/12/2013.
 */
@RestController
@RequestMapping("/api/podcast")
public class PodcastController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource PodcastBusiness podcastBusiness;
    @Resource FindPodcastBusiness findPodcastBusiness;
    @Resource StatsBusiness statsBusiness;

    @RequestMapping(method = {RequestMethod.PUT, RequestMethod.POST})
    public Podcast create(@RequestBody Podcast podcast) {
        return podcastBusiness.create(podcast);
    }

    @JsonView(Podcast.PodcastDetailsView.class)
    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.GET)
    public Podcast findById(@PathVariable Integer id) {
        return podcastBusiness.findOne(id);
    }

    @JsonView(Podcast.PodcastDetailsView.class)
    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.PUT)
    public Podcast update(@RequestBody Podcast podcast, @PathVariable(value = "id") Integer id) {
        podcast.setId(id);
        return podcastBusiness.reatachAndSave(podcast);
    }

    @JsonView(Podcast.PodcastDetailsView.class)
    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.PATCH)
    public Podcast patchUpdate(@RequestBody Podcast podcast, @PathVariable(value = "id") Integer id) throws PodcastNotFoundException {
        podcast.setId(id);
        Podcast patchedPodcast = podcastBusiness.patchUpdate(podcast);
        patchedPodcast.setItems(null);
        return patchedPodcast;
    }

    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void delete (@PathVariable Integer id) {
        podcastBusiness.delete(id);
    }

    @JsonView(Podcast.PodcastListingView.class)
    @RequestMapping(method = RequestMethod.GET)
    public List<Podcast> findAll() {
        //TODO : Using JSONVIEW or Waiting https://jira.spring.io/browse/SPR-7156 to be solved in 1/JUL/2014
        return podcastBusiness.findAll();
                
    }

    @RequestMapping(value="{id:[\\d]+}/rss", method = RequestMethod.GET, produces = "application/xml; charset=utf-8")
    public String getRss(@PathVariable Integer id, @RequestParam(value="limit", required = false, defaultValue = "true") Boolean limit) {
        return podcastBusiness.getRss(id, limit);
    }

    @Deprecated
    @RequestMapping(value="{id:[\\d]+}/upload", method=RequestMethod.POST)
    public String handleFileUpload(@PathVariable Integer id, @RequestParam("file") MultipartFile file, @RequestParam("name") String name) throws PodcastNotFoundException {
        logger.info("Envoie du fichier : {}", name);
        if (!file.isEmpty()) {
            try {
                podcastBusiness.addItemByUpload(id, file, name);
                logger.debug("Réception de {} effectuée", name);
                return "You successfully uploaded " + name;
            } catch (Exception e) {
                logger.error("Réception de {} échouée", name, e);
                return "You failed to upload " + name + " => " + e.getMessage();
            }
        } else {
            logger.debug("You failed to upload {} because the file was empty.", name);
            return "You failed to upload " + name + " because the file was empty.";
        }
    }

    @JsonView(Podcast.PodcastDetailsView.class)
    @RequestMapping(value="fetch", method = RequestMethod.POST)
    public Podcast fetchPodcastInfoByUrl(@RequestBody String url) throws FindPodcastNotFoundException {
        return findPodcastBusiness.fetchPodcastInfoByUrl(url);
    }

    @RequestMapping(value="{id:[\\d]+}/stats/byPubdate", method = RequestMethod.POST)
    public Set<NumberOfItemByDateWrapper> statsByPubdate(@PathVariable Integer id, @RequestBody Long numberOfMonth) {
        return statsBusiness.statByPubDate(id, numberOfMonth);
    }

    @RequestMapping(value="{id:[\\d]+}/stats/byDownloaddate", method = RequestMethod.POST)
    public Set<NumberOfItemByDateWrapper> statsByDownloadDate(@PathVariable Integer id, @RequestBody Long numberOfMonth) {
        return statsBusiness.statsByDownloadDate(id, numberOfMonth);
    }
}
