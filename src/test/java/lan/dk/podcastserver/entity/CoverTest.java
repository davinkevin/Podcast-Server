package lan.dk.podcastserver.entity;

import org.junit.Test;

import static lan.dk.podcastserver.entity.CoverAssert.assertThat;


/**
 * Created by kevin on 14/06/15 for HackerRank problem
 */
public class CoverTest {

    public static final String NOWHERE_URL = "http://nowhere.com";
    public static final Cover COVER_WITH_DIMENSIONS = new Cover(NOWHERE_URL, 100, 200);

    @Test
    public void should_create_a_cover_with_url() {
        assertThat(new Cover(NOWHERE_URL))
                .hasUrl(NOWHERE_URL)
                .hasHeight(0)
                .hasWidth(0)
                .hasId(null);
    }
    
    @Test
    public void should_create_a_cover_with_all_parameters() {
        assertThat(COVER_WITH_DIMENSIONS)
                .hasUrl(NOWHERE_URL)
                .hasWidth(100)
                .hasHeight(200)
                .hasId(null);
    }

    @Test
    public void should_create_and_init_after() {
        assertThat(new Cover().setUrl(NOWHERE_URL).setWidth(100).setHeight(200))
                .hasUrl(NOWHERE_URL)
                .hasWidth(100)
                .hasHeight(200)
                .hasId(null);
    }

    @Test
    public void should_interact_with_id() {
        /* Given */ Cover cover = new Cover();
        /* When */  cover.setId(123);
        /* Then */  assertThat(cover).hasId(123);
    }

    @Test
    public void should_be_equals_and_has_same_hashcode() {
        /* Given */
        Cover cover = new Cover(NOWHERE_URL);
        Cover coverWithSameUrl = new Cover(NOWHERE_URL);
        Object notCover = new Object();
        Cover coverWithDiffUrl = new Cover("NotNowhereUrl");

        /* Then */
        assertThat(cover).isEqualTo(cover);
        assertThat(cover).isEqualTo(coverWithSameUrl);
        assertThat(cover).isNotEqualTo(notCover);
        assertThat(cover).isNotEqualTo(coverWithDiffUrl);
        org.assertj.core.api.Assertions.
                assertThat(cover.hashCode()).isEqualTo(coverWithSameUrl.hashCode());

    }
}