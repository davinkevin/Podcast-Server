package lan.dk.podcastserver.entity;

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
public class Playlist {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID id;

    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<Item> items = Sets.newHashSet();

    public Playlist add(Item item) {
        items.add(item);
        return this;
    }
}
