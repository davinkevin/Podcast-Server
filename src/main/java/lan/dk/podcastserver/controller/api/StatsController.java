package lan.dk.podcastserver.controller.api;

import lan.dk.podcastserver.business.stats.StatsBusiness;
import lan.dk.podcastserver.utils.facade.stats.StatsPodcastType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * Created by kevin on 28/04/15 for HackerRank problem
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Resource
    StatsBusiness itemStatsBusiness;

    @RequestMapping(value="byType", method = RequestMethod.POST)
    public List<StatsPodcastType> statsByType(@RequestBody Integer numberOfMonth) {
        return itemStatsBusiness.allStatsByType(numberOfMonth);
    }

}
