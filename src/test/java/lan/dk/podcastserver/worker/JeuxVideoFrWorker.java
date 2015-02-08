package lan.dk.podcastserver.worker;

import lan.dk.podcastserver.config.PropertyConfig;
import lan.dk.podcastserver.context.ValidatorConfig;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.updater.JeuxVideoFRUpdater;
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
 * Created by kevin on 22/02/2014.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PropertyConfig.class, ValidatorConfig.class}, loader=AnnotationConfigContextLoader.class)
public class JeuxVideoFrWorker {

    private final Logger logger = LoggerFactory.getLogger(JeuxVideoFrWorker.class);
    private static Podcast defisJVFR, insertDisk;

    @Resource JeuxVideoFRUpdater jeuxVideoFRUpdater;

    @Before
    public void initPodcast() {
        defisJVFR = new Podcast();
        defisJVFR.setTitle("Les défis de la rédaction de JeuxVideoFr");
        defisJVFR.setUrl("http://www.jeuxvideo.fr/video/defis-de-la-redaction/");
        defisJVFR.setType("JeuxVideoFr");
        defisJVFR.setCover(new Cover("http://1.im6.fr/00C3006E3541664-c1-photo-oYToxOntzOjE6InciO2k6MTk1O30%3D-defi-chaine.jpg", 250, 166));

        insertDisk = new Podcast();
        insertDisk.setTitle("Insert Disk");
        insertDisk.setUrl("http://www.jeuxvideo.fr/video/insert-disk/");
        insertDisk.setType("JeuxVideoFr");
        insertDisk.setCover(new Cover("http://2.im6.fr/00C3006E5450055-c1-photo-oYToxOntzOjE6InciO2k6MTk1O30%3D-insert-disk-logo-dishonored.jpg", 250,160));
    }

    @Test
    public void signatureDefisJVFR() {

        String signature = jeuxVideoFRUpdater.generateSignature(defisJVFR);
        logger.info("Signature 1 : {}", signature);
        String signature2 = jeuxVideoFRUpdater.generateSignature(defisJVFR);
        logger.info("Signature 2 : {}", signature2);

        assertThat(signature).isEqualTo(signature2);
    }

    @Test
    public void updateFeedDefisJVFR() {
        jeuxVideoFRUpdater.updateAndAddItems(defisJVFR);
    }

    @Test
    public void updateInsertDisk() {
        jeuxVideoFRUpdater.updateAndAddItems(insertDisk);
    }
}
