package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.PodcastAssert;
import lan.dk.podcastserver.entity.Tag;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.JdomService;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.PodcastServerParameters;
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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 27/07/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class PodcastBusinessTest {

    @Mock PodcastServerParameters podcastServerParameters;
    @Mock JdomService jdomService;
    @Mock PodcastRepository podcastRepository;
    @Mock TagBusiness tagBusiness;
    @Mock CoverBusiness coverBusiness;
    @Mock MimeTypeService mimeTypeService;
    @InjectMocks PodcastBusiness podcastBusiness;

    @Test
    public void should_find_all() {
        /* Given */
        ArrayList<Podcast> listOfPodcast = new ArrayList<>();
        when(podcastRepository.findAll()).thenReturn(listOfPodcast);

        /* When */
        List<Podcast> podcasts = podcastBusiness.findAll();

        /* Then */
        assertThat(podcasts).isSameAs(listOfPodcast);
        verify(podcastRepository, times(1)).findAll();
    }

    @Test
    public void should_save() {
        /* Given */
        Podcast podcast = new Podcast();
        when(podcastRepository.save(any(Podcast.class))).then(i -> i.getArguments()[0]);

        /* When */
        Podcast savedPodcast = podcastBusiness.save(podcast);

        /* Then */
        assertThat(savedPodcast).isSameAs(podcast);
        verify(podcastRepository, times(1)).save(eq(podcast));
    }

    @Test
    public void should_find_one() {
        /* Given */
        Integer podcastId = 1;
        Podcast podcast = new Podcast();
        when(podcastRepository.findOne(anyInt())).then(i -> {
            podcast.setId((Integer) i.getArguments()[0]);
            return podcast;
        });

        /* When */
        Podcast aPodcast = podcastBusiness.findOne(podcastId);

        /* Then */
        PodcastAssert.assertThat(aPodcast)
                .hasId(podcastId)
                .isSameAs(podcast);
        verify(podcastRepository, times(1)).findOne(eq(podcastId));
    }

    @Test
    public void should_delete() {
        /* Given */ Integer podcastId = 1;
        /* When */  podcastBusiness.delete(podcastId);
        /* Then */  verify(podcastRepository, times(1)).delete(eq(podcastId));
    }

    @Test
    public void should_delete_by_entity() {
       /* Given */ Podcast podcast = new Podcast();
       /* When */ podcastBusiness.delete(podcast);
       /* Then */ verify(podcastRepository, times(1)).delete(eq(podcast));
    }

    @Test
    public void should_find_with_url_not_null() {
       /* Given */
        ArrayList<Podcast> listOfPodcast = new ArrayList<>();
        when(podcastRepository.findByUrlIsNotNull()).thenReturn(listOfPodcast);

       /* When */
        List<Podcast> podcasts = podcastBusiness.findByUrlIsNotNull();

       /* Then */
        assertThat(podcasts).isSameAs(listOfPodcast);
        verify(podcastRepository, times(1)).findByUrlIsNotNull();
    }

    @Test
    public void should_get_items() {
       /* Given */
        Integer idPodcast = 2;
        Podcast podcast = new Podcast();
        podcast.setItems(new HashSet<>());
        when(podcastRepository.findOne(anyInt())).thenReturn(podcast);

       /* When */
        Set<Item> items = podcastBusiness.getItems(idPodcast);

       /* Then */
        assertThat(items).isSameAs(podcast.getItems());
        verify(podcastRepository, times(1)).findOne(eq(idPodcast));
    }

    @Test
    public void should_reattach_and_save() {
       /* Given */
        Set<Tag> tags = new HashSet<>();
        tags.add(new Tag().setName("Tag1"));
        tags.add(new Tag().setName("Tag2"));
        Podcast podcast = new Podcast()
                .setTags(tags);

        when(tagBusiness.getTagListByName(anySetOf(Tag.class))).thenReturn(tags);
        when(podcastRepository.save(any(Podcast.class))).then(i -> i.getArguments()[0]);

       /* When */
        Podcast savedPodcast = podcastBusiness.reatachAndSave(podcast);

       /* Then */
        PodcastAssert
                .assertThat(savedPodcast)
                .hasTags(tags.toArray(new Tag[tags.size()]));
        verify(tagBusiness, times(1)).getTagListByName(eq(tags));
        verify(podcastRepository, times(1)).save(eq(podcast));
    }
}