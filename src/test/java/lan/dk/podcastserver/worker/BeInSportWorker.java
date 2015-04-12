package lan.dk.podcastserver.worker;

import lan.dk.podcastserver.manager.worker.updater.BeInSportsUpdater;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * Created by kevin on 22/02/2014.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {/*PropertyConfig.class*/}, loader=AnnotationConfigContextLoader.class)
public class BeInSportWorker {

    private final Logger logger = LoggerFactory.getLogger(BeInSportWorker.class);

    BeInSportsUpdater beInSportsUpdater = new BeInSportsUpdater();

/*
    @Test
    public void signatureFeedExpresso() {

        Podcast lexpresso = new Podcast("L'expresso", "http://www.beinsports.fr/replay/category/3361/name/lexpresso",
                "", "BeInSport", new Timestamp(System.currentTimeMillis()), null, new Cover("http://www.beinsports.fr/di/library/bein/52/dd/lexpresso_xyp5eq14bu9m1o275gi8i1xlb.jpg?t=1074981292", 250, 166), null, true);

        String signature = beInSportUpdater.generateSignature(lexpresso);
        logger.info("Signature 1 : {}", signature);
        String signature2 = beInSportUpdater.generateSignature(lexpresso);
        logger.info("Signature 2 : {}", signature2);

        Assert.assertEquals(signature, signature2);

    }

    @Test
    public void updateFeedExpresso() {

        Podcast lexpresso = new Podcast("L'expresso", "http://www.beinsports.fr/replay/category/3361/name/lexpresso",
                "", "BeInSport", new Timestamp(System.currentTimeMillis()), null, new Cover("http://www.beinsports.fr/di/library/bein/52/dd/lexpresso_xyp5eq14bu9m1o275gi8i1xlb.jpg?t=1074981292", 250, 166), null, true);

        beInSportUpdater.updateAndAddItems(lexpresso);
    }*/
}
