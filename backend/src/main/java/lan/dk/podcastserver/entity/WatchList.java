package lan.dk.podcastserver.entity;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@Entity
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Accessors(chain = true)
public class WatchList {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;

    private String name;

    @ManyToMany
    @JsonView(WatchListDetailsListView.class)
    private Set<Item> items = new HashSet<>();

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

    public interface WatchListDetailsListView {}
}
