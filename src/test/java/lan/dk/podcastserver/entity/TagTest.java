package lan.dk.podcastserver.entity;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import static lan.dk.podcastserver.entity.TagAssert.assertThat;

/**
 * Created by kevin on 15/06/15 for HackerRank problem
 */
public class TagTest {

    public static final Podcast PODCAST_1 = new Podcast();
    public static final Podcast PODCAST_2 = new Podcast();

    @Before
    public void init() {
        PODCAST_1.setId(1);
        PODCAST_2.setId(2);
    }

    @Test
    public void should_create_a_tag() {
        Tag tag = new Tag()
            .setName("Humour")
            .setId(1)
            .setPodcasts(Sets.newHashSet(PODCAST_1, PODCAST_2));

        assertThat(tag)
            .hasId(1)
            .hasName("Humour")
            .hasOnlyPodcasts(PODCAST_1, PODCAST_2);
    }

    @Test
    public void should_be_equals() {
        /* Given */
        Tag tag = new Tag()
                .setName("Humour")
                .setId(1);
        Tag notEquals = new Tag()
                    .setName("Conférence")
                    .setId(2);
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