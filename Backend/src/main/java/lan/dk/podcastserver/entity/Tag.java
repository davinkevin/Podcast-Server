package lan.dk.podcastserver.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.builder.EqualsBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.UUID;

/**
 * Created by kevin on 07/06/2014.
 */
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter @Setter
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tag {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(unique = true)
    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof Tag)) return false;

        Tag tag = (Tag) o;

        return new EqualsBuilder()
                .append(id, tag.id)
                .append(name, tag.name)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
