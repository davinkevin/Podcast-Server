package lan.dk.podcastserver.entity;

import org.junit.Test;

import java.util.UUID;

import static lan.dk.podcastserver.entity.CoverAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * Created by kevin on 14/06/15 for HackerRank problem
 */
public class CoverTest {

    private static final String NOWHERE_URL = "http://nowhere.com";
    private static final Cover COVER_WITH_DIMENSIONS = Cover.builder().url(NOWHERE_URL).width(100).height(200).build();

    @Test
    public void should_create_a_cover_with_url() {
        assertThat(Cover.builder().url(NOWHERE_URL).build())
                .hasUrl(NOWHERE_URL)
                .hasHeight(null)
                .hasWidth(null)
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
        assertThat(Cover.builder().url(NOWHERE_URL).width(100).height(200).build())
                .hasUrl(NOWHERE_URL)
                .hasWidth(100)
                .hasHeight(200)
                .hasId(null);
    }

    @Test
    public void should_interact_with_id() {
        /* Given */
        Cover cover = new Cover();
        UUID id = UUID.randomUUID();

        /* When */
        cover.setId(id);

        /* Then */
        assertThat(cover).hasId(id);
    }

    @Test
    public void should_be_equals_and_has_same_hashcode() {
        /* Given */
        Cover cover = Cover.builder().url(NOWHERE_URL).build();
        Cover coverWithSameUrl = Cover.builder().url(NOWHERE_URL).build();
        Object notCover = new Object();
        Cover coverWithDiffUrl = Cover.builder().url("NotNowhereUrl").build();

        /* Then */
        assertThat(cover).isEqualTo(cover);
        assertThat(cover).isEqualTo(coverWithSameUrl);
        assertThat(cover).isNotEqualTo(notCover);
        assertThat(cover).isNotEqualTo(coverWithDiffUrl);
        assertThat(cover.hashCode()).isEqualTo(coverWithSameUrl.hashCode());

    }
}
