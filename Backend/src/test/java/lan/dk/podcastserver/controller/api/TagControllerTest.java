package lan.dk.podcastserver.controller.api;

import lan.dk.podcastserver.business.TagBusiness;
import lan.dk.podcastserver.entity.Tag;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

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
        UUID id = UUID.randomUUID();
        Tag value = new Tag();
        when(tagBusiness.findOne(eq(id))).thenReturn(value);

        /* When */
        Tag tagById = tagController.findById(id);

        /* Then */
        assertThat(tagById).isSameAs(value);
        verify(tagBusiness, only()).findOne(eq(id));
    }

    @Test
    public void should_find_all_tag() {
        /* Given */
        List<Tag> tags = new ArrayList<>();
        when(tagBusiness.findAll()).thenReturn(tags);

        /* When */
        List<Tag> tagList = tagController.findAll();

        /* Then */
        assertThat(tagList).isSameAs(tags);
        verify(tagBusiness, only()).findAll();
    }

    @Test
    public void should_find_by_name() {
        /* Given */
        String name = "Foo";
        List<Tag> tags = new ArrayList<>();
        when(tagBusiness.findByNameLike(eq(name))).thenReturn(tags);

        /* When */
        List<Tag> tagsFoundByName = tagController.findByNameLike(name);

        /* Then */
        assertThat(tagsFoundByName).isSameAs(tags);
        verify(tagBusiness, only()).findByNameLike(eq(name));
    }
}