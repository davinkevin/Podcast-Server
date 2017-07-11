package lan.dk.podcastserver.business.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.time.LocalDate;

/**
 * Created by kevin on 06/04/15.
 */
@Getter
@AllArgsConstructor
public class NumberOfItemByDateWrapper {

    private LocalDate date;
    private Integer numberOfItems;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NumberOfItemByDateWrapper)) return false;

        NumberOfItemByDateWrapper that = (NumberOfItemByDateWrapper) o;
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
