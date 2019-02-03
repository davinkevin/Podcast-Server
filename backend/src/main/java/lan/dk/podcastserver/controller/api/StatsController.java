package lan.dk.podcastserver.controller.api;

import com.github.davinkevin.podcastserver.business.stats.StatsBusiness;
import com.github.davinkevin.podcastserver.business.stats.StatsPodcastType;
import io.vavr.collection.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by kevin on 28/04/15 for Podcast-Server
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsBusiness itemStatsBusiness;

    @java.beans.ConstructorProperties({"itemStatsBusiness"})
    public StatsController(StatsBusiness itemStatsBusiness) {
        this.itemStatsBusiness = itemStatsBusiness;
    }

    @PostMapping("byDownloadDate")
    public List<StatsPodcastType> byDownloadDate(@RequestBody Integer numberOfMonth) {
        return itemStatsBusiness.allStatsByTypeAndDownloadDate(numberOfMonth);
    }

    @PostMapping("byCreationDate")
    public List<StatsPodcastType> byCreationDate(@RequestBody Integer numberOfMonth) {
        return itemStatsBusiness.allStatsByTypeAndCreationDate(numberOfMonth);
    }

    @PostMapping("byPubDate")
    public List<StatsPodcastType> byPubDate(@RequestBody Integer numberOfMonth) {
        return itemStatsBusiness.allStatsByTypeAndPubDate(numberOfMonth);
    }
}
