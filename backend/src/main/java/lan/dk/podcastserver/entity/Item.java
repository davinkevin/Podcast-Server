package lan.dk.podcastserver.entity;

import com.fasterxml.jackson.annotation.*;
import com.github.davinkevin.podcastserver.entity.Cover;
import com.github.davinkevin.podcastserver.entity.Status;
import com.github.davinkevin.podcastserver.manager.worker.upload.UploadUpdater;
import io.vavr.control.Option;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.slf4j.Logger;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.*;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static io.vavr.API.Option;
import static io.vavr.API.Try;
import static java.util.Objects.nonNull;

@Entity
@Indexed
@Table(name = "item", uniqueConstraints = @UniqueConstraint(columnNames={"podcast_id", "url"}))
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties(ignoreUnknown = true, value = { "numberOfTry", "localUri", "addATry", "deleteDownloadedFile", "localPath", "proxyURLWithoutExtention", "extention", "hasValidURL", "reset", "coverPath" })
@EntityListeners(AuditingEntityListener.class)
public class Item {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(Item.class);
    public  static Path rootFolder;
    public  static final Item DEFAULT_ITEM = new Item();
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

    @NotNull @Field
    @Size(min = 1, max = 254)
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

    @JsonIgnore
    private Integer numberOfFail = 0;

    @JsonView(ItemDetailsView.class)
    private ZonedDateTime downloadDate;

    @CreatedDate
    private ZonedDateTime creationDate;

    @JsonIgnore
    @ManyToMany(mappedBy = "items", cascade = CascadeType.REFRESH)
    private java.util.Set<WatchList> watchLists = new HashSet<>();

    @java.beans.ConstructorProperties({"id", "cover", "podcast", "title", "url", "pubDate", "description", "mimeType", "length", "fileName", "status", "progression", "numberOfFail", "downloadDate", "creationDate", "watchLists"})
    @JsonIgnore
    private Item(UUID id, Cover cover, Podcast podcast, @NotNull @Size(min = 1, max = 254) String title, String url, ZonedDateTime pubDate, String description, String mimeType, Long length, String fileName, Status status, Integer progression, Integer numberOfFail, ZonedDateTime downloadDate, ZonedDateTime creationDate, Set<WatchList> watchLists) {
        this.id = id;
        this.cover = cover;
        this.podcast = podcast;
        this.title = title;
        this.url = url;
        this.pubDate = pubDate;
        this.description = description;
        this.mimeType = mimeType;
        this.length = length;
        this.fileName = fileName;
        this.status = status;
        this.progression = progression;
        this.numberOfFail = numberOfFail;
        this.downloadDate = downloadDate;
        this.creationDate = creationDate;
        this.watchLists = watchLists;
    }

    public Item() {
    }

    public static ItemBuilder builder() {
        return new ItemBuilder();
    }

    public String getLocalUri() {
        return (fileName == null) ? null : getLocalPath().toString();
    }

    public Item setLocalUri(String localUri) {
        fileName = FilenameUtils.getName(localUri);
        return this;
    }

    public Item addATry() {
        this.numberOfFail++;
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
                ", numberOfTry=" + numberOfFail +
                '}';
    }

    @Transient @JsonProperty("proxyURL") @JsonView(ItemSearchListView.class)
    public String getProxyURL() {
        // /api/podcasts/%s/items/%s/%s%s
        return this.getProxyURLWithoutExtention() + getExtension();
    }

    public String getProxyURLWithoutExtention() {
        return UriComponentsBuilder.fromPath("/")
                .pathSegment(
                        "api",
                        "podcasts",
                        String.valueOf(podcast.getId()),
                        "items",
                        String.valueOf(id),
                        Option(title).map(s -> s.replaceAll("[^a-zA-Z0-9.-]", "_")).getOrElse("null")
                )
                .build(true)
                .toString();
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

        if (!podcast.getHasToBeDeleted()) {
            return;
        }

        if (Objects.nonNull(this.getCover()) && !this.getCover().equals(podcast.getCover())) {
            getCoverPath().toTry()
                    .mapTry(Files::deleteIfExists)
                    .onFailure(e -> log.error("Error during deletion of cover of {}", this, e));
        }

        if (isDownloaded()) {
            deleteFile();
        }
    }

