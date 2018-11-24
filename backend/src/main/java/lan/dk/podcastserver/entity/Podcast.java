package lan.dk.podcastserver.entity;


import com.fasterxml.jackson.annotation.*;
import com.github.davinkevin.podcastserver.entity.Cover;
import com.github.davinkevin.podcastserver.entity.Tag;
import io.vavr.control.Option;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.slf4j.Logger;
import org.springframework.util.FileSystemUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static io.vavr.API.Option;
import static io.vavr.API.Try;

@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties(ignoreUnknown = true, value = {"signature", "items", "contains", "add", "lastUpdateToNow" })
public class Podcast implements Serializable {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(Podcast.class);
    public static Path rootFolder;
    public static final Podcast DEFAULT_PODCAST = new Podcast();
    public static final String COVER_PROXY_URL = "/api/podcasts/%s/cover.%s";

    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;

    @JsonView(PodcastListingView.class)
    private String title;

    @Column(length = 65535)
    @JsonView(PodcastDetailsView.class)
    private String url;
    private String signature;

    @JsonView(PodcastListingView.class)
    private String type;

    @JsonView(PodcastListingView.class)
    private ZonedDateTime lastUpdate;

    @OneToMany(mappedBy = "podcast", fetch = FetchType.LAZY, cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("PUB_DATE DESC")
    @Fetch(FetchMode.SUBSELECT)
    private Set<Item> items = new HashSet<>();

    @JsonView(PodcastListingView.class)
    @OneToOne(fetch = FetchType.EAGER, cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, orphanRemoval=true)
    private Cover cover;

    @Column(length = 65535 )
    @JsonView(PodcastDetailsView.class)
    private String description;

    @JsonView(PodcastDetailsView.class)
    private Boolean hasToBeDeleted;

    @JsonView(PodcastDetailsView.class)
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER) @Fetch(FetchMode.SUBSELECT)
    @JoinTable(name = "PODCAST_TAGS", joinColumns = @JoinColumn(name = "PODCASTS_ID"), inverseJoinColumns = @JoinColumn(name = "TAGS_ID"))
    private Set<Tag> tags = new HashSet<>();

