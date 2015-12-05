package lan.dk.podcastserver.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor @AllArgsConstructor
@Builder
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

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Basic
    @Column(name = "type")
    @JsonView(PodcastListingView.class)
    public String getType() {
        return type;
    }

    public Podcast setType(String type) {
        this.type = type;
        return this;
    }

    @Basic
    @Column(name = "title")
    @JsonView(PodcastListingView.class)
    public String getTitle() {
        return title;
    }

    public Podcast setTitle(String title) {
        this.title = title;
        return this;
    }

    @Basic
    @Column(name = "url", length = 65535)
    @JsonView(PodcastDetailsView.class)
    public String getUrl() {
        return url;
    }

    public Podcast setUrl(String url) {
        this.url = url;
        return this;
    }

    @Basic
    @Column(name = "signature")
    @JsonIgnore
    public String getSignature() {
        return signature;
    }

    public Podcast setSignature(String signature) {
        this.signature = signature;
        return this;
    }

    @Column(name = "last_update")
    @Type(type = "org.jadira.usertype.dateandtime.threeten.PersistentZonedDateTime")
    @JsonView(PodcastListingView.class)
    public ZonedDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(ZonedDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    @JsonIgnore
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
    @JoinColumn(name="cover_id") @JsonView(PodcastListingView.class)
    public Cover getCover() {
        return cover;
    }

    public void setCover(Cover cover) {
        this.cover = cover;
    }

    @Basic
    @Column(name = "description", length = 65535 )
    @JsonView(PodcastDetailsView.class)
    public String getDescription() {
        return description;
    }

    public Podcast setDescription(String description) {
        this.description = description;
        return this;
    }

    @Basic
    @Column(name = "hasToBeDeleted")
    @JsonView(PodcastDetailsView.class)
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
    @JsonView(PodcastDetailsView.class)
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

        return Objects.equals(id, that.id)
                && !(lastUpdate != null ? !lastUpdate.equals(that.lastUpdate) : that.lastUpdate != null)
                && !(signature != null ? !signature.equals(that.signature) : that.signature != null)
                && !(title != null ? !title.equals(that.title) : that.title != null)
                && !(url != null ? !url.equals(that.url) : that.url != null);

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

    @Transient @JsonIgnore
    public Podcast lastUpdateToNow() {
        this.lastUpdate = ZonedDateTime.now();
        return this;
    }

    public interface PodcastListingView {}
    public interface PodcastDetailsView extends PodcastListingView{}

}
