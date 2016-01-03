package lan.dk.podcastserver.utils.facade;

import org.junit.Test;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;

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
                .hasPage(0);
    }
    
    @Test
    public void should_have_specific_attributes() {
        /* Given */
        PageRequestFacade pageRequestFacade = new PageRequestFacade();

        /* When */
        pageRequestFacade
                .setSize(5)
                .setPage(10);

        /* Then */
        PageRequestFacadeAssert
                .assertThat(pageRequestFacade)
                .hasSize(5)
                .hasPage(10);
    }

    @Test
    public void should_generate_page_request() {
        /* Given */
        PageRequestFacade pageRequestFacade = new PageRequestFacade().setOrders(new ArrayList<>());

        /* When */
        PageRequest pageRequest = pageRequestFacade.toPageRequest();

        /* Then */
        assertThat(pageRequest).isInstanceOf(PageRequest.class);
    }

}