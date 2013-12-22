package lan.dk.podcastserver.worker;

import lan.dk.podcastserver.context.Mock.MockRepository;
import lan.dk.podcastserver.context.Mock.MockService;
import lan.dk.podcastserver.context.MockWorkerContextConfiguration;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.downloader.Downloader;
import lan.dk.podcastserver.manager.worker.updater.CanalPlusUpdater;
import lan.dk.podcastserver.manager.worker.updater.RSSUpdater;
import lan.dk.podcastserver.utils.WorkerUtils;
import org.hamcrest.core.IsNot;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.Timestamp;

import static org.junit.Assert.assertThat;

/**
 * Created by kevin on 14/12/2013.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MockWorkerContextConfiguration.class, MockRepository.class, MockService.class})
public class RssUpdaterTest {

    private final Logger logger = LoggerFactory.getLogger(CanalPlusWorkerTest.class);

    @Autowired
    RSSUpdater rssUpdater;

    @Autowired
    WorkerUtils workerUtils;

    Podcast canalPlusPodcast;

    @Before
    public void initPodcast() {
        logger.debug("InitPodcast");
//        canalPlusPodcast = new Podcast("La Météo de Doria", "http://www.canalplus.fr/c-divertissement/c-le-grand-journal/pid4688-la-meteo-de-doria.html",
//                "", "CanalPlus", new Timestamp(System.currentTimeMillis()), null, new Cover("http://media7.canal-plus.net/image/64/4/321644.jpg", 60, 60));
    }

    @Test
    public void geekIncTest() {
        logger.debug("Download");
        Podcast podcast = new Podcast("Geek Inc HD", "http://www.geekinc.fr/rss/geek-inc-hd.xml", "", "RSS", new Timestamp(System.currentTimeMillis()), null, null);
        rssUpdater.updateFeed(podcast);
        logger.debug(podcast.toString());
    }
}
