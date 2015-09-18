package lan.dk.podcastserver.controller.api;

import lan.dk.podcastserver.business.TagBusiness;
import lan.dk.podcastserver.entity.Tag;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static lan.dk.podcastserver.entity.TagAssert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 15/09/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class TagControllerTest {

    @Mock TagBusiness tagBusiness;
    @InjectMocks TagController tagController;

    @Test
    public void should_find_tag_by_id() {
        /* Given */
        Integer id = 1;
        Tag value = new Tag();
        when(tagBusiness.findOne(eq(id))).thenReturn(value);

        /* When */
        Tag tagById = tagController.findById(id);

        /* Then */
        assertThat(tagById)
                .isSameAs(value);
        verify(tagBusiness, only()).findOne(eq(id));

    }

}