package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Tag;
import lan.dk.podcastserver.repository.TagRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 23/07/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class TagBusinessTest {

    @Mock TagRepository tagRepository;
    @InjectMocks TagBusiness tagBusiness;

    @Test
    public void should_find_all() {
        /* Given */
        ArrayList<Tag> list = new ArrayList<>();
        when(tagRepository.findAll()).thenReturn(list);

        /* When */
        List<Tag> tags = tagBusiness.findAll();

        /* Then */
        assertThat(tags).isSameAs(list);
        verify(tagRepository, times(1)).findAll();
    }

    @Test
    public void should_find_one() {
        /* Given */
        Integer tagId = 1;
        Tag tag = new Tag().setId(tagId);
        when(tagRepository.findOne(any())).thenReturn(tag);

        /* When */
        Tag tagToFind = tagBusiness.findOne(tagId);

        /* Then */
        assertThat(tagToFind).isSameAs(tag);
        verify(tagRepository, times(1)).findOne(eq(tagId));
    }

    @Test
    public void should_find_by_name_like() {
         /* Given */
        String searchWord = "name";
        ArrayList<Tag> list = new ArrayList<>();
        when(tagRepository.findByNameContainsIgnoreCase(anyString())).thenReturn(list);

        /* When */
        List<Tag> tags = tagBusiness.findByNameLike(searchWord);

        /* Then */
        assertThat(tags).isSameAs(list);
        verify(tagRepository, times(1)).findByNameContainsIgnoreCase(eq(searchWord));
    }
    
    @Test
    public void should_get_tag_by_name_in_set() {
        /* Given */
        Set<Tag> tags = new HashSet<>();
        tags.add(new Tag().setId(1).setName("tag1"));
        tags.add(new Tag().setId(2).setName("tag2"));
        tags.add(new Tag().setName("Foo"));
        tags.add(new Tag().setName("Bar"));

        when(tagRepository.findOne(anyInt()))
                .then(invocation -> new Tag()
                        .setId((Integer) invocation.getArguments()[0])
                        .setName("tag" + invocation.getArguments()[0])
                );

        /* When */
        Set<Tag> tagListByName = tagBusiness.getTagListByName(tags);

        /* Then */
        assertThat(tagListByName)
                .containsAll(tags);

        verify(tagRepository, times(2)).findOne(or(eq(1), eq(2)));
    }
}