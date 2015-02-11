package lan.dk.podcastserver.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.*;
import javax.validation.constraints.AssertTrue;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;


@Table(name = "item")
@Entity
@Indexed
@JsonIgnoreProperties(ignoreUnknown = true)
public class Item implements Serializable {

    public static Path rootFolder;
    public static String fileContainer;
    
    private static final String STATUS_NOT_DOWNLOADED = "Not Downloaded";
    private static final String PROXY_URL = "/api/podcast/%s/items/%s/download%s";
    
    private Integer id;
    private String title;
    private String url;

    private Podcast podcast;
    private ZonedDateTime pubdate;
    private String description;
    private String mimeType;
    private Long length;
    private Cover cover;
    private String fileName;

    /* Value for the Download */
    private String status = STATUS_NOT_DOWNLOADED;
    private Integer progression = 0;
    private ZonedDateTime downloadDate;
    private Integer numberOfTry = 0;

    public Item() {
    }

    @Id
    @DocumentId
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    public Integer getId() {
        return id;
    }

    public Item setId(Integer id) {
        this.id = id;
        return this;
    }
    
    @Basic @Column(name = "mimetype")
    public String getMimeType() {
        return mimeType;
    }

    public Item setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    @Basic @Column(name = "length")
    public Long getLength() {
        return length;
    }

    public Item setLength(Long length) {
        this.length = length;
        return this;
    }
   

    @Basic @Field @Column(name = "title")
    public String getTitle() {
        return title;
    }

    public Item setTitle(String title) {
        this.title = title;
        return this;
    }

    @Basic @Column(name = "url", length = 65535, unique = true)
    public String getUrl() {
        return url;
    }

    public Item setUrl(String url) {
        this.url = url;
        return this;
    }

    @Column(name = "pubdate")
    @Type(type = "org.jadira.usertype.dateandtime.threeten.PersistentZonedDateTime")
    public ZonedDateTime getPubdate() {
        return pubdate;
    }

    public Item setPubdate(ZonedDateTime pubdate) {
        this.pubdate = pubdate;
        return this;
    }

    @ManyToOne(cascade={CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinColumn(name = "podcast_id", referencedColumnName = "id")
    @JsonBackReference("podcast-item")
    public Podcast getPodcast() {
        return podcast;
    }

    public Item setPodcast(Podcast podcast) {
        this.podcast = podcast;
        return this;
    }

    @Basic
    @Column(name = "status")
    public String getStatus() {
        return status;
    }

    public Item setStatus(String status) {
        this.status = status;
        return this;
    }

    @Basic
    public String getFileName() {
        return fileName;
    }

    public Item setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    @Transient
    public Integer getProgression() {
        return progression;
    }

    public Item setProgression(Integer progression) {
        this.progression = progression;
        return this;
    }

    @OneToOne(fetch = FetchType.EAGER, cascade=CascadeType.ALL, orphanRemoval=true)
    @JoinColumn(name="cover_id")
    public Cover getCover() {
        return this.cover;
    }

    public Item setCover(Cover cover) {
        this.cover = cover;
        return this;
    }

    @Column(name = "downloadddate")
    @Type(type = "org.jadira.usertype.dateandtime.threeten.PersistentZonedDateTime")
    public ZonedDateTime getDownloadDate() {
        return downloadDate;
    }


    public Item setDownloadDate(ZonedDateTime downloaddate) {
        this.downloadDate = downloaddate;
        return this;
    }

    @Column(name = "description", length = 65535)
    @Basic @Field
    public String getDescription() {
        return description;
    }

    public Item setDescription(String description) {
        this.description = description;
        return this;
    }

    @Transient @JsonIgnore
    public String getLocalUri() {
        return (fileName == null) ? null : getLocalPath().toString();
    }

    public Item setLocalUri(String localUri) {
        fileName = FilenameUtils.getName(localUri);
        return this;
    }

    @JsonIgnore
    @Transient
    public Integer getNumberOfTry() {
        return numberOfTry;
    }

    @JsonIgnore
    @Transient
    public Item setNumberOfTry(Integer numberOfTry) {
        this.numberOfTry = numberOfTry;
        return this;
    }

    @JsonIgnore
    @Transient
    public Item addATry() {
        this.numberOfTry++;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;

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
                ", cover=" + cover +
                ", status='" + status + '\'' +
                ", progression=" + progression +
                ", downloaddate=" + downloadDate +
                ", podcast=" + podcast +
                ", numberOfTry=" + numberOfTry +
                '}';
    }

    /* Helpers */
    @Transient
    public String getLocalUrl() {
        return (fileName == null) ? null : UriComponentsBuilder.fromHttpUrl(fileContainer)
                .pathSegment(podcast.getTitle())
                .pathSegment(fileName)
                .build()
                .toString();
    }
    
    @Transient @JsonProperty("proxyURL")
    public String getProxyURL() {
        return String.format(PROXY_URL, podcast.getId(), id, getExtention());
    }

    @Transient @JsonProperty("isDownloaded")
    public Boolean isDownloaded() {
        return StringUtils.isNotEmpty(fileName);
    }
    
    //* CallBack Method JPA *//
    @PreRemove
    public void preRemove() {
        if (podcast.getHasToBeDeleted() && isDownloaded()) {
            try {
                Files.deleteIfExists(getLocalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Transient @JsonIgnore
    public Path getLocalPath() {
        return rootFolder.resolve(podcast.getTitle()).resolve(fileName);
    }

    @Transient @JsonIgnore
    public String getProxyURLWithoutExtention() {
        return String.format(PROXY_URL, podcast.getId(), id, "");
    }
    
    @Transient @JsonIgnore
    private String getExtention() {
        String ext = FilenameUtils.getExtension(fileName);
        return (ext == null) ? "" : "."+ext;
    }
    
    @Transient @JsonProperty("cover")
    public Cover getCoverOfItemOrPodcast() {
        return (this.cover == null) ? podcast.getCover() : this.cover;
    }

    @Transient @JsonProperty("podcastId")
    public Integer getPodcastId() { return (podcast == null) ? null : podcast.getId();}
        
    @Transient @JsonIgnore @AssertTrue
    public boolean hasValidURL() {
        return (!StringUtils.isEmpty(this.url)) || "send".equals(this.podcast.getType());
    }

    @Transient @JsonIgnore
    public Item reset() {
        preRemove();
        setStatus(STATUS_NOT_DOWNLOADED);
        downloadDate = null;
        fileName = null;
        return this;
    }
}
