package lan.dk.podcastserver.controller.api;

import lan.dk.podcastserver.business.PodcastBusiness;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.PodcastNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;

/**
 * Created by kevin on 26/12/2013.
 */
@RestController
@RequestMapping("/api/podcast")
public class PodcastController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource private PodcastBusiness podcastBusiness;

    @RequestMapping(method = {RequestMethod.PUT, RequestMethod.POST})
    public Podcast create(@RequestBody Podcast podcast) {
        return podcastBusiness.create(podcast);
    }

    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.GET)
    public Podcast findById(@PathVariable Integer id) {
        return podcastBusiness.findOne(id);
    }

    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.PUT)
    public Podcast update(@RequestBody Podcast podcast, @PathVariable(value = "id") Integer id) {
        podcast.setId(id);
        return podcastBusiness.reatachAndSave(podcast);
    }

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

    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    public List<Podcast> findAll() {
        //TODO : Using JSONVIEW or Waiting https://jira.spring.io/browse/SPR-7156 to be solved in 1/JUL/2014
        return podcastBusiness.findAll();
                
    }

    @RequestMapping(value="{id:[\\d]+}/rss", method = RequestMethod.GET, produces = "application/xml; charset=utf-8")
    public String getRss(@PathVariable Integer id) {
        return podcastBusiness.getRss(id);
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
}
