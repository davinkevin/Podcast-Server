package lan.dk.podcastserver.worker;

import lan.dk.podcastserver.context.Mock.MockRepository;
import lan.dk.podcastserver.context.Mock.MockService;
import lan.dk.podcastserver.context.MockWorkerContextConfiguration;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.downloader.Downloader;
import lan.dk.podcastserver.manager.worker.updater.CanalPlusUpdater;
import lan.dk.podcastserver.service.WorkerUtils;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MockWorkerContextConfiguration.class, MockRepository.class, MockService.class})
public class CanalPlusWorkerTest {

    private final Logger logger = LoggerFactory.getLogger(CanalPlusWorkerTest.class);

    @Autowired
    CanalPlusUpdater canalPlusUpdater;

    @Autowired
    WorkerUtils workerUtils;

    Podcast canalPlusPodcast;
/*

    @Before
    public void initPodcast() {
        logger.debug("InitPodcast");
//        canalPlusPodcast = new Podcast("La Météo de Doria", "http://www.canalplus.fr/c-divertissement/c-le-grand-journal/pid4688-la-meteo-de-doria.html",
//                "", "CanalPlus", new Timestamp(System.currentTimeMillis()), null, new Cover("http://media7.canal-plus.net/image/64/4/321644.jpg", 60, 60));
    }

    @Test
    public void updateFeedLePetitJournal() {

        canalPlusPodcast = new Podcast("Le Petit Journal", "http://www.canalplus.fr/c-divertissement/c-le-petit-journal/pid6515-l-emission.html",
                "", "CanalPlus", new Timestamp(System.currentTimeMillis()), null, new Cover("http://media7.canal-plus.net/image/25/2/319252.jpg", 60, 60), null, null);
        logger.debug("Update");
        //canalPlusPodcast = canalPlusUpdater.updateAndAddItems(canalPlusPodcast);
        //org.junit.Assert.assertThat(canalPlusPodcast.getItems().size(), org.hamcrest.core.IsNot(0));
        assertThat(canalPlusPodcast.getItems().size(), IsNot.not(0));

    }

    @Test
    @Transactional
    public void updateFeedLaMeteoDeDoria() {

        canalPlusPodcast = new Podcast("La Météo de Doria", "http://www.canalplus.fr/c-divertissement/c-le-grand-journal/pid4688-la-meteo-de-doria.html",
                "", "CanalPlus", new Timestamp(System.currentTimeMillis()), null, new Cover("http://media7.canal-plus.net/image/64/4/321644.jpg", 60, 60), null, null);
        logger.debug("Update");
        //canalPlusPodcast = canalPlusUpdater.updateAndAddItems(canalPlusPodcast);
        //org.junit.Assert.assertThat(canalPlusPodcast.getItems().size(), org.hamcrest.core.IsNot(0));
        assertThat(canalPlusPodcast.getItems().size(), IsNot.not(0));

    }

    @Test
    public void updateFeedLinstantBarre() {

        canalPlusPodcast = new Podcast("L'instant Barré", "http://www.canalplus.fr/lib/front_tools/ajax/wwwplus_live_onglet.php?pid=3847&ztid=5810&nbPlusVideos1=1",
                "", "CanalPlus", new Timestamp(System.currentTimeMillis()), null, new Cover("http://img15.hostingpics.net/pics/966069Capture20131020092150.png", 60, 60), null, null);
        logger.debug("Update");
        //canalPlusPodcast = canalPlusUpdater.updateAndAddItems(canalPlusPodcast);
        //org.junit.Assert.assertThat(canalPlusPodcast.getItems().size(), org.hamcrest.core.IsNot(0));
        assertThat(canalPlusPodcast.getItems().size(), IsNot.not(0));

    }

    @Test
    public void updateFeedLeTube() {

        canalPlusPodcast = new Podcast("Le Tube", "http://www.canalplus.fr/c-divertissement/pid6427-c-le-tube.html",
                "", "CanalPlus", new Timestamp(System.currentTimeMillis()), null, new Cover("http://img15.hostingpics.net/pics/966069Capture20131020092150.png", 60, 60), null, null);
        logger.debug("Update");
        canalPlusUpdater.updateAndAddItems(canalPlusPodcast);
        //canalPlusPodcast = canalPlusUpdater.updateAndAddItems(canalPlusPodcast);
        //org.junit.Assert.assertThat(canalPlusPodcast.getItems().size(), org.hamcrest.core.IsNot(0));
        //assertThat(canalPlusPodcast.getItems().size(), IsNot.not(0));

    }

    @Test
    public void updateBefore() {

        canalPlusPodcast = new Podcast("Le Before", "http://www.canalplus.fr/c-divertissement/c-le-before-du-grand-journal/pid6429-l-emission.html",
                "", "CanalPlus", new Timestamp(System.currentTimeMillis()), null, new Cover("http://img15.hostingpics.net/pics/966069Capture20131020092150.png", 60, 60), null, null);
        logger.debug("Update");
        canalPlusUpdater.updateAndAddItems(canalPlusPodcast);
        canalPlusUpdater.updateAndAddItems(canalPlusPodcast);
        canalPlusUpdater.updateAndAddItems(canalPlusPodcast);
        //canalPlusPodcast = canalPlusUpdater.updateAndAddItems(canalPlusPodcast);
        //org.junit.Assert.assertThat(canalPlusPodcast.getItems().size(), org.hamcrest.core.IsNot(0));
        //assertThat(canalPlusPodcast.getItems().size(), IsNot.not(0));

    }
*/

    //@Test
    public void downloadItemCanalPlus() {
        logger.debug("Download");
        //canalPlusPodcast = canalPlusUpdater.updateAndAddItems(canalPlusPodcast);

        Downloader downloader = workerUtils.getDownloaderByType(canalPlusPodcast.getItems().iterator().next());

        logger.debug(downloader.getItem().toString());

        logger.debug(downloader.download().toString());

    }

}
