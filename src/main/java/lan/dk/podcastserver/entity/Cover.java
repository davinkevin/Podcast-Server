package lan.dk.podcastserver.entity;

import javax.persistence.*;

@Table(name = "cover")
@Entity
public class Cover {

    private Integer id;
    private String url;
    private int width;
    private int height;

    private Item item;
    private Podcast podcast;

    public Cover(String url, int width, int height) {
        this.url = url;
        this.width = width;
        this.height = height;
    }

    public Cover() {
    }

    public Cover(String url) {
        this.url = url;
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

    @Column(name = "url")
    @Basic
    public String getUrl() {
        return url;
    }

    public Cover setUrl(String url) {
        this.url = url;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cover)) return false;

        Cover cover = (Cover) o;

        if (!url.equals(cover.url)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}
