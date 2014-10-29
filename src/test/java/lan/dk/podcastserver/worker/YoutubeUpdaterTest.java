package lan.dk.podcastserver.worker;

import lan.dk.podcastserver.context.Mock.MockRepository;
import lan.dk.podcastserver.context.Mock.MockService;
import lan.dk.podcastserver.context.MockWorkerContextConfiguration;
import lan.dk.podcastserver.manager.worker.updater.YoutubeUpdater;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Created by kevin on 21/12/2013.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MockWorkerContextConfiguration.class, MockRepository.class, MockService.class})
public class YoutubeUpdaterTest {

    private final Logger logger = LoggerFactory.getLogger(YoutubeUpdaterTest.class);

    @Autowired
    YoutubeUpdater youtubeUpdater;


    @Before
    public void initPodcast() {
        logger.debug("InitPodcast");
    }
/*

    @Test
    public void updateCauet() {
        Podcast podcast = new Podcast("Cauet", "http://www.youtube.com/channel/UCe2YQ986DdKliHNvE8va4kQ", "", "Youtube", null, null, new Cover(), null, null);
        youtubeUpdater.updateAndAddItems(podcast);

        logger.debug(podcast.toString());

    }

    @Test
    public void updateWillAndCo() {
        Podcast podcast = new Podcast("Will & Co", "http://www.youtube.com/channel/UCzMawL8sevUd5nZ14x4wRpg", "", "Youtube", null, null, new Cover(), null, null);
        youtubeUpdater.updateAndAddItems(podcast);

        logger.debug(podcast.toString());

    }
    @Test
    public void updateNowTechTvFr() {
        Podcast podcast = new Podcast("NowTechTvFr", "https://www.youtube.com/nowtechtvfr", "", "Youtube", null, null, new Cover(), null, null);
        youtubeUpdater.updateAndAddItems(podcast);

        logger.debug(podcast.toString());

    }

    @Test
    public void androTechPlayslist() {
        Podcast podcast = new Podcast("AndroTech", "http://gdata.youtube.com/feeds/api/playlists/PLN6bvn-Db2BoPcPRqSgtvi-TjZgIB9PvW", "", "Youtube", null, null, new Cover(), null, null);
        youtubeUpdater.updateAndAddItems(podcast);

        logger.debug(podcast.toString());

    }
*/

}
