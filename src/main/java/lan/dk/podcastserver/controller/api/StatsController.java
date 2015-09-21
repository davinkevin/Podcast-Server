package lan.dk.podcastserver.controller.api;

import lan.dk.podcastserver.business.stats.StatsBusiness;
import lan.dk.podcastserver.utils.facade.stats.StatsPodcastType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by kevin on 28/04/15 for Podcast-Server
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    final StatsBusiness itemStatsBusiness;

    @Autowired StatsController(StatsBusiness itemStatsBusiness) {
        this.itemStatsBusiness = itemStatsBusiness;
    }

    @RequestMapping(value="byType", method = RequestMethod.POST)
    public List<StatsPodcastType> statsByType(@RequestBody Integer numberOfMonth) {
        return itemStatsBusiness.allStatsByType(numberOfMonth);
    }

}
