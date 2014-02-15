package lan.dk.podcastserver.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import javax.validation.constraints.AssertTrue;
import java.io.File;
import java.io.Serializable;
import java.sql.Timestamp;


@Table(name = "item")
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class Item implements Serializable {

    private int id;
    private String title;
    private String url;
    private Timestamp pubdate;
    private String description;
    private String mimeType;
    private long length;
    private Cover cover;

    /* Value for the Download */
    private String localUrl;
    private String localUri;
    private String status = "Not Downloaded";
    private int progression;
    private Timestamp downloaddate;
    private Podcast podcast;

    public Item() {
    }

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
    public long getLength() {
        return length;
    }

    public Item setLength(long length) {
        this.length = length;
        return this;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    public int getId() {
        return id;
    }

    public Item setId(int id) {
        this.id = id;
        return this;
    }

    @Column(name = "title")
    @Basic
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
    @Basic
    public Timestamp getPubdate() {
        return pubdate;
    }

    public Item setPubdate(Timestamp pubdate) {
        this.pubdate = pubdate;
        return this;
    }

    @ManyToOne(cascade={CascadeType.MERGE})
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
    public int getProgression() {
        return progression;
    }

    public Item setProgression(int progression) {
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
    @Basic
    public Timestamp getDownloaddate() {
        return downloaddate;
    }

    public Item setDownloaddate(Timestamp downloaddate) {
        this.downloaddate = downloaddate;
        return this;
    }

    @Column(name = "description", length = 65535)
    @Basic
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;

        Item item = (Item) o;

        /*if (url != null ? !url.equals(item.url) : item.url != null) return false;
        if (localUrl != null ? !localUrl.equals(item.localUrl) : item.localUrl != null) return false;
        if (!podcast.equals(item.podcast)) return false;
        if (title != null ? !title.equals(item.title) : item.title != null) return false;*/
        if (localUrl != null && item.localUrl != null && localUrl.equals(item.localUrl))
            return true;
        if (url != null && item.url != null && url.equals(item.url))
            return true;

        return false;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (localUrl != null ? localUrl.hashCode() : 0);
        //result = 31 * result + podcast.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Item{" +
                "status='" + status + '\'' +
                ", localUrl='" + localUrl + '\'' +
                ", pubdate=" + pubdate +
                ", url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", id=" + id +
                '}';
    }

    /* Helpers */
    @Transient
    @JsonProperty("proxyURL")
    public String getProxyURL() {
        return "/api/item/" + this.id + "/download";
    }

    @Transient
    @JsonIgnore
    public String getFileURI() {
        return File.separator + this.podcast.getTitle() + File.separator + FilenameUtils.getName(this.localUrl);
    }

    //* CallBack Method JPA *//
    @PreRemove
    public void preRemove() {
        if (this.getLocalUri() != null && !this.getLocalUri().equals("")) {
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

    @Transient
    @JsonIgnore
    @AssertTrue
    public boolean hasValidURL() {
        return (!StringUtils.isEmpty(this.url)) || (this.podcast.getType() != null && this.podcast.getType().equals("send"));
    }
}
