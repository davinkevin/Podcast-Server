package lan.dk.podcastserver.controller.api;

import lan.dk.podcastserver.business.PodcastBusiness;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.PodcastNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;

/**
 * Created by kevin on 26/12/2013.
 */
@Controller
@RequestMapping("/api/podcast")
public class PodcastController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource private PodcastBusiness podcastBusiness;

    @RequestMapping(method = {RequestMethod.PUT, RequestMethod.POST}, produces = "application/json")
    @ResponseBody
    public Podcast create(@RequestBody Podcast podcast) {
        return podcastBusiness.reatachAndSave(podcast);
    }

    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.GET)
    @ResponseBody
    public Podcast findById(@PathVariable int id) {
        Podcast podcast = podcastBusiness.findOne(id);
        podcast.setItems(null);
        return podcast;
    }

    /*
    @RequestMapping(value="{id:[\\d]+}/items", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Collection<Item> findItemsByPodcastId(@PathVariable int id) {
        return podcastBusiness.getItems(id);
    }
    */

    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.PUT, produces = "application/json")
    @ResponseBody
    public Podcast update(@RequestBody Podcast podcast, @PathVariable(value = "id") int id) {
        podcast.setId(id);
        return podcastBusiness.reatachAndSave(podcast);
    }

    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.PATCH, produces = "application/json")
    @ResponseBody
    public Podcast patchUpdate(@RequestBody Podcast podcast, @PathVariable(value = "id") int id) throws PodcastNotFoundException {
        podcast.setId(id);
        Podcast patchedPodcast = podcastBusiness.patchUpdate(podcast);
        patchedPodcast.setItems(null);
        return patchedPodcast;
    }

    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void delete (@PathVariable int id) {
        podcastBusiness.delete(id);
    }

    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<Podcast> findAll() {

        //TODO : Using JSONVIEW or Waiting https://jira.spring.io/browse/SPR-7156 to be solved in 1/JUL/2014
        List<Podcast> podcastList = podcastBusiness.findAll();
        for(Podcast podcast : podcastList) {
            podcast.setItems(null);
        }

        return podcastList;
    }

    @RequestMapping(value="{id:[\\d]+}/rss", method = RequestMethod.GET, produces = "application/xml; charset=utf-8")
    @ResponseBody
    public String getRss(@PathVariable int id) {
        return podcastBusiness.getRss(id);
    }

    @RequestMapping(value="{id:[\\d]+}/upload", method=RequestMethod.POST)
    @ResponseBody
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
