package lan.dk.podcastserver.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Created by kevin on 07/06/2014.
 */

@Entity
@Getter @Setter
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tag {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(unique = true)
    private String name;

    @JsonIgnore
    @ManyToMany(mappedBy = "tags")
    private Set<Podcast> podcasts = new HashSet<>();

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
