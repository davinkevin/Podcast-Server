package lan.dk.podcastserver.business.stats;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javaslang.collection.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by kevin on 28/04/15 for HackerRank problem
 */
@Getter
@AllArgsConstructor
public class StatsPodcastType {

    private final String type;
    private final Set<NumberOfItemByDateWrapper> values;

    @JsonIgnore
    public Boolean isEmpty() {
        return values.isEmpty();
    }
}
