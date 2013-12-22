package lan.dk.podcastserver.service.api;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.utils.jDomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

//@Service
@Controller
@RequestMapping("/api/podcast")
public class PodcastService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private PodcastRepository podcastRepository;

    @Value("${serverURL}")
    private String serveurURL;

    @Transactional
    @RequestMapping(method = {RequestMethod.PUT, RequestMethod.POST}, produces = "application/json")
    @ResponseBody
    public Podcast create(@RequestBody Podcast podcast) {
        logger.debug("Creation of Podcast : " + podcast.toString());
        return podcastRepository.save(podcast);
    }

    @Transactional(readOnly = true)
    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Podcast findById(@PathVariable int id) {
        return podcastRepository.findOne(id);
    }

    @Transactional//(rollbackFor = PersonNotFoundException.class)
    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.PUT, produces = "application/json")
    public Podcast update(@RequestBody Podcast podcast, @PathVariable(value = "id") int id) {
        podcast.setId(id);
        return podcastRepository.save(podcast);
    }

    public Podcast update(Podcast podcast) {
        return podcastRepository.save(podcast);
    }


    @Transactional//(rollbackFor = PersonNotFoundException.class)
    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void delete (int id) {
        Podcast podcastToDelete = podcastRepository.findOne(id);
        logger.debug("Delete of Podcast : " + podcastToDelete.toString());
        podcastRepository.delete(podcastToDelete);
    }

    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @Transactional(readOnly = true)
    public List<Podcast> findAll() {
        return podcastRepository.findAll();
    }

    @RequestMapping(value="generatePodcastFromURL", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public Podcast generatePodcastFromURL(@RequestBody String URL) {
        logger.debug("URL = " + URL);
        try {
            return jDomUtils.getPodcastFromURL(new URL(URL));
        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }


    @Transactional(readOnly = true)
    @RequestMapping(value="{id:[\\d]+}/rss", method = RequestMethod.GET, produces = "application/xml; charset=utf-8")
    @ResponseBody
    public String getRss(@PathVariable int id) {
        return podcastRepository.findOne(id).toXML(serveurURL);
    }


}
