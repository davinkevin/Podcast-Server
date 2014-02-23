package lan.dk.podcastserver.worker.externalTools;

import lan.dk.podcastserver.context.Mock.MockRepository;
import lan.dk.podcastserver.context.Mock.MockService;
import lan.dk.podcastserver.context.MockWorkerContextConfiguration;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.downloader.YoutubeDownloader;
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
public class YoutubeDownloaderTest {

    @Resource
    YoutubeDownloader youtubeDownloader;

    @Test
    public void getYoutubeVideo() {
        Item item = new Item()
                            .setUrl("http://www.youtube.com/watch?v=E-0WGB9WCRY&amp;feature=youtube_gdata")
                            .setPodcast(new Podcast());
        item.getPodcast().setTitle("Test");

        //Downloader downloader = workerUtils.getDownloaderByType(canalPlusPodcast.getItems().iterator().next());
        youtubeDownloader.setItem(item);
        youtubeDownloader.download();
    }

    @Test
    public void getShortVideo() {
        Item item = new Item()
                            .setUrl("http://www.youtube.com/watch?v=Hzgzim5m7oU")
                            .setPodcast(new Podcast());
        item.getPodcast().setTitle("Test");

        //Downloader downloader = workerUtils.getDownloaderByType(canalPlusPodcast.getItems().iterator().next());
        youtubeDownloader.setItem(item);
        youtubeDownloader.download();
    }

}
