package lan.dk.podcastserver.controller.api;

import lan.dk.podcastserver.manager.worker.selector.UpdaterSelector;
import lan.dk.podcastserver.manager.worker.updater.AbstractUpdater;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Created by kevin on 12/05/15 for Podcast Server
 */
@RestController
@RequestMapping("/api/types")
@RequiredArgsConstructor
public class TypeController {

    private final UpdaterSelector updaterSelector;

    @Cacheable("types")
    @GetMapping
    public Set<AbstractUpdater.Type> types() {
        return updaterSelector.types();
    }
}
