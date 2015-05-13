package lan.dk.podcastserver.controller.api;

import lan.dk.podcastserver.manager.worker.updater.AbstractUpdater;
import lan.dk.podcastserver.service.WorkerService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Set;

/**
 * Created by kevin on 12/05/15 for Podcast Server
 */
@RestController
@RequestMapping("/api/types")
public class TypeController {

    @Resource WorkerService workerService;

    @RequestMapping(method = RequestMethod.GET)
    public Set<AbstractUpdater.Type> types() {
        return workerService.types();
    }
}
