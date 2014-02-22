package lan.dk.podcastserver.worker;

import lan.dk.podcastserver.config.PropertyConfig;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.updater.JeuxVideoFRUpdater;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.sql.Timestamp;

/**
 * Created by kevin on 22/02/2014.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PropertyConfig.class}, loader=AnnotationConfigContextLoader.class)
public class JeuxVideoFrWorker {

    private final Logger logger = LoggerFactory.getLogger(JeuxVideoFrWorker.class);

    JeuxVideoFRUpdater jeuxVideoFRUpdater = new JeuxVideoFRUpdater();


    @Test
    public void signatureDefisJVFR() {

        Podcast defisJVFR = new Podcast("Les défis de la rédaction de JeuxVideoFr", "http://www.jeuxvideo.fr/video/defis-de-la-redaction/",
                "", "JeuxVideoFr", new Timestamp(System.currentTimeMillis()), null, new Cover("http://1.im6.fr/00C3006E3541664-c1-photo-oYToxOntzOjE6InciO2k6MTk1O30%3D-defi-chaine.jpg", 250, 166), null, true);

        String signature = jeuxVideoFRUpdater.signaturePodcast(defisJVFR);
        logger.info("Signature 1 : {}", signature);
        String signature2 = jeuxVideoFRUpdater.signaturePodcast(defisJVFR);
        logger.info("Signature 2 : {}", signature2);

        Assert.assertEquals(signature, signature2);
    }

    @Test
    public void updateFeedExpresso() {

        Podcast defisJVFR = new Podcast("Les défis de la rédaction de JeuxVideoFr", "http://www.jeuxvideo.fr/video/defis-de-la-redaction/",
                "", "JeuxVideoFr", new Timestamp(System.currentTimeMillis()), null, new Cover("http://1.im6.fr/00C3006E3541664-c1-photo-oYToxOntzOjE6InciO2k6MTk1O30%3D-defi-chaine.jpg", 250, 166), null, true);

        jeuxVideoFRUpdater.updateFeed(defisJVFR);
    }
}
