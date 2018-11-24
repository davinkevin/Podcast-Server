package lan.dk.podcastserver.controller.api;

import com.github.davinkevin.podcastserver.business.TagBusiness;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import com.github.davinkevin.podcastserver.entity.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Created by kevin on 07/06/2014 for Podcast Server
 */
@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagBusiness tagBusiness;

    @java.beans.ConstructorProperties({"tagBusiness"})
    public TagController(TagBusiness tagBusiness) {
        this.tagBusiness = tagBusiness;
    }

    @GetMapping("{id}")
    public Tag findById(@PathVariable UUID id) {
        return tagBusiness.findOne(id);
    }

    @GetMapping
    public List<Tag> findAll() {
        return tagBusiness.findAll();
    }

    @PostMapping
    public Set<Tag> findByNameLike(@RequestParam String name) {
        return tagBusiness.findByNameLike(name);
    }
}
