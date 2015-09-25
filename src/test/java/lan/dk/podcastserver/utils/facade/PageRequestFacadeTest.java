package lan.dk.podcastserver.utils.facade;

import org.junit.Test;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Created by kevin on 23/09/15 for Podcast Server
 */
public class PageRequestFacadeTest {

    @Test
    public void should_have_default_value() {
        PageRequestFacadeAssert
                .assertThat(new PageRequestFacade())
                .hasSize(10)
                .hasPage(0)
                .hasDirection("DESC")
                .hasProperties(null);
    }
    
    @Test
    public void should_have_specific_attributes() {
        /* Given */
        PageRequestFacade pageRequestFacade = new PageRequestFacade();

        /* When */
        pageRequestFacade
                .setSize(5)
                .setPage(10)
                .setDirection("ASC")
                .setProperties("Foo");

        /* Then */
        PageRequestFacadeAssert
                .assertThat(pageRequestFacade)
                .hasSize(5)
                .hasPage(10)
                .hasDirection("ASC")
                .hasProperties("Foo");
    }

    @Test
    public void should_generate_page_request() {
        /* Given */
        PageRequestFacade pageRequestFacade = new PageRequestFacade().setProperties("Property");

        /* When */
        PageRequest pageRequest = pageRequestFacade.toPageRequest();

        /* Then */
        assertThat(pageRequest)
                .isInstanceOf(PageRequest.class);
    }

}