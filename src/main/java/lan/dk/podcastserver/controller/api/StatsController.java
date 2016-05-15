package lan.dk.podcastserver.controller.api;

import lan.dk.podcastserver.business.stats.StatsBusiness;
import lan.dk.podcastserver.utils.facade.stats.StatsPodcastType;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class StatsController {

    final StatsBusiness itemStatsBusiness;

    @RequestMapping(value="byDownloadDate", method = RequestMethod.POST)
    public List<StatsPodcastType> byDownloadDate(@RequestBody Integer numberOfMonth) {
        return itemStatsBusiness.allStatsByTypeAndDownloadDate(numberOfMonth);
    }

    @RequestMapping(value="byCreationDate", method = RequestMethod.POST)
    public List<StatsPodcastType> byCreationDate(@RequestBody Integer numberOfMonth) {
        return itemStatsBusiness.allStatsByTypeAndCreationDate(numberOfMonth);
    }

    @RequestMapping(value="byPubDate", method = RequestMethod.POST)
    public List<StatsPodcastType> byPubDate(@RequestBody Integer numberOfMonth) {
        return itemStatsBusiness.allStatsByTypeAndPubDate(numberOfMonth);
    }
}
