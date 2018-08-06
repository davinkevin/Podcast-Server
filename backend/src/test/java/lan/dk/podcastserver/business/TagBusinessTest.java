package lan.dk.podcastserver.business;

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Tag;
import lan.dk.podcastserver.repository.TagRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static io.vavr.API.None;
import static io.vavr.API.Option;
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

    private @Mock TagRepository tagRepository;
    private @InjectMocks TagBusiness tagBusiness;

    @Test
    public void should_find_all() {
        /* Given */
        ArrayList<Tag> list = new ArrayList<>();
        when(tagRepository.findAll()).thenReturn(list);

        /* When */
        io.vavr.collection.List<Tag> tags = tagBusiness.findAll();

        /* Then */
        assertThat(tags.toJavaList()).isEqualTo(list);
        verify(tagRepository, times(1)).findAll();
    }

    @Test
    public void should_find_one() {
        /* Given */
        UUID tagId = UUID.randomUUID();
        Tag tag = new Tag().setId(tagId);
        when(tagRepository.findById(any(UUID.class))).thenReturn(Optional.of(tag));

        /* When */
        Tag tagToFind = tagBusiness.findOne(tagId);

        /* Then */
        assertThat(tagToFind).isSameAs(tag);
        verify(tagRepository, times(1)).findById(eq(tagId));
    }

    @Test
    public void should_find_by_name_like() {
         /* Given */
        String searchWord = "name";
        Set<Tag> foundTags = HashSet.empty();
        when(tagRepository.findByNameContainsIgnoreCase(anyString())).thenReturn(foundTags);

        /* When */
        Set<Tag> tags = tagBusiness.findByNameLike(searchWord);

        /* Then */
        assertThat(tags).isSameAs(foundTags);
        verify(tagRepository, times(1)).findByNameContainsIgnoreCase(eq(searchWord));
    }
    
    @Test
    public void should_get_tag_by_name_in_set() {
        /* Given */
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Tag tag1 = Tag.builder().id(id1).name("tag" + id1).build();
        Tag tag2 = Tag.builder().id(id2).name("tag" + id2).build();
        Tag tag3 = Tag.builder().name("Foo").build();
        Tag tag4 = Tag.builder().name("Bar").build();
        Set<Tag> tags = HashSet.of(tag1, tag2, tag3, tag4);

        when(tagRepository.findByNameIgnoreCase(eq(tag1.getName()))).thenReturn(Option(tag1));
        when(tagRepository.findByNameIgnoreCase(eq(tag2.getName()))).thenReturn(Option(tag2));
        when(tagRepository.findByNameIgnoreCase(and(not(eq(tag1.getName())), not(eq(tag1.getName()))))).thenReturn(None());

        when(tagRepository.save(any(Tag.class))).then(t -> t.getArgumentAt(0, Tag.class).setId(UUID.randomUUID()));

        /* When */
        Set<Tag> tagListByName = tagBusiness.getTagListByName(tags);

        /* Then */
        assertThat(tagListByName.toJavaSet())
                .extracting("name", String.class)
                .contains(tag1.getName(), tag2.getName(), tag3.getName(), tag4.getName());
    }

    @Test
    public void should_find_all_by_names() {
        /* GIVEN */
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Tag tag1 = Tag.builder().id(id1).name("Super Foo").build();
        Tag tag2 = Tag.builder().id(id2).name("BarOu !").build();
        when(tagRepository.findByNameIgnoreCase("foo")).thenReturn(Option(tag1));
        when(tagRepository.findByNameIgnoreCase("bar")).thenReturn(Option(tag2));
        Set<String> names = HashSet.of("foo", "bar");

        /* WHEN  */
        Set<Tag> tags = tagBusiness.findAllByName(names);

        /* THEN  */
        assertThat(tags).contains(tag1, tag2);
    }
}
