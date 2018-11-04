package lan.dk.podcastserver.controller.api;

import io.vavr.collection.List;
import io.vavr.collection.Set;
import com.github.davinkevin.podcastserver.business.TagBusiness;
import lan.dk.podcastserver.entity.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Created by kevin on 07/06/2014 for Podcast Server
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tags")
public class TagController {

    private final TagBusiness tagBusiness;

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
