package lan.dk.podcastserver.manager.worker.updater;

import groovy.util.logging.Slf4j;
import lan.dk.podcastserver.context.ValidatorConfig;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 15/11/2015 for Podcast Server
 */
@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ValidatorConfig.class, PodcastServerParameters.class, SignatureService.class, UrlService.class, JdomService.class, MimeTypeService.class, HtmlService.class, ImageService.class}, loader=AnnotationConfigContextLoader.class)
public class YoutubeUpdaterIntegrationTest {

    @Autowired YoutubeUpdater youtubeUpdater;
    @Autowired PodcastServerParameters podcastServerParameters;

    @Before
    public void beforeEach() {
        PodcastServerParameters.Api api = new PodcastServerParameters.Api();
        api.setYoutube("KEY");
        podcastServerParameters.setApi(api);
    }

    @Test
    public void should_do_di() { assertThat(youtubeUpdater).isNotNull(); }

    @Test
    public void should_get_items() {
        /* Given */
        Podcast podcast = Podcast.builder().title("Joueur du Grenier").url("https://www.youtube.com/user/joueurdugrenier").build();

        /* When */
        Set<Item> items = youtubeUpdater.getItems(podcast);

        /* Then */
        items.forEach(System.out::println);
    }
}