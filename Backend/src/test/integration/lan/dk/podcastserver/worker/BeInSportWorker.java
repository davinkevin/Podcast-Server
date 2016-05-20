package lan.dk.podcastserver.worker;

import lan.dk.podcastserver.context.ValidatorConfig;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.updater.BeInSportsUpdater;
import lan.dk.podcastserver.service.*;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 22/02/2014.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ValidatorConfig.class, PodcastServerParameters.class, SignatureService.class, UrlService.class, JdomService.class, MimeTypeService.class, HtmlService.class, ImageService.class}, loader=AnnotationConfigContextLoader.class)
@Ignore
public class BeInSportWorker {

    private final Logger logger = LoggerFactory.getLogger(BeInSportWorker.class);

    @Autowired BeInSportsUpdater beInSportsUpdater;

    @Test
    public void signatureFeedExpresso() {

        Podcast lexpresso = new Podcast();
        lexpresso.setTitle("L'Expresso")
                .setUrl("http://www.beinsports.com/france/replay/lexpresso");

        String signature = beInSportsUpdater.signatureOf(lexpresso);
        logger.info("Signature 1 : {}", signature);
        String signature2 = beInSportsUpdater.signatureOf(lexpresso);
        logger.info("Signature 2 : {}", signature2);

        assertThat(signature).isEqualTo(signature2);

    }

    @Test
    public void updateFeedExpresso() {

        Podcast lexpresso = new Podcast();
        lexpresso.setTitle("L'Expresso")
                .setUrl("http://www.beinsports.com/france/replay/lexpresso");


        /*"L'expresso", "http://www.beinsports.fr/replay/category/3361/name/lexpresso",
                "", "BeInSport", new Timestamp(System.currentTimeMillis()), null, new Cover("http://www.beinsports.fr/di/library/bein/52/dd/lexpresso_xyp5eq14bu9m1o275gi8i1xlb.jpg?t=1074981292", 250, 166), null, true);*/

        beInSportsUpdater.getItems(lexpresso);
    }
}
