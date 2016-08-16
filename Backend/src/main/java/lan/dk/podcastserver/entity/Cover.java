package lan.dk.podcastserver.entity;

import lombok.*;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.UUID;

@Entity
@Builder(toBuilder = true)
@Setter @Getter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Cover {

    public static final Cover DEFAULT_COVER = new Cover();

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    private UUID id;
    private String url;
    private Integer width;
    private Integer height;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cover)) return false;

        Cover cover = (Cover) o;
        return new EqualsBuilder()
                .append(StringUtils.lowerCase(url), StringUtils.lowerCase(cover.url))
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(StringUtils.lowerCase(url))
                .toHashCode();
    }
}
