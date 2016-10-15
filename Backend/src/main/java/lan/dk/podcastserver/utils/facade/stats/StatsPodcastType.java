package lan.dk.podcastserver.utils.facade.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;

import java.util.Set;

/**
 * Created by kevin on 28/04/15 for HackerRank problem
 */
@AllArgsConstructor
public class StatsPodcastType {

    private final String type;
    private final Set<NumberOfItemByDateWrapper> values;

    @JsonProperty("type")
    public String type() {
        return type;
    }

    @JsonProperty("values")
    public Set<NumberOfItemByDateWrapper> values() {
        return values;
    }
}
