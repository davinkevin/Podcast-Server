package lan.dk.podcastserver.business;

import javaslang.control.Option;
import lan.dk.podcastserver.entity.Tag;
import lan.dk.podcastserver.repository.TagRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
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
        javaslang.collection.List<Tag> tags = tagBusiness.findAll();

        /* Then */
        assertThat(tags.toJavaList()).isEqualTo(list);
        verify(tagRepository, times(1)).findAll();
    }

    @Test
    public void should_find_one() {
        /* Given */
        UUID tagId = UUID.randomUUID();
        Tag tag = new Tag().setId(tagId);
        when(tagRepository.findOne(any(UUID.class))).thenReturn(tag);

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
        javaslang.collection.Set<Tag> foundTags = javaslang.collection.HashSet.empty();
        when(tagRepository.findByNameContainsIgnoreCase(anyString())).thenReturn(foundTags);

        /* When */
        javaslang.collection.Set<Tag> tags = tagBusiness.findByNameLike(searchWord);

        /* Then */
        assertThat(tags).isSameAs(foundTags);
        verify(tagRepository, times(1)).findByNameContainsIgnoreCase(eq(searchWord));
    }
    
    @Test
    public void should_get_tag_by_name_in_set() {
        /* Given */
        Set<Tag> tags = new HashSet<>();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Tag tag1 = new Tag().setId(id1).setName("tag" + id1);
        Tag tag2 = new Tag().setId(id2).setName("tag" + id2);
        Tag tag3 = new Tag().setName("Foo");
        Tag tag4 = new Tag().setName("Bar");
        tags.add(tag1);
        tags.add(tag2);
        tags.add(tag3);
        tags.add(tag4);


        when(tagRepository.findByNameIgnoreCase(eq(tag1.getName()))).thenReturn(Option.of(tag1));
        when(tagRepository.findByNameIgnoreCase(eq(tag2.getName()))).thenReturn(Option.of(tag2));
        when(tagRepository.findByNameIgnoreCase(and(not(eq(tag1.getName())), not(eq(tag1.getName()))))).thenReturn(Option.none());

        when(tagRepository.save(any(Tag.class))).then(t -> t.getArgumentAt(0, Tag.class).setId(UUID.randomUUID()));

        /* When */
        javaslang.collection.Set<Tag> tagListByName = tagBusiness.getTagListByName(tags);

        /* Then */
        assertThat(tagListByName.toJavaSet())
                .extracting("name", String.class)
                .contains(tag1.getName(), tag2.getName(), tag3.getName(), tag4.getName());
    }
}