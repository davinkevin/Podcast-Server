package lan.dk.podcastserver.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lan.dk.podcastserver.utils.jDomUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Table(name = "podcast")
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class Podcast implements Serializable {

    private Integer id;
    private String title;
    private String url;
    private String signature;
    private String type;
    private ZonedDateTime lastUpdate;
    private Set<Item> items = new HashSet<>();
    private Cover cover;
    private String description;
    private Boolean hasToBeDeleted;
    private Set<Tag> tags = new HashSet<>();


    public Podcast() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "type")
    @Basic
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Column(name = "title")
    @Basic
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Column(name = "url", length = 65535)
    @Basic
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Column(name = "signature")
    @Basic
    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Column(name = "last_update")
    @Type(type = "org.jadira.usertype.dateandtime.threeten.PersistentZonedDateTime")
    public ZonedDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(ZonedDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    @OneToMany(mappedBy = "podcast", fetch = FetchType.LAZY, cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("pubdate DESC")
    @Fetch(FetchMode.SUBSELECT)
    public Set<Item> getItems() {
        return items;
    }

    public void setItems(Set<Item> items) {
        this.items = items;
    }

    @OneToOne(fetch = FetchType.EAGER, cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, orphanRemoval=true)
    @JoinColumn(name="cover_id")
    public Cover getCover() {
        return cover;
    }

    public void setCover(Cover cover) {
        this.cover = cover;
    }

    @Column(name = "description", length = 65535 )
    @Basic
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Column(name = "hasToBeDeleted")
    @Basic
    public Boolean getHasToBeDeleted() {
        return hasToBeDeleted;
    }

    public void setHasToBeDeleted(Boolean hasToBeDeleted) {
        this.hasToBeDeleted = hasToBeDeleted;
    }

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinTable(name="PODCAST_TAG",
            joinColumns={@JoinColumn(name="PODCAST_ID", referencedColumnName="ID")},
            inverseJoinColumns={@JoinColumn(name="TAG_ID", referencedColumnName="ID")})
    @Fetch(FetchMode.SUBSELECT)
    public Set<Tag> getTags() {
        return tags;
    }

    public Podcast setTags(Set<Tag> tags) {
        this.tags = tags;
        return this;
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
        if (o == null || getClass() != o.getClass()) return false;

        Podcast that = (Podcast) o;

        return Objects.equals(id, that.id) && !(lastUpdate != null ? !lastUpdate.equals(that.lastUpdate) : that.lastUpdate != null) && !(signature != null ? !signature.equals(that.signature) : that.signature != null) && !(title != null ? !title.equals(that.title) : that.title != null) && !(url != null ? !url.equals(that.url) : that.url != null);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (signature != null ? signature.hashCode() : 0);
        result = 31 * result + (lastUpdate != null ? lastUpdate.hashCode() : 0);
        return result;
    }

    /* XML Methods */
    @Transient @JsonIgnore
    public String toXML(String serveurURL) {
        return jDomUtils.podcastToXMLGeneric(this, serveurURL);
    }

    @Transient @JsonIgnore
    public Podcast add(Tag tag) {
        this.tags.add(tag);
        return this;
    }

    @Transient @JsonIgnore
    public Boolean contains(Item item) {
        return items.contains(item);
    }
    
    @Transient @JsonIgnore
    public Podcast add(Item item) {
        items.add(item.setPodcast(this));
        return this;
    }
}
