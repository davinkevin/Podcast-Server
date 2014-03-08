package lan.dk.podcastserver.entity;

import javax.persistence.*;

@Table(name = "cover", schema = "", catalog = "")
@Entity
public class Cover {

    private int id;
    private String URL;
    private int width;
    private int height;

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

    public void setURL(String URL) {
        this.URL = URL;
    }

    @Column(name = "width")
    @Basic
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @Column(name = "height")
    @Basic
    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

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
