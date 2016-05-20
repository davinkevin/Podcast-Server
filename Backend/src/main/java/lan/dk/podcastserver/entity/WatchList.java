package lan.dk.podcastserver.entity;

import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.collect.Sets;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Set;
import java.util.UUID;

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@Entity
@Builder
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WatchList {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    private UUID id;

    private String name;

    @ManyToMany
    @JsonView(WatchListDetailsListView.class)
    private Set<Item> items = Sets.newHashSet();

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

    public interface WatchListDetailsListView {};
}
