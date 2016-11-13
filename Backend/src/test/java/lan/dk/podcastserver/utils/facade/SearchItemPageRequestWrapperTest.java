package lan.dk.podcastserver.utils.facade;

import com.google.common.collect.Lists;
import javaslang.collection.List;
import lan.dk.podcastserver.entity.Tag;
import org.junit.Test;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

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
        Tag tag1 = new Tag().setId(UUID.randomUUID());
        Tag tag2 = new Tag().setId(UUID.randomUUID());
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

    @Test
    public void should_be_equals() {
        /* Given */
        Tag foo = Tag.builder().id(UUID.randomUUID()).name("Foo").build();
        Tag bar = Tag.builder().id(UUID.randomUUID()).name("Bar").build();

        PageRequestFacade prf1 = new SearchItemPageRequestWrapper()
                .setDownloaded(Boolean.TRUE)
                .setTags(Lists.newArrayList(foo, bar))
                .setTerm("Search")
                .setPage(1)
                .setSize(12)
                .setOrders(List.of(new PageRequestFacade.OrderFacade(Sort.Direction.ASC.toString(), "downloadedDate")));

        PageRequestFacade prf2 = new SearchItemPageRequestWrapper()
                .setDownloaded(Boolean.TRUE)
                .setTags(Lists.newArrayList(foo, bar))
                .setTerm("Search")
                .setPage(1)
                .setSize(12)
                .setOrders(List.of(new PageRequestFacade.OrderFacade(Sort.Direction.ASC.toString(), "downloadedDate")));


        /* When */

        /* Then */
        assertThat(prf1).isEqualTo(prf2);
    }


}