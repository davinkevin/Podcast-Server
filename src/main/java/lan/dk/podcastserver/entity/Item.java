package lan.dk.podcastserver.entity;

import com.fasterxml.jackson.annotation.*;
import com.google.common.collect.Sets;
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
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.*;
import javax.validation.constraints.AssertTrue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Set;

import static java.util.Objects.isNull;


@Entity
@Indexed
@Slf4j
@Builder
@Getter @Setter
@Table(name = "item")
@Accessors(chain = true)
@NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true, value = { "numberOfTry", "localUri", "addATry", "deleteDownloadedFile", "localPath", "proxyURLWithoutExtention", "extention", "hasValidURL", "reset" })
@EntityListeners(AuditingEntityListener.class)
public class Item {

    public  static Path rootFolder;
    public  static String fileContainer;
    public  static final Item DEFAULT_ITEM = new Item();
    private static final String PROXY_URL = "/api/podcast/%s/items/%s/download%s";

    @Id
    @DocumentId
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @OneToOne(fetch = FetchType.EAGER, cascade=CascadeType.ALL, orphanRemoval=true)
    private Cover cover;

    @ManyToOne(cascade={CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JsonBackReference("podcast-item")
    private Podcast podcast;

    @Field @Boost(2.0F)
    @JsonView(ItemSearchListView.class)
    private String title;

    @Column(length = 65535, unique = true)
    @JsonView(ItemSearchListView.class)
    private String url;

    @JsonView(ItemPodcastListView.class)
    private ZonedDateTime pubdate;

    @Field
    @Column(length = 65535)
    @JsonView(ItemPodcastListView.class)
    private String description;

    @Column(name = "mimetype")
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

        Item item = (Item) o;

        if (id != null && item.id != null)
            return id.equals(item.id);

        if (url != null && item.url != null) {
            return url.equals(item.url) || FilenameUtils.getName(item.url).equals(FilenameUtils.getName(url));
        }

        return StringUtils.equals(getLocalUrl(), item.getLocalUrl());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(url)
                .append((pubdate != null) ? pubdate.toInstant() : null)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "Item{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", pubdate=" + pubdate +
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

    /* Helpers */
    @Transient @JsonView(ItemSearchListView.class)
    public String getLocalUrl() {
        return (fileName == null) ? null : UriComponentsBuilder.fromHttpUrl(fileContainer)
                .pathSegment(podcast.getTitle(), fileName)
                .build()
                .toString();
    }
    
    @Transient @JsonProperty("proxyURL") @JsonView(ItemSearchListView.class)
    public String getProxyURL() {
        return String.format(PROXY_URL, podcast.getId(), id, getExtention());
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
        if (podcast.getHasToBeDeleted() && isDownloaded()) {
            deleteFile();
        }
    }

    private void deleteFile() {
        try {
            Files.deleteIfExists(getLocalPath());
        } catch (IOException e) {
            log.error("Error during deletion of {}", this, e);
        }
    }

    @Transient @JsonIgnore
    public Item deleteDownloadedFile() {
        deleteFile();
        status = Status.DELETED;
        fileName = null;
        return this;
    }

    public Path getLocalPath() {
        return rootFolder.resolve(podcast.getTitle()).resolve(fileName);
    }

    public String getProxyURLWithoutExtention() {
        return String.format(PROXY_URL, podcast.getId(), id, "");
    }

    private String getExtention() {
        String ext = FilenameUtils.getExtension(fileName);
        return (ext == null) ? "" : "."+ext;
    }
    
    @JsonProperty("cover") @JsonView(ItemSearchListView.class)
    public Cover getCoverOfItemOrPodcast() {
        return isNull(this.cover) ? podcast.getCover() : this.cover;
    }

    @JsonProperty("podcastId") @JsonView(ItemSearchListView.class)
    public Integer getPodcastId() { return isNull(podcast) ? null : podcast.getId();}
        
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
