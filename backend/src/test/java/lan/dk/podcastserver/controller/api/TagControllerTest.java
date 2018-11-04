package lan.dk.podcastserver.controller.api;

import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import com.github.davinkevin.podcastserver.business.TagBusiness;
import lan.dk.podcastserver.entity.Tag;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 15/09/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class TagControllerTest {

    private @Mock TagBusiness tagBusiness;
    private @InjectMocks TagController tagController;

    @Test
    public void should_find_tag_by_id() {
        /* Given */
        UUID id = UUID.randomUUID();
        Tag value = new Tag();
        when(tagBusiness.findOne(id)).thenReturn(value);

        /* When */
        Tag tagById = tagController.findById(id);

        /* Then */
        assertThat(tagById).isSameAs(value);
        verify(tagBusiness, only()).findOne(id);
    }

    @Test
    public void should_find_all_tag() {
        /* Given */
        List<Tag> tags = List.empty();
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
        Set<Tag> tags = HashSet.empty();
        when(tagBusiness.findByNameLike(name)).thenReturn(tags);

        /* When */
        Set<Tag> tagsFoundByName = tagController.findByNameLike(name);

        /* Then */
        assertThat(tagsFoundByName).isSameAs(tags);
        verify(tagBusiness, only()).findByNameLike(name);
    }
}
