package lan.dk.podcastserver.worker;

import lan.dk.podcastserver.config.PropertyConfig;
import lan.dk.podcastserver.context.ValidatorConfig;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.updater.PluzzUpdater;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import javax.annotation.Resource;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 12/07/2014.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PropertyConfig.class, ValidatorConfig.class}, loader=AnnotationConfigContextLoader.class)
public class PluzzUpdaterTest {

    private final Logger logger = LoggerFactory.getLogger(PluzzUpdaterTest.class);
    private Podcast COMMENT_CA_VA_BIEN;
    private Podcast SECRET_DHISTOIRE;

    @Resource
    PluzzUpdater pluzzUpdater;

    @Before
    public void initPodcast(){
        COMMENT_CA_VA_BIEN = new Podcast();
        COMMENT_CA_VA_BIEN.setTitle("Comment ça va bien");
        COMMENT_CA_VA_BIEN.setUrl("http://pluzz.francetv.fr/videos/comment_ca_va_bien.html");
        COMMENT_CA_VA_BIEN.setType("Pluzz");
        COMMENT_CA_VA_BIEN.setLastUpdate(ZonedDateTime.now());
        COMMENT_CA_VA_BIEN.setCover(new Cover("http://lol.net/s/date-sortie-iron-man-la-serie-anime-vol-1-dvd.jpg", 250, 166));

        SECRET_DHISTOIRE = new Podcast();
        SECRET_DHISTOIRE.setTitle("Secret d'histoire");
        SECRET_DHISTOIRE.setUrl("http://pluzz.francetv.fr/videos/secrets_d_histoire.html");
        SECRET_DHISTOIRE.setType("Pluzz");
        SECRET_DHISTOIRE.setLastUpdate(ZonedDateTime.now());
        SECRET_DHISTOIRE.setCover(new Cover("http://www.france2.fr/emissions/sites/default/files/images/logo-site/2013/01/02/secrets-dhistoire-19733-29247.png", 250, 166));
    }


    @Test
    public void signaturePluzz() {

        String signatureSecretDhistoire = pluzzUpdater.generateSignature(SECRET_DHISTOIRE);
        String signatureIronMan = pluzzUpdater.generateSignature(COMMENT_CA_VA_BIEN);

        String signatureSecretDhistoire2 = pluzzUpdater.generateSignature(SECRET_DHISTOIRE);
        String signatureIronMan2 = pluzzUpdater.generateSignature(COMMENT_CA_VA_BIEN);

        assertThat(signatureIronMan).isNotEmpty().isNotNull().isEqualTo(signatureIronMan2);
        assertThat(signatureSecretDhistoire).isNotEmpty().isNotNull().isEqualTo(signatureSecretDhistoire2);

    }

    @Test
    public void updateFeedPluzz() {
        Podcast secretDhistoire = pluzzUpdater.updateAndAddItems(SECRET_DHISTOIRE);
        assertThat(secretDhistoire).isNotNull();
        assertThat(secretDhistoire.getItems()).isNotEmpty();

        Podcast commentCaVaBien = pluzzUpdater.updateAndAddItems(COMMENT_CA_VA_BIEN);
        assertThat(commentCaVaBien).isNotNull();
        assertThat(commentCaVaBien.getItems()).isNotEmpty();

    }

}
