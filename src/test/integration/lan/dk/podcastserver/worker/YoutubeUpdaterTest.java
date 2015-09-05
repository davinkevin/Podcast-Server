package lan.dk.podcastserver.worker;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.updater.YoutubeUpdater;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lan.dk.podcastserver.service.SignatureService;
import lan.dk.podcastserver.service.JdomService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;

import javax.validation.Validator;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 21/12/2013.
 */
@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(classes = {YoutubeUpdater.class})
@Ignore
public class YoutubeUpdaterTest {

    private final Logger logger = LoggerFactory.getLogger(YoutubeUpdaterTest.class);

    @InjectMocks YoutubeUpdater youtubeUpdater;
    @Mock PodcastServerParameters podcastServerParameters;
    @Mock Validator validator;
    @Spy JdomService jdomService;
    @Spy SignatureService signatureService;

    @Before
    public void initPodcast() {
        logger.debug("InitPodcast");
    }
    
    @Test
    public void should_generate_signature () {
        /* Given */
        Podcast nowTechTvFr = new Podcast();
        nowTechTvFr.setUrl("https://www.youtube.com/user/NowTechTVfr");

        /* When */
        String signature = youtubeUpdater.signatureOf(nowTechTvFr);
        String signatureBis = youtubeUpdater.signatureOf(nowTechTvFr);

        /* Then */
        assertThat(signature).isNotNull().isNotEmpty().isEqualTo(signatureBis);
    }

    @Test
    public void should_get_items () {
        /* Given */
        Podcast nowTechTvFr = new Podcast();
        nowTechTvFr.setUrl("https://www.youtube.com/user/NowTechTVfr");
                
        /* When */
        Set<Item> items = youtubeUpdater.getItems(nowTechTvFr);
        
        /* Then */
        assertThat(items).isNotNull().isNotEmpty();
    }
}
