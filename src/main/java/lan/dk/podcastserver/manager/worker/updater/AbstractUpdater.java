package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Podcast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import javax.validation.Validator;
import java.util.concurrent.Future;

//@Component
//@Scope("prototype")
public abstract class AbstractUpdater implements Updater {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:25.0) Gecko/20100101 Firefox/25.0";

    @Value("${serverURL:http://localhost:8080}") private String serverURL;
    //@Resource PodcastRepository podcastRepository;
    @Resource(name="Validator") Validator validator;


    public Future<Podcast> updateFeedAsync(Podcast podcast) {
        return /*new AsyncResult<>(podcastRepository.save(updateFeed(podcast)))*/
                null;
    }

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

}
