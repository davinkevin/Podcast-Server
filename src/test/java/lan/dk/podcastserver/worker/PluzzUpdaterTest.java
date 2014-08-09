package lan.dk.podcastserver.worker;

import lan.dk.podcastserver.config.PropertyConfig;
import lan.dk.podcastserver.context.ValidatorConfig;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.updater.PluzzUpdater;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import javax.annotation.Resource;
import java.sql.Timestamp;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created by kevin on 12/07/2014.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PropertyConfig.class, ValidatorConfig.class}, loader=AnnotationConfigContextLoader.class)
public class PluzzUpdaterTest {

    private final Logger logger = LoggerFactory.getLogger(PluzzUpdaterTest.class);

    @Resource
    PluzzUpdater pluzzUpdater;

    Podcast IRON_MAN = new Podcast("Iron Man", "http://pluzz.francetv.fr/videos/iron_man.html",
            "", "Pluzz", new Timestamp(System.currentTimeMillis()), null, new Cover("http://lol.net/s/date-sortie-iron-man-la-serie-anime-vol-1-dvd.jpg", 250, 166), null, true);

    Podcast SECRET_DHISTOIRE = new Podcast("Secret d'histoire", "http://pluzz.francetv.fr/videos/secrets_d_histoire.html",
            "", "Pluzz", new Timestamp(System.currentTimeMillis()), null, new Cover("http://www.france2.fr/emissions/sites/default/files/images/logo-site/2013/01/02/secrets-dhistoire-19733-29247.png", 250, 166), null, true);


    @Test
    public void signaturePluzz() {

        String signatureSecretDhistoire = pluzzUpdater.signaturePodcast(SECRET_DHISTOIRE);
        String signatureIronMan = pluzzUpdater.signaturePodcast(IRON_MAN);

        String signatureSecretDhistoire2 = pluzzUpdater.signaturePodcast(SECRET_DHISTOIRE);
        String signatureIronMan2 = pluzzUpdater.signaturePodcast(IRON_MAN);

        assertThat(signatureIronMan).isNotEmpty().isNotNull().isEqualTo(signatureIronMan2);
        assertThat(signatureSecretDhistoire).isNotEmpty().isNotNull().isEqualTo(signatureSecretDhistoire2);

    }

    @Test
    public void updateFeedPluzz() {
        Podcast secretDhistoire = pluzzUpdater.updateFeed(SECRET_DHISTOIRE);
        Podcast ironMan = pluzzUpdater.updateFeed(IRON_MAN);

        assertThat(ironMan).isNotNull();
        assertThat(ironMan.getItems()).isNotEmpty().hasSize(8);

        assertThat(secretDhistoire).isNotNull();
        assertThat(secretDhistoire.getItems()).isNotEmpty().hasSize(2);
    }

}
