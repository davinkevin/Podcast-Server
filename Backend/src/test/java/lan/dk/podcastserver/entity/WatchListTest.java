package lan.dk.podcastserver.entity;

import io.vavr.collection.HashSet;
import org.junit.Test;

import java.util.Set;
import java.util.UUID;

import static lan.dk.podcastserver.assertion.Assertions.assertThat;

/**
 * Created by kevin on 19/03/2016 for Podcast Server
 */
public class WatchListTest {

    @Test
    public void should_have_getters_and_setters() {
        /* Given */
        String uuid = "b4156ceb-ddd8-437f-b822-941bf1c14723";
        Set<Item> items = HashSet.<Item>empty().toJavaSet();
        String name = "Foo";

        /* When */
        WatchList watchList = new WatchList()
                .setId(UUID.fromString(uuid))
                .setItems(items)
                .setName(name);

        /* Then */
        assertThat(watchList)
                .hasId(UUID.fromString(uuid))
                .hasNoItems()
                .hasName(name);
    }

}
