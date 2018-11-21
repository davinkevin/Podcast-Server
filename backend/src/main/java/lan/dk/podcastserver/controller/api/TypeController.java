package lan.dk.podcastserver.controller.api;

import io.vavr.collection.Set;
import com.github.davinkevin.podcastserver.manager.selector.UpdaterSelector;
import lan.dk.podcastserver.manager.worker.Type;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by kevin on 12/05/15 for Podcast Server
 */
@RestController
@RequestMapping("/api/types")
@RequiredArgsConstructor
public class TypeController {

    private final UpdaterSelector updaterSelector;

    @GetMapping
    @Cacheable("types")
    public Set<Type> types() {
        return updaterSelector.types();
    }
}
