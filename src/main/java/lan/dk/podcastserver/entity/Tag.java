package lan.dk.podcastserver.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.builder.EqualsBuilder;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by kevin on 07/06/2014.
 */

@Table(name = "tag")
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tag {

    private Integer id;
    private String name;
    private Set<Podcast> podcasts = new HashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    public Integer getId() {
        return id;
    }

    public Tag setId(Integer id) {
        this.id = id;
        return this;
    }

    @Basic
    @Column(name = "name", unique = true)
    public String getName() {
        return name;
    }

    public Tag setName(String name) {
        this.name = name;
        return this;
    }

    @ManyToMany(mappedBy = "tags")
    @JsonIgnore
    public Set<Podcast> getPodcasts() {
        return podcasts;
    }

    public Tag setPodcasts(Set<Podcast> podcasts) {
        this.podcasts = podcasts;
        return this;
    }

    public Tag addPodcast(Podcast podcast) {
        this.podcasts.add(podcast);
        return this;
    }

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
