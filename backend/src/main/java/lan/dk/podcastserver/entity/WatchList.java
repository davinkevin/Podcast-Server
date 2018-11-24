package lan.dk.podcastserver.entity;

import com.fasterxml.jackson.annotation.JsonView;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@Entity
public class WatchList {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;

    private String name;

    @ManyToMany
    @JsonView(WatchListDetailsListView.class)
    private Set<Item> items = new HashSet<>();

    @java.beans.ConstructorProperties({"id", "name", "items"})
    private WatchList(UUID id, String name, Set<Item> items) {
        this.id = id;
        this.name = name;
        this.items = items;
    }

    public WatchList() {
    }

    public static WatchListBuilder builder() {
        return new WatchListBuilder();
    }

    public WatchList add(Item item) {
        item.getWatchLists().add(this);
        items.add(item);
        return this;
    }

    public WatchList remove(Item item) {
        item.getWatchLists().remove(this);
        items.remove(item);
        return this;
    }

    public UUID getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Set<Item> getItems() {
        return this.items;
    }

    public WatchList setId(UUID id) {
        this.id = id;
        return this;
    }

    public WatchList setName(String name) {
        this.name = name;
        return this;
    }

    public WatchList setItems(Set<Item> items) {
        this.items = items;
        return this;
    }

    public interface WatchListDetailsListView {}

    public static class WatchListBuilder {
        private UUID id;
        private String name;
        private Set<Item> items;

        WatchListBuilder() {
        }

        public WatchList.WatchListBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public WatchList.WatchListBuilder name(String name) {
            this.name = name;
            return this;
        }

        public WatchList.WatchListBuilder items(Set<Item> items) {
            this.items = items;
            return this;
        }

        public WatchList build() {
            return new WatchList(id, name, items);
        }

        public String toString() {
            return "WatchList.WatchListBuilder(id=" + this.id + ", name=" + this.name + ", items=" + this.items + ")";
        }
    }
}
