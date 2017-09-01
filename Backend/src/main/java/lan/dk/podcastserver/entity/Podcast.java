package lan.dk.podcastserver.entity;


import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.util.FileSystemUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static io.vavr.API.Try;

@Slf4j
@Entity
@Builder
@Getter @Setter
@Accessors(chain = true)
@NoArgsConstructor @AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties(ignoreUnknown = true, value = {"signature", "items", "contains", "add", "lastUpdateToNow" })
public class Podcast implements Serializable {

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

    public interface PodcastListingView {}
    public interface PodcastDetailsView extends PodcastListingView{}

}
