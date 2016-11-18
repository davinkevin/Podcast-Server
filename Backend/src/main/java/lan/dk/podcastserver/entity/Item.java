package lan.dk.podcastserver.entity;

import com.fasterxml.jackson.annotation.*;
import com.google.common.collect.Sets;
import javaslang.control.Option;
import javaslang.control.Try;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.AssertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@Entity
@Indexed
@Slf4j
@Builder
@Getter @Setter
@Table(name = "item", uniqueConstraints = @UniqueConstraint(columnNames={"podcast_id", "url"}))
@Accessors(chain = true)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@NoArgsConstructor @AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true, value = { "numberOfTry", "localUri", "addATry", "deleteDownloadedFile", "localPath", "proxyURLWithoutExtention", "extention", "hasValidURL", "reset", "coverPath" })
@EntityListeners(AuditingEntityListener.class)
public class Item {

    public  static Path rootFolder;
    public  static final Item DEFAULT_ITEM = new Item();
    private static final String PROXY_URL = "/api/podcasts/%s/items/%s/download%s";
    private static final String COVER_PROXY_URL = "/api/podcasts/%s/items/%s/cover.%s";

    @Id
    @DocumentId
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;

    @OneToOne(fetch = FetchType.EAGER, cascade=CascadeType.ALL, orphanRemoval=true)
    private Cover cover;

    @ManyToOne(cascade={CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JsonBackReference("podcast-item")
    private Podcast podcast;

    @Field @Boost(2.0F)
    @JsonView(ItemSearchListView.class)
    private String title;

    @Column(length = 65535)
    @JsonView(ItemSearchListView.class)
    private String url;

    @JsonView(ItemPodcastListView.class)
    private ZonedDateTime pubDate;

    @Field
    @Column(length = 2147483647)
    @JsonView(ItemPodcastListView.class)
    private String description;

    @JsonView(ItemSearchListView.class)
    private String mimeType;

    @JsonView(ItemDetailsView.class)
    private Long length;

    @JsonView(ItemDetailsView.class)
    private String fileName;

    /* Value for the Download */
    @Enumerated(EnumType.STRING)
    @JsonView(ItemSearchListView.class)
    private Status status = Status.NOT_DOWNLOADED;

    @Transient
    @JsonView(ItemDetailsView.class)
    private Integer progression = 0;

    @Transient
    private Integer numberOfTry = 0;

    @JsonView(ItemDetailsView.class)
    private ZonedDateTime downloadDate;

    @CreatedDate
    private ZonedDateTime creationDate;

    @JsonIgnore
    @ManyToMany(mappedBy = "items", cascade = CascadeType.REFRESH)
    private Set<WatchList> watchLists = Sets.newHashSet();


    public String getLocalUri() {
        return (fileName == null) ? null : getLocalPath().toString();
    }

    public Item setLocalUri(String localUri) {
        fileName = FilenameUtils.getName(localUri);
        return this;
    }

    public Item addATry() {
        this.numberOfTry++;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;
        if (this == DEFAULT_ITEM && o != DEFAULT_ITEM || this != DEFAULT_ITEM && o == DEFAULT_ITEM) return false;

        Item item = Item.class.cast(o);

        if (nonNull(id) && nonNull(item.id))
            return id.equals(item.id);

        if (nonNull(url) && nonNull(item.url)) {
            return url.equals(item.url);
        }

        return StringUtils.equals(getProxyURL(), item.getProxyURL());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(url)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "Item{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", pubDate=" + pubDate +
                ", description='" + description + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", length=" + length +
                ", status='" + status + '\'' +
                ", progression=" + progression +
                ", downloaddate=" + downloadDate +
                ", podcast=" + podcast +
                ", numberOfTry=" + numberOfTry +
                '}';
    }

    @Transient @JsonProperty("proxyURL") @JsonView(ItemSearchListView.class)
    public String getProxyURL() {
        return String.format(PROXY_URL, podcast.getId(), id, getExtension());
    }

    @Transient @JsonProperty("isDownloaded") @JsonView(ItemSearchListView.class)
    public Boolean isDownloaded() {
        return StringUtils.isNotEmpty(fileName);
    }
    
    //* CallBack Method JPA *//
    @PreRemove
    public void preRemove() {
        checkAndDelete();
        watchLists.forEach(watchList -> watchList.remove(this));
    }

    private void checkAndDelete() {

        if (!podcast.getHasToBeDeleted())
            return;

        Try .of(() -> Files.deleteIfExists(getCoverPath()))
            .onFailure(e -> log.error("Error during deletion of cover of {}", this, e));

        if (isDownloaded()) {
            deleteFile();
        }
    }

    private void deleteFile() {
        Try.of(() -> Files.deleteIfExists(getLocalPath())).onFailure(e -> log.error("Error during deletion of {}", this, e));
    }

    @Transient @JsonIgnore
    public Item deleteDownloadedFile() {
        deleteFile();
        status = Status.DELETED;
        fileName = null;
        return this;
    }

    public Path getLocalPath() {
        return getPodcastPath().resolve(fileName);
    }

    public Path getCoverPath() {
        String url = isNull(cover) ? "" : cover.getUrl();
        return getPodcastPath().resolve(id + "." + FilenameUtils.getExtension(url));
    }

    private Path getPodcastPath() {
        return rootFolder.resolve(podcast.getTitle());
    }

    public String getProxyURLWithoutExtention() {
        return String.format(PROXY_URL, podcast.getId(), id, "");
    }

    private String getExtension() {
        return Option.of(fileName)
                .map(FilenameUtils::getExtension)
                .map(ext -> "."+ext)
                .getOrElse("");
    }

    @JsonProperty("cover") @JsonView(ItemSearchListView.class)
    public Cover getCoverOfItemOrPodcast() {
        return isNull(this.cover)
                ? podcast.getCover()
                : this.cover.toBuilder().url(String.format(COVER_PROXY_URL, podcast.getId(), id, FilenameUtils.getExtension(this.cover.getUrl()))).build();
    }

    @JsonProperty("podcastId") @JsonView(ItemSearchListView.class)
    public UUID getPodcastId() { return isNull(podcast) ? null : podcast.getId();}
        
    @AssertTrue
    public boolean hasValidURL() {
        return (!StringUtils.isEmpty(this.url)) || "send".equals(this.podcast.getType());
    }

    public Item reset() {
        checkAndDelete();
        setStatus(Status.NOT_DOWNLOADED);
        downloadDate = null;
        fileName = null;
        return this;
    }

    public interface ItemSearchListView {}
    public interface ItemPodcastListView extends ItemSearchListView {}
    public interface ItemDetailsView extends ItemPodcastListView {}
}
