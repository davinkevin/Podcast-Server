package lan.dk.podcastserver.controller.api;

import lan.dk.podcastserver.business.TagBusiness;
import lan.dk.podcastserver.entity.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * Created by kevin on 07/06/2014.
 */
@RestController
@RequestMapping("/api/tag")
public class TagController {

    @Resource
    TagBusiness tagBusiness;

    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.GET)
    public Tag findById(@PathVariable Integer id) {
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
