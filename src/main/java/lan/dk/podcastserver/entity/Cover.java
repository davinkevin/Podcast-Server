package lan.dk.podcastserver.entity;

import javax.persistence.*;

@Table(name = "cover")
@Entity
public class Cover {

    private int id;
    private String URL;
    private int width;
    private int height;

    private Item item;
    private Podcast podcast;

    public Cover(String URL, int width, int height) {
        this.URL = URL;
        this.width = width;
        this.height = height;
    }

    public Cover() {
    }

    public Cover(String URL) {
        this.URL = URL;
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

    @Column(name = "url")
    @Basic
    public String getURL() {
        return URL;
    }

    public Cover setURL(String URL) {
        this.URL = URL;
        return this;
    }

    @Column(name = "width")
    @Basic
    public int getWidth() {
        return width;
    }

    public Cover setWidth(int width) {
        this.width = width;
        return this;
    }

    @Column(name = "height")
    @Basic
    public int getHeight() {
        return height;
    }

    public Cover setHeight(int height) {
        this.height = height;
        return this;
    }

   /* @JsonIgnore
    @OneToOne(mappedBy = "cover", fetch = FetchType.LAZY)
    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    @JsonIgnore
    @OneToOne(mappedBy = "cover", fetch = FetchType.LAZY)
    public Podcast getPodcast() {
        return podcast;
    }

    public void setPodcast(Podcast podcast) {
        this.podcast = podcast;
    }*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cover)) return false;

        Cover cover = (Cover) o;

        if (!URL.equals(cover.URL)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return URL.hashCode();
    }
}
