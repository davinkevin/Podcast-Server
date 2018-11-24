package lan.dk.podcastserver.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tag {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(unique = true)
    private String name;

    @java.beans.ConstructorProperties({"id", "name"})
    private Tag(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public Tag() {
    }

    public static TagBuilder builder() {
        return new TagBuilder();
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

    public UUID getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Tag setId(UUID id) {
        this.id = id;
        return this;
    }

    public Tag setName(String name) {
        this.name = name;
        return this;
    }

    public static class TagBuilder {
        private UUID id;
        private String name;

        TagBuilder() {
        }

        public Tag.TagBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public Tag.TagBuilder name(String name) {
            this.name = name;
            return this;
        }

        public Tag build() {
            return new Tag(id, name);
        }

        public String toString() {
            return "Tag.TagBuilder(id=" + this.id + ", name=" + this.name + ")";
        }
    }
}
