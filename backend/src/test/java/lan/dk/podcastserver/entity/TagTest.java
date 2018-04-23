package lan.dk.podcastserver.entity;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static lan.dk.podcastserver.entity.TagAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
/**
 * Created by kevin on 15/06/15 for HackerRank problem
 */
public class TagTest {

    private static final Podcast PODCAST_1 = new Podcast();
    private static final Podcast PODCAST_2 = new Podcast();

    @Before
    public void init() {
        PODCAST_1.setId(UUID.randomUUID());
        PODCAST_2.setId(UUID.randomUUID());
    }

    @Test
    public void should_create_a_tag() {
        UUID id = UUID.randomUUID();
        Tag tag = new Tag()
            .setName("Humour")
            .setId(id);

        assertThat(tag)
            .hasId(id)
            .hasName("Humour");
    }

    @Test
    public void should_be_equals() {
        /* Given */
        Tag tag = new Tag()
                .setName("Humour")
                .setId(UUID.randomUUID());
        Tag notEquals = new Tag()
                    .setName("Conférence")
                    .setId(UUID.randomUUID());
        Object notATag = new Object();

        /* When */
        boolean notSameType = tag.equals(notATag);

        /* Then */
        assertThat(tag).isEqualTo(tag);
        assertThat(tag).isNotEqualTo(notEquals);
        assertThat(tag).isNotEqualTo(notSameType);
        assertThat(tag.hashCode()).isEqualTo("Humour".hashCode());
    }

}
