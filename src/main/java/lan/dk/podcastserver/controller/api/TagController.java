package lan.dk.podcastserver.controller.api;

import lan.dk.podcastserver.business.TagBusiness;
import lan.dk.podcastserver.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Created by kevin on 07/06/2014 for Podcast Server
 */
@RestController
@RequestMapping("/api/tag")
public class TagController {

    final TagBusiness tagBusiness;

    @Autowired TagController(TagBusiness tagBusiness) {
        this.tagBusiness = tagBusiness;
    }

    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.GET)
    public Tag findById(@PathVariable UUID id) {
        return tagBusiness.findOne(id);
    }

    @RequestMapping(method = RequestMethod.GET)
    public List<Tag> findAll() {
        return tagBusiness.findAll();
    }

    @RequestMapping(method = RequestMethod.POST)
    public List<Tag> findByNameLike(@RequestParam String name) {
        return tagBusiness.findByNameLike(name);
    }
}