    @java.beans.ConstructorProperties({"id", "title", "url", "signature", "type", "lastUpdate", "items", "cover", "description", "hasToBeDeleted", "tags"})
    @JsonIgnore
    public Podcast(UUID id, String title, String url, String signature, String type, ZonedDateTime lastUpdate, Set<Item> items, Cover cover, String description, Boolean hasToBeDeleted, Set<Tag> tags) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.signature = signature;
        this.type = type;
        this.lastUpdate = lastUpdate;
        this.items = items;
        this.cover = cover;
        this.description = description;
        this.hasToBeDeleted = hasToBeDeleted;
        this.tags = tags;
    }

    public Podcast() {
    }

    public static PodcastBuilder builder() {
        return new PodcastBuilder();
    }

    @Override
    public String toString() {
        return "Podcast{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", signature='" + signature + '\'' +
                ", type='" + type + '\'' +
                ", lastUpdate=" + lastUpdate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Podcast)) return false;

        Podcast podcast = (Podcast) o;

        return new EqualsBuilder()
                .append(id, podcast.id)
                .append(title, podcast.title)
                .append(url, podcast.url)
                .append(signature, podcast.signature)
                .append(lastUpdate, podcast.lastUpdate)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(title)
                .append(url)
                .append(signature)
                .append(lastUpdate)
                .toHashCode();
    }

    @PostRemove
    public void postRemove() {

        if (!this.getHasToBeDeleted()) return;

        Path folder = rootFolder.resolve(this.getTitle());

        Try(() -> FileSystemUtils.deleteRecursively(folder.toFile()))
            .onFailure(e -> log.error("Error during deletion of podcast of {}", this, e));
    }

    public Boolean contains(Item item) {
        return items.contains(item);
    }

    public Podcast add(Item item) {
        items.add(item.setPodcast(this));
        return this;
    }

    public Podcast lastUpdateToNow() {
        this.lastUpdate = ZonedDateTime.now();
        return this;
    }

    @JsonIgnore
    Option<Path> getCoverPath() {
        return Option(cover)
                .map(Cover::getUrl)
                .map(FilenameUtils::getExtension)
                .map(ext -> rootFolder.resolve(title).resolve("cover." + ext));
    }

    public UUID getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public String getUrl() {
        return this.url;
    }

    public String getSignature() {
        return this.signature;
    }

    public String getType() {
        return this.type;
    }

    public ZonedDateTime getLastUpdate() {
        return this.lastUpdate;
    }

    public Set<Item> getItems() {
        return this.items;
    }

    public Cover getCover() {
        return this.cover;
    }

    public String getDescription() {
        return this.description;
    }

    public Boolean getHasToBeDeleted() {
        return this.hasToBeDeleted;
    }

    public Set<Tag> getTags() {
        return this.tags;
    }

    public Podcast setId(UUID id) {
        this.id = id;
        return this;
    }

    public Podcast setTitle(String title) {
        this.title = title;
        return this;
    }

    public Podcast setUrl(String url) {
        this.url = url;
        return this;
    }

    public Podcast setSignature(String signature) {
        this.signature = signature;
        return this;
    }

    public Podcast setType(String type) {
        this.type = type;
        return this;
    }

    public Podcast setLastUpdate(ZonedDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
        return this;
    }

    public Podcast setItems(Set<Item> items) {
        this.items = items;
        return this;
    }

    public Podcast setCover(Cover cover) {
        this.cover = cover;
        return this;
    }

    public Podcast setDescription(String description) {
        this.description = description;
        return this;
    }

    public Podcast setHasToBeDeleted(Boolean hasToBeDeleted) {
        this.hasToBeDeleted = hasToBeDeleted;
        return this;
    }

    public Podcast setTags(Set<Tag> tags) {
        this.tags = tags;
        return this;
    }

    public interface PodcastListingView {}
    public interface PodcastDetailsView extends PodcastListingView{}

    public static class PodcastBuilder {
        private UUID id;
        private String title;
        private String url;
        private String signature;
        private String type;
        private ZonedDateTime lastUpdate;
        private Set<Item> items;
        private Cover cover;
        private String description;
        private Boolean hasToBeDeleted;
        private Set<Tag> tags;

        PodcastBuilder() {
        }

        public Podcast.PodcastBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public Podcast.PodcastBuilder title(String title) {
            this.title = title;
            return this;
        }

        public Podcast.PodcastBuilder url(String url) {
            this.url = url;
            return this;
        }

        public Podcast.PodcastBuilder signature(String signature) {
            this.signature = signature;
            return this;
        }

        public Podcast.PodcastBuilder type(String type) {
            this.type = type;
            return this;
        }

        public Podcast.PodcastBuilder lastUpdate(ZonedDateTime lastUpdate) {
            this.lastUpdate = lastUpdate;
            return this;
        }

        public Podcast.PodcastBuilder items(Set<Item> items) {
            this.items = items;
            return this;
        }

        public Podcast.PodcastBuilder cover(Cover cover) {
            this.cover = cover;
            return this;
        }

        public Podcast.PodcastBuilder description(String description) {
            this.description = description;
            return this;
        }

        public Podcast.PodcastBuilder hasToBeDeleted(Boolean hasToBeDeleted) {
            this.hasToBeDeleted = hasToBeDeleted;
            return this;
        }

        public Podcast.PodcastBuilder tags(Set<Tag> tags) {
            this.tags = tags;
            return this;
        }

        public Podcast build() {
            return new Podcast(id, title, url, signature, type, lastUpdate, items, cover, description, hasToBeDeleted, tags);
        }

        public String toString() {
            return "Podcast.PodcastBuilder(id=" + this.id + ", title=" + this.title + ", url=" + this.url + ", signature=" + this.signature + ", type=" + this.type + ", lastUpdate=" + this.lastUpdate + ", items=" + this.items + ", cover=" + this.cover + ", description=" + this.description + ", hasToBeDeleted=" + this.hasToBeDeleted + ", tags=" + this.tags + ")";
        }
    }
}
