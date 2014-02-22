package lan.dk.podcastserver.worker.externalTools;

import lan.dk.podcastserver.config.BeanConfigScan;
import lan.dk.podcastserver.config.JPAEmbeddedContext;
import lan.dk.podcastserver.config.PropertyConfig;
import lan.dk.podcastserver.context.Mock.MockRepository;
import lan.dk.podcastserver.context.Mock.MockService;
import lan.dk.podcastserver.context.MockWorkerContextConfiguration;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.downloader.M3U8Downloader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

/**
 * Created by kevin on 02/02/2014.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PropertyConfig.class, BeanConfigScan.class, JPAEmbeddedContext.class})
@ActiveProfiles("data-embedded")
public class M3U8DownloaderTest {

    @Resource
    M3U8Downloader m3U8Downloader;

    @Test
    public void getSpecificVideo() throws InterruptedException {
        Item item = new Item()
                .setUrlAndHash("http://us-cplus-aka.canal-plus.com/i/1401/LE_PETIT_JOURNAL_BONUS_140110_CAN_396168_video_,MOB,L,H,HD,.mp4.csmil/index_3_av.m3u8")
                .setPodcast(new Podcast());
        item.getPodcast().setTitle("TestM3U8");

        //Downloader downloader = workerUtils.getDownloaderByType(canalPlusPodcast.getItems().iterator().next());
        m3U8Downloader.setItem(item);
        m3U8Downloader.download();


        Thread.sleep(1000000000L);
    }
}
