package lan.dk.podcastserver.entity;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static lan.dk.podcastserver.entity.TagAssert.assertThat;

/**
 * Created by kevin on 15/06/15 for HackerRank problem
 */
public class TagTest {

    public static final Podcast PODCAST_1 = new Podcast();
    public static final Podcast PODCAST_2 = new Podcast();

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
            .setId(id)
            .setPodcasts(Sets.newHashSet(PODCAST_1, PODCAST_2));

        assertThat(tag)
            .hasId(id)
            .hasName("Humour")
            .hasOnlyPodcasts(PODCAST_1, PODCAST_2);
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
        org.assertj.core.api.Assertions.
                assertThat(tag.hashCode()).isEqualTo("Humour".hashCode());
    }

}