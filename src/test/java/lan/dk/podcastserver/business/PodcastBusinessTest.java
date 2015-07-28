package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.PodcastAssert;
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
import java.util.List;

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
}