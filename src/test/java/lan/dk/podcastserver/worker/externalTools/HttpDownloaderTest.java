package lan.dk.podcastserver.worker.externalTools;

import lan.dk.podcastserver.context.Mock.MockRepository;
import lan.dk.podcastserver.context.Mock.MockService;
import lan.dk.podcastserver.context.MockWorkerContextConfiguration;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.downloader.HTTPDownloader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

/**
 * Created by kevin on 14/12/2013.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MockWorkerContextConfiguration.class, MockRepository.class, MockService.class})
public class HttpDownloaderTest {

    @Resource
    HTTPDownloader httpDownloader;

    @Test
    public void getBerneWithProblem() {
        Item item = new Item()
                            .setUrlAndHash("http://rtl.proxycast.org/m/media/273073201584.mp3?c=DIVERTISSEMENT&p=a-la-bonne-heure&l3=&l4=&media_url=http%3A%2F%2Fadmedia.rtl.fr%2Fonline%2Fsound%2F2014%2F0127%2F7769188939_l-integrale-philippe-lellouche-et-vanessa-demouy.mp3")
                            .setPodcast(new Podcast());
        item.getPodcast().setTitle("Test");

        //Downloader downloader = workerUtils.getDownloaderByType(canalPlusPodcast.getItems().iterator().next());
        httpDownloader.setItem(item);
        httpDownloader.download();
    }

}
