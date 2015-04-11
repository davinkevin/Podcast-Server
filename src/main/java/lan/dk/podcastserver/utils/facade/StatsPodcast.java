package lan.dk.podcastserver.utils.facade;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.time.LocalDate;

/**
 * Created by kevin on 06/04/15.
 */
public class StatsPodcast {

    private LocalDate date;
    private Long numberOfItems;

    public StatsPodcast(LocalDate date, Long numberOfItems) {
        this.date = date;
        this.numberOfItems = numberOfItems;
    }

    public LocalDate getDate() {
        return date;
    }

    public Long getNumberOfItems() {
        return numberOfItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StatsPodcast)) return false;

        StatsPodcast that = (StatsPodcast) o;
        return new EqualsBuilder()
                .append(date, that.date)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(date)
                .toHashCode();
    }
}
