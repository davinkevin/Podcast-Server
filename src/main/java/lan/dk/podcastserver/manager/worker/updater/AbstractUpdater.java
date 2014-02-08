package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.repository.PodcastRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.AsyncResult;

import javax.annotation.Resource;
import java.util.concurrent.Future;

//@Component
//@Scope("prototype")
public abstract class AbstractUpdater implements Updater {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${serverURL}")
    private String serverURL;

    @Resource PodcastRepository podcastRepository;

    public Future<Podcast> updateFeedAsync(Podcast podcast) {
        return new AsyncResult<>(podcastRepository.save(updateFeed(podcast)));
    }

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

}