    private void deleteFile() {
        Try(() -> Files.deleteIfExists(getLocalPath())).onFailure(e -> log.error("Error during deletion of {}", this, e));
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

    public Option<Path> getCoverPath() {
        return Option(cover)
                .map(Cover::getUrl)
                .filter(StringUtils::isNotEmpty)
                .map(FilenameUtils::getExtension)
                .map(ext -> getPodcastPath().resolve(id + "." + ext))
                .orElse(() -> podcast.getCoverPath());
    }

    private Path getPodcastPath() {
        return rootFolder.resolve(podcast.getTitle());
    }


    private String getExtension() {
        return Option(fileName)
                .map(FilenameUtils::getExtension)
                .map(ext -> "."+ext)
                .getOrElse("");
    }

    @JsonProperty("cover") @JsonView(ItemSearchListView.class)
    public Cover getCoverOfItemOrPodcast() {
        return Option(cover)
                .map(c -> String.format(COVER_PROXY_URL, podcast.getId(), id, FilenameUtils.getExtension(c.getUrl())))
                .map(url -> {
                    Cover c = new Cover();
                    c.setUrl(url);
                    c.setHeight(this.cover.getHeight());
                    c.setWidth(this.cover.getWidth());
                    c.setId(this.cover.getId());
                    return c;
                })
                .getOrElse(() -> podcast.getCover());
    }

    @JsonProperty("podcastId") @JsonView(ItemSearchListView.class)
    public UUID getPodcastId() { return Option(podcast).map(Podcast::getId).getOrElse(() -> null);}
        
    @AssertTrue
    public boolean hasValidURL() {
        return (!StringUtils.isEmpty(this.url)) || Objects.equals(UploadUpdater.Companion.getTYPE().getKey(), podcast.getType());
    }

    public Item reset() {
        checkAndDelete();
        setStatus(Status.NOT_DOWNLOADED);
        downloadDate = null;
        fileName = null;
        numberOfFail = 0;
        return this;
    }

    public UUID getId() {
        return this.id;
    }

    public Cover getCover() {
        return this.cover;
    }

    public Podcast getPodcast() {
        return this.podcast;
    }

    public @NotNull @Size(min = 1, max = 254) String getTitle() {
        return this.title;
    }

    public String getUrl() {
        return this.url;
    }

    public ZonedDateTime getPubDate() {
        return this.pubDate;
    }

    public String getDescription() {
        return this.description;
    }

    public String getMimeType() {
        return this.mimeType;
    }

    public Long getLength() {
        return this.length;
    }

    public String getFileName() {
        return this.fileName;
    }

    public Status getStatus() {
        return this.status;
    }

    public Integer getProgression() {
        return this.progression;
    }

    public Integer getNumberOfFail() {
        return this.numberOfFail;
    }

    public ZonedDateTime getDownloadDate() {
        return this.downloadDate;
    }

    public ZonedDateTime getCreationDate() {
        return this.creationDate;
    }

    public Set<WatchList> getWatchLists() {
        return this.watchLists;
    }

    public Item setId(UUID id) {
        this.id = id;
        return this;
    }

    public Item setCover(Cover cover) {
        this.cover = cover;
        return this;
    }

    public Item setPodcast(Podcast podcast) {
        this.podcast = podcast;
        return this;
    }

    public Item setTitle(@NotNull @Size(min = 1, max = 254) String title) {
        this.title = title;
        return this;
    }

    public Item setUrl(String url) {
        this.url = url;
        return this;
    }

    public Item setPubDate(ZonedDateTime pubDate) {
        this.pubDate = pubDate;
        return this;
    }

    public Item setDescription(String description) {
        this.description = description;
        return this;
    }

    public Item setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public Item setLength(Long length) {
        this.length = length;
        return this;
    }

    public Item setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public Item setStatus(Status status) {
        this.status = status;
        return this;
    }

    public Item setProgression(Integer progression) {
        this.progression = progression;
        return this;
    }

    public Item setNumberOfFail(Integer numberOfFail) {
        this.numberOfFail = numberOfFail;
        return this;
    }

    public Item setDownloadDate(ZonedDateTime downloadDate) {
        this.downloadDate = downloadDate;
        return this;
    }

    public Item setCreationDate(ZonedDateTime creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public Item setWatchLists(Set<WatchList> watchLists) {
        this.watchLists = watchLists;
        return this;
    }

    public interface ItemSearchListView {}
    public interface ItemPodcastListView extends ItemSearchListView {}
    public interface ItemDetailsView extends ItemPodcastListView {}

    public static class ItemBuilder {
        private UUID id;
        private Cover cover;
        private Podcast podcast;
        private @NotNull @Size(min = 1, max = 254) String title;
        private String url;
        private ZonedDateTime pubDate;
        private String description;
        private String mimeType;
        private Long length;
        private String fileName;
        private Status status;
        private Integer progression;
        private Integer numberOfFail;
        private ZonedDateTime downloadDate;
        private ZonedDateTime creationDate;
        private Set<WatchList> watchLists;

        ItemBuilder() {
        }

        public ItemBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public ItemBuilder cover(Cover cover) {
            this.cover = cover;
            return this;
        }

        public ItemBuilder podcast(Podcast podcast) {
            this.podcast = podcast;
            return this;
        }

        public ItemBuilder title(@NotNull @Size(min = 1, max = 254) String title) {
            this.title = title;
            return this;
        }

        public ItemBuilder url(String url) {
            this.url = url;
            return this;
        }

        public ItemBuilder pubDate(ZonedDateTime pubDate) {
            this.pubDate = pubDate;
            return this;
        }

        public ItemBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ItemBuilder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public ItemBuilder length(Long length) {
            this.length = length;
            return this;
        }

        public ItemBuilder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public ItemBuilder status(Status status) {
            this.status = status;
            return this;
        }

        public ItemBuilder progression(Integer progression) {
            this.progression = progression;
            return this;
        }

        public ItemBuilder numberOfFail(Integer numberOfFail) {
            this.numberOfFail = numberOfFail;
            return this;
        }

        public ItemBuilder downloadDate(ZonedDateTime downloadDate) {
            this.downloadDate = downloadDate;
            return this;
        }

        public ItemBuilder creationDate(ZonedDateTime creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public ItemBuilder watchLists(Set<WatchList> watchLists) {
            this.watchLists = watchLists;
            return this;
        }

        public Item build() {
            return new Item(id, cover, podcast, title, url, pubDate, description, mimeType, length, fileName, status, progression, numberOfFail, downloadDate, creationDate, watchLists);
        }

        public String toString() {
            return "Item.ItemBuilder(id=" + this.id + ", cover=" + this.cover + ", podcast=" + this.podcast + ", title=" + this.title + ", url=" + this.url + ", pubDate=" + this.pubDate + ", description=" + this.description + ", mimeType=" + this.mimeType + ", length=" + this.length + ", fileName=" + this.fileName + ", status=" + this.status + ", progression=" + this.progression + ", numberOfFail=" + this.numberOfFail + ", downloadDate=" + this.downloadDate + ", creationDate=" + this.creationDate + ", watchLists=" + this.watchLists + ")";
        }
    }
}
