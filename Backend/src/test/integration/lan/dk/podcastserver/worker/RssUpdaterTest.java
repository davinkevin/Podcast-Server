package lan.dk.podcastserver.worker;

import lan.dk.podcastserver.business.PodcastBusiness;
import lan.dk.podcastserver.config.BeanConfigScan;
import lan.dk.podcastserver.context.PropertyConfigTest;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.updater.RSSUpdater;
import lan.dk.podcastserver.service.WorkerService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

import static org.junit.Assert.assertThat;

/**
 * Created by kevin on 14/12/2013.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {/*HibernateSearchConfig.class, */BeanConfigScan.class, PropertyConfigTest.class})
@ActiveProfiles("data-embedded")
@Ignore
public class RssUpdaterTest {

    private final Logger logger = LoggerFactory.getLogger(CanalPlusWorkerTest.class);

    @Autowired
    RSSUpdater rssUpdater;

    @Resource
    PodcastBusiness podcastBusiness;

    @Autowired
    WorkerService workerService;

    Podcast canalPlusPodcast;

    @Before
    public void initPodcast() {
        logger.debug("InitPodcast");
//        canalPlusPodcast = new Podcast("La Météo de Doria", "http://www.canalplus.fr/c-divertissement/c-le-grand-journal/pid4688-la-meteo-de-doria.html",
//                "", "CanalPlus", new Timestamp(System.currentTimeMillis()), null, new Cover("http://media7.canal-plus.net/image/64/4/321644.jpg", 60, 60));
    }
/*
    @Test
    public void geekIncTest() {
        logger.debug("Download");
        Podcast podcast = new Podcast("Geek Inc HD", "http://www.geekinc.fr/rss/geek-inc-hd.xml", "", "RSS", new Timestamp(System.currentTimeMillis()), null, null, null, null);
        rssUpdater.updateAndAddItems(podcast);
        logger.debug(podcast.toString());
    }
    @Test
    public void podMyDev() {
        logger.debug("Download");
        Podcast podcast = new Podcast("PodMyDev", "http://pipes.yahoo.com/pipes/pipe.run?URL=http%3A%2F%2Fwww.podmydev.com%2F%3Ffeed%3Dpodcast&_id=1c04fdbe524a909e33308bc4fab9d5ae&_render=rss", "", "RSS", new Timestamp(System.currentTimeMillis()), null, null, null, null);
        rssUpdater.updateAndAddItems(podcast);
        logger.debug(podcast.toString());
    }

    @Test
    public void twig() {
        logger.debug("Download");
        Podcast podcast = new Podcast("TWIG", "http://feeds.twit.tv/twig_video_hd", "", "RSS", new Timestamp(System.currentTimeMillis()), null, null, null, null);
        rssUpdater.updateAndAddItems(podcast);
        logger.debug(podcast.toString());
    }
    @Test
    public void rmcAfterFoot() {
        logger.debug("Download");
        Podcast podcast = new Podcast("After Foot", "http://podcast.rmc.fr/channel59/RMCInfochannel59.xml", "", "RSS", new Timestamp(System.currentTimeMillis()), null, new Cover("http://rmc.bfmtv.com/img/podcast/picto_afterfoot.jpg", 200, 200), null, null);
        podcastBusiness.save(podcast);

        podcast = podcastBusiness.findOne(podcast.getId());

        rssUpdater.updateAndAddItems(podcast);
        logger.debug(podcast.toString());
    }
    @Test
    public void seasonOne() {
        logger.debug("Download");
        //INSERT INTO `podcast` (`id`, `description`, `last_update`, `signature`, `title`, `type`, `url`, `cover_id`) VALUES
        //(47, NULL, NULL, NULL, 'Apple - Keynotes', 'RSS', 'http://itstreaming.apple.com/podcasts/apple_keynotes_1080p/apple_keynotes_1080p.xml', 909);

        Podcast podcast = new Podcast("Season1", "http://www.season1.fr/category/Podcast/feed/", "", "RSS", new Timestamp(System.currentTimeMillis()), null, null, null, null);
        rssUpdater.updateAndAddItems(podcast);
        logger.debug(podcast.toString());
    }*/
}
