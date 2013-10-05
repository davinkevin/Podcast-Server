package lan.dk.podcastserver.entity;

import org.apache.commons.io.FilenameUtils;
import org.codehaus.jackson.annotate.JsonBackReference;
import org.codehaus.jackson.annotate.JsonIgnore;

import javax.persistence.*;
import java.io.File;
import java.io.Serializable;
import java.sql.Timestamp;


@Table(name = "item", schema = "", catalog = "podcastserver")
@Entity
public class Item implements Serializable {

    private int id;
    private String title;
    private String url;
    private Timestamp pubdate;

    /* Value for the Download */
    private String localUrl;
    private String localUri;
    private String status = "Not Downloaded";
    private Cover cover;
    private int progression;

    private Timestamp downloaddate;
    private Podcast podcast;
    private String description;

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

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "title")
    @Basic
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Column(name = "url")
    @Basic
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Column(name = "pubdate")
    @Basic
    public Timestamp getPubdate() {
        return pubdate;
    }

    public void setPubdate(Timestamp pubdate) {
        this.pubdate = pubdate;
    }

    @ManyToOne(cascade=CascadeType.MERGE)
    @JoinColumn(name = "podcast_id", referencedColumnName = "id")
    @JsonBackReference("podcast-item")
    public Podcast getPodcast() {
        return podcast;
    }

    public void setPodcast(Podcast podcast) {
        this.podcast = podcast;
    }

//    @Transient
//    public Cover getPodcastCover() {
//        return this.podcast.getCover();
//    }

    @Column(name = "local_url")
    @Basic
    public String getLocalUrl() {
        return localUrl;
    }

    public void setLocalUrl(String localUrl) {
        this.localUrl = localUrl;
    }

    @Column(name = "status")
    @Basic
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Transient
    public int getProgression() {
        return progression;
    }

    public void setProgression(int progression) {
        this.progression = progression;
    }

    @OneToOne(fetch = FetchType.EAGER, cascade=CascadeType.ALL, orphanRemoval=true)
    @JoinColumn(name="cover_id")
    public Cover getCover() {
        return this.cover;
    }

    public void setCover(Cover cover) {
        this.cover = cover;
    }

    @Column(name = "downloadddate")
    @Basic
    public Timestamp getDownloaddate() {
        return downloaddate;
    }

    public void setDownloaddate(Timestamp downloaddate) {
        this.downloaddate = downloaddate;
    }

    @Column(name = "description")
    @Basic
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Column(name = "local_uri")
    @Basic
    public String getLocalUri() {
        return localUri;
    }

    public void setLocalUri(String localUri) {
        this.localUri = localUri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;

        Item item = (Item) o;

        if (!url.equals(item.url)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return url.hashCode();
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


}
