package lan.dk.podcastserver.worker;

import lan.dk.podcastserver.context.ValidatorConfig;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.updater.ParleysUpdater;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import javax.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 12/07/2014.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ValidatorConfig.class}, loader=AnnotationConfigContextLoader.class)
public class ParleysUpdaterTest {

    private final Logger logger = LoggerFactory.getLogger(BeInSportWorker.class);

    @Resource
    ParleysUpdater parleysUpdater;

    Podcast DEVOXX_2014;

    @Before
    public void beforeEach() {
        DEVOXX_2014 = new Podcast();
        DEVOXX_2014.setType("Parleys");
        DEVOXX_2014.setTitle("Devoxx 2014");
        DEVOXX_2014.setUrl("https://www.parleys.com/channel/5459089ce4b030b13206d2ea/");
    }

    @Test
    public void signatureFeedParleys() {
        String signature =  parleysUpdater.signatureOf(DEVOXX_2014);
        String signature2 = parleysUpdater.signatureOf(DEVOXX_2014);

        assertThat(signature).isEqualTo(signature2);
    }
}
