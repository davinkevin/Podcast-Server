package lan.dk.podcastserver.manager.worker.updater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

//@Component
//@Scope("prototype")
public abstract class AbstractUpdater implements Updater {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${serverURL}")
    private String serverURL;

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

}
