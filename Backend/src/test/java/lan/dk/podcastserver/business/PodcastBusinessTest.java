package lan.dk.podcastserver.business;

import com.google.common.collect.Sets;
import lan.dk.podcastserver.entity.*;
import lan.dk.podcastserver.exception.PodcastNotFoundException;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.JdomService;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
        UUID podcastId = UUID.randomUUID();
        Podcast podcast = new Podcast();
        when(podcastRepository.findOne(any(UUID.class))).then(i -> {
            podcast.setId((UUID) i.getArguments()[0]);
            return podcast;
        });

        /* When */
        Podcast aPodcast = podcastBusiness.findOne(podcastId);

        /* Then */
        PodcastAssert
                .assertThat(aPodcast)
                .hasId(podcastId)
                .isSameAs(podcast);
        verify(podcastRepository, times(1)).findOne(eq(podcastId));
    }

    @Test(expected = PodcastNotFoundException.class)
    public void should_throw_exception_if_id_not_found() {
        /* Given */
        when(podcastRepository.findOne(any(UUID.class))).thenReturn(null);
        /* When */
        podcastBusiness.findOne(UUID.randomUUID());
        /* Then see @Test */
    }

    @Test
    public void should_delete() {
        /* Given */ UUID podcastId = UUID.randomUUID();
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
        Set<Podcast> listOfPodcast = Sets.newHashSet();
        when(podcastRepository.findByUrlIsNotNull()).thenReturn(listOfPodcast);

       /* When */
        Set<Podcast> podcasts = podcastBusiness.findByUrlIsNotNull();

       /* Then */
        assertThat(podcasts).isSameAs(listOfPodcast);
        verify(podcastRepository, times(1)).findByUrlIsNotNull();
    }

    @Test
    public void should_get_items() {
       /* Given */
        UUID idPodcast = UUID.randomUUID();
        Podcast podcast = new Podcast();
        podcast.setItems(new HashSet<>());
        when(podcastRepository.findOne(any(UUID.class))).thenReturn(podcast);

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

    @Test
    public void should_create_podcast() {
        /* Given */
        Set<Tag> tags = new HashSet<>();
        tags.add(new Tag().setName("Tag1"));
        tags.add(new Tag().setName("Tag2"));
        Podcast podcast = new Podcast().setTags(tags);
        Cover cover = Cover.builder().url("http://fakeurl.com/image.png").build();
        podcast.setCover(cover);

        when(coverBusiness.download(any(Podcast.class))).then(i -> ((Podcast) i.getArguments()[0]).getCover().getUrl());
        when(tagBusiness.getTagListByName(anySetOf(Tag.class))).thenReturn(tags);
        when(podcastRepository.save(any(Podcast.class))).then(i -> i.getArguments()[0]);

        /* When */
        Podcast savedPodcast = podcastBusiness.create(podcast);

        /* Then */
        PodcastAssert
                .assertThat(savedPodcast)
                .hasTags(tags.toArray(new Tag[tags.size()]))
                .hasCover(cover);

        verify(coverBusiness, times(1)).download(eq(podcast));
        verify(tagBusiness, times(1)).getTagListByName(eq(tags));
        verify(podcastRepository, times(1)).save(eq(podcast));
    }

    @Test
    public void should_get_rss_with_default_limit() throws IOException {
        /* Given */
        Podcast podcast = new Podcast();
        String response = "Success";
        when(podcastRepository.findOne(any(UUID.class))).thenReturn(podcast);
        when(jdomService.podcastToXMLGeneric(eq(podcast), anyString(), anyBoolean())).thenReturn(response);
        UUID id = UUID.randomUUID();

        /* When */
        String rssReturn = podcastBusiness.getRss(id, true, "http://localhost");

        /* Then */
        assertThat(rssReturn).isEqualTo(response);
        verify(podcastRepository, times(1)).findOne(eq(id));
        verify(jdomService, times(1)).podcastToXMLGeneric(eq(podcast), eq("http://localhost"), eq(true));
    }

    @Test
    public void should_get_rss_with_define_limit() throws IOException {
        /* Given */
        Podcast podcast = new Podcast();
        String response = "Success";
        when(podcastRepository.findOne(any(UUID.class))).thenReturn(podcast);
        when(jdomService.podcastToXMLGeneric(eq(podcast), anyString(), anyBoolean())).thenReturn(response);
        UUID id = UUID.randomUUID();

        /* When */
        String rssReturn = podcastBusiness.getRss(id, false, "http://localhost");

        /* Then */
        assertThat(rssReturn).isEqualTo(response);
        verify(podcastRepository, times(1)).findOne(eq(id));
        verify(jdomService, times(1)).podcastToXMLGeneric(eq(podcast), eq("http://localhost"), eq(false));
    }

    @Test
    public void should_return_empty_string_for_exception() throws IOException {
        /* Given */
        doThrow(IOException.class).when(jdomService).podcastToXMLGeneric(any(Podcast.class), anyString(), anyBoolean());
        when(podcastRepository.findOne(any(UUID.class))).thenReturn(Podcast.DEFAULT_PODCAST);

        /* When */
        String rssReturn = podcastBusiness.getRss(UUID.randomUUID(), false, "http://localhost");

        /* Then */
        assertThat(rssReturn).isEqualTo("");
    }

    @Test(expected = PodcastNotFoundException.class)
    public void should_reject_patch_of_podcast() {
        podcastBusiness.patchUpdate(new Podcast());
    }

    @Test
    public void should_patch_podcast() {
        /* Given */
        UUID id = UUID.randomUUID();
        Set<Tag> tags = new HashSet<>();
        tags.add(new Tag().setName("Tag1"));
        tags.add(new Tag().setName("Tag2"));

        Podcast retrievePodcast = new Podcast(),
                patchPodcast = new Podcast();

        retrievePodcast.setCover(Cover.builder().url("http://fake.url/image2.png").build());
        retrievePodcast.setId(id);

        patchPodcast.setId(id);
        patchPodcast.setTitle("Toto");
        patchPodcast.setUrl("http://fake.url/podcast.rss");
        patchPodcast.setType("RSS");
        UUID idCover = UUID.randomUUID();
        patchPodcast.setCover(Cover.builder().id(idCover).url("http://fake.url/image.png").build());
        patchPodcast.setDescription("Description");
        patchPodcast.setHasToBeDeleted(true);
        patchPodcast.setTags(tags);

        when(podcastRepository.findOne(eq(patchPodcast.getId()))).thenReturn(retrievePodcast);
        when(coverBusiness.hasSameCoverURL(any(Podcast.class), any(Podcast.class))).thenReturn(false);
        when(coverBusiness.findOne(any(UUID.class))).then(i -> new Cover().setId((UUID) i.getArguments()[0]).setHeight(100).setWidth(100).setUrl("http://a.pretty.url.com/image.png"));
        when(tagBusiness.getTagListByName(anySetOf(Tag.class))).then(i -> i.getArguments()[0]);
        when(podcastRepository.save(any(Podcast.class))).then(i -> i.getArguments()[0]);

        /* When */
        Podcast updatedPodcast = podcastBusiness.patchUpdate(patchPodcast);

        /* Then */
        PodcastAssert
                .assertThat(updatedPodcast)
                .hasId(patchPodcast.getId())
                .hasTitle(patchPodcast.getTitle())
                .hasUrl(patchPodcast.getUrl())
                .hasType(patchPodcast.getType())
                .hasCover(patchPodcast.getCover())
                .hasDescription(patchPodcast.getDescription())
                .hasHasToBeDeleted(patchPodcast.getHasToBeDeleted())
                .hasTags(tags.toArray(new Tag[tags.size()]));

        verify(podcastRepository, times(1)).findOne(eq(id));
        verify(coverBusiness, times(1)).hasSameCoverURL(eq(patchPodcast), eq(retrievePodcast));
        verify(coverBusiness, times(1)).findOne(eq(idCover));
        verify(tagBusiness, times(1)).getTagListByName(eq(tags));
        verify(podcastRepository, times(1)).save(eq(retrievePodcast));
    }

    @Test
    public void should_get_cover_of() {
        /* Given */
        UUID podcastId = UUID.randomUUID();
        Path coverPath = Paths.get("/");
        Podcast podcast = Podcast.builder().url("http://an/url").title("Foo").id(podcastId).build();

        when(podcastRepository.findOne(eq(podcastId))).thenReturn(podcast);
        when(coverBusiness.getCoverPathOf(eq(podcast))).thenReturn(coverPath);

        /* When */
        Path path = podcastBusiness.coverOf(podcastId);

        /* Then */
        assertThat(path).isSameAs(coverPath);
        verify(podcastRepository).findOne(eq(podcastId));
        verify(coverBusiness).getCoverPathOf(eq(podcast));
    }
}