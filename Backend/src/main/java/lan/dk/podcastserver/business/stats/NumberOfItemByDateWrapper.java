package lan.dk.podcastserver.business.stats;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.time.LocalDate;

/**
 * Created by kevin on 06/04/15.
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode(of = {"date"})
public class NumberOfItemByDateWrapper {
    private LocalDate date;
    private Integer numberOfItems;
}
