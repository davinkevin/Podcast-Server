package lan.dk.podcastserver.utils.facade.stats;

import java.util.Set;

/**
 * Created by kevin on 28/04/15 for HackerRank problem
 */
public class StatsPodcastType {

    public final String type;
    public final Set<NumberOfItemByDateWrapper> values;

    public StatsPodcastType(String type, Set<NumberOfItemByDateWrapper> values) {
        this.type = type;
        this.values = values;
    }

    public String type() {
        return type;
    }

    public Set<NumberOfItemByDateWrapper> values() {
        return values;
    }
}
