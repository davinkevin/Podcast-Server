package lan.dk.podcastserver.utils.facade;

import lan.dk.podcastserver.entity.Tag;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 23/09/15 for Podcast Server
 */
public class SearchItemPageRequestWrapperTest {

    @Test
    public void should_have_default_value() {
        SearchItemPageRequestWrapperAssert
            .assertThat(new SearchItemPageRequestWrapper())
            .hasDownloaded(Boolean.TRUE);
    }


    @Test
    public void should_has_term_and_tags() {
        /* Given */
        SearchItemPageRequestWrapper searchItemPageRequestWrapper = new SearchItemPageRequestWrapper();

        /* When */
        Tag tag1 = new Tag().setId(1);
        Tag tag2 = new Tag().setId(2);
        searchItemPageRequestWrapper
                .setTerm("A Term to Find")
                .setTags(Arrays.asList(tag1, tag2));

        /* Then */
        SearchItemPageRequestWrapperAssert
                .assertThat(searchItemPageRequestWrapper)
                .hasTerm("A Term to Find")
                .hasOnlyTags(tag1, tag2);
    }

    @Test
    public void should_be_search() {
        /* Given */
        SearchItemPageRequestWrapper siWithTags = new SearchItemPageRequestWrapper().setTags(new ArrayList<>());
        SearchItemPageRequestWrapper siWithTerm = new SearchItemPageRequestWrapper().setTerm("Foo");
        SearchItemPageRequestWrapper siWithDownload = new SearchItemPageRequestWrapper();
        SearchItemPageRequestWrapper siWithoutDownload = new SearchItemPageRequestWrapper().setDownloaded(null);

        /* Then */
        assertThat(siWithTags.isSearch()).isTrue();
        assertThat(siWithTerm.isSearch()).isTrue();
        assertThat(siWithDownload.isSearch()).isTrue();
        assertThat(siWithoutDownload.isSearch()).isFalse();
    }


}