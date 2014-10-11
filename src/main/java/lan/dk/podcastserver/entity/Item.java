package lan.dk.podcastserver.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.*;
import javax.validation.constraints.AssertTrue;
import java.io.File;
import java.io.Serializable;
import java.time.ZonedDateTime;


@Table(name = "item")
@Entity
@Indexed
@JsonIgnoreProperties(ignoreUnknown = true)
public class Item implements Serializable {

    private static final String STATUS_NOT_DOWNLOADED = "Not Downloaded";
    private static final String PROXY_URL = "/api/podcast/%s/items/%s/download";

    private Integer id;
    private String title;
    private String url;

    private ZonedDateTime pubdate;
    private String description;
    private String mimeType;
    private Long length;
    private Cover cover;

    /* Value for the Download */
    private String localUrl;
    private String localUri;
    private String status = STATUS_NOT_DOWNLOADED;
    private Integer progression = 0;
    private ZonedDateTime downloaddate;
    private Podcast podcast;
    private Integer numberOfTry = 0;

    public Item() {
    }
/*

    public Item(String title, String url, Timestamp pubdate, Podcast podcast) {
        this.title = title;
        this.url = url;
        this.pubdate = pubdate;
        this.podcast = podcast;
    }

    public Item(String title, String url, Timestamp pubdate) {

        this.title = title;
        this.url = url;
        this.pubdate = pubdate;
    }
*/

    @Column(name = "mimetype")
    @Basic
    public String getMimeType() {
        return mimeType;
    }

    public Item setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    @Column(name = "length")
    @Basic
    public Long getLength() {
        return length;
    }

    public Item setLength(Long length) {
        this.length = length;
        return this;
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

    @Column(name = "title")
    @Basic @Field
    public String getTitle() {
        return title;
    }

    public Item setTitle(String title) {
        this.title = title;
        return this;
    }

    @Column(name = "url", length = 65535, unique = true)
    @Basic
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

    @Column(name = "local_url")
    @Basic
    public String getLocalUrl() {
        return localUrl;
    }

    public Item setLocalUrl(String localUrl) {
        this.localUrl = localUrl;
        return this;
    }

    @Column(name = "status")
    @Basic
    public String getStatus() {
        return status;
    }

    public Item setStatus(String status) {
        this.status = status;
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
    public ZonedDateTime getDownloaddate() {
        return downloaddate;
    }


    //public Timestamp getDownloaddate() {return new Timestamp(new Date().getTime());}

    public Item setDownloaddate(ZonedDateTime downloaddate) {
        this.downloaddate = downloaddate;
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

    @Column(name = "local_uri")
    @Basic
    public String getLocalUri() {
        return localUri;
    }

    public Item setLocalUri(String localUri) {
        this.localUri = localUri;
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

        if (url != null && item.url != null)
            return url.equals(item.url);

        return localUrl != null && item.localUrl != null && localUrl.equals(item.localUrl);

    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
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
                ", localUrl='" + localUrl + '\'' +
                ", localUri='" + localUri + '\'' +
                ", status='" + status + '\'' +
                ", progression=" + progression +
                ", downloaddate=" + downloaddate +
                ", podcast=" + podcast +
                ", numberOfTry=" + numberOfTry +
                '}';
    }

    /* Helpers */
    @Transient
    @JsonProperty("proxyURL")
    public String getProxyURL() {
        return String.format(PROXY_URL, podcast.getId(), id);
    }

    @Transient
    @JsonIgnore
    public String getFileURI() {
        return File.separator + this.podcast.getTitle() + File.separator + FilenameUtils.getName(this.localUrl);
    }

    //* CallBack Method JPA *//
    @PreRemove
    public void preRemove() {
        if (this.podcast.getHasToBeDeleted() && !StringUtils.isEmpty(this.getLocalUri())) {
            File fileToDelete = new File(this.getLocalUri());
            if (fileToDelete.exists()) {
                fileToDelete.delete();
            }
        }
    }

    @Transient
    @JsonProperty("cover")
    public Cover getCoverOfItemOrPodcast() {
        return (this.cover == null) ? podcast.getCover() : this.cover;
    }

    @JsonProperty("podcastId")
    @Transient
    public Integer getPodcastId() { return (podcast == null) ? null : podcast.getId();}

    @Transient
    @JsonIgnore
    @AssertTrue
    public boolean hasValidURL() {
        return (!StringUtils.isEmpty(this.url)) || "send".equals(this.podcast.getType());
    }

    @Transient @JsonIgnore
    public Item reset() {
        preRemove();
        setStatus(STATUS_NOT_DOWNLOADED);
        setLocalUrl(null);
        setLocalUri(null);
        //setDownloaddate(null);
        downloaddate = null;
        return this;
    }
}
