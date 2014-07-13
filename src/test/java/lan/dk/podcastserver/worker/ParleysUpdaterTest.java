package lan.dk.podcastserver.worker;

import lan.dk.podcastserver.config.PropertyConfig;
import lan.dk.podcastserver.context.ValidatorConfig;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.updater.ParleysUpdater;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import javax.annotation.Resource;
import java.sql.Timestamp;

/**
 * Created by kevin on 12/07/2014.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PropertyConfig.class, ValidatorConfig.class}, loader=AnnotationConfigContextLoader.class)
public class ParleysUpdaterTest {

    private final Logger logger = LoggerFactory.getLogger(BeInSportWorker.class);

    @Resource
    ParleysUpdater parleysUpdater;

    Podcast DEVOXX_FRANCE_2014 = new Podcast("Devoxx France 2014", "http://www.parleys.com/channel/5355419ce4b0524a2f28bca0/presentations?sort=date&state=public",
            "", "Parleys", new Timestamp(System.currentTimeMillis()), null, new Cover("http://www.devoxx.com/download/attachments/5342010/logo_devoxx_france_big.jpg?version=2&modificationDate=1321095236000", 250, 166), null, true);


    @Test
    public void signatureFeedParleys() {
        String signature = parleysUpdater.signaturePodcast(DEVOXX_FRANCE_2014);
        String signature2 = parleysUpdater.signaturePodcast(DEVOXX_FRANCE_2014);

        Assert.assertEquals(signature, signature2);
    }

    @Test
    public void updateFeedParleys() {
        Podcast podcast = parleysUpdater.updateFeed(DEVOXX_FRANCE_2014);
        Assert.assertEquals(100, podcast.getItems().size());
    }

}
