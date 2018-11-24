package lan.dk.podcastserver.controller.api;

import com.github.davinkevin.podcastserver.manager.selector.UpdaterSelector;
import com.github.davinkevin.podcastserver.manager.worker.Type;
import io.vavr.collection.Set;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by kevin on 12/05/15 for Podcast Server
 */
@RestController
@RequestMapping("/api/types")
public class TypeController {

    private final UpdaterSelector updaterSelector;

    @java.beans.ConstructorProperties({"updaterSelector"})
    public TypeController(UpdaterSelector updaterSelector) {
        this.updaterSelector = updaterSelector;
    }

    @GetMapping
    @Cacheable("types")
    public Set<Type> types() {
        return updaterSelector.types();
    }
}
